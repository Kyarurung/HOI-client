package dev.hoi.client.ui.hud;

import dev.hoi.client.screen.ScreenMenu;
import dev.hoi.protocol.menu.MenuView;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;

public final class HudTooltipLayerTest {
    private HudTooltipLayerTest() {}
    public static void run(ClientGameTestContext game,MenuView menu) {
        try {
            for(int width:new int[]{2560,1280}) {
                game.getInput().resizeWindow(width,width*9/16);game.runOnClient(c->{c.options.guiScale().set(2);HudTooltip.clear();});
                var screen=game.computeOnClient(c->new ScreenMenu(menu));game.setScreen(()->screen);
                game.runOnClient(c->ScreenEvents.afterExtract(screen).register((s,g,mx,my,delta)->{g.nextStratum();g.fill(0,25,g.guiWidth(),g.guiHeight(),0xFF123456);}));
                game.runOnClient(c->screen.mouseScrolled(50,5,-100,0));
                int x=HudMenuBar.statsRight(width/2)-3-134+20;
                game.getInput().setCursorPos(x*2,12);game.waitTicks(4);
                var image=ImageIO.read(game.takeScreenshot("hoi-hud-tooltip-final-layer-"+width+"-gui2").toFile());
                int visible=0;
                for(int y=54;y<Math.min(image.getHeight()-8,350);y++)for(int px=Math.max(0,x*2-360);px<Math.min(image.getWidth(),x*2+360);px++)if((image.getRGB(px,y)&0xFFFFFF)!=0x123456)visible++;
                if(visible<100)throw new AssertionError("GDP tooltip must remain visible above a later opaque HUD layer");
            }
        } catch(java.io.IOException error){throw new AssertionError(error);}
        sourceHud(game);
    }
    private static void sourceHud(ClientGameTestContext game) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");
        if(directory==null)throw new AssertionError("Authoritative commander fixtures are required for overlapping HUD capture");
        var previous=game.computeOnClient(c->dev.hoi.client.ClientCampaignHud.visible()?dev.hoi.protocol.hud.HudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(),dev.hoi.client.ClientCampaignHud.hud()):dev.hoi.protocol.hud.HudPayloads.State.HIDDEN);
        try {
            var menu=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-appointment-menu.json")),MenuView.class);
            var source=new dev.hoi.protocol.commander.CommanderHudPayloads.Snapshot(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-command-panel-army-assigned.json"))).view();
            for(int width:new int[]{2560,1280})for(String icon:new String[]{"debt","gdp","political_power"}) {
                game.getInput().resizeWindow(width,width*9/16);
                game.runOnClient(c->{
                    c.options.guiScale().set(2);HudTooltip.clear();
                    dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.of(menu.country(),menu.hud()));
                    dev.hoi.client.map.commander.CommanderHud.reset();
                    dev.hoi.client.map.commander.CommanderHud.accept(new dev.hoi.protocol.commander.CommanderHudPayloads.View(source.country(),c.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),source.running(),source.theaters(),source.armyTheaters()));
                });
                game.setScreen(dev.hoi.client.screen.map.MapCursor::new);game.waitTicks(2);
                game.runOnClient(c->{
                    var card=dev.hoi.client.map.commander.CommanderHud.boxes().stream().filter(box->box.request()!=null&&box.request().mode()==dev.hoi.protocol.commander.CommanderHudPayloads.Mode.ASSIGN_ARMY).findFirst().orElseThrow();
                    dev.hoi.client.map.commander.CommanderHud.clickAt(card.x()+card.width()/2.,card.y()+card.height()/2.);
                });
                game.waitTicks(2);
                int x=game.computeOnClient(c->{
                    int guiWidth=width/2,maximum=HudMenuBar.maximumScroll(guiWidth,menu.hud()),offset=icon.equals("political_power")?0:maximum;
                    if(offset>0)c.gui.screen().mouseScrolled(50,5,-100,0);
                    var items=HudMenuBar.indicators(menu.hud());int left=HudMenuBar.statsLeft(guiWidth)-offset;
                    int padding=maximum==0?Math.max(0,HudMenuBar.statsRight(guiWidth)-HudMenuBar.statsLeft(guiWidth)-items.stream().mapToInt(HudMenuBar.HudIndicator::width).sum()-3):0;
                    for(var item:items){if(item.icon().equals("gdp"))left+=padding;if(item.icon().equals(icon))return left+item.width()/2;left+=item.width();}
                    throw new AssertionError("Missing HUD indicator "+icon);
                });
                game.getInput().setCursorPos(x*2,12);game.waitTicks(4);
                game.takeScreenshot("hoi-hud-"+icon+"-over-commander-source-"+width+"-gui2");
            }
        } catch(java.io.IOException error){throw new AssertionError(error);}
        finally{game.runOnClient(c->{dev.hoi.client.map.commander.CommanderHud.reset();dev.hoi.client.ClientCampaignHud.accept(previous);});}
    }
}
