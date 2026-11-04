package dev.hoi.client.screen.industry;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.client.ui.UiScrollBar;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.research.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;

public final class IndustryProductionOfferTest {
    private static final Gson JSON = new Gson();
    private IndustryProductionOfferTest() {}
    private static <T> T source(String name, Class<T> type) throws java.io.IOException {
        return JSON.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"), name + ".json")), type);
    }
    private static Object field(Object screen, String name, Object... value) {
        try { var f = screen.getClass().getDeclaredField(name); f.setAccessible(true); if (value.length > 0) f.set(screen, value[0]); return f.get(screen); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static AbstractWidget named(Screen screen, String name) {
        return screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals(name)).map(w -> (AbstractWidget)w).findFirst().orElseThrow(() -> new AssertionError("Missing " + name));
    }
    private static void click(Screen screen, AbstractWidget widget, int button) {
        var event = new MouseButtonEvent(widget.getX() + widget.getWidth() / 2., widget.getY() + widget.getHeight() / 2., new MouseButtonInfo(button, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    public static void run(ClientGameTestContext game) {
        int previous = game.computeOnClient(c -> c.options.guiScale().get());
        try {
            var offer = source("kor-production-mio-offer", IndustryView.class);
            var equipment = offer.equipment().stream().filter(e -> !e.manufacturers().isEmpty()).findFirst().orElseThrow();
            var original = JSON.toJsonTree(offer).getAsJsonObject();
            original.addProperty("revision", offer.revision() - 1);
            for (var item : original.getAsJsonArray("equipment")) { item.getAsJsonObject().add("manufacturers", new com.google.gson.JsonArray()); item.getAsJsonObject().addProperty("manufacturerEmpty", ""); }
            var before = JSON.fromJson(original, IndustryView.class);
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720}, new int[]{854,480})) {
                game.getInput().resizeWindow(size[0], size[1]); game.runOnClient(c -> c.options.guiScale().set(2));
                for (String action : List.of("assign", "empty", "escape", "outside")) {
                    var requests = new ArrayList<IndustryPayloads.Request>();
                    var screen = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, before.session(), before, requests::add)); game.setScreen(() -> screen);
                    game.runOnClient(c -> {
                        field(screen, "picker", "equipment"); field(screen, "group", equipment.group()); screen.init(screen.width, screen.height);
                        for (int i = 0; i < 100 && screen.children().stream().noneMatch(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals("생산: " + equipment.name())); i++) { field(screen, "pickerScroll", i * 10); screen.init(screen.width, screen.height); }
                        requests.clear(); var row = named(screen, "생산: " + equipment.name());
                        click(screen, row, InputConstants.MOUSE_BUTTON_RIGHT);
                        if (!requests.isEmpty()) throw new AssertionError("Right click must not offer equipment");
                        click(screen, row, InputConstants.MOUSE_BUTTON_LEFT); click(screen, row, InputConstants.MOUSE_BUTTON_LEFT);
                        if (requests.size() != 1 || requests.getFirst().action() != IndustryPayloads.Action.OFFER || !requests.getFirst().item().equals(equipment.id()) || requests.getFirst().revision() != before.revision()) throw new AssertionError("One authenticated OFFER required: " + requests);
                        screen.update(before);
                        if (screen.children().stream().anyMatch(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals("군수산업체 없이 생산"))) throw new AssertionError("Stale response must not expose offer choices");
                        screen.update(offer);
                    });
                    game.getInput().setCursorPos(size[0] - 2, size[1] - 2); game.waitTicks(3);
                    if (action.equals("assign")) game.takeScreenshot("hoi-production-offer-" + size[0] + "-gui2");
                    game.runOnClient(c -> {
                        int beforeSort=requests.size();
                        click(screen,named(screen,"규모 정렬"),InputConstants.MOUSE_BUTTON_LEFT);
                        click(screen,named(screen,"규모 정렬"),InputConstants.MOUSE_BUTTON_LEFT);
                        click(screen,named(screen,"이름 정렬"),InputConstants.MOUSE_BUTTON_LEFT);
                        var expected=equipment.manufacturers().stream().map(IndustryView.LineManufacturer::name).sorted().toList();
                        var actual=screen.children().stream().filter(w->w instanceof AbstractWidget b&&expected.contains(b.getMessage().getString())).map(w->((AbstractWidget)w).getMessage().getString()).toList();
                        if(!actual.equals(expected.subList(0,actual.size()))||requests.size()!=beforeSort)throw new AssertionError("Manufacturer sorting must order visible rows without sending production requests");
                        var blank = new MouseButtonEvent((int)field(screen, "manufacturerX") + 5, (int)field(screen, "manufacturerY") + 24, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                        int requestCount = requests.size(); screen.mouseClicked(blank, false); screen.mouseReleased(blank);
                        if (requests.size() != requestCount || ((String)field(screen, "manufacturerEquipment")).isEmpty()) throw new AssertionError("Blank popup space must not dismiss or send a request");
                        screen.mouseScrolled(0, 0, 0, -100);
                        var last = named(screen, equipment.manufacturers().getLast().name());
                        if (last.getBottom() > screen.height) throw new AssertionError("Last manufacturer must be reachable");
                        screen.mouseScrolled(0, 0, 0, 100);
                        var empty = named(screen, "군수산업체 없이 생산");
                        var choice = named(screen, equipment.manufacturers().getFirst().name());
                        for (var widget : List.of(empty, choice)) if (widget.getX() < 0 || widget.getRight() > screen.width || widget.getY() < 0 || widget.getBottom() > screen.height) throw new AssertionError("Offer choice outside viewport");
                        requests.clear();
                        switch (action) {
                            case "assign" -> { click(screen, choice, InputConstants.MOUSE_BUTTON_LEFT); click(screen, choice, InputConstants.MOUSE_BUTTON_LEFT); }
                            case "empty" -> click(screen, empty, InputConstants.MOUSE_BUTTON_LEFT);
                            case "escape" -> screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                            default -> { var event = new MouseButtonEvent(1, 1, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); screen.mouseClicked(event, false); screen.mouseReleased(event); }
                        }
                        boolean add = action.equals("assign") || action.equals("empty");
                        if (requests.size() != 1 || requests.getFirst().action() != (add ? IndustryPayloads.Action.ADD : IndustryPayloads.Action.OFFER) || !requests.getFirst().item().equals(add ? equipment.id() : "") || !requests.getFirst().other().equals(action.equals("assign") ? equipment.manufacturers().getFirst().id() : "")) throw new AssertionError("Unexpected offer result " + action + ": " + requests);
                    });
                }
                directProduction(game, before, equipment, size[0]);
                research(game, size[0]);
            }
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(previous)); }
    }
    private static void directProduction(ClientGameTestContext game, IndustryView view, IndustryView.Equipment offered, int width) {
        for (boolean switching : List.of(false, true)) {
            var equipment = game.computeOnClient(c -> switching ? offered : view.equipment().stream().filter(e -> e.unlocked() && !e.outdated() && e.manufacturerChoices() == 0).filter(e -> { var design = dev.hoi.client.ui.UiEquipmentDesigns.productionDesign(e); return !design.blueprint() && !design.unfinished(); }).findFirst().orElseThrow());
            var line = view.lines().stream().filter(l -> !l.equipment().equals(equipment.id())).findFirst().orElseThrow();
            var requests = new ArrayList<IndustryPayloads.Request>();
            var screen = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, view.session(), view, requests::add)); game.setScreen(() -> screen);
            game.runOnClient(c -> {
                field(screen, "picker", "equipment"); field(screen, "group", equipment.group());
                if (switching) field(screen, "switchLine", line.id());
                screen.init(screen.width, screen.height);
                for (int i = 0; i < 200 && screen.children().stream().noneMatch(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals("생산: " + equipment.name())); i++) { field(screen, "pickerScroll", i * 10); screen.init(screen.width, screen.height); }
                requests.clear(); click(screen, named(screen, "생산: " + equipment.name()), InputConstants.MOUSE_BUTTON_LEFT);
                if (requests.size() != 1 || requests.getFirst().action() != (switching ? IndustryPayloads.Action.SWITCH : IndustryPayloads.Action.ADD) || !requests.getFirst().item().equals(switching ? line.id() : equipment.id()) || !requests.getFirst().other().equals(switching ? equipment.id() : "")) throw new AssertionError("Direct production / existing SWITCH changed: " + requests);
            });
        }
        var unavailable = view.lines().stream().filter(l -> l.manufacturer() == null && l.manufacturers().isEmpty()).findFirst().orElseThrow();
        var screen = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, view.session(), view, request -> {})); game.setScreen(() -> screen);
        game.runOnClient(c -> {
            for (int i = 0; i < 300 && screen.children().stream().noneMatch(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals("군수산업체 · " + unavailable.id())); i++) { field(screen, "scroll", i * 10); screen.init(screen.width, screen.height); }
        });
        var icon = game.computeOnClient(c -> named(screen, "군수산업체 · " + unavailable.id()));
        game.runOnClient(c -> { if (icon.active || unavailable.manufacturerTooltip().isBlank()) throw new AssertionError("Unavailable manufacturer requires disabled icon and source tooltip"); });
        game.getInput().setCursorPos((icon.getX() + 4) * 2, (icon.getY() + 4) * 2); game.waitTicks(4);
        game.takeScreenshot("hoi-production-no-manufacturer-" + width + "-gui2");
    }
    private static void research(ClientGameTestContext game, int width) throws java.io.IOException {
        var view = source("kor-research-design-team-before", ResearchView.class);
        var tech = view.technologies().stream().filter(t -> t.source() != null).max(Comparator.comparingInt(t -> t.source().description().length())).orElseThrow();
        researchButtonStates(game, view, tech, width);
        var sent = new ArrayList<ResearchPayloads.Request>();
        var screen = game.computeOnClient(c -> new ResearchScreen(view, sent::add)); game.setScreen(() -> screen); game.runOnClient(c -> screen.showDetail(tech.id())); game.waitTicks(3);
        game.takeScreenshot("hoi-research-description-top-" + width + "-gui2");
        game.runOnClient(c -> {
            var bar = (UiScrollBar)field(screen, "descriptionBar");
            if (bar == null || bar.maximum() <= 0 || bar.thumbHeight() != 8) throw new AssertionError("Long original description needs the 16px native scrollbar");
            screen.mouseScrolled(bar.x() - 20, bar.top() + 10, 0, -100);
            if ((int)field(screen, "descriptionScroll") != bar.maximum()) throw new AssertionError("Wheel must reach description end");
            var up = new MouseButtonEvent(bar.x() + 4, bar.top() + 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); screen.mouseClicked(up, false); screen.mouseReleased(up);
            if ((int)field(screen, "descriptionScroll") >= bar.maximum()) throw new AssertionError("Description arrow must move");
            var drag = new MouseButtonEvent(bar.x() + 4, bar.top() + 15, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); screen.mouseClicked(drag, false);
            var end = new MouseButtonEvent(bar.x() + 4, bar.bottom() - 10, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); screen.mouseDragged(end, 0, 50); screen.mouseReleased(end);
            if ((int)field(screen, "descriptionScroll") != bar.maximum()) throw new AssertionError("Drag must reach description end");
            screen.mouseDragged(drag, 0, -50);
            if ((int)field(screen, "descriptionScroll") != bar.maximum() || !sent.isEmpty()) throw new AssertionError("Released drag must not move or request research");
        });
        game.waitTicks(2); game.takeScreenshot("hoi-research-description-bottom-" + width + "-gui2");
    }
    private static void researchButtonStates(ClientGameTestContext game, ResearchView view, ResearchView.Tech tech, int width) throws java.io.IOException {
        for (String state : List.of("available", "locked", "completed", "active", "full", "pending")) {
            var json = JSON.toJsonTree(view).getAsJsonObject();
            String status = switch (state) { case "locked" -> "LOCKED"; case "completed" -> "COMPLETED"; case "active" -> "ACTIVE"; default -> "AVAILABLE"; };
            for (var item : json.getAsJsonArray("technologies")) if (item.getAsJsonObject().get("id").getAsString().equals(tech.id())) item.getAsJsonObject().addProperty("status", status);
            String occupied = state.equals("active") ? tech.id() : state.equals("full") ? view.technologies().stream().filter(t -> !t.id().equals(tech.id())).findFirst().orElseThrow().id() : "";
            json.add("slots", JSON.toJsonTree(List.of(new ResearchView.Slot(0, occupied, 0))));
            var sample = JSON.fromJson(json, ResearchView.class);
            var requests = new ArrayList<ResearchPayloads.Request>();
            var screen = game.computeOnClient(c -> new ResearchScreen(sample, requests::add)); game.setScreen(() -> screen);
            var button = game.computeOnClient(c -> {
                screen.showDetail(tech.id());
                if (state.equals("pending")) { field(screen, "pending", 100); screen.init(screen.width, screen.height); }
                int x = (int)field(screen, "panelX") + 179, y = (int)field(screen, "panelY") + 29;
                var control = screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.getX() == x && b.getY() == y).map(w -> (AbstractWidget)w).findFirst().orElseThrow();
                String full = switch (state) { case "locked" -> "선행 연구 필요"; case "completed" -> "연구 완료"; case "active" -> "연구 중단"; case "full" -> "빈 슬롯 필요"; case "pending" -> "응답 대기…"; default -> "연구"; };
                if (!control.getMessage().getString().equals("연구") || c.font.width(control.getMessage()) > control.getWidth() - 30) throw new AssertionError("Research caption outside visible source background: " + state);
                if (control.getX() < 0 || control.getRight() > screen.width || control.getY() < 0 || control.getBottom() > screen.height) throw new AssertionError("Research button outside viewport");
                if (control.active != (state.equals("available") || state.equals("active"))) throw new AssertionError("Research button authorization changed: " + state);
                {
                    try {
                        var tooltipField = AbstractWidget.class.getDeclaredField("tooltip"); tooltipField.setAccessible(true);
                        var holder = (net.minecraft.client.gui.components.WidgetTooltipHolder)tooltipField.get(control);
                        var message = net.minecraft.client.gui.components.Tooltip.class.getDeclaredField("message"); message.setAccessible(true);
                        String text = ((net.minecraft.network.chat.Component)message.get(holder.get())).getString();
                        if (!text.lines().findFirst().orElse("").equals(full)) throw new AssertionError("Research status must be first tooltip line: " + text);
                    } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                }
                if (!control.active) { click(screen, control, InputConstants.MOUSE_BUTTON_LEFT); click(screen, control, InputConstants.MOUSE_BUTTON_RIGHT); }
                if (!requests.isEmpty()) throw new AssertionError("Disabled research button sent a request");
                return control;
            });
            game.getInput().setCursorPos(width - 3, 3); game.waitTicks(2);
            var image = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-research-button-synthetic-" + state + "-" + width + "-gui2").toFile());
            int white = 0;
            for (int py = (button.getY() + 3) * 2; py < (button.getBottom() - 3) * 2; py++) for (int px = (button.getX() + 28) * 2; px < (button.getRight() - 28) * 2; px++) {
                int rgb = image.getRGB(px, py);
                if ((rgb >> 16 & 255) > 230 && (rgb >> 8 & 255) > 230 && (rgb & 255) > 230) white++;
            }
            if (white < 12) throw new AssertionError("Research text must render white in every state: " + state + ", pixels=" + white);
            game.getInput().setCursorPos((button.getX() + 5) * 2, (button.getY() + 5) * 2); game.waitTicks(4);
            game.takeScreenshot("hoi-research-button-tooltip-synthetic-" + state + "-" + width + "-gui2");
        }
    }
}
