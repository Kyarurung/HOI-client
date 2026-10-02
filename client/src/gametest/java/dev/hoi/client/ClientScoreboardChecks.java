package dev.hoi.client;

import dev.hoi.protocol.ProtocolCampaignStyle;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

final class ClientScoreboardChecks {
    static void run(ClientGameTestContext context) {
        for (String speed : new String[]{"PAUSED", "X1", "X10"}) {
            context.runOnClient(client -> {
                var board = client.level.getScoreboard();
                var previous = board.getObjective("hoi_preview");
                if (previous != null) board.removeObjective(previous);
                var title = Component.empty();
                String name = "The Fire Rises";
                for (int i = 0; i < name.length(); i++) {
                    int a = i < 6 ? 0xc46cda : 0xe9a8f2, b = i < 6 ? 0xe9a8f2 : 0xfff0fa;
                    double t = i < 6 ? i / 5.0 : (i - 6) / (name.length() - 7.0);
                    int color = 0;
                    for (int shift : new int[]{16, 8, 0}) color |= (int)Math.round(((a >> shift) & 255) * (1-t) + ((b >> shift) & 255) * t) << shift;
                    title.append(Component.literal(name.substring(i, i + 1)).withColor(color).withStyle(net.minecraft.ChatFormatting.BOLD));
                }
                var objective = board.addObjective("hoi_preview", ObjectiveCriteria.DUMMY, title, ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
                var rows = new Component[]{Component.literal("게임 속도: ").append(ProtocolCampaignStyle.speed(speed)),
                        Component.literal("Kiruy, ").append(Component.literal("대한민국").withColor(0x4d64af))};
                for (int i = 0; i < rows.length; i++) {
                    var score = board.getOrCreatePlayerScore(ScoreHolder.forNameOnly("hoi_row_" + i), objective);
                    score.set(rows.length - i); score.display(rows[i]);
                }
                board.setDisplayObjective(DisplaySlot.SIDEBAR, objective);
            });
            context.setScreen(() -> null); context.waitTicks(3);
            ResearchScreenTest.gui2Screenshot(context, "hoi-scoreboard-" + speed.toLowerCase(java.util.Locale.ROOT));
        }
        context.runOnClient(client -> {
            var board = client.level.getScoreboard();
            board.removeObjective(board.getObjective("hoi_preview"));
        });
    }
}
