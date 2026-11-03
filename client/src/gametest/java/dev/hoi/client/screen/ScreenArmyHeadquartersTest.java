package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.input.KeyEvent;

public final class ScreenArmyHeadquartersTest {
    private ScreenArmyHeadquartersTest() {}
    private static void checkTooltip(ScreenArmyHeadquarters screen,String expected){
        try{
            var field=ScreenArmyHeadquarters.class.getDeclaredField("tooltipText");field.setAccessible(true);
            if(expected.isEmpty()||!expected.equals(field.get(screen)))throw new AssertionError("HQ must render the server tooltip verbatim without client additions");
        }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    public static void run(ClientGameTestContext context,ProtocolMenuView menu)throws java.io.IOException{
        String directory=System.getenv("HOI_HEADQUARTERS_PREVIEW");if(directory==null)throw new AssertionError("Authoritative HQ fixtures are required");
        var officerMenu=new com.google.gson.Gson().fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-selected-menu.json")),ProtocolMenuView.class);
        if(officerMenu.page(ProtocolMenuTab.OFFICER_CORPS).sections().stream().noneMatch(s->s.icon().equals("army_stats")&&!s.entries().isEmpty()))throw new AssertionError("HQ backdrop fixture must contain authoritative officer stats");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            for(String stage:List.of("list","draft","saved","preview","conflict")){
                var view=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"headquarters-"+stage+".json"))).view();var requests=new ArrayList<ProtocolIndustryPayloads.Request>();
                context.setScreen(()->new ScreenMenu(officerMenu,ProtocolMenuTab.OFFICER_CORPS,()->{}));context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                var parentPixels=view.headquarters().draft()==null?javax.imageio.ImageIO.read(context.takeScreenshot("hoi-hq-parent-before-"+stage+"-"+size[0]+"-gui2").toFile()):null;
                context.runOnClient(client->{var parent=client.gui.screen();parent.resize(parent.width+20,parent.height+20);client.gui.setScreen(new ScreenArmyHeadquarters(parent,view.session(),view,requests::add));});context.waitTicks(3);
                context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();for(var child:screen.children())if(child instanceof AbstractWidget widget)if(widget.getX()<0||widget.getY()<0||widget.getX()+widget.getWidth()>screen.width||widget.getY()+widget.getHeight()>screen.height)throw new AssertionError("HQ widget escaped GUI2 bounds: "+stage);if(!requests.isEmpty())throw new AssertionError("Rendering must not mutate HQ state");if(view.headquarters().draft()!=null&&screen.children().stream().filter(c->c instanceof dev.hoi.client.ui.UiHoverRegion r&&r.getMessage().getString().startsWith("HQ 지역 변동치")).count()!=11)throw new AssertionError("HQ requires eleven source terrain icons");if(view.headquarters().draft()!=null&&(view.headquarters().details().stats().size()!=28||screen.children().stream().filter(dev.hoi.client.ui.UiHoverRegion.class::isInstance).count()!=49))throw new AssertionError("HQ requires 27 table stats, four summary stats, army experience, four cost fields, two capability fields and eleven terrain tooltips");});
                if(view.headquarters().draft()!=null)context.runOnClient(client->{
                    var screen=(ScreenArmyHeadquarters)client.gui.screen();
                    if(screen.children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("닫기")))throw new AssertionError("HQ footer must contain only the source save action");
                    button(screen,"HQ 설계 닫기");button(screen,"새 설계");
                    var slots=screen.children().stream().filter(dev.hoi.client.ui.UiPanelButton.class::isInstance).map(dev.hoi.client.ui.UiPanelButton.class::cast).filter(b->!b.getMessage().getString().equals("저장")).toList();
                    if(slots.size()!=8)throw new AssertionError("HQ must retain four battalion and four support slots");
                    double slotCenter=slots.get(0).getX()+slots.get(0).getWidth()/2d;
                    if(Math.abs(slotCenter-slots.get(2).getX()-slots.get(2).getWidth()/2d)>1)throw new AssertionError("HQ lead battalion must align with the middle battalion");
                    double supportCenter=(slots.get(4).getX()+slots.get(7).getX()+slots.get(7).getWidth())/2d;
                    if(Math.abs(slotCenter+3-supportCenter)>1)throw new AssertionError("HQ visible battalion backgrounds and support row must share their center");
                    for(String field:List.of("HQ 배치 일수","HQ 배치 지휘력","HQ 생산 자원","HQ 예상 생산 비용"))if(screen.children().stream().noneMatch(c->c instanceof dev.hoi.client.ui.UiHoverRegion r&&r.getMessage().getString().equals(field)))throw new AssertionError("Missing authoritative HQ cost field: "+field);
                    for(String id:List.of("HARDNESS","ORGANIZATION","HP","SPEED"))if(screen.children().stream().noneMatch(c->c instanceof dev.hoi.client.ui.UiHoverRegion r&&r.getMessage().getString().equals("HQ 요약 · "+id)))throw new AssertionError("Missing authoritative HQ summary: "+id);
                    var terrain=screen.children().stream().filter(c->c instanceof dev.hoi.client.ui.UiHoverRegion r&&r.getMessage().getString().startsWith("HQ 지역 변동치")).map(dev.hoi.client.ui.UiHoverRegion.class::cast).toList();
                    if(terrain.get(0).getY()!=terrain.get(4).getY()||terrain.get(5).getY()!=terrain.get(9).getY()||terrain.get(10).getY()<=terrain.get(9).getY())throw new AssertionError("HQ terrain must use the shared five/five/one arrangement");
                    double center=(terrain.get(0).getX()+terrain.get(4).getX()+terrain.get(4).getWidth())/2d;
                    if(Math.abs(center-terrain.get(10).getX()-terrain.get(10).getWidth()/2d)>1)throw new AssertionError("HQ final terrain row must remain centered");
                });
                var rendered=context.takeScreenshot("hoi-hq-"+stage+"-"+size[0]+"x"+size[1]+"-gui2");
                if(parentPixels!=null){var after=javax.imageio.ImageIO.read(rendered.toFile());int changed=0;for(int py=140;py<size[1]-20;py++)for(int px=0;px<dev.hoi.client.ui.UiPanelLayout.width(ProtocolMenuTab.OFFICER_CORPS,size[0]/2)*2;px++)if(parentPixels.getRGB(px,py)!=after.getRGB(px,py))changed++;if(changed>0)throw new AssertionError("HQ list changed the visible officer panel: "+changed+" pixels at "+size[0]);}
                if(stage.equals("draft")){
                    context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();var edit=screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();edit.setValue("HQ edited name");screen.onClose();screen.onClose();if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_DISCARD)throw new AssertionError("Dirty close must discard once without a confirmation");});
                    context.waitTicks(2);context.takeScreenshot("hoi-hq-discard-no-confirmation-"+size[0]+"-gui2");
                    requests.clear();context.setScreen(()->new ScreenArmyHeadquarters(null,view.session(),view,requests::add));
                    context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();var edit=screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();edit.setValue("HQ edited name");press(screen,"저장");if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_SAVE||!requests.getFirst().item().equals("HQ edited name"))throw new AssertionError("HQ save must use the current draft name exactly once");screen.onClose();if(requests.size()!=1)throw new AssertionError("Pending save must prevent duplicate actions");});
                }
                if(stage.equals("conflict")){
                    for(int index:List.of(0,1,2,3,4,5,6)){
                        int[] point=new int[2];context.runOnClient(client->{
                            var screen=(ScreenArmyHeadquarters)client.gui.screen();
                            var control=screen.children().stream().filter(dev.hoi.client.ui.UiPanelButton.class::isInstance).map(dev.hoi.client.ui.UiPanelButton.class::cast).filter(b->!b.getMessage().getString().equals("저장")).toList().get(index);
                            point[0]=(control.getX()+control.getWidth()/2)*2;point[1]=(control.getY()+control.getHeight()/2)*2;
                            var slot=view.headquarters().slots().get(index);
                            if(control.active!=slot.reason().isEmpty())throw new AssertionError("HQ slot availability must follow the server reason");
                            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(point[0]/2.0,point[1]/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0)),false);
                            if(!requests.isEmpty())throw new AssertionError("HQ right click must not mutate the draft");
                            if(!control.active)screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(point[0]/2.0,point[1]/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
                            if(!requests.isEmpty())throw new AssertionError("Locked HQ slot must not send requests");
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                        context.takeScreenshot("hoi-hq-slot-"+index+"-hover-"+size[0]+"-gui2");
                    }
                    for(var slot:view.headquarters().slots())if(!slot.current().isEmpty()){
                        int[] point=new int[2];context.runOnClient(client->{
                            var screen=(ScreenArmyHeadquarters)client.gui.screen();
                            var control=screen.children().stream().filter(dev.hoi.client.ui.UiPanelButton.class::isInstance).map(dev.hoi.client.ui.UiPanelButton.class::cast).filter(b->!b.getMessage().getString().equals("저장")).toList().get(view.headquarters().slots().indexOf(slot));
                            point[0]=(control.getX()+control.getWidth()/2)*2;point[1]=(control.getY()+control.getHeight()/2)*2;
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(8);
                        context.runOnClient(client->checkTooltip((ScreenArmyHeadquarters)client.gui.screen(),slot.tooltip()));
                        context.takeScreenshot("hoi-hq-slot-source-"+slot.current()+"-"+size[0]+"-gui2");
                    }
                    context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                    context.runOnClient(client->press((ScreenArmyHeadquarters)client.gui.screen(),view.headquarters().labels().get("hq_support_company")));
                    int[] supportPoint=new int[2],supportBounds=new int[3];context.runOnClient(client->{
                        var control=button((ScreenArmyHeadquarters)client.gui.screen(),view.headquarters().labels().get("hq_air_liaison"));
                        supportPoint[0]=(control.getX()+control.getWidth()/2)*2;supportPoint[1]=(control.getY()+control.getHeight()/2)*2;
                        supportBounds[0]=control.getX()*2;supportBounds[1]=control.getY()*2;supportBounds[2]=control.getWidth()*2;
                    });
                    context.waitTicks(2);
                    var idleCandidate=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-hq-support-candidate-idle-"+size[0]+"-gui2").toFile());
                    context.getInput().setCursorPos(supportPoint[0],supportPoint[1]);context.waitTicks(8);
                    context.runOnClient(client->checkTooltip((ScreenArmyHeadquarters)client.gui.screen(),view.headquarters().slots().stream().filter(slot->slot.support()&&!slot.current().isEmpty()).findFirst().orElseThrow().choices().stream().filter(choice->choice.id().equals("hq_air_liaison")).findFirst().orElseThrow().tooltip()));
                    var hoveredCandidate=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-hq-support-candidate-source-"+size[0]+"-gui2").toFile());
                    for(int px=supportBounds[0];px<supportBounds[0]+supportBounds[2];px++)if(idleCandidate.getRGB(px,supportBounds[1])!=hoveredCandidate.getRGB(px,supportBounds[1]))throw new AssertionError("Hover must preserve the source candidate border at "+size[0]+": "+px);
                    context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                    context.runOnClient(client->press((ScreenArmyHeadquarters)client.gui.screen(),"뒤로"));
                    context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();if(screen.allowsMovement())throw new AssertionError("Designer must block world and army input");String unit=view.headquarters().labels().get(view.headquarters().draft().battalions().getFirst());press(screen,unit);if(!requests.isEmpty())throw new AssertionError("Opening the picker must not mutate the draft");});context.waitTicks(2);
                    context.takeScreenshot("hoi-hq-picker-"+size[0]+"x"+size[1]+"-gui2");
                    context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();press(screen,"보병 대대");if(button(screen,view.headquarters().labels().get(view.headquarters().draft().battalions().getFirst())).active)throw new AssertionError("Current HQ candidate must be marked and inactive");});
                    context.waitTicks(2);context.takeScreenshot("hoi-hq-infantry-picker-"+size[0]+"-gui2");
                    context.runOnClient(client->press((ScreenArmyHeadquarters)client.gui.screen(),"뒤로"));
                    context.runOnClient(client->{
                        var screen=(ScreenArmyHeadquarters)client.gui.screen();
                        var choices=screen.children().stream().filter(dev.hoi.client.ui.UiPanelButton.class::isInstance).map(Button.class::cast).toList();
                        if(choices.size()<2||choices.get(0).getY()!=choices.get(1).getY()||choices.get(0).getX()>=choices.get(1).getX())throw new AssertionError("HQ candidates must use two columns");
                        var stats=screen.children().stream().filter(dev.hoi.client.ui.UiHoverRegion.class::isInstance).map(dev.hoi.client.ui.UiHoverRegion.class::cast).toList();
                        if(stats.size()!=38||choices.stream().anyMatch(b->b.getX()+b.getWidth()>stats.stream().mapToInt(AbstractWidget::getX).min().orElseThrow()))throw new AssertionError("HQ candidate list must preserve the right-side stats and terrain");
                    });
                    context.runOnClient(client->press((ScreenArmyHeadquarters)client.gui.screen(),"기갑 대대"));context.waitTicks(2);context.takeScreenshot("hoi-hq-armor-picker-"+size[0]+"-gui2");
                    int[] hover=new int[2];context.runOnClient(client->{var b=button((ScreenArmyHeadquarters)client.gui.screen(),view.headquarters().labels().get("hq_light_armor"));double scale=client.getWindow().getGuiScale();hover[0]=(int)((b.getX()+b.getWidth()/2.0)*scale);hover[1]=(int)((b.getY()+b.getHeight()/2.0)*scale);});context.getInput().setCursorPos(hover[0],hover[1]);context.waitTicks(8);context.takeScreenshot("hoi-hq-candidate-delta-"+size[0]+"x"+size[1]+"-gui2");context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();checkTooltip(screen,view.headquarters().slots().getFirst().choices().stream().filter(choice->choice.id().equals("hq_light_armor")).findFirst().orElseThrow().tooltip());var tip=screen.detailTooltip();if(tip==null||tip.x()<0||tip.y()<0||tip.x()+tip.width()>screen.width||tip.y()+tip.height()>screen.height)throw new AssertionError("HQ candidate tooltip clipped");if(tip.maxScroll()>0){screen.mouseScrolled(tip.x()+2,tip.y()+2,0,-2);if(!requests.isEmpty())throw new AssertionError("Tooltip scroll must not select candidate");}});context.waitTicks(2);context.runOnClient(client->{var tip=((ScreenArmyHeadquarters)client.gui.screen()).detailTooltip();if(tip.maxScroll()>0&&tip.scroll()==0)throw new AssertionError("HQ tooltip scroll did not advance");});context.takeScreenshot("hoi-hq-candidate-scrolled-"+size[0]+"x"+size[1]+"-gui2");context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                    context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();press(screen,"뒤로");press(screen,"뒤로");if(!requests.isEmpty())throw new AssertionError("Back in picker must only close picker");press(screen,view.headquarters().labels().get(view.headquarters().draft().battalions().getFirst()));screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));if(!requests.isEmpty())throw new AssertionError("Esc in picker must only close picker");press(screen,view.headquarters().labels().get(view.headquarters().draft().battalions().getFirst()));press(screen,"기갑 대대");press(screen,view.headquarters().labels().get("hq_light_armor"));if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_SLOT||!requests.getFirst().item().equals("hq_light_armor")||!requests.getFirst().other().equals("line")||requests.getFirst().amount()!=0)throw new AssertionError("Picker must send the selected HQ slot once");screen.onClose();if(requests.size()!=1)throw new AssertionError("Pending selection must block close");});
                }
                if(stage.equals("saved"))context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();screen.update(ProtocolIndustryView.revoked(view.session(),"권한 만료"));if(client.gui.screen() instanceof ScreenArmyHeadquarters)throw new AssertionError("Revision-zero revocation must close the private HQ screen");});
                if(stage.equals("list"))context.runOnClient(client->{press((ScreenArmyHeadquarters)client.gui.screen(),"새 설계");if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_NEW)throw new AssertionError("New HQ draft must use the existing authoritative industry session");});
                if(stage.equals("preview")){
                    int[] point=new int[2];context.runOnClient(client->{var screen=client.gui.screen();var region=screen.children().stream().filter(c->c instanceof dev.hoi.client.ui.UiHoverRegion r&&r.getMessage().getString().equals("HQ 지역 변동치 · forest")).map(dev.hoi.client.ui.UiHoverRegion.class::cast).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();point[0]=(int)((region.getX()+region.getWidth()/2.0)*scale);point[1]=(int)((region.getY()+region.getHeight()/2.0)*scale);});context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(8);context.takeScreenshot("hoi-hq-terrain-tooltip-"+size[0]+"x"+size[1]+"-gui2");context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
                }
                if(stage.equals("preview"))context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();if(button(screen,"저장").active!=view.headquarters().saveReason().isEmpty())throw new AssertionError("HQ saving must follow the authoritative eligibility reason");screen.onClose();if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_DISCARD)throw new AssertionError("Discard must affect only the transient draft");});
            }
        }
        var draft=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"headquarters-draft.json"))).view();
        var conflictInput=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"headquarters-conflict.json"))).view();
        context.getInput().setCursorPos(1278,718);context.waitTicks(2);
        for(int[] command:List.of(new int[]{0,com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT},new int[]{0,com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL},new int[]{4,com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT})){
            var requests=new ArrayList<ProtocolIndustryPayloads.Request>();context.setScreen(()->new ScreenArmyHeadquarters(null,conflictInput.session(),conflictInput,requests::add));context.getInput().setCursorPos(2,2);context.waitTicks(2);
            context.runOnClient(client->{
                var screen=(ScreenArmyHeadquarters)client.gui.screen();var slot=conflictInput.headquarters().slots().get(command[0]);
                var control=button(screen,conflictInput.headquarters().labels().get(slot.current()));
                var right=new net.minecraft.client.input.MouseButtonEvent(control.getX()+1,control.getY()+1,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,command[1]));screen.mouseClicked(right,false);screen.mouseReleased(right);
                if(!requests.isEmpty())throw new AssertionError("HQ modifier right clicks must remain inert");
                var click=new net.minecraft.client.input.MouseButtonEvent(control.getX()+1,control.getY()+1,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,command[1]));screen.mouseClicked(click,false);screen.mouseReleased(click);screen.mouseClicked(click,false);
                String target=slot.support()?"support":command[1]==com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL?"line_all":"line";
                if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_SLOT||!requests.getFirst().item().isEmpty()||!requests.getFirst().other().equals(target)||requests.getFirst().amount()!=slot.index()||requests.getFirst().revision()!=conflictInput.revision()||!requests.getFirst().session().equals(conflictInput.session()))throw new AssertionError("HQ modifier removal must preserve the exact authenticated contract once: command="+Arrays.toString(command)+", slot="+slot.index()+", requests="+requests+", event="+click+", control="+control.getX()+","+control.getY()+","+control.active+", tooltip="+screen.detailTooltip());
            });
        }
        for(boolean escape:List.of(false,true)){
            var requests=new ArrayList<ProtocolIndustryPayloads.Request>();context.setScreen(()->new ScreenArmyHeadquarters(null,draft.session(),draft,requests::add));context.getInput().setCursorPos(2,2);context.waitTicks(2);
            context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();if(escape)screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));else press(screen,"HQ 설계 닫기");if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_DISCARD)throw new AssertionError("Source X and Esc must share HQ_DISCARD");});
        }
        for(boolean remove:List.of(false,true)){
            var requests=new ArrayList<ProtocolIndustryPayloads.Request>();context.setScreen(()->new ScreenArmyHeadquarters(null,draft.session(),draft,requests::add));context.getInput().setCursorPos(2,2);context.waitTicks(2);
            context.runOnClient(client->{
                var screen=(ScreenArmyHeadquarters)client.gui.screen();
                var slots=screen.children().stream().filter(dev.hoi.client.ui.UiPanelButton.class::isInstance).map(dev.hoi.client.ui.UiPanelButton.class::cast).filter(b->!b.getMessage().getString().equals("저장")).toList();
                var control=slots.get(remove?1:draft.headquarters().draft().battalions().size());
                for(int modifier:List.of(com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT,com.mojang.blaze3d.platform.InputConstants.MOD_CONTROL))screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(control.getX()+1,control.getY()+1,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,modifier)),false);
                if(!requests.isEmpty())throw new AssertionError("Unsupported HQ modifiers must not send requests");
                var event=new net.minecraft.client.input.MouseButtonEvent(control.getX()+control.getWidth()/2.0,control.getY()+control.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                screen.mouseClicked(event,false);screen.mouseReleased(event);screen.mouseClicked(event,false);
                if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_SLOT||requests.getFirst().amount()!=(remove?1:draft.headquarters().draft().battalions().size())||!requests.getFirst().other().equals("line")||!requests.getFirst().item().equals(remove?"":draft.headquarters().draft().battalions().getFirst()))throw new AssertionError("HQ lower slots must add/remove directly exactly once");
            });
        }
        var list=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"headquarters-list.json"))).view();
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            for(int priority=0;priority<3;priority++){
                int value=priority;var requests=new ArrayList<ProtocolIndustryPayloads.Request>();
                context.setScreen(()->new ScreenArmyHeadquarters(null,draft.session(),draft,requests::add));context.getInput().setCursorPos(2,2);context.waitTicks(2);
                context.runOnClient(client->{
                    var screen=(ScreenArmyHeadquarters)client.gui.screen();var control=button(screen,"HQ 우선순위 · "+value);press(screen,control.getMessage().getString());
                    screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(control.getX()+1,control.getY()+1,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
                    if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_PRIORITY||requests.getFirst().amount()!=value||!requests.getFirst().session().equals(draft.session())||requests.getFirst().revision()!=draft.revision())throw new AssertionError("HQ priority must use the authoritative session once");
                    var json=com.google.gson.JsonParser.parseString(ProtocolIndustryPayloads.Response.of(draft).json()).getAsJsonObject();json.addProperty("revision",draft.revision()+1);json.getAsJsonObject("headquarters").getAsJsonObject("draft").addProperty("priority",value);
                    screen.update(new ProtocolIndustryPayloads.Response(json.toString()).view());
                    if(!button(screen,"HQ 우선순위 · "+value).active)throw new AssertionError("HQ priority response must restore input");
                });context.waitTicks(2);context.takeScreenshot("hoi-hq-priority-synthetic-"+value+"-"+size[0]+"-gui2");
            }
            var saved=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"headquarters-saved.json"))).view();var row=saved.headquarters().templates().getFirst();
            for(String action:List.of("새 설계","편집")){
                var controls=new ArrayList<ProtocolIndustryPayloads.Request>();context.setScreen(()->new ScreenArmyHeadquarters(null,saved.session(),saved,controls::add));context.waitTicks(2);
                context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();press(screen,action);var expected=action.equals("새 설계")?ProtocolIndustryPayloads.Action.HQ_NEW:ProtocolIndustryPayloads.Action.HQ_EDIT;if(controls.size()!=1||controls.getFirst().action()!=expected||!controls.getFirst().item().equals(action.equals("새 설계")?"":row.id())||controls.getFirst().revision()!=saved.revision())throw new AssertionError("Officer HQ controls must target authoritative templates");});
            }
            var requests=new ArrayList<ProtocolIndustryPayloads.Request>();context.setScreen(()->new ScreenArmyHeadquarters(null,saved.session(),saved,requests::add));context.waitTicks(2);
            context.runOnClient(client->{
                var screen=(ScreenArmyHeadquarters)client.gui.screen();press(screen,"HQ 편제 삭제 · "+row.name());
                if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_DELETE||!requests.getFirst().item().equals(row.id())||requests.getFirst().revision()!=saved.revision())throw new AssertionError("HQ list delete must target exactly one authoritative template");
                screen.update(saved);if(button(screen,"HQ 편제 삭제 · "+row.name()).active)throw new AssertionError("Stale response must not acknowledge deletion");
                var json=com.google.gson.JsonParser.parseString(ProtocolIndustryPayloads.Response.of(saved).json()).getAsJsonObject();json.addProperty("revision",saved.revision()+1);json.getAsJsonObject("headquarters").add("templates",new com.google.gson.JsonArray());screen.update(new ProtocolIndustryPayloads.Response(json.toString()).view());
                if(screen.children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().startsWith("HQ 편제 삭제")))throw new AssertionError("Acknowledged HQ deletion must remove the matching list row");
            });context.waitTicks(2);context.takeScreenshot("hoi-hq-delete-response-synthetic-"+size[0]+"-gui2");
            var scrollingJson=com.google.gson.JsonParser.parseString(ProtocolIndustryPayloads.Response.of(saved).json()).getAsJsonObject();var scrollingRows=new com.google.gson.JsonArray();
            for(int i=0;i<20;i++){var copy=com.google.gson.JsonParser.parseString(new com.google.gson.Gson().toJson(row)).getAsJsonObject();copy.addProperty("id","scroll-"+i);copy.addProperty("name","HQ scroll "+i);scrollingRows.add(copy);}
            scrollingJson.getAsJsonObject("headquarters").add("templates",scrollingRows);var scrollingView=new ProtocolIndustryPayloads.Response(scrollingJson.toString()).view();requests.clear();context.setScreen(()->new ScreenArmyHeadquarters(null,scrollingView.session(),scrollingView,requests::add));context.waitTicks(2);
            context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();if(screen.children().stream().anyMatch(c->c instanceof Button b&&b.getMessage().getString().equals("목록 갱신")))throw new AssertionError("HQ source panel has no refresh button");screen.mouseScrolled(20,100,0,-1000);button(screen,"HQ 편제 삭제 · HQ scroll 19");if(!requests.isEmpty())throw new AssertionError("HQ list scrolling must not send actions");});context.waitTicks(2);context.takeScreenshot("hoi-hq-list-scroll-synthetic-"+size[0]+"-gui2");
            context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();int top=dev.hoi.client.ui.UiMenuBar.height(screen.width)+54,bottom=Math.min(screen.height-4,top-52+Math.max(100,Math.round(screen.height*.7f)));var click=new net.minecraft.client.input.MouseButtonEvent(184,(top+bottom)/2d,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);var drag=new net.minecraft.client.input.MouseButtonEvent(184,top,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseDragged(drag,0,top-(top+bottom)/2d);screen.mouseReleased(drag);button(screen,"HQ 편제 삭제 · HQ scroll 0");if(!requests.isEmpty())throw new AssertionError("HQ scrollbar drag must not mutate state");});
            var conflict=new ProtocolIndustryPayloads.Response(Files.readString(Path.of(directory,"headquarters-conflict.json"))).view();requests.clear();context.setScreen(()->new ScreenArmyHeadquarters(null,conflict.session(),conflict,requests::add));context.waitTicks(2);
            context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();press(screen,"HQ 편제 목록");if(!requests.isEmpty())throw new AssertionError("Opening HQ template dropdown must not mutate state");});context.waitTicks(2);context.takeScreenshot("hoi-hq-template-dropdown-"+size[0]+"-gui2");
            context.runOnClient(client->{var screen=(ScreenArmyHeadquarters)client.gui.screen();var target=conflict.headquarters().templates().getFirst();press(screen,target.name());if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.HQ_EDIT||!requests.getFirst().item().equals(target.id()))throw new AssertionError("HQ dropdown must replace the open draft through HQ_EDIT");});
        }
        var source=new ProtocolCommanderHud.Snapshot(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-command-panel-army-assigned.json"))).view();
        for(boolean notification:List.of(false,true)){
            var requests=new ArrayList<ProtocolIndustryPayloads.Request>();
            context.runOnClient(client->{
                dev.hoi.client.ClientCampaignHud.accept(ProtocolHudPayloads.State.of(source.country(),officerMenu.hud()));
                dev.hoi.client.map.MapCommanderHud.accept(new ProtocolCommanderHud.View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards()));
                var parent=new ScreenMenu(officerMenu,ProtocolMenuTab.OFFICER_CORPS,()->{});
                client.gui.setScreen(notification?parent:new ScreenArmyHeadquarters(parent,list.session(),list,requests::add));
                if(notification){
                    var event=new ProtocolDialogView(UUID.randomUUID().toString(),1,ProtocolDialogView.Kind.DIPLOMACY,"완료","","","","","",List.of(),List.of(new ProtocolDialogView.Choice("ack","뒤로")),"focus_complete");
                    ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(event,true));
                }
            });context.waitTicks(3);
            context.runOnClient(client->{
                var point=dev.hoi.client.map.MapCommanderHudTest.armyCardCenter();
                double px=point[0],py=point[1];
                boolean handled=client.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(px,py,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
                if(!handled||!(ScreenDialogCoordinator.contentScreen() instanceof ScreenMapCursor))throw new AssertionError("HQ and notification backdrops must switch to the selected army and retain the cursor");
                if(notification){if(!(client.gui.screen() instanceof ScreenDialogStack))throw new AssertionError("Map navigation must retain pending notifications");}
                else if(requests.size()!=1||requests.getFirst().action()!=ProtocolIndustryPayloads.Action.CLOSE||!requests.getFirst().session().equals(list.session())||requests.getFirst().revision()!=list.revision())throw new AssertionError("Leaving HQ for the map must close the authenticated session once");
            });context.waitTicks(2);context.takeScreenshot("hoi-map-navigation-"+(notification?"notification":"hq")+"-gui2");
            context.runOnClient(client->{ScreenDialogCoordinator.reset();dev.hoi.client.map.MapCommanderHud.reset();dev.hoi.client.map.MapUnitHud.reset();});
        }
        context.setScreen(()->null);
    }
    private static Button button(ScreenArmyHeadquarters screen,String label){return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().equals(label)).findFirst().orElseThrow(()->new AssertionError("Missing HQ button "+label));}
    private static void press(ScreenArmyHeadquarters screen,String label){var button=button(screen,label);if(!button.active)throw new AssertionError("HQ button inactive "+label);var event=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));if(!screen.mouseClicked(event,false))throw new AssertionError("HQ click was not consumed: "+label);screen.mouseReleased(event);}
}
