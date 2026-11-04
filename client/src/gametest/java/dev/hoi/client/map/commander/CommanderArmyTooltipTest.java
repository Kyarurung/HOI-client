package dev.hoi.client.map.commander;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import static dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CommanderArmyTooltipTest {
    private CommanderArmyTooltipTest() {}
    public static void selectCard(String id){CommanderPanels.select(id,false);}
    public static void assertPanelBackdrop(){if(CommanderPanels.hits().stream().noneMatch(h->h.icon().equals("army_supply")))throw new AssertionError("Modal must retain selected army panel");}
    public static void run(ClientGameTestContext game,MenuView menu){
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var gson=new com.google.gson.Gson();var card=gson.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-army-panel-tooltips.json")),Card.class);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->{c.options.guiScale().set(2);CommanderHud.reset();CommanderUnitHud.reset();ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderOrders.selectionObserver=x->{};CommanderOrders.commandObserver=x->{};CommanderHud.accept(new View("KOR",c.level.dimension().identifier().toString(),Mode.ARMY,List.of(card)));CommanderOrders.accept(new CommanderOrderPayloads.Context(UUID.randomUUID().toString(),7,"KOR",c.level.dimension().identifier().toString(),""));CommanderPanels.select(card.id(),false);});
                game.setScreen(MapCursor::new);game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-army-source-panel-"+size[0]+"-gui2");
                var member=card.panel().members().getFirst();
                var expected=new LinkedHashMap<String,String>();
                for(String key:List.of("supply","fuel","averages"))expected.put("army_"+key,card.panel().tooltips().getOrDefault(key,""));
                expected.put("deploy_hq_button",card.panel().headquarters().tooltip());
                expected.put("division_strategic",card.panel().tooltips().get(Boolean.TRUE.equals(member.data().strategic())?"strategic_on":"strategic_off"));
                expected.put("army_portrait",card.panel().tooltips().getOrDefault("commander",""));
                for(String key:List.of("organization","strength","template"))expected.put("division_"+key,member.data().tooltips().getOrDefault(key,""));
                expected.put("division_entrenchment/"+member.id(),member.data().tooltips().get("entrenchment"));
                for(var entry:expected.entrySet()){
                    var hit=game.computeOnClient(c->CommanderPanels.hits().stream().filter(h->h.icon().equals(entry.getKey())).findFirst().orElseThrow());
                    game.getInput().setCursorPos((hit.x()+hit.w()/2.)*2,(hit.y()+hit.h()/2.)*2);game.waitTicks(3);
                    game.runOnClient(c->{if(entry.getValue().isEmpty()||!CommanderHud.renderedTooltip().equals(UiCommandLabels.withoutShortcuts(entry.getValue())))throw new AssertionError("Tooltip differs at "+entry.getKey()+": "+CommanderHud.renderedTooltip());});
                    game.takeScreenshot("hoi-army-tooltip-"+entry.getKey().replaceAll("[^A-Za-z0-9_-]","-")+"-"+size[0]+"-gui2");
                    if(entry.getKey().startsWith("division_entrenchment/")){game.waitTicks(25);game.runOnClient(c->{if(!CommanderHud.renderedTooltip().equals(UiCommandLabels.withoutShortcuts(member.data().tooltips().get("entrenchment_detail"))))throw new AssertionError("Long hover must reveal source detail");});game.takeScreenshot("hoi-army-entrenchment-detail-"+size[0]+"-gui2");}
                }
                game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(2);
                game.runOnClient(c->{var badge=CommanderPanels.hits().stream().filter(h->h.icon().equals("division_army")).findFirst().orElseThrow();if(!badge.tip().equals(UiCommandLabels.text("DIVISION_VIEW_SELECT_ARMY","")))throw new AssertionError("Division shield must describe army selection");CommanderHud.clickAt(badge.x()+badge.w()/2.,badge.y()+badge.h()/2.,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,false);if(!CommanderPanels.selectedCards().contains(card.id()))throw new AssertionError("Division shield must select its attached army");});
                var synthetic=gson.toJsonTree(card).getAsJsonObject();var panel=synthetic.getAsJsonObject("panel");panel.addProperty("forces",.25);panel.getAsJsonObject("tooltips").addProperty("forces","SYNTHETIC forces 25%");panel.getAsJsonArray("members").get(0).getAsJsonObject().getAsJsonObject("data").addProperty("coldAcclimatization",.5);
                game.runOnClient(c->{CommanderHud.accept(new View("KOR",c.level.dimension().identifier().toString(),Mode.ARMY,List.of(gson.fromJson(synthetic,Card.class))));CommanderPanels.select(card.id(),false);});game.waitTicks(3);
                var climate=game.computeOnClient(c->CommanderPanels.hits().stream().filter(h->h.icon().equals("division_acclimatization")).findFirst().orElseThrow());game.getInput().setCursorPos((climate.x()+2)*2,(climate.y()+2)*2);game.waitTicks(3);game.runOnClient(c->{if(!CommanderHud.renderedTooltip().equals(UiCommandLabels.withoutShortcuts(member.data().tooltips().get("acclimatization"))))throw new AssertionError("Climate must preserve server tooltip");});game.takeScreenshot("hoi-army-climate-synthetic-visible-"+size[0]+"-gui2");
                var footer=game.computeOnClient(c->CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(card.id())&&b.height()==CommanderLayout.units(c.getWindow().getGuiScaledWidth(),c.getWindow().getGuiScaledHeight()).size(14)).findFirst().orElseThrow());double fx=footer.x()+footer.width()/2.,fy=footer.y()+footer.height()-4;game.getInput().setCursorPos(fx*2,fy*2);game.waitTicks(3);game.runOnClient(c->{if(!CommanderHud.renderedTooltip().equals("SYNTHETIC forces 25%"))throw new AssertionError("Force ratio must have its source hover");var requests=new ArrayList<Open>();CommanderHud.requestObserver=requests::add;try{CommanderHud.clickAt(fx,fy,com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,false);if(!requests.equals(List.of(footer.request())))throw new AssertionError("Force hover must preserve the existing footer command");}finally{CommanderHud.requestObserver=null;}});game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(2);game.takeScreenshot("hoi-army-forces-quarter-synthetic-"+size[0]+"-gui2");
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.runOnClient(c->{CommanderOrders.commandObserver=null;CommanderOrders.selectionObserver=null;CommanderHud.reset();CommanderUnitHud.reset();c.options.guiScale().set(previous);});game.setScreen(()->null);}
    }
}
