package dev.hoi.client;

import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.hud.HudPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ClientCampaignHud {
    private static String country = "";
    private static CountryHud hud = CountryHud.UNKNOWN;
    private static HudPayloads.Clock clock = new HudPayloads.Clock("", "", false);
    private ClientCampaignHud() {}
    public static void accept(HudPayloads.State state) {
        var next = state.hud();
        if(!state.country().isEmpty()&&!state.country().equals(country))dev.hoi.client.audio.AudioKoreanMusic.enable();
        country = state.country(); hud = next;
        if (country.isEmpty()) clock = new HudPayloads.Clock("", "", false);
    }
    public static void accept(HudPayloads.Clock next) { clock = next; }
    public static HudPayloads.Clock clock() { return clock; }
    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor g) {
        if (visible()) HudMenuBar.drawPassive(g, g.guiWidth(), hud, country);
    }
    public static String country() { return country; }
    public static CountryHud hud() { return hud; }
    public static boolean visible() { return !country.isEmpty(); }
    public static void register() {
        HudPayloads.registerPayloadTypes();
        dev.hoi.client.ui.hud.HudTimeControls.register();
        ClientPlayNetworking.registerGlobalReceiver(HudPayloads.State.TYPE, (packet, context) -> {
            try { accept(packet); }
            catch (IllegalArgumentException error) { accept(HudPayloads.State.HIDDEN); }
        });
        ClientPlayNetworking.registerGlobalReceiver(HudPayloads.Clock.TYPE, (packet, context) -> accept(packet));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> accept(HudPayloads.State.HIDDEN)));
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.fromNamespaceAndPath("hoi", "country_hud"), (g, delta) -> {
            var client = Minecraft.getInstance();
            if (visible() && client.level != null && client.gui.screen() == null)
                HudMenuBar.drawPassive(g, g.guiWidth(), hud, country);
        });
    }
}
