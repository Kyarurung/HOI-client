package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import dev.hoi.client.ui.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class ScreenDecisionChecks {
    public static void run(ClientGameTestContext context, ProtocolCountryHud hud) {
        var sent = new ArrayList<ProtocolDecisionPayloads.Request>();
        var rows = new ArrayList<ProtocolDecisionPayloads.Row>();
        for(int i=0;i<24;i++) rows.add(new ProtocolDecisionPayloads.Row("fixture_"+i,"category_"+(i/6),i<6?"국군 혁신":"결정 카테고리 "+(i/6),
                "GFX_decision_icon_KOR_lee_army_innovation_icon",i==0?"차세대 6.8mm 소총 개발":i==1?"전투원 장비 개선":"검증용 결정 "+i,
                "원본 에셋과 상태 갱신을 확인하는 화면 검증용 데이터입니다.\n연구 보너스: §a+150%§r",
                "GFX_decision_icon_KOR_lee_army_descion_iocn_5_icon",50,i==1?720:0,i==1?"진행 중":"실행 가능",i!=1));
        var view = new ProtocolDecisionPayloads.View("KOR",UUID.randomUUID().toString(),1,"",0,rows.size(),hud,rows);
        context.setScreen(() -> new ScreenDecision(view,sent::add));context.waitTicks(3);
        context.runOnClient(c -> {
            for(String path:List.of("decision/category","decision/row","decision/timer","decision/select","decision/icon/gfx_decision_icon_kor_lee_army_descion_iocn_5_icon"))
                if(c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+path+".png")).isEmpty())throw new AssertionError("Missing decision art: "+path);
            var screen=(ScreenDecision)c.gui.screen();
            if(button(screen,"전투원 장비 개선").active)throw new AssertionError("An active decision must not be clickable again");
            button(screen,"차세대 6.8mm 소총 개발").onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            if(sent.size()!=1||sent.getFirst().action()!=ProtocolDecisionPayloads.Action.TAKE||!sent.getFirst().id().equals("fixture_0"))throw new AssertionError("Decision must issue exactly one action");
            if(button(screen,"차세대 6.8mm 소총 개발").active)throw new AssertionError("Pending action must lock repeated clicks");
            screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",view.session(),2,"결정 실행",0,rows.size(),hud,rows)));
        });
        context.getInput().setCursorPos(100,260);context.waitTicks(2);
        context.takeScreenshot("hoi-decisions-original-assets");
        dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-decisions-gui2");
        context.runOnClient(c -> {
            var screen=(ScreenDecision)c.gui.screen();
            button(screen,"국군 혁신").onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            screen.mouseScrolled(20,screen.height/2,0,-4);int scroll=screen.scrollOffset();
            screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",view.session(),3,"",0,rows.size(),hud,rows)));
            if(!screen.collapsed("category_0")||scroll!=screen.scrollOffset()||scroll==0)throw new AssertionError("Refresh must preserve fold and scroll");
        });
        context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-decisions-compact");
        context.runOnClient(c -> {
            var screen=(ScreenDecision)c.gui.screen();
            for(var child:screen.children())if(child instanceof Button b && (b.getX()<0||b.getY()<0||b.getRight()>screen.width||b.getBottom()>screen.height))throw new AssertionError("Decision control overflow");
            screen.update(new ProtocolDecisionPayloads.Response(UUID.randomUUID().toString(),""));
            if(c.gui.screen()!=screen)throw new AssertionError("Unrelated response must not close the screen");
            screen.update(new ProtocolDecisionPayloads.Response(screen.token(),""));
            if(c.gui.screen()!=null)throw new AssertionError("Revoked decisions must close");
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        ScreenDecisionInteractionTest.run(context,hud);
        ScreenDecisionPanelTest.run(context,hud);
        ScreenDecisionTargetTest.run(context,hud);
        ScreenDecisionStateTest.run(context,hud);
        ScreenDecisionSourceTest.run(context);
    }
    private static Button button(ScreenDecision screen,String name) {
        return (Button)screen.children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow();
    }
}
