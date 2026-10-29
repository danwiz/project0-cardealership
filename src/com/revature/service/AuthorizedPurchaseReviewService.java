package com.revature.service;

import java.util.Objects;

import com.revature.cardealer.Offer;
import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;
import com.revature.cardealer.PaymentPlan;
import com.revature.cardealer.AccountRole;
import com.revature.cardealer.User;

/** Credential-verified boundary for legacy purchase-request decisions. */
public final class AuthorizedPurchaseReviewService {
    private final UserLoginService login;
    private final RoleAuthorizationService authorization = new RoleAuthorizationService();
    private final Offer offers;
    private final UserAccountRepository accounts;

    public AuthorizedPurchaseReviewService(UserAccountRepository accounts, Offer offers) {
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        login = new UserLoginService(this.accounts);
        this.offers = Objects.requireNonNull(offers, "offers");
    }

    /**
     * Prepares ownership before consuming a pending offer. Validation failures
     * leave inventory and the pending request unchanged.
     * This is an in-memory boundary, not yet a durable database transaction.
     */
    public void approveForCustomer(User reviewer, int offerNumber, int months,
            CustomerLoginService customers) {
        requireReviewer(reviewer);
        Objects.requireNonNull(customers, "customers");
        if (months <= 0) {
            throw new IllegalArgumentException("payment term must be positive");
        }
        synchronized (offers) {
            String buyer = offers.getPendingBuyer(offerNumber);
            User account = loginAccount(buyer);
            if (account.getRole() != AccountRole.CUSTOMER) {
                throw new SecurityException("pending buyer must be a registered customer");
            }
            int price = offers.getListingPrice(offerNumber);
            Car vehicle = Objects.requireNonNull(offers.getCarDB(offerNumber), "listed vehicle");
            if (offers.getListingStock(offerNumber) <= 0) {
                throw new IllegalStateException("No inventory available for approval");
            }
            // Prepare a complete ownership record before consuming the pending request.
            OwnedVehicle ownership = new OwnedVehicle(vehicle, new PaymentPlan(price, months));
            offers.setAccept(offerNumber, months, true);
            customers.addPreparedPurchaseForCustomer(buyer, ownership);
        }
    }

    private User loginAccount(String username) {
        return accounts.findByUsername(username)
                .orElseThrow(() -> new SecurityException("pending buyer account no longer exists"));
    }

    /**
     * Identifies the customer whose pending request would be approved.
     * The caller must not assign the vehicle to the employee reviewing it.
     */
    public String pendingBuyer(User credentials, int offerNumber) {
        requireReviewer(credentials);
        return offers.getPendingBuyer(offerNumber);
    }

    public void rejectAll(User credentials) {
        requireReviewer(credentials);
        offers.rejectAllOffers();
    }

    private void requireReviewer(User credentials) {
        User verified = login.authenticate(credentials)
                .orElseThrow(() -> new SecurityException("valid credentials required"));
        authorization.requireAuthorized(verified, Permission.REVIEW_PURCHASE_REQUESTS);
    }
}
