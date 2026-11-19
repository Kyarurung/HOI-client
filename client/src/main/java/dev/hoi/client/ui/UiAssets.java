package dev.hoi.client.ui;

import dev.hoi.client.screen.research.ResearchDetails;
import dev.hoi.client.screen.research.ResearchIcons;
import dev.hoi.protocol.research.ResearchView;
import dev.hoi.client.ui.country.CountryDisplays;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.tooltip.TooltipKeywordIcons;
import dev.hoi.client.ui.tooltip.Tooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.io.DataInputStream;
import java.io.IOException;

public final class UiAssets {
    private static final Map<Identifier, int[]> DIMENSIONS = new HashMap<>();
    private static final Map<Identifier, int[]> CONTENT_BOUNDS = new HashMap<>();
    private static final Map<Identifier, Identifier> GRAYSCALE = new HashMap<>();
    private static com.google.gson.JsonObject eventImageSizes;
    private UiAssets() {}
    public static void clear() { GRAYSCALE.values().forEach(id -> Minecraft.getInstance().getTextureManager().release(id)); GRAYSCALE.clear(); dev.hoi.client.ui.country.CountryFlags.clear(); eventImageSizes = null; UiStoryAssets.clear(); UiCommandLabels.clear(); DIMENSIONS.clear(); CONTENT_BOUNDS.clear(); ResearchDetails.clear(); TooltipKeywordIcons.clear(); HudMenuBar.clear(); IndustryEquipmentDesigns.clear(); Tooltips.clear(); dev.hoi.client.screen.decision.DecisionScreen.resourcesReloaded(); dev.hoi.client.screen.focus.FocusScreen.resourcesReloaded(); UiDiplomacyAssets.resourcesReloaded(); }
    public static int[] eventImageSize(String path) {
        if (eventImageSizes == null) {
            eventImageSizes = new com.google.gson.JsonObject();
            var resource = Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:ui/event_image_sizes.json"));
            if (resource.isPresent()) try (var input = resource.get().openAsReader()) {
                eventImageSizes = com.google.gson.JsonParser.parseReader(input).getAsJsonObject();
            } catch (IOException | IllegalStateException ignored) { }
        }
        var size = eventImageSizes.getAsJsonArray(path);
        return size != null && size.size() == 2 ? new int[]{size.get(0).getAsInt(), size.get(1).getAsInt()} : new int[]{0, 0};
    }
    public static int[] eventSuperOffset(String path) {
        eventImageSize(path);
        var offsets = eventImageSizes.getAsJsonObject("_super_event_offsets");
        var value = offsets == null ? null : offsets.getAsJsonArray(path);
        return value != null && value.size() == 2 ? new int[]{value.get(0).getAsInt(), value.get(1).getAsInt()} : new int[]{-491, -297};
    }
    public static boolean cover(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        return cover(g, path, x, y, w, h, false);
    }
    private static boolean cover(GuiGraphicsExtractor g, String path, int x, int y, int w, int h, boolean grayscale) {
        path = CountryDisplays.texture(path);
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 0 || size[1] <= 0) return false;
        if (grayscale && (id = GRAYSCALE.computeIfAbsent(id, UiAssets::grayscale)) == null) return false;
        double scale = Math.max((double)w / size[0], (double)h / size[1]);
        float u = (float)(w / (size[0] * scale)), v = (float)(h / (size[1] * scale));
        g.blit(id, x, y, x + w, y + h, (1 - u) / 2, (1 + u) / 2, (1 - v) / 2, (1 + v) / 2);
        return true;
    }
    public static boolean draw(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        return draw(g, path, x, y, w, h, 0, 0);
    }
    public static void drawLeaderControl(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,boolean enabled) {
        if(path.startsWith("commander/commands/idea_traits_strip/")) {
            draw(g,"commander/commands/empty_paper/frame_0",x,y,w,h);int insetX=Math.round(w*10.5f/49),insetY=Math.round(h*10.5f/49);x+=insetX;y+=insetY;w-=insetX*2;h-=insetY*2;
        }
        if(enabled)draw(g,path,x,y,w,h);else drawGrayscale(g,path,x,y,w,h);
    }
    public static int[] drawLeaderControlAnchored(GuiGraphicsExtractor g,String path,int edge,int bottom,int w,int h,boolean right,boolean enabled) {
        String background=path.startsWith("commander/commands/idea_traits_strip/")?"commander/commands/empty_paper/frame_0":path;
        var id=Identifier.parse("hoi:textures/gui/"+background+".png");
        int[] bounds=CONTENT_BOUNDS.computeIfAbsent(id,UiAssets::contentBounds);
        if(bounds.length==0){int x=right?edge-w:edge;drawLeaderControl(g,path,x,bottom-h,w,h,enabled);return new int[]{x,bottom-h,w,h};}
        double scale=Math.min((double)w/bounds[0],(double)h/bounds[1]);
        int fitW=Math.max(1,(int)Math.round(bounds[0]*scale)),fitH=Math.max(1,(int)Math.round(bounds[1]*scale));
        int insetX=(w-fitW)/2+(int)Math.round(bounds[2]*fitW/(double)bounds[0]),insetY=(h-fitH)/2+(int)Math.round(bounds[3]*fitH/(double)bounds[1]);
        int visibleW=(int)Math.round(bounds[4]*fitW/(double)bounds[0])-(int)Math.round(bounds[2]*fitW/(double)bounds[0]),visibleH=(int)Math.round(bounds[5]*fitH/(double)bounds[1])-(int)Math.round(bounds[3]*fitH/(double)bounds[1]);
        int x=right?edge-visibleW:edge,y=bottom-visibleH;
        drawLeaderControl(g,path,x-insetX,y-insetY,w,h,enabled);
        return new int[]{x,y,visibleW,visibleH};
    }
    public static int[] visibleBounds(String path,int x,int y,int w,int h) {
        var id=Identifier.parse("hoi:textures/gui/"+path+".png");
        int[] bounds=CONTENT_BOUNDS.computeIfAbsent(id,UiAssets::contentBounds);
        if(bounds.length==0)return new int[]{x,y,w,h};
        double scale=Math.min((double)w/bounds[0],(double)h/bounds[1]);
        int fitW=Math.max(1,(int)Math.round(bounds[0]*scale)),fitH=Math.max(1,(int)Math.round(bounds[1]*scale));
        int left=x+(w-fitW)/2,top=y+(h-fitH)/2;
        int x1=left+(int)Math.round(bounds[2]*fitW/(double)bounds[0]),y1=top+(int)Math.round(bounds[3]*fitH/(double)bounds[1]);
        int x2=left+(int)Math.round(bounds[4]*fitW/(double)bounds[0]),y2=top+(int)Math.round(bounds[5]*fitH/(double)bounds[1]);
        return new int[]{x1,y1,x2-x1,y2-y1};
    }
    public static boolean drawGrayscale(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        return draw(g, path, x, y, w, h, 0, 0, 0xFFFFFFFF, true);
    }
    public static boolean drawGrayscale(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,int trimLeft,int trimRight) {
        return draw(g,path,x,y,w,h,trimLeft,trimRight,0xFFFFFFFF,true);
    }
    public static boolean drawTinted(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,int tint) {
        return draw(g,path,x,y,w,h,0,0,tint);
    }
    public static boolean draw(GuiGraphicsExtractor g, String path, int x, int y, int w, int h, int trimLeft, int trimRight) {
        return draw(g,path,x,y,w,h,trimLeft,trimRight,0xFFFFFFFF);
    }
    private static boolean draw(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,int trimLeft,int trimRight,int tint) {
        return draw(g, path, x, y, w, h, trimLeft, trimRight, tint, false);
    }
    private static boolean draw(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,int trimLeft,int trimRight,int tint,boolean grayscale) {
        path = CountryDisplays.texture(path);
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 0 || size[1] <= 0) return false;
        if (path.startsWith("politics/appointments/") && size[0] == 156 && size[1] == 210) {
            float scale = Math.min(w / 62f, h / 67f);
            g.pose().pushMatrix();
            g.pose().translate(x + (w - 62 * scale) / 2, y + (h - 67 * scale) / 2);
            g.pose().scale(scale);
            draw(g, "politics/vacant", 0, 0, 62, 67, 0, 0, 0xFFFFFFFF, grayscale);
            g.pose().translate(30, 33);
            g.pose().rotate(-.07f);
            cover(g, path, -16, -23, 32, 46, grayscale);
            g.pose().popMatrix();
            return true;
        }
        int sourceW = size[0] - trimLeft - trimRight;
        if (sourceW <= 0) return false;
        if (grayscale && (id = GRAYSCALE.computeIfAbsent(id, UiAssets::grayscale)) == null) return false;
        double scale = Math.min((double) w / sourceW, (double) h / size[1]);
        int fitW = Math.max(1, (int)Math.round(sourceW * scale)), fitH = Math.max(1, (int)Math.round(size[1] * scale));
        int left = x + (w - fitW) / 2, top = y + (h - fitH) / 2;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,id,left,top,trimLeft,0,fitW,fitH,sourceW,size[1],size[0],size[1],tint);
        return true;
    }
    private static Identifier grayscale(Identifier source) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(source);
        if (resource.isEmpty()) return null;
        try (var input = resource.get().open()) {
            var image = com.mojang.blaze3d.platform.NativeImage.read(input);
            net.minecraft.client.renderer.texture.DynamicTexture texture;
            try {
                for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                    image.setPixel(x, y, UiDiplomacyAssets.shade(image.getPixel(x, y), UiDiplomacyAssets.Effect.BUTTON_STATE, UiDiplomacyAssets.State.DISABLED, 0));
                }
                texture = new net.minecraft.client.renderer.texture.DynamicTexture(() -> "HOI grayscale " + source, image);
            } catch (RuntimeException | Error failure) {
                image.close();
                throw failure;
            }
            var id = Identifier.fromNamespaceAndPath("hoi", "grayscale/" + source.getPath());
            try {
                Minecraft.getInstance().getTextureManager().register(id, texture);
            } catch (RuntimeException | Error failure) {
                texture.close();
                throw failure;
            }
            return id;
        } catch (IOException ignored) {
            return null;
        }
    }
    public static boolean drawBottom(GuiGraphicsExtractor g,String path,int x,int y,int width,int height) {
        int fittedHeight=Math.min(height,imageHeight(path,width));
        return fittedHeight>0&&draw(g,path,x,y+height-fittedHeight,width,fittedHeight);
    }
    public static int[] contentSize(String path) {
        var id=Identifier.tryParse("hoi:textures/gui/"+CountryDisplays.texture(path)+".png");
        int[] bounds=id==null?new int[0]:CONTENT_BOUNDS.computeIfAbsent(id,UiAssets::contentBounds);
        return bounds.length==0?imageSize(path):new int[]{bounds[4]-bounds[2],bounds[5]-bounds[3]};
    }
    public static boolean drawContent(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) {
        path = CountryDisplays.texture(path);
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return false;
        int[] bounds = CONTENT_BOUNDS.computeIfAbsent(id, UiAssets::contentBounds);
        if (bounds.length == 0) return false;
        int sourceW = bounds[4] - bounds[2], sourceH = bounds[5] - bounds[3];
        double scale = Math.min((double)w / sourceW, (double)h / sourceH);
        int fitW = Math.max(1, (int)Math.round(sourceW * scale)), fitH = Math.max(1, (int)Math.round(sourceH * scale));
        int left = x + (w - fitW) / 2, top = y + (h - fitH) / 2;
        g.blit(id, left, top, left + fitW, top + fitH, (float)bounds[2] / bounds[0], (float)bounds[4] / bounds[0], (float)bounds[3] / bounds[1], (float)bounds[5] / bounds[1]);
        return true;
    }
    public static void drawCropped(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,float scale) {
        int[] size=imageSize(path);
        if(size[0]<=0 || size[1]<=0 || scale<=0)return;
        g.enableScissor(x,y,x+w,y+h);
        g.pose().pushMatrix();
        g.pose().translate(x+(w-size[0]*scale)/2,y+(h-size[1]*scale)/2);
        g.pose().scale(scale);
        draw(g,path,0,0,size[0],size[1]);
        g.pose().popMatrix();
        g.disableScissor();
    }
    public static boolean drawAlignedContent(GuiGraphicsExtractor g,String path,String reference,float x,float y,float w,float h) {
        path = CountryDisplays.texture(path);
        reference = CountryDisplays.texture(reference);
        var id=Identifier.tryParse("hoi:textures/gui/"+path+".png");
        var anchor=Identifier.tryParse("hoi:textures/gui/"+reference+".png");
        if(id==null||anchor==null||w<=0||h<=0)return false;
        int[] bounds=CONTENT_BOUNDS.computeIfAbsent(anchor,UiAssets::contentBounds);
        int[] imageSize=DIMENSIONS.computeIfAbsent(id,UiAssets::dimensions);
        if(bounds.length==0||imageSize[0]!=bounds[0]||imageSize[1]!=bounds[1])return false;
        int contentW=bounds[4]-bounds[2],contentH=bounds[5]-bounds[3];
        float scale=Math.min(w/imageSize[0],h/imageSize[1]);
        float guiScale=Minecraft.getInstance().getWindow().getGuiScale();
        float left=x+w/2f-(bounds[2]+contentW/2f)*scale,top=y+h/2f-(bounds[3]+contentH/2f)*scale;
        g.pose().pushMatrix();
        g.pose().translate(Math.round(left*guiScale)/guiScale,Math.round(top*guiScale)/guiScale);
        g.pose().scale(scale);
        g.blit(id,0,0,imageSize[0],imageSize[1],0,1,0,1);
        g.pose().popMatrix();
        return true;
    }
    private static int[] contentBounds(Identifier id) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) try (var input = resource.get().open()) {
            var image = javax.imageio.ImageIO.read(input);
            if (image == null) return new int[0];
            int width = image.getWidth(), height = image.getHeight(), left = width, top = height, right = 0, bottom = 0;
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                if ((image.getRGB(x, y) >>> 24) == 0) continue;
                left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x + 1); bottom = Math.max(bottom, y + 1);
            }
            return right > left && bottom > top ? new int[]{width, height, left, top, right, bottom} : new int[0];
        } catch (IOException ignored) { }
        return new int[0];
    }
    public static void nineSlice(GuiGraphicsExtractor g, String path, int x, int y, int w, int h, int border, double scale) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null || w <= 0 || h <= 0) return;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        if (size[0] <= 2 * border || size[1] <= 2 * border) {draw(g, path, x, y, w, h); return;}
        int edge = Math.min(Math.min(w, h) / 2, Math.max(1, (int)Math.round(border * scale)));
        int[] xs = {x, x + edge, x + w - edge, x + w}, ys = {y, y + edge, y + h - edge, y + h};
        float[] us = {0, (float)border / size[0], 1 - (float)border / size[0], 1}, vs = {0, (float)border / size[1], 1 - (float)border / size[1], 1};
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++)
            g.blit(id, xs[col], ys[row], xs[col + 1], ys[row + 1], us[col], us[col + 1], vs[row], vs[row + 1]);
    }
    public static void tiledNineSlice(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,int border,double scale) {
        tiledNineSlice(g,path,x,y,w,h,border,scale,0);
    }
    public static void tiledNineSlice(GuiGraphicsExtractor g,String path,int x,int y,int w,int h,int border,double scale,int declared) {
        var id=Identifier.tryParse("hoi:textures/gui/"+path+".png");
        if(id==null || w<=0 || h<=0 || scale<=0)return;
        int[] size=DIMENSIONS.computeIfAbsent(id,UiAssets::dimensions);
        int declaredW=Math.max(declared,size[0]),declaredH=Math.max(declared,size[1]);
        if(size[0]<=declaredW-border || size[1]<=declaredH-border || declaredW<=2*border || declaredH<=2*border)return;
        int edge=Math.min(Math.min(w,h)/2,Math.max(1,(int)Math.round(border*scale)));
        int[] xs={x,x+edge,x+w-edge,x+w},ys={y,y+edge,y+h-edge,y+h};
        int[] us={0,border,declaredW-border,declaredW},vs={0,border,declaredH-border,declaredH};
        for(int row=0;row<3;row++)for(int col=0;col<3;col++) {
            int tileWidth=col==1?Math.max(1,(int)Math.round((us[2]-us[1])*scale)):xs[col+1]-xs[col];
            int tileHeight=row==1?Math.max(1,(int)Math.round((vs[2]-vs[1])*scale)):ys[row+1]-ys[row];
            if(tileWidth<=0 || tileHeight<=0)continue;
            for(int ty=ys[row];ty<ys[row+1];ty+=tileHeight)for(int tx=xs[col];tx<xs[col+1];tx+=tileWidth) {
                int tw=Math.min(tileWidth,xs[col+1]-tx),th=Math.min(tileHeight,ys[row+1]-ty);
                double u0=us[col],u1=us[col]+(us[col+1]-us[col])*(double)tw/tileWidth,v0=vs[row],v1=vs[row]+(vs[row+1]-vs[row])*(double)th/tileHeight;
                double shownU=Math.min(u1,size[0]),shownV=Math.min(v1,size[1]);
                int drawW=(int)Math.round(tw*(shownU-u0)/(u1-u0)),drawH=(int)Math.round(th*(shownV-v0)/(v1-v0));
                if(drawW<=0 || drawH<=0)continue;
                g.blit(id,tx,ty,tx+drawW,ty+drawH,(float)(u0/size[0]),(float)(shownU/size[0]),(float)(v0/size[1]),(float)(shownV/size[1]));
            }
        }
    }
    public static boolean drawExact(GuiGraphicsExtractor g,String path,float x,float y,float w,float h) {
        path=CountryDisplays.texture(path);
        var id=Identifier.tryParse("hoi:textures/gui/"+path+".png");
        if(id==null || w<=0 || h<=0)return false;
        int[] size=DIMENSIONS.computeIfAbsent(id,UiAssets::dimensions);
        if(size[0]<=0 || size[1]<=0)return false;
        g.pose().pushMatrix();
        g.pose().translate(x,y);
        g.pose().scale(w/size[0],h/size[1]);
        g.blit(id,0,0,size[0],size[1],0,1,0,1);
        g.pose().popMatrix();
        return true;
    }
    private static int[] dimensions(Identifier id) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isPresent()) try (var input = new DataInputStream(resource.get().open())) {
            if (input.readLong() == 0x89504E470D0A1A0AL && input.readInt() == 13 && input.readInt() == 0x49484452) {
                int w = input.readInt(), h = input.readInt();
                if (w > 0 && h > 0 && w <= 32768 && h <= 32768) return new int[]{w, h};
            }
        } catch (IOException ignored) { }
        return new int[]{0, 0};
    }
    public static String technologyPath(String country, ResearchView.Tech tech) {
        if (!tech.id().startsWith("hoi:")) return "";
        String common = "technology/" + tech.id().substring(4);
        if (country != null && country.matches("[A-Za-z0-9_]{1,16}")) {
            String national = "technology/country/" + country.toLowerCase(Locale.ROOT) + "/" + tech.id().substring(4);
            if (hasTexture(national)) return national;
        }
        return hasTexture(common) ? common : "";
    }
    public static int[] imageSize(String path) {
        path=UiStoryAssets.asset(path);
        var id=Identifier.tryParse("hoi:textures/gui/"+path+".png");
        return id==null?new int[2]:DIMENSIONS.computeIfAbsent(id,UiAssets::dimensions).clone();
    }
    public static int imageHeight(String path, int width) {
        path = UiStoryAssets.asset(path);
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        if (id == null) return 0;
        int[] size = DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions);
        return size[0] > 0 ? Math.max(1, Math.round(width * (float) size[1] / size[0])) : 0;
    }
    private static boolean hasTexture(String path) {
        var id = Identifier.tryParse("hoi:textures/gui/" + path + ".png");
        return id != null && DIMENSIONS.computeIfAbsent(id, UiAssets::dimensions)[0] > 0;
    }
    public static void technology(GuiGraphicsExtractor g, String country, ResearchView.Tech tech, int x, int y, int w, int h, int color) {
        String path = technologyPath(country, tech);
        if (!path.isEmpty() && draw(g, path, x, y, w, h)) return;
        if (draw(g, "category/" + tech.category().toLowerCase(Locale.ROOT), x, y, w, h)) return;
        ResearchIcons.fallback(g, tech.category(), x + w / 2 - 10, y + h / 2 - 8, color, 2);
    }
}
