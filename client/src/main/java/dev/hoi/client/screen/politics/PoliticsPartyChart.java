package dev.hoi.client.screen.politics;

import dev.hoi.protocol.menu.MenuView;
import java.util.List;

public final class PoliticsPartyChart {
    private PoliticsPartyChart() {}
    public static MenuView.Entry at(List<MenuView.Entry> parties,double dx,double dy,double radius) {
        if(radius<=0||dx*dx+dy*dy>=radius*radius)return null;
        double angle=(Math.atan2(dy,dx)+Math.PI*2.5)%(Math.PI*2)/(Math.PI*2),sum=0;
        for(var party:parties)if(party.progress()>0) {
            sum+=party.progress();
            if(angle<sum)return party;
        }
        return null;
    }
}
