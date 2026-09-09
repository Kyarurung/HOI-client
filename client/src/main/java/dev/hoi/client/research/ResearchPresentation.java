package dev.hoi.client.research;

import com.google.gson.JsonParser;
import dev.hoi.protocol.ResearchView.Tech;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import java.util.*;


public final class ResearchPresentation {
    private ResearchPresentation() {}
    public static ResearchLayout apply(ResearchLayout fallback, ResourceManager resources, int viewport) {
        if (TfrResearchLayout.applies(fallback)) return TfrResearchLayout.fit(fallback, viewport);
        if(fallback.sourceTree()) return fallback;
        var asset = resources.getResource(Identifier.fromNamespaceAndPath("hoi", "ui/research_layout.json"));
        if (asset.isEmpty()) return fallback;
        try (var reader = asset.get().openAsReader()) {
            var document = JsonParser.parseReader(reader).getAsJsonObject();
            if (document.get("version").getAsInt() != 1) return fallback;
            var positions = document.getAsJsonObject("positions");
            var nodes = new ArrayList<ResearchLayout.Node>();
            int width = fallback.width(), height = fallback.height();
            for (var node : fallback.nodes()) {
                var values = positions.getAsJsonArray(sourceId(node.tech()));
                if (values == null || values.isEmpty()) { nodes.add(node); continue; }
                var position = values.get(0).getAsJsonObject();
                int x = position.get("x").getAsInt() + (int)(viewport * position.get("xRelative").getAsDouble());
                int y = position.get("y").getAsInt();
                if (x < 0 || y < 0 || x > 20_000 || y > 20_000) { nodes.add(node); continue; }
                nodes.add(new ResearchLayout.Node(node.tech(), x, y));
                width = Math.max(width, x + ResearchLayout.CARD_WIDTH + 20);
                height = Math.max(height, y + ResearchLayout.CARD_HEIGHT + 20);
            }

            for (int i = 0; i < nodes.size(); i++) for (int j = i + 1; j < nodes.size(); j++) {
                var a = nodes.get(i); var b = nodes.get(j);
                if (Math.abs(a.x() - b.x()) < ResearchLayout.CARD_WIDTH && Math.abs(a.y() - b.y()) < ResearchLayout.CARD_HEIGHT) return fallback;
            }
            return new ResearchLayout(List.copyOf(nodes), fallback.years(), width, height);
        } catch (Exception invalidPack) { return fallback; }
    }
    private static String sourceId(Tech tech) {
        if (tech.id().startsWith("hoi:tfr/technology/")) return tech.id().substring("hoi:tfr/technology/".length());
        if (tech.id().startsWith("hoi:")) return tech.id().substring(4);
        return tech.id();
    }
}
