package com.revature.cardealer;

import java.io.Serializable;
import java.util.Objects;

public class PurchaseRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final int listingId;
    private final String customerName;
    private PurchaseRequestStatus status = PurchaseRequestStatus.PENDING;
    private int paymentMonths;
    private int monthlyPayment;

    public PurchaseRequest(int id, int listingId, String customerName) {
        if (id < 0 || listingId < 0) {
            throw new IllegalArgumentException("request and listing ids must not be negative");
        }
        this.customerName = Objects.requireNonNull(customerName, "customerName").trim();
        if (this.customerName.isEmpty()) {
            throw new IllegalArgumentException("customer name must not be blank");
        }
        this.id = id;
        this.listingId = listingId;
    }

    public int getId() { return id; }
    public int getListingId() { return listingId; }
    public String getCustomerName() { return customerName; }
    public PurchaseRequestStatus getStatus() { return status; }
    public int getPaymentMonths() { return paymentMonths; }
    public int getMonthlyPayment() { return monthlyPayment; }
    public boolean isPending() { return status == PurchaseRequestStatus.PENDING; }

    public void accept(int price, int months) {
        requirePending();
        if (months <= 0) throw new IllegalArgumentException("payment months must be positive");
        status = PurchaseRequestStatus.ACCEPTED;
        paymentMonths = months;
        monthlyPayment = price / months;
    }

    public void reject() {
        requirePending();
        status = PurchaseRequestStatus.REJECTED;
    }

    private void requirePending() {
        if (!isPending()) throw new IllegalStateException("purchase request has already been decided");
    }
}
