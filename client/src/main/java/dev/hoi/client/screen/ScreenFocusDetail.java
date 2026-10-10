package dev.hoi.client.screen;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiKeywordIcons;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiTooltipText;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.ProtocolFocusPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ScreenFocusDetail {
    record Bounds(int x,int y,int width,int height) {
        boolean contains(double px,double py){return px>=x && py>=y && px<x+width && py<y+height;}
        int[] array(){return new int[]{x,y,width,height};}
    }
    private record Row(FormattedCharSequence text,int indent,ProtocolFocusPayloads.Assessment assessment) {}
    private static final float TEXT_SCALE=2f/3f;
    private Bounds bounds,requirements,description,rewards;
    private int requirementScroll,descriptionScroll,rewardScroll,requirementMaximum,descriptionMaximum,rewardMaximum;
    private Integer movedX,movedY;
    private Object rowSource;
    private Font rowFont;
    private int rowWidth;
    private boolean rowLoading;
    private List<Row> requirementRows=List.of(),descriptionRows=List.of(),rewardRows=List.of();
    void layout(int width,int height,int top) {
        int w=Math.min(275,Math.max(180,width-12));
        int h=Math.min(383,Math.max(160,height-Math.max(6,top-20)-6));
        int x=Math.clamp(movedX==null?250:movedX,6,Math.max(6,width-w-6));
        int y=Math.clamp(movedY==null?Math.max(50,top-20):movedY,6,Math.max(6,height-h-6));
        bounds=new Bounds(x,y,w,h);
        int header=Math.min(110,Math.max(80,h/3));
        int innerWidth=Math.round(w*485f/550),descriptionHeight=Math.max(25,120-Math.max(0,383-h)*240/455);
        requirements=new Bounds(x+Math.round(w*215f/550),y+50,Math.round(w*287f/550),Math.max(20,header-60));
        description=new Bounds(x+17,y+header+15,innerWidth,descriptionHeight);
        int rewardTop=description.y()+description.height()+25;
        rewards=new Bounds(x+25,rewardTop,Math.round(w*455f/550),Math.max(12,y+h-6-rewardTop));
    }
    Bounds bounds(){return bounds;}
    Bounds requirements(){return requirements;}
    Bounds description(){return description;}
    Bounds rewards(){return rewards;}
    Bounds start(){return new Bounds(bounds.x()+bounds.width()-80,bounds.y()+27,62,18);}
    Bounds close(){return new Bounds(bounds.x()+bounds.width()-21,bounds.y()+4,16,17);}
    boolean header(double x,double y){return bounds!=null && new Bounds(bounds.x(),bounds.y(),bounds.width(),22).contains(x,y);}
    void move(double dx,double dy,int width,int height,int top){movedX=bounds.x()+(int)Math.round(dx);movedY=bounds.y()+(int)Math.round(dy);layout(width,height,top);}
    void reset(){requirementScroll=0;descriptionScroll=0;rewardScroll=0;}
    void resetTree(){reset();movedX=null;movedY=null;}
    int scroll(){return descriptionScroll+rewardScroll;}
    boolean scroll(double x,double y,double amount) {
        if(bounds==null || !bounds.contains(x,y))return false;
        int delta=(int)Math.round(-amount*12);
        if(requirements.contains(x,y))requirementScroll=Math.clamp(requirementScroll+delta,0,requirementMaximum);
        else if(rewards.contains(x,y))rewardScroll=Math.clamp(rewardScroll+delta,0,rewardMaximum);
        else descriptionScroll=Math.clamp(descriptionScroll+delta,0,descriptionMaximum);
        return true;
    }
    void draw(GuiGraphicsExtractor g,Font font,ProtocolFocusPayloads.Node node,ProtocolFocusPayloads.Detail detail,boolean loading,int mx,int my) {
        var b=bounds;
        g.enableScissor(b.x(),b.y(),b.x()+b.width(),b.y()+b.height());
        UiMenuStyle.panel(g,b.x(),b.y(),b.width(),b.height());
        UiAssets.nineSlice(g,"commander/commands/tiled_window_thin_border2/frame_0",b.x(),b.y(),b.width(),b.height(),64,.5);
        g.pose().pushMatrix();g.pose().translate(b.x()+.5f,b.y()+.5f);g.pose().scale(.5f);
        UiAssets.draw(g,"focus/detail/top",0,0,b.width()*2-2,220);g.pose().popMatrix();
        int paperTop=description.y()-15;
        paper(g,new Bounds(b.x()+10,paperTop,b.width()-20,b.y()+b.height()-paperTop-6));
        drawCentered(g,font,detail==null?node.name():detail.name(),new Bounds(b.x()+15,b.y()+6,b.width()-50,15),8f/font.lineHeight);
        String art=node.icon().matches("[A-Za-z0-9_]+")?"politics/art/"+node.icon().toLowerCase(Locale.ROOT):"";
        boolean drawn=!art.isEmpty() && UiAssets.draw(g,art,b.x()+10,b.y()+30,70,70);
        if(!drawn && !art.isEmpty() && !node.icon().startsWith("GFX_"))drawn=UiAssets.draw(g,"politics/art/gfx_"+node.icon().toLowerCase(Locale.ROOT),b.x()+10,b.y()+30,70,70);
        if(!drawn)UiAssets.draw(g,"politics/empty/focus",b.x()+10,b.y()+30,70,70);
        Bounds time=new Bounds(b.x()+104,b.y()+30,85,7);
        progress(g,detail);
        String duration=detail==null || detail.durationHours()==0?"?":days(detail.durationHours())+"일";
        drawCentered(g,font,duration,time,TEXT_SCALE);
        if(time.contains(mx,my) && detail!=null)UiTooltips.drawExact(g,font,(detail.durationHours()==0?"소요 시간: 알 수 없음":duration+" · "+detail.durationHours()+"시간")+"\n경과: "+detail.elapsedHours()+"시간\n저장된 중점 시간: "+detail.bankedHours()+"시간\n정치력을 지불한 저장 시간: "+detail.paidBankedHours()+"시간\n일일 정치력 소모: "+BigDecimal.valueOf(detail.politicalPowerPerDay()).stripTrailingZeros().toPlainString(),mx,my);
        g.pose().pushMatrix();g.pose().translate(b.x()+102.5f,b.y()+46);g.pose().scale(.5f);
        UiAssets.draw(g,"focus/detail/prerequisites",0,0,307,113);g.pose().popMatrix();
        Object nextSource=detail==null?node:detail;
        if(rowSource!=nextSource || rowFont!=font || rowWidth!=b.width() || rowLoading!=loading) {
            rowSource=nextSource;rowFont=font;rowWidth=b.width();rowLoading=loading;
            String descriptionText=detail==null?node.description():detail.description();
            requirementRows=detail==null?textRows(font,loading?"상세 정보를 불러오는 중…":node.status(),requirements.width(),0,null):detailRows(font,detail.requirements(),requirements.width(),true);
            descriptionRows=textRows(font,descriptionText,description.width(),0,null);
            rewardRows=detail==null?List.of():detailRows(font,detail.rewards(),rewards.width(),false);
            if(detail!=null && !detail.limitation().isEmpty()) {
                var extended=new ArrayList<>(rewardRows);
                if(!extended.isEmpty())extended.add(new Row(FormattedCharSequence.EMPTY,0,null));
                extended.addAll(textRows(font,detail.limitation(),rewards.width(),0,null));rewardRows=List.copyOf(extended);
            }
        }
        requirementMaximum=maximum(font,requirementRows,requirements);descriptionMaximum=maximum(font,descriptionRows,description);rewardMaximum=maximum(font,rewardRows,rewards);
        requirementScroll=Math.clamp(requirementScroll,0,requirementMaximum);descriptionScroll=Math.clamp(descriptionScroll,0,descriptionMaximum);rewardScroll=Math.clamp(rewardScroll,0,rewardMaximum);
        drawRows(g,font,requirementRows,requirements,requirementScroll,requirementMaximum,false);
        drawRows(g,font,descriptionRows,description,descriptionScroll,descriptionMaximum,true);
        int labelY=rewards.y()-20;
        UiAssets.draw(g,"commander/commands/unit_list_header/frame_0",b.x()+42,labelY,b.width()-79,18);
        drawCentered(g,font,"효과",new Bounds(b.x()+25,labelY+3,b.width()-43,12),TEXT_SCALE);
        drawRows(g,font,rewardRows,rewards,rewardScroll,rewardMaximum,false);
        g.disableScissor();
    }
    private static void paper(GuiGraphicsExtractor g,Bounds area) {
        var id=Identifier.fromNamespaceAndPath("hoi","textures/gui/focus/detail/paper.png");
        if(Minecraft.getInstance().getResourceManager().getResource(id).isEmpty())return;
        int width=area.width()*2,height=area.height()*2,border=Math.min(64,Math.min(width,height)/2);
        int[] xs={0,border,width-border,width},ys={0,border,height-border,height};
        float[] uv={0,1f/3,2f/3,1};
        g.pose().pushMatrix();g.pose().translate(area.x(),area.y());g.pose().scale(.5f);
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)if(row!=1 || col!=1)g.blit(id,xs[col],ys[row],xs[col+1],ys[row+1],uv[col],uv[col+1],uv[row],uv[row+1]);
        for(int y=border;y<height-border;y+=64)for(int x=border;x<width-border;x+=64){int w=Math.min(64,width-border-x),h=Math.min(64,height-border-y);g.blit(id,x,y,x+w,y+h,1f/3,(64+w)/192f,1f/3,(64+h)/192f);}
        g.pose().popMatrix();
    }
    private void progress(GuiGraphicsExtractor g,ProtocolFocusPayloads.Detail detail) {
        var atlas=Identifier.fromNamespaceAndPath("hoi","textures/gui/focus/detail/progress_atlas.png");
        var background=Identifier.fromNamespaceAndPath("hoi","textures/gui/focus/detail/progress_background.png");
        int filled=detail==null || detail.durationHours()==0?0:(int)Math.clamp(Math.round(166d*detail.elapsedHours()/detail.durationHours()),0,166);
        g.pose().pushMatrix();g.pose().translate(bounds.x()+104.5f,bounds.y()+29.5f);g.pose().scale(.5f);
        if(filled<166 && Minecraft.getInstance().getResourceManager().getResource(background).isPresent())g.blit(background,filled,0,166,21,filled/166f,1,0,1);
        if(filled>0 && Minecraft.getInstance().getResourceManager().getResource(atlas).isPresent()) {
            g.blit(atlas,0,0,filled,21,0,filled/166f,1f/3,2f/3);
            if(filled>1)g.blit(atlas,1,1,Math.min(filled,165),2,(167-filled)/166f,(166-filled+Math.min(filled,165))/166f,43f/63,44f/63);
            g.blit(atlas,0,2,filled,21,(166-filled)/166f,1,44f/63,1);
        }
        g.pose().popMatrix();
        g.pose().pushMatrix();g.pose().translate(bounds.x()+102.5f,bounds.y()+28.5f);g.pose().scale(.5f);
        UiAssets.draw(g,"focus/detail/progress_frame",0,0,175,30);g.pose().popMatrix();
    }
    private static List<Row> detailRows(Font font,List<ProtocolFocusPayloads.DetailLine> source,int width,boolean indicators) {
        var rows=new ArrayList<Row>();
        for(var line:source)rows.addAll(textRows(font,line.text(),width,Math.min(width/2,line.depth()*7),indicators?line.assessment():null));
        return List.copyOf(rows);
    }
    private static List<Row> textRows(Font font,String text,int width,int indent,ProtocolFocusPayloads.Assessment assessment) {
        var rows=new ArrayList<Row>();
        boolean first=true;
        int textWidth=Math.max(16,(int)((width-indent-(assessment==null?0:8))/TEXT_SCALE));
        var component=UiKeywordIcons.decorateLines(UiTooltips.officerText(UiTooltipText.sourceSpacing(text)),false);
        var lines=new ArrayList<net.minecraft.network.chat.MutableComponent>();lines.add(net.minecraft.network.chat.Component.empty());
        component.visit((style,part) -> {
            String[] parts=part.split("\\R",-1);
            for(int i=0;i<parts.length;i++){if(i>0)lines.add(net.minecraft.network.chat.Component.empty());lines.getLast().append(net.minecraft.network.chat.Component.literal(parts[i]).withStyle(style));}
            return java.util.Optional.empty();
        },net.minecraft.network.chat.Style.EMPTY);
        for(var line:lines)for(var wrapped:line.getString().isEmpty()?List.of(FormattedCharSequence.EMPTY):font.split(line,textWidth)){rows.add(new Row(wrapped,indent+(first || assessment==null?0:8),first?assessment:null));first=false;}
        if(rows.isEmpty())rows.add(new Row(FormattedCharSequence.EMPTY,indent,assessment));
        return List.copyOf(rows);
    }
    private static int rowHeight(Font font){return Math.max(1,(int)Math.ceil(font.lineHeight*TEXT_SCALE));}
    private static int maximum(Font font,List<Row> rows,Bounds area){return Math.max(0,rows.size()*rowHeight(font)-area.height());}
    private static void drawRows(GuiGraphicsExtractor g,Font font,List<Row> rows,Bounds area,int scroll,int maximum,boolean centered) {
        g.enableScissor(area.x(),area.y(),area.x()+area.width(),area.y()+area.height());
        int y=area.y()-scroll;
        for(var row:rows) {
            if(y+rowHeight(font)>area.y() && y<area.y()+area.height()) {
                int iconSpace=row.assessment()==null?0:8;
                if(row.assessment()!=null)indicator(g,font,row.assessment(),area.x()+row.indent(),y);
                float x=centered?area.x()+(area.width()-font.width(row.text())*TEXT_SCALE)/2:area.x()+row.indent()+iconSpace;
                g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(TEXT_SCALE);g.text(font,row.text(),0,0,0xFFE0E0E0,false);g.pose().popMatrix();
            }
            y+=rowHeight(font);
        }
        g.disableScissor();
        if(maximum>0) {
            int height=Math.max(8,area.height()*area.height()/(maximum+area.height()));
            int top=area.y()+(area.height()-height)*scroll/maximum;
            g.fill(area.x()+area.width()+1,area.y(),area.x()+area.width()+3,area.y()+area.height(),0xFF202226);
            g.fill(area.x()+area.width()+1,top,area.x()+area.width()+3,top+height,0xFF787D86);
        }
    }
    private static void indicator(GuiGraphicsExtractor g,Font font,ProtocolFocusPayloads.Assessment assessment,int x,int y) {
        String symbol=switch(assessment){case MET -> "✓";case UNMET -> "×";case UNKNOWN -> "?";case NOT_EVALUATED -> "·";};
        int color=switch(assessment){case MET -> 0xFF009F03;case UNMET -> 0xFFFF3232;case UNKNOWN -> 0xFFFFBD00;case NOT_EVALUATED -> 0xFF8893A5;};
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(TEXT_SCALE);g.text(font,symbol,0,0,color,false);g.pose().popMatrix();
    }
    private static void drawCentered(GuiGraphicsExtractor g,Font font,String text,Bounds area,float scale) {
        var value=UiTooltips.officerText(text);
        float fit=Math.min(scale,area.width()/(float)Math.max(1,font.width(value)));
        g.pose().pushMatrix();g.pose().translate(area.x()+(area.width()-font.width(value)*fit)/2,area.y()+(area.height()-font.lineHeight*fit)/2);g.pose().scale(fit);g.text(font,value,0,0,0xFFE0E0E0,false);g.pose().popMatrix();
    }
    static String days(long hours){return BigDecimal.valueOf(hours).divide(BigDecimal.valueOf(24),3,RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
}
