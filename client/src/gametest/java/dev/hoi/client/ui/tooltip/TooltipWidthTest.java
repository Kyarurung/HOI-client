package dev.hoi.client.ui.tooltip;

import dev.hoi.client.screen.decision.DecisionText;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.politics.PoliticsTooltip;
import dev.hoi.protocol.menu.MenuView;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TooltipWidthTest extends Screen {
    private static final String LONG = "§e가로 폭과 줄바꿈 검증§r\n\n긴 문장의 원문과 §a+5.00%§r 값, 색상, 빈 줄을 유지하며 상자의 오른쪽 경계를 넘어가지 않는지 확인합니다. ".repeat(4);
    private final String mode;
    private final DecisionText decision = new DecisionText();
    private Tooltips.ScrolledTooltip bounds;
    private TooltipWidthTest(String mode) { super(Component.literal("Synthetic tooltip width regression")); this.mode = mode; }
    @Override protected void init() {
        if (mode.startsWith("widget")) {
            var button = new UiMenuButton("Tooltip", width - 180, height - 80, 160, 60, () -> {});
            button.setTooltip(Tooltips.createExact(mode.equals("widget-short") ? "§e연구§r" : LONG));
            addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0xFF24212A);
        switch (mode) {
            case "scroll" -> bounds = Tooltips.drawSourceScrollable(g, font, LONG.repeat(12), mx, my, width, height, 30);
            case "decision" -> bounds = decision.tooltip(g, font, LONG, mx, my, width, height, 0);
            case "politics" -> PoliticsTooltip.drawCountryDetail(g, font, new MenuView.Entry("", "", LONG, -1, ""), "KOR", mx, my, width, height, 0);
            case "image" -> PoliticsTooltip.drawTextDetail(g, font, "[image:country/kor/flag]\n\n" + LONG, mx, my, width, height, 0);
            default -> super.extractRenderState(g, mx, my, delta);
        }
    }
    private static void balancedTail(net.minecraft.client.gui.Font font) {
        var text = Component.literal("공수 작전 준비 완료까지 앞으로 남은 날짜를 표시합니다 요");boolean changed = false;
        for (int step = 60; step <= 200; step += 3) {
            int width = step;var plain = font.split(text, width);var balanced = Tooltips.balanced(font, text, width);
            if (balanced.size() != plain.size() || balanced.stream().anyMatch(line -> font.width(line) > width)) throw new AssertionError("Balanced wrapping keeps the line count and width: " + width);
            if (font.width(balanced.getLast()) < font.width(plain.getLast())) throw new AssertionError("Balanced wrapping never shortens the last line: " + width);
            changed |= font.width(balanced.getLast()) > font.width(plain.getLast());
        }
        if (!changed) throw new AssertionError("A short last line must take words from the line above");
    }
    public static void run(ClientGameTestContext context) {
        var scale = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int width : new int[]{2560, 1280}) {
                context.getInput().resizeWindow(width, width * 9 / 16);
                context.runOnClient(client -> client.options.guiScale().set(2));
                for (String mode : List.of("widget-long", "widget-short", "scroll", "decision", "politics", "image")) {
                    context.setScreen(() -> new TooltipWidthTest(mode));
                    context.getInput().setCursorPos(width - 80, width * 9 / 16 - 80); context.waitTicks(15);
                    context.runOnClient(client -> {
                        var screen = (TooltipWidthTest)client.gui.screen();
                        if (screen.bounds != null) {
                            var b = screen.bounds;
                            if (b.width() + 8 > Tooltips.MAX_WIDTH || b.x() < 4 || b.y() < 4 || b.x() + b.width() + 4 > screen.width || b.y() + b.height() + 4 > screen.height)
                                throw new AssertionError("Tooltip exceeds source width or viewport: " + mode + " " + b);
                            if (mode.equals("scroll") && (b.maxScroll() <= 0 || b.scroll() != Math.min(30, b.maxScroll()))) throw new AssertionError("Width wrapping lost scrolling: " + b);
                        }
                        var nativeLines = Tooltip.splitTooltip(client, Tooltips.officerText(LONG));
                        if (nativeLines.stream().anyMatch(line -> client.font.width(line) + 14 > Tooltips.MAX_WIDTH)) throw new AssertionError("Native widget tooltip bypasses common width");
                        if (Tooltips.tooltipLines(client.font, "타이중 - 3개 공수사단 - 9 군 - 수송기 50기", screen.width).size() != 1) throw new AssertionError("A one-line label must not leave its tail on a second line");
                        balancedTail(client.font);
                        if (Tooltips.tooltipLines(client.font, "§e연구§r", screen.width).stream().mapToInt(client.font::width).max().orElseThrow() != client.font.width("연구"))
                            throw new AssertionError("Short tooltip lost content fitting");
                    });
                    context.takeScreenshot("hoi-tooltip-width-" + mode + "-synthetic-" + width + "-gui2");
                }
            }
        } finally { context.setScreen(() -> null); context.runOnClient(client -> client.options.guiScale().set(scale)); }
    }
}
