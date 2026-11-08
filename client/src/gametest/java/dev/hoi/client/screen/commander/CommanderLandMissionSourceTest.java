package dev.hoi.client.screen.commander;

import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.dialog.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CommanderLandMissionSourceTest {
    private CommanderLandMissionSourceTest() {}
    private static DialogView fixture(String name) {
        try(var stream=CommanderLandMissionSourceTest.class.getResourceAsStream("/land/"+name+".json")) {
            if(stream==null)throw new AssertionError("Missing server land fixture "+name);
            return new DialogPayloads.Show(new String(stream.readAllBytes(),StandardCharsets.UTF_8),true).view();
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static net.minecraft.client.input.MouseButtonEvent event(int button,double x,double y){return new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));}
    private static void mapGesture(ClientGameTestContext context) {
        var issued=fixture("land-targets");if(!issued.image().startsWith("mission/land-target/"))throw new AssertionError("Map test requires actual server-issued target screen");
        var sent=new ArrayList<CommanderHudPayloads.PlanMapPick>();
        context.runOnClient(client->{client.player.setXRot(60);client.player.setYRot(0);});context.waitTicks(3);
        context.setScreen(()->new CommanderRoster(issued,null,a->{throw new AssertionError("Map click is not a row action");},sent::add));context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(CommanderRoster)client.gui.screen();screen.mouseClicked(event(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,700,420),false);
            for(int i=1;i<=80;i++)screen.mouseDragged(event(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,700+i*4,420+i),4,1);
        });context.waitTicks(1);context.takeScreenshot("hoi-land-map-drag-preview-gui2");
        context.runOnClient(client->{var screen=(CommanderRoster)client.gui.screen();screen.mouseReleased(event(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,1020,500));screen.mouseReleased(event(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,1020,500));});
        if(sent.size()!=1||sent.getFirst().toggle()||sent.getFirst().rays().size()<2||sent.getFirst().rays().size()>64||!sent.getFirst().token().equals(issued.token())||sent.getFirst().revision()!=issued.revision())throw new AssertionError("Bounded map drag must preserve private issued identity: "+sent);
        sent.clear();context.setScreen(()->new CommanderRoster(issued,null,a->{},sent::add));context.waitTicks(2);
        context.runOnClient(client->{var screen=(CommanderRoster)client.gui.screen();screen.mouseClicked(event(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,700,500),false);screen.update(issued);screen.mouseClicked(event(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,700,500),true);});
        if(sent.size()!=1||!sent.getFirst().toggle()||sent.getFirst().rays().size()!=1)throw new AssertionError("Single map toggle must not replay on equal revision refresh");
    }
    public static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
        for(var sample:List.of(new String[]{"land-plans","type/FALLBACK"},new String[]{"land-targets","target/"},new String[]{"land-target-selected","continue"},new String[]{"land-review","create"},new String[]{"land-manage","activate"},new String[]{"land-running","deactivate"},new String[]{"land-stopped","plan/"},new String[]{"land-units","unit/"},new String[]{"land-unit-ready","exercise"},new String[]{"land-unit-training","stop-exercise"},new String[]{"land-unit-stopped","exercise"},new String[]{"land-plan-units","unit/"},new String[]{"land-plan-unassigned","unit/"},new String[]{"land-plan-reassigned","automatic"},new String[]{"land-plan-automatic","unit/"})) {
            var view=fixture(sample[0]);var choice=view.choices().stream().filter(c->c.id().startsWith(sample[1])).findFirst().orElseThrow();
            var sent=new ArrayList<DialogPayloads.Action>();context.setScreen(()->new CommanderRoster(view,null,sent::add));context.waitTicks(2);
            int[] at=context.computeOnClient(client->{
                var screen=(CommanderRoster)client.gui.screen();
                for(int i=0;i<=view.tiles().size();i++) {
                    var button=Optional.ofNullable(screen.choiceButton(choice.id())).filter(c->c.active);
                    if(button.isPresent()){var b=button.get();return new int[]{b.getX()+b.getWidth()/2,b.getY()+b.getHeight()/2};}
                    screen.mouseScrolled(40,180,0,-1);
                }
                throw new AssertionError("Issued land choice inaccessible: "+choice.id());
            });
            context.getInput().setCursorPos(at[0]*2,at[1]*2);context.waitTicks(2);context.takeScreenshot("hoi-"+sample[0]+"-gui2");
            context.runOnClient(client->{
                var event=new net.minecraft.client.input.MouseButtonEvent(at[0],at[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                var screen=(CommanderRoster)client.gui.screen();screen.mouseClicked(event,false);screen.update(view);screen.mouseClicked(event,true);
            });
            if(!sent.equals(List.of(new DialogPayloads.Action(view.token(),view.revision(),choice.id()))))throw new AssertionError("Land choice must preserve issued identity and pending single submission: "+sent);
        }
        mapGesture(context);
        context.setScreen(()->null);context.getInput().resizeWindow(1600,1000);
    }
}
