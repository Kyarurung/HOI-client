package dev.hoi.client.research;

import dev.hoi.protocol.ProtocolResearchPayloads;
import dev.hoi.protocol.ProtocolResearchView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;

public final class ResearchSlotTooltipTest {
    public static void run(ClientGameTestContext context, ProtocolResearchView base) {
        var technologies = new ArrayList<>(base.technologies());
        var original = technologies.get(2);
        var completed = new ProtocolResearchView.Tech(original.id(), original.category(), original.name(),
                original.year(), original.tier(), original.baseDays(), original.baseDays(), original.dailyRate(),
                original.prerequisites(), original.effects(), original.unlocks(), ProtocolResearchView.Status.COMPLETED,
                original.source(), original.tooltipOnlyEffects());
        technologies.set(2, completed);
        var slots = new ArrayList<>(base.slots());
        slots.set(2, new ProtocolResearchView.Slot(2, completed.id(), 0));
        var view = new ProtocolResearchView(base.session(), base.revision(), base.country(), base.countryName(),
                base.day(), base.date(), base.speed(), slots, technologies, base.message(), base.hud(), base.benefits());
        var requests = new ArrayList<ProtocolResearchPayloads.Request>();
        context.getInput().resizeWindow(1600, 1000);
        context.runOnClient(client -> client.options.guiScale().set(2));
        overview(context, view, requests);
        for (int index = 0; index < 3; index++) {
            final int slotIndex = index;
            var point = context.computeOnClient(client -> {
                var button = slot((ResearchScreen)client.gui.screen(), slotIndex);
                check(tooltip(button) == null, "Slot hover tooltip remains: " + slotIndex);
                var technology = view.technology(view.slots().get(slotIndex).technology());
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
                    "Hovering a research slot must not enqueue a rendered tooltip: " + slotIndex));
            context.takeScreenshot("hoi-research-slot-hover-" + List.of("empty", "active", "completed").get(index));
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
            check(mutations(requests).equals(List.of(new ProtocolResearchPayloads.Request(
                    ProtocolResearchPayloads.Action.START, view.session(), view.revision(), 0, available.id()))),
                    "Empty-slot selection preserves authenticated START");
        });
        overview(context, view, requests);
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            click(screen, slot(screen, 1));
            check(mutations(requests).isEmpty(), "Selecting active research does not cancel it");
            click(screen, action(screen, "연구 중단"));
            check(mutations(requests).equals(List.of(new ProtocolResearchPayloads.Request(
                    ProtocolResearchPayloads.Action.CANCEL, view.session(), view.revision(), 1,
                    view.slots().get(1).technology()))), "Active-slot selection preserves authenticated CANCEL");
        });
        overview(context, view, requests);
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            click(screen, slot(screen, 2));
            check(action(screen, "연구 완료").visible, "Completed slot still opens its completed detail");
            check(mutations(requests).isEmpty(), "Selecting completed research does not send a mutation");
            check(screen.tooltipLines(completed).stream().anyMatch(line -> line.getString().equals("연구됨")),
                    "Completed technology explanation remains available");
            check(completed.fraction() == 1 && completed.remainingDays(0) == 0, "Completed progress is preserved");
        });
    }
    private static void overview(ClientGameTestContext context, ProtocolResearchView view,
            ArrayList<ProtocolResearchPayloads.Request> requests) {
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
    private static List<ProtocolResearchPayloads.Request> mutations(List<ProtocolResearchPayloads.Request> requests) {
        return requests.stream().filter(request -> request.action() == ProtocolResearchPayloads.Action.START
                || request.action() == ProtocolResearchPayloads.Action.CANCEL).toList();
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
