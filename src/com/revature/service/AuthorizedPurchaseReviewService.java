package com.revature.service;

import java.util.Objects;

import com.revature.cardealer.Offer;
import com.revature.cardealer.User;

/** Credential-verified boundary for legacy purchase-request decisions. */
public final class AuthorizedPurchaseReviewService {
    private final UserLoginService login;
    private final RoleAuthorizationService authorization = new RoleAuthorizationService();
    private final Offer offers;

    public AuthorizedPurchaseReviewService(UserAccountRepository accounts, Offer offers) {
        login = new UserLoginService(Objects.requireNonNull(accounts, "accounts"));
        this.offers = Objects.requireNonNull(offers, "offers");
    }

    public int approve(User credentials, int offerNumber, int months) {
        requireReviewer(credentials);
        if (offerNumber < 0 || offerNumber >= 20 || months <= 0) {
            throw new IllegalArgumentException("invalid offer number or payment term");
        }
        int purchasePrice = offers.getListingPrice(offerNumber);
        offers.setAccept(offerNumber, months, true);
        return purchasePrice;
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
