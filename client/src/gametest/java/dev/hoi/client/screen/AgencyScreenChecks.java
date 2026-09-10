package dev.hoi.client.screen;

import dev.hoi.protocol.AgencyView.Item;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

public final class AgencyScreenChecks {
    public static void run(ClientGameTestContext context, List<Item> items) {
        var partial = items.stream().filter(i -> i.id().equals("upgrade:domestic_intelligence")).findFirst().orElseThrow();
        if (AgencyScreen.upgradeStages(partial) != 4 || AgencyScreen.upgradeLevel(partial) != 2
                || !AgencyScreen.upgradeIndicator(partial).equals("agency/researched/11"))
            throw new AssertionError("Two of four agency levels must use the original two-stripe indicator");
        context.runOnClient(client -> {
            for (int i = 0; i < 14; i++)
                if (client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/agency/researched/" + i + ".png")).isEmpty())
                    throw new AssertionError("Missing original agency indicator frame " + i);
        });
    }
}
