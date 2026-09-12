package dev.hoi.client.screen;

import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.HudTooltip;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class CompletionScreenChecks {
    public static void run(ClientGameTestContext context) {
        var sent = new ArrayList<String>();
        for (int[] size : new int[][]{{1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            for (String style : List.of("research_complete", "focus_complete")) {
                String art = style.equals("research_complete") ? "category/infantry" : "focus/tfr/ast_war_beyond_the_seas";
                var view = new DialogView(UUID.randomUUID().toString(), 3, DialogView.Kind.DIPLOMACY, "완료된 연구·중점 이름", "",
                        "표시하면 안 되는 설명 본문\n".repeat(22), "", art, "", List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), style);
                context.setScreen(() -> new CompletionScreen(view, (v, choice) -> sent.add(v.token() + "/" + v.revision() + "/" + choice)));
                context.waitTicks(3); context.takeScreenshot("hoi-" + style + "-" + size[0]);
                context.runOnClient(c -> {
                    var s = (CompletionScreen)c.gui.screen();
                    check(Math.abs(s.panelLeft() * 2 + s.panelWidth() - s.width) <= 1, "Completion stays centered");
                    check(c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + art + ".png")).isPresent(), "Original completion art exists");
                    for (var child : s.children()) if (child instanceof Button b)
                        check(b.getY() >= 0 && b.getBottom() <= s.height && b.getRight() <= s.width, "Buttons fit viewport");
                    press(s, "세부 사항");
                    check(sent.getLast().equals(view.token() + "/3/details"), "Details use an issued choice instead of expanding the body");
                });
                context.waitTicks(3);
                context.runOnClient(c -> {
                    var s = (CompletionScreen)c.gui.screen(); s.mouseScrolled(s.width / 2.0, s.height / 2.0, 0, -100);
                    check(s.scrollOffset() == 0, "Description body never enters the compact popup"); press(s, "확인");
                    check(sent.getLast().equals(view.token() + "/3/ack"), "Acknowledgement uses issued session without applying rewards");
                    for (int key : new int[]{GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER}) {
                        int before = sent.size();
                        s.keyPressed(new KeyEvent(key, 0, 0));
                        check(sent.size() == before + 1 && sent.getLast().equals(view.token() + "/3/ack"), "Enter confirms exactly once while details are focused");
                    }
                });
                context.waitTicks(2); context.takeScreenshot("hoi-" + style + "-details-" + size[0]);
                context.setScreen(() -> new net.minecraft.client.gui.screens.PauseScreen(true));
                context.runOnClient(c -> {
                    DialogClient.receive(DialogProtocol.Show.of(view, true));
                    check(c.gui.screen() instanceof CompletionScreen, "Completion opens immediately even while another in-game screen is open");
                    DialogClient.close(view.token()); DialogClient.receive(DialogProtocol.Show.of(view.issued(view.token(), 4), false));
                    check(c.gui.screen() == null, "Stale refresh cannot reopen completion");
                    DialogClient.details(new DialogProtocol.Details(view.token(), "focus/hoi:focus/political_effort"));
                    check(c.gui.screen() == null, "Revoked completion cannot navigate to private details");
                    DialogClient.receive(DialogProtocol.Show.of(view, true));
                    DialogClient.details(new DialogProtocol.Details(view.token(), "focus/hoi:focus/political_effort"));
                    check(c.gui.screen() instanceof HoiMenuScreen, "Confirmed focus details open the country menu");
                    var menu = (HoiMenuScreen)c.gui.screen();
                    var entry = new MenuView.Entry("정치적 노력", "완료", "hoi:focus/political_effort\n완료한 중점");
                    var pages = MenuTab.ORDER.stream().map(t -> new MenuView.Page(t, List.of(new MenuView.Section(
                            t == MenuTab.POLITICS ? "국가 중점" : t.label(), "research", t == MenuTab.POLITICS ? List.of(entry) : List.of())))).toList();
                    menu.update(new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages));
                    check(!menu.allowsMovement(), "Completed focus entry opens directly in its detail panel");
                });
            }
        }
        backdrop(context);
        hud(context);
        context.setScreen(() -> null);
        context.runOnClient(c -> {
            check(java.util.Arrays.stream(c.options.keyMappings).noneMatch(k -> k.getName().equals("key.hoi.research")), "R research binding is removed");
            while (c.options.keySwapOffhand.consumeClick()) { }
            try {
                var method = net.minecraft.client.KeyboardHandler.class.getDeclaredMethod("keyPress", long.class, int.class, KeyEvent.class);
                method.setAccessible(true);
                check(!net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(MenuProtocol.Refresh.TYPE), "Fixture server has no HOI menu receiver");
                var messagesField = net.minecraft.client.gui.components.ChatComponent.class.getDeclaredField("allMessages");
                messagesField.setAccessible(true);
                var messages = (java.util.List<?>) messagesField.get(c.gui.hud.getChat());
                int messageCount = messages.size();
                dev.hoi.client.CampaignHud.accept(HudProtocol.State.HIDDEN);
                dev.hoi.client.HoiClient.openMenu(MenuTab.POLITICS);
                check(c.gui.screen() == null, "A menu is never constructed before campaign participation is authorized");
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_PRESS, new KeyEvent(GLFW.GLFW_KEY_F, 0, GLFW.GLFW_MOD_SHIFT));
                check(!c.options.keySwapOffhand.consumeClick(), "Shift+F is intercepted before vanilla queues an offhand swap");
                check(messages.size() == messageCount && c.gui.screen() == null, "Shift+F on an unsupported server must not print chat or open a menu");
                for(int i=0;i<5;i++)method.invoke(c.keyboardHandler,c.getWindow().handle(),GLFW.GLFW_REPEAT,new KeyEvent(GLFW.GLFW_KEY_F,0,GLFW.GLFW_MOD_SHIFT));
                for(int i=0;i<5;i++)method.invoke(c.keyboardHandler,c.getWindow().handle(),GLFW.GLFW_REPEAT,new KeyEvent(GLFW.GLFW_KEY_F,0,0));
                check(!c.options.keySwapOffhand.consumeClick(), "Releasing Shift while F still repeats cannot leak an offhand action");
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_RELEASE, new KeyEvent(GLFW.GLFW_KEY_F, 0, 0));
                check(c.gui.screen()==null&&!c.options.keySwapOffhand.consumeClick(), "Holding Shift+F then releasing Shift first never opens UI or leaks a swap");
                method.invoke(c.keyboardHandler,c.getWindow().handle(),GLFW.GLFW_PRESS,new KeyEvent(GLFW.GLFW_KEY_F,0,GLFW.GLFW_MOD_SHIFT));
                for(int i=0;i<20;i++) {
                    method.invoke(c.keyboardHandler,c.getWindow().handle(),GLFW.GLFW_REPEAT,new KeyEvent(GLFW.GLFW_KEY_F,0,GLFW.GLFW_MOD_SHIFT));
                    check(c.gui.screen()==null, "Held shortcut never constructs a transient menu");
                }
                method.invoke(c.keyboardHandler,c.getWindow().handle(),GLFW.GLFW_RELEASE,new KeyEvent(GLFW.GLFW_KEY_F,0,GLFW.GLFW_MOD_SHIFT));
                check(c.gui.screen()==null&&!c.options.keySwapOffhand.consumeClick(), "Long press followed by F release while Shift remains held never opens UI");
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_PRESS, new KeyEvent(GLFW.GLFW_KEY_F, 0, 0));
                check(c.options.keySwapOffhand.consumeClick(), "Plain F still queues vanilla offhand swap");
                method.invoke(c.keyboardHandler, c.getWindow().handle(), GLFW.GLFW_RELEASE, new KeyEvent(GLFW.GLFW_KEY_F, 0, 0));
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
    private static void backdrop(ClientGameTestContext context) {
        var requests = new ArrayList<ResearchProtocol.Request>();
        var research = new ResearchView("completion-parent", 1, "KOR", "대한민국", 0, "2020-01-01", "PAUSED",
                List.of(new ResearchView.Slot(0, "", 0)), List.of(), "");
        var done = new DialogView(java.util.UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "완료 알림", "", "", "", "menu/research", "",
                List.of(), List.of(new DialogView.Choice("ack", "확인")), "research_complete");
        context.setScreen(() -> new ResearchScreen(research, requests::add));
        context.runOnClient(c -> {
            var parent = c.gui.screen();
            DialogClient.receive(DialogProtocol.Show.of(done, true));
            check(c.gui.screen() instanceof CompletionScreen overlay && overlay.backdrop() == parent, "Completion retains the same research screen underneath");
            check(requests.stream().noneMatch(r -> r.action() == ResearchProtocol.Action.CLOSE), "Overlay does not close the research session");
            check(DialogClient.contentScreen() == parent, "Private updates continue to target the obscured menu");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-completion-over-research");
        context.runOnClient(c -> {
            var parent = DialogClient.contentScreen();
            DialogClient.close(done.token());
            check(c.gui.screen() == parent, "Acknowledgement restores the exact menu instance");
            DialogClient.receive(DialogProtocol.Show.of(done.issued(done.token(), 2), true));
            ((CompletionScreen)c.gui.screen()).onClose();
            check(c.gui.screen() == parent, "Escape also restores the existing menu");
        });
        context.setScreen(() -> null);
        check(requests.stream().filter(r -> r.action() == ResearchProtocol.Action.CLOSE).count() == 1, "Only actually leaving the research screen closes its session");
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
            var flag=(Button)c.gui.screen().children().stream().filter(w->w instanceof HoiMenuBar.TabButton b&&b.getX()==4).findFirst().orElseThrow();
            check(flag.getWidth()>flag.getHeight(),"Country flag keeps a wide aspect ratio");
            check(c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/menu/flag_overlay.png")).isPresent(),"Original TFR flag overlay loads from the split UI pack");
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
