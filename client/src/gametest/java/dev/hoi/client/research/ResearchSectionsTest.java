package dev.hoi.client.research;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ResearchSectionsTest {
    public static void run(ClientGameTestContext game) {
        try {
            var gson=new com.google.gson.Gson();
            var techs=List.of(gson.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-research-sections.json")),ProtocolResearchView.Tech[].class));
            if(techs.get(0).remainingDays(0)!=210||techs.get(2).remainingDays(0)!=263)throw new AssertionError("Research days must use source floor values 210/263");
            var teams=gson.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-research-design-team-before.json")),ProtocolResearchView.class);
            ProtocolResearchView base;
            try(var in=ResearchSectionsTest.class.getResourceAsStream("/tfr-research-view.json")){base=new ProtocolResearchPayloads.Response(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view();}
            var view=new ProtocolResearchView(base.session(),base.revision(),base.country(),base.countryName(),base.day(),base.date(),base.speed(),List.of(new ProtocolResearchView.Slot(0,"",0)),techs,"",base.hud(),base.benefits(),teams.designTeams(),teams.designTeamText());
            int scale=game.computeOnClient(c->c.options.guiScale().get());
            try {
                for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                    game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(c->c.options.guiScale().set(2));
                    var requests=new ArrayList<ProtocolResearchPayloads.Request>();
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
                    if(requests.stream().anyMatch(r->r.action()==ProtocolResearchPayloads.Action.START||r.action()==ProtocolResearchPayloads.Action.CANCEL))throw new AssertionError("Inspecting sections mutated research");
                }
            }finally{game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(scale));}
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
}
