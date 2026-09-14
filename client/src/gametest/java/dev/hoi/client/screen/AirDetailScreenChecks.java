package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.components.Button;
import java.util.*;

public final class AirDetailScreenChecks {
    public static void show(DialogView view) {DialogClient.receive(DialogProtocol.Show.of(view,true));}
    static void run(ClientGameTestContext context) {
        var tiles=new ArrayList<DialogView.Tile>();
        for(int i=0;i<4;i++) {
            tiles.add(t("own","아군",i==0?"199":i==2?"200":"0","air_detail/own_"+i));
            tiles.add(t("enemy","적군",i==0?"191":i==1?"12":i==2?"53":"0","air_detail/enemy_"+i));
        }
        tiles.add(new DialogView.Tile("balance","공중 우세","65%","air_detail/progress_good","공중 우세: §a65%§r",new DialogView.Gauge(.65,"65%")));
        tiles.add(t("weather","비","","air_detail/weather_light_rain"));
        tiles.add(t("weather_icon","비","","map_detail/weather_rain"));
        tiles.add(t("detection","아군 항공 탐지력","28%","air_detail/air_detection_strip_1"));
        tiles.add(t("enemy_efficiency","적의 평균 임무 효율","88%","air_detail/air_mission_efficiency_strip_3"));
        tiles.add(t("enemy_detection","적 항공 탐지력","5%","air_detail/air_detection_strip_1"));
        tiles.add(t("efficiency","아군의 평균 임무 효율","97%","air_detail/air_mission_efficiency_strip_1"));
        tiles.add(new DialogView.Tile("air_losses","아군 공군 부대 손실","53","air_detail/damage_skull","§e아군 공군 부대 손실§r\n이 공역에서 집계 시작 이후 누적: §e53§r\n대한민국 · 근접항공지원기: §c53§r\n  공중전: §c50§r\n  대공에 손실된 공군 부대: §c2§r\n  항공기 사고: §c1§r"));
        tiles.add(t("air_kills","적 공군 부대 격추","12","air_detail/damage_skull"));
        tiles.add(t("air_loss_art","아군 근접항공지원기","53","production/equipment/cas"));
        tiles.add(t("air_kill_art","적군 전투기","12","production/equipment/fighter"));
        tiles.add(t("mission_0","근접항공지원","200","production/equipment/cas"));
        tiles.add(t("mission_1","해상 타격","20","production/equipment/naval_bomber"));
        tiles.add(t("mission_2","전략폭격","100","production/equipment/strategic_bomber"));
        for(int[] size:new int[][]{{2560,1440},{1600,1000},{854,480}}) {
            context.runOnClient(c->c.options.guiScale().set(2));context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(3);
            context.setScreen(()->null);
            var view=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.STATE,"화베이 평원","","","","air_detail/background","",tiles,List.of());
            context.runOnClient(c->{
                DialogClient.receive(DialogProtocol.Show.of(view,true));
                if(!(c.gui.screen() instanceof AirDetailScreen screen))throw new AssertionError("Air marker must open air details");
                if(Math.abs(screen.panelLeft()*2+screen.panelWidth()-screen.width)>1)throw new AssertionError("Air detail must be centered");
                if(screen.panelTop()<dev.hoi.client.ui.HoiMenuBar.height(screen.width)||screen.panelTop()+screen.panelHeight()>screen.height)throw new AssertionError("Air panel escaped viewport");
                for(var child:screen.children())if(child instanceof Button b&&(b.getX()<0||b.getY()<0||b.getRight()>screen.width||b.getBottom()>screen.height))throw new AssertionError("Air button escaped viewport");
                if(size[0]==2560&&screen.panelWidth()!=414)throw new AssertionError("Air panel must grow 50 percent at GUI2");
                for(var child:screen.children())if(child instanceof Button b&&List.of("아군 임무","세부 사항","적군 임무").contains(b.getMessage().getString()))throw new AssertionError("Removed air buttons remain");
                for(var line:dev.hoi.client.ui.HoiTooltips.lines(c.font,"이동: 95.0%",300,false))line.accept((index,style,code)->{
                    if(code>=0xe000&&code<=0xf8ff)throw new AssertionError("Air tooltip must not inject icons");
                    return true;
                });
                for(var tile:tiles)if(!tile.icon().isBlank()&&c.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/"+tile.icon()+".png")).isEmpty())throw new AssertionError("Missing air asset: "+tile.icon());
            });
            context.getInput().setCursorPos(0,0);context.waitTicks(3);context.takeScreenshot("hoi-air-detail-"+size[0]+"-gui2");
            if(size[0]==2560)for(int[] point:new int[][]{{193,191},{325,191},{45,126},{113,126},{164,66}}) {
                var cursor=context.computeOnClient(c->{var screen=(AirDetailScreen)c.gui.screen();double scale=screen.panelWidth()/552.0,gui=c.getWindow().getGuiScale();return new double[]{(screen.panelLeft()+point[0]*scale)*gui,(screen.panelTop()+point[1]*scale)*gui};});
                context.getInput().setCursorPos(cursor[0],cursor[1]);context.waitTicks(3);context.takeScreenshot("hoi-air-tooltip-"+point[0]+"-gui2");
            }
            if(size[0]==2560)for(int state=0;state<3;state++) {
                var scenario=new ArrayList<>(tiles);
                scenario.removeIf(t->t.section().equals("balance"));
                double fraction=state==0?0:state==1?.8:.2;
                String value=state==0?"통제 없음":state==1?"80%":"20%";
                scenario.add(new DialogView.Tile("balance","공중 우세",value,"air_detail/progress_"+(state==0?"disabled":"good"),"공중 우세: "+value,new DialogView.Gauge(fraction,value)));
                if(state==0) {
                    scenario.removeIf(t->t.section().startsWith("air_")||t.section().startsWith("mission_"));
                    scenario.replaceAll(t->t.section().equals("own")||t.section().equals("enemy")?new DialogView.Tile(t.section(),t.name(),"0",t.icon(),t.detail()):t);
                }
                var preview=new DialogView(UUID.randomUUID().toString(),1,view.kind(),view.title(),"","","","air_detail/background","",scenario,List.of());
                context.setScreen(()->new AirDetailScreen(preview));
                context.getInput().setCursorPos(0,0);context.waitTicks(3);
                context.takeScreenshot("hoi-air-control-"+List.of("none","friendly","enemy").get(state)+"-gui2");
            }
            if(size[0]==2560)for(int state=0;state<3;state++) {
                var scenario=new ArrayList<>(tiles);
                scenario.removeIf(t->List.of("weather","weather_icon","efficiency","detection").contains(t.section()));
                String name=List.of("맑음","폭풍","눈보라").get(state);
                String image=List.of("none","heavy_rain","heavy_snow_night").get(state);
                String detail="§e"+name+"§r\n공군 작전 효율: §c"+(state==0?"0%":"-30%")+"§r\n대공 탐지: §c"+(state==0?"0%":state==1?"-20%":"-30%")+"§r\n항공기 사고 확률: §c+"+(state==0?"0%":"3%")+"§r";
                scenario.add(new DialogView.Tile("weather",name,"","air_detail/weather_"+image,detail));
                if(state!=0)scenario.add(new DialogView.Tile("weather_icon",name,"","map_detail/weather_"+(state==1?"storm":"blizzard"),detail));
                scenario.add(t("efficiency","아군의 평균 임무 효율",state==0?"100%":"70%","air_detail/air_mission_efficiency_strip_1"));
                scenario.add(t("detection","아군 항공 탐지력",state==0?"30%":state==1?"24%":"21%","air_detail/air_detection_strip_1"));
                var preview=new DialogView(UUID.randomUUID().toString(),1,view.kind(),view.title(),"","","","air_detail/background","",scenario,List.of());
                context.setScreen(()->new AirDetailScreen(preview));
                var cursor=context.computeOnClient(c->{var screen=(AirDetailScreen)c.gui.screen();double scale=screen.panelWidth()/552.0,gui=c.getWindow().getGuiScale();return new double[]{(screen.panelLeft()+280*scale)*gui,(screen.panelTop()+60*scale)*gui};});
                context.getInput().setCursorPos(cursor[0],cursor[1]);context.waitTicks(3);
                context.takeScreenshot("hoi-air-weather-"+List.of("clear","storm","blizzard-night").get(state)+"-gui2");
            }
            context.runOnClient(c->{
                var state=new DialogView(view.token(),2,view.kind(),view.title(),"","","","map_detail/state","",List.of(),List.of());
                DialogClient.receive(DialogProtocol.Show.of(state,true));if(!(c.gui.screen() instanceof MapDetailScreen))throw new AssertionError("Map mode refresh must switch renderer");
                DialogClient.receive(DialogProtocol.Show.of(view.issued(view.token(),3),true));if(!(c.gui.screen() instanceof AirDetailScreen))throw new AssertionError("Air mode refresh must switch renderer");
                DialogClient.receive(DialogProtocol.Show.of(state,true));if(!(c.gui.screen() instanceof AirDetailScreen))throw new AssertionError("Stale state view replaced air view");
            });
        }
        context.setScreen(()->null);
    }
    private static DialogView.Tile t(String section,String name,String value,String icon){return new DialogView.Tile(section,name,value,icon,"§e"+name+"§r\n"+value);}
}
