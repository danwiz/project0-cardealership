package com.revature.application;

import java.util.Objects;

import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.User;
import com.revature.service.Permission;

/** Application use case for adding and removing inventory listings. */
public final class ManageInventoryCommand {

    private final DealershipApplicationContext context;

    public ManageInventoryCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void add(User actor, String make, String model, int year, int price, int stockQuantity) {
        context.getAuthorizationService().requireAuthorized(actor, Permission.MANAGE_INVENTORY);
        context.getInventory().registerOffer(make, model, year, price, "yes", stockQuantity);
    }

    public void remove(User actor, int listingId) {
        context.getAuthorizationService().requireAuthorized(actor, Permission.MANAGE_INVENTORY);
        context.getInventory().removeOffer(listingId);
    }
}
