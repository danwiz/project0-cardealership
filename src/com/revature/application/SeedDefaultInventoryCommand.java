package com.revature.application;

import java.util.Objects;

import com.revature.cardealer.DealershipApplicationContext;

/** Idempotently seeds the default dealership inventory. */
public final class SeedDefaultInventoryCommand {

    private final DealershipApplicationContext context;

    public SeedDefaultInventoryCommand(DealershipApplicationContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public boolean execute() {
        if (context.getInventory().getListingCount() > 0) {
            return false;
        }
        context.getInventory().registerOffer("Honda ", "Accord ", 2017, 15000, "yes", 7);
        context.getInventory().registerOffer("Chevy ", "Malibu ", 2020, 17456, "yes", 4);
        context.getInventory().registerOffer("BMW   ", "4Series", 2016, 10456, "yes", 6);
        context.getInventory().registerOffer("Toyota", "Corolla", 2014, 13456, "yes", 3);
        return true;
    }
}
