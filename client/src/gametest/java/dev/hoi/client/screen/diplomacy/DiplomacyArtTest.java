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
            try {
                var image = javax.imageio.ImageIO.read(capture.toFile());
                int tooltipX = Math.clamp((int)(point[0] / 2) + 11, 7, width / 2 - dev.hoi.client.ui.tooltip.Tooltips.width(width / 2, 14) - 7) * 2;
                int skin = 0;
                for (int y = 120; y < Math.min(440, image.getHeight()); y++) for (int x = tooltipX; x < Math.min(tooltipX + 190, image.getWidth()); x++) {
                    int rgb = image.getRGB(x, y), red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                    if (red > green + 8 && green > blue + 5 && red > 70 && green > 45) skin++;
                }
                if (skin < 100) throw new AssertionError("Portrait did not render in delayed tooltip: " + art.id() + " " + skin);
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            context.runOnClient(client -> client.gui.screen().mouseScrolled(point[0] / 2, point[1] / 2, 0, -1000));
            context.waitTicks(3);
            context.takeScreenshot("hoi-diplomacy-art-end-" + art.id() + "-" + width + "-gui2");
        }
    }
}
