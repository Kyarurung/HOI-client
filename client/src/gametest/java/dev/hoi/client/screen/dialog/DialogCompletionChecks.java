package dev.hoi.client.screen.dialog;

import dev.hoi.client.screen.research.ResearchScreen;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.hud.HudTooltip;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.agency.AgencyScreen;
import dev.hoi.client.screen.doctrine.DoctrineNotificationTest;
import dev.hoi.client.screen.focus.FocusScreen;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.research.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class DialogCompletionChecks {
    public static void run(ClientGameTestContext context) {
        DoctrineNotificationTest.run(context);
        DialogCompletionDesignTeamTest.run(context);
        context.runOnClient(client -> {
            var played = new ArrayList<String>();
            net.minecraft.client.sounds.SoundEventListener listener = (sound, event, range) -> {
                if (sound.getIdentifier().getPath().endsWith(".complete")) {
                    check(sound.getSource() == net.minecraft.sounds.SoundSource.MASTER, "Completion sound uses MASTER");
                    played.add(sound.getIdentifier().getPath());
                }
            };
            client.getSoundManager().addListener(listener);
            try {
                DialogCoordinator.reset();
                for (String style : List.of("research_complete", "focus_complete")) {
                    var view = new DialogView(java.util.UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "완료", "", "", "", "", "", List.of(), List.of(new DialogView.Choice("ack", "확인")), style);
                    DialogCoordinator.receive(DialogPayloads.Show.of(view, true));
                    DialogCoordinator.receive(DialogPayloads.Show.of(view.issued(view.token(), 2), false));
                }
                check(played.equals(List.of("ui.research.complete", "ui.focus.complete")), "New notifications play once; refreshes do not repeat the sound");
            } finally {
                client.getSoundManager().removeListener(listener);
                DialogCoordinator.reset();
            }
        });
        context.runOnClient(client -> {
            var heard = new ArrayList<String>();
            net.minecraft.client.sounds.SoundEventListener listener = (sound, event, range) -> {
                if (Set.of("ui.click", "ui.confirm").contains(sound.getIdentifier().getPath())) {
                    check(sound.getSource() == net.minecraft.sounds.SoundSource.MASTER, "Popup buttons use MASTER even with music muted");
                    heard.add(sound.getIdentifier().getPath());
                }
            };
            var music = client.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC);
            double volume = music.get();
            music.set(0.0);
            client.getSoundManager().addListener(listener);
            try {
                for (String style : List.of("research_complete", "focus_complete")) {
                    var view = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "완료", "", "", "", "", "", List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), style);
                    var screen = new DialogCompletion(view, (v, choice) -> {});
                    client.gui.setScreen(screen);
                    heard.clear();
                    press(screen, "세부 사항");
                    press(screen, "확인");
                    screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                    check(heard.equals(List.of("ui.click", "ui.confirm", "ui.confirm")), "Details, OK and Enter each play exactly one original click sound");
                }
            } finally {
                client.getSoundManager().removeListener(listener);
                music.set(volume);
                client.gui.setScreen(null);
            }
        });
        var sent = new ArrayList<String>();
        context.runOnClient(client -> client.options.guiScale().set(2));
        for (int[] size : new int[][]{{2560, 1369}, {1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            for (String style : List.of("research_complete", "focus_complete")) {
                String art = style.equals("research_complete") ? "category/infantry" : "politics/art/gfx_kor_discussion_of_covid_response";
                var view = new DialogView(UUID.randomUUID().toString(), 3, DialogView.Kind.DIPLOMACY, style.equals("research_complete") ? "완료된 연구·중점 이름" : "코로나 대응책 논의", "",
                        "표시하면 안 되는 설명 본문\n".repeat(22), "", art, "", List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), style);
                context.setScreen(() -> new DialogCompletion(view, (v, choice) -> sent.add(v.token() + "/" + v.revision() + "/" + choice)));
                context.waitTicks(3); context.takeScreenshot("hoi-" + style + "-" + size[0]);
                context.runOnClient(c -> {
                    var s = (DialogCompletion)c.gui.screen();
                    check(Math.abs(s.panelLeft() * 2 + s.panelWidth() - s.width) <= 1, "Completion stays centered");
                    check(c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + art + ".png")).isPresent(), "Original completion art exists");
                    for (var child : s.children()) if (child instanceof Button b)
                        check(b.getY() >= 0 && b.getBottom() <= s.height && b.getRight() <= s.width, "Buttons fit viewport");
                    press(s, "세부 사항");
                    check(sent.getLast().equals(view.token() + "/3/details"), "Details use an issued choice instead of expanding the body");
                });
                context.waitTicks(3);
                context.runOnClient(c -> {
                    var s = (DialogCompletion)c.gui.screen(); s.mouseScrolled(s.width / 2.0, s.height / 2.0, 0, -100);
                    check(s.scrollOffset() == 0, "Description body never enters the compact popup"); press(s, "확인");
                    check(sent.getLast().equals(view.token() + "/3/ack"), "Acknowledgement uses issued session without applying rewards");
                    for (int key : new int[]{com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER}) {
                        int before = sent.size();
                        s.keyPressed(new KeyEvent(key, 0, 0));
                        check(sent.size() == before + 1 && sent.getLast().equals(view.token() + "/3/ack"), "Enter confirms exactly once while details are focused");
                    }
                });
                context.waitTicks(2); context.takeScreenshot("hoi-" + style + "-details-" + size[0]);
                context.setScreen(() -> new net.minecraft.client.gui.screens.PauseScreen(true));
                context.runOnClient(c -> {
                    DialogCoordinator.receive(DialogPayloads.Show.of(view, true));
                    check(c.gui.screen() instanceof DialogStack, "Completion opens immediately even while another in-game screen is open");
                    DialogCoordinator.close(view.token()); DialogCoordinator.receive(DialogPayloads.Show.of(view.issued(view.token(), 4), false));
                    check(c.gui.screen() == null, "Stale refresh cannot reopen completion");
                    DialogCoordinator.details(new DialogPayloads.Details(view.token(), "focus/hoi:focus/political_effort"));
                    check(c.gui.screen() == null, "Revoked completion cannot navigate to private details");
                    DialogCoordinator.receive(DialogPayloads.Show.of(view, true));
                    DialogCoordinator.details(new DialogPayloads.Details(view.token(), "focus/hoi:focus/political_effort"));
                    check(c.gui.screen() == null, "Confirmed focus details never fabricate a menu without a server snapshot");
                    var entry = new MenuView.Entry("정치적 노력", "완료", "hoi:focus/political_effort\n완료한 중점");
                    var pages = MenuTab.ORDER.stream().map(t -> new MenuView.Page(t, List.of(new MenuView.Section(
                            t == MenuTab.POLITICS ? "국가 중점" : t.label(), "research", t == MenuTab.POLITICS ? List.of(entry) : List.of())))).toList();
                    var menu = ScreenMenu.forFocus(new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages), "hoi:focus/political_effort");
                    c.gui.setScreen(menu);
                    check(!menu.allowsMovement(), "Completed focus entry opens directly in its detail panel");
                });
            }
        }
        stacking(context);
        completionActions(context);
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
                check(!net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(MenuPayloads.Refresh.TYPE), "Fixture server has no HOI menu receiver");
                var messagesField = net.minecraft.client.gui.components.ChatComponent.class.getDeclaredField("allMessages");
                messagesField.setAccessible(true);
                var messages = (java.util.List<?>) messagesField.get(c.gui.hud.getChat());
                int messageCount = messages.size();
                dev.hoi.client.ClientCampaignHud.accept(HudPayloads.State.HIDDEN);
                dev.hoi.client.HoiClient.openMenu(MenuTab.POLITICS);
                check(c.gui.screen() == null, "A menu is never constructed before campaign participation is authorized");
                for(int modifiers:new int[]{com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT,0}) {
                    method.invoke(c.keyboardHandler,c.getWindow().handle(),com.mojang.blaze3d.platform.InputConstants.PRESS,new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F,0,modifiers));
                    check(c.options.keySwapOffhand.consumeClick(), "Outside a campaign F retains vanilla offhand swap");
                    check(messages.size()==messageCount&&c.gui.screen()==null, "F never opens politics or prints chat on unsupported servers");
                    method.invoke(c.keyboardHandler,c.getWindow().handle(),com.mojang.blaze3d.platform.InputConstants.RELEASE,new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F,0,modifiers));
                }
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
    private static void completionActions(ClientGameTestContext context) {
        for(String style:List.of("focus_complete","research_complete","agency_upgrade_complete"))for(String choice:List.of("ack","details")) {
            var sent=new ArrayList<DialogPayloads.Action>();
            var event=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.EVENT,"국가 이벤트","","본문","","","",List.of(),List.of(new DialogView.Choice("yes","선택")));
            var done=new DialogView(UUID.randomUUID().toString(),2,DialogView.Kind.DIPLOMACY,"완료","","","","menu/research","",List.of(),List.of(new DialogView.Choice("ack","확인"),new DialogView.Choice("details","세부 사항")),style);
            var stack=context.computeOnClient(c->{
                var screen=new DialogStack(null,(view,id)->sent.add(new DialogPayloads.Action(view.token(),view.revision(),id)));
                screen.add(event);screen.add(done);return screen;
            });
            context.setScreen(()->stack);context.waitTicks(2);
            context.runOnClient(c->{
                var button=(Button)stack.find(done.token()).children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(choice.equals("ack")?"확인":"세부 사항")).findFirst().orElseThrow();
                var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                check(stack.mouseClicked(click,false),"Completion accepts the actual mouse click over an event");stack.mouseReleased(click);
                check(sent.equals(List.of(new DialogPayloads.Action(done.token(),2,choice))),"Visible button sends its own issued token, revision and choice exactly once");
                stack.close(done.token());
                check(stack.views().equals(List.of(event)),"Server acknowledgement removes only the completion");
            });
        }
        context.runOnClient(c->{
            DialogCoordinator.reset();
            var event=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.EVENT,"보존할 국가 이벤트","","본문","","","",List.of(),List.of(new DialogView.Choice("yes","선택")));
            c.gui.setScreen(null);DialogCoordinator.receive(DialogPayloads.Show.of(event,true));
            var stack=(DialogStack)c.gui.screen();
            var removed=new java.util.concurrent.atomic.AtomicInteger();
            var old=new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("이전 상세")){@Override public void removed(){removed.incrementAndGet();}};
            DialogCoordinator.setContent(old);
            for(var screen:List.<net.minecraft.client.gui.screens.Screen>of(new AgencyScreen(null),new FocusScreen(""))) {
                DialogCoordinator.setContent(screen);
                check(c.gui.screen()==stack&&DialogCoordinator.contentScreen()==screen,"Details navigation changes the content without replacing the event stack");
                check(stack.views().equals(List.of(event)),"Details navigation preserves the unresolved event and revision");
            }
            check(removed.get()==1,"Replaced detail screen is closed once");
            DialogCoordinator.setContent(null);
            check(c.gui.screen()==stack&&stack.layers().contains(stack.find(event.token())),"Closing details reveals the same pending event");
            DialogCoordinator.reset();
        });
    }
    private static void stacking(ClientGameTestContext context) {
        context.runOnClient(client -> client.options.guiScale().set(2));
        for (int[] size : new int[][]{{2560, 1369}, {1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            var first = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "보병 장비 연구 완료", "", "", "", "menu/research", "",
                    List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), "research_complete");
            var second = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "국가 중점 완료", "", "", "", "menu/focus", "",
                    List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), "focus_complete");
            var event = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.EVENT, "동시에 발생한 국가 이벤트", "", "다른 완료 알림이 열린 상태에서도 표시됩니다.", "", "", "",
                    List.of(), List.of(new DialogView.Choice("yes", "확인")));
            context.runOnClient(c -> {
                DialogCoordinator.reset();
                DialogCoordinator.receive(DialogPayloads.Show.of(first, true));
                DialogCoordinator.receive(DialogPayloads.Show.of(second, true));
                DialogCoordinator.receive(DialogPayloads.Show.of(event, true));
                var stack = (DialogStack)c.gui.screen();
                check(stack.views().size() == 3, "Research, focus and event windows coexist");
                check(stack.find(first.token()).panelLeft() == stack.find(second.token()).panelLeft()
                        && stack.find(first.token()).panelTop() * 2 + stack.find(first.token()).panelHeight() == stack.find(second.token()).panelTop() * 2 + stack.find(second.token()).panelHeight(), "Completion panels share the same center despite different content heights");
                DialogCoordinator.receive(DialogPayloads.Show.of(first.issued(first.token(), 2), false));
                check(stack.find(first.token()).view().revision() == 2, "Hidden window receives its own revision");
                DialogCoordinator.receive(DialogPayloads.Show.of(first, false));
                check(stack.find(first.token()).view().revision() == 2, "Old revisions cannot overwrite a hidden window");
                for (var view : stack.views()) {
                    var window = stack.find(view.token());
                    check(window.panelLeft() >= 0 && window.panelTop() >= 0 && window.panelLeft() + window.panelWidth() <= stack.width
                            && window.panelTop() + window.panelHeight() <= stack.height, "Stacked panels fit viewport");
                }
            });
            context.waitTicks(3); context.takeScreenshot("hoi-stacked-notifications-" + size[0]);
            context.runOnClient(c -> {
                var stack = (DialogStack)c.gui.screen();
                check(stack.layers().getFirst() == stack.find(event.token()) && stack.layers().getLast() == stack.find(second.token()), "Completion layer stays above later events");
                check(stack.inputAt(stack.width / 2.0, stack.height / 2.0) == stack.find(second.token()), "Input reaches the same front completion as rendering");
                var heard = new ArrayList<String>();
                net.minecraft.client.sounds.SoundEventListener listener = (sound, value, range) -> heard.add(sound.getIdentifier().getPath());
                c.getSoundManager().addListener(listener);
                try {
                    for (var completed : List.of(second, first)) {
                        heard.clear();
                        for (String name : List.of("세부 사항", "확인")) {
                            var window = stack.find(completed.token());
                            var button = (Button)window.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow();
                            var click = new net.minecraft.client.input.MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                                    button.getY() + button.getHeight() / 2.0, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                            check(stack.inputAt(click.x(), click.y()) == window, "Event behind completion must not receive its button click");
                            check(stack.mouseClicked(click, false), "Stack accepts completion button click");
                            stack.mouseReleased(click);
                        }
                        check(heard.equals(List.of("ui.click", "ui.confirm")), "Both completion buttons respond with simultaneous event");
                        if (completed == second) DialogCoordinator.close(second.token());
                    }
                } finally { c.getSoundManager().removeListener(listener); }
                check(stack.views().size() == 2 && stack.find(first.token()) != null && stack.find(event.token()) != null, "Closing a hidden window preserves other sessions");
                stack.onClose();
                check(stack.views().size() == 1 && stack.find(event.token()) != null, "Escape closes the front completion before the event");
                DialogCoordinator.reset();
                DialogCoordinator.receive(DialogPayloads.Show.of(first.issued(first.token(), 3), false));
                check(c.gui.screen() == null, "Reset closes every window and stale refresh cannot reopen it");
            });
        }
    }
    private static void layers(ClientGameTestContext context) {
        var event = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.EVENT, "국가 이벤트", "", "", "", "", "", List.of(), List.of());
        var global = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.GLOBAL_EVENT, "글로벌 이벤트", "", "", "", "", "", List.of(), List.of());
        var superEvent = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.SUPER_EVENT, "슈퍼 이벤트", "", "", "", "", "", List.of(), List.of());
        var done = new DialogView(UUID.randomUUID().toString(), 0, DialogView.Kind.DIPLOMACY, "완료 알림", "", "", "", "menu/research", "", List.of(), List.of(), "research_complete");
        var requests = new ArrayList<ResearchPayloads.Request>();
        var view = new ResearchView("layer-parent", 1, "KOR", "대한민국", 0, "2020-01-01", "PAUSED", List.of(new ResearchView.Slot(0, "", 0)), List.of(), "");
        context.setScreen(() -> new ResearchScreen(view, requests::add));
        context.runOnClient(c -> {
            var parent = c.gui.screen();
            DialogCoordinator.receive(DialogPayloads.Show.of(event, true));
            DialogCoordinator.receive(DialogPayloads.Show.of(done, true));
            DialogCoordinator.receive(DialogPayloads.Show.of(superEvent, true));
            DialogCoordinator.receive(DialogPayloads.Show.of(global, true));
            var stack = (DialogStack)c.gui.screen();
            check(stack.layers().equals(List.of(stack.find(global.token()), stack.find(superEvent.token()), parent, stack.find(done.token()))), "Fixed draw order overrides arrival order");
            check(stack.find(event.token()).panelLeft() != stack.find(global.token()).panelLeft(), "Country and news frames retain their different native CENTER offsets");
            check(stack.inputAt(20, 80) == parent, "Sidebar input remains above events");
            check(requests.stream().noneMatch(r -> r.action() == ResearchPayloads.Action.CLOSE), "Background event cannot close the research session");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-notification-layers-over-research");
        context.runOnClient(c -> {
            var stack = (DialogStack)c.gui.screen();
            DialogCoordinator.close(done.token());
            check(stack.layers().getLast() == DialogCoordinator.contentScreen(), "UI becomes foremost after completion closes");
            DialogCoordinator.close(superEvent.token()); DialogCoordinator.close(global.token());
            check(stack.layers().getFirst() == stack.find(event.token()), "Closing newer global event reveals previous country event");
            var parent = DialogCoordinator.contentScreen();
            var future = new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("교리 상세 테스트")) {};
            stack.backdrop(future); future.init(stack.width, stack.height);
            check(stack.layers().getLast() == future && stack.inputAt(stack.width / 2.0, stack.height / 2.0) == future, "Future full detail UI is above events by default");
            stack.backdrop(parent);
            DialogCoordinator.reset();
            check(c.gui.screen() == null, "Reset clears all layers");
        });
    }
    private static void backdrop(ClientGameTestContext context) {
        var requests = new ArrayList<ResearchPayloads.Request>();
        var research = new ResearchView("completion-parent", 1, "KOR", "대한민국", 0, "2020-01-01", "PAUSED",
                List.of(new ResearchView.Slot(0, "", 0)), List.of(), "");
        var done = new DialogView(java.util.UUID.randomUUID().toString(), 1, DialogView.Kind.DIPLOMACY, "완료 알림", "", "", "", "menu/research", "",
                List.of(), List.of(new DialogView.Choice("ack", "확인")), "research_complete");
        context.setScreen(() -> new ResearchScreen(research, requests::add));
        context.runOnClient(c -> {
            var parent = c.gui.screen();
            DialogCoordinator.receive(DialogPayloads.Show.of(done, true));
            check(c.gui.screen() instanceof DialogStack overlay && overlay.backdrop() == parent, "Completion retains the same research screen underneath");
            check(requests.stream().noneMatch(r -> r.action() == ResearchPayloads.Action.CLOSE), "Overlay does not close the research session");
            check(DialogCoordinator.contentScreen() == parent, "Private updates continue to target the obscured menu");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-completion-over-research");
        context.runOnClient(c -> {
            var parent = DialogCoordinator.contentScreen();
            DialogCoordinator.close(done.token());
            check(c.gui.screen() == parent, "Acknowledgement restores the exact menu instance");
            DialogCoordinator.receive(DialogPayloads.Show.of(done.issued(done.token(), 2), true));
            c.gui.screen().onClose();
            check(c.gui.screen() == parent, "Escape also restores the existing menu");
        });
        context.setScreen(() -> null);
        check(requests.stream().filter(r -> r.action() == ResearchPayloads.Action.CLOSE).count() == 1, "Only actually leaving the research screen closes its session");
    }
    private static void hud(ClientGameTestContext context) {
        var rows = new ArrayList<HudDetail.Row>();
        for (int i = 0; i < 32; i++) rows.add(new HudDetail.Row("현재 적용되는 국가 효과 " + i, "+2.5%", HudDetail.Tone.GOOD));
        rows.add(new HudDetail.Row("마지막 설명", "0", HudDetail.Tone.MUTED));
        var hud = new CountryHud(null, null, null, null, null, null, false, null,
                CountryHud.NationalIndicators.UNKNOWN, Map.of("political_power", new HudDetail(rows), "supplies", new HudDetail(List.of(new HudDetail.Row("병참 상황", "", HudDetail.Tone.HEADING), new HudDetail.Row("요구", "0", HudDetail.Tone.NORMAL), new HudDetail.Row("비축 물자", "0 (0%)", HudDetail.Tone.NORMAL)))));
        var pages = MenuTab.ORDER.stream().map(t -> new MenuView.Page(t, List.of(new MenuView.Section(t.label(), t.id(), List.of())))).toList();
        context.setScreen(() -> new ScreenMenu(new MenuView("KOR", "대한민국", "2020-01-01", "PAUSED", pages, hud)));
        context.getInput().setCursorPos(135, 14); context.waitTicks(3);
        context.runOnClient(c -> {
            var flag=(Button)c.gui.screen().children().stream().filter(w->w instanceof HudMenuBar.HudTabButton b&&b.getMessage().getString().equals(MenuTab.POLITICS.label()+" 메뉴")).findFirst().orElseThrow();
            check(flag.getWidth()>flag.getHeight(),"Country flag keeps a wide aspect ratio");
            check(c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/menu/flag_overlay.png")).isPresent(),"Original TFR flag overlay loads from the split UI pack");
            var lines = HudTooltip.body(c.font, c.gui.screen().width, hud, HudMenuBar.indicators(hud).getFirst());
            for (var item : HudMenuBar.indicators(hud)) {
                check(item.value().equals((item.icon().equals("party_support")||item.icon().equals("supplies")) ? "0%" : "0"), "Unknown HUD value displays zero: " + item.icon());
                var text = new StringBuilder();
                for (var line : HudTooltip.body(c.font, c.gui.screen().width, hud, item))
                    line.accept((index, style, character) -> { text.appendCodePoint(character); return true; });
                check((item.icon().equals("supplies") || !text.toString().contains("현재: 0")) && text.indexOf("—") < 0, "Zero tooltip rows must be hidden: " + item.icon());
            }
            var supply = HudMenuBar.indicators(hud).stream().filter(item -> item.icon().equals("supplies")).findFirst().orElseThrow();
            var supplyText = new StringBuilder();
            for (var line : HudTooltip.body(c.font, c.gui.screen().width, hud, supply))
                line.accept((index, style, character) -> { supplyText.appendCodePoint(character); return true; });
            check(supplyText.toString().startsWith("병참 상황")&&!supplyText.toString().contains("현재:"), "Logistics begins with its original heading without a duplicate percentage");
            check(supplyText.toString().contains("요구: 0") && supplyText.toString().contains("비축 물자: 0 (0%)"), "Supply requirements and empty stockpiles remain visible");
            check(HudMenuBar.rawNumber(-1.25).equals("-1.25"), "Actual negative effects retain their sign and decimals");
            for (var line : lines) check(c.font.width(line) <= 310, "HUD lines wrap within viewport");
        });
        context.takeScreenshot("hoi-hud-detail-compact");
    }
    private static void press(DialogCompletion s, String name) {
        var button = (Button)s.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow();
        var event = new net.minecraft.client.input.MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
        check(s.mouseClicked(event, false), "Visible completion button accepts a mouse click");
        s.mouseReleased(event);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
