package dev.hoi.client;

import dev.hoi.protocol.ResearchView;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Real Minecraft renderer with synthetic UI fixtures, not a live campaign/network test. */
public final class ResearchScreenGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var dispatcher = new com.mojang.brigadier.CommandDispatcher<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>();
            HoiClient.registerCommands(dispatcher);
            if (dispatcher.getRoot().getChild("hoi") != null) throw new AssertionError("Client must not shadow /hoi start, /hoi map or /hoi stop");
            if (dispatcher.getRoot().getChild("hoi-research") == null) throw new AssertionError("Research shortcut is registered separately");
            dev.hoi.protocol.ResearchProtocol.registerPayloadTypes();
            dev.hoi.protocol.ResearchProtocol.registerPayloadTypes();
        });
        var technologies = new ArrayList<ResearchView.Tech>();
        int[] infantryYears = {2000, 2006, 2012, 2018, 2021, 2024, 2027, 2030, 2033, 2036};
        for (int i = 0; i < 36; i++) {
            var status = i == 2 ? ResearchView.Status.ACTIVE : i == 18 ? ResearchView.Status.AVAILABLE
                    : new ResearchView.Status[]{ResearchView.Status.LOCKED, ResearchView.Status.AVAILABLE, ResearchView.Status.COMPLETED}[i % 3];
            technologies.add(new ResearchView.Tech("hoi:test/infantry_" + i, "INFANTRY", "보병 장비 연구 " + (i + 1),
                    infantryYears[(i % 9)], 1, 160, status == ResearchView.Status.ACTIVE ? 63 : 0, 1.25,
                    i == 0 ? List.of() : List.of("hoi:test/infantry_" + (i - 1)),
                    List.of("보병 연구 속도 +5.0%"), List.of("시험용 보병 장비"), status));
        }
        int[] armorYears = {1980, 2018, 2020, 2022, 2024, 2028, 2032};
        for (int i = 0; i < armorYears.length; i++) technologies.add(new ResearchView.Tech(
                "hoi:test/armor_" + i, "ARMOR", "기갑 연구 " + (i + 1), armorYears[i], 1, 180, 0, 1,
                i == 0 ? List.of() : List.of("hoi:test/armor_" + (i - 1)), List.of(), List.of(),
                ResearchView.Status.AVAILABLE));
        var view = new ResearchView("ui-test", 1, "KOR", "대한민국", 72, "2020년 3월 13일 01시", "X1",
                List.of(new ResearchView.Slot(0, "", 12), new ResearchView.Slot(1, "hoi:test/infantry_2", 0),
                        new ResearchView.Slot(2, "", 0)), technologies, "UI 검증용 데이터");
        context.getInput().resizeWindow(1600, 1000);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> new ResearchScreen(view)); context.waitTicks(5);
        context.takeScreenshot("hoi-research-tree");
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            screen.clearFocus();
            screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
            screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
            boolean button = screen.children().stream().anyMatch(c -> c instanceof Button b && b.getMessage().getString().equals("선택 슬롯에서 연구 시작"));
            if (!button) throw new AssertionError("Keyboard must open the selected technology details");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-research-detail");
        context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0)));
        context.getInput().resizeWindow(854, 480);
        context.waitTicks(3); context.takeScreenshot("hoi-research-compact");
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            screen.mouseScrolled(100, screen.height - 40, -30, -30);
            screen.clearFocus(); screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0));
            screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
        });
        context.waitTicks(2); context.takeScreenshot("hoi-research-compact-detail");
        context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE, 0, 0)));
        context.getInput().resizeWindow(1600, 1000);
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            var armor = screen.children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals("기갑"))
                    .findFirst().orElseThrow();
            screen.setFocused(armor);
            screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ENTER, 0, 0));
            if (screen.children().stream().noneMatch(c -> c instanceof Button b && b.getMessage().getString().equals("• 기갑")))
                throw new AssertionError("Focused category button must receive Enter instead of reopening a technology");
        });
        context.waitTicks(3); context.takeScreenshot("hoi-research-armor");
        context.setScreen(() -> null);
    }
}
