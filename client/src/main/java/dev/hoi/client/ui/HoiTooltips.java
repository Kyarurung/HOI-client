package dev.hoi.client.ui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;


public final class HoiTooltips {
    private HoiTooltips() {}

    public static List<FormattedCharSequence> lines(Font font, String text, int viewportWidth) {
        int width = Math.max(80, Math.min(300, viewportWidth - 24));
        var lines = new ArrayList<FormattedCharSequence>();
        for (String line : text.split("\\R", -1)) {
            if (line.isEmpty()) lines.add(Component.empty().getVisualOrderText());
            else lines.addAll(font.split(KeywordIcons.decorate(Component.literal(line)), width));
        }
        return List.copyOf(lines);
    }

    public static void requested(GuiGraphicsExtractor g, net.minecraft.client.gui.components.AbstractWidget button, net.minecraft.client.gui.components.Tooltip tooltip, int mx, int my) {
        if(!button.isMouseOver(mx,my)||tooltip==null)return;
        var client=net.minecraft.client.Minecraft.getInstance();
        var lines=tooltip.toCharSequence(client);
        var value=new StringBuilder();
        for(var line:lines)line.accept((i,style,code)->{value.appendCodePoint(code);return true;});
        String text=value.toString();
        if(!(text.startsWith("우선순위를")||text.equals("닫기")||text.startsWith("첩보장")||text.startsWith("모집된 요원")||text.startsWith("적에게")
                ||text.startsWith("즉시 배치")||text.startsWith("훈련 취소")||text.equals("편제를 목록에서 삭제합니다.")||text.contains("모든 라인 즉시 배치")))return;
        var font=client.font;float scale=UiText.scale(font);
        int w=(int)Math.ceil(lines.stream().mapToInt(font::width).max().orElse(0)*scale)+12,h=lines.size()*12+10;
        int x=Math.clamp(mx+12,4,Math.max(4,g.guiWidth()-w-4));int y=Math.clamp(my+12,4,Math.max(4,client.getWindow().getGuiScaledHeight()-h-4));
        g.nextStratum();g.fill(x,y,x+w,y+h,0xF0100812);g.outline(x,y,w,h,0xFF873B98);
        for(int i=0;i<lines.size();i++)UiText.text(g,font,lines.get(i),x+6,y+5+i*12,0xFFFFFFFF);
    }
    public static void draw(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        graphics.setTooltipForNextFrame(font, lines(font, text, graphics.guiWidth()), x, y);
    }
}
