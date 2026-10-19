package dev.hoi.client.screen;

import com.google.gson.JsonObject;
import dev.hoi.client.ui.*;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

final class ScreenDecisionText {
    private static final Map<Character,Integer> SOURCE_COLORS=Map.ofEntries(
            Map.entry('C',0x23CEFF),Map.entry('L',0xA2B9CF),Map.entry('W',0xFFFFFF),Map.entry('B',0x3333FF),
            Map.entry('G',0x009F03),Map.entry('R',0xFF3232),Map.entry('b',0x000000),Map.entry('g',0xB0B0B0),
            Map.entry('Y',0xFFBD00),Map.entry('H',0xFFBD00),Map.entry('T',0xFFFFFF),Map.entry('O',0xFF7019),
            Map.entry('p',0xFB607F),Map.entry('d',0xA2231D),Map.entry('A',0xCD3737),Map.entry('P',0x3659D9),
            Map.entry('n',0x7C4324),Map.entry('s',0xFF0A66),Map.entry('a',0x414141),Map.entry('V',0x4D2C6E),
            Map.entry('y',0xC6C548),Map.entry('.',0xAA6234),Map.entry('I',0x9370DB),Map.entry('w',0xD3C900),
            Map.entry('l',0xFFF47D),Map.entry('0',0xD700F5),Map.entry('1',0x8078D3),Map.entry('2',0x5170F3),
            Map.entry('3',0x518FDC),Map.entry('4',0x5ABEE7),Map.entry('5',0x3FB5C2),Map.entry('6',0x77CCBA),
            Map.entry('7',0x99D199),Map.entry('8',0xCCA333),Map.entry('9',0xFCA97D),Map.entry('t',0xFF4C4D));
    record Run(String text,Integer color) {}
    private static final Pattern TOKENS = Pattern.compile("£([A-Za-z0-9_]+(?:\\|[0-9]+)?)£?|\\[([A-Z]{3})\\.GetFlag\\]");
    private static final FontDescription FLAG_FONT = new FontDescription.Resource(Identifier.parse("hoi:decision_flags"));
    private Map<String,String> flags=Map.of();
    private final Map<String,List<FormattedCharSequence>> cache = new LinkedHashMap<>();
    private final Map<String,List<Component>> sourceRows = new LinkedHashMap<>();
    private Map<String,Image> images=Map.of();
    private record Image(String path,int width,int height) {}
    private record Block(FormattedCharSequence text,Image image,int width,int height) {}
    void clear() { cache.clear();sourceRows.clear(); }
    void flags(JsonObject root) {
        var next=new HashMap<String,String>();
        if(root.has("version") && root.get("version").getAsInt()==1 && root.get("flags") instanceof JsonObject entries
                && entries.size()<=1024)for(var entry:entries.entrySet()) {
            String glyph=entry.getValue().getAsString();
            if(entry.getKey().matches("[A-Z]{3}") && glyph.length()==1 && glyph.charAt(0)>=0xe000 && glyph.charAt(0)<=0xf8ff)
                next.put(entry.getKey(),glyph);
        }
        flags=Map.copyOf(next);clear();
    }
    void assets(JsonObject root) {
        var next=new HashMap<String,Image>();
        if(root.has("textImages"))for(var entry:root.getAsJsonObject("textImages").entrySet()) {
            var value=entry.getValue().getAsJsonObject();
            String path=value.get("path").getAsString();
            int width=value.get("width").getAsInt(),height=value.get("height").getAsInt();
            if(entry.getKey().matches("[A-Za-z0-9_]+") && path.matches("[a-z0-9_/.-]+") && !path.contains("..")
                    && width>0 && height>0 && width<=4096 && height<=4096)next.put(entry.getKey(),new Image(path,width,height));
        }
        images=Map.copyOf(next);clear();
    }
    Component component(String text) {
        var result=Component.empty();
        for(var run:sourceRuns(text)) {
            var style=run.color()==null?Style.EMPTY:Style.EMPTY.withColor(run.color());
            String part=run.text();
            var matcher=TOKENS.matcher(part);
            int start=0;
            while(matcher.find()) {
                result.append(Component.literal(part.substring(start,matcher.start())).withStyle(style));
                var original=Component.literal(matcher.group()).withStyle(style);
                if(matcher.group(2)!=null) {
                    String glyph=flags.get(matcher.group(2));
                    result.append(glyph==null?original:Component.literal(glyph)
                            .withStyle(Style.EMPTY.withFont(FLAG_FONT).withColor(0xFFFFFF).withShadowColor(0)));
                } else {
                    var icon=UiKeywordIcons.decorateLines(original,false);
                    result.append(icon.getString().isEmpty()?original:icon);
                }
                start=matcher.end();
            }
            result.append(Component.literal(part.substring(start)).withStyle(style));
        }
        return result;
    }
    static List<Run> sourceRuns(String text) {
        var runs=new ArrayList<Run>();Integer color=null;
        var codes=new ArrayList<Character>();
        for(int index=0;index<text.length();) {
            int length=codeLength(text,index);
            if(length==0)index++;else {codes.add(text.charAt(index+1));index+=length;}
        }
        var balanced=new BitSet();int resets=0;
        for(int index=codes.size()-1;index>=0;index--) {
            if(codes.get(index)=='!')resets++;
            else if(resets>0) {balanced.set(index);resets--;}
        }
        var parents=new ArrayList<Integer>();int codeIndex=0;boolean scoped=false;
        String[] lines=text.split("\\R",-1);
        for(int row=0;row<lines.length;row++) {
            String line=lines[row];boolean separator=separator(line);int start=0,index=0;
            while(index<line.length()) {
                int length=codeLength(line,index);
                if(length==0){index++;continue;}
                if(!separator&&start<index)runs.add(new Run(line.substring(start,index),color));
                char code=line.charAt(index+1);
                if(code=='!') {color=parents.isEmpty()?null:parents.removeLast();scoped=!parents.isEmpty();}
                else {
                    if(balanced.get(codeIndex))parents.add(scoped?color:null);else parents.clear();
                    scoped=balanced.get(codeIndex);
                    color=code=='#'?Integer.valueOf(line.substring(index+2,index+8),16):SOURCE_COLORS.get(code);
                }
                codeIndex++;
                index+=length;start=index;
            }
            if(!separator&&start<line.length())runs.add(new Run(line.substring(start),color));
            if(row<lines.length-1)runs.add(new Run("\n",color));
        }
        return List.copyOf(runs);
    }
    private static boolean separator(String line) {
        var plain=new StringBuilder();
        for(int index=0;index<line.length();) {
            int length=codeLength(line,index);
            if(length>0)index+=length;else plain.append(line.charAt(index++));
        }
        return plain.toString().strip().matches("-{3,}");
    }
    private static int codeLength(String text,int index) {
        if(text.charAt(index)!='§'||index+1>=text.length())return 0;
        char code=text.charAt(index+1);
        if(code=='!'||SOURCE_COLORS.containsKey(code))return 2;
        return code=='#'&&index+8<=text.length()&&text.substring(index+2,index+8).matches("[0-9a-fA-F]{6}")?8:0;
    }
    List<Component> rows(String text) {
        var cached=sourceRows.get(text);
        if(cached!=null)return cached;
        var rows=new ArrayList<net.minecraft.network.chat.MutableComponent>();
        rows.add(Component.empty());
        component(text).visit((style,part)->{
            String[] lines=part.split("\\R",-1);
            for(int i=0;i<lines.length;i++) {
                if(i>0)rows.add(Component.empty());
                rows.getLast().append(Component.literal(lines[i]).withStyle(style));
            }
            return Optional.empty();
        },Style.EMPTY);
        List<Component> result=List.copyOf(rows);
        if(sourceRows.size()>=256)sourceRows.clear();
        sourceRows.put(text,result);
        return result;
    }
    List<FormattedCharSequence> lines(Font font,String text,int width) {
        String key=width+"\n"+text;
        var cached=cache.get(key);
        if(cached!=null)return cached;
        var result=new ArrayList<FormattedCharSequence>();
        for(var line:rows(text)) {
            var wrapped=font.split(line,Math.max(1,width));
            if(wrapped.isEmpty())result.add(FormattedCharSequence.EMPTY);
            else result.addAll(wrapped);
        }
        var lines=List.copyOf(result);
        if(cache.size()>=256)cache.clear();
        cache.put(key,lines);
        return lines;
    }
    UiTooltips.ScrolledTooltip tooltip(GuiGraphicsExtractor graphics,Font font,String text,int mouseX,int mouseY,
            int viewportWidth,int viewportHeight,int scroll) {
        var blocks=blocks(font,text,Math.max(1,Math.min(324,viewportWidth-20)));
        int width=Math.min(Math.max(1,viewportWidth-20),Math.max(80,blocks.stream().mapToInt(Block::width).max().orElse(80)));
        int total=blocks.stream().mapToInt(Block::height).sum();
        int height=Math.min(total,Math.max(1,viewportHeight-20)),maxScroll=Math.max(0,total-height);
        scroll=Math.clamp(scroll,0,maxScroll);
        int x=Math.clamp(mouseX+12,5,Math.max(5,viewportWidth-width-5));
        int y=Math.clamp(mouseY-12,5,Math.max(5,viewportHeight-height-5));
        graphics.nextStratum();
        UiTooltips.background(graphics,x-4,y-4,width+8,height+8);
        graphics.enableScissor(x,y,x+width,y+height);
        int offset=0;
        for(var block:blocks) {
            int top=y+offset-scroll;offset+=block.height();
            if(top>=y+height)break;
            if(top+block.height()<=y)continue;
            if(block.image()==null)graphics.text(font,block.text(),x,top,0xFFE0E0E0);
            else UiAssets.draw(graphics,block.image().path(),x+(width-block.width())/2,top,block.width(),block.height());
        }
        graphics.disableScissor();
        if(maxScroll>0) {
            int thumb=Math.max(8,height*height/total),thumbY=y+(height-thumb)*scroll/maxScroll;
            graphics.fill(x+width+1,thumbY,x+width+3,thumbY+thumb,UiTooltips.GOLD|0xFF000000);
        }
        return new UiTooltips.ScrolledTooltip(x,y,width,height,scroll,maxScroll);
    }
    private List<Block> blocks(Font font,String text,int width) {
        var result=new ArrayList<Block>();
        for(var row:rows(text)) {
            var matcher=TOKENS.matcher(row.getString().strip());
            Image image=matcher.matches() && matcher.group(1)!=null?images.get(matcher.group(1)):null;
            if(image!=null) {
                int imageWidth=Math.min(width,image.width());
                int imageHeight=Math.max(1,(int)Math.round(image.height()*(double)imageWidth/image.width()));
                result.add(new Block(null,image,imageWidth,imageHeight));
            } else {
                var lines=font.split(row,width);
                if(lines.isEmpty())result.add(new Block(FormattedCharSequence.EMPTY,null,0,font.lineHeight+1));
                else for(var line:lines)result.add(new Block(line,null,font.width(line),font.lineHeight+1));
            }
        }
        return result;
    }
}
