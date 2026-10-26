package dev.hoi.client.ui;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

public final class UiTooltipSpacingTest {
    private UiTooltipSpacingTest() {}
    public static void verify(ClientGameTestContext context,String text) {
        context.runOnClient(client->{
            var font=client.font;
            var before=new ArrayList<FormattedCharSequence>();
            for(String line:UiTooltipText.colorLines(UiCommandLabels.withoutShortcuts(text))) {
                if(UiTooltipText.blank(line))before.add(FormattedCharSequence.EMPTY);
                else before.addAll(font.split(UiKeywordIcons.decorateLines(UiTooltips.officerText(line),false),300));
            }
            var after=UiTooltips.sourceLines(font,text,324);
            if(!visible(before).equals(visible(after)))throw new AssertionError("Spacing changed visible text, styles or line order");
            int first=0,last=after.size()-1;
            while(first<=last&&UiTooltips.blank(after.get(first)))first++;
            while(last>=first&&UiTooltips.blank(after.get(last)))last--;
            for(int i=first+1;i<last;i++)if(UiTooltips.blank(after.get(i))&&UiTooltips.blank(after.get(i-1)))throw new AssertionError("Consecutive interior blank lines remain");
        });
    }
    private static List<List<Object>> visible(List<FormattedCharSequence> lines) {
        var result=new ArrayList<List<Object>>();
        for(var line:lines)if(!UiTooltips.blank(line))line.accept((index,style,code)->{result.add(List.of(style,code));return true;});
        return result;
    }
    public static void components(ClientGameTestContext context) {
        context.runOnClient(client->{
            String glyph=UiKeywordIcons.decorateLines(Component.literal("£command_power"),false).getString();
            if(glyph.equals("£command_power"))throw new AssertionError("Command power glyph missing");
            for(var sample:Map.of("£command_power 35.00 만큼","35.00 만큼","£command_power의 지휘력이"," 지휘력이","£command_power. 지휘력을"," 지휘력을","£command_power만큼의","큼의").entrySet())if(!UiKeywordIcons.decorateLines(Component.literal(sample.getKey()),false).getString().equals(glyph+sample.getValue()))throw new AssertionError("Source icon delimiter: "+sample.getKey());
            var styled=Component.literal("£command_").append(Component.literal("power ").withColor(0xFFBD00)).append(Component.literal("35.00 만큼").withColor(0xA2B9CF));
            var decorated=UiKeywordIcons.decorateLines(styled,false);
            if(!decorated.getString().equals(glyph+"35.00 만큼"))throw new AssertionError("Split icon token");
            decorated.visit((style,part)->{if(part.contains("35.00")&&style.getColor().getValue()!=0xA2B9CF)throw new AssertionError("Icon delimiter changed following style");return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
            var text=Component.literal("heading\n").withColor(0xFFBD00).append(Component.literal("\n§r\n---\n")).append(Component.literal("body 0.00%\n\n").withColor(0xA2B9CF));
            var normalized=UiTooltips.sourceSpacing(text);
            if(!normalized.getString().equals("heading\n\nbody 0.00%\n\n"))throw new AssertionError("Component boundaries must collapse without trimming margins");
            var lines=UiTooltips.split(client.font,text,300);
            if(lines.stream().filter(line->!UiTooltips.blank(line)).count()!=2)throw new AssertionError("Styled body rows were lost");
            var shortcut=Component.literal("heading\n\n단축").withColor(0xFFBD00).append(Component.literal("키: Z\n\n\n").withColor(0xFF3232)).append(Component.literal("  body 0.00% 또는 £ctrl + £b").withColor(0xA2B9CF));
            var filtered=UiTooltips.sourceSpacing(shortcut);
            if(!filtered.getString().equals("heading\n  body 0.00%"))throw new AssertionError("Shortcuts spanning styled components remain");
            filtered.visit((style,part)->{if(part.contains("body")&&style.getColor().getValue()!=0xA2B9CF)throw new AssertionError("Shortcut removal changed source color");return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        });
    }
}
