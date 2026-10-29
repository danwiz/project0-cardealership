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
