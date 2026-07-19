package com.revature.repository;

import java.util.List;
import java.util.Optional;

import com.revature.cardealer.Car;
import com.revature.cardealer.OwnedVehicle;

public interface OwnershipRepository {
    List<OwnedVehicle> findAll();
    Optional<OwnedVehicle> findById(long ownershipId);
    long add(Car car, int purchasePrice, int paymentMonths);
    void replaceAll(List<OwnedVehicle> vehicles);
}
