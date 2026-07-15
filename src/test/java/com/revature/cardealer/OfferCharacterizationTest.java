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
    @Tag("KNOWN-DEFECT")
    @DisplayName("accepting a pending offer does not execute the acceptance loop")
    void setAcceptReturnsListingPriceWithoutAccepting() {
        Offer offer = new Offer();
        offer.registerOffer("Honda", "Accord", 2018, 15_000, "yes", 2);
        offer.setpOffer("customer-one", 0);

        int result = offer.setAccept(0, 12, true);

        assertEquals(15_000, result);
        assertFalse(offer.getAccept());
        assertEquals(2, offer.getAmt());
        assertEquals(0, offer.getMpay());
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
    @Tag("KNOWN-DEFECT")
    @DisplayName("rejectAllOffers clears only index zero")
    void rejectAllOffersLeavesLaterPendingEntriesUntouched() throws Exception {
        Offer offer = new Offer();
        int[] pendingOfferNumbers = privateIntArray(offer, "cpOffer");
        int[] pendingPrices = privateIntArray(offer, "cpPrice");
        String[] customerNames = privateStringArray(offer, "cName");

        pendingOfferNumbers[0] = 1;
        pendingOfferNumbers[1] = 2;
        pendingPrices[0] = 10_000;
        pendingPrices[1] = 20_000;
        customerNames[0] = "first";
        customerNames[1] = "second";

        offer.rejectAllOffers();

        assertEquals(0, pendingOfferNumbers[0]);
        assertEquals(0, pendingPrices[0]);
        assertEquals(" ", customerNames[0]);
        assertEquals(2, pendingOfferNumbers[1]);
        assertEquals(20_000, pendingPrices[1]);
        assertEquals("second", customerNames[1]);
    }

    @Test
    @Tag("KNOWN-DEFECT")
    @DisplayName("getpOffers walks one element beyond the fixed array")
    void getPendingOffersTerminatesWithArrayIndexFailure() {
        Offer offer = new Offer();

        assertThrows(ArrayIndexOutOfBoundsException.class, offer::getpOffers);
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
