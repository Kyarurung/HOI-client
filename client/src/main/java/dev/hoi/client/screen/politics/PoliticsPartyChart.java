package dev.hoi.client.screen.politics;

import dev.hoi.protocol.menu.MenuView;
import java.util.List;

public final class PoliticsPartyChart {
    private PoliticsPartyChart() {}
    public static int overlayHeight(int size) { return Math.round(size * 87 / 90f); }
    public static double centerX(int size) { return size * 45 / 90.0; }
    public static double centerY(int size) { return (size - size * 87 / 90.0) / 2 + size * 44 / 90.0; }
    public static double radius(int size) { return size * 39 / 90.0; }
    public static double angle(double dx, double dy) { return (Math.atan2(-dy, dx) + Math.PI * 2) % (Math.PI * 2) / (Math.PI * 2); }
    public static MenuView.Entry at(List<MenuView.Entry> parties,double dx,double dy,double radius) {
        if(radius<=0||dx*dx+dy*dy>=radius*radius)return null;
        double angle=angle(dx,dy),sum=0;
        for(var party:parties)if(party.progress()>0) {
            sum+=party.progress();
            if(angle<sum)return party;
        }
        return null;
    }
}
