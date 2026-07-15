package com.revature.cardealer;

import java.util.Optional;
import java.util.Scanner;

import com.revature.DAOService.DAOService;
import com.revature.DAOService.DataDAO;
import com.revature.DAOService.LoadResult;
import com.revature.DAOService.SaveResult;
import com.revature.service.AdminLoginService;
import com.revature.service.CustomerLoginService;
import com.revature.service.EmployeeLoginService;
import com.revature.service.InMemoryUserAccountRepository;
import com.revature.service.Permission;
import com.revature.service.RoleAuthorizationService;
import com.revature.service.UserAccountRepository;
import com.revature.service.UserLoginService;

public class CarDealership {

    private static final String STATE_FILE = "CarDealer.dat";
    private static final UserAccountRepository USER_REPOSITORY = new InMemoryUserAccountRepository();
    private static final UserLoginService uls = new UserLoginService(USER_REPOSITORY);
    private static final CustomerLoginService cls = new CustomerLoginService(USER_REPOSITORY);
    private static final EmployeeLoginService els = new EmployeeLoginService(USER_REPOSITORY);
    private static final AdminLoginService als = new AdminLoginService(USER_REPOSITORY);
    private static final RoleAuthorizationService authorization = new RoleAuthorizationService();

    private static User customer = new User();
    private static User employee = new User();
    private static User currentAccount;

    static Offer Carlot = new Offer();
    static Payments PaymentsDB = new Payments();
    DataDAO cDao = new DAOService();
    private static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) {
        String option = "";
        do {
            System.out.println("Welcome: Car Dealership!!\n");
            System.out.println("[1] Register User");
            System.out.println("[2] Customer Login");
            System.out.println("[3] Employee Login");
            System.out.println("[4] Exit");
            System.out.println("[5] Admin features");
            option = scan.nextLine();
            performUserAction(option);
        } while (!"4".equals(option));
    }

    private static void performUserAction(String option) {
        switch (option) {
        case "1": registerAccount(); break;
        case "2": authenticateAndAuthorize(cls, Permission.VIEW_INVENTORY)
                .ifPresent(CarDealership::showCustomerMenu); break;
        case "3": authenticateAndAuthorize(els, Permission.MANAGE_INVENTORY)
                .ifPresent(CarDealership::showEmployeeMenu); break;
        case "4": System.out.println("goodbye"); break;
        case "5": authenticateAndAuthorize(als, Permission.MANAGE_PERSISTENCE)
                .ifPresent(CarDealership::showAdminMenu); break;
        default: System.out.println("did not understand input"); break;
        }
    }

    private static void registerAccount() {
        System.out.println("Welcome: Car Dealership!!\n");
        System.out.println("[1] Register Customer");
        System.out.println("[2] Register Employee");
        System.out.println("[3] Register Administrator");
        System.out.println("Select an option:  ");
        String option = scan.nextLine();
        User credentials = getUserInfo();
        try {
            if ("1".equals(option)) {
                customer = cls.registerUser(credentials.getUsername(), credentials.getPassword());
                System.out.println("Customer registered");
            } else if ("2".equals(option)) {
                employee = els.registerUser(credentials.getUsername(), credentials.getPassword());
                System.out.println("Employee registered");
            } else if ("3".equals(option)) {
                als.registerUser(credentials.getUsername(), credentials.getPassword());
                System.out.println("Administrator registered");
            } else {
                System.out.println("did not understand input");
            }
        } catch (IllegalArgumentException exception) {
            System.out.println("Registration failed: " + exception.getMessage());
        }
    }

    private static Optional<User> authenticateAndAuthorize(UserLoginService loginService, Permission permission) {
        Optional<User> authenticated = loginService.authenticate(getUserInfo());
        if (!authenticated.isPresent()) {
            currentAccount = null;
            System.out.println("failure");
            return Optional.empty();
        }
        User account = authenticated.get();
        if (!authorization.isAuthorized(account, permission)) {
            currentAccount = null;
            System.out.println("access denied");
            return Optional.empty();
        }
        currentAccount = account;
        return authenticated;
    }

    private static void showCustomerMenu(User account) {
        System.out.println("Welcome To The Dealership: \n");
        System.out.println("[1] View Car Lot");
        System.out.println("[2] View Cars Owned");
        System.out.println("[3] View Payments");
        System.out.println("\nSelect an option: ");
        String option = scan.nextLine();
        if ("1".equals(option)) {
            authorization.requireAuthorized(account, Permission.VIEW_INVENTORY);
            seedInventoryIfEmpty();
            Carlot.getOfferAll();
            System.out.println("\n\n Enter Listing Number to Request Purchase:");
            int listingNumber = Integer.parseInt(scan.nextLine());
            authorization.requireAuthorized(account, Permission.REQUEST_PURCHASE);
            if (listingNumber >= 0 && listingNumber < Carlot.getListingCount()) {
                Carlot.setpOffer(account.getUsername(), listingNumber);
            } else {
                System.out.println("Invalid listing number");
            }
        } else if ("2".equals(option)) {
            authorization.requireAuthorized(account, Permission.VIEW_OWNED_VEHICLES);
            cls.getCarsOwned();
        } else if ("3".equals(option)) {
            authorization.requireAuthorized(account, Permission.VIEW_OWN_PAYMENTS);
            PaymentsDB.getPaymentsAll();
        } else {
            System.out.println("did not understand input");
        }
    }

    private static void seedInventoryIfEmpty() {
        if (Carlot.getListingCount() > 0) return;
        Carlot.registerOffer("Honda ", "Accord ", 2017, 15000, "yes", 7);
        Carlot.registerOffer("Chevy ", "Malibu ", 2020, 17456, "yes", 4);
        Carlot.registerOffer("BMW   ", "4Series", 2016, 10456, "yes", 6);
        Carlot.registerOffer("Toyota", "Corolla", 2014, 13456, "yes", 3);
    }

    private static void showEmployeeMenu(User account) {
        System.out.println("Employee View: \n");
        System.out.println("[1] View Car Lot");
        System.out.println("[2] View Pending Requests");
        System.out.println("[3] View Customer Payments");
        System.out.println("\n Select an option: ");
        String option = scan.nextLine();
        if ("1".equals(option)) {
            authorization.requireAuthorized(account, Permission.MANAGE_INVENTORY);
            manageInventory();
        } else if ("2".equals(option)) {
            authorization.requireAuthorized(account, Permission.REVIEW_PURCHASE_REQUESTS);
            reviewPurchaseRequests();
        } else if ("3".equals(option)) {
            authorization.requireAuthorized(account, Permission.VIEW_CUSTOMER_PAYMENTS);
            PaymentsDB.getPaymentsAll();
        } else {
            System.out.println("did not understand input");
        }
    }

    private static void manageInventory() {
        Carlot.getOfferAll();
        System.out.println("\n[1] Add Car to Lot: ");
        System.out.println("\n[2] Remove Car From Lot: ");
        String option = scan.nextLine();
        if ("1".equals(option)) {
            String[] input = scan.nextLine().split("\\s*,\\s*");
            if (input.length != 5) {
                System.out.println("Invalid vehicle input");
                return;
            }
            Carlot.registerOffer(input[0], input[1], Integer.parseInt(input[2]),
                    Integer.parseInt(input[3]), "yes", Integer.parseInt(input[4]));
        } else if ("2".equals(option)) {
            Carlot.removeOffer(Integer.parseInt(scan.nextLine()));
        }
    }

    private static void reviewPurchaseRequests() {
        Carlot.getpOffers();
        int requestNumber = Integer.parseInt(scan.nextLine());
        int months = Integer.parseInt(scan.nextLine());
        if (months > 0 && requestNumber >= 0 && requestNumber < Carlot.getPurchaseRequests().size()) {
            PurchaseRequest request = Carlot.getPurchaseRequests().get(requestNumber);
            int price = Carlot.setAccept(requestNumber, months, true);
            cls.setCarsOwned(Carlot.getCarDB(request.getListingId()), price, months);
        } else {
            System.out.println("\nMessage: Can Not Accept The Payment terms ");
        }
        String input = scan.nextLine();
        if ("yes".equalsIgnoreCase(input) || "y".equalsIgnoreCase(input)) Carlot.rejectAllOffers();
    }

    private static void showAdminMenu(User account) {
        authorization.requireAuthorized(account, Permission.MANAGE_PERSISTENCE);
        System.out.println("You are now an Admin");
        System.out.println("\n [1] Serialize and Save Data");
        System.out.println("\n [2] Deserialize and Load Data");
        String input = scan.nextLine();
        CarDealership application = new CarDealership();
        if ("1".equals(input)) application.saveData();
        else if ("2".equals(input)) application.loadData();
    }

    public static User getUserInfo() {
        User user = new User();
        System.out.println("Enter username:");
        user.setUsername(scan.nextLine());
        System.out.println("Enter password");
        user.setPassword(scan.nextLine());
        return user;
    }

    static User getCurrentAccount() { return currentAccount; }

    public LoadResult loadData() {
        LoadResult result = cDao.loadData(STATE_FILE);
        if (result.isSuccess()) {
            ApplicationStateSnapshot snapshot = result.getSnapshot().get();
            System.out.println("Loaded application state version " + snapshot.getVersion()
                    + " with " + snapshot.getAccounts().size() + " accounts.");
        } else {
            System.out.println("Load failed [" + result.getStatus() + "]: " + result.getMessage());
        }
        return result;
    }

    public SaveResult saveData() {
        Data state = new Data(cls, els, employee, customer, Carlot, PaymentsDB);
        SaveResult result = cDao.saveData(state, STATE_FILE);
        System.out.println(result.isSuccess() ? "Application state saved." :
                "Save failed [" + result.getStatus() + "]: " + result.getMessage());
        return result;
    }
}
