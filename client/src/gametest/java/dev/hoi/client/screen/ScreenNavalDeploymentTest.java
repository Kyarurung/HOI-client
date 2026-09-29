package dev.hoi.client.screen;

import dev.hoi.client.ResearchScreenTest;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class ScreenNavalDeploymentTest {
    public static void run(ClientGameTestContext context) {
        try(var in=ScreenNavalDeploymentTest.class.getResourceAsStream("/industry-fixture.json")) {
            run(context,new ProtocolIndustryPayloads.Response(new String(Objects.requireNonNull(in).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view());
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
    }
    public static void run(ClientGameTestContext context,ProtocolIndustryView fixture) {
        var gson=new com.google.gson.Gson();
        var json=gson.toJsonTree(fixture).getAsJsonObject();
        var ship=fixture.equipment().stream().filter(e->e.naval()&&!e.id().contains("convoy")).findFirst().orElseThrow();
        for(var item:json.getAsJsonArray("equipment"))if(item.getAsJsonObject().get("id").getAsString().equals(ship.id()))item.getAsJsonObject().addProperty("stockpile",2);
        var destinations=new TreeMap<String,String>();
        String navalLine="hoi:test/naval-production";
        json.getAsJsonArray("lines").add(gson.toJsonTree(new ProtocolIndustryView.Line(navalLine,ship.id(),1,10,.5,0,1,0)));
        destinations.put(navalLine,"");
        json.add("navalDeployment",gson.toJsonTree(new ProtocolIndustryView.NavalDeployment(List.of(
                new ProtocolIndustryView.Choice("busan","부산",""),new ProtocolIndustryView.Choice("jinhae","진해","")),Map.of(navalLine,"PRODUCTION_CAN_NOT_DEPLOY_MANPOWER"),destinations,List.of(new ProtocolIndustryView.NavalTarget("fleet/test","대한민국 함대","busan",0,12),new ProtocolIndustryView.NavalTarget("busan","부산","busan",5,0),new ProtocolIndustryView.NavalTarget("jinhae","진해","jinhae",3,0)))));
        json.getAsJsonObject("economy").addProperty("manpower",99);
        json.getAsJsonObject("hud").getAsJsonObject("national").addProperty("manpower",99);
        var requests=new ArrayList<ProtocolIndustryPayloads.Request>();
        context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),gson.fromJson(json,ProtocolIndustryView.class),requests::add));
        context.runOnClient(client->{
            var screen=(ScreenIndustry)client.gui.screen();
            if(screen.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().contains("완성 함선")))throw new AssertionError("Completed ships stay on the production line");
            var view=gson.fromJson(json,ProtocolIndustryView.class);
            var line=view.lines().stream().filter(l->l.id().equals(navalLine)).findFirst().orElseThrow();
            if(line.progress()!=1||line.daily()!=0)throw new AssertionError("Pending hull retains full gauge and suspends construction");
            String tip=ScreenProductionTooltips.navalWaiting(view,line);
            if(!tip.equals(dev.hoi.client.ui.UiEquipmentDesigns.tooltip("PRODUCTION_CAN_NOT_DEPLOY")+"\n"+dev.hoi.client.ui.UiEquipmentDesigns.tooltip("PRODUCTION_CAN_NOT_DEPLOY_MANPOWER"))||!tip.contains("인력 부족"))throw new AssertionError("Pending hull uses original tooltip data");
        });
        ResearchScreenTest.gui2Screenshot(context,"hoi-navy-production-manpower-wait");
        var point=context.computeOnClient(client->{var b=button((ScreenIndustry)client.gui.screen(),ship.name()+" · 장비 교체");double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(12);context.takeScreenshot("hoi-navy-production-manpower-tooltip-gui2");
        String line=destinations.firstKey();press(context,"생산 배치 대상 · "+line);
        ResearchScreenTest.gui2Screenshot(context,"hoi-navy-production-port-picker");
        var portPoint=context.computeOnClient(client->{var b=button((ScreenIndustry)client.gui.screen(),"부산");double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
        context.getInput().setCursorPos(portPoint[0],portPoint[1]);context.waitTicks(12);context.takeScreenshot("hoi-navy-production-port-tooltip-gui2");
        context.runOnClient(client->{String tip=ScreenNavalDeployment.tooltip(new ProtocolIndustryView.NavalTarget("busan","부산","busan",5,0));if(!tip.contains("항구에 배치: §e부산 (5)")||!tip.contains("우클릭"))throw new AssertionError("Port tooltip uses source text and actual level");});
        context.runOnClient(client->{
            var screen=(ScreenIndustry)client.gui.screen();var b=button(screen,"부산");
            b.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(b.getX()+2,b.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0)),false);
            var locate=requests.getLast();
            if(locate.action()!=ProtocolIndustryPayloads.Action.NAVAL_LOCATE||!locate.item().equals("busan"))throw new AssertionError("Right click locates the port without assigning it");
        });
        context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),gson.fromJson(json,ProtocolIndustryView.class),requests::add));
        press(context,"생산 배치 대상 · "+line);
        press(context,"부산");
        var request=requests.getLast();
        if(request.action()!=ProtocolIndustryPayloads.Action.NAVAL_PORT||!request.item().equals(line)||!request.other().equals("busan"))throw new AssertionError("Production line sends its deployment destination");
        json.getAsJsonArray("lines").remove(json.getAsJsonArray("lines").size()-1);
        json.getAsJsonArray("lines").add(gson.toJsonTree(new ProtocolIndustryView.Line(navalLine,ship.id(),1,10,.5,.025,.25,0)));
        json.getAsJsonObject("navalDeployment").add("waiting",new com.google.gson.JsonObject());
        context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),gson.fromJson(json,ProtocolIndustryView.class),requests::add));
        context.getInput().setCursorPos(1500,900);
        ResearchScreenTest.gui2Screenshot(context,"hoi-navy-production-days-auto");
        String longName="대한민국 태평양 원정 함대의 아주 긴 배치 대상 이름";
        var target=json.getAsJsonObject("navalDeployment").getAsJsonArray("targets").get(0).getAsJsonObject();
        target.addProperty("name",longName);
        json.getAsJsonObject("navalDeployment").getAsJsonObject("destinations").addProperty(navalLine,"fleet/test");
        context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),gson.fromJson(json,ProtocolIndustryView.class),requests::add));
        ResearchScreenTest.gui2Screenshot(context,"hoi-navy-production-long-destination");
        press(context,"생산 배치 대상 · "+navalLine);
        ResearchScreenTest.gui2Screenshot(context,"hoi-navy-production-long-picker");
        context.runOnClient(client->{
            var font=client.font;int width=font.width("대한민국...");
            String clipped=dev.hoi.client.ui.UiText.ellipsize(font,longName,width,"...");
            if(!clipped.endsWith("...")||font.width(clipped)>width)throw new AssertionError("Long destination fits its cell with literal ellipsis");
            if(!button((ScreenIndustry)client.gui.screen(),longName).active)throw new AssertionError("Clipping must preserve the full destination and selection");
        });
        json.getAsJsonObject("navalDeployment").getAsJsonObject("destinations").addProperty(navalLine,"");
        for(int slots:new int[]{5,10}) {
            for(var item:json.getAsJsonArray("equipment"))if(item.getAsJsonObject().get("id").getAsString().equals(ship.id()))item.getAsJsonObject().addProperty("factoryLimit",slots);
            context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),gson.fromJson(json,ProtocolIndustryView.class),requests::add));
            ResearchScreenTest.gui2Screenshot(context,"hoi-navy-production-"+slots+"-slots-aligned");
        }
        context.setScreen(()->null);
    }
    private static Button button(ScreenIndustry screen,String name){return screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(name)).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError("Missing "+name));}
    private static void press(ClientGameTestContext context,String name){context.runOnClient(client->{var b=button((ScreenIndustry)client.gui.screen(),name);if(!b.active)throw new AssertionError("Disabled "+name);b.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));});context.waitTicks(2);}
}
