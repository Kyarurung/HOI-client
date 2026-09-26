package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolMenuView;
import java.util.List;

final class ScreenPartyChart {
    private ScreenPartyChart() {}
    static ProtocolMenuView.Entry at(List<ProtocolMenuView.Entry> parties,double dx,double dy,double radius) {
        if(radius<=0||dx*dx+dy*dy>=radius*radius)return null;
        double angle=(Math.atan2(dy,dx)+Math.PI*2.5)%(Math.PI*2)/(Math.PI*2),sum=0;
        for(var party:parties)if(party.progress()>0) {
            sum+=party.progress();
            if(angle<sum)return party;
        }
        return null;
    }
}
