package com.revature.cardealer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("LEGACY-BEHAVIOR")
class OfferCharacterizationTest {

    @Test
    @DisplayName("setOffer ignores the supplied availability text and stores yes")
    void setOfferForcesAvailabilityToYes() {
        Offer offer = new Offer();

        offer.setOffer(new Car("Honda", "Accord", 2018), 15_000, "no", 2);

        assertEquals("yes", offer.getAvail());
        assertEquals(15_000, offer.getPrice());
        assertEquals(2, offer.getAmt());
    }

    @Test
    @DisplayName("accepting a pending offer consumes the request and calculates monthly payment")
    void setAcceptConsumesPendingRequest() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15_000, "yes", 2);
        offer.setpOffer("customer-one", 0);

        assertEquals(1250, offer.setAccept(0, 12, true));
        assertTrue(offer.getAccept());
        assertEquals(1, offer.getAmt());
        assertEquals(1250, offer.getMpay());
        assertThrows(IllegalArgumentException.class, () -> offer.setAccept(0, 12, true));
    }

    @Test
    void approvalsConsumeOnlyTheSelectedListingInventory() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offer.registerOffer("Toyota", "Corolla", 2020, 9000, "yes", 3);
        offer.setpOffer("buyer-honda", 0);
        offer.setpOffer("buyer-toyota", 1);
        assertEquals(1, offer.getListingStock(0));
        assertEquals(3, offer.getListingStock(1));
        assertEquals(1250, offer.setAccept(0, 12, true));
        assertEquals(0, offer.getListingStock(0));
        assertEquals(3, offer.getListingStock(1));
        assertEquals(750, offer.setAccept(1, 12, true));
        assertEquals(2, offer.getListingStock(1));
    }

    @Test
    void exhaustedListingDoesNotConsumePendingRequestOrOtherListingStock() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offer.registerOffer("Toyota", "Corolla", 2020, 9000, "yes", 2);
        offer.setpOffer("first", 0);
        offer.setpOffer("second", 0);
        offer.setpOffer("other", 1);
        offer.setAccept(0, 12, true);
        assertThrows(IllegalStateException.class, () -> offer.setAccept(0, 12, true));
        assertEquals(0, offer.getListingStock(0));
        assertEquals(2, offer.getListingStock(1));
        assertEquals(750, offer.setAccept(1, 12, true));
    }

    @Test
    void registrationRejectsInvalidStockPriceAndFullListingCapacity() {
        Offer offer = new Offer();
        assertThrows(IllegalArgumentException.class,
                () -> offer.registerOffer("Honda", "Accord", 2018, -1, "yes", 1));
        assertThrows(IllegalArgumentException.class,
                () -> offer.registerOffer("Honda", "Accord", 2018, 15000, "yes", -1));
        assertThrows(IllegalArgumentException.class,
                () -> offer.registerOffer("", "Accord", 2018, 15000, "yes", 1));
        for (int i = 0; i < 20; i++) {
            offer.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        }
        assertThrows(IllegalArgumentException.class,
                () -> offer.registerOffer("Toyota", "Corolla", 2020, 9000, "yes", 1));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void purchaseRequestsDoNotConsumeVehicleListingSlots() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15000, "yes", 8);
        for (int i = 0; i < 5; i++) {
            offer.setpOffer("request-" + i, 0);
        }

        offer.registerOffer("Toyota", "Corolla", 2020, 9000, "yes", 2);

        assertEquals(15000, offer.getListingPrice(0));
        assertEquals(9000, offer.getListingPrice(1));
        assertEquals(2, offer.getListingStock(1));
        assertEquals("request-0", offer.getPendingBuyer(0));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void completedRequestsReleaseCapacityWithoutChangingFifoOrder() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 12000, "yes", 21);
        for (int i = 0; i < 20; i++) {
            offer.setpOffer("buyer-" + i, 0);
        }
        assertThrows(IllegalArgumentException.class,
                () -> offer.setpOffer("overflow", 0));

        assertEquals(1000, offer.setAccept(0, 12, true));
        assertEquals("buyer-1", offer.getPendingBuyer(0));

        offer.setpOffer("replacement", 0);
        // The new request joins the end rather than overtaking old requests.
        assertEquals("buyer-1", offer.getPendingBuyer(0));
        for (int i = 1; i < 20; i++) {
            assertEquals("buyer-" + i, offer.getPendingBuyer(0));
            assertEquals(1000, offer.setAccept(0, 12, true));
        }
        assertEquals("replacement", offer.getPendingBuyer(0));
        assertEquals(1000, offer.setAccept(0, 12, true));
        assertEquals(0, offer.getListingStock(0));
        assertThrows(IllegalArgumentException.class,
                () -> offer.getPendingBuyer(0));
    }

    @Test
    @Tag("TARGET-BEHAVIOR")
    void postCompactionFailureRestoresOldestBuyerAndPendingCount() {
        Offer offer = new Offer() {
            private boolean failFirstApproval = true;

            @Override
            public synchronized int setAccept(int listing, int months, boolean accept) {
                int payment = super.setAccept(listing, months, accept);
                if (failFirstApproval) {
                    failFirstApproval = false;
                    throw new IllegalStateException("injected failure after queue compaction");
                }
                return payment;
            }
        };
        offer.registerOffer("Honda", "Accord", 2018, 12000, "yes", 3);
        offer.setpOffer("first", 0);
        offer.setpOffer("second", 0);

        assertThrows(IllegalStateException.class,
                () -> offer.acceptWithRollback(0, 12));
        assertEquals(3, offer.getListingStock(0));
        assertEquals("first", offer.getPendingBuyer(0));

        offer.setpOffer("third", 0);
        assertEquals(1000, offer.acceptWithRollback(0, 12));
        assertEquals("second", offer.getPendingBuyer(0));
        assertEquals(1000, offer.acceptWithRollback(0, 12));
        assertEquals("third", offer.getPendingBuyer(0));
        assertEquals(1000, offer.acceptWithRollback(0, 12));
        assertEquals(0, offer.getListingStock(0));
        assertThrows(IllegalArgumentException.class,
                () -> offer.getPendingBuyer(0));
    }

    @Test
    void soldOutListingCannotReceiveNewPurchaseRequest() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15000, "yes", 1);
        offer.setpOffer("first", 0);
        offer.setAccept(0, 12, true);
        assertThrows(IllegalArgumentException.class, () -> offer.setpOffer("second", 0));
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @DisplayName("getAccept uses an inverted loop condition and leaves state unchanged")
    void getAcceptDoesNotInspectProvidedOffers() {
        Offer offer = new Offer();
        offer.setAccept();

        offer.getAccept(new Offer[] { offer });

        assertTrue(offer.getAccept());
    }

    @Test
    @DisplayName("rejectAllOffers clears all submitted pending requests")
    void rejectAllOffersClearsPendingRequests() throws Exception {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15_000, "yes", 2);
        offer.setpOffer("first", 0);
        offer.setpOffer("second", 0);
        offer.rejectAllOffers();
        assertEquals(0, privateIntArray(offer, "cpOffer")[0]);
        assertEquals(0, privateIntArray(offer, "cpOffer")[1]);
        assertEquals(null, privateStringArray(offer, "cName")[0]);
        assertEquals(null, privateStringArray(offer, "cName")[1]);
        assertThrows(IllegalArgumentException.class, () -> offer.setAccept(0, 12, true));
    }

    @Test
    @DisplayName("pending offer listing handles empty inventory")
    void getPendingOffersHandlesEmptyState() {
        Offer offer = new Offer();
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(offer::getpOffers);
    }

    @Test
    @DisplayName("removeOffer replaces a listing with an empty string")
    void removeOfferLeavesEmptySlotText() {
        Offer offer = new Offer();
        offer.registerOffer("Toyota", "Corolla", 2017, 12_000, "yes", 1);

        offer.removeOffer(0);

        assertEquals("", offer.offerDB[0]);
    }

    private int[] privateIntArray(Offer target, String fieldName) throws Exception {
        Field field = Offer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (int[]) field.get(target);
    }

    private String[] privateStringArray(Offer target, String fieldName) throws Exception {
        Field field = Offer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String[]) field.get(target);
    }
}
