package dev.hoi.client.screen.focus;

import com.google.gson.GsonBuilder;
import dev.hoi.protocol.focus.FocusPayloads;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class FocusScreenPixelsTest {
    private static final String SHADER_SHA256="1ec6aba6ac18ccfcad5c5c69f87e0f95309780a6b248e6e86dc00b30c09123a7";
    private static final Map<String,String> ASSET_SHA256=Map.of(
            "progress_atlas","e6ce9c5c90cc6796c18b58c30b2466b32014389300ef3f75f4ed1c4155aad0b9",
            "progress_background","1d082d521145d754eff267ee627ee648e9ce7cf20c395744979f8edae5eaabc0",
            "progress_frame","2854e53eb821a4a75d199e7f81ac80a20a61f6af280a8e8f38cbc327c29203de",
            "paper","790639624396a8ee64b12ec4767580170ed2da9bb9b4ffe4451ce59703ed2447");
    private static final Method PROGRESS=method("progress",GuiGraphicsExtractor.class,FocusPayloads.Detail.class);
    private static final Method PAPER=method("paper",GuiGraphicsExtractor.class,FocusDetail.Bounds.class);
    private record State(String name,int elapsed,int duration) {}
    private record Region(int x,int y,int width,int height) {}
    private record Difference(long comparedPixels,long exactMismatchPixels,long overOneLevelPixels,int maximumChannelDelta,String firstMismatch) {}
    private FocusScreenPixelsTest() {}
    public static List<String> run(ClientGameTestContext context,int pixelWidth) {
        var assets=context.computeOnClient(FocusScreenPixelsTest::assets);
        var results=new ArrayList<Map<String,Object>>();
        var failures=new ArrayList<String>();
        Path destination=null;
        for(int background:List.of(0xFF000000,0xFFFFFFFF)) {
            String backgroundName=background==0xFF000000?"black":"white";
            for(var state:List.of(new State("0",0,24),new State("50",12,24),new State("100",24,24),
                    new State("quarter-texel-1",1,664),new State("quarter-texel-3",3,664),new State("quarter-texel-331",331,664),
                    new State("quarter-texel-661",661,664),new State("quarter-texel-663",663,664))) {
                var screen=context.computeOnClient(client->new PixelsScreen(state,background,false));
                context.setScreen(()->screen);
                context.getInput().setCursorPos(0,0);
                context.waitTicks(2);
                Region region=context.computeOnClient(client->screen.region(client));
                String name="hoi-focus-pixels-progress-"+state.name()+"-"+backgroundName+"-"+pixelWidth+"-gui2";
                Path screenshot=context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
                destination=screenshot.getParent();
                var expected=progress(assets,state,background);
                var actual=read(screenshot);
                check(actual.getWidth()==pixelWidth,"Actual screenshot width differs from the requested window width");
                var difference=compare(actual,region,expected,0);
                Path oracle=destination.resolve("cpu-oracle-"+name+".png");
                writeImage(expected,oracle);
                var result=new LinkedHashMap<String,Object>();
                result.put("kind","progress");result.put("capture",screenshot.toString());result.put("cpuOracle",oracle.toString());
                result.put("region",region);result.put("backgroundArgb",Integer.toHexString(background));
                result.put("elapsedHours",state.elapsed());result.put("durationHours",state.duration());
                result.put("sourceCurrentState",(float)state.elapsed()/state.duration());
                result.put("comparison",difference);result.put("passed",difference.exactMismatchPixels()==0);
                results.add(result);
                if(difference.exactMismatchPixels()!=0)failures.add(name+": "+difference.firstMismatch());
            }
            var screen=context.computeOnClient(client->new PixelsScreen(new State("paper",0,24),background,true));
            context.setScreen(()->screen);context.getInput().setCursorPos(0,0);context.waitTicks(2);
            Region region=context.computeOnClient(client->screen.region(client));
            String name="hoi-focus-pixels-paper-"+backgroundName+"-"+pixelWidth+"-gui2";
            Path screenshot=context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
            destination=screenshot.getParent();
            var actual=read(screenshot);
            var stretched=paper(assets.get("paper"),region.width(),region.height(),background,false);
            var repeated=paper(assets.get("paper"),region.width(),region.height(),background,true);
            var center=compare(actual,region,repeated,1);
            var corners=compare(actual,region,repeated,2);
            var result=new LinkedHashMap<String,Object>();
            result.put("kind","paper");result.put("capture",screenshot.toString());result.put("region",region);
            result.put("backgroundArgb",Integer.toHexString(background));result.put("center",center);result.put("corners",corners);
            result.put("edgeStretchCandidate",compare(actual,region,stretched,3));
            result.put("edgeRepeatCandidate",compare(actual,region,repeated,3));
            result.put("edgeVerdict","DEFERRED: tilingCenter=yes establishes center tiling; original engine edge UV behavior is unconfirmed.");
            result.put("centerAndCornersPassed",center.exactMismatchPixels()==0 && corners.exactMismatchPixels()==0);
            results.add(result);
            if(center.exactMismatchPixels()!=0 || corners.exactMismatchPixels()!=0)failures.add(name+": center/corner mismatch");
        }
        var report=new LinkedHashMap<String,Object>();
        report.put("evidence","Actual Fabric mainRenderTarget GPU screenshot compared to independent CPU source equations. CPU oracle PNG files are expected values, not GPU evidence.");
        report.put("guiScale",2);report.put("requestedPixelWidth",pixelWidth);report.put("assetsSha256",ASSET_SHA256);
        report.put("sourceShader","TFR gfx/FX/progress_startend.shader");report.put("sourceShaderSha256",SHADER_SHA256);
        report.put("sourceConversion","Pinned decoded PNG resources; ImageIO.getRGB exposes straight 8-bit ARGB. Original DDS/source files remain untouched.");
        report.put("sampling","Pixel-center UV; Point, no mip, Wrap. Atlas start/middle/stop are y=0..20/21..41/42..62. CurrentState stays float and is not rounded to filled width.");
        report.put("oracleScope","Source shader equations at normalized pixel centers; original HOI4 engine vertex UV/half-texel conventions still require native GPU capture. Exact half-texel states are not compared: at CurrentState*166 ending in .5 the float32 source equation selects different stop texels for individual pixels, so no stable expected image exists; quarter-texel states probe sub-texel progress instead. Passing compares Minecraft to this explicit oracle, not to an unobserved HOI4 framebuffer.");
        report.put("composition","lerp(middle,start,start.a), lerp(result,stop,stop.a), output alpha=middle.a, then SRC_ALPHA/INV_SRC_ALPHA; frame follows. CPU attachment channels round to nearest 8-bit integer.");
        report.put("threshold","RGB delta=0 for passing. One-level differences are counted separately for quantization diagnosis and never accepted automatically.");
        report.put("alphaEvidence","Screenshot RGB does not expose source alpha. Independent black/white background composites test transparency behavior; direct GPU attachment alpha is not claimed.");
        report.put("paperReference","TFR interface/countrytechtreeview.gfx:47-53; 192x192, border64, tilingCenter=yes. Center and corners are checked; edge repeat/stretch remains deferred.");
        report.put("referencePhotos","Existing seven source screenshots retain their prior visual scope; this fixture makes no new source layout claim.");
        report.put("cases",results);report.put("failures",failures);
        writeReport(destination.resolve("hoi-focus-pixels-"+pixelWidth+"-gui2.json"),report);
        return List.copyOf(failures);
    }
    private static Map<String,BufferedImage> assets(Minecraft client) {
        var result=new LinkedHashMap<String,BufferedImage>();
        for(var entry:ASSET_SHA256.entrySet()) {
            var id=Identifier.fromNamespaceAndPath("hoi","textures/gui/focus/detail/"+entry.getKey()+".png");
            try(var stream=client.getResourceManager().getResource(id).orElseThrow().open()) {
                byte[] bytes=stream.readAllBytes();
                String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
                check(sha.equals(entry.getValue()),"Unreviewed focus source asset: "+id+" SHA256="+sha);
                var image=ImageIO.read(new ByteArrayInputStream(bytes));
                check(image!=null,"Unable to decode focus source asset: "+id);
                result.put(entry.getKey(),image);
            } catch(IOException | NoSuchAlgorithmException failure) {throw new AssertionError("Unable to read focus source asset "+id,failure);}
        }
        dimensions(result.get("progress_atlas"),166,63);
        dimensions(result.get("progress_background"),166,21);
        dimensions(result.get("progress_frame"),175,30);
        dimensions(result.get("paper"),192,192);
        var atlas=result.get("progress_atlas");
        for(int y=0;y<21;y++)for(int x=0;x<166;x++) {
            check((atlas.getRGB(x,y)>>>24)==0,"Source start layer alpha changed");
            int alpha=atlas.getRGB(x,y+21)>>>24;
            int expected=y==0 || y==1 && (x==0 || x==165)?0:255;
            check(alpha==expected,"Source middle binary alpha mask changed at "+x+","+y);
        }
        return Map.copyOf(result);
    }
    private static BufferedImage progress(Map<String,BufferedImage> assets,State state,int background) {
        var result=new BufferedImage(175,30,BufferedImage.TYPE_INT_ARGB);
        var atlas=assets.get("progress_atlas");
        var empty=assets.get("progress_background");
        var frame=assets.get("progress_frame");
        float current=(float)state.elapsed()/state.duration();
        for(int y=0;y<30;y++)for(int x=0;x<175;x++) {
            int value=background;
            int px=x-4,py=y-2;
            if(px>=0 && px<166 && py>=0 && py<21) {
                float u=(px+.5f)/166f;
                if(u<=current) {
                    int start=atlas.getRGB(px,py),middle=atlas.getRGB(px,py+21);
                    int stop=atlas.getRGB(pointWrap(u+(1f-current),166),py+42);
                    float alpha=(middle>>>24)/255f;
                    int rgb=0xFF000000;
                    for(int shift:List.of(16,8,0)) {
                        float first=channel(middle,shift)+(channel(start,shift)-channel(middle,shift))*((start>>>24)/255f);
                        float mixed=first+(channel(stop,shift)-first)*((stop>>>24)/255f);
                        rgb|=Math.clamp(Math.round(mixed*alpha+channel(value,shift)*(1f-alpha)),0,255)<<shift;
                    }
                    value=rgb;
                } else value=over(empty.getRGB(px,py),value);
            }
            result.setRGB(x,y,over(frame.getRGB(x,y),value));
        }
        return result;
    }
    private static BufferedImage paper(BufferedImage source,int width,int height,int background,boolean repeatEdges) {
        var result=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            boolean centerX=x>=64 && x<width-64,centerY=y>=64 && y<height-64;
            int sx=paperCoordinate(x,width,centerY || repeatEdges);
            int sy=paperCoordinate(y,height,centerX || repeatEdges);
            result.setRGB(x,y,over(source.getRGB(sx,sy),background));
        }
        return result;
    }
    private static int paperCoordinate(int position,int size,boolean repeat) {
        if(position<64)return position;
        if(position>=size-64)return 192-size+position;
        return repeat?64+(position-64)%64:64+Math.min(63,(int)Math.floor((position-64+.5d)*64/(size-128)));
    }
    private static int pointWrap(float uv,int size) {
        float wrapped=uv-(float)Math.floor(uv);
        return Math.clamp((int)Math.floor(wrapped*size),0,size-1);
    }
    private static int over(int foreground,int background) {
        float alpha=(foreground>>>24)/255f;
        int rgb=0xFF000000;
        for(int shift:List.of(16,8,0))rgb|=Math.clamp(Math.round(channel(foreground,shift)*alpha+channel(background,shift)*(1f-alpha)),0,255)<<shift;
        return rgb;
    }
    private static int channel(int pixel,int shift){return pixel>>>shift&255;}
    private static Difference compare(BufferedImage actual,Region region,BufferedImage expected,int section) {
        check(region.x()>=0 && region.y()>=0 && region.x()+region.width()<=actual.getWidth() && region.y()+region.height()<=actual.getHeight(),"Pixel region outside actual framebuffer");
        check(region.width()==expected.getWidth() && region.height()==expected.getHeight(),"Oracle region dimensions differ");
        long compared=0,mismatch=0,overOne=0;
        int maximum=0;
        String first="";
        for(int y=0;y<region.height();y++)for(int x=0;x<region.width();x++) {
            boolean edgeX=x<64 || x>=region.width()-64,edgeY=y<64 || y>=region.height()-64;
            if(section==1 && (edgeX || edgeY) || section==2 && !(edgeX && edgeY) || section==3 && edgeX==edgeY)continue;
            int found=actual.getRGB(region.x()+x,region.y()+y),wanted=expected.getRGB(x,y),delta=0;
            for(int shift:List.of(16,8,0))delta=Math.max(delta,Math.abs(channel(found,shift)-channel(wanted,shift)));
            compared++;maximum=Math.max(maximum,delta);
            if(delta!=0) {
                mismatch++;
                if(first.isEmpty())first="local("+x+","+y+") expected="+Integer.toHexString(wanted)+" actual="+Integer.toHexString(found)+" delta="+delta;
            }
            if(delta>1)overOne++;
        }
        check(compared>0,"Pixel comparison region is empty");
        return new Difference(compared,mismatch,overOne,maximum,first);
    }
    private static Method method(String name,Class<?>... arguments) {
        try {
            var method=FocusDetail.class.getDeclaredMethod(name,arguments);
            method.setAccessible(true);
            return method;
        } catch(ReflectiveOperationException failure) {throw new ExceptionInInitializerError(failure);}
    }
    private static void invoke(Method method,Object receiver,Object... arguments) {
        try {method.invoke(receiver,arguments);}
        catch(InvocationTargetException failure) {throw new AssertionError("Production focus drawing failed",failure.getCause());}
        catch(ReflectiveOperationException failure) {throw new AssertionError("Unable to call production focus drawing",failure);}
    }
    private static BufferedImage read(Path path) {
        try {
            var image=ImageIO.read(path.toFile());check(image!=null,"Unreadable actual framebuffer PNG: "+path);return image;
        } catch(IOException failure) {throw new AssertionError("Unable to read actual framebuffer PNG",failure);}
    }
    private static void writeImage(BufferedImage image,Path path) {
        try {check(ImageIO.write(image,"png",path.toFile()),"PNG writer unavailable");}
        catch(IOException failure) {throw new AssertionError("Unable to write labelled CPU oracle",failure);}
    }
    private static void writeReport(Path path,Object report) {
        try {Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report),StandardCharsets.UTF_8);}
        catch(IOException failure) {throw new AssertionError("Unable to write focus GPU comparison report",failure);}
    }
    private static void dimensions(BufferedImage image,int width,int height){check(image.getWidth()==width && image.getHeight()==height,"Unexpected source image dimensions");}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static final class PixelsScreen extends Screen {
        private final FocusDetail panel=new FocusDetail();
        private final State state;
        private final int background;
        private final boolean paper;
        private FocusDetail.Bounds paperBounds;
        private PixelsScreen(State state,int background,boolean paper) {
            super(Component.literal("Focus source pixel verification"));
            this.state=state;this.background=background;this.paper=paper;
        }
        @Override protected void init() {
            panel.layout(width,height,65);
            var b=panel.bounds();
            int top=panel.description().y()-15;
            paperBounds=new FocusDetail.Bounds(b.x()+10,top,b.width()-20,b.y()+b.height()-top-6);
        }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
            graphics.fill(0,0,width,height,background);
            if(paper)invoke(PAPER,null,graphics,paperBounds);
            else {
                var detail=new FocusPayloads.Detail("pixel_fixture","","",state.duration(),state.elapsed(),0,0,0,"test-fixture/pixels",1,List.of(),List.of(),true,"",true);
                invoke(PROGRESS,panel,graphics,detail);
            }
        }
        private Region region(Minecraft client) {
            check(client.getWindow().getGuiScale()==2,"Pixel oracle requires actual GUI scale two");
            if(paper)return new Region(paperBounds.x()*2,paperBounds.y()*2,paperBounds.width()*2,paperBounds.height()*2);
            var b=panel.bounds();
            return new Region(b.x()*2+205,b.y()*2+57,175,30);
        }
    }
}
