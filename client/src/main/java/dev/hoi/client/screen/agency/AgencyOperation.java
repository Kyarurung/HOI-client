package dev.hoi.client.screen.agency;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.agency.AgencyView;
import dev.hoi.client.ui.country.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class AgencyOperation extends Screen {
    private final AgencyScreen parent;
    private AgencyView.Item item;
    private final Consumer<List<String>> submit;
    private final List<String> arguments = new ArrayList<>();
    private int x, y, w, h, scroll, contentHeight, rewardScroll, rewardHeight, choosing = -1, choiceScroll;
    private boolean scrolling, rewardScrolling, dragging, submitted, positioned;
    AgencyOperation(AgencyScreen parent, AgencyView.Item item, Consumer<List<String>> submit) {
        super(Component.literal(item.title()));
        this.parent = parent;
        this.item = item;
        this.submit = submit;
        normalizeArguments();
    }
    private void normalizeArguments() {
        while (arguments.size() > item.parameters().size()) arguments.removeLast();
        for (int i = 0; i < item.parameters().size(); i++) {
            var choices = item.parameters().get(i).choices();
            if (arguments.size() <= i) arguments.add("");
            String selected = arguments.get(i);
            if (choices.stream().noneMatch(choice -> choice.value().equals(selected))) {
                arguments.set(i, i == startParameter() ? "manual" : i == 0
                        ? choices.stream().map(AgencyView.Choice::value).findFirst().orElse("") : "");
            }
        }
    }
    void update(AgencyView view) {
        var next = view.items().stream().filter(value -> value.id().equals(item.id())).findFirst().orElse(null);
        if (next == null) { minecraft.gui.setScreen(parent); return; }
        item = next;
        submitted = false;
        choosing = -1;
        normalizeArguments();
        rebuildWidgets();
    }
    public AgencyScreen backdrop() { return parent; }
    boolean updatePicker(AgencyView view, int parameter) {
        var next = view.items().stream().filter(value -> value.id().equals(item.id())).findFirst().orElse(null);
        if (next == null || parameter >= next.parameters().size()) return false;
        item = next; submitted = false; normalizeArguments(); rebuildWidgets(); return true;
    }
    List<AgencyView.Candidate> candidates(int parameter) {
        return item.parameters().get(parameter).choices().stream().map(choice -> parent.snapshot().operatives().stream()
                .filter(operative -> operative.id().equals(choice.value())).findFirst().map(operative -> new AgencyView.Candidate(
                        operative.id(), operative.name(), operative.portrait(), operative.level(), operative.rank(), getTitle().getString(), operative.traits())).orElse(null))
                .filter(Objects::nonNull).toList();
    }
    boolean candidateEnabled(int parameter, String id) {
        return !submitted && !parent.awaitingResponse() && item.enabled() && item.parameters().get(parameter).choices().stream().anyMatch(choice -> choice.value().equals(id))
                && java.util.stream.IntStream.range(1, operativeEnd()).noneMatch(index -> index != parameter && arguments.get(index).equals(id));
    }
    void closePicker() { choosing = -1; rebuildWidgets(); }
    private int startParameter() {
        if (item.parameters().isEmpty()) return -1;
        int index = item.parameters().size() - 1;
        var choices = item.parameters().get(index).choices();
        return choices.stream().anyMatch(choice -> choice.value().equals("manual"))
                && choices.stream().allMatch(choice -> choice.value().matches("(?:manual|auto_commence|auto_repeat|auto_commence\\+auto_repeat)(?:\\+resume[1-3])*")) ? index : -1;
    }
    boolean resumes(int parameter) {
        int index = startParameter();
        return index >= 0 && List.of(arguments.get(index).split("\\+")).contains("resume" + parameter);
    }
    boolean canResume(int parameter) {
        int index = startParameter();
        return index >= 0 && parameter > 0 && parameter < index && item.parameters().get(index).choices().stream()
                .anyMatch(choice -> List.of(choice.value().split("\\+")).contains("resume" + parameter));
    }
    void resume(int parameter, boolean enabled) {
        int index = startParameter();
        if (submitted || parent.awaitingResponse() || !item.enabled() || !canResume(parameter)) return;
        var parts = new ArrayList<>(List.of(arguments.get(index).split("\\+")));
        parts.remove("resume" + parameter);
        if (enabled) parts.add("resume" + parameter);
        String value = String.join("+", parts.stream().filter(part -> !part.startsWith("resume")).toList());
        for (int slot = 1; slot < index; slot++) if (parts.contains("resume" + slot)) value += "+resume" + slot;
        String next = value;
        if (item.parameters().get(index).choices().stream().anyMatch(choice -> choice.value().equals(next))) arguments.set(index, next);
    }
    private int operativeEnd() { int start = startParameter(); return start < 0 ? arguments.size() : start; }
    private AgencyView.Operation operation() {
        return parent.snapshot().operations().stream().filter(value -> value.item().equals(item.id())).findFirst().orElse(null);
    }
    private AgencyView.OperationOffer offer() {
        return parent.snapshot().offers().stream().filter(value -> value.item().equals(item.id())).findFirst().orElse(null);
    }
    static String sprite(String name) {
        return name.isEmpty() ? "" : "agency/operations/" + name.toLowerCase(Locale.ROOT) + "/frame_0";
    }
    static String texture(String path) {
        return path.startsWith("agency/operations/") && !path.contains("/frame_") && !path.endsWith("/atlas") ? path + "/frame_0" : path;
    }
    static void token(GuiGraphicsExtractor g, AgencyView.Token token, float x, float y) {
        String name = switch (token.icon()) {
            case "GFX_infiltrate_army_bg" -> "infiltrate_army";
            case "GFX_infiltrate_navy_bg" -> "infiltrate_navy";
            case "GFX_infiltrate_air_bg" -> "infiltrate_air";
            case "GFX_infiltrate_civilian_bg" -> "infiltrate_civilian";
            case "GFX_contact_resistance_bg" -> "contact_resistance";
            default -> "";
        };
        if (name.isEmpty()) return;
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(.5f);
        if (token.required()) UiAssets.draw(g, sprite(token.icon()), 0, 0, 33, 29);
        else UiAssets.draw(g, "keywords/text_" + name + "_token", 0, 0, name.equals("infiltrate_air") ? 24 : 19, 21);
        g.pose().popMatrix();
    }
    private String country() {
        var operation = operation();
        var offer = offer();
        return operation != null ? operation.country() : offer != null ? offer.country() : arguments.isEmpty() ? "" : arguments.getFirst();
    }
    private boolean ready() {
        var offer = offer();
        return !submitted && item.enabled() && (offer == null || offer.enabled()) && arguments.stream().noneMatch(String::isEmpty)
                && (operativeEnd() < 2 || arguments.subList(1, operativeEnd()).stream().distinct().count() == operativeEnd() - 1);
    }
    @Override public void tick() { parent.tick(); }
    @Override protected void init() {
        w = Math.min(390, width - 16);
        h = Math.min(280, height - HudMenuBar.height(width) - 12);
        if (!positioned) { x = (width - w) / 2; y = Math.max(HudMenuBar.height(width) + 4, (height - h) / 2); positioned = true; }
        x = Math.clamp(x, 0, Math.max(0, width - w));
        y = Math.clamp(y, HudMenuBar.height(width), Math.max(HudMenuBar.height(width), height - h));
        addRenderableWidget(new UiMenuButton("×", x + w - 21, y + 2, 18, 18, this::onClose).caption("").background("menu/close").sound("ui.close"));
        for (int i = 0; i < item.parameters().size(); i++) {
            if (i == startParameter()) continue;
            int index = i;
            var parameter = item.parameters().get(i);
            if (parameter.choices().size() == 1 && i == 0) continue;
            String selected = parameter.choices().stream().filter(choice -> choice.value().equals(arguments.get(index)))
                    .map(AgencyView.Choice::label).findFirst().orElse("");
            var button = new UiPanelButton(parameter.label() + ": " + selected,
                    x + 11 + Math.max(0, i - 1) * 30, y + 72, i == 0 ? 115 : 27, i == 0 ? 23 : 37, () -> {
                choosing = choosing == index ? -1 : index; choiceScroll = 0; rebuildWidgets();
                if (index > 0 && choosing == index) minecraft.gui.setScreen(new AgencyCandidates(parent, parent.snapshot(), id -> {
                    if (!candidateEnabled(index, id)) return;
                    arguments.set(index, id); closePicker(); minecraft.gui.setScreen(this);
                }, this, index));
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    if (index == 0) { super.extractContents(g, mx, my, delta); return; }
                    portrait(g, arguments.get(index), getX(), getY());
                }
            };
            button.active = !submitted && !parameter.choices().isEmpty();
            button.setTooltip(Tooltips.create(Component.literal(parameter.label() + "\n" + selected)));
            addRenderableWidget(button);
        }
        if (choosing == 0) {
            var choices = item.parameters().get(choosing).choices();
            int visible = Math.max(1, (h - 65) / 42);
            choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, choices.size() - visible));
            for (int i = choiceScroll; i < Math.min(choices.size(), choiceScroll + visible); i++) {
                var choice = choices.get(i);
                var button = new UiPanelButton(choice.label(), right() + 2, y + 28 + (i - choiceScroll) * 42, area() - 10, 40, () -> {
                    arguments.set(choosing, choice.value()); choosing = -1; rebuildWidgets();
                }) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        UiAssets.draw(g, "commander/ui/army", getX(), getY(), getWidth(), getHeight());
                        if (choosing > 0) portrait(g, choice.value(), getX() + 2, getY() + 1);
                        UiText.left(g, font, choice.label(), getX() + 33, getY() + 2, getWidth() - 36, 36, active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED);
                    }
                };
                button.active = !submitted && (choosing == 0 || java.util.stream.IntStream.range(1, operativeEnd())
                        .noneMatch(index -> index != choosing && arguments.get(index).equals(choice.value())));
                addRenderableWidget(button);
            }
        }
        var operation = operation();
        if (operation != null) {
            checkbox("auto_commence:" + operation.id(), operation.autoCommence(), x + 115);
            checkbox("auto_repeat:" + operation.id(), operation.autoRepeat(), x + 272);
            if (operation.stage().equals("COMPLETED")) {
                var close = new UiMenuButton(UiCommandLabels.text("CLOSE", "닫기"), x + 134, y + h - 24, 111, 18, () -> invoke(item.id()))
                        .background(sprite("GFX_button_221x34_anim")).sound("ui.close");
                close.active = !submitted && !parent.awaitingResponse() && item.enabled();
                close.setTooltip(Tooltips.create(Component.literal(item.detail())));
                addRenderableWidget(close);
                var cancel = action("cancel_operation:" + operation.id());
                if (cancel != null) {
                    var control = new UiMenuButton(cancel.button(), x + 251, y + h - 22, 14, 14, () -> invoke(cancel.id()))
                            .caption("").background(sprite("GFX_operation_refund_operation"));
                    control.active = !submitted && !parent.awaitingResponse() && cancel.enabled();
                    control.setTooltip(Tooltips.create(Component.literal(cancel.detail())));
                    addRenderableWidget(control);
                }
                return;
            }
            var commence = action("commence_operation:" + operation.id());
            String caption = operation.stage().equals("RUNNING") ? UiCommandLabels.text("OPERATION_VIEW_PROGRESS", "작전 중...")
                    : operation.stage().equals("COLLECTING") ? UiCommandLabels.text("OPERATION_VIEW_COLLECTING", "준비 중...")
                    : commence == null ? UiCommandLabels.text("PREPARED", "준비됨") : commence.button();
            var start = new UiMenuButton(caption, x + 134, y + h - 24, 111, 18, () -> invoke("commence_operation:" + operation.id()))
                    .background(sprite("GFX_button_221x34_anim")).sound("ui.close");
            start.active = !submitted && !parent.awaitingResponse() && commence != null && commence.enabled();
            if (commence != null) start.setTooltip(Tooltips.create(Component.literal(commence.detail())));
            addRenderableWidget(start);
            var cancel = new UiMenuButton(item.button(), x + 251, y + h - 22, 14, 14, () -> invoke(item.id()))
                    .caption("").background(sprite("GFX_operation_refund_operation")).sound("ui.close");
            cancel.active = !submitted && !parent.awaitingResponse() && item.enabled();
            cancel.setTooltip(Tooltips.create(Component.literal(item.detail())));
            addRenderableWidget(cancel);
            return;
        }
        if (startParameter() >= 0) {
            draftCheckbox("auto_commence", x + 115);
            draftCheckbox("auto_repeat", x + 272);
        }
        var action = new UiMenuButton("준비하기", x + 134, y + h - 24, 111, 18, () -> {
            if (!ready()) return;
            submitted = true;
            minecraft.gui.setScreen(parent);
            submit.accept(List.copyOf(arguments));
        }).background(sprite("GFX_button_221x34_anim")).sound("ui.close");
        action.active = choosing < 0 && ready();
        action.setTooltip(Tooltips.create(Component.literal(item.detail())));
        addRenderableWidget(action);
    }
    private AgencyView.Item action(String id) {
        return parent.snapshot().items().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
    }
    private void invoke(String id) {
        var action = action(id);
        if (submitted || parent.awaitingResponse() || action == null || !action.enabled()) return;
        submitted = true;
        parent.operationAction(id);
        rebuildWidgets();
    }
    private void checkbox(String id, boolean checked, int bx) {
        var action = action(id);
        if (action == null) return;
        checkbox(action.title(), action.detail(), checked, bx, action.enabled(), () -> invoke(id));
    }
    private String draftLabel(String flag) {
        return item.parameters().get(startParameter()).choices().stream().filter(choice -> choice.value().equals(flag))
                .map(AgencyView.Choice::label).findFirst().orElse("");
    }
    private void draftCheckbox(String flag, int bx) {
        int index = startParameter();
        checkbox(draftLabel(flag), draftLabel(flag), arguments.get(index).contains(flag), bx, item.enabled(), () -> {
            if (submitted || parent.awaitingResponse()) return;
            boolean commence = arguments.get(index).contains("auto_commence") ^ flag.equals("auto_commence");
            boolean repeat = arguments.get(index).contains("auto_repeat") ^ flag.equals("auto_repeat");
            String value = commence ? repeat ? "auto_commence+auto_repeat" : "auto_commence" : repeat ? "auto_repeat" : "manual";
            for (int slot = 1; slot < index; slot++) if (resumes(slot)) value += "+resume" + slot;
            String next = value;
            if (item.parameters().get(index).choices().stream().anyMatch(choice -> choice.value().equals(next))) arguments.set(index, next);
            rebuildWidgets();
        });
    }
    private void checkbox(String title, String detail, boolean checked, int bx, boolean enabled, Runnable press) {
        var button = new UiPanelButton(title, bx, y + h - 22, 13, 13, press) {
            @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                UiAssets.drawLeaderControl(g, "agency/operations/gfx_generic_checkbox3/frame_" + (checked ? 1 : 0), getX(), getY(), 13, 13, active);
            }
        };
        button.active = !submitted && !parent.awaitingResponse() && enabled;
        button.sound("ui.decision.track");
        button.setTooltip(Tooltips.create(Component.literal(detail)));
        addRenderableWidget(button);
    }
    private int right() { return x + 135; }
    private void portrait(GuiGraphicsExtractor g, String id, int px, int py) {
        var operative = parent.snapshot().operatives().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        String path = operative == null ? "" : operative.portrait().replace('\\', '/').replaceAll("\\.[^.]+$", "").toLowerCase(Locale.ROOT);
        String image = path.isEmpty() ? sprite("GFX_operative_unknown") : "agency/operative/" + (path.contains("/kor/") ? "korea/" : "tfr/") + path;
        UiAssets.draw(g, image, px + 2, py + 3, 24, 33);
        UiAssets.draw(g, sprite("GFX_unit_leader_portrait_frame_badge"), px, py, 27, 37);
        if (id.isEmpty()) UiAssets.draw(g, sprite("GFX_group_add_operative_anim"), px + 1, py + 2, 26, 35);
        if (operative != null) {
            if (operation() != null && operation().resume().contains(id)) UiAssets.draw(g, sprite("GFX_operative_resume_mission_icon_small"), px + 7, py + 25, 14, 14);
            int frame = dev.hoi.client.map.agency.AgencyHud.missionFrame(parent.snapshot(), operative);
            if (frame >= 0) {
                UiAssets.draw(g, sprite("GFX_mapicon_operative_mission_background"), px + 18, py + 27, 12, 12);
                UiAssets.draw(g, "agency/operations/gfx_operative_mission_icons_small/frame_" + frame, px + 19, py + 28, 10, 10);
            }
        }
    }
    private int area() { return w - 144; }
    private UiScrollBar bar() { return new UiScrollBar(x + w - 12, y + 25, y + h - 61, h - 86, contentHeight); }
    private UiScrollBar rewardBar() { return new UiScrollBar(x + w - 12, y + h - 57, y + h - 29, 28, rewardHeight); }
    private int text(GuiGraphicsExtractor g, String value, int tx, int ty, int width) {
        var lines = Tooltips.split(font, TooltipKeywordIcons.decorateLines(Tooltips.officerText(value.replaceAll("\\bGFX_(text_infiltrate_(?:civilian|army|navy|air)_token)\\b", "£$1£")), false), Math.max(1, width));
        for (var line : lines) {
            g.text(font, line, tx, ty, UiMenuStyle.TEXT, false); ty += 10;
        }
        return ty;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        parent.extractRenderState(g, -1, -1, delta);
        g.nextStratum();
        UiAssets.nineSlice(g, sprite("GFX_tiled_window"), x, y, w, h, 6, .5);
        UiAssets.nineSlice(g, sprite("GFX_tiled_header"), x + 2, y + 2, w - 4, 20, 6, .5);
        g.pose().pushMatrix(); g.pose().translate(x + 23, y + 3); g.pose().scale(1.5f);
        g.text(font, item.title(), 0, 0, UiMenuStyle.TEXT, false); g.pose().popMatrix();
        g.pose().pushMatrix(); g.pose().translate(x + 7.5f, y + 23); g.pose().scale(.5f);
        UiAssets.draw(g, sprite("GFX_container_target_info_bg"), 0, 0, 254, 94);
        UiAssets.draw(g, texture(item.texture()), 10, 3, 85, 85);
        g.pose().popMatrix();
        dev.hoi.client.ui.country.CountryFlags.draw(g,"country/" + country().toLowerCase(Locale.ROOT) + "/flag","small2", x + 57, y + 28, 23, 16);
        String countryName = parent.snapshot().items().stream().flatMap(value -> value.parameters().stream())
                .flatMap(parameter -> parameter.choices().stream()).filter(choice -> choice.value().equals(country()))
                .map(AgencyView.Choice::label).findFirst().orElse(CountryDisplays.name(country(), country()));
        text(g, countryName, x + 57, y + 46, 70);
        int clipboardX = right() - 3, clipboardRight = clipboardX + area() + 5;
        int clipboardTop = y + 23, clipboardBottom = y + h - 60;
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + sprite("GFX_operations_clipboard_top_bg") + ".png"),
                clipboardX, clipboardTop, clipboardRight, clipboardTop + 43, 0, 1, 43f / 129, 1);
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + sprite("GFX_operations_clipboard_mid_bg") + ".png"),
                clipboardX, clipboardTop + 43, clipboardRight, clipboardBottom - 96, 0, 1, 0, 1);
        g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + sprite("GFX_operations_clipboard_bottom_bg") + ".png"),
                clipboardX, clipboardBottom - 96, clipboardRight, clipboardBottom, 0, 1, 0, 192f / 209);
        UiAssets.nineSlice(g, "agency/ui/operation_bottom", x + 5, y + h - 25, w - 10, 23, 4, .5);
        var operation = operation();
        var offer = offer();
        boolean completed = operation != null && operation.stage().equals("COMPLETED");
        int required = operation != null ? operation.required() : offer != null ? offer.required() : 0;
        if (operation != null) {
            for (int i = 0; i < operation.operatives().size(); i++) {
                String id = operation.operatives().get(i);
                var operative = parent.snapshot().operatives().stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
                if (operative == null) continue;
                int px = x + 11 + i % 4 * 30, py = y + 72 + i / 4 * 38;
                portrait(g, id, px, py);
                if (operation.resume().contains(id) && mx >= px + 7 && mx < px + 21 && my >= py + 25 && my < py + 39)
                    g.setTooltipForNextFrame(font, Tooltips.tooltipLines(font, UiCommandLabels.text("OPERATIVE_RESUME_MISSION_AFTER_OPERATION_INDICATOR_TOOLTIP", "이 요원은 작전이 시작했을 때 배정받은 임무를 재개할 것입니다."), g.guiWidth()), mx, my);
            }
        }
        int days = operation != null ? operation.total() - operation.days() : offer != null ? offer.days() : 0;
        int outlookY = y + 76 + (((operation != null ? operation.operatives().size() : required) + 3) / 4) * 38;
        String outlook = operation != null ? operation.outlook() : offer != null ? offer.outlook() : "";
        String outlookLabel = operation != null ? operation.outlookLabel() : offer != null ? offer.outlookLabel() : "";
        for (int i = 0; i < 2; i++) {
            int rowY = outlookY + i * 18;
            UiAssets.draw(g, sprite("GFX_list_entry_bg"), x + 9, rowY, 120, 18);
            if (i == 1 && completed) {
                text(g, "§G" + UiCommandLabels.text("OPERATION_VIEW_COMPLETED", "완료") + "§!", x + 25, rowY + 5, 100);
                continue;
            }
            UiAssets.draw(g, sprite("GFX_operation_progress_bar_frame"), x + 62, rowY + 2, 65, 13);
            text(g, i == 0 ? UiCommandLabels.text("OPERATION_OUTLOOK_LABEL", "전망") : UiCommandLabels.text("DURATION", "기간"), x + 25, rowY + 5, 36);
            if (i == 1) UiText.centered(g, font, days + "일", x + 63, rowY + 1, 63, 14, 0xFFFFCC33, false);
            else UiText.centered(g, font, Tooltips.officerText(outlookLabel, false), x + 63, rowY + 1, 63, 14, UiMenuStyle.TEXT, false);
        }
        var preparation = operation != null ? operation.preparation() : offer != null ? offer.preparation() : AgencyView.Preparation.NONE;
        int preparationY = outlookY + 36;
        if (!preparation.label().isEmpty()) {
            UiAssets.draw(g, sprite("GFX_list_entry_bg"), x + 9, preparationY, 120, 20);
            g.pose().pushMatrix(); g.pose().translate(x + 9, preparationY); g.pose().scale(.5f);
            UiAssets.draw(g, sprite("GFX_resource_bg"), 15, 2, 85, 27);
            UiAssets.draw(g, "agency/operations/gfx_industrial_capacity_icon/frame_1", 42, 7, 26, 22);
            UiAssets.draw(g, sprite("GFX_efficiency_progressbar_background"), 113, 16, 93, 9);
            int filled = (int)Math.round(93 * Math.clamp(preparation.progress(), 0, 1));
            if (filled > 0) {
                for (int row = 0; row < 8; row++) {
                    int sourceRow = 9 + row + (row >= 5 ? 1 : 0);
                    g.blit(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + sprite("GFX_efficiency_progressbar_fill") + ".png"),
                            113, 17 + row, 113 + filled, 18 + row, 0, filled / 93f, sourceRow / 27f, (sourceRow + 1) / 27f);
                }
            }
            UiAssets.draw(g, sprite("GFX_efficiency_progressbar_frame"), 110, 13, 99, 15);
            g.pose().popMatrix();
            var label = Tooltips.officerText(preparation.label(), false);
            var labelBounds = font.prepareText(label.getVisualOrderText(), 0, 0, UiMenuStyle.TEXT, false, false, 0).bounds();
            float labelBottom = labelBounds == null ? font.lineHeight : labelBounds.bottom();
            g.pose().pushMatrix();
            g.pose().translate(x + 88 - font.width(label) / 2f, preparationY + 5 - labelBottom);
            g.text(font, label, 0, 0, UiMenuStyle.TEXT, false);
            g.pose().popMatrix();
            if (operation != null) UiAssets.draw(g, sprite(preparation.progress() < 1
                    ? "GFX_operation_resource_progress_unfinished" : "GFX_operation_resource_progress_finished"), x + 119, preparationY + 5, 9, 9);
        }
        if (operation != null) {
            var autoCommence = action("auto_commence:" + operation.id());
            var autoRepeat = action("auto_repeat:" + operation.id());
            if (autoCommence != null) UiText.cell(g, font, autoCommence.title(), x + 18, y + h - 22, 93, 13, UiMenuStyle.TEXT, true, false);
            if (autoRepeat != null) text(g, autoRepeat.title(), x + 289, y + h - 19, 90);
        } else if (startParameter() >= 0) {
            UiText.cell(g, font, draftLabel("auto_commence"), x + 18, y + h - 22, 93, 13, UiMenuStyle.TEXT, true, false);
            text(g, draftLabel("auto_repeat"), x + 289, y + h - 19, 90);
            UiAssets.draw(g, sprite("GFX_operation_refund_operation"), x + 251, y + h - 22, 14, 14);
        }
        int origin = y + 27;
        scroll = Math.clamp(scroll, 0, bar().maximum());
        int cy = origin - scroll;
        g.enableScissor(right() - 3, y + 25, x + w - 13, y + h - 61);
        if (choosing == 0) {
            cy = origin;
        } else if (operation != null && !operation.phases().isEmpty() || offer != null && !offer.phases().isEmpty()) {
            var phases = operation != null ? operation.phases() : offer.phases();
            int pitch = (area() - 6) / 3;
            for (int first = 0; first < phases.size(); first += 3) {
                for (int i = first; i < Math.min(first + 3, phases.size()); i++) {
                    var phase = phases.get(i);
                    int px = right() + (i - first) * pitch;
                    UiAssets.draw(g, sprite(phase.picture()), px, cy, pitch - 4, 59);
                }
                if (completed) UiAssets.draw(g, sprite("GFX_operation_complete"), right(), cy, pitch * 3 - 4, 83);
                for (int i = first; i < Math.min(first + 3, phases.size()); i++) {
                    UiText.centered(g, font, phases.get(i).name(), right() + (i - first) * pitch, cy + 60, pitch - 4, 24, UiMenuStyle.TEXT);
                }
                cy += 83;
            }
            if (completed) cy = text(g, operation.description(), right() + 3, cy + 15, area() - 12) + 8;
            else for (var phase : phases) {
                UiAssets.draw(g, sprite(phase.icon()), right() - 3, cy - 3, 36, 36);
                cy = Math.max(cy + 35, text(g, phase.description(), right() + 35, cy + 2, area() - 43) + 6);
            }
        } else {
            cy = text(g, offer == null ? item.detail() : offer.description(), right() + 3, cy + 3, area() - 12) + 8;
        }
        contentHeight = cy + scroll - (y + 25);
        g.disableScissor();
        bar().draw(g, scroll);
        String reward = operation != null ? operation.reward() : offer != null ? offer.reward() : "";
        UiAssets.nineSlice(g, sprite("GFX_reward_bg"), right() - 3, y + h - 60, area() + 5, 33, 4, .5);
        UiAssets.draw(g, sprite("GFX_reward_icon"), right() - 3, y + h - 65, 48, 48);
        rewardScroll = Math.clamp(rewardScroll, 0, rewardBar().maximum());
        g.enableScissor(right() + 44, y + h - 57, x + w - 13, y + h - 29);
        if (!reward.isEmpty() && !country().isEmpty()) CountryFlags.draw(g, "country/" + country().toLowerCase(Locale.ROOT) + "/flag", "smallest", right() + 44, y + h - 54 - rewardScroll, 8, 6);
        rewardHeight = text(g, reward, right() + 55, y + h - 55 - rewardScroll, area() - 66) - (y + h - 55 - rewardScroll);
        g.disableScissor();
        rewardBar().draw(g, rewardScroll);
        super.extractRenderState(g, mx, my, delta);
        if (!outlook.isEmpty() && mx >= x + 9 && mx < x + 129 && my >= outlookY && my < outlookY + 18) {
            g.setTooltipForNextFrame(font, Tooltips.tooltipLines(font, outlook, g.guiWidth()), mx, my);
        }
        if (!preparation.detail().isEmpty() && mx >= x + 9 && mx < x + 129 && my >= preparationY && my < preparationY + 20)
            g.setTooltipForNextFrame(font, Tooltips.tooltipLines(font, preparation.detail(), g.guiWidth()), mx, my);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return true;
        if (rewardBar().contains(event.x(), event.y())) {
            rewardScrolling = !rewardBar().arrow(event.y());
            rewardScroll = rewardScrolling ? rewardBar().offset(event.y()) : rewardBar().step(event.y(), rewardScroll, 8);
            return true;
        }
        if (bar().contains(event.x(), event.y())) {
            scrolling = !bar().arrow(event.y());
            scroll = scrolling ? bar().offset(event.y()) : bar().step(event.y(), scroll, 20);
            return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;
        dragging = event.x() >= x && event.x() < x + w && event.y() >= y && event.y() < y + 22;
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && rewardScrolling) { rewardScroll = rewardBar().offset(event.y()); return true; }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && scrolling) { scroll = bar().offset(event.y()); return true; }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && dragging) { x += (int)Math.round(dx); y += (int)Math.round(dy); rebuildWidgets(); return true; }
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) { scrolling = false; rewardScrolling = false; dragging = false; return super.mouseReleased(event); }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (choosing >= 0) { choiceScroll -= (int)(vertical * 3); rebuildWidgets(); return true; }
        if (mx >= right() && mx < x + w && my >= y + h - 60 && my < y + h - 27) {
            rewardScroll = Math.clamp(rewardScroll - (int)(vertical * 8), 0, rewardBar().maximum()); return true;
        }
        if (mx >= right() && mx < x + w && my >= y + 25 && my < y + h - 61)
            scroll = Math.clamp(scroll - (int)(vertical * 20), 0, bar().maximum());
        return true;
    }
    @Override public void onClose() { if (choosing >= 0) { choosing = -1; rebuildWidgets(); return; } scrolling = false; rewardScrolling = false; dragging = false; minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
