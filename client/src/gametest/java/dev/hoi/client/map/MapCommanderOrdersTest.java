package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.protocol.ProtocolCommanderOrders.*;
import java.nio.file.*;
import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class MapCommanderOrdersTest {
    private MapCommanderOrdersTest() {}
    private static void assertHintOutsideControls(){
        var hint=MapCommanderOrders.hintBounds();if(hint==null)throw new AssertionError("Direct map input must render its visible hint");
        var screen=net.minecraft.client.Minecraft.getInstance().gui.screen();int ceiling=MapCommanderHud.boxes().stream().mapToInt(MapCommanderHud.Box::y).min().orElseThrow()-new dev.hoi.client.ui.UiUnits(screen.width,screen.height).size(32);if(hint.y()+hint.height()>ceiling)throw new AssertionError("Direct hint must remain above card decorations and the whole bottom actionbar");
        for(var box:MapCommanderHud.boxes())if(hint.x()<box.x()+box.width()&&hint.x()+hint.width()>box.x()&&hint.y()<box.y()+box.height()&&hint.y()+hint.height()>box.y())throw new AssertionError("Direct hint overlaps commander card");
        for(var hit:MapCommanderPanels.hits())if(hint.x()<hit.x()+hit.w()&&hint.x()+hint.width()>hit.x()&&hint.y()<hit.y()+hit.h()&&hint.y()+hint.height()>hit.y())throw new AssertionError("Direct hint overlaps panel/actionbar/popup");
    }
    private static void clickLabel(String key){String prefix=UiCommandLabels.withoutShortcuts(UiCommandLabels.text(key,"missing/"+key));var hit=MapCommanderPanels.hits().stream().filter(h->h.tip().startsWith(prefix)).findFirst().orElseThrow(()->new AssertionError("Missing mouse control: "+key));net.minecraft.client.Minecraft.getInstance().gui.screen().mouseClicked(new MouseButtonEvent(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);}
    private static MouseButtonEvent right(double x,double y){return new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT,0));}
    private static void ack(Context issued,long revision,String pending){MapCommanderOrders.accept(new Context(issued.token(),revision,issued.country(),issued.dimension(),pending));}
    static void run(ClientGameTestContext game,ProtocolMenuView menu,String directory) throws java.io.IOException {
        try{
            for(String kind:List.of("army","navy","air")){
                var source=new Snapshot(Files.readString(Path.of(directory,"kor-command-panel-"+kind+".json"))).view();
                for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                    var commands=new ArrayList<Command>();var selections=new ArrayList<Selection>();var issued=new Context[1];
                    game.getInput().resizeWindow(size[0],size[1]);
                    game.runOnClient(client->{
                        client.options.guiScale().set(2);client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud()));MapCommanderOrders.reset();MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.requestObserver=null;
                        MapCommanderOrders.commandObserver=commands::add;MapCommanderOrders.selectionObserver=selections::add;
                        MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));
                        issued[0]=new Context(UUID.randomUUID().toString(),10,"KOR",client.level.dimension().identifier().toString(),"");MapCommanderOrders.accept(issued[0]);
                        if(kind.equals("air"))MapCommanderPanels.selectWings(source.wings().stream().limit(2).map(Member::id).toList());
                        else if(kind.equals("navy"))MapCommanderPanels.select(source.cards().getFirst().id(),false);
                        else {var army=source.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&!c.panel().members().isEmpty()).findFirst().orElseThrow();MapCommanderPanels.select(army.id(),false);MapUnitHud.selectDivisions(army.panel().members().stream().limit(2).map(Member::id).toList(),false);}
                        client.player.setXRot(60);client.player.setYRot(0);
                    });
                    game.setScreen(ScreenMapCursor::new);game.waitTicks(3);
                    game.runOnClient(client->{var exact=MapCommanderPanels.commandSelection();var cards=MapCommanderPanels.selectedCards();String pending=MapCommanderOrders.pending();boolean point=MapCommanderOrders.pointMode(),draws=MapCommanderOrders.drawsPlanLine();int before=commands.size(),published=selections.size();for(int key:new int[]{InputConstants.KEY_Z,InputConstants.KEY_X,InputConstants.KEY_C,InputConstants.KEY_V,InputConstants.KEY_B,InputConstants.KEY_H,InputConstants.KEY_K,InputConstants.KEY_D,InputConstants.KEY_S,InputConstants.KEY_G,InputConstants.KEY_A,InputConstants.KEY_O,InputConstants.KEY_DELETE,InputConstants.KEY_LCONTROL,InputConstants.KEY_LALT})for(int modifiers:new int[]{0,InputConstants.MOD_CONTROL,InputConstants.MOD_SHIFT,InputConstants.MOD_ALT}){var event=new net.minecraft.client.input.KeyEvent(key,0,modifiers);client.gui.screen().keyPressed(event);client.gui.screen().keyReleased(event);}if(commands.size()!=before||selections.size()!=published||!MapCommanderPanels.commandSelection().equals(exact)||!MapCommanderPanels.selectedCards().equals(cards)||!MapCommanderOrders.pending().equals(pending)||MapCommanderOrders.pointMode()!=point||MapCommanderOrders.drawsPlanLine()!=draws)throw new AssertionError("Removed branch keys must not send direct orders, publish changed selection or latch targeting modes");});
                    var refreshSelection=game.computeOnClient(client->MapCommanderPanels.commandSelection());
                    var refreshCards=game.computeOnClient(client->MapCommanderPanels.selectedCards());
                    for(int refresh=0;refresh<3;refresh++){
                        game.waitTicks(45);
                        game.runOnClient(client->{
                            if(!MapCommanderPanels.selectedCards().equals(refreshCards)||!MapCommanderPanels.commandSelection().equals(refreshSelection)||MapCommanderHud.boxes().isEmpty()||MapCommanderPanels.hits().isEmpty()||!MapCommanderOrders.ready())throw new AssertionError("Delayed daily updates must retain cards, abilities, exact selection and the valid command token");
                            MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));ack(issued[0],10,"");
                            if(!MapCommanderPanels.selectedCards().equals(refreshCards)||!MapCommanderPanels.commandSelection().equals(refreshSelection)||!MapCommanderOrders.ready())throw new AssertionError("Normal snapshot refresh must not revoke the current selection or session");
                        });
                    }
                    game.waitTicks(2);
                    game.runOnClient(client->{
                        if(kind.equals("army")&&MapUnitHud.selectedKeys().isEmpty())throw new AssertionError("Actual row-selected divisions must survive client ticks before map input even without a counter packet");
                        if(selections.size()<2||!selections.getLast().keys().equals(MapCommanderPanels.commandSelection()))throw new AssertionError("Every selection change must publish its exact canonical scope");
                        long previous=-1;for(var selection:selections){if(!selection.token().equals(issued[0].token())||selection.sequence()<=previous)throw new AssertionError("Selection sequence must increase monotonically within issued token");previous=selection.sequence();}
                        String key=kind.equals("army")?"UNITS_VIEW_BTN_START_TRAINING":kind.equals("navy")?"NAVAL_MISSION_NAME_PATROL":"AIRWING_MISSION_TYPE_AIR_SUPERIORITY";
                        var hit=MapCommanderPanels.hits().stream().filter(h->h.tip().startsWith(kind.equals("army")?UiCommandLabels.withoutShortcuts(source.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().controls().stream()).filter(c->c.id().equals("EXERCISE")).findFirst().orElseThrow().tooltip()):UiCommandLabels.withoutShortcuts(UiCommandLabels.text(key,"missing")))).findFirst().orElseThrow();
                        var screen=client.gui.screen();MapCommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.);MapCommanderHud.clickAt(hit.x()+hit.w()/2.,hit.y()+hit.h()/2.);
                        String action=kind.equals("army")?"ARMY/EXERCISE":kind.equals("navy")?"NAVY/PATROL":"AIR/SUPERIORITY";
                        if(commands.size()!=1||!commands.getFirst().action().equals(action)||commands.getFirst().revision()!=10||!commands.getFirst().token().equals(issued[0].token())||client.gui.screen()!=screen)throw new AssertionError("Actual toolbar must send one private direct command without opening a mission modal: "+commands);
                        var expected=kind.equals("army")?MapCommanderPanels.selectedCards().stream().sorted().toList():kind.equals("navy")?source.cards().getFirst().panel().members().stream().map(Member::id).toList():source.wings().stream().limit(2).map(Member::id).toList();
                        if(!new HashSet<>(commands.getFirst().selection()).equals(new HashSet<>(expected)))throw new AssertionError("Direct toolbar cannot broaden the selected units");
                        ack(issued[0],10,"");if(MapCommanderOrders.ready())throw new AssertionError("Equal revision cannot acknowledge a pending command");
                        ack(issued[0],11,kind.equals("army")?"":action);if(!MapCommanderOrders.ready())throw new AssertionError("New authoritative revision must release pending input");
                    });
                    if(!kind.equals("army"))game.runOnClient(client->{if(MapCommanderHud.boxes().stream().anyMatch(box->box.action()!=null&&box.tip().equals("선택한 부대 임무 패널")))throw new AssertionError("Direct mission controls must not be duplicated by an intermediate mission button");});
                    game.getInput().setCursorPos(size[0]-30,90);game.runOnClient(client->{if(kind.equals("army"))MapCommanderOrders.message("현재 지도의 명령 정보를 기다리고 있습니다.");});game.waitTicks(1);game.runOnClient(client->{if(kind.equals("army"))assertHintOutsideControls();else if(MapCommanderOrders.hintBounds()!=null)throw new AssertionError("Target mode must not create a synthetic footer instruction");});game.takeScreenshot("hoi-direct-command-"+kind+"-"+size[0]+"x"+size[1]+"-gui2");
                    game.runOnClient(client->{
                        var screen=client.gui.screen();double x=screen.width*.78,y=screen.height*.48;int before=commands.size();var exact=MapCommanderPanels.commandSelection();
                        screen.mouseClicked(right(x,y),false);screen.mouseDragged(right(x+12,y+8),12,8);screen.mouseReleased(right(x+12,y+8));screen.mouseReleased(right(x+12,y+8));
                        if(commands.size()!=before+1||!commands.getLast().action().equals("MAP")||commands.getLast().rays().size()!=1||!commands.getLast().selection().equals(exact))throw new AssertionError("Ordinary map right-release must send one ray with exact current unit selection");
                        ack(issued[0],12,"");
                    });
                    if(kind.equals("army")){
                        game.runOnClient(client->{
                            var first=source.cards().stream().filter(c->MapCommanderPanels.selected(c.id())&&c.role()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
                            var exact=MapUnitHud.selectedKeys();MapCommanderHud.accept(new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings()));if(!MapUnitHud.selectedKeys().equals(exact))throw new AssertionError("Authoritative refresh must retain the manually selected member subset");
                            var front=MapCommanderPanels.hits().stream().filter(h->first.panel().controls().stream().anyMatch(c->c.id().equals("FRONT")&&UiCommandLabels.withoutShortcuts(c.tooltip()).equals(h.tip()))).findFirst().orElse(null);
                            if(front!=null)throw new AssertionError("Selection invalidates stale panel hitboxes until the next frame");
                        });game.waitTicks(2);
                        game.runOnClient(client->{
                            var first=source.cards().stream().filter(c->MapCommanderPanels.selected(c.id())&&c.role()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
                            var front=MapCommanderPanels.hits().stream().filter(h->first.panel().controls().stream().anyMatch(c->c.id().equals("FRONT")&&UiCommandLabels.withoutShortcuts(c.tooltip()).equals(h.tip()))).findFirst().orElseThrow();MapCommanderHud.clickAt(front.x()+front.w()/2.,front.y()+front.h()/2.);
                            if(!commands.getLast().action().equals("LAND/FRONT")||!commands.getLast().selection().equals(List.of(first.id()))||!(client.gui.screen() instanceof ScreenMapCursor))throw new AssertionError("Front button must enter direct map drawing for the actual Army without a plan chooser");
                            ack(issued[0],13,"LAND/FRONT");var screen=client.gui.screen();double x=screen.width*.60,y=screen.height*.42;screen.mouseClicked(right(x,y),false);
                            for(int n=1;n<=80;n++)screen.mouseDragged(right(x+n*screen.width*.002,y+n*screen.height*.001),screen.width*.002,screen.height*.001);
                        });game.waitTicks(65);game.runOnClient(client->{if(MapCommanderOrders.hintBounds()!=null)throw new AssertionError("Drawing mode must not create a synthetic footer instruction");});game.takeScreenshot("hoi-direct-land-drag-"+size[0]+"x"+size[1]+"-gui2");
                        game.runOnClient(client->{
                            var screen=client.gui.screen();int before=commands.size();screen.mouseReleased(right(screen.width*.76,screen.height*.5));
                            if(commands.size()!=before+1||!commands.getLast().action().equals("MAP")||commands.getLast().rays().size()<2||commands.getLast().rays().size()>64)throw new AssertionError("Actual land drag must send its bounded sampled ray path once");
                            ack(issued[0],14,"LAND/OFFENSIVE/OBJECTIVE");var exact=MapCommanderPanels.commandSelection();
                            if(!screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_ESCAPE,0,0))||!commands.getLast().action().equals("CANCEL")||!MapCommanderPanels.commandSelection().equals(exact))throw new AssertionError("Escape cancels the issued target mode without silently dropping unit selection");
                            ack(issued[0],15,"");var army=source.cards().stream().filter(c->MapCommanderPanels.selected(c.id())&&c.role()==Mode.ASSIGN_ARMY).findFirst().orElseThrow();
                            int beforeAbility=commands.size();if(MapCommanderOrders.route(new Open(Mode.ARMY_ABILITIES,"force_attack",List.of(army.id())))||MapCommanderOrders.route(new Open(Mode.ARMY_ABILITIES,"ALL",List.of(army.id())))||commands.size()!=beforeAbility)throw new AssertionError("Read-only ability query must reach the authorized information dialog without sending an activation command");
                            ack(issued[0],16,"");ack(issued[0],17,"LAND/OFFENSIVE/OBJECTIVE");int beforeCancel=commands.size();MapUnitHud.clearSelection();MapCommanderOrders.selectionChanged();
                            if(commands.size()!=beforeCancel+1||!commands.getLast().action().equals("CANCEL"))throw new AssertionError("Changing selected units in target mode must explicitly cancel that mode once");ack(issued[0],18,"");MapCommanderPanels.select(army.id(),false);
                            if(!new HashSet<>(MapCommanderPanels.commandSelection()).equals(army.panel().divisionIds().stream().map(id->"unit/"+id).collect(java.util.stream.Collectors.toSet())))throw new AssertionError("Army card selection must select every actual member and no other division");
                        });
                    }
                    if(kind.equals("army")){
                        var group=source.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP).findFirst();
                        if(group.isPresent()){
                            game.runOnClient(client->MapCommanderPanels.select(group.get().id(),false));game.waitTicks(2);
                            game.runOnClient(client->{var children=source.cards().stream().filter(c->c.panel()!=null&&c.role()==Mode.ASSIGN_ARMY&&c.panel().parent().equals(group.get().id())).map(Card::id).toList();if(children.isEmpty())throw new AssertionError("Actual group fixture must supply child Armies");clickLabel("UNITS_VIEW_BTN_DRAW_FALLBACK");if(!commands.getLast().action().equals("LAND/FALLBACK")||!commands.getLast().selection().equals(children))throw new AssertionError("Group drawing must directly target actual child Army IDs without opening a chooser");ack(issued[0],19,"");});
                        }
                    }
                    if(kind.equals("army"))game.runOnClient(client->{
                        var screen=client.gui.screen();clickLabel("UNITS_VIEW_BTN_DELETE_ORDER");
                        if(!commands.getLast().action().equals("LAND/DELETE_PLAN")||!(client.gui.screen() instanceof ScreenMapCursor))throw new AssertionError("Delete button must enter single-plan map deletion, not a list modal or bulk removal");
                        ack(issued[0],20,"LAND/DELETE_PLAN");int before=commands.size();double x=screen.width*.8,y=screen.height*.45;screen.mouseClicked(right(x,y),false);screen.mouseDragged(right(x+12,y+8),12,8);screen.mouseReleased(right(x+12,y+8));
                        if(commands.size()!=before+1||!commands.getLast().action().equals("MAP")||commands.getLast().rays().size()!=1)throw new AssertionError("Single-plan deletion must send exactly one map point even after pointer movement");ack(issued[0],21,"");
                        var exact=MapCommanderPanels.commandSelection();MapCommanderOrders.assignSelected();if(!commands.getLast().action().equals("LAND/ASSIGN_UNITS")||!commands.getLast().selection().equals(exact))throw new AssertionError("Assignment must capture only selected division IDs");ack(issued[0],22,"LAND/ASSIGN_UNITS");
                        var left=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));before=commands.size();screen.mouseClicked(left,false);screen.mouseReleased(left);if(commands.size()!=before+1||!commands.getLast().action().equals("MAP")||!commands.getLast().selection().equals(exact)||!MapCommanderPanels.commandSelection().equals(exact))throw new AssertionError("Left click in assignment mode must assign the exact subset without changing map selection");ack(issued[0],23,"");
                        if(MapCommanderOrders.route(new Open(Mode.ARMY_TOOLBAR,"REMOVE",List.copyOf(MapCommanderPanels.selectedCards()))))throw new AssertionError("Bulk deletion must reach authoritative confirmation instead of direct command routing");
                        MapCommanderOrders.land("EDIT_PLAN",List.copyOf(MapCommanderPanels.selectedCards()));ack(issued[0],24,"LAND/EDIT_PLAN");if(!MapCommanderOrders.pointMode()||MapCommanderOrders.drawsPlanLine())throw new AssertionError("Edit must first select an existing plan");screen.mouseClicked(left,false);screen.mouseReleased(left);ack(issued[0],25,"LAND/EDIT_PLAN/FRONT");if(!MapCommanderOrders.drawsPlanLine())throw new AssertionError("Selected edit target must accept a replacement line preview");before=commands.size();screen.mouseClicked(right(x,y),false);screen.mouseDragged(right(x+20,y+20),20,20);screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_ESCAPE,0,0));screen.mouseReleased(right(x+20,y+20));if(commands.size()!=before+1||!commands.getLast().action().equals("CANCEL"))throw new AssertionError("Esc cancels edit preview without sending replacement geometry");ack(issued[0],26,"");
                        MapCommanderOrders.land("EDIT_PLAN",MapCommanderPanels.armyIds());ack(issued[0],43,"LAND/EDIT_PLAN");screen.mouseClicked(left,false);screen.mouseReleased(left);ack(issued[0],44,"LAND/EDIT_PLAN/FRONT");before=commands.size();screen.mouseClicked(left,false);var destination=new net.minecraft.client.input.MouseButtonEvent(x+20,y+20,new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseDragged(destination,20,20);screen.mouseReleased(destination);if(commands.size()!=before+1||!commands.getLast().action().equals("MAP/POINT")||commands.getLast().rays().size()!=2)throw new AssertionError("Left control point drag sends exactly the original and replacement rays");ack(issued[0],45,"");
                        MapCommanderOrders.land("FRONT",MapCommanderPanels.armyIds());ack(issued[0],46,"LAND/FRONT");before=commands.size();screen.mouseClicked(left,false);screen.mouseReleased(left);if(commands.size()!=before+1||!commands.getLast().action().equals("MAP")||commands.getLast().rays().size()!=1)throw new AssertionError("A single left click in FRONT mode sends exactly one border ray without changing selection");ack(issued[0],47,"");
                    });
                    game.runOnClient(client->{
                        String action=kind.equals("army")?"LAND/FRONT":kind.equals("navy")?"NAVY/PATROL":"AIR/SUPERIORITY";
                        if(!MapCommanderOrders.send(action,commands.getFirst().selection(),List.of()))throw new AssertionError("Ready direct target request must be accepted");long waiting=commands.getLast().revision();int queued=commands.size();
                        if(!MapCommanderOrders.cancelTarget()||commands.size()!=queued)throw new AssertionError("Cancellation during command wait must queue without replaying data");
                        ack(issued[0],waiting+1,action);if(commands.size()!=queued+1||!commands.getLast().action().equals("CANCEL"))throw new AssertionError("The next authoritative target context must deliver exactly one queued cancellation");ack(issued[0],waiting+1,action);if(commands.size()!=queued+1)throw new AssertionError("Equal context cannot repeat queued cancellation");ack(issued[0],waiting+2,"");
                        int before=commands.size();var foreign=new Context(UUID.randomUUID().toString(),99,"PRK",issued[0].dimension(),"NAVY/PATROL");MapCommanderOrders.accept(foreign);
                        if(MapCommanderOrders.gestureContext().startsWith(foreign.token()))throw new AssertionError("Foreign context cannot replace current country input");
                        var replacement=new Context(UUID.randomUUID().toString(),0,"KOR",issued[0].dimension(),"");MapCommanderOrders.accept(replacement);MapCommanderOrders.accept(new Context(issued[0].token(),1000,"KOR",issued[0].dimension(),"LAND/FRONT"));if(!MapCommanderOrders.gestureContext().startsWith(replacement.token()))throw new AssertionError("Retired context token cannot replace the new session even with a higher revision");
                        var screen=client.gui.screen();screen.mouseClicked(right(screen.width*.8,screen.height*.45),false);MapCommanderOrders.reset();screen.mouseReleased(right(screen.width*.8,screen.height*.45));if(MapCommanderOrders.ready()||!MapCommanderOrders.pending().isEmpty()||MapCommanderOrders.send("CANCEL",List.of(),List.of())||commands.size()!=before)throw new AssertionError("Revoked context cannot send commands or reopen a pending mode");MapCommanderOrders.accept(replacement);if(MapCommanderOrders.ready())throw new AssertionError("Reset must retire its active token so a delayed same-scope packet cannot reopen input");
                    });
                }
            }
            MapAreaDefenseTest.run(game,menu,directory);
        }finally{game.runOnClient(client->{MapCommanderOrders.commandObserver=null;MapCommanderOrders.selectionObserver=null;MapCommanderOrders.reset();MapCommanderHud.requestObserver=null;MapCommanderHud.reset();MapUnitHud.reset();});}
    }
}
