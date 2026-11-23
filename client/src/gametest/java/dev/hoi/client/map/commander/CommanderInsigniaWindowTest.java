package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.commander.CommanderOrderPayloads;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderInsigniaWindowTest {
    private CommanderInsigniaWindowTest() {}
    static View with(View view,String id,String field,int value) {
        var gson=new com.google.gson.Gson();var json=gson.toJsonTree(view).getAsJsonObject();
        for(var item:json.getAsJsonArray("cards")){var card=item.getAsJsonObject();if(!card.get("id").getAsString().equals(id))continue;var panel=card.getAsJsonObject("panel");panel.addProperty(field,value);if(field.equals("armyColor"))for(var member:panel.getAsJsonArray("members"))member.getAsJsonObject().getAsJsonObject("data").addProperty("armyColor",value);}
        return gson.fromJson(json,View.class);
    }
    private static double[] tab(ClientGameTestContext game,String id) {
        return game.computeOnClient(client->{
            var header=CommanderPanels.hits().stream().filter(h->h.icon().equals("army_color_button")).findFirst().orElseThrow(()->new AssertionError("Colour tab must be clickable for "+id));
            if(!header.tip().equals(UiCommandLabels.text("CHOOSE_INSIGNIA","")))throw new AssertionError("Colour tab keeps the original CHOOSE_INSIGNIA tooltip");
            if(CommanderPanels.hits().stream().filter(h->"닫기".equals(h.tip())).anyMatch(h->h.x()<header.x()+header.w()&&header.x()<h.x()+h.w()&&h.y()<header.y()+header.h()&&header.y()<h.y()+h.h()))throw new AssertionError("Colour and close click areas must not overlap");
            return new double[]{header.x()+header.w()/2.,header.y()+header.h()/2.};
        });
    }
    private static double[] at(ClientGameTestContext game,double px,double py){return game.computeOnClient(client->{var frame=CommanderInsigniaWindow.windowFrame();return new double[]{frame.x(px),frame.y(py)};});}
    private static void move(ClientGameTestContext game,double[] point){double scale=game.computeOnClient(client->client.getWindow().getGuiScale());game.getInput().setCursorPos(point[0]*scale,point[1]*scale);game.waitTicks(1);}
    private static void click(ClientGameTestContext game,double[] point,int button){move(game,point);game.getInput().pressMouse(button);game.waitTicks(2);}
    private static void answer(ClientGameTestContext game,View[] source,List<CommanderOrderPayloads.Command> commands,String id,String field,int value) {
        game.runOnClient(client->{var issued=CommanderOrders.currentContext();CommanderOrders.accept(new CommanderOrderPayloads.Context(issued.token(),issued.revision()+1,issued.country(),issued.dimension(),""));commands.clear();source[0]=with(source[0],id,field,value);CommanderHud.accept(source[0]);});
        game.waitTicks(2);
    }
    private static void expect(List<CommanderOrderPayloads.Command> commands,String action,List<String> selection,String message){if(commands.size()!=1||!commands.getFirst().action().equals(action)||!commands.getFirst().selection().equals(selection)||!commands.getFirst().rays().isEmpty())throw new AssertionError(message+": "+commands);}
    static void army(ClientGameTestContext game,View[] source,List<CommanderOrderPayloads.Command> commands,int[] size) {
        game.runOnClient(client->{source[0]=with(source[0],"A","armyColor",0xdb0b0b);CommanderHud.accept(source[0]);});game.waitTicks(2);
        var tab=tab(game,"A");
        click(game,tab,InputConstants.MOUSE_BUTTON_RIGHT);
        game.runOnClient(client->{if(!CommanderInsigniaWindow.target().isEmpty()||!commands.isEmpty())throw new AssertionError("Colour tab right click must not open the window or send a command");});
        click(game,tab,InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->{
            if(!CommanderInsigniaWindow.target().equals("A"))throw new AssertionError("Army colour tab must open the original insignia window");
            var frame=CommanderInsigniaWindow.windowFrame();var units=CommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());float scale=Math.min(units.scale()*.5f,Math.min((client.getWindow().getGuiScaledWidth()-8f)/CommanderInsigniaWindow.WIDTH,(client.getWindow().getGuiScaledHeight()-dev.hoi.client.ui.hud.HudMenuBar.height(client.getWindow().getGuiScaledWidth())-8f)/CommanderInsigniaWindow.HEIGHT));
            if(Math.abs(frame.scale()-scale)>1e-4)throw new AssertionError("Insignia window uses the commander source scale: "+frame);
        });
        game.takeScreenshot("hoi-army-insignia-window-"+size[0]+"x"+size[1]+"-gui2");
        var preset=at(game,223+25+12,315+12);
        click(game,preset,InputConstants.MOUSE_BUTTON_RIGHT);
        game.runOnClient(client->{if(!commands.isEmpty())throw new AssertionError("Preset right click must not send a command");});
        click(game,preset,InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->{
            expect(commands,"ARMY_COLOR",List.of("A","rgb/"+0x0bdbdb),"Preset must send the exact authenticated army colour");
            var card=source[0].cards().stream().filter(c->c.id().equals("A")).findFirst().orElseThrow();if(card.panel().armyColor()!=0xdb0b0b)throw new AssertionError("Colour must wait for the server");
            if(CommanderInsigniaWindow.color(card)!=0x0bdbdb)throw new AssertionError("Open window previews the chosen colour on its marker");
            CommanderOrders.send("ARMY_COLOR",List.of("A","rgb/"+0x730bdb),List.of());if(commands.size()!=1)throw new AssertionError("Repeat colour input must wait for the next revision");
        });
        answer(game,source,commands,"A","armyColor",0x0bdbdb);
        game.takeScreenshot("hoi-army-color-authoritative-"+size[0]+"x"+size[1]+"-gui2");
        click(game,at(game,27+5*42+16,88+16),InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->expect(commands,"ARMY_INSIGNIA",List.of("A","frame/5"),"Insignia entry must send the exact army insignia"));
        answer(game,source,commands,"A","insignia",5);
        game.runOnClient(client->{var card=CommanderHud.view().cards().stream().filter(c->c.id().equals("A")).findFirst().orElseThrow();if(!CommanderInsigniaWindow.shield(card).endsWith("theatre_army_shield/frame_5"))throw new AssertionError("Header shield must use the authoritative insignia");});
        var knob=at(game,24+192*.5,316+8);move(game,knob);game.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(1);move(game,at(game,260,316+8));
        game.runOnClient(client->{if(!commands.isEmpty())throw new AssertionError("Dragging a slider must wait for release");});
        game.takeScreenshot("hoi-army-insignia-hue-drag-"+size[0]+"x"+size[1]+"-gui2");
        game.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(2);
        game.runOnClient(client->{float[] hsb=java.awt.Color.RGBtoHSB(0x0b,0xdb,0xdb,null);int expected=java.awt.Color.HSBtoRGB(1f,hsb[1],hsb[2])&0xffffff;expect(commands,"ARMY_COLOR",List.of("A","rgb/"+expected),"Hue slider release must send one colour");});
        answer(game,source,commands,"A","armyColor",java.awt.Color.HSBtoRGB(1f,java.awt.Color.RGBtoHSB(0x0b,0xdb,0xdb,null)[1],java.awt.Color.RGBtoHSB(0x0b,0xdb,0xdb,null)[2])&0xffffff);
        move(game,at(game,150,200));game.getInput().scroll(-1);game.waitTicks(2);
        game.runOnClient(client->{if(CommanderInsigniaWindow.listOffset()!=CommanderInsigniaWindow.SLOT_H)throw new AssertionError("Wheel scrolls the 81 army insignia by one row");});
        game.takeScreenshot("hoi-army-insignia-scrolled-"+size[0]+"x"+size[1]+"-gui2");
        var before=at(game,0,0);var grab=at(game,150,420);move(game,grab);game.getInput().holdMouse(InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(1);move(game,new double[]{grab[0]+40,grab[1]+30});game.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_LEFT);game.waitTicks(2);
        var after=at(game,0,0);if(Math.abs(after[0]-before[0]-40)>1||Math.abs(after[1]-before[1]-30)>1)throw new AssertionError("Window background drag must move the window: "+Arrays.toString(before)+" -> "+Arrays.toString(after));
        game.runOnClient(client->{if(!commands.isEmpty())throw new AssertionError("Moving the window must not send a command");});
        click(game,at(game,282+15,4+15),InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->{if(!CommanderInsigniaWindow.target().isEmpty())throw new AssertionError("Close button must close the insignia window");});
        click(game,tab,InputConstants.MOUSE_BUTTON_LEFT);click(game,tab,InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->{if(!CommanderInsigniaWindow.target().isEmpty())throw new AssertionError("A second tab click must close the window");});
        click(game,tab,InputConstants.MOUSE_BUTTON_LEFT);game.getInput().pressKey(InputConstants.KEY_ESCAPE);game.waitTicks(2);
        game.runOnClient(client->{if(!CommanderInsigniaWindow.target().isEmpty())throw new AssertionError("Escape must close the insignia window");if(!commands.isEmpty())throw new AssertionError("Closing must not send a command");});
    }
    static void group(ClientGameTestContext game,View[] source,List<CommanderOrderPayloads.Command> commands,int[] size) {
        game.runOnClient(client->CommanderPanels.select("G1",false));game.waitTicks(2);
        click(game,tab(game,"G1"),InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->{if(!CommanderInsigniaWindow.target().equals("G1"))throw new AssertionError("Army group colour tab must open the insignia window");});
        game.takeScreenshot("hoi-army-group-insignia-window-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{if(CommanderInsigniaWindow.listOffset()!=0)throw new AssertionError("Six group insignia need no scroll");});
        click(game,at(game,223+25+12,315+12),InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->{expect(commands,"GROUP_COLOR",List.of("G1","rgb/"+0x0bdbdb),"Group preset must send the exact group colour");if(CommanderHud.groups.color("G1",source[0])!=0xff730bdb)throw new AssertionError("Group colour must wait for the server");});
        answer(game,source,commands,"G1","color",0x0bdbdb);
        game.runOnClient(client->{var card=CommanderHud.view().cards().stream().filter(c->c.id().equals("G1")).findFirst().orElseThrow();if(CommanderPanels.shieldTint(card)!=0xff0bdbdb)throw new AssertionError("Grayscale laurel takes the group colour");});
        click(game,at(game,27+3*42+16,88+16),InputConstants.MOUSE_BUTTON_LEFT);
        game.runOnClient(client->expect(commands,"GROUP_INSIGNIA",List.of("G1","frame/3"),"Group insignia entry must send the exact group insignia"));
        answer(game,source,commands,"G1","insignia",3);
        game.runOnClient(client->{var card=CommanderHud.view().cards().stream().filter(c->c.id().equals("G1")).findFirst().orElseThrow();if(!CommanderInsigniaWindow.shield(card).endsWith("theatre_army_group_shield/frame_3")||CommanderPanels.shieldTint(card)!=0xffffffff)throw new AssertionError("Group shield must use the authoritative insignia and keep coloured insignia art");if(CommanderHud.bands().stream().filter(b->b.owner().equals("G1")).anyMatch(b->b.color()!=0xff0bdbdb))throw new AssertionError("Group metadata must use received RGB exactly");});
        game.takeScreenshot("hoi-army-group-insignia-authoritative-"+size[0]+"x"+size[1]+"-gui2");
        game.getInput().pressKey(InputConstants.KEY_ESCAPE);game.waitTicks(2);
    }
}
