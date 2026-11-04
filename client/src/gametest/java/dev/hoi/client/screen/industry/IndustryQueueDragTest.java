package dev.hoi.client.screen.industry;

import dev.hoi.protocol.country.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;

public final class IndustryQueueDragTest {
    private static final Gson JSON = new Gson();
    private static Object field(Object target, String name) {
        try { var field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static int method(Object target, String name) {
        try { var method = target.getClass().getDeclaredMethod(name); method.setAccessible(true); return (int) method.invoke(target); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); }
    private static void drag(Screen screen, double x, double from, double to) {
        screen.mouseClicked(mouse(x, from), false);
        screen.mouseDragged(mouse(x, to), 0, to - from);
        screen.mouseReleased(mouse(x, to));
    }
    private static <T> T revision(T view, Class<T> type, long revision) {
        var json = JSON.toJsonTree(view).getAsJsonObject(); json.addProperty("revision", revision); return JSON.fromJson(json, type);
    }
    public static void run(ClientGameTestContext game, CountryHud hud) {
        int previous = game.computeOnClient(c -> c.options.guiScale().get());
        var source = IndustryScreenChecks.tankFixture("production-source-fixture.json");
        if (source.lines().size() < 2) throw new AssertionError("Production source needs two issued lines");
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                game.getInput().resizeWindow(size[0], size[1]); game.runOnClient(c -> c.options.guiScale().set(2));
                var sent = new ArrayList<IndustryPayloads.Request>();
                var screen = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, source.session(), source, sent::add));
                game.setScreen(() -> screen);
                game.runOnClient(c -> {
                    int top = (int)field(screen, "top") + 147, pitch = method(screen, "productionRowHeight");
                    int x = 6 + Math.round((screen.panelWidth() - 22) * 80f / 511);
                    sent.clear(); drag(screen, x, top + 5, top + 5);
                    if (!sent.isEmpty()) throw new AssertionError("Click must not reorder production");
                    screen.mouseClicked(mouse(x, top + 5), false); screen.mouseDragged(mouse(x, top + pitch + 5), 0, pitch);
                    screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)); screen.mouseReleased(mouse(x, top + pitch + 5));
                    if (!sent.isEmpty()) throw new AssertionError("Escape must cancel production drag");
                    screen.mouseClicked(mouse(x, top + 5), false); screen.mouseDragged(mouse(x, top + pitch + 5), 0, pitch);
                    screen.update(revision(source, IndustryView.class, source.revision() + 1)); screen.mouseReleased(mouse(x, top + pitch + 5));
                    if (!sent.isEmpty()) throw new AssertionError("New revision must cancel production drag");
                    drag(screen, x, top + 5, top + pitch + 5);
                    var request = sent.getFirst();
                    if (sent.size() != 1 || request.action() != IndustryPayloads.Action.MOVE || !request.item().equals(source.lines().getFirst().id()) || request.amount() != 1 || request.revision() != source.revision() + 1) throw new AssertionError("Production MOVE contract: " + sent);
                    if (!((IndustryView)field(screen, "view")).lines().equals(source.lines())) throw new AssertionError("Production must await authoritative order");
                    drag(screen, x, top + 5, top + pitch + 5); screen.mouseReleased(mouse(x, top + pitch + 5));
                    if (sent.size() != 1) throw new AssertionError("Pending production drag must not repeat");
                    screen.update(revision(source, IndustryView.class, source.revision() + 2));
                });
                game.getInput().setCursorPos(size[0] - 4, size[1] - 4); game.waitTicks(2); game.takeScreenshot("hoi-production-lines-source-" + size[0] + "-gui2");
                preview(game,screen,6+Math.round((screen.panelWidth()-22)*80f/511),game.computeOnClient(c -> (int)field(screen,"top")+147),game.computeOnClient(c -> method(screen,"productionRowHeight")),"production-expanded",size[0]);
                int rowTop = game.computeOnClient(c -> (int)field(screen,"top") + 147);
                game.getInput().setCursorPos(180,(rowTop+5)*2); game.waitTicks(3);
                game.takeScreenshot("hoi-production-drag-tooltip-expanded-" + size[0] + "-gui2");
                game.getInput().setCursorPos(size[0]-4,size[1]-4);
                game.runOnClient(c -> {
                    var button = screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("생산 라인 접기")).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                    var click = mouse(button.getX() + 2, button.getY() + 2); screen.mouseClicked(click, false); screen.mouseReleased(click);
                    if (!((Set<?>)field(screen, "collapsedProduction")).contains(source.lines().getFirst().id())) throw new AssertionError("Collapse button must remain independent from drag");
                    sent.clear(); int top = (int)field(screen, "top") + 147;
                    int pitch = Math.round((screen.panelWidth() - 22) * 63f / 511) + 2;
                    int x = 6 + Math.round((screen.panelWidth() - 22) * 80f / 511);
                    drag(screen, x, top + 5, top + pitch + 5);
                    if (sent.size() != 1 || sent.getFirst().amount() != 1 || sent.getFirst().action() != IndustryPayloads.Action.MOVE) throw new AssertionError("Collapsed row MOVE index");
                });
                game.waitTicks(2); game.takeScreenshot("hoi-production-lines-collapsed-source-" + size[0] + "-gui2");
                game.runOnClient(c -> screen.update(revision(source,IndustryView.class,source.revision()+3)));
                game.getInput().setCursorPos(180,(rowTop+5)*2); game.waitTicks(3);
                game.takeScreenshot("hoi-production-drag-tooltip-collapsed-" + size[0] + "-gui2");
                game.getInput().setCursorPos(size[0]-4,size[1]-4);
                preview(game,screen,6+Math.round((screen.panelWidth()-22)*80f/511),rowTop,Math.round((screen.panelWidth()-22)*63f/511)+2,"production-collapsed",size[0]);
                construction(game, hud);
                equipmentBounds(game, source, size[0]);
            }
        } finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(previous)); }
    }
    private static void preview(ClientGameTestContext game,net.minecraft.client.gui.screens.Screen screen,int x,int top,int pitch,String name,int width) {
        game.getInput().setCursorPos(width-4,4);game.waitTicks(2);
        var before=game.takeScreenshot("hoi-queue-"+name+"-rest-"+width+"-gui2");
        game.runOnClient(c->{screen.mouseClicked(mouse(x,top+5),false);screen.mouseDragged(mouse(x,top+pitch+5),0,pitch);});
        game.waitTicks(2);
        var during=game.takeScreenshot("hoi-queue-"+name+"-dragging-"+width+"-gui2");
        if(!"1".equals(System.getenv("HOI_CAPTURE_BEFORE"))) {
            int[] bounds=game.computeOnClient(c->{
                if(screen instanceof IndustryScreen industry){int h=name.contains("collapsed")?Math.round((industry.panelWidth()-22)*60f/511):Math.round((industry.panelWidth()-22)*108f/511);return new int[]{6,industry.panelWidth()-16,h,Math.min(top+pitch,screen.height-7-h)};}
                return new int[]{6,method(screen,"paletteX")-15,27,Math.min(top+pitch,screen.height-30-29)};
            });
            try {
                var a=javax.imageio.ImageIO.read(before.toFile());var b=javax.imageio.ImageIO.read(during.toFile());int changed=0,total=0;
                for(int y=0;y<bounds[2]*2;y++)for(int px=bounds[0]*2;px<bounds[1]*2;px++){total++;int first=a.getRGB(px,top*2+y),second=b.getRGB(px,bounds[3]*2+y);if(Math.abs((first&255)-(second&255))>5||Math.abs((first>>8&255)-(second>>8&255))>5||Math.abs((first>>16&255)-(second>>16&255))>5)changed++;}
                if(changed>total*.01)throw new AssertionError("Dragged row lost its original rendering: "+name+" changed "+changed+"/"+total);
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
        game.runOnClient(c->screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE,0,0)));game.waitTicks(2);
    }
    private static void equipmentBounds(ClientGameTestContext game, IndustryView source, int width) {
        for (String group : List.of("infantry", "navy", "air")) {
            var line = source.lines().stream().filter(l -> source.equipment().stream().anyMatch(e -> e.id().equals(l.equipment()) && e.group().equals(group))).findFirst().orElseThrow();
            var json = JSON.toJsonTree(source).getAsJsonObject();
            var lines = new com.google.gson.JsonArray(); lines.add(JSON.toJsonTree(line)); json.add("lines", lines);
            var view = JSON.fromJson(json, IndustryView.class);
            var screen = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, view.session(), view, request -> {}));
            game.setScreen(() -> screen);
            for (boolean collapsed : new boolean[]{false,true}) {
                if (collapsed) game.runOnClient(c -> {
                    var button = screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("생산 라인 접기")).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                    var event=mouse(button.getX()+2,button.getY()+2); screen.mouseClicked(event,false); screen.mouseReleased(event);
                });
                game.getInput().setCursorPos(width-4,4); game.waitTicks(2);
                var rendered=game.takeScreenshot("hoi-production-bounds-"+group+"-"+(collapsed?"collapsed":"expanded")+"-"+width+"-gui2");
                int[] bounds=game.computeOnClient(c -> {
                    var button=screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().endsWith(" · 장비 교체")).map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                    if (!collapsed) {
                        var arrows=screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && (b.getMessage().getString().equals("생산 라인 한 칸 위로") || b.getMessage().getString().equals("생산 라인 한 칸 아래로"))).map(w -> (net.minecraft.client.gui.components.Button)w).toList();
                        if(arrows.size()!=2||arrows.stream().anyMatch(a->a.getX()!=button.getRight()))throw new AssertionError("Priority controls must touch fitted equipment: "+group);
                        var equipment=source.equipment().stream().filter(e->e.id().equals(line.equipment())).findFirst().orElseThrow();
                        int[] image=dev.hoi.client.ui.UiAssets.imageSize(equipment.texture());
                        int drawnWidth=(int)Math.round(image[0]*Math.min(button.getWidth()/(double)image[0],button.getHeight()/(double)image[1]));
                        if(drawnWidth!=button.getWidth())throw new AssertionError("Equipment button must use actual fitted artwork width: "+group);
                    }
                    button.visible=false;
                    int top=(int)field(screen,"top")+147;
                    return new int[]{12,top*2,(screen.panelWidth()-16)*2,(top+Math.round((collapsed?56:104)*(screen.panelWidth()-22)/511f))*2};
                });
                game.waitTicks(2);
                var hidden=game.takeScreenshot("hoi-production-bounds-hidden-"+group+"-"+collapsed+"-"+width);
                try {
                    var a=javax.imageio.ImageIO.read(rendered.toFile()); var b=javax.imageio.ImageIO.read(hidden.toFile());
                    int changed=0;
                    for(int y=bounds[1]-4;y<bounds[3]+4;y++)for(int x=0;x<bounds[2]+4;x++) {
                        if(a.getRGB(x,y)==b.getRGB(x,y))continue;
                        if(x<bounds[0]||x>=bounds[2]||y<bounds[1]||y>=bounds[3])throw new AssertionError("Equipment/label outside production border: "+group+" "+collapsed+" @ "+x+","+y);
                        changed++;
                    }
                    if(changed<10)throw new AssertionError("Production equipment was not rendered");
                } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
            }
        }
    }
    private static void construction(ClientGameTestContext game, CountryHud hud) {
        var summary = new IndustryConstructionView.Summary(43, 7, 7, 25, 4, 0, 4, 1.31, 10, 21.86);
        var projects = new ArrayList<IndustryConstructionView.Project>();
        for (int i = 0; i < 12; i++) projects.add(new IndustryConstructionView.Project("drag-project-" + i, "state-" + i, "civilian_factory", "건설 입력 " + i, 1, 0, 0, 0, 10800));
        var view = new IndustryConstructionView("queue-drag", 1, "KOR", hud, "", summary, List.of(), projects, "");
        var sent = new ArrayList<IndustryConstructionPayloads.Request>();
        var screen = game.computeOnClient(c -> new IndustryConstruction(view.session(), view, sent::add)); game.setScreen(() -> screen);
        game.runOnClient(c -> {
            int y = method(screen, "queueY") + 14;
            sent.clear(); drag(screen, 20, y, y);
            if (!sent.isEmpty()) throw new AssertionError("Construction click must not reorder");
            screen.mouseClicked(mouse(20, y), false); screen.mouseDragged(mouse(20, y + 29), 0, 29);
            screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)); screen.mouseReleased(mouse(20, y + 29));
            if (!sent.isEmpty()) throw new AssertionError("Construction Escape must cancel");
            screen.mouseClicked(mouse(20, y), false); screen.mouseDragged(mouse(20, y + 29), 0, 29);
            screen.update(revision(view, IndustryConstructionView.class, 2)); screen.mouseReleased(mouse(20, y + 29));
            if (!sent.isEmpty()) throw new AssertionError("Construction revision must cancel");
            drag(screen, 20, y, y + 29);
            var request = sent.getFirst();
            if (sent.size() != 1 || request.action() != IndustryConstructionPayloads.Action.MOVE || !request.item().equals(projects.getFirst().id()) || request.amount() != 1 || request.revision() != 2) throw new AssertionError("Construction MOVE contract: " + sent);
            if (!((IndustryConstructionView)field(screen, "view")).projects().equals(projects)) throw new AssertionError("Construction must await authoritative order");
            drag(screen, 20, y, y + 29); screen.mouseReleased(mouse(20, y + 29));
            if (sent.size() != 1) throw new AssertionError("Pending construction drag must not repeat");
            screen.update(revision(view, IndustryConstructionView.class, 3)); sent.clear();
            screen.mouseClicked(mouse(20, y), false); screen.mouseDragged(mouse(screen.panelWidth() + 20, y + 29), 20, 29); screen.mouseReleased(mouse(screen.panelWidth() + 20, y + 29));
            if (!sent.isEmpty()) throw new AssertionError("Outside drop must cancel construction drag");
        });
        preview(game,screen,20,game.computeOnClient(c -> method(screen,"queueY")),29,"construction-synthetic",game.computeOnClient(c->c.getWindow().getWidth()));
        int y = game.computeOnClient(c -> method(screen,"queueY")+14);
        game.getInput().setCursorPos(40,y*2); game.waitTicks(3);
        game.takeScreenshot("hoi-construction-drag-tooltip-synthetic-"+game.computeOnClient(c->c.getWindow().getWidth())+"-gui2");
    }
}
