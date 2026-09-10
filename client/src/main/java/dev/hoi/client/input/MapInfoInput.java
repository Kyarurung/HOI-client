package dev.hoi.client.input;

import dev.hoi.protocol.DialogProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class MapInfoInput {
    private MapInfoInput() {}
    public static boolean click(Minecraft client) {
        if (client.player == null || client.gui.screen() != null || !ClientPlayNetworking.canSend(DialogProtocol.MapClick.TYPE)) return false;
        var item = client.player.getMainHandItem();
        int selector = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("hoi_atlas_selector", 0);
        if (!item.is(Items.CARROT_ON_A_STICK) || selector < 1 || selector > 9) return false;
        ClientPlayNetworking.send(DialogProtocol.MapClick.INSTANCE); return true;
    }
}
