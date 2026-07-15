package com.revature.cardealer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("LEGACY-BEHAVIOR")
class PaymentsCharacterizationTest {

    @Test
    @Tag("KNOWN-DEFECT")
    @DisplayName("setAmtDue writes the paid amount instead of the due amount")
    void setAmtDueChangesPaidButLeavesDueAtDefault() {
        Payments payments = new Payments();

        payments.setAmtDue(425);

        assertEquals(425, payments.getAmtPaid());
        assertEquals(0, payments.getAmtDue());
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @DisplayName("MPay returns the quotient before constructing payment summary text")
    void monthlyPaymentCalculationLeavesSummaryNull() {
        Payments payments = new Payments();
        payments.setAmtOwed(12_000);

        int monthlyAmount = payments.MPay(12);

        assertEquals(1_000, monthlyAmount);
        assertEquals(1_000, payments.getAmtDue());
        assertNull(payments.getpPayment());
    }

    @Test
    @DisplayName("makePayment accumulates amounts and recalculates balance")
    void makePaymentAccumulatesPaidAmount() {
        Payments payments = new Payments();
        payments.setAmtOwed(1_000);

        payments.makePayment("customer", 250);
        assertEquals(750, payments.getBalance());

        payments.makePayment("customer", 300);
        assertEquals(450, payments.getBalance());
        assertEquals(300, payments.getAmtPaid());
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @DisplayName("overpayment is accepted and produces a negative balance")
    void makePaymentAllowsNegativeBalance() {
        Payments payments = new Payments();
        payments.setAmtOwed(500);

        payments.makePayment("customer", 700);

        assertEquals(-200, payments.getBalance());
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @DisplayName("the twenty-first payment exceeds fixed array capacity")
    void paymentHistoryFailsAtFixedCapacityBoundary() {
        Payments payments = new Payments();
        payments.setAmtOwed(10_000);

        for (int index = 0; index < 20; index++) {
            payments.makePayment("customer", 1);
        }

        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> payments.makePayment("customer", 1));
    }

    @Test
    @DisplayName("zero-length payment term returns zero")
    void zeroLengthPaymentTermReturnsZero() {
        Payments payments = new Payments();
        payments.setAmtOwed(1_200);

        assertEquals(0, payments.MPay(0));
    }
}
