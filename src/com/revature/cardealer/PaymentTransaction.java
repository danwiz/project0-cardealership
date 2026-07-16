package com.revature.cardealer;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;

/** Immutable ledger entry for a customer payment. */
public final class PaymentTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String transactionId;
    private final String customerName;
    private final Long ownershipId;
    private final int amount;
    private final int totalPaid;
    private final int remainingBalance;
    private final Instant recordedAt;

    public PaymentTransaction(String transactionId, String customerName, int amount,
            int totalPaid, int remainingBalance, Instant recordedAt) {
        this(transactionId, customerName, null, amount, totalPaid, remainingBalance, recordedAt);
    }

    public PaymentTransaction(String transactionId, String customerName, Long ownershipId, int amount,
            int totalPaid, int remainingBalance, Instant recordedAt) {
        this.transactionId = requireText(transactionId, "transactionId");
        this.customerName = requireText(customerName, "customerName");
        if (ownershipId != null && ownershipId <= 0) {
            throw new IllegalArgumentException("ownershipId must be positive when present");
        }
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        if (totalPaid < amount) throw new IllegalArgumentException("totalPaid must include the transaction amount");
        if (remainingBalance < 0) throw new IllegalArgumentException("remainingBalance must not be negative");
        this.ownershipId = ownershipId;
        this.amount = amount;
        this.totalPaid = totalPaid;
        this.remainingBalance = remainingBalance;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    public String getTransactionId() { return transactionId; }
    public String getCustomerName() { return customerName; }
    public OptionalLong getOwnershipId() {
        return ownershipId == null ? OptionalLong.empty() : OptionalLong.of(ownershipId);
    }
    public int getAmount() { return amount; }
    public int getTotalPaid() { return totalPaid; }
    public int getRemainingBalance() { return remainingBalance; }
    public Instant getRecordedAt() { return recordedAt; }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
