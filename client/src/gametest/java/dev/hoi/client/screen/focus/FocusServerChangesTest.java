package dev.hoi.client.screen.focus;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenServerChangesTest;
import dev.hoi.protocol.focus.FocusPayloads;
import dev.hoi.protocol.hud.HudPayloads;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class FocusServerChangesTest {
    private FocusServerChangesTest() {}
    public static void run(ClientGameTestContext game,int[] size){
        for(String id:List.of("KOR_gwr_plan","PRC_theory_of_neo_socialism","PRC_salvage_the_situation","PRC_made_in_china_2025","PRC_operation_wu_qi","JAP_covid_populism")){
            if(id.equals("PRC_operation_wu_qi")){var before=ScreenServerChangesTest.source("focus-PRC_operation_wu_qi-before",FocusPayloads.View.class);if(before.nodes().stream().anyMatch(n->n.id().equals(id)))throw new AssertionError("Unfulfilled original branch must stay hidden");game.setScreen(()->new FocusScreen(before,r->{}));game.getInput().setCursorPos(size[0]-5,size[1]-5);game.waitTicks(3);ScreenServerChangesTest.capture(game,"focus-"+id+"-before-condition",size);}
            var view=ScreenServerChangesTest.source("focus-"+id,FocusPayloads.View.class);var node=view.nodes().stream().filter(n->n.id().equals(id)).findFirst().orElseThrow();var detail=view.detail();
            ScreenServerChangesTest.clean(node.name());ScreenServerChangesTest.clean(detail.name());ScreenServerChangesTest.clean(detail.description());if(!node.name().equals(detail.name()))throw new AssertionError("Dynamic focus tile and detail title differ: "+id);
            game.runOnClient(c->ClientCampaignHud.accept(HudPayloads.State.of(view.country(),view.hud())));game.setScreen(()->new FocusScreen(view,r->{}));game.runOnClient(c->{var screen=(FocusScreen)c.gui.screen();screen.update(FocusPayloads.Response.of(screen.token(),view));var bounds=screen.nodeBounds(id);var drag=new MouseButtonEvent(5,180,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_MIDDLE,0));screen.mouseClicked(drag,false);screen.mouseDragged(drag,screen.width/2.-bounds[0]-bounds[2]/2.,130-bounds[1]);screen.mouseReleased(drag);});
            var point=game.computeOnClient(c->{var screen=(FocusScreen)c.gui.screen();ScreenServerChangesTest.clean(screen.hoverTooltip(node));if(!screen.hoverTooltip(node).contains(node.name()))throw new AssertionError("Dynamic focus hover title differs: "+id);var b=screen.nodeBounds(id);return new int[]{b[0]+b[2]/2,b[1]+b[3]/2};});game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);ScreenServerChangesTest.capture(game,"focus-"+id+"-tile-hover",size);
            game.runOnClient(c->{var screen=(FocusScreen)c.gui.screen();screen.mouseClicked(new MouseButtonEvent(point[0],point[1],new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);screen.update(FocusPayloads.Response.of(screen.token(),view));if(!screen.selectedId().equals(id)||!screen.detailId().equals(id)||!screen.sourceDetailText().startsWith(detail.description()+"\n"))throw new AssertionError("Actual source focus selection and detail text must be preserved: "+id);});game.getInput().setCursorPos(size[0]-5,size[1]-5);game.waitTicks(3);ScreenServerChangesTest.capture(game,"focus-"+id+"-detail",size);
        }
    }
}
