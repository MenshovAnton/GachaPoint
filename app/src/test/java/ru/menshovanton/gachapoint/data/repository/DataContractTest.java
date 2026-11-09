package ru.menshovanton.gachapoint.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.CatalogItem;
import ru.menshovanton.gachapoint.domain.models.PatchInfo;
import ru.menshovanton.gachapoint.domain.models.PromoCode;

public class DataContractTest {

    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,30}$");
    private static final Set<String> BANNER_STATUSES = new HashSet<>(
            Arrays.asList("CURRENT", "ANNOUNCED", "EXPIRED", "PAST")
    );
    private static final Set<String> CODE_STATUSES = new HashSet<>(
            Arrays.asList("ACTIVE", "STREAM", "NEW", "PERMANENT", "EXPIRED")
    );
    private static final Set<String> ITEM_TYPES = new HashSet<>(
            Arrays.asList("CHARACTER", "WEAPON", "BANGBOO", "ASTRAL_OP", "ELF")
    );
    private static final String ICON_PREFIX = DataSyncConfig.BASE_REMOTE_URL + "avatars/";

    @Test
    public void catalogParsesAndMatchesContract() throws Exception {
        Map<Integer, List<CatalogItem>> catalog = new HashMap<>();
        Map<Integer, Map<String, CatalogItem>> lookup = new HashMap<>();

        CatalogRepository.parseJson(readAsset("game_catalog.json"), catalog, lookup);

        assertEquals(4, catalog.size());
        for (int gameType = 0; gameType <= 3; gameType++) {
            List<CatalogItem> items = catalog.get(gameType);
            assertNotNull("Missing game " + gameType, items);
            assertFalse("Empty game " + gameType, items.isEmpty());
            assertNotNull(lookup.get(gameType));
            assertFalse(Objects.requireNonNull(lookup.get(gameType)).isEmpty());

            Set<String> names = new HashSet<>();
            for (CatalogItem item : items) {
                assertFalse(item.getNameRu().trim().isEmpty());
                assertFalse(item.getNameEn().trim().isEmpty());
                assertTrue(ITEM_TYPES.contains(item.getType()));
                assertTrue(allowedRarities(gameType).contains(item.getRarity()));
                assertTrue("Duplicate catalog name: " + item.getNameRu(),
                        names.add(item.getNameRu().toLowerCase(Locale.ROOT)));
                if (item.getIconUrl() != null) {
                    assertTrue(item.getIconUrl().startsWith(ICON_PREFIX));
                }
            }
        }
    }

    @Test
    public void bannerScheduleParsesAndMatchesContract() throws Exception {
        String json = readAsset("banners_schedule.json");
        Map<Integer, List<PatchInfo>> patches = new HashMap<>();
        Map<Integer, List<BannerScheduleItem>> banners = new HashMap<>();

        BannerScheduleRepository.parseScheduleJson(json, patches, banners);

        assertEquals(4, banners.size());
        for (int gameType = 0; gameType <= 3; gameType++) {
            List<BannerScheduleItem> items = banners.get(gameType);
            assertNotNull("Missing game " + gameType, items);
            assertFalse("Empty game " + gameType, items.isEmpty());

            Set<String> ids = new HashSet<>();
            for (BannerScheduleItem item : items) {
                assertTrue("Duplicate banner id: " + item.getId(), ids.add(item.getId()));
                assertFalse(item.getVersion().isEmpty());
                assertTrue(item.getPhase() >= 1 && item.getPhase() <= 3);
                assertTrue(DATE_PATTERN.matcher(item.getStartDate()).matches());
                assertTrue(DATE_PATTERN.matcher(item.getEndDate()).matches());
                assertFalse(LocalDate.parse(item.getEndDate()).isBefore(LocalDate.parse(item.getStartDate())));
                assertTrue(BANNER_STATUSES.contains(item.getStatus().toUpperCase(Locale.ROOT)));
                assertFalse("Missing 5-star list: " + item.getId(), item.getFeaturedFiveStars().isEmpty());
                assertEquals(item.getFeaturedFiveStarsRu().size(), item.getFeaturedFiveStarsEn().size());
                assertEquals(item.getFeaturedFourStarsRu().size(), item.getFeaturedFourStarsEn().size());
            }

            List<PatchInfo> patchItems = patches.get(gameType);
            assertNotNull("Missing patches for game " + gameType, patchItems);
            for (PatchInfo patch : patchItems) {
                assertFalse(patch.getVersion().isEmpty());
                assertTrue(DATE_PATTERN.matcher(patch.getStartDate()).matches());
                assertTrue(DATE_PATTERN.matcher(patch.getEndDate()).matches());
                assertFalse(LocalDate.parse(patch.getEndDate()).isBefore(LocalDate.parse(patch.getStartDate())));
            }
        }

        assertCurrentBannersAreVerified(json);
    }

    @Test
    public void promocodesParseAndMatchContract() throws Exception {
        String json = readAsset("promocodes.json");
        List<PromoCode> codes = PromocodeRepository.parseJson(json, -1);

        assertFalse(codes.isEmpty());
        Set<String> keys = new HashSet<>();
        for (PromoCode code : codes) {
            assertTrue(code.getGameType() >= 0 && code.getGameType() <= 3);
            assertTrue("Invalid code: " + code.getCode(), CODE_PATTERN.matcher(code.getCode()).matches());
            assertFalse(code.getRewardsDescription().trim().isEmpty());
            assertTrue(CODE_STATUSES.contains(code.getStatus().toUpperCase(Locale.ROOT)));
            assertTrue("Duplicate code: " + code.getCode(),
                    keys.add(code.getGameType() + ":" + code.getCode()));
            if (code.getExpiresAt() != null && !code.getExpiresAt().isEmpty()) {
                assertTrue(isInstant(code.getExpiresAt()) || DATE_PATTERN.matcher(code.getExpiresAt()).matches());
            }
        }

        List<PromoCode> genshin = PromocodeRepository.parseJson(json, 0);
        assertFalse(genshin.isEmpty());
        for (PromoCode code : genshin) {
            assertEquals(0, code.getGameType());
        }
    }

    @Test
    public void activeCodeWithoutExpiryIsNotExpired() {
        PromoCode code = new PromoCode(
                "test", 0, "ACTIVECODE", "60 Primogems",
                "60 Камней Истока", "60 Primogems", Collections.emptyList(),
                "ACTIVE", null, "2026-11-01T00:00:00Z",
                false, true, false, false
        );

        assertFalse(code.isPermanent());
        assertFalse(code.isExpired());
        assertTrue(code.isActive());
    }

    @Test
    public void zzzTvcmIsNotPermanent() {
        PromoCode code = new PromoCode(
                "test", 2, "ZZZTVCM", "50 Polychromes",
                "50 Полихромов", "50 Polychromes", Collections.emptyList(),
                "EXPIRED", null, "2024-07-04T00:00:00Z",
                false, false, false, false
        );

        assertFalse(code.isPermanent());
        assertTrue(code.isExpired());
    }

    @Test
    public void cheerCaptainIsNotPermanent() {
        PromoCode code = new PromoCode(
                "test", 3, "CHEERSCAPTAIN", "100 Crystals",
                "100 Кристаллов", "100 Crystals", Collections.emptyList(),
                "EXPIRED", null, "2026-04-12T12:00:00Z",
                false, false, false, false
        );

        assertFalse(code.isPermanent());
        assertTrue(code.isExpired());
    }

    @Test
    public void catalogCleanKeyNormalizesNames() {
        assertEquals("kukishinobu", CatalogRepository.cleanKey("Kuki Shinobu"));
        assertEquals("кукисинобу", CatalogRepository.cleanKey("Куки Синобу"));
    }

    @Test
    public void syncConfigValidatesCodesAndPaths() {
        assertFalse(DataSyncConfig.isValidCode("ABC123"));
        assertTrue(DataSyncConfig.isValidCode("bad code"));
        assertEquals("ABC123", DataSyncConfig.sanitizeCode(" abc123 "));
        assertNull(DataSyncConfig.getOfficialRedeemUrl(GameType.GENSHIN, "bad code"));
        assertTrue(Objects.requireNonNull(DataSyncConfig.getOfficialRedeemUrl(GameType.GENSHIN, "ABC123")).contains("code=ABC123"));
        assertEquals(DataSyncConfig.BASE_REMOTE_URL + "avatars/0/a.png",
                DataSyncConfig.resolveRemotePath("/avatars/0/a.png"));
    }

    @Test
    public void assetsMatchDataHubWhenPresent() throws Exception {
        File hub = new File("../data-automation");
        if (!hub.isDirectory()) {
            hub = new File("data-automation");
        }
        if (!hub.isDirectory()) {
            hub = new File("../../data-automation");
        }
        Assume.assumeTrue(hub.isDirectory());

        for (String name : Arrays.asList("promocodes.json", "banners_schedule.json", "game_catalog.json")) {
            String asset = normalizeLineEndings(readAsset(name));
            String hubJson = normalizeLineEndings(Files.readString(new File(hub, name).toPath()));
            assertEquals("Asset drift: " + name, hubJson, asset);
        }
    }

    private static void assertCurrentBannersAreVerified(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray games = root.getAsJsonArray("games");
        for (JsonElement game : games) {
            JsonArray banners = game.getAsJsonObject().getAsJsonArray("banners");
            for (JsonElement banner : banners) {
                JsonObject object = banner.getAsJsonObject();
                String status = object.get("status").getAsString();
                if (!"CURRENT".equalsIgnoreCase(status) && !"ANNOUNCED".equalsIgnoreCase(status)) {
                    continue;
                }
                String id = object.get("id").getAsString();
                assertTrue("Missing source_url: " + id, object.has("source_url"));
                assertEquals("Unverified banner: " + id,
                        "verified", object.get("verification_status").getAsString());
                assertFalse(object.get("verified_at").getAsString().isEmpty());
            }
        }
    }

    private static Set<String> allowedRarities(int gameType) {
        switch (gameType) {
            case 0:
            case 1:
                return new HashSet<>(Arrays.asList("4★", "5★"));
            case 2:
                return new HashSet<>(Arrays.asList("S", "A"));
            case 3:
                return new HashSet<>(Arrays.asList("S", "A", "B"));
            default:
                return Collections.emptySet();
        }
    }

    private static boolean isInstant(String value) {
        try {
            Instant.parse(value);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n");
    }

    private static String readAsset(String name) throws Exception {
        File direct = new File("src/main/assets/data", name);
        if (direct.isFile()) {
            return Files.readString(direct.toPath());
        }
        File module = new File("app/src/main/assets/data", name);
        if (module.isFile()) {
            return Files.readString(module.toPath());
        }
        throw new IllegalStateException("Asset not found: " + name);
    }
}
