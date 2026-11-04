package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.country.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class DiplomacyMonthlyLeaseTest {
    private DiplomacyMonthlyLeaseTest() {}
    public static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        var requests = new ArrayList<CountryPayloads.Action>();
        String session = UUID.randomUUID().toString();
        var reports = Arrays.stream(AgencyIntelDomain.values()).map(d -> new CountryView.Report(d, 0, List.of())).toList();
        var once = new CountryView.Action("lend:fixture", "무기대여 시작 · 보병 장비", "일회성", true,
                new CountryView.AmountInput("보병 장비", 2));
        var monthly = new CountryView.Action("monthly_lend:fixture", "매월 · 무기대여 시작 · 보병 장비",
                "매월 보낼 장비의 수량을 선택하십시오.\n보병 장비\n저장량: 2\n수신국 수락 필요\n첫 배송: 수락 후 30일. 이후 실제 배송일로부터 30일마다 같은 수량을 보냅니다.", true,
                new CountryView.AmountInput("보병 장비", 2));
        var view = new CountryView("KOR", CountryHud.UNKNOWN, "PRK", "조선민주주의인민공화국", false,
                List.of(), List.of(), List.of(), reports, new CountryView.Actions(session, 18, "", List.of(once, monthly)));
        context.setScreen(() -> new DiplomacyCountryScreen(view, requests::add));
        context.waitTicks(3);
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            press(button(screen, monthly.name()));
            input(screen).setValue("12");
            if (!button(screen, "확인").active || !requests.isEmpty()) throw new AssertionError("Monthly input incorrectly caps stock or sends before confirmation");
        });
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-monthly-lend-lease-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            press(button(screen, "취소"));
            press(button(screen, monthly.name()));
            input(screen).setValue("12");
            press(button(screen, "확인"));
            press(button(screen, "확인"));
            if (requests.size() != 1 || !requests.getFirst().id().equals(monthly.id()) || requests.getFirst().amount() != 12)
                throw new AssertionError("Monthly confirmation lost issued mode or duplicated request");
        });
        context.getInput().resizeWindow(1600, 1000);
        context.waitTicks(3);
        context.setScreen(() -> new DiplomacyCountryScreen(view, requests::add));
        context.waitTicks(3);
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            press(button(screen, monthly.name()));
            for (var child : screen.children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
                if (widget.getX() < 0 || widget.getY() < 0 || widget.getRight() > screen.width || widget.getBottom() > screen.height)
                    throw new AssertionError("Monthly form exceeds compact GUI2 bounds");
        });
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-monthly-lend-lease-compact-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            press(button(screen, "취소"));
            if (requests.size() != 1) throw new AssertionError("Compact cancellation sends an extra request");
        });
    }
    private static EditBox input(DiplomacyCountryScreen screen) {
        return (EditBox)screen.children().stream().filter(widget -> widget instanceof EditBox).findFirst().orElseThrow();
    }
    private static Button button(DiplomacyCountryScreen screen, String name) {
        return (Button)screen.children().stream().filter(widget -> widget instanceof Button button && button.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }
    private static void press(Button button) {
        button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
    }
}
