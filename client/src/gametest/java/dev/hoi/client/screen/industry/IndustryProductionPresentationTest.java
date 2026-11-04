package dev.hoi.client.screen.industry;

import dev.hoi.protocol.menu.MenuTab;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.List;

final class IndustryProductionPresentationTest {
    private IndustryProductionPresentationTest() {}
    static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        var fixture=IndustryScreenChecks.bundledFixture("production-source-fixture.json");
        researchedSuccessors(context);
        stockColors(context,fixture);
        priorityTooltips(context,fixture);
        var designer=IndustryScreenChecks.tankFixture("initial-air-fixture.json").tankDesigner();
        context.getInput().setCursorPos(2400,1300);
        context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,fixture.session(),fixture,request->{}));
        context.waitTicks(3);
        try {
            var normal=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-source-gui2").toFile());
            var bounds=context.computeOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();
                var image=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                return new int[]{image.getX()*2,image.getY()*2,image.getWidth()*2,image.getHeight()*2};
            });
            var modal=fixture.withTankDesigner(designer);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,modal.session(),modal,request->{}));
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
            var source=IndustryScreenChecks.bundledFixture(file);
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,source.session(),source,request->{}));
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
                String tooltip=IndustryProductionTooltips.equipment(source,first);
                if(tooltip.contains("원본 기본 능력치")||!tooltip.contains("\n\n"))throw new AssertionError("Source equipment tooltip sections are missing");
                var air=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("항공기 제작")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                air.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-production-air-missions-"+source.country().toLowerCase(java.util.Locale.ROOT)+"-gui2");
            researchedEquipment(context, source);
            for (String category : List.of("보병 및 포병 장비 제작", "기갑 차량 제작", "함선 건조")) {
                context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, source.session(), source, request -> {}));
                context.runOnClient(client -> {
                    var screen = client.gui.screen();
                    var button = screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(category)).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                    button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                });
                context.waitTicks(3);
                context.takeScreenshot("hoi-production-catalog-" + source.country().toLowerCase(java.util.Locale.ROOT) + "-" + category);
                if (category.equals("함선 건조")) context.runOnClient(client -> {
                    var convoy = source.equipment().stream().filter(e -> e.family().equals("convoy") && e.unlocked()).findFirst().orElseThrow();
                    if (client.gui.screen().children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("생산: " + convoy.name()))) throw new AssertionError("The convoy must be present in the visible naval picker");
                });
            }
        }
    }
    private static void researchedSuccessors(ClientGameTestContext context) {
        researchedSuccessor(context, "hoi:train", "hoi:tfr/equipment/train_equipment_2", "train");
        researchedSuccessor(context, "hoi:tfr/equipment/exoskeleton_equipment_1", "hoi:tfr/equipment/exoskeleton_equipment_2", "exoskeleton");
    }
    private static void researchedSuccessor(ClientGameTestContext context, String oldId, String newId, String family) {
        var source = IndustryScreenChecks.tankFixture("production-train-upgrade-fixture.json");
        var old = source.equipment().stream().filter(e -> e.id().equals(oldId)).findFirst().orElseThrow();
        var newer = source.equipment().stream().filter(e -> e.id().equals(newId)).findFirst().orElseThrow();
        if (!old.unlocked() || !old.outdated() || !newer.unlocked() || newer.outdated()) throw new AssertionError("Server research successor state is incorrect: " + family);
        var gson = new com.google.gson.Gson();
        var json = gson.toJsonTree(source).getAsJsonObject();
        for (var value : json.getAsJsonArray("equipment")) {
            var equipment = value.getAsJsonObject();
            if (!java.util.Set.of(old.id(), newer.id()).contains(equipment.get("id").getAsString())) equipment.addProperty("unlocked", false);
        }
        var view = new dev.hoi.protocol.industry.IndustryPayloads.Response(gson.toJson(json)).view();
        context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, view.session(), view, request -> {}));
        context.runOnClient(client -> {
            var button = client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("보병 및 포병 장비 제작")).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
            button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
        });
        for (boolean showOld : new boolean[]{false, true}) {
            context.runOnClient(client -> {
                if (showOld) {
                    var toggle = client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("구형 장비 표시")).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                    toggle.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                }
                var names = client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button).map(w -> ((net.minecraft.client.gui.components.Button)w).getMessage().getString()).toList();
                if (names.contains("생산: " + old.name()) != showOld || !names.contains("생산: " + newer.name())) throw new AssertionError("Obsolete toggle does not match the researched successor: " + family);
            });
            context.getInput().setCursorPos(2400,1300);
            context.waitTicks(3);
            context.takeScreenshot("hoi-production-" + family + "-obsolete-" + showOld + "-gui2");
        }
    }
    private static void researchedEquipment(ClientGameTestContext context, dev.hoi.protocol.industry.IndustryView source) {
        var ids = java.util.Set.of("exoskeleton_equipment_1", "exoskeleton_equipment_2", "train_equipment_2", "train_equipment_3", "floating_harbor_equipment_1");
        var freight = source.equipment().stream().filter(e -> e.id().equals("hoi:tfr/equipment/train_equipment_2")).findFirst().orElseThrow();
        var armored = source.equipment().stream().filter(e -> e.id().equals("hoi:tfr/equipment/train_equipment_3")).findFirst().orElseThrow();
        if (freight.texture().equals(armored.texture())) throw new AssertionError("Armored freight train must use its dedicated source picture: " + source.country());
        var gson = new com.google.gson.Gson();
        var json = gson.toJsonTree(source).getAsJsonObject();
        var missing = new java.util.TreeSet<>(ids);
        for (var value : json.getAsJsonArray("equipment")) {
            var equipment = value.getAsJsonObject();
            boolean selected = ids.contains(equipment.get("id").getAsString().replace("hoi:tfr/equipment/", ""));
            equipment.addProperty("unlocked", selected);
            if (selected) missing.remove(equipment.get("id").getAsString().replace("hoi:tfr/equipment/", ""));
        }
        if (!missing.isEmpty()) throw new AssertionError("Missing research-unlocked production models for " + source.country() + ": " + missing);
        var view = new dev.hoi.protocol.industry.IndustryPayloads.Response(gson.toJson(json)).view();
        for (String category : List.of("보병 및 포병 장비 제작", "함선 건조")) {
            context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, view.session(), view, request -> {}));
            context.runOnClient(client -> {
                var button = client.gui.screen().children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(category)).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                for (var equipment : view.equipment()) if (equipment.unlocked() && equipment.naval() == category.equals("함선 건조")) {
                    if (client.gui.screen().children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("생산: " + equipment.name())))
                        throw new AssertionError("Researched model is absent from its picker: " + equipment.id());
                }
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-production-researched-" + source.country().toLowerCase(java.util.Locale.ROOT) + "-" + category);
        }
    }
    private static void priorityTooltips(ClientGameTestContext context, dev.hoi.protocol.industry.IndustryView fixture) {
        var requests = new java.util.ArrayList<dev.hoi.protocol.industry.IndustryPayloads.Request>();
        for (boolean up : new boolean[]{true, false}) {
            context.setScreen(() -> new IndustryScreen(MenuTab.PRODUCTION, fixture.session(), fixture, requests::add));
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
                if (!up && (button.getBottom() > equipment.getBottom() || equipment.getBottom() - button.getBottom() > 2)) throw new AssertionError("Priority buttons must stay within the equipment bottom edge, allowing its one-GUI offset and rounding");
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
        if (requests.stream().anyMatch(r -> r.action() != dev.hoi.protocol.industry.IndustryPayloads.Action.REFRESH && r.action() != dev.hoi.protocol.industry.IndustryPayloads.Action.CLOSE)) throw new AssertionError("Priority hover must not mutate production: " + requests);
        context.getInput().setCursorPos(2400, 1300);
    }
    private static void stockColors(ClientGameTestContext context,dev.hoi.protocol.industry.IndustryView fixture) {
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
            var next=new dev.hoi.protocol.industry.IndustryPayloads.Response(data.toString()).view();
            if(!opened)context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,next.session(),next,request->{}));
            else context.runOnClient(client->((IndustryScreen)client.gui.screen()).update(next));
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
