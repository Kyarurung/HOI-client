package dev.hoi.fabric;

import dev.hoi.persistence.PersistencePlayableCampaign;
import dev.hoi.protocol.ProtocolCommanderHud.*;
import dev.hoi.scenario.ScenarioWorldBootstrap;
import dev.hoi.simulation.SimulationWorldService;
import dev.hoi.military.MilitaryArmyMotorization;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FabricArmyReviewTest {
    @Test void exportCurrentArmyPresentation() throws Exception {
        var sim=new SimulationWorldService(ScenarioWorldBootstrap.install(PersistencePlayableCampaign.initialState()));
        var warfare=sim.warfare();var output=Path.of("build/army-review-fixtures");Files.createDirectories(output);
        sim.stateAffairs().setScenarioValue("KOR","command_power",100);
        var units=sim.divisions().stream().filter(d->d.country().equals("KOR")).map(d->d.id()).toList();
        var armies=new ArrayList<String>();
        for(int n=0;n<3;n++)armies.add(warfare.army("KOR",null,null,"KOR"));
        for(int n=0;n<units.size();n++)warfare.assign("KOR",armies.get(n%3),units.get(n));
        Files.writeString(output.resolve("ungrouped.json"),Snapshot.of(FabricCommanderOffers.hud(sim,"KOR","hoi:atlas_army_kor")).json());
        for(String army:armies){
            var view=FabricCommanderOffers.view(sim,"KOR",new Open(Mode.ASSIGN_ARMY,army));
            String leader=view.choices().stream().filter(c->!c.id().equals("remove")&&!c.id().startsWith("traits/")).findFirst().orElseThrow().id();
            warfare.assignCommander("KOR",army,leader,false);
        }
        String group=warfare.createArmyGroup("KOR",new LinkedHashSet<>(armies));
        var marshals=FabricArmyGroupOffers.view(sim,"KOR",new Open(Mode.ASSIGN_GROUP_MARSHAL,group));
        String marshal=marshals.choices().stream().filter(c->!c.id().equals("remove")&&!c.id().startsWith("traits/")).findFirst().orElseThrow().id();
        warfare.assignMarshal("KOR",group,marshal);
        for(int phase=0;phase<3;phase++){
            var view=FabricCommanderOffers.hud(sim,"KOR","hoi:atlas_army_kor");
            assertEquals(units.size(),view.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY).mapToInt(Card::count).sum());
            for(var card:view.cards())if(card.role()==Mode.ASSIGN_ARMY){assertNotNull(card.panel().leader());assertNotNull(card.panel().headquarters());assertFalse(card.panel().templateSummaries().isEmpty());for(var row:card.panel().members()){assertEquals(card.panel().insignia(),row.data().insignia());assertEquals(card.panel().armyColor(),row.data().armyColor());}}
            Files.writeString(output.resolve("motorization-"+phase+".json"),Snapshot.of(view).json());
            for(String army:armies)MilitaryArmyMotorization.cycle(warfare,"KOR",army);
        }
        var gson=new com.google.gson.Gson();var readyState=gson.toJsonTree(sim.snapshot()).getAsJsonObject();
        readyState.getAsJsonObject("warfare").getAsJsonObject("landOrderSettings").add("leaderEnRoute",new com.google.gson.JsonObject());
        var ready=new SimulationWorldService(gson.fromJson(readyState,dev.hoi.persistence.PersistenceGameSnapshot.class));
        Files.writeString(output.resolve("settled.json"),Snapshot.of(FabricCommanderOffers.hud(ready,"KOR","hoi:atlas_army_kor")).json());
        for(var entry:readyState.getAsJsonObject("warfare").getAsJsonArray("commanders")){var commander=entry.getAsJsonObject();if(commander.get("country").getAsString().equals("KOR")){commander.addProperty("skill",9);commander.addProperty("experience",0);}}
        var capped=new SimulationWorldService(gson.fromJson(readyState,dev.hoi.persistence.PersistenceGameSnapshot.class));
        var cappedView=FabricCommanderOffers.hud(capped,"KOR","hoi:atlas_army_kor");
        for(var card:cappedView.cards()){assertEquals(9,card.panel().leader().skill());assertEquals(1.0,card.panel().experience());assertTrue(card.tooltip().contains("기술: §e9§r(최대)"));}
        Files.writeString(output.resolve("skill-cap.json"),Snapshot.of(cappedView).json());
        String army=armies.getFirst();var unit=sim.divisions().stream().filter(d->d.id().equals(units.getFirst())).findFirst().orElseThrow();
        String state=sim.foreignAffairs().provinces().stream().filter(p->p.id().equals(unit.currentProvince())).findFirst().orElseThrow().stateId();
        var plan=dev.hoi.military.MilitaryLandMissionOrders.view(warfare,"KOR",army,dev.hoi.military.MilitaryLandMissionOrders.TargetKind.NONE);
        dev.hoi.military.MilitaryDirectLandOrders.createPlans(warfare,"KOR",Map.of(army,plan.configuration()),dev.hoi.military.MilitaryWarfareRules.PlanType.AREA_DEFENSE,List.of(state),List.of(),false);
        var area=dev.hoi.military.MilitaryAreaDefenseOrders.views(warfare,"KOR",List.of(army)).get(army);
        dev.hoi.military.MilitaryLandMissionOrders.assignSelected(warfare,"KOR",army,area.configuration(),area.plan(),List.of(unit.id()));
        Files.writeString(output.resolve("orders.json"),Snapshot.of(FabricCommanderOffers.hud(sim,"KOR","hoi:atlas_army_kor")).json());
        dev.hoi.military.MilitaryArmyCommandOrders.issue(warfare,"KOR",dev.hoi.military.MilitaryArmyCommandOrders.view(warfare,"KOR",List.of(army)).configuration(),dev.hoi.military.MilitaryArmyCommandOrders.Action.ACTIVATE);
        Files.writeString(output.resolve("defending.json"),Snapshot.of(FabricCommanderOffers.hud(sim,"KOR","hoi:atlas_army_kor")).json());
        dev.hoi.military.MilitaryArmyCommandOrders.issue(warfare,"KOR",dev.hoi.military.MilitaryArmyCommandOrders.view(warfare,"KOR",List.of(army)).configuration(),dev.hoi.military.MilitaryArmyCommandOrders.Action.DEACTIVATE);
        dev.hoi.military.MilitaryArmyCommandOrders.issue(warfare,"KOR",dev.hoi.military.MilitaryArmyCommandOrders.view(warfare,"KOR",List.of(army)).configuration(),dev.hoi.military.MilitaryArmyCommandOrders.Action.EXERCISE);
        Files.writeString(output.resolve("training.json"),Snapshot.of(FabricCommanderOffers.hud(sim,"KOR","hoi:atlas_army_kor")).json());
    }
}
