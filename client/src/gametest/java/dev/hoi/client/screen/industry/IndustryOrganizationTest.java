package dev.hoi.client.screen.industry;

import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;

public final class IndustryOrganizationTest {
    private IndustryOrganizationTest() {}
    public static void types(ClientGameTestContext game) {
        int previous = game.computeOnClient(c -> c.options.guiScale().get());
        try {
            var types = new Gson().fromJson(json("mio-types"), com.google.gson.JsonArray.class);
            if (types.asList().stream().filter(item -> item.getAsJsonObject().get("owned").getAsBoolean()).count() != 24) throw new AssertionError("Expected all 24 actual owned MIO types");
            for (var item : types) {
                var type = item.getAsJsonObject(); String fixture = type.get("fixture").getAsString();
                if (!type.get("owned").getAsBoolean()) continue;
                if (System.getProperty("hoi.testSuite", "all").equals("mio-long-type") && !fixture.equals("mio-type-20")) continue;
                String menuFixture = type.get("menu").getAsString();
                var menuView = new Gson().fromJson(json(menuFixture.isEmpty() ? "mio-menu-kor" : menuFixture), MenuPayloads.OpenScreen.class).view();
                for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                    game.getInput().resizeWindow(size[0],size[1]); game.runOnClient(c -> c.options.guiScale().set(2));
                    var menu = game.computeOnClient(c -> new ScreenMenu(menuView, MenuTab.POLITICS, () -> {})); game.setScreen(() -> menu);
                    game.runOnClient(c -> {
                        var organization = menuView.manufacturers().stream().filter(m -> m.id().equals(type.get("organization").getAsString())).findFirst();
                        if (organization.isPresent()) {
                            try { var f = ScreenMenu.class.getDeclaredField("manufacturerGroup"); f.setAccessible(true); f.set(menu, organization.get().groups().getFirst()); menu.init(menu.width,menu.height); }
                            catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                        }
                    });
                    game.getInput().setCursorPos(size[0]-3,80); game.waitTicks(2);
                    if (!menuFixture.isEmpty()) game.takeScreenshot("hoi-"+fixture+"-list-"+size[0]+"-gui2");
                    for (String stage : List.of("detail","queue","policies")) {
                        var view = source(fixture+"-"+stage);
                        if (stage.equals("detail") && view.tiles().stream().filter(t -> t.traitNode()!=null).count()!=type.get("traits").getAsInt()) throw new AssertionError("Trait fixture count: "+fixture);
                        if (stage.equals("detail")) game.runOnClient(c -> {
                            for (var tile : view.tiles()) if (tile.traitNode()!=null && dev.hoi.client.ui.UiAssets.imageSize(tile.icon())[0]==0) throw new AssertionError("Missing owned MIO trait image: "+fixture+"/"+tile.icon());
                        });
                        var screen = game.computeOnClient(c -> new IndustryOrganization(view,menu,(v,id)->{})); game.setScreen(() -> screen);
                        if (stage.equals("queue")) game.runOnClient(c -> click(screen,screen.control("queue"),0));
                        game.getInput().setCursorPos(size[0]-3,80); game.waitTicks(2);
                        var capture=game.takeScreenshot("hoi-"+fixture+"-"+stage+"-"+size[0]+"-gui2");
                        if(fixture.equals("mio-type-20")&&stage.equals("detail")){
                            var pixels=javax.imageio.ImageIO.read(capture.toFile());
                            for(var trait:view.tiles())if(trait.traitNode()!=null&&!trait.traitNode().exclusive().isEmpty()){
                                var other=view.tiles().stream().filter(t->t.section().equals("trait/"+trait.traitNode().exclusive().getFirst())).findFirst().orElseThrow();
                                var first=screen.control(trait.traitNode().column()<other.traitNode().column()?trait.section():other.section());
                                var second=screen.control(trait.traitNode().column()<other.traitNode().column()?other.section():trait.section());
                                int green=0,red=0;
                                for(int y=(first.getY()+8)*2;y<(first.getY()+24)*2;y++)for(int x=(first.getX()+32)*2;x<second.getX()*2;x++){
                                    int rgb=pixels.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                                    if(g>r*1.3&&g>b*1.2&&g>40)green++;
                                    if(r>g*1.5&&r>b*1.2&&r>40)red++;
                                }
                                if(green<12||trait.traitNode().row()!=2&&red<4)throw new AssertionError("Original exclusive sprites must render for one-sided relation: "+trait.section());
                            }
                            levelCenter(pixels,size[0],size[1]);
                            int panelLeft=size[0]/2-Math.min(640,size[0]/2-12),panelTop=size[1]/2-Math.min(360,size[1]/2-12);
                            var headerColors=new HashSet<Integer>();
                            for(int x=panelLeft+400;x<panelLeft+900;x++)headerColors.add(pixels.getRGB(x,panelTop+72));
                            if(headerColors.size()<3)throw new AssertionError("Empty transparent header must reveal the patterned tree background");
                            var synthetic=new Gson().toJsonTree(view).getAsJsonObject();
                            for(var tile:synthetic.getAsJsonArray("tiles"))if(tile.getAsJsonObject().get("section").getAsString().equals("summary"))tile.getAsJsonObject().addProperty("value","12");
                            var longer=new Gson().fromJson(synthetic,DialogView.class);
                            game.setScreen(()->new IndustryOrganization(longer,menu,(v,id)->{}));game.waitTicks(2);
                            levelCenter(javax.imageio.ImageIO.read(game.takeScreenshot("hoi-mio-level-12-synthetic-"+size[0]+"-gui2").toFile()),size[0],size[1]);
                            var sample=synthetic.getAsJsonArray("tiles").asList().stream().map(com.google.gson.JsonElement::getAsJsonObject).filter(t->t.has("traitNode")&&!t.get("traitNode").isJsonNull()&&t.getAsJsonObject("traitNode").get("row").getAsInt()==0).findFirst().orElseThrow();
                            sample.getAsJsonObject("traitNode").add("bonuses",new com.google.gson.JsonArray());
                            var empty=detailPixels(game,synthetic,menu,"empty",size[0]);
                            sample.getAsJsonObject("traitNode").add("bonuses",new Gson().toJsonTree(DialogView.TraitNode.BONUSES));
                            var crowded=detailPixels(game,synthetic,menu,"overlap",size[0]);
                            var traitControl=screen.control(sample.get("section").getAsString());int changed=0;
                            for(int y=traitControl.getY()*2;y<(traitControl.getY()+42)*2;y++)for(int x=(traitControl.getX()-8)*2;x<(traitControl.getX()+40)*2;x++)if(empty.getRGB(x,y)!=crowded.getRGB(x,y)){
                                changed++;
                                if(x<(traitControl.getX()+4)*2||x>=(traitControl.getX()+29)*2||y<(traitControl.getY()+27)*2||y>=(traitControl.getY()+39)*2)throw new AssertionError("Overlapping bonus icons escaped their original slot");
                            }
                            if(changed<20)throw new AssertionError("Crowded bonus row must remain visible");
                            for(double fraction:List.of(.5,1.0)){
                                for(var tile:synthetic.getAsJsonArray("tiles"))if(tile.getAsJsonObject().get("section").getAsString().equals("summary"))tile.getAsJsonObject().getAsJsonObject("gauge").addProperty("fraction",fraction);
                                var progress=detailPixels(game,synthetic,menu,"funds-"+fraction,size[0]);int progressChanges=0;
                                for(int y=panelTop+104;y<panelTop+119;y++)for(int x=panelLeft+136;x<panelLeft+307;x++)if(crowded.getRGB(x,y)!=progress.getRGB(x,y))progressChanges++;
                                if(progressChanges<200)throw new AssertionError("Authoritative funds fraction must change original bar pixels");
                            }
                            game.setScreen(()->screen);
                        }
                        if (stage.equals("detail")) {
                            game.runOnClient(c -> screen.mouseScrolled(screen.width/2.,screen.height/2.,0,-100)); game.waitTicks(2);
                            game.takeScreenshot("hoi-"+fixture+"-tree-bottom-"+size[0]+"-gui2");
                        }
                    }
                }
            }
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(previous)); }
    }
    private static java.awt.image.BufferedImage detailPixels(ClientGameTestContext game,com.google.gson.JsonObject json,Screen parent,String label,int width)throws java.io.IOException{
        var view=new Gson().fromJson(json,DialogView.class);
        game.setScreen(()->new IndustryOrganization(view,parent,(v,id)->{}));game.waitTicks(2);
        return javax.imageio.ImageIO.read(game.takeScreenshot("hoi-mio-spacing-"+label+"-synthetic-"+width+"-gui2").toFile());
    }
    private static void levelCenter(java.awt.image.BufferedImage image,int width,int height){
        int left=width/2-Math.min(640,width/2-12),top=height/2-Math.min(360,height/2-12),min=Integer.MAX_VALUE,max=-1;
        for(int y=top+84;y<top+102;y++)for(int x=left+86;x<left+124;x++){
            int rgb=image.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
            if(r>180&&g>180&&b>180){min=Math.min(min,x);max=Math.max(max,x);}
        }
        if(max<0||Math.abs((min+max)/2.0-(left+107))>2)throw new AssertionError("Level digits must center in the red field at "+width+": "+min+".."+max);
    }
    private static String json(String name)throws java.io.IOException{return Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),name+".json"));}
    private static DialogView source(String name)throws java.io.IOException{return new DialogPayloads.Show(json(name),true).view().issued(UUID.randomUUID().toString(),7);}
    private static MouseButtonEvent event(AbstractWidget b,int modifiers,int button){return new MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+b.getHeight()/2.,new MouseButtonInfo(button,modifiers));}
    private static void click(Screen screen,AbstractWidget b,int modifiers){if(b==null)throw new AssertionError("Missing control");var e=event(b,modifiers,InputConstants.MOUSE_BUTTON_LEFT);screen.mouseClicked(e,false);screen.mouseReleased(e);}
    private static AbstractWidget named(Screen screen,String name){return screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals(name)).map(w->(AbstractWidget)w).findFirst().orElseThrow(()->new AssertionError(name));}
    private static void unavailableQueue(ClientGameTestContext game,DialogView source,Screen parent,int width) {
        var denied=new DialogView(source.token(),source.revision(),source.kind(),source.title(),source.subtitle(),source.body(),source.flag(),source.image(),source.sound(),source.tiles(),source.choices().stream().filter(c->!c.id().startsWith("queue/")&&!c.id().startsWith("up/")&&!c.id().startsWith("down/")).toList(),source.presentation());
        var actions=new ArrayList<String>();
        var screen=game.computeOnClient(c->new IndustryOrganization(denied,parent,(v,id)->actions.add(id)));
        game.setScreen(()->screen);
        var target=source.tiles().stream().filter(t->t.traitNode()!=null&&t.traitNode().row()==0&&source.choices().stream().anyMatch(c->c.id().equals("queue/"+t.section().substring(6)))).findFirst().orElseThrow();
        game.runOnClient(c->{
            click(screen,screen.control(target.section()),InputConstants.MOD_SHIFT);
            click(screen,screen.control(target.section()),InputConstants.MOD_SHIFT);
            if(!actions.isEmpty())throw new AssertionError("Missing queue choice must reject repeated Shift clicks");
            if(screen.control("history")!=null||screen.control("traits")!=null)throw new AssertionError("MIO detail tabs must remain absent");
            click(screen,screen.control("queue"),0);
            var queued=source.tiles().stream().filter(t->t.traitNode()!=null&&!t.value().isEmpty()).sorted(Comparator.comparingInt(t->Integer.parseInt(t.value()))).toList();
            var first=screen.control("queue-row/"+queued.getFirst().section().substring(6));
            var last=screen.control("queue-row/"+queued.getLast().section().substring(6));
            screen.mouseClicked(event(last,0,InputConstants.MOUSE_BUTTON_LEFT),false);
            screen.mouseReleased(event(first,0,InputConstants.MOUSE_BUTTON_LEFT));
            screen.mouseClicked(event(first,0,InputConstants.MOUSE_BUTTON_LEFT),false);
            screen.mouseReleased(event(last,0,InputConstants.MOUSE_BUTTON_LEFT));
            if(!actions.isEmpty())throw new AssertionError("Missing up/down choices must reject both queue drag directions");
        });
        game.getInput().setCursorPos(width-5,80);game.waitTicks(3);game.takeScreenshot("hoi-mio-queue-unavailable-synthetic-"+width+"-gui2");
        game.runOnClient(c->{screen.onClose();screen.update(source.issued(source.token(),source.revision()+1));click(screen,screen.control(target.section()),InputConstants.MOD_SHIFT);if(!actions.equals(List.of("queue/"+target.section().substring(6))))throw new AssertionError("New authoritative choices must restore queue input");});
    }
    public static void run(ClientGameTestContext game){
        int scale=game.computeOnClient(c->c.options.guiScale().get());
        try{
            var menuView=new Gson().fromJson(json("kor-mio-menu"),MenuPayloads.OpenScreen.class).view();
            var base=source("kor-mio-detail-lig");var unlocked=source("kor-mio-detail-lig-unlocked");var policies=source("kor-mio-policies-lig");
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                var menu=game.computeOnClient(c->new ScreenMenu(menuView,MenuTab.POLITICS,()->{}));game.setScreen(()->menu);
                game.runOnClient(c->{
                    menu.mouseScrolled(30,menu.height-50,0,-40);
                    var entry=menuView.page(MenuTab.POLITICS).sections().stream().filter(s->s.icon().equals("research_production")).findFirst().orElseThrow().entries().getFirst();
                    var open=menu.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().startsWith(entry.name()+" · ")).map(w->(AbstractWidget)w).findFirst().orElseThrow();click(menu,open,0);
                    var requests=new ArrayList<CommanderHudPayloads.Open>();
                    try{var observer=dev.hoi.client.map.commander.CommanderHud.class.getDeclaredField("requestObserver");observer.setAccessible(true);observer.set(null,(java.util.function.Consumer<CommanderHudPayloads.Open>)requests::add);try{var details=menu.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("세부 사항")&&b.getX()>menu.panelWidth()).map(w->(AbstractWidget)w).findFirst().orElseThrow();click(menu,details,0);}finally{observer.set(null,null);}}catch(ReflectiveOperationException e){throw new RuntimeException(e);}
                    if(requests.size()!=1||requests.getFirst().mode()!=CommanderHudPayloads.Mode.ORGANIZATION||menuView.manufacturers().stream().noneMatch(m->m.id().equals(requests.getFirst().target())&&m.groups().contains("armor")))throw new AssertionError("Politics list must open the selected organization through its server mode: "+requests);
                });
                for(String group:List.of("armor","navy","air","materiel","points")){
                    game.runOnClient(c->{try{var field=ScreenMenu.class.getDeclaredField("manufacturerGroup");field.setAccessible(true);field.set(menu,group);menu.init(menu.width,menu.height);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}int expected=(int)menuView.manufacturers().stream().filter(m->group.equals("points")?m.points()>0:m.groups().contains(group)).count();long visible=menu.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals("세부 사항")).count();if(visible<Math.min(expected,4))throw new AssertionError("Manufacturer filter lost visible source entries");});
                    game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-mio-list-"+group+"-"+size[0]+"-gui2");
                    if(!group.equals("points")){game.runOnClient(c->{click(menu,named(menu,"이름"),0);click(menu,named(menu,"이름"),0);menu.mouseScrolled(menu.width-20,100,0,-8);menu.mouseScrolled(menu.width-20,100,0,8);});}
                }
                game.runOnClient(c->{try{var f=ScreenMenu.class.getDeclaredField("manufacturerGroup");f.setAccessible(true);f.set(menu,"materiel");var sorted=ScreenMenu.class.getDeclaredField("manufacturerSorted");sorted.setAccessible(true);sorted.set(menu,false);menu.init(menu.width,menu.height);}catch(ReflectiveOperationException e){throw new RuntimeException(e);}});
                for(var view:List.of(base,unlocked,policies)){
                    var sent=new ArrayList<DialogPayloads.Action>();var screen=game.computeOnClient(c->new IndustryOrganization(view,menu,(v,id)->sent.add(new DialogPayloads.Action(v.token(),v.revision(),id))));game.setScreen(()->screen);game.waitTicks(3);
                    String stage=view==base?"detail":view==unlocked?"unlocked":"policies";game.takeScreenshot("hoi-mio-"+stage+"-"+size[0]+"-gui2");
                    if(view==base){
                        var blocked=view.tiles().stream().filter(t->t.traitNode()!=null&&!t.traitNode().parents().isEmpty()&&t.traitNode().row()<2&&t.value().isEmpty()&&view.choices().stream().noneMatch(ch->ch.id().equals("queue/"+t.section().substring(6)))).findFirst().orElseThrow();
                        var blockedButton=game.computeOnClient(c->screen.control(blocked.section()));
                        game.runOnClient(c->{click(screen,blockedButton,InputConstants.MOD_SHIFT);click(screen,blockedButton,InputConstants.MOD_SHIFT);if(!sent.isEmpty())throw new AssertionError("Authoritative prerequisite-blocked trait must not send a queue request");});
                        game.getInput().setCursorPos((blockedButton.getX()+12)*2,(blockedButton.getY()+12)*2);game.waitTicks(3);game.takeScreenshot("hoi-mio-prerequisite-blocked-"+size[0]+"-gui2");
                    }
                    if(view==policies){var first=game.computeOnClient(c->screen.control("attach/"+view.tiles().getFirst().section().substring(7)));game.getInput().setCursorPos((first.getX()+20)*2,(first.getY()+20)*2);game.waitTicks(3);game.takeScreenshot("hoi-mio-policy-tooltip-"+size[0]+"-gui2");game.runOnClient(c->{if(view.choices().stream().anyMatch(ch->ch.id().startsWith("attach/")))throw new AssertionError("Initial policy fixture should be unavailable");screen.mouseScrolled(screen.width/2.,screen.height/2.,0,-5);if(!sent.isEmpty())throw new AssertionError("Scrolling policy choices must not apply one");click(screen,screen.control("back"),0);if(!sent.equals(List.of(new DialogPayloads.Action(view.token(),7,"back"))))throw new AssertionError("Policy back authentication");});continue;}
                    var target=view.tiles().stream().filter(t->t.section().startsWith("trait/")&&view.choices().stream().anyMatch(ch->ch.id().equals("queue/"+t.section().substring(6)))&&t.traitNode().row()==0).findFirst().orElseThrow();
                    var control=game.computeOnClient(c->screen.control(target.section()));game.getInput().setCursorPos((control.getX()+12)*2,(control.getY()+12)*2);game.waitTicks(3);game.takeScreenshot("hoi-mio-trait-tooltip-"+stage+"-"+size[0]+"-gui2");
                    game.runOnClient(c->{var right=event(control,0,InputConstants.MOUSE_BUTTON_RIGHT);screen.mouseClicked(right,false);if(!sent.isEmpty())throw new AssertionError("Right click must not mutate traits");click(screen,control,InputConstants.MOD_SHIFT);click(screen,control,InputConstants.MOD_SHIFT);if(!sent.equals(List.of(new DialogPayloads.Action(view.token(),7,"queue/"+target.section().substring(6)))))throw new AssertionError("Queue must send one authenticated request");screen.update(view);if(screen.control("policies").active)throw new AssertionError("Same revision must preserve pending lock");screen.update(view.issued(view.token(),8));click(screen,screen.control("queue"),0);});
                    game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(3);game.takeScreenshot("hoi-mio-queue-"+stage+"-"+size[0]+"-gui2");
                    if(view==unlocked){game.runOnClient(c->{var queued=view.tiles().stream().filter(t->t.section().startsWith("trait/")&&!t.value().isEmpty()).sorted(Comparator.comparingInt(t->Integer.parseInt(t.value()))).toList();var last=queued.getLast();String token=last.section().substring(6);var row=screen.control("queue-row/"+token);var first=screen.control("queue-row/"+queued.getFirst().section().substring(6));sent.clear();screen.mouseClicked(event(row,0,InputConstants.MOUSE_BUTTON_LEFT),false);screen.mouseReleased(event(first,0,InputConstants.MOUSE_BUTTON_LEFT));var expected=view.choices().stream().anyMatch(ch->ch.id().equals("up/"+token))?List.of(new DialogPayloads.Action(view.token(),8,"up/"+token)):List.<DialogPayloads.Action>of();if(!sent.equals(expected))throw new AssertionError("Drag must honor the current authoritative queue move choice");screen.update(view.issued(view.token(),8));if(!sent.equals(expected))throw new AssertionError("Duplicate revision must not repeat a queue move");});}
                    game.runOnClient(c->{screen.onClose();screen.update(view.issued(view.token(),9));if(screen.control("history")!=null||screen.control("traits")!=null)throw new AssertionError("MIO details must expose the trait tree without history or trait tabs");});game.waitTicks(3);game.takeScreenshot("hoi-mio-traits-only-"+stage+"-"+size[0]+"-gui2");
                }
                unavailableQueue(game,unlocked,menu,size[0]);
                var available=source("kor-mio-detail-lig-points");
                var actions=new ArrayList<String>();var screen=game.computeOnClient(c->new IndustryOrganization(available,menu,(v,id)->actions.add(id)));game.setScreen(()->screen);
                game.runOnClient(c->{click(screen,screen.control("trait/"+available.choices().stream().filter(ch->ch.id().startsWith("unlock/")).findFirst().orElseThrow().id().substring(7)),0);if(!actions.isEmpty()||screen.control("ok-confirm")==null)throw new AssertionError("Unlock must show confirmation first");click(screen,screen.control("cancel-confirm"),0);if(!actions.isEmpty())throw new AssertionError("Cancel must not unlock");click(screen,screen.control("trait/"+available.choices().stream().filter(ch->ch.id().startsWith("unlock/")).findFirst().orElseThrow().id().substring(7)),0);click(screen,screen.control("ok-confirm"),0);if(!actions.equals(List.of(available.choices().stream().filter(ch->ch.id().startsWith("unlock/")).findFirst().orElseThrow().id())))throw new AssertionError("Unlock confirm must send exactly one action");});
                var policyAvailable=source("kor-mio-policies-lig-ready");
                String attach=policyAvailable.choices().stream().filter(ch->ch.id().startsWith("attach/")).findFirst().orElseThrow().id();
                var policyActions=new ArrayList<DialogPayloads.Action>();var policyScreen=game.computeOnClient(c->new IndustryOrganization(policyAvailable,menu,(v,id)->policyActions.add(new DialogPayloads.Action(v.token(),v.revision(),id))));game.setScreen(()->policyScreen);
                game.runOnClient(c->{click(policyScreen,policyScreen.control(attach),0);if(!policyActions.isEmpty()||policyScreen.control("ok-confirm")==null)throw new AssertionError("Policy must show confirmation first");click(policyScreen,policyScreen.control("cancel-confirm"),0);if(!policyActions.isEmpty())throw new AssertionError("Cancel must not attach policy");click(policyScreen,policyScreen.control(attach),0);click(policyScreen,policyScreen.control("ok-confirm"),0);click(policyScreen,policyScreen.control(attach),0);if(!policyActions.equals(List.of(new DialogPayloads.Action(policyAvailable.token(),policyAvailable.revision(),attach))))throw new AssertionError("Policy confirm must send one authenticated action");policyScreen.update(policyAvailable);if(policyScreen.control(attach).active)throw new AssertionError("Duplicate response must not unlock policy actions");policyScreen.update(policyAvailable.issued(UUID.randomUUID().toString(),11));if(policyScreen.control(attach).active)throw new AssertionError("Another session must not unlock policy actions");});
                var preview=game.computeOnClient(c->new IndustryOrganization(base,menu,(v,id)->{}));game.setScreen(()->preview);
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
