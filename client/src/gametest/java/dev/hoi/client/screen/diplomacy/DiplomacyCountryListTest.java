package dev.hoi.client.screen.diplomacy;

import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.ProtocolTooltip;
import dev.hoi.protocol.agency.AgencyIntelDomain;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.country.CountryView;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class DiplomacyCountryListTest {
    private static final String JAPAN_OURS = ProtocolTooltip.delayed("§f[JAP.GetFlag]§e일본국§f에 대한 §e한국§f의 평판: §c-75§f§f", "\n--------------\n", "세력 배신자: §c-75§f");
    private DiplomacyCountryListTest() {}
    static CountryView fixture() {
        var reports = Arrays.stream(AgencyIntelDomain.values()).map(d -> new CountryView.Report(d, 0, List.of())).toList();
        var filters = new ArrayList<CountryView.Filter>();
        String[][] continents = {{"europe", "유럽"}, {"north_america", "북아메리카"}, {"south_america", "남아메리카"}, {"australia", "오세아니아"}, {"africa", "아프리카"}, {"asia", "아시아"}, {"middle_east", "중동"}};
        for (var continent : continents) filters.add(new CountryView.Filter("continent:" + continent[0], "continent", continent[1]));
        String[][] ideologies = {{"totalitarian_socialist", "전체주의적 사회주의"}, {"communist", "권위사회주의"}, {"libertarian_socialist", "자유지상적 사회주의"}, {"social_democrat", "사회민주주의"},
                {"social_liberal", "사회자유주의"}, {"market_liberal", "시장자유주의"}, {"conservative", "보수주의"}, {"authoritarian_democrat", "권위민주주의"}, {"nationalist", "전제정"}, {"fascist", "파시즘"}, {"national_socialist", "국가사회주의"}};
        for (var ideology : ideologies) filters.add(new CountryView.Filter("ideology:" + ideology[0], "ideology", ideology[1]));
        filters.add(new CountryView.Filter("major", "faction", "강대국"));
        filters.add(new CountryView.Filter("faction:PDTO", "faction", "태평양방위조약기구"));
        var texts = Map.of("COUNTRYLIST_LABEL", "국가", "DIPLOMACY_SORT_ALPHABETICAL", "이름순으로 정렬", "DIPLOMACY_SORT_OPINION_OURS", "상대를 향한 의견순 정렬", "DIPLOMACY_SORT_OPINION_THEIRS", "우리를 향한 의견순 정렬");
        var rows = List.of(
                new CountryView.Choice("KOR", "대한민국", new CountryView.Listing("§a클릭§f하여 [KOR.GetFlag]§e대한민국§f 측과의 외교 열기", null, null, "", "", List.of("continent:asia", "ideology:conservative", "major"), List.of())),
                new CountryView.Choice("AST", "오스트레일리아 연방", new CountryView.Listing("§a클릭§f하여 [AST.GetFlag]§e오스트레일리아 연방§f 측과의 외교 열기", 5, 3, "", "", List.of("continent:australia", "ideology:social_liberal", "faction:PDTO"), List.of())),
                new CountryView.Choice("JAP", "일본국", new CountryView.Listing("§a클릭§f하여 [JAP.GetFlag]§e일본국§f 측과의 외교 열기", -75, 10, JAPAN_OURS, "", List.of("continent:asia", "ideology:conservative", "major", "faction:PDTO"),
                        List.of(new CountryView.Mark("diplomacy/relations/guarantee", "§e대한민국§f 측이 독립을 보장하는 국가: §e일본국§f"),
                                new CountryView.Mark("diplomacy/relations/military_access", "§e대한민국§f 측이 군사 통행권을 준 국가: §e일본국§f")))),
                new CountryView.Choice("PRK", "조선민주주의인민공화국", new CountryView.Listing("§a클릭§f하여 [PRK.GetFlag]§e조선민주주의인민공화국§f 측과의 외교 열기", 0, -100, "", "", List.of("continent:asia", "ideology:totalitarian_socialist"),
                        List.of(new CountryView.Mark("diplomacy/relations/embargo", "§e대한민국§f 측이 금수 조치한 국가: §e조선민주주의인민공화국§f")))));
        return new CountryView("KOR", CountryHud.UNKNOWN, "JAP", "일본국", false, rows, List.of(), List.of(), reports, null, List.of(), null, List.of(), "", new CountryView.CountryList(filters, texts));
    }
    public static void run(ClientGameTestContext context) {
        var view = fixture();
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.setScreen(() -> new DiplomacyCountryScreen(view, action -> {}));
            context.waitTicks(2);
            context.runOnClient(client -> {
                var screen = (DiplomacyCountryScreen) client.gui.screen();
                button(screen, "국가 목록 열기").onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
            });
            context.getInput().setCursorPos(size[0] - 4, size[1] / 2);
            context.waitTicks(3);
            context.takeScreenshot("hoi-diplomacy-list-fixture-" + size[0] + "x" + size[1] + "-gui2");
            context.runOnClient(client -> {
                var screen = (DiplomacyCountryScreen) client.gui.screen();
                if (!screen.choosing()) throw new AssertionError("The country list button opens the list");
                for (String hidden : List.of("국가 목록 열기", "외교", "첩보 장부"))
                    if (screen.children().stream().anyMatch(w -> w instanceof Button b && b.getMessage().getString().equals(hidden))) throw new AssertionError("List mode hides " + hidden);
                expect(screen, List.of("KOR", "AST", "JAP", "PRK"), "Rows follow Korean display names");
                var geometry = Geometry.of(screen);
                click(screen, geometry.cell(1, 1, 0));
                expect(screen, List.of("KOR", "JAP", "PRK"), "The Asia filter keeps only Asian capitals");
                click(screen, geometry.cell(2, 3, 0));
                expect(screen, List.of("KOR", "JAP"), "Filters of different groups must all match");
                click(screen, geometry.cell(1, 1, 0));
                click(screen, geometry.cell(2, 3, 0));
                expect(screen, List.of("KOR", "AST", "JAP", "PRK"), "Clearing the filters restores every row");
                double[] filters = geometry.cell(0, 0, 0);
                screen.mouseScrolled(filters[0], filters[1], 0, -10);
                int scrolled = screen.list().filterScroll();
                if (scrolled <= 0) throw new AssertionError("The filter window scrolls to its hidden faction row");
                click(screen, geometry.cell(1, 5, scrolled));
                expect(screen, List.of("AST", "JAP"), "The faction filter keeps its members");
                click(screen, geometry.cell(1, 5, scrolled));
                click(screen, geometry.flag(true));
                expect(screen, List.of("AST", "PRK", "JAP", "KOR"), "Our opinion sorts from the highest value with our own row last");
                click(screen, geometry.flag(true));
                expect(screen, List.of("KOR", "JAP", "PRK", "AST"), "A second click reverses the opinion order");
                click(screen, geometry.flag(false));
                expect(screen, List.of("JAP", "AST", "PRK", "KOR"), "Their opinion sorts from the highest value");
                click(screen, geometry.name());
                expect(screen, List.of("KOR", "AST", "JAP", "PRK"), "The name header restores alphabetical order");
                var list = screen.list();
                double[] box = geometry.opinion(2, true);
                if (!list.tooltip(view, box[0], box[1]).equals(JAPAN_OURS)) throw new AssertionError("Our opinion box shows the original opinion tooltip");
                double[] mark = geometry.mark(2, 0, 2);
                if (!list.tooltip(view, mark[0], mark[1]).contains("독립을 보장하는 국가")) throw new AssertionError("Relation icons show their original relation text");
                double[] row = geometry.row(1);
                if (!list.tooltip(view, row[0], row[1]).startsWith("§a클릭§f하여 [AST.GetFlag]")) throw new AssertionError("Rows show the original diplomacy opening tooltip");
                double[] header = geometry.flag(false);
                if (!list.tooltip(view, header[0], header[1]).equals("우리를 향한 의견순 정렬")) throw new AssertionError("Header flags show the original sort tooltips");
                if (!DiplomacyCountryList.fit(client.font, "전체주의적 사회주의", 50, 1).endsWith("...")) throw new AssertionError("Long filter labels end with the original ellipsis");
                if (DiplomacyCountryList.nameLines(client.font, "그레이트브리튼 및 북아일랜드 연합왕국", 87, 1, 2).size() != 2) throw new AssertionError("Long names wrap to two lines");
            });
            if (size[0] == 2560) {
                double[] box = context.computeOnClient(client -> Geometry.of((DiplomacyCountryScreen) client.gui.screen()).opinion(2, true));
                context.getInput().setCursorPos(box[0] * 2, box[1] * 2);
                for (int frame = 0; frame < 12; frame++) {
                    context.waitTicks(1);
                    pause(100);
                }
                context.waitTicks(1);
                context.runOnClient(client -> {
                    if (!dev.hoi.client.ui.tooltip.TooltipDelay.visible(JAPAN_OURS).equals(dev.hoi.client.ui.tooltip.TooltipDelay.full(JAPAN_OURS)))
                        throw new AssertionError("After one second of hover the opinion tooltip shows its breakdown");
                });
                context.takeScreenshot("hoi-diplomacy-list-opinion-tooltip-" + size[0] + "-gui2");
            }
            context.runOnClient(client -> {
                var screen = (DiplomacyCountryScreen) client.gui.screen();
                click(screen, Geometry.of(screen).row(2));
                if (screen.choosing() || !screen.target().equals("JAP")) throw new AssertionError("Clicking a row opens that country's diplomacy view");
            });
        }
        context.setScreen(() -> null);
    }
    private record Geometry(float s, float top, float header, float rows) {
        static Geometry of(DiplomacyCountryScreen screen) {
            float s = screen.panelWidth() / 550f;
            int top = HudMenuBar.height(screen.width), header = Math.round(top + 213 * s);
            return new Geometry(s, top, header, Math.round(Math.round(header + 30 * s) + 8 * s));
        }
        double[] cell(int column, int row, int scroll) { return new double[]{(108 + column * 105 + 50) * s, Math.round(top + 54 * s) - scroll + (1 + row * 35 + 14.5) * s}; }
        double[] flag(boolean ours) { return new double[]{(ours ? 258 : 320) * s, header + 12.5 * s}; }
        double[] name() { return new double[]{114 * s, header + 16.5 * s}; }
        double[] row(int index) { return new double[]{150 * s, rows + 5 * s + index * 45 * s + 22 * s}; }
        double[] opinion(int index, boolean ours) { return new double[]{(12 + (ours ? 238 : 300) + 18) * s, rows + 5 * s + index * 45 * s + 23.5 * s}; }
        double[] mark(int index, int mark, int count) {
            float step = count <= 1 ? 0 : Math.min(35, (80 - 34) / (float) (count - 1));
            return new double[]{(12 + 420 + mark * step + 17) * s, rows + 5 * s + index * 45 * s + 25 * s};
        }
    }
    private static void click(DiplomacyCountryScreen screen, double[] at) {
        screen.mouseClicked(new MouseButtonEvent(at[0], at[1], new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }
    private static void expect(DiplomacyCountryScreen screen, List<String> rows, String message) {
        if (!screen.listed().equals(rows)) throw new AssertionError(message + ": " + screen.listed());
    }
    private static Button button(DiplomacyCountryScreen screen, String name) {
        return (Button) screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }
    private static void pause(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
}
