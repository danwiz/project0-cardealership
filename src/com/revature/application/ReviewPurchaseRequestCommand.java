package com.revature.application;

import java.util.Objects;

import com.revature.cardealer.DealershipApplicationContext;
import com.revature.cardealer.PurchaseRequest;
import com.revature.cardealer.User;
import com.revature.service.Permission;

/** Application use case for approving or rejecting purchase requests. */
public final class ReviewPurchaseRequestCommand {

    private final DealershipApplicationContext context;

    public ReviewPurchaseRequestCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public int approve(User actor, int requestId, int paymentMonths, boolean rejectOtherPending) {
        context.getAuthorizationService().requireAuthorized(actor, Permission.REVIEW_PURCHASE_REQUESTS);
        if (paymentMonths <= 0) {
            throw new IllegalArgumentException("payment months must be positive");
        }
        if (requestId < 0 || requestId >= context.getInventory().getPurchaseRequests().size()) {
            throw new IllegalArgumentException("unknown purchase request number: " + requestId);
        }

        PurchaseRequest request = context.getInventory().getPurchaseRequests().get(requestId);
        int price = context.getInventory().setAccept(requestId, paymentMonths, true);
        context.getCustomerLoginService().setCarsOwned(
                context.getInventory().getCarDB(request.getListingId()), price, paymentMonths);

        if (rejectOtherPending) {
            context.getInventory().rejectAllOffers();
        }
        return price;
    }

    public void reject(User actor, int requestId) {
        context.getAuthorizationService().requireAuthorized(actor, Permission.REVIEW_PURCHASE_REQUESTS);
        context.getInventory().setAccept(requestId, 1, false);
    }
}
