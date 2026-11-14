package dev.hoi.client.ui.tooltip;

import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TooltipLayer {
    private static final ArrayList<Runnable> queued=new ArrayList<>();
    private static GuiGraphicsExtractor graphics;
    private static Runnable vanilla;
    private static boolean flushing;
    private TooltipLayer() {}
    public static boolean defer(GuiGraphicsExtractor target,Runnable draw) {
        if(flushing)return false;
        if(graphics!=target){queued.clear();vanilla=null;graphics=target;}
        var pose=new org.joml.Matrix3x2f(target.pose());
        queued.add(()->{target.pose().pushMatrix();target.pose().set(pose);try{draw.run();}finally{target.pose().popMatrix();}});
        return true;
    }
    public static boolean deferVanilla(GuiGraphicsExtractor target,int x,int y,float delta) {
        if(flushing)return false;
        if(graphics!=target){queued.clear();graphics=target;}
        vanilla=()->target.extractDeferredElements(x,y,delta);
        return true;
    }
    public static void flush() {
        if(graphics==null)return;
        flushing=true;
        try {
            for(var draw:queued)draw.run();
            if(vanilla!=null)vanilla.run();
            dev.hoi.client.ui.hud.HudTooltip.extractDeferred(graphics);
        } finally {queued.clear();vanilla=null;graphics=null;flushing=false;}
    }
}
