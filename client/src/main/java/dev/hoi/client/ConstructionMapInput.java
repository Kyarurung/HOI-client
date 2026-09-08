package dev.hoi.client;

import dev.hoi.protocol.ConstructionProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Left clicks in the unobstructed atlas send intent; the server resolves its own view ray. */
public final class ConstructionMapInput {
    private ConstructionMapInput() {}

    public static boolean cancel(Minecraft client) {
        if (client.player == null || client.gui.screen() != null
                || !ClientPlayNetworking.canSend(ConstructionProtocol.CancelMap.TYPE)) return false;
        var item = client.player.getMainHandItem();
        if (!item.is(Items.CARROT_ON_A_STICK) || item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("hoi_atlas_selector", 0) != 5) return false;
        ClientPlayNetworking.send(ConstructionProtocol.CancelMap.INSTANCE);
        return true;
    }
}
