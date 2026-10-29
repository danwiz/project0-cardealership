package com.revature.service;

/**
 * Application-facing boundary for consuming a pending purchase request.
 * Implementations must provide a documented atomicity and failure model.
 * This contract does not itself guarantee database transactions or durability.
 */
public interface PurchaseInventoryPort {
    void approvePendingPurchase(int listingNumber, int paymentMonths);
}
