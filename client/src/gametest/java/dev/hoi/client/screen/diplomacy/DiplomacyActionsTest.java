package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.hud.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class DiplomacyActionsTest {
    private DiplomacyActionsTest() {}
    public static void run(ClientGameTestContext context) {
        source(context);
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        String session = UUID.randomUUID().toString();
        var requests = new ArrayList<CountryPayloads.Action>();
        var view = fixture(session, 3);
        context.setScreen(() -> new DiplomacyCountryScreen(view, requests::add));
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-actions-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            var disabled = button(screen, "독립 보장");
            if (disabled.active) throw new AssertionError("Authoritative disabled action became enabled");
            var accept = button(screen, "군사 통행권 수락");
            int x = accept.getX() + accept.getWidth() / 2, y = accept.getY() + accept.getHeight() / 2;
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, 0)), false);
            if (screen.children().stream().anyMatch(w -> w instanceof Button b && b.getMessage().getString().equals("확인")))
                throw new AssertionError("Right click opened a diplomatic action");
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            if (screen.allowsMovement()) throw new AssertionError("Confirmation failed to capture movement input");
            if (!requests.isEmpty()) throw new AssertionError("Opening confirmation sent an action");
        });
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-proposal-confirm-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            screen.mouseScrolled(screen.panelWidth() + 20, 100, 0, -20);
            if (!requests.isEmpty()) throw new AssertionError("Scrolling confirmation sent an action");
            screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE, 0, 0));
            if (!screen.allowsMovement() || !requests.isEmpty()) throw new AssertionError("Escape cancellation mutated diplomacy");
            press(button(screen, "군사 통행권 수락"));
            press(button(screen, "확인"));
            press(button(screen, "확인"));
            if (requests.size() != 1 || !requests.getFirst().session().equals(session) || requests.getFirst().revision() != 3
                    || !requests.getFirst().id().equals("accept:foreign_1")) throw new AssertionError("Request did not retain its authoritative session and proposal");
            for (int tick = 0; tick < 100; tick++) screen.tick();
            if (screen.children().stream().anyMatch(w -> w instanceof Button b && b.getMessage().getString().equals("확인")))
                throw new AssertionError("Lost response left diplomacy permanently pending");
            if (requests.size() != 1) throw new AssertionError("Timeout retransmitted a mutation");
            screen.update(CountryPayloads.Response.of(screen.token(), fixture(session, 4)));
            screen.update(CountryPayloads.Response.of(screen.token(), view));
            if (screen.view().actions().revision() != 4) throw new AssertionError("Stale revision replaced current diplomacy");
        });
        var point = context.computeOnClient(c -> {
            var button = button((DiplomacyCountryScreen)c.gui.screen(), "독립 보장");
            return new double[]{(button.getX() + button.getWidth() / 2.0) * c.getWindow().getGuiScale(), (button.getY() + button.getHeight() / 2.0) * c.getWindow().getGuiScale()};
        });
        context.getInput().setCursorPos(point[0], point[1]);
        context.waitTicks(15);
        context.takeScreenshot("hoi-diplomacy-disabled-condition-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            screen.mouseScrolled(screen.panelWidth() - 15, screen.height - 70, 0, -50);
            press(button(screen, "시장 접근권 협상"));
            press(button(screen, "확인"));
            if (requests.size() != 2 || !requests.getLast().id().equals("market_access")) throw new AssertionError("Final diplomacy action was unreachable");
            String old = screen.token();
            screen.select("JAP");
            screen.update(CountryPayloads.Response.of(old, view));
            if (screen.view() != null) throw new AssertionError("Previous country response crossed target selection");
            var reopened = new DiplomacyCountryScreen(view, requests::add);
            c.gui.setScreen(reopened);
            reopened.update(CountryPayloads.Response.of(old, fixture(session, 7)));
            if (reopened.view().actions().revision() != 3) throw new AssertionError("Old session response crossed reopen");
        });
        context.getInput().resizeWindow(1600, 1000);
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-actions-compact-gui2");
        context.runOnClient(c -> {
            var screen = (DiplomacyCountryScreen)c.gui.screen();
            for (var child : screen.children()) if (child instanceof Button b && (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height))
                throw new AssertionError("Diplomacy action escaped compact screen bounds");
        });
    }
    private static CountryView fixture(String session, long revision) {
        var actions = new ArrayList<CountryView.Action>();
        actions.add(new CountryView.Action("accept:foreign_1", "군사 통행권 수락", "제안: 군사 통행권\nPRK → KOR\n정치력 비용 없음 · 30일 남음\n상대 국가의 군대가 우리 영토에서 이동할 수 있습니다.", true));
        actions.add(new CountryView.Action("decline:foreign_1", "군사 통행권 거절", "제안을 거절합니다. 정치력 비용 없음.", true));
        actions.add(new CountryView.Action("guarantee", "독립 보장", "정치력 15\n\n§c같은 이념이 필요하며 중복 보장할 수 없습니다.§r", false));
        for (int i = 0; i < 19; i++) actions.add(new CountryView.Action("withdraw:foreign_" + (i + 2), "제안 취소 " + (i + 2), "정치력 비용 없음 · 상대국 응답 대기", true));
        actions.add(new CountryView.Action("market_access", "시장 접근권 협상", "", true));
        actions.add(new CountryView.Action("naval_blockade", "해상 봉쇄 실시", "", false));
        var reports = Arrays.stream(AgencyIntelDomain.values()).map(d -> new CountryView.Report(d, 0, List.of())).toList();
        return new CountryView("KOR", CountryHud.UNKNOWN, "PRK", "조선민주주의인민공화국", false,
                List.of(new CountryView.Choice("PRK", "조선민주주의인민공화국")), List.of(), List.of(), reports,
                new CountryView.Actions(session, revision, "", actions), List.of());
    }
    private static void source(ClientGameTestContext context) {
        String directory=System.getenv("HOI_AGENCY_OPERATION_PREVIEW");
        if(directory==null)directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            var view=new CountryPayloads.Response(UUID.randomUUID().toString(),java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-diplomacy-PRK.json"))).view();
            if(!view.viewer().equals("KOR")||!view.target().equals("PRK")||view.actions().entries().size()!=23||!view.actions().entries().getLast().id().equals("market_access"))throw new AssertionError("Actual KOR to PRK response must end with market access in one list");
            var spirit=view.diplomacy().stream().filter(e->e.name().equals("자립경제")).findFirst().orElseThrow();
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(c->{c.options.guiScale().set(2);dev.hoi.client.ClientCampaignHud.accept(HudPayloads.State.of(view.viewer(),view.hud()));if(dev.hoi.client.ui.UiAssets.imageHeight(spirit.icon(),30)<=0)throw new AssertionError("Actual self-reliance spirit must resolve to its original image");});
                context.setScreen(()->new DiplomacyCountryScreen(view,a->{}));context.getInput().setCursorPos(size[0]-5,80);context.waitTicks(3);context.takeScreenshot("hoi-diplomacy-source-PRK-"+size[0]+"x"+size[1]+"-gui2");
                context.runOnClient(c->{var screen=(DiplomacyCountryScreen)c.gui.screen();if(screen.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().contains("다음 외교 행동")))throw new AssertionError("Source list must not insert an extra page action");screen.mouseScrolled(screen.panelWidth()-15,screen.height-70,0,-50);});context.waitTicks(3);
                context.runOnClient(c->{var screen=(DiplomacyCountryScreen)c.gui.screen();var market=view.actions().entries().getLast();var control=button(screen,market.name());if(control.getY()<0||control.getBottom()>screen.height||control.active!=market.enabled())throw new AssertionError("Market access must remain reachable with authoritative availability");});
                context.takeScreenshot("hoi-diplomacy-source-market-"+size[0]+"x"+size[1]+"-gui2");
                var requests = new ArrayList<CountryPayloads.Action>();
                context.setScreen(() -> new DiplomacyCountryScreen(view, requests::add));
                context.runOnClient(c -> press(button((DiplomacyCountryScreen)c.gui.screen(), "첩보 장부")));
                for (var domain : AgencyIntelDomain.values()) {
                    context.runOnClient(c -> press(button((DiplomacyCountryScreen)c.gui.screen(), domain.label + " 정보")));
                    context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                    context.takeScreenshot("hoi-intel-ledger-" + domain.name().toLowerCase(Locale.ROOT) + "-" + size[0] + "-gui2");
                    if (domain == AgencyIntelDomain.ARMY && !view.report(domain).sections().isEmpty()) {
                        context.runOnClient(client -> {
                            var screen = (DiplomacyCountryScreen)client.gui.screen();
                            var tab = button(screen, "비축량");
                            var event = new net.minecraft.client.input.MouseButtonEvent(tab.getX() + 5, tab.getY() + 5, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                            screen.mouseClicked(event, false); screen.mouseReleased(event);
                            String expected = view.report(domain).sections().stream().filter(section -> section.id().equals("stockpiles")).findFirst().orElseThrow().tooltip();
                            if (!screen.ledgerDetailAt(tab.getX() + 5, tab.getBottom() + 15).equals(expected)) throw new AssertionError("Stockpile tab did not retain issued section tooltip");
                        });
                        context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                        context.takeScreenshot("hoi-intel-ledger-stockpiles-" + size[0] + "-gui2");
                    }
                    var point = context.computeOnClient(c -> {
                        var tab = button((DiplomacyCountryScreen)c.gui.screen(), domain.label + " 정보");
                        return new int[]{tab.getX() + tab.getWidth() / 2, tab.getY() + tab.getHeight() / 2};
                    });
                    context.getInput().setCursorPos(point[0] * 2, point[1] * 2); context.waitTicks(15);
                    context.takeScreenshot("hoi-intel-ledger-" + domain.name().toLowerCase(Locale.ROOT) + "-report-tooltip-" + size[0] + "-gui2");
                    var research = view.report(domain).entries().stream().filter(entry -> entry.detail().contains("계통도")).findFirst().orElse(null);
                    if (research != null) {
                        var at = context.computeOnClient(c -> {
                            var screen = (DiplomacyCountryScreen)c.gui.screen();
                            var hover = screen.children().stream().filter(child -> child instanceof dev.hoi.client.ui.UiHoverRegion widget
                                && widget.getMessage().getString().equals(research.name())).map(dev.hoi.client.ui.UiHoverRegion.class::cast).findFirst().orElseThrow();
                            int x = hover.getX() + hover.getWidth() / 2, y = hover.getY() + hover.getHeight() / 2;
                            if (!screen.ledgerDetailAt(x, y).equals(research.detail())) throw new AssertionError("Research tooltip changed authoritative wording or line breaks");
                            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, 0)), false);
                            return new int[]{x, y};
                        });
                        context.getInput().setCursorPos(at[0] * 2, at[1] * 2); context.waitTicks(15);
                        context.takeScreenshot("hoi-intel-ledger-" + domain.name().toLowerCase(Locale.ROOT) + "-research-tooltip-" + size[0] + "-gui2");
                    }
                }
                if (!requests.isEmpty()) throw new AssertionError("Intel ledger inspection must not send diplomatic actions");
            }
        }catch(java.io.IOException error){throw new AssertionError("Actual KOR to PRK fixture unavailable",error);}
    }
    private static Button button(DiplomacyCountryScreen screen, String name) {
        return (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }
    private static void press(Button button) { button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0)); }
}
