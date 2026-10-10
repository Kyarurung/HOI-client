package dev.hoi.client.ui;

import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static dev.hoi.client.ui.UiDiplomacyAssets.*;
import static org.junit.jupiter.api.Assertions.*;

class UiDiplomacyAssetsTest {
    @Test void sourceDisabledEffectUsesLuminanceWithoutDarkeningAlpha() {
        assertEquals(0x80363636, shade(0x80FF0000, Effect.BUTTON_STATE, State.DISABLED, 0));
        assertEquals(0xFFB6B6B6, shade(0xFF00FF00, Effect.BUTTON_STATE, State.DISABLED, 0));
        assertEquals(0xFF121212, shade(0xFF0000FF, Effect.NO_DOWN, State.DISABLED, 0));
        assertEquals(0x804080C0, shade(0x804080C0, Effect.NONE, State.DISABLED, 1));
    }
    @Test void originalNonMonotonicButtonTimingAndDifferentRatesArePreserved() {
        assertEquals(0f, mix(Effect.BUTTON_STATE, State.OVER, 0));
        assertEquals(.0975f, mix(Effect.BUTTON_STATE, State.OVER, .125f), .000001f);
        assertEquals(-.0975f, mix(Effect.BUTTON_STATE, State.DOWN, .03125f), .000001f);
        assertEquals(.1215f, mix(Effect.BUTTON_STATE, State.OVER, .225f), .000001f);
        assertEquals(.12f, mix(Effect.BUTTON_STATE, State.OVER, .25f), .000001f);
        assertEquals(.12f, mix(Effect.BUTTON_STATE, State.OVER, 100), .000001f);
        assertEquals(0f, mix(Effect.BUTTON_STATE, State.OVER, -1));
        assertEquals(0xFF0F0F0F, shade(0xFF000000, Effect.BUTTON_STATE, State.OVER, .225f));
        assertEquals(0xFFDADADA, shade(0xFFFFFFFF, Effect.BUTTON_STATE, State.DOWN, .03125f));
    }
    @Test void noDownEffectAddsPointOneAndLeavesPressedPixelsUnchanged() {
        assertEquals(0x805A9ADA, shade(0x804080C0, Effect.NO_DOWN, State.OVER, 0));
        assertEquals(0x804080C0, shade(0x804080C0, Effect.NO_DOWN, State.DOWN, 10));
        assertEquals(0x80FFFFFF, shade(0x80F0F0F0, Effect.NO_DOWN, State.OVER, 10));
    }
    @Test void linearClampSamplingOccursBeforeShaderAdjustmentAndOutputClamping() {
        var image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFF000000);
        image.setRGB(1, 0, 0xFFFFFFFF);
        assertEquals(0xFF808080, sample(image, .5f, .5f, Effect.BUTTON_STATE, State.UP, 0));
        assertEquals(0xFF9E9E9E, sample(image, .5f, .5f, Effect.BUTTON_STATE, State.OVER, .1215f));
        assertEquals(0xFF000000, sample(image, 0, .5f, Effect.BUTTON_STATE, State.UP, 0));
        assertEquals(0xFFFFFFFF, sample(image, 1, .5f, Effect.BUTTON_STATE, State.UP, 0));
        assertEquals(0xFFFFFFFF, sample(image, .5f, .5f, Effect.NO_DOWN, State.UP, 0));
    }
    @Test void controlsMaintainIndependentTimesAndResetOnStateOrSpriteChanges() {
        try (var assets = new UiDiplomacyAssets()) {
            assertEquals(0f, assets.animation("a", "one", Effect.BUTTON_STATE, State.OVER, 100));
            assertEquals(.2f, assets.animation("a", "one", Effect.BUTTON_STATE, State.OVER, 200_000_100), .000001f);
            assertEquals(0f, assets.animation("b", "one", Effect.BUTTON_STATE, State.OVER, 200_000_100));
            assertEquals(0f, assets.animation("a", "one", Effect.BUTTON_STATE, State.DOWN, 200_000_100));
            assertEquals(0f, assets.animation("a", "two", Effect.BUTTON_STATE, State.DOWN, 400_000_100));
            assets.close();
            assertEquals(0f, assets.animation("a", "two", Effect.BUTTON_STATE, State.DOWN, 600_000_100));
            resourcesReloaded();
            assertEquals(0f, assets.animation("a", "two", Effect.BUTTON_STATE, State.DOWN, 800_000_100));
        }
    }
    @Test void actualTexturePixelsRetainSixtyFourPixelBordersAndRepeatTheSixtyTwoPixelCenter() {
        var tiles = tiles(375, 501, 190, 190, 64, 64, 1);
        var firstCenter = tiles.stream().filter(tile -> tile.x0() == 64 && tile.y0() == 64).findFirst().orElseThrow();
        assertEquals(126, firstCenter.x1());
        assertEquals(126, firstCenter.y1());
        assertEquals(64f / 190, firstCenter.u0());
        assertEquals(126f / 190, firstCenter.u1());
        var lastCenter = tiles.stream().filter(tile -> tile.x0() == 250 && tile.y0() == 436).findFirst().orElseThrow();
        assertEquals(311, lastCenter.x1());
        assertEquals(437, lastCenter.y1());
        assertEquals(125f / 190, lastCenter.u1());
        assertEquals(65f / 190, lastCenter.v1());
        assertCoveredOnce(375, 501, tiles);
    }
    @Test void scrollbarHasNoHorizontalBorderAndKeepsBothFiftyTwoPixelCaps() {
        var tiles = tiles(18, 300, 18, 156, 0, 52, 1);
        assertEquals(6, tiles.size());
        assertTrue(tiles.stream().allMatch(tile -> tile.x0() == 0 && tile.x1() == 18 && tile.u0() == 0 && tile.u1() == 1));
        assertEquals(52, tiles.getFirst().y1());
        assertEquals(248, tiles.getLast().y0());
        assertEquals(104f / 156, tiles.getLast().v0());
        assertCoveredOnce(18, 300, tiles);
        assertCoveredOnce(12, 180, tiles(12, 180, 12, 12, 4, 4, 1));
        assertCoveredOnce(40, 30, tiles(40, 30, 190, 190, 64, 64, 1));
    }
    @Test void invalidTileGeometryCannotProduceQuads() {
        assertTrue(tiles(100, 100, 190, 190, 95, 64, 1).isEmpty());
        assertTrue(tiles(100, 100, 190, 190, 64, 64, Double.NaN).isEmpty());
        assertTrue(tiles(100, 0, 190, 190, 64, 64, 1).isEmpty());
    }
    private static void assertCoveredOnce(int width, int height, java.util.List<Tile> tiles) {
        var coverage = new byte[width * height];
        for (Tile tile : tiles) {
            assertTrue(tile.u0() >= 0 && tile.u1() <= 1 && tile.v0() >= 0 && tile.v1() <= 1);
            for (int y = tile.y0(); y < tile.y1(); y++) for (int x = tile.x0(); x < tile.x1(); x++) coverage[y * width + x]++;
        }
        for (byte count : coverage) assertEquals(1, count);
    }
}
