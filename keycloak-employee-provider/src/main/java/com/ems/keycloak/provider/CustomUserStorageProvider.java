package com.ems.keycloak.provider;

import com.ems.keycloak.dao.EmployeeDao;
import com.ems.keycloak.entity.EmployeeEntity;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CustomUserStorageProvider implements UserStorageProvider, 
        UserLookupProvider, CredentialInputValidator, UserQueryProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomUserStorageProvider.class);
    
    private final KeycloakSession session;
    private final ComponentModel model;
    private final EmployeeDao employeeDao;
    
    public CustomUserStorageProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        this.employeeDao = new EmployeeDao();
        logger.info("CustomUserStorageProvider initialized");
    }
    
    @Override
    public void close() {
        logger.debug("Closing CustomUserStorageProvider");
    }
    
    @Override
    public UserModel getUserById(RealmModel realm, String id) {
        logger.debug("getUserById called with id: {}", id);
        String externalId = StorageId.externalId(id);
        return getUserByUsername(realm, externalId);
    }
    
    @Override
    public UserModel getUserByUsername(RealmModel realm, String username) {
        logger.debug("getUserByUsername called with username: {}", username);
        
        EmployeeEntity employee = employeeDao.findByEmail(username);
        if (employee != null) {
            return createUserModel(realm, employee);
        }
        
        return null;
    }
    
    @Override
    public UserModel getUserByEmail(RealmModel realm, String email) {
        logger.debug("getUserByEmail called with email: {}", email);
        return getUserByUsername(realm, email);
    }
    
    private UserModel createUserModel(RealmModel realm, EmployeeEntity employee) {
        return new AbstractUserAdapter(session, realm, model) {
            @Override
            public String getUsername() {
                return employee.getEmail();
            }
            
            @Override
            public void setUsername(String username) {
                employee.setEmail(username);
            }
            
            @Override
            public String getEmail() {
                return employee.getEmail();
            }
            
            @Override
            public void setEmail(String email) {
                employee.setEmail(email);
            }
            
            @Override
            public String getFirstName() {
                String[] nameParts = employee.getName().split(" ", 2);
                return nameParts[0];
            }
            
            @Override
            public String getLastName() {
                String[] nameParts = employee.getName().split(" ", 2);
                return nameParts.length > 1 ? nameParts[1] : "";
            }
            
            @Override
            public void setFirstName(String firstName) {
                String lastName = getLastName();
                employee.setName(firstName + (lastName.isEmpty() ? "" : " " + lastName));
            }
            
            @Override
            public void setLastName(String lastName) {
                String firstName = getFirstName();
                employee.setName(firstName + (lastName.isEmpty() ? "" : " " + lastName));
            }
            
            @Override
            public String getId() {
                return StorageId.keycloakId(model, employee.getEmail());
            }
            
            @Override
            public Set<String> getRequiredActions() {
                return new HashSet<>();
            }
            
            @Override
            public Map<String, List<String>> getAttributes() {
                Map<String, List<String>> attributes = super.getAttributes();
                attributes.put("department", Arrays.asList(employee.getDepartment()));
                attributes.put("phone", Arrays.asList(employee.getPhone()));
                
                // Map employee role to Keycloak roles
                List<String> roles = mapEmployeeRoleToKeycloakRoles(employee.getRole());
                attributes.put("roles", roles);
                
                return attributes;
            }
            
            private List<String> mapEmployeeRoleToKeycloakRoles(String employeeRole) {
                List<String> roles = new ArrayList<>();
                
                switch (employeeRole.toUpperCase()) {
                    case "ADMIN":
                        roles.add("FULL_ACCESS");
                        break;
                    case "MANAGER":
                        roles.add("READ_EMPLOYEES");
                        roles.add("CREATE_EMPLOYEES");
                        roles.add("UPDATE_EMPLOYEES");
                        break;
                    case "HR":
                        roles.add("READ_EMPLOYEES");
                        roles.add("CREATE_EMPLOYEES");
                        roles.add("UPDATE_EMPLOYEES");
                        roles.add("DELETE_EMPLOYEES");
                        break;
                    case "EMPLOYEE":
                    default:
                        roles.add("READ_EMPLOYEES");
                        break;
                }
                
                return roles;
            }
        };
    }
    
    @Override
    public boolean supportsCredentialType(String credentialType) {
        return PasswordCredentialModel.TYPE.equals(credentialType);
    }
    
    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return supportsCredentialType(credentialType) && getPassword(user) != null;
    }
    
    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        if (!supportsCredentialType(credentialInput.getType()) || !(credentialInput instanceof UserCredentialModel)) {
            return false;
        }
        
        UserCredentialModel cred = (UserCredentialModel) credentialInput;
        String username = user.getUsername();
        String password = cred.getChallengeResponse();
        
        logger.debug("Validating password for user: {}", username);
        
        boolean isValid = employeeDao.validatePassword(username, password);
        logger.info("Password validation for user {}: {}", username, isValid ? "SUCCESS" : "FAILED");
        
        return isValid;
    }
    
    private String getPassword(UserModel user) {
        String username = user.getUsername();
        EmployeeEntity employee = employeeDao.findByEmail(username);
        return employee != null ? employee.getPassword() : null;
    }
    
    // UserQueryProvider implementation
    @Override
    public int getUsersCount(RealmModel realm) {
        return 0; // Not implemented for this example
    }
    
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, String search, Integer firstResult, Integer maxResults) {
        return Stream.empty(); // Not implemented for this example
    }
    
    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        return Stream.empty(); // Not implemented for this example
    }
    
    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realm, GroupModel group, Integer firstResult, Integer maxResults) {
        return Stream.empty(); // Not implemented for this example
    }
    
    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realm, String attrName, String attrValue) {
        return Stream.empty(); // Not implemented for this example
    }
}