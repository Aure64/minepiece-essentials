package com.minepiece.essentials.donate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
