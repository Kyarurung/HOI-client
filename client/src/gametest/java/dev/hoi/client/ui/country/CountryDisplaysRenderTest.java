package dev.hoi.client.ui.country;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.diplomacy.DiplomacyCountryScreen;
import dev.hoi.protocol.country.CountryDisplay;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.protocol.menu.MenuView;
import dev.hoi.protocol.country.CountryView;
import dev.hoi.protocol.agency.AgencyIntelDomain;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiStoryAssets;
import dev.hoi.client.ui.hud.HudMenuBar;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CountryDisplaysRenderTest {
    private CountryDisplaysRenderTest() {}
    public static void run(ClientGameTestContext context, MenuView menu) {
        int scale = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            context.getInput().resizeWindow(2560, 1440);
            context.runOnClient(client -> {
                client.options.guiScale().set(2);
                ClientCampaignHud.accept(HudPayloads.State.of("KOR", menu.hud()));
                CountryDisplays.accept(new CountryDisplay.Table(List.of(new CountryDisplay.Entry("KOR", "대한제국", "KOR_empire"))));
                if (!HudMenuBar.flagTexture("KOR").equals(UiStoryAssets.flag("KOR", "KOR_empire"))) throw new AssertionError("Use the exact server flag key");
                if (HudMenuBar.flagTexture("KOR").equals("country/kor/flag")) throw new AssertionError("Cosmetic sample must exist in the external pack");
                if (UiAssets.imageHeight("GFX_KOR_hog_ha_tae_kyung", 20) <= 0) throw new AssertionError("Raw portrait must resolve to its original external texture");
                if (UiAssets.imageHeight("politics/art/gfx_idea_prc_plan_dynamic", 20) <= 0) throw new AssertionError("Server normalized dynamic modifier icon must resolve");
                for(String key:List.of("gfx_idea_prk_economic_sovereignty","gfx_idea_prk_military_preparedness","gfx_idea_prc_made_in_china_idea"))if(UiAssets.imageHeight("politics/art/"+key,20)<=0)throw new AssertionError("Starting national spirit must resolve its original image: "+key);
                client.gui.setScreen(new ScreenMenu(assetMenu(menu), MenuTab.POLITICS, () -> {}));
            });
            context.waitTicks(4);
            context.takeScreenshot("hoi-country-display-cosmetic-synthetic-gui2");
            context.getInput().resizeWindow(1280, 720);
            context.waitTicks(3);
            context.takeScreenshot("hoi-country-display-original-assets-synthetic-compact-gui2");
            flagHover(context, "cosmetic");
            context.getInput().resizeWindow(2560, 1440);
            context.runOnClient(client -> client.gui.setScreen(new DiplomacyCountryScreen(new CountryView("KOR", menu.hud(), "KOR", menu.countryName(), true,
                    List.of(), List.of(), List.of(), java.util.Arrays.stream(AgencyIntelDomain.values()).map(domain -> new CountryView.Report(domain, 0, List.of())).toList()))));
            context.waitTicks(3);
            context.takeScreenshot("hoi-country-display-name-cosmetic-synthetic-gui2");
            context.runOnClient(client -> {
                UiAssets.clear();
                if (!CountryDisplays.name("KOR", "").equals("대한제국")) throw new AssertionError("Asset reload must retain server display data");
                CountryDisplays.accept(new CountryDisplay.Table(List.of(new CountryDisplay.Entry("KOR", "대한민국", "KOR"))));
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-country-display-restored-synthetic-gui2");
            context.getInput().resizeWindow(1280, 720);
            context.waitTicks(3);
            context.takeScreenshot("hoi-country-display-restored-synthetic-compact-gui2");
            flagHover(context, "restored");
            context.runOnClient(client -> {
                CountryDisplays.accept(new CountryDisplay.Table(List.of(new CountryDisplay.Entry("KOR", "대한민국", "unavailable_source_key"))));
                if (!HudMenuBar.flagTexture("KOR").equals("country/kor/flag")) throw new AssertionError("Missing original falls back to the stable tag");
                CountryDisplays.clear();
                if (!CountryDisplays.name("KOR", "재접속 스냅샷").equals("재접속 스냅샷")) throw new AssertionError("Disconnect drops the old table");
            });
        } finally {
            context.runOnClient(client -> { CountryDisplays.clear(); client.gui.setScreen(null); client.options.guiScale().set(scale); });
            context.getInput().resizeWindow(1600, 1000);
            context.waitTicks(3);
        }
    }
    private static void flagHover(ClientGameTestContext context, String state) {
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.getInput().setCursorPos(32, 30);
            context.waitTicks(3);
            context.takeScreenshot("hoi-country-flag-name-" + state + "-" + size[0] + "-gui2");
        }
        context.getInput().setCursorPos(600, 350);
    }
    private static MenuView assetMenu(MenuView menu) {
        var pages = menu.pages().stream().map(page -> page.tab() != MenuTab.POLITICS ? page :
                new MenuView.Page(page.tab(), page.sections().stream().map(section ->
                        new MenuView.Section(section.title(), section.icon(), section.title().equals("국가 정신") ?
                                List.of(new MenuView.Entry("합성 원본 아이콘", "", "합성 렌더 입력: 실제 수정치 효과 검증이 아님.", -1,
                                        "politics/art/gfx_idea_prc_plan_dynamic"),
                                        new MenuView.Entry("자립경제","","원본 그림 바인딩 검증용 합성 입력",-1,"politics/art/gfx_idea_prk_economic_sovereignty"),
                                        new MenuView.Entry("선군정치","","원본 그림 바인딩 검증용 합성 입력",-1,"politics/art/gfx_idea_prk_military_preparedness"),
                                        new MenuView.Entry("중국 특색 사회주의","","원본 그림 바인딩 검증용 합성 입력",-1,"politics/art/gfx_idea_prc_made_in_china_idea")) : section.entries().stream().map(entry ->
                                        entry.name().equals("지도자") ? new MenuView.Entry(entry.name(), "합성 원본 초상",
                                                "합성 렌더 입력: 실제 인물 역할 검증이 아님.", -1, UiStoryAssets.asset("GFX_KOR_hog_ha_tae_kyung")) : entry).toList())).toList())).toList();
        return new MenuView(menu.country(), menu.countryName(), menu.date(), menu.speed(), pages, menu.hud(), menu.manufacturers(), menu.balance());
    }
}
