package com.revature.repository;

import java.util.List;

import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;

public interface OwnershipRepository {
    List<OwnedVehicle> findAll();
    void add(Car car, int purchasePrice, int paymentMonths);
    void replaceAll(List<OwnedVehicle> vehicles);
}
