package dev.hoi.client.map.commander;

import dev.hoi.client.map.agency.AgencyHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.client.ui.tooltip.TooltipDelay;
import dev.hoi.protocol.commander.CommanderAirWingPayloads;
import dev.hoi.protocol.commander.CommanderAirWingPayloads.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

public final class CommanderAirWingHud {
    public static Consumer<Request> requestObserver;
    private static CommanderAirWingWindow window;
    private CommanderAirWingHud() {}
    static void register() {
        CommanderAirWingPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(Snapshot.TYPE,(packet,context)->accept(packet.view()));
        HudElementRegistry.attachElementAfter(Identifier.parse("hoi:ship_window"),Identifier.parse("hoi:air_wing_window"),(g,delta)->{if(Minecraft.getInstance().gui.screen()==null)draw(g,g.guiWidth()/2,g.guiHeight()/2);});
    }
    static void send(Request request){if(requestObserver!=null)requestObserver.accept(request);else if(ClientPlayNetworking.canSend(Request.TYPE))ClientPlayNetworking.send(request);}
    public static CommanderAirWingWindow window(){return window;}
    public static void refresh(){if(window!=null)window.request();}
    public static void open(String wing) {
        if(window!=null&&window.wing().equals(wing))return;
        var next=new CommanderAirWingWindow(wing);
        if(window!=null){next.place(window);window.close();}
        window=next;layout();window.open();
    }
    public static void accept(View view){if(window!=null&&!window.accept(view))close();}
    private static void layout() {
        var client=Minecraft.getInstance();int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight();
        window.layout(width,height,CommanderLayout.units(width,height).scale()*.5f);
    }
    private static boolean visible() {
        var client=Minecraft.getInstance();
        return window!=null&&client.level!=null&&!client.gui.hud.isHidden()&&!AgencyHud.active()&&(client.gui.screen()==null||client.gui.screen() instanceof MapCursor);
    }
    public static boolean covers(double x,double y){return visible()&&window.contains(x,y);}
    public static void draw(GuiGraphicsExtractor g,int mx,int my) {
        if(!visible())return;
        layout();window.draw(g,Minecraft.getInstance().font);
        String tooltip=window.tooltip(mx,my);if(!tooltip.isEmpty())CommanderHud.drawTooltip(g,mx,my,TooltipDelay.visible(tooltip));
    }
    public static void drawBackdrop(GuiGraphicsExtractor g){if(window!=null){layout();window.draw(g,Minecraft.getInstance().font);}}
    public static boolean click(MouseButtonEvent event){return visible()&&window.mouseClicked(event,CommanderAirWingHud::close);}
    public static boolean drag(MouseButtonEvent event){if(!visible()||!window.mouseDragged(event))return false;layout();return true;}
    public static boolean release(){return window!=null&&window.mouseReleased()&&visible();}
    static boolean closeWindow(){if(!visible())return false;close();return true;}
    private static void close(){var old=window;window=null;old.close();}
    public static void reset(){if(window!=null)close();}
}
