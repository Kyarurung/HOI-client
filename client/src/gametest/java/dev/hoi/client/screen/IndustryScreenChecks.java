package dev.hoi.client.screen;

import dev.hoi.client.ResearchScreenGameTest;
import dev.hoi.client.ui.HoiPanelLayout;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.nio.charset.StandardCharsets;
import java.util.*;


public final class IndustryScreenChecks {
    public static void run(ClientGameTestContext context, CountryHud hud) {
        IndustryView fixture;
        try (var in = IndustryScreenChecks.class.getResourceAsStream("/industry-fixture.json")) {
            fixture = new IndustryProtocol.Response(new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8)).view();
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        var v = copy(fixture, hud, fixture.revision(), null);
        var empty = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), List.of(), List.of(), null, "", v.navalRepairs(), v.specialForces());
        var requests = new ArrayList<IndustryProtocol.Request>();
        for (var tab : List.of(MenuTab.PRODUCTION, MenuTab.TRADE, MenuTab.LOGISTICS, MenuTab.RECRUITMENT)) {
            context.setScreen(() -> new IndustryScreen(tab, v.session(), tab == MenuTab.RECRUITMENT ? empty : v, requests::add));
            context.waitTicks(3); context.takeScreenshot("hoi-industry-" + tab.id()); ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-" + tab.id());
            context.runOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                check(screen.panelWidth() == HoiPanelLayout.width(tab, screen.width), "Original sidebar proportions");
                checkBounds(screen);
                if (tab == MenuTab.LOGISTICS) {
                    check(IndustryScreen.LOGISTICS_HEADERS.equals(List.of("평균 생산 효율","장비 유형","생산","상태","수요","균형","비축량","자원")), "Eight logistics columns follow the supplied order");
                    for (String icon : List.of("efficiency","production","need","balance","stockpile"))
                        check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/"+icon+".png")).isPresent(), "Original logistics header icon " + icon);
                }
                if (tab == MenuTab.PRODUCTION) {
                    check(screen.repairDockyards().equals("2/3"), "Server-issued repair use and capacity");
                    check(screen.productionIndicators().stream().map(IndustryView.Modifier::id).toList().equals(List.of("dockyard","factory","cap","retention","growth","damage")), "Requested six production indicators remain ordered");
                    check(screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("생산 라인 삭제")).count() >= Math.min(3,v.lines().size()), "Three production lines fit the standard viewport");
                }
                for (String texture : java.util.stream.Stream.concat(v.equipment().stream().map(IndustryView.Equipment::texture),
                        java.util.stream.Stream.concat(v.battalions().stream().map(IndustryView.Choice::texture), v.companies().stream().map(IndustryView.Choice::texture))).toList())
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + texture + ".png")).isPresent(), "Original external asset " + texture);
                for (String path : List.of("resources/oil", "resources/aluminum", "resources/rubber", "resources/tungsten", "resources/iron", "resources/chromium", "resources/coal",
                        "designer/add", "designer/locked", "designer/slot", "units/motorized_infantry", "actions/infantry", "actions/armor", "actions/air", "actions/navy", "actions/repair",
                        "modifiers/dockyard", "modifiers/factory", "modifiers/cap", "modifiers/retention", "modifiers/growth", "modifiers/damage",
                        "summary/military_factory_icon", "summary/dockyard_icon", "summary/dockyard_icon_with_wrench"))
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/" + path + ".png")).isPresent(), "Production and designer asset " + path);
            });
            if (tab == MenuTab.TRADE) { click(context, "경제"); context.waitTicks(2); context.takeScreenshot("hoi-industry-economy"); click(context, "무역"); }
            context.getInput().resizeWindow(854, 480); context.waitTicks(3);
            context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); s.mouseScrolled(20, 170, 0, -100); checkBounds(s); });
            context.waitTicks(2); context.takeScreenshot("hoi-industry-compact-" + tab.id());
            context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        }
        context.setScreen(() -> new IndustryScreen(MenuTab.TRADE, v.session(), v, requests::add)); context.waitTicks(2);
        var seller = v.partners().stream().filter(p -> p.route() && p.exports().getOrDefault("IRON", 0.0) > 0).findFirst().orElseThrow();
        click(context, seller.name() + "에서 수입"); context.waitTicks(2); context.takeScreenshot("hoi-industry-trade-contract"); ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-trade-contract");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        click(context, "수입 계약 체결");
        context.runOnClient(client -> {
            var request = requests.getLast();
            check(request.action() == IndustryProtocol.Action.TRADE && request.item().equals(seller.id()) && request.other().equals("IRON") && request.amount() == 1,
                    "Trade submits the issued seller, resource and civilian factory count");
        });
        requests.clear();
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, v.session(), v, requests::add)); context.waitTicks(2);
        click(context, "공장 늘리기 · 보유량 안에서 배정");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen(); var request = requests.getLast();
            check(request.action() == IndustryProtocol.Action.ASSIGN && request.amount() == v.lines().getFirst().factories() + 1
                    && request.revision() == v.revision(), "Only a bounded authenticated factory request");
            check(!button(s, "공장 늘리기 · 보유량 안에서 배정").active, "No duplicate request while waiting");
            s.update(copy(v, hud, v.revision() + 1, null));
        });
        click(context, "보병 및 포병 장비 제작"); context.waitTicks(2); context.takeScreenshot("hoi-industry-equipment-picker");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        click(context, "목록 닫기");
        for (String name : List.of("기갑 차량 제작", "항공기 제작", "함선 건조")) {
            click(context, name); context.waitTicks(1);
            context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
            click(context, "목록 닫기");
        }
        click(context, "해군 수리 대기열"); context.waitTicks(2); context.takeScreenshot("hoi-industry-naval-repairs");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        context.getInput().resizeWindow(854, 480); context.waitTicks(3); context.takeScreenshot("hoi-industry-compact-naval-repairs");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        checkGenerations(context,hud);
        context.setScreen(() -> new IndustryScreen(MenuTab.RECRUITMENT, empty.session(), empty, requests::add)); context.waitTicks(2);
        context.runOnClient(client -> check(client.gui.screen().children().stream().noneMatch(w -> w instanceof Button b && b.getMessage().getString().equals("훈련 취소 · 지급 장비와 인력 반환")), "No training rows before a server-issued recruit exists"));
        String template = v.templates().getFirst().name();
        click(context, template + " 훈련");
        context.runOnClient(client -> {
            check(requests.getLast().action() == IndustryProtocol.Action.RECRUIT && requests.getLast().other().isEmpty(), "Training starts with no deployment location");
            ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 1, null));
            check(client.gui.screen().children().stream().anyMatch(w -> w instanceof Button b && b.getMessage().getString().equals("훈련 취소 · 지급 장비와 인력 반환")), "Authoritative recruit response creates a training line");
        });
        context.waitTicks(2); ResearchScreenGameTest.gui2Screenshot(context, "hoi-recruitment-after-training");
        var trainingRows = new ArrayList<IndustryView.Recruit>();
        for (int group = 0; group < 2; group++) {
            var t = v.templates().get(group);
            for (int row = 0; row < 4; row++) trainingRows.add(new IndustryView.Recruit("alignment-" + group + "-" + row,
                    t.id(), group == 0 ? "" : v.recruits().getFirst().location(), row % 3, .25 + row * .2, t.manpower(), t.equipment(), group == 0 ? 0 : 5, row));
        }
        var trainingView = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), trainingRows, List.of(), null, "", v.navalRepairs(), v.specialForces(), new IndustryView.RecruitmentPolicy(Set.of(), Map.of("reinforcement", 2, "upgrade", 1, "supply", 1, "operation", 1, "garrison", 1, "raid", 0), .65, Map.of()));
        for (int[] size : new int[][]{{1600, 1000}, {2560, 1440}}) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.setScreen(() -> new IndustryScreen(MenuTab.RECRUITMENT, trainingView.session(), trainingView, requests::add));
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.waitTicks(3);
            context.runOnClient(client -> check(client.gui.screen().width == size[0] / 2 && client.gui.screen().height == size[1] / 2, "Reference captures require effective GUI scale 2"));
            context.takeScreenshot("hoi-recruitment-multiple-lines-" + size[0] + "-gui2");
            context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
            click(context, "클릭하여 접기");
            context.waitTicks(2); context.takeScreenshot("hoi-recruitment-collapsed-" + size[0] + "-gui2");
            context.runOnClient(client -> {
                var screen = (IndustryScreen)client.gui.screen();
                check(screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("훈련 취소 · 지급 장비와 인력 반환")).count() == 4, "Collapse hides only the chosen group's training rows");
                for (String asset : List.of("collapse", "expand", "cancel_line", "delete_template", "increase", "decrease", "icon_bg"))
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/recruitment/" + asset + ".png")).isPresent(), "Original recruitment asset " + asset);
            });
            click(context, "클릭하여 펼치기");
            click(context, v.templates().getFirst().name() + " 연속 훈련 횟수 증가");
            context.runOnClient(client -> check(requests.getLast().action() == IndustryProtocol.Action.SERIES && requests.getLast().amount() == 1, "Infinity increments to one, without adding a line"));
        }
        context.getInput().resizeWindow(1600, 1000);
        context.setScreen(() -> new IndustryScreen(MenuTab.RECRUITMENT, v.session(), v, requests::add));
        context.waitTicks(2);
        click(context, template + " 편제");
        context.runOnClient(client -> ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 2, v.templates().getFirst())));
        context.waitTicks(3); context.takeScreenshot("hoi-industry-division-designer"); ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-division-designer");
        context.getInput().setCursorPos(220,615); context.waitTicks(2); context.takeScreenshot("hoi-industry-division-equipment-tooltip");
        context.getInput().setCursorPos(1500,800);
        context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); check(!s.allowsMovement(), "Designer blocks movement"); checkBounds(s); });
        click(context, v.battalions().stream().filter(c -> c.id().equals(v.templates().getFirst().line().getFirst())).findFirst().orElseThrow().name());
        context.waitTicks(2); context.takeScreenshot("hoi-industry-battalion-choices");
        click(context, "대대 목록 닫기");
        context.runOnClient(client -> ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 3, v.templates().get(1))));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-support-add-and-lock");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen();
            check(button(s,"지원중대 추가").active,"Only the next empty support slot is editable");
            check(!button(s,"앞선 빈 지원중대 칸부터 추가").active,"Locked support slots cannot issue edits");
            checkBounds(s);
        });
        context.getInput().resizeWindow(854, 480); context.waitTicks(3);
        context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); checkBounds(s); s.mouseScrolled(150, 160, 0, -100); checkBounds(s); });
        context.waitTicks(2); context.takeScreenshot("hoi-industry-compact-designer");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen();
            s.update(IndustryView.revoked(v.session(), "권한 만료"));
            check(client.gui.screen() == null, "Revocation closes private screen");
            s.update(copy(v, hud, v.revision() + 100, v.templates().getFirst()));
            check(client.gui.screen() == null, "Late response cannot resurrect revoked screen");
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
    private static void checkGenerations(ClientGameTestContext context, CountryHud hud) {
        IndustryView fixture;
        try (var in=IndustryScreenChecks.class.getResourceAsStream("/industry-upgrade-fixture.json")) {
            fixture=new IndustryProtocol.Response(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8)).view();
        } catch(java.io.IOException e) { throw new RuntimeException(e); }
        var v=copy(fixture,hud,fixture.revision(),null);
        var old=v.equipment().stream().filter(IndustryView.Equipment::outdated).findFirst().orElseThrow();
        var next=v.equipment().stream().filter(e->e.id().equals(old.replacement())).findFirst().orElseThrow();
        var requests=new ArrayList<IndustryProtocol.Request>();
        var rows=LogisticsRows.create(v.equipment(),v.lines(),"all");
        var family=rows.stream().filter(row->row.models().contains(old)).findFirst().orElseThrow();
        check(family.representative().equals(next),"Logistics uses the latest researched model even with no stock of it");
        check(family.stockpile()==old.stockpile()+next.stockpile(),"Old stock remains in the family total");
        check(rows.stream().flatMap(row->row.models().stream()).allMatch(e->e.unlocked()||e.stockpile()>0),"Unresearched empty equipment is absent");
        for (int[] size : new int[][]{{1600,1000},{854,480}}) {
            context.getInput().resizeWindow(size[0],size[1]); context.waitTicks(2);
            context.setScreen(()->new IndustryScreen(MenuTab.LOGISTICS,v.session(),v,requests::add));
            context.waitTicks(2); context.takeScreenshot("hoi-logistics-latest-researched-"+size[0]);
            context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        }
        context.getInput().resizeWindow(1600,1000); context.waitTicks(2);
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,v.session(),v,requests::add));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-outdated-line");
        requests.clear();
        context.runOnClient(client->check(client.gui.screen().children().stream().noneMatch(w->w instanceof net.minecraft.client.gui.components.Checkbox),"No obsolete checkbox on production details"));
        click(context,old.name()+" · 장비 교체"); context.waitTicks(2);
        context.runOnClient(client->{
            var s=(IndustryScreen)client.gui.screen();
            check(button(s,"생산: "+next.name()).active,"Clicking the line offers its researched replacement");
            check(s.children().stream().noneMatch(w->w instanceof Button b && b.getMessage().getString().equals("생산: "+old.name())),"Old equipment is hidden by default");
            check(s.children().stream().noneMatch(w->w instanceof Button b && b.getMessage().getString().equals("생산: "+v.equipment().stream().filter(e->e.id().equals("hoi:support_gear")).findFirst().orElseThrow().name())),"Replacement picker stays within the registered family");
            check(obsoleteCheckbox(s).active,"Obsolete toggle starts enabled inside the picker"); checkBounds(s);
        });
        context.takeScreenshot("hoi-industry-current-model-picker");
        context.runOnClient(client->obsoleteCheckbox((IndustryScreen)client.gui.screen()).onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-show-outdated-picker");
        context.runOnClient(client->{
            var s=(IndustryScreen)client.gui.screen();
            check(obsoleteCheckbox(s).active,"User can toggle old models again");
            check(!button(s,"생산: "+old.name()).active,"Current equipment is visible but cannot retool to itself");
            check(requests.isEmpty(),"Changing a display filter sends no simulation request");
        });
        context.getInput().resizeWindow(854,480); context.waitTicks(3);
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.takeScreenshot("hoi-industry-compact-model-picker");
        click(context,"생산: "+next.name());
        context.runOnClient(client->{
            var request=requests.getLast();
            check(request.action()==IndustryProtocol.Action.SWITCH && request.item().equals(v.lines().getFirst().id())
                    && request.other().equals(next.id()) && request.revision()==v.revision(),"Replacement uses the existing bounded line command and issued revision");
        });
        context.getInput().resizeWindow(1600,1000); context.waitTicks(3);
    }
    private static Button obsoleteCheckbox(IndustryScreen screen) { return button(screen,"구형 장비 표시"); }
    private static IndustryView copy(IndustryView v, CountryHud hud, long revision, IndustryView.Template draft) {
        return new IndustryView(v.session(), revision, v.country(), hud, v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(),
                v.battalions(), v.companies(), v.locations(), v.recruits(), v.deployed(), draft, "",
                new IndustryView.NavalRepairs(3, List.of(
                        new IndustryView.NavalRepair("fixture-ship-1", "초계함 · 렌더링 예제", "production/equipment/destroyer", "부산", 30, 50, "정박 · 수리 대기"),
                        new IndustryView.NavalRepair("fixture-ship-2", "잠수함 · 렌더링 예제", "production/equipment/submarine", "진해", 12, 40, "귀항 중")), 2), new IndustryView.SpecialForces(3, 6));
    }
    private static Button button(IndustryScreen screen, String name) {
        return (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow(() -> new AssertionError("Missing control: " + name));
    }
    private static void click(ClientGameTestContext context, String name) {
        context.runOnClient(client -> { var b = button((IndustryScreen)client.gui.screen(), name); check(b.active, "Enabled control: " + name); b.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0)); });
    }
    private static void checkBounds(IndustryScreen screen) {
        var buttons = screen.children().stream().filter(w -> w instanceof Button).map(w -> (Button)w).toList();
        for (var b : buttons) check(b.getX() >= 0 && b.getY() >= 0 && b.getRight() <= screen.width && b.getBottom() <= screen.height, "Industry control inside viewport: " + b.getMessage().getString());
        for (int i = 0; i < buttons.size(); i++) for (int j = i + 1; j < buttons.size(); j++) {
            var a = buttons.get(i); var b = buttons.get(j);
            check(a.getRight() <= b.getX() || b.getRight() <= a.getX() || a.getBottom() <= b.getY() || b.getBottom() <= a.getY(),
                    "Controls must not overlap: " + a.getMessage().getString() + " / " + b.getMessage().getString());
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
