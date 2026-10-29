package com.revature.service;

import java.util.Objects;

import com.revature.cardealer.Offer;
import com.revature.cardealer.User;

/**
 * Authenticated application boundary for inventory mutations.
 * Callers supply credentials, never a self-declared role.
 */
public final class AuthorizedInventoryService {
    private final UserLoginService login;
    private final RoleAuthorizationService authorization = new RoleAuthorizationService();
    private final Offer inventory;

    public AuthorizedInventoryService(UserAccountRepository accounts, Offer inventory) {
        this.login = new UserLoginService(Objects.requireNonNull(accounts, "accounts"));
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    public void addVehicle(User credentials, String make, String model, int year,
            int price, String available, int quantity) {
        requireStaff(credentials);
        inventory.registerOffer(make, model, year, price, available, quantity);
    }

    public void removeVehicle(User credentials, int offerNumber) {
        requireStaff(credentials);
        inventory.removeOffer(offerNumber);
    }

    private void requireStaff(User credentials) {
        User verified = login.authenticate(credentials)
                .orElseThrow(() -> new SecurityException("valid credentials required"));
        authorization.requireAuthorized(verified, Permission.MANAGE_INVENTORY);
    }
}
