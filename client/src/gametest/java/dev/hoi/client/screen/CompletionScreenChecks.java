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
                    check(c.gui.screen() instanceof DialogStackScreen, "Completion opens immediately even while another in-game screen is open");
                    DialogClient.close(view.token()); DialogClient.receive(DialogProtocol.Show.of(view.issued(view.token(), 4), false));
                    check(c.gui.screen() == null, "Stale refresh cannot reopen completion");
                    DialogClient.details(new DialogProtocol.Details(view.token(), "focus/hoi:focus/political_effort"));
                    check(c.gui.screen() == null, "Revoked completion cannot navigate to private details");
                    DialogClient.receive(DialogProtocol.Show.of(view, true));
                    DialogClient.details(new DialogProtocol.Details(view.token(), "focus/hoi:focus/political_effort"));
                    check(c.gui.screen() == null, "Confirmed focus details never fabricate a menu without a server snapshot");
                    var entry = new MenuView.Entry("정치적 노력", "완료", "hoi:focus/political_effort\n완료한 중점");
                    var pages = MenuTab.ORDER.stream().map(t -> new MenuView.Page(t, List.of(new MenuView.Section(
                            t == MenuTab.POLITICS ? "국가 중점" : t.label(), "research", t == MenuTab.POLITICS ? List.of(entry) : List.of())))).toList();
                    var menu = HoiMenuScreen.forFocus(new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages), "hoi:focus/political_effort");
                    c.gui.setScreen(menu);
                    check(!menu.allowsMovement(), "Completed focus entry opens directly in its detail panel");
                });
            }
        }
        stacking(context);
        layers(context);
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
    private static void stacking(ClientGameTestContext context) {
        for (int[] size : new int[][]{{1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            var first = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "보병 장비 연구 완료", "", "", "", "menu/research", "",
                    List.of(), List.of(new DialogView.Choice("ack", "확인")), "research_complete");
            var second = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "국가 중점 완료", "", "", "", "menu/focus", "",
                    List.of(), List.of(new DialogView.Choice("ack", "확인")), "focus_complete");
            var event = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.EVENT, "동시에 발생한 국가 이벤트", "", "다른 완료 알림이 열린 상태에서도 표시됩니다.", "", "", "",
                    List.of(), List.of(new DialogView.Choice("yes", "확인")));
            context.runOnClient(c -> {
                DialogClient.reset();
                DialogClient.receive(DialogProtocol.Show.of(first, true));
                DialogClient.receive(DialogProtocol.Show.of(second, true));
                DialogClient.receive(DialogProtocol.Show.of(event, true));
                var stack = (DialogStackScreen)c.gui.screen();
                check(stack.views().size() == 3, "Research, focus and event windows coexist");
                check(stack.find(first.token()).panelLeft() == stack.find(second.token()).panelLeft() && stack.find(first.token()).panelTop() == stack.find(second.token()).panelTop(), "Completion panels overlap at exactly the same position");
                DialogClient.receive(DialogProtocol.Show.of(first.issued(first.token(), 2), false));
                check(stack.find(first.token()).view().revision() == 2, "Hidden window receives its own revision");
                DialogClient.receive(DialogProtocol.Show.of(first, false));
                check(stack.find(first.token()).view().revision() == 2, "Old revisions cannot overwrite a hidden window");
                for (var view : stack.views()) {
                    var window = stack.find(view.token());
                    check(window.panelLeft() >= 0 && window.panelTop() >= 0 && window.panelLeft() + window.panelWidth() <= stack.width
                            && window.panelTop() + window.panelHeight() <= stack.height, "Stacked panels fit viewport");
                }
            });
            context.waitTicks(3); context.takeScreenshot("hoi-stacked-notifications-" + size[0]);
            context.runOnClient(c -> {
                var stack = (DialogStackScreen)c.gui.screen();
                check(stack.layers().getFirst() == stack.find(event.token()) && stack.layers().getLast() == stack.find(second.token()), "Completion layer stays above later events");
                check(stack.inputAt(stack.width / 2.0, stack.height / 2.0) == stack.find(second.token()), "Input reaches the same front completion as rendering");
                DialogClient.close(second.token());
                check(stack.views().size() == 2 && stack.find(first.token()) != null && stack.find(event.token()) != null, "Closing a hidden window preserves other sessions");
                stack.onClose();
                check(stack.views().size() == 1 && stack.find(event.token()) != null, "Escape closes the front completion before the event");
                DialogClient.reset();
                DialogClient.receive(DialogProtocol.Show.of(first.issued(first.token(), 3), false));
                check(c.gui.screen() == null, "Reset closes every window and stale refresh cannot reopen it");
            });
        }
    }
    private static void layers(ClientGameTestContext context) {
        var event = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.EVENT, "국가 이벤트", "", "", "", "", "", List.of(), List.of());
        var global = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.GLOBAL_EVENT, "글로벌 이벤트", "", "", "", "", "", List.of(), List.of());
        var superEvent = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.SUPER_EVENT, "슈퍼 이벤트", "", "", "", "", "", List.of(), List.of());
        var done = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.DIPLOMACY, "완료 알림", "", "", "", "menu/research", "", List.of(), List.of(), "research_complete");
        var requests = new ArrayList<ResearchProtocol.Request>();
        var view = new ResearchView("layer-parent", 1, "KOR", "대한민국", 0, "2020-01-01", "PAUSED", List.of(new ResearchView.Slot(0, "", 0)), List.of(), "");
        context.setScreen(() -> new ResearchScreen(view, requests::add));
        context.runOnClient(c -> {
            var parent = c.gui.screen();
            DialogClient.receive(DialogProtocol.Show.of(event, true));
            DialogClient.receive(DialogProtocol.Show.of(done, true));
            DialogClient.receive(DialogProtocol.Show.of(superEvent, true));
            DialogClient.receive(DialogProtocol.Show.of(global, true));
            var stack = (DialogStackScreen)c.gui.screen();
            check(stack.layers().equals(List.of(stack.find(global.token()), stack.find(superEvent.token()), parent, stack.find(done.token()))), "Fixed draw order overrides arrival order");
            check(stack.find(event.token()).panelLeft() == stack.find(global.token()).panelLeft() && stack.find(event.token()).panelTop() == stack.find(global.token()).panelTop(), "Global and country events share one position");
            check(stack.inputAt(20, 80) == parent, "Sidebar input remains above events");
            check(requests.stream().noneMatch(r -> r.action() == ResearchProtocol.Action.CLOSE), "Background event cannot close the research session");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-notification-layers-over-research");
        context.runOnClient(c -> {
            var stack = (DialogStackScreen)c.gui.screen();
            DialogClient.close(done.token());
            check(stack.layers().getLast() == DialogClient.contentScreen(), "UI becomes foremost after completion closes");
            DialogClient.close(superEvent.token()); DialogClient.close(global.token());
            check(stack.layers().getFirst() == stack.find(event.token()), "Closing newer global event reveals previous country event");
            var parent = DialogClient.contentScreen();
            var future = new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("교리 상세 테스트")) {};
            stack.backdrop(future); future.init(stack.width, stack.height);
            check(stack.layers().getLast() == future && stack.inputAt(stack.width / 2.0, stack.height / 2.0) == future, "Future full detail UI is above events by default");
            stack.backdrop(parent);
            DialogClient.reset();
            check(c.gui.screen() == null, "Reset clears all layers");
        });
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
            check(c.gui.screen() instanceof DialogStackScreen overlay && overlay.backdrop() == parent, "Completion retains the same research screen underneath");
            check(requests.stream().noneMatch(r -> r.action() == ResearchProtocol.Action.CLOSE), "Overlay does not close the research session");
            check(DialogClient.contentScreen() == parent, "Private updates continue to target the obscured menu");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-completion-over-research");
        context.runOnClient(c -> {
            var parent = DialogClient.contentScreen();
            DialogClient.close(done.token());
            check(c.gui.screen() == parent, "Acknowledgement restores the exact menu instance");
            DialogClient.receive(DialogProtocol.Show.of(done.issued(done.token(), 2), true));
            c.gui.screen().onClose();
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
