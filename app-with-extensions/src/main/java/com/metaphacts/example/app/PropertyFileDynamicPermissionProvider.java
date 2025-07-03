/*
 * Copyright (C) 2015-2024, metaphacts GmbH
 */
package com.metaphacts.example.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;

import com.metaphacts.cache.CacheManager;
import com.metaphacts.config.Configuration;
import com.metaphacts.config.groups.EnvironmentConfiguration;
import com.metaphacts.security.MetaphactsSecurityManager;
import com.metaphacts.security.Permissions.ONTOLOGIES;
import com.metaphacts.security.Permissions.ROLES;
import com.metaphacts.security.Permissions.VOCABULARIES;
import com.metaphacts.security.PlatformRoleManager;
import com.metaphacts.security.SecurityService;
import com.metaphacts.security.dynamic.AbstractCachedDynamicPermissionProvider;
import com.metaphacts.security.dynamic.ContextObject;
import com.metaphacts.security.dynamic.DynamicContextCache;
import com.metaphacts.security.dynamic.DynamicPermissionProvider;
import com.metaphacts.services.assets.OntologyAssetType;
import com.metaphacts.services.assets.VocabularyAssetType;
import com.metaphacts.services.storage.api.ObjectKind;
import com.metaphacts.services.storage.api.ObjectRecord;
import com.metaphacts.services.storage.api.PlatformStorage;
import com.metaphacts.services.storage.api.StoragePath;

/**
 * {@link DynamicPermissionProvider} for asset (ontology, vocabulary) ownership and authorship permissions based on
 * configurations in property files.
 * 
 * <p>
 * The file {@value #PROPERTY_FILENAME} has to be located in the config folder of the registered security storage (see
 * {@link EnvironmentConfiguration#getSecurityConfigStorageId()} setting) and the provider has to be configured in the
 * environment configuration property for shiro dynamic permission providers (see
 * {@link EnvironmentConfiguration#getShiroDynamicPermissionProviders()). If no security storage is configured, this
 * provider does nothing. Permissions are only loaded once during startup. Therefore, permission changes require a
 * restart of the platform.
 * </p>
 * 
 * <p>
 * Example of entry in property file:
 * </p>
 * <code>
 *  myOntology.iri=https://ontologies.metaphacts.com/access-test/0.1
 *  myOntology.type=ontology
 *  myOntology.username=tester
 *  myOntology.roles=author,owner
 * </code>
 */
public class PropertyFileDynamicPermissionProvider extends AbstractCachedDynamicPermissionProvider {

    private static final Logger logger = LogManager.getLogger(PropertyFileDynamicPermissionProvider.class);
    private static final String PROPERTY_FILENAME = "dynamicPermissions.prop";
    private static final String CACHE_IDENTIFIER = "app.security.dynamic.PropertyFileDynamicPermissionCache";

    // Mapping from asset IRIs to their configured AssetRoleAssignments
    private Map<IRI, Set<AssetRoleAssignment>> permissionMappings = new HashMap<>();

    public PropertyFileDynamicPermissionProvider(PlatformStorage platformStorage,
            EnvironmentConfiguration environmentConfig, CacheManager cacheManager) {
        super(getCache(cacheManager));
        importPermissionMappings(platformStorage, environmentConfig);
    }

    @Override
    protected List<Permission> doGetDynamicPermissions(ContextObject contextObject) {
        if (permissionMappings.isEmpty()) {
            return Collections.emptyList();
        }

        // context object contains the IRI of an asset
        IRI assetIri = contextObject.getId();
        Set<AssetRoleAssignment> assetRoleAssignments = permissionMappings.get(assetIri);

        if (assetRoleAssignments == null) {
            return Collections.emptyList();
        }

        logger.trace("Found {} permission configuration(s) applicable for {}", assetRoleAssignments.size(), assetIri);

        String username = SecurityService.getUserName();
        PlatformRoleManager roleManager = ((MetaphactsSecurityManager) SecurityUtils.getSecurityManager())
                .getPlatformRoleManager();

        List<Permission> result = new ArrayList<>();
        assetRoleAssignments.stream().forEach(assetRoleAssignment -> {

            if (!assetRoleAssignment.username.equals(username)) {
                return;
            }

            assetRoleAssignment.roles.forEach(role -> {
                logger.trace("Found dynamic role for '{}': {}", assetIri, role);
                result.addAll(roleManager.getRole(role.toRoleString(assetRoleAssignment.type)).getPermissions());
            });
        });

        return result;
    }

    @Override
    public boolean isApplicable(Permission permission) {
        String permissionString = permission.toString();
        return permissionString.startsWith(ONTOLOGIES.PREFIX) || permissionString.startsWith(VOCABULARIES.PREFIX);
    }

    /**
     * Import permission mappings from the property file {@value #PROPERTY_FILENAME}.
     * 
     * @param platformStorage Injected {@link PlatformStorage} instance
     */
    private void importPermissionMappings(PlatformStorage platformStorage, EnvironmentConfiguration environmentConfig) {
        Properties properties;
        try (InputStream is = getPropertyFileInputStream(platformStorage, environmentConfig)) {
            if (is == null) {
                return;
            }
            // Load property file and parse entries into PropertyPermission objects
            properties = loadPropertyFile(is);
        } catch (IOException e) {
            logger.warn("Failed to get dynamic permission property file: ", e.getMessage());
            logger.debug("Details: ", e);
            return;
        }

        permissionMappings = propertiesToPermissionMappings(properties);
    }

    private InputStream getPropertyFileInputStream(PlatformStorage platformStorage,
            EnvironmentConfiguration environmentConfig) throws IOException {
        StoragePath storagePath = ObjectKind.CONFIG.resolve(PROPERTY_FILENAME);
        String securityConfigStorageId = environmentConfig.getSecurityConfigStorageId();

        // Try to read permission property file from security storage
        if (securityConfigStorageId != null) {
            Optional<ObjectRecord> objectRecord = platformStorage.getStorage(securityConfigStorageId)
                    .getObject(storagePath, null);
            if (objectRecord.isEmpty()) {
                logger.debug("No dynamic permission property file found in security storage");
                return null;
            }
            logger.debug("Loading content of dynamic permission property file '{}'",
                    objectRecord.get().getPath().toString());
            return objectRecord.get().getLocation().readContent();
        } else { // fallback if no security storage is configured => try to read property file from filesystem
            logger.debug("No security storage configured");
            File propertyFile = Paths.get(Configuration.getConfigBasePath() + PROPERTY_FILENAME).toFile();
            if (!propertyFile.exists()) {
                logger.debug("No dynamic permission property file found in {}", Configuration.getConfigBasePath());
                return null;
            }
            logger.debug("Loading content of dynamic permission property file '{}'", propertyFile.getAbsolutePath());
            return new FileInputStream(propertyFile);
        }
    }

    protected static Map<IRI, Set<AssetRoleAssignment>> propertiesToPermissionMappings(Properties properties) {
        // Use property identifier as key to group entries
        Map<String, AssetRoleAssignment> propertyMappings = new HashMap<>();

        for (Entry<Object, Object> entry : properties.entrySet()) {
            mapToAssetRoleAssignment(propertyMappings, entry.getKey(), entry.getValue());
        }

        int validAssetRoleAssignmentsCount = 0;
        Map<IRI, Set<AssetRoleAssignment>> resultMap = new HashMap<>();
        // Filter out invalid property permissions and re-map them for easier lookup based on their asset IRI
        for (Entry<String, AssetRoleAssignment> assetRoleAssignmentEntry : propertyMappings.entrySet()) {
            AssetRoleAssignment assetRoleAssignment = assetRoleAssignmentEntry.getValue();
            if (assetRoleAssignment.isValid()) {
                resultMap.computeIfAbsent(assetRoleAssignment.iri, k -> new HashSet<>())
                        .add(assetRoleAssignment);
                validAssetRoleAssignmentsCount++;
            } else {
                logger.trace("Invalid asset role assignment '{}' has been ignored",
                        assetRoleAssignmentEntry.getKey());
            }
        }
        logger.debug("{} permission definitions have been configured for {} different assets",
                validAssetRoleAssignmentsCount, resultMap.size());

        return resultMap;
    }

    private static DynamicContextCache<List<Permission>> getCache(CacheManager cacheManager) {
        DynamicContextCache<List<Permission>> cache = DynamicContextCache.getOrCreate(cacheManager, CACHE_IDENTIFIER,
                cacheBuilder -> cacheBuilder.expireAfterWrite(Duration.ofMinutes(5)));
        // Disable cache invalidation after repository writes
        cache.setInvalidateAfterWrite(false);
        return cache;
    }

    private static Properties loadPropertyFile(InputStream is) {
        Properties permissionProperties = new Properties();
        try {
            permissionProperties.load(is);
        } catch (IOException e) {
            logger.warn("Failed to load content of dynamic permission property file: {}", e.getMessage());
            logger.debug("Details: ", e);
        }
        return permissionProperties;
    }

    private static void mapToAssetRoleAssignment(Map<String, AssetRoleAssignment> mapping, Object key, Object value) {
        String property = key.toString();
        String val = value.toString();
        int idx = property.indexOf(".");
        if (idx == -1) {
            logger.debug("Wrong permission property syntax in '{}'", property);
            return;
        }
        String id = property.substring(0, idx);
        AssetRoleAssignment assetRoleAssignment = mapping.getOrDefault(id, new AssetRoleAssignment());
        String subProperty = property.substring(idx + 1);
        switch (subProperty.toLowerCase()) {
            case "iri" -> assetRoleAssignment.iri = Values.iri(val);
            case "type" -> assetRoleAssignment.type = toAssetType(val);
            case "username" -> assetRoleAssignment.username = val;
            case "roles" -> assetRoleAssignment.roles = Role.toRoles(val);
            default -> logger.warn("Unknown property: {}", subProperty);
        }
        
        mapping.put(id, assetRoleAssignment);
    }

    private static String toAssetType(String val) {
        return val.equalsIgnoreCase(OntologyAssetType.TYPE_NAME) ? "ontology"
                : val.equalsIgnoreCase(VocabularyAssetType.TYPE_NAME) ? "vocabulary" : null;
    }
    
    /**
     * Helper class for asset role assignments.
     */
    protected static class AssetRoleAssignment {
        private IRI iri;
        private String type;
        private String username;
        private Set<Role> roles = new HashSet<>();

        public IRI getIri() {
            return iri;
        }

        public String getType() {
            return type;
        }

        public String getUsername() {
            return username;
        }

        public Set<Role> getRoles() {
            return roles;
        }

        /**
         * Check if a {@link AssetRoleAssignment} has all of its fields set
         * 
         * @return <code>true</code> if this is a valid {@link AssetRoleAssignment}, otherwise <code>false</code>
         */
        private boolean isValid() {
            return iri != null 
                    && type != null && !type.isBlank()
                    && username != null && !username.isBlank()
                    && roles != null && !roles.isEmpty();
        }

    }

    /**
     * Enum for owner and author roles.
     */
    protected enum Role {
        OWNER, AUTHOR;

        /**
         * Map this {@link Role} to its role fragment in the platform based on the given asset type.
         * 
         * <p>
         * Example: OWNER(Role) + ontology(type) => ontology-admin
         * <p>
         * 
         * @param type Type of the asset. Possible values: {@code ontology}, {@code vocabulary}
         * @return a {@link String} containing the corresponding role fragment in the platform
         */
        String toRoleString(String type) {
            return switch (this) {
                case OWNER -> type + "-admin";
                case AUTHOR -> type + "-edit";
            };
        }

        /**
         * Parses a given {@link String} into a {@link Set} of {@link Role}s.
         * 
         * @param roleString The {@link String} containing a comma-separated list of {@link ROLES} (case-insensitive)
         * @return a {@link Set} of the parsed {@link Role}s or <code>null</code> if the roles are not parsable
         */
        static Set<Role> toRoles(String roleString) {
            if (roleString == null || roleString.isBlank()) {
                return null;
            }
            try {
                return Arrays.asList(roleString.split(",")).stream().map(v -> Role.valueOf(v.trim().toUpperCase()))
                        .collect(Collectors.toSet());
            } catch (IllegalArgumentException e) {
                logger.debug("{} can not be read as a role", roleString);
                return null;
            }
        }
    }

}
