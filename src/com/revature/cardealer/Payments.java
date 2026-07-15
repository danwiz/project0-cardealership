package com.revature.cardealer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compatibility facade over an explicit payment ledger.
 */
public class Payments {

    private int aOwed;
    private int aPaid;
    private int aDue;
    private int pBalance;
    private int pLength;
    private int tPaid;
    private String pPayment;
    private long nextTransactionSequence = 1;
    private final List<PaymentTransaction> transactions = new ArrayList<>();

    public void setAmtOwed(int owed) {
        if (owed < 0) {
            throw new IllegalArgumentException("amount owed must not be negative");
        }
        if (owed < tPaid) {
            throw new IllegalArgumentException("amount owed must not be less than amount already paid");
        }
        aOwed = owed;
        pBalance = aOwed - tPaid;
        updateSummary();
    }

    public void setAmtPaid(int paid) {
        if (paid < 0) {
            throw new IllegalArgumentException("amount paid must not be negative");
        }
        aPaid = paid;
    }

    public void setAmtDue(int due) {
        if (due < 0) {
            throw new IllegalArgumentException("amount due must not be negative");
        }
        aDue = due;
        updateSummary();
    }

    public void balance(int bal) {
        if (bal < 0) {
            throw new IllegalArgumentException("balance must not be negative");
        }
        pBalance = bal;
        updateSummary();
    }

    public void Pmtlength(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("payment length must not be negative");
        }
        pLength = len;
    }

    public int getAmtOwed() {
        return aOwed;
    }

    public int getAmtPaid() {
        return aPaid;
    }

    public int getAmtDue() {
        return aDue;
    }

    public int getBalance() {
        return pBalance;
    }

    public int getPmtLength() {
        return pLength;
    }

    public int getTotalPaid() {
        return tPaid;
    }

    public List<PaymentTransaction> getTransactions() {
        return Collections.unmodifiableList(new ArrayList<>(transactions));
    }

    public void getPaymentsAll() {
        if (transactions.isEmpty()) {
            System.out.println("No payments have been recorded.");
            return;
        }

        for (PaymentTransaction transaction : transactions) {
            System.out.println("[" + transaction.getTransactionId() + "] Cost: " + aOwed
                    + " Amount Paid: " + transaction.getAmount()
                    + " Total Paid: " + transaction.getTotalPaid()
                    + " Balance: " + transaction.getRemainingBalance()
                    + " Monthly Due: " + aDue
                    + " Customer: " + transaction.getCustomerName()
                    + " Recorded At: " + transaction.getRecordedAt());
        }
    }

    public void makePayment(String cname, int pamt) {
        if (cname == null || cname.trim().isEmpty()) {
            throw new IllegalArgumentException("customer name must not be blank");
        }
        if (pamt <= 0) {
            throw new IllegalArgumentException("payment amount must be positive");
        }
        if (pamt > pBalance) {
            throw new IllegalArgumentException("payment amount exceeds remaining balance");
        }

        aPaid = pamt;
        tPaid += pamt;
        pBalance = aOwed - tPaid;

        PaymentTransaction transaction = new PaymentTransaction(
                String.format("PAY-%06d", nextTransactionSequence++),
                cname,
                pamt,
                tPaid,
                pBalance,
                Instant.now());
        transactions.add(transaction);
        updateSummary();
    }

    public int MPay(int len) {
        if (len < 0) {
            throw new IllegalArgumentException("payment length must not be negative");
        }
        pLength = len;
        if (len == 0) {
            aDue = 0;
            updateSummary();
            return 0;
        }
        aDue = aOwed / len;
        updateSummary();
        return aDue;
    }

    public String getpPayment() {
        return pPayment;
    }

    public void setpPayment(String cname, int cprice, int len) {
        if (cname == null || cname.trim().isEmpty()) {
            throw new IllegalArgumentException("customer name must not be blank");
        }
        setAmtOwed(cprice);
        MPay(len);
    }

    private void updateSummary() {
        pPayment = "Cost: " + aOwed
                + " Amounts Paid: " + tPaid
                + " Balance: " + pBalance
                + " Monthly Due: " + aDue;
    }
}
