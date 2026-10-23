package dev.hoi.client.ui;

import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Component;

public final class UiTooltipCursorTest extends Screen {
    private UiTooltipCursorTest() {super(Component.literal("Tooltip cursor regression"));}
    @Override protected void init() {addRenderableWidget(new UiMenuButton("Cursor",40,60,180,70,()->{}));}
    public static void run(ClientGameTestContext context) {
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(UiTooltipCursorTest::new);context.getInput().setCursorPos(140,160);context.waitTicks(3);
            context.runOnClient(client->{
                var screen=(UiTooltipCursorTest)client.gui.screen();var a=position(screen,70,80);var b=position(screen,76,86);
                if(b[0]-a[0]!=6||b[1]-a[1]!=6)throw new AssertionError("Native widget tooltip must follow both cursor axes");
                var widget=(UiMenuButton)screen.children().getFirst();widget.setX(screen.width-180);widget.setY(screen.height-70);
                var edge=position(screen,screen.width-2,screen.height-2);
                if(edge[0]<7||edge[1]<7||edge[0]+edge[2]+7>screen.width||edge[1]+edge[3]+7>screen.height)throw new AssertionError("Native tooltip background must fit viewport edges");
                var g=new GuiGraphicsExtractor(client,new GuiRenderState(),80,90);String text="Source tooltip\n".repeat(100);
                var first=UiTooltips.drawSourceScrollable(g,client.font,text,80,90,screen.width,screen.height,0);
                var moved=UiTooltips.drawSourceScrollable(g,client.font,text,86,96,screen.width,screen.height,40);
                if(moved.x()-first.x()!=6||moved.maxScroll()<=0||moved.scroll()!=40||moved.y()+moved.height()+4>screen.height)throw new AssertionError("Long tooltip must follow the cursor while preserving scroll and viewport bounds");
                var positions=new ArrayList<Integer>();
                var aligned=new GuiGraphicsExtractor(client,new GuiRenderState(),80,90){@Override public void text(Font font,net.minecraft.util.FormattedCharSequence line,int x,int y,int color,boolean shadow){positions.add(x);super.text(font,line,x,y,color,shadow);}};
                var tooltip=UiTooltips.drawOfficerScrollable(aligned,client.font,"긴 툴팁 제목\n짧음\n서로 길이가 다른 본문",80,90,screen.width,screen.height,0);
                if(positions.size()!=3||positions.stream().anyMatch(x->x!=tooltip.x()))throw new AssertionError("Every officer tooltip line must share the left edge: "+positions);
            });
        }
    }
    private static int[] position(Screen screen,int mx,int my) {
        var client=Minecraft.getInstance();var result=new int[4];
        var g=new GuiGraphicsExtractor(client,new GuiRenderState(),mx,my) {
            @Override public void tooltip(Font font,List<ClientTooltipComponent> components,int x,int y,ClientTooltipPositioner positioner,net.minecraft.resources.Identifier style,boolean focused) {
                var observed=new ArrayList<>(components);var original=observed.getFirst();
                observed.set(0,new ClientTooltipComponent() {
                    public int getWidth(Font f) {return original.getWidth(f);}
                    public int getHeight(Font f) {return original.getHeight(f);}
                    public void extractText(GuiGraphicsExtractor graphics,Font f,int px,int py) {result[0]=px;result[1]=py;result[2]=getWidth(f);result[3]=getHeight(f)-2;original.extractText(graphics,f,px,py);}
                });
                super.tooltip(font,observed,x,y,positioner,style,focused);
            }
        };
        screen.extractRenderState(g,mx,my,0);g.extractDeferredElements(mx,my,0);
        if(result[2]==0)throw new AssertionError("Native widget tooltip was not rendered");return result;
    }
}
