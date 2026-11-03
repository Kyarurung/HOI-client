package dev.hoi.client.input;

import dev.hoi.protocol.ProtocolDialogPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class InputMapInfo {
    private InputMapInfo() {}
    public static void register() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof InputSidebarMovement.InputMovementScreen sidebar) {
                net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents.afterMouseClick(screen).register((current, event, consumed) -> {
                    if (consumed || client.gui.screen() != current || !sidebar.allowsMovement()
                            || event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                            || event.x() < sidebar.unitHudLeft() || event.y() < dev.hoi.client.ui.UiMenuBar.height(current.width)) return consumed;
                    return clickAt(client, event.x(), event.y(), current.width, current.height);
                });
            }
        });
    }
    private static void closeContent(net.minecraft.client.gui.screens.Screen previous) {
        if (previous != null && !(previous instanceof dev.hoi.client.screen.ScreenMapCursor)
                && dev.hoi.client.screen.ScreenDialogCoordinator.contentScreen() == previous) {
            if (previous instanceof dev.hoi.client.screen.ScreenArmyHeadquarters) previous.onClose();
            dev.hoi.client.screen.ScreenDialogCoordinator.setContent(new dev.hoi.client.screen.ScreenMapCursor());
        }
    }
    public static boolean clickAt(Minecraft client, double x, double y, int width, int height) {
        var previous = dev.hoi.client.screen.ScreenDialogCoordinator.contentScreen();
        if(dev.hoi.client.map.MapCommanderHud.clickAt(x,y)){closeContent(previous);return true;}
        if(dev.hoi.client.map.MapUnitHud.selectAt(x,y)){closeContent(previous);return true;}
        if(client.player==null||!ClientPlayNetworking.canSend(ProtocolDialogPayloads.MapClick.TYPE)||!client.options.getCameraType().isFirstPerson())return false;
        var matrix=client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
        var ray=matrix.transformProject(new org.joml.Vector3f((float)(x/width*2-1),(float)(1-y/height*2),1)).normalize();
        closeContent(previous);
        ClientPlayNetworking.send(new ProtocolDialogPayloads.MapClick(ray.x,ray.y,ray.z));return true;
    }
    public static boolean click(Minecraft client) {
        if (client.player == null || client.gui.screen() != null) return false;
        if (dev.hoi.client.map.MapRaidHud.click(new net.minecraft.client.input.MouseButtonEvent(
                client.getWindow().getGuiScaledWidth() / 2.0, client.getWindow().getGuiScaledHeight() / 2.0,
                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,
                        client.player.isShiftKeyDown() ? com.mojang.blaze3d.platform.InputConstants.MOD_SHIFT : 0)), false)) {
            client.gui.setScreen(new dev.hoi.client.screen.ScreenMapCursor()); return true;
        }
        if(dev.hoi.client.map.MapCommanderHud.clickAt(client.getWindow().getGuiScaledWidth()/2.0,client.getWindow().getGuiScaledHeight()/2.0))return true;
        if(dev.hoi.client.map.MapUnitHud.selectAt(client.getWindow().getGuiScaledWidth()/2.0,client.getWindow().getGuiScaledHeight()/2.0))return true;
        if(!ClientPlayNetworking.canSend(ProtocolDialogPayloads.MapClick.TYPE))return false;
        var item = client.player.getMainHandItem();
        int selector = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("hoi_atlas_selector", 0);
        if (!item.is(Items.CARROT_ON_A_STICK) || selector < 1 || selector > 9) return false;
        ClientPlayNetworking.send(ProtocolDialogPayloads.MapClick.INSTANCE); return true;
    }
}
