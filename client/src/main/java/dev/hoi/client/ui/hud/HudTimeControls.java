package dev.hoi.client.ui.hud;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.map.commander.CommanderHud;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.commander.CommanderOverviewPayloads.Tab;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public final class HudTimeControls {
    static final int DEFCON_X = 289, DEFCON_Y = 3, STEPS = 5, DATE_RIGHT = 249, DATE_TOP = 17;
    static final FontDescription CLOCK = new FontDescription.Resource(Identifier.fromNamespaceAndPath("hoi", "clock"));
    record HudBranch(String sprite, String label, Tab tab, int selector, int x) {}
    static final List<HudBranch> BRANCHES = List.of(new HudBranch("army", "TOPBAR_ARMY", Tab.ARMY, 1, 124),
            new HudBranch("navy", "TOPBAR_NAVY", Tab.NAVY, 2, 173), new HudBranch("air", "TOPBAR_AIR", Tab.AIR, 3, 219));
    private static int resume = 1;
    private static Tab pressed;
    private static boolean held;
    private static long flashUntil;
    private HudTimeControls() {}
    public static void register() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.afterMouseRelease(screen).register((current, event, consumed) -> { held = false; return consumed; }));
    }
    static float unit(int width) { return HudMenuBar.scale(width) / 2; }
    public static float x(int width, double bx) { return HudMenuBar.tensionLeft(width) + (float)((bx - DEFCON_X) * unit(width)); }
    public static float y(int width, double by) { return (float)((by - DEFCON_Y) * unit(width)); }
    public static int left(int width) { return (int)Math.floor(x(width, 0)); }
    static int level(String speed) { return speed.startsWith("X") ? Integer.parseInt(speed.substring(1)) : 0; }
    static int selected() {
        int level = level(ClientCampaignHud.clock().speed());
        if (level > 0) resume = level;
        return resume;
    }
    static int litSteps() { return (selected() + 1) / 2; }
    static boolean control() { return ClientCampaignHud.clock().control(); }
    static void select(int target) {
        if (target == selected()) return;
        if (level(ClientCampaignHud.clock().speed()) > 0) speed(target);
        else resume = target;
    }
    static void pause() {
        int current = level(ClientCampaignHud.clock().speed());
        speed(current > 0 ? 0 : selected());
    }
    static void draw(GuiGraphicsExtractor g, int width) {
        var clock = ClientCampaignHud.clock(); float k = unit(width);
        g.pose().pushMatrix();
        g.pose().translate(x(width, 0), y(width, 0));
        g.pose().scale(k, k);
        UiAssets.draw(g, "hud/topbar/overview_background", 0, 0, 289, 101);
        if (clock.speed().equals("PAUSED")) {
            double phase = Util.getMillis() % 2000 / 1000.0;
            UiAssets.draw(g, "hud/topbar/paused/frame_0", 77, 9, 206, 28);
            UiAssets.drawTinted(g, "hud/topbar/paused/frame_1", 77, 9, 206, 28, (int)Math.round((phase < 1 ? phase : 2 - phase) * 255) << 24 | 0xFFFFFF);
        } else UiAssets.draw(g, "hud/topbar/pause_background", 77, 9, 206, 28);
        int lit = litSteps();
        for (int i = 0; i < STEPS; i++) UiAssets.draw(g, "hud/topbar/speed_step/frame_" + (i >= lit ? 0 : i == STEPS - 1 ? 2 : 1), 115 + 25 * i, 36, 28, 10);
        for (int direction : new int[]{-1, 1}) {
            String sprite = direction < 0 ? "hud/topbar/speed_down" : "hud/topbar/speed_up";
            UiAssets.drawTinted(g, sprite, direction < 0 ? 79 : 254, 10, 27, 27, control() ? 0xFFFFFFFF : 0xFF808080);
        }
        if (Minecraft.getInstance().gui.screen() == null) held = false;
        if (pressed != null && !held && Util.getMillis() >= flashUntil) pressed = null;
        for (var branch : BRANCHES) UiAssets.draw(g, "hud/topbar/" + branch.sprite() + "_button/frame_" + (branch.tab() == pressed ? 1 : 0), branch.x(), 46, 38, 38);
        UiAssets.draw(g, "music/toggle", 88, 48, 33, 33);
        var font = Minecraft.getInstance().font;
        var date = Component.literal(clock.date()).withStyle(style -> style.withFont(CLOCK));
        g.text(font, date, DATE_RIGHT - font.width(date), DATE_TOP, 0xFFFFFFFF, false);
        g.pose().popMatrix();
    }
    public static java.util.function.Consumer<String> commandObserver;
    private static void speed(int level) {
        String command = "속도 " + Math.clamp(level, 0, 10);
        var client = Minecraft.getInstance();
        if (commandObserver != null) commandObserver.accept(command);
        else if (client.player != null) client.player.connection.sendCommand(command);
    }
    static final class HudTimeButton extends Button {
        private final String sound, over;
        private final BooleanSupplier enabled;
        private boolean hovering;
        HudTimeButton(int width, double bx, double by, double bw, double bh, String label, String sound, String over, BooleanSupplier enabled, OnPress press) {
            super(Math.round(x(width, bx)), Math.round(y(width, by)), Math.max(1, Math.round(x(width, bx + bw)) - Math.round(x(width, bx))),
                    Math.max(1, Math.round(y(width, by + bh)) - Math.round(y(width, by))), Component.literal(label), press, DEFAULT_NARRATION);
            this.sound = sound; this.over = over; this.enabled = enabled; active = enabled.getAsBoolean();
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play(sound); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            active = enabled.getAsBoolean();
            boolean now = isHovered() && active;
            if (now && !hovering && over != null) AudioUiSounds.play(over);
            hovering = now;
        }
    }
    static List<Button> buttons(int width) {
        var result = new ArrayList<Button>();
        for (int direction : new int[]{-1, 1}) {
            var button = new HudTimeButton(width, direction < 0 ? 79 : 254, 10, 27, 27, direction < 0 ? "속도 느리게" : "속도 빠르게", direction < 0 ? "ui.time_down" : "ui.time_up",
                    "ui.menu_over", HudTimeControls::control, b -> select(Math.clamp(selected() + direction, 1, 10)));
            button.setTooltip(Tooltips.createOfficer(IndustryEquipmentDesigns.tooltip(direction < 0 ? "SPEED_DECREASE" : "SPEED_INCREASE")));
            result.add(button);
        }
        result.add(new HudTimeButton(width, 106, 9, 148, 27, "일시정지", "ui.pause", null, HudTimeControls::control, b -> pause()));
        for (int i = 0; i < STEPS; i++) {
            int target = 2 * (i + 1);
            result.add(new HudTimeButton(width, 115 + 25 * i, 36, i == STEPS - 1 ? 28 : 25, 10, target + "배속", "ui.click", null, HudTimeControls::control, b -> select(target)));
        }
        for (var branch : BRANCHES) {
            var button = new HudTimeButton(width, branch.x(), 46, 38, 38, IndustryEquipmentDesigns.tooltip(branch.label() + "_DESC"), "ui.close", "ui.menu_over", () -> true,
                    b -> {
                        pressed = branch.tab();
                        held = true;
                        flashUntil = Util.getMillis() + 150;
                        CommanderHud.toggleOverview(branch.tab(), branch.selector());
                    });
            button.setTooltip(Tooltips.createOfficer("§Y" + IndustryEquipmentDesigns.tooltip(branch.label()) + "§!\n" + IndustryEquipmentDesigns.tooltip(branch.label() + "_DESC")));
            result.add(button);
        }
        return result;
    }
}
