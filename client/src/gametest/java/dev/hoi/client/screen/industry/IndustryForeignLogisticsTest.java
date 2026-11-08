package dev.hoi.client.screen.industry;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenLeaseFixturesTest;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.industry.IndustryPayloads;
import dev.hoi.protocol.industry.IndustryView;
import dev.hoi.protocol.menu.MenuTab;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;

public final class IndustryForeignLogisticsTest {
    private IndustryForeignLogisticsTest() {}
    public static void run(ClientGameTestContext context) {
        if (!ScreenLeaseFixturesTest.available()) { synthetic(context); return; }
        var previous = context.computeOnClient(client -> ClientCampaignHud.visible() ? HudPayloads.State.of(ClientCampaignHud.country(), ClientCampaignHud.hud()) : HudPayloads.State.HIDDEN);
        try {
            for (int width : List.of(2560, 1280)) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> client.options.guiScale().set(2));
                for (String target : List.of("PRK", "JAP")) {
                    var view = ScreenLeaseFixturesTest.fixture("kor-logistics-" + target + ".json", IndustryView.class);
                    if (view.viewing() == null || !view.viewing().country().equals(target)) throw new AssertionError("Source foreign logistics target differs");
                    context.runOnClient(client -> ClientCampaignHud.accept(HudPayloads.State.of(view.country(), view.hud())));
                    var requests = new ArrayList<IndustryPayloads.Request>();
                    context.setScreen(() -> new IndustryScreen(MenuTab.LOGISTICS, view.session(), view, requests::add));
                    context.getInput().setCursorPos(width - 8, width * 9 / 16 - 8);
                    ScreenLeaseFixturesTest.screenshot(context, "kor-logistics-" + target, width);
                    var point = context.computeOnClient(client -> {
                        var screen = (IndustryScreen)client.gui.screen();
                        return new double[]{2 * screen.panelWidth() * 465.0 / 550, 2 * (HudMenuBar.height(screen.width) + screen.panelWidth() * 24.0 / 550)};
                    });
                    context.getInput().setCursorPos(point[0], point[1]);
                    context.waitTicks(15);
                    ScreenLeaseFixturesTest.screenshot(context, "kor-logistics-" + target + "-hover", width);
                    context.runOnClient(client -> {
                        var screen = (IndustryScreen)client.gui.screen();
                        for (var child : screen.children()) if (child instanceof Button button && button.getMessage().getString().endsWith(" 메뉴") && !button.getMessage().getString().equals("군수 메뉴")) throw new AssertionError("Foreign logistics exposed another menu");
                        for (int i = 0; i < 61; i++) screen.tick();
                        if (requests.isEmpty() || requests.stream().anyMatch(request -> request.action() != IndustryPayloads.Action.REFRESH)) throw new AssertionError("Foreign logistics issued a mutation");
                        screen.mouseScrolled(30, screen.height - 50, 0, -100);
                    });
                    context.getInput().setCursorPos(width - 8, width * 9 / 16 - 8);
                    ScreenLeaseFixturesTest.screenshot(context, "kor-logistics-" + target + "-bottom", width);
                }
            }
        } finally { context.runOnClient(client -> ClientCampaignHud.accept(previous)); }
    }
    private static void synthetic(ClientGameTestContext context) {
        var json = new com.google.gson.Gson().toJsonTree(IndustryScreenChecks.tankFixture("production-source-fixture.json")).getAsJsonObject();
        var viewing = new com.google.gson.JsonObject();
        viewing.addProperty("country", "PRK");
        viewing.addProperty("title", "현재 §e조선민주주의인민공화국§r의 군수를 보고 있습니다.");
        json.add("viewing", viewing);
        var view = new com.google.gson.Gson().fromJson(json, IndustryView.class);
        for (int width : List.of(2560, 1280)) {
            context.getInput().resizeWindow(width, width * 9 / 16);
            context.runOnClient(client -> client.options.guiScale().set(2));
            var requests = new ArrayList<IndustryPayloads.Request>();
            context.setScreen(() -> new IndustryScreen(MenuTab.LOGISTICS, view.session(), view, requests::add));
            context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = (IndustryScreen)client.gui.screen();
                for (var child : screen.children()) if (child instanceof Button button && button.getMessage().getString().endsWith(" 메뉴") && !button.getMessage().getString().equals("군수 메뉴")) throw new AssertionError("Foreign logistics exposed another menu");
                for (int i = 0; i < 61; i++) screen.tick();
                if (requests.isEmpty() || requests.stream().anyMatch(request -> request.action() != IndustryPayloads.Action.REFRESH)) throw new AssertionError("Foreign logistics issued a mutation");
                screen.mouseScrolled(30, screen.height - 50, 0, -100);
            });
            context.takeScreenshot("hoi-foreign-logistics-synthetic-" + width + "-gui2");
        }
    }
}
