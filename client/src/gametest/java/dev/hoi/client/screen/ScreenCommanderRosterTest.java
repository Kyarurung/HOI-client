package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ScreenCommanderRosterTest {
    public static void run(ClientGameTestContext game) {
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(c->c.options.guiScale().set(2));
                source(game,size);
                for(boolean group:List.of(false,true)) {
                    var tiles=List.of(tile("general-low","장군",1),tile("marshal-high","원수",5),tile("general-high","장군",4),tile("marshal-low","원수",2));
                    var choices=tiles.stream().filter(t->!t.section().equals("marshal-low")).map(t->new ProtocolDialogView.Choice(t.section(),t.name())).toList();
                    var view=new ProtocolDialogView(UUID.randomUUID().toString(),7,ProtocolDialogView.Kind.COMMANDER,group?"집단군 임명 순서 합성 검증":"군 임명 순서 합성 검증","","","",group?"commander/group":"commander/view","",tiles,choices);
                    var sent=new ArrayList<ProtocolDialogPayloads.Action>();
                    var screen=game.computeOnClient(c->new ScreenCommanderRoster(view,null,sent::add));
                    game.setScreen(()->screen);game.waitTicks(2);
                    game.runOnClient(c->{
                        var order=screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiMenuButton).map(w->(dev.hoi.client.ui.UiMenuButton)w).map(w->w.getMessage().getString()).filter(s->s.contains("-")).toList();
                        var expected=group?List.of("marshal-high","marshal-low","general-high","general-low"):List.of("general-high","general-low","marshal-high","marshal-low");
                        if(!order.equals(expected))throw new AssertionError("Rank must precede descending skill: "+order);
                        if(screen.choiceButton("marshal-low")!=null)throw new AssertionError("Sorting granted an unavailable choice");
                    });
                    game.getInput().setCursorPos(size[0]-10,size[1]-10);game.waitTicks(2);
                    game.takeScreenshot("hoi-commander-rank-order-"+(group?"group":"army")+"-synthetic-"+size[0]+"x"+size[1]+"-gui2");
                    game.runOnClient(c->{
                        var button=screen.choiceButton(group?"marshal-high":"general-high");
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                        if(sent.size()!=1||!sent.getFirst().token().equals(view.token())||sent.getFirst().revision()!=7)throw new AssertionError("Sorted row must preserve authenticated request");
                        if(!sent.getFirst().choice().equals(group?"marshal-high":"general-high"))throw new AssertionError("Sorted row sent another candidate");
                        if(screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget).map(w->(net.minecraft.client.gui.components.AbstractWidget)w).anyMatch(w->w.active))throw new AssertionError("Pending selection must disable repeat input");
                    });
                }
            }
        } finally {game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(previous));}
    }
    private static void source(ClientGameTestContext game,int[] size) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            for(boolean group:List.of(false,true)) {
                String kind=group?"army-group":"army";
                var view=new ProtocolDialogPayloads.Show(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-appointment-"+kind+"-mixed.json")),true).view().issued(UUID.randomUUID().toString(),7);
                String preferred=group?"원수":"장군";
                var rows=view.tiles().stream().filter(t->t.leader()!=null).sorted(Comparator.<ProtocolDialogView.Tile>comparingInt(t->t.leader().role().equals(preferred)?0:1).thenComparing(Comparator.comparingInt((ProtocolDialogView.Tile t)->t.leader().skill()).reversed()).thenComparing(ProtocolDialogView.Tile::name).thenComparing(ProtocolDialogView.Tile::section)).toList();
                if(rows.stream().noneMatch(t->t.leader().role().equals("원수"))||rows.stream().noneMatch(t->t.leader().role().equals("장군")))throw new AssertionError("Authoritative mixed ranks are required");
                var sent=new ArrayList<ProtocolDialogPayloads.Action>();
                var screen=game.computeOnClient(c->new ScreenCommanderRoster(view,null,sent::add));game.setScreen(()->screen);game.waitTicks(2);
                game.runOnClient(c->{
                    var labels=screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiMenuButton).map(w->((dev.hoi.client.ui.UiMenuButton)w).getMessage().getString()).filter(s->rows.stream().anyMatch(t->t.name().equals(s))).toList();
                    if(labels.isEmpty()||!labels.equals(rows.stream().limit(labels.size()).map(ProtocolDialogView.Tile::name).toList()))throw new AssertionError("Source rank and skill order changed: "+labels);
                });
                game.getInput().setCursorPos(size[0]-10,size[1]-10);game.waitTicks(2);game.takeScreenshot("hoi-commander-source-appointment-"+kind+"-"+size[0]+"x"+size[1]+"-gui2");
                var candidate=rows.stream().filter(t->t.leader().role().equals("장군")&&view.choices().stream().anyMatch(c->c.id().equals(t.section()))).findFirst().orElseThrow();
                if(group&&(!candidate.value().startsWith("지휘력 ")||!candidate.detail().contains("진급")))throw new AssertionError("Source general promotion cost and explanation are required");
                game.runOnClient(c->{var search=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).map(w->(net.minecraft.client.gui.components.EditBox)w).findFirst().orElseThrow();search.setValue(candidate.name());});game.waitTicks(2);
                var position=game.computeOnClient(c->{var button=screen.choiceButton(candidate.section());if(button==null)throw new AssertionError("Source candidate choice disappeared");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
                game.getInput().setCursorPos(position[0]*2,position[1]*2);game.waitTicks(16);game.takeScreenshot("hoi-commander-source-candidate-"+kind+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(c->{
                    var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                    screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                    if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),view.revision(),candidate.section()))))throw new AssertionError("Source candidate must send its authenticated choice exactly once");
                });
            }
        } catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static ProtocolDialogView.Tile tile(String id,String role,int skill) {
        var leader=new ProtocolDialogView.Leader(role,skill,List.of(1,2,3,4),role.equals("원수")?5:24,false,List.of());
        return new ProtocolDialogView.Tile(id,id,role+" · "+skill,"politics/empty/leader","",null,"",leader);
    }
}
