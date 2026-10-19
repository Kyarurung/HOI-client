package dev.hoi.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiStoryAssets;
import dev.hoi.protocol.ProtocolCountryHud;
import dev.hoi.protocol.ProtocolDecisionPayloads;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class ScreenDecisionTargetTest {
    private ScreenDecisionTargetTest() {}
    public static void run(ClientGameTestContext context, ProtocolCountryHud hud) {
        var sent=new ArrayList<ProtocolDecisionPayloads.Request>();
        String session=UUID.randomUUID().toString();
        var country=new ProtocolDecisionPayloads.Target("country","KOR","대한민국","KOR_chousen_tag_communism");
        var state=new ProtocolDecisionPayloads.Target("state","1","검증용 주","");
        var rows=List.of(row("synthetic@c:KOR",country,true,0),row("synthetic@s:1",state,true,0),
                row("active@s:1",state,false,48));
        int scale=context.computeOnClient(client->client.options.guiScale().get());
        var sounds=new ArrayList<String>();
        net.minecraft.client.sounds.SoundEventListener listener=(sound,event,range)->{
            if(sound.getIdentifier().getPath().equals("ui.decision.take"))sounds.add("take");
        };
        context.runOnClient(client->client.getSoundManager().addListener(listener));
        try {
            context.getInput().resizeWindow(2560,1440);
            context.runOnClient(client->client.options.guiScale().set(2));
            context.setScreen(()->new ScreenDecision(view(session,1,hud,rows),sent::add));
            context.waitTicks(3);
            context.runOnClient(client->{
                String mapped=UiStoryAssets.flag("KOR",country.flag());
                if(!mapped.startsWith("story/flags/kor/") || UiAssets.imageHeight(mapped,20)<=0)
                    throw new AssertionError("Exact server cosmetic flag must resolve to the external original asset");
                if(!UiStoryAssets.flag("KOR","unregistered").equals("country/kor/flag"))
                    throw new AssertionError("Missing flags must retain the country's default flag");
                var screen=(ScreenDecision)client.gui.screen();
                if(button(screen,rows.get(2)).active)throw new AssertionError("An active target mission must stay disabled");
            });
            context.takeScreenshot("hoi-decisions-targets-synthetic-gui2");
            int[] point=context.computeOnClient(client->point(button((ScreenDecision)client.gui.screen(),rows.getFirst())));
            context.getInput().setCursorPos(point[0]*2,point[1]*2);
            context.waitTicks(2);
            context.takeScreenshot("hoi-decisions-targets-synthetic-tooltip-gui2");
            context.getInput().resizeWindow(1280,720);context.waitTicks(3);
            context.takeScreenshot("hoi-decisions-targets-synthetic-compact-gui2");
            context.getInput().resizeWindow(2560,1440);context.waitTicks(3);
            context.runOnClient(client->{
                var screen=(ScreenDecision)client.gui.screen();
                screen.mouseClicked(click(point,InputConstants.MOUSE_BUTTON_RIGHT),false);
                screen.mouseDragged(click(point,InputConstants.MOUSE_BUTTON_LEFT),4,4);
                if(!sent.isEmpty())throw new AssertionError("Right click and dragging must not execute a target decision");
                screen.mouseClicked(click(point,InputConstants.MOUSE_BUTTON_LEFT),false);
                screen.mouseClicked(click(point,InputConstants.MOUSE_BUTTON_LEFT),true);
                if(sent.size()!=1 || !sent.getFirst().id().equals("synthetic@c:KOR") || sounds.size()!=1)
                    throw new AssertionError("Country TAKE must preserve the full row ID and lock duplicate input/sound: requests="+sent+", sounds="+sounds+", pending="+screen.pending());
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),view(session,1,hud,rows)));
                if(!screen.pending())throw new AssertionError("An equal revision must retain the action lock");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),view(session,2,hud,rows.subList(1,3))));
                if(screen.pending() || screen.rowCount()!=2)throw new AssertionError("Removed targets must disappear after an authoritative revision");
                screen.mouseClicked(click(point(button(screen,rows.get(1))),InputConstants.MOUSE_BUTTON_LEFT),false);
                if(sent.size()!=2 || !sent.getLast().id().equals("synthetic@s:1") || sent.getLast().revision()!=2)
                    throw new AssertionError("State TAKE must preserve the full row ID and current revision");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),view(session,3,hud,List.of(rows.get(2)))));
                if(screen.pending() || button(screen,rows.get(2)).active)throw new AssertionError("An active mission must remain authoritative");
            });
            context.getInput().resizeWindow(1280,720);context.waitTicks(3);
            context.takeScreenshot("hoi-decisions-targets-synthetic-active-compact-gui2");
            context.runOnClient(client->{
                var screen=(ScreenDecision)client.gui.screen();
                for(var child:screen.children())if(child instanceof Button button && (button.getX()<0 || button.getY()<0 || button.getRight()>screen.width || button.getBottom()>screen.height))
                    throw new AssertionError("Target controls must stay in the viewport");
                screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),view(session,4,hud,List.of())));
                if(screen.rowCount()!=0)throw new AssertionError("Expired targets must disappear");
                screen.update(ProtocolDecisionPayloads.Response.of(UUID.randomUUID().toString(),view(session,5,hud,rows)));
                if(screen.rowCount()!=0)throw new AssertionError("Unrelated responses must not restore targets");
                screen.update(new ProtocolDecisionPayloads.Response(screen.token(),""));
                if(client.gui.screen()!=null)throw new AssertionError("Revocation must close the private screen");
            });
        } finally {
            context.runOnClient(client->{client.getSoundManager().removeListener(listener);client.options.guiScale().set(scale);});
            context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        }
    }
    private static ProtocolDecisionPayloads.Row row(String id,ProtocolDecisionPayloads.Target target,boolean enabled,long hours) {
        return new ProtocolDecisionPayloads.Row(id,"targets","합성 대상 입력 검증","",id.startsWith("active")?"검증용 임무":"검증용 결정","§Y원문 형식§!\n\n수치: 0.00","",50.0,hours,
                enabled?"":"진행 중",enabled,"50","",enabled?"READY":"ACTIVE",hours==0?null:48L,target,List.of(1));
    }
    private static ProtocolDecisionPayloads.View view(String session,long revision,ProtocolCountryHud hud,List<ProtocolDecisionPayloads.Row> rows) {
        return new ProtocolDecisionPayloads.View("KOR",session,revision,"합성 입력: 실제 캠페인 producer 검증과 구별",0,rows.size(),hud,rows);
    }
    private static Button button(ScreenDecision screen,ProtocolDecisionPayloads.Row row) {
        return (Button)screen.children().stream().filter(child->child instanceof Button button && button.getMessage().getString().equals(ScreenDecision.rowName(row))).findFirst().orElseThrow();
    }
    private static int[] point(Button button) { return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2}; }
    private static MouseButtonEvent click(int[] point,int button) { return new MouseButtonEvent(point[0],point[1],new MouseButtonInfo(button,0)); }
}
