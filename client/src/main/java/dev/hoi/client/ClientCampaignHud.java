package dev.hoi.client;

import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.protocol.ProtocolCountryHud;
import dev.hoi.protocol.ProtocolHudPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class ClientCampaignHud {
    private static String country = "";
    private static ProtocolCountryHud hud = ProtocolCountryHud.UNKNOWN;
    private ClientCampaignHud() {}
    public static void accept(ProtocolHudPayloads.State state) {
        var next = state.hud();
        if(!state.country().isEmpty()&&!state.country().equals(country))dev.hoi.client.audio.AudioKoreanMusic.enable();
        country = state.country(); hud = next;
    }
    public static void draw(net.minecraft.client.gui.GuiGraphicsExtractor g) {
        if (visible()) UiMenuBar.drawPassive(g, g.guiWidth(), hud, country);
    }
    public static String country() { return country; }
    public static ProtocolCountryHud hud() { return hud; }
    public static boolean visible() { return !country.isEmpty(); }
    public static void register() {
        ProtocolHudPayloads.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(ProtocolHudPayloads.State.TYPE, (packet, context) -> {
            try { accept(packet); }
            catch (IllegalArgumentException error) { accept(ProtocolHudPayloads.State.HIDDEN); }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> accept(ProtocolHudPayloads.State.HIDDEN));
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.fromNamespaceAndPath("hoi", "country_hud"), (g, delta) -> {
            var client = Minecraft.getInstance();
            if (visible() && client.level != null && client.gui.screen() == null)
                UiMenuBar.drawPassive(g, g.guiWidth(), hud, country);
        });
    }
}
