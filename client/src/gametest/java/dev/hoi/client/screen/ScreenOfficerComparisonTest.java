package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ScreenOfficerComparisonTest {
    public static void run(ClientGameTestContext game){
        int scale=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var gson=new com.google.gson.Gson();var root=Path.of(System.getenv("HOI_COMMANDER_PREVIEW"));
            var menu=gson.fromJson(Files.readString(root.resolve("kor-selected-menu.json")),ProtocolMenuView.class);
            var roles=new ProtocolDialogPayloads.Show(Files.readString(root.resolve("kor-jeon-advisor-roles.json")),true).view().issued(UUID.randomUUID().toString(),7);
            var locked=new ProtocolDialogPayloads.Show(Files.readString(root.resolve("kor-leader-tactics-low-skill.json")),true).view();
            var tactic=new ProtocolDialogPayloads.Show(Files.readString(root.resolve("kor-leader-tactics-choices.json")),true).view();
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));game.getInput().setCursorPos(size[0]-10,size[1]-10);
                game.setScreen(()->new ScreenMenu(menu,ProtocolMenuTab.OFFICER_CORPS,()->{}));game.getInput().setCursorPos(size[0]-10,80);game.waitTicks(3);game.takeScreenshot("hoi-officer-corps-source-"+size[0]+"-gui2");
                game.setScreen(()->new ScreenCommanderTraits(roles,null,request->{throw new AssertionError("Role comparison sent request");}));game.getInput().setCursorPos(size[0]-10,80);game.waitTicks(3);game.takeScreenshot("hoi-officer-role-source-"+size[0]+"-gui2");
                game.setScreen(()->new ScreenPreferredTactics(locked,null,request->{throw new AssertionError("Locked preference sent request");}));game.waitTicks(3);game.takeScreenshot("hoi-officer-tactic-locked-"+size[0]+"-gui2");
                var requests=new ArrayList<ProtocolDialogPayloads.Action>();var screen=game.computeOnClient(c->new ScreenPreferredTactics(tactic,null,requests::add));game.setScreen(()->screen);game.waitTicks(3);game.takeScreenshot("hoi-officer-tactic-source-"+size[0]+"-gui2");
                for(int tab=1;tab<=2;tab++){
                    int selected=tab;game.runOnClient(c->{var b=screen.choiceButton("filter/"+selected);screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(b.getX()+2,b.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);if(!requests.isEmpty())throw new AssertionError("Filter changed preference");});game.waitTicks(3);game.takeScreenshot("hoi-officer-tactic-tab-"+tab+"-"+size[0]+"-gui2");
                }
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
    }
}
