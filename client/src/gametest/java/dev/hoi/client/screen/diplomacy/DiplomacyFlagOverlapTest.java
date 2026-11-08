package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.country.CountryFlagStack;
import dev.hoi.protocol.country.CountryView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

final class DiplomacyFlagOverlapTest {
    private DiplomacyFlagOverlapTest() {}
    static void run(ClientGameTestContext context, CountryView source) {
        var countries = List.of(new CountryView.Choice("PRK", "조선민주주의인민공화국"), new CountryView.Choice("JAP", "일본"), new CountryView.Choice("KOR", "대한민국"), new CountryView.Choice("SOV", "러시아"), new CountryView.Choice("PRC", "중국"), new CountryView.Choice("USA", "미국"), new CountryView.Choice("GER", "독일"), new CountryView.Choice("FRA", "프랑스"), new CountryView.Choice("ENG", "영국"), new CountryView.Choice("ITA", "이탈리아"), new CountryView.Choice("CAN", "캐나다"), new CountryView.Choice("AUS", "오스트리아 공화국"), new CountryView.Choice("AST", "오스트레일리아"), new CountryView.Choice("BRA", "브라질"), new CountryView.Choice("TUR", "튀르키예"), new CountryView.Choice("POL", "폴란드"), new CountryView.Choice("UKR", "우크라이나"), new CountryView.Choice("SWE", "스웨덴"));
        var embargo = List.of("ALB", "AST", "BEL", "BRS", "BUL", "CAN", "CHI", "CRO", "CZE", "DEN", "ENG", "EST", "FRA", "GER", "GRE", "HOL", "HUN", "ICE", "IRE", "ITA", "JAP", "KOR", "LAT", "LIT", "NOR", "NZL", "PLD", "POR", "ROM", "SLO", "SLV", "SPR", "TUR", "USA");
        for (int width : List.of(2560, 1280)) {
            context.getInput().resizeWindow(width, width * 9 / 16);
            context.runOnClient(client -> client.options.guiScale().set(2));
            for (int count : List.of(3, 5, 18, 33, 34)) {
                var selected = count < 33 ? countries.subList(0, count) : embargo.stream().filter(tag -> count == 34 || !tag.equals("KOR")).map(tag -> new CountryView.Choice(tag, tag)).toList();
                var relation = new CountryView.Relation("무역 금수 조치", "diplomacy/relations/embargo_receiver", selected);
                var view = new CountryView(source.viewer(), source.hud(), source.target(), source.name(), source.shared(), source.countries(), source.diplomacy(), source.focuses(), source.reports(), source.actions(), List.of(relation));
                context.setScreen(() -> new DiplomacyCountryScreen(view));
                context.getInput().setCursorPos(width - 4, width * 9 / 16 - 4);
                context.waitTicks(3);
                int[] row = context.computeOnClient(client -> {
                    var screen = (DiplomacyCountryScreen)client.gui.screen();
                    int pane = screen.panelWidth(), left = 9 + DiplomacyHeader.scaled(40, pane), y = DiplomacyHeader.body(dev.hoi.client.ui.hud.HudMenuBar.height(screen.width), pane);
                    int flagWidth = DiplomacyHeader.scaled(47, pane), stripWidth = DiplomacyHeader.scaled(160, pane);
                    for (int index = 0; index < count; index++) {
                        String path = dev.hoi.client.ui.country.CountryDisplays.texture("country/" + selected.get(index).tag().toLowerCase(Locale.ROOT) + "/flag");
                        if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + path + ".png")).isEmpty()) throw new AssertionError("Missing trial flag " + selected.get(index).tag());
                        int x = CountryFlagStack.left(index, count, left, stripWidth, flagWidth);
                        int visible = CountryFlagStack.visibleWidth(index, count, left, stripWidth, flagWidth);
                        for (int pixel = 0; pixel < visible; pixel++) if (!screen.flagAt(x + pixel, y + DiplomacyHeader.scaled(25, pane)).tag().equals(selected.get(index).tag())) throw new AssertionError("Overlapped flag hit target " + index);
                    }
                    if (CountryFlagStack.left(count - 1, count, left, stripWidth, flagWidth) + flagWidth > left + stripWidth || screen.flagAt(left + stripWidth, y + DiplomacyHeader.scaled(25, pane)) != null) throw new AssertionError("Relation strip boundary");
                    return new int[]{pane, y};
                });
                var capture = context.takeScreenshot("hoi-relation-after-" + count + "-" + width + "-gui2");
                if (count == 5) try {
                    var image = javax.imageio.ImageIO.read(capture.toFile());
                    int pane = row[0], left = 9 + DiplomacyHeader.scaled(40, pane), fw = DiplomacyHeader.scaled(47, pane), strip = DiplomacyHeader.scaled(160, pane);
                    int upper = Math.round(DiplomacyHeader.flagTop(row[1], pane) * 2), fh = DiplomacyHeader.scaled(32, pane) * 2;
                    for (int index = 0; index < count; index++) {
                        int x = CountryFlagStack.left(index, count, left, strip, fw) * 2, visible = CountryFlagStack.visibleWidth(index, count, left, strip, fw) * 2, bright = 0;
                        for (int py = upper + 3; py < upper + fh - 3; py++) for (int px = x + 3; px < x + visible - 2; px++) {
                            int color = image.getRGB(px, py);
                            if (Math.max(color >> 16 & 255, Math.max(color >> 8 & 255, color & 255)) > 150) bright++;
                        }
                        if (bright < 10) throw new AssertionError("Flag pixels missing for " + selected.get(index).tag() + ": " + bright);
                    }
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                for (int index : count == 5 ? List.of(0, 1, 2, 3, 4) : List.of(0, count - 1)) {
                    int left = 9 + DiplomacyHeader.scaled(40, row[0]), flagWidth = DiplomacyHeader.scaled(47, row[0]), stripWidth = DiplomacyHeader.scaled(160, row[0]);
                    int x = CountryFlagStack.left(index, count, left, stripWidth, flagWidth);
                    int visible = CountryFlagStack.visibleWidth(index, count, left, stripWidth, flagWidth);
                    context.getInput().setCursorPos((x + Math.max(0, visible / 2)) * 2, (row[1] + DiplomacyHeader.scaled(25, row[0])) * 2);
                    context.waitTicks(12);
                    context.takeScreenshot("hoi-relation-hover-" + count + "-" + index + "-" + width + "-gui2");
                }
                context.setScreen(() -> new Screen(Component.literal("CMP-97 input6 relation renderer replay")) {
                    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        int pane = row[0], y = row[1];
                        g.fill(0, 0, this.width, this.height, 0xFF202226);
                        UiAssets.draw(g, "diplomacy/relations", 9, y + DiplomacyHeader.scaled(3, pane), DiplomacyHeader.scaled(235, pane), DiplomacyHeader.scaled(44, pane));
                        UiAssets.draw(g, relation.icon(), 9 - DiplomacyHeader.scaled(3, pane), y + DiplomacyHeader.scaled(12, pane), DiplomacyHeader.scaled(46, pane), DiplomacyHeader.scaled(27, pane));
                        int fw = DiplomacyHeader.scaled(47, pane), fh = DiplomacyHeader.scaled(32, pane);
                        for (int j = 0; j < relation.countries().size(); j++) {
                            var country = relation.countries().get(j);
                            int fx = CountryFlagStack.left(j, relation.countries().size(), 9 + DiplomacyHeader.scaled(40, pane), DiplomacyHeader.scaled(160, pane), fw), fy = y + DiplomacyHeader.scaled(5, pane);
                            UiAssets.draw(g, "country/" + country.tag().toLowerCase(Locale.ROOT) + "/flag", fx, fy, fw, fh);
                        }
                    }
                });
                context.waitTicks(3);
                context.takeScreenshot("hoi-relation-input6-replay-" + count + "-" + width + "-gui2");
                try {
                    java.nio.file.Files.writeString(java.nio.file.Path.of("relation-row-" + count + "-" + width + ".json"), "{\"pane\":" + row[0] + ",\"y\":" + row[1] + ",\"flags\":" + count + ",\"allExposedPixelsChecked\":true}");
                } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            }
        }
    }
}
