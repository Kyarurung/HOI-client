package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolMenuTab;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.List;

final class ScreenProductionPresentationTest {
    private ScreenProductionPresentationTest() {}
    static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        var fixture=ScreenIndustryChecks.tankFixture("production-source-fixture.json");
        stockColors(context,fixture);
        priorityTooltips(context,fixture);
        var designer=ScreenIndustryChecks.tankFixture("initial-air-fixture.json").tankDesigner();
        context.getInput().setCursorPos(2400,1300);
        context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),fixture,request->{}));
        context.waitTicks(3);
        try {
            var normal=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-source-gui2").toFile());
            var bounds=context.computeOnClient(client->{
                var screen=(ScreenIndustry)client.gui.screen();
                var image=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                return new int[]{image.getX()*2,image.getY()*2,image.getWidth()*2,image.getHeight()*2};
            });
            var modal=fixture.withTankDesigner(designer);
            context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,modal.session(),modal,request->{}));
            context.waitTicks(3);
            var opened=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-designer-background-gui2").toFile());
            int matching=0,total=0;
            for(int y=bounds[1]+4;y<bounds[1]+bounds[3]-4;y++)for(int x=bounds[0]+4;x<bounds[0]+bounds[2]-4;x++) {
                total++;
                if(normal.getRGB(x,y)==opened.getRGB(x,y))matching++;
            }
            if(matching<total*.98)throw new AssertionError("Opening designer changed production image pixels: "+matching+"/"+total);
            context.runOnClient(client->{
                if(client.gui.screen().children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")))throw new AssertionError("Background production image must not receive modal clicks");
            });
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        for(String file:List.of("production-source-fixture.json","production-china-fixture.json")) {
            var source=ScreenIndustryChecks.tankFixture(file);
            context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,source.session(),source,request->{}));
            context.waitTicks(3);
            var point=context.computeOnClient(client->{
                var picture=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                return new double[]{(picture.getX()+8)*client.getWindow().getGuiScale(),(picture.getY()+8)*client.getWindow().getGuiScale()};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("hoi-production-equipment-tooltip-"+source.country().toLowerCase(java.util.Locale.ROOT)+"-gui2");
            context.getInput().setCursorPos(2400,1300);
            context.runOnClient(client->{
                var first=source.equipment().stream().filter(e->!e.stats().isEmpty()).findFirst().orElseThrow();
                String tooltip=ScreenProductionTooltips.equipment(source,first);
                if(tooltip.contains("원본 기본 능력치")||!tooltip.contains("\n\n"))throw new AssertionError("Source equipment tooltip sections are missing");
                var air=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("항공기 제작")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                air.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-production-air-missions-"+source.country().toLowerCase(java.util.Locale.ROOT)+"-gui2");
        }
    }
    private static void priorityTooltips(ClientGameTestContext context, dev.hoi.protocol.ProtocolIndustryView fixture) {
        var requests = new java.util.ArrayList<dev.hoi.protocol.ProtocolIndustryPayloads.Request>();
        for (boolean up : new boolean[]{true, false}) {
            context.setScreen(() -> new ScreenIndustry(ProtocolMenuTab.PRODUCTION, fixture.session(), fixture, requests::add));
            context.waitTicks(3);
            var bounds = context.computeOnClient(client -> {
                String name = up ? "생산 라인 한 칸 위로" : "생산 라인 한 칸 아래로";
                var buttons = client.gui.screen().children().stream()
                        .filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(name))
                        .map(w -> (net.minecraft.client.gui.components.Button) w).toList();
                if (up && buttons.getFirst().active) throw new AssertionError("First production line cannot move up");
                var button = buttons.stream().filter(b -> b.active).findFirst().orElseThrow();
                var equipment = client.gui.screen().children().stream()
                        .filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().endsWith(" · 장비 교체") && b.getY() < button.getY())
                        .map(w -> (net.minecraft.client.gui.components.Button) w).reduce((a, b) -> b).orElseThrow();
                if (Math.abs(button.getX() - equipment.getRight()) > 1) throw new AssertionError("Priority arrows must sit beside equipment artwork");
                if (!up) {
                    var above = client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("생산 라인 한 칸 위로") && b.getY() < button.getY()).map(w -> (net.minecraft.client.gui.components.Button) w).reduce((a, b) -> b).orElseThrow();
                    if (above.getBottom() != button.getY()) throw new AssertionError("Priority buttons must remain adjacent without overlapping");
                }
                if (!up && button.getBottom() != equipment.getBottom() + 2) throw new AssertionError("Both priority buttons must extend downward by one GUI unit each");
                return new int[]{button.getX(), button.getY(), button.getWidth(), button.getHeight()};
            });
            context.getInput().setCursorPos((bounds[0] + bounds[2] / 2.0) * 2, (bounds[1] + bounds[3] / 2.0) * 2);
            context.waitTicks(8);
            var screenshot = context.takeScreenshot("hoi-production-priority-" + (up ? "up" : "down") + "-gui2");
            if (up) try {
                var image = javax.imageio.ImageIO.read(screenshot.toFile());
                int green = 0;
                for (int y = bounds[1] * 2; y < (bounds[1] + bounds[3]) * 2; y++) {
                    for (int x = bounds[0] * 2; x < (bounds[0] + bounds[2]) * 2; x++) {
                        int rgb = image.getRGB(x, y), r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                        if (g > r + 10 && g > b + 20) green++;
                    }
                }
                if (green < 10) throw new AssertionError("Priority up must use the original green arrow");
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }
        if (requests.stream().anyMatch(r -> r.action() != dev.hoi.protocol.ProtocolIndustryPayloads.Action.REFRESH && r.action() != dev.hoi.protocol.ProtocolIndustryPayloads.Action.CLOSE)) throw new AssertionError("Priority hover must not mutate production: " + requests);
        context.getInput().setCursorPos(2400, 1300);
    }
    private static void stockColors(ClientGameTestContext context,dev.hoi.protocol.ProtocolIndustryView fixture) {
        var data=new com.google.gson.Gson().toJsonTree(fixture).getAsJsonObject();
        var line=data.getAsJsonArray("lines").get(0).deepCopy();
        String id=line.getAsJsonObject().get("equipment").getAsString();
        var item=new com.google.gson.Gson().toJsonTree(fixture.equipment().stream().filter(e->e.id().equals(id)).findFirst().orElseThrow()).getAsJsonObject();
        item.addProperty("stockpile",1234);
        item.addProperty("deployed",0);
        item.addProperty("replacement","");
        var old=item.deepCopy();old.addProperty("id","hoi:test/old_model");old.addProperty("stockpile",0);old.addProperty("demand",0);
        var equipment=new com.google.gson.JsonArray();equipment.add(item);equipment.add(old);data.add("equipment",equipment);
        var lines=new com.google.gson.JsonArray();lines.add(line);data.add("lines",lines);
        long revision=fixture.revision();
        boolean opened=false;
        for(int target:new int[]{2,1,0,2}) {
            item.addProperty("demand",target==2?1235:0);
            old.addProperty("deployed",target==1?1:0);old.addProperty("replacement",target==1?id:"");
            data.addProperty("revision",++revision);
            var next=new dev.hoi.protocol.ProtocolIndustryPayloads.Response(data.toString()).view();
            if(!opened)context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,next.session(),next,request->{}));
            else context.runOnClient(client->((ScreenIndustry)client.gui.screen()).update(next));
            opened=true;
            context.waitTicks(3);
            var bounds=context.computeOnClient(client->{
                var button=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 재고량 · 군수 보기")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                return new int[]{button.getX()*2-4,button.getY()*2,button.getWidth()*2+4,button.getHeight()*2};
            });
            try {
                var image=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-stock-"+target+"-"+revision+"-gui2").toFile());
                int expected=target==0?0x009F03:0xFFBD00,unexpected=target==0?0xFFBD00:0x009F03,matching=0,wrong=0;
                for(int y=bounds[1];y<bounds[1]+bounds[3];y++)for(int x=bounds[0];x<bounds[0]+bounds[2];x++) {
                    int color=image.getRGB(x,y)&0xFFFFFF;
                    if(color==expected)matching++;
                    if(color==unexpected)wrong++;
                }
                if(matching<4||wrong>0)throw new AssertionError("Production stock color for target "+target+": expected pixels="+matching+", wrong pixels="+wrong);
            } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        }
    }
}
