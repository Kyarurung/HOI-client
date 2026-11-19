package dev.hoi.client.screen.industry;

import dev.hoi.client.ResearchScreenTest;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class IndustryScreenChecks {
    public static IndustryView tankFixture(String file) {
        String directory = System.getenv("HOI_PRODUCTION_PREVIEW");
        if (directory != null && (file.equals("production-source-fixture.json") || file.equals("production-china-fixture.json") || file.equals("production-train-upgrade-fixture.json"))) {
            try { return new IndustryPayloads.Response(java.nio.file.Files.readString(java.nio.file.Path.of(directory, file))).view(); }
            catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        return bundledFixture(file);
    }
    static IndustryView bundledFixture(String file) {
        try(var in=IndustryScreenChecks.class.getResourceAsStream("/"+file)) {
            return new IndustryPayloads.Response(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8)).view();
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
    }
    private static void branchDesigners(ClientGameTestContext context) {
        var requests=new ArrayList<IndustryPayloads.Request>();
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        var missionFixture=tankFixture("air-mission-effects-fixture.json");
        context.runOnClient(client->{
            var design=missionFixture.tankDesigner();
            String naval=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.missionTooltip(design,"naval_bomber");
            check(naval.contains("+7.5")&&naval.contains("(평균)"),"Mixed armaments display the server average and original average suffix");
            check(naval.split("\\n").length==5&&!naval.contains("\n\n"),"Mission tooltip contains one title and four aggregate stat rows");
            check(!dev.hoi.client.ui.industry.IndustryEquipmentDesigns.missionTooltip(design,"naval_patrol").contains("\n"),"Patrol without source modifiers has only its title");
            for(var equipment:missionFixture.equipment())if(!equipment.group().equals("air"))check(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionRoleIcon(equipment).isEmpty(),"Non-air equipment has no resource-adjacent role icon");
        });
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,missionFixture.session(),missionFixture,requests::add));context.waitTicks(2);
        for(String mission:List.of("naval_bomber","port_strike","attack_logistics","naval_patrol")) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"설계 조정치 · "+mission);return new double[]{2*(b.getX()+b.getWidth()/2.0),2*(b.getY()+b.getHeight()/3.0)};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);context.takeScreenshot("hoi-air-effects-"+mission+"-gui2");
        }
        for(String group:List.of("navy","air")) {
            var fixture=tankFixture("initial-"+group+"-fixture.json");
            check(fixture.equipment().stream().anyMatch(e->e.group().equals(group)&&e.designer()!=null&&!e.id().startsWith("hoi:design/")&&dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionDesign(e).editable()),"Initial national equipment exposes an editable design");
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
            context.runOnClient(client->{
                checkBounds((IndustryScreen)client.gui.screen());
                for(var slot:fixture.tankDesigner().slots())if(!slot.selected().isEmpty())check(slot.choices().stream().anyMatch(part->part.id().equals(slot.selected())),"Installed initial modules remain visible in their choices");
            });
            context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-initial-"+group+"-designer-gui2");
        }
        for(String id:List.of("ship_hull_light_1","ship_hull_cruiser_2","ship_hull_carrier_2","ship_hull_carrier_conversion_ca","ship_hull_submarine_3","ship_hull_heavy_2","ship_hull_super_heavy_1","small_plane_airframe_3","large_plane_airframe_1","medium_plane_airframe_1","cv_small_plane_airframe_3")) {
            var fixture=tankFixture("branch-"+id+"-fixture.json");
            check(!fixture.tankDesigner().family().isEmpty(),"Designer family is supplied independently of production-list eligibility");
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();checkBounds(screen);
                var labels=fixture.tankDesigner().slots().stream().map(slot->slot.name()+(slot.required()?" · 필수":"")+slot.choices().stream().filter(part->part.id().equals(slot.selected())).map(part->"\n"+part.name()).findFirst().orElse("")).toList();
                long count=screen.children().stream().filter(widget->widget instanceof Button b&&labels.contains(b.getMessage().getString())).count();
                check(count==fixture.tankDesigner().slots().size(),"Every source slot remains visible and interactive: "+id);
                check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/design/blueprints/"+id+".png")).isPresent(),"Each chassis resolves its source blueprint");
            });
            context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-designer-"+id+"-gui2");
        }
        for(String id:List.of("ship_hull_light_1","small_plane_airframe_1")) {
            var fixture=tankFixture("branch-ready-"+id+"-fixture.json");boolean navy=id.startsWith("ship");
            var production=fixture.withTankDesigner(null);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,production.session(),production,requests::add));context.waitTicks(2);
            click(context,navy?"함선 건조":"항공기 제작");context.waitTicks(2);
            context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-production-"+(navy?"navy":"air")+"-blueprint-gui2");
            click(context,navy?"함선 설계사":"항공기 설계사");
            check(requests.getLast().action()==IndustryPayloads.Action.TANK_OPEN,"Branch list opens its issued chassis designer");
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
            context.takeScreenshot("hoi-designer-"+id+"-ready-gui2");
            if(!navy) {
                check(!fixture.tankDesigner().missions().isEmpty(),"Server supplies source mission profiles");
                var mission=fixture.tankDesigner().missions().getFirst();int count=requests.size();
                click(context,"설계 조정치 · "+mission.id());context.waitTicks(2);
                check(requests.size()==count,"Mission preview does not send a gameplay mutation");
                var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"설계 조정치 · "+mission.id());return new double[]{2*(b.getX()+b.getWidth()/2.0),2*(b.getY()+b.getHeight()/3.0)};});
                context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
                context.takeScreenshot("hoi-designer-air-mission-gui2");
                click(context,"항공기 역할 · 주무장 선택");context.waitTicks(2);
                var primary=fixture.tankDesigner().slots().stream().filter(item->item.id().equals("fixed_main_weapon_slot")).findFirst().orElseThrow();
                var choice=primary.choices().getFirst();
                if(context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryIds(fixture.tankDesigner().chassis(),primary.id()).size()>1)) {
                    String category=context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryName(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(choice.id())));
                    click(context,"모듈 분류: "+category);context.waitTicks(2);
                }
                click(context,choice.name());
                check(requests.getLast().action()==IndustryPayloads.Action.TANK_MODULE&&requests.getLast().other().equals(choice.id()),"Role field selects the issued main armament");
                context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
            }
            var slot=fixture.tankDesigner().slots().getFirst();
            String label=slot.name()+(slot.required()?" · 필수":"")+slot.choices().stream().filter(part->part.id().equals(slot.selected())).map(part->"\n"+part.name()).findFirst().orElse("");
            click(context,label);context.waitTicks(2);context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
            context.takeScreenshot("hoi-designer-"+(navy?"navy":"air")+"-modules-gui2");
            var part=slot.choices().getFirst();
            if(context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryIds(fixture.tankDesigner().chassis(),slot.id()).size()>1)) {
                String category=context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryName(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(part.id())));
                click(context,"모듈 분류: "+category);context.waitTicks(2);
            }
            if(!navy)context.runOnClient(client->check(client.gui.screen().children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("장착 모듈 제거")),"Air module chooser omits the remove button"));
            {
                var hover=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),part.name());return new double[]{(b.getX()+b.getWidth()/2.0)*2,(b.getY()+b.getHeight()/2.0)*2};});
                context.getInput().setCursorPos(hover[0],hover[1]);context.waitTicks(5);
                context.takeScreenshot("hoi-"+(navy?"naval":"air")+"-module-tooltip-gui2");
            }
            click(context,part.name());
            check(requests.getLast().action()==IndustryPayloads.Action.TANK_MODULE&&requests.getLast().other().equals(part.id()),"Module picker submits the issued part");
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
            click(context,"설계 저장 · "+(navy?"해군":"공군")+" 경험치 "+fixture.tankDesigner().experience());
            check(requests.getLast().action()==IndustryPayloads.Action.TANK_SAVE,"Save uses the branch experience control");
            for(int[] dimensions:List.of(new int[]{1600,1000},new int[]{1280,720})) {
                context.getInput().resizeWindow(dimensions[0],dimensions[1]);
                context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
                context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
            }
            context.getInput().resizeWindow(2560,1440);
        }
        context.getInput().resizeWindow(1600,1000);
    }
    private static void longNames(ClientGameTestContext context,IndustryView base) {
        String name="사용자가 직접 지정한 매우 긴 이름의 장비와 사단 편제 이름을 끝까지 보존하는 테스트";
        var gson=new com.google.gson.Gson();var json=gson.toJsonTree(base).getAsJsonObject();
        for(var row:json.getAsJsonArray("equipment"))row.getAsJsonObject().addProperty("name",name);
        for(var row:json.getAsJsonArray("templates"))row.getAsJsonObject().addProperty("name",name);
        if(json.has("navalRepairs")&&!json.get("navalRepairs").isJsonNull())for(var row:json.getAsJsonObject("navalRepairs").getAsJsonArray("ships"))row.getAsJsonObject().addProperty("name",name);
        var fixture=gson.fromJson(json,IndustryView.class);
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
        for(var tab:List.of(MenuTab.PRODUCTION,MenuTab.RECRUITMENT)) {
            context.setScreen(()->new IndustryScreen(tab,fixture.session(),fixture,request->{}));context.waitTicks(2);
            context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
            context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-long-names-"+tab.name().toLowerCase(Locale.ROOT)+"-gui2");
            if(tab==MenuTab.PRODUCTION) {
                for(String category:List.of("보병 및 포병 장비 제작","기갑 차량 제작","함선 건조","항공기 제작")) {
                    context.setScreen(()->new IndustryScreen(tab,fixture.session(),fixture,request->{}));context.waitTicks(1);
                    click(context,category);context.waitTicks(2);context.takeScreenshot("hoi-long-equipment-picker-"+category+"-gui2");
                }
                context.setScreen(()->new IndustryScreen(tab,fixture.session(),fixture,request->{}));context.waitTicks(1);
                click(context,"해군 수리 대기열");click(context,"수리 목록 펼치기/접기");context.waitTicks(2);context.takeScreenshot("hoi-long-repair-ships-gui2");
            }
        }
        context.getInput().resizeWindow(1600,1000);
    }
    private static void tankStatTooltips(ClientGameTestContext context,IndustryView view) {
        context.runOnClient(client->{
            for(var stat:view.tankDesigner().stats()) {
                String text=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statTooltip(stat);
                check(!text.contains("$"),"Source tooltip references are resolved: "+stat.id());
                check(text==dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statTooltip(stat),"Unchanged snapshots reuse formatted stat descriptions");
                if(!stat.contributions().isEmpty())check(text.contains("\n\n"),"Source contributions have an empty paragraph separator");
                if(stat.change()!=0&&!stat.id().equals("build_cost_ic"))check(text.contains("변경 사항이 저장되지 않음:"),"Unsaved delta is derived from the installed design");
            }
            var stat=new IndustryTankDesigner.Stat("fuel_consumption","연료 사용량",2,false,List.of(new IndustryTankDesigner.Contribution("엔진",2)),.05);
            String fuel=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statTooltip(stat);
            check(fuel.contains("§c+2.00")&&fuel.contains("§c+0.05"),"Higher fuel use is red with source precision");
            String text="§e관통\n§f설명\n\n§a+170.0";
            var lines=dev.hoi.client.ui.tooltip.Tooltips.lines(client.font,text,1280);
            check(lines==dev.hoi.client.ui.tooltip.Tooltips.lines(client.font,text,1280),"Repeated hover reuses wrapped text");
            dev.hoi.client.ui.tooltip.Tooltips.clear();
            check(lines!=dev.hoi.client.ui.tooltip.Tooltips.lines(client.font,text,1280),"Resource reload invalidates wrapped text");
        });
        String[][] keys={{"maximum_speed","reliability","supply_consumption"},{"soft_attack","hard_attack","ap_attack","hardness","armor_value","breakthrough","defense","air_attack"},{"fuel_capacity","fuel_consumption","suppression","recon","entrenchment"}};
        var bounds=context.computeOnClient(client->{var screen=(IndustryScreen)client.gui.screen();int w=Math.min(screen.width-24,Math.max(780,screen.width*5/8));return new int[]{(screen.width-w)/2,(screen.height-w*553/1092)/2,w};});
        for(int col=0;col<keys.length;col++)for(int row=0;row<keys[col].length;row++) {
            context.getInput().setCursorPos(2*(bounds[0]+(543+col*175+80)*bounds[2]/1092),2*(bounds[1]+(83+row*20)*bounds[2]/1092));
            context.waitTicks(3);context.takeScreenshot("hoi-tank-stat-"+keys[col][row]+"-gui2");
        }
        for(int[] target:List.of(new int[]{76,0},new int[]{331,1},new int[]{456,2},new int[]{1010,3})) {
            context.getInput().setCursorPos(2*(bounds[0]+target[0]*bounds[2]/1092),2*(bounds[1]+522*bounds[2]/1092));
            context.waitTicks(15);context.takeScreenshot("hoi-tank-footer-"+List.of("reset","experience","save","cost").get(target[1])+"-gui2");
        }
        context.getInput().setCursorPos(2400,1300);
    }
    private static void tankDesigner(ClientGameTestContext context) {
        var empty=tankFixture("tank-empty-fixture.json");var ready=tankFixture("tank-designer-fixture.json");
        var requests=new ArrayList<IndustryPayloads.Request>();
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        var installed=tankFixture("tank-installed-fixture.json");
        var replaced=tankFixture("tank-replaced-fixture.json");
        var reset=tankFixture("tank-reset-fixture.json");
        check(installed.tankDesigner().equals(reset.tankDesigner()),"Reset response restores installed modules, upgrades and stats");
        check(!replaced.tankDesigner().equals(reset.tankDesigner()),"Replacement draft differs from the installed baseline");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,replaced.session(),replaced,requests::add));context.waitTicks(2);
        click(context,"설계 초기화");
        check(requests.getLast().action()==IndustryPayloads.Action.TANK_RESET,"Reset submits the authoritative baseline restore request");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,reset.session(),reset,requests::add));context.waitTicks(2);
        context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-tank-reset-installed-gui2");
        var production=ready.withTankDesigner(null);
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,production.session(),production,requests::add));context.waitTicks(3);
        click(context,"기갑 차량 제작");context.waitTicks(2);
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-production-tank-blueprint-gui2");
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var blueprint=production.equipment().stream().filter(e->e.id().startsWith("hoi:chassis/modern_tank_")&&e.unlocked()&&e.replacement().isEmpty()).findFirst().orElseThrow();
            var row=button(screen,"생산: "+blueprint.name());
            var designer=screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("전차 설계사")&&b.getY()>=row.getY()&&b.getY()<row.getBottom()).map(w->(Button)w).findFirst().orElseThrow();
            check(row.active&&designer.active,"Issued chassis exposes its row and designer entry points");
            designer.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
        });
        check(requests.getLast().action()==IndustryPayloads.Action.TANK_OPEN&&requests.getLast().item().startsWith("hoi:chassis/"),"Blueprint equipment retains the issued chassis designer request");
        requests.clear();
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,empty.session(),empty,requests::add));context.waitTicks(3);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();checkBounds(screen);
            check(screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().contains("필수 모듈")).anyMatch(w->!((Button)w).active),"Unfinished tank cannot be saved");
        });
        context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-tank-designer-unfinished-gui2");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,ready.session(),ready,requests::add));context.waitTicks(3);
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.takeScreenshot("hoi-tank-designer-ready-gui2");
        tankStatTooltips(context,ready);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var adjusters=screen.children().stream().filter(widget->widget instanceof Button b&&b.getMessage().getString().startsWith("설계 조정치 · ")).map(widget->(Button)widget).toList();
            check(adjusters.size()==11,"Tank designer exposes all eleven environment adjusters");
            var rows=adjusters.stream().collect(java.util.stream.Collectors.groupingBy(Button::getY,java.util.TreeMap::new,java.util.stream.Collectors.counting()));
            check(new ArrayList<>(rows.values()).equals(List.of(5L,5L,1L)),"Environment adjusters use five/five/one rows");
        });
        click(context,"전차 역할 선택");context.waitTicks(2);context.takeScreenshot("hoi-tank-roles-gui2");
        click(context,"전차 역할 선택");context.waitTicks(2);
        for(String environment:List.of("plains","forest","hills","mountain","urban","jungle","marsh","desert","fort","river","amphibious")) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"설계 조정치 · "+environment);return new double[]{b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/3.0};});
            context.getInput().setCursorPos(point[0]*2,point[1]*2);context.waitTicks(3);
            context.takeScreenshot("hoi-tank-adjuster-"+environment+"-gui2");
        }
        context.runOnClient(client->{
            var engine=ready.tankDesigner().upgrades().getFirst();
            var armor=ready.tankDesigner().upgrades().getLast();
            String engineText=IndustryScreen.tankUpgradeChanges(engine,1),armorText=IndustryScreen.tankUpgradeChanges(armor,1);
            check(IndustryScreen.tankValue(new IndustryTankDesigner.Stat("maximum_speed","",9.085,false)).equals("9.0km/h"),"Source design speed truncates to one decimal");
            check(IndustryScreen.tankValue(new IndustryTankDesigner.Stat("reliability","",.8865,true)).equals("88.6%"),"Source design reliability truncates after percent conversion");
            check(IndustryScreen.tankValue(new IndustryTankDesigner.Stat("armor_value","",24.75,false)).equals("24.7"),"Source armor preview retains native precision");
            check(IndustryScreen.tankValue(new IndustryTankDesigner.Stat("build_cost_ic","",5.519999999999999,false)).equals("5.52"),"Binary arithmetic residue does not reduce a displayed cost");
            check(engineText.contains("-1.5%")&&engineText.contains("0.10")&&engineText.contains("0.05")&&engineText.contains("0.1km/h"),"Engine tooltip uses source upgrade coefficients before design multipliers");
            check(armorText.contains("-1.5%")&&armorText.contains("0.20")&&armorText.contains("2.5")&&armorText.contains("1.2")&&armorText.contains("-0.1km/h"),"Armor tooltip preserves native coefficients and truncated breakthrough precision");
            check(IndustryScreen.tankUpgradeChanges(engine,-1).contains("1.5%")&&IndustryScreen.tankUpgradeChanges(engine,5).contains("-7.5%"),"Upgrade tooltips retain signed decrease and multi-level coefficients");
            check(engine.steps().stream().filter(step->step.amount()==1).flatMap(step->step.changes().stream()).anyMatch(stat->stat.id().equals("build_cost_ic")&&Math.abs(stat.value()-.08)<1e-8),"Actual preview still uses common design multipliers instead of raw tooltip coefficients");
        });
        var enginePoint=context.computeOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var plus=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(ready.tankDesigner().upgrades().getFirst().name()+" 증가")).findFirst().orElseThrow();
            return new double[]{plus.getX()+plus.getWidth()/2.0,plus.getY()+plus.getHeight()/2.0,plus.getWidth()*37.5/29};
        });
        context.getInput().setCursorPos(enginePoint[0]*2,enginePoint[1]*2);context.waitTicks(5);
        context.takeScreenshot("hoi-tank-engine-upgrade-tooltip-gui2");
        context.getInput().setCursorPos((enginePoint[0]-enginePoint[2])*2,enginePoint[1]*2);context.waitTicks(5);
        context.takeScreenshot("hoi-tank-engine-level-tooltip-gui2");
        var armorPoint=context.computeOnClient(client->{
            var plus=button((IndustryScreen)client.gui.screen(),ready.tankDesigner().upgrades().getLast().name()+" 증가");
            return new double[]{plus.getX()+plus.getWidth()/2.0,plus.getY()+plus.getHeight()/2.0,plus.getWidth()*37.5/29};
        });
        context.getInput().setCursorPos(armorPoint[0]*2,armorPoint[1]*2);context.waitTicks(5);
        context.takeScreenshot("hoi-tank-armor-upgrade-tooltip-gui2");
        context.getInput().setCursorPos((armorPoint[0]-armorPoint[2])*2,armorPoint[1]*2);context.waitTicks(5);
        context.takeScreenshot("hoi-tank-armor-level-tooltip-gui2");
        context.getInput().setCursorPos(2400,1300);context.waitTicks(3);
        var slot=ready.tankDesigner().slots().stream().filter(s->s.id().equals("main_armament_slot")).findFirst().orElseThrow();
        String label=slot.name()+" · 필수"+slot.choices().stream().filter(p->p.id().equals(slot.selected())).map(p->"\n"+p.name()).findFirst().orElse("");
        var slotPoint=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),label);return new double[]{b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0};});
        context.getInput().setCursorPos(slotPoint[0]*2,slotPoint[1]*2);context.waitTicks(4);context.takeScreenshot("hoi-tank-slot-tooltip-gui2");
        click(context,label);context.waitTicks(3);
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.takeScreenshot("hoi-tank-module-categories-gui2");
        var part=slot.choices().getFirst();
        String category=context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryName(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(part.id())));
        int beforeCategory=requests.size();click(context,"모듈 분류: "+category);context.waitTicks(3);
        check(requests.size()==beforeCategory,"Choosing a module category does not mutate the design");
        context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-tank-modules-gui2");
        var partPoint=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),part.name());return new double[]{b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0};});
        context.getInput().setCursorPos(partPoint[0]*2,partPoint[1]*2);context.waitTicks(5);context.takeScreenshot("hoi-tank-module-tooltip-gui2");
        context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-tank-module-preview-restored-gui2");
        check(requests.size()==beforeCategory,"Hovering a replacement restores the installed preview without submitting a module change");
        click(context,"모듈 선택 뒤로");context.waitTicks(2);
        click(context,"모듈 분류: "+category);context.waitTicks(2);click(context,part.name());
        check(requests.getLast().action()==IndustryPayloads.Action.TANK_MODULE&&requests.getLast().item().equals(slot.id())&&requests.getLast().other().equals(part.id()),"Module selection uses the server-issued slot and part");
        for(String slotId:List.of("special_type_slot_1","suspension_type_slot","armor_type_slot","engine_type_slot","special_type_slot_5","turret_type_slot")) {
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,ready.session(),ready,requests::add));context.waitTicks(2);
            var current=ready.tankDesigner().slots().stream().filter(entry->entry.id().equals(slotId)).findFirst().orElseThrow();
            String currentLabel=current.name()+(current.required()?" · 필수":"")+current.choices().stream().filter(entry->entry.id().equals(current.selected())).map(entry->"\n"+entry.name()).findFirst().orElse("");
            click(context,currentLabel);context.waitTicks(2);context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-tank-categories-"+slotId+"-gui2");
            var candidate=slotId.equals("special_type_slot_1")?current.choices().stream().filter(entry->entry.id().equals("auto_loader")).findFirst().orElseThrow():current.choices().getFirst();
            String moduleCategory=context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryName(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(candidate.id())));
            int beforeList=requests.size();
            boolean categorized=slotId.equals("special_type_slot_1");
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();
                check(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("장착 모듈 제거")),"Armored module list has no removal button");
                check(screen.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().startsWith("모듈 분류: "))==categorized,"Only categorized slots open a classification screen");
            });
            if(categorized)click(context,"모듈 분류: "+moduleCategory);
            context.waitTicks(2);context.takeScreenshot("hoi-tank-parts-"+slotId+"-gui2");
            var candidatePoint=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),candidate.name());return new double[]{b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0};});
            context.getInput().setCursorPos(candidatePoint[0]*2,candidatePoint[1]*2);context.waitTicks(4);context.takeScreenshot("hoi-tank-tooltip-"+slotId+"-gui2");
            click(context,"모듈 선택 뒤로");context.waitTicks(2);
            check(requests.size()==beforeList,"Opening, hovering and leaving the list does not change installed modules");
            if(!categorized) {
                context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),currentLabel).active,"Back returns directly to the designer"));
                click(context,currentLabel);context.waitTicks(2);click(context,candidate.name());
                check(requests.getLast().action()==IndustryPayloads.Action.TANK_MODULE&&requests.getLast().item().equals(slotId)&&requests.getLast().other().equals(candidate.id()),"Direct list still selects the server-issued module");
            }
        }
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,ready.session(),ready,requests::add));context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var plus=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(ready.tankDesigner().upgrades().getFirst().name()+" 증가")).findFirst().orElseThrow();
            plus.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
        });
        check(requests.getLast().action()==IndustryPayloads.Action.TANK_UPGRADE&&requests.getLast().amount()==1,"Engine upgrade sends a bounded level request");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,ready.session(),ready,requests::add));context.waitTicks(2);
        int beforeManufacturer=requests.size();
        check(ready.tankDesigner().designTeam()==null,"Legacy fixture has no design-team contract");
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var manufacturer=button(screen,"전차 설계사 · 군수산업체");
            check(!manufacturer.active,"Manufacturer selection without a server contract is disabled");
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(manufacturer.getX()+manufacturer.getWidth()/2.0,manufacturer.getY()+manufacturer.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
        });
        check(requests.size()==beforeManufacturer,"Manufacturer selection without a server contract does not send gameplay requests");
        click(context,"설계 저장 · 육군 경험치 "+ready.tankDesigner().experience());
        check(requests.getLast().action()==IndustryPayloads.Action.TANK_SAVE&&requests.getLast().item().equals(ready.tankDesigner().name()),"Save retains the source design name");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,empty.session(),empty,requests::add));context.waitTicks(3);
        var emptySlot=empty.tankDesigner().slots().stream().filter(entry->entry.id().equals("main_armament_slot")).findFirst().orElseThrow();
        check(emptySlot.selected().isEmpty(),"Module preview starts from an empty main armament slot");
        context.runOnClient(client->{
            var rifled=emptySlot.choices().stream().filter(entry->entry.id().equals("tank_heavy_cannon")).findFirst().orElseThrow();
            for(String key:List.of("hard_attack","ap_attack")) {
                String name=key.equals("hard_attack")?"대물 공격":"관통";
                String token=key.equals("hard_attack")?"hard_attack_texticon":"piercing_texticon";
                check(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statName(key).contains(name),"Zero-valued tank stats retain Korean source labels: "+key);
                check(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statCaption(key,key).contains("£"+token),"Tank stat tooltip uses its TFR modifier icon: "+key);
                check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/design/stats/"+key+".png")).isPresent(),"Tank stat texture exists: "+key);
                var change=rifled.changes().stream().filter(stat->stat.id().equals(key)).findFirst().orElseThrow();
                check(change.name().contains(name)&&change.value()==(key.equals("hard_attack")?20.0:90.0),"Empty-slot rifled gun preview retains source hard attack and piercing values: "+key);
            }
        });
        click(context,emptySlot.name()+" · 필수");context.waitTicks(2);
        click(context,"모듈 분류: "+category);context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var back=button(screen,"모듈 선택 뒤로");
            int minY=dev.hoi.client.ui.hud.HudMenuBar.height(screen.width)+client.font.lineHeight+8;
            int w=Math.min(Math.min(screen.width-24,Math.max(780,screen.width*5/8)),(screen.height-minY-12)*1092/553);
            int x=(screen.width-w)/2,y=Math.max(minY,(screen.height-w*553/1092)/2);
            check(back.getX()==x+406*w/1092&&back.getY()==y+71*w/1092
                    &&back.getWidth()==123*w/1092&&back.getHeight()==36*w/1092,"Back button retains the original sprite position and dimensions");
        });
        context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-tank-empty-module-preview-gui2");
        int beforeHover=requests.size();
        for(var candidate:emptySlot.choices().stream().filter(entry->entry.id().equals("tank_heavy_cannon")||entry.id().equals("tank_high_velocity_cannon")).toList()) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),candidate.name());return new double[]{b.getX()+b.getWidth()/2.0,b.getY()+b.getHeight()/2.0};});
            context.getInput().setCursorPos(point[0]*2,point[1]*2);context.waitTicks(4);
            context.takeScreenshot("hoi-tank-empty-module-preview-"+candidate.id()+"-gui2");
        }
        context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-tank-empty-module-preview-restored-gui2");
        check(requests.size()==beforeHover,"Empty-slot hover previews never install equipment or send mutation requests");
        for(int generation=2;generation<=5;generation++) {
            var variant=tankFixture("tank-generation-"+generation+"-fixture.json");
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,variant.session(),variant,requests::add));context.waitTicks(2);
            int currentGeneration=generation;
            context.runOnClient(client->{
                var input=(net.minecraft.client.gui.components.EditBox)client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();
                check(input.getScreenX(0)==input.getX()&&input.getCursorPosition()==0,"Tank name starts at the left edge, including its caret");
            });
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();checkBounds(screen);
                int minY=dev.hoi.client.ui.hud.HudMenuBar.height(screen.width)+client.font.lineHeight+8;
                int w=Math.min(Math.min(screen.width-24,Math.max(780,screen.width*5/8)),(screen.height-minY-12)*1092/553);
                int h=w*553/1092,x=(screen.width-w)/2,y=Math.max(minY,(screen.height-h)/2);
                for(int extra=6;extra<=7;extra++) {
                    int bx=x+(extra==6?487:47)*w/1092,by=y+(extra==6?169:219)*w/1092;
                    long buttons=screen.children().stream().filter(widget->widget instanceof Button b&&b.getX()<=bx&&b.getRight()>bx&&b.getY()<=by&&b.getBottom()>by&&b.active).count();
                    check(buttons==(currentGeneration>=extra-2?1:0),"Only source-unlocked slots accept clicks for generation "+currentGeneration);
                }
            });
            context.getInput().setCursorPos(2400,1300);context.takeScreenshot("hoi-tank-generation-"+generation+"-gui2");
            if(generation==5)context.takeScreenshot("hoi-tank-left-name-compact-cost-gui2");
            if(generation>=4) {
                String extra=generation==4?"special_type_slot_6":"special_type_slot_7";
                var extraSlot=variant.tankDesigner().slots().stream().filter(v->v.id().equals(extra)).findFirst().orElseThrow();
                context.runOnClient(client->{
                    var screen=(IndustryScreen)client.gui.screen();
                    var buttons=screen.children().stream().filter(widget->widget instanceof Button b&&b.active&&b.getMessage().getString().equals(extraSlot.name())).map(widget->(Button)widget).toList();
                    var target=generationSlot(buttons,extra.equals("special_type_slot_7"));
                    target.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                });
                context.waitTicks(2);
                var module=extraSlot.choices().getFirst();
                String extraCategory=context.computeOnClient(client->dev.hoi.client.ui.industry.IndustryEquipmentDesigns.categoryName(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleCategory(module.id())));
                click(context,"모듈 분류: "+extraCategory);context.waitTicks(2);click(context,module.name());
                check(requests.getLast().action()==IndustryPayloads.Action.TANK_MODULE&&requests.getLast().item().equals(extra),"Extra slot submits its own server-issued identity");
            }
        }
        for(int[] size:List.of(new int[]{1600,1000},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,ready.session(),ready,requests::add));context.waitTicks(2);
            context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        }
        context.getInput().resizeWindow(1600,1000);
    }
    public static void designers(ClientGameTestContext context) {
        IndustryProductionPresentationTest.run(context);
        branchDesigners(context);
        context.runOnClient(client->{
            var stats=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleEffects("ship_anti_air_1");
            check(stats.contains("§e1.5")&&stats.contains("§a+10.0%")&&stats.contains("§c-0.5%"),"Installed naval modules distinguish base values and signed modifiers");
            check(!stats.contains("kn")&&!stats.contains("km/h"),"Multipliers never include speed units");
            String category=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.shipCategoryTooltip("screen");
            check(category.split("\n\n").length==3&&category.contains("§e보조함§f")&&category.contains("\n\n§e장비 모듈 선택은"),"Screen ship tooltip retains all three source paragraphs and yellow emphasis");
            String personnel=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.statTooltip(new IndustryTankDesigner.Stat("manpower","인력",250,false));
            check(!personnel.contains("\n")&&personnel.startsWith("§f"),"Personnel tooltip is the original white caption only");
        });
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        var army=tankFixture("tank-designer-fixture.json");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,army.session(),army,request->{}));context.waitTicks(2);
        int[] armyColumns=context.computeOnClient(client->client.gui.screen().children().stream().filter(widget->widget instanceof Button b&&b.getMessage().getString().startsWith("설계 조정치 · ")).map(widget->(Button)widget).limit(5).mapToInt(Button::getX).toArray());
        var naval=tankFixture("branch-ready-ship_hull_light_1-fixture.json");
        check(dev.hoi.client.ui.hud.HudMenuBar.indicators(naval.hud()).stream().filter(indicator->indicator.icon().endsWith("_experience")).map(dev.hoi.client.ui.hud.HudMenuBar.HudIndicator::icon).toList().equals(List.of("army_experience","navy_experience","air_experience")),"Menu experience order is army, navy, air");
        check(naval.tankDesigner().adjustments().stream().anyMatch(adjustment->adjustment.effects().stream().anyMatch(stat->stat.id().equals("naval_mine_hit_chance")&&stat.value()==-.5)),"Naval environment effects come from the server snapshot");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,naval.session(),naval,request->{}));context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            var adjusters=screen.children().stream().filter(widget->widget instanceof Button b&&b.getMessage().getString().startsWith("설계 조정치 · ")).map(widget->(Button)widget).toList();
            for(int i=0;i<3;i++)check(adjusters.get(i).getX()==armyColumns[i],"Naval adjusters start at the upper left on the armor column baselines");
            check(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.moduleEffects("ship_anti_air_1").contains("£navprod_cost"),"Naval module costs always use navprod_cost");
        });
        var categoryPoint=context.computeOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            int minY=dev.hoi.client.ui.hud.HudMenuBar.height(screen.width)+client.font.lineHeight+8;
            int w=Math.min(Math.min(screen.width-24,Math.max(780,screen.width*5/8)),(screen.height-minY-12)*1092/553);
            int h=w*553/1092,x=(screen.width-w)/2,y=Math.max(minY,(screen.height-h)/2);
            return new double[]{2*(x+494*w/1092),2*(y+73*w/1092)};
        });
        context.getInput().setCursorPos(categoryPoint[0],categoryPoint[1]);context.waitTicks(5);
        context.takeScreenshot("hoi-naval-screen-tooltip-gui2");
        for(String environment:List.of("water_fjords","water_shallow_sea","water_deep_ocean")) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"설계 조정치 · "+environment);return new double[]{(b.getX()+b.getWidth()/2.0)*2,(b.getY()+b.getHeight()/3.0)*2};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("hoi-naval-adjuster-"+environment+"-gui2");
        }
        for(int badge=1;badge<=2;badge++) {
            int index=badge;
            var point=context.computeOnClient(client->{
                var b=button((IndustryScreen)client.gui.screen(),"설계 조정치 · water_deep_ocean");
                return new double[]{2*(b.getX()+b.getWidth()*(index==1?42.0:62.0)/84),2*(b.getY()+b.getHeight()*50.0/67)};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("hoi-naval-badge-"+(badge==1?"combat":"environment")+"-gui2");
        }
        for(String file:List.of("tank-designer-fixture.json","branch-ready-ship_hull_light_1-fixture.json","branch-ready-small_plane_airframe_1-fixture.json")) {
            var fixture=tankFixture(file);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,request->{}));context.waitTicks(2);
            context.runOnClient(client->check(client.gui.screen().children().stream().noneMatch(widget->widget instanceof Button button&&(button.getMessage().getString().contains("파일로 저장")||button.getMessage().getString().contains("저장된 설계"))),"All designers omit file preset controls"));
        }
    }
    private static Button generationSlot(List<Button> buttons,boolean secondRow) {
        return buttons.stream().max(secondRow?Comparator.comparingInt(Button::getY):Comparator.comparingInt(Button::getX)).orElseThrow();
    }
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
        var json = new com.google.gson.Gson().toJsonTree(v).getAsJsonObject();
        var equipment = json.getAsJsonArray("equipment").get(0).getAsJsonObject();
        var costs = new com.google.gson.JsonObject();
        costs.addProperty("IRON", 2); costs.addProperty("RUBBER", 2); costs.addProperty("TUNGSTEN", 2);
        equipment.add("resources", costs);
        var lines = new com.google.gson.JsonArray();
        for (int i = 0; i < 3; i++) {
            var line = new IndustryView.Line("resource-check-" + i, equipment.get("id").getAsString(), 15, 150, .5, 5, .2,
                    i == 1 ? .5 : 0, null, i == 2 ? 0 : 15, 0, .001, .001, i == 1 ? .5 : 0,
                    i == 1 ? java.util.Set.of("RUBBER") : java.util.Set.of());
            lines.add(new com.google.gson.Gson().toJsonTree(line));
        }
        json.add("lines", lines);
        var fixture = new IndustryPayloads.Response(json.toString()).view();
        var shortage = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), resources, v.modifiers(), fixture.equipment(), fixture.lines(),
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
                int y = dev.hoi.client.ui.hud.HudMenuBar.height(screen.width) + 29;
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
            var geometry = context.computeOnClient(client -> {
                var screen = (IndustryScreen)client.gui.screen();
                float scale = (screen.panelWidth() - 22) / 511f;
                int x = 6 + Math.round(462 * scale);
                return new float[]{scale, x + Math.round(5 * scale), x + Math.round(48 * scale), dev.hoi.client.ui.hud.HudMenuBar.height(screen.width) + 147,
                        Math.round((screen.panelWidth() - 22) * 116f / 511) + 2, x + Math.round(25 * scale)};
            });
            var model = fixture.equipment().getFirst();
            for (int i = 0; i < 2; i++) {
                int row = 0;
                for (String resource : model.resources().keySet()) {
                    int y0 = (int)geometry[3] + i * (int)geometry[4] + Math.round((31 + row++ * 24) * geometry[0]);
                    int iconRed = 0, numberRed = 0;
                    for (int y = y0 * 2; y < (y0 + Math.round(24 * geometry[0])) * 2; y++)
                        for (int x = (int)geometry[1] * 2; x < (int)geometry[2] * 2; x++) {
                            int pixel = image.getRGB(x, y), red = pixel >> 16 & 255, green = pixel >> 8 & 255, blue = pixel & 255;
                            if (x < (int)geometry[5] * 2) {
                                if (red > 100 && red > green + 20 && red > blue + 20) iconRed++;
                            } else if ((pixel & 0xFFFFFF) == dev.hoi.client.ui.tooltip.Tooltips.BAD) numberRed++;
                        }
                    boolean missing = i == 1 && resource.equals("RUBBER");
                    check(missing ? iconRed > 5 && numberRed > 5 : iconRed == 0 && numberRed == 0,
                            "Only the deficient line resource has original red icon and TFR red number: " + i + ":" + resource);
                }
            }
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
    }
    public static void productionPickerDetails(ClientGameTestContext context) {
        var data=new com.google.gson.Gson().toJsonTree(tankFixture("industry-upgrade-fixture.json")).getAsJsonObject();
        data.getAsJsonArray("equipment").forEach(row->row.getAsJsonObject().addProperty("unlocked",true));
        var fixture=new IndustryPayloads.Response(data.toString()).view();
        var requests=new ArrayList<IndustryPayloads.Request>();
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        context.runOnClient(client->{
            String retired=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionObsoleteTooltip(false);
            String restored=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionObsoleteTooltip(true);
            check(retired.startsWith("퇴역\n\n§a클릭§f"),"Retirement preserves source title, blank line and green click instruction");
            check(restored.startsWith("재취역\n\n§a클릭§f"),"Recommission preserves source title, blank line and green click instruction");
            check(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("PRODUCTION_CREATE_VARIANT").equals("§e파생형 제작§f"),"Variant title uses source gold");
        });
        for(String category:List.of("보병 및 포병 장비 제작","기갑 차량 제작","항공기 제작","함선 건조")) {
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
            click(context,category);context.waitTicks(2);
            var rowName=context.computeOnClient(client->client.gui.screen().children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().startsWith("생산: ")).map(w->((Button)w).getMessage().getString()).findFirst().orElseThrow());
            for(boolean obsolete:List.of(true,false)) {
                var point=context.computeOnClient(client->{
                    var row=button((IndustryScreen)client.gui.screen(),rowName);
                    return new double[]{(row.getX()+row.getWidth()+13)*client.getWindow().getGuiScale(),(row.getY()+(obsolete?8:row.getHeight()-13))*client.getWindow().getGuiScale()};
                });
                context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
                context.takeScreenshot("hoi-production-"+category+"-"+(obsolete?"retire":"variant")+"-gui2");
            }
            if(category.equals("함선 건조")) {
                var models=new LinkedHashMap<String,IndustryView.Equipment>();
                fixture.equipment().stream().filter(e->e.unlocked()&&e.naval()&&!"convoy".equals(e.family())).forEach(e->models.putIfAbsent(e.family(),e));
                for(var model:models.values()) {
                    click(context,model.familyName()==null?model.name():model.familyName());context.waitTicks(2);
                    context.getInput().setCursorPos(2400,1300);context.waitTicks(2);
                    context.runOnClient(client->{
                        var screen=(IndustryScreen)client.gui.screen();checkBounds(screen);
                        var visible=screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().startsWith("생산: ")).map(w->((Button)w).getMessage().getString()).sorted().toList();
                        var expected=fixture.equipment().stream().filter(e->e.naval()&&e.unlocked()&&!e.outdated()&&Objects.equals(e.family(),model.family())).map(e->"생산: "+e.name()).sorted().toList();
                        check(visible.equals(expected),"Naval selection filters only its family: "+model.family());
                    });
                    context.takeScreenshot("hoi-production-naval-filter-"+model.family()+"-gui2");
                }
            }
        }
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));context.waitTicks(2);
        click(context,"보병 및 포병 장비 제작");click(context,"구형 장비 표시");context.waitTicks(2);
        var old=fixture.equipment().stream().filter(IndustryView.Equipment::outdated).findFirst().orElseThrow();
        var point=context.computeOnClient(client->{
            var row=button((IndustryScreen)client.gui.screen(),"생산: "+old.name());
            return new double[]{(row.getX()+row.getWidth()+13)*client.getWindow().getGuiScale(),(row.getY()+8)*client.getWindow().getGuiScale()};
        });
        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);context.takeScreenshot("hoi-production-recommission-gui2");
        var replacements=data.getAsJsonArray("equipment");
        String first=replacements.get(0).getAsJsonObject().get("id").getAsString(),second=replacements.get(1).getAsJsonObject().get("id").getAsString();
        replacements.forEach(row->row.getAsJsonObject().addProperty("replacement",row.getAsJsonObject().get("id").getAsString().equals(first)?second:first));
        var obsoleteFixture=new IndustryPayloads.Response(data.toString()).view();
        check(obsoleteFixture.equipment().stream().allMatch(IndustryView.Equipment::outdated),"Every branch fixture has an obsolete replacement");
        for(String category:List.of("보병 및 포병 장비 제작","기갑 차량 제작","항공기 제작","함선 건조")) {
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,obsoleteFixture.session(),obsoleteFixture,requests::add));context.waitTicks(2);
            click(context,category);click(context,"구형 장비 표시");context.waitTicks(2);
            context.getInput().setCursorPos(2400,1300);context.waitTicks(2);
            context.takeScreenshot("hoi-production-obsolete-mask-"+category+"-gui2");
        }
        check(requests.stream().allMatch(r->r.action()==IndustryPayloads.Action.REFRESH||r.action()==IndustryPayloads.Action.CLOSE),"Tooltip and naval category checks send no gameplay mutations: "+requests);
        for(String file:List.of("tank-designer-fixture.json","branch-ready-ship_hull_light_1-fixture.json","branch-ready-small_plane_airframe_1-fixture.json")) {
            var v=tankFixture(file).withTankDesigner(null);
            String expectedGroup=file.startsWith("tank-")?"armor":file.contains("ship_")?"navy":"air";
            var e=v.equipment().stream().filter(item->item.group().equals(expectedGroup)&&dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionDesign(item).editable()).findFirst().orElseThrow();
            String category=switch(e.group()){case "navy"->"함선 건조";case "air"->"항공기 제작";default->"기갑 차량 제작";};
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,v.session(),v,requests::add));context.waitTicks(2);click(context,category);context.waitTicks(2);
            var design=dev.hoi.client.ui.industry.IndustryEquipmentDesigns.productionDesign(e);
            var position=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),design.title());return new double[]{(b.getX()+b.getWidth()/2.0)*client.getWindow().getGuiScale(),(b.getY()+b.getHeight()/2.0)*client.getWindow().getGuiScale()};});
            context.getInput().setCursorPos(position[0],position[1]);context.waitTicks(5);context.takeScreenshot("hoi-production-modular-"+e.group()+"-variant-gui2");
            click(context,design.title());
            check(requests.getLast().action()==IndustryPayloads.Action.TANK_OPEN,"Modular variant opens the issued designer");
        }
        context.getInput().resizeWindow(1600,1000);
    }
    public static void run(ClientGameTestContext context, CountryHud hud) {
        IndustryForeignLogisticsTest.run(context);
        IndustryProductionPresentationTest.run(context);
        productionPickerDetails(context);
        IndustryView fixture;
        try (var in = IndustryScreenChecks.class.getResourceAsStream("/industry-fixture.json")) {
            fixture = new IndustryPayloads.Response(new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.UTF_8)).view();
        } catch (java.io.IOException e) { throw new RuntimeException(e); }
        var v = copy(fixture, hud, fixture.revision(), null);
        for(var r:v.resources())check(Math.abs(r.extracted()+r.imported()-r.exported()-r.available())<1e-8,"Server fixture retains signed extraction in resource balance: "+r.id());
        var chromium=v.resources().stream().filter(r->r.id().equals("CHROMIUM")).findFirst().orElseThrow();
        check(chromium.extracted()<0&&chromium.imported()>0&&chromium.available()<0,"Partial chromium imports still leave a deficit");
        tankDesigner(context);
        branchDesigners(context);
        longNames(context,v);
        tradeTooltips(context,v);
        var sourceEquipment=new ArrayList<IndustryView.Equipment>();
        var names=List.of("견인포","자주대공포","경기계화","정찰전차","다목적 헬리콥터","대전차 장비","수륙양용장갑차","휴대용 대공 미사일","차량화 로켓포","수륙양용전차");
        var ids=List.of("artillery_equipment","spaa_equipment","light_mechanized_equipment","light_tank_chassis","utility_helicopter_equipment","anti_tank_equipment","amphibious_mechanized_equipment","anti_air_equipment","motorized_rocket_equipment","amphibious_tank_chassis");
        for(int i=0;i<ids.size();i++)sourceEquipment.add(new IndustryView.Equipment("hoi:tfr/equipment/"+ids.get(i),names.get(i),
                "production/equipment/tfr/equipment/"+ids.get(i),"infantry",false,true,1,1,Map.of(),1,0,0,0,"",ids.get(i),names.get(i),0L,true));
        var illustrations=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),sourceEquipment,List.of(),v.partners(),v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),List.of(),List.of(),null,"",v.navalRepairs());
        context.setScreen(()->new IndustryScreen(MenuTab.LOGISTICS,v.session(),illustrations,request->{}));
        ResearchScreenTest.gui2Screenshot(context,"hoi-logistics-source-equipment-images");
        var supplyModels = v.equipment().stream().map(e -> new IndustryView.Equipment(e.id(), e.name(), e.texture(), e.group(), e.naval(), e.unlocked(),
                e.cost(), e.factoryLimit(), e.id().equals("hoi:infantry_gear") ? Map.of("OIL", 1.0, "ALUMINUM", 2.0, "RUBBER", 3.0, "TUNGSTEN", 4.0, "IRON", 5.0, "CHROMIUM", 6.0) : e.resources(), e.stockpile() == 0 ? 50 : e.stockpile(), e.reserved(), e.deployed(), e.deficit(), e.replacement(), e.family(), e.familyName(), 100L)).toList();
        var supply = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), supplyModels, v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), v.recruits(), v.deployed(), v.draft(), v.message(), v.navalRepairs(), v.specialForces());
        context.setScreen(() -> new IndustryScreen(MenuTab.LOGISTICS, supply.session(), supply, request -> {}));
        ResearchScreenTest.gui2Screenshot(context, "hoi-logistics-supply-status");
        context.getInput().resizeWindow(2560, 1440); context.waitTicks(3);
        for (int row : new int[]{0, 1}) {
            var point = context.computeOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                int pane = screen.panelWidth(), x = 6 + (pane - 12) * 83 / 100;
                double scale = client.getWindow().getGuiScale();
                return new double[]{(x + 8) * scale, (dev.hoi.client.ui.hud.HudMenuBar.height(screen.width) + 59 + row * 39) * scale};
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(3);
            context.takeScreenshot(row == 0 ? "hoi-logistics-resource-icon-hover-gui2" : "hoi-logistics-empty-resource-cell-gui2");
        }
        for (int col = 0; col < 8; col++) {
            int column = col;
            var point = context.computeOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                String tooltip = IndustryLogisticsTooltips.header(column);
                check(tooltip.startsWith("§e") && !tooltip.contains("LOGISTICS_"), "Original gold logistics title " + column);
                check(column == 1 ? !tooltip.contains("\n") : tooltip.contains("§f\n\n"), "Original logistics blank line " + column);
                int[] fractions = {0, 5, 29, 41, 55, 64, 74, 83, 100};
                double scale = client.getWindow().getGuiScale();
                return new double[]{(6 + (screen.panelWidth() - 12) * (fractions[column] + fractions[column + 1]) / 200.0) * scale,
                        (dev.hoi.client.ui.hud.HudMenuBar.height(screen.width) + 37) * scale};
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(2);
            context.takeScreenshot("hoi-logistics-header-" + col + "-gui2");
        }
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        var empty = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), List.of(), List.of(), null, "", v.navalRepairs(), v.specialForces());
        conversion(context,v);
        factoryControls(context,v);
        repairPolicies(context,v);
        IndustryNavalDeploymentTest.run(context,v);
        resourceShortages(context, v);
        var requests = new ArrayList<IndustryPayloads.Request>();
        for (var tab : List.of(MenuTab.PRODUCTION, MenuTab.TRADE, MenuTab.LOGISTICS, MenuTab.RECRUITMENT)) {
            context.setScreen(() -> new IndustryScreen(tab, v.session(), tab == MenuTab.RECRUITMENT ? empty : v, requests::add));
            context.waitTicks(3); context.takeScreenshot("hoi-industry-" + tab.id()); ResearchScreenTest.gui2Screenshot(context, "hoi-industry-" + tab.id());
            context.runOnClient(client -> {
                var screen = (IndustryScreen) client.gui.screen();
                check(screen.panelWidth() == UiPanelLayout.width(tab, screen.width), "Original sidebar proportions");
                checkBounds(screen);
                if (tab == MenuTab.LOGISTICS) {
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:font/korean.json")).isEmpty(), "Cancelled Korean font override is absent");
                    check(IndustryScreen.LOGISTICS_HEADERS.equals(List.of("평균 생산 효율","장비 유형","생산","상태","수요","균형","비축량","자원")), "Eight logistics columns follow the supplied order");
                    for (String icon : List.of("efficiency","production","need","balance","stockpile"))
                        check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/logistics/"+icon+".png")).isPresent(), "Original logistics header icon " + icon);
                }
                if (tab == MenuTab.PRODUCTION) {
                    check(screen.repairDockyards().equals("2/3"), "Server-issued repair use and capacity");
                    check(IndustryScreen.repairTime(v.navalRepairs().ships().getFirst()).equals("2일"),"Repair completion estimate appears in the gauge");
                    check(IndustryScreen.repairTime(v.navalRepairs().ships().getLast()).equals("—"),"Returning ships have no invented repair estimate");
                    check(v.navalRepairs().ships().stream().map(IndustryScreen::repairState).collect(java.util.stream.Collectors.toSet()).equals(Set.of("수리 대기","수리 중","귀항 중")), "Queued, repairing and returning damaged ships stay distinct");
                    check(screen.productionIndicators().stream().map(IndustryView.Modifier::id).toList().equals(List.of("dockyard","factory","cap","retention","growth","damage")), "Requested six production indicators remain ordered");
                    check(screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("생산 라인 삭제")).count() >= Math.min(3,v.lines().size()), "Three production lines fit the standard viewport");
                }
                for (String texture : java.util.stream.Stream.concat(v.equipment().stream().map(IndustryView.Equipment::texture),
                        java.util.stream.Stream.concat(v.battalions().stream().map(IndustryView.Choice::texture), v.companies().stream().map(IndustryView.Choice::texture))).toList())
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + texture + ".png")).isPresent(), "Original external asset " + texture);
                for (String path : List.of("resources/oil", "resources/aluminum", "resources/rubber", "resources/tungsten", "resources/iron", "resources/chromium", "resources/coal",
                        "designer/add", "designer/locked", "designer/slot", "units/motorized_infantry", "actions/infantry", "actions/armor", "actions/air", "actions/navy", "actions/repair",
                        "modifiers/dockyard", "modifiers/factory", "modifiers/cap", "modifiers/retention", "modifiers/growth", "modifiers/damage",
                        "summary/military_factory_icon", "summary/dockyard_icon", "summary/dockyard_icon_with_wrench",
                        "picker/land", "picker/naval", "picker/check_on", "picker/check_off", "picker/repair_title", "picker/repair_port", "picker/repair_ship", "picker/repair_state", "picker/repair_org", "picker/repair_hp", "picker/repair_stat_bg", "picker/repair_progress", "picker/repair_progress_bg", "picker/repair_progress_waiting", "picker/repair_progress_returning",
                        "picker/variant", "picker/variant_armor", "picker/variant_air", "picker/variant_navy", "picker/expand", "picker/collapse", "picker/manufacturer", "picker/role_any", "picker/filter_carrier"))
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
        click(context, seller.name() + "에서 수입"); context.waitTicks(2); context.takeScreenshot("hoi-industry-trade-contract"); ResearchScreenTest.gui2Screenshot(context, "hoi-industry-trade-contract");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        context.runOnClient(client -> {
            check(!button((IndustryScreen)client.gui.screen(),"수입 계약 체결").active,"A new zero-quantity contract cannot be sent");
            check(!button((IndustryScreen)client.gui.screen(),"무역 공장 줄이기").active,"Minimum import quantity disables decrease");
            check(IndustryTradeAmounts.neededTradeFactories(20,24,16,8,10)==2,"Auto quantity replaces current imports instead of adding them twice");
            check(IndustryTradeAmounts.neededTradeFactories(20,40,16,8,10)==0,"Surplus supply cancels unnecessary imports");
            check(IndustryTradeAmounts.neededTradeFactories(21,4,0,8,10)==3,"Resource shortage rounds up to complete factories");
            check(IndustryTradeAmounts.neededTradeFactories(100,0,0,8,2)==2,"Auto quantity respects available factories and export limits");
            for(int factories=0;factories<=6;factories++) {
                double amount=Math.min(44,factories*8),position=IndustryTradeAmounts.tradeSliderPosition(factories,44,8);
                check(Math.abs(position-amount/44)<1e-9,"Slider position follows resource share including the partial last factory");
                check(IndustryTradeAmounts.tradeSliderFactories(position,44,8)==factories,"Resource slider steps round-trip through all 44-resource positions");
            }
            check(IndustryTradeAmounts.tradeSliderFactories(1,41,8)==6,"A one-resource remainder still costs the final factory");
            check(IndustryTradeAmounts.convoyCell(0).equals("-")&&IndustryTradeAmounts.convoyCell(9).equals("9")&&IndustryTradeAmounts.convoyCell(null).equals("—"),"Trade convoy cells distinguish land, per-factory sea demand and unknown routes");
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
            slider.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RIGHT,0,0));
        });context.waitTicks(2);
        ResearchScreenTest.gui2Screenshot(context,"hoi-industry-trade-contract-quantity");
        click(context, "수입 계약 체결");
        context.runOnClient(client -> {
            var request = requests.getLast();
            check(request.action() == IndustryPayloads.Action.TRADE && request.item().equals(seller.id()) && request.other().equals("IRON") && request.amount() == 1,
                    "Trade submits the issued seller, resource and civilian factory count");
        });
        var partialPartners=v.partners().stream().map(p->p.id().equals(seller.id())?new IndustryView.Partner(p.id(),p.name(),Map.of("IRON",44.0),p.route(),p.convoys(),p.influence(),8.0,p.freeConvoys(),p.buyers()):p).toList();
        var partial=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),v.equipment(),v.lines(),partialPartners,v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),v.draft(),v.message(),v.navalRepairs(),v.specialForces());
        context.setScreen(()->new IndustryScreen(MenuTab.TRADE,partial.session(),partial,requests::add));context.waitTicks(2);
        click(context,seller.name()+"에서 수입");
        for(int i=0;i<6;i++)click(context,"무역 공장 늘리기");
        ResearchScreenTest.gui2Screenshot(context,"hoi-industry-trade-partial-44");
        context.runOnClient(client->check(!button((IndustryScreen)client.gui.screen(),"무역 공장 늘리기").active,"The last partial resource batch reaches the import limit"));
        click(context,"수입 계약 체결");
        context.runOnClient(client->check(requests.getLast().amount()==6,"Importing all 44 resources reserves six civilian factories"));
        requests.clear();
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, v.session(), v, requests::add)); context.waitTicks(2);
        click(context, "공장 늘리기");
        context.runOnClient(client -> {
            var s = (IndustryScreen)client.gui.screen(); var request = requests.getLast();
            check(request.action() == IndustryPayloads.Action.ASSIGN && request.amount() == v.lines().getFirst().factories() + 1
                    && request.revision() == v.revision(), "Only a bounded authenticated factory request");
            check(!button(s, "공장 늘리기").active, "No duplicate request while waiting");
            s.update(copy(v, hud, v.revision() + 1, null));
        });
        click(context, "보병 및 포병 장비 제작"); context.waitTicks(2); ResearchScreenTest.gui2Screenshot(context,"hoi-industry-equipment-picker");
        context.runOnClient(client -> { var screen=(IndustryScreen)client.gui.screen(); checkBounds(screen); check(button(screen,"함선 건조").active,"Docked picker preserves production actions"); });
        var filteredEquipment=v.equipment().stream().filter(e->e.group().equals("infantry")&&e.unlocked()&&!e.outdated()&&!e.resources().isEmpty()).findFirst().orElseThrow();
        String filteredResource=filteredEquipment.resources().keySet().iterator().next();
        String filterName=v.resources().stream().filter(r->r.id().equals(filteredResource)).findFirst().orElseThrow().name()+" 사용 장비 표시";
        int requestsBeforeFilter=requests.size();
        click(context,filterName);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            check(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("생산: "+filteredEquipment.name())),"Unchecked resource hides equipment requiring that resource");
            check(requests.size()==requestsBeforeFilter,"Resource filters never send production mutations");
        });
        click(context,filterName);
        for (String name : List.of("기갑 차량 제작", "항공기 제작", "함선 건조")) {
            click(context, name); context.waitTicks(1); ResearchScreenTest.gui2Screenshot(context,"hoi-production-picker-"+name);
            context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        }
        click(context, "해군 수리 대기열"); context.waitTicks(2);
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 제주").active,"Empty authoritative repair ports remain visible"));
        ResearchScreenTest.gui2Screenshot(context,"hoi-industry-naval-repairs-collapsed");
        int collapsedY=context.computeOnClient(client->button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY());
        click(context,"수리 항구: 부산");context.waitTicks(2); ResearchScreenTest.gui2Screenshot(context,"hoi-industry-naval-repairs");
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()>collapsedY,"Individual port expands beneath its header"));
        click(context,"수리 목록 펼치기/접기");context.waitTicks(2);
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()>collapsedY,"Global expand opens the remaining ports after individual expansion"));
        ResearchScreenTest.gui2Screenshot(context,"hoi-industry-naval-repairs-expanded");
        click(context,"수리 목록 펼치기/접기");context.waitTicks(2);
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()==collapsedY,"Collapse all restores adjacent port rows"));
        click(context,"수리 항구: 부산");
        for(int column:new int[]{46,235,320}) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"수리 항구: 부산");double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()*column/400.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);context.takeScreenshot("hoi-naval-repair-tooltip-"+column+"-gui2");
        }
        for(int column:new int[]{3,8,310}) {
            var point=context.computeOnClient(client->{
                var b=button((IndustryScreen)client.gui.screen(),"수리 항구: 부산");
                int sx=b.getX()+8,sw=b.getWidth()-9,sy=b.getY()+Math.max(17,Math.round(b.getWidth()*35f/400));
                double scale=client.getWindow().getGuiScale();
                return new double[]{(sx+(column<10?column:sw*column/401.0))*scale,(sy+9)*scale};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("hoi-naval-repair-progress-"+column+"-gui2");
        }
        context.getInput().setCursorPos(1500,1000);
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        context.getInput().resizeWindow(854, 480); context.waitTicks(3); context.takeScreenshot("hoi-industry-compact-naval-repairs");
        context.runOnClient(client -> checkBounds((IndustryScreen)client.gui.screen()));
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        checkGenerations(context,hud);
        context.setScreen(() -> new IndustryScreen(MenuTab.RECRUITMENT, empty.session(), empty, requests::add)); context.waitTicks(2);
        context.runOnClient(client -> check(client.gui.screen().children().stream().noneMatch(w -> w instanceof Button b && b.getMessage().getString().equals("훈련 취소")), "No training rows before a server-issued recruit exists"));
        String template = v.templates().getFirst().name();
        click(context, template + " 훈련");
        context.runOnClient(client -> {
            check(requests.getLast().action() == IndustryPayloads.Action.RECRUIT && requests.getLast().other().isEmpty(), "Training starts with no deployment location");
            ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 1, null));
            check(client.gui.screen().children().stream().anyMatch(w -> w instanceof Button b && b.getMessage().getString().equals("훈련 취소")), "Authoritative recruit response creates a training line");
        });
        context.waitTicks(2); ResearchScreenTest.gui2Screenshot(context, "hoi-recruitment-after-training");
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            var cancel = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().endsWith(" 부대 추가")).findFirst().orElseThrow();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(cancel.getX() - 20, cancel.getY() + cancel.getHeight()/2,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            check(screen.selectingDeployment() && screen.children().isEmpty(), "Location selection hides the client menu");
            check(requests.getLast().action() == IndustryPayloads.Action.SELECT_LOCATION, "Selection starts with an authenticated request");
            screen.update(copy(v, hud, v.revision() + 2, null));
        });
        context.waitTicks(2); context.takeScreenshot("hoi-deployment-menu-hidden");
        context.runOnClient(client -> {
            var screen = (IndustryScreen)client.gui.screen();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(screen.width / 2, screen.height / 2,
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
            check(!screen.selectingDeployment() && !screen.children().isEmpty(), "Map left-click restores recruitment menu");
            check(requests.getLast().action() == IndustryPayloads.Action.LOCATION_AT, "Map click submits the selected ray");
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
        var trainingView = new IndustryView(v.session(), v.revision(), v.country(), v.hud(), v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(), v.battalions(), v.companies(), v.locations(), trainingRows, List.of(), null, "", v.navalRepairs(), v.specialForces(), new IndustryView.RecruitmentPolicy(Set.of(), Map.of("reinforcement", 2, "upgrade", 1, "supply", 1, "operation", 1, "garrison", 1, "raid", 0), .65, Map.of("hoi:missing-reinforcement-label", 26L), null, Map.of("supply", .25, "operation", .5, "garrison", .75, "raid", 1.0)));
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
                check(screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("훈련 취소")).count() == 4, "Collapse hides only the chosen group's training rows");
                for (String asset : List.of("collapse", "expand", "cancel_line", "delete_template", "increase", "decrease", "icon_bg"))
                    check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/recruitment/" + asset + ".png")).isPresent(), "Original recruitment asset " + asset);
            });
            click(context, "클릭하여 펼치기");
            click(context, v.templates().getFirst().name() + " 연속 훈련 횟수 증가");
            context.runOnClient(client -> check(requests.getLast().action() == IndustryPayloads.Action.SERIES && requests.getLast().amount() == 1, "Infinity increments to one, without adding a line"));
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
        context.waitTicks(3); context.takeScreenshot("hoi-industry-division-designer"); ResearchScreenTest.gui2Screenshot(context, "hoi-industry-division-designer");
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
        ResearchScreenTest.gui2Screenshot(context, "hoi-industry-battalion-categories");
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
        ResearchScreenTest.gui2Screenshot(context, "hoi-industry-first-slot-details");
        click(context, v.battalions().stream().filter(c -> c.id().equals("TANK")).findFirst().orElseThrow().name());
        context.runOnClient(client -> {
            check(requests.getLast().action() == IndustryPayloads.Action.LINE_COLUMN && requests.getLast().amount() == 0
                    && requests.getLast().item().equals("TANK"), "Category choice replaces the column through its first slot");
            ((IndustryScreen)client.gui.screen()).update(copy(v, hud, v.revision() + 4, v.templates().getFirst()));
            var s = (IndustryScreen)client.gui.screen();
            var first = button(s, v.battalions().stream().filter(c -> c.id().equals(v.templates().getFirst().line().getFirst())).findFirst().orElseThrow().name());
            var lower = (Button)s.children().stream().filter(w -> w instanceof Button b && b.getX() == first.getX() && b.getY() > first.getY())
                    .min(java.util.Comparator.comparingInt(w -> ((Button)w).getY())).orElseThrow();
            lower.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
            check(s.children().stream().noneMatch(w -> w instanceof Button b && (b.getMessage().getString().equals("기갑 대대")
                    || b.getMessage().getString().equals("기동 대대"))), "Lower slots cannot switch the column category");
            check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/designer/remove.png")).isPresent(), "Original removal image is packaged");
            var controls = List.copyOf(s.children());
            for (int tick = 0; tick < 60; tick++) s.tick();
            check(s.children().equals(controls) && button(s, "제거").active, "Passive refresh keeps button instances and enabled icons");
        });
        context.waitTicks(2); context.takeScreenshot("hoi-industry-battalion-choices");
        ResearchScreenTest.gui2Screenshot(context, "hoi-industry-battalion-choices");
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
        context.takeScreenshot("hoi-industry-compact-designer-before-bounds");
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
            fixture=new IndustryPayloads.Response(new String(Objects.requireNonNull(in).readAllBytes(),StandardCharsets.UTF_8)).view();
        } catch(java.io.IOException e) { throw new RuntimeException(e); }
        var v=copy(fixture,hud,fixture.revision(),null);
        var old=v.equipment().stream().filter(IndustryView.Equipment::outdated).findFirst().orElseThrow();
        var next=v.equipment().stream().filter(e->e.id().equals(old.replacement())).findFirst().orElseThrow();
        var requests=new ArrayList<IndustryPayloads.Request>();
        var rows=IndustryLogisticsRows.create(v.equipment(),v.lines(),"all");
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
            check(button(s,"생산: "+v.equipment().stream().filter(e->e.id().equals("hoi:support_gear")).findFirst().orElseThrow().name()).active,"Infantry image opens the whole infantry and artillery production category");
            check(obsoleteCheckbox(s).active,"Obsolete checkbox starts unchecked inside the picker"); checkBounds(s);
        });
        context.takeScreenshot("hoi-industry-current-model-picker");
        context.runOnClient(client->obsoleteCheckbox((IndustryScreen)client.gui.screen()).onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0)));
        context.waitTicks(2); context.takeScreenshot("hoi-industry-show-outdated-picker");
        context.runOnClient(client->{
            var s=(IndustryScreen)client.gui.screen();
            check(obsoleteCheckbox(s).active,"Obsolete checkbox is checked when old models are visible");
            check(!button(s,"생산: "+old.name()).active,"Current equipment is visible but cannot retool to itself");
            check(requests.isEmpty(),"Changing a display filter sends no simulation request");
        });
        context.runOnClient(client->obsoleteCheckbox((IndustryScreen)client.gui.screen()).onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0)));
        context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            check(obsoleteCheckbox(screen).active,"Second toggle clears the obsolete checkbox");
            check(screen.children().stream().noneMatch(w->w instanceof Button b && b.getMessage().getString().equals("생산: "+old.name())),"Unchecking hides obsolete equipment");
            check(requests.isEmpty(),"Both checkbox toggles stay local");
        });
        context.getInput().resizeWindow(854,480); context.waitTicks(3);
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.takeScreenshot("hoi-industry-compact-model-picker");
        click(context,"생산: "+next.name());
        context.runOnClient(client->{
            var request=requests.getLast();
            check(request.action()==IndustryPayloads.Action.SWITCH && request.item().equals(v.lines().getFirst().id())
                    && request.other().equals(next.id()) && request.revision()==v.revision(),"Replacement uses the existing bounded line command and issued revision");
        });
        context.getInput().resizeWindow(1600,1000); context.waitTicks(3);
    }
    private static void factoryControls(ClientGameTestContext context, IndustryView v) {
        context.runOnClient(client -> IndustryScreen.clearPreferences());
        var gun=v.equipment().stream().filter(e->e.id().equals("hoi:modern_infantry_gear")).findFirst().orElseThrow();
        var sourceGun=new IndustryView.Equipment(gun.id(),gun.name(),gun.texture(),gun.group(),gun.naval(),gun.unlocked(),gun.cost(),gun.factoryLimit(),gun.resources(),gun.stockpile(),gun.reserved(),gun.deployed(),gun.deficit(),gun.replacement(),gun.family(),gun.familyName(),gun.demand(),gun.acquired(),"infantry_equipment_3",null,null,List.of(new IndustryTankDesigner.Stat("defense","방어",1.5,false),new IndustryTankDesigner.Stat("fuel_consumption","연료 사용량",.4,false)));
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
        var requests=new ArrayList<IndustryPayloads.Request>();
        check(IndustryScreen.productionFactoryState(0,1,1,0,0,15,-1)==0,"Operating factories use the normal frame");
        check(IndustryScreen.productionFactoryState(10,1,0,1,0,15,-1)==2,"Damage uses the red frame");
        check(IndustryScreen.productionFactoryState(12,1,0,0,1,15,-1)==3,"Reservations use the dark gray frame");
        check(IndustryScreen.productionFactoryState(20,10,0,0,0,15,30)==1,"Adding factories previews bright icons");
        check(IndustryScreen.productionFactoryState(20,10,10,0,0,40,20)==4,"Cancelling previews warm icons after the target cell");
        check(IndustryScreen.productionFactoryState(10,10,0,2,3,15,-1)==2,"A mixed group uses one complete damage icon");
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));
        context.waitTicks(2);ResearchScreenTest.gui2Screenshot(context,"hoi-production-factory-damage-reservations");
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();checkBounds(screen);
            check(screen.panelWidth()>=275,"Production keeps GUI2 reference cell space when the viewport permits");
            check(screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().matches("(military · 공장|naval · 조선소) [0-9]+칸")).count()==30,"Both production pools expose all fifteen cells");
            check(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().startsWith("naval ×")),"Dockyards have no multiplier buttons");
            for(String icon:List.of("factory_0","factory_1","factory_2","factory_3","dockyard_0","dockyard_1","dockyard_2","dockyard_3","x5_normal","x5_selected","x10_normal","x10_selected","efficiency_frame","efficiency_fill","efficiency_bg","progress_fill","progress_bg"))
                check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/line/"+icon+".png")).isPresent(),"Original production state frame "+icon);
        });
        context.getInput().resizeWindow(2560,1440);context.waitTicks(2);
        java.awt.image.BufferedImage cells;
        try{cells=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-factory-cells-centered-gui2").toFile());}catch(java.io.IOException failure){throw new AssertionError(failure);}
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();double scale=client.getWindow().getGuiScale();
            for(String name:List.of("military · 공장 1칸","naval · 조선소 1칸")){
                var b=button(screen,name);
                int x0=(int)Math.round(b.getX()*scale),y0=(int)Math.round(b.getY()*scale),x1=(int)Math.round((b.getX()+b.getWidth())*scale),y1=(int)Math.round((b.getY()+b.getHeight())*scale);
                int left=x1,right=x0,top=y1,bottom=y0;
                for(int y=y0;y<y1;y++)for(int x=x0;x<x1;x++){int c=cells.getRGB(x,y),r=c>>16&255,g=c>>8&255,bl=c&255;if(Math.max(r,Math.max(g,bl))-Math.min(r,Math.min(g,bl))>16){left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);}}
                check(right>left,"Production cell draws its icon: "+name);
                check(Math.abs(left+right-x0-x1)<=2&&Math.abs(top+bottom-y0-y1)<=2,"Production cell icon is centered: "+name+" icon "+left+","+top+"-"+right+","+bottom+" cell "+x0+","+y0+"-"+x1+","+y1);
            }
        });
        context.runOnClient(client->{
            var text=dev.hoi.client.ui.tooltip.Tooltips.productionText(net.minecraft.network.chat.Component.literal(
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
            String equipmentTip=IndustryProductionTooltips.equipment(issued,lines.getFirst(),sourceGun);
            check(!equipmentTip.contains("원본 기본 능력치")&&equipmentTip.contains("§e1.5§r")&&equipmentTip.contains("§e0.40§r"),"Equipment reference stats retain source precision");
            check(equipmentTip.contains("일일 생산량")&&equipmentTip.contains("공장당 자원 사용:"),"Equipment tooltip uses actual production and resources");
            String gauge=IndustryProductionTooltips.efficiency(issued,lines.getFirst());
            check(gauge.contains("96.38%")&&gauge.contains("0.00%")&&!gauge.contains("0.003%"),"Gauge precision is two decimal places");
            check(gauge.contains("매일 기본 획득량: §e0.01%")&&gauge.contains("자원 부족: §c-64.53%"),"Gauge includes server base gain and shortage with two decimals");
            for(double efficiency:new double[]{.9674,.9674-1e-15,.98}) {
                var capped=new IndustryView.Line("military",gun.id(),15,150,efficiency,10,.2,.8,null,10,2,0.0,0.0,.6453);
                check(!IndustryProductionTooltips.efficiency(issued,capped).contains("(매일"),"Daily gain is hidden at or above the actual efficiency cap");
                String cappedTip=IndustryProductionTooltips.efficiency(issued,capped);
                check(cappedTip.contains("매일 기본 획득량: §e0.00%")&&cappedTip.contains("자원 부족: §c-64.53%"),"Capped efficiency retains base gain and resource modifiers");
            }
            var below=new IndustryView.Line("military",gun.id(),15,150,.967399,10,.2,.8,null,10,2,.000001);
            check(IndustryProductionTooltips.efficiency(issued,below).contains("매일"),"Rounded percentages do not hide growth below the cap");
            check(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/production/tooltip.png")).isPresent(),"Global tooltip uses the original TFR dialog asset");
        });
        for(int indicator=0;indicator<6;indicator++) {
            final int column=indicator;
            var point=context.computeOnClient(client->{var screen=(IndustryScreen)client.gui.screen();int cell=(screen.panelWidth()-12)/6;double scale=client.getWindow().getGuiScale();return new double[]{(6+column*cell+cell/2.0)*scale,(dev.hoi.client.ui.hud.HudMenuBar.height(screen.width)+70)*scale};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(2);
            context.takeScreenshot("hoi-production-modifier-"+List.of("dockyard","factory","cap","retention","growth","damage").get(indicator)+"-gui2");
        }
        var gaugePoint=context.computeOnClient(client->{var screen=(IndustryScreen)client.gui.screen();double scale=client.getWindow().getGuiScale();return new double[]{(6+Math.round(270*(screen.panelWidth()-12)/511f))*scale,(dev.hoi.client.ui.hud.HudMenuBar.height(screen.width)+140)*scale};});
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
        context.runOnClient(client->{check(requests.stream().allMatch(request->request.action()==IndustryPayloads.Action.CLOSE),"Tooltip fixture transitions only close their previous screen");requests.clear();});
        for(int multiplier:new int[]{5,10,1}) {
            click(context,"military ×"+(multiplier==1?10:multiplier));
            check(requests.isEmpty(),"Display multiplier does not mutate production");
            context.waitTicks(2);ResearchScreenTest.gui2Screenshot(context,"hoi-production-factories-x"+multiplier);
        }
        click(context,"military ×10");
        context.getInput().resizeWindow(2560,1440);context.waitTicks(2);
        for(int hovered:new int[]{3,1}) {
            var point=context.computeOnClient(client->{var b=button((IndustryScreen)client.gui.screen(),"military · 공장 "+hovered+"칸");double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(2);
            context.takeScreenshot(hovered==3?"hoi-production-factory-add-hover-gui2":"hoi-production-factory-cancel-hover-gui2");
        }
        context.getInput().setCursorPos(700,700);context.getInput().resizeWindow(1600,1000);context.waitTicks(2);
        click(context,"military ×10");
        click(context,"military · 공장 15칸");
        context.runOnClient(client->{var r=requests.getLast();check(r.action()==IndustryPayloads.Action.ASSIGN&&r.item().equals("military")&&r.amount()==15&&r.revision()==issued.revision(),"X1 allows reserving fifteen factories despite only twelve owned");});
        for(int multiplier:new int[]{5,10}) {
            requests.clear();context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);
            click(context,"military ×"+multiplier);
            context.setScreen(()->null);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);
            ResearchScreenTest.gui2Screenshot(context,"hoi-production-reopened-x"+multiplier);
            click(context,"military · 공장 3칸");
            context.runOnClient(client->check(requests.getLast().amount()==3*multiplier,"Factory cell applies its selected multiplier"));
        }
        requests.clear();context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);
        click(context,"naval · 조선소 15칸");context.runOnClient(client->check(requests.getLast().amount()==15&&requests.getLast().item().equals("naval"),"Dockyard cells reserve one per cell"));
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,issued.session(),issued,requests::add));context.waitTicks(1);requests.clear();
        context.runOnClient(client->check(client.gui.screen().children().stream().noneMatch(w->w instanceof Button button&&button.getMessage().getString().equals(ship.name()+" · 재고량 · 군수 보기")),"Dockyard production quantities remain hidden"));
        click(context,gun.name()+" · 재고량 · 군수 보기");
        context.runOnClient(client->{
            var screen=(IndustryScreen)client.gui.screen();
            check(screen.getTitle().getString().equals("군수"),"Stockpile opens logistics in the current authenticated session");
            check(requests.isEmpty(),"Opening stockpile does not close or mutate the server session");
            checkBounds(screen);
        });
        context.waitTicks(2);ResearchScreenTest.gui2Screenshot(context,"hoi-production-stockpile-logistics");
        context.runOnClient(client -> IndustryScreen.clearPreferences());
    }
    private static void conversion(ClientGameTestContext context,IndustryView v) {
        var source=v.equipment().stream().filter(e->e.id().equals("hoi:infantry_gear")).findFirst().orElseThrow();
        var target=v.equipment().stream().filter(e->e.id().equals("hoi:modern_infantry_gear")).findFirst().orElseThrow();
        var issued=new IndustryView.Conversion(source.id(),List.of(new IndustryView.Choice(source.id(),source.name(),source.texture())),0,1.0,1.8,Map.of("IRON",1.0));
        var line=new IndustryView.Line("conversion",target.id(),1,10,.5,0,.4,0,issued);
        var view=new IndustryView(v.session(),v.revision(),v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),v.equipment(),List.of(line),v.partners(),v.trades(),v.templates(),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),null,"",v.navalRepairs());
        var requests=new ArrayList<IndustryPayloads.Request>();
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,view.session(),view,requests::add));
        context.waitTicks(3);ResearchScreenTest.gui2Screenshot(context,"hoi-production-conversion");
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.runOnClient(client->check(((IndustryScreen)client.gui.screen()).children().stream().noneMatch(widget->widget instanceof Button b && Set.of("전환 변경","재고 전환").contains(b.getMessage().getString())),"Production rows have no conversion controls"));
        click(context,target.name()+" · 장비 교체");
        context.waitTicks(2);ResearchScreenTest.gui2Screenshot(context,"hoi-production-conversion-picker");
        context.runOnClient(client->checkBounds((IndustryScreen)client.gui.screen()));
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"구형 장비 표시").active,"Conversion-capable line also opens the category picker first"));
        context.runOnClient(client->check(((IndustryScreen)client.gui.screen()).children().stream().noneMatch(widget->widget instanceof Button b && b.getMessage().getString().contains("전환")),"Equipment picker has no stock conversion button"));
        click(context,"구형 장비 표시");
        click(context,"생산: "+source.name());
        context.runOnClient(client->{var request=requests.getLast();
            check(request.action()==IndustryPayloads.Action.SWITCH&&request.item().equals(line.id())&&request.other().equals(source.id())&&request.revision()==view.revision(),"Equipment replacement retains the issued line and revision");
            check(requests.stream().noneMatch(r->r.action()==IndustryPayloads.Action.CONVERSION),"Production UI never requests stock conversion");
        });
    }
    private static Button obsoleteCheckbox(IndustryScreen screen) {
        return button(screen,"구형 장비 표시");
    }
    private static IndustryView copy(IndustryView v, CountryHud hud, long revision, IndustryView.Template draft) {
        return new IndustryView(v.session(), revision, v.country(), hud, v.economy(), v.resources(), v.modifiers(), v.equipment(), v.lines(), v.partners(), v.trades(), v.templates(),
                v.battalions(), v.companies(), v.locations(), v.recruits(), v.deployed(), draft, "",
                new IndustryView.NavalRepairs(3, List.of(
                        new IndustryView.NavalRepair("fixture-ship-1", "초계함 · 렌더링 예제", "production/equipment/destroyer", "부산", 30, 50, "수리 중",12.0,40.0,36L),
                        new IndustryView.NavalRepair("fixture-ship-waiting", "수리 대기 함선 · 렌더링 예제", "production/equipment/destroyer", "부산", 20, 50, "조선소 배정 대기",24.0,40.0,null),
                        new IndustryView.NavalRepair("fixture-ship-2", "잠수함 · 렌더링 예제", "production/equipment/submarine", "진해", 12, 40, "귀항 중",10.0,30.0,null)), 2,List.of(new IndustryView.RepairPort("jeju","제주",6,0,List.of()),new IndustryView.RepairPort("busan","부산",6,1,List.of("fixture-ship-1","fixture-ship-waiting")),new IndustryView.RepairPort("jinhae","진해",3,0,List.of("fixture-ship-2")))), new IndustryView.SpecialForces(3, 6));
    }
    private static Button button(IndustryScreen screen, String name) {
        return (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow(() -> new AssertionError("Missing control: " + name));
    }
    private static void repairPolicies(ClientGameTestContext context,IndustryView fixture) {
        var json=new com.google.gson.Gson().toJsonTree(fixture).getAsJsonObject();
        var gson=new com.google.gson.Gson();
        var requests=new ArrayList<IndustryPayloads.Request>();
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,requests::add));
        context.waitTicks(2);click(context,"해군 수리 대기열");
        int collapsed=context.computeOnClient(client->button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY());
        for(int expected:new int[]{2,0,1}) {
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();var row=button(screen,"수리 항구: 부산");
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(row.getX()+row.getWidth()*46/400.0,row.getY()+row.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
                var request=requests.getLast();
                check(request.action()==IndustryPayloads.Action.REPAIR_PRIORITY&&request.item().equals("busan")&&request.amount()==expected,"Port icon cycles normal, high, low, normal");
                check(button(screen,"수리 항구: 진해").getY()==collapsed,"Priority icon never expands port rows");
                for(var item:json.getAsJsonObject("navalRepairs").getAsJsonArray("ports"))if(item.getAsJsonObject().get("id").getAsString().equals("busan"))item.getAsJsonObject().addProperty("priority",expected);
                json.addProperty("revision",json.get("revision").getAsLong()+1);screen.update(gson.fromJson(json,IndustryView.class));
            });
            context.waitTicks(2);ResearchScreenTest.gui2Screenshot(context,"hoi-repair-priority-"+expected);
        }
        for(boolean disabled:new boolean[]{true,false}) {
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();var row=button(screen,"수리 항구: 부산");
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(row.getX()+row.getWidth()*46/400.0,row.getY()+row.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0)),false);
                var request=requests.getLast();check(request.action()==IndustryPayloads.Action.REPAIR_ENABLED&&request.amount()==(disabled?0:1),"Right click toggles repair prohibition");
                for(var item:json.getAsJsonObject("navalRepairs").getAsJsonArray("ports"))if(item.getAsJsonObject().get("id").getAsString().equals("busan"))item.getAsJsonObject().addProperty("disabled",disabled);
                json.addProperty("revision",json.get("revision").getAsLong()+1);screen.update(gson.fromJson(json,IndustryView.class));
            });
            context.waitTicks(2);ResearchScreenTest.gui2Screenshot(context,"hoi-repair-disabled-"+disabled);
        }
        int count=requests.size();
        click(context,"수리 항구: 부산");context.waitTicks(2);
        context.runOnClient(client->{check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()>collapsed,"Port body independently expands its ships");check(requests.size()==count,"Expansion is local and does not change repair policy");});
        click(context,"수리 목록 펼치기/접기");context.waitTicks(2);
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()>collapsed,"Expand all after opening one port must keep its ships open"));
        ResearchScreenTest.gui2Screenshot(context,"hoi-repair-individual-then-expand-all");
        click(context,"수리 목록 펼치기/접기");context.waitTicks(2);
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()==collapsed,"Collapse all closes every port"));
        click(context,"수리 항구: 부산");context.waitTicks(2);
        click(context,"수리 항구: 부산");context.waitTicks(2);
        click(context,"수리 목록 펼치기/접기");context.waitTicks(2);
        context.runOnClient(client->check(button((IndustryScreen)client.gui.screen(),"수리 항구: 진해").getY()>collapsed,"Repeated individual toggles preserve the global expand action"));
    }
    private static void click(ClientGameTestContext context, String name) {
        context.runOnClient(client -> { var b = button((IndustryScreen)client.gui.screen(), name); check(b.active, "Enabled control: " + name); b.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0)); });
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
