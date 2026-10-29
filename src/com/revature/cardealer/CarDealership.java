package com.revature.cardealer;

import java.util.Optional;
import java.util.Scanner;

import com.revature.DAOService.DAOService;
import com.revature.DAOService.DataDAO;
import com.revature.service.AdminLoginService;
import com.revature.service.AuthorizedInventoryService;
import com.revature.service.AuthorizedPurchaseReviewService;
import com.revature.service.CustomerLoginService;
import com.revature.service.EmployeeLoginService;
import com.revature.service.InMemoryUserAccountRepository;
import com.revature.service.Permission;
import com.revature.service.RoleAuthorizationService;
import com.revature.service.UserAccountRepository;
import com.revature.service.UserLoginService;

/**
 * Legacy console entry point with explicit authentication and authorization
 * boundaries. Inventory, offer, payment, and persistence behavior remain legacy
 * concerns for later increments.
 */
public class CarDealership {

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
    private static final AuthorizedInventoryService inventoryService =
            new AuthorizedInventoryService(USER_REPOSITORY, Carlot);
    static Payments PaymentsDB = new Payments();
    private static final AuthorizedPurchaseReviewService purchaseReviewService =
            new AuthorizedPurchaseReviewService(USER_REPOSITORY, Carlot);

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
        currentAccount = null;
        switch (option) {
        case "1":
            registerAccount();
            break;
        case "2":
            authenticateAndAuthorize(cls, Permission.VIEW_INVENTORY)
                    .ifPresent(CarDealership::showCustomerMenu);
            break;
        case "3":
            authenticateAndAuthorize(els, Permission.MANAGE_INVENTORY)
                    .ifPresent(CarDealership::showEmployeeMenu);
            break;
        case "4":
            System.out.println("goodbye");
            break;
        case "5":
            authenticateAndAuthorize(als, Permission.MANAGE_PERSISTENCE)
                    .ifPresent(CarDealership::showAdminMenu);
            break;
        default:
            System.out.println("did not understand input");
            break;
        }
    }

    private static void registerAccount() {
        System.out.println("Welcome: Car Dealership!!\n");
        System.out.println("[1] Register Customer");
        System.out.println("Employee and administrator accounts require verified provisioning.");
        System.out.println("Select an option:  ");
        String option = scan.nextLine();
        if (!"1".equals(option)) {
            System.out.println("access denied: privileged registration requires administrator");
            return;
        }

        User credentials = getUserInfo();
        try {
            customer = cls.registerUser(credentials.getUsername(), credentials.getPassword());
            System.out.println("Customer registered");
        } catch (IllegalArgumentException exception) {
            System.out.println("Registration failed: " + exception.getMessage());
        }
    }

    private static Optional<User> authenticateAndAuthorize(
            UserLoginService loginService, Permission permission) {
        Optional<User> authenticated = loginService.authenticate(getUserInfo());
        if (!authenticated.isPresent()) {
            currentAccount = null;
            System.out.println("failure");
            return Optional.empty();
        }

        User account = authenticated.get();
        if (!isCurrentRoleAuthorized(account, permission)) {
            currentAccount = null;
            System.out.println("access denied");
            return Optional.empty();
        }

        currentAccount = account;
        return authenticated;
    }

    static boolean isCurrentRoleAuthorized(User account, Permission permission) {
        return isCurrentRoleAuthorized(USER_REPOSITORY, account, permission);
    }

    static boolean isCurrentRoleAuthorized(UserAccountRepository repository, User account,
            Permission permission) {
        if (repository == null || account == null || account.getUsername() == null) {
            return false;
        }
        return repository.findByUsername(account.getUsername())
                .filter(stored -> stored.getRole() == account.getRole())
                .filter(stored -> authorization.isAuthorized(stored, permission))
                .isPresent();
    }

    private static void requireCurrentRole(User account, Permission permission) {
        if (!isCurrentRoleAuthorized(account, permission)) {
            throw new SecurityException("account authorization is no longer valid");
        }
    }

    private static void showCustomerMenu(User account) {
        System.out.println("Welcome To The Dealership: \n");
        System.out.println("[1] View Car Lot");
        System.out.println("[4] Request Purchase");
        System.out.println("[2] View Cars Owned");
        System.out.println("[3] View Payments");
        System.out.println("\nSelect an option: ");
        String option = scan.nextLine();

        if ("1".equals(option)) {
            requireCurrentRole(account, Permission.VIEW_INVENTORY);
            Carlot.getOfferAll();
        } else if ("4".equals(option)) {
            requireCurrentRole(account, Permission.REQUEST_PURCHASE);
            System.out.println("\n\n Enter Offer Number to Request Purchase:");
            int offerNumber = Integer.parseInt(scan.nextLine());
            requireCurrentRole(account, Permission.REQUEST_PURCHASE);
            if (offerNumber >= 0 && offerNumber < Carlot.offerDB.length
                    && Carlot.offerDB[offerNumber] != null
                    && !Carlot.offerDB[offerNumber].isEmpty()) {
                Carlot.setpOffer(account.getUsername(), offerNumber);
            }
        } else if ("2".equals(option)) {
            requireCurrentRole(account, Permission.VIEW_OWNED_VEHICLES);
            System.out.println(" Cars Owned:-  \n\n");
            cls.getCarsOwned();
        } else if ("3".equals(option)) {
            requireCurrentRole(account, Permission.VIEW_OWN_PAYMENTS);
            System.out.println(" View Car Payments\n\n");
        } else {
            System.out.println("did not understand input");
        }
    }

    private static void showEmployeeMenu(User account) {
        System.out.println("Employee View: \n");
        System.out.println("[1] View Car Lot");
        System.out.println("[2] View Pending Requests");
        System.out.println("[3] View Customer Payments");
        System.out.println("\n Select an option: ");
        String option = scan.nextLine();

        if ("1".equals(option)) {
            requireCurrentRole(account, Permission.MANAGE_INVENTORY);
            manageInventory(account);
        } else if ("2".equals(option)) {
            requireCurrentRole(account, Permission.REVIEW_PURCHASE_REQUESTS);
            reviewPurchaseRequests(account);
        } else if ("3".equals(option)) {
            requireCurrentRole(account, Permission.VIEW_CUSTOMER_PAYMENTS);
            System.out.println("[3] View Customer Payments");
        } else {
            System.out.println("did not understand input");
        }
    }

    private static void manageInventory(User account) {
        System.out.println("Welcome to The Car Dealership");
        Carlot.getOfferAll();
        System.out.println("\n[1] Add Car to Lot: ");
        System.out.println("\n[2] Remove Car From Lot: ");
        System.out.println("\n Select an option: ");
        String option = scan.nextLine();

        if ("1".equals(option)) {
            System.out.println("\n Enter comma-separated Make, Model, Year, Price, Stock Qty:");
            String[] input = scan.nextLine().split("\\s*,\\s*");
            if (input.length != 5) {
                System.out.println("Invalid vehicle input");
                return;
            }
            inventoryService.addVehicle(getUserInfo(), input[0], input[1],
                    Integer.parseInt(input[2]), Integer.parseInt(input[3]),
                    "Yes", Integer.parseInt(input[4]));
        } else if ("2".equals(option)) {
            System.out.println("\n Enter CarLot Number  --> [ ] <--: ");
            inventoryService.removeVehicle(getUserInfo(), Integer.parseInt(scan.nextLine()));
        }
    }

    private static void reviewPurchaseRequests(User account) {
        System.out.println("Pending Purchase Requests: \n");
        requireCurrentRole(account, Permission.REVIEW_PURCHASE_REQUESTS);
        Carlot.getpOffers();
        System.out.println("\nSelect an Offer to Approve: ");
        int offerNumber = Integer.parseInt(scan.nextLine());
        System.out.println("\nEnter Number of Months Customer has to pay: ");
        int months = Integer.parseInt(scan.nextLine());

        if (months > 0 && offerNumber > 0) {
            int price = purchaseReviewService.approve(getUserInfo(), offerNumber, months);
            cls.setCarsOwned(Carlot.getCarDB(offerNumber), price, months);
        } else {
            System.out.println("\nMessage: Can Not Accept The Payment terms ");
        }

        System.out.println("\nMessage: Reject all other pending offers? [Yes] or [No]: ");
        String input = scan.nextLine();
        if ("yes".equalsIgnoreCase(input) || "y".equalsIgnoreCase(input)) {
            purchaseReviewService.rejectAll(getUserInfo());
        }
    }

    private static void showAdminMenu(User account) {
        requireCurrentRole(account, Permission.MANAGE_PERSISTENCE);
        System.out.println("You are now an Admin");
        System.out.println("\n [1] Serialize and Save Data");
        System.out.println("\n [2] Deserialize and Load Data");
        System.out.println("\n Enter Choice: ");
        String input = scan.nextLine();

        CarDealership application = new CarDealership();
        if ("1".equals(input)) {
            application.saveData();
        } else if ("2".equals(input)) {
            application.loadData();
        }
    }

    public static User getUserInfo() {
        User user = new User();
        System.out.println("Enter username:");
        user.setUsername(scan.nextLine());
        System.out.println("Enter password");
        user.setPassword(scan.nextLine());
        return user;
    }

    static User getCurrentAccount() {
        return currentAccount;
    }

    public void loadData() {
        requireCurrentRole(currentAccount, Permission.MANAGE_PERSISTENCE);
        cDao.readData("CarDealer" + ".dat").getCustomerLoginService();
    }

    public void saveData() {
        requireCurrentRole(currentAccount, Permission.MANAGE_PERSISTENCE);
        Data myData = new Data(cls, els, customer, employee, Carlot, PaymentsDB);
        cDao.createData(myData);
    }
}
