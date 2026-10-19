package dev.hoi.client.map;

import dev.hoi.protocol.ProtocolCommanderHud.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class MapArmySourceReviewTest {
    static void run(ClientGameTestContext game,int[] size) {
        String directory=System.getenv("HOI_ARMY_REVIEW_PREVIEW");if(directory==null)return;
        try {
            for(String name:List.of("ungrouped","motorization-0","motorization-1","motorization-2","orders","defending","training","settled","skill-cap")){
                var source=new Snapshot(Files.readString(Path.of(directory,name+".json"))).view();
                var view=game.computeOnClient(client->new View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true,source.theaters()));
                if(Set.of("settled","skill-cap").contains(name)){commanderDetails(game,size,view,name);if(name.equals("settled"))overlappingTraits(game,size,view);continue;}
                if(Set.of("orders","defending","training").contains(name)){MapCommanderContractRenderTest.state(game,size,view,name);continue;}
                var armies=view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY).toList();var army=armies.stream().filter(c->c.panel().leader()!=null&&c.panel().leader().role().equals("장군")).findFirst().orElse(armies.getFirst());
                game.runOnClient(client->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(view);MapCommanderPanels.select(army.id(),false);});game.waitTicks(2);
                capture(game,size,name+"-army");game.runOnClient(client->assertHeaderButtons(false));
                game.runOnClient(client->{boolean visible=MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("theater_create"));if(visible==army.panel().parent().isEmpty())throw new AssertionError("New theater requires an army belonging to a group");});
                if(name.equals("ungrouped")){hover(game,size,name+"-portrait",hit("army_portrait"));continue;}
                if(army.panel().leader()==null||army.panel().headquarters()==null)throw new AssertionError("Review needs current server leader and HQ data together");
                hover(game,size,name+"-priority",hit("motorization_level_toggle_btn"));
                if(!name.equals("motorization-0"))continue;
                int armyRight=game.computeOnClient(client->hit("nv_btn_close").x());
                game.runOnClient(client->{if(!MapCommanderPanels.armyUnitIcon("unit_hud/headquarters").equals("commander/commands/unit_bus_icon_medium/frame_0"))throw new AssertionError("Regional command must use the original bus silhouette");});
                cardHover(game,size,"army-card-tooltip",army);
                hover(game,size,"army-theater-tooltip",game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("theater_card")&&h.tip().equals(MapCommanderHud.cardTooltip(army))).findFirst().orElseThrow()));
                for(String icon:List.of("select_all_armylist_short_btn","railway_gun_toggle_btn","army_select_wings_btn","deploy_hq_button","army_portrait"))hover(game,size,name+"-"+icon,hit(icon));
                var traits=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_trait")).toList());
                if(!traits.stream().map(MapCommanderPanels.Hit::tip).toList().equals(army.panel().leader().traitDetails().stream().map(dev.hoi.protocol.ProtocolDialogView.LeaderTrait::tooltip).toList()))throw new AssertionError("General panel must render exactly the server trait list");
                for(int n=0;n<traits.size();n++)hover(game,size,"general-trait-"+n,traits.get(n));
                var group=view.cards().stream().filter(c->c.role()==Mode.ARMY_GROUP).findFirst().orElseThrow();
                game.runOnClient(client->MapCommanderPanels.select(group.id(),false));game.waitTicks(2);capture(game,size,"group-divisions");game.runOnClient(client->assertHeaderButtons(true));
                groupDivisionOrder(game,size,view,group);
                MapCommanderTraitRenderTest.run(game,size,view);
                game.runOnClient(client->{if(MapCommanderPanels.hits().stream().anyMatch(h->h.icon().equals("theater_create")))throw new AssertionError("Selected group must hide new theater");});
                game.runOnClient(client->{if(hit("nv_btn_close").x()!=armyRight)throw new AssertionError("Army and group headers must share the same right edge");});
                if(group.panel().headquarters()!=null)throw new AssertionError("Army groups must not receive HQ rows");
                cardHover(game,size,"group-card-tooltip",group);
                hover(game,size,"group-theater-tooltip",game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("theater_card")&&h.tip().equals(MapCommanderHud.cardTooltip(group))).findFirst().orElseThrow()));
                hover(game,size,"group-toggle",hit("army_group_toggle_armies_divisions_btn"));
                hover(game,size,"group-disband",hit("army_group_disband"));
                hover(game,size,"marshal-portrait",hit("army_portrait"));
                var groupTraits=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_trait")).toList());
                if(!groupTraits.stream().map(MapCommanderPanels.Hit::tip).toList().equals(group.panel().leader().traitDetails().stream().map(dev.hoi.protocol.ProtocolDialogView.LeaderTrait::tooltip).toList()))throw new AssertionError("Marshal panel must render exactly the server trait list");
                for(int n=0;n<groupTraits.size();n++)hover(game,size,"marshal-trait-"+n,groupTraits.get(n));
                game.runOnClient(client->{var toggle=hit("army_group_toggle_armies_divisions_btn");MapCommanderHud.clickAt(toggle.x()+toggle.w()/2.,toggle.y()+toggle.h()/2.);});game.waitTicks(2);capture(game,size,"group-armies");
                var summaries=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("template_summary")).toList());
                if(summaries.isEmpty()||summaries.stream().anyMatch(h->!h.tip().contains("편제:")))throw new AssertionError("Current source composition tooltips required");
                for(int n=0;n<summaries.size();n++)hover(game,size,"template-"+n,summaries.get(n));
                MapCommanderContractRenderTest.run(game,size,view);
            }
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static void assertHeaderButtons(boolean group){
        var icons=group?List.of("select_all_armylist_short_btn","motorization_level_toggle_btn","army_group_toggle_armies_divisions_btn"):List.of("select_all_armylist_short_btn","motorization_level_toggle_btn","railway_gun_toggle_btn","army_select_wings_btn");
        for(int n=0;n<icons.size();n++){var button=hit(icons.get(n));if(n+1<icons.size()&&button.x()+button.w()!=hit(icons.get(n+1)).x())throw new AssertionError("Commander controls must share a boundary without overlapping hit targets");if(!MapCommanderPanels.tooltipAt(button.x(),button.y()+button.h()/2.).equals(button.tip()))throw new AssertionError("Left edge must belong to its visible commander button");}
    }
    private static void groupDivisionOrder(ClientGameTestContext game,int[] size,View view,Card group){
        var armies=view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel().parent().equals(group.id())).toList();
        var expected=armies.stream().flatMap(c->c.panel().members().stream().sorted(Comparator.comparing(Member::id))).toList();
        if(armies.size()!=3||expected.size()!=57||armies.stream().anyMatch(c->c.panel().members().size()!=19))throw new AssertionError("Group order review requires three armies of nineteen divisions");
        for(var army:armies)for(var member:army.panel().members())if(member.data()==null||!army.id().equals(member.data().attachedArmy())||!Objects.equals(army.panel().armyColor(),member.data().armyColor()))throw new AssertionError("Division shields must identify their owning army card");
        groupOrderCapture(game,size,expected,"top",0);
        for(int boundary:List.of(19,38)){
            game.runOnClient(client->{var row=divisionHits().getFirst();for(int n=0;n<6;n++)MapCommanderPanels.scrollAt(row.x()+row.w()/2.,row.y()+row.h()/2.,-1);});game.waitTicks(2);
            groupOrderCapture(game,size,expected,"boundary-"+boundary+"-"+(boundary+1),boundary-1);
            game.runOnClient(client->{if(divisionHits().stream().noneMatch(h->h.tip().startsWith(expected.get(boundary).name()+"\n")))throw new AssertionError("Both sides of army boundary must be visible");});
        }
        game.runOnClient(client->{var bar=hit("army_scrollbar");if(!MapCommanderPanels.pressScroll(bar.x()+bar.w()/2.,bar.y()+bar.h()-1))throw new AssertionError("Scrollbar press must be inside its hit bounds");MapCommanderPanels.dragScroll(bar.y()+bar.h()+100);MapCommanderPanels.releaseScroll();});game.waitTicks(2);
        groupOrderCapture(game,size,expected,"last",56);
        var reordered=new ArrayList<>(view.cards());var reversed=new ArrayList<>(armies);Collections.reverse(reversed);int next=0;for(int n=0;n<reordered.size();n++)if(armies.contains(reordered.get(n)))reordered.set(n,reversed.get(next++));
        var response=new View(view.country(),view.dimension(),view.branch(),reordered,view.wings(),view.running(),view.theaters());
        var reversedRows=reversed.stream().flatMap(c->c.panel().members().stream().sorted(Comparator.comparing(Member::id))).toList();
        game.runOnClient(client->{MapCommanderHud.accept(response);MapCommanderPanels.select(group.id(),false);});game.waitTicks(2);groupOrderCapture(game,size,reversedRows,"reordered",0);
        game.runOnClient(client->MapCommanderPanels.select(reversed.getFirst().id(),true));game.waitTicks(2);groupOrderCapture(game,size,reversedRows,"multiple-cards",0);
        game.runOnClient(client->{MapCommanderHud.accept(view);MapCommanderPanels.select(group.id(),false);});game.waitTicks(2);
    }
    private static List<MapCommanderPanels.Hit> divisionHits(){return MapCommanderPanels.hits().stream().filter(h->h.gesture()!=null&&h.tip().contains("Ctrl+클릭")&&h.tip().contains("사단 위치로 이동")).toList();}
    private static void groupOrderCapture(ClientGameTestContext game,int[] size,List<Member> expected,String name,int required){
        game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);
        var rows=game.computeOnClient(client->{var hits=divisionHits();if(hits.isEmpty())throw new AssertionError("Missing group division rows");int first=-1;for(int n=0;n<expected.size();n++)if(hits.getFirst().tip().startsWith(expected.get(n).name()+"\n")){first=n;break;}if(first<0||required<first||required>=first+hits.size())throw new AssertionError("Required group row is outside the viewport: "+name);for(int n=0;n<hits.size();n++)if(!hits.get(n).tip().startsWith(expected.get(first+n).name()+"\n"))throw new AssertionError("Group rows differ from concatenated army lists: "+name);if(MapUnitHud.selectedKeys().size()!=57)throw new AssertionError("Ordering must preserve all selected divisions");return hits;});
        var image=game.takeScreenshot("hoi-group-order-57-"+name+"-"+size[0]+"x"+size[1]+"-gui2");
        try{var pixels=javax.imageio.ImageIO.read(image.toFile());for(var row:rows){var member=expected.stream().filter(m->row.tip().startsWith(m.name()+"\n")).findFirst().orElseThrow();int color=member.data().armyColor(),matches=0;for(int y=row.y()*2;y<(row.y()+row.h())*2;y++)for(int x=(row.x()+row.w()-17)*2;x<(row.x()+row.w())*2;x++){int rgb=pixels.getRGB(x,y);int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(color==0xdb0b0b?r>g*2&&r>b*2:color==0x0bdbdb?g>r*2&&b>r*2:b>r&&r>g*2)matches++;}if(matches<5)throw new AssertionError("Army shield color missing: "+member.id());}}catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static MapCommanderPanels.Hit hit(String icon){return MapCommanderPanels.hits().stream().filter(h->h.icon().equals(icon)).findFirst().orElseThrow(()->new AssertionError("Missing review control "+icon));}
    private static void overlappingTraits(ClientGameTestContext game,int[] size,View view){
        var json=com.google.gson.JsonParser.parseString(Snapshot.of(view).json()).getAsJsonObject();
        for(var entry:json.getAsJsonArray("cards")){var leader=entry.getAsJsonObject().getAsJsonObject("panel").getAsJsonObject("leader");var traits=leader.getAsJsonArray("traitDetails");if(traits.isEmpty())continue;var source=traits.get(0).deepCopy();traits=new com.google.gson.JsonArray();for(int n=0;n<12;n++){var trait=source.deepCopy().getAsJsonObject();trait.addProperty("tooltip",trait.get("tooltip").getAsString()+"\nSynthetic overlap "+n);traits.add(trait);}leader.add("traitDetails",traits);}
        var synthetic=new Snapshot(json.toString()).view();
        game.runOnClient(client->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(synthetic);});
        for(var role:List.of(Mode.ASSIGN_ARMY,Mode.ARMY_GROUP)){
            var card=synthetic.cards().stream().filter(c->c.role()==role&&!c.panel().leader().traitDetails().isEmpty()).findFirst().orElseThrow();
            game.runOnClient(client->MapCommanderPanels.select(card.id(),false));game.waitTicks(2);
            var traits=game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("army_trait")).toList());
            if(traits.size()!=12||!traits.stream().map(MapCommanderPanels.Hit::tip).toList().equals(card.panel().leader().traitDetails().stream().map(dev.hoi.protocol.ProtocolDialogView.LeaderTrait::tooltip).toList()))throw new AssertionError("Overlapping traits must retain every server item in order");
            if(traits.getFirst().w()>=traits.getLast().w())throw new AssertionError("Overflow must reduce spacing without shrinking the final medal");
            for(int n=1;n<traits.size();n++){var trait=traits.get(n);game.runOnClient(client->{if(!MapCommanderPanels.tooltipAt(trait.x(),trait.y()+trait.h()/2.).equals(trait.tip()))throw new AssertionError("Topmost overlapping medal must own the tooltip");});}
            String name="synthetic-overlap-"+(role==Mode.ARMY_GROUP?"group":"army");capture(game,size,name);hover(game,size,name+"-tooltip",traits.get(6));
        }
    }
    private static void commanderDetails(ClientGameTestContext game,int[] size,View view,String name){
        game.runOnClient(client->{MapCommanderHud.reset();MapUnitHud.reset();MapCommanderHud.accept(view);});
        for(var role:List.of(Mode.ASSIGN_ARMY,Mode.ARMY_GROUP)){
            var card=view.cards().stream().filter(c->c.role()==role).findFirst().orElseThrow();String type=role==Mode.ARMY_GROUP?"group":"army";
            if(card.panel().leaderEnRouteDays()!=null)throw new AssertionError("Settled fixture must include active commander effects");
            if(name.equals("skill-cap")&&(card.panel().leader().skill()!=9||card.panel().experience()!=1.0||!card.tooltip().contains("기술: §e9§r(최대)")))throw new AssertionError("Level nine must show the cap and a full experience bar");
            game.runOnClient(client->MapCommanderPanels.select(card.id(),false));game.waitTicks(2);capture(game,size,name+"-"+type+"-panel");
            cardHover(game,size,name+"-"+type+"-card-tooltip",card);
            hover(game,size,name+"-"+type+"-theater-tooltip",game.computeOnClient(client->MapCommanderPanels.hits().stream().filter(h->h.icon().equals("theater_card")&&h.tip().equals(MapCommanderHud.cardTooltip(card))).findFirst().orElseThrow()));
        }
    }
    private static void cardHover(ClientGameTestContext game,int[] size,String name,Card card){
        var box=game.computeOnClient(client->{String tip=MapCommanderHud.cardTooltip(card);if(card.tooltip().isEmpty()||!tip.endsWith(card.tooltip())||!card.tooltip().contains("기술:"))throw new AssertionError("Card must append authoritative commander tooltip");return MapCommanderHud.boxes().stream().filter(b->b.tip().equals(tip)).findFirst().orElseThrow();});
        game.getInput().setCursorPos((box.x()+box.width()/2.)*2,(box.y()+box.height()/2.)*2);game.waitTicks(16);
        game.runOnClient(client->{if(!MapCommanderHud.renderedTooltip().equals(box.tip()))throw new AssertionError("Card tooltip content changed");var bounds=MapCommanderHud.tooltipBounds();if(bounds==null||bounds.x()<0||bounds.y()<0||bounds.x()+bounds.width()>client.getWindow().getGuiScaledWidth()||bounds.y()+bounds.height()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Card tooltip escaped GUI2 viewport");var lines=dev.hoi.client.ui.UiTooltips.sourceLines(client.font,box.tip(),client.getWindow().getGuiScaledWidth());for(var line:lines){var text=new StringBuilder();line.accept((index,style,code)->{text.appendCodePoint(code);return true;});if(text.toString().contains("----"))throw new AssertionError("Source separator must render as a blank line");}});
        game.takeScreenshot("hoi-army-source-review-"+name+"-"+size[0]+"x"+size[1]+"-gui2");
    }
    private static void capture(ClientGameTestContext game,int[] size,String name){game.getInput().setCursorPos(size[0]/2.,size[1]/2.);game.waitTicks(2);game.takeScreenshot("hoi-army-source-review-"+name+"-"+size[0]+"x"+size[1]+"-gui2");}
    private static void hover(ClientGameTestContext game,int[] size,String name,MapCommanderPanels.Hit hit){
        game.getInput().setCursorPos((hit.x()+hit.w()/2.)*2,(hit.y()+hit.h()/2.)*2);game.waitTicks(16);
        game.runOnClient(client->{if(!MapCommanderHud.renderedTooltip().equals(hit.tip()))throw new AssertionError("Review tooltip changed or was obscured: "+name);var bounds=MapCommanderHud.tooltipBounds();if(bounds==null||bounds.x()<0||bounds.y()<0||bounds.x()+bounds.width()>client.getWindow().getGuiScaledWidth()||bounds.y()+bounds.height()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Review tooltip escaped GUI2 viewport: "+name);});
        game.takeScreenshot("hoi-army-source-review-"+name+"-"+size[0]+"x"+size[1]+"-gui2");
    }
}
