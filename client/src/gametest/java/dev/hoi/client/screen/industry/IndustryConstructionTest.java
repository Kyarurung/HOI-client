package dev.hoi.client.screen.industry;

import dev.hoi.client.ResearchScreenTest;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class IndustryConstructionTest {
    public static void run(ClientGameTestContext context,CountryHud hud) {
        var requests=new ArrayList<IndustryConstructionPayloads.Request>();String token=UUID.randomUUID().toString();
        context.setScreen(()->new IndustryConstruction(token,null,requests::add));
        context.runOnClient(client->{
            var screen=(IndustryConstruction)client.gui.screen();screen.open();
            for(int i=0;i<400;i++)screen.tick();
            if(requests.size()!=1)throw new AssertionError("Missing initial response must not retry forever");
            if(screen.children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("다시 시도")))throw new AssertionError("Timeout must not add a retry button");
        });
        context.waitTicks(2);
        context.runOnClient(client->{for(int i=0;i<200;i++)client.gui.screen().tick();});
        ResearchScreenTest.gui2Screenshot(context,"hoi-construction-timeout");
        context.setScreen(()->null);requests.clear();
        var buildings=new ArrayList<IndustryConstructionView.Building>();
        for(String id:List.of("infrastructure","air_base","anti_air","radar","military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","rocket_site","nuclear_reactor","power_plant","energy_farm","conversion","fort","coastal_fort","port","hub","railway","dam","dam_mountain"))
            buildings.add(new IndustryConstructionView.Building(id,buildingName(id),"construction/"+id,!Set.of("dam","dam_mountain","refinery","rocket_site").contains(id),"좌클릭: 건설 · Shift+좌클릭: 최대 건설\n우클릭: 취소 · Shift+우클릭: 전체 취소",id.equals("refinery")?"§e합성 정제소§r\n§c£trigger_no 기술 필요 (합성 가스 실험)§r\n\n이 건물은 해당 주에 다음을 제공함: §e+1§r £resources_strip|3\n\n정유 공장당 연료 획득량: §a+48.00§r\n\n현재 연료 획득 혜택: §a0%§r\n\n건설 비용: £civ_factory §e14500§r (보유 중인 §e합성 정제소§r 레벨당 §e+1450§r)":null));
        var projects=new ArrayList<IndustryConstructionView.Project>();
        for(int i=0;i<16;i++)projects.add(new IndustryConstructionView.Project("p"+i,"state"+i,i%2==0?"civilian_factory":"military_factory",i%2==0?"경상북도":"서울",i==0?16:1,i==0?15:i==1?10:0,2500,90,10800,"다음 완성: 0:00, 16 3월, 2020\n\n§e민간공장§r(은)는 §e2500§r/§e10800§r (§e23%§r) 완료되었습니다.\n\n공장 생산량: §e5.00§r\n\n건설 속도: §e90.00§r\n§e15§r개의 공장을 통한 생산량: §e75.00§r\n건설 속도 변동치:\n£stability_texticon 안정도: §a+7.25%§r\n발전소: §a+20.00%§r\n\n주 기반시설: x§e1.60§r\n\n건설 비용: £civ_factory §e10800§r"));
        for(int i=0;i<projects.size();i++) {
            var p=projects.get(i);
            String building=i==2?"nuclear_reactor":i==3?"power_plant":i==4?"energy_farm":p.building();
            String name=buildingName(building);
            String title=p.factories()>0?"§e"+p.name()+"§r에 §e"+name+"§r §e"+p.levels()+"§r개를 건설 중":"§e"+name+"§r §e"+p.levels()+"§r개가 건설 대기열에 있습니다.";
            String days="다음 건물까지 남은 일수: "+(p.factories()>0?"§e93§r":"§c불가§r")+"\n모든 건물 완공까지 남은 일수: "+(p.factories()>0?"§e"+(i==0?1742:93)+"§r":"§c불가§r");
            String description=switch(building) {
                case "nuclear_reactor","power_plant","energy_farm" -> "이 건물은 국가에 §a+"+(i==2?12:i==3?6:4)+"§r £resources_strip|7을 제공하며, "+(i==2?"§c-4§r £resources_strip|6":i==3?"§c-4§r £resources_strip|1 및 §c-1§r £resources_strip|5":"§c-2§r £resources_strip|5")+"을 소비합니다.\n\n\n§c이 주는 §e"+(i==2?"발전소§c나 §e에너지 농장":i==3?"원자로§c나 §e에너지 농장":"원자로§c나 §e발전소")+"§c을 건설할 수 없는 주입니다!§r\n\n주의 최대 공장: §e0§r\n주의 최대 공장: §e0%§r\n현지 건설 속도: §e0%§r"+(i==2?"\n핵 생산: §e0%§r":"");
                case "military_factory" -> "이 건물은 §c군수공장 유지비§r를 대가로 주에 신규 군사 장비 생산을 위한 산업을 제공합니다. 필요한 경우 §e민간공장§r으로 전환될 수 있습니다.";
                default -> "이 건물은 해당 주에 민간 상품의 생산이나 새로운 건물을 짓기 위한 산업력, 그리고 현재 §e사업 가치§r 및 §e법인세율§r에 따라 각각 §aGDP§r와 §a세금 수입§r을 제공합니다. 필요한 경우 §e군수공장§r으로 전환될 수 있습니다.\n\n강철 소비량: §c+2§r";
            };
            projects.set(i,new IndustryConstructionView.Project(p.id(),p.state(),building,p.name(),p.levels(),p.factories(),p.progress(),p.daily(),p.cost(),p.tooltip(),title+"\n"+days+"\n\n"+description));
        }
        var summary=new IndustryConstructionView.Summary(43,7,7,25,4,0,4,1.31,10,21.862500000000004,2,
                "§e£construction_speed_texticon 건설 속도§r\n자유무역: §a+20.00%§r\n건설 기술: §a+5.00%§r\n£stability_texticon 안정도: §a+6.00%§r\n\n모든 건물의 건설 속도 변동치.",
                "사용 가능한 §e에너지§r: §e10§r\n\n특별 프로젝트의 요구량: §e0§r\n생산 라인의 요구량: §e22§r\n부족분: §c12§r\n\n£energy_consumption_texticon 공장 에너지 소비량\n복합산업: §c+30.0%§r\n공동부유: §a-20.0%§r\n\n§a클릭하여 무역 화면으로 이동§r\n\n전기 에너지는 현대 산업, 기반시설, 일상생활의 근본적인 동력원입니다. 이는 제조, 통신, 운송, 사회 전반의 필수 서비스 운영에 필요합니다.",
                "§e소비재§r\n현재 우리의 경제 정책에 따라, 국민들은 §e53§r 민간 및 군사 공장의 §e14.0%§r가 소비재를 생산할 것으로 기대하고 있습니다. 이 수요는 §e43§r개의 민간 공장에서 충족되며, 이로 인해 §e36§r개의 공장이 남게 됩니다. 여기서 §e36§r는 무역에 사용할 수 있습니다.\n\n민간 및 군수공장: §e53§r\n필요한 소비재 공장: §e7§r\n기본 기대치:\n  전시 경제: §e20%§r\n요인:\n  산업 보정: §e-30.0%§r\n배수 요인 합계: §e70%§r",
                "§e사용 중인 공장§r\n미사용: §e0§r\n\n총합: §e43§r\n  보유량: §e43§r\n\n사용 중: §c-43§r\n  소비재: §c-7§r\n  건설: §c-25§r\n  수리: §c-4§r\n  무역 수입: §c-2§r\n  국가 계획: §c-5§r",5,"§e국가 계획§r\n외국 정보: §c5§r\n\n다른 요소로부터 비롯된 국가 건설 계획","§e무역 상품§r\n타국에 민간공장의 사용권을 파는 대가로 전략 자원을 받는 무역을 할 수 있습니다. 해외 무역에는 수송선이 필요합니다.\n\n§e중화인민공화국§r: §e강철§r §a16§r개를 위한 민간공장 §c2§r개");
        var view=new IndustryConstructionView(token,1,"KOR",hud,"civilian_factory",summary,buildings,projects,"");
        var ref=new AtomicReference<IndustryConstruction>();
        context.setScreen(()->{var screen=new IndustryConstruction(token,view,requests::add);ref.set(screen);return screen;});
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));context.waitTicks(3);
        context.runOnClient(client->{
            var locked=buildingButton(ref.get(),"refinery");
            if(locked.active)throw new AssertionError("Research-locked building remains disabled");
            var icon=dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(Tooltips.styled(net.minecraft.network.chat.Component.literal("§c£trigger_no 기술 필요§r"),false));
            if(icon.getString().codePoints().noneMatch(c->c>=0xe000&&c<=0xf8ff))throw new AssertionError("Research requirement cross must resolve to an original glyph");
            ref.get().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(locked.getX()+2,locked.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
            if(!requests.isEmpty())throw new AssertionError("Disabled building cannot send a selection request");
        });
        int headerY=2*(dev.hoi.client.ui.hud.HudMenuBar.height(1280)+39);
        for(int column=0;column<3;column++) {
            int x=34+column*66;
            context.getInput().setCursorPos(x*2,headerY);context.waitTicks(3);
            context.takeScreenshot("hoi-construction-header-"+column+"-gui2");
        }
        context.getInput().setCursorPos(80,2*(dev.hoi.client.ui.hud.HudMenuBar.height(1280)+85));context.waitTicks(3);
        context.takeScreenshot("hoi-construction-consumer-source-gui2");
        for(int row=0;row<2;row++) {
            context.getInput().setCursorPos(80,2*(dev.hoi.client.ui.hud.HudMenuBar.height(1280)+112+27*row));context.waitTicks(3);
            context.takeScreenshot("hoi-construction-reservation-"+row+"-gui2");
        }
        for(String group:List.of("infrastructure","military_factory","hub")) {
            var position=context.computeOnClient(client->{var b=buildingButton(ref.get(),group);return new int[]{b.getX()+10,b.getY()-12};});
            context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);
            context.takeScreenshot("hoi-construction-group-"+group+"-gui2");
        }
        for(int column=0;column<4;column++) {
            int[] centers={47,103,140,181};
            context.getInput().setCursorPos(centers[column]*2,2*(dev.hoi.client.ui.hud.HudMenuBar.height(1280)+64));context.waitTicks(3);
            context.takeScreenshot("hoi-construction-column-"+column+"-gui2");
        }
        for(String name:List.of("↑","↓")) {
            var position=context.computeOnClient(client->{var b=(Button)ref.get().children().stream().filter(w->w instanceof Button button&&button.active&&button.getMessage().getString().equals(name)).findFirst().orElseThrow();return new int[]{b.getX()+7,b.getY()+8};});
            context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(8);
            context.takeScreenshot("hoi-construction-priority-"+(name.equals("↑")?"up":"down")+"-gui2");
        }
        var cursor=context.computeOnClient(client->{var b=buildingButton(ref.get(),"refinery");return new int[]{b.getX()+b.getWidth()/2,b.getY()+b.getHeight()/2};});
        context.getInput().setCursorPos(cursor[0]*2,cursor[1]*2);context.waitTicks(8);context.takeScreenshot("hoi-construction-research-locked-gui2");
        context.getInput().setCursorPos(80,2*(dev.hoi.client.ui.hud.HudMenuBar.height(1280)+162));context.waitTicks(3);context.takeScreenshot("hoi-construction-source-project-tooltip-gui2");
        for(int row:new int[]{0,2,3,4}) {
            context.getInput().setCursorPos(206,2*(dev.hoi.client.ui.hud.HudMenuBar.height(1280)+170+29*row));context.waitTicks(3);
            context.takeScreenshot("hoi-construction-task-"+row+"-gui2");
        }
        context.runOnClient(client->{
            var old=buildings.stream().filter(b->b.id().equals("refinery")).findFirst().orElseThrow();
            int index=buildings.indexOf(old);
            buildings.set(index,new IndustryConstructionView.Building(old.id(),old.name(),old.texture(),true,old.reason(),"§e합성 정제소§r\n클릭하여 건설을 시작하십시오."));
            ref.get().update(new IndustryConstructionView(token,2,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            if(!buildingButton(ref.get(),"refinery").active)throw new AssertionError("Research unlock activates the existing palette button");
        });
        context.getInput().setCursorPos(cursor[0]*2,cursor[1]*2);context.waitTicks(8);context.takeScreenshot("hoi-construction-research-unlocked-gui2");
        context.getInput().setCursorPos(800,500);context.getInput().resizeWindow(1600,1000);
        context.waitTicks(3);context.takeScreenshot("hoi-construction-queue");
        ResearchScreenTest.gui2Screenshot(context,"hoi-construction-queue");
        context.runOnClient(client -> ref.get().update(new IndustryConstructionView(token,3,"KOR",hud,"civilian_factory",
                new IndustryConstructionView.Summary(43,7,2,30,4,0,4,1.31,30,21.8625,2),buildings,projects,"")));
        context.waitTicks(2); context.takeScreenshot("hoi-construction-energy-surplus");
        context.runOnClient(client -> ref.get().update(new IndustryConstructionView(token,4,"KOR",hud,"civilian_factory",
                new IndustryConstructionView.Summary(43,7,2,30,4,0,4,1.31,10,10,2),buildings,projects,"")));
        context.waitTicks(2); context.takeScreenshot("hoi-construction-energy-zero");
        context.runOnClient(client -> ref.get().update(new IndustryConstructionView(token,5,"KOR",hud,"civilian_factory",summary,buildings,projects,"")));
        context.getInput().setCursorPos(40,190); context.waitTicks(2); context.takeScreenshot("hoi-construction-multiline-tooltip");
        context.runOnClient(client -> {
            var lines=Tooltips.lines(client.font,"첫 줄\n사용 가능한 에너지 10 / 필요량 21.86\n긴 설명 "+"읽기 쉬운 설명 ".repeat(40),427);
            if(lines.size()<4)throw new AssertionError("Tooltips split explicit lines and wrap long text");
            for(var line:lines) {
                if(client.font.width(line)>300)throw new AssertionError("Tooltip width is bounded");
                line.accept((index,style,codepoint)->{if(codepoint==10||codepoint==13)throw new AssertionError("Never render newline control glyphs");return true;});
            }
        });
        context.getInput().setCursorPos(800,500);
        context.runOnClient(client->{
            for(String art:java.util.stream.Stream.concat(buildings.stream().map(IndustryConstructionView.Building::texture).filter(t->!t.isBlank()),java.util.stream.Stream.of("construction/refinery_disabled","construction/rocket_site_disabled","keywords/construction_technology_required","construction/consumer_goods","construction/energy","construction/energy_deficit","construction/summary/civilian_factory","construction/summary/civilian_factory_repair","construction/conversion_background","construction/conversion_civilian","construction/conversion_military","construction/ui/entry","construction/ui/special_entry","construction/ui/group_state","construction/ui/group_shared","construction/ui/group_province","construction/ui/national_projects","construction/ui/trade_goods")).toList())
                if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+art+".png")).isEmpty())throw new AssertionError("Missing construction art: "+art);
            var screen=ref.get();if(screen.panelWidth()!=UiPanelLayout.width(MenuTab.CONSTRUCTION,screen.width))throw new AssertionError("Construction sidebar ratio");
            assertNoDams(screen);
            var stateBottom=List.of("infrastructure","air_base","anti_air","radar").stream().map(id->buildingButton(screen,id)).mapToInt(b->b.getY()+b.getHeight()).max().orElseThrow();
            var sharedTop=buildingButton(screen,"군수공장").getY();
            var sharedBottom=buildingButton(screen,"conversion").getY()+buildingButton(screen,"conversion").getHeight();
            if(buildingButton(screen,"rocket_site").getY()!=buildingButton(screen,"nuclear_reactor").getY()
                    ||buildingButton(screen,"power_plant").getY()!=buildingButton(screen,"energy_farm").getY()
                    ||buildingButton(screen,"conversion").getY()<=buildingButton(screen,"power_plant").getY())throw new AssertionError("Original drone/reactor, power/energy, conversion order");
            var localTop=buildingButton(screen,"hub").getY();
            var hub=buildingButton(screen,"hub");var rail=buildingButton(screen,"railway");
            var port=buildingButton(screen,"port");var fort=buildingButton(screen,"fort");var coast=buildingButton(screen,"coastal_fort");
            if(hub.getY()!=rail.getY()||hub.getX()>=rail.getX()||port.getY()!=fort.getY()||port.getX()>=fort.getX()||port.getY()<=hub.getY()||coast.getY()<=port.getY())
                throw new AssertionError("Province palette order: hub/railway, port/fort, coastal fort");
            if(sharedTop-stateBottom<7||localTop-sharedBottom<7)throw new AssertionError("State, shared, and local buildings need separate rows and divider space");
            var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("민간공장")).findFirst().orElseThrow();
            button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            if(requests.stream().filter(r->r.action()!=IndustryConstructionPayloads.Action.REFRESH).count()!=1||requests.getLast().action()!=IndustryConstructionPayloads.Action.SELECT)throw new AssertionError("Building must issue a server selection");
            screen.update(new IndustryConstructionView(token,6,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            var plus=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("+")).findFirst().orElseThrow();
            plus.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            if(requests.getLast().action()!=IndustryConstructionPayloads.Action.REPAIR||requests.getLast().amount()!=5)throw new AssertionError("Repair factory control must issue a bounded server request");
            screen.update(new IndustryConstructionView(token,7,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            for(int mouseButton:new int[]{com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT}) {
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(screen.panelWidth()+50,screen.height/2.0,
                        new net.minecraft.client.input.MouseButtonInfo(mouseButton,0)),false);
                var last=requests.getLast();
                if(last.action()!=(mouseButton==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT?IndustryConstructionPayloads.Action.PLACE:IndustryConstructionPayloads.Action.CANCEL_AT))
                    throw new AssertionError("Left click builds and right click cancels on the map");
                if(Math.abs(last.dx()*last.dx()+last.dy()*last.dy()+last.dz()*last.dz()-1)>.001)
                    throw new AssertionError("Map input sends a normalized ray, not a trusted province");
                screen.update(new IndustryConstructionView(token,last.revision()+1,"KOR",hud,"civilian_factory",summary,buildings,projects,""));
            }
            screen.mouseScrolled(30,170,0,-100);
        });
        context.waitTicks(2);context.takeScreenshot("hoi-construction-scrolled");
        context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-construction-compact");
        context.getInput().setCursorPos(40,190); context.waitTicks(2); context.takeScreenshot("hoi-construction-compact-tooltip");
        context.getInput().setCursorPos(500,400);
        context.runOnClient(client->{
            var screen=ref.get();
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("↑")))throw new AssertionError("Last queue row must remain accessible after compact resize");
            for(var w:screen.children())if(w instanceof Button b && (b.getX()<0||b.getY()<0||b.getX()+b.getWidth()>screen.width||b.getY()+b.getHeight()>screen.height))throw new AssertionError("Construction control escaped viewport");
            screen.mouseScrolled(screen.panelWidth()-8,170,0,-100);
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("해안 요새")))throw new AssertionError("Last local building must remain accessible by palette scrolling");
            assertNoDams(screen);
        });
        context.waitTicks(2);context.takeScreenshot("hoi-construction-palette-scrolled");
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        context.runOnClient(client->{
            var camera=client.gameRenderer.mainCamera();var forward=camera.forwardVector();
            var center=camera.position().add(forward.x()*12,forward.y()*12,forward.z()*12);
            var states=List.of(new IndustryConstructionView.ConversionState("test-state","경기도",4056,center.x,center.y,center.z,5,4));
            ref.get().update(new IndustryConstructionView(token,1000,"KOR",hud,"conversion",summary,buildings,projects,"",states));
        });
        context.waitTicks(3);ResearchScreenTest.gui2Screenshot(context,"hoi-construction-conversion");
        context.runOnClient(client->{
            var screen=ref.get();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(screen.width/2.0-37,screen.height/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
            var request=requests.getLast();
            if(request.action()!=IndustryConstructionPayloads.Action.CONVERT||request.amount()!=0||!request.item().equals("test-state"))throw new AssertionError("Conversion civilian icon sends civilian target for the issued state");
        });
        context.runOnClient(client->{
            ref.get().update(new IndustryConstructionView(token,0,"",null,"",null,List.of(),List.of(),"권한 만료"));
            if(client.gui.screen()!=null)throw new AssertionError("Revoked construction must close");
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }
    private static String buildingName(String id) { return switch(id) {case "infrastructure"->"기반시설";case "air_base"->"공군기지";case "anti_air"->"대공포";case "radar"->"레이더";case "military_factory"->"군수공장";case "civilian_factory"->"민간공장";case "dockyard"->"해군 조선소";case "office_park"->"사무단지";case "refinery"->"합성 정제소";case "fuel_silo"->"연료 저장고";case "rocket_site"->"드론 발사장";case "nuclear_reactor"->"원자로";case "power_plant"->"발전소";case "energy_farm"->"에너지 농장";case "conversion"->"전환";case "fort"->"요새";case "coastal_fort"->"해안 요새";case "port"->"해군기지";case "hub"->"보급 중심지";case "railway"->"철도";case "dam","dam_mountain"->"댐";default->id;}; }
    private static Button buildingButton(IndustryConstruction screen,String name) {
        return (Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(buildingName(name))).findFirst().orElseThrow();
    }
    private static void assertNoDams(IndustryConstruction screen) {
        if(screen.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().equals("댐")))
            throw new AssertionError("Neither dam type belongs in the construction palette");
    }
}
