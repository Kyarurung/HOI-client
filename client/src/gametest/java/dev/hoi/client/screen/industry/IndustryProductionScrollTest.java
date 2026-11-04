package dev.hoi.client.screen.industry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.*;

public final class IndustryProductionScrollTest {
    private IndustryProductionScrollTest() {}
    private static Object field(Object target,String name) {
        try {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
        catch(ReflectiveOperationException e){throw new RuntimeException(e);}
    }
    private static MouseButtonEvent mouse(double x,double y) {return new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));}
    private static void press(IndustryScreen screen,String name) {
        var button=screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(name)).map(w->(Button)w).findFirst().orElseThrow(()->new AssertionError(name));
        var click=mouse(button.getX()+2,button.getY()+2);
        screen.mouseClicked(click,false);screen.mouseReleased(click);
    }
    public static void run(ClientGameTestContext game) {
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        var gson=new Gson();
        var source=IndustryScreenChecks.bundledFixture("production-source-fixture.json");
        var industry=IndustryScreenChecks.bundledFixture("industry-fixture.json");
        var pages=List.of("lines","infantry","armor","air","navy","hull","repairs","ports");
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                for(boolean overflow:List.of(false,true))for(String page:pages) {
                    var json=gson.toJsonTree(source).getAsJsonObject();
                    var equipment=new JsonArray();
                    for(String group:List.of("infantry","armor","air","navy")) {
                        var model=source.equipment().stream().filter(e->e.group().equals(group)&&e.unlocked()&&!e.outdated()&&(!e.naval()||!e.family().equals("convoy"))).findFirst().orElseThrow();
                        for(int i=0;i<(overflow?28:1);i++) {
                            var item=gson.toJsonTree(model).getAsJsonObject();item.addProperty("id",model.id()+"/scroll/"+i);item.addProperty("name",model.name()+" "+(i+1));equipment.add(item);
                        }
                    }
                    json.add("equipment",equipment);
                    var lines=new JsonArray();
                    String ship=equipment.get((overflow?28:1)*3).getAsJsonObject().get("id").getAsString();
                    for(int i=0;i<(overflow?28:1);i++)lines.add(gson.toJsonTree(new IndustryView.Line("scroll-line-"+i,ship,1,1,.5,1,.1,0)));
                    json.add("lines",lines);
                    var targets=new ArrayList<IndustryView.NavalTarget>();
                    for(int i=0;i<(overflow?40:1);i++)targets.add(new IndustryView.NavalTarget("port-"+i,"항구 "+(i+1),"port-"+i,5,0));
                    json.add("navalDeployment",gson.toJsonTree(new IndustryView.NavalDeployment(List.of(),Map.of(),Map.of("scroll-line-0",""),targets)));
                    var repairs=gson.toJsonTree(industry.navalRepairs()).getAsJsonObject();
                    var ports=new JsonArray();
                    for(int i=0;i<(overflow?40:1);i++)ports.add(gson.toJsonTree(new IndustryView.RepairPort("port-"+i,"항구 "+(i+1),5,0,List.of())));
                    repairs.add("ports",ports);repairs.add("ships",new JsonArray());json.add("navalRepairs",repairs);
                    var view=gson.fromJson(json,IndustryView.class);
                    var sent=new ArrayList<IndustryPayloads.Request>();
                    var screen=game.computeOnClient(c->new IndustryScreen(MenuTab.PRODUCTION,view.session(),view,sent::add));
                    game.setScreen(()->screen);
                    game.runOnClient(c->{
                        if(!page.equals("lines"))press(screen,switch(page){case "infantry"->"보병 및 포병 장비 제작";case "armor"->"기갑 차량 제작";case "air"->"항공기 제작";case "navy","hull"->"함선 건조";case "repairs"->"해군 수리 대기열";default->"생산 배치 대상 · scroll-line-0";});
                        if(page.equals("hull")) {
                            var model=view.equipment().stream().filter(IndustryView.Equipment::naval).findFirst().orElseThrow();
                            var b=screen.children().stream().filter(w->w instanceof Button button&&button.getMessage().getString().equals(model.familyName())).map(w->(Button)w).findFirst().orElseThrow();
                            b.onPress(new KeyEvent(InputConstants.KEY_RETURN,0,0));
                        }
                    });
                    game.getInput().setCursorPos(size[0]-3,size[1]-3);game.waitTicks(2);
                    String name="hoi-production-scroll-"+page+"-"+(overflow?"overflow":"fit")+"-"+size[0];
                    game.takeScreenshot(name);
                    String barField=page.equals("lines")?"productionScrollbar":"productionPickerScrollbar",offsetField=page.equals("lines")?"scroll":"pickerScroll";
                    game.runOnClient(c->{
                        var bar=(UiScrollBar)field(screen,barField);
                        if(bar==null||(bar.maximum()>0)!=overflow)throw new AssertionError("Unexpected overflow: "+name);
                        if(bar.x()<0||bar.x()+9>screen.width||bar.top()<0||bar.bottom()>screen.height)throw new AssertionError("Scrollbar bounds: "+name);
                        int maximum=bar.maximum();
                        screen.mouseScrolled(bar.x()-15,bar.top()+10,0,-1);
                        int wheel=(int)field(screen,offsetField);
                        if(overflow?wheel<=0:wheel!=0)throw new AssertionError("Wheel: "+name);
                        var down=mouse(bar.x()+4,bar.bottom()-3);screen.mouseClicked(down,false);screen.mouseReleased(down);
                        int advanced=(int)field(screen,offsetField);
                        if(overflow&&advanced<=wheel)throw new AssertionError("Down arrow: "+name);
                        var up=mouse(bar.x()+4,bar.top()+3);screen.mouseClicked(up,false);screen.mouseReleased(up);
                        if(overflow&&(int)field(screen,offsetField)>=advanced)throw new AssertionError("Up arrow: "+name);
                        if(overflow) {
                            screen.mouseClicked(mouse(bar.x()+4,bar.top()+12),false);
                            screen.mouseDragged(mouse(bar.x()+4,bar.bottom()-8),0,bar.bottom()-bar.top());
                            screen.mouseReleased(mouse(bar.x()+4,bar.bottom()-8));
                            if((int)field(screen,offsetField)!=maximum)throw new AssertionError("Drag to last row: "+name);
                            String last=switch(page) {
                                case "lines"->"군수산업체 · scroll-line-27";
                                case "repairs"->"수리 항구: 항구 40";
                                case "ports"->"항구 40";
                                default->"생산: "+view.equipment().stream().filter(e->e.group().equals(page.equals("hull")?"navy":page)).reduce((a,b)->b).orElseThrow().name();
                            };
                            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals(last)&&b.getBottom()<=bar.bottom()))throw new AssertionError("Last row is unreachable: "+name+" / "+last);
                            if(!page.equals("lines")&&(int)field(screen,"scroll")!=0)throw new AssertionError("Picker scroll changed production list: "+name);
                        }
                        if(sent.stream().anyMatch(r->r.action()!=IndustryPayloads.Action.REFRESH))throw new AssertionError("Scrolling mutated gameplay: "+sent);
                    });
                    if(overflow){game.waitTicks(2);game.takeScreenshot(name+"-bottom");}
                }
            }
        } finally {game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(previous));}
    }
}
