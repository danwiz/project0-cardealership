package com.revature.cardealer;

import java.util.Objects;

import com.revature.DAOService.DAOService;
import com.revature.DAOService.DataDAO;
import com.revature.service.AdminLoginService;
import com.revature.service.CustomerLoginService;
import com.revature.service.EmployeeLoginService;
import com.revature.service.InMemoryUserAccountRepository;
import com.revature.service.RoleAuthorizationService;
import com.revature.service.UserAccountRepository;
import com.revature.service.UserLoginService;

/** Owns the mutable runtime graph independently of any user-interface adapter. */
public final class DealershipApplicationContext {

    private UserAccountRepository accountRepository;
    private UserLoginService userLoginService;
    private CustomerLoginService customerLoginService;
    private EmployeeLoginService employeeLoginService;
    private AdminLoginService adminLoginService;
    private Offer inventory;
    private Payments payments;
    private final RoleAuthorizationService authorizationService;
    private final SnapshotRehydrator snapshotRehydrator;
    private final DataDAO dataStore;
    private User currentAccount;
    private User latestCustomer = new User();
    private User latestEmployee = new User();

    public DealershipApplicationContext() {
        this(new InMemoryUserAccountRepository(), new RoleAuthorizationService(),
                new SnapshotRehydrator(), new DAOService());
    }

    public DealershipApplicationContext(UserAccountRepository repository,
            RoleAuthorizationService authorizationService,
            SnapshotRehydrator snapshotRehydrator, DataDAO dataStore) {
        this.accountRepository = Objects.requireNonNull(repository, "repository");
        this.authorizationService = Objects.requireNonNull(authorizationService, "authorizationService");
        this.snapshotRehydrator = Objects.requireNonNull(snapshotRehydrator, "snapshotRehydrator");
        this.dataStore = Objects.requireNonNull(dataStore, "dataStore");
        this.userLoginService = new UserLoginService(repository);
        this.customerLoginService = new CustomerLoginService(repository);
        this.employeeLoginService = new EmployeeLoginService(repository);
        this.adminLoginService = new AdminLoginService(repository);
        this.inventory = new Offer();
        this.payments = new Payments();
    }

    public void replaceRuntime(RehydratedApplicationState replacement) {
        Objects.requireNonNull(replacement, "replacement");
        accountRepository = replacement.getAccountRepository();
        userLoginService = replacement.getUserLoginService();
        customerLoginService = replacement.getCustomerLoginService();
        employeeLoginService = replacement.getEmployeeLoginService();
        adminLoginService = replacement.getAdminLoginService();
        inventory = replacement.getInventory();
        payments = replacement.getPayments();
        currentAccount = null;
    }

    public Data snapshotSource() {
        return new Data(customerLoginService, employeeLoginService, latestEmployee,
                latestCustomer, inventory, payments);
    }

    public UserAccountRepository getAccountRepository() { return accountRepository; }
    public UserLoginService getUserLoginService() { return userLoginService; }
    public CustomerLoginService getCustomerLoginService() { return customerLoginService; }
    public EmployeeLoginService getEmployeeLoginService() { return employeeLoginService; }
    public AdminLoginService getAdminLoginService() { return adminLoginService; }
    public Offer getInventory() { return inventory; }
    public Payments getPayments() { return payments; }
    public RoleAuthorizationService getAuthorizationService() { return authorizationService; }
    public SnapshotRehydrator getSnapshotRehydrator() { return snapshotRehydrator; }
    public DataDAO getDataStore() { return dataStore; }
    public User getCurrentAccount() { return currentAccount; }
    public void setCurrentAccount(User currentAccount) { this.currentAccount = currentAccount; }
    public void setLatestCustomer(User latestCustomer) { this.latestCustomer = latestCustomer; }
    public void setLatestEmployee(User latestEmployee) { this.latestEmployee = latestEmployee; }
}
