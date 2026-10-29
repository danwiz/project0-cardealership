package com.revature.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;
import com.revature.cardealer.PaymentPlan;
import com.revature.cardealer.User;

public class CustomerLoginService extends UserLoginService {

    private final List<OwnedVehicle> ownedVehicles = new ArrayList<>();
    private final Map<String, List<OwnedVehicle>> vehiclesByCustomer = new HashMap<>();

    public CustomerLoginService() {
        super();
    }

    public CustomerLoginService(UserAccountRepository userRepository) {
        super(userRepository);
    }

    @Override
    public User registerUser(String username, String password) {
        return super.registerUser(username, password);
    }

    public void setCarsOwned(Car owned, int purchasePrice, int termMonths) {
        ownedVehicles.add(new OwnedVehicle(owned, new PaymentPlan(purchasePrice, termMonths)));
    }

    /** Records a vehicle against the verified buyer username, not the reviewer. */
    public synchronized void recordPurchaseForCustomer(String username, Car vehicle, int purchasePrice, int termMonths) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("buyer username must not be blank");
        }
        OwnedVehicle record = new OwnedVehicle(vehicle, new PaymentPlan(purchasePrice, termMonths));
        vehiclesByCustomer.computeIfAbsent(username, ignored -> new ArrayList<>()).add(record);
    }

    /** Accepts a fully validated, preconstructed ownership record. */
    public synchronized void addPreparedPurchaseForCustomer(String username, OwnedVehicle record) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("buyer username must not be blank");
        }
        java.util.Objects.requireNonNull(record, "ownership record");
        vehiclesByCustomer.computeIfAbsent(username, ignored -> new ArrayList<>()).add(record);
    }

    public synchronized List<OwnedVehicle> getOwnedVehicleRecords(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("buyer username must not be blank");
        }
        return Collections.unmodifiableList(new ArrayList<>(
                vehiclesByCustomer.getOrDefault(username, Collections.emptyList())));
    }

    public void getCarsOwned(String username) {
        List<OwnedVehicle> records = getOwnedVehicleRecords(username);
        if (records.isEmpty()) {
            System.out.println("No vehicles owned.");
            return;
        }
        for (OwnedVehicle record : records) {
            PaymentPlan plan = record.getPaymentPlan();
            System.out.println(record.getVehicle().getCar() + "   Price: " + plan.getPurchasePrice()
                    + "   Amount Paid: " + plan.getAmountPaid()
                    + "   Balance: " + plan.getRemainingBalance()
                    + "   Monthly Cost: " + plan.getMonthlyPayment());
        }
    }

    /**
     * Legacy compatibility method: applies a payment to the most recently added vehicle.
     */
    public void setPayments(int payment) {
        if (ownedVehicles.isEmpty()) {
            throw new IllegalStateException("no owned vehicle is available for payment");
        }
        OwnedVehicle latest = ownedVehicles.get(ownedVehicles.size() - 1);
        latest.getPaymentPlan().recordPayment(payment);
    }

    public void setPayments(String[] payments) {
        if (payments == null) {
            throw new IllegalArgumentException("payments must not be null");
        }
        for (String payment : payments) {
            setPayments(Integer.parseInt(payment));
        }
    }

    public void recordPayment(int ownedVehicleIndex, int payment) {
        getOwnedVehicle(ownedVehicleIndex).getPaymentPlan().recordPayment(payment);
    }

    public OwnedVehicle getOwnedVehicle(int index) {
        if (index < 0 || index >= ownedVehicles.size()) {
            throw new IndexOutOfBoundsException("owned vehicle index out of range: " + index);
        }
        return ownedVehicles.get(index);
    }

    public List<OwnedVehicle> getOwnedVehicleRecords() {
        return Collections.unmodifiableList(new ArrayList<>(ownedVehicles));
    }

    public void getCarsOwned() {
        if (ownedVehicles.isEmpty()) {
            System.out.println("No vehicles owned.");
            return;
        }

        for (int i = 0; i < ownedVehicles.size(); i++) {
            OwnedVehicle ownedVehicle = ownedVehicles.get(i);
            PaymentPlan plan = ownedVehicle.getPaymentPlan();
            System.out.println("[" + i + "] " + ownedVehicle.getVehicle().getCar()
                    + "   Price: " + plan.getPurchasePrice()
                    + "   Amount Paid: " + plan.getAmountPaid()
                    + "   Balance: " + plan.getRemainingBalance()
                    + "   Monthly Cost: " + plan.getMonthlyPayment());
        }
    }
}