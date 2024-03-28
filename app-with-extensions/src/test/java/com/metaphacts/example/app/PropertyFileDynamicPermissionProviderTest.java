package com.metaphacts.example.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.metaphacts.example.app.PropertyFileDynamicPermissionProvider.AssetRoleAssignment;
import com.metaphacts.example.app.PropertyFileDynamicPermissionProvider.Role;

class PropertyFileDynamicPermissionProviderTest {

    @Test
    void testPropertiesToPermissionMappings() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getResourceAsStream("/dynamicPermissions.prop"));
        Map<IRI, Set<AssetRoleAssignment>> mappings = PropertyFileDynamicPermissionProvider
                .propertiesToPermissionMappings(properties);

        Assertions.assertEquals(4, mappings.size());

        IRI iri = Values.iri("https://ontologies.metaphacts.com/owner-test/0.1");
        Set<AssetRoleAssignment> roleAssignments = mappings.get(iri);
        Assertions.assertEquals(1, roleAssignments.size());
        AssetRoleAssignment roleAssignment = roleAssignments.iterator().next();
        Assertions.assertEquals(iri, roleAssignment.getIri());
        Assertions.assertEquals(Set.of(Role.AUTHOR, Role.OWNER), roleAssignment.getRoles());
        Assertions.assertEquals("alice", roleAssignment.getUsername());
        Assertions.assertEquals("ontology", roleAssignment.getType());

        iri = Values.iri("https://vocabularies.metaphacts.com/owner-test/0.1");
        roleAssignments = mappings.get(iri);
        Assertions.assertEquals(1, roleAssignments.size());
        roleAssignment = roleAssignments.iterator().next();
        Assertions.assertEquals(iri, roleAssignment.getIri());
        Assertions.assertEquals(Set.of(Role.OWNER), roleAssignment.getRoles());
        Assertions.assertEquals("bob", roleAssignment.getUsername());
        Assertions.assertEquals("vocabulary", roleAssignment.getType());

        iri = Values.iri("https://ontologies.metaphacts.com/author-test/0.1");
        roleAssignments = mappings.get(iri);
        Assertions.assertEquals(2, roleAssignments.size());
        List<AssetRoleAssignment> orderedRoleAssignments = new ArrayList<>(roleAssignments);
        orderedRoleAssignments.sort((o1, o2) -> o1.getUsername().compareTo(o2.getUsername()));
        roleAssignment = orderedRoleAssignments.get(0);
        Assertions.assertEquals(iri, roleAssignment.getIri());
        Assertions.assertEquals(Set.of(Role.AUTHOR), roleAssignment.getRoles());
        Assertions.assertEquals("frank", roleAssignment.getUsername());
        Assertions.assertEquals("ontology", roleAssignment.getType());
        roleAssignment = orderedRoleAssignments.get(1);
        Assertions.assertEquals(iri, roleAssignment.getIri());
        Assertions.assertEquals(Set.of(Role.AUTHOR), roleAssignment.getRoles());
        Assertions.assertEquals("peter@example.com", roleAssignment.getUsername());
        Assertions.assertEquals("ontology", roleAssignment.getType());

        iri = Values.iri("https://vocabularies.metaphacts.com/author-test/0.1");
        roleAssignments = mappings.get(iri);
        Assertions.assertEquals(1, roleAssignments.size());
        roleAssignment = roleAssignments.iterator().next();
        Assertions.assertEquals(iri, roleAssignment.getIri());
        Assertions.assertEquals(Set.of(Role.AUTHOR), roleAssignment.getRoles());
        Assertions.assertEquals("john@example.com", roleAssignment.getUsername());
        Assertions.assertEquals("vocabulary", roleAssignment.getType());
    }

    @Test
    void testParseRoles() {
        Assertions.assertNull(Role.toRoles(null));
        Assertions.assertNull(Role.toRoles("unknown"));
        Assertions.assertEquals(Set.of(Role.AUTHOR), Role.toRoles("author"));
        Assertions.assertEquals(Set.of(Role.OWNER), Role.toRoles("owner"));
        Assertions.assertEquals(Set.of(Role.OWNER, Role.AUTHOR), Role.toRoles("author,owner"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "ontology", "vocabulary" })
    void testRoleStringToRoles(String assetType) {
        Assertions.assertEquals(assetType + "-admin", Role.OWNER.toRoleString(assetType));
        Assertions.assertEquals(assetType + "-edit", Role.AUTHOR.toRoleString(assetType));
    }

}
