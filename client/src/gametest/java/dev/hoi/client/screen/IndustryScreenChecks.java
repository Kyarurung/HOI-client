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
    private static void tradeTooltips(ClientGameTestContext context, IndustryView v) {
        var trade=v.trades().stream().filter(IndustryView.Trade::importing).findFirst().orElseThrow();
        var partner=v.partners().stream().filter(p->p.id().equals(trade.partner())).findFirst().orElseThrow();
        check(partner.detail()!=null,"Server fixture supplies original trade tooltips");
        var law=v.partners().stream().filter(p->p.id().equals("AST")).findFirst().orElseThrow().detail().law();
        check(law.contains("§4+40.0%§r")&&law.contains("§4+20.0%§r"),"Source intelligence leaks retain red one-decimal percentages");
        check(law.equals(law.strip()),"Source law has no trailing blank tooltip rows");
        var detail=partner.detail().resources().get(trade.resource());
        check(detail.requested()>0&&detail.transported().contains("진행 중"),"Active resource has original trade status");
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        context.setScreen(()->new IndustryScreen(MenuTab.TRADE,v.session(),v,request->{}));context.waitTicks(3);
        click(context,v.resources().stream().filter(r->r.id().equals(trade.resource())).findFirst().orElseThrow().name());
        context.getInput().setCursorPos(2400,1300);context.waitTicks(3);
        context.takeScreenshot("hoi-trade-active-resource-gui2");
        for(int column=0;column<4;column++) {
            final int col=column;
            var point=context.computeOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();var row=button(screen,partner.name()+"에서 수입");
                int pane=row.getWidth()+12;int[] edges={8,pane*44/100,pane*65/100,pane*83/100,pane-7};
                return new double[]{(edges[col]+edges[col+1])*client.getWindow().getGuiScale()/2,(row.getY()+row.getHeight()/2.0)*client.getWindow().getGuiScale()};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("hoi-trade-tooltip-"+List.of("country","export","delivered","convoys").get(column)+"-gui2");
        }
        context.getInput().setCursorPos(2400,1300);
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }
    private static void resourceShortages(ClientGameTestContext context, IndustryView v) {
        var resources = v.resources().stream().map(r -> new IndustryView.Resource(r.id(), r.name(), r.extracted(), r.imported(), r.exported(),
                r.id().equals("ALUMINUM") ? 32 : r.id().equals("COAL") ? 0 : r.demand(),
                r.id().equals("ALUMINUM") ? 210 : r.id().equals("COAL") ? 129 : r.demand())).toList();
        var shortage = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), resources, v.modifiers(), v.equipment(), v.lines(),
                v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), v.recruits(), v.deployed(), v.draft(), v.message(), v.navalRepairs(), v.specialForces());
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, v.session(), shortage, request -> {}));
        context.getInput().setCursorPos(1800, 800);
        context.waitTicks(3);
        try {
            var image = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-resource-shortages-gui2").toFile());
            var cells = context.computeOnClient(client -> {
                var screen = (IndustryScreen)client.gui.screen();
                int cell = (screen.panelWidth() - 12) / resources.size();
                int y = dev.hoi.client.ui.HoiMenuBar.height(screen.width) + 29;
                return new int[]{cell, y};
            });
            for (int i = 0; i < resources.size(); i++) {
                boolean missing = resources.get(i).available() < resources.get(i).demand();
                int iconRed = 0, numberRed = 0;
                for (int y = 0; y < 27 * 2; y++) for (int x = 0; x < (cells[0] - 1) * 2; x++) {
                    int pixel = image.getRGB((6 + i * cells[0]) * 2 + x, cells[1] * 2 + y);
                    int red = pixel >> 16 & 255, green = pixel >> 8 & 255, blue = pixel & 255;
                    if (red > 100 && red > green + 20 && red > blue + 20) {
                        if (y < 16 * 2) iconRed++; else numberRed++;
                    }
                }
                check(missing ? iconRed > 5 && numberRed > 5 : numberRed == 0, "Shortage icon and signed amount follow each resource balance: " + resources.get(i).id());
            }
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    public static void run(ClientGameTestContext context, CountryHud hud) {
        IndustryView fixture;
        try (var in = IndustryScreenChecks.class.getResourceAsStream("/industry-fixture.json")) {
            fixture = new IndustryProtocol.Response(new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8)).view();
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        var v = copy(fixture, hud, fixture.revision(), null);
        for(var r:v.resources())check(Math.abs(r.extracted()+r.imported()-r.exported()-r.available())<1e-8,"Server fixture retains signed extraction in resource balance: "+r.id());
        var chromium=v.resources().stream().filter(r->r.id().equals("CHROMIUM")).findFirst().orElseThrow();
        check(chromium.extracted()<0&&chromium.imported()>0&&chromium.available()<0,"Partial chromium imports still leave a deficit");
        tradeTooltips(context,v);
        var sourceEquipment=new ArrayList<IndustryView.Equipment>();
        var names=List.of("견인포","자주대공포","경기계화","정찰전차","다목적 헬리콥터","대전차 장비","수륙양용장갑차","휴대용 대공 미사일","차량화 로켓포","수륙양용전차");
        var ids=List.of("artillery_equipment","spaa_equipment","light_mechanized_equipment","light_tank_chassis","utility_helicopter_equipment","anti_tank_equipment","amphibious_mechanized_equipment","anti_air_equipment","motorized_rocket_equipment","amphibious_tank_chassis");
        for(int i=0;i<ids.size();i++)sourceEquipment.add(new IndustryView.Equipment("hoi:tfr/equipment/"+ids.get(i),names.get(i),
                "production/equipment/tfr/equipment/"+ids.get(i),"infantry",false,true,1,1,Map.of(),1,0,0,0,"",ids.get(i),names.get(i),0L,true));
        var illustrations=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),sourceEquipment,List.of(),v.partners(),v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),List.of(),List.of(),null,"",v.navalRepairs());
        context.setScreen(()->new IndustryScreen(MenuTab.LOGISTICS,v.session(),illustrations,request->{}));
        ResearchScreenGameTest.gui2Screenshot(context,"hoi-logistics-source-equipment-images");
        var supplyModels = v.equipment().stream().map(e -> new IndustryView.Equipment(e.id(), e.name(), e.texture(), e.group(), e.naval(), e.unlocked(),
                e.cost(), e.factoryLimit(), e.id().equals("hoi:infantry_gear") ? Map.of("OIL", 1.0, "ALUMINUM", 2.0, "RUBBER", 3.0, "TUNGSTEN", 4.0, "IRON", 5.0, "CHROMIUM", 6.0) : e.resources(), e.stockpile() == 0 ? 50 : e.stockpile(), e.reserved(), e.deployed(), e.deficit(), e.replacement(), e.family(), e.familyName(), 100L)).toList();
        var supply = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), supplyModels, v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), v.recruits(), v.deployed(), v.draft(), v.message(), v.navalRepairs(), v.specialForces());
        context.setScreen(() -> new IndustryScreen(MenuTab.LOGISTICS, supply.session(), supply, request -> {}));
        ResearchScreenGameTest.gui2Screenshot(context, "hoi-logistics-supply-status");
        context.getInput().resizeWindow(2560, 1440); context.waitTicks(3);
        for (int row : new int[]{0, 1}) {
            var point = context.computeOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                int pane = screen.panelWidth(), x = 6 + (pane - 12) * 83 / 100;
                double scale = client.getWindow().getGuiScale();
                return new double[]{(x + 8) * scale, (dev.hoi.client.ui.HoiMenuBar.height(screen.width) + 59 + row * 39) * scale};
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(3);
            context.takeScreenshot(row == 0 ? "hoi-logistics-resource-icon-hover-gui2" : "hoi-logistics-empty-resource-cell-gui2");
        }
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        var empty = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), List.of(), List.of(), null, "", v.navalRepairs(), v.specialForces());
        conversion(context,v);
        factoryControls(context,v);
        resourceShortages(context, v);
        var requests = new ArrayList<IndustryProtocol.Request>();
        for (var tab : List.of(MenuTab.PRODUCTION, MenuTab.TRADE, MenuTab.LOGISTICS, MenuTab.RECRUITMENT)) {
            context.setScreen(() -> new IndustryScreen(tab, v.session(), tab == MenuTab.RECRUITMENT ? empty : v, requests::add));
            context.waitTicks(3); context.takeScreenshot("hoi-industry-" + tab.id()); ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-" + tab.id());
            context.runOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                check(screen.panelWidth() == HoiPanelLayout.width(tab, screen.width), "Original sidebar proportions");
                checkBounds(screen);
                if (tab == MenuTab.LOGISTICS) {
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:font/korean.json")).isEmpty(), "Cancelled Korean font override is absent");
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
        context.runOnClient(client -> {
            check(!button((IndustryScreen)client.gui.screen(),"수입 계약 체결").active,"A new zero-quantity contract cannot be sent");
            check(!button((IndustryScreen)client.gui.screen(),"무역 공장 줄이기").active,"Minimum import quantity disables decrease");
            check(IndustryScreen.neededTradeFactories(20,24,16,8,10)==2,"Auto quantity replaces current imports instead of adding them twice");
            check(IndustryScreen.neededTradeFactories(20,40,16,8,10)==0,"Surplus supply cancels unnecessary imports");
            check(IndustryScreen.neededTradeFactories(21,4,0,8,10)==3,"Resource shortage rounds up to complete factories");
            check(IndustryScreen.neededTradeFactories(100,0,0,8,2)==2,"Auto quantity respects available factories and export limits");
            for(int factories=0;factories<=6;factories++) {
                double amount=Math.min(44,factories*8),position=IndustryScreen.tradeSliderPosition(factories,44,8);
                check(Math.abs(position-amount/44)<1e-9,"Slider position follows resource share including the partial last factory");
                check(IndustryScreen.tradeSliderFactories(position,44,8)==factories,"Resource slider steps round-trip through all 44-resource positions");
            }
            check(IndustryScreen.tradeSliderFactories(1,41,8)==6,"A one-resource remainder still costs the final factory");
            check(IndustryScreen.convoyCell(0).equals("-")&&IndustryScreen.convoyCell(9).equals("9")&&IndustryScreen.convoyCell(null).equals("—"),"Trade convoy cells distinguish land, per-factory sea demand and unknown routes");
        });
        click(context, "무역 공장 늘리기"); context.waitTicks(2);
        click(context, "무역 공장 줄이기"); context.waitTicks(2);
        click(context, "필요한 수입량 설정"); context.waitTicks(2);
        context.getInput().resizeWindow(2560,1440); context.waitTicks(3);
        var needPoint=context.computeOnClient(client->{
            var b=button((IndustryScreen)client.gui.screen(),"필요한 수입량 설정");
            double scale=client.getWindow().getGuiScale();
            return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};
        });
        context.getInput().setCursorPos(needPoint[0],needPoint[1]);context.waitTicks(10);
        context.takeScreenshot("hoi-industry-trade-auto-tooltip-gui2");
        context.getInput().setCursorPos(0,0);
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        context.setScreen(() -> new IndustryScreen(MenuTab.TRADE, v.session(), v, requests::add));context.waitTicks(2);
        click(context,seller.name()+"에서 수입");
        context.runOnClient(client->{
            var slider=(net.minecraft.client.gui.components.AbstractSliderButton)client.gui.screen().children().stream()
                    .filter(w->w instanceof net.minecraft.client.gui.components.AbstractSliderButton).findFirst().orElseThrow();
            check(seller.resourcesPerFactory()==8,"Ordinary trade imports eight resources per civilian factory");
            slider.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT,0,0));
        });context.waitTicks(2);
        ResearchScreenGameTest.gui2Screenshot(context,"hoi-industry-trade-contract-quantity");
        click(context, "수입 계약 체결");
        context.runOnClient(client -> {
            var request = requests.getLast();
            check(request.action() == IndustryProtocol.Action.TRADE && request.item().equals(seller.id()) && request.other().equals("IRON") && request.amount() == 1,
                    "Trade submits the issued seller, resource and civilian factory count");
        });
        var partialPartners=v.partners().stream().map(p->p.id().equals(seller.id())?new IndustryView.Partner(p.id(),p.name(),Map.of("IRON",44.0),p.route(),p.convoys(),p.influence(),8.0,p.freeConvoys(),p.buyers()):p).toList();
        var partial=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),v.equipment(),v.lines(),partialPartners,v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),v.draft(),v.message(),v.navalRepairs(),v.specialForces());
        context.setScreen(()->new IndustryScreen(MenuTab.TRADE,partial.session(),partial,requests::add));context.waitTicks(2);
        click(context,seller.name()+"에서 수입");
        for(int i=0;i<6;i++)click(context,"무역 공장 늘리기");
        ResearchScreenGameTest.gui2Screenshot(context,"hoi-industry-trade-partial-44");
        context.runOnClient(client->check(!button((IndustryScreen)client.gui.screen(),"무역 공장 늘리기").active,"The last partial resource batch reaches the import limit"));
        click(context,"수입 계약 체결");
        context.runOnClient(client->check(requests.getLast().amount()==6,"Importing all 44 resources reserves six civilian factories"));
        requests.clear();
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, v.session(), v, requests::add)); context.waitTicks(2);
        click(context, "공장 늘리기 · 부족분 예약");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen(); var request = requests.getLast();
            check(request.action() == IndustryProtocol.Action.ASSIGN && request.amount() == v.lines().getFirst().factories() + 1
                    && request.revision() == v.revision(), "Only a bounded authenticated factory request");
            check(!button(s, "공장 늘리기 · 부족분 예약").active, "No duplicate request while waiting");
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
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            var cancel = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().endsWith(" 부대 추가")).findFirst().orElseThrow();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(cancel.getX() - 20, cancel.getY() + cancel.getHeight()/2,
                    new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
            check(screen.selectingDeployment() && screen.children().isEmpty(), "Location selection hides the client menu");
            check(requests.getLast().action() == IndustryProtocol.Action.SELECT_LOCATION, "Selection starts with an authenticated request");
            screen.update(copy(v, hud, v.revision() + 2, null));
        });
        context.waitTicks(2); context.takeScreenshot("hoi-deployment-menu-hidden");
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(screen.width / 2, screen.height / 2,
                    new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
            check(!screen.selectingDeployment() && !screen.children().isEmpty(), "Map left-click restores recruitment menu");
            check(requests.getLast().action() == IndustryProtocol.Action.LOCATION_AT, "Map click submits the selected ray");
            check(!screen.acceptsDeploymentOverlay(v.session(), Long.MAX_VALUE), "Late green hatch cannot reappear after assignment");
        });
        context.waitTicks(2); context.takeScreenshot("hoi-deployment-menu-restored");

        context.runOnClient(client -> {
            var needs = Map.of("rifles", 900L, "support", 100L);
            check(IndustryScreen.fillRatio(needs, Map.of("rifles", 450L)) == .45, "A missing equipment family must not hide supplied equipment");
            check(IndustryScreen.fillRatio(needs, Map.of("rifles", 1800L, "unneeded", 500L)) == .9, "Surplus and unrelated equipment cannot replace a missing family");
            check(IndustryScreen.fillRatio(needs, Map.of()) == 0, "No issued equipment stays empty");
            check(IndustryScreen.fillRatio(needs, needs) == 1, "Fully issued equipment fills the gauge");
            check(IndustryScreen.fillRatio(Map.of(), Map.of()) == 1, "No required equipment is already satisfied");
        });
        var trainingRows = new ArrayList<IndustryView.Recruit>();
        for (int group = 0; group < 2; group++) {
            var t = v.templates().get(group);
            for (int row = 0; row < 4; row++) {
                var supplied = new TreeMap<String, Long>();
                int portion = row;
                t.equipment().forEach((id, count) -> supplied.put(id, count * portion / 3));
                if (row == 1 && supplied.size() > 1) supplied.remove(supplied.firstKey());
                trainingRows.add(new IndustryView.Recruit("alignment-" + group + "-" + row,
                        t.id(), group == 0 ? "" : v.recruits().getFirst().location(), row % 3, .25 + row * .2, t.manpower(), supplied, group == 0 ? 0 : 5, row));
            }
        }
        var trainingView = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), trainingRows, List.of(), null, "", v.navalRepairs(), v.specialForces(), new IndustryView.RecruitmentPolicy(Set.of(), Map.of("reinforcement", 2, "upgrade", 1, "supply", 1, "operation", 1, "garrison", 1, "raid", 0), .65, Map.of(), null, Map.of("supply", .25, "operation", .5, "garrison", .75, "raid", 1.0)));
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
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            var input = (net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();
            screen.setFocused(input); input.setValue("입력 중인 편제"); input.setCursorPosition(3); input.setHighlightPos(6);
            screen.update(copy(v, hud, v.revision() + 3, v.templates().getFirst()));
            check(screen.children().contains(input) && screen.getFocused() == input && input.isFocused(), "Daily refresh must retain the live editor and focus");
            check(input.getValue().equals("입력 중인 편제") && input.getCursorPosition() == 3 && input.getHighlighted().equals("중인 "), "Daily refresh must retain text, cursor and selection");
            input.insertText("새 "); check(input.getValue().equals("입력 새 편제"), "Typing continues at the retained selection");
            input.setValue(v.templates().getFirst().name());
        });
        context.waitTicks(3); context.takeScreenshot("hoi-industry-division-designer"); ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-division-designer");
        context.getInput().setCursorPos(220,615); context.waitTicks(2); context.takeScreenshot("hoi-industry-division-equipment-tooltip");
        context.getInput().setCursorPos(1500,800);
        context.runOnClient(client -> { var s = (IndustryScreen)client.gui.screen(); check(!s.allowsMovement(), "Designer blocks movement"); checkBounds(s); });
        click(context, v.battalions().stream().filter(c -> c.id().equals(v.templates().getFirst().line().getFirst())).findFirst().orElseThrow().name());
        context.waitTicks(2); context.takeScreenshot("hoi-industry-battalion-categories");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen();
            check(button(s, "보병 대대").active, "First slot can choose a subtype in its current category");
            check(button(s, "기갑 대대").active && button(s, "기동 대대").active, "First slot offers whole-column category replacement");
        });
        ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-battalion-categories");
        int beforeCategory = requests.size();
        click(context, "보병 대대");
        context.runOnClient(client -> {
            check(requests.size() == beforeCategory, "Opening infantry choices cannot mutate the template");
            check(button((IndustryScreen)client.gui.screen(), "제거").active, "Current category opens detailed battalion choices");
        });
        click(context, "대대 목록 닫기");
        click(context, "기갑 대대");
        context.runOnClient(client -> check(requests.size() == beforeCategory, "Selecting a category must not insert a generic battalion"));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-first-slot-details");
        ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-first-slot-details");
        click(context, v.battalions().stream().filter(c -> c.id().equals("TANK")).findFirst().orElseThrow().name());
        context.runOnClient(client -> {
            check(requests.getLast().action() == IndustryProtocol.Action.LINE_COLUMN && requests.getLast().amount() == 0
                    && requests.getLast().item().equals("TANK"), "Category choice replaces the column through its first slot");
            ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 4, v.templates().getFirst()));
            var s = (IndustryScreen)client.gui.screen();
            var first = button(s, v.battalions().stream().filter(c -> c.id().equals(v.templates().getFirst().line().getFirst())).findFirst().orElseThrow().name());
            var lower = (Button)s.children().stream().filter(w -> w instanceof Button b && b.getX() == first.getX() && b.getY() > first.getY())
                    .min(java.util.Comparator.comparingInt(w -> ((Button)w).getY())).orElseThrow();
            lower.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
            check(s.children().stream().noneMatch(w -> w instanceof Button b && (b.getMessage().getString().equals("기갑 대대")
                    || b.getMessage().getString().equals("기동 대대"))), "Lower slots cannot switch the column category");
            check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/designer/remove.png")).isPresent(), "Original removal image is packaged");
            var controls = List.copyOf(s.children());
            for (int tick = 0; tick < 60; tick++) s.tick();
            check(s.children().equals(controls) && button(s, "제거").active, "Passive refresh keeps button instances and enabled icons");
        });
        context.waitTicks(2); context.takeScreenshot("hoi-industry-battalion-choices");
        ResearchScreenGameTest.gui2Screenshot(context, "hoi-industry-battalion-choices");
        click(context, "대대 목록 닫기");
        context.runOnClient(client -> ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 5, v.templates().stream().filter(t -> t.support().size() < 4).findFirst().orElseThrow())));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-support-add-and-lock");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen();
            check(button(s,"지원중대 추가").active,"Only the next empty support slot is editable");
            check(!button(s,"앞선 빈 지원중대 칸부터 추가").active,"Locked support slots cannot issue edits");
            checkBounds(s);
        });
        click(context,"지원중대 추가"); context.waitTicks(2); context.takeScreenshot("hoi-industry-support-choices");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        click(context,"대대 목록 닫기");
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
            check(obsoleteCheckbox(s).active && !obsoleteCheckbox(s).selected(),"Obsolete checkbox starts unchecked inside the picker"); checkBounds(s);
        });
        context.takeScreenshot("hoi-industry-current-model-picker");
        context.runOnClient(client->obsoleteCheckbox((IndustryScreen)client.gui.screen()).onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-show-outdated-picker");
        context.runOnClient(client->{
            var s=(IndustryScreen)client.gui.screen();
            check(obsoleteCheckbox(s).active && obsoleteCheckbox(s).selected(),"Obsolete checkbox is checked when old models are visible");
            check(!button(s,"생산: "+old.name()).active,"Current equipment is visible but cannot retool to itself");
            check(requests.isEmpty(),"Changing a display filter sends no simulation request");
        });
        context.runOnClient(client->obsoleteCheckbox((IndustryScreen)client.gui.screen()).onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)));
        context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            check(!obsoleteCheckbox(screen).selected(),"Second toggle clears the obsolete checkbox");
            check(screen.children().stream().noneMatch(w->w instanceof Button b && b.getMessage().getString().equals("생산: "+old.name())),"Unchecking hides obsolete equipment");
            check(requests.isEmpty(),"Both checkbox toggles stay local");
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
    private static void factoryControls(ClientGameTestContext context, IndustryView v) {
        context.runOnClient(client -> IndustryScreen.clearPreferences());
        var gun=v.equipment().stream().filter(e->e.id().equals("hoi:modern_infantry_gear")).findFirst().orElseThrow();
        var sourceGun=new IndustryView.Equipment(gun.id(),gun.name(),gun.texture(),gun.group(),gun.naval(),gun.unlocked(),gun.cost(),gun.factoryLimit(),gun.resources(),gun.stockpile(),gun.reserved(),gun.deployed(),gun.deficit(),gun.replacement(),gun.family(),gun.familyName(),gun.demand(),gun.acquired(),"infantry_equipment_3");
        var ship=v.equipment().stream().filter(e->e.naval()&&e.factoryLimit()==15).findFirst().orElseThrow();
        var lines=List.of(new IndustryView.Line("military",gun.id(),15,150,.96378,10,.2,.8,null,10,2,.00003,.00009,.6453),
                new IndustryView.Line("naval",ship.id(),15,15,1,2,.2,0,null,10,2));
        var economy=v.economy();
        var owned=new IndustryView.Economy(12,12,economy.civilian(),economy.freeCivilian(),economy.consumer(),economy.fuel(),economy.gdp(),economy.debt(),economy.manpower(),economy.armyXp(),economy.energy(),economy.energyDemand());
        var modifiers=v.modifiers().stream().map(m->{
            if(!m.id().equals("cap"))return m;
            int split=m.detail().lastIndexOf("\n\n");
            String description=split<0?m.detail():m.detail().substring(split+2);
            return new IndustryView.Modifier(m.id(),m.name(),.9674,
                    "기본 한도: §e50.00%§r\n£tech_mod 개선된 공작기계: §a+2.50%§r\n£pol_idea 산업 정책: §a+48.52%§r\n인플레이션: §c-4.28%§r\n\n"+description);
        }).toList();
        var issued=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),owned,v.resources(),modifiers,v.equipment().stream().map(e->e.id().equals(gun.id())?sourceGun:e).toList(),lines,v.partners(),v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),null,"",v.navalRepairs());
        var requests=new ArrayList<IndustryProtocol.Request>();
        check(IndustryScreen.productionFactoryState(0,1,1,0,0,15,-1)==0,"Operating factories use the normal frame");
        check(IndustryScreen.productionFactoryState(10,1,0,1,0,15,-1)==2,"Damage uses the red frame");
        check(IndustryScreen.productionFactoryState(12,1,0,0,1,15,-1)==3,"Reservations use the dark gray frame");
        check(IndustryScreen.productionFactoryState(20,10,0,0,0,15,30)==1,"Adding factories previews bright icons");
        check(IndustryScreen.productionFactoryState(20,10,10,0,0,40,20)==4,"Cancelling previews warm icons after the target cell");
        check(IndustryScreen.productionFactoryState(10,10,0,2,3,15,-1)==2,"A mixed group uses one complete damage icon");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));
        context.waitTicks(2);ResearchScreenGameTest.gui2Screenshot(context,"hoi-production-factory-damage-reservations");
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();checkBounds(screen);
            check(screen.panelWidth()>=275,"Production keeps GUI2 reference cell space when the viewport permits");
            check(screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().matches("(military · 공장|naval · 조선소) [0-9]+칸")).count()==30,"Both production pools expose all fifteen cells");
            check(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().startsWith("naval · ×")),"Dockyards have no multiplier buttons");
            for(String icon:List.of("factory_0","factory_1","factory_2","factory_3","dockyard_0","dockyard_1","dockyard_2","dockyard_3","x5_normal","x5_selected","x10_normal","x10_selected","efficiency_frame","efficiency_fill","efficiency_bg","progress_fill","progress_bg"))
                check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/line/"+icon+".png")).isPresent(),"Original production state frame "+icon);
        });
        context.getInput().resizeWindow(2560,1440);context.waitTicks(2);
        context.runOnClient(client->{
            var text=dev.hoi.client.ui.HoiTooltips.productionText(net.minecraft.network.chat.Component.literal(
                    "£prod_eff 생산 효율: §e96.38%§r (매일 §e0.00%§r)\n£tech_mod 기술: §a+2.50%§r\n£pol_idea 정책: §c-3.25%§r"));
            check(!text.getString().contains("£"),"Source texticon tokens resolve to original sprites");
            check(text.getString().chars().filter(c->c>=0xe000&&c<=0xf8ff).count()==3,"Inline icons appear exactly once");
            check(text.getString().contains("0.00%"),"Production tooltips retain stationary and rounded zero values");
            text.visit((style,part)->{
                if(part.contains("96.38%"))check(style.getColor().getValue()==0xFFBD00,"TFR general tooltip gold");
                if(part.contains("+2.50%"))check(style.getColor().getValue()==0x009F03,"TFR general tooltip green");
                if(part.contains("-3.25%"))check(style.getColor().getValue()==0xFF3232,"TFR general tooltip red");
                return java.util.Optional.empty();
            },net.minecraft.network.chat.Style.EMPTY);
            String equipmentTip=ProductionTooltips.equipment(issued,lines.getFirst(),sourceGun);
            check(equipmentTip.contains("원본 기본 능력치")&&equipmentTip.contains("1.5")&&!equipmentTip.contains("1.50"),"Equipment reference stats retain source precision");
            check(equipmentTip.contains("일일 생산량")&&equipmentTip.contains("공장당 자원 사용:"),"Equipment tooltip uses actual production and resources");
            String gauge=ProductionTooltips.efficiency(issued,lines.getFirst());
            check(gauge.contains("96.38%")&&gauge.contains("0.00%")&&!gauge.contains("0.003%"),"Gauge precision is two decimal places");
            check(gauge.contains("매일 기본 획득량: §e0.01%")&&gauge.contains("자원 부족: §c-64.53%"),"Gauge includes server base gain and shortage with two decimals");
            for(double efficiency:new double[]{.9674,.9674-1e-15,.98}) {
                var capped=new IndustryView.Line("military",gun.id(),15,150,efficiency,10,.2,.8,null,10,2,0.0,0.0,.6453);
                check(!ProductionTooltips.efficiency(issued,capped).contains("(매일"),"Daily gain is hidden at or above the actual efficiency cap");
                String cappedTip=ProductionTooltips.efficiency(issued,capped);
                check(cappedTip.contains("매일 기본 획득량: §e0.00%")&&cappedTip.contains("자원 부족: §c-64.53%"),"Capped efficiency retains base gain and resource modifiers");
            }
            var below=new IndustryView.Line("military",gun.id(),15,150,.967399,10,.2,.8,null,10,2,.000001);
            check(ProductionTooltips.efficiency(issued,below).contains("매일"),"Rounded percentages do not hide growth below the cap");
            check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/tooltip.png")).isPresent(),"Global tooltip uses the original TFR dialog asset");
        });
        for(int indicator=0;indicator<6;indicator++) {
            final int column=indicator;
            var point=context.computeOnClient(client->{var screen=(IndustryScreen)client.gui.screen();int cell=(screen.panelWidth()-12)/6;double scale=client.getWindow().getGuiScale();return new double[]{(6+column*cell+cell/2.0)*scale,(dev.hoi.client.ui.HoiMenuBar.height(screen.width)+70)*scale};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(2);
            context.takeScreenshot("hoi-production-modifier-"+List.of("dockyard","factory","cap","retention","growth","damage").get(indicator)+"-gui2");
        }
        var gaugePoint=context.computeOnClient(client->{var screen=(IndustryScreen)client.gui.screen();double scale=client.getWindow().getGuiScale();return new double[]{(6+Math.round(270*(screen.panelWidth()-12)/511f))*scale,(dev.hoi.client.ui.HoiMenuBar.height(screen.width)+140)*scale};});
        context.getInput().setCursorPos(gaugePoint[0],gaugePoint[1]);context.waitTicks(2);
        context.takeScreenshot("hoi-production-efficiency-tooltip-gui2");
        var equipmentPoint=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),gun.name()+" · 장비 교체");double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
        context.getInput().setCursorPos(equipmentPoint[0],equipmentPoint[1]);context.waitTicks(3);
        context.takeScreenshot("hoi-production-equipment-tooltip-gui2");
        var cappedLines=new ArrayList<>(lines);
        cappedLines.set(0,new IndustryView.Line("military",gun.id(),15,150,.9674,10,.2,.8,null,10,2,0.0,0.0,.6453));
        var cappedView=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),owned,v.resources(),modifiers,issued.equipment(),cappedLines,v.partners(),v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),null,"",v.navalRepairs());
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,cappedView.session(),cappedView,requests::add));
        context.getInput().setCursorPos(gaugePoint[0],gaugePoint[1]);context.waitTicks(2);
        context.takeScreenshot("hoi-production-efficiency-capped-tooltip-gui2");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));
        context.getInput().setCursorPos(700,700);context.getInput().resizeWindow(1600,1000);context.waitTicks(2);
        context.runOnClient(client->{check(requests.stream().allMatch(request->request.action()==IndustryProtocol.Action.CLOSE),"Tooltip fixture transitions only close their previous screen");requests.clear();});
        for(int multiplier:new int[]{5,10,1}) {
            click(context,"military · ×"+(multiplier==1?10:multiplier));
            check(requests.isEmpty(),"Display multiplier does not mutate production");
            context.waitTicks(2);ResearchScreenGameTest.gui2Screenshot(context,"hoi-production-factories-x"+multiplier);
        }
        click(context,"military · ×10");
        context.getInput().resizeWindow(2560,1440);context.waitTicks(2);
        for(int hovered:new int[]{3,1}) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"military · 공장 "+hovered+"칸");double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(2);
            context.takeScreenshot(hovered==3?"hoi-production-factory-add-hover-gui2":"hoi-production-factory-cancel-hover-gui2");
        }
        context.getInput().setCursorPos(700,700);context.getInput().resizeWindow(1600,1000);context.waitTicks(2);
        click(context,"military · ×10");
        click(context,"military · 공장 15칸");
        context.runOnClient(client->{var r=requests.getLast();check(r.action()==IndustryProtocol.Action.ASSIGN&&r.item().equals("military")&&r.amount()==15&&r.revision()==issued.revision(),"X1 allows reserving fifteen factories despite only twelve owned");});
        for(int multiplier:new int[]{5,10}) {
            requests.clear();context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);
            click(context,"military · ×"+multiplier);
            context.setScreen(()->null);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);
            ResearchScreenGameTest.gui2Screenshot(context,"hoi-production-reopened-x"+multiplier);
            click(context,"military · 공장 3칸");
            context.runOnClient(client->check(requests.getLast().amount()==3*multiplier,"Factory cell applies its selected multiplier"));
        }
        requests.clear();context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);
        click(context,"naval · 조선소 15칸");context.runOnClient(client->check(requests.getLast().amount()==15&&requests.getLast().item().equals("naval"),"Dockyard cells reserve one per cell"));
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);requests.clear();
        click(context,ship.name()+" · 재고량 · 군수 보기");
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            check(screen.getTitle().getString().equals("군수"),"Stockpile opens logistics in the current authenticated session");
            check(requests.isEmpty(),"Opening stockpile does not close or mutate the server session");
            checkBounds(screen);
        });
        context.waitTicks(2);ResearchScreenGameTest.gui2Screenshot(context,"hoi-production-stockpile-logistics");
        context.runOnClient(client -> IndustryScreen.clearPreferences());
    }
    private static void conversion(ClientGameTestContext context,IndustryView v) {
        var source=v.equipment().stream().filter(e->e.id().equals("hoi:infantry_gear")).findFirst().orElseThrow();
        var target=v.equipment().stream().filter(e->e.id().equals("hoi:modern_infantry_gear")).findFirst().orElseThrow();
        var issued=new IndustryView.Conversion(source.id(),List.of(new IndustryView.Choice(source.id(),source.name(),source.texture())),0,1.0,1.8,Map.of("IRON",1.0));
        var line=new IndustryView.Line("conversion",target.id(),1,10,.5,0,.4,0,issued);
        var view=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),v.equipment(),List.of(line),v.partners(),v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),null,"",v.navalRepairs());
        var requests=new ArrayList<IndustryProtocol.Request>();
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,view.session(),view,requests::add));
        context.waitTicks(3);ResearchScreenGameTest.gui2Screenshot(context,"hoi-production-conversion");
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.runOnClient(client->check(((IndustryScreen)client.gui.screen()).children().stream().noneMatch(widget->widget instanceof Button b && Set.of("전환 변경","재고 전환").contains(b.getMessage().getString())),"Conversion opens from the equipment image without a separate row button"));
        click(context,target.name()+" · 장비·전환 변경");
        context.waitTicks(2);ResearchScreenGameTest.gui2Screenshot(context,"hoi-production-conversion-picker");
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        click(context,"신규 장비 생산 · 진행 중인 1개분의 작업량 초기화");
        context.runOnClient(client->{var request=requests.getLast();
            check(request.action()==IndustryProtocol.Action.CONVERSION&&request.item().equals(line.id())&&request.other().isEmpty()&&request.revision()==view.revision(),"Conversion mode change uses the issued line and revision");
        });
    }
    private static net.minecraft.client.gui.components.Checkbox obsoleteCheckbox(IndustryScreen screen) {
        return screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Checkbox)
                .map(w -> (net.minecraft.client.gui.components.Checkbox) w).findFirst().orElseThrow();
    }
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
