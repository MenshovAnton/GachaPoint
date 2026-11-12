package ru.menshovanton.gachapoint.data.local;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.testutil.InMemoryTrackerTestHarness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PreferencesTest {

    private Preferences preferences;

    @Before
    public void setUp() {
        InMemoryTrackerTestHarness harness = new InMemoryTrackerTestHarness();
        preferences = harness.preferences;
    }

    @Test
    public void isGameEnabledReflectsEnabledGamesList() {
        preferences.setEnabledGames(Arrays.asList(GameType.GENSHIN, GameType.ZZZ));

        assertTrue(preferences.isGameEnabled(GameType.GENSHIN));
        assertFalse(preferences.isGameEnabled(GameType.HSR));
        assertTrue(preferences.isGameEnabled(GameType.ZZZ));
        assertFalse(preferences.isGameEnabled(GameType.HI3));
    }

    @Test
    public void disablingDefaultGameSwitchesDefaultToFirstEnabledGame() {
        preferences.setEnabledGames(Arrays.asList(GameType.GENSHIN, GameType.HSR, GameType.ZZZ));
        preferences.setDefaultGame(GameType.GENSHIN);

        preferences.setGameEnabled(GameType.GENSHIN, false);

        assertFalse(preferences.isGameEnabled(GameType.GENSHIN));
        assertEquals(GameType.HSR, preferences.getDefaultGame());
    }
}
