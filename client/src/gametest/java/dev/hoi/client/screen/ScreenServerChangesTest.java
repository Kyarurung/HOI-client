package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.commander.CommanderRoster;
import dev.hoi.client.screen.commander.CommanderTraits;
import dev.hoi.client.screen.decision.DecisionScreen;
import dev.hoi.client.screen.decision.DecisionScreenSourceTest;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogScreen;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.tooltip.TooltipKeywordIcons;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.decision.DecisionPayloads;
import dev.hoi.protocol.dialog.DialogPayloads;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.menu.MenuView;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;

public final class ScreenServerChangesTest {
    private static final Gson JSON=new Gson();
    private ScreenServerChangesTest() {}
    public static <T> T source(String name,Class<T> type){try{return JSON.fromJson(Files.readString(Path.of(System.getenv("HOI_V34_PREVIEW"),name+".json")),type);}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}}
    private static DialogView dialog(String name){return source(name,DialogView.class);}
    private static Object invoke(Object target,String name,Class<?>[] types,Object... args){try{var m=target.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(target,args);}catch(ReflectiveOperationException error){throw new AssertionError(error);}}
    public static void clean(String text){String shown=TooltipKeywordIcons.decorateLines(Tooltips.officerText(text),false).getString();if(shown.contains("[")||shown.contains("$")||shown.contains("£"))throw new AssertionError("Unresolved visible source text: "+shown);}
    public static void capture(ClientGameTestContext game,String name,int[] size){try{var path=game.takeScreenshot("hoi-v34-"+name+"-"+size[0]+"x"+size[1]+"-gui2");var image=javax.imageio.ImageIO.read(path.toFile());if(image.getWidth()!=size[0]||image.getHeight()!=size[1])throw new AssertionError("Capture resolution differs: "+path);}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}}
    public static void run(ClientGameTestContext game,MenuView menu){
        int[] previous=game.computeOnClient(c->new int[]{c.options.guiScale().get(),c.getWindow().getScreenWidth(),c.getWindow().getScreenHeight()});
        var previousHud=game.computeOnClient(c->ClientCampaignHud.visible()?HudPayloads.State.of(ClientCampaignHud.country(),ClientCampaignHud.hud()):HudPayloads.State.HIDDEN);
        try{
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->{c.options.guiScale().set(2);DialogCoordinator.reset();ClientCampaignHud.accept(HudPayloads.State.of(menu.country(),menu.hud()));});
                var event=dialog("kor-super-coup");clean(event.title());clean(event.body());event.choices().forEach(c->clean(c.label()));
                game.setScreen(()->new DialogScreen(event,(v,choice)->{}));game.waitTicks(4);game.runOnClient(c->{var s=(DialogScreen)c.gui.screen();if(event.kind()!=DialogView.Kind.SUPER_EVENT||s.panelLeft()<0||s.panelTop()<0||s.panelLeft()+s.panelWidth()>s.width||s.panelTop()+s.panelHeight()>s.height)throw new AssertionError("Source super event must fit viewport");});capture(game,"super-coup",size);
                politicalPower(game,menu,size);
                decisions(game,"kor-kari","KOR_kari_category",size);
                decisions(game,"jap-diet","JAP_national_diet_category",size);
                decisions(game,"kor-covid-day10","KOR_coronavirus_category",size);
                decisions(game,"kor-territorial","KOR_national_territorial_plan",size);
                decisions(game,"kor-three-year","KOR_lee_army_yec_three_year_plan",size);
                startup(game,size);
                for(String target:List.of("ground_pounder","smoke_screen_expert"))for(String state:List.of("unavailable","ready","owned"))traits(game,target,state,size);
                roster(game,size);
                dev.hoi.client.map.commander.CommanderServerChangesTest.run(game,size);
                dev.hoi.client.screen.focus.FocusServerChangesTest.run(game,size);
            }
        }finally{game.runOnClient(c->{DialogCoordinator.reset();ClientCampaignHud.accept(previousHud);c.gui.setScreen(null);c.options.guiScale().set(previous[0]);});game.getInput().resizeWindow(previous[1],previous[2]);game.waitTicks(2);}
    }
    private static void politicalPower(ClientGameTestContext game,MenuView menu,int[] size){
        var hud=source("kor-political-power",CountryHud.class);var view=new MenuView(menu.country(),menu.countryName(),menu.date(),menu.speed(),menu.pages(),hud,menu.manufacturers(),menu.balance());
        game.runOnClient(c->{ClientCampaignHud.accept(HudPayloads.State.of(menu.country(),hud));HudMenuBar.clear();});game.setScreen(()->new ScreenMenu(view));game.runOnClient(c->c.gui.screen().mouseScrolled(100,5,1000,0));game.waitTicks(2);
        int left=game.computeOnClient(c->{try{var m=HudMenuBar.class.getDeclaredMethod("statsLeft",int.class);m.setAccessible(true);return (int)m.invoke(null,c.getWindow().getGuiScaledWidth());}catch(ReflectiveOperationException error){throw new AssertionError(error);}});
        game.getInput().setCursorPos((left+20)*2,12);game.waitTicks(4);game.runOnClient(c->{try{var f=dev.hoi.client.ui.hud.HudTooltip.class.getDeclaredField("activeItem");f.setAccessible(true);var item=(HudMenuBar.HudIndicator)f.get(null);var h=dev.hoi.client.ui.hud.HudTooltip.class.getDeclaredField("activeHud");h.setAccessible(true);var shown=(CountryHud)h.get(null);if(item==null||!item.icon().equals("political_power")||!hud.equals(shown))throw new AssertionError("Political power source tooltip must use the authoritative HUD");var text=new StringBuilder();for(var line:dev.hoi.client.ui.hud.HudTooltip.body(c.font,c.gui.screen().width,shown,item))line.accept((index,style,point)->{text.appendCodePoint(point);return true;});if(!text.toString().contains("-0.25")||!text.toString().contains("+1.30"))throw new AssertionError("Actual political power tooltip lost cost or daily total: "+text);}catch(ReflectiveOperationException error){throw new AssertionError(error);}});capture(game,"political-power-daily-cost",size);game.getInput().setCursorPos(size[0]-5,size[1]-5);
    }
    private static void decisions(ClientGameTestContext game,String name,String categoryId,int[] size){
        var pages=source(name+"-pages",DecisionPayloads.View[].class);var all=Arrays.stream(pages).flatMap(p->p.categories().stream()).toList();var selected=all.stream().filter(c->c.id().equals(categoryId)).findFirst().orElseThrow();clean(selected.name());clean(selected.description());
        if(name.equals("kor-covid-day10")){var missions=Arrays.stream(pages).flatMap(p->p.rows().stream()).filter(r->r.id().equals("KOR_coronavirus_infection_mission")||r.id().equals("KOR_coronavirus_death_mission")).toList();if(missions.size()!=2||missions.stream().anyMatch(r->r.remainingHours()!=(r.id().contains("infection")?240:720)||r.totalHours()!=(r.id().contains("infection")?240:720)))throw new AssertionError("Day ten source missions retain infection ten days and deaths thirty days");}
        game.runOnClient(c->ClientCampaignHud.accept(HudPayloads.State.of(pages[0].country(),pages[0].hud())));game.setScreen(()->new DecisionScreen(null,r->{}));game.runOnClient(c->{var screen=(DecisionScreen)c.gui.screen();for(var page:pages)screen.update(new DecisionPayloads.Response(screen.token(),JSON.toJson(page)));});game.waitTicks(3);
        game.runOnClient(c->{var screen=(DecisionScreen)c.gui.screen();for(var category:all){boolean collapsed=((Set<?>)field(screen,"collapsed")).contains(category.id());if(collapsed==category.id().equals(categoryId)){DecisionScreenSourceTest.locate(screen,category.name());var label=TooltipKeywordIcons.decorateLines(Tooltips.officerText(category.name()),false).getString();var button=screen.children().stream().filter(b->b instanceof Button x&&x.getMessage().getString().equals(label)).map(Button.class::cast).findFirst().orElseThrow();screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX()+2,button.getY()+2,new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);}}DecisionScreenSourceTest.locate(screen,selected.name());});
        game.getInput().setCursorPos(size[0]-5,size[1]-5);game.waitTicks(4);capture(game,name,size);
        if(name.equals("kor-three-year")){game.runOnClient(c->c.gui.screen().mouseScrolled(100,200,0,-4));game.waitTicks(3);capture(game,name+"-engine-values-bottom",size);}
        if(name.equals("kor-covid-day10"))for(var row:Arrays.stream(pages).flatMap(p->p.rows().stream()).filter(r->r.category().equals(categoryId)).toList()){clean(row.name());var point=game.computeOnClient(c->DecisionScreenSourceTest.locate((DecisionScreen)c.gui.screen(),row.name()));game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);capture(game,name+"-"+row.id(),size);}
        if(name.equals("kor-territorial")){var rows=Arrays.stream(pages).flatMap(p->p.rows().stream()).filter(r->r.category().equals(categoryId)).toList();if(rows.size()!=25)throw new AssertionError("All twenty-five source territorial decisions must reach the actual screen");for(var row:rows){clean(row.name());var point=game.computeOnClient(c->DecisionScreenSourceTest.locate((DecisionScreen)c.gui.screen(),row.name()));game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);capture(game,name+"-"+row.id(),size);}}
    }
    private static void startup(ClientGameTestContext game,int[] size){
        for(String name:List.of("brm-bur-1","chi-taiwan-4","ins-asean-2","jap-jap-13","jap-jap-16","mal-asean-1","mon-MON-1","nep-nep-3","lao-laos-1")){
            var view=dialog("startup-"+name);clean(view.title());clean(view.body());view.choices().forEach(c->{clean(c.label());clean(c.tooltip());});game.setScreen(()->new DialogScreen(view,(v,choice)->{}));game.getInput().setCursorPos(size[0]-5,size[1]-5);game.waitTicks(3);game.runOnClient(c->{var screen=(DialogScreen)c.gui.screen();if(!screen.getTitle().getString().equals(view.title())||screen.panelLeft()<0||screen.panelTop()<0||screen.panelWidth()<=0||screen.panelHeight()<=0||screen.panelLeft()+screen.panelWidth()>screen.width||screen.panelTop()+screen.panelHeight()>screen.height)throw new AssertionError("Empty original event title must retain an intact frame without a placeholder: "+name);});capture(game,"startup-"+name,size);
            if(Set.of("brm-bur-1","nep-nep-3","lao-laos-1").contains(name)&&!view.title().isEmpty())throw new AssertionError("Untranslated source event title keys must stay empty: "+name);
            for(int index=0;index<view.choices().size();index++){int selected=index;var choice=view.choices().get(index);var point=game.computeOnClient(c->{var labels=view.choices().stream().map(value->TooltipKeywordIcons.decorateLines(Tooltips.officerText(value.label()),false).getString()).toList();var button=c.gui.screen().children().stream().filter(b->b instanceof Button x&&labels.contains(x.getMessage().getString())).map(Button.class::cast).toList().get(selected);return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);capture(game,"startup-"+name+"-"+choice.id().replace('.','-')+"-hover",size);}
        }
    }
    private static void traits(ClientGameTestContext game,String target,String state,int[] size){
        var view=dialog("navy-"+target+"-"+state);var tile=view.tiles().stream().filter(t->t.section().equals(target)).findFirst().orElseThrow();clean(tile.name());clean(tile.detail());
        if(tile.traitNode().available()!=state.equals("ready")||tile.traitNode().owned()!=state.equals("owned"))throw new AssertionError("Unexpected authoritative trait state: "+target+"/"+state);
        var sent=new ArrayList<DialogPayloads.Action>();game.setScreen(()->new CommanderTraits(view,null,sent::add));game.waitTicks(3);
        var point=game.computeOnClient(c->{var screen=(CommanderTraits)c.gui.screen();for(int n=0;n<100&&!screen.visible(tile.traitNode());n++)screen.mouseScrolled(screen.x(200),screen.y(180),0,-1);if(!screen.visible(tile.traitNode()))throw new AssertionError("Trait node unreachable");var button=(AbstractWidget)invoke(screen,"choiceButton",new Class<?>[]{String.class},target);if((button!=null)!=state.equals("ready"))throw new AssertionError("Only available server choices get a purchase control");return new int[]{screen.x(160+104*tile.traitNode().column()),screen.y(64+27.5*tile.traitNode().row())-(int)field(screen,"scroll")};});
        game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);game.runOnClient(c->{if(((CommanderTraits)c.gui.screen()).tooltipBounds()==null)throw new AssertionError("Trait source tooltip must be reachable");});capture(game,"navy-"+target+"-"+state+"-hover",size);
        if(state.equals("ready"))game.runOnClient(c->{var screen=(CommanderTraits)c.gui.screen();var button=(AbstractWidget)invoke(screen,"choiceButton",new Class<?>[]{String.class},target);var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,new net.minecraft.client.input.MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(!sent.equals(List.of(new DialogPayloads.Action(view.token(),view.revision(),target))))throw new AssertionError("Trait purchase must retain issued token and revision and reject repeated input: "+target+" "+sent);});
    }
    private static Object field(Object target,String name){try{var field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}catch(ReflectiveOperationException error){throw new AssertionError(error);}}
    private static void roster(ClientGameTestContext game,int[] size){
        var evidence=source("admiral-transfer-evidence",com.google.gson.JsonObject.class);String name=evidence.get("name").getAsString(),id=evidence.get("id").getAsString();
        for(String fixture:List.of("prc-admirals-before","prc-admirals-after","srl-admirals-after")){
            var view=dialog(fixture);boolean present=view.tiles().stream().anyMatch(t->t.section().equals("admiral/"+id));if(present==fixture.equals("prc-admirals-after"))throw new AssertionError("Nationality transfer roster differs: "+fixture);
            game.setScreen(()->new CommanderRoster(view,null,a->{}));game.runOnClient(c->c.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().setValue(name));game.getInput().setCursorPos(size[0]-5,size[1]-5);game.waitTicks(3);capture(game,fixture+"-yuan-search",size);
        }
    }
}
