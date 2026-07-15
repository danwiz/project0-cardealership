package com.revature.cardealer;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable ledger entry for a customer payment.
 */
public final class PaymentTransaction {

    private final String transactionId;
    private final String customerName;
    private final int amount;
    private final int totalPaid;
    private final int remainingBalance;
    private final Instant recordedAt;

    public PaymentTransaction(String transactionId, String customerName, int amount,
            int totalPaid, int remainingBalance, Instant recordedAt) {
        this.transactionId = requireText(transactionId, "transactionId");
        this.customerName = requireText(customerName, "customerName");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (totalPaid < amount) {
            throw new IllegalArgumentException("totalPaid must include the transaction amount");
        }
        if (remainingBalance < 0) {
            throw new IllegalArgumentException("remainingBalance must not be negative");
        }
        this.amount = amount;
        this.totalPaid = totalPaid;
        this.remainingBalance = remainingBalance;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getAmount() {
        return amount;
    }

    public int getTotalPaid() {
        return totalPaid;
    }

    public int getRemainingBalance() {
        return remainingBalance;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
