package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.*;

public final class ScreenAdvisorPoliticsTest {
    private ScreenAdvisorPoliticsTest() {}
    private static String json(String name)throws java.io.IOException{return Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),name+".json"));}
    private static ProtocolDialogView dialog(String name)throws java.io.IOException{return new ProtocolDialogPayloads.Show(json(name),true).view().issued(UUID.randomUUID().toString(),7);}
    private static void click(ScreenCommanderRoster screen,AbstractWidget button){var event=new MouseButtonEvent(button.getX()+1.,button.getY()+1.,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);}
    public static void run(ClientGameTestContext game){
        int scale=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var ins=new Gson().fromJson(json("ins-menu-open-screen"),ProtocolMenuPayloads.OpenScreen.class).view();
            var before=dialog("kor-politics-staff-before-promote");var after=dialog("kor-politics-staff-after-promote");
            var added=after.tiles().stream().filter(t->before.tiles().stream().noneMatch(b->b.section().equals(t.section()))).toList();
            if(added.size()!=1||after.tiles().size()!=before.tiles().size()+1||after.choices().stream().noneMatch(c->c.id().equals(added.getFirst().section())))throw new AssertionError("Server advisor promotion must add exactly one selectable political candidate");
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                for(String stage:List.of("before","after")){
                    var roles=dialog("kor-advisor-roles-"+stage+"-promote");
                    game.setScreen(()->new ScreenCommanderTraits(roles,null));game.getInput().setCursorPos(size[0]-8,80);game.waitTicks(3);game.takeScreenshot("hoi-advisor-roles-"+stage+"-"+size[0]+"-gui2");
                    var view=stage.equals("before")?before:after;var sent=new ArrayList<ProtocolDialogPayloads.Action>();
                    var screen=game.computeOnClient(c->new ScreenPoliticsChoice(view,null,sent::add));game.setScreen(()->screen);game.waitTicks(3);
                    game.takeScreenshot("hoi-politics-advisor-"+stage+"-"+size[0]+"-gui2");
                    if(stage.equals("after"))game.runOnClient(c->{var button=screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals(added.getFirst().name())).map(w->(AbstractWidget)w).findFirst().orElseThrow();var event=new MouseButtonEvent(button.getX()+1.,button.getY()+1.,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);screen.mouseClicked(event,true);screen.mouseReleased(event);if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),7,added.getFirst().section()))))throw new AssertionError("Added political candidate must send one authenticated request: "+sent+" active="+button.active+" token="+view.token());});
                }
                var roster=dialog("kor-army-roster-tooltips");var sent=new ArrayList<ProtocolDialogPayloads.Action>();var screen=game.computeOnClient(c->new ScreenCommanderRoster(roster,null,sent::add));game.setScreen(()->screen);
                for(boolean descending:List.of(true,false)){
                    game.runOnClient(c->{var button=screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("고문 역할 정렬")).map(w->(AbstractWidget)w).findFirst().orElseThrow();click(screen,button);var comparator=Comparator.<ProtocolDialogView.Tile>comparingInt(t->t.leader().advisorState()).thenComparingInt(t->t.leader().skill());if(descending)comparator=comparator.reversed();var expected=roster.tiles().stream().filter(t->t.leader()!=null).sorted(comparator).toList();var visible=expected.stream().filter(t->screen.choiceButton(t.section())!=null).sorted(Comparator.comparingInt(t->screen.choiceButton(t.section()).getY())).toList();if(visible.isEmpty()||!visible.equals(expected.subList(0,visible.size()))||!sent.isEmpty())throw new AssertionError("Advisor sorting must preserve server state and skill ordering without a command");});
                    game.waitTicks(3);game.takeScreenshot("hoi-advisor-sort-"+(descending?"descending":"ascending")+"-"+size[0]+"-gui2");
                }
                var menu=game.computeOnClient(c->new ScreenMenu(ins,ProtocolMenuTab.POLITICS,()->{}));game.setScreen(()->menu);game.waitTicks(3);
                var point=game.computeOnClient(c->{var box=menu.politicsLayout().spirits();for(int x=box.x();x<box.x()+box.width();x++){var spirit=menu.politicsHover(x,box.y()+8);if(spirit!=null&&spirit.name().equals("정글전 전문성")){if(!spirit.detail().equals("정글 지형 사단 공격 혜택: §G10.0%§!\n산악 지형 사단 공격 혜택: §G10.8%§!"))throw new AssertionError("Original custom tooltip wording and precision must survive");return new int[]{x,box.y()+8};}}throw new AssertionError("INS jungle expertise hover not found");});
                game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(4);game.takeScreenshot("hoi-ins-jungle-expertise-"+size[0]+"-gui2");
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
    }
}
