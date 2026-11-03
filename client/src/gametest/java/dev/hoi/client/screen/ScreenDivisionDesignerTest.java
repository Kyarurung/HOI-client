package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.*;

public final class ScreenDivisionDesignerTest {
    private ScreenDivisionDesignerTest() {}
    private static void field(Object screen, String name, Object value) {
        try { var f = screen.getClass().getDeclaredField(name); f.setAccessible(true); f.set(screen, value); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    public static void run(ClientGameTestContext game) {
        int previous = game.computeOnClient(c -> c.options.guiScale().get());
        var fullSlots = new HashMap<String, Set<String>>();
        try {
            game.runOnClient(c -> {
                for (var sample : List.of(new ProtocolIndustryView.Stat("속도", "BASE", 10, "KM_PER_HOUR", "SPEED"),
                        new ProtocolIndustryView.Stat("내구도", "BASE", 151, "NUMBER", "HP"),
                        new ProtocolIndustryView.Stat("회복률", "BASE", 0.81, "NUMBER", "RECOVERY_RATE"),
                        new ProtocolIndustryView.Stat("평균 신뢰도", "BASE", 0.9, "PERCENT", "AVERAGE_RELIABILITY"),
                        new ProtocolIndustryView.Stat("전선 복귀", "BASE", 0, "PERCENT", "TRICKLEBACK"))) {
                    String expected = switch (sample.id()) { case "SPEED" -> "10.0km/h"; case "HP" -> "151.0"; case "RECOVERY_RATE" -> "0.81"; case "AVERAGE_RELIABILITY" -> "90.0%"; default -> "0.00%"; };
                    if (!dev.hoi.client.ui.UiEquipmentDesigns.divisionStatValue(sample).equals(expected)) throw new AssertionError("Original division number format: " + sample.id());
                }
            });
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0], size[1]); game.runOnClient(c -> c.options.guiScale().set(2));
                for (String fixture : List.of("kor-division-designer", "kor-division-designer-new", "prc-division-designer", "kor-division-designer-motorized")) {
                    var view = new Gson().fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"), fixture + ".json")), ProtocolIndustryView.class);
                    if (fixture.equals("prc-division-designer")) priorityIcons(game, view, size[0]);
                    if (fixture.equals("kor-division-designer-motorized")) paradropIcons(game, view, size[0]);
                    headerControls(game, view, fixture, size[0]);
                    var sent = new ArrayList<ProtocolIndustryPayloads.Request>();
                    var screen = game.computeOnClient(c -> new ScreenIndustry(ProtocolMenuTab.RECRUITMENT, view.session(), view, sent::add));
                    game.setScreen(() -> screen);
                    game.runOnClient(c -> { field(screen, "designer", true); field(screen, "draftName", view.draft().name()); screen.init(screen.width, screen.height); });
                    game.getInput().setCursorPos(size[0]-3, size[1]-3); game.waitTicks(3);
                    game.runOnClient(c -> {
                        var adjustments = screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.getMessage().getString().startsWith("사단 조정치 · ")).map(w -> (AbstractWidget)w).toList();
                        if (adjustments.size() != view.draft().adjustments().size()) throw new AssertionError("All authoritative terrain adjustments must be visible");
                        for (var b : adjustments) if (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height) throw new AssertionError("Terrain adjustment outside screen");
                        if (adjustments.stream().map(AbstractWidget::getY).distinct().count() != (adjustments.size() + 4) / 5) throw new AssertionError("Terrain adjustments must preserve the original cell pitch across the merged panel");
                        for (int rowY : adjustments.stream().map(AbstractWidget::getY).distinct().toList()) {
                            var row = adjustments.stream().filter(b -> b.getY() == rowY).toList();
                            int left = row.stream().mapToInt(AbstractWidget::getX).min().orElseThrow();
                            int right = row.stream().mapToInt(AbstractWidget::getRight).max().orElseThrow();
                            int center = (screen.width - 600) / 2 + 334 + 130;
                            if (Math.abs(left + right - 2 * center) > 1) throw new AssertionError("Every terrain row must share the panel center");
                        }
                        if (size[0] == 2560) fullSlots.put(fixture, slots(screen, (screen.width - 600) / 2, (screen.height - 360) / 2, 0));
                        var buttons = screen.children().stream().filter(w -> w instanceof Button).map(w -> (Button)w).toList();
                        for (int i = 0; i < buttons.size(); i++) for (int j = i + 1; j < buttons.size(); j++) {
                            var a = buttons.get(i); var b = buttons.get(j);
                            if (a.getRight() > b.getX() && b.getRight() > a.getX() && a.getBottom() > b.getY() && b.getBottom() > a.getY()) throw new AssertionError("Designer controls overlap: " + a.getMessage().getString() + " / " + b.getMessage().getString());
                        }
                    });
                    game.takeScreenshot("hoi-" + fixture + "-" + size[0] + "-gui2");
                    emptySlotHover(game, screen, fixture, size[0]);
                    occupiedSlotHover(game, screen, fixture, size[0]);
                    game.getInput().setCursorPos(((screen.width - 600) / 2 + 39) * 2, ((screen.height - 360) / 2 + 65) * 2); game.waitTicks(4);
                    game.takeScreenshot("hoi-" + fixture + "-niche-tooltip-" + size[0] + "-gui2");
                    if (fixture.equals("prc-division-designer")) {
                        String[] names = {"hardness", "width", "support", "regiment", "paradrop", "special", "reset", "duplicate", "experience", "resources", "cost"};
                        int[][] positions = {{60,102},{155,126},{290,153},{145,290},{219,36},{232,36},{35,318},{100,318},{165,318},{290,320},{450,320}};
                        int panelX = game.computeOnClient(c -> (screen.width - 600) / 2), panelY = game.computeOnClient(c -> (screen.height - 360) / 2);
                        for (int index = 0; index < names.length; index++) {
                            game.getInput().setCursorPos((panelX + positions[index][0]) * 2, (panelY + positions[index][1]) * 2); game.waitTicks(4);
                            game.takeScreenshot("hoi-" + fixture + "-" + names[index] + "-tooltip-" + size[0] + "-gui2");
                        }
                        var locked = game.computeOnClient(c -> button(screen, view.draft().designer().rowLocked()));
                        game.getInput().setCursorPos((locked.getX() + 10) * 2, (locked.getY() + 10) * 2); game.waitTicks(4);
                        game.takeScreenshot("hoi-" + fixture + "-row-locked-tooltip-" + size[0] + "-gui2");
                    }
                    if (view.draft().adjustments().stream().anyMatch(value -> value.id().equals("mountain"))) {
                        var terrain = game.computeOnClient(c -> screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals("사단 조정치 · mountain")).map(w -> (AbstractWidget)w).findFirst().orElseThrow());
                        game.getInput().setCursorPos((terrain.getX()+10)*2, (terrain.getY()+10)*2); game.waitTicks(4);
                        game.takeScreenshot("hoi-" + fixture + "-mountain-tooltip-" + size[0] + "-gui2");
                    }
                    game.runOnClient(c -> {
                        int x = (screen.width - 600) / 2, y = (screen.height - 360) / 2;
                        var support = screen.children().stream().filter(w -> w instanceof Button b && b.getWidth() == 32 && b.getY() == y + 160).map(w -> (Button)w).findFirst().orElseThrow();
                        if (support.getRight() > x + 326) throw new AssertionError("Support slot must stay inside the left panel");
                        var click = new MouseButtonEvent(support.getX(), support.getY() + 1, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
                        screen.mouseClicked(click, false); screen.mouseReleased(click);
                        try {
                            var supportField = screen.getClass().getDeclaredField("supportSlot"); supportField.setAccessible(true);
                            if (!supportField.getBoolean(screen)) throw new AssertionError("Support left edge must open support choices, not battalion choices");
                        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                        var back = screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("대대 목록 닫기")).map(w -> (Button)w).findFirst().orElseThrow();
                        back.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                        if (sent.stream().anyMatch(r -> r.action() != ProtocolIndustryPayloads.Action.REFRESH)) throw new AssertionError("Opening support choices must not change server state");
                    });
                    game.runOnClient(c -> {
                        var slot = screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.active && (b.getMessage().getString().equals("지원중대 추가") || b.getMessage().getString().equals("대대 추가"))).map(w -> (AbstractWidget)w).findFirst().orElseThrow();
                        var click = new MouseButtonEvent(slot.getX()+2, slot.getY()+2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false); screen.mouseReleased(click);
                        if (screen.children().stream().noneMatch(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals("대대 목록 닫기"))) throw new AssertionError("Slot must open the unit picker");
                        if (sent.stream().anyMatch(r -> r.action()!=ProtocolIndustryPayloads.Action.REFRESH)) throw new AssertionError("Opening a slot must not change server state");
                    });
                    game.getInput().setCursorPos(size[0]-3,size[1]-3); game.waitTicks(2);
                    game.runOnClient(c -> {
                        int footer = (screen.height - 360) / 2 + 307;
                        for (var child : screen.children()) if (child instanceof AbstractWidget b && b.getWidth() == 125 && b.getHeight() == 33 && b.getBottom() > footer) throw new AssertionError("Battalion choices must end above resources and experience");
                    });
                    game.takeScreenshot("hoi-" + fixture + "-unit-picker-" + size[0] + "-gui2");
                }
            }
            game.getInput().resizeWindow(854, 480); game.runOnClient(c -> c.options.guiScale().set(2));
            for (String fixture : List.of("kor-division-designer", "kor-division-designer-new", "prc-division-designer", "kor-division-designer-motorized")) {
                var view = new Gson().fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"), fixture + ".json")), ProtocolIndustryView.class);
                headerControls(game, view, fixture, 854);
                compactContent(game, view, fixture, fullSlots.get(fixture));
            }
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(previous)); }
    }
    private static Set<String> slots(ScreenIndustry screen, int x, int y, int scroll) {
        var result = new HashSet<String>();
        for (var child : screen.children()) if (child instanceof AbstractWidget b && (b.getWidth() == 44 && b.getHeight() == 21 || b.getWidth() == 32 && b.getHeight() == 16)) result.add(b.getMessage().getString() + ":" + (b.getX() - x) + ":" + (b.getY() - y + scroll));
        return result;
    }
    private static void occupiedSlotHover(ClientGameTestContext game, ScreenIndustry screen, String fixture, int width) throws java.io.IOException {
        var slot = game.computeOnClient(c -> screen.children().stream().filter(w -> w instanceof AbstractWidget b
                && b.active && b.getWidth() == 44 && b.getHeight() == 21 && !b.getMessage().getString().equals("대대 추가"))
                .map(AbstractWidget.class::cast).findFirst().orElse(null));
        if (slot == null) return;
        game.getInput().setCursorPos(width - 3, 3); game.waitTicks(2);
        var idle = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-" + fixture + "-occupied-idle-" + width + "-gui2").toFile());
        game.getInput().setCursorPos((slot.getX() + slot.getWidth() / 2) * 2, (slot.getBottom() - 1) * 2); game.waitTicks(2);
        var hovered = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-" + fixture + "-occupied-replace-" + width + "-gui2").toFile());
        game.getInput().setCursorPos(width - 3, 3); game.waitTicks(2);
        var restored = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-" + fixture + "-occupied-restored-" + width + "-gui2").toFile());
        int changed = 0;
        for (int y = (slot.getY() + 3) * 2; y < (slot.getBottom() - 3) * 2; y++) for (int x = (slot.getX() + 9) * 2; x < (slot.getRight() - 3) * 2; x++) {
            if (idle.getRGB(x, y) != hovered.getRGB(x, y)) changed++;
            if (idle.getRGB(x, y) != restored.getRGB(x, y)) throw new AssertionError("Battalion artwork must return after leaving the slot");
        }
        if (changed == 0) throw new AssertionError("Occupied battalion must show replacement artwork on hover");
    }
    private static void emptySlotHover(ClientGameTestContext game, ScreenIndustry screen, String fixture, int width) throws java.io.IOException {
        var slot = game.computeOnClient(c -> screen.children().stream().filter(w -> w instanceof AbstractWidget b
                && b.getMessage().getString().equals("대대 추가")).map(AbstractWidget.class::cast).findFirst().orElse(null));
        if (slot != null) {
            var before = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-" + fixture + "-empty-idle-" + width + "-gui2").toFile());
            game.getInput().setCursorPos((slot.getX() + slot.getWidth() / 2) * 2, (slot.getBottom() - 1) * 2);
            game.waitTicks(2);
            var after = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-" + fixture + "-empty-hover-" + width + "-gui2").toFile());
            for (int y = slot.getY() * 2; y < slot.getBottom() * 2; y++) for (int x = slot.getX() * 2; x < slot.getRight() * 2; x++)
                if (before.getRGB(x, y) != after.getRGB(x, y)) throw new AssertionError("Empty battalion artwork changed on hover");
        }
        var disabled = game.computeOnClient(c -> screen.children().stream().filter(w -> w instanceof AbstractWidget b
                && !b.active && b instanceof Button).map(AbstractWidget.class::cast).findFirst().orElse(null));
        if (disabled != null) {
            game.getInput().setCursorPos((disabled.getX() + disabled.getWidth() / 2) * 2, (disabled.getY() + disabled.getHeight() / 2) * 2);
            game.waitTicks(2);
            game.runOnClient(c -> {
                try {
                    var field = c.getWindow().getClass().getDeclaredField("currentCursor"); field.setAccessible(true);
                    if (field.get(c.getWindow()) == com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED)
                        throw new AssertionError("HOI disabled controls must retain the arrow cursor");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
        }
    }
    private static void priorityIcons(ClientGameTestContext game, ProtocolIndustryView view, int width) throws java.io.IOException {
        for (int priority : List.of(0, 2)) {
            var gson = new Gson(); var json = gson.toJsonTree(view).getAsJsonObject();
            json.getAsJsonObject("draft").addProperty("priority", priority);
            json.getAsJsonObject("draft").getAsJsonObject("designer").addProperty("armor", priority == 2);
            for (var template : json.getAsJsonArray("templates")) template.getAsJsonObject().addProperty("priority", priority);
            var sample = gson.fromJson(json, ProtocolIndustryView.class);
            var screen = game.computeOnClient(c -> new ScreenIndustry(ProtocolMenuTab.RECRUITMENT, sample.session(), sample, request -> { if (request.action() != ProtocolIndustryPayloads.Action.CLOSE) throw new AssertionError("Header rendering must not send mutations"); }));
            game.setScreen(() -> screen);
            game.getInput().setCursorPos(width - 3, 3); game.waitTicks(3);
            game.takeScreenshot("hoi-division-list-priority-synthetic-" + priority + "-" + width + "-gui2");
            game.runOnClient(c -> { field(screen, "designer", true); field(screen, "draftName", sample.draft().name()); screen.init(screen.width, screen.height); });
            game.waitTicks(3);
            var image = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-division-header-priority-synthetic-" + priority + "-armor-" + (priority == 2) + "-" + width + "-gui2").toFile());
            int x = (screen.width - 600) + 105, y = (screen.height - 360) + 56, colored = 0;
            for (int py = y; py < y + 35; py++) for (int px = x; px < x + 16; px++) {
                int rgb = image.getRGB(px, py), red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
                if (red > 90 && red > green * 1.2 && red > blue * 1.5) colored++;
            }
            if (colored < 15) throw new AssertionError("Reserve and elite chevrons must render at the original header position: " + priority);
        }
    }
    private static void compactContent(ClientGameTestContext game, ProtocolIndustryView view, String fixture, Set<String> expectedSlots) {
        var requests = new ArrayList<ProtocolIndustryPayloads.Request>();
        var screen = game.computeOnClient(c -> new ScreenIndustry(ProtocolMenuTab.RECRUITMENT, view.session(), view, requests::add));
        game.setScreen(() -> screen);
        game.runOnClient(c -> { field(screen, "designer", true); field(screen, "draftName", view.draft().name()); screen.init(screen.width, screen.height); });
        var terrains = new HashSet<String>();
        var reachedSlots = new HashSet<String>();
        for (int step = 0; step < 22; step++) {
            game.runOnClient(c -> {
                try {
                    var scroll = screen.getClass().getDeclaredField("designerScroll"); scroll.setAccessible(true);
                    reachedSlots.addAll(slots(screen, 0, 0, scroll.getInt(screen)));
                } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                for (var child : screen.children()) if (child instanceof AbstractWidget widget) {
                    if (widget.getX() < 0 || widget.getY() < 0 || widget.getRight() > screen.width || widget.getBottom() > screen.height) throw new AssertionError("Compact designer widget outside viewport: " + widget.getMessage().getString());
                    if (widget.getMessage().getString().startsWith("사단 조정치 · ")) terrains.add(widget.getMessage().getString().substring("사단 조정치 · ".length()));
                }
            });
            if (step == 0 || step == 4 || step == 9 || step == 12 || step == 21) {
                game.getInput().setCursorPos(850, 476); game.waitTicks(2);
                game.takeScreenshot("hoi-" + fixture + "-compact-content-" + step);
            }
            game.runOnClient(c -> screen.mouseScrolled(150, 160, 0, -1));
        }
        if (!terrains.equals(view.draft().adjustments().stream().map(ProtocolTankDesigner.Adjustment::id).collect(java.util.stream.Collectors.toSet()))) throw new AssertionError("Every compact terrain adjustment must be reachable by wheel");
        if (!reachedSlots.containsAll(expectedSlots)) throw new AssertionError("Every full-size battalion and support slot must remain reachable in the compact designer: " + expectedSlots.stream().filter(slot -> !reachedSlots.contains(slot)).toList());
        if (!requests.isEmpty()) throw new AssertionError("Compact scrolling must not mutate server state");
    }
    private static void paradropIcons(ClientGameTestContext game, ProtocolIndustryView view, int width) throws java.io.IOException {
        for (boolean allowed : List.of(false, true)) {
            var gson = new Gson(); var json = gson.toJsonTree(view).getAsJsonObject();
            json.getAsJsonObject("draft").getAsJsonObject("designer").addProperty("paradrop", allowed);
            var sample = gson.fromJson(json, ProtocolIndustryView.class);
            var screen = game.computeOnClient(c -> new ScreenIndustry(ProtocolMenuTab.RECRUITMENT, sample.session(), sample, request -> {}));
            game.setScreen(() -> screen);
            game.runOnClient(c -> { field(screen, "designer", true); field(screen, "draftName", sample.draft().name()); screen.init(screen.width, screen.height); });
            game.getInput().setCursorPos(width - 3, 3); game.waitTicks(3);
            var image = javax.imageio.ImageIO.read(game.takeScreenshot("hoi-division-paradrop-synthetic-" + allowed + "-" + width + "-gui2").toFile());
            int x = ((screen.width - Math.min(600, screen.width)) / 2 + 213) * 2;
            int y = ((screen.height - Math.min(360, screen.height)) / 2 + 30) * 2;
            int red = 0, green = 0;
            for (int py = y + 10; py < y + 24; py++) for (int px = x + 13; px < x + 26; px++) {
                int rgb = image.getRGB(px, py), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
                if (r > 90 && r > g * 1.4 && r > b * 1.4) red++;
                if (g > 70 && g > r * 1.15 && g > b * 1.3) green++;
            }
            if (allowed ? green < 3 || green <= red : red < 3 || red <= green) throw new AssertionError("Paradrop must render " + (allowed ? "green check (frame 1)" : "red cross (frame 0)") + ": red=" + red + ", green=" + green);
        }
    }
    private static void headerControls(ClientGameTestContext game, ProtocolIndustryView view, String fixture, int width) {
        var draft = view.draft();
        if (draft.designer() == null) throw new AssertionError("Division designer requires the authoritative header contract");
        var requests = new ArrayList<ProtocolIndustryPayloads.Request>();
        var screen = game.computeOnClient(c -> new ScreenIndustry(ProtocolMenuTab.RECRUITMENT, view.session(), view, requests::add));
        game.getInput().setCursorPos(width - 3, 3);
        game.setScreen(() -> screen);
        game.runOnClient(c -> { field(screen, "designer", true); field(screen, "draftName", draft.name()); screen.init(screen.width, screen.height); });
        game.runOnClient(c -> click(screen, "편제 목록")); game.waitTicks(2);
        game.takeScreenshot("hoi-" + fixture + "-template-dropdown-" + width + "-gui2");
        game.runOnClient(c -> scrollDropdown(screen));
        game.runOnClient(c -> {
            var option = button(screen, dev.hoi.client.ui.UiEquipmentDesigns.tooltip("DESIGNER_CREATE_NEW_DIVISION"));
            var right = new MouseButtonEvent(option.getX() + 2, option.getY() + 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0));
            screen.mouseClicked(right, false); screen.mouseReleased(right);
            if (!requests.isEmpty()) throw new AssertionError("Right-clicking a dropdown must not select an entry");
            screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            if (screen.children().stream().anyMatch(w -> w instanceof Button b && b.getMessage().equals(option.getMessage()))) throw new AssertionError("Escape closes the dropdown without closing the designer");
            click(screen, "사단 이름 그룹 목록");
        });
        game.waitTicks(2); game.takeScreenshot("hoi-" + fixture + "-name-dropdown-" + width + "-gui2");
        game.runOnClient(c -> scrollDropdown(screen));
        if (!draft.designer().nameGroups().isEmpty()) {
            var group = draft.designer().nameGroups().getFirst();
            var option = game.computeOnClient(c -> button(screen, group.name()));
            game.getInput().setCursorPos((option.getX() + 3) * 2, (option.getY() + 3) * 2); game.waitTicks(4);
            game.takeScreenshot("hoi-" + fixture + "-name-examples-" + width + "-gui2");
            game.runOnClient(c -> {
                click(screen, group.name());
                checkRequest(requests, ProtocolIndustryPayloads.Action.NAME_GROUP, group.id(), 0);
                screen.update(response(view, draft.withSettings(draft.priority(), group.id()), view.revision() + 1));
            });
        } else game.runOnClient(c -> screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
        game.runOnClient(c -> {
            click(screen, "편제 목록");
            var outside = new MouseButtonEvent(screen.width - 2, screen.height - 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            int count = requests.size(); screen.mouseClicked(outside, false); screen.mouseReleased(outside);
            if (requests.size() != count) throw new AssertionError("Dismissing a dropdown must not activate an underlying control");
        });
        long revision = view.revision() + 2;
        for (int priority = 0; priority < 3; priority++) {
            int value = priority; long currentRevision = revision++;
            String name = dev.hoi.client.ui.UiEquipmentDesigns.tooltip("TEMPLATE_PRIO_" + priority);
            var control = game.computeOnClient(c -> button(screen, name));
            game.getInput().setCursorPos((control.getX() + 3) * 2, (control.getY() + 3) * 2); game.waitTicks(4);
            game.takeScreenshot("hoi-" + fixture + "-priority-" + priority + "-tooltip-" + width + "-gui2");
            game.runOnClient(c -> {
                click(screen, name); checkRequest(requests, ProtocolIndustryPayloads.Action.TEMPLATE_PRIORITY, "", value);
                int count = requests.size(); click(screen, name);
                if (requests.size() != count) throw new AssertionError("Repeated priority clicks must wait for the authoritative response");
                screen.update(response(view, draft.withSettings(value, draft.nameGroup()), currentRevision));
            });
        }
        game.runOnClient(c -> {
            if (draft.designer().baseline().isEmpty()) {
                if (button(screen, "복제").active) throw new AssertionError("A new draft cannot duplicate an unloaded template");
            } else { click(screen, "복제"); checkRequest(requests, ProtocolIndustryPayloads.Action.DUPLICATE_TEMPLATE, "", 0); }
        });
        long copyRevision = revision++;
        game.runOnClient(c -> screen.update(response(view, draft, copyRevision))); game.waitTicks(2);
        game.runOnClient(c -> {
            click(screen, "초기화");
            checkRequest(requests, draft.designer().baseline().isEmpty() ? ProtocolIndustryPayloads.Action.NEW_TEMPLATE : ProtocolIndustryPayloads.Action.LOAD_TEMPLATE, draft.designer().baseline(), 0);
        });
        long resetRevision = revision;
        game.runOnClient(c -> screen.update(response(view, draft, resetRevision))); game.waitTicks(2);
        game.runOnClient(c -> {
            var save = button(screen, "편제 저장");
            if (draft.line().isEmpty() && save.active) throw new AssertionError("An empty draft cannot be saved");
            if (save.active) { click(screen, "편제 저장"); checkRequest(requests, ProtocolIndustryPayloads.Action.SAVE_TEMPLATE, draft.name(), 0); }
        });
    }
    private static ProtocolIndustryView response(ProtocolIndustryView view, ProtocolIndustryView.Template draft, long revision) {
        var gson = new Gson(); var json = gson.toJsonTree(view).getAsJsonObject();
        json.add("draft", gson.toJsonTree(draft)); json.addProperty("revision", revision);
        return gson.fromJson(json, ProtocolIndustryView.class);
    }
    private static void scrollDropdown(ScreenIndustry screen) {
        try {
            var barField = screen.getClass().getDeclaredField("divisionPickerBar"); barField.setAccessible(true);
            var offsetField = screen.getClass().getDeclaredField("divisionPickerScroll"); offsetField.setAccessible(true);
            var bar = (dev.hoi.client.ui.UiScrollBar)barField.get(screen);
            if (bar.maximum() == 0) return;
            var down = new MouseButtonEvent(bar.x() + 3, bar.top() + 12, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(down, false);
            var bottom = new MouseButtonEvent(bar.x() + 3, bar.bottom() - 9, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
            screen.mouseDragged(bottom, 0, bottom.y() - down.y()); screen.mouseReleased(bottom);
            if (offsetField.getInt(screen) != bar.maximum()) throw new AssertionError("Dropdown dragging must reach the final entry");
            screen.mouseScrolled(bar.x() - 20, bar.top() + 12, 0, 100);
            if (offsetField.getInt(screen) != 0) throw new AssertionError("Dropdown wheel must return to the first entry");
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static Button button(ScreenIndustry screen, String label) {
        return screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(label)).map(w -> (Button)w).findFirst().orElseThrow(() -> new AssertionError("Missing " + label));
    }
    private static void click(ScreenIndustry screen, String label) {
        var button = button(screen, label);
        var event = new MouseButtonEvent(button.getX() + 2, button.getY() + 2, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void checkRequest(List<ProtocolIndustryPayloads.Request> requests, ProtocolIndustryPayloads.Action action, String item, int amount) {
        if (requests.isEmpty()) throw new AssertionError("Missing request " + action);
        var request = requests.getLast();
        if (request.action() != action || !request.item().equals(item) || request.amount() != amount) throw new AssertionError("Incorrect designer request " + request);
    }
}
