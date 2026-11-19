package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.screen.decision.DecisionScreen;
import dev.hoi.client.screen.decision.DecisionText;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.dialog.DialogStack;
import dev.hoi.client.screen.diplomacy.DiplomacyLease;
import dev.hoi.client.screen.diplomacy.DiplomacyVolunteers;
import dev.hoi.client.screen.research.ResearchScreen;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiStoryAssets;
import dev.hoi.client.ui.country.CountryDisplays;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.country.CountryDisplay;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.decision.DecisionPayloads;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.protocol.diplomacy.DiplomacyView;
import dev.hoi.protocol.diplomacy.DiplomacyVolunteerView;
import dev.hoi.protocol.hud.HudPayloads;
import dev.hoi.protocol.industry.IndustryView;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.protocol.menu.MenuView;
import dev.hoi.protocol.research.ResearchView;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenAuthoritativeFieldsTest {
    private ScreenAuthoritativeFieldsTest() {}
    private static <T> T read(String variable,String name,Class<T> type) throws java.io.IOException {
        return new Gson().fromJson(Files.readString(Path.of(System.getenv(variable),name+".json")),type);
    }
    public static void run(ClientGameTestContext game,MenuView menu) {
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        var hud=game.computeOnClient(c->ClientCampaignHud.visible()?HudPayloads.State.of(ClientCampaignHud.country(),ClientCampaignHud.hud()):HudPayloads.State.HIDDEN);
        try {
            game.runOnClient(c->DialogCoordinator.reset());
            var industry=read("HOI_DESIGN_TEAM_PREVIEW","kor-designer-mio-unassigned",IndustryView.class);
            var order=List.of("OIL","ALUMINUM","RUBBER","TUNGSTEN","IRON","CHROMIUM","COAL");
            int multiple=0;
            for(var row:industry.equipment()) {
                var keys=List.copyOf(row.resources().keySet());
                if(!keys.equals(order.stream().filter(keys::contains).toList()))throw new AssertionError("Server equipment resource order: "+row.id()+" "+keys);
                if(keys.size()>1)multiple++;
            }
            if(multiple==0)throw new AssertionError("Current server fixture must exercise multiple resources");
            var answer=read("HOI_LEASE_REQUEST_FIELDS","jap-lease-answer-selected",DiplomacyView.class);
            if(answer.equipment().stream().noneMatch(e->!e.typeName().isEmpty()))throw new AssertionError("Current server must supply equipment type names");
            var volunteers=read("HOI_VOLUNTEER_PREVIEW","unselected",DiplomacyVolunteerView.class);
            if(volunteers.armies().stream().noneMatch(a->a.color()!=null))throw new AssertionError("Current server must supply army colors");
            if(volunteers.armies().stream().anyMatch(a->a.insignia()!=null))throw new AssertionError("Unassigned source fixture unexpectedly acquired an insignia");
            var event=read("HOI_LEADER_EVENT_PREVIEW","event-korea-21",DialogView.class).issued(UUID.randomUUID().toString(),1);
            var research=read("HOI_DESIGN_TEAM_PREVIEW","prc-research-slot-design-team",ResearchView.class);
            var probe=read("HOI_HUD_TOOLTIP_FIELDS","kor-command-panel-army-probing-active",dev.hoi.protocol.commander.CommanderHudPayloads.View.class);
            String tooltip=probe.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().abilityDetails().stream()).filter(a->a.id().equals("probing_attack")).map(a->a.tooltip()).collect(java.util.stream.Collectors.joining("\n"));
            if(tooltip.contains("dont_lose_dig_in_on_attack")||!tooltip.contains("공격 패퇴 중 참호구축 불가"))throw new AssertionError("Probing attack lost its original translated modifier label: "+tooltip);
            dev.hoi.client.ui.tooltip.TooltipSpacingTest.verify(game,tooltip);
            var navy=read("HOI_HUD_TOOLTIP_FIELDS","kor-command-panel-navy",dev.hoi.protocol.commander.CommanderHudPayloads.View.class);
            var details=navy.cards().stream().filter(c->c.panel()!=null).flatMap(c->c.panel().members().stream()).map(m->m.detail()).filter(s->s.contains("\n\n")).toList();
            if(details.isEmpty())throw new AssertionError("Current naval member fixture must contain server blank separators");
            for(String detail:details)dev.hoi.client.ui.tooltip.TooltipSpacingTest.verify(game,detail);
            game.runOnClient(c->{
                var lines=dev.hoi.client.ui.tooltip.Tooltips.sourceLines(c.font,"상단\n\n하단",400);
                if(lines.size()!=3||!dev.hoi.client.ui.tooltip.Tooltips.blank(lines.get(1)))throw new AssertionError("One server blank separator must remain exactly one rendered row");
            });
            for(int[] size:List.of(new int[]{2560,1440,2},new int[]{1280,720,2},new int[]{1600,1000,3})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(c->{c.options.guiScale().set(size[2]);CountryDisplays.clear();ClientCampaignHud.accept(HudPayloads.State.of(menu.country(),menu.hud()));});
                eventDays(game,event,menu,size);
                game.runOnClient(c->ClientCampaignHud.accept(HudPayloads.State.of(answer.viewer(),CountryHud.UNKNOWN)));
                game.setScreen(()->new DiplomacyLease(null,answer,DiplomacyLease.Presentation.ANSWER,r->{}));
                game.waitTicks(3);
                var leaseImage=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-k1-lease-answer-"+size[0]+"-gui"+size[2]).toFile());
                leaseNames(game,answer,size,leaseImage);
                shields(game,volunteers,size);
                game.runOnClient(c->ClientCampaignHud.accept(HudPayloads.State.of(research.country(),research.hud())));
                game.setScreen(()->new ResearchScreen(research,r->{}));game.waitTicks(3);
                dev.hoi.client.screen.research.ResearchSlotTooltipTest.empty(game);
                game.takeScreenshot("hoi-k1-research-date-empty-slots-"+size[0]+"-gui"+size[2]);
                var point=game.computeOnClient(c->{var button=c.gui.screen().children().stream().filter(w->w instanceof dev.hoi.client.screen.research.ResearchSlotButton b&&b.getMessage().getString().equals(dev.hoi.client.ui.UiCommandLabels.text("RESEARCH_NO_RESEARCH", "사용되지 않음"))).map(w->(Button)w).findFirst().orElseThrow();double scale=c.getWindow().getGuiScale();return new double[]{(button.getX()+button.getWidth()/2.)*scale,(button.getY()+button.getHeight()/2.)*scale};});
                game.getInput().setCursorPos(point[0],point[1]);game.waitTicks(3);
                game.takeScreenshot("hoi-k1-research-empty-slot-hover-no-invented-tooltip-"+size[0]+"-gui"+size[2]);
                liveHud(game,menu,size);
                flags(game,size);
            }
            System.out.println("K1 authoritative fields: "+industry.equipment().size()+" equipment rows, "+multiple+" multi-resource rows; "+answer.equipment().size()+" answer rows; "+volunteers.armies().size()+" army shields");
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {
            game.runOnClient(c->{DialogCoordinator.reset();CountryDisplays.clear();ClientCampaignHud.accept(hud);c.gui.setScreen(null);c.options.guiScale().set(previous);});
        }
    }
    private static void leaseNames(ClientGameTestContext game,DiplomacyView source,int[] size,BufferedImage before) throws java.io.IOException {
        var bounds=game.computeOnClient(c->{
            var input=c.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox b&&b.getMessage().getString().equals(source.equipment().getFirst().name())).map(w->(net.minecraft.client.gui.components.EditBox)w).findFirst().orElseThrow();
            double units=input.getWidth()/50d,scale=c.getWindow().getGuiScale();
            return new int[]{(int)Math.round((input.getX()-220*units)*scale),(int)Math.round((input.getY()-40*units)*scale),(int)Math.round(180*units*scale),(int)Math.round(71*units*scale)};
        });
        var json=new Gson().toJsonTree(source).getAsJsonObject();
        for(var choice:json.getAsJsonArray("equipment"))choice.getAsJsonObject().addProperty("typeName","Hidden type name regression");
        var changed=new Gson().fromJson(json,DiplomacyView.class);
        game.setScreen(()->new DiplomacyLease(null,changed,DiplomacyLease.Presentation.ANSWER,r->{}));game.waitTicks(3);
        var after=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-k1-lease-hidden-type-name-synthetic-"+size[0]+"-gui"+size[2]).toFile());
        if(!same(before,after,bounds[0],bounds[1],bounds[2],bounds[3]))throw new AssertionError("Equipment type name must not be painted over the equipment image");
    }
    private static void shields(ClientGameTestContext game,DiplomacyVolunteerView source,int[] size) throws java.io.IOException {
        game.runOnClient(c->ClientCampaignHud.accept(HudPayloads.State.of(source.viewer(),CountryHud.UNKNOWN)));
        game.setScreen(()->new DiplomacyVolunteers(null,source,r->{}));game.waitTicks(3);
        var image=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-k1-volunteer-unassigned-source-"+size[0]+"-gui"+size[2]).toFile());
        var button=game.computeOnClient(c->c.gui.screen().children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(source.armies().getFirst().name())).map(w->(Button)w).findFirst().orElseThrow());
        double scale=game.computeOnClient(c->c.getWindow().getGuiScale());
        int x=(int)Math.round(button.getX()*scale),y=(int)Math.round(button.getY()*scale),width=(int)Math.round(button.getWidth()*44/489d*scale),height=(int)Math.round(button.getHeight()*scale);
        var json=new Gson().toJsonTree(source).getAsJsonObject();
        json.getAsJsonArray("armies").get(0).getAsJsonObject().addProperty("insignia",7);
        var framed=new Gson().fromJson(json,DiplomacyVolunteerView.class);
        game.setScreen(()->new DiplomacyVolunteers(null,framed,r->{}));game.waitTicks(3);
        var drawn=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-k1-volunteer-frame7-synthetic-"+size[0]+"-gui"+size[2]).toFile());
        if(same(image,drawn,x,y,width,height))throw new AssertionError("Received non-null insignia did not render");
        json.getAsJsonArray("armies").get(0).getAsJsonObject().addProperty("color",source.armies().get(1).color());
        var colored=new Gson().fromJson(json,DiplomacyVolunteerView.class);
        game.setScreen(()->new DiplomacyVolunteers(null,colored,r->{}));game.waitTicks(3);
        var tinted=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-k1-volunteer-color-synthetic-"+size[0]+"-gui"+size[2]).toFile());
        if(same(drawn,tinted,x,y,width,height))throw new AssertionError("Received army color did not tint the insignia");
        json.getAsJsonArray("armies").get(0).getAsJsonObject().remove("color");
        var missing=new Gson().fromJson(json,DiplomacyVolunteerView.class);
        game.setScreen(()->new DiplomacyVolunteers(null,missing,r->{}));game.waitTicks(3);
        var hidden=javax.imageio.ImageIO.read(game.takeScreenshot("hoi-k1-volunteer-null-color-synthetic-"+size[0]+"-gui"+size[2]).toFile());
        if(!same(image,hidden,x,y,width,height))throw new AssertionError("Missing army color must not invent a default tint");
    }
    private static void eventDays(ClientGameTestContext game,DialogView source,MenuView menu,int[] size) throws java.io.IOException {
        var stack=game.computeOnClient(c->new DialogStack(null,(v,id)->{}));
        var view=new DecisionPayloads.View(menu.country(),UUID.randomUUID().toString(),1,"",0,0,menu.hud(),List.of());
        var screen=game.computeOnClient(c->new DecisionScreen(view,r->{}));
        game.runOnClient(c->{stack.add(source.withRemainingDays(12));screen.eventStack(stack);});
        game.setScreen(()->screen);game.waitTicks(3);
        var button=game.computeOnClient(c->screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(source.title())).map(w->(Button)w).findFirst().orElseThrow());
        double scale=game.computeOnClient(c->c.getWindow().getGuiScale());
        int x=(int)Math.round((button.getRight()-80)*scale),y=(int)Math.round(button.getY()*scale),w=(int)Math.round(80*scale),h=(int)Math.round(button.getHeight()*scale);
        BufferedImage previous=null;
        for(Integer days:new Integer[]{12,0,null}) {
            game.runOnClient(c->stack.add(source.withRemainingDays(days)));game.waitTicks(3);
            if(game.computeOnClient(c->screen.eventCount())!=1)throw new AssertionError("Deadline-only update duplicated or removed an event");
            var file=game.takeScreenshot("hoi-k1-event-days-"+days+"-"+size[0]+"-gui"+size[2]);
            var image=javax.imageio.ImageIO.read(file.toFile());
            if(previous!=null&&same(previous,image,x,y,w,h))throw new AssertionError("Event timer did not redraw "+days);
            previous=image;
        }
    }
    private static void liveHud(ClientGameTestContext game,MenuView menu,int[] size) {
        var json=new Gson().toJsonTree(menu.hud()).getAsJsonObject();
        long manpower=1234567;
        json.getAsJsonObject("national").addProperty("manpower",manpower);
        var next=new Gson().fromJson(json,CountryHud.class);
        var paused=new MenuView(menu.country(),menu.countryName(),menu.date(),"PAUSED",menu.pages(),menu.hud(),menu.manufacturers(),menu.balance());
        game.runOnClient(c->ClientCampaignHud.accept(HudPayloads.State.of(menu.country(),menu.hud())));
        game.setScreen(()->new ScreenMenu(paused,MenuTab.POLITICS,()->{}));game.waitTicks(2);
        var backdrop=game.computeOnClient(c->c.gui.screen());
        game.setScreen(()->new DialogStack(backdrop));
        game.runOnClient(c->{
            ClientCampaignHud.accept(HudPayloads.State.of(menu.country(),next));
            if(!HudMenuBar.snapshot(menu.hud()).equals(next))throw new AssertionError("Open paused menu masked the next HUD");
            var item=HudMenuBar.indicators(menu.hud()).stream().filter(i->i.icon().equals("manpower")).findFirst().orElseThrow();
            if(item.raw().longValue()!=manpower)throw new AssertionError("Manpower indicator retained stale menu state");
        });
        game.waitTicks(2);game.takeScreenshot("hoi-k1-paused-hud-update-synthetic-"+size[0]+"-gui"+size[2]);
        game.runOnClient(c->{ClientCampaignHud.accept(HudPayloads.State.HIDDEN);if(HudMenuBar.snapshot(menu.hud())!=menu.hud())throw new AssertionError("Hidden live HUD must retain the supplied fallback");});
    }
    private static void flags(ClientGameTestContext game,int[] size) throws java.io.IOException {
        var text=new DecisionText();
        game.runOnClient(c->{
            try(var reader=c.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:ui/decision_flags.json")).orElseThrow().openAsReader()) {
                text.flags(JsonParser.parseReader(reader).getAsJsonObject());
            } catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
        });
        var line=game.computeOnClient(c->text.component("[KOR.GetFlag]§Y 대한민국§!").getVisualOrderText());
        String[] key={"KOR"};
        int flagWidth=game.computeOnClient(c->Math.max(1,c.font.width(text.component("[KOR.GetFlag]"))-1)),lineHeight=game.computeOnClient(c->c.font.lineHeight);
        game.setScreen(()->new Screen(Component.literal("현재 국가 표시 국기")) {
            @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
                g.fill(0,0,width,height,0xFF101010);
                text.draw(g,font,line,10,10,0xFFFFFFFF,false);
                if(key[0].equals("KOR_empire"))UiAssets.drawContent(g,UiStoryAssets.flag("KOR",key[0]),10,30,flagWidth,lineHeight);
                else g.text(font,text.component("[KOR.GetFlag]").getVisualOrderText(),10,30,0xFFFFFFFF,false);
            }
        });
        int stage=0;
        for(String flag:List.of("KOR","KOR_empire","unavailable_source_key","KOR","KOR_empire")) {
            key[0]=flag;
            game.runOnClient(c->{CountryDisplays.accept(new CountryDisplay.Table(List.of(new CountryDisplay.Entry("KOR","대한민국",flag))));if(flag.equals("KOR_empire"))UiAssets.clear();});
            game.waitTicks(3);
            var file=game.takeScreenshot("hoi-k1-decision-flag-"+(stage++)+"-"+flag+"-"+size[0]+"-gui"+size[2]);
            var image=javax.imageio.ImageIO.read(file.toFile());
            double scale=game.computeOnClient(c->c.getWindow().getGuiScale());
            int x=(int)Math.round(10*scale),y=(int)Math.round(10*scale),reference=(int)Math.round(30*scale),w=(int)Math.round(flagWidth*scale),h=(int)Math.round(lineHeight*scale);
            for(int row=0;row<h;row++)for(int col=0;col<w;col++)if(image.getRGB(x+col,y+row)!=image.getRGB(x+col,reference+row))throw new AssertionError("Cached decision flag differs from current display: "+flag+" "+col+","+row);
        }
    }
    private static boolean same(BufferedImage before,BufferedImage after,int x,int y,int width,int height) {
        for(int row=0;row<height;row++)for(int col=0;col<width;col++)if(before.getRGB(x+col,y+row)!=after.getRGB(x+col,y+row))return false;
        return true;
    }
}
