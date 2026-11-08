package dev.hoi.client.screen.research;

import dev.hoi.protocol.research.ResearchPayloads;
import dev.hoi.protocol.research.ResearchView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;

public final class ResearchSlotTooltipTest {
    public static void runSource(ClientGameTestContext context) {
        for (String fixture : List.of("prc-research-slot-design-team", "research-year-4-51", "research-year-4-00")) {
        var source = new ResearchPayloads.Response(dev.hoi.client.screen.agency.AgencyDesignTeamCandidatesTest.fixture(fixture)).view();
        if (fixture.startsWith("research-year-")) check(source.slots().getFirst().yearTooltip().contains(fixture.endsWith("51") ? "4.51년" : "4.00년"), "Server year formatting retains two places");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            var requests = new ArrayList<ResearchPayloads.Request>();overview(context,source,requests);
            var point = context.computeOnClient(client->{
                var screen=(ResearchScreen)client.gui.screen();var button=slot(screen,0);
                int x=button.sourceX(59),y=button.sourceY(62);
                check(screen.slotTooltipAt(x,y).equals(source.slots().getFirst().yearTooltip()),"Year region preserves authoritative year tooltip exactly");
                double scale=client.getWindow().getGuiScale();return new double[]{x*scale,y*scale};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(4);context.takeScreenshot("hoi-research-year-source-"+fixture+"-"+size[0]+"-gui2");
            context.runOnClient(client->check(requests.isEmpty(),"Year hover must not mutate research"));
        }
        }
    }
    public static void run(ClientGameTestContext context, ResearchView base) {
        var technologies = new ArrayList<>(base.technologies());
        var active = technologies.get(1);
        var source = new ResearchView.Source("tooltip_fixture", "infantry", 0, 0, false, List.of(), List.of(), List.of(), List.of(), "시험 설명 첫 줄\n-----\n시험 설명 둘째 줄", List.of(), java.util.Map.of());
        technologies.set(1, new ResearchView.Tech(active.id(), active.category(), active.name(), active.year(), active.tier(), active.baseDays(), active.progress(), active.dailyRate(), active.prerequisites(), active.effects(), active.unlocks(), active.status(), source, active.tooltipOnlyEffects(), List.of(), List.of(new ResearchView.UnitCard("infantry", "보병", List.of(new ResearchView.StatLine("대인 공격", "+5.00%")), List.of()))));
        var original = technologies.get(2);
        var completed = new ResearchView.Tech(original.id(), original.category(), original.name(),
                original.year(), original.tier(), original.baseDays(), original.baseDays(), original.dailyRate(),
                original.prerequisites(), original.effects(), original.unlocks(), ResearchView.Status.COMPLETED,
                original.source(), original.tooltipOnlyEffects());
        technologies.set(2, completed);
        var slots = new ArrayList<>(base.slots());
        slots.set(2, new ResearchView.Slot(2, completed.id(), 0));
        var view = new ResearchView(base.session(), base.revision(), base.country(), base.countryName(),
                base.day(), base.date(), base.speed(), slots, technologies, base.message(), base.hud(), base.benefits());
        var requests = new ArrayList<ResearchPayloads.Request>();
        for (int[] size : List.of(new int[]{2560,1440},new int[]{1280,720})) {
        context.getInput().resizeWindow(size[0], size[1]);
        context.runOnClient(client -> client.options.guiScale().set(2));
        lineStates(context, view, size);
        overview(context, view, requests);
        for (int index = 0; index < 3; index++) {
            final int slotIndex = index;
            var point = context.computeOnClient(client -> {
                var button = slot((ResearchScreen)client.gui.screen(), slotIndex);
                check(tooltip(button) == null, "Slot uses its source tooltip renderer: " + slotIndex);
                var technology = view.technology(view.slots().get(slotIndex).technology());
                var screen = (ResearchScreen)client.gui.screen();
                String text = screen.slotTooltipAt(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0);
                check(technology == null ? text.isEmpty() : text.startsWith("§Y"+technology.name()), "Only occupied slots show the authoritative technology tooltip");
                check(!text.contains("연구 시간") && !text.contains("앞선 기술") && !text.contains("-----"), "Slot tooltip omits tree timing and text separators");
                if (slotIndex == 1) {
                    check(text.contains("£unit_infantry_icon_medium£") && text.contains("+5.00%") && text.indexOf("보병") < text.indexOf("시험 설명"), "Slot preserves unit icon, exact precision and effect/description order");
                    check(text.contains("\n\n시험 설명") && text.contains("첫 줄\n\n시험 설명 둘째 줄"), "Description and source separators use one blank line");
                }
                check(button.getMessage().getString().equals("슬롯 " + (slotIndex + 1) + " · "
                        + (technology == null ? "연구 선택" : technology.name())), "Slot accessible name changed");
                check(button.active && button.visible, "Slot selection remains available");
                double scale = client.getWindow().getGuiScale();
                return new double[]{(button.getX() + button.getWidth() / 2.0) * scale,
                        (button.getY() + button.getHeight() / 2.0) * scale};
            });
            context.getInput().setCursorPos(point[0], point[1]);
            context.waitTicks(12);
            context.runOnClient(client -> check(!deferredTooltip(client, slot((ResearchScreen)client.gui.screen(), slotIndex)),
                    "Custom source tooltip must not enqueue a second widget tooltip: " + slotIndex));
            context.takeScreenshot("hoi-research-slot-hover-" + List.of("empty", "active", "completed").get(index)+"-"+size[0]+"-gui2");
        }
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            click(screen, slot(screen, 0));
            check(mutations(requests).isEmpty(), "Selecting an empty slot does not send a mutation");
            check(!screen.allowsMovement(), "Empty slot still opens the research tree");
            check(screen.children().stream().filter(ResearchTabButton.class::isInstance)
                    .map(ResearchTabButton.class::cast).anyMatch(button -> tooltip(button) != null),
                    "Category tooltips remain available");
            var available = view.technologies().getFirst();
            var tooltip = screen.tooltipLines(available).stream().map(line -> line.getString()).toList();
            check(tooltip.contains(available.name()) && tooltip.stream().anyMatch(line -> line.contains("시험 효과 +5%")),
                    "Technology tooltip keeps its title and original effects");
        });
        var tabPoint = context.computeOnClient(client -> {
            var tab = firstTab((ResearchScreen)client.gui.screen());
            double scale = client.getWindow().getGuiScale();
            return new double[]{(tab.getX() + tab.getWidth() / 2.0) * scale,
                    (tab.getY() + tab.getHeight() / 2.0) * scale};
        });
        context.getInput().setCursorPos(tabPoint[0], tabPoint[1]);
        context.waitTicks(12);
        context.runOnClient(client -> check(deferredTooltip(client, firstTab((ResearchScreen)client.gui.screen())),
                "Category hover still enqueues its rendered tooltip"));
        context.takeScreenshot("hoi-research-category-hover-preserved");
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            var available = view.technologies().getFirst();
            screen.showDetail(available.id());
            click(screen, action(screen, "연구"));
            check(mutations(requests).equals(List.of(new ResearchPayloads.Request(
                    ResearchPayloads.Action.START, view.session(), view.revision(), 0, available.id()))),
                    "Empty-slot selection preserves authenticated START");
        });
        overview(context, view, requests);
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            click(screen, slot(screen, 1));
            check(mutations(requests).isEmpty(), "Selecting active research does not cancel it");
            click(screen, action(screen, "연구"));
            check(mutations(requests).equals(List.of(new ResearchPayloads.Request(
                    ResearchPayloads.Action.CANCEL, view.session(), view.revision(), 1,
                    view.slots().get(1).technology()))), "Active-slot selection preserves authenticated CANCEL");
        });
        overview(context, view, requests);
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            click(screen, slot(screen, 2));
            check(action(screen, "연구").visible, "Completed slot still opens its completed detail");
            check(mutations(requests).isEmpty(), "Selecting completed research does not send a mutation");
            check(screen.tooltipLines(completed).stream().anyMatch(line -> line.getString().equals("연구됨")),
                    "Completed technology explanation remains available");
            check(completed.fraction() == 1 && completed.remainingDays(0) == 0, "Completed progress is preserved");
        });
        }
    }
    private static void lineStates(ClientGameTestContext context, ResearchView base, int[] size) {
        var gson = new com.google.gson.Gson();
        java.awt.image.BufferedImage plain = null;
        for (int state = 0; state < 4; state++) {
            int percent = state == 3 ? 50 : state * 50;
            boolean assigned = state == 3;
            var json = gson.toJsonTree(base).getAsJsonObject();
            var tech = json.getAsJsonArray("technologies").get(1).getAsJsonObject();
            tech.addProperty("progress", tech.get("baseDays").getAsInt() * percent / 100.0);
            var first = json.getAsJsonArray("slots").get(0).getAsJsonObject();
            first.addProperty("savedDays", percent * 30 / 100);
            if (assigned) {
                var team = new ResearchView.DesignTeam("slot_visual", "시험 업체", "mio/country/kor/gfx_idea_kor_hdhyundai/frame_0", 1, "시험 유형", List.of(), 0, 0, 0, "", List.of(), List.of(), List.of(), "시험 업체 툴팁");
                json.add("designTeams", gson.toJsonTree(List.of(team)));
                json.getAsJsonArray("slots").get(1).getAsJsonObject().addProperty("designTeam", team.id());
            }
            var view = gson.fromJson(json, ResearchView.class);
            var requests = new ArrayList<ResearchPayloads.Request>();
            overview(context, view, requests);
            context.getInput().setCursorPos(size[0] - 2, size[1] - 2);
            context.waitTicks(3);
            var geometry = context.computeOnClient(client -> {
                check(client.options.guiScale().get() == 2 && client.getWindow().getGuiScale() == 2, "Research evidence must use actual GUI2");
                var screen = (ResearchScreen)client.gui.screen();
                var button = slot(screen, 1);
                check(button.getBottom() <= slot(screen, 2).getY(), "Source-height research lines do not overlap");
                return new double[]{button.getX() * 2, button.getY() * 2, button.sourceScale() * 2, slot(screen, 0).getY() * 2};
            });
            var path = context.takeScreenshot("hoi-research-line-state-" + percent + (assigned ? "-assigned-" : "-plain-") + size[0] + "-gui2");
            try {
                var image = javax.imageio.ImageIO.read(path.toFile());
                check(image.getWidth() == size[0] && image.getHeight() == size[1], "Screenshot preserves the actual framebuffer aspect ratio");
                for (int row = 0; row < 2; row++) {
                    int y = (int)Math.floor((row == 0 ? geometry[3] : geometry[1]) + 83 * geometry[2]);
                    int start = image.getRGB((int)Math.floor(geometry[0] + 100 * geometry[2]), y) & 0xFFFFFF;
                    int end = image.getRGB((int)Math.floor(geometry[0] + 420 * geometry[2]), y) & 0xFFFFFF;
                    check(percent == 0 ? start == 0 : Math.abs((start >> 16) - (start & 255)) < 4 && (start & 255) > 100, "Research and banked-days bars preserve the original neutral fill");
                    check(percent == 100 ? (end & 255) > 100 : end == 0, "Progress endpoints use the authoritative fraction");
                }
                if (state == 1) plain = image;
                if (assigned) {
                    int changed = 0;
                    for (int y = 26; y < 64; y++) for (int x = 8; x < 55; x++) {
                        int px = (int)Math.floor(geometry[0] + x * geometry[2]), py = (int)Math.floor(geometry[1] + y * geometry[2]);
                        if (image.getRGB(px, py) != plain.getRGB(px, py)) changed++;
                    }
                    check(changed > 100, "Assigned research uses the original gear background");
                }
                java.nio.file.Files.writeString(path.resolveSibling(path.getFileName() + ".json"), gson.toJson(java.util.Map.of("width", image.getWidth(), "height", image.getHeight(), "guiScale", 2, "geometry", geometry, "percent", percent, "assigned", assigned, "synthetic", true)));
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
            check(mutations(requests).isEmpty(), "Rendering progress and assignment must not mutate research");
        }
    }
    private static void overview(ClientGameTestContext context, ResearchView view,
            ArrayList<ResearchPayloads.Request> requests) {
        context.setScreen(() -> new ResearchScreen(view, requests::add, tab -> {}));
        context.waitTicks(2);
        context.runOnClient(client -> requests.clear());
    }
    private static ResearchSlotButton slot(ResearchScreen screen, int index) {
        return screen.children().stream().filter(ResearchSlotButton.class::isInstance)
                .map(ResearchSlotButton.class::cast).toList().get(index);
    }
    private static Button action(ResearchScreen screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.visible && button.getMessage().getString().equals(label)).findFirst().orElseThrow();
    }
    private static void click(ResearchScreen screen, Button button) {
        check(button.active, "Action is active: " + button.getMessage().getString());
        var event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        check(screen.mouseClicked(event, false), "Mouse selection accepted: " + button.getMessage().getString());
    }
    private static List<ResearchPayloads.Request> mutations(List<ResearchPayloads.Request> requests) {
        return requests.stream().filter(request -> request.action() == ResearchPayloads.Action.START
                || request.action() == ResearchPayloads.Action.CANCEL).toList();
    }
    private static net.minecraft.client.gui.components.Tooltip tooltip(Button button) {
        try {
            var field = net.minecraft.client.gui.components.AbstractWidget.class.getDeclaredField("tooltip");
            field.setAccessible(true);
            return ((net.minecraft.client.gui.components.WidgetTooltipHolder)field.get(button)).get();
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Minecraft widget tooltip holder changed", failure);
        }
    }
    private static ResearchTabButton firstTab(ResearchScreen screen) {
        return screen.children().stream().filter(ResearchTabButton.class::isInstance)
                .map(ResearchTabButton.class::cast).findFirst().orElseThrow();
    }
    private static boolean deferredTooltip(net.minecraft.client.Minecraft client, Button button) {
        int x = button.getX() + button.getWidth() / 2;
        int y = button.getY() + button.getHeight() / 2;
        var graphics = new net.minecraft.client.gui.GuiGraphicsExtractor(client,
                new net.minecraft.client.renderer.state.gui.GuiRenderState(), x, y);
        client.gui.screen().extractRenderState(graphics, x, y, 0);
        try {
            var field = net.minecraft.client.gui.GuiGraphicsExtractor.class.getDeclaredField("deferredTooltip");
            field.setAccessible(true);
            return field.get(graphics) != null;
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("Minecraft deferred tooltip field changed", failure);
        }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
