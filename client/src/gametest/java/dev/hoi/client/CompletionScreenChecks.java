package dev.hoi.client;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

final class CompletionScreenChecks {
    static void run(ClientGameTestContext context) {
        var sent = new ArrayList<String>();
        for (int[] size : new int[][]{{1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            for (String style : List.of("research_complete", "focus_complete")) {
                String art = style.equals("research_complete") ? "category/infantry" : "focus/tfr/ast_war_beyond_the_seas";
                var view = new DialogView(UUID.randomUUID().toString(), 3, DialogView.Kind.DIPLOMACY, "완료된 연구·중점 이름", "",
                        "완료 효과와 해금 항목을 서버가 제공합니다.\n".repeat(22), "", art, "", List.of(), List.of(new DialogView.Choice("ack", "확인")), style);
                context.setScreen(() -> new CompletionScreen(view, (v, choice) -> sent.add(v.token() + "/" + v.revision() + "/" + choice)));
                context.waitTicks(3); context.takeScreenshot("hoi-" + style + "-" + size[0]);
                context.runOnClient(c -> {
                    var s = (CompletionScreen)c.gui.screen();
                    check(Math.abs(s.panelLeft() * 2 + s.panelWidth() - s.width) <= 1, "Completion stays centered");
                    check(c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + art + ".png")).isPresent(), "Original completion art exists");
                    for (var child : s.children()) if (child instanceof Button b)
                        check(b.getY() >= 0 && b.getBottom() <= s.height && b.getRight() <= s.width, "Buttons fit viewport");
                    press(s, "세부 사항");
                });
                context.waitTicks(3);
                context.runOnClient(c -> {
                    var s = (CompletionScreen)c.gui.screen(); s.mouseScrolled(s.width / 2.0, s.height / 2.0, 0, -100);
                    check(s.scrollOffset() > 0, "Long details scroll"); press(s, "확인");
                    check(sent.getLast().equals(view.token() + "/3/ack"), "Acknowledgement uses issued session without applying rewards");
                    for (int key : new int[]{GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER}) {
                        int before = sent.size();
                        s.keyPressed(new KeyEvent(key, 0, 0));
                        check(sent.size() == before + 1 && sent.getLast().equals(view.token() + "/3/ack"), "Enter confirms exactly once while details are focused");
                    }
                });
                context.waitTicks(2); context.takeScreenshot("hoi-" + style + "-details-" + size[0]);
                context.setScreen(() -> null);
                context.runOnClient(c -> {
                    DialogClient.receive(DialogProtocol.Show.of(view, true));
                    check(c.gui.screen() instanceof CompletionScreen, "Server completion automatically opens compact popup");
                    DialogClient.close(view.token()); DialogClient.receive(DialogProtocol.Show.of(view.issued(view.token(), 4), false));
                    check(c.gui.screen() == null, "Stale refresh cannot reopen completion");
                });
            }
        }
        hud(context);
        context.setScreen(() -> null);
        context.runOnClient(c -> {
            check(java.util.Arrays.stream(c.options.keyMappings).noneMatch(k -> k.getName().equals("key.hoi.research")), "R research binding is removed");
            while (c.options.keySwapOffhand.consumeClick()) { }
            try {
                var method = net.minecraft.client.KeyboardHandler.class.getDeclaredMethod("keyPress", long.class, int.class, KeyEvent.class);
                method.setAccessible(true);
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_PRESS, new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT));
                check(!c.options.keySwapOffhand.consumeClick(), "Shift+F is intercepted before vanilla queues an offhand swap");
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_RELEASE, new KeyEvent(GLFW.GLFW_KEY_F, 0, 0));
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_PRESS, new KeyEvent(GLFW.GLFW_KEY_F, 0, 0));
                check(c.options.keySwapOffhand.consumeClick(), "Plain F still queues vanilla offhand swap");
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_RELEASE, new KeyEvent(GLFW.GLFW_KEY_F, 0, 0));
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
    private static void hud(ClientGameTestContext context) {
        var rows = new ArrayList<HudDetail.Row>();
        for (int i = 0; i < 32; i++) rows.add(new HudDetail.Row("현재 적용되는 국가 효과 " + i, "+2.5%", HudDetail.Tone.GOOD));
        rows.add(new HudDetail.Row("마지막 설명", "—", HudDetail.Tone.MUTED));
        var hud = new CountryHud(null, null, null, null, null, null, false, null,
                CountryHud.NationalIndicators.UNKNOWN, Map.of("political_power", new HudDetail(rows)));
        var pages = MenuTab.ORDER.stream().map(t -> new MenuView.Page(t, List.of(new MenuView.Section(t.label(), t.id(), List.of())))).toList();
        context.setScreen(() -> new HoiMenuScreen(new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages, hud)));
        context.getInput().setCursorPos(135, 14); context.waitTicks(3);
        context.runOnClient(c -> {
            var lines = HudTooltip.body(c.font, c.gui.screen().width, hud, HoiMenuBar.indicators(hud).getFirst());
            check(lines.size() > HudTooltip.visibleRows(c.gui.screen().height, c.font), "Long HUD gets a scroll window");
            for (var line : lines) check(c.font.width(line) <= 310, "HUD lines wrap within viewport");
        });
        context.takeScreenshot("hoi-hud-detail-compact");
        context.getInput().holdKey(o -> o.keyShift); context.waitTicks(2);
        context.runOnClient(c -> check(HudTooltip.scroll(-100), "Shift wheel scrolls the active HUD tooltip"));
        context.waitTicks(2); context.takeScreenshot("hoi-hud-detail-compact-scrolled");
        context.getInput().releaseKey(o -> o.keyShift);
    }
    private static void press(CompletionScreen s, String name) {
        ((Button)s.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow())
                .onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
