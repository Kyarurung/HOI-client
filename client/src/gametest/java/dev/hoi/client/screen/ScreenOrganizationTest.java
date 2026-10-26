package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;

public final class ScreenOrganizationTest {
    private ScreenOrganizationTest() {}
    private static String json(String name)throws java.io.IOException{return Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),name+".json"));}
    private static ProtocolDialogView source(String name)throws java.io.IOException{return new ProtocolDialogPayloads.Show(json(name),true).view().issued(UUID.randomUUID().toString(),7);}
    private static MouseButtonEvent event(AbstractWidget b,int modifiers,int button){return new MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+b.getHeight()/2.,new MouseButtonInfo(button,modifiers));}
    private static void click(Screen screen,AbstractWidget b,int modifiers){if(b==null)throw new AssertionError("Missing control");var e=event(b,modifiers,InputConstants.MOUSE_BUTTON_LEFT);screen.mouseClicked(e,false);screen.mouseReleased(e);}
    private static AbstractWidget named(Screen screen,String name){return screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals(name)).map(w->(AbstractWidget)w).findFirst().orElseThrow(()->new AssertionError(name));}
    public static void run(ClientGameTestContext game){
        int scale=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var menuView=new Gson().fromJson(json("kor-mio-menu"),ProtocolMenuPayloads.OpenScreen.class).view();
            var base=source("kor-mio-detail-lig");var unlocked=source("kor-mio-detail-lig-unlocked");var policies=source("kor-mio-policies-lig");
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                var menu=game.computeOnClient(c->new ScreenMenu(menuView,ProtocolMenuTab.POLITICS,()->{}));game.setScreen(()->menu);
                game.runOnClient(c->{
                    menu.mouseScrolled(30,menu.height-50,0,-40);
                    var entry=menuView.page(ProtocolMenuTab.POLITICS).sections().stream().filter(s->s.icon().equals("research_production")).findFirst().orElseThrow().entries().getFirst();
                    var open=menu.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().startsWith(entry.name()+" · ")).map(w->(AbstractWidget)w).findFirst().orElseThrow();click(menu,open,0);
                    var requests=new ArrayList<ProtocolCommanderHud.Open>();
                    try{var observer=dev.hoi.client.map.MapCommanderHud.class.getDeclaredField("requestObserver");observer.setAccessible(true);observer.set(null,(java.util.function.Consumer<ProtocolCommanderHud.Open>)requests::add);try{var details=menu.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("세부 사항")&&b.getX()>menu.panelWidth()).map(w->(AbstractWidget)w).findFirst().orElseThrow();click(menu,details,0);}finally{observer.set(null,null);}}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                    if(requests.size()!=1||requests.getFirst().mode()!=ProtocolCommanderHud.Mode.ORGANIZATION||menuView.manufacturers().stream().noneMatch(m->m.id().equals(requests.getFirst().target())&&m.groups().contains("armor")))throw new AssertionError("Politics list must open the selected organization through its server mode: "+requests);
                });
                for(String group:List.of("armor","navy","air","materiel","points")){
                    game.runOnClient(c->{try{var field=ScreenMenu.class.getDeclaredField("manufacturerGroup");field.setAccessible(true);field.set(menu,group);menu.init(menu.width,menu.height);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}int expected=(int)menuView.manufacturers().stream().filter(m->group.equals("points")?m.points()>0:m.groups().contains(group)).count();long visible=menu.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("세부 사항")).count();if(visible<Math.min(expected,4))throw new AssertionError("Manufacturer filter lost visible source entries");});
                    game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-mio-list-"+group+"-"+size[0]+"-gui2");
                    if(!group.equals("points")){game.runOnClient(c->{click(menu,named(menu,"이름"),0);click(menu,named(menu,"이름"),0);menu.mouseScrolled(menu.width-20,100,0,-8);menu.mouseScrolled(menu.width-20,100,0,8);});}
                }
                game.runOnClient(c->{try{var f=ScreenMenu.class.getDeclaredField("manufacturerGroup");f.setAccessible(true);f.set(menu,"materiel");var sorted=ScreenMenu.class.getDeclaredField("manufacturerSorted");sorted.setAccessible(true);sorted.set(menu,false);menu.init(menu.width,menu.height);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}});
                for(var view:List.of(base,unlocked,policies)){
                    var sent=new ArrayList<ProtocolDialogPayloads.Action>();var screen=game.computeOnClient(c->new ScreenOrganization(view,menu,(v,id)->sent.add(new ProtocolDialogPayloads.Action(v.token(),v.revision(),id))));game.setScreen(()->screen);game.waitTicks(3);
                    String stage=view==base?"detail":view==unlocked?"unlocked":"policies";game.takeScreenshot("hoi-mio-"+stage+"-"+size[0]+"-gui2");
                    if(view==policies){var first=game.computeOnClient(c->screen.control("attach/"+view.tiles().getFirst().section().substring(7)));game.getInput().setCursorPos((first.getX()+20)*2,(first.getY()+20)*2);game.waitTicks(3);game.takeScreenshot("hoi-mio-policy-tooltip-"+size[0]+"-gui2");game.runOnClient(c->{if(view.choices().stream().anyMatch(ch->ch.id().startsWith("attach/")))throw new AssertionError("Initial policy fixture should be unavailable");screen.mouseScrolled(screen.width/2.,screen.height/2.,0,-5);if(!sent.isEmpty())throw new AssertionError("Scrolling policy choices must not apply one");click(screen,screen.control("back"),0);if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),7,"back"))))throw new AssertionError("Policy back authentication");});continue;}
                    var target=view.tiles().stream().filter(t->t.section().startsWith("trait/")&&view.choices().stream().anyMatch(ch->ch.id().equals("queue/"+t.section().substring(6)))&&t.traitNode().row()==0).findFirst().orElseThrow();
                    var control=game.computeOnClient(c->screen.control(target.section()));game.getInput().setCursorPos((control.getX()+12)*2,(control.getY()+12)*2);game.waitTicks(3);game.takeScreenshot("hoi-mio-trait-tooltip-"+stage+"-"+size[0]+"-gui2");
                    game.runOnClient(c->{var right=event(control,0,InputConstants.MOUSE_BUTTON_RIGHT);screen.mouseClicked(right,false);if(!sent.isEmpty())throw new AssertionError("Right click must not mutate traits");click(screen,control,InputConstants.MOD_SHIFT);click(screen,control,InputConstants.MOD_SHIFT);if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),7,"queue/"+target.section().substring(6)))))throw new AssertionError("Queue must send one authenticated request");screen.update(view);if(screen.control("policies").active)throw new AssertionError("Same revision must preserve pending lock");screen.update(view.issued(view.token(),8));click(screen,screen.control("queue"),0);});
                    game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-mio-queue-"+stage+"-"+size[0]+"-gui2");
                    if(view==unlocked){game.runOnClient(c->{var queued=view.tiles().stream().filter(t->t.section().startsWith("trait/")&&!t.value().isEmpty()).sorted(Comparator.comparingInt(t->Integer.parseInt(t.value()))).toList();var last=queued.getLast();String token=last.section().substring(6);var row=screen.control("queue-row/"+token);var first=screen.control("queue-row/"+queued.getFirst().section().substring(6));sent.clear();screen.mouseClicked(event(row,0,InputConstants.MOUSE_BUTTON_LEFT),false);screen.mouseReleased(event(first,0,InputConstants.MOUSE_BUTTON_LEFT));if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),8,"up/"+token))))throw new AssertionError("Drag must move queue through the current revision");screen.update(view.issued(view.token(),8));if(sent.size()!=1)throw new AssertionError("Duplicate revision must not repeat a queue move");});}
                    game.runOnClient(c->{screen.onClose();screen.update(view.issued(view.token(),9));click(screen,screen.control("history"),0);});game.waitTicks(3);game.takeScreenshot("hoi-mio-history-"+stage+"-"+size[0]+"-gui2");
                }
                var available=source("kor-mio-detail-lig-points");
                var actions=new ArrayList<String>();var screen=game.computeOnClient(c->new ScreenOrganization(available,menu,(v,id)->actions.add(id)));game.setScreen(()->screen);
                game.runOnClient(c->{click(screen,screen.control("trait/"+available.choices().stream().filter(ch->ch.id().startsWith("unlock/")).findFirst().orElseThrow().id().substring(7)),0);if(!actions.isEmpty()||screen.control("ok-confirm")==null)throw new AssertionError("Unlock must show confirmation first");click(screen,screen.control("cancel-confirm"),0);if(!actions.isEmpty())throw new AssertionError("Cancel must not unlock");click(screen,screen.control("trait/"+available.choices().stream().filter(ch->ch.id().startsWith("unlock/")).findFirst().orElseThrow().id().substring(7)),0);click(screen,screen.control("ok-confirm"),0);if(!actions.equals(List.of(available.choices().stream().filter(ch->ch.id().startsWith("unlock/")).findFirst().orElseThrow().id())))throw new AssertionError("Unlock confirm must send exactly one action");});
                var policyAvailable=source("kor-mio-policies-lig-ready");
                String attach=policyAvailable.choices().stream().filter(ch->ch.id().startsWith("attach/")).findFirst().orElseThrow().id();
                var policyActions=new ArrayList<ProtocolDialogPayloads.Action>();var policyScreen=game.computeOnClient(c->new ScreenOrganization(policyAvailable,menu,(v,id)->policyActions.add(new ProtocolDialogPayloads.Action(v.token(),v.revision(),id))));game.setScreen(()->policyScreen);
                game.runOnClient(c->{click(policyScreen,policyScreen.control(attach),0);if(!policyActions.isEmpty()||policyScreen.control("ok-confirm")==null)throw new AssertionError("Policy must show confirmation first");click(policyScreen,policyScreen.control("cancel-confirm"),0);if(!policyActions.isEmpty())throw new AssertionError("Cancel must not attach policy");click(policyScreen,policyScreen.control(attach),0);click(policyScreen,policyScreen.control("ok-confirm"),0);click(policyScreen,policyScreen.control(attach),0);if(!policyActions.equals(List.of(new ProtocolDialogPayloads.Action(policyAvailable.token(),policyAvailable.revision(),attach))))throw new AssertionError("Policy confirm must send one authenticated action");policyScreen.update(policyAvailable);if(policyScreen.control(attach).active)throw new AssertionError("Duplicate response must not unlock policy actions");policyScreen.update(policyAvailable.issued(UUID.randomUUID().toString(),11));if(policyScreen.control(attach).active)throw new AssertionError("Another session must not unlock policy actions");});
                var preview=game.computeOnClient(c->new ScreenOrganization(base,menu,(v,id)->{}));game.setScreen(()->preview);
                game.runOnClient(c->preview.update(policyAvailable.issued(base.token(),20)));game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-mio-policy-ready-overlay-"+size[0]+"-gui2");
                var attached=source("kor-mio-policies-lig-attached");
                game.runOnClient(c->preview.update(attached.issued(base.token(),21)));game.waitTicks(3);game.takeScreenshot("hoi-mio-policy-attached-overlay-"+size[0]+"-gui2");
                game.runOnClient(c->{if(attached.tiles().stream().noneMatch(t->t.overlay().equals("current"))||attached.choices().stream().anyMatch(ch->ch.id().startsWith("attach/")))throw new AssertionError("Applied policy must mark current and lock replacements");});
                var detailPolicy=source("kor-mio-detail-lig-policy");game.runOnClient(c->preview.update(detailPolicy.issued(base.token(),22)));game.waitTicks(3);game.takeScreenshot("hoi-mio-policy-detail-"+size[0]+"-gui2");
                game.runOnClient(c->preview.update(unlocked.issued(base.token(),23)));
                var owned=unlocked.tiles().stream().filter(t->t.traitNode()!=null&&t.traitNode().owned()&&t.section().endsWith("generic_mio_trait_rapid_fire")).findFirst().orElseThrow();
                var ownedButton=game.computeOnClient(c->preview.control(owned.section()));game.getInput().setCursorPos((ownedButton.getX()+12)*2,(ownedButton.getY()+12)*2);game.waitTicks(3);game.takeScreenshot("hoi-mio-owned-rapid-fire-tooltip-"+size[0]+"-gui2");
                if(owned.detail().contains("특성 점수 1점 소모"))throw new AssertionError("Owned trait must use its actual source tooltip");
            }
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
    }
}
