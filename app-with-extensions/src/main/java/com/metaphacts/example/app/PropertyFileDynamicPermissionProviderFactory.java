/*
 * Copyright (C) 2015-2024, metaphacts GmbH
 */
package com.metaphacts.example.app;

import com.metaphacts.cache.CacheManager;
import com.metaphacts.config.groups.EnvironmentConfiguration;
import com.metaphacts.security.dynamic.DynamicPermissionProvider;
import com.metaphacts.security.dynamic.DynamicPermissionProviderFactory;
import com.metaphacts.services.storage.api.PlatformStorage;

import jakarta.inject.Inject;

/**
 * {@link DynamicPermissionProviderFactory} for {@link PropertyFileDynamicPermissionProvider}.
 * 
 * <p>
 * This class has to be registered in the following file using Java SPI:
 * {@code META-INF/services/com.metaphacts.security.dynamic.DynamicPermissionProviderFactory}
 * </p>
 */
public class PropertyFileDynamicPermissionProviderFactory implements DynamicPermissionProviderFactory {

    @Inject
    private PlatformStorage platformStorage;

    @Inject
    private EnvironmentConfiguration environmentConfiguration;

    @Inject
    private CacheManager cacheManager;

    @Override
    public DynamicPermissionProvider createProvider() {
        return new PropertyFileDynamicPermissionProvider(platformStorage, environmentConfiguration, cacheManager);
    }

    @Override
    public String getName() {
        return PropertyFileDynamicPermissionProvider.class.getSimpleName();
    }

}
