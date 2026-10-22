package dev.hoi.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Style;

public final class ScreenDecisionInteractionTest {
    private ScreenDecisionInteractionTest() {}
    public static void run(ClientGameTestContext context,ProtocolCountryHud hud) {
        var sounds=new ArrayList<String>();
        net.minecraft.client.sounds.SoundEventListener listener=(sound,event,range)->{
            if(sound.getIdentifier().getPath().startsWith("ui.decision."))sounds.add(sound.getIdentifier().getPath());
        };
        context.runOnClient(client->client.getSoundManager().addListener(listener));
        try {
            var sent=new ArrayList<ProtocolDecisionPayloads.Request>();
            String session=UUID.randomUUID().toString();
            var rows=List.of(
                    new ProtocolDecisionPayloads.Row("ready","decisions","결정 검증","","실행 가능한 결정","50일, 150과 정치력 비용은 별개입니다.","",50,0,"실행 가능",true,"50","","READY",null),
                    new ProtocolDecisionPayloads.Row("cooldown","decisions","결정 검증","","재사용 대기 결정","§Y첫 줄\n둘째 줄§!\n---\n수량: 0.00\n£unregistered_decision_fixture","",50,24,"재사용 대기",false,"50","","COOLDOWN",48L));
            var categories=List.of(new ProtocolDecisionPayloads.Category("empty","행 없는 카테고리","§Y원문 설명\n다음 줄§!\n\n0.00","","","",true),
                    new ProtocolDecisionPayloads.Category("decisions","결정 검증","","","","",false));
            var initial=new ProtocolDecisionPayloads.View("KOR",session,1,"",0,2,hud,rows,categories,1);
            int scale=context.computeOnClient(client->client.options.guiScale().get());
            context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
            context.setScreen(()->new ScreenDecision(initial,sent::add));context.waitTicks(3);
            context.runOnClient(client->{
                var screen=(ScreenDecision)client.gui.screen();
                if(button(screen,"행 없는 카테고리")==null)throw new AssertionError("Independent empty category is required");
                var text=new ScreenDecisionText();
                var lines=text.rows("§Y첫 줄\n둘째 줄§!\n---\n수량: 0.00\n£unregistered_decision_fixture");
                var colors=new ArrayList<Integer>();
                lines.get(1).visit((style,part)->{if(!part.isEmpty())colors.add(style.getColor()==null?-1:style.getColor().getValue());return Optional.empty();},Style.EMPTY);
                if(colors.isEmpty() || colors.stream().anyMatch(color->color!=UiTooltips.GOLD))throw new AssertionError("Source color must survive a newline");
                if(!lines.get(2).getString().isEmpty() || !lines.get(3).getString().equals("수량: 0.00")
                        || !lines.get(4).getString().isEmpty())throw new AssertionError("Source separators and precision must survive while unknown icons stay hidden");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,2,"",0,2,hud,List.of(),categories,2)));
                if(screen.rowCount()!=2 || button(screen,"실행 가능한 결정").active || sent.size()!=1 || sent.getFirst().page()!=1)
                    throw new AssertionError("A partial snapshot must retain old rows and lock actions");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,2,"",1,2,hud,rows,List.of(),2)));
                if(!button(screen,"실행 가능한 결정").active)throw new AssertionError("The complete snapshot must unlock actions");
            });
            var point=context.computeOnClient(client->{var button=button((ScreenDecision)client.gui.screen(),"실행 가능한 결정");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
            context.getInput().setCursorPos(point[0]*2,point[1]*2);context.waitTicks(2);context.takeScreenshot("hoi-decisions-authoritative-pages-gui2");
            context.runOnClient(client->{
                var screen=(ScreenDecision)client.gui.screen();
                screen.mouseClicked(click(point,InputConstants.MOUSE_BUTTON_RIGHT),false);
                if(sent.size()!=1)throw new AssertionError("Right click must not take a decision");
                screen.mouseClicked(click(point,InputConstants.MOUSE_BUTTON_LEFT),false);
                screen.mouseClicked(click(point,InputConstants.MOUSE_BUTTON_LEFT),true);
                if(sent.size()!=2 || !sent.getLast().id().equals("ready") || sent.getLast().revision()!=2 || !screen.pending()
                        || sounds.stream().filter(id->id.equals("ui.decision.take")).count()!=1)
                    throw new AssertionError("Repeated clicks must issue one authoritative TAKE");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,2,"",0,2,hud,rows,categories,1)));
                if(!screen.pending() || button(screen,"실행 가능한 결정").active)throw new AssertionError("Equal revision must not unlock a pending action");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,3,"",0,2,hud,rows,categories,1)));
                if(screen.pending() || !button(screen,"실행 가능한 결정").active || button(screen,"재사용 대기 결정").active
                        || sounds.size()!=1)
                    throw new AssertionError("Only authoritative available rows may be taken");
            });
            context.getInput().resizeWindow(1280,720);context.waitTicks(3);context.takeScreenshot("hoi-decisions-authoritative-pages-compact-gui2");
            context.runOnClient(client->{
                var screen=(ScreenDecision)client.gui.screen();
                for(var child:screen.children())if(child instanceof Button button && (button.getX()<0 || button.getY()<0 || button.getRight()>screen.width || button.getBottom()>screen.height))
                    throw new AssertionError("Decision controls must stay in the viewport");
                screen.onClose();client.options.guiScale().set(scale);
            });
            context.getInput().resizeWindow(1600,1000);context.waitTicks(2);
            legacy(context,hud);
        } finally {context.runOnClient(client->client.getSoundManager().removeListener(listener));}
    }
    private static void legacy(ClientGameTestContext context,ProtocolCountryHud hud) {
        var sent=new ArrayList<ProtocolDecisionPayloads.Request>();
        String session=UUID.randomUUID().toString();
        var row=new ProtocolDecisionPayloads.Row("legacy","legacy_category","구형 결정","","구형 실행","","",25,0,"실행 가능",true);
        context.setScreen(()->new ScreenDecision(null,sent::add));context.waitTicks(2);
        context.runOnClient(client->{
            var screen=(ScreenDecision)client.gui.screen();
            screen.update(legacyPacket(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,7,"",0,49,hud,List.of(row))));
            var next=button(screen,"→");
            String oldToken=screen.token();
            screen.mouseClicked(click(new int[]{next.getX()+next.getWidth()/2,next.getY()+next.getHeight()/2},InputConstants.MOUSE_BUTTON_LEFT),false);
            if(sent.size()!=1 || sent.getLast().page()!=1 || oldToken.equals(screen.token()))throw new AssertionError("Legacy pages require a fresh response token");
            screen.update(new ProtocolDecisionPayloads.Response(oldToken,""));
            if(client.gui.screen()!=screen)throw new AssertionError("A delayed legacy token must not revoke the current page");
            screen.update(legacyPacket(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,8,"",1,49,hud,List.of(row))));
            var take=button(screen,"구형 실행");
            oldToken=screen.token();
            screen.mouseClicked(click(new int[]{take.getX()+take.getWidth()/2,take.getY()+take.getHeight()/2},InputConstants.MOUSE_BUTTON_LEFT),false);
            if(sent.size()!=2 || !screen.pending() || oldToken.equals(screen.token()))throw new AssertionError("Legacy TAKE must rotate its response token");
            screen.update(legacyPacket(screen.token(),new ProtocolDecisionPayloads.View("KOR",UUID.randomUUID().toString(),0,"",0,1,hud,List.of(row))));
            if(screen.pending() || screen.rowCount()!=1 || !button(screen,"구형 실행").active)throw new AssertionError("A correlated legacy TAKE reply may reset its session");
            screen.update(new ProtocolDecisionPayloads.Response(oldToken,""));
            if(client.gui.screen()!=screen)throw new AssertionError("Previous legacy action replies must stay rejected");
            screen.onClose();
        });
    }
    private static ProtocolDecisionPayloads.Response legacyPacket(String token,ProtocolDecisionPayloads.View view) {
        var json=com.google.gson.JsonParser.parseString(ProtocolDecisionPayloads.Response.of(token,view).json()).getAsJsonObject();
        json.remove("pageCount");json.remove("categories");
        return new ProtocolDecisionPayloads.Response(token,json.toString());
    }
    private static MouseButtonEvent click(int[] point,int button) {
        return new MouseButtonEvent(point[0],point[1],new MouseButtonInfo(button,0));
    }
    private static Button button(ScreenDecision screen,String name) {
        return screen.children().stream().filter(child->child instanceof Button button && button.getMessage().getString().equals(name))
                .map(child->(Button)child).findFirst().orElseThrow();
    }
}
