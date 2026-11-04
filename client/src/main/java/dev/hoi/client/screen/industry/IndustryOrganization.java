package dev.hoi.client.screen.industry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogScreen;
import java.util.*;
import java.util.function.BiConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;

public final class IndustryOrganization extends DialogScreen {
    private final Screen parent;
    private IndustryOrganization detailBackdrop;
    private final Map<String,AbstractWidget> buttons=new LinkedHashMap<>();
    private final Map<String,DialogView.Tile> nodes=new LinkedHashMap<>();
    private int left,top,pane,panelHeight,scroll,sideScroll,queueScroll;
    private int queueX(){return width/2-125;}
    private int queueHeight(){return Math.min(360,height-12);}
    private int queueY(){return Math.clamp(height/2-125,6,height-queueHeight()-6);}
    private int queueRow(int index){return queueY()+72+index*27-queueScroll;}
    private boolean pending,queueOpen;
    private int draggingScroll=-1;
    private String confirmation="",dragToken="",moveToken="";
    private int moveTarget=-1,movePrevious=-1,tooltipScroll;
    private String hoveredText="";
    private UiTooltips.ScrolledTooltip tooltipBounds;
    public IndustryOrganization(DialogView view,Screen parent){this(view,parent,DialogCoordinator::choose);}
    IndustryOrganization(DialogView view,Screen parent,BiConsumer<DialogView,String> send){super(view,send);this.parent=parent;}
    public Screen backdrop(){return parent;}
    public void dismiss(){DialogCoordinator.setContent(parent);}
    AbstractWidget control(String id){return buttons.get(id);}
    public static String art(String name){return "mio/ui/gfx_"+name+"/frame_0";}
    static String equipmentIcon(String token){return art(token.startsWith("mio_cat_")?token:"military_industrial_organization_"+token);}
    private void icon(GuiGraphicsExtractor g,String path,int x,int y,int w,int h){if(!UiAssets.draw(g,path,x,y,w,h)&&path.startsWith("mio/country/")){String[] parts=path.split("/",4);if(parts.length==4)UiAssets.draw(g,"mio/ui/"+parts[3],x,y,w,h);}}
    private boolean policies(){return view().image().equals("organization/policies");}
    private boolean offered(String id){return view().choices().stream().anyMatch(c->c.id().equals(id));}
    private DialogView.Tile tile(String section){return view().tiles().stream().filter(t->t.section().equals(section)).findFirst().orElse(null);}
    private List<DialogView.Tile> tiles(String prefix){return view().tiles().stream().filter(t->t.section().startsWith(prefix)).toList();}
    private List<DialogView.Tile> queued(){return tiles("trait/").stream().filter(t->!t.value().isEmpty()).sorted(Comparator.comparingInt(t->Integer.parseInt(t.value()))).toList();}
    private void button(String id,String label,int x,int y,int w,int h,boolean enabled,Runnable action){
        var b=new UiMenuButton(label,x,y,w,h,action).textShadow(false);b.setTooltip(null);b.active=enabled;buttons.put(id,b);addRenderableWidget(b);
    }
    private void send(String id){
        if(pending||view().token().isEmpty()||!offered(id))return;
        pending=true;buttons.values().forEach(b->b.active=false);super.choose(id);
    }
    private void confirm(String id){if(!pending&&offered(id)){confirmation=id;rebuildWidgets();}}
    private void advanceMove(){
        if(moveToken.isEmpty())return;
        var order=queued();int from=-1;for(int i=0;i<order.size();i++)if(order.get(i).section().equals("trait/"+moveToken))from=i;
        if(from<0||from==moveTarget||from==movePrevious){moveToken="";return;}
        String id=(from>moveTarget?"up/":"down/")+moveToken;
        if(offered(id)){movePrevious=from;send(id);}else moveToken="";
    }
    @Override public void update(DialogView next){
        if(!next.token().equals(view().token())||next.revision()<view().revision()||pending&&next.revision()==view().revision())return;
        boolean changed=!next.image().equals(view().image());
        if(changed&&next.image().equals("organization/policies"))cacheDetail();
        pending=false;confirmation="";
        if(changed){scroll=0;queueOpen=false;moveToken="";}
        super.update(next);advanceMove();
    }
    private void cacheDetail(){detailBackdrop=new IndustryOrganization(view(),parent,(v,id)->{});detailBackdrop.scroll=scroll;detailBackdrop.sideScroll=sideScroll;detailBackdrop.init(width,height);}
    @Override protected void init(){
        buttons.clear();nodes.clear();pane=Math.min(policies()?268:640,width-12);panelHeight=Math.min(policies()?243:360,height-12);left=(width-pane)/2;top=(height-panelHeight)/2;
        for(var t:tiles("trait/"))if(t.traitNode()!=null)nodes.put(t.section().substring(6),t);
        if(!confirmation.isEmpty()){button("cancel-confirm","취소",width/2-90,height/2+51,80,18,true,()->{confirmation="";rebuildWidgets();});button("ok-confirm","OK",width/2+10,height/2+51,80,18,!pending,()->{String id=confirmation;confirmation="";send(id);rebuildWidgets();});return;}
        if(queueOpen){
            int x=queueX(),y=queueY();button("close-queue","×",x+228,y,19,19,true,()->{queueOpen=false;rebuildWidgets();});((UiMenuButton)buttons.get("close-queue")).background("none");
            var order=queued();queueScroll=Math.clamp(queueScroll,0,Math.max(0,order.size()*27-(queueHeight()-78)));
            for(int i=0;i<order.size();i++){var t=order.get(i);String token=t.section().substring(6);int row=queueRow(i);if(row<y+72||row+27>y+queueHeight()-6)continue;button("unqueue/"+token,"×",x+218,row+3,19,19,!pending&&offered("unqueue/"+token),()->send("unqueue/"+token));((UiMenuButton)buttons.get("unqueue/"+token)).background("none");button("queue-row/"+token,t.name(),x+10,row,208,27,false,()->{});}
            return;
        }
        button("close","×",left+pane-22,top+3,18,18,true,this::onClose);
        if(policies())buttons.put("back",buttons.get("close"));
        if(policies()){
            var rows=tiles("policy/");scroll=Math.clamp(scroll,0,Math.max(0,rows.size()*48-(panelHeight-70)));
            for(int i=0;i<rows.size();i++){var t=rows.get(i);int y=top+48+i*48-scroll;if(y<top+48||y+44>top+panelHeight-22)continue;String id="attach/"+t.section().substring(7);button(id,t.name(),left+8,y,pane-16,44,!pending&&offered(id),()->confirm(id));buttons.get(id).setTooltip(UiTooltips.createOfficer(t.detail()));}
            return;
        }
        button("queue","특성 대기열"+(queued().isEmpty()?"":" ("+queued().size()+")"),left+18,top+85,111,17,true,()->{cacheDetail();queueOpen=true;rebuildWidgets();});((UiMenuButton)buttons.get("queue")).background(art("button_221x34"));
        button("policies","정책",left+10,top+panelHeight-44,136,35,!pending&&offered("policies"),()->send("policies"));
        ((UiMenuButton)buttons.get("policies")).background(art("mio_policy_slot_button"));
        if(tile("policy")!=null&&!tile("policy").name().isEmpty())((UiMenuButton)buttons.get("policies")).caption("");
        buttons.get("policies").setTooltip(UiTooltips.createOfficer(Optional.ofNullable(tile("policy")).map(DialogView.Tile::detail).orElse("")));
        var traits=tiles("trait/");int maxRow=traits.stream().map(DialogView.Tile::traitNode).filter(Objects::nonNull).mapToInt(DialogView.TraitNode::row).max().orElse(0);
        scroll=Math.clamp(scroll,0,Math.max(0,maxRow*56+96-(panelHeight-30)));
        for(var t:traits){if(t.traitNode()==null)continue;String token=t.section().substring(6);nodes.put(token,t);int x=nodeX(t),y=nodeY(t);if(y<top+48||y+40>top+panelHeight-8)continue;button(t.section(),t.name(),x,y,32,40,false,()->{});buttons.get(t.section()).setTooltip(UiTooltips.createOfficer(t.detail()));}
    }
    private void funds(GuiGraphicsExtractor g,DialogView.Tile summary){
        g.pose().pushMatrix();g.pose().translate(left+68,top+53);g.pose().scale(.5f);
        UiAssets.draw(g,art("leader_exp_bg"),0,0,171,11);
        UiAssets.draw(g,art("leader_exp_fg_mid_background"),0,-1,171,11);
        if(summary.gauge()!=null){
            int progress=(int)Math.round(171*Math.clamp(summary.gauge().fraction(),0,1));
            if(progress>0)g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+art("leader_exp_fg_mid_fill")+".png"),0,-1,progress,10,0,progress/171f,0,1);
            UiAssets.draw(g,art("leader_exp_fg_begin"),0,0,3,11);
            UiAssets.draw(g,art("leader_exp_fg_end"),Math.min(168,progress),0,3,11);
        }
        g.pose().popMatrix();
    }
    private int nodeX(DialogView.Tile t){return left+170+t.traitNode().column()*44;}
    private int nodeY(DialogView.Tile t){return top+56+t.traitNode().row()*56-scroll;}
    private void text(GuiGraphicsExtractor g,String value,int x,int y,int w,int h,int color){UiText.leftSingleLine(g,font,UiKeywordIcons.decorateLines(UiTooltips.officerText(value),false),x,y,w,h,color);}
    private void line(GuiGraphicsExtractor g,int x1,int y1,int x2,int y2,int color,boolean dotted){int steps=Math.max(Math.abs(x2-x1),Math.abs(y2-y1));for(int i=0;i<=steps;i++)if(!dotted||i%4<2){int x=x1+(x2-x1)*i/Math.max(1,steps),y=y1+(y2-y1)*i/Math.max(1,steps);g.fill(x,y,x+(dotted?2:1),y+(dotted?2:1),color);}}
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta){
        if((policies()||queueOpen)&&detailBackdrop!=null){detailBackdrop.extractRenderState(g,-1,-1,delta);g.nextStratum();}else if(parent!=null){parent.extractRenderState(g,-1,-1,delta);g.nextStratum();}
        String hovered="";int tipX=mx,tipY=my;boolean traitTooltip=false;
        if(!queueOpen){g.fill(left,top,left+pane,top+panelHeight,0xFF111316);UiAssets.tiledNineSlice(g,art("tiled_plain_bg"),left,top,pane,panelHeight,64,.5);
        if(policies()){UiAssets.nineSlice(g,art("tiled_plain_bg"),left,top,pane,48,5,.5);g.pose().pushMatrix();g.pose().translate(left+pane/2f,top+7);g.pose().scale(1.25f);g.centeredText(font,net.minecraft.network.chat.Component.literal(view().title()).withStyle(net.minecraft.ChatFormatting.BOLD),0,0,0xFFFFFFFF);g.pose().popMatrix();}else {var title=net.minecraft.network.chat.Component.literal(view().subtitle()).withStyle(net.minecraft.ChatFormatting.BOLD);g.pose().pushMatrix();g.pose().translate(left+10,top+6);g.pose().scale(1.25f);g.text(font,net.minecraft.network.chat.Component.literal(UiText.ellipsize(font,title.getString(),184)).withStyle(net.minecraft.ChatFormatting.BOLD),0,0,0xFFFFFFFF,false);g.pose().popMatrix();}
        hovered=!policies()&&mx>=left+160&&mx<left+pane-6&&my>=top+26&&my<top+panelHeight-6?view().body():"";
        if(policies()){
            UiText.centered(g,font,view().subtitle(),left+10,top+26,pane-20,16,UiMenuStyle.TEXT,false);var rows=tiles("policy/");g.enableScissor(left+6,top+48,left+pane-6,top+panelHeight-23);
            for(int i=0;i<rows.size();i++){var t=rows.get(i);int y=top+48+i*48-scroll;UiAssets.draw(g,art("mio_entry_bg"),left+8,y,pane-16,44);if(offered("attach/"+t.section().substring(7))||t.overlay().equals("current"))icon(g,t.icon(),left+17,y+3,38,38);else {String path=t.icon();if(!UiAssets.drawGrayscale(g,path,left+17,y+3,38,38)&&path.startsWith("mio/country/"))UiAssets.drawGrayscale(g,"mio/ui/"+path.split("/",4)[3],left+17,y+3,38,38);}text(g,t.name(),left+62,y+13,pane-125,18,t.overlay().equals("current")?0xFF80DD80:UiMenuStyle.TEXT);text(g,"£GFX_pol_power_icon §Y"+t.value(),left+pane-60,y+14,50,18,UiMenuStyle.ACCENT);if(mx>=left+8&&mx<left+pane-8&&my>=Math.max(top+48,y)&&my<Math.min(top+panelHeight-23,y+44))hovered=t.detail();}g.disableScissor();scrollbar(0).draw(g,scroll);
        }else{
            UiAssets.tiledNineSlice(g,art("tiled_window2_1b_border"),left+3,top+75,155,panelHeight-80,64,.5);
            UiAssets.tiledNineSlice(g,art("tiled_plain_bg"),left+3,top+25,155,50,64,.5);var summary=tile("summary");if(summary!=null){UiAssets.draw(g,summary.icon(),left+10,top+33,32,30);UiAssets.draw(g,art("land_battle_leader_skill_bg"),left+43,top+38,21,30);UiText.centered(g,font,summary.value(),left+43,top+41,21,9,UiMenuStyle.TEXT,false);var points=tile("points");if(points!=null)text(g,points.name(),left+68,top+38,86,10,UiMenuStyle.TEXT);funds(g,summary);if(summary.gauge()!=null&&mx>=left+68&&mx<left+154&&my>=top+50&&my<top+63)hovered=summary.gauge().tooltip();}
            int sideTop=top+108,sideBottom=top+panelHeight-50;
            int content=sideContentHeight(),visible=sideBottom-sideTop;
            sideScroll=Math.clamp(sideScroll,0,Math.max(0,content-visible));
            g.enableScissor(left+8,sideTop,left+154,sideBottom);int y=sideTop-sideScroll;
            UiText.centered(g,font,UiTooltips.officerText("속성",true),left+12,y,135,16,UiMenuStyle.TEXT,false);y+=19;
            for(var t:tiles("attribute/")){String value=t.section().endsWith("assign_cost")?"£GFX_pol_power_icon "+t.value()+"/일":t.value();text(g,t.name()+": "+(t.icon().equals("good")?"§G":"§Y")+value,left+12,y,137,14,UiMenuStyle.TEXT);if(mx>=left+8&&mx<left+154&&my>=Math.max(sideTop,y)&&my<Math.min(sideBottom,y+14))hovered=t.detail();y+=15;}
            UiText.centered(g,font,UiTooltips.officerText("장비 혜택",true),left+12,y,135,16,UiMenuStyle.TEXT,false);y+=18;var type=tile("type");if(type!=null){UiText.left(g,font,type.name(),left+12,y,135,typeHeight()-3,0xFFFFAA00);y+=typeHeight();}
            for(var t:tiles("equipment/")){UiAssets.draw(g,equipmentIcon(t.section().substring(10)),left+12,y+5,23,23);text(g,UiText.ellipsize(font,t.name(),110),left+38,y,110,14,UiMenuStyle.TEXT);var counts=t.value().split("/",-1);if(counts.length==2){UiAssets.draw(g,art("design_team_icon"),left+39,y+17,10,10);text(g,counts[0],left+52,y+16,30,12,UiMenuStyle.TEXT);UiAssets.draw(g,art("industrial_manufacturer_icon"),left+83,y+17,10,10);text(g,counts[1],left+96,y+16,30,12,UiMenuStyle.TEXT);}if(mx>=left+8&&mx<left+154&&my>=Math.max(sideTop,y)&&my<Math.min(sideBottom,y+37))hovered=t.detail();y+=37;}
            g.disableScissor();if(content>visible)scrollbar(1).draw(g,sideScroll);g.enableScissor(left+160,top+25,left+pane-6,top+panelHeight-6);
            UiAssets.draw(g,art("mio_details_background"),left+160,top+28,pane-166,panelHeight-34);
                UiAssets.tiledNineSlice(g,art("mio_header_transparent"),left+160,top+25,pane-166,22,16,.5);
                for(var t:tiles("header/")){int x=left+186+(int)(Double.parseDouble(t.value())*44);UiText.centeredSingleLine(g,font,UiTooltips.officerText(t.name(),true),x-80,top+31,160,12,UiMenuStyle.TEXT);}
                for(var t:nodes.values()){int x=nodeX(t)+16,y1=nodeY(t);boolean hot=mx>=x-16&&mx<x+16&&my>=y1&&my<y1+40;for(String parentId:t.traitNode().parents()){var p=nodes.get(parentId);if(p==null)continue;int px=nodeX(p)+16,py=nodeY(p)+38,mid=py+10;int color=hot?0xFFFFDD55:0xFFA3A3D7;boolean dot=t.traitNode().needed()!=t.traitNode().parents().size();line(g,px,py,px,mid,color,dot);line(g,px,mid,x,mid,color,dot);line(g,x,mid,x,y1,color,dot);}}
                var exclusivePairs=new HashSet<Set<String>>();
                for(var t:nodes.values())for(String exclusive:t.traitNode().exclusive()){
                    var other=nodes.get(exclusive);
                    if(other==null||other==t||!exclusivePairs.add(Set.of(t.section(),other.section())))continue;
                    var first=nodeX(t)<nodeX(other)?t:other;var second=first==t?other:t;
                    int x=nodeX(first)+30,end=nodeX(second)+2,linkY=nodeY(first)+8;
                    g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+art("focus_exclusive_line1")+".png"),x+8,linkY+6,end-8,linkY+14,0,1,0,1);
                    UiAssets.draw(g,"mio/ui/gfx_focus_link_exclusive/frame_1",x,linkY,16,16);
                    UiAssets.draw(g,"mio/ui/gfx_focus_link_exclusive/frame_2",end-16,linkY,16,16);
                    UiAssets.draw(g,art("focus_link_exclusive"),(x+end)/2-8,linkY,16,16);
                }
                for(var t:nodes.values()){int x=nodeX(t),y1=nodeY(t);int frame=t.traitNode().owned()?0:2;String sprite=t.traitNode().kind().equals("mio_special")?"country_spefific_org_trait_button":"industrial_org_trait_button";UiAssets.draw(g,"mio/ui/gfx_"+sprite+"/frame_"+frame,x,y1,32,40);UiAssets.draw(g,t.icon(),x+5,y1+4,22,24);var bonuses=t.traitNode().bonuses();int bonusWidth=Math.min(25,bonuses.size()*10);for(int i=0;i<bonuses.size();i++){String bonus=bonuses.get(i);int bonusX=UiFlagStack.left(i,bonuses.size(),x+16-bonusWidth/2,bonusWidth,10);String image=switch(bonus){case "equipment"->"design_team_icon";case "production"->"industrial_manufacturer_icon";default->"organization_modifier_icon";};UiAssets.draw(g,art(image),bonusX,y1+27,10,12);}if(!t.value().isEmpty()){UiAssets.draw(g,"mio/ui/gfx_topbar_alert_bg/frame_3",x+24,y1+22,13,13);UiText.centered(g,font,t.value(),x+24,y1+22,13,13,UiMenuStyle.TEXT,false);}if(mx>=x&&mx<x+32&&my>=Math.max(top+48,y1)&&my<Math.min(top+panelHeight-8,y1+40)){hovered=t.detail();traitTooltip=true;tipX=x+29;tipY=y1+20;}}
            g.disableScissor();
        }
        for(var entry:buttons.entrySet())if(!entry.getKey().startsWith("trait/")&&!entry.getKey().startsWith("attach/")&&!entry.getKey().startsWith("queue-row/"))((net.minecraft.client.gui.components.Renderable)entry.getValue()).extractRenderState(g,mx,my,delta);
        if(!policies()&&!queueOpen&&confirmation.isEmpty()&&tile("policy")!=null&&!tile("policy").name().isEmpty()){var policy=tile("policy");icon(g,policy.icon(),left+15,top+panelHeight-40,28,28);text(g,policy.name(),left+48,top+panelHeight-41,93,16,UiMenuStyle.TEXT);text(g,policy.value(),left+48,top+panelHeight-25,93,14,UiMenuStyle.ACCENT);}
        }
        if(queueOpen){
            int x=queueX(),y=queueY();g.nextStratum();g.fill(x,y,x+248,y+queueHeight(),0xFF101114);UiAssets.nineSlice(g,art("tiled_plain_bg"),x,y,248,70,4,.5);
            g.pose().pushMatrix();g.pose().translate(x+124,y+7);g.pose().scale(1.25f);g.centeredText(font,net.minecraft.network.chat.Component.literal("특성 대기열").withStyle(net.minecraft.ChatFormatting.BOLD),0,0,0xFFFFFFFF);g.pose().popMatrix();
            int instructionY=y+25;for(var line:UiTooltips.sourceLines(font,"이 제조사가 특성 점수를 가지고 있다면, 이 대기열의 사용 가능한 첫 특성이 획득 완료됩니다.\n특성을 §GShift + 클릭§!하여 §Y이 대기열에 추가§!\n§G끌어 놓기§!로 §Y대기열 순서 변경§!",238)){g.text(font,line,x+(248-font.width(line))/2,instructionY,UiMenuStyle.TEXT,false);instructionY+=11;}
            hovered="";
            for(var t:queued()){
                var b=buttons.get("queue-row/"+t.section().substring(6));if(b==null)continue;
                UiAssets.draw(g,t.icon(),b.getX()+20,b.getY()+4,23,23);
                if(t.value().equals("1"))UiAssets.draw(g,"mio/ui/gfx_topbar_alert_bg/frame_3",b.getX()+7,b.getY()+8,13,13);
                else {g.fill(b.getX()+7,b.getY()+13,b.getX()+10,b.getY()+15,0xFF41933D);g.fill(b.getX()+18,b.getY()+13,b.getX()+21,b.getY()+15,0xFF41933D);}
                UiText.centered(g,font,t.value(),b.getX()+7,b.getY()+8,14,13,UiMenuStyle.TEXT,false);
                text(g,t.name(),b.getX()+48,b.getY()+7,160,16,UiMenuStyle.TEXT);if(UiTooltips.contains(b,mx,my))hovered=t.detail();
            }
            for(var entry:buttons.entrySet())if(!entry.getKey().startsWith("queue-row/"))((net.minecraft.client.gui.components.Renderable)entry.getValue()).extractRenderState(g,mx,my,delta);
        }
        if(queueOpen&&scrollbar(2).maximum()>0)scrollbar(2).draw(g,queueScroll);
        if(!confirmation.isEmpty()){g.nextStratum();int x=width/2-145,y=height/2-90;g.fill(x,y,x+290,y+168,0xFF191B20);UiAssets.nineSlice(g,art("tiled_window_pol_goal"),x,y,290,168,20,.5);var choice=view().choices().stream().filter(c->c.id().equals(confirmation)).findFirst().orElseThrow();text(g,choice.label(),x+10,y+7,270,20,UiMenuStyle.TEXT);var lines=UiTooltips.exactLines(font,choice.tooltip(),264);int row=y+35;g.enableScissor(x+12,row,x+278,y+135);for(var line:lines){g.text(font,line,x+12,row,UiMenuStyle.TEXT,false);row+=11;}g.disableScissor();for(var b:buttons.values())((net.minecraft.client.gui.components.Renderable)b).extractRenderState(g,mx,my,delta);hovered="";}
        if(!hovered.equals(hoveredText)){hoveredText=hovered;tooltipScroll=0;}
        tooltipBounds=hovered.isEmpty()?null:UiTooltips.drawSourceScrollable(g,font,hovered,tipX,tipY,width,height,tooltipScroll,traitTooltip?194:260,traitTooltip);
    }
    private int typeHeight(){var type=tile("type");return type==null?0:Math.max(19,font.split(net.minecraft.network.chat.Component.literal(type.name()),135).size()*(UiText.MIN_HEIGHT+1)+3);}
    private int sideContentHeight(){return tiles("attribute/").size()*15+tiles("equipment/").size()*37+38+typeHeight();}
    private UiScrollBar scrollbar(int area){
        return switch(area){
            case 1->new UiScrollBar(left+148,top+92,top+panelHeight-42,panelHeight-158,sideContentHeight());
            case 2->new UiScrollBar(queueX()+237,queueY()+70,queueY()+queueHeight()-5,queueHeight()-78,queued().size()*27);
            default->new UiScrollBar(left+pane-11,top+40,top+panelHeight-14,panelHeight-70,tiles("policy/").size()*48);
        };
    }
    private void scrollTo(int area,int next){if(area==1)sideScroll=next;else if(area==2)queueScroll=next;else scroll=next;rebuildWidgets();}
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(draggingScroll>=0){scrollTo(draggingScroll,scrollbar(draggingScroll).offset(event.y()));return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
        if(!confirmation.isEmpty())return super.mouseClicked(event,twice);
        int area=queueOpen?2:policies()?0:1;var bar=scrollbar(area);
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT&&bar.contains(event.x(),event.y())){int current=area==1?sideScroll:area==2?queueScroll:scroll;if(bar.arrow(event.y()))scrollTo(area,bar.step(event.y(),current,30));else{draggingScroll=area;scrollTo(area,bar.offset(event.y()));}return true;}
        if(event.button()==InputConstants.MOUSE_BUTTON_LEFT&&!pending){
            if(queueOpen){for(var t:queued()){var b=buttons.get("queue-row/"+t.section().substring(6));if(b!=null&&UiTooltips.contains(b,event.x(),event.y())){dragToken=t.section().substring(6);return true;}}}
            else for(var t:nodes.values()){var b=buttons.get(t.section());if(b==null||!UiTooltips.contains(b,event.x(),event.y()))continue;String token=t.section().substring(6);if((event.modifiers()&InputConstants.MOD_SHIFT)!=0)send((t.value().isEmpty()?"queue/":"unqueue/")+token);else confirm("unlock/"+token);dev.hoi.client.audio.AudioUiSounds.play("ui.click");return true;}
        }
        return super.mouseClicked(event,twice);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event){if(draggingScroll>=0){draggingScroll=-1;return true;}if(!dragToken.isEmpty()){if(event.button()==InputConstants.MOUSE_BUTTON_LEFT){moveToken=dragToken;movePrevious=-1;moveTarget=Math.clamp((int)(event.y()-(queueY()+72)+queueScroll)/27,0,Math.max(0,queued().size()-1));advanceMove();}dragToken="";return true;}return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){if(tooltipBounds!=null&&tooltipBounds.maxScroll()>0&&(InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)||x>=tooltipBounds.x()-4&&x<tooltipBounds.x()+tooltipBounds.width()+4&&y>=tooltipBounds.y()-4&&y<tooltipBounds.y()+tooltipBounds.height()+4)){tooltipScroll=Math.clamp(tooltipScroll-(int)(vertical*33),0,tooltipBounds.maxScroll());return true;}if(!confirmation.isEmpty())return true;if(queueOpen){queueScroll-=(int)(vertical*27);}else if(!policies()&&x<left+158){sideScroll=Math.clamp(sideScroll-(int)(vertical*30),0,Math.max(0,sideContentHeight()-(panelHeight-158)));}else scroll-=(int)(vertical*28);rebuildWidgets();return true;}
    @Override public void onClose(){if(!confirmation.isEmpty()){confirmation="";rebuildWidgets();return;}if(queueOpen){queueOpen=false;rebuildWidgets();return;}if(policies()&&offered("back")){send("back");return;}super.choose("");dismiss();}
}
