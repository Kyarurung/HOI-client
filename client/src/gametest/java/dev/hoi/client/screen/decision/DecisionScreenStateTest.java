package dev.hoi.client.screen.decision;

import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.decision.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.map.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class DecisionScreenStateTest {
    private DecisionScreenStateTest() {}
    public static void run(ClientGameTestContext context,CountryHud hud) {
        int scale=context.computeOnClient(client->client.options.guiScale().get());
        String session=UUID.randomUUID().toString();
        var sent=new ArrayList<DecisionPayloads.Request>();
        var row=new DecisionPayloads.Row("state@s:1041","category","합성 입력 검증","","주 결정","","",30.0,0,"",true,"30","","READY",null,
                new DecisionPayloads.Target("state","1041","수도권",""),List.of(1041),false,"",null);
        var view=new DecisionPayloads.View("KOR",session,1,"합성 입력: 서버 캠페인 장면과 구별",0,1,hud,List.of(row));
        context.runOnClient(client->detailLabels(hud));
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                sent.clear();context.getInput().resizeWindow(size[0],size[1]);
                context.runOnClient(client->client.options.guiScale().set(2));
                context.setScreen(()->new DecisionScreen(view,sent::add));context.waitTicks(3);
                context.runOnClient(client->{
                    var screen=(DecisionScreen)client.gui.screen();
                    var track=button(screen,"추적: "+DecisionScreen.rowName(row));
                    screen.mouseClicked(click(track,InputConstants.MOUSE_BUTTON_RIGHT),false);
                    if(!screen.preferences.tracking("fixture","KOR",row.id()))throw new AssertionError("Right click must not toggle tracking");
                    screen.mouseClicked(click(track,InputConstants.MOUSE_BUTTON_LEFT),false);
                    if(screen.preferences.tracking("fixture","KOR",row.id()) || !sent.isEmpty())throw new AssertionError("Tracking must be local and must not execute TAKE");
                    screen.mouseClicked(click(button(screen,"분류 추적: 합성 입력 검증"),InputConstants.MOUSE_BUTTON_LEFT),false);
                    if(!screen.preferences.tracking("fixture","KOR",row.id()))throw new AssertionError("Category tracking must enable its decisions");
                    screen.mouseClicked(click(button(screen,"사건 팝업"),InputConstants.MOUSE_BUTTON_LEFT),false);
                    if(screen.preferences.popup("events") || !screen.preferences.popup("news"))throw new AssertionError("Popup switches must remain independent");
                });
                context.takeScreenshot("hoi-decisions-settings-synthetic-"+size[0]+"-gui2");
                int[] hover=context.computeOnClient(client->{var b=button((DecisionScreen)client.gui.screen(),DecisionScreen.rowName(row));return new int[]{b.getX()+b.getWidth()/2,b.getY()+b.getHeight()/2};});
                context.getInput().setCursorPos(hover[0]*2,hover[1]*2);context.waitTicks(2);
                var geometry=new MapAtlasScene.Page[1];
                context.runOnClient(client->{
                    var screen=(DecisionScreen)client.gui.screen();
                    for(int i=0;i<12;i++)screen.mapTick();
                    if(sent.size()!=1 || sent.getFirst().action()!=DecisionPayloads.Action.HIGHLIGHT || !sent.getFirst().id().equals(row.id())
                            || !sent.getFirst().session().equals(session))throw new AssertionError("Hover must send one authenticated highlight request per revision");
                    var page=new MapAtlasScene.Page(UUID.randomUUID(),screen.mapDimension(),0,1,List.of(new MapAtlasScene.Box((float)client.player.getX()-8,(float)client.player.getY()+.02f,(float)client.player.getZ()-8,16,.01f,16,0,MapAtlasScene.Material.DEPLOYMENT)));
                    geometry[0]=page;
                    screen.update(new DecisionPayloads.Highlight(UUID.randomUUID().toString(),session,1,row.id(),page));
                    if(overlayTiles()!=0)throw new AssertionError("A foreign token must not draw private geometry");
                    screen.update(new DecisionPayloads.Highlight(screen.token(),session,1,row.id(),page));
                    if(overlayTiles()==0)throw new AssertionError("Validated private geometry must publish to the map renderer");
                });
                context.waitTicks(3);
                context.takeScreenshot("hoi-decisions-private-map-synthetic-"+size[0]+"-gui2");
                context.runOnClient(client->{
                    var screen=(DecisionScreen)client.gui.screen();var page=geometry[0];
                    if(screen.children().stream().anyMatch(child->child instanceof Button button&&button.getMessage().getString().startsWith("지도에서 보기:")))throw new AssertionError("Decision rows must not invent a map button");
                    if(sent.size()!=1 || sent.getFirst().action()!=DecisionPayloads.Action.HIGHLIGHT)throw new AssertionError("Hover must preserve authenticated highlighting without GOTO");
                    detailRefresh(screen,session,hud,row);
                    if(overlayTiles()!=0 || !screen.preferences.tracking("fixture","KOR",row.id()) || screen.preferences.popup("events"))throw new AssertionError("Revision changes must clear private geometry and preserve local settings");
                    screen.update(new DecisionPayloads.Highlight(screen.token(),session,1,row.id(),page));
                    if(overlayTiles()!=0)throw new AssertionError("Stale geometry must not reappear");
                    screen.update(new DecisionPayloads.Response(screen.token(),""));
                    if(client.gui.screen()!=null || overlayTiles()!=0)throw new AssertionError("Revocation must clear the screen and geometry");
                });
            }
            popups(context);
        } finally {context.setScreen(()->null);context.runOnClient(client->client.options.guiScale().set(scale));context.getInput().resizeWindow(1600,1000);context.waitTicks(2);}
    }
    @SuppressWarnings("unchecked")
    private static void detailLabels(CountryHud hud) {
        try {
            for(String kind:List.of("","GOOD","BAD","SELECTABLE"))for(boolean ready:List.of(false,true))for(boolean met:List.of(false,true))for(long days:List.of(0L,7L)) {
                String session=UUID.randomUUID().toString();
                var row=new DecisionPayloads.Row("mission","category","category","","mission","","",30.0,ready?0:72,"",ready,"","",ready?"READY":"ACTIVE",240L,null,List.of(),met,ready?"":"GOOD",null);
                var screen=new DecisionScreen(new DecisionPayloads.View("KOR",session,1,"",0,1,hud,List.of(row)),request->{});
                var line=new DecisionDetailPayloads.Line("원문 효과",0,DecisionDetailPayloads.Assessment.MET,"source",true);
                var sections=List.of(new DecisionDetailPayloads.Section(DecisionDetailPayloads.Phase.READY_REQUIREMENTS,List.of(line)),new DecisionDetailPayloads.Section(DecisionDetailPayloads.Phase.COMPLETE,List.of(line)),new DecisionDetailPayloads.Section(DecisionDetailPayloads.Phase.MODIFIER,List.of(line),days),new DecisionDetailPayloads.Section(DecisionDetailPayloads.Phase.CANCEL,List.of(line)),new DecisionDetailPayloads.Section(DecisionDetailPayloads.Phase.TIMEOUT,List.of(line)));
                var detail=new DecisionDetailPayloads.View("KOR",session,1,row.id(),row.name(),"","fixture",1,sections,"",true,kind,kind.isEmpty()?0:days);
                var field=DecisionScreen.class.getDeclaredField("details");field.setAccessible(true);((Map<String,DecisionDetailPayloads.View>)field.get(screen)).put(row.id(),detail);
                var hitType=Class.forName("dev.hoi.client.screen.decision.DecisionScreen$DecisionHit");var ctor=hitType.getDeclaredConstructor(String.class,String.class,int.class,int.class);ctor.setAccessible(true);
                var method=DecisionScreen.class.getDeclaredMethod("detailText",hitType);method.setAccessible(true);
                String text=(String)method.invoke(screen,ctor.newInstance(row.id(),"",0,20));
                String header=switch(kind) {case "BAD"->"완료 조건:";case "GOOD"->"실패 조건:";default->"선행 조건:";};
                if(!text.contains(header)||!text.contains("중단 시:")||text.contains("취소 시 효과:")||text.contains("기한 만료 시 효과:"))throw new AssertionError("Decision detail must use original headers: "+text);
                String complete=switch(kind) {case "BAD"->"완료 시 효과:";case "GOOD"->"실패 시 효과:";default->"선택 시 효과:";};
                if(!text.contains(complete))throw new AssertionError("Completion header must follow authoritative mission kind: "+text);
                String modifier=kind.isEmpty()?(days>0?"7일 동안:":"활성화된 동안:"):(kind.equals("SELECTABLE")?"선택 시":"완료 시")+(days>0?" 7일 동안:":":");
                if(!text.contains(modifier))throw new AssertionError("Modifier header must follow mission kind and section days: "+text);
                if(!kind.isEmpty()&&days>0) {
                    String timeout=switch(kind) {case "BAD"->"7일 내로 미완료 시 효과:";case "GOOD"->"7일 후 효과:";default->"7일 내로 미선택 시 효과:";};
                    if(!text.contains(timeout))throw new AssertionError("Timeout must use missionDays instead of row duration: "+text);
                } else if(text.contains("일 내로")||text.contains("일 후 효과:"))throw new AssertionError("Ordinary decisions and unknown mission duration must not invent a timeout: "+text);
                if(kind.isEmpty()&&(text.contains("완료 조건:")||text.contains("실패 조건:")||text.contains("완료 시 효과:")||text.contains("실패 시 효과:")))throw new AssertionError("Ordinary ACTIVE decisions must not use mission headers: "+text);
                if(ready) {
                    String cost="결정 선택에 £pol_power §"+(met?"H":"R")+"30§! 소모";
                    if(!text.endsWith(cost))throw new AssertionError("Source cost must be last and preserve insufficient color: "+text);
                } else if(text.contains("결정 선택에"))throw new AssertionError("Active decisions must not repeat selection cost: "+text);
            }
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    @SuppressWarnings("unchecked")
    private static void detailRefresh(DecisionScreen screen,String session,CountryHud hud,DecisionPayloads.Row row) {
        try {
            var requestsField=DecisionScreen.class.getDeclaredField("detailRequests");requestsField.setAccessible(true);
            var requests=(Map<String,Integer>)requestsField.get(screen);
            var detailsField=DecisionScreen.class.getDeclaredField("details");detailsField.setAccessible(true);
            var details=(Map<String,DecisionDetailPayloads.View>)detailsField.get(screen);
            var old=new DecisionDetailPayloads.View("KOR",session,1,row.id(),"합성 상세","이전 상세","fixture",1,List.of(),"",true);
            requests.put(row.id(),0);screen.update(DecisionDetailPayloads.Response.of(screen.token(),old));
            if(!old.equals(details.get(row.id())))throw new AssertionError("Requested authoritative detail must be cached");
            screen.update(DecisionPayloads.Response.of(screen.token(),new DecisionPayloads.View("KOR",session,2,"",0,1,hud,List.of(row))));
            if(!old.equals(details.get(row.id())) || !requests.isEmpty())throw new AssertionError("A same-session refresh must retain details while allowing a new request");
            var fresh=new DecisionDetailPayloads.View("KOR",session,2,row.id(),"합성 상세","새 상세","fixture",1,List.of(),"",true);
            screen.update(DecisionDetailPayloads.Response.of(screen.token(),fresh));
            if(!old.equals(details.get(row.id())))throw new AssertionError("An unsolicited detail must not replace the cache");
            requests.put(row.id(),0);
            for(var invalid:List.of(old,new DecisionDetailPayloads.View("PRC",session,2,row.id(),"합성","","fixture",1,List.of(),"",true),
                    new DecisionDetailPayloads.View("KOR",UUID.randomUUID().toString(),2,row.id(),"합성","","fixture",1,List.of(),"",true))) {
                screen.update(DecisionDetailPayloads.Response.of(screen.token(),invalid));
                if(!old.equals(details.get(row.id())))throw new AssertionError("Stale or foreign details must not replace the cache");
            }
            screen.update(DecisionDetailPayloads.Response.of(UUID.randomUUID().toString(),fresh));
            if(!old.equals(details.get(row.id())))throw new AssertionError("A foreign token must not replace details");
            screen.update(DecisionDetailPayloads.Response.of(screen.token(),fresh));
            if(!fresh.equals(details.get(row.id())))throw new AssertionError("A requested current detail must replace retained data");
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private static int overlayTiles() {
        try {
            var field=dev.hoi.client.map.scene.SceneRenderer.class.getDeclaredField("decision");field.setAccessible(true);
            Object mesh=field.get(null);if(mesh==null)return 0;
            var tiles=mesh.getClass().getDeclaredMethod("tiles");tiles.setAccessible(true);return ((List<?>)tiles.invoke(mesh)).size();
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private static void popups(ClientGameTestContext context) {
        context.runOnClient(client->{
            try {
                var field=DecisionPreferences.class.getDeclaredField("current");field.setAccessible(true);Object old=field.get(null);
                try {
                    var preferences=new DecisionPreferences(null);preferences.popup("events",false);preferences.popup("news",false);field.set(null,preferences);
                    DialogCoordinator.reset();
                    for(var kind:List.of(DialogView.Kind.EVENT,DialogView.Kind.GLOBAL_EVENT)) {
                        var event=new DialogView(UUID.randomUUID().toString(),1,kind,"합성 사건 목록 검증","","내용","","","",List.of(),List.of(new DialogView.Choice("ok","확인")));
                        DialogCoordinator.receive(DialogPayloads.Show.of(event,true));
                        var stack=DialogCoordinator.eventStack();
                        if(stack==null || stack.hasVisibleWindows() || stack.events().stream().noneMatch(v->v.token().equals(event.token())) || client.gui.screen()!=null)
                            throw new AssertionError("Disabled popup must remain in the event list with its server token");
                        stack.reopen(event.token());
                        if(client.gui.screen()!=stack || !stack.hasVisibleWindows())throw new AssertionError("An event-list click must reopen the same session");
                        DialogCoordinator.close(event.token());
                        if(DialogCoordinator.eventStack()!=null || client.gui.screen()!=null)throw new AssertionError("Server Close must remove a suppressed or reopened event");
                    }
                    var superEvent=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.SUPER_EVENT,"합성","","","","","",List.of(),List.of());
                    if(!preferences.opens(superEvent))throw new AssertionError("Popup settings must not suppress super events");
                } finally {DialogCoordinator.reset();field.set(null,old);}
            } catch(ReflectiveOperationException | java.io.IOException error) {throw new AssertionError(error);}
        });
    }
    private static Button button(DecisionScreen screen,String name) {return (Button)screen.children().stream().filter(child->child instanceof Button button && button.getMessage().getString().equals(name)).findFirst().orElseThrow(()->new AssertionError("Missing control: "+name));}
    private static MouseButtonEvent click(Button button,int kind) {return new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(kind,0));}
}
