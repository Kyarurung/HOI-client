package dev.hoi.client.map;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.*;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class MapCommanderSourceStateTest {
    static void run(ClientGameTestContext game,ProtocolMenuView menu,Path directory) throws java.io.IOException {
        var source=new Snapshot(Files.readString(directory.resolve("kor-command-panel-army-trait-skills.json"))).view();
        var active=new Snapshot(Files.readString(directory.resolve("kor-command-panel-army-probing-active.json"))).view();
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(c->{c.options.guiScale().set(2);ClientCampaignHud.accept(ProtocolHudPayloads.State.of(source.country(),menu.hud()));});
                game.setScreen(ScreenMapCursor::new);
                for(String id:List.of("mil_1","mil_3","mil_4","mil_2")) {
                    var card=source.cards().stream().filter(c->c.id().equals(id)).findFirst().orElseThrow();
                    game.runOnClient(c->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(source.country(),c.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true,source.theaters()));MapCommanderPanels.select(id,false);});
                    game.waitTicks(2);
                    var skills=game.computeOnClient(c->MapCommanderPanels.hits().stream().filter(h->h.icon().startsWith("ability_")).toList());
                    if(!skills.stream().map(h->h.icon().substring(8)).toList().equals(card.panel().abilities().stream().limit(4).toList()))throw new AssertionError("Rendered skills must retain at most four source skills including the three basics: "+id);
                    if(id.equals("mil_1")&&!card.panel().abilities().equals(List.of("force_attack","last_stand","staff_office_plan","probing_attack")))throw new AssertionError("Army must retain its first acquired trait skill after the three basics");
                    if(id.equals("mil_3")&&!card.panel().abilities().equals(List.of("force_attack","last_stand","staff_office_plan","glider_planes")))throw new AssertionError("Unsupported first acquired skill must occupy the fourth slot");
                    var traits=game.computeOnClient(c->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_trait")).toList());
                    if(!traits.stream().map(MapCommanderPanels.Hit::tip).toList().equals(card.panel().leader().traitDetails().stream().map(ProtocolDialogView.LeaderTrait::tooltip).toList()))throw new AssertionError("Source trait tooltip changed: "+id);
                    if(id.equals("mil_4")&&!card.panel().abilities().equals(List.of("force_attack","last_stand","staff_office_plan","extra_suplies")))throw new AssertionError("Group must exclude general trait skills");
                    if(id.equals("mil_2")&&(card.panel().leader().traitDetails().stream().filter(t->Boolean.FALSE.equals(t.active())).count()!=3||card.panel().abilities().contains("extra_suplies")))throw new AssertionError("Directly commanding marshal must have inactive marshal traits and no marshal skill");
                    var image=capture(game,size,id);
                    var pixels=javax.imageio.ImageIO.read(image.toFile());
                    for(int n=0;n<traits.size();n++) {
                        var hit=traits.get(n);boolean inactive=Boolean.FALSE.equals(card.panel().leader().traitDetails().get(n).active());int red=0;
                        for(int y=hit.y()*2;y<(hit.y()+hit.h())*2;y++)for(int x=hit.x()*2;x<(hit.x()+hit.w())*2;x++){int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(r>40&&r>g*1.7&&r>b*1.7)red++;}
                        if(inactive&&red<30)throw new AssertionError("Source inactive medal is missing its red mask: "+id+" "+n+" "+red);
                    }
                    for(var detail:card.panel().abilityDetails()) {
                        if(!card.panel().abilities().stream().limit(4).toList().contains(detail.id()))continue;
                        var hit=skills.stream().filter(h->h.icon().equals("ability_"+detail.id())).findFirst().orElseThrow();
                        if(!hit.tip().equals(detail.tooltip()))throw new AssertionError("Source skill tooltip changed: "+detail.id());
                        if(!Set.of("glider_planes","faster_naval_invasion_planning").contains(detail.id()))continue;
                        if(detail.enabled()||detail.affordable()!=null)throw new AssertionError("Unsupported skill source contract changed");
                        for(int y=(hit.y()+3)*2;y<(hit.y()+hit.h()-3)*2;y++)for(int x=(hit.x()+3)*2;x<(hit.x()+hit.w()-3)*2;x++){int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))>4)throw new AssertionError("Unsupported source skill must be grayscale");}
                    }
                    if(id.equals("mil_2")) {
                        var hit=traits.get(1);game.getInput().setCursorPos((hit.x()+hit.w()/2.)*2,(hit.y()+hit.h()/2.)*2);game.waitTicks(16);
                        game.runOnClient(c->{if(!MapCommanderHud.renderedTooltip().equals(hit.tip()))throw new AssertionError("Inactive source hover lost the warning");});
                        game.takeScreenshot("hoi-commander-source-inactive-tooltip-"+size[0]+"x"+size[1]+"-gui2");
                    }
                    game.runOnClient(c->{String tip=MapCommanderHud.cardTooltip(card);if(!tip.endsWith(card.tooltip())||tip.contains("\n\n\n")||!card.tooltip().startsWith("\n")||!card.tooltip().split("\n",-1)[1].startsWith("기술:"))throw new AssertionError("Card must preserve exactly one blank separator before authoritative skill text");});
                }
                game.runOnClient(c->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(new View(active.country(),c.level.dimension().identifier().toString(),active.branch(),active.cards(),active.wings(),true,active.theaters()));MapCommanderPanels.select("mil_1",false);});
                game.waitTicks(2);
                var activePanel=active.cards().stream().filter(c->c.id().equals("mil_1")).findFirst().orElseThrow().panel();
                var probing=activePanel.abilityDetails().stream().filter(a->a.id().equals("probing_attack")).findFirst().orElseThrow();
                if(!activePanel.abilities().stream().limit(4).toList().contains(probing.id())) {
                    game.runOnClient(c->{if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("ability_probing_attack")))throw new AssertionError("Active skills outside the first four must stay hidden");});
                    continue;
                }
                var hit=game.computeOnClient(c->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("ability_probing_attack")).findFirst().orElseThrow());
                if(!probing.enabled()||!hit.tip().equals(probing.tooltip())||!hit.tip().replaceAll("§.","").startsWith("취소: 탐색 공격"))throw new AssertionError("Active source skill must retain its cancel title");
                game.getInput().setCursorPos((hit.x()+hit.w()/2.)*2,(hit.y()+hit.h()/2.)*2);game.waitTicks(16);
                game.runOnClient(c->{if(!MapCommanderHud.renderedTooltip().equals(probing.tooltip()))throw new AssertionError("Source cancel tooltip changed");});
                game.takeScreenshot("hoi-commander-source-probing-active-"+size[0]+"x"+size[1]+"-gui2");
            }
        } finally {game.runOnClient(c->{MapCommanderHud.reset();MapUnitHud.reset();c.options.guiScale().set(previous);});}
    }
    private static Path capture(ClientGameTestContext game,int[] size,String id) {
        game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);
        return game.takeScreenshot("hoi-commander-source-"+id+"-"+size[0]+"x"+size[1]+"-gui2");
    }
}
