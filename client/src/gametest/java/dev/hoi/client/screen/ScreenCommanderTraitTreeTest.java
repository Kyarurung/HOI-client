package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.*;
import java.util.*;

public final class ScreenCommanderTraitTreeTest {
    private ScreenCommanderTraitTreeTest() {}
    private static ProtocolDialogView source(String directory,String name) throws java.io.IOException {return new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,name+".json")),true).view();}
    public static void run(ClientGameTestContext context) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            var tree=source(directory,"kor-marshal-trait-tree").issued(UUID.randomUUID().toString(),4);var sent=new ArrayList<ProtocolDialogPayloads.Action>();
            context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new ScreenCommanderTraits(tree,null,sent::add));context.waitTicks(3);
            var position=context.computeOnClient(client->{var screen=(ScreenCommanderTraits)client.gui.screen();var button=screen.choiceButton("fast_planner");if(button==null||!button.active)throw new AssertionError("Server-issued trait purchase must be clickable");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
            context.getInput().setCursorPos(2500,1400);context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-tree-marshal-gui2");
            context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-tree-native-tooltip-gui2");
            context.runOnClient(client->{var screen=(ScreenCommanderTraits)client.gui.screen();var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(tree.token(),4,"fast_planner"))))throw new AssertionError("Trait must send one issued token/revision/choice");screen.update(tree.issued(tree.token(),3));screen.mouseClicked(click,false);if(sent.size()!=1)throw new AssertionError("Stale trait reply must not unlock pending purchase");screen.update(tree.issued(tree.token(),5));if(!screen.choiceButton("fast_planner").active)throw new AssertionError("Authoritative rejection must unlock purchase");});
            var confirmation=source(directory,"kor-marshal-trait-confirm").issued(UUID.randomUUID().toString(),7);var confirmSent=new ArrayList<ProtocolDialogPayloads.Action>();context.setScreen(()->new ScreenCommanderTraits(confirmation,null,confirmSent::add));context.waitTicks(3);context.getInput().setCursorPos(2500,1400);context.takeScreenshot("hoi-commander-trait-confirm-gui2");
            context.runOnClient(client->{var screen=(ScreenCommanderTraits)client.gui.screen();var cancel=screen.choiceButton("cancel");screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(cancel.getX()+2,cancel.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);if(!confirmSent.equals(List.of(new ProtocolDialogPayloads.Action(confirmation.token(),7,"cancel"))))throw new AssertionError("Cancel must send its own issued choice");});
            for(String name:List.of("kor-marshal-trait-owned","kor-jeon-trait-tree","kor-navy-trait-tree")) {var view=source(directory,name).issued(UUID.randomUUID().toString(),0);context.setScreen(()->new ScreenCommanderTraits(view,null,a->{}));context.waitTicks(3);context.takeScreenshot("hoi-commander-"+name+"-gui2");}
            context.runOnClient(client->{var screen=(ScreenCommanderTraits)client.gui.screen();int x=screen.width/2,y=screen.height/2;if(!screen.mouseScrolled(x,y,0,-20))throw new AssertionError("Overflowing regular tree must accept independent scroll");});context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-regular-scroll-gui2");
            context.getInput().resizeWindow(854,480);context.setScreen(()->new ScreenCommanderTraits(tree,null,a->{}));context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-tree-small-window");
            context.getInput().resizeWindow(2560,1440);
            ScreenCommanderAdvisorTest.run(context,directory);
        } catch(java.io.IOException error){throw new AssertionError("Server trait tree fixtures unavailable",error);}
    }
}
