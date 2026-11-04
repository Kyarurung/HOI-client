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
            }
        } finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(previous)); }
    }
    private static Button access(MapDetail screen,int index) {
        return screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiPanelButton).map(w->(Button)w).toList().get(index);
    }
    private static void click(MapDetail screen,Button button,int mouseButton) {
        var event=new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,new MouseButtonInfo(mouseButton,0));
        screen.mouseClicked(event,false); screen.mouseReleased(event);
    }
}
