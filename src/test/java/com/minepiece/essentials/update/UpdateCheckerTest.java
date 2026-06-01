package com.minepiece.essentials.update;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Semantic-ish version comparison used to decide if an update notice should show. */
class UpdateCheckerTest {

    @Test
    void newerPatchIsNewer() {
        assertTrue(UpdateChecker.isNewer("1.1.2", "1.1.1"));
    }

    @Test
    void newerMinorIsNewer() {
        assertTrue(UpdateChecker.isNewer("1.2.0", "1.1.9"));
    }

    @Test
    void sameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.1.1", "1.1.1"));
    }

    @Test
    void olderIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("1.1.0", "1.1.1"));
    }

    @Test
    void ignoresBuildSuffix() {
        assertFalse(UpdateChecker.isNewer("1.1.1", "1.1.1+build.5"));
        assertTrue(UpdateChecker.isNewer("1.2.0+build.1", "1.1.1"));
    }
}
