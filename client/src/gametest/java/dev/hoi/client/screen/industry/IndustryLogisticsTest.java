package dev.hoi.client.screen.industry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class IndustryLogisticsTest {
    private static final String[] LEVELS = {"연료 우선순위 낮음", "연료 우선순위 중간", "연료 우선순위 높음"};
    private IndustryLogisticsTest() {}
    public static void run(ClientGameTestContext context) {
        var json = new com.google.gson.Gson().toJsonTree(IndustryScreenChecks.bundledFixture("industry-fixture.json")).getAsJsonObject();
        var policy = json.getAsJsonObject("recruitmentPolicy");
        var priorities = policy.getAsJsonObject("priorities"); var ratios = policy.has("equipmentRatios") ? policy.getAsJsonObject("equipmentRatios") : new com.google.gson.JsonObject();
        priorities.addProperty("fuel_army", 1); priorities.addProperty("fuel_navy", 0); priorities.addProperty("fuel_air", 2);
        ratios.addProperty("fuel_army", 1.0); ratios.addProperty("fuel_navy", .25); ratios.addProperty("fuel_air", .5); policy.add("equipmentRatios", ratios);
        var view = new IndustryPayloads.Response(json.toString()).view();
        var requests = new ArrayList<IndustryPayloads.Request>();
        var ref = new AtomicReference<IndustryScreen>();
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> { var screen = new IndustryScreen(MenuTab.LOGISTICS, view.session(), view, requests::add); ref.set(screen); return screen; });
        context.waitTicks(3);
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame();
            check(screen.logisticsListTop() == frame.iy(77) && screen.logisticsRowTop(0) == frame.iy(77), "Equipment rows start at the original list top");
            check(screen.logisticsRowTop(1) - screen.logisticsRowTop(0) == frame.len(58) || Math.abs(screen.logisticsRowTop(1) - screen.logisticsRowTop(0) - frame.size(58)) <= 1, "Equipment rows follow the original 58 px pitch");
            var dots = screen.children().stream().filter(w -> w instanceof Button b && Arrays.asList(LEVELS).contains(b.getMessage().getString())).map(w -> (Button) w).toList();
            check(dots.size() == 9, "Each branch has three fuel priority buttons");
            int top = dots.getFirst().getY();
            check(dots.stream().allMatch(b -> Math.abs(b.getY() - top) <= 1), "Fuel priority buttons share the original row");
            check(Math.abs(top - frame.y(screen.logisticsWindowHeight() - 210 + 58)) <= 1, "Fuel priority buttons sit at the original box offset");
            for (int box = 0; box < 3; box++) for (int level = 0; level < 3; level++) {
                double x = frame.x(20 + 127 * box + 7 + 20 * level); int b = box, l = level;
                check(dots.stream().anyMatch(d -> Math.abs(d.getX() - x) <= 1 && d.getMessage().getString().equals(LEVELS[l])), "Fuel priority button " + b + "/" + l + " at its original x");
            }
            var stored = button(screen, "비축량");
            check(Math.abs(stored.getX() - frame.x(408)) <= 1 && Math.abs(stored.getY() - frame.y(screen.logisticsWindowHeight() - 164)) <= 1, "The stored fuel button sits at its original place");
            check(dots.stream().filter(d -> !d.active).count() == 3, "The selected priority of each branch is not pressable");
            noOverlap(screen);
        });
        context.getInput().setCursorPos(2400, 1300); context.waitTicks(3);
        var shot = context.takeScreenshot("hoi-logistics-gui2");
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame(); double scale = client.getWindow().getGuiScale(); int bottom = screen.logisticsWindowHeight();
            java.awt.image.BufferedImage image;
            try { image = javax.imageio.ImageIO.read(shot.toFile()); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            centred(image, scale, frame.x(60), frame.x(300), frame.y(bottom - 207), frame.y(bottom - 180), frame.y(bottom - 192.5), "The fuel prioritization label sits on its header bar centre");
            centred(image, scale, frame.x(48), frame.x(170), frame.y(50), frame.y(80), frame.y(64.5), "The equipment type label sits on the header bar centre");
        });
        context.runOnClient(client -> {
            var screen = ref.get();
            var high = screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(LEVELS[2]))
                    .map(w -> (Button) w).sorted(Comparator.comparingInt(Button::getX)).toList();
            check(high.size() == 3, "Three high priority buttons");
            check(!high.get(2).active, "Air already has high priority");
            high.get(1).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(!requests.isEmpty() && requests.getLast().action() == IndustryPayloads.Action.ALLOCATION_PRIORITY && requests.getLast().item().equals("fuel_navy") && requests.getLast().amount() == 2, "Navy high priority sends its fuel category");
        });
        var history = new com.google.gson.JsonArray();
        for (int day = 0; day < 31; day++) { var point = new com.google.gson.JsonObject(); point.addProperty("day", 100 + day); point.addProperty("stored", 500 - day * 7 + (day % 5) * 20); point.addProperty("consumed", 10 + day % 7);
            point.addProperty("army", 4 + day % 3); point.addProperty("navy", 3); point.addProperty("air", 3 + day % 7 - day % 3); point.addProperty("produced", 48); history.add(point); }
        json.getAsJsonObject("economy").add("fuelDays", history);
        json.addProperty("revision", view.revision() + 1);
        var withHistory = new IndustryPayloads.Response(json.toString()).view();
        context.runOnClient(client -> ref.get().update(withHistory));
        context.runOnClient(client -> { var screen = ref.get(); button(screen, "비축량").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)); });
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame();
            for (String name : List.of("소모된 양", "일", "월", "년", "×")) button(screen, name);
            check(Math.abs(button(screen, "소모된 양").getX() - frame.x(545 + 196)) <= 1 && Math.abs(button(screen, "일").getY() - frame.y(69)) <= 1, "Fuel window buttons follow the original positions");
            check(screen.fuelSeries().size() == 31 && screen.fuelSeries().getFirst() == 500.0, "The stored fuel series shows the 31 daily samples");
            check(fuelToggles(screen) == 1, "The stored chart lists only the total stockpile line");
            button(screen, "소모된 양").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.fuelSeries().getFirst() == 10.0, "The consumed button switches the series");
            check(fuelToggles(screen) == 6 && screen.fuelSeries("produced").getFirst() == 48.0 && screen.fuelSeries("army").getFirst() == 4.0, "The consumed chart lists production and the four consumption lines");
            check(screen.fuelSeries("other").getFirst() == 0.0, "Other consumption is the unexplained remainder");
            button(screen, "월").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.fuelSeries().isEmpty(), "Months without completed months stay empty");
            button(screen, "일").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        });
        context.getInput().setCursorPos(2400, 1300); context.waitTicks(3);
        context.takeScreenshot("hoi-logistics-fuel-window-gui2");
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame();
            var close = screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("×") && Math.abs(b.getX() - frame.x(545 + 360)) <= 1)
                    .map(w -> (Button) w).findFirst().orElseThrow(() -> new AssertionError("Missing fuel window close button"));
            close.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.children().stream().noneMatch(w -> w instanceof Button b && b.getMessage().getString().equals("소모된 양")), "The close button hides the fuel window");
        });
        ledgerChecks(context, json, view, ref, requests);
        context.getInput().resizeWindow(854, 480); context.waitTicks(3);
        context.runOnClient(client -> { bounds(ref.get()); noOverlap(ref.get()); });
        context.takeScreenshot("hoi-logistics-compact");
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        context.setScreen(() -> null);
    }
    private static int fuelToggles(IndustryScreen screen) {
        return (int) screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().matches("총비축량|총생산|총소비량|육군 소비량|해군 소비량|공군 소비량|기타 소비량")).count();
    }
    private static com.google.gson.JsonObject ledger(String equipment, double need, java.util.function.IntToLongFunction day, int months) {
        var ledger = new com.google.gson.JsonObject(); ledger.addProperty("equipment", equipment); ledger.addProperty("need", need); ledger.addProperty("needDays", 30); ledger.addProperty("day", 1000);
        var days = new com.google.gson.JsonArray(); for (int i = 0; i < 31; i++) days.add(day.applyAsLong(i)); ledger.add("days", days);
        var monthly = new com.google.gson.JsonArray(); for (int i = 0; i < months; i++) monthly.add(day.applyAsLong(i * 3)); ledger.add("months", monthly);
        return ledger;
    }
    private static void ledgerChecks(ClientGameTestContext context, com.google.gson.JsonObject json, IndustryView view, AtomicReference<IndustryScreen> ref, List<IndustryPayloads.Request> requests) {
        for (var element : json.getAsJsonArray("equipment")) { var e = element.getAsJsonObject(); if (e.get("id").getAsString().equals("hoi:infantry_gear")) e.addProperty("acquired", true); }
        for (var element : json.getAsJsonArray("resources")) { var r = element.getAsJsonObject(); if (r.get("id").getAsString().equals("RUBBER")) r.addProperty("demand", 3.0); }
        var ledgers = new com.google.gson.JsonArray();
        ledgers.add(ledger("hoi:truck", .2, i -> 1140 - i * 3L, 14));
        ledgers.add(ledger("hoi:infantry_gear", .5, i -> i < 5 ? 0 : -2290 + i, 3));
        json.getAsJsonObject("economy").add("equipmentLedger", ledgers);
        json.addProperty("revision", view.revision() + 2);
        var next = new IndustryPayloads.Response(json.toString()).view();
        context.runOnClient(client -> ref.get().update(next));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = ref.get();
            var rows = screen.logisticsRows();
            var truck = rows.stream().filter(row -> row.models().stream().anyMatch(m -> m.id().equals("hoi:truck"))).findFirst().orElseThrow();
            var infantry = rows.stream().filter(row -> row.models().stream().anyMatch(m -> m.id().equals("hoi:infantry_gear"))).findFirst().orElseThrow();
            check(truck.daily() == 0 && truck.need() == .2 && IndustryLogisticsRows.signedAmount(truck.balance()).equals("-0.2"), "A consumed-only row shows its daily deficit");
            check(truck.inStock() == 1140 && IndustryLogisticsRows.signedStock(truck.inStock()).equals("+1.1K"), "In stock subtracts outstanding requests from the stockpile");
            check(truck.coverage() == 0 && truck.marker() == IndustryLogisticsRows.BAR, "Without production the bar is grey and the marker sits at the right end");
            check(infantry.coverage() == 1 && infantry.marker() < 0 && infantry.marker() > -IndustryLogisticsRows.SURPLUS_SHIFT, "Stockpiling rows fill green and move the marker left of the bar start");
            check(IndustryLogisticsRows.signedStock(infantry.inStock()).equals("-2.3K") && IndustryLogisticsRows.amount(infantry.daily()).equals("1.2"), "A deficit row shows the outstanding shortfall");
            String needs = IndustryLogisticsTooltips.row(truck, 4, next.lines());
            check(needs.contains("평균 수요량") && needs.contains("사단 보충을 위해 하루 평균") && needs.contains("0.2"), "The needs tooltip follows the official reinforcement text: " + needs);
            check(!IndustryLogisticsTooltips.row(truck, 6, next.lines()).contains("$"), "The in stock tooltip has no raw placeholders");
            button(screen, IndustryLogisticsTooltips.plain("LOGISTICS_NEEDS_SORT_BY")).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.logisticsRows().getFirst().models().stream().anyMatch(m -> m.id().equals("hoi:infantry_gear")), "Sorting by needs puts the largest daily need first");
            button(screen, IndustryLogisticsTooltips.plain("LOGISTICS_NEEDS_SORT_BY")).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.logisticsRows().getLast().models().stream().anyMatch(m -> m.id().equals("hoi:infantry_gear")) || screen.logisticsRows().getLast().need() == 0, "Clicking the same header reverses the order");
            button(screen, truck.name()).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.logisticsInfo().equals(truck.representative().id()), "Clicking an equipment row opens its information window");
            for (String name : List.of("일", "월", "년", "×", IndustryLogisticsTooltips.plain("LOGISTICS_DELETE_EQUIPMENT"))) button(screen, name);
            check(screen.equipmentSeries(truck).size() == 31 && screen.equipmentSeries(truck).getFirst() == 1140.0, "The day chart plots the in stock history");
            button(screen, "월").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(screen.equipmentSeries(truck).size() == 13, "The month chart keeps the last thirteen month ends");
            button(screen, "년").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
            check(!screen.equipmentSeries(truck).isEmpty() && screen.equipmentSeries(truck).size() <= 3, "The year chart keeps one point per year");
            button(screen, "일").onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        });
        context.getInput().setCursorPos(2400, 1300); context.waitTicks(3);
        context.takeScreenshot("hoi-logistics-info-window-gui2");
        context.runOnClient(client -> {
            var screen = ref.get(); var frame = screen.tradeFrame(); double scale = client.getWindow().getGuiScale();
            var delete = button(screen, IndustryLogisticsTooltips.plain("LOGISTICS_DELETE_EQUIPMENT"));
            check(Math.abs(delete.getX() - frame.x(545 + 13 + 322)) <= 1, "The delete button sits at the variant row's right end");
        });
        var variant = context.computeOnClient(client -> { var frame = ref.get().tradeFrame(); double scale = client.getWindow().getGuiScale(); return new double[]{frame.x(545 + 100) * scale, frame.y(325) * scale}; });
        context.getInput().setCursorPos(variant[0], variant[1]); context.waitTicks(3);
        context.takeScreenshot("hoi-logistics-variant-tooltip-gui2");
        context.runOnClient(client -> button(ref.get(), IndustryLogisticsTooltips.plain("LOGISTICS_DELETE_EQUIPMENT")).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0)));
        context.waitTicks(2);
        context.takeScreenshot("hoi-logistics-delete-confirm-gui2");
        context.runOnClient(client -> {
            check(client.gui.screen() instanceof dev.hoi.client.screen.commander.CommanderConfirmation, "Deleting asks for the original confirmation");
            var confirm = (dev.hoi.client.screen.commander.CommanderConfirmation) client.gui.screen();
            check(confirm.bodyText().getString().contains("90"), "The confirmation names the ninety-day seizure window");
            ((Button) confirm.choiceButton("ok")).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        });
        context.waitTicks(2);
        context.runOnClient(client -> {
            check(client.gui.screen() == ref.get(), "Confirming returns to the logistics screen");
            check(!requests.isEmpty() && requests.getLast().action() == IndustryPayloads.Action.DELETE_EQUIPMENT && requests.getLast().item().equals("hoi:truck"), "Confirming sends the delete request for the variant");
            check(requests.stream().noneMatch(r -> r.action() == IndustryPayloads.Action.CLOSE), "The confirmation does not close the industry session");
            var screen = ref.get();
            screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            check(screen.logisticsInfo().isEmpty() && client.gui.screen() == screen, "Escape closes the information window first");
        });
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
    private static Button button(IndustryScreen screen, String name) {
        return (Button) screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(name)).findFirst().orElseThrow(() -> new AssertionError("Missing control: " + name));
    }
    private static void bounds(IndustryScreen screen) {
        for (var w : screen.children()) if (w instanceof Button b)
            check(b.getX() >= 0 && b.getY() >= 0 && b.getRight() <= screen.width && b.getBottom() <= screen.height, "Logistics control inside viewport: " + b.getMessage().getString());
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
