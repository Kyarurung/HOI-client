package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

public final class ScreenAdmiralSourceTest {
    private ScreenAdmiralSourceTest() {}
    public static void run(ClientGameTestContext context){
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try{
            var view=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-navy-admirals.json")),true).view().issued(UUID.randomUUID().toString(),7);var sent=new ArrayList<ProtocolDialogPayloads.Action>();String choice=view.choices().stream().filter(c->c.id().startsWith("admiral/")).findFirst().orElseThrow().id();String name=view.tiles().stream().filter(t->t.section().equals(choice)).findFirst().orElseThrow().name();context.getInput().resizeWindow(1280,720);context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new ScreenCommanderRoster(view,null,sent::add));context.waitTicks(3);
            var position=context.computeOnClient(client->{var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.active&&c.getMessage().getString().equals(name)).findFirst().orElseThrow();return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-navy-admiral-assignment-gui2");
            context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(sent.size()!=1||!sent.getFirst().equals(new ProtocolDialogPayloads.Action(view.token(),7,choice)))throw new AssertionError("Admiral click must send exactly one issued action");});
        }catch(java.io.IOException error){throw new AssertionError("Admiral server fixture unavailable",error);}
    }
}
