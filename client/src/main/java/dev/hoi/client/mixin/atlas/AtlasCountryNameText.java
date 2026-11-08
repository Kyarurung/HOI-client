package dev.hoi.client.mixin.atlas;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.TextDisplay.class)
public interface AtlasCountryNameText {
    @Invoker("getText") Component hoi$countryText();
}
