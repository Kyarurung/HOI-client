package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;

public final class ScreenCommanderCaptivityTest {
    private ScreenCommanderCaptivityTest(){}
    public static void run(ClientGameTestContext context,ProtocolMenuView menu)throws java.io.IOException{
        String directory=System.getenv("HOI_CAPTIVITY_PREVIEW");if(directory==null)throw new AssertionError("Authoritative captivity fixtures are required");
        var officerMenu=new com.google.gson.Gson().fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-selected-menu.json")),ProtocolMenuView.class);
        if(officerMenu.page(ProtocolMenuTab.OFFICER_CORPS).sections().stream().noneMatch(s->s.icon().equals("army_stats")&&!s.entries().isEmpty()))throw new AssertionError("Captivity backdrop requires actual officer statistics");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            for(String stage:List.of("empty","ours","search","no-results","theirs","failure")){
                var view=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"captivity-"+stage+".json"))).view();var requests=new ArrayList<ProtocolIndustryPayloads.Request>();
                context.runOnClient(client -> {
                    for (var row : view.captivity().rows()) if (row.portrait().isBlank() || dev.hoi.client.ui.UiAssets.imageHeight(row.portrait(), 100) <= 0)
                        throw new AssertionError("Issued captured commander portrait is unavailable: " + row.id());
                });
                context.setScreen(()->new ScreenMenu(officerMenu,ProtocolMenuTab.OFFICER_CORPS,()->{}));context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                var before=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-captivity-parent-before-"+stage+"-"+size[0]+"-gui2").toFile());
                context.runOnClient(client->client.gui.setScreen(new ScreenCommanderCaptivity(client.gui.screen(),view.session(),view.captivity().ours(),view,requests::add)));context.waitTicks(3);
                context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();for(var child:screen.children())if(child instanceof AbstractWidget widget)if(widget.getX()<0||widget.getY()<0||widget.getX()+widget.getWidth()>screen.width||widget.getY()+widget.getHeight()>screen.height)throw new AssertionError("Captivity widget escaped GUI2 bounds: "+stage);if(!requests.isEmpty()||screen.allowsMovement())throw new AssertionError("Rendering must be read-only and block world input");});
                context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                var after=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-captivity-"+stage+"-"+size[0]+"x"+size[1]+"-gui2").toFile());
                int pane=dev.hoi.client.ui.UiPanelLayout.width(ProtocolMenuTab.OFFICER_CORPS,size[0]/2)*2;
                for(int y=dev.hoi.client.ui.UiMenuBar.height(size[0]/2)*2;y<size[1];y++)for(int x=0;x<pane;x++)if(before.getRGB(x,y)!=after.getRGB(x,y))throw new AssertionError("Opening captivity changed officer backdrop at "+x+","+y);
                if(stage.equals("ours")){
                    int[] cursor=new int[2];context.runOnClient(client->{var screen=client.gui.screen();var tab=button(screen,"우리의 실종된 지휘관");double scale=client.getWindow().getGuiScale();cursor[0]=(int)((tab.getX()+90)*scale);cursor[1]=(int)((tab.getY()+30)*scale);});context.getInput().setCursorPos(cursor[0],cursor[1]);context.waitTicks(3);
                    context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();var tip=screen.detailTooltip();if(tip==null||tip.x()-4<0||tip.y()-4<0||tip.x()+tip.width()+4>screen.width||tip.y()+tip.height()+4>screen.height)throw new AssertionError("Captive tooltip must remain inside GUI2 bounds");screen.mouseScrolled(tip.x()+tip.width()/2.0,tip.y()+tip.height()/2.0,0,-10000);});context.waitTicks(2);context.takeScreenshot("hoi-captivity-tooltip-"+size[0]+"x"+size[1]+"-gui2");
                    context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);context.runOnClient(client->{if(((ScreenCommanderCaptivity)client.gui.screen()).detailTooltip()!=null)throw new AssertionError("Tooltip must clear after leaving row and tooltip");});
                    context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();if(screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).anyMatch(b->List.of("새로고침","<",">").contains(b.getMessage().getString())))throw new AssertionError("Source captivity list has no footer controls");var tab=button(screen,"우리의 실종된 지휘관");screen.mouseScrolled(tab.getX()+90,tab.getY()+30,0,-1);if(requests.size()!=1||requests.getFirst().amount()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.CAPTIVITY_QUERY)throw new AssertionError("Scrolling must query the next server page");screen.mouseScrolled(tab.getX()+90,tab.getY()+30,0,-1);if(requests.size()!=1)throw new AssertionError("Pending page must prevent duplicate requests");});context.waitTicks(2);
                    context.takeScreenshot("hoi-captivity-loading-"+size[0]+"x"+size[1]+"-gui2");
                    context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();screen.failed();var tab=button(screen,"우리의 실종된 지휘관");int x=tab.getX()+260,y=tab.getY()+170;var event=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseDragged(event,0,180);screen.mouseReleased(event);if(requests.size()!=2)throw new AssertionError("Source scrollbar must support retry and suppress repeated drag requests");screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));if(client.gui.screen() instanceof ScreenCommanderCaptivity||requests.getLast().action()!=ProtocolIndustryPayloads.Action.CLOSE)throw new AssertionError("Esc must close a pending query");screen.update(view);if(client.gui.screen() instanceof ScreenCommanderCaptivity)throw new AssertionError("Late response reopened a closed screen");});
                }
                if(stage.equals("empty"))context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();press(screen,"우리에게 포획된 지휘관",InputConstants.MOUSE_BUTTON_LEFT);if(requests.size()!=1||!requests.getFirst().other().startsWith("theirs/"))throw new AssertionError("Enemy tab query mismatch");});
                if(stage.equals("theirs"))context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();press(screen,"우리의 실종된 지휘관",InputConstants.MOUSE_BUTTON_LEFT);if(requests.size()!=1||!requests.getFirst().other().startsWith("ours/"))throw new AssertionError("Missing tab query mismatch");});
                if(stage.equals("search"))context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();var edit=screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();screen.setFocused(edit);edit.setValue("new query");for(int i=0;i<8;i++)screen.tick();if(requests.size()!=1||!requests.getFirst().item().equals("new query")||requests.getFirst().amount()!=0)throw new AssertionError("Search must query all rows at page zero");edit=screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();if(!edit.isFocused())throw new AssertionError("Searching must preserve keyboard focus");for(int key:List.of(InputConstants.KEY_RETURN,InputConstants.KEY_NUMPADENTER)){screen.failed();screen.keyPressed(new KeyEvent(key,0,0));if(requests.getLast().action()!=ProtocolIndustryPayloads.Action.CAPTIVITY_QUERY)throw new AssertionError("Enter must submit search with current Minecraft key constants");}if(requests.size()!=3)throw new AssertionError("Each Enter should send exactly one query");});
                if(stage.equals("no-results"))context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();press(screen,"수용량",InputConstants.MOUSE_BUTTON_LEFT);if(!requests.getFirst().other().contains("/capacity/"))throw new AssertionError("Capacity sort mismatch");});
                if(stage.equals("failure"))context.runOnClient(client->{var screen=(ScreenCommanderCaptivity)client.gui.screen();screen.update(ProtocolIndustryView.revoked(view.session(),"권한 만료"));if(client.gui.screen() instanceof ScreenCommanderCaptivity)throw new AssertionError("Revocation must close the private screen");});
            }
            for(int mouse:List.of(InputConstants.MOUSE_BUTTON_LEFT,InputConstants.MOUSE_BUTTON_RIGHT)){
                context.setScreen(()->new ScreenMenu(menu,ProtocolMenuTab.OFFICER_CORPS,()->{}));context.waitTicks(2);
                context.runOnClient(client->{press(client.gui.screen(),"포획된 지휘관",mouse);if(!(client.gui.screen() instanceof ScreenCommanderCaptivity screen)||screen.ownCommanders()!=(mouse==InputConstants.MOUSE_BUTTON_LEFT))throw new AssertionError("Officer button left/right tab mismatch");});
            }
        }
        context.setScreen(()->null);
    }
    private static Button button(Screen screen,String label){return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new AssertionError("Missing captivity button "+label));}
    private static void press(Screen screen,String label,int mouse){var button=button(screen,label);if(!button.active)throw new AssertionError("Inactive captivity button "+label);var event=new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(mouse,0));if(!screen.mouseClicked(event,false))throw new AssertionError("Captivity click was not consumed: "+label);screen.mouseReleased(event);}
}
