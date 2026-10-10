package dev.hoi.client.map;

import dev.hoi.client.ui.UiUnits;

final class MapCommanderLayout {
    private MapCommanderLayout() {}
    record Cell(int x,int y,int width,int height) {}
    private static final double[] ARMY_LEFT={4,23.5,42,59,79.5,96,114,135.5,154,172.5,194,213,234.5,252.5,275,298};
    private static final double[] ARMY_RIGHT={21,40,57,73,94,112,129,151,170.5,187.5,210.5,228,251,268.5,291,314.5};
    static Cell armyCell(UiUnits units,int x,int y,int index){int left=x+units.size(ARMY_LEFT[index]),right=x+units.size(ARMY_RIGHT[index]);return new Cell(left,y+units.size(16),right-left,units.size(17));}
    static int cardsBottom(UiUnits units){return units.height()-Math.max(82,units.size(82));}
    static int commandTop(UiUnits units,boolean navy){return cardsBottom(units)-units.size(100)-units.size(navy?50:39.5);}
    static int popupBottom(UiUnits units,boolean navy){return commandTop(units,navy)-units.size(navy?4:28);}
}
