package dev.hoi.client.map.commander;

import dev.hoi.client.ui.UiUnits;

final class CommanderLayout {
    private CommanderLayout() {}
    record Cell(int x,int y,int width,int height) {}
    record BadgeRow(float first,float step,boolean compressed) {float x(int index){return compressed?first+index*step:(float)Math.ceil(first+index*step);}}
    static BadgeRow navalBadges(UiUnits u,int slots,int width,float lastRight,float normalFirst,double pixelsPerGui){
        float step=u.length(15.5),first=u.length(normalFirst);if(slots<2||first+(slots-1)*step+lastRight+u.length(7.5)<=width)return new BadgeRow(first,step,false);
        double available=(width-u.length(1)-u.length(7.5)-lastRight-u.length(3.5))*pixelsPerGui;long pixels=Math.max(1,Math.round(available/(slots-1)));double remainder=available-pixels*(slots-1);return new BadgeRow((float)((Math.round(u.length(3.5)*pixelsPerGui)+Math.floor(remainder/2))/pixelsPerGui),(float)(pixels/pixelsPerGui),true);
    }
    private static final double[] ARMY_LEFT={6.5,21.5,39,56.5,78,96.5,112,135.5,154,171,193.5,211.5,234.5,252.5,274.5,297};
    private static final double[] ARMY_RIGHT={21,39,56.5,73,94,112,128,152,170.5,187.5,210,228,251,269,291,313.5};
    static Cell armyCell(UiUnits units,int x,int y,int index){int left=x+units.size(ARMY_LEFT[index]),right=x+units.size(ARMY_RIGHT[index]);return new Cell(left,y+units.size(16),right-left,units.size(17));}
    static UiUnits units(int width,int height){return new UiUnits(width,height,1);}
    static int cardsBottom(UiUnits units){return units.height();}
    static int commandLeft(UiUnits units,boolean navy){return (units.width()-units.size(navy?266:320.5))/2;}
    static int commandTop(UiUnits units,boolean navy){return cardsBottom(units)-units.size(navy?122.5:139.5)-(navy?units.size(1):0);}
    static int popupBottom(UiUnits units,boolean navy){return commandTop(units,navy)-units.size(navy?4:28);}
}
