package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.*;

public final class ScreenWorldTensionChecks {
    public static void run(ClientGameTestContext context, ProtocolCountryHud hud) {
        ProtocolWorldTension.registerPayloadTypes(); ProtocolWorldTension.registerPayloadTypes();
        var entries=List.of(new ProtocolWorldTensionView.Entry("a","KOR","대한민국",40,5.0,4.8,"독립 보장"),
                new ProtocolWorldTensionView.Entry("b","PRC","중화인민공화국",5,10.0,9.5,"전쟁 선포"),
                new ProtocolWorldTensionView.Entry("c","JAP","일본국",20,null,0,"의용군 파병"));
        var a=new ProtocolWorldTensionView.Participant("KOR","대한민국","58000","24","43",null,.75,.1,false,false,true);
        var b=new ProtocolWorldTensionView.Participant("PRC","중화인민공화국","50000–59999","20–29","40–49",null,1.0,.3,false,false,true);
        var war=new ProtocolWorldTensionView.War("war","대한민국–중화인민공화국 전쟁",10,.6,List.of(a),List.of(b));
        var view=new ProtocolWorldTensionView("KOR",hud,42,0,3,entries,List.of(war));
        context.setScreen(() -> new ScreenWorldTension(view)); context.waitTicks(3);
        context.runOnClient(client -> {
            var screen=(ScreenWorldTension)client.gui.screen();
            screen.select(ProtocolWorldTensionView.Sort.COUNTRY);
            if(!screen.view().entries().getFirst().country().equals("KOR"))throw new AssertionError("Country sorting");
            screen.select(ProtocolWorldTensionView.Sort.TENSION);
            if(!screen.view().entries().getFirst().id().equals("b"))throw new AssertionError("Tension sorting");
            screen.update(new ProtocolWorldTension.Response(UUID.randomUUID().toString(),""));
            if(client.gui.screen()!=screen)throw new AssertionError("Stale rejection closed current screen");
        });
        dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-world-tension-history");
        context.runOnClient(client -> ((ScreenWorldTension)client.gui.screen()).select(ProtocolWorldTensionView.Sort.WARS));
        context.waitTicks(3); dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-world-tension-wars");
        context.runOnClient(client -> ((ScreenWorldTension)client.gui.screen()).openWar("war"));
        context.waitTicks(3); dev.hoi.client.ResearchScreenTest.gui2Screenshot(context,"hoi-world-tension-war-detail");
        context.runOnClient(client -> {
            var screen=(ScreenWorldTension)client.gui.screen();screen.onClose();
            if(screen.warId()!=null||client.gui.screen()!=screen)throw new AssertionError("Detail close must return to history");
            screen.update(new ProtocolWorldTension.Response(screen.token(),""));
            if(client.gui.screen()!=null)throw new AssertionError("Revoked assignment must close tension screen");
        });
    }
}
