package dev.hoi.client.screen.diplomacy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.audio.AudioUiSounds;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.UiDiplomacyAssets;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.diplomacy.DiplomacyVolunteerPayloads;
import dev.hoi.protocol.diplomacy.DiplomacyVolunteerView;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static dev.hoi.protocol.diplomacy.DiplomacyVolunteerPayloads.Operation.*;

public final class DiplomacyVolunteers extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int LIST_TOP = 134;
    private static final int LIST_BOTTOM = 324;
    private static final int ROW_HEIGHT = 38;
    private final Screen parent;
    private final Consumer<DiplomacyVolunteerPayloads.Request> transport;
    private final Map<AbstractWidget, String> tips = new LinkedHashMap<>();
    private final UiDiplomacyAssets assets = new UiDiplomacyAssets();
    private DiplomacyVolunteerView view;
    private Map<String, String> labels;
    private String token = UUID.randomUUID().toString();
    private DiplomacyVolunteerPayloads.Operation pending;
    private boolean closed, draggingScroll, draggingWindow, refreshRequired;
    private int left, top, scroll, requestTicks, offsetX, offsetY;
    private double scale, dragX, dragY;
    private Tooltips.ScrolledTooltip tooltip;
    private String tooltipText = "";
    private int tooltipScroll;
    private VolunteerButton pressed;
    public DiplomacyVolunteers(Screen parent, DiplomacyVolunteerView initial) {
        this(parent, initial, request -> {
            if (ClientPlayNetworking.canSend(DiplomacyVolunteerPayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public DiplomacyVolunteers(Screen parent, DiplomacyVolunteerView initial,
                                    Consumer<DiplomacyVolunteerPayloads.Request> transport) {
        super(Component.literal(Objects.requireNonNull(initial).title()));
        this.parent = parent;
        this.view = initial;
        this.transport = Objects.requireNonNull(transport);
    }
    public DiplomacyVolunteerView view() { return view; }
    boolean pending() { return pending != null; }
    int scrollPosition() { return scroll; }
    boolean canSubmit() { return contextValid() && !closed && pending == null && !refreshRequired && view.submitEnabled(); }
    private boolean contextValid() { return ClientCampaignHud.visible() && view.viewer().equals(ClientCampaignHud.country()); }
    private boolean editable() { return contextValid() && !closed && pending == null; }
    public void update(DiplomacyVolunteerPayloads.Response packet) {
        if (closed) return;
        if (!contextValid()) { revoke(); return; }
        if (!token.equals(packet.token())) return;
        switch (packet.status()) {
            case OPEN, UPDATED -> accept(packet.view(), true);
            case REVOKED -> revoke();
            case COMPLETED -> {
                closed = true;
                if (parent instanceof DiplomacyCountryScreen country) country.requestRefresh();
                DialogCoordinator.setContent(parent);
            }
        }
    }
    public void update(DiplomacyVolunteerView next) { accept(next, false); }
    private void accept(DiplomacyVolunteerView next, boolean reply) {
        if (closed) return;
        if (!contextValid()) { revoke(); return; }
        if (!view.session().equals(next.session()) || !view.viewer().equals(next.viewer()) || !view.target().equals(next.target())
                || next.revision() < view.revision() || next.revision() == view.revision() && (!reply || pending == null)) return;
        view = next;
        pending = null;
        refreshRequired = false;
        requestTicks = 0;
        rebuildWidgets();
    }
    public void revoke() {
        if (closed) return;
        closed = true;
        pending = null;
        DialogCoordinator.setContent(null);
    }
    void select(String key) {
        if (!editable()) return;
        var row = view.armies().stream().filter(army -> army.key().equals(key)).findFirst().orElse(null);
        if (row == null) return;
        boolean selected = view.selectedArmyKeys().contains(key);
        if (!selected && !row.allowed()) return;
        send(SELECT, key, !selected);
    }
    void toggleAir() { if (editable() && (view.airEnabled() || view.airSelected())) send(AIR, "", !view.airSelected()); }
    void submit() { if (canSubmit()) send(SUBMIT, "", false); }
    private void send(DiplomacyVolunteerPayloads.Operation operation, String key, boolean selected) {
        if (!editable()) return;
        token = UUID.randomUUID().toString();
        pending = operation;
        requestTicks = 0;
        transport.accept(new DiplomacyVolunteerPayloads.Request(token, view.session(), view.revision(), operation, key, selected));
        rebuildWidgets();
    }
    @Override public void tick() {
        if (closed) return;
        if (!contextValid()) { revoke(); return; }
        if (pending != null && ++requestTicks >= 100) {
            var expired = pending;
            pending = null;
            refreshRequired = true;
            if (expired != OPEN) send(OPEN, "", false);
            else rebuildWidgets();
        }
    }
    @Override public void onClose() {
        if (closed || pending != null) return;
        if (!contextValid()) { revoke(); return; }
        closed = true;
        transport.accept(new DiplomacyVolunteerPayloads.Request(UUID.randomUUID().toString(), view.session(), view.revision(), CANCEL, "", false));
        if (parent instanceof DiplomacyCountryScreen country) country.requestRefresh();
        DialogCoordinator.setContent(parent);
    }
    @Override public boolean allowsMovement() { return false; }
    @Override public int unitHudLeft() { return parent instanceof InputSidebarMovement.InputMovementScreen screen ? screen.unitHudLeft() : 0; }
    @Override public void removed() { assets.close(); InputSidebarMovement.release(minecraft); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private int s(int value) { return (int)Math.round(value * scale); }
    private int x(int value) { return left + s(value); }
    private int y(int value) { return top + s(value); }
    private String label(String key) { return labels.getOrDefault(key, key); }
    private void loadLabels() {
        if (labels != null) return;
        labels = Map.of();
        var resource = minecraft.getResourceManager().getResource(Identifier.parse("hoi:ui/diplomacy_labels.json"));
        if (resource.isEmpty()) return;
        try (var reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
            Map<String, String> source = new Gson().fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (source != null) labels = Map.copyOf(source);
        } catch (IOException | RuntimeException invalid) { labels = Map.of(); }
    }
    @Override protected void init() {
        loadLabels();
        tips.clear();
        tooltip = null;
        int toolbar = HudMenuBar.height(width);
        scale = Math.max(.1, Math.min(.5, Math.min((width - 12) / 527.0, (height - toolbar - 12) / 612.0)));
        int centerX = (width - s(527)) / 2;
        int centerY = Math.max(toolbar + 4, (height - s(612)) / 2);
        left = Math.clamp(centerX + offsetX, 4, Math.max(4, width - s(527) - 4));
        top = Math.clamp(centerY + offsetY, toolbar + 4, Math.max(toolbar + 4, height - s(612) - 4));
        offsetX = left - centerX;
        offsetY = top - centerY;
        if (parent != null) parent.init(width, height);
        scroll = Math.clamp(scroll, 0, maximumScroll());
        for (int index = 0; index < view.armies().size(); index++) {
            var row = view.armies().get(index);
            int rowY = rowY(index);
            int hitTop = Math.max(LIST_TOP, rowY), hitBottom = Math.min(LIST_BOTTOM, rowY + ROW_HEIGHT);
            if (hitBottom <= hitTop) continue;
            var button = button(row.name(), "", x(13), y(hitTop), s(489), s(hitBottom - hitTop), "ui.click", true, () -> select(row.key()));
            button.active = editable() && (row.allowed() || view.selectedArmyKeys().contains(row.key()));
            if (!row.reason().isEmpty()) tips.put(button, row.reason());
        }
        var air = button(label("AIR_VOLUNTEERS"), "diplomacy/dialog/checkbox/frame_" + (view.airSelected() ? 1 : 0),
                x(333), y(88), s(34), s(30), "ui.checkbox", true, this::toggleAir);
        air.active = editable() && (view.airEnabled() || view.airSelected());
        if (!view.airReason().isEmpty()) tips.put(air, view.airReason());
        var back = button(label("BACK"), "diplomacy/dialog/footer_button/frame_0", x(70), y(552), s(123), s(36), "ui.close", false, this::onClose);
        back.active = editable();
        var execute = button(label("DIPLOMACY_SEND"), "diplomacy/dialog/footer_button/frame_0", x(350), y(552), s(123), s(36), "ui.air.filter", false, this::submit);
        execute.active = canSubmit();
        if (!view.submitReason().isEmpty()) tips.put(execute, view.submitReason());
    }
    private VolunteerButton button(String text, String texture, int x, int y, int width, int height, String sound, boolean icon, Runnable action) {
        var button = new VolunteerButton(text, texture, x, y, Math.max(1, width), Math.max(1, height), sound, icon, action);
        addRenderableWidget(button);
        return button;
    }
    private int headerRows() { return view.unassignedText().isEmpty() ? 0 : 1; }
    private int rowY(int index) { return LIST_TOP + (index + headerRows()) * ROW_HEIGHT - scroll; }
    private int maximumScroll() { return Math.max(0, (view.armies().size() + headerRows()) * ROW_HEIGHT - (LIST_BOTTOM - LIST_TOP)); }
    private void setScroll(int value) { scroll = Math.clamp(value, 0, maximumScroll()); rebuildWidgets(); }
    private void moveScroll(double mouseY) {
        int trackTop = y(LIST_TOP + 18);
        int travel = Math.max(1, s(LIST_BOTTOM - LIST_TOP - 52));
        setScroll((int)Math.round((mouseY - trackTop - s(8)) * maximumScroll() / travel));
    }
    private boolean overTooltip(double mouseX, double mouseY) {
        return tooltip != null && mouseX >= tooltip.x() - 4 && mouseX < tooltip.x() + tooltip.width() + 4
                && mouseY >= tooltip.y() - 4 && mouseY < tooltip.y() + tooltip.height() + 4;
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean twice) {
        if (!editable() || overTooltip(event.x(), event.y())) return true;
        if (event.button() != MOUSE_BUTTON_LEFT) return true;
        if (maximumScroll() > 0 && event.x() >= x(505) && event.x() < x(523) && event.y() >= y(LIST_TOP) && event.y() < y(LIST_BOTTOM)) {
            if (event.y() < y(LIST_TOP + 18)) setScroll(scroll - ROW_HEIGHT);
            else if (event.y() >= y(LIST_BOTTOM - 18)) setScroll(scroll + ROW_HEIGHT);
            else { draggingScroll = true; moveScroll(event.y()); }
            return true;
        }
        if (event.x() >= x(8) && event.x() < x(519) && event.y() >= y(8) && event.y() < y(67)) {
            draggingWindow = true;
            dragX = event.x();
            dragY = event.y();
            return true;
        }
        pressed = children().stream().filter(VolunteerButton.class::isInstance).map(VolunteerButton.class::cast)
                .filter(button -> button.active && Tooltips.contains(button, event.x(), event.y())).findFirst().orElse(null);
        super.mouseClicked(event, twice);
        return true;
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!editable() || event.button() != MOUSE_BUTTON_LEFT) return true;
        if (draggingScroll) { moveScroll(event.y()); return true; }
        if (draggingWindow) {
            offsetX += (int)Math.round(event.x() - dragX);
            offsetY += (int)Math.round(event.y() - dragY);
            dragX = event.x();
            dragY = event.y();
            rebuildWidgets();
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        draggingWindow = false;
        pressed = null;
        super.mouseReleased(event);
        return true;
    }
    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (overTooltip(mx, my)) tooltipScroll = Math.clamp(tooltipScroll - (int)Math.round(vertical * 3 * (font.lineHeight + 1)), 0, tooltip.maxScroll());
        else if (editable() && mx >= x(13) && mx < x(523) && my >= y(LIST_TOP) && my < y(LIST_BOTTOM)) setScroll(scroll - (int)Math.round(vertical * ROW_HEIGHT));
        return true;
    }
    private static String asset(String path) {
        return path.startsWith("hoi:textures/gui/") && path.endsWith(".png") ? path.substring(17, path.length() - 4) : path;
    }
    private void text(GuiGraphicsExtractor g, String text, int x, int y, int width, int height, int color) {
        UiText.centeredSingleLine(g, font, Tooltips.officerText(text.replace("§T", "§W")), x, y, width, height, color);
    }
    private void paragraph(GuiGraphicsExtractor g, String text, int x, int y, int width, int height) {
        float naturalScale = Math.min(1f, (float)(18 * scale / font.lineHeight));
        var lines = Tooltips.sourceLines(font, text.replace("§T", "§W"), Math.max(1, (int)(width / naturalScale)));
        if (lines.isEmpty()) return;
        float fitted = Math.min(naturalScale, height / (float)Math.max(1, lines.size() * (font.lineHeight + 1)));
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(fitted);
        for (int index = 0; index < lines.size(); index++) g.text(font, lines.get(index), Math.round((width / fitted - font.width(lines.get(index))) / 2), index * (font.lineHeight + 1), 0xFFFFFFFF);
        g.pose().popMatrix();
    }
    private void row(GuiGraphicsExtractor g, DiplomacyVolunteerView.ArmyRow row, int rowY) {
        int rowX = x(13), rowTop = y(rowY);
        UiAssets.draw(g, "diplomacy/volunteers/row/frame_" + (view.selectedArmyKeys().contains(row.key()) ? 1 : 0), rowX, rowTop, s(489), s(38));
        if (row.insignia() != null && row.color() != null) UiAssets.drawTinted(g, "diplomacy/volunteers/shield/frame_" + row.insignia(), rowX + s(5), rowTop + s(4), s(33), s(32), 0xFF000000 | row.color());
        text(g, row.divisionText(), rowX + s(5), rowTop + s(4), s(33), s(32), 0xFFFFFFFF);
        if (!row.commanderSkillText().isEmpty()) {
            UiAssets.draw(g, "diplomacy/volunteers/skill", rowX + s(43), rowTop + s(2), s(25), s(35));
            text(g, row.commanderSkillText(), rowX + s(43), rowTop + s(2), s(25), s(35), 0xFFFFFFFF);
        }
        g.pose().pushMatrix();
        g.pose().translate((float)(rowX + (83 - 156 * .17 / 2) * scale), (float)(rowTop + (19 - 210 * .17 / 2) * scale));
        g.pose().scale((float)(.17 * scale));
        UiAssets.draw(g, asset(row.commanderPortrait()), 0, 0, 156, 210);
        g.pose().popMatrix();
        text(g, row.name(), rowX + s(106), rowTop + s(2), s(178), s(17), 0xFFFFFFFF);
        text(g, row.commanderName(), rowX + s(106), rowTop + s(16), s(178), s(19), 0xFFFFFFFF);
        double step = Math.min(31, 171.0 / Math.max(1, row.unitCounts().size() - 1));
        for (int index = 0; index < row.unitCounts().size(); index++) {
            var unit = row.unitCounts().get(index);
            int itemX = rowX + s(284) + (int)Math.round(index * step * scale);
            UiAssets.draw(g, asset(unit.icon()), itemX + s(1), rowTop + s(7), s(30), s(12));
            text(g, unit.text(), itemX, rowTop + s(16), s(31), s(16), 0xFFFFFFFF);
        }
    }
    private void scrollbar(GuiGraphicsExtractor g) {
        if (maximumScroll() == 0) return;
        String path = "commander/commands/";
        int x = x(505), top = y(LIST_TOP), height = s(LIST_BOTTOM - LIST_TOP);
        assets.tile(g, path + "scrollbar_vertical_bg/frame_0", x, top, s(18), height, 0, 52, scale);
        assets.tile(g, path + "scroll_track/frame_0", x + s(3), top + s(18), s(12), height - s(36), 4, 4, scale);
        UiAssets.draw(g, path + "scroll_up/frame_0", x + s(2), top + s(3), s(16), s(16));
        UiAssets.draw(g, path + "scroll_down/frame_0", x + s(1), top + height - s(19), s(16), s(16));
        int travel = Math.max(1, height - s(52));
        UiAssets.draw(g, path + "scroll_drager/frame_0", x, top + s(18) + (int)Math.round(travel * scroll / (double)maximumScroll()), s(16), s(16));
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (!contextValid() || closed) return;
        assets.beginFrame();
        if (parent != null) { parent.extractRenderState(g, -1, -1, delta); g.nextStratum(); }
        UiAssets.draw(g, "diplomacy/volunteers/background", left, top, s(527), s(612));
        UiText.centeredSingleLine(g, font, Tooltips.officerText(view.title(), true), x(-3), y(29), s(530), s(20), 0xFFFFFFFF);
        UiAssets.draw(g, "diplomacy/volunteers/time/frame_0", x(60), y(80), s(46), s(46));
        text(g, view.transferText(), x(110), y(91), s(100), s(24), 0xFFFFFFFF);
        text(g, label("AIR_VOLUNTEERS"), x(370), y(91), s(150), s(24), 0xFFFFFFFF);
        assets.tile(g, "diplomacy/dialog/panel_border", x(0), y(121), s(527), s(216), 64, 64, scale);
        String hovered = "";
        g.enableScissor(x(13), y(LIST_TOP), x(503), y(LIST_BOTTOM));
        if (!view.unassignedText().isEmpty()) {
            UiAssets.draw(g, "diplomacy/volunteers/row/frame_0", x(13), y(LIST_TOP - scroll), s(489), s(38));
            text(g, view.unassignedText(), x(26), y(LIST_TOP + 9 - scroll), s(470), s(19), 0xFFFFFFFF);
        }
        for (int index = 0; index < view.armies().size(); index++) {
            var army = view.armies().get(index);
            int rowY = rowY(index);
            if (rowY + ROW_HEIGHT <= LIST_TOP || rowY >= LIST_BOTTOM) continue;
            row(g, army, rowY);
            if (mx >= x(52) && mx < x(297) && my >= y(Math.max(rowY, LIST_TOP)) && my < y(Math.min(rowY + ROW_HEIGHT, LIST_BOTTOM))) hovered = army.commanderTooltip();
        }
        g.disableScissor();
        String description = view.summaryText() + (view.capText().isEmpty() ? "" : "\n" + view.capText()) + view.threatText() + view.sourceDescription();
        paragraph(g, description, x(28), y(342), s(470), s(202));
        scrollbar(g);
        boolean over = overTooltip(mx, my);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(g, over ? -1 : mx, over ? -1 : my, delta);
        if (over) hovered = tooltipText;
        else if (hovered.isEmpty()) for (var entry : tips.entrySet()) if (Tooltips.contains(entry.getKey(), mx, my)) { hovered = entry.getValue(); break; }
        if (hovered.isEmpty()) { tooltip = null; tooltipText = ""; tooltipScroll = 0; }
        else {
            if (!hovered.equals(tooltipText)) { tooltipText = hovered; tooltipScroll = 0; }
            tooltip = Tooltips.drawSourceScrollable(g, font, tooltipText.replace("§T", "§W"), mx, my, width, height, tooltipScroll);
            tooltipScroll = tooltip.scroll();
        }
    }
    private final class VolunteerButton extends Button {
        private final String texture, sound;
        private final boolean icon;
        VolunteerButton(String text, String texture, int x, int y, int width, int height, String sound, boolean icon, Runnable action) {
            super(x, y, width, height, Component.literal(text), button -> action.run(), DEFAULT_NARRATION);
            this.texture = texture;
            this.sound = sound;
            this.icon = icon;
        }
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) { AudioUiSounds.play(sound); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
            if (!texture.isEmpty()) {
                var effect = icon ? UiDiplomacyAssets.Effect.NONE : UiDiplomacyAssets.Effect.BUTTON_STATE;
                var state = !active ? UiDiplomacyAssets.State.DISABLED : pressed == this ? UiDiplomacyAssets.State.DOWN
                        : Tooltips.contains(this, mx, my) ? UiDiplomacyAssets.State.OVER : UiDiplomacyAssets.State.UP;
                assets.button(g, texture + ":" + getMessage().getString(), texture, getX(), getY(), getWidth(), getHeight(), effect, state);
            }
            if (!icon) text(g, getMessage().getString(), getX() + 1, getY(), getWidth() - 2, getHeight(), 0xFFFFFFFF);
        }
    }
}
