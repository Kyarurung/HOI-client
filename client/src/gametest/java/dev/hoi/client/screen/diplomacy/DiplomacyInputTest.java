package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.agency.AgencyIntelDomain;
import dev.hoi.protocol.country.CountryView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

final class DiplomacyInputTest {
    private DiplomacyInputTest() {}
    static void run(ClientGameTestContext context, CountryView source) {
        String days = String.join(" ", IntStream.range(0, 79).mapToObj(i -> LocalDate.of(2020, 1, 2).plusDays(i * 14L).toString()).toList());
        var reports = source.reports().stream().map(report -> {
            if (report.domain() != AgencyIntelDomain.CIVILIAN) return report;
            var sections = report.sections().stream().map(section -> {
                if (!section.id().startsWith("graph_")) return section;
                String main = switch (section.id()) {
                    case "graph_industry" -> "industry";
                    case "graph_convoys" -> "convoys_produced";
                    case "graph_bombers" -> "bombers_produced";
                    case "graph_trucks" -> "trucks";
                    default -> "trains";
                };
                var rows = new ArrayList<CountryView.Row>();
                rows.add(new CountryView.Row("days", true, "", "2022-12-29", days, ""));
                rows.add(new CountryView.Row(main, false, "", "178", String.join(" ", IntStream.range(0, 79).mapToObj(i -> String.valueOf(100 + i)).toList()), ""));
                if (main.equals("industry") || main.endsWith("_produced")) rows.add(new CountryView.Row(main.replace("_produced", "") + "_lost", false, "", "18", String.join(" ", IntStream.range(0, 79).mapToObj(i -> String.valueOf(i % 20)).toList()), ""));
                return new CountryView.Section(section.id(), "shown", "", section.tooltip(), rows);
            }).toList();
            return new CountryView.Report(report.domain(), 100, report.entries(), report.tooltip(), sections);
        }).toList();
        var view = new CountryView(source.viewer(), source.hud(), source.target(), source.name(), source.shared(), source.countries(), source.diplomacy(), source.focuses(), reports, source.actions(), source.relations(), new CountryView.Opinion(-100, 50));
        for (int width : List.of(2560, 1280)) {
            context.getInput().resizeWindow(width, width * 9 / 16);
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.setScreen(() -> new DiplomacyCountryScreen(view, request -> { throw new AssertionError("Synthetic input preview sent a mutation"); }));
            context.getInput().setCursorPos(width - 5, 80);
            context.waitTicks(3);
            context.takeScreenshot("hoi-diplomacy-opinion-synthetic-" + width + "-gui2");
            context.runOnClient(client -> {
                var screen = (DiplomacyCountryScreen)client.gui.screen();
                press(screen, "첩보 장부");
                screen.mouseScrolled(40, screen.height - 90, 0, -100);
            });
            for (String period : List.of("3개월", "1년", "3년")) {
                context.runOnClient(client -> press((DiplomacyCountryScreen)client.gui.screen(), period));
                context.waitTicks(3);
                try {
                    var image = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-intel-graph-synthetic-" + period + "-" + width + "-gui2").toFile());
                    int green = 0, red = 0;
                    for (int y = 100; y < image.getHeight() - 80; y++) for (int x = 80; x < 520; x++) {
                        int rgb = image.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                        if (g > r + 60 && g > b + 60) green++;
                        if (r > g + 60 && r > b + 60) red++;
                    }
                    if (green < 100 || red < 100) throw new AssertionError("Issued chart series did not render both original colors: " + green + "/" + red);
                } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
            }
        }
    }
    private static void press(DiplomacyCountryScreen screen, String label) {
        screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getMessage().getString().equals(label)).findFirst().orElseThrow().onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
    }
}
