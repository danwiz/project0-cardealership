package com.revature.cardealer;

import java.util.Optional;

import com.revature.DAOService.LoadResult;
import com.revature.DAOService.SaveResult;
import com.revature.service.Permission;
import com.revature.service.UserLoginService;

/** Coordinates console commands against an explicit application context. */
public final class ConsoleCommandHandler {
    private static final String STATE_FILE = "CarDealer.dat";

    private final DealershipApplicationContext context;
    private final ConsoleIO io;

    public ConsoleCommandHandler(DealershipApplicationContext context, ConsoleIO io) {
        this.context = context;
        this.io = io;
    }

    public void run() {
        String option;
        do {
            io.writeLine("Welcome: Car Dealership!!\n");
            io.writeLine("[1] Register User");
            io.writeLine("[2] Customer Login");
            io.writeLine("[3] Employee Login");
            io.writeLine("[4] Exit");
            io.writeLine("[5] Admin features");
            option = io.readLine();
            handleMainCommand(option);
        } while (!"4".equals(option));
    }

    public void handleMainCommand(String option) {
        switch (option) {
        case "1": registerAccount(); break;
        case "2": authenticateAndAuthorize(context.getCustomerLoginService(), Permission.VIEW_INVENTORY)
                .ifPresent(this::showCustomerMenu); break;
        case "3": authenticateAndAuthorize(context.getEmployeeLoginService(), Permission.MANAGE_INVENTORY)
                .ifPresent(this::showEmployeeMenu); break;
        case "4": io.writeLine("goodbye"); break;
        case "5": authenticateAndAuthorize(context.getAdminLoginService(), Permission.MANAGE_PERSISTENCE)
                .ifPresent(this::showAdminMenu); break;
        default: io.writeLine("did not understand input"); break;
        }
    }

    private void registerAccount() {
        io.writeLine("Welcome: Car Dealership!!\n");
        io.writeLine("[1] Register Customer");
        io.writeLine("[2] Register Employee");
        io.writeLine("[3] Register Administrator");
        String option = io.readLine();
        User credentials = readCredentials();
        try {
            if ("1".equals(option)) {
                User account = context.getCustomerLoginService().registerUser(
                        credentials.getUsername(), credentials.getPassword());
                context.setLatestCustomer(account);
                io.writeLine("Customer registered");
            } else if ("2".equals(option)) {
                User account = context.getEmployeeLoginService().registerUser(
                        credentials.getUsername(), credentials.getPassword());
                context.setLatestEmployee(account);
                io.writeLine("Employee registered");
            } else if ("3".equals(option)) {
                context.getAdminLoginService().registerUser(
                        credentials.getUsername(), credentials.getPassword());
                io.writeLine("Administrator registered");
            } else {
                io.writeLine("did not understand input");
            }
        } catch (IllegalArgumentException exception) {
            io.writeLine("Registration failed: " + exception.getMessage());
        }
    }

    private Optional<User> authenticateAndAuthorize(UserLoginService service, Permission permission) {
        Optional<User> authenticated = service.authenticate(readCredentials());
        if (!authenticated.isPresent()) {
            context.setCurrentAccount(null);
            io.writeLine("failure");
            return Optional.empty();
        }
        User account = authenticated.get();
        if (!context.getAuthorizationService().isAuthorized(account, permission)) {
            context.setCurrentAccount(null);
            io.writeLine("access denied");
            return Optional.empty();
        }
        context.setCurrentAccount(account);
        return authenticated;
    }

    private void showCustomerMenu(User account) {
        io.writeLine("Welcome To The Dealership: \n[1] View Car Lot\n[2] View Cars Owned\n[3] View Payments");
        String option = io.readLine();
        if ("1".equals(option)) {
            context.getAuthorizationService().requireAuthorized(account, Permission.VIEW_INVENTORY);
            seedInventoryIfEmpty();
            context.getInventory().getOfferAll();
            int listingNumber = Integer.parseInt(io.readLine());
            context.getAuthorizationService().requireAuthorized(account, Permission.REQUEST_PURCHASE);
            if (listingNumber >= 0 && listingNumber < context.getInventory().getListingCount()) {
                context.getInventory().setpOffer(account.getUsername(), listingNumber);
            } else {
                io.writeLine("Invalid listing number");
            }
        } else if ("2".equals(option)) {
            context.getAuthorizationService().requireAuthorized(account, Permission.VIEW_OWNED_VEHICLES);
            context.getCustomerLoginService().getCarsOwned();
        } else if ("3".equals(option)) {
            context.getAuthorizationService().requireAuthorized(account, Permission.VIEW_OWN_PAYMENTS);
            context.getPayments().getPaymentsAll();
        } else {
            io.writeLine("did not understand input");
        }
    }

    private void showEmployeeMenu(User account) {
        io.writeLine("Employee View: \n[1] View Car Lot\n[2] View Pending Requests\n[3] View Customer Payments");
        String option = io.readLine();
        if ("1".equals(option)) {
            context.getAuthorizationService().requireAuthorized(account, Permission.MANAGE_INVENTORY);
            manageInventory();
        } else if ("2".equals(option)) {
            context.getAuthorizationService().requireAuthorized(account, Permission.REVIEW_PURCHASE_REQUESTS);
            reviewPurchaseRequests();
        } else if ("3".equals(option)) {
            context.getAuthorizationService().requireAuthorized(account, Permission.VIEW_CUSTOMER_PAYMENTS);
            context.getPayments().getPaymentsAll();
        } else {
            io.writeLine("did not understand input");
        }
    }

    private void manageInventory() {
        context.getInventory().getOfferAll();
        String option = io.readLine();
        if ("1".equals(option)) {
            String[] input = io.readLine().split("\\s*,\\s*");
            if (input.length != 5) {
                io.writeLine("Invalid vehicle input");
                return;
            }
            context.getInventory().registerOffer(input[0], input[1], Integer.parseInt(input[2]),
                    Integer.parseInt(input[3]), "yes", Integer.parseInt(input[4]));
        } else if ("2".equals(option)) {
            context.getInventory().removeOffer(Integer.parseInt(io.readLine()));
        }
    }

    private void reviewPurchaseRequests() {
        context.getInventory().getpOffers();
        int requestNumber = Integer.parseInt(io.readLine());
        int months = Integer.parseInt(io.readLine());
        if (months > 0 && requestNumber >= 0
                && requestNumber < context.getInventory().getPurchaseRequests().size()) {
            PurchaseRequest request = context.getInventory().getPurchaseRequests().get(requestNumber);
            int price = context.getInventory().setAccept(requestNumber, months, true);
            context.getCustomerLoginService().setCarsOwned(
                    context.getInventory().getCarDB(request.getListingId()), price, months);
        } else {
            io.writeLine("\nMessage: Can Not Accept The Payment terms ");
        }
        String input = io.readLine();
        if ("yes".equalsIgnoreCase(input) || "y".equalsIgnoreCase(input)) {
            context.getInventory().rejectAllOffers();
        }
    }

    private void showAdminMenu(User account) {
        context.getAuthorizationService().requireAuthorized(account, Permission.MANAGE_PERSISTENCE);
        io.writeLine("You are now an Admin\n [1] Serialize and Save Data\n [2] Deserialize and Load Data");
        String input = io.readLine();
        if ("1".equals(input)) saveData();
        else if ("2".equals(input)) loadData();
    }

    public LoadResult loadData() {
        LoadResult result = context.getDataStore().loadData(STATE_FILE);
        if (!result.isSuccess()) {
            io.writeLine("Load failed [" + result.getStatus() + "]: " + result.getMessage());
            return result;
        }
        try {
            RehydratedApplicationState replacement = context.getSnapshotRehydrator()
                    .rehydrate(result.getSnapshot().get());
            context.replaceRuntime(replacement);
            io.writeLine("Application state loaded and activated.");
            return result;
        } catch (IllegalArgumentException exception) {
            io.writeLine("Load rejected [INVALID_CONTENT]: " + exception.getMessage());
            return LoadResult.failure(LoadResult.Status.INVALID_CONTENT, exception.getMessage());
        }
    }

    public SaveResult saveData() {
        SaveResult result = context.getDataStore().saveData(context.snapshotSource(), STATE_FILE);
        io.writeLine(result.isSuccess() ? "Application state saved."
                : "Save failed [" + result.getStatus() + "]: " + result.getMessage());
        return result;
    }

    public User readCredentials() {
        User user = new User();
        io.writeLine("Enter username:");
        user.setUsername(io.readLine());
        io.writeLine("Enter password");
        user.setPassword(io.readLine());
        return user;
    }

    private void seedInventoryIfEmpty() {
        if (context.getInventory().getListingCount() > 0) return;
        context.getInventory().registerOffer("Honda ", "Accord ", 2017, 15000, "yes", 7);
        context.getInventory().registerOffer("Chevy ", "Malibu ", 2020, 17456, "yes", 4);
        context.getInventory().registerOffer("BMW   ", "4Series", 2016, 10456, "yes", 6);
        context.getInventory().registerOffer("Toyota", "Corolla", 2014, 13456, "yes", 3);
    }
}
