package dev.hoi.client.input;

import dev.hoi.protocol.ProtocolDialogPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class InputMapInfo {
    private InputMapInfo() {}
    public static boolean clickAt(Minecraft client, double x, double y, int width, int height) {
        if(dev.hoi.client.map.MapCommanderHud.clickAt(x,y))return true;
        if(dev.hoi.client.map.MapUnitHud.selectAt(x,y))return true;
        if(client.player==null||!ClientPlayNetworking.canSend(ProtocolDialogPayloads.MapClick.TYPE)||!client.options.getCameraType().isFirstPerson())return false;
        var matrix=client.gameRenderer.mainCamera().getViewRotationProjectionMatrix(new org.joml.Matrix4f()).invert();
        var ray=matrix.transformProject(new org.joml.Vector3f((float)(x/width*2-1),(float)(1-y/height*2),1)).normalize();
        ClientPlayNetworking.send(new ProtocolDialogPayloads.MapClick(ray.x,ray.y,ray.z));return true;
    }
    public static boolean click(Minecraft client) {
        if (client.player == null || client.gui.screen() != null) return false;
        if(dev.hoi.client.map.MapCommanderHud.clickAt(client.getWindow().getGuiScaledWidth()/2.0,client.getWindow().getGuiScaledHeight()/2.0))return true;
        if(dev.hoi.client.map.MapUnitHud.selectAt(client.getWindow().getGuiScaledWidth()/2.0,client.getWindow().getGuiScaledHeight()/2.0))return true;
        if(!ClientPlayNetworking.canSend(ProtocolDialogPayloads.MapClick.TYPE))return false;
        var item = client.player.getMainHandItem();
        int selector = item.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntOr("hoi_atlas_selector", 0);
        if (!item.is(Items.CARROT_ON_A_STICK) || selector < 1 || selector > 9) return false;
        ClientPlayNetworking.send(ProtocolDialogPayloads.MapClick.INSTANCE); return true;
    }
}
