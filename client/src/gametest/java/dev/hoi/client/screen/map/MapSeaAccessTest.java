package dev.hoi.client.screen.map;

import dev.hoi.protocol.dialog.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.*;

public final class MapSeaAccessTest {
    private MapSeaAccessTest() {}
    public static void run(ClientGameTestContext game) {
        var campaignRoot = Path.of(System.getenv("HOI_CAMPAIGN_PREVIEW"));
        var root = campaignRoot.resolve(Files.isDirectory(campaignRoot.resolve("cmp82")) ? "cmp82" : "cmp80-6");
        var views = new ArrayList<DialogView>();
        try {
            for (String level : List.of("allowed", "avoid", "blocked")) views.add(new DialogPayloads.Show(Files.readString(root.resolve("sea-detail-kor-76-" + level + ".json")), true).view());
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        int previous = game.computeOnClient(c -> c.options.guiScale().get());
        try {
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(c -> c.options.guiScale().set(2));
                var sent = new ArrayList<DialogPayloads.Action>();
                var screen = game.computeOnClient(c -> new MapDetail(views.getFirst(), (v, choice) -> sent.add(new DialogPayloads.Action(v.token(),v.revision(),choice))));
                game.setScreen(() -> screen);
                for (int i = 0; i < 3; i++) {
                    int index = i;
                    game.runOnClient(c -> {
                        if (index > 0) screen.update(views.get(index));
                        var view = screen.view();
                        var button = access(screen,index);
                        sent.clear();
                        click(screen,button,InputConstants.MOUSE_BUTTON_RIGHT);
                        if (!sent.isEmpty()) throw new AssertionError("Right click must not change sea access: " + sent);
                        click(screen,button,InputConstants.MOUSE_BUTTON_LEFT);
                        click(screen,access(screen,index),InputConstants.MOUSE_BUTTON_LEFT);
                        var expected = view.choices().get(index).id();
                        if (sent.size()!=1 || !sent.getFirst().choice().equals(expected) || sent.getFirst().revision()!=view.revision() || !sent.getFirst().token().equals(view.token())) throw new AssertionError("Sea access request contract: " + sent);
                        if (!screen.view().equals(view)) throw new AssertionError("Sea selection must await server response");
                        screen.update(view);
                        click(screen,access(screen,index),InputConstants.MOUSE_BUTTON_LEFT);
                        if (sent.size()!=1) throw new AssertionError("Same revision must not unlock repeated requests");
                        screen.update(views.getFirst().issued(UUID.randomUUID().toString(),view.revision()+10));
                        screen.update(views.getFirst());
                        if (!screen.view().equals(view)) throw new AssertionError("Foreign/stale sea response accepted");
                    });
                    var point = game.computeOnClient(c -> { var b=access(screen,index); return new double[]{(b.getX()+b.getWidth()/2.0)*2,(b.getY()+b.getHeight()/2.0)*2}; });
                    game.getInput().setCursorPos(point[0],point[1]); game.waitTicks(3);
                    game.takeScreenshot("hoi-sea-access-"+List.of("allowed","avoid","blocked").get(index)+"-"+size[0]+"-gui2");
                }
                game.runOnClient(c -> {
                    var gson = new com.google.gson.Gson();
                    var json = gson.toJsonTree(views.getLast()).getAsJsonObject();
                    json.addProperty("revision",views.getLast().revision()+1);
                    json.add("choices",new com.google.gson.JsonArray());
                    for (var tile:json.getAsJsonArray("tiles")) if (tile.getAsJsonObject().get("section").getAsString().equals("access")) tile.getAsJsonObject().addProperty("value","");
                    screen.update(gson.fromJson(json,DialogView.class));
                    sent.clear();
                    for(int i=0;i<3;i++) { var b=access(screen,i); if(b.active)throw new AssertionError("Spectator access enabled"); click(screen,b,InputConstants.MOUSE_BUTTON_LEFT); }
                    if(!sent.isEmpty())throw new AssertionError("Spectator sent access request");
                });
                game.waitTicks(3); game.takeScreenshot("hoi-sea-access-spectator-"+size[0]+"-gui2");
                presence(game,size[0]);
            }
        } finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(previous)); }
    }
    private static void presence(ClientGameTestContext game,int width) {
        DialogView view;
        try { view = new DialogPayloads.Show(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"),"kor-sea-region.json")), true).view(); } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        var sent = new ArrayList<String>();
        var screen = game.computeOnClient(c -> new MapDetail(view, (v, choice) -> sent.add(choice)));
        game.setScreen(() -> screen);
        game.getInput().setCursorPos(width-8,8); game.waitTicks(3);
        var bar = view.tiles().stream().filter(t -> t.section().equals("dominance")).findFirst().orElseThrow();
        double ratio = screen.panelWidth()/433.0, top = screen.panelTop();
        try (var image = com.mojang.blaze3d.platform.NativeImage.read(Files.newInputStream(game.takeScreenshot("hoi-sea-presence-"+width+"-gui2")))) {
            int y = (int)Math.round((top+124.5*ratio)*2);
            for (double part : new double[]{bar.gauge().fraction()/2,(1+bar.gauge().fraction())/2}) {
                if (Math.abs(part-.656)<.05) continue;
                int color = image.getPixel((int)Math.round((125.5+183*part)*ratio*2),y), red = color>>16&255, blue = color&255;
                if (part<bar.gauge().fraction() ? blue<=red : red<=blue) throw new AssertionError("The dominance bar draws the own share blue and the hostile share red at "+part+": "+Integer.toHexString(color));
            }
        } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        String flag = view.tiles().stream().filter(t -> t.section().equals("dominant")).findFirst().orElseThrow().icon().substring(8,11).toUpperCase(Locale.ROOT);
        if (!game.computeOnClient(c -> screen.flagAt(373*ratio,top+123*ratio)).equals(flag)) throw new AssertionError("The dominating country shows its flag");
        game.getInput().setCursorPos((378+18)*ratio*2,(top+(245+18)*ratio)*2); game.waitTicks(4);
        game.takeScreenshot("hoi-sea-sunk-ships-"+width+"-gui2");
        var overviews = new ArrayList<dev.hoi.protocol.commander.CommanderOverviewPayloads.Request>(); var records = new ArrayList<dev.hoi.protocol.commander.CommanderRecordPayloads.Request>();
        game.runOnClient(c -> { dev.hoi.client.screen.commander.CommanderOverview.requestObserver = overviews::add; dev.hoi.client.map.commander.CommanderCombatLogHud.requestObserver = records::add; });
        try {
            game.runOnClient(c -> {
                var button = screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiPanelButton).map(w->(Button)w).toList().get(3);
                click(screen,button,InputConstants.MOUSE_BUTTON_LEFT);
                if (!sent.equals(List.of(""))) throw new AssertionError("Sunk ships close the sea panel on the server: "+sent);
                if (!dev.hoi.client.map.commander.CommanderHud.overviewShown(dev.hoi.protocol.commander.CommanderOverviewPayloads.Tab.NAVY)) throw new AssertionError("Sunk ships open the navy overview");
                if (overviews.isEmpty() || records.size()!=1 || records.getFirst().action()!=dev.hoi.protocol.commander.CommanderRecordPayloads.Action.NAVY) throw new AssertionError("Sunk ships ask for the naval losses: "+records);
            });
            game.setScreen(() -> null);
        } finally { game.runOnClient(c -> { dev.hoi.client.screen.commander.CommanderOverview.requestObserver = null; dev.hoi.client.map.commander.CommanderCombatLogHud.requestObserver = null; }); }
    }
    private static Button access(MapDetail screen,int index) {
        return screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiPanelButton).map(w->(Button)w).toList().get(index);
    }
    private static void click(MapDetail screen,Button button,int mouseButton) {
        var event=new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(mouseButton,0));
        screen.mouseClicked(event,false); screen.mouseReleased(event);
    }
}
