package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.country.CountryPayloads;
import dev.hoi.protocol.country.CountryView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.ArrayList;

final class DiplomacyArtTest {
    private DiplomacyArtTest() {}
    static void run(ClientGameTestContext context, CountryView view, int width) {
        if (view.art().isEmpty()) return;
        var requests = new ArrayList<CountryPayloads.Action>();
        context.setScreen(() -> new DiplomacyCountryScreen(view, requests::add));
        context.getInput().setCursorPos(width - 4, 80);
        context.waitTicks(3);
        context.runOnClient(client -> {
            var screen = (DiplomacyCountryScreen)client.gui.screen();
            int pane = screen.panelWidth(), top = dev.hoi.client.ui.hud.HudMenuBar.height(screen.width);
            var header = new DiplomacyHeader(view, pane);
            var box = DiplomacyHeader.chartBox(pane, top);
            double cx = box[0] + box[2] / 2, cy = box[1] + box[2] / 2, radius = box[2] * 30 / 71, sum = 0;
            for (var party : view.diplomacy()) if ((party.icon().startsWith("politics/party/") || party.icon().startsWith("politics/ideology/")) && party.progress() > 0) {
                double angle = (sum + party.progress() / 2) * 2 * Math.PI;
                if (!party.equals(header.partyAt(view, cx + Math.cos(angle) * radius * .7, cy - Math.sin(angle) * radius * .7, pane, top))) throw new AssertionError("Chart segment hover does not match source order and angle");
                sum += party.progress();
            }
            if (header.partyAt(view, cx + radius + 1, cy, pane, top) != null) throw new AssertionError("Chart hover escaped disc");
            var metrics = new java.util.LinkedHashMap<String, Object>();
            metrics.put("pane", pane);
            metrics.put("cell", DiplomacyHeader.chartCell(pane, top));
            metrics.put("overlay", box);
            metrics.put("discRadius", radius);
            metrics.put("nativePixels", Math.round(box[2] * client.getWindow().getGuiScale()));
            metrics.put("art", view.art().stream().map(art -> java.util.Map.of("id", art.id(), "bounds", DiplomacyArt.bounds(art, pane, top))).toList());
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("political-chart-" + width + ".json"), new com.google.gson.Gson().toJson(metrics)); }
            catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        });
        context.takeScreenshot("hoi-diplomacy-political-chart-" + width + "-gui2");
        for (var art : view.art()) {
            var point = context.computeOnClient(client -> {
                var screen = (DiplomacyCountryScreen)client.gui.screen();
                int pane = screen.panelWidth(), top = dev.hoi.client.ui.hud.HudMenuBar.height(screen.width);
                var box = DiplomacyArt.bounds(art, pane, top);
                float x = box[0], y = box[1], w = box[2], h = box[3];
                if (!art.equals(DiplomacyArt.at(view, pane, top, x + w / 2, y + h / 2)) || DiplomacyArt.at(view, pane, top, 0, 0) != null) throw new AssertionError("Country art hit region");
                if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + art.icon() + ".png")).isEmpty()) throw new AssertionError("Missing issued country art");
                for (int button : new int[]{0, 1}) screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x + w / 2, y + h / 2, new net.minecraft.client.input.MouseButtonInfo(button, 0)), false);
                if (!requests.isEmpty() || client.gui.screen() != screen) throw new AssertionError("Display-only country art submitted an action");
                var matcher = java.util.regex.Pattern.compile("£GFX_([A-Za-z0-9_]+)").matcher(art.tooltip());
                while (matcher.find()) {
                    String key = matcher.group(1);
                    if (key.startsWith("PRK_cabinet_premier_") || key.startsWith("PRK_eternal_leader_")) {
                        if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/diplomacy/tfrkp/gfx_" + key.toLowerCase(java.util.Locale.ROOT) + ".png")).isEmpty()) throw new AssertionError("Missing portrait token " + key);
                        continue;
                    }
                    var token = dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal(matcher.group()), false);
                    if (token.getString().codePoints().noneMatch(code -> code >= 0xe000 && code <= 0xf8ff)) throw new AssertionError("Missing art tooltip token " + matcher.group());
                }
                return new double[]{(x + w / 2.0) * 2, (y + h / 2.0) * 2};
            });
            context.getInput().setCursorPos(point[0], point[1]);
            context.waitTicks(3);
            context.takeScreenshot("hoi-diplomacy-art-title-" + art.id() + "-" + width + "-gui2");
            context.waitTicks(25);
            var capture = context.takeScreenshot("hoi-diplomacy-art-delayed-" + art.id() + "-" + width + "-gui2");
            var portrait = context.computeOnClient(client -> {
                var font = client.font;
                int guiWidth = client.getWindow().getGuiScaledWidth(), guiHeight = client.getWindow().getGuiScaledHeight();
                int textWidth = dev.hoi.client.ui.tooltip.Tooltips.width(guiWidth, 14), lineHeight = font.lineHeight + 1;
                var rows = DiplomacyArt.rows(font, DiplomacyArt.text(art, true), textWidth);
                int total = rows.stream().mapToInt(row -> DiplomacyArt.height(row, lineHeight)).sum();
                var position = dev.hoi.client.ui.tooltip.Tooltips.position(guiWidth, guiHeight, (int)(point[0] / 2), (int)(point[1] / 2), textWidth, Math.min(total, guiHeight - 20));
                int[] box = null;
                boolean previousBlank = false;
                for (int index = 0, offset = 0; index < rows.size(); offset += DiplomacyArt.height(rows.get(index), lineHeight), index++) {
                    var row = rows.get(index);
                    String plain = plain(row.text());
                    boolean blank = row.image().isEmpty() && plain.isBlank();
                    if (blank && previousBlank) throw new AssertionError("Repeated blank rows in art tooltip " + art.id() + " at " + index);
                    if (plain.contains("\\") || plain.matches(".*-{3,}.*") || plain.matches(".*[\\uE000-\\uF8FF] 동지.*")) throw new AssertionError("Unconverted source text in art tooltip " + art.id() + ": " + plain);
                    previousBlank = blank;
                    if (row.image().isEmpty()) continue;
                    if (box != null || row.caption() == null || plain(row.caption()).isBlank() || index < 2 || !plain(rows.get(index - 1).text()).isBlank() || plain(rows.get(index - 2).text()).isBlank())
                        throw new AssertionError("Portrait must follow its label and carry its name plate: " + art.id());
                    if (art.id().contains("premier") && !plain(row.caption()).equals("김재룡")) throw new AssertionError("Premier name plate " + plain(row.caption()));
                    int captionWidth = font.width(row.caption());
                    box = new int[]{position.x() + row.imageX(), position.y() + offset, position.x() + row.imageX() + (DiplomacyArt.PORTRAIT_WIDTH - captionWidth) / 2, captionWidth};
                }
                if (box == null) throw new AssertionError("Missing portrait row in " + art.id());
                return box;
            });
            try {
                var image = javax.imageio.ImageIO.read(capture.toFile());
                int skin = 0, plate = 0, below = 0;
                for (int y = portrait[1] * 2; y < (portrait[1] + DiplomacyArt.PORTRAIT_HEIGHT) * 2 && y < image.getHeight(); y++) for (int x = portrait[0] * 2; x < (portrait[0] + DiplomacyArt.PORTRAIT_WIDTH) * 2; x++) {
                    int rgb = image.getRGB(x, y), red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                    if (red > green + 8 && green > blue + 5 && red > 70 && green > 45) skin++;
                }
                int captionTop = portrait[1] + Math.round(DiplomacyArt.PORTRAIT_HEIGHT * 224f / 258), captionBottom = portrait[1] + Math.round(DiplomacyArt.PORTRAIT_HEIGHT * 247f / 258);
                for (int y = captionTop * 2; y < captionBottom * 2; y++) for (int x = portrait[2] * 2; x < (portrait[2] + portrait[3]) * 2; x++) if (bright(image.getRGB(x, y))) plate++;
                for (int y = (portrait[1] + DiplomacyArt.PORTRAIT_HEIGHT) * 2; y < (portrait[1] + DiplomacyArt.PORTRAIT_HEIGHT + 8) * 2; y++) for (int x = portrait[0] * 2; x < (portrait[0] + DiplomacyArt.PORTRAIT_WIDTH) * 2; x++) if (bright(image.getRGB(x, y))) below++;
                if (skin < 100 || plate < 20 || below > 0) throw new AssertionError("Portrait name plate did not render in delayed tooltip: " + art.id() + " skin=" + skin + " plate=" + plate + " below=" + below);
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            context.runOnClient(client -> client.gui.screen().mouseScrolled(point[0] / 2, point[1] / 2, 0, -1000));
            context.waitTicks(3);
            context.takeScreenshot("hoi-diplomacy-art-end-" + art.id() + "-" + width + "-gui2");
        }
    }
    private static boolean bright(int rgb) {
        return (rgb >> 16 & 255) >= 200 && (rgb >> 8 & 255) >= 200 && (rgb & 255) >= 200;
    }
    private static String plain(net.minecraft.util.FormattedCharSequence text) {
        var result = new StringBuilder();
        text.accept((index, style, code) -> { result.appendCodePoint(code); return true; });
        return result.toString();
    }
}
