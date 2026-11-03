package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;

public final class ScreenPreferredTacticsTest {
    private ScreenPreferredTacticsTest() {}
    private static ProtocolDialogView fixture(String name) throws Exception {
        String directory=System.getenv("HOI_TACTIC_PREVIEW");
        if(directory==null||directory.isBlank())throw new AssertionError("Fresh authoritative tactic fixtures required: run :client:runClientGameTest so prepareRendererFixtures supplies HOI_TACTIC_PREVIEW");
        if(!Files.isDirectory(Path.of(directory)))throw new AssertionError("Generated authoritative tactic directory is missing: "+directory);
        return new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,name+".json")),true).view();
    }
    private static void click(ClientGameTestContext context,String id) {
        context.runOnClient(client->{
            var screen=(ScreenPreferredTactics)client.gui.screen();var button=screen.choiceButton(id);
            if(button==null)throw new AssertionError("Missing visible tactic action "+id);
            screen.mouseClicked(new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);
        });
    }
    private static void hover(ClientGameTestContext context,String id) {
        var point=context.computeOnClient(client->{
            var button=((ScreenPreferredTactics)client.gui.screen()).choiceButton(id);
            return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};
        });
        float scale=context.computeOnClient(client->client.getWindow().getGuiScale());
        context.getInput().setCursorPos(Math.round(point[0]*scale),Math.round(point[1]*scale));context.waitTicks(3);
        context.runOnClient(client->{
            var screen=(ScreenPreferredTactics)client.gui.screen();var bounds=screen.tooltipBounds();
            if(bounds==null||bounds.x()-4<0||bounds.y()-4<0||bounds.x()+bounds.width()+4>screen.width||bounds.y()+bounds.height()+4>screen.height)
                throw new AssertionError("Visible unlocked, selected and disabled tactic rows must render their bounded source tooltip");
        });
    }
    public static void run(ClientGameTestContext context) {
        try {
            var national=fixture("national-tactic-choices");var leader=fixture("leader-tactic-choices");
            if(national.tiles().stream().anyMatch(t->t.tactic()==null)||leader.tiles().stream().anyMatch(t->!t.value().isEmpty()))
                throw new AssertionError("Current native tactic metadata and separate leader quote required");
            for(var resolution:List.of(new int[]{2560,1440,2},new int[]{1280,720,2},new int[]{1920,1080,2},new int[]{2560,1440,1},new int[]{1280,720,3})) {
                context.getInput().resizeWindow(resolution[0],resolution[1]);context.runOnClient(client->client.options.guiScale().set(resolution[2]));
                var actions=new ArrayList<ProtocolDialogPayloads.Action>();
                context.setScreen(()->new ScreenPreferredTactics(national,null,actions::add));
                context.runOnClient(client->{
                    var screen=(ScreenPreferredTactics)client.gui.screen();
                    for(var child:screen.children())if(child instanceof AbstractWidget widget)
                        if(widget.getX()<0||widget.getY()<0||widget.getX()+widget.getWidth()>screen.width||widget.getY()+widget.getHeight()>screen.height)
                            throw new AssertionError("Tactic hitbox outside viewport "+widget.getMessage().getString());
                });
                hover(context,"tactic_basic_attack");
                context.runOnClient(client->{
                    var screen=(ScreenPreferredTactics)client.gui.screen();
                    var units=new dev.hoi.client.ui.UiUnits(screen.width,screen.height);
                    if(resolution[2]==2&&screen.bodyLineCount()<2)throw new AssertionError("Full native explanation must wrap instead of shrinking into one unreadable line");
                    if(screen.bodyLineCount()*(client.font.lineHeight+1)*screen.bodyTextScale()>screen.bodyAreaHeight()+.001f)throw new AssertionError("Tactic explanation escaped its own body rectangle");
                    if(screen.bodyTextScale()<units.scale()*.5f)throw new AssertionError("Native tactic explanation became unreadably small: "+Arrays.toString(resolution)+" scale="+screen.bodyTextScale()+" units="+units.scale()+" lines="+screen.bodyLineCount());
                });
                context.takeScreenshot("hoi-national-tactics-"+resolution[0]+"x"+resolution[1]+"-gui"+resolution[2]);
                click(context,"filter/2");hover(context,"tactic_basic_defend");context.takeScreenshot("hoi-national-defensive-tactics-"+resolution[0]+"x"+resolution[1]+"-gui"+resolution[2]);
                context.runOnClient(client->{
                    var screen=(ScreenPreferredTactics)client.gui.screen();
                    if(screen.choiceButton("tactic_basic_attack")!=null||screen.choiceButton("tactic_basic_defend")==null||!actions.isEmpty())
                        throw new AssertionError("Defensive filter must be local and preserve choice state");
                });
                click(context,"filter/1");click(context,"tactic_basic_attack");click(context,"tactic_basic_attack");
                context.runOnClient(client->{
                    var screen=(ScreenPreferredTactics)client.gui.screen();screen.update(national);
                    if(actions.size()!=1||screen.choiceButton("tactic_basic_attack").active)throw new AssertionError("Duplicate or stale tactic submission");
                    screen.update(national.issued(national.token(),national.revision()+1));
                    if(!screen.choiceButton("tactic_basic_attack").active)throw new AssertionError("New revision must allow a refreshed choice");
                    screen.onClose();
                    if(actions.size()!=2||!actions.getLast().choice().isEmpty())throw new AssertionError("Cancel must send one close action");
                });
            }
            context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
            var single=new ProtocolDialogView(national.token(),national.revision(),national.kind(),national.title(),national.subtitle(),national.body(),national.flag(),national.image(),national.sound(),national.tiles(),List.of(national.choices().stream().filter(c->c.id().equals("tactic_basic_attack")).findFirst().orElseThrow()));
            var keyboard=new ArrayList<ProtocolDialogPayloads.Action>();context.setScreen(()->new ScreenPreferredTactics(single,null,keyboard::add));
            context.runOnClient(client->{
                var screen=(ScreenPreferredTactics)client.gui.screen();var enter=new net.minecraft.client.input.KeyEvent(InputConstants.KEY_RETURN,0,0);
                screen.keyPressed(enter);screen.keyPressed(enter);
                if(keyboard.size()!=1||!keyboard.getFirst().choice().equals("tactic_basic_attack"))throw new AssertionError("Keyboard selection must use the same bounded single-submission path");
            });
            var insufficient=fixture("national-tactic-insufficient");var locked=new ArrayList<ProtocolDialogPayloads.Action>();
            context.setScreen(()->new ScreenPreferredTactics(insufficient,null,locked::add));hover(context,"tactic_basic_attack");context.takeScreenshot("hoi-national-tactics-insufficient-cp-gui2");click(context,"tactic_basic_attack");
            context.runOnClient(client->{if(!locked.isEmpty())throw new AssertionError("Insufficient national CP cannot issue a tactic action");});
            var actions=new ArrayList<ProtocolDialogPayloads.Action>();context.setScreen(()->new ScreenPreferredTactics(leader,null,actions::add));
            hover(context,leader.tiles().getFirst().section());context.takeScreenshot("hoi-general-preferred-tactics-gui2");click(context,leader.tiles().getFirst().section());
            if(actions.size()!=1||!actions.getFirst().token().equals(leader.token()))throw new AssertionError("Leader must use its own issued session");
            var low=fixture("leader-tactic-low-skill");var rejected=new ArrayList<ProtocolDialogPayloads.Action>();
            context.setScreen(()->new ScreenPreferredTactics(low,null,rejected::add));hover(context,leader.tiles().getFirst().section());context.takeScreenshot("hoi-general-tactics-low-skill-tooltip-gui2");click(context,leader.tiles().getFirst().section());
            context.runOnClient(client->{if(!rejected.isEmpty())throw new AssertionError("Below-level leader selection must retain its authoritative lock");});
            var lowTree=fixture("leader-tactic-low-skill-trait-tree");var entryActions=new ArrayList<ProtocolDialogPayloads.Action>();
            context.setScreen(()->new ScreenCommanderTraits(lowTree,null,entryActions::add));
            var entry=context.computeOnClient(client->{var button=((ScreenCommanderTraits)client.gui.screen()).choiceButton("preferred-tactic");
                if(button==null||button.active)throw new AssertionError("Low-skill entry must remain disabled");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
            context.getInput().setCursorPos(entry[0]*2,entry[1]*2);context.waitTicks(3);
            context.runOnClient(client->{var screen=(ScreenCommanderTraits)client.gui.screen();var bounds=screen.tooltipBounds();
                if(bounds==null)throw new AssertionError("Low-skill preferred-tactic entry must expose its source requirement tooltip");
                screen.mouseClicked(new MouseButtonEvent(entry[0],entry[1],new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);
                if(!entryActions.isEmpty())throw new AssertionError("Disabled entry hover or click cannot issue a command");});
            context.takeScreenshot("hoi-general-tactics-low-skill-entry-tooltip-gui2");
            context.setScreen(()->new ScreenDialog(fixtureUnchecked("leader-tactic-confirm")));context.waitTicks(3);context.takeScreenshot("hoi-general-preferred-tactic-confirm-gui2");
            context.setScreen(()->new ScreenPreferredTactics(fixtureUnchecked("leader-tactic-selected"),null));
            hover(context,leader.tiles().getFirst().section());context.takeScreenshot("hoi-general-preferred-tactic-selected-gui2");context.setScreen(()->null);
        }catch(Exception failure){throw new AssertionError("Native preferred tactic renderer verification failed",failure);}
    }
    private static ProtocolDialogView fixtureUnchecked(String name) {
        try{return fixture(name);}catch(Exception error){throw new RuntimeException(error);}
    }
}
