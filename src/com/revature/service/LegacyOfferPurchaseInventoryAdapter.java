package com.revature.service;

import java.util.Objects;
import com.revature.cardealer.Offer;

/**
 * Compatibility adapter for the process-local legacy Offer inventory.
 * Rollback is limited to the state captured by Offer.acceptWithRollback;
 * this adapter is NOT an ACID or durable transaction implementation.
 */
public final class LegacyOfferPurchaseInventoryAdapter implements PurchaseInventoryPort {
    private final Offer offers;

    public LegacyOfferPurchaseInventoryAdapter(Offer offers) {
        this.offers = Objects.requireNonNull(offers, "offers");
    }

    @Override
    public void approvePendingPurchase(int listingNumber, int paymentMonths) {
        offers.acceptWithRollback(listingNumber, paymentMonths);
    }
}
