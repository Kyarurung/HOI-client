package dev.hoi.client.map;

import dev.hoi.client.mixin.atlas.AtlasCountryNameText;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class MapCountryNameVisibility {
    private static final FontDescription FONT = new FontDescription.Resource(Identifier.parse("hoi:country_names"));
    public static boolean atlas(Level level) {
        return level != null && level.dimension().identifier().toString().startsWith("hoi:atlas_");
    }
    public static boolean countryFont(FormattedCharSequence text) {
        return !text.accept((index, style, code) -> !FONT.equals(style.getFont()));
    }
    public static boolean independent(Entity entity) {
        return atlas(entity.level()) && entity instanceof Display.TextDisplay text
                && countryFont(((AtlasCountryNameText)text).hoi$countryText().getVisualOrderText());
    }
    private MapCountryNameVisibility() {}
}
