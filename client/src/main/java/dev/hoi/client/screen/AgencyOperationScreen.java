package dev.hoi.client.screen;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.AgencyView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Preparation uses only the choices issued by the private agency session. */
final class AgencyOperationScreen extends Screen {
    private final AgencyScreen parent;
    private AgencyView.Item item;
    private final Consumer<List<String>> submit;
    private final List<String> arguments = new ArrayList<>();
    private int x,y,w,h,left,scroll;
    AgencyOperationScreen(AgencyScreen parent, AgencyView.Item item, Consumer<List<String>> submit) {
        super(Component.literal(item.title())); this.parent=parent; this.item=item; this.submit=submit;
        for (int i=0;i<item.parameters().size();i++) {
            var choices=item.parameters().get(i).choices();
            arguments.add(choices.isEmpty()?"":choices.get(Math.min(i==2?1:0,choices.size()-1)).value());
        }
    }
    void update(AgencyView view) {
        var next=view.items().stream().filter(i->i.id().equals(item.id())).findFirst().orElse(null);
        if(next==null){minecraft.gui.setScreen(parent);return;}
        item=next;
        for(int i=0;i<item.parameters().size();i++) {
            int index=i;var choices=item.parameters().get(i).choices();
            if(choices.stream().noneMatch(c->c.value().equals(arguments.get(index))))arguments.set(i,choices.isEmpty()?"":choices.getFirst().value());
        }
        rebuildWidgets();
    }
    AgencyScreen backdrop() { return parent; }
    @Override public void tick() { parent.tick(); }
    @Override protected void init() {
        w=Math.min(620,width-16); h=Math.min(350,height-HoiMenuBar.height(width)-12);
        x=(width-w)/2; y=Math.max(HoiMenuBar.height(width)+4,(height-h)/2); left=Math.min(190,w/3);
        addRenderableWidget(new HoiMenuButton("×",x+w-25,y+3,20,20,this::onClose));
        for(int i=0;i<item.parameters().size();i++) {
            int index=i; var parameter=item.parameters().get(i);
            if(parameter.choices().size() == 1) continue;
            String selected=parameter.choices().stream().filter(c->c.value().equals(arguments.get(index))).map(AgencyView.Choice::label).findFirst().orElse("선택 없음");
            var button=new HoiMenuButton(parameter.label()+": "+selected,x+8,y+98+i*39,left-16,35,()->{
                var choices=parameter.choices(); if(choices.isEmpty())return;
                int at=0;for(int j=0;j<choices.size();j++)if(choices.get(j).value().equals(arguments.get(index)))at=j;
                arguments.set(index,choices.get((at+1)%choices.size()).value());rebuildWidgets();
            });
            button.setTooltip(Tooltip.create(Component.literal(parameter.label()+" · 클릭하여 다음 선택\n"+selected)));
            addRenderableWidget(button);
        }
        var prepare=new HoiMenuButton("준비하기",x+w/2-60,y+h-28,120,22,()->{
            minecraft.gui.setScreen(parent); submit.accept(List.copyOf(arguments));
        });
        prepare.active=item.enabled()&&!arguments.contains("")&&(arguments.size()<3||!arguments.get(1).equals(arguments.get(2)));
        addRenderableWidget(prepare);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        parent.extractRenderState(g,-1,-1,delta);g.nextStratum();
        HoiMenuStyle.panel(g,x,y,w,h);
        HoiMenuStyle.heading(g,font,item.title(),x+12,y+9,w-48,HoiMenuStyle.TEXT);
        HoiMenuStyle.recess(g,x+6,y+29,left-9,h-65);
        UiAssets.nineSlice(g,"agency/ui/target",x+9,y+32,left-15,61,5,.5);
        UiAssets.cover(g,item.texture(),x+12,y+35,55,55);
        String country=item.parameters().isEmpty()?"":item.parameters().getFirst().choices().stream().filter(c->c.value().equals(arguments.getFirst())).findFirst().map(AgencyView.Choice::value).orElse("");
        String countryName=item.parameters().isEmpty()?"":item.parameters().getFirst().choices().stream().filter(c->c.value().equals(arguments.getFirst())).findFirst().map(AgencyView.Choice::label).orElse("");
        UiAssets.draw(g,"country/"+country.toLowerCase(Locale.ROOT)+"/flag",x+73,y+37,25,17);
        UiText.centered(g,font,countryName,x+72,y+57,left-82,30,HoiMenuStyle.TEXT);
        UiText.centered(g,font,item.value(),x+10,y+h-97,left-17,48,0xFFFFCC33);
        int right=x+left+5,area=w-left-14,pitch=area/3;
        UiAssets.nineSlice(g,"agency/ui/clipboard_mid",right-3,y+30,area+4,h-67,5,.5);
        UiAssets.nineSlice(g,"agency/ui/operation_bottom",x+4,y+h-32,w-8,28,4,.5);
        String[] art={"phase_border","phase_bribe","phase_escape"};
        String[] names={"침투","작전 수행","탈출"};
        for(int i=0;i<3;i++) {
            UiAssets.cover(g,"agency/"+art[i],right+i*pitch,y+34,pitch-5,63);
            UiText.centered(g,font,names[i],right+i*pitch,y+99,pitch-5,14,HoiMenuStyle.TEXT);
        }
        var lines=font.split(Component.literal(item.detail()),Math.max(1,(int)(area/UiText.scale(font))));
        int visible=Math.max(1,(h-155)/13);scroll=Math.clamp(scroll,0,Math.max(0,lines.size()-visible));
        for(int i=scroll;i<Math.min(lines.size(),scroll+visible);i++) UiText.text(g,font,lines.get(i),right,y+121+(i-scroll)*13,HoiMenuStyle.TEXT);
        super.extractRenderState(g,mx,my,delta);
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {scroll-=(int)(vertical*3);return true;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
}
