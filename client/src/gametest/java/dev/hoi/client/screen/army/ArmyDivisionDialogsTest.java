package dev.hoi.client.screen.army;

import dev.hoi.client.screen.commander.CommanderConfirmation;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;

public final class ArmyDivisionDialogsTest {
    private ArmyDivisionDialogsTest() {}
    private static MouseButtonEvent click(AbstractWidget b,int button){return new MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+b.getHeight()/2.,new MouseButtonInfo(button,0));}
    public static void run(ClientGameTestContext game,MenuView menu){
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var source=new DialogPayloads.Show(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-disband-confirm.json")),true).view().issued(UUID.randomUUID().toString(),7);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->{c.options.guiScale().set(2);dev.hoi.client.ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));try{var card=new com.google.gson.Gson().fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-army-panel-tooltips.json")),CommanderHudPayloads.Card.class);dev.hoi.client.map.commander.CommanderHud.accept(new CommanderHudPayloads.View("KOR",c.level.dimension().identifier().toString(),CommanderHudPayloads.Mode.ARMY,List.of(card)));dev.hoi.client.map.commander.CommanderArmyTooltipTest.selectCard(card.id());}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}});
                var views=new LinkedHashMap<String,DialogView>();views.put("disband-source",source);
                for(String name:List.of("kor-template-confirm","kor-template-list")){var p=Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),name+".json");if(Files.exists(p))views.put(name,new DialogPayloads.Show(Files.readString(p),true).view().issued(UUID.randomUUID().toString(),7));}
                if(!views.containsKey("kor-template-confirm"))views.put("template-confirm-synthetic",new DialogView(UUID.randomUUID().toString(),7,source.kind(),dev.hoi.client.ui.UiCommandLabels.text("CONFIRMCHANGE_UNIT_TEMPLATE",""),"","필요:\n§Y1367§! 인력(409K 이용 가능)\n\n획득:\n§Y100§! 보병 장비","","","",List.of(),source.choices()));
                for(var entry:views.entrySet()){
                    if(ArmyDivisionTemplates.matches(entry.getValue()))continue;
                    var view=entry.getValue();game.runOnClient(c->{DialogCoordinator.reset();DialogCoordinator.receive(new DialogPayloads.Show(new com.google.gson.Gson().toJson(view),true));if(!(c.gui.screen() instanceof CommanderConfirmation))throw new AssertionError("Division confirmation routing");});
                    for(String id:List.of("confirm","cancel")){
                        var sent=new ArrayList<String>();var screen=game.computeOnClient(c->new CommanderConfirmation(view,(v,choice)->sent.add(v.token()+"/"+v.revision()+"/"+choice)));game.setScreen(()->screen);game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.runOnClient(c->dev.hoi.client.map.commander.CommanderArmyTooltipTest.assertPanelBackdrop());game.takeScreenshot("hoi-"+entry.getKey()+"-"+id+"-"+size[0]+"-gui2");
                        game.runOnClient(c->{var b=screen.choiceButton(id);screen.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_RIGHT),false);if(!sent.isEmpty())throw new AssertionError("Right click confirmed");screen.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),false);screen.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),true);if(!sent.equals(List.of(view.token()+"/7/"+id)))throw new AssertionError("One issued action required");screen.update(view);if(screen.choiceButton(id).active)throw new AssertionError("Same revision unlocked");});
                    }
                }
                var longView=new DialogView(UUID.randomUUID().toString(),7,source.kind(),source.title(),source.subtitle(),source.body()+"\n\n"+source.body().repeat(3),source.flag(),source.image(),source.sound(),source.tiles(),source.choices());var longScreen=game.computeOnClient(c->new CommanderConfirmation(longView,(v,id)->{}));game.setScreen(()->longScreen);game.waitTicks(2);game.runOnClient(c->longScreen.mouseScrolled(longScreen.width/2.,longScreen.height/2.,0,-100));game.waitTicks(2);game.takeScreenshot("hoi-disband-long-scrolled-synthetic-"+size[0]+"-gui2");
                var blocked=new DialogView(UUID.randomUUID().toString(),7,source.kind(),source.title(),source.subtitle(),"§R전투 중인 사단은 해산할 수 없습니다.§!",source.flag(),source.image(),source.sound(),source.tiles(),source.choices().stream().filter(c->c.id().equals("cancel")).toList());var sent=new ArrayList<String>();var denied=game.computeOnClient(c->new CommanderConfirmation(blocked,(v,id)->sent.add(id)));game.setScreen(()->denied);game.waitTicks(2);game.takeScreenshot("hoi-disband-blocked-synthetic-"+size[0]+"-gui2");game.runOnClient(c->{if(denied.choiceButton("confirm")!=null)throw new AssertionError("Blocked confirm visible");denied.onClose();denied.onClose();if(!sent.equals(List.of("cancel")))throw new AssertionError("Cancel must be singular");});
                var tiles=new ArrayList<DialogView.Tile>();var choices=new ArrayList<DialogView.Choice>();for(int i=0;i<18;i++){String id="template/test-"+i;tiles.add(new DialogView.Tile(id,"시험 편제 "+i,"","unit_hud/infantry",i==0?"특수부대 상한을 초과하게 되므로 이 편제로 변경할 수 없습니다.":"편제 시험 툴팁"));if(i>0)choices.add(new DialogView.Choice(id,"시험 편제 "+i));}choices.add(new DialogView.Choice("cancel","취소"));
                var list=views.getOrDefault("kor-template-list",new DialogView(UUID.randomUUID().toString(),7,source.kind(),"사단 편제 변경","","","","","",tiles,choices));game.runOnClient(c->{DialogCoordinator.reset();var json=new com.google.gson.Gson();DialogCoordinator.receive(new DialogPayloads.Show(json.toJson(list),true));if(!(c.gui.screen() instanceof ArmyDivisionTemplates))throw new AssertionError("Template routing");DialogCoordinator.receive(new DialogPayloads.Show(json.toJson(source.issued(list.token(),list.revision())),true));if(!(c.gui.screen() instanceof ArmyDivisionTemplates))throw new AssertionError("Same revision replaced template");DialogCoordinator.receive(new DialogPayloads.Show(json.toJson(source.issued(list.token(),list.revision()+1)),true));if(!(c.gui.screen() instanceof CommanderConfirmation))throw new AssertionError("New revision confirmation routing");DialogCoordinator.reset();});var actions=new ArrayList<String>();var screen=game.computeOnClient(c->new ArmyDivisionTemplates(list,(v,id)->actions.add(id)));game.setScreen(()->screen);game.getInput().setCursorPos(48,280);game.waitTicks(3);game.takeScreenshot("hoi-template-list-"+(views.containsKey("kor-template-list")?"source":"synthetic")+"-"+size[0]+"-gui2");
                var blockedTemplate=list.tiles().stream().filter(t->list.choices().stream().noneMatch(choice->choice.id().equals(t.section()))).findFirst().orElseThrow();
                game.runOnClient(c->{for(int step=0;step<list.tiles().size()&&screen.choiceButton(blockedTemplate.section())==null;step++)screen.mouseScrolled(40,140,0,-1);});
                var blockedButton=game.computeOnClient(c->screen.choiceButton(blockedTemplate.section()));game.getInput().setCursorPos((blockedButton.getX()+blockedButton.getWidth()/2.)*2,(blockedButton.getY()+4)*2);game.waitTicks(10);game.takeScreenshot("hoi-template-blocked-tooltip-"+size[0]+"-gui2");
                game.runOnClient(c->screen.mouseScrolled(40,140,0,100));
                game.runOnClient(c->{var first=list.tiles().getFirst();var b=screen.choiceButton(first.section());if(list.choices().stream().noneMatch(x->x.id().equals(first.section()))){if(b.active)throw new AssertionError("Unissued template active");screen.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),false);if(!actions.isEmpty())throw new AssertionError("Disabled template sent");}screen.mouseScrolled(40,140,0,-100);});game.waitTicks(2);game.takeScreenshot("hoi-template-list-scrolled-"+size[0]+"-gui2");
                game.runOnClient(c->{if(list.tiles().size()>5){double x=121,y=dev.hoi.client.ui.UiMenuBar.height(screen.width)+110;var press=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));if(!screen.mouseClicked(press,false))throw new AssertionError("Scrollbar press");screen.mouseDragged(new MouseButtonEvent(x,y+120,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),0,120);screen.mouseReleased(press);if(screen.choiceButton(list.tiles().getLast().section())==null)throw new AssertionError("Scrollbar drag must expose last row");}});
                game.runOnClient(c->{var b=screen.children().stream().filter(x->x instanceof AbstractWidget w&&w.active).map(x->(AbstractWidget)x).findFirst().orElseThrow();screen.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),false);screen.mouseClicked(click(b,InputConstants.MOUSE_BUTTON_LEFT),true);if(actions.size()!=1)throw new AssertionError("Template choice must send once");screen.update(list);if(screen.children().stream().anyMatch(x->x instanceof AbstractWidget w&&w.active))throw new AssertionError("Same revision unlocked template");});
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.runOnClient(c->{DialogCoordinator.reset();dev.hoi.client.map.commander.CommanderHud.reset();c.options.guiScale().set(previous);});game.setScreen(()->null);}
    }
}
