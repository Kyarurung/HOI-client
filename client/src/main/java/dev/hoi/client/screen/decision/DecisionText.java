package dev.hoi.client.screen.decision;

import com.google.gson.JsonObject;
import dev.hoi.client.ui.*;
import dev.hoi.client.ui.tooltip.*;
import java.util.*;
import java.util.regex.Pattern;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

public final class DecisionText {
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
    public record Run(String text,Integer color) {}
    private static final Pattern TOKENS = Pattern.compile("£([A-Za-z0-9_]+(?:\\|[0-9]+)?)£?|\\[([A-Z]{3})\\.GetFlag\\]");
    private static final FontDescription FLAG_FONT = new FontDescription.Resource(Identifier.parse("hoi:decision_flags"));
    private Map<String,String> flags=Map.of();
    private Map<Integer,String> flagCountries=Map.of();
    private final Map<String,List<FormattedCharSequence>> cache = new LinkedHashMap<>();
    private final Map<String,List<Component>> sourceRows = new LinkedHashMap<>();
    private Map<String,Image> images=Map.of();
    private Set<String> sprites=Set.of();
    private record Image(String path,int width,int height,boolean namedPortrait,boolean banner) {}
    record Block(FormattedCharSequence text,Image image,int width,int height,int captionOffset) {}
    public void clear() { cache.clear();sourceRows.clear(); }
    public void flags(JsonObject root) {
        var next=new HashMap<String,String>();
        if(root.has("version") && root.get("version").getAsInt()==1 && root.get("flags") instanceof JsonObject entries
                && entries.size()<=1024)for(var entry:entries.entrySet()) {
            String glyph=entry.getValue().getAsString();
            if(entry.getKey().matches("[A-Z]{3}") && glyph.length()==1 && glyph.charAt(0)>=0xe000 && glyph.charAt(0)<=0xf8ff)
                next.put(entry.getKey(),glyph);
        }
        flags=Map.copyOf(next);
        var countries=new HashMap<Integer,String>();
        next.forEach((country,glyph)->countries.put((int)glyph.charAt(0),country));
        flagCountries=Map.copyOf(countries);clear();
    }
    public void assets(JsonObject root) {
        var next=new HashMap<String,Image>();
        if(root.has("textImages"))for(var entry:root.getAsJsonObject("textImages").entrySet()) {
            var value=entry.getValue().getAsJsonObject();
            String path=value.get("path").getAsString();
            int width=value.get("width").getAsInt(),height=value.get("height").getAsInt();
            if(entry.getKey().matches("[A-Za-z0-9_]+") && path.matches("[a-z0-9_/.-]+") && !path.contains("..")
                    && width>0 && height>0 && width<=4096 && height<=4096)next.put(entry.getKey(),new Image(path,width,height,value.has("source")&&value.get("source").getAsString().contains("/texticon_portraits/"),value.has("source")&&value.get("source").getAsString().contains("/texticons/banners/")));
        }
        images=Map.copyOf(next);
        sprites=root.get("sprites") instanceof JsonObject entries?Set.copyOf(entries.keySet()):Set.of();
        clear();
    }
    public JsonObject load(net.minecraft.server.packs.resources.ResourceManager resources) {
        flags(new JsonObject());
        var flagFile=resources.getResource(Identifier.parse("hoi:ui/decision_flags.json"));
        if(flagFile.isPresent())try(var input=flagFile.get().open()) {
            byte[] bytes=input.readNBytes(262145);
            if(bytes.length<=262144)flags(com.google.gson.JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        } catch(Exception invalid) {flags(new JsonObject());}
        var resource=resources.getResource(Identifier.parse("hoi:ui/decision_assets.json"));
        if(resource.isEmpty())return null;
        try(var input=resource.get().open()) {
            byte[] bytes=input.readNBytes(4194305);
            if(bytes.length>4194304)return null;
            var root=com.google.gson.JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            if(root.get("version").getAsInt()!=1)return null;
            assets(root);
            return root;
        } catch(Exception invalid) {return null;}
    }
    public Component component(String text) {
        return Tooltips.sourceSpacing(sourceComponent(text,false));
    }
    public void draw(GuiGraphicsExtractor graphics,Font font,FormattedCharSequence line,int x,int y,int color,boolean shadow) {
        boolean plain=line.accept((index,style,codepoint)->!FLAG_FONT.equals(style.getFont())||!flagCountries.containsKey(codepoint));
        if(plain){graphics.text(font,line,x,y,color,shadow);return;}
        var parts=new ArrayList<FormattedCharSequence>();
        int[] left={x};
        line.accept((index,style,codepoint)->{
            var glyph=FormattedCharSequence.codepoint(codepoint,style);
            String country=FLAG_FONT.equals(style.getFont())?flagCountries.get(codepoint):null;
            if(country==null)parts.add(glyph);
            else {
                if(!parts.isEmpty()) {
                    var text=FormattedCharSequence.composite(List.copyOf(parts));
                    graphics.text(font,text,left[0],y,color,shadow);left[0]+=font.width(text);parts.clear();
                }
                String flag=dev.hoi.client.ui.country.CountryDisplays.flag(country);
                if(flag.equals("country/"+country.toLowerCase(Locale.ROOT)+"/flag"))graphics.text(font,glyph,left[0],y,0xFFFFFFFF,false);
                else UiAssets.drawContent(graphics,flag,left[0],y,Math.max(1,font.width(glyph)-1),font.lineHeight);
                left[0]+=font.width(glyph);
            }
            return true;
        });
        if(!parts.isEmpty())graphics.text(font,FormattedCharSequence.composite(parts),left[0],y,color,shadow);
    }
    void scrollingCell(GuiGraphicsExtractor graphics,Font font,Component value,int x,int y,int width,int height,int color,boolean centered,long elapsedMillis) {
        value=TooltipKeywordIcons.decorateLines(value,false);
        var sequence=value.getVisualOrderText();
        var bounds=font.prepareText(sequence,0,0,color,false,false,0).bounds();
        int inset=bounds==null?0:Math.min(0,bounds.left());
        int textWidth=Math.max(font.width(value),bounds==null?0:bounds.right())-inset,overflow=Math.max(0,textWidth-width);
        int left=overflow>0?x-UiText.scrollingOffset(overflow,elapsedMillis):centered?x+(width-textWidth)/2:x;
        graphics.enableScissor(x,y,x+width,y+height);
        draw(graphics,font,sequence,left-inset,Math.round(UiText.centeredY(font,sequence,y,height,1)),color,false);
        graphics.disableScissor();
    }
    private Component sourceComponent(String text,boolean preserveImages) {
        var result=Component.empty();
        var runs=sourceRuns(text);
        String[] lines=preserveImages?runs.stream().map(Run::text).collect(java.util.stream.Collectors.joining()).split("\n",-1):new String[0];
        int lineIndex=0;
        for(var run:runs) {
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
                            .withStyle(Style.EMPTY.withFont(FLAG_FONT).withColor(0xFFFFFF).withShadowColor(0).withBold(false)));
                } else {
                    boolean image=preserveImages && lines[lineIndex].strip().equals(matcher.group()) && (images.containsKey(matcher.group(1))
                            || sprites.contains(matcher.group(1)) || sprites.contains("GFX_"+matcher.group(1)));
                    result.append(image?original:TooltipKeywordIcons.decorateLines(original,false));
                }
                start=matcher.end();
            }
            result.append(Component.literal(part.substring(start)).withStyle(style));
            if(part.equals("\n"))lineIndex++;
        }
        return result;
    }
    public static List<Run> sourceRuns(String text) {
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
    public List<Component> rows(String text) {
        return TooltipText.collapseBlankLines(sourceRows(text),row->TooltipText.blank(row.getString()));
    }
    List<Component> sourceRows(String text) {
        var cached=sourceRows.get(text);
        if(cached!=null)return cached;
        var rows=new ArrayList<net.minecraft.network.chat.MutableComponent>();
        rows.add(Component.empty());
        sourceComponent(text,true).visit((style,part)->{
            String[] lines=part.split("\\R",-1);
            for(int i=0;i<lines.length;i++) {
                if(i>0)rows.add(Component.empty());
                rows.getLast().append(Component.literal(lines[i]).withStyle(style));
            }
            return Optional.empty();
        },Style.EMPTY);
        List<Component> result=rows.stream().<Component>map(row->TooltipText.blank(row.getString())?Component.empty():row).toList();
        if(sourceRows.size()>=256)sourceRows.clear();
        sourceRows.put(text,result);
        return result;
    }
    List<FormattedCharSequence> lines(Font font,String text,int width) {
        return lines(font,text,width,false);
    }
    List<FormattedCharSequence> lines(Font font,String text,int width,boolean bold) {
        String key=width+"\n"+bold+"\n"+text;
        var cached=cache.get(key);
        if(cached!=null)return cached;
        var result=new ArrayList<FormattedCharSequence>();
        for(var line:rows(text)) {
            var value=TooltipKeywordIcons.decorateLines(line,false);
            var wrapped=font.split(bold?value.copy().withStyle(net.minecraft.ChatFormatting.BOLD):value,Math.max(1,width));
            if(wrapped.isEmpty())result.add(FormattedCharSequence.EMPTY);
            else result.addAll(wrapped);
        }
        var lines=List.copyOf(result);
        if(cache.size()>=256)cache.clear();
        cache.put(key,lines);
        return lines;
    }
    public Tooltips.ScrolledTooltip tooltip(GuiGraphicsExtractor graphics,Font font,String text,int mouseX,int mouseY,
            int viewportWidth,int viewportHeight,int scroll) {
        int imageWidth=0;
        for(var row:sourceRows(text)) {
            var matcher=TOKENS.matcher(row.getString().strip());
            var image=matcher.matches()&&matcher.group(1)!=null?images.get(matcher.group(1)):null;
            if(image!=null)imageWidth=Math.max(imageWidth,Math.max(1,(int)Math.round(image.width()/2d)));
        }
        int limit=Math.min(Tooltips.width(viewportWidth,8),imageWidth>0?imageWidth:Integer.MAX_VALUE);
        var blocks=blocks(font,text,limit);
        int width=imageWidth>0?limit:Math.min(limit,Math.max(1,blocks.stream().mapToInt(Block::width).max().orElse(1)));
        int total=blocks.stream().mapToInt(Block::height).sum();
        int height=Math.min(total,Math.max(1,viewportHeight-20)),maxScroll=Math.max(0,total-height);
        scroll=Math.clamp(scroll,0,maxScroll);
        int x=Math.clamp(mouseX+12,5,Math.max(5,viewportWidth-width-5));
        int y=Math.clamp(mouseY-12,5,Math.max(5,viewportHeight-height-5));
        int requestedScroll=scroll;
        if(TooltipLayer.defer(graphics,()->tooltip(graphics,font,text,mouseX,mouseY,viewportWidth,viewportHeight,requestedScroll)))return new Tooltips.ScrolledTooltip(x,y,width,height,scroll,maxScroll);
        graphics.nextStratum();
        Tooltips.background(graphics,x-4,y-4,width+8,height+8);
        graphics.enableScissor(x,y,x+width,y+height);
        int offset=0;
        for(var block:blocks) {
            int top=y+offset-scroll;offset+=block.height();
            if(top>=y+height)break;
            if(top+block.height()<=y)continue;
            if(block.image()==null)draw(graphics,font,block.text(),x,top,0xFFE0E0E0,true);
            else {
                int imageX=x+(width-block.width())/2;
                if(block.image().banner())UiAssets.drawContent(graphics,block.image().path(),imageX,top,block.width(),block.height());
                else UiAssets.draw(graphics,block.image().path(),imageX,top,block.width(),block.height());
                if(block.text()!=null)draw(graphics,font,block.text(),imageX+block.captionOffset(),top+(int)Math.round(block.height()*235.0/258)-font.lineHeight/2,0xFFFFFFFF,true);
            }
        }
        graphics.disableScissor();
        if(maxScroll>0) {
            int thumb=Math.max(8,height*height/total),thumbY=y+(height-thumb)*scroll/maxScroll;
            graphics.fill(x+width+1,thumbY,x+width+3,thumbY+thumb,Tooltips.GOLD|0xFF000000);
        }
        return new Tooltips.ScrolledTooltip(x,y,width,height,scroll,maxScroll);
    }
    List<Block> blocks(Font font,String text,int width) {
        var result=new ArrayList<Block>();
        var rows=Tooltips.collapseBlankLines(sourceRows(text),value->TooltipText.blank(value.getString()));
        for(int index=0;index<rows.size();index++) {
            var row=rows.get(index);
            var matcher=TOKENS.matcher(row.getString().strip());
            Image image=matcher.matches() && matcher.group(1)!=null?images.get(matcher.group(1)):null;
            if(image!=null) {
                int imageWidth=Math.min(width,Math.max(1,(int)Math.round(image.width()/2d)));
                int imageHeight=Math.max(1,(int)Math.round(image.height()*(double)imageWidth/image.width()));
                if(image.banner()) {
                    int[] content=UiAssets.contentSize(image.path());
                    double scale=(double)imageWidth/image.width();
                    imageWidth=Math.max(1,(int)Math.round(content[0]*scale));
                    imageHeight=Math.max(1,(int)Math.round(content[1]*scale));
                }
                FormattedCharSequence caption=null;int captionOffset=0,next=index+1;
                while(next<rows.size()&&TooltipText.blank(rows.get(next).getString()))next++;
                if(image.namedPortrait()&&next<rows.size()) {
                    var name=rows.get(next);String plain=name.getString(),trimmed=plain.strip();
                    if(!trimmed.isEmpty()&&plain.startsWith(" ")) {
                        caption=name.getVisualOrderText();
                        captionOffset=(imageWidth-font.width(trimmed))/2-font.width(plain.substring(0,plain.length()-plain.stripLeading().length()));
                        index=next;
                    }
                }
                result.add(new Block(caption,image,imageWidth,imageHeight,captionOffset));
            } else {
                var lines=font.split(TooltipKeywordIcons.decorateLines(row,false),width);
                if(lines.isEmpty())result.add(new Block(FormattedCharSequence.EMPTY,null,0,font.lineHeight+1,0));
                else for(var line:lines)result.add(new Block(line,null,font.width(line),font.lineHeight+1,0));
            }
        }
        return Tooltips.collapseBlankLines(result,block->block.image()==null&&Tooltips.blank(block.text()));
    }
}
