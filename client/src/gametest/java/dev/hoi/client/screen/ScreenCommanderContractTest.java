package dev.hoi.client.screen;

import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;

public final class ScreenCommanderContractTest {
    private ScreenCommanderContractTest() {}
    private static ProtocolDialogView source(String name)throws java.io.IOException{return new ProtocolDialogPayloads.Show(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),name+".json")),true).view().issued(UUID.randomUUID().toString(),7);}
    private static MouseButtonEvent click(AbstractWidget b,int modifiers,int button){return new MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+2,new MouseButtonInfo(button,modifiers));}
    public static void run(ClientGameTestContext game){
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try{
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                var source=source("kor-army-roster-tooltips");var candidate=source.tiles().stream().filter(t->t.leader()!=null&&source.choices().stream().anyMatch(c->c.id().equals("deploy/"+t.section()))).findFirst().orElseThrow();
                game.runOnClient(c->{String parsed=dev.hoi.client.ui.UiKeywordIcons.decorateLines(dev.hoi.client.ui.UiTooltips.officerText("40.00 £command_power의 지휘력이 £command_power£ 유지"),false).getString();if(parsed.contains("의")||parsed.contains("£")||!parsed.contains(" 지휘력이 ")||!parsed.endsWith(" 유지"))throw new AssertionError("Source icon token suffix must be consumed");});
                var sent=new ArrayList<ProtocolDialogPayloads.Action>();var screen=game.computeOnClient(c->new ScreenCommanderRoster(source,null,sent::add));game.setScreen(()->screen);
                game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-roster-full-rows-"+size[0]+"-gui2");
                game.runOnClient(c->{var toggle=screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("임명된 지휘관 보기")).map(w->(AbstractWidget)w).findFirst().orElseThrow();screen.mouseClicked(click(toggle,0,InputConstants.MOUSE_BUTTON_LEFT),false);try{var rows=ScreenCommanderRoster.class.getDeclaredField("rows");rows.setAccessible(true);if(((List<ProtocolDialogView.Tile>)rows.get(screen)).stream().anyMatch(t->t.leader().assigned()))throw new AssertionError("Assigned leaders must disappear without a search");}catch(ReflectiveOperationException e){throw new RuntimeException(e);}});game.waitTicks(3);game.takeScreenshot("hoi-roster-hide-assigned-"+size[0]+"-gui2");
                game.runOnClient(c->{var toggle=screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("임명된 지휘관 보기")).map(w->(AbstractWidget)w).findFirst().orElseThrow();screen.mouseClicked(click(toggle,0,InputConstants.MOUSE_BUTTON_LEFT),false);});
                game.runOnClient(c->screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).map(w->(net.minecraft.client.gui.components.EditBox)w).findFirst().orElseThrow().setValue(candidate.name()));game.waitTicks(2);
                var row=game.computeOnClient(c->screen.choiceButton(candidate.section()));
                var tips=List.of(candidate.leader().skillTooltip(),candidate.leader().capacityTooltip(),candidate.leader().assignTooltip(),UiCommandLabels.withoutShortcuts(candidate.detail()));
                int[][] points={{row.getX()+14,row.getY()+12},{row.getX()+230,row.getY()+12},{row.getX()+150,row.getY()+2},{row.getX()+40,row.getY()+10}};
                for(int i=0;i<tips.size();i++){int index=i;game.getInput().setCursorPos(points[i][0]*2,points[i][1]*2);game.waitTicks(3);game.runOnClient(c->{if(tips.get(index).isBlank()||!screen.renderedTooltip().equals(tips.get(index)))throw new AssertionError("Source roster tooltip zone "+index+" differs");});game.takeScreenshot("hoi-roster-contract-tooltip-"+i+"-"+size[0]+"x"+size[1]+"-gui2");}
                for(boolean deploy:List.of(true,false)){
                    var view=deploy?source:new ProtocolDialogView(source.token(),9,source.kind(),source.title(),source.subtitle(),source.body(),source.flag(),source.image(),source.sound(),source.tiles(),source.choices().stream().filter(c->!c.id().startsWith("deploy/")).toList());
                    game.runOnClient(c->{screen.update(view);sent.clear();var b=screen.choiceButton(candidate.section());screen.mouseClicked(click(b,InputConstants.MOD_SHIFT,InputConstants.MOUSE_BUTTON_RIGHT),false);if(!sent.isEmpty())throw new AssertionError("Right click must not assign");screen.mouseClicked(click(b,InputConstants.MOD_SHIFT,InputConstants.MOUSE_BUTTON_LEFT),false);screen.mouseClicked(click(b,InputConstants.MOD_SHIFT,InputConstants.MOUSE_BUTTON_LEFT),true);String expected=(deploy?"deploy/":"")+candidate.section();if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),view.revision(),expected))))throw new AssertionError("Shift selection must use the offered deployment or fall back to assignment: "+sent);});
                }
                for(String file:List.of("kor-advisor-promote-confirm","kor-promote-confirm")){
                    var confirmation=source(file);
                    game.setScreen(()->null);game.runOnClient(c->{ScreenDialogCoordinator.reset();ScreenDialogCoordinator.receive(new ProtocolDialogPayloads.Show(new com.google.gson.Gson().toJson(confirmation),true));if(!(c.gui.screen() instanceof ScreenCommanderConfirmation))throw new AssertionError("Server confirmation must route to its modal");});
                    for(String id:confirmation.choices().stream().map(ProtocolDialogView.Choice::id).toList()){
                        var actions=new ArrayList<ProtocolDialogPayloads.Action>();var modal=game.computeOnClient(c->new ScreenCommanderConfirmation(confirmation,(v,choice)->actions.add(new ProtocolDialogPayloads.Action(v.token(),v.revision(),choice))));game.runOnClient(c->modal.backdrop(new ScreenCommanderRoster(source,null,sent::add)));game.setScreen(()->modal);game.runOnClient(c->{try{var f=ScreenCommanderConfirmation.class.getDeclaredField("parent");f.setAccessible(true);((ScreenCommanderRoster)f.get(modal)).init(modal.width,modal.height);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}});game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-"+file+"-"+id+"-"+size[0]+"x"+size[1]+"-gui2");
                        game.runOnClient(c->{var b=modal.choiceButton(id);if(b==null||!b.active||modal.bodyText().getString().contains("짙"))throw new AssertionError("Confirmation must preserve offered choices and keyword icons");modal.mouseClicked(click(b,0,InputConstants.MOUSE_BUTTON_RIGHT),false);if(!actions.isEmpty())throw new AssertionError("Right click must not confirm");modal.mouseClicked(click(b,0,InputConstants.MOUSE_BUTTON_LEFT),false);modal.mouseClicked(click(b,0,InputConstants.MOUSE_BUTTON_LEFT),true);if(!actions.equals(List.of(new ProtocolDialogPayloads.Action(confirmation.token(),7,id))))throw new AssertionError("Confirmation must send one authenticated action");modal.update(confirmation);if(modal.choiceButton(id).active)throw new AssertionError("Same revision must not unlock repeated confirmation");});
                    }
                    var denied=new ProtocolDialogView(UUID.randomUUID().toString(),7,confirmation.kind(),confirmation.title(),confirmation.subtitle(),confirmation.body(),confirmation.flag(),confirmation.image(),confirmation.sound(),confirmation.tiles(),confirmation.choices().stream().filter(c->c.id().equals("cancel")).toList());
                    var actions=new ArrayList<String>();var modal=game.computeOnClient(c->new ScreenCommanderConfirmation(denied,(v,choice)->actions.add(choice)));game.setScreen(()->modal);game.runOnClient(c->{if(modal.choiceButton("confirm")!=null||modal.choiceButton("promote")!=null)throw new AssertionError("Insufficient cost must not create an approval choice");modal.onClose();modal.onClose();if(!actions.equals(List.of("cancel")))throw new AssertionError("Closing must cancel once");});
                }
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.runOnClient(c->{ScreenDialogCoordinator.reset();c.options.guiScale().set(previous);});game.setScreen(()->null);}
    }
}
