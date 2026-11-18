package dev.hoi.client.screen.research;

import dev.hoi.protocol.research.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ResearchSectionsTest {
    public static void run(ClientGameTestContext game) {
        try {
            var gson=new com.google.gson.Gson();
            var techs=List.of(gson.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-research-sections.json")),ResearchView.Tech[].class));
            if(techs.get(0).remainingDays(0)!=210||techs.get(2).remainingDays(0)!=263)throw new AssertionError("Research days must use source floor values 210/263");
            var teams=gson.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-research-design-team-before.json")),ResearchView.class);
            ResearchView base;
            try(var in=ResearchSectionsTest.class.getResourceAsStream("/tfr-research-view.json")){base=new ResearchPayloads.Response(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view();}
            var view=new ResearchView(base.session(),base.revision(),base.country(),base.countryName(),base.day(),base.date(),base.speed(),List.of(new ResearchView.Slot(0,"",0)),techs,"",base.hud(),base.benefits(),teams.designTeams(),teams.designTeamText());
            int scale=game.computeOnClient(c->c.options.guiScale().get());
            try {
                for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                    game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                    slotArtwork(game,teams,size[0]);
                    var requests=new ArrayList<ResearchPayloads.Request>();
                    var screen=game.computeOnClient(c->new ResearchScreen(view,requests::add));game.setScreen(()->screen);
                    int index=0;
                    for(var tech:techs){
                        game.runOnClient(c->{screen.showDetail(tech.id());if(tech.upgrades().isEmpty()&&tech.units().isEmpty())throw new AssertionError("Producer sections absent");});
                        game.getInput().setCursorPos(size[0]-10,size[1]-10);game.waitTicks(3);game.takeScreenshot("hoi-research-sections-"+index+"-"+size[0]+"-gui2");
                        game.runOnClient(c->screen.mouseScrolled(screen.width/2.,screen.height/2.,0,-20));game.waitTicks(3);game.takeScreenshot("hoi-research-sections-bottom-"+index+"-"+size[0]+"-gui2");index++;
                    }
                    game.runOnClient(c->{screen.showDetail(techs.getFirst().id());var button=(net.minecraft.client.gui.components.AbstractWidget)screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget v&&v.getMessage().getString().equals("연구")).findFirst().orElseThrow();screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);});
                    var team=teams.designTeams().stream().filter(t->t.name().equals("SNT모티브")).findFirst().orElseThrow();
                    var row=game.computeOnClient(c->(net.minecraft.client.gui.components.AbstractWidget)screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget v&&v.getMessage().getString().equals(team.name())).findFirst().orElseThrow());
                    game.getInput().setCursorPos((row.getX()+100)*2,(row.getY()+10)*2);game.waitTicks(4);
                    game.runOnClient(c->{long days=screen.teamDays(techs.getFirst(),0);if(days!=192)throw new AssertionError("Expected floor hover 192: "+days);});
                    game.takeScreenshot("hoi-research-floor-hover-"+size[0]+"-gui2");
                    if(requests.stream().anyMatch(r->r.action()==ResearchPayloads.Action.START||r.action()==ResearchPayloads.Action.CANCEL))throw new AssertionError("Inspecting sections mutated research");
                    referenceShell(game,teams,size[0]);
                    navalTerrain(game,teams,size[0]);
                }
            }finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
    private static void slotArtwork(ClientGameTestContext game,ResearchView view,int width) {
        var ids=List.of("hoi:tfr/technology/mechanised_infantry3","hoi:tfr/technology/motorised_infantry2","hoi:tfr/technology/artillery1");
        var slots=java.util.stream.IntStream.range(0,ids.size()).mapToObj(i->new ResearchView.Slot(i,ids.get(i),0)).toList();
        var projected=new ResearchView(view.session(),view.revision(),view.country(),view.countryName(),view.day(),view.date(),view.speed(),slots,view.technologies(),view.message(),view.hud(),view.benefits(),view.designTeams(),view.designTeamText());
        var requests=new ArrayList<ResearchPayloads.Request>();
        game.setScreen(()->new ResearchScreen(projected,requests::add));
        game.getInput().setCursorPos(width-2,80);game.waitTicks(3);
        game.takeScreenshot("hoi-research-native-slot-art-projection-"+width+"-gui2");
        var teamIds=List.of("KOR_kia_manufacturer","KOR_kia_manufacturer","KOR_lig_defense_aerospace_organization");
        var assigned=java.util.stream.IntStream.range(0,ids.size()).mapToObj(i->{
            String team=view.designTeams().stream().filter(t->t.id().equals(teamIds.get(i))&&t.technologies().contains(ids.get(i))).map(ResearchView.DesignTeam::id).findFirst().orElseThrow(()->new AssertionError("Source input must allow "+teamIds.get(i)+" for "+ids.get(i)));
            return new ResearchView.Slot(i,ids.get(i),0,team);
        }).toList();
        if(!assigned.getFirst().designTeam().equals("KOR_kia_manufacturer")||!assigned.get(1).designTeam().equals("KOR_kia_manufacturer")||!assigned.get(2).designTeam().equals("KOR_lig_defense_aerospace_organization"))throw new AssertionError("Display projection requires the issued KIA/KIA/LIG choices");
        game.runOnClient(c->{
            var tech=view.technology(ids.getFirst());
            if(!dev.hoi.client.ui.UiAssets.technologyPath(view.country(),tech).equals("technology/country/kor/tfr/technology/mechanised_infantry3"))throw new AssertionError("K-21 must use its national research artwork");
            if(!dev.hoi.client.ui.UiAssets.technologyPath("USA",tech).equals("technology/tfr/technology/mechanised_infantry3"))throw new AssertionError("Korean artwork cannot replace another country's common artwork");
        });
        var withTeams=new ResearchView(view.session(),view.revision(),view.country(),view.countryName(),view.day(),view.date(),view.speed(),assigned,view.technologies(),view.message(),view.hud(),view.benefits(),view.designTeams(),view.designTeamText());
        game.setScreen(()->new ResearchScreen(withTeams,requests::add));game.getInput().setCursorPos(width-2,80);game.waitTicks(3);
        game.takeScreenshot("hoi-research-native-slot-art-teams-projection-"+width+"-gui2");
        if(requests.stream().anyMatch(r->r.action()==ResearchPayloads.Action.START||r.action()==ResearchPayloads.Action.CANCEL))throw new AssertionError("Source artwork inspection cannot issue a research mutation");
    }
    private static void navalTerrain(ClientGameTestContext game,ResearchView view,int width) {
        var source=dev.hoi.client.screen.industry.IndustryScreenChecks.tankFixture("branch-ready-ship_hull_light_1-fixture.json").tankDesigner();
        var terrains=game.computeOnClient(c->source.adjustments().stream().map(a->new ResearchView.TerrainValue(a.id(),dev.hoi.client.ui.industry.IndustryEquipmentDesigns.adjusterName(a.id()),a.movement(),a.attack(),a.defense())).toList());
        if(terrains.size()!=3||terrains.stream().anyMatch(t->!t.id().startsWith("water_")))throw new AssertionError("Naval icon projection requires three authoritative sea adjustments");
        var gson=new com.google.gson.Gson();
        var json=gson.toJsonTree(view.technology("hoi:tfr/technology/basic_light_shell")).getAsJsonObject();
        json.add("upgrades",gson.toJsonTree(List.of(new ResearchView.Upgrade("",List.of(),terrains,""))));
        var tech=gson.fromJson(json,ResearchView.Tech.class);
        var projected=new ResearchView(view.session(),view.revision(),view.country(),view.countryName(),view.day(),view.date(),view.speed(),List.of(new ResearchView.Slot(0,"",0)),List.of(tech),view.message(),view.hud(),view.benefits(),view.designTeams(),view.designTeamText());
        var requests=new ArrayList<ResearchPayloads.Request>();
        var screen=game.computeOnClient(c->new ResearchScreen(projected,requests::add));
        game.setScreen(()->screen);game.runOnClient(c->screen.showDetail(tech.id()));
        game.getInput().setCursorPos(width-10,10);game.waitTicks(3);
        game.takeScreenshot("hoi-research-naval-icons-projection-"+width+"-gui2");
        if(!requests.isEmpty())throw new AssertionError("Inspecting naval icons must not mutate research");
    }
    private static void referenceShell(ClientGameTestContext game,ResearchView view,int width)throws java.io.IOException{
        var tech=view.technology("hoi:tfr/technology/basic_light_shell");
        var team=view.designTeams().stream().filter(t->t.id().equals("KOR_hj_shipbuilding_organization")).findFirst().orElseThrow();
        int index=team.technologies().indexOf(tech.id());
        if(tech.status()!=ResearchView.Status.AVAILABLE||index<0||team.funds().get(index)!=250||team.dailyCost()!=.05||tech.upgrades().isEmpty())throw new AssertionError("Original shell/HJ reference requires the authoritative technology and its 250-fund offer");
        var groups=ResearchScreen.upgradeGroups(tech.upgrades());
        if(groups.size()!=1||groups.getFirst().size()!=6||groups.getFirst().getFirst().lines().size()!=2)throw new AssertionError("Original shell must show six ship icons above two shared effect lines");
        var first=tech.upgrades().getFirst();
        var category=new ResearchView.Upgrade(first.scope(),first.lines(),first.terrain(),"category_all_infantry");
        var distinct=new ResearchView.Upgrade(first.scope(),List.of(new ResearchView.StatLine("different","+1.0%")),first.terrain(),first.scopeId());
        if(ResearchScreen.upgradeGroups(List.of(first,category,first,distinct,first)).size()!=5)throw new AssertionError("Category, unequal and nonadjacent research effects must remain separate");
        var requests=new ArrayList<ResearchPayloads.Request>();
        var screen=game.computeOnClient(c->new ResearchScreen(view,requests::add));game.setScreen(()->screen);
        game.runOnClient(c->screen.showDetail(tech.id()));game.getInput().setCursorPos(width-10,10);game.waitTicks(3);
        game.takeScreenshot("hoi-research-reference-shell-detail-"+width+"-gui2");
        game.runOnClient(c->{
            String expected=view.designTeamText().get("available_title")+"\n"+String.join("\n",view.designTeams().stream().filter(t->t.technologies().contains(tech.id())).map(t->view.designTeamText().get("available_item").replace("$NAME$",t.name())).toList());
            if(!screen.availableTeamsTooltip(tech.id()).equals(expected)||!screen.availableTeamsTooltip("hoi:unknown").isEmpty())throw new AssertionError("Flask tooltip must preserve only the matching source organization list");
            var close=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget b&&b.visible&&b.active&&b.getMessage().getString().equals("×")&&screen.overDetailWindows(b.getX()+b.getWidth()/2.,b.getY()+b.getHeight()/2.)).map(net.minecraft.client.gui.components.AbstractWidget.class::cast).findFirst().orElseThrow();
            if(screen.children().stream().noneMatch(w->w instanceof net.minecraft.client.gui.components.AbstractWidget b&&b.visible&&b.active&&b.getMessage().getString().equals("×")&&!screen.overDetailWindows(b.getX()+b.getWidth()/2.,b.getY()+b.getHeight()/2.)))throw new AssertionError("The research screen close button must stay drawn and clickable while the detail is open");
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(close.getX()+2,close.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
        });
        var icon=game.computeOnClient(c->{
            try{
                var layoutField=ResearchScreen.class.getDeclaredField("layout");layoutField.setAccessible(true);
                var layout=(ResearchLayout)layoutField.get(screen);var node=layout.nodes().stream().filter(n->n.tech().id().equals(tech.id())).findFirst().orElseThrow();
                var sx=ResearchScreen.class.getDeclaredField("scrollX");sx.setAccessible(true);var sy=ResearchScreen.class.getDeclaredField("scrollY");sy.setAccessible(true);var top=ResearchScreen.class.getDeclaredField("treeTop");top.setAccessible(true);
                return new int[]{(int)(node.x()-sx.getDouble(screen)),(int)(top.getInt(screen)+node.y()+node.height()-Math.round(10*node.scale())-sy.getDouble(screen)),Math.max(1,(int)Math.round(10*node.scale()))};
            }catch(ReflectiveOperationException e){throw new AssertionError(e);}
        });
        game.getInput().setCursorPos(width-10,10);game.waitTicks(3);
        var tree=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-research-reference-shell-tree-"+width+"-gui2").toFile());
        game.runOnClient(c->{
            if(screen.children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.AbstractWidget b&&b.visible&&List.of("잠김","연구 가능","진행 중","완료").contains(b.getMessage().getString())))throw new AssertionError("Research tree must not add status legend controls");
            try{var bottom=ResearchScreen.class.getDeclaredField("treeBottom");bottom.setAccessible(true);if(bottom.getInt(screen)!=screen.height-6)throw new AssertionError("Research tree must use the fullscreen frame interior");}
            catch(ReflectiveOperationException error){throw new AssertionError(error);}
        });
        int purple=0;for(int y=icon[1]*2;y<(icon[1]+icon[2])*2;y++)for(int x=icon[0]*2;x<(icon[0]+icon[2])*2;x++){int rgb=tree.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(b>g+20&&r>g+10)purple++;}
        if(purple<3)throw new AssertionError("Matching organization flask is missing from rendered technology");
        game.getInput().setCursorPos((icon[0]+icon[2]/2)*2,(icon[1]+icon[2]/2)*2);game.waitTicks(3);
        game.takeScreenshot("hoi-research-reference-shell-flask-tooltip-"+width+"-gui2");
        game.runOnClient(c->{
            var event=new net.minecraft.client.input.MouseButtonEvent(icon[0]+icon[2]/2.,icon[1]+icon[2]/2.,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);
            if(!requests.isEmpty())throw new AssertionError("Flask click must open the technology without assigning or starting research");
        });
        game.runOnClient(c->{
            var button=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget b&&b.getMessage().getString().equals("연구")).map(net.minecraft.client.gui.components.AbstractWidget.class::cast).findFirst().orElseThrow();
            var event=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2d,button.getY()+button.getHeight()/2d,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);
        });
        game.waitTicks(3);game.takeScreenshot("hoi-research-reference-shell-teams-"+width+"-gui2");
        int step=0;
        for(String label:List.of("이름","이름","비용")){
            var sort=game.computeOnClient(c->sortButton(screen,label));
            game.runOnClient(c->{var event=new net.minecraft.client.input.MouseButtonEvent(sort.getX()+sort.getWidth()/2d,sort.getY()+sort.getHeight()/2d,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(event,false);screen.mouseReleased(event);});
            game.getInput().setCursorPos(width-10,10);game.waitTicks(3);
            var shot=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-research-reference-shell-teams-sort-"+(++step)+"-"+width+"-gui2").toFile());
            var name=game.computeOnClient(c->sortButton(screen,"이름"));var cost=game.computeOnClient(c->sortButton(screen,"비용"));
            var active=label.equals("이름")?name:cost;var idle=label.equals("이름")?cost:name;
            if(arrowPixels(shot,active)<5||arrowPixels(shot,idle)>0)throw new AssertionError("Original sort button frames must mark only the active sort: "+label+" "+step);
        }
        if(!requests.isEmpty())throw new AssertionError("Sorting organizations must not send research requests");
        var row=game.computeOnClient(c->screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget b&&b.getMessage().getString().equals(team.name())).map(net.minecraft.client.gui.components.AbstractWidget.class::cast).findFirst().orElseThrow());
        game.getInput().setCursorPos((row.getX()+row.getWidth()/2)*2,(row.getY()+row.getHeight()/2)*2);game.waitTicks(4);
        game.runOnClient(c->{if(screen.teamDays(tech,0)!=Math.max(1,(long)Math.floor((tech.baseDays()-tech.progress())/team.rates().get(index))))throw new AssertionError("Shell hover must use the matching HJ research rate");});
        game.takeScreenshot("hoi-research-reference-shell-hj-tooltip-"+width+"-gui2");
        if(!requests.isEmpty())throw new AssertionError("Inspecting the original shell and HJ offer must not start research");
    }
    private static dev.hoi.client.ui.UiMenuButton sortButton(ResearchScreen screen,String label){return screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiMenuButton b&&b.visible&&b.active&&b.getMessage().getString().equals(label)).map(dev.hoi.client.ui.UiMenuButton.class::cast).findFirst().orElseThrow();}
    private static int arrowPixels(java.awt.image.BufferedImage image,net.minecraft.client.gui.components.AbstractWidget button){
        int count=0;for(int y=button.getY()*2;y<(button.getY()+button.getHeight())*2;y++)for(int x=button.getX()*2;x<(button.getX()+button.getWidth())*2;x++){int rgb=image.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(r>140&&g>90&&b<110&&r-b>60)count++;}
        return count;
    }
}
