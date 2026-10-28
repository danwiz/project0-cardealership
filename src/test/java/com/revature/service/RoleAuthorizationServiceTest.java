package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;

class RoleAuthorizationServiceTest {

    private final RoleAuthorizationService authorization = new RoleAuthorizationService();

    @Test
    @Tag("security")
    void customerReceivesOnlyCustomerCapabilities() {
        User customer = account("customer", AccountRole.CUSTOMER);

        assertTrue(authorization.isAuthorized(customer, Permission.VIEW_INVENTORY));
        assertTrue(authorization.isAuthorized(customer, Permission.REQUEST_PURCHASE));
        assertTrue(authorization.isAuthorized(customer, Permission.VIEW_OWNED_VEHICLES));
        assertFalse(authorization.isAuthorized(customer, Permission.MANAGE_INVENTORY));
        assertFalse(authorization.isAuthorized(customer, Permission.MANAGE_USERS));
        assertFalse(authorization.isAuthorized(customer, Permission.MANAGE_PERSISTENCE));
    }

    @Test
    @Tag("security")
    void employeeCanManageDealershipOperationsButNotAdministration() {
        User employee = account("employee", AccountRole.EMPLOYEE);

        assertTrue(authorization.isAuthorized(employee, Permission.MANAGE_INVENTORY));
        assertTrue(authorization.isAuthorized(employee, Permission.REVIEW_PURCHASE_REQUESTS));
        assertTrue(authorization.isAuthorized(employee, Permission.VIEW_CUSTOMER_PAYMENTS));
        assertFalse(authorization.isAuthorized(employee, Permission.MANAGE_USERS));
        assertFalse(authorization.isAuthorized(employee, Permission.MANAGE_PERSISTENCE));
    }

    @Test
    @Tag("security")
    void administratorReceivesEveryDefinedPermission() {
        User administrator = account("admin", AccountRole.ADMINISTRATOR);

        for (Permission permission : Permission.values()) {
            assertTrue(authorization.isAuthorized(administrator, permission));
        }
    }

    @Test
    void missingIdentityDataIsDeniedSafely() {
        User withoutRole = new User();
        withoutRole.setRole(null);

        assertFalse(authorization.isAuthorized(null, Permission.VIEW_INVENTORY));
        assertFalse(authorization.isAuthorized(withoutRole, Permission.VIEW_INVENTORY));
        assertFalse(authorization.isAuthorized(account("dane", AccountRole.CUSTOMER), null));
        assertThrows(SecurityException.class,
                () -> authorization.requireAuthorized(account("dane", AccountRole.CUSTOMER), Permission.MANAGE_USERS));
    }

    @Test
    @Tag("target-behavior")
    void authenticationReturnsVerifiedAccountWithPersistedRole() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        provisionEmployee(repository);
        EmployeeLoginService service = new EmployeeLoginService(repository);

        Optional<User> authenticated = service.authenticate(credentials("employee", "secret"));

        assertTrue(authenticated.isPresent());
        assertEquals(AccountRole.EMPLOYEE, authenticated.orElseThrow().getRole());
        assertFalse(service.authenticate(credentials("employee", "wrong")).isPresent());
    }

    @Test
    @Tag("security")
    void unauthenticatedRegistrationCannotGrantElevatedRoles() {
        assertEquals(AccountRole.CUSTOMER,
                new CustomerLoginService().registerUser("customer", "secret").getRole());
        assertThrows(SecurityException.class,
                () -> new EmployeeLoginService().registerUser("employee", "secret"));
        assertThrows(SecurityException.class,
                () -> new AdminLoginService().registerUser("admin", "secret"));
    }

    @Test
    @Tag("security")
    void forgedCustomerRoleAndInvalidAdministratorPasswordCannotProvisionStaff() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        seedAdministrator(repository);
        UserLoginService service = new UserLoginService(repository);
        User customer = service.registerUser("customer", "pw");
        customer.setRole(AccountRole.ADMINISTRATOR);

        User forgedCredentials = credentials("customer", "pw");
        forgedCredentials.setRole(AccountRole.ADMINISTRATOR);
        assertThrows(SecurityException.class,
                () -> service.provisionUser(forgedCredentials, "employee", "pw", AccountRole.EMPLOYEE));
        assertThrows(SecurityException.class,
                () -> service.provisionUser(credentials("bootstrap-admin", "wrong"),
                        "employee", "pw", AccountRole.EMPLOYEE));
    }

    @Test
    @Tag("security")
    void callerMutationCannotPromoteStoredAccount() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        UserLoginService service = new UserLoginService(repository);
        User returned = service.registerUser("customer", "pw");
        returned.setRole(AccountRole.ADMINISTRATOR);
        User queried = repository.findByUsername("customer").orElseThrow();
        queried.setRole(AccountRole.ADMINISTRATOR);

        User authenticated = service.authenticate(credentials("customer", "pw")).orElseThrow();
        assertEquals(AccountRole.CUSTOMER, authenticated.getRole());
        assertFalse(authorization.isAuthorized(authenticated, Permission.MANAGE_USERS));
    }

    @Test
    @Tag("security")
    void verifiedAdministratorCanProvisionOnlyThroughAuthenticatedPath() {
        InMemoryUserAccountRepository repository = new InMemoryUserAccountRepository();
        seedAdministrator(repository);
        UserLoginService service = new UserLoginService(repository);

        User employee = service.provisionUser(credentials("bootstrap-admin", "bootstrap-secret"),
                "employee", "secret", AccountRole.EMPLOYEE);
        assertEquals(AccountRole.EMPLOYEE, employee.getRole());
        User admin = service.provisionUser(credentials("bootstrap-admin", "bootstrap-secret"),
                "other-admin", "secret", AccountRole.ADMINISTRATOR);
        assertEquals(AccountRole.ADMINISTRATOR, admin.getRole());
    }

    private static void provisionEmployee(UserAccountRepository repository) {
        seedAdministrator(repository);
        new UserLoginService(repository).provisionUser(
                credentials("bootstrap-admin", "bootstrap-secret"),
                "employee", "secret", AccountRole.EMPLOYEE);
    }

    private static void seedAdministrator(UserAccountRepository repository) {
        User administrator = account("bootstrap-admin", AccountRole.ADMINISTRATOR);
        administrator.setPassword(new Pbkdf2PasswordHasher().hash("bootstrap-secret"));
        repository.save(administrator);
    }

    private static User account(String username, AccountRole role) {
        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private static User credentials(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
