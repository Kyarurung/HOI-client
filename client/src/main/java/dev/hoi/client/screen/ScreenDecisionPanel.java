package dev.hoi.client.screen;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.protocol.ProtocolDecisionPanel;
import java.io.DataInputStream;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

final class ScreenDecisionPanel {
    record Hit(String id,String immediate,String delayed,int x,int y,int width,int height) {}
    record Result(int height,List<Hit> hits) {}
    private record Rect(double x,double y,double width,double height) {
        Rect intersect(Rect other) {
            double left=Math.max(x,other.x),top=Math.max(y,other.y);
            return new Rect(left,top,Math.max(0,Math.min(x+width,other.x+other.width)-left),Math.max(0,Math.min(y+height,other.y+other.height)-top));
        }
    }
    private record Draw(ProtocolDecisionPanel.Element element,Rect bounds,Rect clip,String path,double fontScale,double lineHeight) {}
    private record FontMetrics(double size,double lineHeight) {}
    private final Map<String,int[]> dimensions=new HashMap<>();
    private final Map<String,FontMetrics> fonts=new HashMap<>();
    private record Progress(String foreground,String background,int width,int height,boolean horizontal,double scale) {}
    private final Map<String,Progress> progress=new HashMap<>();
    private final ScreenDecisionRadial radial=new ScreenDecisionRadial();
    void assets(com.google.gson.JsonObject root) {
        radial.configure(root);dimensions.clear();fonts.clear();progress.clear();
        if(root.get("progressBars") instanceof com.google.gson.JsonObject bars)for(var entry:bars.entrySet()) {
            var value=entry.getValue().getAsJsonObject();var textures=value.getAsJsonArray("textures");
            String foreground=textures.get(0).getAsJsonObject().get("path").getAsString(),background=textures.get(1).getAsJsonObject().get("path").getAsString();
            int width=value.get("width").getAsInt(),height=value.get("height").getAsInt();
            double scale=value.get("scale").getAsDouble();
            if(foreground.matches("[a-z0-9_/]+") && background.matches("[a-z0-9_/]+") && width>0 && height>0 && width<=4096 && height<=4096 && Double.isFinite(scale) && scale>0)
                progress.put(entry.getKey(),new Progress(foreground,background,width,height,value.get("horizontal").getAsBoolean(),scale));
        }
        if(root.get("fonts") instanceof com.google.gson.JsonObject entries)for(var entry:entries.entrySet()) {
            var value=entry.getValue().getAsJsonObject();
            double size=value.get("size").getAsDouble(),lineHeight=value.get("lineHeight").getAsDouble();
            if(Double.isFinite(size) && Double.isFinite(lineHeight) && size>=1 && size<=128 && lineHeight>=1 && lineHeight<=256)
                fonts.put(entry.getKey(),new FontMetrics(size,lineHeight));
        }
    }
    void clear() { dimensions.clear(); }
    void close() { radial.close();dimensions.clear();fonts.clear();progress.clear(); }
    Result render(GuiGraphicsExtractor graphics,Font font,ProtocolDecisionPanel panel,Map<String,String> assets,
            ScreenDecisionText text,int x,int y,int width) {
        if(panel==null || panel.elements().isEmpty())return new Result(0,List.of());
        var root=panel.elements().getFirst();
        double sourceWidth=measure(root.layout().width(),width,0),sourceHeight=measure(root.layout().height(),width,0);
        var bounds=new Rect(0,0,sourceWidth,sourceHeight);
        var draws=new ArrayList<Draw>();
        boolean coronavirus=panel.scriptedGui().equals("TFR_KOR_coronavirus");
        layout(root,bounds,new Rect(-1048576,-1048576,2097152,2097152),true,assets,font,text,draws,coronavirus);
        double left=0,top=0,right=sourceWidth,bottom=sourceHeight;
        for(var draw:draws) {
            if(!painted(draw.element()))continue;
            var paintedBounds=draw.bounds();
            if(draw.element().kind().equals("radial")) {
                double transparent=paintedBounds.height()*.482;
                paintedBounds=new Rect(paintedBounds.x(),paintedBounds.y()+transparent,paintedBounds.width(),paintedBounds.height()-transparent);
            }
            if(coronavirus && draw.element().kind().equals("text") && draw.element().text()!=null) {
                var lines=text.lines(font,draw.element().text().value(),Math.max(1,(int)(paintedBounds.width()/draw.fontScale())),Objects.requireNonNullElse(draw.element().layout().font(),"").endsWith("header"));
                double ink=lines.stream().mapToInt(font::width).max().orElse(0)*draw.fontScale();
                double offset=switch(Objects.requireNonNullElse(draw.element().layout().alignment(),"left")) {
                    case "center" -> (paintedBounds.width()-ink)/2;
                    case "right" -> paintedBounds.width()-ink;
                    default -> 0;
                };
                paintedBounds=new Rect(paintedBounds.x()+offset,paintedBounds.y(),ink,lines.size()*draw.lineHeight());
            }
            var visible=paintedBounds.intersect(draw.clip());
            if(visible.width()<=0 || visible.height()<=0)continue;
            left=Math.min(left,visible.x());top=Math.min(top,visible.y());
            right=Math.max(right,visible.x()+visible.width());bottom=Math.max(bottom,visible.y()+visible.height());
        }
        var canvas=new Rect(left,top,right-left,bottom-top);
        if(canvas.width()<=0 || canvas.height()<=0)return new Result(0,List.of());
        double scale=width/canvas.width();
        var hits=new ArrayList<Hit>();
        var hitNames=new HashMap<String,Integer>();
        for(var draw:draws) {
            var element=draw.element();var rectangle=draw.bounds();var clipped=rectangle.intersect(draw.clip()).intersect(canvas);
            if(clipped.width()<=0 || clipped.height()<=0)continue;
            var tooltip=element.tooltip();
            if(tooltip!=null && !Boolean.TRUE.equals(element.layout().alwaysTransparent())) {
                String immediate=tooltip.immediate()==null?"":tooltip.immediate().value();
                String delayed=tooltip.delayed()==null?"":tooltip.delayed().value();
                int instance=hitNames.merge(element.name(),1,Integer::sum)-1;
                String hitId=element.name()+(instance==0?"":"#"+instance);
                if(!immediate.isEmpty() || !delayed.isEmpty())hits.add(new Hit(hitId,immediate,delayed,
                        x+(int)Math.floor((clipped.x()-canvas.x())*scale),y+(int)Math.floor((clipped.y()-canvas.y())*scale),
                        Math.max(1,(int)Math.ceil(clipped.width()*scale)),Math.max(1,(int)Math.ceil(clipped.height()*scale))));
            }
            if(graphics==null)continue;
            var clip=draw.clip().intersect(canvas);
            graphics.enableScissor(x+(int)Math.floor((clip.x()-canvas.x())*scale),y+(int)Math.floor((clip.y()-canvas.y())*scale),
                    x+(int)Math.ceil((clip.x()+clip.width()-canvas.x())*scale),y+(int)Math.ceil((clip.y()+clip.height()-canvas.y())*scale));
            graphics.pose().pushMatrix();
            graphics.pose().translate((float)(x+(rectangle.x()-canvas.x())*scale),(float)(y+(rectangle.y()-canvas.y())*scale));
            graphics.pose().scale((float)scale);
            if(element.kind().equals("text") && element.text()!=null) {
                graphics.pose().scale((float)draw.fontScale());
                int available=Math.max(1,(int)Math.floor(rectangle.width()/draw.fontScale()));
                var lines=text.lines(font,element.text().value(),available,Objects.requireNonNullElse(element.layout().font(),"").endsWith("header"));
                for(int row=0;row<lines.size();row++) {
                    var line=lines.get(row);
                    double textLeft=switch(Objects.requireNonNullElse(element.layout().alignment(),"left")) {
                        case "center" -> (available-font.width(line))/2.0;
                        case "right" -> available-font.width(line);
                        default -> 0;
                    };
                    graphics.text(font,line,(int)Math.round(textLeft),(int)Math.round(row*draw.lineHeight()/draw.fontScale()),UiMenuStyle.TEXT);
                }
            } else if(progress.containsKey(element.sprite())) {
                var bar=progress.get(element.sprite());
                int w=Math.max(1,(int)Math.round(rectangle.width())),h=Math.max(1,(int)Math.round(rectangle.height()));
                graphics.pose().pushMatrix();
                if(!bar.horizontal()) {
                    graphics.pose().translate(0,h);
                    graphics.pose().rotate((float)(-Math.PI/2));
                    int previous=w;w=h;h=previous;
                }
                if(bar.horizontal())UiAssets.draw(graphics,bar.background(),0,0,w,h);
                else graphics.blit(Identifier.parse("hoi:textures/gui/"+bar.background()+".png"),0,0,w,h,0,1,1,0);
                if(element.frame()!=null) {
                    float fraction=(float)Math.clamp(element.frame()/bar.scale(),0,1);
                    int fill=(int)Math.round(w*fraction);
                    if(fill>0)graphics.blit(Identifier.parse("hoi:textures/gui/"+bar.foreground()+".png"),0,0,fill,h,0,(float)fill/w,bar.horizontal()?0:1,bar.horizontal()?1:0);
                }
                graphics.pose().popMatrix();
                if(element.frame()==null)graphics.text(font,"?",((int)Math.round(rectangle.width())-font.width("?"))/2,((int)Math.round(rectangle.height())-font.lineHeight)/2,UiMenuStyle.MUTED);
            } else if(element.kind().equals("radial") && radial.draw(graphics,element.sprite(),element.frame(),0,0,
                    Math.max(1,(int)Math.round(rectangle.width())),Math.max(1,(int)Math.round(rectangle.height())))) {
            } else if(element.kind().equals("background") && "GFX_tiled_window_transparent".equals(element.sprite())) {
                UiAssets.nineSlice(graphics,draw.path(),0,0,Math.max(1,(int)Math.round(rectangle.width())),Math.max(1,(int)Math.round(rectangle.height())),32,3.0/95);
            } else if(!draw.path().isEmpty()) {
                UiAssets.draw(graphics,draw.path(),0,0,Math.max(1,(int)Math.round(rectangle.width())),Math.max(1,(int)Math.round(rectangle.height())));
            } else if(element.kind().equals("radial") || element.frameVariable()!=null && element.frame()==null) {
                graphics.text(font,"?",Math.max(0,(int)(rectangle.width()-font.width("?"))/2),Math.max(0,(int)(rectangle.height()-font.lineHeight)/2),UiMenuStyle.MUTED);
            }
            graphics.pose().popMatrix();
            graphics.disableScissor();
        }
        return new Result((int)Math.ceil(canvas.height()*scale),List.copyOf(hits));
    }
    private static boolean painted(ProtocolDecisionPanel.Element element) {
        return element.kind().equals("text") || element.kind().equals("radial") || element.sprite()!=null;
    }
    private void layout(ProtocolDecisionPanel.Element element,Rect parent,Rect inheritedClip,boolean root,Map<String,String> assets,
            Font font,ScreenDecisionText text,List<Draw> draws,boolean coronavirus) {
        if(!Boolean.TRUE.equals(element.visible()))return;
        var layout=element.layout();
        String path=path(element,assets);
        var bar=progress.get(element.sprite());
        int[] size=bar!=null?new int[]{bar.width(),bar.height()}:element.kind().equals("radial")?radial.dimensions(element.sprite()):dimensions.computeIfAbsent(
                path.isEmpty() && element.sprite()!=null?assets.getOrDefault(element.sprite(),""):path,ScreenDecisionPanel::dimensions);
        double ownScale=Objects.requireNonNullElse(layout.scale(),1.0);
        var metrics=fonts.getOrDefault(layout.font(),new FontMetrics(font.lineHeight,font.lineHeight+1));
        double fontScale=(metrics.size()+(element.name().equals("NatAsm_member_text")?2:0))/font.lineHeight;
        double defaultWidth=element.kind().equals("text")?parent.width():size[0];
        double width=measure(layout.width(),parent.width(),defaultWidth)*ownScale;
        double defaultHeight=element.kind().equals("text") && element.text()!=null
                ?text.lines(font,element.text().value(),Math.max(1,(int)(width/fontScale)),Objects.requireNonNullElse(layout.font(),"").endsWith("header")).size()*metrics.lineHeight():size[1];
        double height=measure(layout.height(),parent.height(),defaultHeight)*ownScale;
        if(coronavirus && element.kind().equals("text") && !Boolean.TRUE.equals(layout.fixedSize()))height=Math.max(height,defaultHeight*ownScale);
        double left=parent.x(),top=parent.y();
        if(root) {width=parent.width();height=parent.height();}
        else {
            String anchor=Objects.requireNonNullElse(layout.orientation(),"upper_left").toLowerCase(Locale.ROOT);
            if(Set.of("center","center_up","center_down","upper_center","lower_center").contains(anchor))left+=parent.width()/2;
            else if(anchor.contains("right"))left+=parent.width();
            if(Set.of("center","center_left","center_right").contains(anchor))top+=parent.height()/2;
            else if(anchor.contains("down") || anchor.contains("lower"))top+=parent.height();
            left+=Objects.requireNonNullElse(layout.x(),0.0);top+=Objects.requireNonNullElse(layout.y(),0.0);
            if(Boolean.TRUE.equals(layout.centerPosition())) {left-=width/2;top-=height/2;}
        }
        var rectangle=new Rect(left,top,width,height);
        Rect clip=Boolean.TRUE.equals(layout.clipping())?inheritedClip.intersect(rectangle):inheritedClip;
        if(element.kind().equals("text") && (Boolean.TRUE.equals(layout.fixedSize()) || !coronavirus && layout.height()!=null))clip=clip.intersect(rectangle);
        draws.add(new Draw(element,rectangle,clip,path,fontScale,metrics.lineHeight()));
        for(var child:element.children())layout(child,rectangle,clip,false,assets,font,text,draws,coronavirus);
    }
    private static String path(ProtocolDecisionPanel.Element element,Map<String,String> assets) {
        if(element.sprite()==null || element.kind().equals("radial"))return "";
        String path=assets.getOrDefault(element.sprite(),"");
        if(element.frameVariable()!=null && element.frame()==null)return "";
        if(element.frame()!=null) {
            double frame=element.frame();
            if(frame<1 || frame!=Math.rint(frame))return "";
            if(path.endsWith("/frame_0"))path=path.substring(0,path.length()-1)+(long)(frame-1);
            else if(frame!=1)return "";
        }
        return path;
    }
    private static double measure(ProtocolDecisionPanel.Measure value,double parent,double fallback) {
        return value==null?fallback:value.relative()?value.value()*parent/100:value.value();
    }
    private static int[] dimensions(String path) {
        if(path.isEmpty())return new int[]{0,0};
        var identifier=Identifier.tryParse("hoi:textures/gui/"+path+".png");
        if(identifier==null)return new int[]{0,0};
        var resource=Minecraft.getInstance().getResourceManager().getResource(identifier);
        if(resource.isPresent())try(var input=new DataInputStream(resource.get().open())) {
            if(input.readLong()==0x89504E470D0A1A0AL && input.readInt()==13 && input.readInt()==0x49484452) {
                int width=input.readInt(),height=input.readInt();
                if(width>0 && height>0 && width<=32768 && height<=32768)return new int[]{width,height};
            }
        } catch(java.io.IOException ignored) { }
        return new int[]{0,0};
    }
}
