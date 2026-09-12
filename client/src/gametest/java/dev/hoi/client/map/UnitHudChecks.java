package dev.hoi.client.map;

import dev.hoi.client.input.SidebarMovement;
import dev.hoi.protocol.UnitHudProtocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class UnitHudChecks {
    private static Snapshot fixture(net.minecraft.client.Minecraft client) {
        var camera=client.gameRenderer.mainCamera();
        var forward=new net.minecraft.world.phys.Vec3(camera.forwardVector());
        var center=camera.position().add(forward.scale(12));
        var rows=new ArrayList<Counter>();
        for(String type:List.of("tank","infantry","mechanized_infantry"))
            rows.add(new Counter("stack","KOR","대한민국",type,"제1사단",type.equals("infantry")?20:2,
                    center.x,center.y-3.1,center.z,.777,.941,.853,Map.of(type,type.equals("infantry")?140:14)));
        var behind=camera.position().subtract(forward.scale(12));
        rows.add(new Counter("behind","KOR","대한민국","tank","후방",1,behind.x,behind.y-3.1,behind.z,1,1,1,Map.of()));
        rows.add(new Counter("distant","KOR","대한민국","tank","원거리",1,client.player.getX()+1000,center.y,center.z,1,1,1,Map.of()));
        return new Snapshot(client.level.dimension().identifier().toString(),rows);
    }
    public static void run(ClientGameTestContext context) {
        context.setScreen(()->null);context.waitTicks(3);
        context.runOnClient(client->{
            var snapshot=fixture(client);
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),net.minecraft.core.RegistryAccess.EMPTY);
            try {
                Snapshot.CODEC.encode(b,snapshot);
                if(!snapshot.equals(Snapshot.CODEC.decode(b))||b.readableBytes()!=0)throw new AssertionError("Unit HUD codec");
                b.clear();b.writeUtf(snapshot.dimension(),128);b.writeVarInt(193);
                try{Snapshot.CODEC.decode(b);throw new AssertionError("Oversized HUD accepted");}catch(IllegalArgumentException expected){}
            }finally{b.release();}
            UnitHud.accept(snapshot);
        });
        context.waitTicks(3);
        context.runOnClient(client->{
            if(UnitHud.boxes().size()!=3)throw new AssertionError("HUD must cull behind camera and distant units: "+UnitHud.boxes());
            int x=client.getWindow().getGuiScaledWidth()/2,y=client.getWindow().getGuiScaledHeight()/2;
            if(UnitHud.boxes().stream().noneMatch(b->b.counter().type().equals("infantry")&&b.contains(x,y)))throw new AssertionError("Crosshair must select the infantry detail tooltip");
        });
        context.takeScreenshot("hoi-unit-hud-counters-and-hover");context.waitTicks(3);
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->{client.options.guiScale().set(2);UnitHud.accept(fixture(client));});
        context.waitTicks(3);context.takeScreenshot("hoi-unit-hud-counters-and-hover-gui2");
        context.getInput().resizeWindow(1600,1000);
        context.runOnClient(client->UnitHud.accept(fixture(client)));
        context.setScreen(()->new Sidebar(true));context.waitTicks(3);
        context.runOnClient(client->{if(!UnitHud.boxes().isEmpty())throw new AssertionError("Sidebar must cover counters");});
        context.setScreen(()->new Sidebar(false));context.waitTicks(3);
        context.runOnClient(client->{if(UnitHud.boxes().size()!=3)throw new AssertionError("World area retains counters behind sidebar");});
        context.setScreen(()->null);
        context.getInput().resizeWindow(854,480);context.waitTicks(3);
        context.runOnClient(client->UnitHud.accept(fixture(client)));context.waitTicks(3);
        context.runOnClient(client->{if(UnitHud.boxes().size()!=3)throw new AssertionError("Compact viewport counters");});
        context.takeScreenshot("hoi-unit-hud-compact");context.waitTicks(3);
        context.runOnClient(client->{
            UnitHud.accept(fixture(client));for(int i=0;i<40;i++)UnitHud.tick();
        });
        context.waitTicks(2);
        context.runOnClient(client->{
            if(!UnitHud.boxes().isEmpty())throw new AssertionError("Expired snapshot remains visible");
            UnitHud.accept(fixture(client));UnitHud.accept(new Snapshot("hoi:elsewhere",List.of()));
            if(!UnitHud.boxes().isEmpty())throw new AssertionError("Dimension change preserves private HUD");
            UnitHud.reset();
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }
    private static final class Sidebar extends Screen implements SidebarMovement.Screen {
        private final boolean covered;
        Sidebar(boolean covered){super(Component.literal("HUD 영역 검사"));this.covered=covered;}
        @Override public boolean allowsMovement(){return true;}
        @Override public int unitHudLeft(){return covered?width:80;}
        @Override public boolean isPauseScreen(){return false;}
        @Override public boolean isInGameUi(){return true;}
    }
}
