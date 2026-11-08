package dev.hoi.client.screen.commander;

import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.commander.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.country.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import java.util.*;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import static dev.hoi.protocol.industry.IndustryPayloads.Action.*;

public final class CommanderCaptivity extends Screen implements InputSidebarMovement.InputMovementScreen {
    private final Screen parent;
    private final String token;
    private final Consumer<IndustryPayloads.Request> transport;
    private IndustryView view;
    private EditBox search;
    private Tooltips.ScrolledTooltip tooltip;
    private String tooltipText="";
    private int tooltipScroll;
    Tooltips.ScrolledTooltip detailTooltip(){return tooltip;}
    private boolean overTooltip(double x,double y){return tooltip!=null&&x>=tooltip.x()-4&&x<tooltip.x()+tooltip.width()+4&&y>=tooltip.y()-4&&y<tooltip.y()+tooltip.height()+4;}
    private void clearTooltip(){tooltip=null;tooltipText="";tooltipScroll=0;}
    private String query="",sort="name",failure="";
    private boolean ours,descending,pending,closed,dirty,dragging;
    private int left,top,pane,bottom,ticks,pendingTicks,editTicks,scroll;
    public CommanderCaptivity(Screen parent,boolean ours){this(parent,UUID.randomUUID().toString(),ours,null,request->{if(!ClientPlayNetworking.canSend(IndustryPayloads.Request.TYPE))throw new IllegalStateException("서버에 연결할 수 없습니다.");ClientPlayNetworking.send(request);});}
    public CommanderCaptivity(Screen parent,String token,boolean ours,IndustryView fixture,Consumer<IndustryPayloads.Request> transport){super(Component.literal("포획된 지휘관"));this.parent=parent;this.token=token;this.ours=ours;this.view=fixture;this.transport=transport;failure=fixture==null?"":fixture.message();if(state()!=null){this.ours=state().ours();query=state().query();sort=state().sort();descending=state().descending();}}
    boolean ownCommanders(){return ours;}
    public void open(){send(CAPTIVITY_OPEN,0);}
    private CommanderCaptivityView state(){return view==null?null:view.captivity();}
    private int pages(){return state()==null?1:Math.max(1,(state().total()+CommanderCaptivityView.PAGE_SIZE-1)/CommanderCaptivityView.PAGE_SIZE);}
    private UiScrollBar scrollbar(){return new UiScrollBar(left+pane-11,top+55,bottom-5,1,pages());}
    private void page(int value){if(state()!=null&&!pending){int next=Math.clamp(value,0,pages()-1);if(next!=state().page())send(CAPTIVITY_QUERY,next);}}
    private void send(IndustryPayloads.Action action,int page){if(closed||pending)return;pending=true;pendingTicks=0;failure="";dirty=false;try{transport.accept(new IndustryPayloads.Request(action,token,view==null?0:view.revision(),query,(ours?"ours":"theirs")+"/"+sort+"/"+(descending?"desc":"asc"),page));}catch(IllegalArgumentException|IllegalStateException error){pending=false;failure="지휘관 목록을 요청하지 못했습니다. 다시 시도하세요.";}if(minecraft!=null)refreshWidgets();}
    public void update(IndustryView next){
        if(closed||!next.session().equals(token))return;
        if(next.country().isEmpty()||view!=null&&!next.country().equals(view.country())){closed=true;DialogCoordinator.setContent(null);return;}
        if(view!=null&&next.revision()<=view.revision())return;
        boolean typed=dirty;String typedQuery=query;view=next;pending=false;failure=next.message();
        if(state()!=null){ours=state().ours();sort=state().sort();descending=state().descending();query=typed?typedQuery:state().query();dirty=typed&&!query.equals(state().query());if(parent instanceof ScreenMenu menu)menu.captivityCounts(state().missing(),state().held());}
        else failure="지휘관 목록을 받지 못했습니다. 다시 시도하세요.";
        scroll=0;clearTooltip();refreshWidgets();
    }
    private void refreshWidgets(){boolean focused=search!=null&&search.isFocused();rebuildWidgets();if(focused&&search!=null)setFocused(search);}
    public void failed(){pending=false;failure="지휘관 목록을 읽지 못했습니다. 다시 시도하세요.";refreshWidgets();}
    private UiMenuButton button(String caption,int x,int y,int w,int h,Runnable action){var b=new UiMenuButton(caption,x,y,w,h,action);b.active=!pending;addRenderableWidget(b);return b;}
    private void tab(boolean own){if(ours==own)return;ours=own;scroll=0;send(CAPTIVITY_QUERY,0);}
    private void order(String key){if(sort.equals(key))descending=!descending;else{sort=key;descending=!key.equals("name");}send(CAPTIVITY_QUERY,0);}
    @Override protected void init(){
        boolean focused=search!=null&&search.isFocused();
        pane=Math.min(275,Math.max(180,width-8));left=Math.min(width-pane-4,parent==null?4:UiPanelLayout.width(MenuTab.OFFICER_CORPS,width)+3);left=Math.max(4,left);top=HudMenuBar.height(width)+2;bottom=Math.min(height-5,top+306);
        var close=button("×",left+pane-19,top+2,16,17,this::onClose).caption("").background("menu/close").sound("ui.close");close.active=true;
        button("숙련도",left+8,top+20,17,15,()->order("skill")).caption("").background("commander/ui/sort_skill");
        button("이름",left+47,top+20,42,15,()->order("name")).background("commander/ui/sort_text");
        search=new EditBox(font,left+100,top+22,57,13,Component.literal("검색"));search.setBordered(false);search.setMaxLength(64);search.setValue(query);search.setHint(Component.literal("검색"));search.setResponder(value->{query=value;dirty=true;editTicks=0;});addRenderableWidget(search);if(focused)setFocused(search);
        for(int i=0;i<4;i++){String key=List.of("attack","defense","planning","logistics").get(i);button(List.of("공격","방어","계획","군수").get(i),left+161+i*13,top+20,13,15,()->order(key)).caption("").background("commander/ui/"+key);}
        button("수용량",left+220,top+20,43,15,()->order("capacity")).background("commander/ui/sort_text");
        button("우리의 실종된 지휘관",left+6,top+36,129,21,()->tab(true)).bold().background("officer/hq/captured_tab/frame_"+(ours?0:1));
        button("우리에게 포획된 지휘관",left+133,top+36,129,21,()->tab(false)).bold().background("officer/hq/captured_tab/frame_"+(ours?1:0));
        scroll=Math.clamp(scroll,0,Math.max(0,(state()==null?0:state().rows().size()*49)-(bottom-top-60)));
    }
    @Override public void tick(){if(closed)return;ticks++;if(dirty)editTicks++;if(pending&&++pendingTicks>=200){pending=false;failure="응답을 받지 못했습니다. 다시 시도하세요.";refreshWidgets();}if(!pending&&failure.isEmpty()){if(dirty&&editTicks>=8)send(state()==null?CAPTIVITY_OPEN:CAPTIVITY_QUERY,0);else if(!dirty&&ticks%80==0)send(state()==null?CAPTIVITY_OPEN:CAPTIVITY_QUERY,state()==null?0:state().page());}}
    @Override public void onClose(){if(closed)return;closed=true;try{transport.accept(new IndustryPayloads.Request(CLOSE,token,view==null?0:view.revision(),"","",0));}catch(IllegalArgumentException|IllegalStateException ignored){}DialogCoordinator.setContent(parent);}
    @Override public boolean allowsMovement(){return false;}
    @Override public int unitHudLeft(){return left+pane;}
    @Override public boolean keyPressed(KeyEvent event){if(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){onClose();return true;}if(search!=null&&search.isFocused()&&(event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_RETURN||event.key()==com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER)){send(state()==null?CAPTIVITY_OPEN:CAPTIVITY_QUERY,0);return true;}return super.keyPressed(event);}
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,boolean twice){if(state()!=null&&event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT&&scrollbar().contains(event.x(),event.y())){dragging=!scrollbar().arrow(event.y());page(dragging?scrollbar().offset(event.y()):scrollbar().step(event.y(),state().page(),1));return true;}super.mouseClicked(event,twice);return true;}
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,double dx,double dy){if(dragging){page(scrollbar().offset(event.y()));return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event){dragging=false;super.mouseReleased(event);return true;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(overTooltip(mx,my)){tooltipScroll=Math.clamp(tooltipScroll-(int)Math.round(vertical*3*(font.lineHeight+1)),0,tooltip.maxScroll());return true;}if(mx>=left&&mx<left+pane&&my>=top+55&&my<bottom-5&&state()!=null&&!pending&&vertical!=0){clearTooltip();int maximum=Math.max(0,state().rows().size()*49-(bottom-top-60));if(vertical<0&&scroll>=maximum||vertical>0&&scroll<=0)page(state().page()+(vertical<0?1:-1));else{scroll=Math.clamp(scroll-(int)Math.round(vertical*49),0,maximum);refreshWidgets();}}return true;}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        UiAssets.draw(g,"commander/ui/panel",left,top,pane,bottom-top);UiText.centered(g,font,Tooltips.officerText("포획된 지휘관",true),left+7,top+3,pane-36,14,UiMenuStyle.TEXT);
        g.fill(left+2,top+54,left+pane-2,bottom-5,0xFF101012);String hovered="";
        if(state()!=null){
            g.enableScissor(left+3,top+55,left+pane-12,bottom-5);
            for(int i=0;i<state().rows().size();i++){
                var row=state().rows().get(i);var leader=row.leader();int y=top+56+i*49-scroll;
                UiAssets.draw(g,"officer/hq/captured_row",left+6,y,255,49);var skill=Component.literal(Integer.toString(leader.skill())).withStyle(net.minecraft.ChatFormatting.BOLD);g.pose().pushMatrix();g.pose().translate(left+7+(25-font.width(skill)*2)/2f,y+9);g.pose().scale(2);g.text(font,skill,0,0,0xFF000000,false);g.pose().popMatrix();UiAssets.draw(g,row.portrait(),left+34,y+2,33,44);UiAssets.draw(g,"officer/hq/captured_bars",left+30,y-1,40,53);
                UiText.leftSingleLine(g,font,Tooltips.officerText(leader.role(),true),left+70,y+6,75,9,0xFF000000);UiText.leftSingleLine(g,font,Tooltips.officerText(row.name(),true),left+70,y+15,90,9,0xFF000000);
                if(mx>=left+6&&mx<left+261&&my>=Math.max(top+55,y)&&my<Math.min(bottom-5,y+49))hovered=row.detail();
                for(int a=0;a<4;a++){int ax=left+6+List.of(155,168,182,195).get(a);UiAssets.draw(g,"commander/ui/"+List.of("attack","defense","planning","logistics").get(a),ax,y+5,14,13);UiText.centered(g,font,Integer.toString(leader.abilities().get(a)),ax-1,y+15,15,11,0xFF000000);if(mx>=ax&&mx<ax+13&&my>=y+3&&my<y+26&&!leader.abilityTooltips().isEmpty())hovered=leader.abilityTooltips().get(a);}
                UiText.centered(g,font,Integer.toString(leader.capacity()),left+218,y+8,43,12,0xFF000000);
                for(int j=0;j<leader.traitDetails().size();j++){var trait=leader.traitDetails().get(j);int x=CountryFlagStack.left(j,leader.traitDetails().size(),left+69,78,12),visible=CountryFlagStack.visibleWidth(j,leader.traitDetails().size(),left+69,78,12);UiAssets.draw(g,trait.icon(),x,y+30,12,16);if(mx>=x&&mx<x+visible&&my>=y+30&&my<y+46)hovered=trait.tooltip();}
                CountryFlags.draw(g,"country/"+row.originalCountry().toLowerCase(Locale.ROOT)+"/flag","small2",left+10,y+30,21,14);UiAssets.draw(g,"map_detail/flag_frame",left+7,y+28,26,18);CountryFlags.draw(g,"country/"+row.captor().toLowerCase(Locale.ROOT)+"/flag","smallest",left+56,y+33,10,7);
            }
            if(state().rows().isEmpty())UiText.centered(g,font,query.isEmpty()?"해당 지휘관이 없습니다.":"검색 결과가 없습니다.",left+8,top+64,pane-16,24,UiMenuStyle.MUTED);
            g.disableScissor();scrollbar().draw(g,state().page());
        }else UiText.centered(g,font,pending?"불러오는 중…":"목록을 받지 못했습니다.",left+8,top+64,pane-16,24,UiMenuStyle.MUTED);
        if(!failure.isEmpty()||pending){g.fill(left+4,bottom-43,left+pane-4,bottom-23,0xEE151515);UiText.centered(g,font,pending?"불러오는 중…":failure,left+7,bottom-42,pane-14,18,pending?UiMenuStyle.MUTED:0xFFE15B5B);}
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable widget)widget.extractRenderState(g,mx,my,delta);
        if(my<top+55||my>=bottom-5)hovered="";
        if(overTooltip(mx,my))hovered=tooltipText;
        if(hovered.isEmpty())clearTooltip();else{
            if(!hovered.equals(tooltipText)){tooltipText=hovered;tooltipScroll=0;}
            tooltip=Tooltips.drawOfficerScrollable(g,font,tooltipText,mx,my,width,height,tooltipScroll);tooltipScroll=tooltip.scroll();
        }
    }
}
