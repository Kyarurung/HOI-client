package dev.hoi.client.screen.map;

import dev.hoi.client.ui.*;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class MapDetail extends DialogScreen implements InputSidebarMovement.InputMovementScreen {
    private int left, top, pane, tall, sharedScroll;
    private double scale;
    private final UiFlagStack ownerFlags = new UiFlagStack(), claimFlags = new UiFlagStack();
    private int tooltipScroll;
    private DialogView.Tile hovered;
    private long accessPendingRevision = -1;
    private record MapHover(DialogView.Tile tile, int x, int y, int w, int h) {}
    private final List<MapHover> hovers = new ArrayList<>();
    public MapDetail(DialogView view) { super(view); }
    MapDetail(DialogView view, java.util.function.BiConsumer<DialogView, String> send) { super(view, send); }
    @Override public void update(DialogView next) {
        if (next.token().equals(view().token()) && next.revision() > view().revision()) accessPendingRevision = -1;
        super.update(next);
    }
    private boolean sea() { return view().image().equals("map_detail/sea"); }
    @Override public int panelLeft() { return left; }
    @Override public int panelTop() { return top; }
    @Override public int panelWidth() { return pane; }
    @Override public int panelHeight() { return tall; }
    @Override protected void init() {
        int sourceWidth = sea() ? 433 : 463, sourceHeight = sea() ? 300 : 683;
        pane = Math.min(width - 8, (int)Math.round((sea()?1.20:1.35) * Math.max(sourceWidth / 2.0, width * sourceWidth / 2560.0)));
        scale = Math.min(pane / (double)sourceWidth, (height - UiMenuBar.height(width) - 4) / (double)sourceHeight);
        pane = (int)Math.round(sourceWidth * scale); tall = (int)Math.round(sourceHeight * scale);
        left = 0; top = height - tall;
        if (sea()) {
            var access = tiles("access");
            var levels = List.of("ALLOWED", "AVOID", "BLOCKED");
            for (int i = 0; i < Math.min(3, access.size()); i++) {
                var tile = access.get(i);
                String choice = "naval_access/" + levels.get(i), texture = "map_detail/access_" + i;
                boolean authorized = view().choices().stream().anyMatch(c -> c.id().equals(choice));
                String label = view().choices().stream().filter(c -> c.id().equals(choice)).map(DialogView.Choice::label).findFirst().orElse(tile.detail().split("\\R", 2)[0]);
                var button = new UiPanelButton(label, x(21+i*35), y(248), n(31), n(33), () -> {
                    if (accessPendingRevision >= 0 || view().choices().stream().noneMatch(c -> c.id().equals(choice))) return;
                    accessPendingRevision = view().revision();
                    choose(choice);
                    rebuildWidgets();
                }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        if (authorized) UiAssets.draw(g, texture, getX(), getY(), getWidth(), getHeight());
                        else UiAssets.drawGrayscale(g, texture, getX(), getY(), getWidth(), getHeight());
                        if (tile.value().equals("selected")) UiAssets.draw(g, "map_detail/access_highlight", getX(), getY(), getWidth(), getHeight());
                    }
                };
                button.active = authorized && accessPendingRevision < 0;
                addRenderableWidget(button);
            }
        }
        addRenderableWidget(new UiMenuButton("×", x(sea()?398:425), y(sea()?5:9), n(25), n(25), this::onClose));
        if(dev.hoi.client.ClientCampaignHud.visible())UiMenuBar.buttons(width,dev.hoi.client.ClientCampaignHud.country(),null,dev.hoi.client.HoiClient::openMenu).forEach(this::addRenderableWidget);
    }
    private int x(double n) { return left + (int)Math.round(n * scale); }
    private int y(double n) { return top + (int)Math.round(n * scale); }
    private int n(double n) { return (int)Math.round(n * scale); }
    private List<DialogView.Tile> tiles(String section) { return view().tiles().stream().filter(t -> t.section().equals(section)).toList(); }
    private void art(GuiGraphicsExtractor g, String path, int x, int y, int w, int h) { UiAssets.draw(g,path,x(x),y(y),n(w),n(h)); }
    private void label(GuiGraphicsExtractor g,String value,int x,int y,int w,int h,int color) {
        label(g,value,x,y,w,h,color,12,true);
    }
    private void label(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color,int size,boolean centered) {
        label(g,value,px,py,w,h,color,size,centered,true);
    }
    private void label(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color,int size,boolean centered,boolean wrap) {
        label(g,value,px,py,w,h,color,size,centered,wrap,false);
    }
    private void label(GuiGraphicsExtractor g,String value,int px,int py,int w,int h,int color,int size,boolean centered,boolean wrap,boolean bold) {
        float textScale=(float)(Math.max(10,size*scale)/font.lineHeight);
        var text=UiTooltips.sentences(Component.literal(value).withStyle(style->style.withBold(bold)));
        if(!wrap)textScale=Math.min(textScale,n(w)/(float)Math.max(1,font.width(text)));
        var lines=UiTooltips.split(font,text,Math.max(1,(int)(n(w)/textScale)));
        if(lines.isEmpty())return;
        var line=wrap?lines.getFirst():text.getVisualOrderText();
        float offset=centered?(n(w)-font.width(line)*textScale)/2:0;
        g.pose().pushMatrix();
        g.pose().translate(x(px)+offset,y(py)+(n(h)-font.lineHeight*textScale)/2);
        g.pose().scale(textScale);
        g.text(font,line,0,0,color);
        g.pose().popMatrix();
    }
    private void cell(GuiGraphicsExtractor g,DialogView.Tile tile,int x,int y,int w,int h,boolean standing) {
        art(g,standing?"map_detail/standing":"map_detail/shared",x,y,w,h);
        double cellScale=Math.min(n(w)/(double)w,n(h)/(double)h);
        int cellWidth=(int)Math.round(w*cellScale),cellHeight=(int)Math.round(h*cellScale);
        int cellLeft=x(x)+(n(w)-cellWidth)/2,cellTop=y(y)+(n(h)-cellHeight)/2;
        int slotSourceX=standing?4:2,slotSourceY=standing?9:3,slotExtension=standing?-1:1;
        int slotX=cellLeft+(int)Math.round(slotSourceX*cellWidth/(double)w);
        int slotY=cellTop+(int)Math.round(slotSourceY*cellHeight/(double)h);
        int slotRight=cellLeft+(int)Math.round((slotSourceX+7)*cellWidth/(double)w);
        int slotBottom=cellTop+(int)Math.round((slotSourceY+40)*cellHeight/(double)h);
        float slotLeft=standing?slotX-.5f:slotX,slotEnd=standing?slotRight+.5f:slotRight+slotExtension;
        var frame=net.minecraft.resources.Identifier.parse("hoi:textures/gui/map_detail/"+(standing?"standing":"shared")+".png");
        g.pose().pushMatrix();
        g.pose().translate(slotLeft,slotY);
        g.blit(frame,0,0,(int)(slotEnd-slotLeft),slotBottom-slotY,slotSourceX/(float)w,(slotSourceX+7f)/w,slotSourceY/(float)h,(slotSourceY+40f)/h);
        g.pose().popMatrix();
        if(!tile.icon().isEmpty())art(g,tile.icon(),x+(standing?12:10),y+(standing?7:2),45,45);
        if(!tile.overlay().isEmpty())art(g,tile.overlay(),x+(standing?12:10),y+(standing?7:2),45,45);
        if(tile.icon().isEmpty()&&!standing)g.fill(x(x+2),y(y+2),x(x+w-2),y(y+h-2),0x99000000);
        if(standing){
            art(g,"map_detail/level_bg",x+(w-50)/2,y+50,50,28);
            g.pose().pushMatrix();
            g.pose().translate(tile.section().equals("province")&&tile.icon().endsWith("/port")?1:0,tile.section().equals("province")||tile.section().equals("state")?1:0);
            label(g,level(tile.value()),x+(w-50)/2,y+50,50,28,UiMenuStyle.TEXT,14,true);
            g.pose().popMatrix();
        }
        if(tile.icon().isEmpty()&&!standing)label(g,tile.value(),x,y,w,h,UiMenuStyle.TEXT);
        hovers.add(new MapHover(tile,x(x),y(y),n(w),n(h)));
        if(tile.gauge()!=null) {
            int gaugeWidth=Math.max(1,(int)Math.round(5*cellScale)),gaugeHeight=Math.max(1,(int)Math.round(38*cellScale));
            float gaugeX=(slotLeft+slotEnd-gaugeWidth)/2f;
            float gaugeY=(slotY+slotBottom-gaugeHeight)/2f;
            g.enableScissor((int)Math.floor(gaugeX),(int)Math.floor(gaugeY),(int)Math.ceil(gaugeX+gaugeWidth),(int)Math.ceil(gaugeY+gaugeHeight));
            g.pose().pushMatrix();
            g.pose().translate(gaugeX,gaugeY+gaugeHeight);
            g.pose().rotate((float)(-Math.PI/2));
            g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/map_detail/damage_bg.png"),0,0,gaugeHeight,gaugeWidth,0,1,0,1);
            float fraction=(float)tile.gauge().fraction();
            if(fraction>0)g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/map_detail/damage_bar.png"),0,0,Math.round(gaugeHeight*fraction),gaugeWidth,0,fraction,0,1);
            g.pose().popMatrix();
            g.disableScissor();
            var gauge=new DialogView.Tile(tile.section(),tile.name(),"","",tile.gauge().tooltip());
            hovers.addFirst(new MapHover(gauge,(int)Math.floor(slotLeft),slotY,(int)Math.ceil(slotEnd)-(int)Math.floor(slotLeft),slotBottom-slotY));
        }
    }
    private static String level(String value) {
        String[] parts=value.split("/");
        if(value.equals("0/0"))return "§7"+value+"§r";
        if(parts.length==2&&parts[0].equals(parts[1]))return "§a"+value+"§r";
        return value;
    }
    private void flags(GuiGraphicsExtractor g,List<DialogView.Tile> values,UiFlagStack stack,int py) {
        var order=stack.update(values.stream().map(t->t.section()+":"+t.icon()).toList());
        for(int i=0;i<order.size();i++) {
            String id=order.get(i);
            var tile=values.stream().filter(t->id.equals(t.section()+":"+t.icon())).findFirst().orElseThrow();
            int px=UiFlagStack.left(i,order.size(),x(20),n(120),n(52));
            UiFlagStack.draw(g,tile.icon(),px,y(py),n(52),n(38));
            hovers.add(new MapHover(tile,px,y(py),UiFlagStack.visibleWidth(i,order.size(),x(20),n(120),n(52)),n(38)));
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        hovers.clear();
        if(!DialogCoordinator.renderingContent())dev.hoi.client.ClientCampaignHud.draw(g);
        art(g,sea()?"map_detail/sea":"map_detail/state",0,0,sea()?433:463,sea()?300:683);
        if(!sea())label(g,view().title(),13,5,436,30,UiMenuStyle.TEXT,24,true,true,true);
        if(sea())sea(g);else land(g);
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.Renderable r)r.extractRenderState(g,mx,my,delta);
        var current=hovers.stream().filter(h->mx>=h.x()&&mx<h.x()+h.w()&&my>=h.y()&&my<h.y()+h.h()).map(MapHover::tile).findFirst().orElse(null);
        if(!Objects.equals(current,hovered))tooltipScroll=0;
        hovered=current;
        if(current!=null) {
            String detail=current.detail();
            if(detail.isBlank())detail="§e"+current.name()+"§r"+(current.value().isEmpty()?"":"\n"+current.value());
            else if(current.section().equals("features")&&!detail.split("\\R",2)[0].replaceAll("§.","").equals(current.name()))detail="§e"+current.name()+"§r\n"+detail;
            float textScale=(float)(Math.max(10,16*scale)/font.lineHeight);
            g.pose().pushMatrix();
            g.pose().scale(textScale);
            tooltipScroll=UiPoliticalTooltip.drawDetail(g,font,detail,(int)(mx/textScale),(int)(my/textScale),
                    (int)(width/textScale),(int)(height/textScale),tooltipScroll);
            g.pose().popMatrix();
        }
    }
    private void land(GuiGraphicsExtractor g) {
        label(g,"주 소유국",10,41,140,20,UiMenuStyle.TEXT);
        var owners=new ArrayList<>(tiles("owner"));owners.addAll(tiles("controller"));
        flags(g,owners,ownerFlags,54);
        label(g,"타국의 주장",10,97,140,20,UiMenuStyle.TEXT);
        flags(g,tiles("claims"),claimFlags,109);
        var population=tiles("population");
        if(!population.isEmpty()){art(g,"map_detail/population",15,154,38,35);g.pose().pushMatrix();g.pose().translate(0,1);label(g,population.getFirst().value(),54,161,82,21,0xFF000000|UiTooltips.GOLD);g.pose().popMatrix();hovers.add(new MapHover(population.getFirst(),x(15),y(154),n(124),n(38)));}
        for(String section:List.of("resistance","compliance")) {
            var values=tiles(section);if(values.isEmpty())continue;
            var tile=values.getFirst();int py=section.equals("resistance")?198:233;
            art(g,tile.icon(),14,py-5,30,32);
            g.pose().pushMatrix();g.pose().translate(-1,1);
            art(g,"map_detail/"+section+"_bg",50,py,86,22);
            double fraction=0;
            try{fraction=Math.clamp(Double.parseDouble(tile.value().replace("%",""))/100,0,1);}catch(NumberFormatException ignored){}
            if(tile.gauge()!=null)fraction=tile.gauge().fraction();
            int barLeft=x(50)+Math.round(n(86)*3f/87),barTop=y(py)+Math.round(n(22)*2f/19);
            int barRight=x(50)+Math.round(n(86)*84f/87),barBottom=y(py)+Math.round(n(22)*16f/19);
            if(fraction>0) {
                g.enableScissor(barLeft,barTop,barLeft+(int)Math.round((barRight-barLeft)*fraction),barBottom);
                UiAssets.draw(g,"map_detail/"+section+"_bar",barLeft,barTop,barRight-barLeft,barBottom-barTop);
                g.disableScissor();
            }
            var targets=tiles("resistance_target");
            if(section.equals("resistance")&&!targets.isEmpty()&&targets.getFirst().gauge()!=null) {
                var target=targets.getFirst();
                int markerWidth=Math.round(n(86)*10f/87),markerHeight=Math.round(n(22)*24f/19);
                int markerLeft=barLeft+(int)Math.round((barRight-barLeft)*target.gauge().fraction())-markerWidth/2;
                int markerTop=y(py)-Math.round(n(22)*3f/19);
                UiAssets.draw(g,target.icon(),markerLeft,markerTop,markerWidth,markerHeight);
                hovers.add(new MapHover(target,markerLeft-1,markerTop+1,markerWidth,markerHeight));
            }
            label(g,tile.value(),50,py,86,22,UiMenuStyle.TEXT);
            g.pose().popMatrix();
            hovers.add(new MapHover(tile,x(14),y(py-5),n(122),n(32)));
        }
        var buildings=tiles("state");for(int i=0;i<buildings.size();i++)cell(g,buildings.get(i),153+i*59,47,62,84,true);
        var slots=tiles("slots");
        String slotValue=slots.isEmpty()?"0":"§6"+slots.getFirst().value().replace("/","§f/§6");
        float slotScale=(float)(Math.max(10,12*scale)/font.lineHeight);
        var slotNumber=UiTooltips.sentences(Component.literal(slotValue));
        int numberX=font.width("열린 슬롯 ");
        g.pose().pushMatrix();
        g.pose().translate(x(165),y(143)+(n(20)-font.lineHeight*slotScale)/2);
        g.pose().scale(slotScale);
        g.text(font,Component.literal("열린 슬롯 "),0,-1,UiMenuStyle.TEXT);
        g.text(font,slotNumber,numberX,-1,UiMenuStyle.TEXT);
        g.text(font,Component.literal("개"),numberX+font.width(slotNumber),-1,UiMenuStyle.TEXT);
        g.pose().popMatrix();
        if(!slots.isEmpty())hovers.add(new MapHover(slots.getFirst(),x(149),y(137),n(289),n(25)));
        var shared=tiles("shared");
        boolean unknown=shared.isEmpty();
        sharedScroll=Math.clamp(sharedScroll,0,Math.max(0,(shared.size()+4)/5-5));
        for(int i=0;i<25;i++) {
            int index=sharedScroll*5+i;
            if(index>=shared.size()&&!unknown)break;
            var t=unknown?new DialogView.Tile("shared","정보 부족","0","map_detail/unknown","이 주의 공장에 대한 정보가 없습니다"):shared.get(index);
            cell(g,t,149+i%5*58,162+i/5*47,56,46,false);
        }
        var resources=tiles("resources");
        for(int i=0;i<resources.size();i++) {
            int step=resources.size()>6?24:31,cellHeight=step-2;
            var t=resources.get(i);int px=32+i%2*50,py=302+i/2*step;
            int offset=t.value().replaceAll("§.", "").replaceAll("[^0-9]", "").length()>3?-1:0;
            g.pose().pushMatrix();g.pose().translate(offset,0);
            art(g,t.icon(),px,py,14,cellHeight);label(g,t.value(),px+16,py,34,cellHeight,UiMenuStyle.TEXT,12,false,false);
            g.pose().popMatrix();
            hovers.add(new MapHover(t,x(px)+offset,y(py),n(46),n(cellHeight)));
        }
        label(g,"주 변동치",44,398,200,19,UiMenuStyle.TEXT,12,false);
        var features=tiles("features");
        for(int i=0;i<features.size()&&i<6;i++) {
            var t=features.get(i);int px=5+i*64;
            art(g,t.icon(),px,421,64,64);
            hovers.add(new MapHover(t,x(px),y(419),n(64),n(65)));
        }
        for(String section:List.of("energy","building_modifiers")) {
            var values=tiles(section);if(values.isEmpty())continue;
            var t=values.getFirst();int px=section.equals("energy")?432:421,py=section.equals("energy")?392:450;
            if(section.equals("energy"))art(g,"map_detail/energy_bg",416,395,44,26);
            art(g,t.icon(),px,py,section.equals("energy")?32:36,section.equals("energy")?32:36);
            if(section.equals("energy"))label(g,t.value(),416,395,22,26,UiMenuStyle.TEXT,12,true);
            hovers.add(new MapHover(t,x(section.equals("energy")?416:px),y(py),n(section.equals("energy")?48:36),n(36)));
        }
        var terrain=tiles("terrain");label(g,"지역: §e"+(terrain.isEmpty()?view().subtitle():terrain.getFirst().value()),26,487,300,25,UiMenuStyle.TEXT,14,false);
        art(g,view().image(),26,510,413,70);
        int statusX=402;
        for(String section:List.of("province_status","province_weather","province_surface")) {
            var status=tiles(section);if(status.isEmpty())continue;
            UiAssets.draw(g,status.getFirst().icon(),x(statusX)-2,y(529)+2,n(37),n(47));
            hovers.add(new MapHover(status.getFirst(),x(statusX)-2,y(529)+2,n(37),n(47)));
            statusX-=37;
        }
        if(!terrain.isEmpty())hovers.add(new MapHover(terrain.getFirst(),x(20),y(487),n(419),n(94)));
        var province=tiles("province").stream().sorted(Comparator.comparingInt(t->List.of("construction/hub","construction/port","construction/fort","construction/coastal_fort").indexOf(t.icon().replace("map_detail/disabled/","construction/")))).toList();for(int i=0;i<province.size();i++)cell(g,province.get(i),17+i*61,588,62,84,true);
    }
    private void sea(GuiGraphicsExtractor g) {
        var weather=tiles("weather");
        art(g,weather.isEmpty()||weather.getFirst().icon().isEmpty()?"map_detail/ocean":weather.getFirst().icon(),10,36,413,70);
        art(g,"map_detail/sea_header",0,-2,435,62);
        label(g,view().title(),42,6,356,28,UiMenuStyle.TEXT,20,true,true,true);
        art(g,"map_detail/sea_dominance_header_bg",10,106,414,37);
        art(g,"map_detail/dominance",90,114,28,16);
        int frameWidth=n(191),frameHeight=n(20);
        int frameLeft=(x(10)+x(424)-frameWidth)/2,frameTop=(y(106)+y(143)-n(19))/2;
        int barWidth=n(183),barHeight=n(11),barLeft=frameLeft+(frameWidth-barWidth)/2,barTop=frameTop+(n(19)-barHeight)/2;
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/map_detail/progress_disabled.png"),barLeft,barTop,barLeft+barWidth,barTop+barHeight,0,1,0,1);
        var supremacy=tiles("supremacy");
        String value=supremacy.isEmpty()?"—":supremacy.getFirst().value();
        double fraction=value.endsWith("%")?Math.clamp(Double.parseDouble(value.substring(0,value.length()-1))/100,0,1):0;
        g.enableScissor(barLeft,barTop,barLeft+(int)Math.round(barWidth*fraction),barTop+barHeight);
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/map_detail/progress_good.png"),barLeft,barTop,barLeft+barWidth,barTop+barHeight,0,1,0,1);g.disableScissor();
        UiAssets.draw(g,"map_detail/progress_frame",frameLeft,frameTop,frameWidth,frameHeight);
        int markerWidth=n(16),markerHeight=n(12),markerLeft=barLeft+(int)Math.round(barWidth*.6)-markerWidth/2,markerY=barTop+(barHeight-markerHeight)/2;
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/map_detail/marker.png"),markerLeft,markerY,markerLeft+markerWidth,markerY+markerHeight,0,1,0,1);
        var marker=tiles("marker");
        if(!marker.isEmpty())hovers.add(new MapHover(marker.getFirst(),markerLeft,markerY,markerWidth,markerHeight));
        if(!supremacy.isEmpty())hovers.add(new MapHover(supremacy.getFirst(),x(90),y(106),n(221),n(37)));
        var environmentIcons=new ArrayList<>(tiles("weather_icon"));
        environmentIcons.addAll(tiles("sea_condition"));
        for(int i=0;i<environmentIcons.size();i++) {
            var icon=environmentIcons.get(i);int left=384-i*37;
            art(g,icon.icon(),left,62,37,47);
            hovers.add(new MapHover(icon,x(left),y(62),n(37),n(47)));
        }
        var terrain=tiles("sea_terrain");
        if(!terrain.isEmpty())hovers.add(new MapHover(terrain.getFirst(),x(10),y(36),n(413),n(70)));
        int[] px={16,128,218,307},tx={47,138,193,304},tw={80,100,100,80};
        String[][] icons={{"sea_convoys_icon","sea_mine_icon","sea_enemy_mine_icon","sea_enemy_convoys_icon"},{"sea_ship_icon","sea_plane_icon","sea_enemy_plane_icon","sea_enemy_ship_icon"}};
        for(int row=0;row<2;row++) {
            var items=tiles(row==0?"naval":"air");
            for(int i=0;i<4;i++) {
                int py=148+row*38;
                art(g,"map_detail/"+icons[row][i],px[i],py,i==0||i==3?109:86,33);
                if(i>=items.size())continue;
                var t=items.get(i);
                String statistic=t.value().isBlank()||t.value().equals("—")?(i==1||i==2?"0%":"0"):t.value();
                g.pose().pushMatrix();
                g.pose().translate(i==0?2:0,row==1?2:1);
                label(g,statistic,tx[i],py+8,tw[i],18,UiMenuStyle.TEXT);
                g.pose().popMatrix();
                hovers.add(new MapHover(t,x(px[i]),y(py),n(i==0||i==3?109:88),n(33)));
            }
        }
        label(g,"접근",21,225,70,23,UiMenuStyle.TEXT,12,false);
        var access=tiles("access");
        for(int i=0;i<3;i++) {
            if(i<access.size())hovers.add(new MapHover(access.get(i),x(21+i*35),y(248),n(31),n(33)));
        }
    }
    String flagAt(double px,double py) {
        for(var hover:hovers)if(px>=hover.x()&&px<hover.x()+hover.w()&&py>=hover.y()&&py<hover.y()+hover.h()
                &&hover.tile().icon().matches("country/[a-z]{3}/flag"))
            return hover.tile().icon().substring(8,11).toUpperCase(Locale.ROOT);
        return "";
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice) {
        if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            String country=flagAt(event.x(),event.y());
            if(!country.isEmpty()){dev.hoi.client.HoiClient.openCountry(country);return true;}
        }
        if(event.y()<top||event.x()>=pane) {
            if(event.y()<UiMenuBar.height(width))return super.mouseClicked(event,twice);
            if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)return dev.hoi.client.input.InputMapInfo.clickAt(minecraft,event.x(),event.y(),width,height);
        }
        super.mouseClicked(event,twice);return true;
    }
    @Override public boolean mouseScrolled(double x,double y,double h,double v) {
        if(hovered!=null&&!hovered.detail().isEmpty()){tooltipScroll-=(int)v;return true;}
        if(!sea()&&x>=x(150)&&x<pane&&y>=y(170)&&y<y(400)){sharedScroll-=(int)v;return true;}
        return false;
    }
    @Override public boolean allowsMovement(){return true;}
    @Override public int unitHudLeft(){return pane;}
    @Override public boolean keyPressed(KeyEvent e){return InputSidebarMovement.consumes(minecraft,e)||super.keyPressed(e);}
    @Override public boolean keyReleased(KeyEvent e){return InputSidebarMovement.consumes(minecraft,e)||super.keyReleased(e);}
    @Override public void removed(){InputSidebarMovement.release(minecraft);}
}
