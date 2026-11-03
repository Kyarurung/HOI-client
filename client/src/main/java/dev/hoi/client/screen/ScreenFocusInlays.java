package dev.hoi.client.screen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.ProtocolFocusPayloads;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

final class ScreenFocusInlays {
    private static Object resources;
    private static JsonObject windows=new JsonObject();
    private static JsonObject fonts=new JsonObject();
    private ScreenFocusInlays() {}
    static void clear(){resources=null;windows=new JsonObject();fonts=new JsonObject();}
    static JsonObject windows(){
        var manager=Minecraft.getInstance().getResourceManager();
        if(resources!=manager){
            resources=manager;windows=new JsonObject();fonts=new JsonObject();
            var resource=manager.getResource(Identifier.parse("hoi:ui/focus_inlays.json"));
            if(resource.isPresent())try(var input=resource.get().openAsReader()){
                var root=JsonParser.parseReader(input).getAsJsonObject();windows=root.getAsJsonObject("windows");fonts=root.has("fonts")?root.getAsJsonObject("fonts"):new JsonObject();
            }catch(IOException|IllegalStateException failure){windows=new JsonObject();}
        }
        return windows;
    }
    static void draw(GuiGraphicsExtractor g,List<ProtocolFocusPayloads.Inlay> inlays,float panX,float panY,float zoom){
        for(var inlay:inlays){
            var window=windows().getAsJsonObject(inlay.window());
            if(window==null)continue;
            float scale=zoom*.5f;
            for(var element:window.getAsJsonArray("images")){
                var image=element.getAsJsonObject();String name=image.get("name").getAsString();
                String sprite=inlay.images().stream().filter(value->value.name().equals(name)).map(ProtocolFocusPayloads.InlayImage::sprite).findFirst().orElse(image.get("sprite").getAsString());
                String path="focus/inlay/"+sprite.toLowerCase(Locale.ROOT)+"/frame_0";
                int[] dimensions=UiAssets.imageSize(path);
                if(dimensions[0]<=0||dimensions[1]<=0)continue;
                int x=Math.round(panX+(inlay.x()+image.get("x").getAsFloat())*scale);
                int y=Math.round(panY+(inlay.y()+image.get("y").getAsFloat())*scale);
                float imageScale=scale*image.get("scale").getAsFloat();
                int width=Math.round(dimensions[0]*imageScale),height=Math.round(dimensions[1]*imageScale);
                if(image.get("background").getAsBoolean()){
                    UiAssets.nineSlice(g,path,x,y,Math.round(window.getAsJsonArray("size").get(0).getAsFloat()*scale),Math.round(window.getAsJsonArray("size").get(1).getAsFloat()*scale),1,scale);
                    continue;
                }
                var bar=inlay.bars().stream().filter(value->value.name().equals(name)).findFirst().orElse(null);
                if(bar==null){if(!name.equals("cho_bar")&&!name.equals("lee_jun_bar"))UiAssets.draw(g,path,x,y,width,height);continue;}
                int filled=(int)Math.round(width*bar.progress());
                if(filled<=0)continue;
                boolean reverse=image.get("reverse").getAsBoolean();
                g.enableScissor(reverse?x+width-filled:x,y,reverse?x+width:x+filled,y+height);
                UiAssets.draw(g,path,x,y,width,height);
                g.disableScissor();
            }
            if(!window.has("texts"))continue;
            for(var element:window.getAsJsonArray("texts")){
                var box=element.getAsJsonObject();
                var text=inlay.texts().stream().filter(value->value.name().equals(box.get("name").getAsString())).findFirst().orElse(null);
                if(text!=null)drawText(g,text.text(),box,panX+inlay.x()*scale,panY+inlay.y()*scale,scale);
            }
        }
    }
    private static void drawText(GuiGraphicsExtractor g,String value,JsonObject box,float x,float y,float scale){
        var metrics=fonts.getAsJsonObject(box.get("font").getAsString());
        if(metrics==null)return;
        var glyphs=metrics.getAsJsonObject("glyphs");
        var runs=ScreenDecisionText.sourceRuns(value);
        String plain=runs.stream().map(ScreenDecisionText.Run::text).collect(java.util.stream.Collectors.joining());
        float width=0;
        for(char character:plain.toCharArray()){
            var glyph=glyphs.getAsJsonObject(String.valueOf(character));
            if(glyph==null){
                var font=Minecraft.getInstance().font;
                var text=new ScreenDecisionText().component(value);
                float textScale=metrics.get("size").getAsFloat()/font.lineHeight;
                float ink=font.width(text)*textScale,offset=offset(box,ink);
                g.pose().pushMatrix();g.pose().translate(x+(box.get("x").getAsFloat()+offset)*scale,y+box.get("y").getAsFloat()*scale);g.pose().scale(textScale*scale);
                g.text(font,text,0,0,0xFFFFFFFF,false);g.pose().popMatrix();return;
            }
            width+=glyph.get("xadvance").getAsFloat();
        }
        float cursor=offset(box,width);
        g.pose().pushMatrix();g.pose().translate(x+box.get("x").getAsFloat()*scale,y+box.get("y").getAsFloat()*scale);g.pose().scale(scale);
        var texture=Identifier.parse("hoi:textures/gui/"+metrics.get("texture").getAsString()+".png");
        for(var run:runs)for(char character:run.text().toCharArray()){
            var glyph=glyphs.getAsJsonObject(String.valueOf(character));
            int w=glyph.get("width").getAsInt(),h=glyph.get("height").getAsInt();
            g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,texture,Math.round(cursor)+glyph.get("xoffset").getAsInt(),glyph.get("yoffset").getAsInt(),glyph.get("x").getAsFloat(),glyph.get("y").getAsFloat(),w,h,w,h,metrics.get("atlasWidth").getAsInt(),metrics.get("atlasHeight").getAsInt(),0xFF000000|(run.color()==null?0xFFFFFF:run.color()));
            cursor+=glyph.get("xadvance").getAsFloat();
        }
        g.pose().popMatrix();
    }
    private static float offset(JsonObject box,float width){return switch(box.get("format").getAsString()){case "center","centre"->(box.get("width").getAsFloat()-width)/2;case "right"->box.get("width").getAsFloat()-width;default->0;};}
}
