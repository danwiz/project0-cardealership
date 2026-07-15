package com.revature.application;

import java.util.List;
import java.util.Objects;

import com.revature.cardealer.Car;
import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.PurchaseRequest;
import com.revature.cardealer.User;
import com.revature.service.Permission;

/** Binds authorization, inventory, and ownership ports to the in-memory context. */
public final class ContextDomainAdapter implements ApplicationPorts.Authorization,
        ApplicationPorts.Inventory, ApplicationPorts.Ownership {
    private final DealershipApplicationContext context;

    public ContextDomainAdapter(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @Override public boolean isAuthorized(User account, Permission permission) {
        return context.getAuthorizationService().isAuthorized(account, permission);
    }
    @Override public void requireAuthorized(User account, Permission permission) {
        context.getAuthorizationService().requireAuthorized(account, permission);
    }
    @Override public int listingCount() { return context.getInventory().getListingCount(); }
    @Override public void addListing(String make, String model, int year, int price, int stockQuantity) {
        context.getInventory().registerOffer(make, model, year, price, "yes", stockQuantity);
    }
    @Override public void removeListing(int listingId) { context.getInventory().removeOffer(listingId); }
    @Override public void requestPurchase(String customerName, int listingId) {
        context.getInventory().setpOffer(customerName, listingId);
    }
    @Override public List<PurchaseRequest> purchaseRequests() {
        return context.getInventory().getPurchaseRequests();
    }
    @Override public int decideRequest(int requestId, int paymentMonths, boolean accepted) {
        return context.getInventory().setAccept(requestId, paymentMonths, accepted);
    }
    @Override public void rejectAllPendingRequests() { context.getInventory().rejectAllOffers(); }
    @Override public Car carForListing(int listingId) { return context.getInventory().getCarDB(listingId); }
    @Override public void addOwnedVehicle(Car car, int purchasePrice, int paymentMonths) {
        context.getCustomerLoginService().setCarsOwned(car, purchasePrice, paymentMonths);
    }
}