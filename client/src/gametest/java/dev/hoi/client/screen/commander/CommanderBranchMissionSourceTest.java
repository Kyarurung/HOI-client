package dev.hoi.client.screen.commander;

import dev.hoi.protocol.dialog.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CommanderBranchMissionSourceTest {
    private CommanderBranchMissionSourceTest() {}
    private static DialogView fixture(String name) {
        try(var stream=CommanderBranchMissionSourceTest.class.getResourceAsStream("/missions/"+name+".json")) {
            if(stream==null)throw new AssertionError("Missing authoritative mission fixture "+name);
            return new DialogPayloads.Show(new String(stream.readAllBytes(),StandardCharsets.UTF_8),true).view();
        } catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static int[] position(ClientGameTestContext context,String label) {
        return context.computeOnClient(client->{
            var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                .filter(c->c.active&&c.getMessage().getString().equals(label)).findFirst().orElseThrow();
            return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};
        });
    }
    private static void click(ClientGameTestContext context,int[] at) {
        context.runOnClient(client->{
            var event=new net.minecraft.client.input.MouseButtonEvent(at[0],at[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
            client.gui.screen().mouseClicked(event,false);client.gui.screen().mouseClicked(event,true);
        });
    }
    private static void verifyAction(List<DialogPayloads.Action> sent,DialogView issued,String choice) {
        if(sent.size()!=1||!sent.getFirst().equals(new DialogPayloads.Action(issued.token(),issued.revision(),choice)))
            throw new AssertionError("Mission choice must send the issued token/revision once: "+sent);
    }
    public static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
        for(String branch:List.of("naval","air")) {
            var ready=fixture(branch+"-ready");String mission=branch.equals("naval")?"mission/EXERCISE":"mission/SUPERIORITY";
            var sent=new ArrayList<DialogPayloads.Action>();
            context.setScreen(()->new CommanderRoster(ready,null,sent::add));context.waitTicks(2);
            context.runOnClient(client->{
                var screen=(CommanderRoster)client.gui.screen();var observed=new HashSet<String>();
                for(int step=0;step<=ready.tiles().size();step++) {
                    screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                        .map(c->c.getMessage().getString()).forEach(observed::add);
                    screen.mouseScrolled(40,180,0,-1);
                }
                if(ready.tiles().stream().anyMatch(t->!observed.contains(t.name())))throw new AssertionError("Every issued mission must remain accessible by actual wheel scrolling");
                screen.mouseScrolled(40,180,0,100);
            });
            if(!sent.isEmpty())throw new AssertionError("Scrolling must not issue a mission");
            String label=ready.choices().stream().filter(c->c.id().equals(mission)).findFirst().orElseThrow().label();
            int[] at=position(context,label);context.getInput().setCursorPos(at[0]*2,at[1]*2);context.waitTicks(3);
            context.takeScreenshot("hoi-mission-"+branch+"-selection-tooltip-gui2");click(context,at);verifyAction(sent,ready,mission);
            context.runOnClient(client->((CommanderRoster)client.gui.screen()).update(ready));click(context,at);verifyAction(sent,ready,mission);
            context.runOnClient(client->((CommanderRoster)client.gui.screen()).update(ready.issued(ready.token(),ready.revision()+1)));
            position(context,label);
            var target=fixture(branch+"-targets");var targetSent=new ArrayList<DialogPayloads.Action>();
            context.setScreen(()->new CommanderRoster(target,null,targetSent::add));context.waitTicks(2);
            var targetChoice=target.choices().stream().filter(c->c.id().startsWith("target/")).findFirst().orElseThrow();
            at=position(context,targetChoice.label());context.getInput().setCursorPos(2400,1300);context.waitTicks(2);
            context.takeScreenshot("hoi-mission-"+branch+"-targets-gui2");click(context,at);verifyAction(targetSent,target,targetChoice.id());
            var selected=fixture(branch+"-selected");var cancelSent=new ArrayList<DialogPayloads.Action>();
            context.setScreen(()->new CommanderRoster(selected,null,cancelSent::add));context.waitTicks(2);
            context.takeScreenshot("hoi-mission-"+branch+"-selected-gui2");
            var hold=selected.choices().stream().filter(c->c.id().equals("mission/HOLD")).findFirst().orElseThrow();
            at=position(context,hold.label());click(context,at);verifyAction(cancelSent,selected,hold.id());
            var cancelled=fixture(branch+"-cancelled");context.setScreen(()->new CommanderRoster(cancelled,null,a->{}));context.waitTicks(2);
            context.takeScreenshot("hoi-mission-"+branch+"-cancelled-gui2");
        }
        for(String branch:List.of("naval","air")) {
            var issued=fixture(branch+"-training-ready");var sent=new ArrayList<DialogPayloads.Action>();
            context.setScreen(()->new CommanderRoster(issued,null,sent::add));context.waitTicks(2);
            String label=issued.choices().stream().filter(c->c.id().equals("mission/EXERCISE")).findFirst().orElseThrow().label();
            int[] at=position(context,label);
            context.runOnClient(client->{
                var event=new net.minecraft.client.input.MouseButtonEvent(at[0],at[1],new net.minecraft.client.input.MouseButtonInfo(
                        com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT));
                client.gui.screen().mouseClicked(event,false);client.gui.screen().mouseClicked(event,true);
            });
            verifyAction(sent,issued,"mission/EXERCISE_UNTIL_TRAINED");
            context.takeScreenshot("hoi-mission-"+branch+"-shift-training-gui2");
        }
        var training=fixture("air-training-active");context.setScreen(()->new CommanderRoster(training,null,a->{}));context.waitTicks(2);
        if(training.tiles().stream().noneMatch(t->t.detail().contains("완전 훈련 시 자동 종료")))throw new AssertionError("Missing authoritative training completion status");
        context.takeScreenshot("hoi-mission-air-training-status-gui2");
        for(String name:List.of("army-group-management","army-group-marshal-appointment","naval-fleet-management","air-multiple-regions-draft")) {
            var workflow=fixture(name).issued(UUID.randomUUID().toString(),2);var sent=new ArrayList<DialogPayloads.Action>();
            context.setScreen(()->new CommanderRoster(workflow,null,sent::add));context.waitTicks(2);
            if(name.equals("naval-fleet-management"))context.runOnClient(client->{
                if(client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).anyMatch(c->c.getMessage().getString().equals("함대 구성")))
                    throw new AssertionError("Private configuration fingerprint must not appear as a fleet count");
            });
            context.takeScreenshot("hoi-"+name+"-gui2");
            String choice=name.equals("army-group-management")?"marshal":name.equals("army-group-marshal-appointment")?"remove":name.equals("naval-fleet-management")?"members":"confirm";
            String label=workflow.choices().stream().filter(c->c.id().equals(choice)).findFirst().orElseThrow().label();int[] at=position(context,label);click(context,at);verifyAction(sent,workflow,choice);
            context.runOnClient(client->((CommanderRoster)client.gui.screen()).update(workflow));click(context,at);verifyAction(sent,workflow,choice);
        }
        context.setScreen(()->null);context.getInput().resizeWindow(1600,1000);
    }
}
