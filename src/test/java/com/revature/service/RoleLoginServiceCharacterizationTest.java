package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.Car;
import com.revature.cardealer.User;

class RoleLoginServiceCharacterizationTest {

    private PrintStream originalOut;

    @BeforeEach
    void rememberOutput() {
        originalOut = System.out;
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void customerAuthenticationRejectsUnregisteredUser() {
        CustomerLoginService service = new CustomerLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void employeeAuthenticationRejectsUnregisteredUser() {
        EmployeeLoginService service = new EmployeeLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void administratorAuthenticationRejectsUnregisteredUser() {
        AdminLoginService service = new AdminLoginService();

        assertFalse(service.authenticateUser(user("unknown", "wrong")));
    }

    @Test
    @Tag("target-behavior")
    void roleServicesAuthenticateRegisteredCredentials() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        CustomerLoginService customerService = new CustomerLoginService(repository);
        customerService.registerUser("customer", "secret");
        assertTrue(customerService.authenticateUser(user("customer", "secret")));

        User bootstrapAdmin = user("bootstrap-admin", new Pbkdf2PasswordHasher().hash("bootstrap-secret"));
        bootstrapAdmin.setRole(com.revature.cardealer.AccountRole.ADMINISTRATOR);
        repository.save(bootstrapAdmin);
        UserLoginService provisioner = new UserLoginService(repository);
        provisioner.provisionUser(user("bootstrap-admin", "bootstrap-secret"),
                "employee", "secret", com.revature.cardealer.AccountRole.EMPLOYEE);
        provisioner.provisionUser(user("bootstrap-admin", "bootstrap-secret"),
                "admin", "secret", com.revature.cardealer.AccountRole.ADMINISTRATOR);

        assertTrue(new EmployeeLoginService(repository).authenticateUser(user("employee", "secret")));
        assertTrue(new AdminLoginService(repository).authenticateUser(user("admin", "secret")));
    }

    @Test
    @Tag("target-behavior")
    @Tag("security")
    void customerAuthenticationDoesNotPrintSubmittedUsername() {
        CustomerLoginService service = new CustomerLoginService();
        service.registerUser("dane", "secret");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.authenticateUser(user("dane", "secret"));

        assertFalse(output.toString().contains("dane"));
    }

    @Test
    void customerOwnedCarStorageSupportsMoreThanLegacyFixedArrayCapacity() {
        CustomerLoginService service = new CustomerLoginService();
        for (int i = 0; i < 20; i++) {
            service.setCarsOwned(new Car("Make" + i, "Model" + i, 2000 + i), 10000 + i, 12);
        }

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> service.setCarsOwned(new Car("Additional", "Vehicle", 2020), 20000, 12));
        org.junit.jupiter.api.Assertions.assertEquals(21, service.getOwnedVehicleRecords().size());
    }

    @Test
    void viewingOwnedCarsHandlesUnpopulatedSlots() {
        CustomerLoginService service = new CustomerLoginService();
        service.setCarsOwned(new Car("Toyota", "Corolla", 2010), 10000, 12);
        System.setOut(new PrintStream(new ByteArrayOutputStream()));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.getCarsOwned());
    }

    @Test
    void viewingEmptyOwnedCarsPrintsEmptyState() {
        CustomerLoginService service = new CustomerLoginService();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> service.getCarsOwned());
        assertTrue(output.toString().contains("No vehicles owned."));
    }

    @Test
    void ownedCarStorageRejectsInvalidPaymentTermsAndNullVehicles() {
        CustomerLoginService service = new CustomerLoginService();
        assertThrows(IllegalArgumentException.class,
                () -> service.setCarsOwned(new Car("Toyota", "Corolla", 2010), 10000, 0));
        assertThrows(NullPointerException.class,
                () -> service.setCarsOwned(null, 10000, 12));
    }

    @Test
    void customerPaymentsUpdateTheCorrectVehicleAndRejectOverpayment() {
        CustomerLoginService service = new CustomerLoginService();
        service.setCarsOwned(new Car("Honda", "Accord", 2018), 12000, 12);
        service.setCarsOwned(new Car("Toyota", "Corolla", 2020), 6000, 6);
        service.recordPayment(0, 1000);
        service.setPayments(500);
        org.junit.jupiter.api.Assertions.assertEquals(11000,
                service.getOwnedVehicle(0).getPaymentPlan().getRemainingBalance());
        org.junit.jupiter.api.Assertions.assertEquals(5500,
                service.getOwnedVehicle(1).getPaymentPlan().getRemainingBalance());
        assertThrows(IllegalArgumentException.class, () -> service.recordPayment(1, 6000));
        org.junit.jupiter.api.Assertions.assertEquals(5500,
                service.getOwnedVehicle(1).getPaymentPlan().getRemainingBalance());
    }

    @Test
    void paymentsWithoutAnOwnedVehicleFailClosed() {
        CustomerLoginService service = new CustomerLoginService();
        assertThrows(IllegalStateException.class, () -> service.setPayments(100));
        assertThrows(IndexOutOfBoundsException.class, () -> service.recordPayment(0, 100));
        assertThrows(IllegalArgumentException.class, () -> service.setPayments((String[]) null));
    }

    @Test
    void customerOwnershipRecordsAreIsolatedByBuyerUsername() {
        CustomerLoginService service = new CustomerLoginService();
        service.recordPurchaseForCustomer("buyer-a", new Car("Honda", "Accord", 2018), 12000, 12);
        service.recordPurchaseForCustomer("buyer-b", new Car("Toyota", "Corolla", 2020), 9000, 12);
        org.junit.jupiter.api.Assertions.assertEquals(1, service.getOwnedVehicleRecords("buyer-a").size());
        org.junit.jupiter.api.Assertions.assertEquals(1, service.getOwnedVehicleRecords("buyer-b").size());
        org.junit.jupiter.api.Assertions.assertEquals(0, service.getOwnedVehicleRecords("reviewer").size());
        org.junit.jupiter.api.Assertions.assertEquals(12000,
                service.getOwnedVehicleRecords("buyer-a").get(0).getPaymentPlan().getPurchasePrice());
        assertThrows(IllegalArgumentException.class,
                () -> service.recordPurchaseForCustomer("", new Car("Honda", "Civic", 2021), 10000, 12));
    }

    @Test
    void administratorDeleteAllUsersOnlyEmitsWarningText() {
        AdminLoginService service = new AdminLoginService();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        service.deleteAllUsers();

        assertTrue(output.toString().contains("You better be sure"));
    }

    private static User user(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        return user;
    }
}
