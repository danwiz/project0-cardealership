package com.revature.application;

import java.util.Objects;
import java.util.function.Function;

import com.revature.cardealer.DealershipApplicationContext;
import com.revature.repository.InventoryRepository;
import com.revature.repository.OwnershipRepository;
import com.revature.repository.PaymentTransactionRepository;
import com.revature.service.UserAccountRepository;

/** Complete configured runtime boundary, including compatibility context and repository ports. */
public final class ConfiguredDealershipApplication {
    private final InfrastructureConfiguration configuration;
    private final DealershipApplicationContext context;
    private final UserAccountRepository accounts;
    private final InventoryRepository inventory;
    private final Function<String, OwnershipRepository> ownershipRepositories;
    private final Function<String, PaymentTransactionRepository> paymentRepositories;

    ConfiguredDealershipApplication(InfrastructureConfiguration configuration,
            DealershipApplicationContext context, UserAccountRepository accounts,
            InventoryRepository inventory,
            Function<String, OwnershipRepository> ownershipRepositories,
            Function<String, PaymentTransactionRepository> paymentRepositories) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.context = Objects.requireNonNull(context, "context");
        this.accounts = Objects.requireNonNull(accounts, "accounts");
        this.inventory = Objects.requireNonNull(inventory, "inventory");
        this.ownershipRepositories = Objects.requireNonNull(ownershipRepositories, "ownershipRepositories");
        this.paymentRepositories = Objects.requireNonNull(paymentRepositories, "paymentRepositories");
    }

    public InfrastructureConfiguration getConfiguration() { return configuration; }
    public DealershipApplicationContext getContext() { return context; }
    public UserAccountRepository getAccounts() { return accounts; }
    public InventoryRepository getInventory() { return inventory; }
    public OwnershipRepository ownershipFor(String customerName) {
        return ownershipRepositories.apply(customerName);
    }
    public PaymentTransactionRepository paymentsFor(String customerName) {
        return paymentRepositories.apply(customerName);
    }
}