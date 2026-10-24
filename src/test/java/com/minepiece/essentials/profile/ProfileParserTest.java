package com.minepiece.essentials.profile;

import com.minepiece.essentials.pet.PetStat;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** /profil "Information" item as dumped on 2026-10-24. */
class ProfileParserTest {

    private static final List<String> INFO = List.of(
        "Information", "", "❦ Progression", "▪ Ascension: 1", "▪ Level: 97 (67.41%)", "",
        "485712 s m m m m m m m m e   720550", "", "🧪 Statistics",
        "犹 Health +1697", "乘 Strength +205", "挂 Damage +0", "",
        "奖 Critical Chance +23%", "绍 Critical Damage +150", "",
        "厚 Power +152", "纵 Energy +1343", "障 Energy Regeneration +80", "",
        "讯 Speed +87", "涉 Dexterity +111", "彻 Defense +233", "",
        "丈 Regeneration +21", "",
        "⚔ Obtaining", "Increase your player ", "level by fighting ", "monsters.");

    @Test
    void parsesProgression() {
        ProfileData p = ProfileParser.parse(INFO).orElseThrow();
        assertEquals(1, p.ascension());
        assertEquals(97, p.level());
        assertEquals(67.41, p.levelPercent(), 1e-9);
        assertEquals(485712, p.xp());
        assertEquals(720550, p.maxXp());
    }

    @Test
    void parsesEveryStatLineInOrder() {
        ProfileData p = ProfileParser.parse(INFO).orElseThrow();
        assertEquals(12, p.entries().size());
        assertEquals("Damage", p.entries().get(2).label());
        assertNull(p.entries().get(2).stat());
        assertEquals(1697, p.byStat().get(PetStat.VITALITY), 1e-9);
        assertEquals(23, p.byStat().get(PetStat.CRIT_CHANCE), 1e-9);
        assertTrue(p.isPercent(PetStat.CRIT_CHANCE));
        assertFalse(p.isPercent(PetStat.STRENGTH));
        assertEquals(21, p.byStat().get(PetStat.REGEN), 1e-9);
        assertEquals(80, p.byStat().get(PetStat.ENERGY_REGEN), 1e-9);
    }

    @Test
    void rejectsOtherItems() {
        assertTrue(ProfileParser.parse(List.of("Tamago's Hat", "犹 Health +110")).isEmpty());
        assertTrue(ProfileParser.parse(List.of("Information", "🧪 Statistics", "⚔ Obtaining")).isEmpty());
    }
}
