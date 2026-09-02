package com.minepiece.essentials.donate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Couvre uniquement le parsing/validation pur du montant, sans dépendance à un
 * client Minecraft en cours d'exécution.
 */
class DonateScreenTest {

    @Test
    void plainDigitsParse() {
        assertEquals(50000L, DonateScreen.parseAmount("50000"));
    }

    @Test
    void spacesAsThousandsSeparatorsAreTolerated() {
        assertEquals(1000000L, DonateScreen.parseAmount("1 000 000"));
    }

    @Test
    void zeroIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount("0"));
    }

    @Test
    void negativeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount("-100"));
    }

    @Test
    void emptyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount(""));
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount("   "));
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount(null));
    }

    @Test
    void lettersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount("abc"));
        assertThrows(IllegalArgumentException.class, () -> DonateScreen.parseAmount("100k"));
    }

    @Test
    void valueAboveIntMaxParsesAsLong() {
        String big = Long.toString((long) Integer.MAX_VALUE + 1L);
        assertEquals((long) Integer.MAX_VALUE + 1L, DonateScreen.parseAmount(big));
    }

    // --- isArmed : délai d'armement de la confirmation (défense anti double-clic) ---

    @Test
    void notArmedBeforeDelayElapsed() {
        long enteredAt = 1_000L;
        long justBefore = enteredAt + DonateScreen.CONFIRM_ARM_DELAY_MS - 1;
        assertFalse(DonateScreen.isArmed(enteredAt, justBefore));
    }

    @Test
    void armedExactlyAtDelay() {
        long enteredAt = 1_000L;
        long atThreshold = enteredAt + DonateScreen.CONFIRM_ARM_DELAY_MS;
        assertTrue(DonateScreen.isArmed(enteredAt, atThreshold));
    }

    @Test
    void armedAfterDelayElapsed() {
        long enteredAt = 1_000L;
        long justAfter = enteredAt + DonateScreen.CONFIRM_ARM_DELAY_MS + 1;
        assertTrue(DonateScreen.isArmed(enteredAt, justAfter));
    }
}
