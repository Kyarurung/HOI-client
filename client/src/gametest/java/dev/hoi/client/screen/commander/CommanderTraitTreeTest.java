package dev.hoi.client.screen.commander;

import dev.hoi.protocol.dialog.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.*;
import java.util.*;

public final class CommanderTraitTreeTest {
    private CommanderTraitTreeTest() {}
    private static DialogView source(String directory,String name) throws java.io.IOException {return new DialogPayloads.Show(Files.readString(Path.of(directory,name+".json")),true).view();}
    public static void overlay(ClientGameTestContext context,String name) throws java.io.IOException {
        var tree=source(System.getenv("HOI_COMMANDER_PREVIEW"),name).issued(UUID.randomUUID().toString(),0);
        var parent=context.computeOnClient(client->client.gui.screen());context.setScreen(()->new CommanderTraits(tree,parent,a->{}));context.waitTicks(2);
    }
    private static void hoverTracking(ClientGameTestContext context,DialogView view,String name,int[] resolution) {
        var node=view.tiles().stream().filter(t->t.traitNode()!=null&&!t.traitNode().terrain()&&t.traitNode().column()==0&&t.traitNode().row()>=0).findFirst().orElseThrow().traitNode();
        var cursor=context.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();return new int[]{screen.x(180),screen.y(63+27.5*node.row())};});
        dev.hoi.client.ui.UiTooltips.ScrolledTooltip previous=null;
        for(int step=0;step<2;step++) {
            context.getInput().setCursorPos((cursor[0]+step*6)*2,cursor[1]*2);context.waitTicks(3);
            var bounds=context.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var tip=screen.tooltipBounds();if(tip==null||tip.x()<4||tip.y()<4||tip.x()+tip.width()+4>screen.width||tip.y()+tip.height()+4>screen.height)throw new AssertionError("Trait tooltip must fit viewport");return tip;});
            if(previous!=null&&bounds.x()-previous.x()!=6)throw new AssertionError("Trait tooltip must follow cursor within the node");previous=bounds;
            context.takeScreenshot("hoi-trait-cursor-"+step+"-"+name+"-"+resolution[0]+"x"+resolution[1]+"-gui2");
        }
        context.getInput().setCursorPos(resolution[0]-5,80);context.waitTicks(2);
    }
    private static void leaderTooltips(ClientGameTestContext context,DialogView view,String name,int[] resolution) {
        var hero=view.tiles().stream().filter(t->t.section().equals("leader")).findFirst().orElseThrow();
        if(hero.gauge()==null||hero.gauge().tooltip().isBlank())throw new AssertionError("Reviewed source must include the authoritative experience gauge and tooltip");
        for(String target:List.of("experience","ability","preferred")){
            if(target.equals("preferred")&&view.tiles().stream().noneMatch(t->t.section().equals("preferred-tactic-info")))continue;
            var position=context.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();return new int[]{screen.x(target.equals("experience")?70:target.equals("ability")?71:80),screen.y(target.equals("experience")?76:target.equals("ability")?60:365)};});
            context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);
            context.runOnClient(client->{if(((CommanderTraits)client.gui.screen()).tooltipBounds()==null)throw new AssertionError("Leader source tooltip must be reachable: "+target);});
            context.takeScreenshot("hoi-trait-"+target+"-"+name+"-"+resolution[0]+"x"+resolution[1]+"-gui2");
        }
        context.getInput().setCursorPos(resolution[0]-5,80);context.waitTicks(2);
    }
    public static void run(ClientGameTestContext context) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            var tree=source(directory,"kor-marshal-trait-tree").issued(UUID.randomUUID().toString(),4);var sent=new ArrayList<DialogPayloads.Action>();
            context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new CommanderTraits(tree,null,sent::add));context.waitTicks(3);
            var position=context.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var button=screen.choiceButton("fast_planner");if(button==null||!button.active)throw new AssertionError("Server-issued trait purchase must be clickable");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
            context.getInput().setCursorPos(2500,1400);context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-tree-marshal-gui2");
            context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-tree-native-tooltip-gui2");
            context.runOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(!sent.equals(List.of(new DialogPayloads.Action(tree.token(),4,"fast_planner"))))throw new AssertionError("Trait must send one issued token/revision/choice");screen.update(tree.issued(tree.token(),3));screen.mouseClicked(click,false);if(sent.size()!=1)throw new AssertionError("Stale trait reply must not unlock pending purchase");screen.update(tree.issued(tree.token(),5));if(!screen.choiceButton("fast_planner").active)throw new AssertionError("Authoritative rejection must unlock purchase");});
            var confirmation=source(directory,"kor-marshal-trait-confirm").issued(UUID.randomUUID().toString(),7);var confirmSent=new ArrayList<DialogPayloads.Action>();context.setScreen(()->new CommanderTraits(confirmation,null,confirmSent::add));context.waitTicks(3);context.getInput().setCursorPos(2500,1400);context.takeScreenshot("hoi-commander-trait-confirm-gui2");
            context.runOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var cancel=screen.choiceButton("cancel");screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(cancel.getX()+2,cancel.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);if(!confirmSent.equals(List.of(new DialogPayloads.Action(confirmation.token(),7,"cancel"))))throw new AssertionError("Cancel must send its own issued choice");});
            for(int[] resolution:List.of(new int[]{1280,720},new int[]{2560,1440}))for(String name:List.of("kor-marshal-trait-owned","kor-jeon-trait-tree","kor-navy-trait-tree")) {context.getInput().resizeWindow(resolution[0],resolution[1]);var view=source(directory,name).issued(UUID.randomUUID().toString(),0);context.setScreen(()->new CommanderTraits(view,null,a->{}));context.waitTicks(3);if(name.equals("kor-navy-trait-tree"))context.runOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var node=view.tiles().stream().filter(t->t.section().equals("torpedo_bomber")).findFirst().orElseThrow().traitNode();if(!screen.visible(node))throw new AssertionError("Admiral bomber trait must be visible before scrolling");});context.takeScreenshot("hoi-commander-"+name+"-"+resolution[0]+"x"+resolution[1]+"-gui2");hoverTracking(context,view,name,resolution);leaderTooltips(context,view,name,resolution);}
            context.runOnClient(client->{var screen=(CommanderTraits)client.gui.screen();int x=screen.width/2,y=screen.height/2;if(!screen.mouseScrolled(x,y,0,-20))throw new AssertionError("Overflowing regular tree must accept independent scroll");});context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-regular-scroll-gui2");
            context.getInput().resizeWindow(854,480);context.setScreen(()->new CommanderTraits(tree,null,a->{}));context.waitTicks(3);context.takeScreenshot("hoi-commander-trait-tree-small-window");
            context.getInput().resizeWindow(2560,1440);
            CommanderAdvisorTest.run(context,directory);
        } catch(java.io.IOException error){throw new AssertionError("Server trait tree fixtures unavailable",error);}
    }
}
