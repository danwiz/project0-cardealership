package com.revature.cardealer;

import java.util.Objects;

/**
 * One customer-owned vehicle and its associated payment plan.
 */
public class OwnedVehicle {

    private final Car vehicle;
    private final PaymentPlan paymentPlan;

    public OwnedVehicle(Car vehicle, PaymentPlan paymentPlan) {
        this.vehicle = Objects.requireNonNull(vehicle, "vehicle");
        this.paymentPlan = Objects.requireNonNull(paymentPlan, "paymentPlan");
    }

    public Car getVehicle() {
        return vehicle;
    }

    public PaymentPlan getPaymentPlan() {
        return paymentPlan;
    }
}