package dev.hoi.client.screen.commander;

import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class CommanderOfficerComparisonTest {
    public static void verifySpecialBanner(ClientGameTestContext game,MenuView menu)throws java.io.IOException{
        var doctrine=menu.page(MenuTab.OFFICER_CORPS).sections().stream().filter(s->s.icon().equals("special")).findFirst().orElseThrow().entries().getFirst();
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        var failures=new ArrayList<Integer>();
        try{
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                var screen=game.computeOnClient(c->new ScreenMenu(menu,MenuTab.OFFICER_CORPS,()->{}));game.setScreen(()->screen);
                game.runOnClient(c->screen.mouseScrolled(30,screen.height-20,0,-100));game.getInput().setCursorPos(size[0]-10,80);game.waitTicks(3);
                int bannerY=game.computeOnClient(c->screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget).map(w->(net.minecraft.client.gui.components.AbstractWidget)w).filter(w->w.getMessage().getString().equals(doctrine.name()+" · "+doctrine.value())).findFirst().orElseThrow().getY()-19);
                var pixels=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-officer-special-banner-"+size[0]+"-gui2").toFile());
                int gold=0;
                for(int y=(bannerY+2)*2;y<(bannerY+15)*2;y++)for(int x=40;x<90;x++){
                    int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;
                    if(r>100&&g>75&&r>b+25&&g>b+15)gold++;
                }
                if(gold<100)failures.add(size[0]);
            }
            if(!failures.isEmpty())throw new AssertionError("Special forces banner covered by header at widths "+failures);
        }finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(previous));}
    }
    public static void run(ClientGameTestContext game){
        int scale=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var gson=new com.google.gson.Gson();var root=Path.of(System.getenv("HOI_COMMANDER_PREVIEW"));
            var menu=gson.fromJson(Files.readString(root.resolve("kor-selected-menu.json")),MenuView.class);
            verifySpecialBanner(game,menu);
            var roles=new DialogPayloads.Show(Files.readString(root.resolve("kor-jeon-advisor-roles.json")),true).view().issued(UUID.randomUUID().toString(),7);
            var locked=new DialogPayloads.Show(Files.readString(root.resolve("kor-leader-tactics-low-skill.json")),true).view();
            var tactic=new DialogPayloads.Show(Files.readString(root.resolve("kor-leader-tactics-choices.json")),true).view();
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));game.getInput().setCursorPos(size[0]-10,size[1]-10);
                game.setScreen(()->new ScreenMenu(menu,MenuTab.OFFICER_CORPS,()->{}));game.getInput().setCursorPos(size[0]-10,80);game.waitTicks(3);game.takeScreenshot("hoi-officer-corps-source-"+size[0]+"-gui2");
                game.setScreen(()->new CommanderTraits(roles,null,request->{throw new AssertionError("Role comparison sent request");}));game.getInput().setCursorPos(size[0]-10,80);game.waitTicks(3);game.takeScreenshot("hoi-officer-role-source-"+size[0]+"-gui2");
                game.setScreen(()->new CommanderPreferredTactics(locked,null,request->{throw new AssertionError("Locked preference sent request");}));game.waitTicks(3);game.takeScreenshot("hoi-officer-tactic-locked-"+size[0]+"-gui2");
                var requests=new ArrayList<DialogPayloads.Action>();var screen=game.computeOnClient(c->new CommanderPreferredTactics(tactic,null,requests::add));game.setScreen(()->screen);game.waitTicks(3);game.takeScreenshot("hoi-officer-tactic-source-"+size[0]+"-gui2");
                for(int tab=1;tab<=2;tab++){
                    int selected=tab;game.runOnClient(c->{var b=screen.choiceButton("filter/"+selected);screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(b.getX()+2,b.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);if(!requests.isEmpty())throw new AssertionError("Filter changed preference");});game.waitTicks(3);game.takeScreenshot("hoi-officer-tactic-tab-"+tab+"-"+size[0]+"-gui2");
                }
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
    }
}
