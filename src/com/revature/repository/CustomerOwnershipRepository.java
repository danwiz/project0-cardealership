package com.revature.repository;

import java.util.List;
import java.util.Objects;

import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;
import com.revature.service.CustomerLoginService;

public final class CustomerOwnershipRepository implements OwnershipRepository {
    private final CustomerLoginService customers;

    public CustomerOwnershipRepository(CustomerLoginService customers) {
        this.customers = Objects.requireNonNull(customers, "customers");
    }

    @Override public List<OwnedVehicle> findAll() { return customers.getOwnedVehicleRecords(); }
    @Override public void add(Car car, int purchasePrice, int paymentMonths) {
        customers.setCarsOwned(car, purchasePrice, paymentMonths);
    }
    @Override public void replaceAll(List<OwnedVehicle> vehicles) { customers.restoreOwnedVehicles(vehicles); }
}
