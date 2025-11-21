package com.ems.keycloak.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class CustomUserStorageProviderFactory implements UserStorageProviderFactory<CustomUserStorageProvider> {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProviderFactory.class);
    
    public static final String PROVIDER_NAME = "employee-user-storage";
    
    @Override
    public CustomUserStorageProvider create(KeycloakSession session, ComponentModel model) {
        logger.info("Creating CustomUserStorageProvider instance");
        return new CustomUserStorageProvider(session, model);
    }
    
    @Override
    public String getId() {
        return PROVIDER_NAME;
    }
    
    @Override
    public String getHelpText() {
        return "Employee User Storage Provider - Authenticates users against MySQL employee database";
    }
    
    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }
    
    @Override
    public void validateConfiguration(KeycloakSession session, ComponentModel config) {
        logger.debug("Validating configuration for Employee User Storage Provider");
    }
    
    @Override
    public void onUpdate(KeycloakSession session, ComponentModel oldModel, ComponentModel newModel) {
        logger.info("Employee User Storage Provider configuration updated");
    }
    
    @Override
    public void onCreate(KeycloakSession session, ComponentModel model) {
        logger.info("Employee User Storage Provider created");
    }
}