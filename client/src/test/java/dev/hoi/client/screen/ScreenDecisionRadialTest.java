package dev.hoi.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static dev.hoi.client.screen.ScreenDecisionRadial.*;
import static org.junit.jupiter.api.Assertions.*;

class ScreenDecisionRadialTest {
    private static final Color FIRST = new Color(1f, .8f, 0f, 0f);
    private static final Color SECOND = new Color(.301f, .301f, .301f, 1f);
    private static final Mask EMPTY = new Mask(1, 1, new byte[]{0});
    private static final Mask SOLID = new Mask(1, 1, new byte[]{(byte)255});
    @Test void packedSourceValueUsesCurrentStateTimesOneHundredThousandWithoutAnOffsetOrClamping() {
        var frame = Frame.from(40060, 100000);
        assertEquals(.4f, frame.start(), .00001f);
        assertEquals(.6f, frame.end(), .00001f);
        assertEquals(0f, Frame.from(499, 100000).start());
        assertEquals(.01f, Frame.from(500, 100000).start());
        assertEquals(5f, Frame.from(500, 100000).end());
        assertThrows(IllegalArgumentException.class, () -> Frame.from(Double.NaN, 100000));
        assertThrows(IllegalArgumentException.class, () -> Frame.from(30, 99999));
    }
    @Test void sourceAngleFunctionRetainsAxisBranchesAndNonAtan2Quadrants() {
        float pi = 3.14159265f;
        assertEquals(0f, atan2Tno(0, 0));
        assertEquals(pi / 2f, atan2Tno(0, 1));
        assertEquals(-pi / 2f, atan2Tno(0, -1));
        assertEquals(pi / 4f, atan2Tno(1, 1), .000001f);
        assertEquals(3f * pi / 4f, atan2Tno(1, -1), .000001f);
        assertEquals(-pi / 4f, atan2Tno(-1, 1), .000001f);
        assertEquals(-3f * pi / 4f, atan2Tno(-1, -1), .000001f);
    }
    @Test void annularMaskPreservesTransparentCenterAndOutsideAndForcesTheFirstAlpha() {
        var frame = Frame.from(100, 100000);
        assertEquals(0, pixel(.5f, .982f, frame, FIRST, SECOND, EMPTY, EMPTY));
        assertEquals(0, pixel(.5f, 0f, frame, FIRST, SECOND, EMPTY, EMPTY));
        assertEquals(0, pixel(-.001f, .982f, frame, FIRST, SECOND, EMPTY, EMPTY));
        assertEquals(0xFFFFCC00, pixel(0f, .982f, frame, FIRST, SECOND, EMPTY, EMPTY));
        assertEquals(0xFFFFCC00, pixel(.5f, .6f, frame, FIRST, SECOND, EMPTY, EMPTY));
        var transparentSecond = new Color(.301f, .301f, .301f, 0f);
        var divided = Frame.from(30, 100000);
        assertEquals(0, atRight(divided, FIRST, transparentSecond, EMPTY, EMPTY) >>> 24);
    }
    @Test void originalIndependentPartyColorsUseBothAlphaMasksAndPointSixFiveLogoShade() {
        var frame = Frame.from(30, 100000);
        assertEquals(0xFFFFCC00, atLeft(frame, FIRST, SECOND, EMPTY, EMPTY));
        assertEquals(0xFFA68500, atLeft(frame, FIRST, SECOND, SOLID, EMPTY));
        assertEquals(0xFF4D4D4D, atRight(frame, FIRST, SECOND, EMPTY, EMPTY));
        assertEquals(0xFF323232, atRight(frame, FIRST, SECOND, EMPTY, SOLID));
        var faint = new Mask(1, 1, new byte[]{1});
        assertEquals(atLeft(frame, FIRST, SECOND, SOLID, EMPTY), atLeft(frame, FIRST, SECOND, faint, EMPTY));
    }
    @Test void rightLogoChecksItsOwnVerticalBoundEvenWhenTheLeftPositionExceedsOne() {
        var frame = Frame.from(80, 100000);
        float leftImageYAtRightMidpoint = (frame.lhsY() - frame.rhsY()) * 3.2f + .5f;
        assertTrue(leftImageYAtRightMidpoint > 1f);
        assertEquals(0xFF323232, atRight(frame, FIRST, SECOND, SOLID, SOLID));
    }
    @Test void pointSamplingWrapsBothAxesAndTransparentImagesRemainValidMasks() {
        var mask = new Mask(2, 2, new byte[]{1, 0, 0, 1});
        assertTrue(mask.pointWrap(0, 0));
        assertFalse(mask.pointWrap(.5f, 0));
        assertTrue(mask.pointWrap(1, 1));
        assertTrue(mask.pointWrap(-.25f, -.25f));
        assertNotNull(Mask.from(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)));
        assertFalse(Mask.from(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)).pointWrap(0, 0));
        assertNull(Mask.from(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)));
    }
    @Test void smallRasterKeepsFinalShaderUvOrientationAndReusesPixelsOnlyForTheSameFrame() {
        var descriptor = new Descriptor(4, 4, 100000, "one", "two", FIRST, SECOND, "source");
        var raster = new Raster(descriptor, EMPTY, EMPTY);
        int[] first = raster.pixels(100);
        assertSame(first, raster.pixels(100));
        assertEquals(0, first[1]);
        assertEquals(0xFFFFCC00, first[2 * 4 + 1]);
        assertNotSame(first, raster.pixels(30));
        assertSame(raster.pixels(30), raster.pixels(30));
        assertThrows(IllegalArgumentException.class, () -> raster.pixels(Double.POSITIVE_INFINITY));
    }
    @Test void descriptorRequiresTheAuditedShaderAndIncludeAndRetainsOriginalMetadata() {
        JsonObject source = source();
        var parsed = descriptor(source);
        assertNotNull(parsed);
        assertEquals(350, parsed.width());
        assertEquals(100000, parsed.steps());
        assertEquals(FIRST, parsed.one());
        assertEquals(SECOND, parsed.two());
        assertEquals(source.toString(), parsed.provenance());
        source.addProperty("effectSha256", "0".repeat(64));
        assertNull(descriptor(source));
        source = source();
        source.getAsJsonArray("includes").get(0).getAsJsonObject().addProperty("sha256", "0".repeat(64));
        assertNull(descriptor(source));
        source = source();
        source.addProperty("steps", 99999);
        assertNull(descriptor(source));
        source = source();
        source.addProperty("texture1", "../../missing");
        assertNull(descriptor(source));
        source = source();
        source.addProperty("scale", 1);
        assertNull(descriptor(source));
    }
    @Test void onlyTheFiveKnownSpritesHaveDimensionsAndUnknownFramesNeverBecomeZero() {
        var root = new JsonObject();
        var radial = new JsonObject();
        root.add("radial", radial);
        for (String name : new String[]{"Ind", "JP", "PP", "PPP", "DP", "invented"}) radial.add("GFX_KOR_" + name + "_Bar", source());
        try (var renderer = new ScreenDecisionRadial()) {
            renderer.configure(root);
            assertArrayEquals(new int[]{350, 350}, renderer.dimensions("GFX_KOR_Ind_Bar"));
            assertArrayEquals(new int[]{0, 0}, renderer.dimensions("GFX_KOR_invented_Bar"));
            assertFalse(renderer.draw(null, "GFX_KOR_Ind_Bar", null, 0, 0, 350, 350));
            assertFalse(validFrame(Double.NaN));
            assertFalse(validFrame(Double.POSITIVE_INFINITY));
            assertFalse(validFrame(Double.MAX_VALUE));
            assertTrue(validFrame(0));
            renderer.clear();
            assertArrayEquals(new int[]{350, 350}, renderer.dimensions("GFX_KOR_Ind_Bar"));
            renderer.configure(null);
            assertArrayEquals(new int[]{0, 0}, renderer.dimensions("GFX_KOR_Ind_Bar"));
        }
    }
    private static int atLeft(Frame frame, Color one, Color two, Mask first, Mask second) {
        return pixel(frame.lhsX() + .5f, .982f - frame.lhsY(), frame, one, two, first, second);
    }
    private static int atRight(Frame frame, Color one, Color two, Mask first, Mask second) {
        return pixel(frame.rhsX() + .5f, .982f - frame.rhsY(), frame, one, two, first, second);
    }
    private static JsonObject source() {
        return JsonParser.parseString("""
                {"width":350,"height":350,"steps":100000,"color":[1,0.8,0,0],"colorTwo":[0.301,0.301,0.301,1],
                "texture1":"decision/panel/gfx_kor_ind_bar/texturefile1","texture2":"decision/panel/gfx_kor_ind_bar/texturefile2",
                "scale":null,"source":"korea/interface/TFRKP/TFR_Korea_NatAsm.gfx",
                "sourceSha256":"f47c32a4acbc8f9f6f663f75e2328c51240706ebf94b6f0e186383c8c304d13b","line":72,
                "effectFile":"gfx/FX/progress_radial_legco.shader",
                "effectSha256":"bb72c5ae8831120923eb9606b5d42b2497b595f8167a54c554cd39685b9f2a07",
                "includes":[{"source":"korea/gfx/FX/the_fire_rises_korea_plus.fxh",
                "sha256":"ce36e0c3593f76798bf7a8ddcf1c0882510a14bdc74c418481589e8766a8f6ed"}]}
                """).getAsJsonObject();
    }
}
