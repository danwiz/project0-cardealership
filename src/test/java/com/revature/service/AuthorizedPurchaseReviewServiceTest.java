package com.revature.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Offer;
import com.revature.cardealer.User;

class AuthorizedPurchaseReviewServiceTest {
    private static User user(String name, String password) {
        User user = new User();
        user.setUsername(name);
        user.setPassword(password);
        return user;
    }

    private static void store(UserAccountRepository repository, String name,
            String password, AccountRole role) {
        User account = user(name, new Pbkdf2PasswordHasher().hash(password));
        account.setRole(role);
        repository.save(account);
    }

    @Test
    @Tag("security")
    void customerCannotApproveOrRejectRequestsEvenWithForgedRole() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "buyer", "secret", AccountRole.CUSTOMER);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, new Offer());
        User forged = user("buyer", "secret");
        forged.setRole(AccountRole.ADMINISTRATOR);
        assertThrows(SecurityException.class, () -> service.approve(forged, 0, 12));
        assertThrows(SecurityException.class, () -> service.rejectAll(forged));
    }

    @Test
    @Tag("security")
    void revokedEmployeeCannotReviewRequests() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, new Offer());
        repository.removeByUsername("staff");
        assertThrows(SecurityException.class, () -> service.approve(user("staff", "secret"), 0, 12));
    }

    @Test
    @Tag("security")
    void approvalReturnsFullPriceNotMonthlyInstallmentAndConsumesRequest() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 2);
        offers.setpOffer("buyer", 0);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        org.junit.jupiter.api.Assertions.assertEquals(15000,
                service.approve(user("staff", "secret"), 0, 12));
        org.junit.jupiter.api.Assertions.assertEquals(1250, offers.getMpay());
        assertThrows(IllegalArgumentException.class,
                () -> service.approve(user("staff", "secret"), 0, 12));
    }

    @Test
    @Tag("security")
    void pendingBuyerLookupRequiresReviewerAndReturnsRequester() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        store(repository, "buyer", "secret", AccountRole.CUSTOMER);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("buyer", 0);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        org.junit.jupiter.api.Assertions.assertEquals("buyer",
                service.pendingBuyer(user("staff", "secret"), 0));
        assertThrows(SecurityException.class,
                () -> service.pendingBuyer(user("buyer", "secret"), 0));
    }

    @Test
    @Tag("security")
    void missingBuyerCannotConsumeInventoryOrPendingRequest() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("missing-buyer", 0);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        CustomerLoginService customers = new CustomerLoginService(repository);
        assertThrows(SecurityException.class,
                () -> service.approveForCustomer(user("staff", "secret"), 0, 12, customers));
        org.junit.jupiter.api.Assertions.assertEquals(1, offers.getListingStock(0));
        org.junit.jupiter.api.Assertions.assertEquals("missing-buyer", offers.getPendingBuyer(0));
        org.junit.jupiter.api.Assertions.assertEquals(0, customers.getOwnedVehicleRecords("missing-buyer").size());
    }

    @Test
    @Tag("security")
    void purchaseApprovalAssignsRegisteredBuyerAndConsumesInventoryOnce() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        store(repository, "buyer", "secret", AccountRole.CUSTOMER);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("buyer", 0);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        CustomerLoginService customers = new CustomerLoginService(repository);
        service.approveForCustomer(user("staff", "secret"), 0, 12, customers);
        org.junit.jupiter.api.Assertions.assertEquals(0, offers.getListingStock(0));
        org.junit.jupiter.api.Assertions.assertEquals(1, customers.getOwnedVehicleRecords("buyer").size());
        org.junit.jupiter.api.Assertions.assertEquals(15000,
                customers.getOwnedVehicleRecords("buyer").get(0).getPaymentPlan().getPurchasePrice());
        assertThrows(IllegalArgumentException.class,
                () -> service.approveForCustomer(user("staff", "secret"), 0, 12, customers));
        org.junit.jupiter.api.Assertions.assertEquals(1, customers.getOwnedVehicleRecords("buyer").size());
    }

    @Test
    @Tag("security")
    void authorizedEmployeeReceivesInputValidation() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, new Offer());
        assertThrows(IllegalArgumentException.class,
                () -> service.approve(user("staff", "secret"), -1, 12));
        assertThrows(IllegalArgumentException.class,
                () -> service.approve(user("staff", "secret"), 0, 0));
    }
}
