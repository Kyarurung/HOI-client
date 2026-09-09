package dev.hoi.client.research;

import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.charset.StandardCharsets;
import java.util.List;


public final class ResearchArtChecks {
    public static void run(ClientGameTestContext context) {
        ResearchView view;
        try (var input = ResearchArtChecks.class.getResourceAsStream("/tfr-research-view.json")) {
            view = new ResearchProtocol.Response(new String(input.readAllBytes(), StandardCharsets.UTF_8)).view();
        } catch (Exception error) { throw new AssertionError(error); }
        var ids = List.of("antiair2", "advanced_small_airframe", "modern_small_airframe");
        context.runOnClient(client -> {
            for (var id : ids) {
                var tech = view.technology("hoi:tfr/technology/" + id);
                var path = UiAssets.technologyPath("KOR", tech);
                String expected = (id.equals("antiair2") ? "technology/" : "technology/country/kor/") + "tfr/technology/" + id;
                if (!path.equals(expected)) throw new AssertionError("Original research art unavailable: " + id);
                if (!id.equals("antiair2") && UiAssets.technologyPath("JAP", tech).contains("/kor/"))
                    throw new AssertionError("Korean artwork leaked into another country");
                var cards = ResearchDetails.cards(tech, "KOR");
                if (cards.stream().filter(c -> c.kind().equals("장비")).anyMatch(c -> !c.texture().equals(path)))
                    throw new AssertionError("Missing equipment illustration in detail: " + id);
            }
            if (view.technologies().stream().anyMatch(t -> t.id().endsWith("/big_gun_tech")))
                throw new AssertionError("Nonexistent research in fixture");
        });
        context.getInput().resizeWindow(1280, 720);
        context.setScreen(() -> new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("연구 원본 이미지 확인")) {
            @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
                g.fill(0, 0, width, height, 0xFF101B22);
                for (int i = 0; i < ids.size(); i++) {
                    var tech = view.technology("hoi:tfr/technology/" + ids.get(i));
                    g.text(font, tech.name(), 24, 22 + i * 96, 0xFFE5E7E8);
                    UiAssets.technology(g, "KOR", tech, 24, 40 + i * 96, 181, 65, 0xFFFFFFFF);
                }
            }
        });
        context.waitTicks(4); context.takeScreenshot("hoi-research-three-original-images");
        context.runOnClient(client -> client.gui.setScreen(null));
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
}
