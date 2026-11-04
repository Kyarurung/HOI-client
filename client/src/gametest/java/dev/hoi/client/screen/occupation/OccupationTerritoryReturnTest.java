package dev.hoi.client.screen.occupation;

import dev.hoi.protocol.occupation.OccupationPayloads.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;

public final class OccupationTerritoryReturnTest {
    private static AbstractWidget button(OccupationScreen screen,String name){return screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals(name)).map(w->(AbstractWidget)w).findFirst().orElseThrow();}
    private static void click(OccupationScreen screen,String name,int mouse){var b=button(screen,name);screen.mouseClicked(new MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+2,new MouseButtonInfo(mouse,0)),false);}
    public static void run(ClientGameTestContext game){
        int scale=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var gson=new com.google.gson.Gson();var root=Path.of(System.getenv("HOI_COMMANDER_PREVIEW"));
            var original=gson.fromJson(Files.readString(root.resolve("occupation-return-view-before.json")),View.class);
            var after=gson.fromJson(Files.readString(root.resolve("occupation-return-view-after.json")),View.class);
            var target=original.rows().stream().filter(r->r.administration().returnTerritory()&&r.administration().enabled()).findFirst().orElseThrow();
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                var requests=new ArrayList<Request>();String token=original.token();var view=original;
                var screen=game.computeOnClient(c->new OccupationScreen(token,view,requests::add));game.setScreen(()->screen);
                game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-occupation-return-release-"+size[0]+"-gui2");
                for(String name:List.of("국가 해방","영토 반환")){
                    var b=game.computeOnClient(c->button(screen,name));game.getInput().setCursorPos((b.getX()+b.getWidth()/2.)*2,(b.getY()+b.getHeight()/2.)*2);game.waitTicks(20);game.takeScreenshot("hoi-occupation-"+(name.equals("국가 해방")?"release-tooltip-":"return-tooltip-")+size[0]+"-gui2");
                }
                game.getInput().setCursorPos(size[0]-5,80);
                game.runOnClient(c->{if(button(screen,"국가 해방").active||!button(screen,"영토 반환").active)throw new AssertionError("Administration authorization");click(screen,"영토 반환",InputConstants.MOUSE_BUTTON_RIGHT);if(screen.children().stream().anyMatch(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("OK")))throw new AssertionError("Right click opened return modal");click(screen,"영토 반환",InputConstants.MOUSE_BUTTON_LEFT);if(requests.stream().anyMatch(r->r.action()==Action.RETURN))throw new AssertionError("Return requires confirmation");});
                game.waitTicks(3);game.takeScreenshot("hoi-occupation-return-confirm-"+size[0]+"-gui2");
                game.runOnClient(c->{click(screen,"취소",InputConstants.MOUSE_BUTTON_LEFT);if(requests.stream().anyMatch(r->r.action()==Action.RETURN))throw new AssertionError("Cancel returned territory");var stale=new OccupationScreen(token,view,requests::add);stale.init(screen.width,screen.height);click(stale,"영토 반환",InputConstants.MOUSE_BUTTON_LEFT);stale.update(new View(token,view.revision()+1,view.country(),view.rows(),view.templates(),view.laws(),view.defaultTemplate(),view.defaultLaw(),view.message(),view.labels()));if(stale.children().stream().anyMatch(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("OK")))throw new AssertionError("New revision retained stale confirmation");click(screen,"영토 반환",InputConstants.MOUSE_BUTTON_LEFT);click(screen,"OK",InputConstants.MOUSE_BUTTON_RIGHT);if(requests.stream().anyMatch(r->r.action()==Action.RETURN))throw new AssertionError("Right click confirmed");click(screen,"OK",InputConstants.MOUSE_BUTTON_LEFT);screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN,0,0));var mutations=requests.stream().filter(r->r.action()==Action.RETURN).toList();if(!mutations.equals(List.of(new Request(Action.RETURN,token,view.revision(),target.state(),target.country()))))throw new AssertionError("Return target/revision or duplicate request "+mutations);});
                game.runOnClient(c->{screen.update(after);if(screen.children().stream().anyMatch(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("영토 반환")))throw new AssertionError("Returned country still has a return button");});
                game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-occupation-return-after-"+size[0]+"-gui2");
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
    }
}
