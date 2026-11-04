package dev.hoi.client.screen.commander;

import dev.hoi.protocol.dialog.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.*;
import java.util.*;

public final class CommanderAdmiralSourceTest {
    private CommanderAdmiralSourceTest() {}
    public static void run(ClientGameTestContext context){
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try{for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            var view=new DialogPayloads.Show(Files.readString(Path.of(directory,"kor-navy-admirals-source.json")),true).view().issued(UUID.randomUUID().toString(),7);var sent=new ArrayList<DialogPayloads.Action>();String choice=view.choices().stream().filter(c->c.id().startsWith("admiral/")).findFirst().orElseThrow().id();var candidate=view.tiles().stream().filter(t->t.section().equals(choice)).findFirst().orElseThrow();String name=candidate.name();if(candidate.leader()==null||!candidate.leader().promotion().isEmpty())throw new AssertionError("Admiral source must provide native leader data without promotion");context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new CommanderRoster(view,null,sent::add));context.waitTicks(3);
            context.getInput().setCursorPos(size[0]-5,size[1]-5);context.waitTicks(3);context.takeScreenshot("hoi-navy-admiral-assignment-"+size[0]+"x"+size[1]+"-gui2");
            context.runOnClient(client->{var search=client.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();search.setValue(name);});context.waitTicks(2);
            var position=context.computeOnClient(client->{var button=((CommanderRoster)client.gui.screen()).choiceButton(choice);if(button==null||!button.active)throw new AssertionError("Searched admiral must retain its issued assignment choice");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
            context.runOnClient(client->{var screen=(CommanderRoster)client.gui.screen();String id="traits/"+choice.substring(8);var details=screen.choiceButton(id);if(details==null||!details.active||screen.choiceButton("promote/"+choice.substring(8))!=null)throw new AssertionError("Admiral row requires only its authorized details control");var click=new net.minecraft.client.input.MouseButtonEvent(details.getX()+2,details.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(!sent.equals(List.of(new DialogPayloads.Action(view.token(),7,id))))throw new AssertionError("Admiral details must not assign the row behind it");sent.clear();});context.setScreen(()->new CommanderRoster(view,null,sent::add));context.waitTicks(2);
            context.runOnClient(client->{var screen=(CommanderRoster)client.gui.screen();screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow().setValue(name);var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(sent.size()!=1||!sent.getFirst().equals(new DialogPayloads.Action(view.token(),7,choice)))throw new AssertionError("Admiral click must send exactly one issued action");});
        }}catch(java.io.IOException error){throw new AssertionError("Admiral server fixture unavailable",error);}
    }
}
