package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.ProtocolFocusPayloads;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

final class ScreenFocusInlaysTest {
    private ScreenFocusInlaysTest() {}
    static void run(ClientGameTestContext context){
        String directory=System.getenv("HOI_FOCUS_PREVIEW");
        if(directory==null||directory.isBlank())return;
        try{
            var source=new Gson().fromJson(Files.readString(Path.of(directory,"../focus/KOR.json")),ProtocolFocusPayloads.View.class);
            var election=new Gson().fromJson(Files.readString(Path.of(directory,"kor-election-2022-view.json")),ProtocolFocusPayloads.View.class);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));context.waitTicks(2);
                var electionInlay=election.inlays().stream().filter(inlay->inlay.texts().size()==2).findFirst().orElseThrow();
                var sourceRequests=new ArrayList<ProtocolFocusPayloads.Request>();context.setScreen(()->new ScreenFocus(election,sourceRequests::add));
                context.runOnClient(client->{
                    var screen=(ScreenFocus)client.gui.screen();var event=new MouseButtonEvent(10,screen.height-10,new MouseButtonInfo(1,0));screen.mouseClicked(event,false);
                    screen.mouseDragged(event,20-screen.panX()-electionInlay.x()*.5,80-screen.panY()-electionInlay.y()*.5);screen.mouseReleased(event);
                    if(size[0]==1280)screen.mouseScrolled(20,80,0,-3);
                    check(sourceRequests.isEmpty(),"Authoritative election inlay presentation must not mutate the campaign");
                });context.getInput().setCursorPos(0,0);context.waitTicks(2);context.takeScreenshot("hoi-focus-inlay-election-source-"+size[0]+"-gui2");
                for(String kind:List.of("president_election","president_election_2027","yoon_father")){
                    int x=kind.equals("president_election")?1600:kind.equals("president_election_2027")?400:100;
                    int y=kind.equals("yoon_father")?440:1550;
                    var images=kind.equals("yoon_father")?List.<ProtocolFocusPayloads.InlayImage>of():List.of(new ProtocolFocusPayloads.InlayImage("center_expect_icon",kind.equals("president_election")?"GFX_lee_certain_president_election":"GFX_cho_prime_president_election_2027"));
                    for(double progress:kind.equals("president_election_2027")?List.of(0d,.3d,1d):List.of(0d)){
                        var bars=kind.equals("president_election_2027")?List.of(new ProtocolFocusPayloads.InlayBar("cho_bar",progress),new ProtocolFocusPayloads.InlayBar("lee_jun_bar",progress)):List.<ProtocolFocusPayloads.InlayBar>of();
                        var texts=kind.equals("president_election")?electionInlay.texts():kind.equals("president_election_2027")?List.of(new ProtocolFocusPayloads.InlayText("cho_2027_support_text",(int)(progress*100)+"%"),new ProtocolFocusPayloads.InlayText("lee_2027_support_text",(int)(progress*100)+"%")):List.<ProtocolFocusPayloads.InlayText>of();
                        var inlay=new ProtocolFocusPayloads.Inlay("KOR_"+kind+"_inlay_window","TFR_KOR_"+kind+"_inner_circle_gui_window",x,y,images,bars,texts);
                        var requests=new ArrayList<ProtocolFocusPayloads.Request>();
                        var view=new ProtocolFocusPayloads.View(source.country(),source.name(),source.tree(),source.session(),source.revision(),"",source.hud(),List.of(),false,null,null,"","",List.of(inlay));
                        context.setScreen(()->new ScreenFocus(view,requests::add));
                        context.runOnClient(client->{
                            var screen=(ScreenFocus)client.gui.screen();
                            check(ScreenFocusInlays.windows().has(inlay.window()),"Every source inlay window must be imported");
                            for(var image:images)check(UiAssets.imageSize("focus/inlay/"+image.sprite().toLowerCase(java.util.Locale.ROOT)+"/frame_0")[0]>0,"Dynamic source artwork must exist");
                            var event=new MouseButtonEvent(10,screen.height-10,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0));
                            screen.mouseClicked(event,false);
                            screen.mouseDragged(event,20-screen.panX()-x*.5,80-screen.panY()-y*.5);
                            screen.mouseReleased(event);
                            if(size[0]==1280)screen.mouseScrolled(20,80,0,kind.equals("yoon_father")?-7:-3);
                            check(requests.isEmpty(),"Panning and zooming inlays must not send gameplay requests");
                        });
                        context.getInput().setCursorPos(0,0);context.waitTicks(2);
                        context.takeScreenshot("hoi-focus-inlay-synthetic-"+kind+"-"+(int)(progress*100)+"-"+size[0]+"-gui2");
                        var textPixels=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-focus-inlay-text-"+kind+"-"+(int)(progress*100)+"-"+size[0]+"-gui2").toFile());
                        context.runOnClient(client->{
                            var screen=(ScreenFocus)client.gui.screen();
                            var noText=new ProtocolFocusPayloads.Inlay(inlay.id(),inlay.window(),inlay.x(),inlay.y(),inlay.images(),inlay.bars(),List.of());
                            screen.update(ProtocolFocusPayloads.Response.of(screen.token(),new ProtocolFocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),view.revision()+1,"",view.hud(),List.of(),false,null,null,"","",List.of(noText))));
                        });context.waitTicks(2);
                        var withoutText=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-focus-inlay-no-text-"+kind+"-"+(int)(progress*100)+"-"+size[0]+"-gui2").toFile());
                        if(!texts.isEmpty()){
                            int changed=0;for(int py=160;py<textPixels.getHeight()-5;py++)for(int px=5;px<textPixels.getWidth()-5;px++)if(textPixels.getRGB(px,py)!=withoutText.getRGB(px,py))changed++;
                            check(changed>10,"Source support glyphs must render and disappear with the authoritative text at "+size[0]);
                        }
                        context.runOnClient(client->{
                            var screen=(ScreenFocus)client.gui.screen();
                            screen.update(ProtocolFocusPayloads.Response.of(screen.token(),new ProtocolFocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),view.revision()+2,"",view.hud(),List.of(),false,null,null,"","",List.of())));
                        });
                        context.waitTicks(2);
                        context.takeScreenshot("hoi-focus-inlay-cleared-"+kind+"-"+(int)(progress*100)+"-"+size[0]+"-gui2");
                    }
                }
                var running=source.nodes().stream().filter(node->node.status().equals("진행 중")).findFirst().orElseThrow();
                var requests=new ArrayList<ProtocolFocusPayloads.Request>();
                var node=new ProtocolFocusPayloads.Node(running.id(),running.name(),running.description(),running.icon(),0,0,List.of(),List.of(),running.status(),running.remaining(),false);
                var view=new ProtocolFocusPayloads.View(source.country(),source.name(),source.tree(),source.session(),source.revision(),"",source.hud(),List.of(node),false,null,null,source.cancelTooltip(),source.startTooltip(),List.of());
                check(!source.cancelable()&&!source.cancelTooltip().isEmpty(),"Starting Korea focus must provide its authoritative disabled cancel tooltip");
                context.setScreen(()->new ScreenFocus(view,requests::add));
                int[] hover=new int[2];
                context.runOnClient(client->{
                    var screen=(ScreenFocus)client.gui.screen();var box=screen.nodeBounds(node.id());
                    var event=new MouseButtonEvent(box[0]+box[2]/2d,box[1]+12,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);
                    var cancel=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button->button.getMessage().getString().equals("중점 취소")).findFirst().orElseThrow();
                    check(!cancel.active,"Noncancelable focus must retain a disabled action");
                    event=new MouseButtonEvent(cancel.getX()+cancel.getWidth()/2d,cancel.getY()+cancel.getHeight()/2d,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                    screen.mouseClicked(event,false);screen.mouseReleased(event);screen.mouseClicked(event,false);screen.mouseReleased(event);
                    check(requests.stream().noneMatch(request->request.action()==ProtocolFocusPayloads.Action.CANCEL),"Repeated disabled cancel clicks must never mutate state");
                    hover[0]=(int)(event.x()*2);hover[1]=(int)(event.y()*2);
                });
                context.getInput().setCursorPos(0,0);context.waitTicks(2);
                var beforeTooltip=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-focus-cancel-disabled-idle-"+size[0]+"-gui2").toFile());
                context.getInput().setCursorPos(hover[0],hover[1]);context.waitTicks(4);
                var afterTooltip=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-focus-cancel-disabled-source-"+size[0]+"-gui2").toFile());
                int newRed=0;
                for(int py=140;py<afterTooltip.getHeight();py++)for(int px=0;px<afterTooltip.getWidth();px++){
                    int color=afterTooltip.getRGB(px,py),red=color>>16&255,green=color>>8&255,blue=color&255;
                    if(red>150&&green<100&&blue<100&&color!=beforeTooltip.getRGB(px,py))newRed++;
                }
                check(newRed>10,"Disabled cancel tooltip must render its source red text at "+size[0]);
            }
        }catch(java.io.IOException failure){throw new AssertionError(failure);}
        context.setScreen(()->null);
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
