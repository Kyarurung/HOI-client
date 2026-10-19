package dev.hoi.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class ScreenDecisionStateTest {
    private ScreenDecisionStateTest() {}
    public static void run(ClientGameTestContext context,ProtocolCountryHud hud) {
        int scale=context.computeOnClient(client->client.options.guiScale().get());
        String session=UUID.randomUUID().toString();
        var sent=new ArrayList<ProtocolDecisionPayloads.Request>();
        var row=new ProtocolDecisionPayloads.Row("state@s:1041","category","합성 입력 검증","","주 결정","","",30.0,0,"",true,"30","","READY",null,
                new ProtocolDecisionPayloads.Target("state","1041","수도권",""),List.of(1041),false,"",null);
        var view=new ProtocolDecisionPayloads.View("KOR",session,1,"합성 입력: 서버 캠페인 장면과 구별",0,1,hud,List.of(row));
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                sent.clear();context.getInput().resizeWindow(size[0],size[1]);
                context.runOnClient(client->client.options.guiScale().set(2));
                context.setScreen(()->new ScreenDecision(view,sent::add));context.waitTicks(3);
                context.runOnClient(client->{
                    var screen=(ScreenDecision)client.gui.screen();
                    var track=button(screen,"추적: "+ScreenDecision.rowName(row));
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
                int[] hover=context.computeOnClient(client->{var b=button((ScreenDecision)client.gui.screen(),ScreenDecision.rowName(row));return new int[]{b.getX()+b.getWidth()/2,b.getY()+b.getHeight()/2};});
                context.getInput().setCursorPos(hover[0]*2,hover[1]*2);context.waitTicks(2);
                var geometry=new ProtocolAtlasScene.Page[1];
                context.runOnClient(client->{
                    var screen=(ScreenDecision)client.gui.screen();
                    for(int i=0;i<12;i++)screen.mapTick();
                    if(sent.size()!=1 || sent.getFirst().action()!=ProtocolDecisionPayloads.Action.HIGHLIGHT || !sent.getFirst().id().equals(row.id())
                            || !sent.getFirst().session().equals(session))throw new AssertionError("Hover must send one authenticated highlight request per revision");
                    var page=new ProtocolAtlasScene.Page(UUID.randomUUID(),screen.mapDimension(),0,1,List.of(new ProtocolAtlasScene.Box((float)client.player.getX()-8,(float)client.player.getY()+.02f,(float)client.player.getZ()-8,16,.01f,16,0,ProtocolAtlasScene.Material.DEPLOYMENT)));
                    geometry[0]=page;
                    screen.update(new ProtocolDecisionPayloads.Highlight(UUID.randomUUID().toString(),session,1,row.id(),page));
                    if(overlayTiles()!=0)throw new AssertionError("A foreign token must not draw private geometry");
                    screen.update(new ProtocolDecisionPayloads.Highlight(screen.token(),session,1,row.id(),page));
                    if(overlayTiles()==0)throw new AssertionError("Validated private geometry must publish to the map renderer");
                });
                context.waitTicks(3);
                context.takeScreenshot("hoi-decisions-private-map-synthetic-"+size[0]+"-gui2");
                context.runOnClient(client->{
                    var screen=(ScreenDecision)client.gui.screen();var page=geometry[0];
                    var go=button(screen,"지도에서 보기: "+ScreenDecision.rowName(row));
                    screen.mouseClicked(click(go,InputConstants.MOUSE_BUTTON_RIGHT),false);
                    screen.mouseClicked(click(go,InputConstants.MOUSE_BUTTON_LEFT),false);
                    screen.mouseClicked(click(go,InputConstants.MOUSE_BUTTON_LEFT),true);
                    if(sent.size()!=2 || sent.getLast().action()!=ProtocolDecisionPayloads.Action.GOTO)throw new AssertionError("GOTO must be authenticated and rate limited without taking the decision");
                    screen.update(ProtocolDecisionPayloads.Response.of(screen.token(),new ProtocolDecisionPayloads.View("KOR",session,2,"",0,1,hud,List.of(row))));
                    if(overlayTiles()!=0 || !screen.preferences.tracking("fixture","KOR",row.id()) || screen.preferences.popup("events"))throw new AssertionError("Revision changes must clear private geometry and preserve local settings");
                    screen.update(new ProtocolDecisionPayloads.Highlight(screen.token(),session,1,row.id(),page));
                    if(overlayTiles()!=0)throw new AssertionError("Stale geometry must not reappear");
                    screen.update(new ProtocolDecisionPayloads.Response(screen.token(),""));
                    if(client.gui.screen()!=null || overlayTiles()!=0)throw new AssertionError("Revocation must clear the screen and geometry");
                });
            }
            popups(context);
        } finally {context.setScreen(()->null);context.runOnClient(client->client.options.guiScale().set(scale));context.getInput().resizeWindow(1600,1000);context.waitTicks(2);}
    }
    private static int overlayTiles() {
        try {
            var field=dev.hoi.client.map.MapSceneRenderer.class.getDeclaredField("decision");field.setAccessible(true);
            Object mesh=field.get(null);if(mesh==null)return 0;
            var tiles=mesh.getClass().getDeclaredMethod("tiles");tiles.setAccessible(true);return ((List<?>)tiles.invoke(mesh)).size();
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private static void popups(ClientGameTestContext context) {
        context.runOnClient(client->{
            try {
                var field=ScreenDecisionPreferences.class.getDeclaredField("current");field.setAccessible(true);Object old=field.get(null);
                try {
                    var preferences=new ScreenDecisionPreferences(null);preferences.popup("events",false);preferences.popup("news",false);field.set(null,preferences);
                    ScreenDialogCoordinator.reset();
                    for(var kind:List.of(ProtocolDialogView.Kind.EVENT,ProtocolDialogView.Kind.GLOBAL_EVENT)) {
                        var event=new ProtocolDialogView(UUID.randomUUID().toString(),1,kind,"합성 사건 목록 검증","","내용","","","",List.of(),List.of(new ProtocolDialogView.Choice("ok","확인")));
                        ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(event,true));
                        var stack=ScreenDialogCoordinator.eventStack();
                        if(stack==null || stack.hasVisibleWindows() || stack.events().stream().noneMatch(v->v.token().equals(event.token())) || client.gui.screen()!=null)
                            throw new AssertionError("Disabled popup must remain in the event list with its server token");
                        stack.reopen(event.token());
                        if(client.gui.screen()!=stack || !stack.hasVisibleWindows())throw new AssertionError("An event-list click must reopen the same session");
                        ScreenDialogCoordinator.close(event.token());
                        if(ScreenDialogCoordinator.eventStack()!=null || client.gui.screen()!=null)throw new AssertionError("Server Close must remove a suppressed or reopened event");
                    }
                    var superEvent=new ProtocolDialogView(UUID.randomUUID().toString(),1,ProtocolDialogView.Kind.SUPER_EVENT,"합성","","","","","",List.of(),List.of());
                    if(!preferences.opens(superEvent))throw new AssertionError("Popup settings must not suppress super events");
                } finally {ScreenDialogCoordinator.reset();field.set(null,old);}
            } catch(ReflectiveOperationException | java.io.IOException error) {throw new AssertionError(error);}
        });
    }
    private static Button button(ScreenDecision screen,String name) {return (Button)screen.children().stream().filter(child->child instanceof Button button && button.getMessage().getString().equals(name)).findFirst().orElseThrow(()->new AssertionError("Missing control: "+name));}
    private static MouseButtonEvent click(Button button,int kind) {return new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(kind,0));}
}
