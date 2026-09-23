package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import java.util.*;

public final class ScreenFocusChecks {
    public static void run(ClientGameTestContext context) {
        for(String tag:List.of("KOR","PRC","PRK","JAP")) {
            FocusProtocol.View view;
            try(var input=ScreenFocusChecks.class.getResourceAsStream("/hoi/focus/"+tag+".json")) {
                if(input==null)throw new AssertionError("Missing source focus fixture "+tag);
                view=new Gson().fromJson(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8),FocusProtocol.View.class);
            }catch(java.io.IOException e){throw new AssertionError(e);}
            var sent=new ArrayList<FocusProtocol.Request>();
            context.setScreen(() -> new ScreenFocus(view,sent::add));context.waitTicks(3);
            context.takeScreenshot("hoi-focus-original-"+tag.toLowerCase(Locale.ROOT));context.waitTicks(3);
            if(tag.equals("KOR")) {
                String target=view.nodes().getFirst().id();
                context.setScreen(()->new ScreenFocus(target));
                context.runOnClient(c->{
                    var screen=(ScreenFocus)c.gui.screen();
                    screen.update(FocusProtocol.Response.of(screen.token(),view));
                    if(screen.children().stream().noneMatch(child->child instanceof net.minecraft.client.gui.components.Button button&&button.getMessage().getString().equals("중점 시작")))
                        throw new AssertionError("Source completion must open the selected focus detail after its private response");
                });
                context.waitTicks(2);context.takeScreenshot("hoi-focus-completion-detail");
                context.setScreen(()->new ScreenFocus(view,sent::add));context.waitTicks(2);
                context.runOnClient(c -> {
                    var screen=(ScreenFocus)c.gui.screen();
                    var search=(EditBox)screen.children().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow();
                    search.setValue("코로나");screen.setFocused(search);
                    screen.mouseScrolled(screen.width/2.0,screen.height/2.0,0,-2);float zoom=screen.zoomLevel();
                    screen.update(FocusProtocol.Response.of(screen.token(),new FocusProtocol.View(view.country(),view.name(),view.tree(),view.session(),view.revision()+1,"",view.hud(),view.nodes(),false)));
                    if(screen.getFocused()!=search || !screen.children().contains(search))throw new AssertionError("Focus refresh replaced the active input");
                    if(!screen.searchText().equals("코로나") || screen.zoomLevel()!=zoom)throw new AssertionError("Focus refresh reset local navigation");
                });
                context.takeScreenshot("hoi-focus-search-preserved");context.waitTicks(3);
                context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-focus-compact");context.waitTicks(3);
                context.runOnClient(c -> {
                    var screen=(ScreenFocus)c.gui.screen();screen.update(new FocusProtocol.Response(UUID.randomUUID().toString(),""));
                    if(c.gui.screen()!=screen)throw new AssertionError("Unrelated focus response changed screen");
                    screen.update(new FocusProtocol.Response(screen.token(),""));if(c.gui.screen()!=null)throw new AssertionError("Revoked focus UI remains open");
                });
                context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
            }
        }
        context.setScreen(() -> null);
    }
}
