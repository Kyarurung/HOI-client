package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class ScreenPoliticsTooltipsTest {
    public static void run(ClientGameTestContext context,ProtocolMenuView base) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(c->c.options.guiScale().set(2));
        String description="점령지에 점령 정책을 설정하고, 해당 구역에 핵심 지역을 가지고 있는 국가를 해방할 수 있습니다.";
        for(boolean occupied:new boolean[]{false,true}) {
            var statuses=List.of(new ProtocolMenuView.Entry("점령지",occupied?"3개 주":"0개 주",(occupied?"§a점령지를 관리하려면 클릭하십시오§r":"§c현재 점령하고 있는 비핵심 영토가 없습니다.§r")+"\n\n"+description,occupied?1:0),new ProtocolMenuView.Entry("협력정부","0개국","§c현재 우리와 협력 중인 정부가 없음§r",0));
            var pages=base.pages().stream().map(page->page.tab()!=ProtocolMenuTab.POLITICS?page:new ProtocolMenuView.Page(page.tab(),page.sections().stream().map(section->new ProtocolMenuView.Section(section.title(),section.icon(),java.util.stream.Stream.concat(section.entries().stream().filter(e->!e.name().equals("점령지")&&!e.name().equals("협력정부")),section==page.sections().getFirst()?statuses.stream():java.util.stream.Stream.empty()).map(entry-> {
                if(entry.name().equals("점령지"))return new ProtocolMenuView.Entry("점령지",occupied?"3개 주":"0개 주",(occupied?"§a점령지를 관리하려면 클릭하십시오§r":"§c현재 점령하고 있는 비핵심 영토가 없습니다.§r")+"\n\n"+description,occupied?1:0);
                if(entry.name().equals("협력정부"))return new ProtocolMenuView.Entry("협력정부","0개국","§c현재 우리와 협력 중인 정부가 없음§r",0);
                if(entry.icon().startsWith("politics/party/") || entry.icon().startsWith("politics/ideology/"))return new ProtocolMenuView.Entry(entry.name(),entry.value(),"§e"+entry.name()+"§r\n정당 이념: §e보수주의§r\n국가 원수: §e문재인§r\n인기도: §e49.29%§r\n매일 변화: §e+0.00§r",entry.progress(),entry.icon());
                return entry;
            }).toList())).toList())).toList();
            var view=new ProtocolMenuView(base.country(),base.countryName(),base.date(),base.speed(),pages,base.hud());
            context.setScreen(()->new ScreenMenu(view));context.waitTicks(3);
            for(int target=0;target<3;target++) {
                final int index=target;
                var point=context.computeOnClient(c->{
                    var screen=(ScreenMenu)c.gui.screen();
                    var box=index==2?screen.politicsLayout().parties():screen.politicsLayout().status(index);
                    int x=box.x()+box.width()/2,y=box.y()+(index==2?8:box.height()/2);
                    var hovered=screen.politicsHover(x,y);
                    if(hovered==null)throw new AssertionError("Political tooltip hit area missing: "+index);
                    if(index<2&&!hovered.name().equals(index==0?"점령지":"협력정부"))throw new AssertionError("Wrong status tooltip target");
                    double scale=c.getWindow().getGuiScale();return new double[]{x*scale,y*scale};
                });
                context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                context.takeScreenshot("hoi-politics-"+List.of("occupation","collaboration","party").get(target)+(occupied?"-active":"-inactive")+"-gui2");
            }
            context.runOnClient(c->{
                var screen=(ScreenMenu)c.gui.screen();
                var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("점령지")).findFirst().orElseThrow();
                if(button.active!=occupied)throw new AssertionError("Occupation availability differs from authoritative view");
                if(occupied) {
                    button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                    if(!(c.gui.screen() instanceof ScreenOccupation))throw new AssertionError("Politics occupation button must open occupation screen");
                }
            });
        }
        context.setScreen(()->null);
        context.getInput().resizeWindow(1600,1000);
    }
}
