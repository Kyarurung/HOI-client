package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.industry.IndustryConstructionView.Summary;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class IndustryConstructionRows {
    private IndustryConstructionRows() {}
    static int count(Summary summary) {
        return 1+(summary.nationalProjects()!=null&&summary.nationalProjects()>0?1:0)+(summary.tradeGoods()!=null&&summary.tradeGoods()>0?1:0);
    }
    static void draw(GuiGraphicsExtractor g,Font font,Summary summary,int x,int y,int width,int height,int mx,int my) {
        row(g,font,"소비재","construction/consumer_goods",summary.consumer(),summary.consumerCalculation(),x,y,width,height,mx,my);
        y+=height;
        if(summary.nationalProjects()!=null&&summary.nationalProjects()>0) {
            row(g,font,"국가 계획","construction/ui/national_projects",summary.nationalProjects(),summary.projectsCalculation(),x,y,width,height,mx,my);y+=height;
        }
        if(summary.tradeGoods()!=null&&summary.tradeGoods()>0)
            row(g,font,"무역 상품","construction/ui/trade_goods",summary.tradeGoods(),summary.tradeCalculation(),x,y,width,height,mx,my);
    }
    private static void row(GuiGraphicsExtractor g,Font font,String name,String icon,int count,String detail,int x,int y,int width,int height,int mx,int my) {
        UiAssets.draw(g,"construction/ui/special_entry",x,y,width,height);
        UiText.cell(g,font,name,x+width*20/402,y+2,width*150/402,height-4,UiMenuStyle.TEXT,false);
        int iconSize=Math.min(22,height-4);
        UiAssets.draw(g,icon,x+width*210/402-iconSize/2,y+2,iconSize,iconSize);
        UiText.cell(g,font,Integer.toString(count),x+width*254/402,y+2,width*50/402,height-4,UiMenuStyle.TEXT,false);
        if(mx>=x&&mx<x+width&&my>=y&&my<y+height)UiTooltips.drawExact(g,font,detail==null?name:detail,mx,my);
    }
}
