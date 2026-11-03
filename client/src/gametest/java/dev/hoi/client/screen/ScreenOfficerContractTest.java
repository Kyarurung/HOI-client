package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;

public final class ScreenOfficerContractTest {
    private static ProtocolDialogView source(String name)throws java.io.IOException{return new ProtocolDialogPayloads.Show(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),name+".json")),true).view().issued(UUID.randomUUID().toString(),7);}
    private static MouseButtonEvent click(AbstractWidget b,int button){return new MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+2,new MouseButtonInfo(button,0));}
    public static void run(ClientGameTestContext game){
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try{
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                for(String branch:List.of("field-officer","ship-captain")){
                    var view=source("kor-"+branch+"s");var confirm=source("kor-"+branch+"-promote");var roster=source(branch.equals("ship-captain")?"kor-navy-admirals":"kor-army-roster-tooltips");
                    if(branch.equals("field-officer")){if(!Integer.valueOf(12).equals(view.tiles().getFirst().officer().insignia())||view.tiles().stream().skip(1).anyMatch(t->t.officer().insignia()!=null))throw new AssertionError("Field officer insignia contract");}else if(view.tiles().stream().anyMatch(t->!Integer.valueOf(3).equals(t.officer().insignia())))throw new AssertionError("Fleet default insignia contract");
                    var actions=new ArrayList<String>();
                    var screen=game.computeOnClient(c->new ScreenOfficerRoster(view,null,(v,id)->actions.add(id)));game.setScreen(()->screen);
                    game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-"+branch+"-list-"+size[0]+"-gui2");
                    var offered=view.choices().stream().filter(c->c.id().startsWith("promote/")).findFirst().orElseThrow();
                    game.runOnClient(c->{var search=(net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();var selected=view.tiles().stream().filter(t->offered.id().equals("promote/"+t.section())).findFirst().orElseThrow();search.setValue(selected.name());if(screen.choiceButton(offered.id())==null||!screen.choiceButton(offered.id()).active)throw new AssertionError("Search lost offered officer");var filtered=(net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();filtered.setValue("no-matching-officer");if(screen.children().stream().anyMatch(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("진급")))throw new AssertionError("Search retained unrelated officers");var empty=(net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();empty.setValue("");screen.mouseScrolled(screen.width/2.,screen.height/2.,0,-100);screen.mouseScrolled(screen.width/2.,screen.height/2.,0,100);if(screen.choiceButton(offered.id())==null||!actions.isEmpty())throw new AssertionError("Roster navigation mutated promotion");});
                    var button=game.computeOnClient(c->screen.choiceButton(offered.id()));
                    game.getInput().setCursorPos((button.getX()+4)*2,(button.getY()+4)*2);game.waitTicks(3);game.takeScreenshot("hoi-"+branch+"-tooltip-"+size[0]+"-gui2");
                    game.runOnClient(c->{screen.mouseClicked(click(button,InputConstants.MOUSE_BUTTON_RIGHT),false);if(!actions.isEmpty())throw new AssertionError("Right click promoted officer");screen.mouseClicked(click(button,InputConstants.MOUSE_BUTTON_LEFT),false);screen.mouseClicked(click(button,InputConstants.MOUSE_BUTTON_LEFT),true);screen.update(view);if(!actions.equals(List.of(offered.id()))||screen.choiceButton(offered.id()).active)throw new AssertionError("Officer promotion must lock repeated same revision");});
                    var denied=new ProtocolDialogView(view.token(),8,view.kind(),view.title(),view.subtitle(),view.body(),view.flag(),view.image(),view.sound(),view.tiles(),List.of());
                    game.runOnClient(c->{screen.update(denied);if(screen.choiceButton(offered.id()).active)throw new AssertionError("Unoffered promotion enabled");screen.choose(offered.id());if(actions.size()!=1)throw new AssertionError("Unoffered promotion sent");});
                    for(String choice:List.of("promote","cancel")){
                        actions.clear();var modal=game.computeOnClient(c->new ScreenCommanderConfirmation(confirm,(v,id)->actions.add(id)));
                        game.runOnClient(c->modal.backdrop(new ScreenCommanderRoster(roster,null,a->{})));game.setScreen(()->modal);game.runOnClient(c->modal.backdrop().init(modal.width,modal.height));
                        game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-"+branch+"-confirm-"+size[0]+"-gui2");
                        game.runOnClient(c->{if(branch.equals("field-officer")&&!modal.bodyText().getString().contains("만큼"))throw new AssertionError("Field officer promotion lost 만큼");var b=modal.choiceButton(choice);modal.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),false);modal.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),true);modal.update(confirm);if(!actions.equals(List.of(choice))||modal.choiceButton(choice).active)throw new AssertionError("Confirmation must submit once");});
                    }
                    game.runOnClient(c->{ScreenDialogCoordinator.reset();ScreenDialogCoordinator.receive(new ProtocolDialogPayloads.Show(new com.google.gson.Gson().toJson(view),true));if(!(c.gui.screen() instanceof ScreenOfficerRoster))throw new AssertionError("Officer list route");ScreenDialogCoordinator.receive(new ProtocolDialogPayloads.Show(new com.google.gson.Gson().toJson(confirm),true));if(!(c.gui.screen() instanceof ScreenCommanderConfirmation))throw new AssertionError("Officer confirmation route");});
                }
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.runOnClient(c->{ScreenDialogCoordinator.reset();c.options.guiScale().set(previous);});game.setScreen(()->null);}
    }
}
