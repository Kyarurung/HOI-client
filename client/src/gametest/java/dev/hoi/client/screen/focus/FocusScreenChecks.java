package dev.hoi.client.screen.focus;

import dev.hoi.protocol.focus.*;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import java.util.*;

public final class FocusScreenChecks {
    public static void run(ClientGameTestContext context) {
        for(String tag:List.of("KOR","PRC","PRK","JAP")) {
            FocusPayloads.View view;
            try(var input=FocusScreenChecks.class.getResourceAsStream("/hoi/focus/"+tag+".json")) {
                if(input==null)throw new AssertionError("Missing source focus fixture "+tag);
                view=new Gson().fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),FocusPayloads.View.class);
            }catch(java.io.IOException e){throw new AssertionError(e);}
            var sent=new ArrayList<FocusPayloads.Request>();
            context.setScreen(() -> new FocusScreen(view,sent::add));context.waitTicks(3);
            context.takeScreenshot("hoi-focus-original-"+tag.toLowerCase(Locale.ROOT));context.waitTicks(3);
            if(tag.equals("KOR")) {
                context.getInput().resizeWindow(2560,1440);context.runOnClient(c->c.options.guiScale().set(2));context.waitTicks(3);
                context.runOnClient(c->{
                    var screen=(FocusScreen)c.gui.screen();
                    var plus=button(screen,"+");var minus=button(screen,"−");var close=button(screen,"×");
                    float s=dev.hoi.client.ui.hud.HudMenuBar.windowTop(screen.width)/78f,center=screen.width-114*s,upper=dev.hoi.client.ui.hud.HudMenuBar.windowTop(screen.width)+11*s;
                    check(minus.getX()==Math.round(center-21*s)&&minus.getRight()==plus.getX()&&plus.getRight()==Math.round(center+56*s),"Zoom buttons keep the original focus view columns");
                    check(minus.getY()==Math.round(upper)&&plus.getY()==minus.getY()&&plus.getHeight()==minus.getHeight(),"Zoom buttons keep the original focus view row");
                    check(close.getX()==Math.round(screen.width-48*s)&&close.getY()==Math.round(upper+2*s)&&plus.getRight()<=close.getX(),"The close button keeps the original corner and never overlaps zoom");
                    float zoom=screen.zoomLevel();
                    plus.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                    check(screen.zoomLevel()>zoom,"The plus button zooms in");
                    minus.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                    check(Math.abs(screen.zoomLevel()-zoom)<1e-4,"The minus button zooms back out");
                    screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP,0,0));
                    check(screen.zoomLevel()>zoom,"Page Up zooms in like the original shortcut");
                    screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN,0,0));
                    check(Math.abs(screen.zoomLevel()-zoom)<1e-4,"Page Down zooms out like the original shortcut");
                });
                context.takeScreenshot("hoi-focus-zoom-gui2");
                var plusCenter=context.computeOnClient(c->{var b=button((FocusScreen)c.gui.screen(),"+");double scale=c.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
                context.getInput().setCursorPos(plusCenter[0],plusCenter[1]);context.waitTicks(5);
                context.takeScreenshot("hoi-focus-zoom-tooltip-gui2");
                context.getInput().setCursorPos(10,10);
                context.getInput().resizeWindow(854,480);context.waitTicks(3);
                context.runOnClient(c->{
                    var screen=(FocusScreen)c.gui.screen();
                    check(button(screen,"+").getRight()<=button(screen,"×").getX()&&button(screen,"−").getRight()==button(screen,"+").getX(),"Small windows keep the zoom and close buttons apart");
                });
                context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
                String target=view.nodes().getFirst().id();
                context.setScreen(()->new FocusScreen(target));
                context.runOnClient(c->{
                    var screen=(FocusScreen)c.gui.screen();
                    screen.update(FocusPayloads.Response.of(screen.token(),view));
                    if(!screen.selectedId().equals(target) || screen.detailBounds()==null)
                        throw new AssertionError("Source completion must open the selected focus detail after its private response");
                    String action=view.nodes().getFirst().status().equals("진행 중")?"중점 취소":"중점 시작";
                    if(screen.children().stream().noneMatch(child->child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals(action)))
                        throw new AssertionError("Source completion detail must expose the action for its authoritative focus status");
                });
                context.waitTicks(2);context.takeScreenshot("hoi-focus-completion-detail");
                context.setScreen(()->new FocusScreen(view,sent::add));context.waitTicks(2);
                context.runOnClient(c -> {
                    var screen=(FocusScreen)c.gui.screen();
                    var search=(EditBox)screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
                    search.setValue("코로나");screen.setFocused(search);
                    screen.mouseScrolled(screen.width/2.0,screen.height/2.0,0,-2);float zoom=screen.zoomLevel();
                    screen.update(FocusPayloads.Response.of(screen.token(),new FocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),view.revision()+1,"",view.hud(),view.nodes(),false)));
                    if(screen.getFocused()!=search || !screen.children().contains(search))throw new AssertionError("Focus refresh replaced the active input");
                    if(!screen.searchText().equals("코로나") || screen.zoomLevel()!=zoom)throw new AssertionError("Focus refresh reset local navigation");
                });
                context.takeScreenshot("hoi-focus-search-preserved");context.waitTicks(3);
                context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-focus-compact");context.waitTicks(3);
                context.runOnClient(c -> {
                    var screen=(FocusScreen)c.gui.screen();screen.update(new FocusPayloads.Response(UUID.randomUUID().toString(),""));
                    if(c.gui.screen()!=screen)throw new AssertionError("Unrelated focus response changed screen");
                    screen.update(new FocusPayloads.Response(screen.token(),""));if(c.gui.screen()!=null)throw new AssertionError("Revoked focus UI remains open");
                });
                context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
            }
        }
        FocusScreenTest.run(context);
        FocusDetailTest.run(context);
        context.setScreen(() -> null);
    }
    private static net.minecraft.client.gui.components.Button button(FocusScreen screen,String name) {
        return (net.minecraft.client.gui.components.Button)screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(name)).findFirst().orElseThrow(()->new AssertionError("Missing focus control: "+name));
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
