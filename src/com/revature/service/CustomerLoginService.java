package com.revature.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.revature.cardealer.AccountRole;
import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;
import com.revature.cardealer.PaymentPlan;
import com.revature.cardealer.User;

public class CustomerLoginService extends UserLoginService {

    private final List<OwnedVehicle> ownedVehicles = new ArrayList<>();

    public CustomerLoginService() { super(); }
    public CustomerLoginService(UserAccountRepository userRepository) { super(userRepository); }

    public void restoreOwnedVehicles(List<OwnedVehicle> restoredVehicles) {
        ownedVehicles.clear();
        ownedVehicles.addAll(Objects.requireNonNull(restoredVehicles, "restoredVehicles"));
    }

    @Override
    public User registerUser(String username, String password) {
        return super.registerUser(username, password, AccountRole.CUSTOMER);
    }

    public void setCarsOwned(Car owned, int purchasePrice, int termMonths) {
        ownedVehicles.add(new OwnedVehicle(owned, new PaymentPlan(purchasePrice, termMonths)));
    }

    public void setPayments(int payment) {
        if (ownedVehicles.isEmpty()) throw new IllegalStateException("no owned vehicle is available for payment");
        ownedVehicles.get(ownedVehicles.size() - 1).getPaymentPlan().recordPayment(payment);
    }

    public void setPayments(String[] payments) {
        if (payments == null) throw new IllegalArgumentException("payments must not be null");
        for (String payment : payments) setPayments(Integer.parseInt(payment));
    }

    public void recordPayment(int ownedVehicleIndex, int payment) {
        getOwnedVehicle(ownedVehicleIndex).getPaymentPlan().recordPayment(payment);
    }

    public OwnedVehicle getOwnedVehicle(int index) {
        if (index < 0 || index >= ownedVehicles.size()) throw new IndexOutOfBoundsException("owned vehicle index out of range: " + index);
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
