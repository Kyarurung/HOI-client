package dev.hoi.client.map.commander;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.map.MapCursor;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import dev.hoi.protocol.hud.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.*;

public final class CommanderCursorTooltipTest {
    private CommanderCursorTooltipTest() {}
    public static void run(ClientGameTestContext game,MenuView menu) {
        String directory=Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW"));
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720}))for(boolean navy:List.of(false,true)) {
                var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-"+(navy?"navy":"army-assigned")+".json"))).view();
                var card=source.cards().stream().filter(c->c.panel()!=null&&c.role()==(navy?Mode.NAVY_FLEET:Mode.ASSIGN_ARMY)).findFirst().orElseThrow();
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running(),source.theaters(),source.armyTheaters()));});
                game.setScreen(MapCursor::new);game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);
                var box=game.computeOnClient(client->CommanderHud.boxes().stream().filter(b->b.request()!=null&&b.request().target().equals(card.id())&&b.request().mode()==(navy?Mode.NAVY_FLEET:Mode.ASSIGN_ARMY)).findFirst().orElseThrow());
                String suffix=(navy?"navy":"army")+"-"+size[0]+"x"+size[1]+"-gui2";
                var idle=game.takeScreenshot("hoi-card-idle-"+suffix);
                Files.writeString(idle.resolveSibling(idle.getFileName()+".json"),new com.google.gson.Gson().toJson(Map.of("x",box.x()*2,"y",box.y()*2,"width",box.width()*2,"height",box.height()*2)));
                CommanderHud.TooltipBounds previous=null;
                for(int step=0;step<2;step++) {
                    int mx=box.x()+box.width()/2+step*6,my=box.y()+box.height()/2;
                    game.getInput().setCursorPos(mx*2,my*2);game.waitTicks(3);
                    var bounds=game.computeOnClient(client->{if(CommanderPanels.selected(card.id()))throw new AssertionError("Hover must not select a card");if(!CommanderHud.renderedTooltip().equals(box.tip()))throw new AssertionError("Card hover must preserve its tooltip");var b=CommanderHud.tooltipBounds();if(b==null||b.x()<0||b.y()<0||b.x()+b.width()>client.getWindow().getGuiScaledWidth()||b.y()+b.height()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Cursor tooltip must fit viewport");return b;});
                    if(previous!=null&&bounds.x()-previous.x()!=6)throw new AssertionError("Card tooltip must follow motion within the same card");previous=bounds;
                    var shot=game.takeScreenshot("hoi-card-hover-"+step+"-"+suffix);
                    Files.writeString(shot.resolveSibling(shot.getFileName()+".json"),new com.google.gson.Gson().toJson(Map.of("cursor",List.of(mx*2,my*2),"tooltip",bounds)));
                }
                game.runOnClient(client->{var click=new MouseButtonEvent(box.x()+box.width()/2.,box.y()+box.height()/2.,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));client.gui.screen().mouseClicked(click,false);client.gui.screen().mouseReleased(click);if(!CommanderPanels.selected(card.id()))throw new AssertionError("Click must select the hovered card");});
                game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-card-selected-"+suffix);
                memberTooltip(game,card.panel().members().getFirst(),suffix,!navy);
                game.runOnClient(client->{CommanderHud.closeSelection();if(CommanderPanels.selected(card.id()))throw new AssertionError("Closing selection must clear the card highlight");});
            }
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-air.json"))).view();var wing=source.wings().getFirst();
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->{client.options.guiScale().set(2);ClientCampaignHud.accept(HudPayloads.State.of("KOR",menu.hud()));CommanderHud.reset();CommanderUnitHud.reset();CommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running(),source.theaters(),source.armyTheaters()));CommanderPanels.showWings(List.of(wing.id()));});
                game.setScreen(MapCursor::new);game.waitTicks(3);memberTooltip(game,wing,"air-"+size[0]+"x"+size[1]+"-gui2",false);
            }
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {game.runOnClient(client->{CommanderHud.reset();CommanderUnitHud.reset();});game.setScreen(()->null);}
    }
    private static void memberTooltip(ClientGameTestContext game,Member member,String suffix,boolean army) throws java.io.IOException {
        var point=game.computeOnClient(client->{
            var hit=CommanderPanels.hits().stream().filter(h->(army||h.icon().isEmpty()&&h.sound().equals(dev.hoi.client.audio.AudioUiSounds.selection(member.request().mode())))&&h.tip().contains(member.name())&&h.tip().contains(member.detail())).findFirst().orElseThrow();
            if(army){String prefix=String.join("\n",dev.hoi.client.ui.UiCommandLabels.text("ARMY_LIST_CLICK_SELECT_DIVISION",""),dev.hoi.client.ui.UiCommandLabels.text("ARMY_LIST_RIGHT_CLICK_PAN",""),dev.hoi.client.ui.UiCommandLabels.text("ARMY_LIST_DOUBLE_CLICK_TEMPLATE",""),dev.hoi.client.ui.UiCommandLabels.text("ARMY_LIST_CTRL_CLICK_TOGGLE",""))+"\n\n§Y"+member.name()+"§!\n";if(!hit.tip().equals(prefix+member.detail()))throw new AssertionError("Division tooltip must preserve native click lines and server detail without status");}
            else if(!hit.tip().equals("§e"+member.name()+"§r\n"+member.detail()))throw new AssertionError("Fleet and wing row tooltips must contain one yellow name and the server body without repeated status");
            for(int y=hit.y()+1;y<hit.y()+hit.h();y+=2)for(int x=hit.x()+1;x<hit.x()+hit.w();x+=2)if(CommanderPanels.tooltipAt(x,y).equals(hit.tip()))return new int[]{x,y};
            throw new AssertionError("Member tooltip has no reachable hover area");
        });
        game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);
        game.runOnClient(client->{String text=CommanderHud.renderedTooltip();if(!text.contains(member.detail()))throw new AssertionError("Actual row tooltip must preserve complete server detail");for(var line:dev.hoi.client.ui.UiTooltips.sourceLines(client.font,text,client.getWindow().getGuiScaledWidth())){var plain=new StringBuilder();line.accept((i,s,c)->{plain.appendCodePoint(c);return true;});if(plain.toString().contains("---")||plain.toString().contains("£organization_texticon"))throw new AssertionError("Separators and organization icons must not leak as raw tokens");}});
        var shot=game.takeScreenshot("hoi-member-tooltip-"+suffix);var bounds=game.computeOnClient(client->CommanderHud.tooltipBounds());Files.writeString(shot.resolveSibling(shot.getFileName()+".json"),new com.google.gson.Gson().toJson(bounds));
    }
}
