package dev.hoi.client.map;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.protocol.ProtocolUnitHud.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class MapUnitHudChecks {
    private static Snapshot fixture(net.minecraft.client.Minecraft client) {
        var camera=client.gameRenderer.mainCamera();
        var forward=new net.minecraft.world.phys.Vec3(camera.forwardVector());
        var center=camera.position().add(forward.scale(12));
        var rows=new ArrayList<Counter>();
        for(String type:List.of("tank","infantry","mechanized_infantry"))
            rows.add(new Counter("stack","KOR","대한민국",type,"제1사단",type.equals("infantry")?20:2,
                    center.x,center.y-3.1,center.z,.777,.941,.853,Map.of(type,type.equals("infantry")?140:14)));
        var right=new net.minecraft.world.phys.Vec3(camera.leftVector()).scale(-2.5);
        var naval=center.add(right);var aerial=center.subtract(right);
        rows.add(new Counter("fleet","KOR","대한민국","navy_heavy_cruiser","제1함대",24,naval.x,naval.y-3.1,naval.z,.77,.94,.8,Map.of(),5,Status.OPERATING));
        rows.add(new Counter("wing-a","KOR","대한민국","air_fighter","F-16",60,aerial.x,aerial.y-3.1,aerial.z,1,.6,.83,Map.of(),0,Status.IDLE));
        rows.add(new Counter("wing-b","KOR","대한민국","air_cas","FA-50",40,aerial.x,aerial.y-3.1,aerial.z,1,.4,.8,Map.of(),0,Status.TRAINING));
        var behind=camera.position().subtract(forward.scale(12));
        rows.add(new Counter("behind","KOR","대한민국","tank","후방",1,behind.x,behind.y-3.1,behind.z,1,1,1,Map.of()));
        rows.add(new Counter("distant","KOR","대한민국","tank","원거리",1,client.player.getX()+1000,center.y,center.z,1,1,1,Map.of()));
        return new Snapshot(client.level.dimension().identifier().toString(),rows.stream().map(c->c.withAllegiance(Allegiance.OURS)).toList());
    }
    public static void run(ClientGameTestContext context) {
        context.setScreen(()->null);
        context.runOnClient(client->{
            client.player.getAbilities().flying=true;
            client.player.onUpdateAbilities();
            client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        });
        context.waitTicks(10);
        context.runOnClient(client->{
            var snapshot=fixture(client);
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            try {
                Snapshot.CODEC.encode(b,snapshot);
                if(!snapshot.equals(Snapshot.CODEC.decode(b))||b.readableBytes()!=0)throw new AssertionError("Unit HUD codec");
                b.clear();b.writeUtf(snapshot.dimension(),128);b.writeVarInt(193);
                try{Snapshot.CODEC.decode(b);throw new AssertionError("Oversized HUD accepted");}catch(IllegalArgumentException expected){}
            }finally{b.release();}
            for(String type:dev.hoi.protocol.ProtocolUnitHud.TYPES)if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/unit_hud/"+type+".png")).isEmpty())throw new AssertionError("Missing non-NATO unit icon: "+type);
            for(String asset:List.of("army_selected","navy_selected"))if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/unit_hud/"+asset+".png")).isEmpty())throw new AssertionError("Missing original selection outline: "+asset);
            MapUnitHud.accept(snapshot);
        });
        context.waitTicks(3);
        context.runOnClient(client->{
            if(MapUnitHud.boxes().size()!=6)throw new AssertionError("HUD must cull behind camera and distant units: "+MapUnitHud.boxes());
            var wings=MapUnitHud.boxes().stream().filter(box->box.counter().air()).toList();
            if(wings.size()!=2||Math.abs(wings.getFirst().y()-wings.getLast().y())<wings.getFirst().height())throw new AssertionError("Air wings at one base must stack without overlap");
            int x=client.getWindow().getGuiScaledWidth()/2,y=client.getWindow().getGuiScaledHeight()/2;
            if(MapUnitHud.boxes().stream().noneMatch(b->b.counter().type().equals("infantry")&&b.contains(x,y)))throw new AssertionError("Crosshair must select the infantry detail tooltip");
        });
        context.takeScreenshot("hoi-unit-hud-counters-and-hover");context.waitTicks(3);
        context.runOnClient(client->{
            var source=fixture(client);var rows=source.counters().stream().map(c->c.navy()||c.air()?c:c.concealed()).toList();
            var snapshot=new Snapshot(source.dimension(),rows);
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            try{Snapshot.CODEC.encode(b,snapshot);if(!snapshot.equals(Snapshot.CODEC.decode(b)))throw new AssertionError("Estimated composition codec");}finally{b.release();}
            MapUnitHud.accept(snapshot);
        });
        context.waitTicks(3);context.takeScreenshot("hoi-unit-hud-unknown-composition");
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->{client.options.guiScale().set(2);MapUnitHud.accept(fixture(client));});
        context.waitTicks(3);context.takeScreenshot("hoi-unit-hud-counters-and-hover-gui2");
        for(var allegiance:Allegiance.values()) {
            context.runOnClient(client->{
                var source=fixture(client);
                MapUnitHud.accept(new Snapshot(source.dimension(),source.counters().stream().map(c->c.withAllegiance(allegiance)).toList()));
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-unit-tooltip-"+allegiance.name().toLowerCase(Locale.ROOT)+"-gui2");
        }
        context.runOnClient(client->{
            var rows=fixture(client).counters();
            for(var c:rows) {
                if(MapUnitHud.selectable(c,"hoi:atlas_army_test")!=( !c.air()&&!c.navy()))throw new AssertionError("Army selection filter");
                if(MapUnitHud.selectable(c,"hoi:atlas_navy_test")!=c.navy())throw new AssertionError("Navy selection filter");
                if(MapUnitHud.selectable(c,"hoi:atlas_air_test")!=c.air())throw new AssertionError("Air selection filter");
                if(MapUnitHud.selectable(c,"hoi:atlas_construction_test"))throw new AssertionError("Other map selection filter");
            }
            dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.ProtocolHudPayloads.State.of("KOR",dev.hoi.protocol.ProtocolCountryHud.UNKNOWN));
        });
        context.setScreen(dev.hoi.client.screen.ScreenMapCursor::new);context.waitTicks(3);
        context.runOnClient(client->{
            MapUnitHud.accept(fixture(client));
        });
        context.waitTicks(2);
        var drag=context.computeOnClient(client->{
            var b=MapUnitHud.boxes();
            return new double[]{b.stream().mapToInt(MapUnitHud.MapHudBox::x).min().orElseThrow()-3,
                    b.stream().mapToInt(MapUnitHud.MapHudBox::y).min().orElseThrow()-3,
                    b.stream().mapToInt(v->v.x()+v.width()).max().orElseThrow()+3,
                    b.stream().mapToInt(v->v.y()+v.height()).max().orElseThrow()+3};
        });
        context.runOnClient(client->{
            var screen=client.gui.screen();
            screen.mouseClicked(mouse(drag[2],drag[3]),false);
            screen.mouseDragged(mouse(drag[0],drag[1]),drag[0]-drag[2],drag[1]-drag[3]);
        });
        context.waitTicks(2);context.takeScreenshot("hoi-unit-hud-drag-rectangle-gui2");
        context.runOnClient(client->{
            client.gui.screen().mouseReleased(mouse(drag[0],drag[1]));
            if(MapUnitHud.selectedKeys().size()!=6)throw new AssertionError("Reverse drag must select every visible intersecting HUD");
            MapUnitHud.accept(fixture(client));
            if(MapUnitHud.selectedKeys().size()!=6)throw new AssertionError("Snapshot refresh loses multi-selection");
        });
        context.waitTicks(2);context.takeScreenshot("hoi-unit-hud-drag-selected-gui2");
        context.runOnClient(client->{
            MapUnitHud.selectArea(0,0,1,1);
            if(!MapUnitHud.selectedKeys().isEmpty())throw new AssertionError("Empty rectangle must clear selection");
        });
        context.setScreen(()->null);
        for(var allegiance:Allegiance.values()) {
            context.runOnClient(client->{
                var snapshot=fixture(client);
                var colored=new Snapshot(snapshot.dimension(),snapshot.counters().stream().map(c->c.withAllegiance(allegiance)).toList());
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
                try {
                    Snapshot.CODEC.encode(buffer,colored);
                    if(!colored.equals(Snapshot.CODEC.decode(buffer))||buffer.readableBytes()!=0)throw new AssertionError("Allegiance codec");
                } finally {buffer.release();}
                MapUnitHud.accept(colored);
            });
            context.waitTicks(2);context.takeScreenshot("hoi-unit-hud-allegiance-"+allegiance.name().toLowerCase(Locale.ROOT));
        }
        context.runOnClient(client->MapUnitHud.accept(fixture(client)));
        context.waitTicks(2);
        for(String type:List.of("infantry","navy_heavy_cruiser","air_fighter")) {
            context.runOnClient(client->{
                var box=MapUnitHud.boxes().stream().filter(b->b.counter().type().equals(type)).findFirst().orElseThrow();
                if(!dev.hoi.client.input.InputMapInfo.clickAt(client,box.x()+box.width()/2.0,box.y()+box.height()/2.0,client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight()))throw new AssertionError("Left click did not select "+type);
                if(!MapUnitHud.selected().equals(box.counter().key()))throw new AssertionError("Wrong counter selected");
                MapUnitHud.accept(fixture(client));
                if(!MapUnitHud.selected().equals(box.counter().key()))throw new AssertionError("Refresh lost selection");
            });
            context.waitTicks(2);context.takeScreenshot("hoi-unit-hud-selected-"+type);
        }
        context.runOnClient(client->{
            if(MapUnitHud.selectAt(0,0)||!MapUnitHud.selected().isEmpty())throw new AssertionError("Blank map must clear selection");
        });
        for (int[] viewport : new int[][]{{1920,1080,2},{2560,1080,2},{1080,1440,2},{1920,1080,3}}) {
            context.runOnClient(client->client.options.guiScale().set(viewport[2]));
            context.getInput().resizeWindow(viewport[0],viewport[1]);
            context.waitTicks(3);
            context.runOnClient(client->MapUnitHud.accept(fixture(client)));
            context.waitTicks(3);
            context.runOnClient(client->{
                var units=new dev.hoi.client.ui.UiUnits(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
                if(MapUnitHud.boxes().size()!=6)throw new AssertionError("Responsive HUD lost counters");
                for(var box:MapUnitHud.boxes()) {
                    if(box.width()!=units.size(box.counter().navy()?37:33))throw new AssertionError("Responsive HUD width");
                    if(!box.contains(box.x()+box.width()-1,box.y()+box.height()-1)||box.contains(box.x()+box.width(),box.y()))throw new AssertionError("Responsive hover bounds");
                    if(!MapUnitHud.selectAt(box.x()+box.width()/2.0,box.y()+box.height()/2.0)||!MapUnitHud.selected().equals(box.counter().key()))throw new AssertionError("Responsive selection bounds");
                }
                if(client.options.guiScale().get()!=viewport[2])throw new AssertionError("HUD changed the user's GUI scale");
            });
            context.takeScreenshot("hoi-unit-hud-responsive-"+viewport[0]+"x"+viewport[1]+"-gui"+viewport[2]);
        }
        context.getInput().resizeWindow(1600,1000);
        context.runOnClient(client->{client.options.guiScale().set(2);MapUnitHud.accept(fixture(client));});
        context.setScreen(()->new MapTestSidebar(true));context.waitTicks(3);
        context.runOnClient(client->{if(!MapUnitHud.boxes().isEmpty())throw new AssertionError("MapTestSidebar must cover counters");});
        context.setScreen(()->new MapTestSidebar(false));context.waitTicks(3);
        context.runOnClient(client->{if(MapUnitHud.boxes().size()!=6)throw new AssertionError("World area retains counters behind sidebar");});
        context.setScreen(()->null);
        context.getInput().resizeWindow(854,480);context.waitTicks(3);
        context.runOnClient(client->MapUnitHud.accept(fixture(client)));context.waitTicks(3);
        context.runOnClient(client->{if(MapUnitHud.boxes().size()!=6)throw new AssertionError("Compact viewport counters");});
        context.takeScreenshot("hoi-unit-hud-compact");context.waitTicks(3);
        var delayedSelection=context.computeOnClient(client->{
            var snapshot=fixture(client);MapUnitHud.accept(snapshot);MapUnitHud.replaceKeys(List.of(snapshot.counters().getFirst().key()));
            var selected=MapUnitHud.selectedKeys();for(int i=0;i<40;i++)MapUnitHud.tick();return selected;
        });
        context.waitTicks(2);
        context.runOnClient(client->{
            if(!MapUnitHud.boxes().isEmpty()||!MapUnitHud.selectedKeys().equals(delayedSelection))throw new AssertionError("Delayed snapshot must hide stale counters and preserve the exact selection");
            MapUnitHud.accept(fixture(client));if(!MapUnitHud.selectedKeys().equals(delayedSelection))throw new AssertionError("A delayed authoritative refresh must retain the surviving selection");
            MapUnitHud.accept(new Snapshot("hoi:elsewhere",List.of()));
            if(!MapUnitHud.boxes().isEmpty()||!MapUnitHud.selectedKeys().isEmpty())throw new AssertionError("Dimension change must clear both private HUD and selection");
            MapUnitHud.reset();
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }
    private static net.minecraft.client.input.MouseButtonEvent mouse(double x,double y) {
        return new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
    }
    private static final class MapTestSidebar extends Screen implements InputSidebarMovement.InputMovementScreen {
        private final boolean covered;
        MapTestSidebar(boolean covered){super(Component.literal("HUD 영역 검사"));this.covered=covered;}
        @Override public boolean allowsMovement(){return true;}
        @Override public int unitHudLeft(){return covered?width:80;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean isInGameUi(){return true;}
    }
}
