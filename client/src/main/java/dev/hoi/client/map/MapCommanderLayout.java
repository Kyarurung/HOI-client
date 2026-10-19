package dev.hoi.client.map;

import dev.hoi.client.ui.UiUnits;

final class MapCommanderLayout {
    private MapCommanderLayout() {}
    record Cell(int x,int y,int width,int height) {}
    private static final double[] ARMY_LEFT={6.5,21.5,39,56.5,78,96.5,112,135.5,154,171,193.5,211.5,234.5,252.5,274.5,297};
    private static final double[] ARMY_RIGHT={21,39,56.5,73,94,112,128,152,170.5,187.5,210,228,251,269,291,313.5};
    static Cell armyCell(UiUnits units,int x,int y,int index){int left=x+units.size(ARMY_LEFT[index]),right=x+units.size(ARMY_RIGHT[index]);return new Cell(left,y+units.size(16),right-left,units.size(17));}
    static UiUnits units(int width,int height){return new UiUnits(width,height,1);}
    static int cardsBottom(UiUnits units){return units.height()-(MapCommanderHud.running()?0:units.size(82));}
    static int commandLeft(UiUnits units,boolean navy){int width=units.size(navy?266:320.5),center=(units.width()-width)/2;return MapCommanderHud.running()?center:Math.max(center,Math.min(units.size(navy?252:MapCommanderPanels.attachmentWings().isEmpty()?224:318),units.width()-width));}
    static int commandTop(UiUnits units,boolean navy){return cardsBottom(units)-units.size(100)-units.size(navy?50:39.5);}
    static int popupBottom(UiUnits units,boolean navy){return commandTop(units,navy)-units.size(navy?4:28);}
}
