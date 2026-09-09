package dev.hoi.client;

import dev.hoi.protocol.ConstructionProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

/** Map clicks send intent; the server resolves its own view ray and validates the selected building. */
public final class ConstructionMapInput {
    private ConstructionMapInput() {}

    private static boolean available(Minecraft client) {
        if (client.player == null || client.gui.screen() != null
                || !ClientPlayNetworking.canSend(ConstructionProtocol.CancelMap.TYPE)) return false;
        var item = client.player.getMainHandItem();
        return item.is(Items.CARROT_ON_A_STICK) && item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("hoi_atlas_selector", 0) == 5;
    }

    public static boolean place(Minecraft client) {
        if (!available(client) || client.gameMode == null) return false;
        client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        return true;
    }

    public static boolean cancel(Minecraft client) {
        if (!available(client)) return false;
        ClientPlayNetworking.send(ConstructionProtocol.CancelMap.INSTANCE);
        return true;
    }
}
