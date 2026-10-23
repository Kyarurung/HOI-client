package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiDiplomacyAssets;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.UiTooltipText;
import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.ProtocolDiplomacyPayloads;
import dev.hoi.protocol.ProtocolDiplomacyView;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import static dev.hoi.protocol.ProtocolDiplomacyPayloads.Operation.*;
import static dev.hoi.protocol.ProtocolDiplomacyView.*;

public final class ScreenDiplomacyLease extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final String LABELS = "hoi:ui/diplomacy_labels.json";
    private static final List<Mode> MODE_ORDER = List.of(Mode.MONTHLY, Mode.PERCENT, Mode.ONCE);
    private final Screen parent;
    private final UiDiplomacyAssets assets = new UiDiplomacyAssets();
    private final Consumer<ProtocolDiplomacyPayloads.Request> transport;
    private final BooleanSupplier contextValid;
    private final LinkedHashMap<String, LeaseLine> draft = new LinkedHashMap<>();
    private final Map<String, String> amounts = new LinkedHashMap<>();
    private final Map<AbstractWidget, String> tips = new LinkedHashMap<>();
    private final Map<String, EditBox> inputs = new LinkedHashMap<>();
    private ProtocolDiplomacyView view;
    private Map<String, String> labels;
    private String token = UUID.randomUUID().toString();
    private ProtocolDiplomacyPayloads.Operation pending;
    private boolean dirty, closed, catalogOpen;
    private Category category = Category.INFANTRY;
    private int left, top, catalogLeft, draftScroll, catalogScroll, editTicks, requestTicks, draggingScroll;
    private double scale;
    private LeaseButton execute;
    private UiTooltips.ScrolledTooltip tooltip;
    private String tooltipText = "", pressedControl = "";
    private int tooltipScroll;
    public ScreenDiplomacyLease(Screen parent, ProtocolDiplomacyView initial) {
        this(parent, initial, request -> {
            if (ClientPlayNetworking.canSend(ProtocolDiplomacyPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        }, () -> dev.hoi.client.ClientCampaignHud.visible() && initial.viewer().equals(dev.hoi.client.ClientCampaignHud.country()));
    }
    public ScreenDiplomacyLease(Screen parent, ProtocolDiplomacyView initial, Consumer<ProtocolDiplomacyPayloads.Request> transport) {
        this(parent, initial, transport, () -> true);
    }
    private ScreenDiplomacyLease(Screen parent, ProtocolDiplomacyView initial, Consumer<ProtocolDiplomacyPayloads.Request> transport, BooleanSupplier contextValid) {
        super(Component.literal(Objects.requireNonNull(initial).title()));
        this.parent = parent;
        this.view = initial;
        this.transport = Objects.requireNonNull(transport);
        this.contextValid = Objects.requireNonNull(contextValid);
        for (var line : initial.lines()) {
            draft.put(line.key(), line);
            amounts.put(line.key(), Long.toString(line.amount()));
        }
    }
    public Screen parent() { return parent; }
    public ProtocolDiplomacyView view() { return view; }
    public List<LeaseLine> draft() { return List.copyOf(draft.values()); }
    String token() { return token; }
    boolean pending() { return pending != null; }
    boolean canSubmit() {
        return !closed && pending == null && !dirty && validAmounts() && !draft.isEmpty()
                && view.submitEnabled() && view.lines().equals(draft());
    }
    public void update(ProtocolDiplomacyPayloads.Response packet) {
        if (closed) return;
        if (!contextValid.getAsBoolean()) { revoke(); return; }
        if (!token.equals(packet.token())) return;
        switch (packet.status()) {
            case OPEN, UPDATED -> update(packet.view());
            case REVOKED -> revoke();
            case COMPLETED -> {
                closed = true;
                if (parent instanceof ScreenCountry country) country.requestRefresh();
                ScreenDialogCoordinator.setContent(parent);
            }
        }
    }
    public void update(ProtocolDiplomacyView next) {
        if (closed) return;
        if (!contextValid.getAsBoolean()) { revoke(); return; }
        if (!view.session().equals(next.session()) || !view.viewer().equals(next.viewer())
                || !view.target().equals(next.target()) || next.revision() <= view.revision()) return;
        view = next;
        draft.keySet().removeIf(key -> equipment(key) == null);
        amounts.keySet().retainAll(draft.keySet());
        pending = null;
        requestTicks = 0;
        if (validAmounts() && next.lines().equals(draft())) dirty = false;
        rebuildWidgets();
    }
    public void revoke() {
        if (closed) return;
        closed = true;
        draft.clear();
        amounts.clear();
        ScreenDialogCoordinator.setContent(null);
    }
    private EquipmentChoice equipment(String key) {
        return view.equipment().stream().filter(choice -> choice.key().equals(key)).findFirst().orElse(null);
    }
    private ModeChoice mode(EquipmentChoice equipment, Mode selected) {
        return equipment == null ? null : equipment.modes().stream().filter(value -> value.mode() == selected).findFirst().orElse(null);
    }
    private boolean editing() { return !closed && pending != SUBMIT; }
    private static long amount(String value, Mode mode) {
        if (value == null || !value.matches("[0-9]+")) return -1;
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 && (mode != Mode.PERCENT || parsed <= 100) ? parsed : -1;
        } catch (NumberFormatException invalid) {
            return -1;
        }
    }
    private boolean validAmounts() {
        return draft.values().stream().allMatch(line -> amount(amounts.get(line.key()), line.mode()) == line.amount());
    }
    private void changed() {
        dirty = true;
        editTicks = 0;
        if (execute != null) execute.active = false;
    }
    void add(String key) {
        if (!editing() || draft.containsKey(key) || draft.size() >= 32) return;
        var choice = equipment(key);
        if (choice == null || !choice.available()) return;
        var initial = choice.modes().stream().filter(value -> value.supported() && value.defaultAmount() > 0).findFirst().orElse(null);
        if (initial == null) return;
        draft.put(key, new LeaseLine(key, initial.mode(), initial.defaultAmount()));
        amounts.put(key, Long.toString(initial.defaultAmount()));
        changed();
        rebuildWidgets();
    }
    void remove(String key) {
        if (!editing() || draft.remove(key) == null) return;
        amounts.remove(key);
        changed();
        rebuildWidgets();
    }
    void clearDraft() {
        if (!editing() || draft.isEmpty()) return;
        draft.clear();
        amounts.clear();
        changed();
        rebuildWidgets();
    }
    void selectMode(String key, Mode selected) {
        if (!editing()) return;
        var line = draft.get(key);
        var choice = mode(equipment(key), selected);
        if (line == null || choice == null || !choice.supported() || choice.defaultAmount() <= 0 || line.mode() == selected) return;
        draft.put(key, new LeaseLine(key, selected, choice.defaultAmount()));
        amounts.put(key, Long.toString(choice.defaultAmount()));
        changed();
        rebuildWidgets();
    }
    void setAmount(String key, String value) {
        if (!editing() || !draft.containsKey(key)) return;
        amounts.put(key, value);
        var line = draft.get(key);
        long parsed = amount(value, line.mode());
        if (parsed > 0) draft.put(key, new LeaseLine(key, line.mode(), parsed));
        changed();
    }
    private void increment(String key, int direction) {
        var line = draft.get(key);
        if (line == null || !editing()) return;
        long parsed = amount(amounts.get(key), line.mode());
        if (parsed <= 0 || direction < 0 && parsed <= 1 || direction > 0 && (parsed == Long.MAX_VALUE || line.mode() == Mode.PERCENT && parsed == 100)) return;
        setAmount(key, Long.toString(parsed + direction));
        rebuildWidgets();
    }
    private void send(ProtocolDiplomacyPayloads.Operation operation) {
        if (closed || pending != null || operation == SUBMIT && !canSubmit()) return;
        if (operation != CLOSE && !validAmounts()) return;
        token = UUID.randomUUID().toString();
        pending = operation;
        requestTicks = 0;
        if (operation == PREVIEW) dirty = false;
        transport.accept(new ProtocolDiplomacyPayloads.Request(token, view.session(), view.revision(), operation,
                operation == CLOSE ? List.of() : draft()));
        rebuildWidgets();
    }
    void submit() { if (canSubmit()) send(SUBMIT); }
    @Override public void tick() {
        if (closed) return;
        if (!contextValid.getAsBoolean()) { revoke(); return; }
        if (pending != null && ++requestTicks >= 100) {
            pending = null;
            changed();
            rebuildWidgets();
        }
        if (dirty && pending == null && validAmounts() && ++editTicks >= 6) send(PREVIEW);
    }
    @Override public void onClose() {
        if (closed) return;
        if (!contextValid.getAsBoolean()) { revoke(); return; }
        closed = true;
        transport.accept(new ProtocolDiplomacyPayloads.Request(UUID.randomUUID().toString(), view.session(), view.revision(), CLOSE, List.of()));
        if (parent instanceof ScreenCountry country) country.requestRefresh();
        ScreenDialogCoordinator.setContent(parent);
    }
    @Override public boolean allowsMovement() { return false; }
    @Override public int unitHudLeft() { return parent instanceof InputSidebarMovement.InputMovementScreen screen ? screen.unitHudLeft() : 0; }
    @Override public void removed() { assets.close(); InputSidebarMovement.release(minecraft); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private int s(double value) { return (int)Math.round(value * scale); }
    private int x(int value) { return left + s(value); }
    private int y(int value) { return top + s(value); }
    private String label(String key) { return labels.getOrDefault(key, key); }
    private static String categoryKey(Category value) {
        return "LEND_LEASE_" + switch (value) {
            case INFANTRY -> "INFANTRY_BUTTON";
            case ARMOR -> "VEHICLES_BUTTON";
            case AIR -> "AIRCRAFT_BUTTON";
            case NAVAL -> "NAVAL_BUTTON";
        };
    }
    private void loadLabels() {
        if (labels != null) return;
        labels = Map.of();
        var resource = minecraft.getResourceManager().getResource(Identifier.parse(LABELS));
        if (resource.isEmpty()) return;
        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            Map<String, String> source = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (source != null) labels = Map.copyOf(source);
        } catch (IOException | RuntimeException invalid) {
            labels = Map.of();
        }
    }
    @Override protected void rebuildWidgets() {
        String focused = inputs.entrySet().stream().filter(entry -> entry.getValue().isFocused()).map(Map.Entry::getKey).findFirst().orElse(null);
        super.rebuildWidgets();
        if (focused != null && inputs.containsKey(focused)) setFocused(inputs.get(focused));
    }
    @Override protected void init() {
        loadLabels();
        var previousInputs = new LinkedHashMap<>(inputs);
        inputs.clear();
        tips.clear();
        tooltip = null;
        int toolbar = UiMenuBar.height(width);
        scale = Math.max(.1, Math.min(.5, Math.min((width - 12) / 915.0, (height - toolbar - 12) / 612.0)));
        left = (width - s(527)) / 2;
        if (catalogOpen) left = Math.max(4, Math.min(left, width - s(910) - 4));
        top = Math.max(toolbar + 4, (height - s(612)) / 2);
        catalogLeft = x(535);
        if (parent != null) parent.init(width, height);
        for (var value : Category.values()) {
            String suffix = value.name().toLowerCase(java.util.Locale.ROOT);
            var button = button(label(categoryKey(value)), "diplomacy/lease/add_" + suffix + (catalogOpen && category == value ? "/frame_1" : "/frame_0"),
                    x(87 + value.ordinal() * 81), y(132), s(81), s(41), true,
                    () -> { category = value; catalogOpen = true; catalogScroll = 0; rebuildWidgets(); });
            button.controlKey = "category:" + value;
            button.active = editing();
            tip(button, label(categoryKey(value)));
        }
        var clear = button(label("GUI_LEND_LEASE_CLEAR_ALL"), "diplomacy/dialog/clear_button/frame_0", x(410), y(137), s(94), s(31), false, this::clearDraft);
        clear.active = editing() && !draft.isEmpty();
        draftScroll = Math.clamp(draftScroll, 0, Math.max(0, draft.size() * 72 - 292));
        int index = 0;
        for (var line : draft.values()) {
            int row = 190 + index++ * 72 - draftScroll;
            if (row < 190 || row + 72 > 487) continue;
            draftWidgets(line, row, previousInputs.get(line.key()));
        }
        var cancel = button(label("DIPLOMACY_CANCEL"), "diplomacy/dialog/footer_button/frame_0", x(70), y(552), s(123), s(36), false, this::onClose);
        cancel.sound = "ui.close";
        cancel.active = !closed;
        execute = button(label("DIPLOMACY_SEND"), "diplomacy/dialog/footer_button/frame_0", x(350), y(552), s(123), s(36), false, this::submit);
        execute.sound = "ui.air.filter";
        execute.active = canSubmit();
        tip(execute, view.submitReason().isEmpty() ? view.acceptanceDetail() : view.submitReason());
        if (catalogOpen) catalogWidgets();
    }
    private void draftWidgets(LeaseLine line, int row, EditBox previous) {
        var choice = equipment(line.key());
        if (choice == null) return;
        int rowLeft = x(17), rowTop = y(row);
        for (int index = 0; index < MODE_ORDER.size(); index++) {
            var selected = MODE_ORDER.get(index);
            var option = mode(choice, selected);
            if (option == null) continue;
            var button = button(option.label(), "diplomacy/dialog/mode_button/frame_0", rowLeft + s(180 + index * 77), rowTop + s(7), s(71), s(26), false,
                    () -> selectMode(line.key(), selected));
            button.active = editing() && option.supported();
            button.controlKey = "mode:" + line.key() + ":" + selected;
            String sourceTip = label("DIPLOMACY_LEND_LEASE_" + switch (selected) { case MONTHLY -> "NUMBER"; case PERCENT -> "PERCENT"; case ONCE -> "ONCE"; } + "_TOOLTIP");
            tip(button, option.reason().isEmpty() ? sourceTip : option.reason());
        }
        var input = previous == null ? new EditBox(font, rowLeft + s(220), rowTop + s(40), Math.max(14, s(50)), Math.max(8, s(20)), Component.literal(choice.name())) : previous;
        input.setResponder(value -> {});
        input.setX(rowLeft + s(220));
        input.setY(rowTop + s(40));
        input.setWidth(Math.max(14, s(50)));
        input.setHeight(Math.max(8, s(20)));
        input.setMessage(Component.literal(choice.name()));
        input.setBordered(false);
        input.setMaxLength(19);
        String raw = amounts.getOrDefault(line.key(), Long.toString(line.amount()));
        if (!input.getValue().equals(raw)) input.setValue(raw);
        input.setEditable(editing());
        input.setResponder(value -> setAmount(line.key(), value));
        addRenderableWidget(input);
        inputs.put(line.key(), input);
        var decrease = button("−", "diplomacy/dialog/decrease/frame_0", rowLeft + s(188), rowTop + s(38), s(27 * .9), s(27 * .9), true, () -> increment(line.key(), -1));
        decrease.controlKey = "decrease:" + line.key();
        decrease.active = editing() && amount(amounts.get(line.key()), line.mode()) > 1;
        var increase = button("+", "diplomacy/dialog/increase/frame_0", rowLeft + s(277), rowTop + s(38), s(27 * .9), s(27 * .9), true, () -> increment(line.key(), 1));
        long value = amount(amounts.get(line.key()), line.mode());
        increase.controlKey = "increase:" + line.key();
        increase.active = editing() && value > 0 && value < (line.mode() == Mode.PERCENT ? 100 : Long.MAX_VALUE);
        var remove = button(label("CLOSE") + " · " + choice.name(), "diplomacy/dialog/remove/frame_2", rowLeft + s(453), rowTop + s(5), s(28), s(28), true, () -> remove(line.key()));
        remove.controlKey = "remove:" + line.key();
        remove.sound = "ui.close";
        remove.active = editing();
    }
    private List<EquipmentChoice> catalog() {
        return view.equipment().stream().filter(choice -> choice.category() == category && !draft.containsKey(choice.key())).toList();
    }
    private void catalogWidgets() {
        button(label("CLOSE"), "diplomacy/dialog/close/frame_0", catalogLeft + s(325), y(5), s(32), s(33), true,
                () -> { catalogOpen = false; rebuildWidgets(); }).sound = "ui.close";
        var choices = catalog();
        catalogScroll = Math.clamp(catalogScroll, 0, Math.max(0, choices.size() * 75 - 470));
        for (int index = 0; index < choices.size(); index++) {
            var choice = choices.get(index);
            int row = 64 + index * 75 - catalogScroll;
            if (row < 64 || row + 72 > 548) continue;
            var button = button(choice.name(), "", catalogLeft + s(12), y(row), s(336), s(71), true, () -> add(choice.key()));
            button.controlKey = "catalog:" + choice.key();
            button.active = editing() && choice.available() && draft.size() < 32
                    && choice.modes().stream().anyMatch(value -> value.supported() && value.defaultAmount() > 0);
            tip(button, choice.detail().isEmpty() ? choice.reason() : choice.reason().isEmpty() || choice.detail().equals(choice.reason()) ? choice.detail() : choice.detail() + "\n\n" + choice.reason());
        }
    }
    private LeaseButton button(String text, String texture, int x, int y, int width, int height, boolean icon, Runnable action) {
        var button = new LeaseButton(text, texture, x, y, Math.max(1, width), Math.max(1, height), icon, action);
        addRenderableWidget(button);
        return button;
    }
    private void tip(AbstractWidget widget, String text) { widget.setTooltip(null); if (!text.isEmpty()) tips.put(widget, text); }
    private void text(GuiGraphicsExtractor g, String text, int x, int y, int width, int height, int color) {
        UiText.centeredSingleLine(g, font, UiTooltips.officerText(text.replace("§T", "§W")), x, y, width, height, color);
    }
    private boolean overTooltip(double x, double y) {
        return tooltip != null && x >= tooltip.x() - 4 && x < tooltip.x() + tooltip.width() + 4
                && y >= tooltip.y() - 4 && y < tooltip.y() + tooltip.height() + 4;
    }
    private int maximumScroll(boolean catalog) { return Math.max(0, (catalog ? catalog().size() * 75 - 470 : draft.size() * 72 - 292)); }
    private int scrollLeft(boolean catalog) { return catalog ? catalogLeft + s(355) : x(507); }
    private int scrollTop(boolean catalog) { return y(catalog ? 64 : 190); }
    private int scrollBottom(boolean catalog) { return y(catalog ? 548 : 487); }
    private void setScroll(boolean catalog, int value) {
        if (catalog) catalogScroll = Math.clamp(value, 0, maximumScroll(true));
        else draftScroll = Math.clamp(value, 0, maximumScroll(false));
        rebuildWidgets();
    }
    private void moveScroll(boolean catalog, double mouseY) {
        int trackTop = scrollTop(catalog) + s(18), travel = Math.max(1, scrollBottom(catalog) - trackTop - s(34));
        setScroll(catalog, (int)Math.round((mouseY - trackTop - s(8)) * maximumScroll(catalog) / travel));
    }
    private boolean scrollClick(double mouseX, double mouseY) {
        for (boolean catalog : new boolean[]{false, true}) {
            if (catalog && !catalogOpen || maximumScroll(catalog) == 0) continue;
            if (mouseX < scrollLeft(catalog) || mouseX >= scrollLeft(catalog) + s(18)
                    || mouseY < scrollTop(catalog) || mouseY >= scrollBottom(catalog)) continue;
            int current = catalog ? catalogScroll : draftScroll, step = catalog ? 75 : 72;
            if (mouseY < scrollTop(catalog) + s(18)) setScroll(catalog, current - step);
            else if (mouseY >= scrollBottom(catalog) - s(18)) setScroll(catalog, current + step);
            else { draggingScroll = catalog ? 2 : 1; moveScroll(catalog, mouseY); }
            return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (draggingScroll != 0 && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) { moveScroll(draggingScroll == 2, event.y()); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        draggingScroll = 0;
        pressedControl = "";
        super.mouseReleased(event);
        return true;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (!overTooltip(event.x(), event.y())) {
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && scrollClick(event.x(), event.y())) return true;
            if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT)
                for (var child : children()) if (child instanceof LeaseButton button && button.active && UiTooltips.contains(button, event.x(), event.y())) { pressedControl = button.controlKey; break; }
            super.mouseClicked(event, twice);
        }
        return true;
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (overTooltip(mx, my)) {
            tooltipScroll = Math.clamp(tooltipScroll - (int)Math.round(vertical * 3 * (font.lineHeight + 1)), 0, tooltip.maxScroll());
        } else if (catalogOpen && mx >= catalogLeft && mx < catalogLeft + s(375) && my >= y(64) && my < y(548)) {
            catalogScroll = Math.max(0, catalogScroll - (int)Math.round(vertical * 75));
            rebuildWidgets();
        } else if (mx >= left && mx < x(527) && my >= y(190) && my < y(487)) {
            draftScroll = Math.max(0, draftScroll - (int)Math.round(vertical * 72));
            rebuildWidgets();
        }
        return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        assets.beginFrame();
        if (parent != null) { parent.extractRenderState(g, -1, -1, delta); g.nextStratum(); }
        UiAssets.draw(g, "diplomacy/lease/background", left, top, s(527), s(612));
        assets.tile(g, "diplomacy/dialog/panel_dark", x(0), y(175), s(527), s(312), 64, 64, scale);
        text(g, view.title(), x(0), y(30), s(530), s(20), UiMenuStyle.TEXT);
        text(g, view.currentStatus(), x(15), y(82), s(190), s(20), UiMenuStyle.TEXT);
        text(g, view.subtitle(), x(220), y(82), s(230), s(20), UiMenuStyle.TEXT);
        int index = 0;
        String hovered = "";
        for (var line : draft.values()) {
            int row = 190 + index++ * 72 - draftScroll;
            if (row < 190 || row + 72 > 487) continue;
            var choice = equipment(line.key());
            if (choice == null) continue;
            drawDraft(g, choice, line, row);
            if (mx >= x(25) && mx < x(180) && my >= y(row) && my < y(row + 72)) hovered = choice.detail();
        }
        UiAssets.draw(g, "diplomacy/dialog/response", x(30), y(500), s(449), s(40));
        text(g, view.acceptanceText(), x(110), y(505), s(315), s(40), UiMenuStyle.TEXT);
        if (mx >= x(30) && mx < x(497) && my >= y(500) && my < y(548)) hovered = view.acceptanceDetail();
        if (catalogOpen) drawCatalog(g);
        drawScrollbar(g, false);
        if (catalogOpen) drawScrollbar(g, true);
        boolean over = overTooltip(mx, my);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget)
            widget.extractRenderState(g, over ? -1 : mx, over ? -1 : my, delta);
        if (over) hovered = tooltipText;
        else for (var entry : tips.entrySet()) if (UiTooltips.contains(entry.getKey(), mx, my)) { hovered = entry.getValue(); break; }
        if (hovered.isEmpty()) { tooltip = null; tooltipText = ""; tooltipScroll = 0; }
        else {
            if (!hovered.equals(tooltipText)) { tooltipText = hovered; tooltipScroll = 0; }
            tooltip = UiTooltips.drawSourceScrollable(g, font, tooltipText.replace("§T", "§W"), mx, my, width, height, tooltipScroll);
            tooltipScroll = tooltip.scroll();
        }
    }
    private void drawScrollbar(GuiGraphicsExtractor g, boolean catalog) {
        int maximum = maximumScroll(catalog);
        if (maximum == 0) return;
        int x = scrollLeft(catalog), top = scrollTop(catalog), bottom = scrollBottom(catalog);
        String path = "commander/commands/";
        assets.tile(g, path + "scrollbar_vertical_bg/frame_0", x, top, s(18), bottom - top, 0, 52, scale);
        assets.tile(g, path + "scroll_track/frame_0", x + s(3), top + s(18), s(12), bottom - top - s(36), 4, 4, scale);
        UiAssets.draw(g, path + "scroll_up/frame_0", x + s(2), top + s(3), s(16), s(16));
        UiAssets.draw(g, path + "scroll_down/frame_0", x + s(1), bottom - s(19), s(16), s(16));
        int travel = Math.max(1, bottom - top - s(52));
        int position = top + s(18) + (int)Math.round(travel * (catalog ? catalogScroll : draftScroll) / (double)maximum);
        UiAssets.draw(g, path + "scroll_drager/frame_0", x, position, s(16), s(16));
    }
    private void paragraph(GuiGraphicsExtractor g, String content, int x, int y, int width, int height) {
        if (content.isEmpty() || width <= 0 || height <= 0) return;
        var text = UiTooltips.officerText(UiTooltipText.sourceSpacing(content).replace("§T", "§W"));
        var lines = font.split(text, width);
        if (lines.isEmpty()) return;
        float fitted = 1f;
        if (lines.size() * (font.lineHeight + 1) - 1 > height || lines.stream().anyMatch(line -> font.width(line) > width)) {
            float lower = 0f, upper = 1f;
            for (int attempt = 0; attempt < 12; attempt++) {
                float candidate = (lower + upper) / 2;
                var wrapped = font.split(text, Math.max(1, (int)(width / candidate)));
                if ((wrapped.size() * (font.lineHeight + 1) - 1) * candidate <= height
                        && wrapped.stream().allMatch(line -> font.width(line) * candidate <= width)) lower = candidate;
                else upper = candidate;
            }
            fitted = lower;
            lines = font.split(text, Math.max(1, (int)(width / fitted)));
        }
        g.enableScissor(x, y, x + width, y + height);
        g.pose().pushMatrix();
        g.pose().translate(x, y + (height - (lines.size() * (font.lineHeight + 1) - 1) * fitted) / 2);
        g.pose().scale(fitted);
        for (int index = 0; index < lines.size(); index++) {
            g.pose().pushMatrix();
            g.pose().translate((width / fitted - font.width(lines.get(index))) / 2, index * (font.lineHeight + 1));
            g.text(font, lines.get(index), 0, 0, UiMenuStyle.TEXT);
            g.pose().popMatrix();
        }
        g.pose().popMatrix();
        g.disableScissor();
    }
    private void drawDraft(GuiGraphicsExtractor g, EquipmentChoice choice, LeaseLine line, int row) {
        int rowLeft = x(17), rowTop = y(row);
        UiAssets.draw(g, "diplomacy/lease/selected_row", rowLeft, rowTop, s(486), s(71));
        UiAssets.draw(g, choice.icon(), rowLeft + s(8), rowTop + s(24), s(164), s(45));
        paragraph(g, choice.name(), rowLeft + s(8), rowTop + s(4), s(164), s(32));
        UiAssets.draw(g, "diplomacy/lease/value60", rowLeft + s(215), rowTop + s(36), s(60), s(27));
        var mode = mode(choice, line.mode());
        if (mode != null) text(g, mode.unitLabel(), rowLeft + s(307), rowTop + s(39), s(140), s(22), UiMenuStyle.TEXT);
    }
    private void drawCatalog(GuiGraphicsExtractor g) {
        assets.tile(g, "diplomacy/dialog/panel_plain", catalogLeft, top, s(375), s(560), 64, 64, scale);
        assets.tile(g, "diplomacy/dialog/panel_border", catalogLeft, y(59), s(375), s(501), 64, 64, scale);
        UiAssets.draw(g, "diplomacy/lease/catalog_header", catalogLeft + s(3), y(42), s(369), s(35));
        text(g, label("DIPLOMACY_LEND_LEASE_HEADER_EQUIPMENT"), catalogLeft + s(10), y(47), s(160), s(20), UiMenuStyle.TEXT);
        text(g, label("DIPLOMACY_LEND_LEASE_HEADER_IN_STORAGE"), catalogLeft + s(130), y(47), s(160), s(20), UiMenuStyle.TEXT);
        text(g, label("DIPLOMACY_LEND_LEASE_HEADER_PRODUCING"), catalogLeft + s(222), y(47), s(160), s(20), UiMenuStyle.TEXT);
        var choices = catalog();
        if (choices.isEmpty()) UiText.centered(g, font, label("DIPLOMACY_LEND_LEASE_NO_EQUIPMENT_CATEGORY"), catalogLeft + s(15), y(69), s(345), s(50), UiMenuStyle.TEXT);
        for (int index = 0; index < choices.size(); index++) {
            var choice = choices.get(index);
            int row = 64 + index * 75 - catalogScroll;
            if (row < 64 || row + 72 > 548) continue;
            int rowLeft = catalogLeft + s(12), rowTop = y(row);
            UiAssets.draw(g, "diplomacy/lease/catalog_row/frame_0", rowLeft, rowTop, s(336), s(71));
            UiAssets.draw(g, choice.icon(), rowLeft + s(8), rowTop + s(24), s(164), s(45));
            paragraph(g, choice.name(), rowLeft + s(8), rowTop + s(4), s(165), s(32));
            UiAssets.draw(g, "diplomacy/lease/value60", rowLeft + s(177), rowTop + s(30), s(60), s(27));
            UiAssets.draw(g, "diplomacy/lease/value88", rowLeft + s(240), rowTop + s(30), s(88), s(27));
            UiAssets.draw(g, "diplomacy/lease/stock", rowLeft + s(194), rowTop + s(7), s(24), s(23));
            UiAssets.draw(g, "diplomacy/lease/balance", rowLeft + s(269), rowTop + s(7), s(30), s(23));
            text(g, choice.stockText(), rowLeft + s(178), rowTop + s(33), s(58), s(20), UiMenuStyle.TEXT);
            paragraph(g, choice.productionText(), rowLeft + s(244), rowTop + s(30), s(80), s(27));
        }
    }
    private final class LeaseButton extends Button {
        private final String texture;
        private final boolean icon;
        private String controlKey;
        private String sound = "ui.click";
        LeaseButton(String text, String texture, int x, int y, int width, int height, boolean icon, Runnable action) {
            super(x, y, width, height, Component.literal(text), button -> action.run(), DEFAULT_NARRATION);
            this.texture = texture;
            this.controlKey = texture + ":" + text;
            this.icon = icon;
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { dev.hoi.client.audio.AudioUiSounds.play(sound); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (!texture.isEmpty()) {
                var effect = switch (texture) {
                    case "diplomacy/dialog/clear_button/frame_0", "diplomacy/dialog/footer_button/frame_0",
                            "diplomacy/dialog/increase/frame_0", "diplomacy/dialog/decrease/frame_0",
                            "diplomacy/dialog/remove/frame_2", "diplomacy/dialog/close/frame_0" -> UiDiplomacyAssets.Effect.BUTTON_STATE;
                    default -> UiDiplomacyAssets.Effect.NONE;
                };
                var state = !active ? UiDiplomacyAssets.State.DISABLED : controlKey.equals(pressedControl) ? UiDiplomacyAssets.State.DOWN
                        : UiTooltips.contains(this, mx, my) ? UiDiplomacyAssets.State.OVER : UiDiplomacyAssets.State.UP;
                assets.button(g, controlKey, texture, getX(), getY(), getWidth(), getHeight(), effect, state);
            }
            if (!icon) text(g, getMessage().getString(), getX() + 2, getY(), getWidth() - 4, getHeight(), active ? UiMenuStyle.TEXT : UiMenuStyle.MUTED);
        }
    }
}