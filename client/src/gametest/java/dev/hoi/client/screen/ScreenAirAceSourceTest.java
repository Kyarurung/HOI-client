package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

public final class ScreenAirAceSourceTest {
    private ScreenAirAceSourceTest() {}
    public static void run(ClientGameTestContext context){
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try{
            for(String fixture:List.of("kor-air-aces","kor-air-aces-assigned")){
                var view=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,fixture+".json")),true).view().issued(UUID.randomUUID().toString(),5);var sent=new ArrayList<ProtocolDialogPayloads.Action>();String choice=fixture.endsWith("assigned")?"remove":view.choices().stream().filter(c->c.id().startsWith("pilot/")).findFirst().orElseThrow().id();String name=view.tiles().stream().filter(t->t.section().equals(choice)).findFirst().orElseThrow().name();
                context.getInput().resizeWindow(1280,720);context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new ScreenCommanderRoster(view,null,sent::add));context.waitTicks(3);
                var position=context.computeOnClient(client->{var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.active&&c.getMessage().getString().equals(name)).findFirst().orElseThrow();if(button.getX()<0||button.getY()<0||button.getRight()>client.getWindow().getGuiScaledWidth()||button.getBottom()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Ace action outside GUI2 viewport");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
                context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-"+fixture+"-gui2");
                context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(sent.size()!=1||!sent.getFirst().equals(new ProtocolDialogPayloads.Action(view.token(),5,choice)))throw new AssertionError("Ace row must send exactly one server-issued action");screen.update(view.issued(view.token(),4));screen.mouseClicked(click,false);if(sent.size()!=1)throw new AssertionError("Stale ace refresh released pending action");});
            }
        }catch(java.io.IOException error){throw new AssertionError("Ace server fixtures unavailable",error);}
    }
}
