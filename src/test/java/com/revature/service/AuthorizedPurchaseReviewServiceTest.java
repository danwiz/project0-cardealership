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
        assertThrows(SecurityException.class, () -> service.approveForCustomer(forged, 0, 12, new CustomerLoginService(repository)));
        assertThrows(SecurityException.class, () -> service.rejectAll(forged));
    }

    @Test
    @Tag("security")
    void revokedEmployeeCannotReviewRequests() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, new Offer());
        repository.removeByUsername("staff");
        assertThrows(SecurityException.class, () -> service.approveForCustomer(user("staff", "secret"), 0, 12, new CustomerLoginService(repository)));
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
    void soldOutApprovalLeavesBuyerOwnershipUnchanged() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        store(repository, "buyer", "secret", AccountRole.CUSTOMER);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("buyer", 0);
        CustomerLoginService customers = new CustomerLoginService(repository);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        service.approveForCustomer(user("staff", "secret"), 0, 12, customers);
        assertThrows(IllegalArgumentException.class,
                () -> service.approveForCustomer(user("staff", "secret"), 0, 12, customers));
        org.junit.jupiter.api.Assertions.assertEquals(1, customers.getOwnedVehicleRecords("buyer").size());
        org.junit.jupiter.api.Assertions.assertEquals(0, offers.getListingStock(0));
    }

    @Test
    @Tag("security")
    void inventoryCommitFailureRemovesOnlyStagedOwnership() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        store(repository, "buyer", "secret", AccountRole.CUSTOMER);
        Offer offers = new Offer() {
            @Override
            public synchronized int setAccept(int listing, int months, boolean accept) {
                throw new IllegalStateException("simulated inventory commit failure");
            }
        };
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("buyer", 0);
        CustomerLoginService customers = new CustomerLoginService(repository);
        customers.recordPurchaseForCustomer("buyer", new com.revature.cardealer.Car("Ford", "Focus", 2017), 9000, 12);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        assertThrows(IllegalStateException.class,
                () -> service.approveForCustomer(user("staff", "secret"), 0, 12, customers));
        org.junit.jupiter.api.Assertions.assertEquals(1, offers.getListingStock(0));
        org.junit.jupiter.api.Assertions.assertEquals("buyer", offers.getPendingBuyer(0));
        org.junit.jupiter.api.Assertions.assertEquals(1, customers.getOwnedVehicleRecords("buyer").size());
    }

    @Test
    @Tag("security")
    void ownershipWriteFailureDoesNotConsumeInventoryOrPendingRequest() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        store(repository, "buyer", "secret", AccountRole.CUSTOMER);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("buyer", 0);
        CustomerLoginService failingLedger = new CustomerLoginService(repository) {
            @Override
            public synchronized void addPreparedPurchaseForCustomer(String buyer,
                    com.revature.cardealer.OwnedVehicle record) {
                throw new IllegalStateException("simulated ownership write failure");
            }
        };
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        assertThrows(IllegalStateException.class,
                () -> service.approveForCustomer(user("staff", "secret"), 0, 12, failingLedger));
        org.junit.jupiter.api.Assertions.assertEquals(1, offers.getListingStock(0));
        org.junit.jupiter.api.Assertions.assertEquals("buyer", offers.getPendingBuyer(0));
        org.junit.jupiter.api.Assertions.assertTrue(failingLedger.getOwnedVehicleRecords("buyer").isEmpty());
    }

    @Test
    @Tag("security")
    void concurrentApprovalsCannotSellOneVehicleTwice() throws Exception {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        store(repository, "buyer-a", "secret", AccountRole.CUSTOMER);
        store(repository, "buyer-b", "secret", AccountRole.CUSTOMER);
        Offer offers = new Offer();
        offers.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offers.setpOffer("buyer-a", 0);
        offers.setpOffer("buyer-b", 0);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, offers);
        CustomerLoginService customers = new CustomerLoginService(repository);
        java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger successes = new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicInteger soldOut = new java.util.concurrent.atomic.AtomicInteger();
        Runnable purchase = () -> {
            ready.countDown();
            try {
                if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    throw new AssertionError("start latch timed out");
                }
                service.approveForCustomer(user("staff", "secret"), 0, 12, customers);
                successes.incrementAndGet();
            } catch (IllegalStateException expected) {
                soldOut.incrementAndGet();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        };
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> first = pool.submit(purchase);
            java.util.concurrent.Future<?> second = pool.submit(purchase);
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(5, java.util.concurrent.TimeUnit.SECONDS));
            start.countDown();
            first.get(10, java.util.concurrent.TimeUnit.SECONDS);
            second.get(10, java.util.concurrent.TimeUnit.SECONDS);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
        org.junit.jupiter.api.Assertions.assertEquals(1, successes.get());
        org.junit.jupiter.api.Assertions.assertEquals(1, soldOut.get());
        org.junit.jupiter.api.Assertions.assertEquals(0, offers.getListingStock(0));
        org.junit.jupiter.api.Assertions.assertEquals(1,
                customers.getOwnedVehicleRecords("buyer-a").size()
                + customers.getOwnedVehicleRecords("buyer-b").size());
    }

    @Test
    @Tag("security")
    void authorizedEmployeeReceivesInputValidation() {
        UserAccountRepository repository = new InMemoryUserAccountRepository();
        store(repository, "staff", "secret", AccountRole.EMPLOYEE);
        AuthorizedPurchaseReviewService service = new AuthorizedPurchaseReviewService(repository, new Offer());
        assertThrows(IllegalArgumentException.class,
                () -> service.approveForCustomer(user("staff", "secret"), -1, 12, new CustomerLoginService(repository)));
        assertThrows(IllegalArgumentException.class,
                () -> service.approveForCustomer(user("staff", "secret"), 0, 0, new CustomerLoginService(repository)));
    }
}
