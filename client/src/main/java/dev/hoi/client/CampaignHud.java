package dev.hoi.client;

import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.protocol.CountryHud;
import dev.hoi.protocol.HudProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class CampaignHud {
    private static String country = "";
    private static CountryHud hud = CountryHud.UNKNOWN;
    private CampaignHud() {}
    public static void accept(HudProtocol.State state) {
        var next = state.hud();
        if(!state.country().isEmpty()&&!state.country().equals(country))dev.hoi.client.audio.KoreanMusic.enable();
        country = state.country(); hud = next;
    }
    public static boolean visible() { return !country.isEmpty(); }
    public static void register() {
        HudProtocol.registerPayloadTypes();
        ClientPlayNetworking.registerGlobalReceiver(HudProtocol.State.TYPE, (packet, context) -> {
            try { accept(packet); }
            catch (IllegalArgumentException error) { accept(HudProtocol.State.HIDDEN); }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> accept(HudProtocol.State.HIDDEN));
        HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, Identifier.fromNamespaceAndPath("hoi", "country_hud"), (g, delta) -> {
            var client = Minecraft.getInstance();
            if (visible() && client.level != null && client.gui.screen() == null)
                HoiMenuBar.drawPassive(g, g.guiWidth(), hud, country);
        });
    }
}
