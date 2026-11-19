package dev.hoi.client.screen.industry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.industry.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class IndustryConstructionScrollTest {
    private IndustryConstructionScrollTest() {}
    private static Object field(Object target,String name) {
        try {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static MouseButtonEvent mouse(double x,double y) {return new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    public static void run(ClientGameTestContext game,CountryHud hud) {
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        var buildings=new ArrayList<IndustryConstructionView.Building>();
        for(String id:List.of("infrastructure","air_base","anti_air","radar","military_factory","civilian_factory","dockyard","office_park","refinery","fuel_silo","rocket_site","nuclear_reactor","power_plant","energy_farm","conversion","hub","railway","port","fort","coastal_fort"))
            buildings.add(new IndustryConstructionView.Building(id,id,"construction/"+id,true,""));
        var summary=new IndustryConstructionView.Summary(43,7,7,25,4,0,4,1.31,10,21.86);
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                for(int count:new int[]{0,1,24}) {
                    var projects=new ArrayList<IndustryConstructionView.Project>();
                    for(int i=0;i<count;i++)projects.add(new IndustryConstructionView.Project("scroll-project-"+i,"state-"+i,"civilian_factory","건설 스크롤 입력 "+(i+1),1,0,0,0,10800));
                    String token=UUID.randomUUID().toString();
                    var view=new IndustryConstructionView(token,1,"KOR",hud,"",summary,buildings,projects,"");
                    var sent=new ArrayList<IndustryConstructionPayloads.Request>();
                    var screen=game.computeOnClient(c->new IndustryConstruction(token,view,sent::add));
                    game.setScreen(()->screen);
                    String name="hoi-construction-scroll-"+count+"-"+size[0];
                    game.getInput().setCursorPos(size[0]-3,size[1]-3);game.waitTicks(2);game.takeScreenshot(name);
                    game.runOnClient(c->{
                        var queue=(UiScrollBar)field(screen,"queueScrollbar");
                        if((queue.maximum()>0)!=(count==24))throw new AssertionError("Queue overflow: "+name);
                        for(String part:List.of("queue","palette")) {
                            var bar=(UiScrollBar)field(screen,part+"Scrollbar");
                            String offset=part.equals("queue")?"scroll":"paletteScroll";
                            if(bar.x()<0||bar.x()+9>screen.panelWidth()||bar.top()<0||bar.bottom()>screen.height)throw new AssertionError("Bounds: "+name+part);
                            if(bar.thumbHeight()!=8)throw new AssertionError("Original thumb stretched: "+name+part);
                            if(bar.maximum()==0)continue;
                            int before=(int)field(screen,offset);
                            screen.mouseScrolled(bar.x()+3,bar.top()+10,0,-1);
                            int wheel=(int)field(screen,offset);
                            if(wheel<=before)throw new AssertionError("Wheel: "+name+part);
                            var down=mouse(bar.x()+4,bar.bottom()-3);screen.mouseClicked(down,false);screen.mouseReleased(down);
                            int advanced=(int)field(screen,offset);
                            if(advanced<=wheel)throw new AssertionError("Down: "+name+part);
                            var up=mouse(bar.x()+4,bar.top()+3);screen.mouseClicked(up,false);screen.mouseReleased(up);
                            if((int)field(screen,offset)>=advanced)throw new AssertionError("Up: "+name+part);
                            screen.mouseClicked(mouse(bar.x()+4,bar.top()+12),false);
                            screen.mouseDragged(mouse(bar.x()+4,bar.bottom()-8),0,bar.bottom()-bar.top());
                            screen.mouseReleased(mouse(bar.x()+4,bar.bottom()-8));
                            if((int)field(screen,offset)!=bar.maximum())throw new AssertionError("Drag: "+name+part);
                        }
                        if(count==24) {
                            int row=screen.projectRow(23);
                            if(screen.rowBottom(row)>screen.listBottom()||screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("×")&&b.getY()>=screen.rowTop(row)&&b.getY()<screen.rowBottom(row)))throw new AssertionError("Last project unreachable: "+name);
                        }
                        if(sent.stream().anyMatch(r->r.action()!=IndustryConstructionPayloads.Action.REFRESH))throw new AssertionError("Scrolling sent gameplay requests: "+sent);
                    });
                    game.waitTicks(2);game.takeScreenshot(name+"-bottom");
                    game.runOnClient(c->{
                        screen.update(new IndustryConstructionView(token,2,"KOR",hud,"",summary,List.of(),List.of(),""));
                        if((int)field(screen,"scroll")!=0||(int)field(screen,"paletteScroll")!=0)throw new AssertionError("Shrink must reset offset");
                        if(((UiScrollBar)field(screen,"queueScrollbar")).maximum()!=0||((UiScrollBar)field(screen,"paletteScrollbar")).maximum()!=0)throw new AssertionError("Empty lists must hide bars");
                    });
                }
            }
        } finally {game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(previous));}
    }
}
