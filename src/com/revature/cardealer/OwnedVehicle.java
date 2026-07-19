package com.revature.cardealer;

import java.io.Serializable;
import java.util.Objects;

/** One customer-owned vehicle and its associated payment plan. */
public class OwnedVehicle implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long ownershipId;
    private final Car vehicle;
    private final PaymentPlan paymentPlan;

    /** Compatibility constructor for historical snapshots without an explicit identifier. */
    public OwnedVehicle(Car vehicle, PaymentPlan paymentPlan) {
        this(0, vehicle, paymentPlan);
    }

    public OwnedVehicle(long ownershipId, Car vehicle, PaymentPlan paymentPlan) {
        if (ownershipId < 0) throw new IllegalArgumentException("ownershipId must not be negative");
        this.ownershipId = ownershipId;
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.paymentPlan = Objects.requireNonNull(paymentPlan, "paymentPlan");
    }

    public long getOwnershipId() { return ownershipId; }
    public boolean hasOwnershipId() { return ownershipId > 0; }
    public Car getVehicle() { return vehicle; }
    public PaymentPlan getPaymentPlan() { return paymentPlan; }
}
