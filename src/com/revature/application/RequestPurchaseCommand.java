package com.revature.application;

import java.util.Objects;

import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.User;
import com.revature.service.Permission;

/** Application use case for submitting a customer purchase request. */
public final class RequestPurchaseCommand {

    private final DealershipApplicationContext context;

    public RequestPurchaseCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void execute(User actor, int listingId) {
        context.getAuthorizationService().requireAuthorized(actor, Permission.REQUEST_PURCHASE);
        if (listingId < 0 || listingId >= context.getInventory().getListingCount()) {
            throw new IllegalArgumentException("unknown listing number: " + listingId);
        }
        context.getInventory().setpOffer(actor.getUsername(), listingId);
    }
}
