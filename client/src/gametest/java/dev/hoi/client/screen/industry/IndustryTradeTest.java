package dev.hoi.client.screen.industry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class IndustryTradeTest {
    private static final List<String> ORDER = List.of("OIL", "ALUMINUM", "RUBBER", "TUNGSTEN", "IRON", "CHROMIUM", "COAL");
    private IndustryTradeTest() {}
    public static void run(ClientGameTestContext context) {
        var json = new com.google.gson.Gson().toJsonTree(IndustryScreenChecks.bundledFixture("industry-fixture.json")).getAsJsonObject();
        for (var element : json.getAsJsonArray("resources")) if (element.getAsJsonObject().get("id").getAsString().equals("OIL")) {
            element.getAsJsonObject().addProperty("fuelCurrent", 40.0); element.getAsJsonObject().addProperty("fuelMaximum", 300.0);
        }
        int exporters = 0;
        for (var element : json.getAsJsonArray("partners")) {
            var partner = element.getAsJsonObject(); var exports = partner.getAsJsonObject("exports");
            boolean iron = exports != null && exports.has("IRON") && exports.get("IRON").getAsDouble() >= .5;
            partner.addProperty("continent", iron ? exporters++ % 2 == 0 ? "europe" : "asia" : "africa");
            partner.addProperty("faction", iron && exporters % 3 == 0); partner.addProperty("subject", false);
        }
        var view = new IndustryPayloads.Response(json.toString()).view().withAutoImport(new IndustryView.AutoImport(false, 0, 12, ORDER, Map.of(), true));
        var requests = new ArrayList<IndustryPayloads.Request>();
        var ref = new AtomicReference<IndustryScreen>();
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> { var screen = new IndustryScreen(MenuTab.TRADE, view.session(), view, requests::add); ref.set(screen); return screen; });
        context.waitTicks(3);
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame();
            check(!screen.tradeCompact(), "The trade list has room for rows at GUI scale 2");
            at(button(screen, "경제와 무역 화면 간 전환"), frame.ix(70), frame.iy(4), "Economy switch uses the original button position");
            at(button(screen, "자동 수입"), frame.ix(28), frame.iy(89), "Automatic import checkbox");
            at(button(screen, "우선순위"), frame.ix(490), frame.iy(89), "Import priority button");
            for (int i = 0; i < 7; i++) at(button(screen, view.resources().get(i).name()), frame.ix(11 + 85 * i), frame.iy(334), "Resource tab " + i);
            for (int i = 0; i < 7; i++) at(button(screen, view.resources().get(i).name() + " 수입 취소"), frame.ix(138 + 68 * i), frame.iy(303), "Cancel import " + i);
            String[] regions = {"유럽", "북아메리카", "남아메리카", "오세아니아", "아프리카", "아시아", "중동", "세력", "종속국"};
            for (int i = 0; i < regions.length; i++) at(button(screen, regions[i]), frame.ix(107 + 150 * (i % 3)), frame.iy(385 + 35 * (i / 3)), "Region filter " + regions[i]);
            String[] sorts = {"국가", "수출", "운송됨", "수송선"}; int[] sortX = {77, 247, 352, 470};
            for (int i = 0; i < sorts.length; i++) at(button(screen, sorts[i]), frame.ix(sortX[i]), frame.iy(501), "Sort " + sorts[i]);
            var rows = IndustryTradeRows.rows(view, "IRON", Set.of(), "export", false);
            check(!rows.isEmpty(), "The fixture lists steel exporters");
            check(button(screen, rows.getFirst().partner().name() + "에서 수입").getY() == screen.tradeRowTop(0), "The first exporter row starts at the original list top");
            check(rows.stream().allMatch(r -> r.partner().exports().getOrDefault("IRON", 0.0) >= .5), "Only exporters of the selected resource are listed");
            for (int i = 1; i < rows.size(); i++) check(rows.get(i - 1).remaining() >= rows.get(i).remaining(), "Exporters are ordered by available export");
            noOverlap(screen);
        });
        context.getInput().setCursorPos(2400, 1300); context.waitTicks(3);
        var shot = context.takeScreenshot("hoi-trade-gui2");
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame(); var rows = IndustryTradeRows.rows(view, "IRON", Set.of(), "export", false);
            java.awt.image.BufferedImage image;
            try { image = javax.imageio.ImageIO.read(shot.toFile()); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            double scale = client.getWindow().getGuiScale();
            centred(image, scale, frame.x(80), frame.x(290), frame.y(6), frame.y(36), frame.y(21), "The economy title sits on its switch box centre");
            centred(image, scale, frame.x(340), frame.x(545), frame.y(6), frame.y(36), frame.y(21), "The trade title sits on its switch box centre");
            centred(image, scale, frame.x(75), frame.x(215), frame.y(54), frame.y(72), frame.y(63), "The civilian factory label sits on its header box centre");
            centred(image, scale, frame.x(60), frame.x(240), frame.y(84), frame.y(126), frame.y(105), "The automatic import label sits on the row and checkbox centre");
            centred(image, scale, frame.x(493), frame.x(582), frame.y(92), frame.y(118), frame.y(105), "The priority caption sits on the row centre");
            centred(image, scale, frame.x(25), frame.x(100), frame.y(167), frame.y(185), frame.y(176), "The produced label sits on its band centre");
            centred(image, scale, frame.x(110), frame.x(228), frame.y(386), frame.y(417), frame.y(401.5), "The Europe filter caption sits on its box centre");
            centred(image, scale, frame.x(80), frame.x(173), frame.y(503), frame.y(528), frame.y(517), "The country sort caption sits on its button centre");
            int row = screen.tradeRowTop(0);
            centred(image, scale, frame.x(70), frame.x(255), row + frame.size(12), row + frame.size(31), row + frame.size(21.5f), "Exporter names sit on their name box centre");
            var slider = (AbstractSliderButton) screen.children().stream().filter(w -> w instanceof AbstractSliderButton).findFirst().orElseThrow();
            check(Math.abs(slider.getY() + slider.getHeight() / 2.0 - frame.y(105)) <= .75, "The budget slider knob sits on the row centre");
        });
        click(context, "자동 수입");
        context.runOnClient(client -> check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT && last(requests).amount() == 1, "The checkbox enables automatic import"));
        var enabled = revision(view, view.revision() + 2).withAutoImport(new IndustryView.AutoImport(true, 2, 12, ORDER, Map.of(), true));
        context.runOnClient(client -> ref.get().update(enabled));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = ref.get();
            var slider = (AbstractSliderButton) screen.children().stream().filter(w -> w instanceof AbstractSliderButton).findFirst().orElseThrow();
            check(slider.active, "The budget slider works while automatic import is on");
            slider.keyPressed(new KeyEvent(InputConstants.KEY_RIGHT, 0, 0));
            check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT_BUDGET && last(requests).amount() == 3, "The budget slider sends the next factory count");
            check(screen.children().stream().noneMatch(w -> w instanceof Button b && b.getMessage().getString().endsWith("에서 수입") && b.active), "Manual trade proposals are blocked while automatic import is on");
            check(screen.children().stream().noneMatch(w -> w instanceof Button b && b.getMessage().getString().endsWith(" 수입 취소") && b.active), "Manual import cancellation is blocked while automatic import is on");
        });
        var priority = revision(view, view.revision() + 3).withAutoImport(new IndustryView.AutoImport(true, 2, 12, ORDER, Map.of("OIL", 3), true));
        context.runOnClient(client -> ref.get().update(priority));
        click(context, "우선순위");
        context.runOnClient(client -> {
            var screen = ref.get(); var at = screen.autoImportFrame();
            check(screen.autoImportOpen(), "The priority button opens the import priority window");
            check(Math.abs(at.x(0) - screen.tradeFrame().x(768)) < 1, "The priority window opens at its original position beside the trade window");
            var plus = button(screen, view.resources().getFirst().name() + " 허용 부족량 증가");
            check(plus.getX() == at.ix(240), "The accepted shortage increase button keeps the original column");
            plus.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT_SHORTAGE && last(requests).item().equals("OIL") && last(requests).amount() == 4, "The increase button adds one accepted shortage");
        });
        context.runOnClient(client -> ref.get().update(revision(priority, view.revision() + 4)));
        click(context, view.resources().getFirst().name() + " 허용 부족량 감소");
        context.runOnClient(client -> check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT_SHORTAGE && last(requests).amount() == 2, "The decrease button removes one accepted shortage"));
        context.runOnClient(client -> ref.get().update(revision(priority, view.revision() + 5)));
        click(context, "연료 부족 시 석유 우선 수입");
        context.runOnClient(client -> check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT_FUEL && last(requests).amount() == 0, "The fuel checkbox turns fuel priority off"));
        context.runOnClient(client -> ref.get().update(revision(priority, view.revision() + 6)));
        click(context, "초기화");
        context.runOnClient(client -> check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT_RESET, "Reset restores the default priority settings"));
        context.runOnClient(client -> ref.get().update(revision(priority, view.revision() + 7)));
        context.waitTicks(2);
        context.takeScreenshot("hoi-trade-priority-gui2");
        context.runOnClient(client -> {
            var screen = ref.get(); var at = screen.autoImportFrame();
            double shift = (button(screen, view.resources().getFirst().name() + " 허용 부족량 증가").getY() - at.y(171.5)) / at.scale();
            double x = at.x(120), from = at.y(158 + shift + 53 * 6 + 26), to = at.y(158 + shift + 10);
            screen.mouseClicked(mouse(x, from), false);
            screen.mouseDragged(mouse(x, (from + to) / 2), 0, (to - from) / 2);
            screen.mouseDragged(mouse(x, to), 0, (to - from) / 2);
            screen.mouseReleased(mouse(x, to));
            check(last(requests).action() == IndustryPayloads.Action.AUTO_IMPORT_ORDER && last(requests).item().equals("COAL") && last(requests).amount() == 0, "Dragging the last resource to the top reorders the import priority");
        });
        context.runOnClient(client -> { ref.get().update(revision(priority, view.revision() + 8)); ref.get().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)); });
        context.runOnClient(client -> check(!ref.get().autoImportOpen() && client.gui.screen() == ref.get(), "Escape closes the priority window before the trade screen"));
        context.runOnClient(client -> ref.get().update(revision(view, view.revision() + 9)));
        click(context, "유럽");
        context.runOnClient(client -> {
            var screen = ref.get(); var expected = IndustryTradeRows.rows(view, "IRON", Set.of("europe"), "export", false);
            var shown = rowNames(screen);
            check(!shown.isEmpty() && shown.equals(expected.stream().limit(shown.size()).map(r -> r.partner().name()).toList()), "The Europe filter lists only European exporters");
        });
        click(context, "유럽");
        click(context, "국가");
        context.runOnClient(client -> {
            var shown = rowNames(ref.get()); var expected = IndustryTradeRows.rows(view, "IRON", Set.of(), "country", false);
            check(shown.equals(expected.stream().limit(shown.size()).map(r -> r.partner().name()).toList()), "The country sort orders exporters by name");
        });
        click(context, "국가");
        context.runOnClient(client -> {
            var shown = rowNames(ref.get()); var expected = IndustryTradeRows.rows(view, "IRON", Set.of(), "country", true);
            check(shown.equals(expected.stream().limit(shown.size()).map(r -> r.partner().name()).toList()), "A second click reverses the sort");
        });
        var imported = view.trades().stream().filter(IndustryView.Trade::importing).findFirst().orElseThrow();
        var importedName = view.resources().stream().filter(r -> r.id().equals(imported.resource())).findFirst().orElseThrow().name();
        click(context, importedName + " 수입 취소");
        context.runOnClient(client -> check(last(requests).action() == IndustryPayloads.Action.CANCEL_IMPORTS && last(requests).item().equals(imported.resource()), "The cancel button cancels every import of its resource"));
        context.runOnClient(client -> ref.get().update(revision(view, view.revision() + 10)));
        context.runOnClient(client -> {
            var screen = ref.get(); int before = screen.tradeRowTop(0);
            boolean scrollable = screen.tradeFrame().len(45 * IndustryTradeRows.rows(view, "IRON", Set.of(), "country", true).size()) > screen.tradeListBottom() - screen.tradeListTop();
            screen.mouseScrolled(screen.tradeFrame().x(300), screen.tradeRowTop(1), 0, -1);
            check(scrollable ? screen.tradeRowTop(0) < before : screen.tradeRowTop(0) == before, "The mouse wheel scrolls the exporter list only when it overflows");
        });
        var economyPanel = economyFixture();
        int[] mark = {requests.size()};
        click(context, "경제와 무역 화면 간 전환");
        context.runOnClient(client -> check(ref.get().children().stream().noneMatch(w -> w instanceof Button b && b.getMessage().getString().equals("유럽")), "The switch opens the economy view"));
        context.runOnClient(client -> ref.get().update(revision(view, view.revision() + 11)));
        context.waitTicks(2);
        context.runOnClient(client -> check(sent(requests, mark[0], IndustryPayloads.Action.ECONOMY, "", 1), "Opening the economy view subscribes to the original panel"));
        context.runOnClient(client -> ref.get().update(revision(view, view.revision() + 12).withEconomyPanel(economyPanel)));
        context.waitTicks(3);
        var loanCentre = new int[2];
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame();
            var loan = button(screen, "gdp_1b_loan_button"); var payment = button(screen, "gdp_1b_debt_payment_button");
            check(loan.active && !payment.active, "Loan and repayment buttons follow their source enabled state");
            check(loan.getX() >= frame.ix(300) && loan.getY() >= screen.economyTop() && payment.getY() > loan.getY(), "Loan buttons sit in the right column below the economy header");
            for (String name : List.of("auto_payment_checkbox", "bank_checkbox", "Economic_Civilian_Investments_Button", "Economic_Military_Investments_Button")) button(screen, name);
            bounds(screen); noOverlap(screen);
            loanCentre[0] = (int) Math.round((loan.getX() + loan.getWidth() / 2.0) * client.getWindow().getGuiScale());
            loanCentre[1] = (int) Math.round((loan.getY() + loan.getHeight() / 2.0) * client.getWindow().getGuiScale());
        });
        context.getInput().setCursorPos(loanCentre[0], loanCentre[1]); context.waitTicks(3);
        var economyShot = context.takeScreenshot("hoi-trade-economy-gui2");
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame();
            java.awt.image.BufferedImage image;
            try { image = javax.imageio.ImageIO.read(economyShot.toFile()); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            double scale = client.getWindow().getGuiScale(); int bright = 0;
            for (int y = (int) (frame.y(55) * scale); y < frame.y(100) * scale; y++) for (int x = (int) (frame.x(20) * scale); x < frame.x(250) * scale; x++) {
                int rgb = image.getRGB(x, y); if (Math.min(rgb >> 16 & 255, Math.min(rgb >> 8 & 255, rgb & 255)) > 175) bright++;
            }
            check(bright > 20, "The GDP frame draws its source text");
        });
        context.runOnClient(client -> mark[0] = requests.size());
        click(context, "gdp_1b_loan_button");
        context.runOnClient(client -> check(sent(requests, mark[0], IndustryPayloads.Action.ECONOMY_CLICK, "gdp_1b_loan_button", 0), "A loan button sends its source element"));
        context.runOnClient(client -> ref.get().update(revision(view, view.revision() + 13).withEconomyPanel(economyPanel)));
        context.runOnClient(client -> mark[0] = requests.size());
        click(context, "경제와 무역 화면 간 전환");
        context.runOnClient(client -> ref.get().update(revision(view, view.revision() + 14)));
        context.waitTicks(2);
        context.runOnClient(client -> check(sent(requests, mark[0], IndustryPayloads.Action.ECONOMY, "", 0), "Leaving the economy view ends the panel subscription"));
        context.getInput().resizeWindow(854, 480); context.waitTicks(3);
        context.runOnClient(client -> {
            var screen = ref.get();
            check(screen.tradeCompact(), "Short windows scroll the whole trade body");
            bounds(screen); noOverlap(screen);
            screen.mouseScrolled(20, screen.height - 20, 0, -100);
            bounds(screen); noOverlap(screen);
        });
        context.takeScreenshot("hoi-trade-compact");
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        context.setScreen(() -> null);
    }
    private static void centred(java.awt.image.BufferedImage image, double scale, float left, float right, float top, float bottom, float expected, String message) {
        int first = Integer.MAX_VALUE, last = -1;
        for (int y = (int) Math.floor(top * scale); y < Math.ceil(bottom * scale); y++) for (int x = (int) Math.floor(left * scale); x < Math.ceil(right * scale); x++) {
            int rgb = image.getRGB(x, y), r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            if (Math.min(r, Math.min(g, b)) > 175) { first = Math.min(first, y); last = Math.max(last, y); }
        }
        check(last >= 0, message + " (text drawn)");
        double centre = (first + last + 1) / 2.0 / scale;
        check(Math.abs(centre - expected) <= .75, message + ": centre " + centre + " expected " + expected);
    }
    private static boolean sent(List<IndustryPayloads.Request> requests, int from, IndustryPayloads.Action action, String item, int amount) {
        return requests.subList(from, requests.size()).stream().anyMatch(r -> r.action() == action && r.item().equals(item) && r.amount() == amount);
    }
    private static dev.hoi.protocol.decision.DecisionPanelView economyFixture() {
        String directory = System.getenv("HOI_DECISION_PREVIEW");
        check(directory != null && !directory.isBlank(), "HOI_DECISION_PREVIEW is required for the server-exported economy panel");
        try {
            var root = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(directory).resolve("decision-economy-panel.json"))).getAsJsonObject();
            return new com.google.gson.Gson().fromJson(root.get("panel"), dev.hoi.protocol.decision.DecisionPanelView.class);
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    private static IndustryView revision(IndustryView view, long revision) {
        var json = new com.google.gson.Gson().toJsonTree(view).getAsJsonObject();
        json.addProperty("revision", revision);
        return new IndustryPayloads.Response(json.toString()).view();
    }
    private static List<String> rowNames(IndustryScreen screen) {
        return screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().endsWith("에서 수입")).map(w -> (Button) w)
                .sorted(Comparator.comparingInt(Button::getY)).map(b -> b.getMessage().getString().replace("에서 수입", "")).toList();
    }
    private static MouseButtonEvent mouse(double x, double y) { return new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)); }
    private static IndustryPayloads.Request last(List<IndustryPayloads.Request> requests) {
        if (requests.isEmpty()) throw new AssertionError("No request was sent");
        return requests.getLast();
    }
    private static void click(ClientGameTestContext context, String name) {
        context.runOnClient(client -> { var b = button((IndustryScreen) client.gui.screen(), name); check(b.active, "Enabled control: " + name); b.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)); });
    }
    private static Button button(IndustryScreen screen, String name) {
        return (Button) screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow(() -> new AssertionError("Missing control: " + name));
    }
    private static void at(Button button, int x, int y, String message) {
        check(button.getX() == x && button.getY() == y, message + " at " + x + "," + y + " but was " + button.getX() + "," + button.getY());
    }
    private static void bounds(IndustryScreen screen) {
        for (var w : screen.children()) if (w instanceof Button b)
            check(b.getX() >= 0 && b.getY() >= 0 && b.getRight() <= screen.width && b.getBottom() <= screen.height, "Trade control inside viewport: " + b.getMessage().getString());
    }
    private static void noOverlap(IndustryScreen screen) {
        var buttons = screen.children().stream().filter(w -> w instanceof Button).map(w -> (Button) w).toList();
        for (int i = 0; i < buttons.size(); i++) for (int j = i + 1; j < buttons.size(); j++) {
            var a = buttons.get(i); var b = buttons.get(j);
            check(a.getRight() <= b.getX() || b.getRight() <= a.getX() || a.getBottom() <= b.getY() || b.getBottom() <= a.getY(), "Controls must not overlap: " + a.getMessage().getString() + " / " + b.getMessage().getString());
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
