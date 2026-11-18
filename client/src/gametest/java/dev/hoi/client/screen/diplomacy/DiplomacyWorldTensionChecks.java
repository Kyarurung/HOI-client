package dev.hoi.client.screen.diplomacy;

import dev.hoi.protocol.country.*;
import dev.hoi.protocol.diplomacy.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

public final class DiplomacyWorldTensionChecks {
    public static void run(ClientGameTestContext context, CountryHud hud) {
        DiplomacyWorldTensionPayloads.registerPayloadTypes(); DiplomacyWorldTensionPayloads.registerPayloadTypes();
        var entries=List.of(new DiplomacyWorldTensionView.Entry("a","KOR","대한민국",40,5.0,4.8,"독립 보장"),
                new DiplomacyWorldTensionView.Entry("b","PRC","중화인민공화국",5,10.0,9.5,"전쟁 선포"),
                new DiplomacyWorldTensionView.Entry("c","JAP","일본국",20,null,0,"의용군 파병"));
        var a=new DiplomacyWorldTensionView.Participant("KOR","대한민국","58000","24","43",null,.75,.1,false,false,true);
        var b=new DiplomacyWorldTensionView.Participant("PRC","중화인민공화국","50000–59999","20–29","40–49",null,1.0,.3,false,false,true);
        var war=new DiplomacyWorldTensionView.War("war","대한민국–중화인민공화국 전쟁",10,.6,List.of(a),List.of(b));
        var view=new DiplomacyWorldTensionView("KOR",hud,42,0,3,entries,List.of(war));
        context.setScreen(() -> new DiplomacyWorldTension(view)); context.waitTicks(3);
        context.runOnClient(client -> {
            var screen=(DiplomacyWorldTension)client.gui.screen();
            screen.select(DiplomacyWorldTensionView.Sort.COUNTRY);
            if(!screen.view().entries().getFirst().country().equals("KOR"))throw new AssertionError("Country sorting");
            screen.select(DiplomacyWorldTensionView.Sort.TENSION);
            if(!screen.view().entries().getFirst().id().equals("b"))throw new AssertionError("Tension sorting");
            screen.update(new DiplomacyWorldTensionPayloads.Response(UUID.randomUUID().toString(),""));
            if(client.gui.screen()!=screen)throw new AssertionError("Stale rejection closed current screen");
        });
        dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-world-tension-history");
        context.runOnClient(client -> ((DiplomacyWorldTension)client.gui.screen()).select(DiplomacyWorldTensionView.Sort.WARS));
        context.waitTicks(3); dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-world-tension-wars");
        context.runOnClient(client -> {
            var screen=(DiplomacyWorldTension)client.gui.screen();
            var entry=screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button candidate && candidate.getMessage().getString().equals(war.name())).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow(() -> new AssertionError("War entry must be clickable"));
            var close=screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button candidate && candidate.getMessage().getString().equals("닫기")).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
            float scale=screen.scale();
            if(entry.getX()-close.getX()!=Math.round(DiplomacyWorldTension.LIST_X*scale)-Math.round(204*scale)||entry.getWidth()!=Math.round(449*scale))
                throw new AssertionError("War entry must use world_tension_war_entry x46 and width449: "+entry.getX()+" "+entry.getWidth());
            screen.openWar("war");
        });
        context.waitTicks(3); dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-world-tension-war-detail");
        context.runOnClient(client -> {
            var screen=(DiplomacyWorldTension)client.gui.screen();
            if(!screen.headerTooltip(30,165).equals("참전국")||!screen.headerTooltip(1080,170).equals("사상자")||!screen.headerTooltip(250,170).equals("전쟁 명분"))
                throw new AssertionError("War overview header icons keep the waroverview tooltips");
            if(!screen.headerTooltip(150,170).isEmpty()) throw new AssertionError("Only the original header icons have tooltips");
            var rows=screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button candidate && (candidate.getMessage().getString().equals("대한민국")||candidate.getMessage().getString().equals("중화인민공화국"))).map(w -> (net.minecraft.client.gui.components.Button)w).toList();
            if(rows.size()!=2||rows.get(0).getY()!=rows.get(1).getY()) throw new AssertionError("Both sides start their participant rows on the same line");
        });
        context.runOnClient(client -> {
            var screen=(DiplomacyWorldTension)client.gui.screen();screen.onClose();
            if(screen.warId()!=null||client.gui.screen()!=screen)throw new AssertionError("Detail close must return to history");
            screen.update(new DiplomacyWorldTensionPayloads.Response(screen.token(),""));
            if(client.gui.screen()!=null)throw new AssertionError("Revoked assignment must close tension screen");
        });
    }
}
