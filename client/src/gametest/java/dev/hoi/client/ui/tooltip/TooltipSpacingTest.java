package dev.hoi.client.ui.tooltip;

import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.politics.PoliticsTooltip;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class TooltipSpacingTest {
    private TooltipSpacingTest() {}
    public static void verify(ClientGameTestContext context,String text) {
        context.runOnClient(client->{
            var font=client.font;
            var before=new ArrayList<FormattedCharSequence>();
            for(String line:TooltipText.colorLines(UiCommandLabels.withoutShortcuts(text))) {
                if(TooltipText.blank(line))before.add(FormattedCharSequence.EMPTY);
                else before.addAll(font.split(TooltipKeywordIcons.decorateLines(Tooltips.officerText(line),false),300));
            }
            var after=Tooltips.sourceLines(font,text,324);
            if(!visible(before).equals(visible(after)))throw new AssertionError("Spacing changed visible text, styles or line order");
            int first=0,last=after.size()-1;
            while(first<=last&&Tooltips.blank(after.get(first)))first++;
            while(last>=first&&Tooltips.blank(after.get(last)))last--;
            for(int i=first+1;i<last;i++)if(Tooltips.blank(after.get(i))&&Tooltips.blank(after.get(i-1)))throw new AssertionError("Consecutive interior blank lines remain");
        });
    }
    private static List<List<Object>> visible(List<FormattedCharSequence> lines) {
        var result=new ArrayList<List<Object>>();
        for(var line:lines)if(!Tooltips.blank(line))line.accept((index,style,code)->{result.add(List.of(style,code));return true;});
        return result;
    }
    public static void components(ClientGameTestContext context) {
        context.runOnClient(client->{
            String glyph=TooltipKeywordIcons.decorateLines(Component.literal("£command_power"),false).getString();
            if(glyph.equals("£command_power"))throw new AssertionError("Command power glyph missing");
            var nativeParty=Tooltips.officerText("§G녹색당§! 인기도가 §Y12.3%§! £GFX_GER_greens_texticon 미만");
            var repeatedParty=TooltipKeywordIcons.decorateLines(nativeParty);
            if(nativeParty.getString().codePoints().filter(point->point>=0xe000&&point<=0xf8ff).count()!=1
                    ||!visible(List.of(nativeParty.getVisualOrderText())).equals(visible(List.of(repeatedParty.getVisualOrderText()))))
                throw new AssertionError("Original inline party icons must retain their source position and styles without inferred duplicates");
            java.util.function.ToLongFunction<String> inferred=line->TooltipKeywordIcons.decorateLines(Component.literal(line)).getString().codePoints().filter(point->point>=0xe000&&point<=0xf8ff).count();
            if(inferred.applyAsLong("방어: 5")!=1||inferred.applyAsLong("녹색당 인기도")!=0||inferred.applyAsLong("인력: 100")!=0||inferred.applyAsLong("연구 속도: +5%")!=0)
                throw new AssertionError("Only stat labels receive an inferred icon");
            for(var sample:Map.of("£command_power 35.00 만큼"," 35.00 만큼","£command_power의 지휘력이","의 지휘력이","£command_power. 지휘력을",". 지휘력을","£command_power만큼의","만큼의","£command_power£ 끝"," 끝").entrySet())if(!TooltipKeywordIcons.decorateLines(Component.literal(sample.getKey()),false).getString().equals(glyph+sample.getValue()))throw new AssertionError("Source icon delimiter: "+sample.getKey());
            var styled=Component.literal("£command_").append(Component.literal("power ").withColor(0xFFBD00)).append(Component.literal("35.00 만큼").withColor(0xA2B9CF));
            var decorated=TooltipKeywordIcons.decorateLines(styled,false);
            if(!decorated.getString().equals(glyph+" 35.00 만큼"))throw new AssertionError("Split icon token");
            decorated.visit((style,part)->{if(part.contains("35.00")&&style.getColor().getValue()!=0xA2B9CF)throw new AssertionError("Icon delimiter changed following style");return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
            Tooltips.officerText("£command_power 제목",true).visit((style,part)->{
                if(part.contains(glyph)&&(!TooltipKeywordIcons.FONT.equals(style.getFont())||style.isBold()||style.isItalic()))throw new AssertionError("Source icon pixels must not inherit heading bold or italic");
                if(part.contains("제목")&&!style.isBold())throw new AssertionError("Source icon conversion must preserve heading bold");
                return Optional.empty();
            },net.minecraft.network.chat.Style.EMPTY);
            var text=Component.literal("heading\n").withColor(0xFFBD00).append(Component.literal("\n§r\n---\n")).append(Component.literal("body 0.00%\n\n").withColor(0xA2B9CF));
            var normalized=Tooltips.sourceSpacing(text);
            if(!normalized.getString().equals("heading\n\nbody 0.00%\n\n"))throw new AssertionError("Component boundaries must collapse without trimming margins");
            var lines=Tooltips.split(client.font,text,300);
            if(lines.stream().filter(line->!Tooltips.blank(line)).count()!=2)throw new AssertionError("Styled body rows were lost");
            if(lines.size()!=3||Tooltips.blank(lines.getFirst())||Tooltips.blank(lines.getLast()))throw new AssertionError("Tooltip margins must be removed");
            var separated=Component.literal("§f─").append(Component.literal(" — -§r\nheading\n§#123456— ─ -\nㅡㅡㅡ\n====\n\nbody 0.00%\n———"));
            var normalizedLines=Tooltips.split(client.font,separated,300);
            if(normalizedLines.size()!=3||!Tooltips.blank(normalizedLines.get(1)))throw new AssertionError("Mixed separators must collapse to one blank row");
            var shortcut=Component.literal("heading\n\n단축").withColor(0xFFBD00).append(Component.literal("키: Z\n\n\n").withColor(0xFF3232)).append(Component.literal("  body 0.00% 또는 £ctrl + £b").withColor(0xA2B9CF));
            var filtered=Tooltips.sourceSpacing(shortcut);
            if(!filtered.getString().equals("heading\n  body 0.00%"))throw new AssertionError("Shortcuts spanning styled components remain");
            filtered.visit((style,part)->{if(part.contains("body")&&style.getColor().getValue()!=0xA2B9CF)throw new AssertionError("Shortcut removal changed source color");return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        });
    }
    public static void geometry(ClientGameTestContext context) {
        class TooltipScreen extends net.minecraft.client.gui.screens.Screen {
            Tooltips.ScrolledTooltip shortTip,longTip;
            TooltipScreen(){super(Component.literal("Tooltip geometry regression"));}
            @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g,int mx,int my,float delta) {
                g.fill(0,0,width,height,0xFF24212A);
                shortTip=Tooltips.drawSourceScrollable(g,font,"§e연구§r",24,30,width,height,0);
                longTip=Tooltips.drawSourceScrollable(g,font,"\n§f— ─ -§r\n§e조정치§r\n---\nㅡㅡㅡ\n====\n\n───\n공격: §a+5.00%§r\n방어: §c-12.50%§r\n————",24,100,width,height,0);
                PoliticsTooltip.drawTextDetail(g,font,"§e연구§r",200,45,width,height,0);
            }
        }
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            var screen=context.computeOnClient(client->new TooltipScreen());context.setScreen(()->screen);context.waitTicks(3);
            context.runOnClient(client->{
                if(screen.shortTip.width()!=client.font.width(Tooltips.officerText("§e연구§r")))throw new AssertionError("Short tooltip must match its text width");
                var lines=Tooltips.sourceLines(client.font,"§f— ─ -§r\n§e조정치§r\n\n───\n공격: §a+5.00%§r\n방어: §c-12.50%§r\n————",184);
                if(screen.longTip.width()!=lines.stream().mapToInt(client.font::width).max().orElseThrow()||screen.longTip.height()!=4*(client.font.lineHeight+1))throw new AssertionError("Tooltip width or separator height");
            });
            context.takeScreenshot("hoi-tooltip-width-separators-synthetic-"+size[0]+"-gui2");
        }
        context.setScreen(()->null);
    }
}
