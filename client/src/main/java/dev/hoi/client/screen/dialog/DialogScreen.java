package dev.hoi.client.screen.dialog;

import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.client.screen.decision.DecisionText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public class DialogScreen extends Screen {
    static final float EVENT_TITLE_SCALE = 1.5f;
    private DialogView view;
    private final java.util.function.BiConsumer<DialogView, String> send;
    private boolean pending, textAssets;
    boolean stacked;
    Runnable minimize;
    private final Map<UiMenuButton, DialogView.Choice> choiceTooltips = new LinkedHashMap<>();
    private final DecisionText tooltipText = new DecisionText();
    private dev.hoi.client.ui.tooltip.Tooltips.ScrolledTooltip tooltipBounds;
    private String hoveredChoice = "";
    private int tooltipScroll;
    private int left, top, pane, panelHeight, bodyTop, bodyBottom, scroll, total, choicePage, quoteViewport;
    private double frameScale;
    private int bodyLeft, bodyWidth, bottomTop, middleHeight, newsShift;
    private int[] eventPicture = new int[4], eventHeading = new int[4];
    private List<String> displayedEventChoices = List.of();
    public DialogScreen(DialogView view) { this(view, DialogCoordinator::choose); }
    public DialogScreen(DialogView view, java.util.function.BiConsumer<DialogView, String> send) { super(Component.literal(view.title())); this.view = view; this.send = send; }
    public DialogView view() { return view; }
    boolean minimizable() { return !view.choices().isEmpty() && (view.kind() == DialogView.Kind.EVENT || view.kind() == DialogView.Kind.GLOBAL_EVENT); }
    public dev.hoi.client.ui.tooltip.Tooltips.ScrolledTooltip tooltipBounds() { return tooltipBounds; }
    void resetChoiceTooltips() {
        choiceTooltips.clear(); tooltipBounds = null; tooltipText.clear();
        if (!textAssets && minecraft != null) { tooltipText.load(minecraft.getResourceManager()); textAssets = true; }
    }
    void choiceTooltip(UiMenuButton button, DialogView.Choice choice) {
        button.setTooltip(null);
        button.active = button.active && !pending;
        choiceTooltips.put(button, choice);
    }
    void minimizeButton(int x, int y, int size) {
        if (!minimizable()) return;
        var button = new UiMenuButton("최소화", "events/leader/minimize", false, x, y, size, size, this::onClose).background("none");
        button.setTooltip(dev.hoi.client.ui.tooltip.Tooltips.createOfficer("§G클릭§!하여 최소화합니다. 사건은 §H사건과 결정§! 아래 사건 목록에 배치됩니다."));
        addRenderableWidget(button);
    }
    void renderChoiceTooltip(GuiGraphicsExtractor g, int mx, int my) {
        String id = "", text = "";
        for (var entry : choiceTooltips.entrySet()) {
            var button = entry.getKey();
            if (!dev.hoi.client.ui.tooltip.Tooltips.contains(button, mx, my)) continue;
            var choice = entry.getValue();
            id = choice.id(); text = choice.tooltip();
            float scale = Math.max(dev.hoi.client.ui.UiText.scale(font), textScale() + 1f / font.lineHeight);
            if (text.isEmpty() && font.width(choice.label()) * scale > button.getWidth() - 6
                    && dev.hoi.client.ui.UiMenuButton.labelHeight(font, choice.label(), button.getWidth(), textScale(), boldEvent()) > button.getHeight()) text = choice.label();
            break;
        }
        if (!id.equals(hoveredChoice)) { hoveredChoice = id; tooltipScroll = 0; }
        tooltipBounds = text.isEmpty() ? null : tooltipText.tooltip(g, font, text, mx, my, width, height, tooltipScroll);
    }
    boolean scrollChoiceTooltip(double x, double y, double vertical) {
        if (tooltipBounds == null || tooltipBounds.maxScroll() == 0) return false;
        boolean over = x >= tooltipBounds.x() - 4 && x < tooltipBounds.x() + tooltipBounds.width() + 4
                && y >= tooltipBounds.y() - 4 && y < tooltipBounds.y() + tooltipBounds.height() + 4;
        if (!over && choiceTooltips.keySet().stream().noneMatch(b -> dev.hoi.client.ui.tooltip.Tooltips.contains(b, x, y))) return false;
        tooltipScroll = Math.clamp(tooltipScroll - (int)(vertical * (font.lineHeight + 1) * 3), 0, tooltipBounds.maxScroll());
        return true;
    }
    public int panelTop() { return top; }
    public int panelLeft() { return left; }
    public int panelWidth() { return pane; }
    public int panelHeight() { return panelHeight; }
    int bodyViewportHeight() { return bodyBottom - bodyTop; }
    public int scrollOffset() { return scroll; }
    public int bodyContentHeight() { return total; }
    record BodyRow(net.minecraft.util.FormattedCharSequence line, String image, int width, int height) {
        int rowHeight() { return image == null ? 12 : (height + 11) / 12 * 12; }
    }
    private static final java.util.regex.Pattern EVENT_TEXTICON = java.util.regex.Pattern.compile("(?:§.)*\\h*£(?:GFX_)?([A-Za-z0-9_]+_event_texticon(?:_[0-9]+)?)£?\\h*(?:§.)*");
    List<BodyRow> bodyRows() {
        int wrap = Math.max(1, (int)(bodyWidth / textScale()));
        var rows = new ArrayList<BodyRow>(); var text = new ArrayList<String>();
        for (String line : view.body().split("\\R", -1)) {
            var match = EVENT_TEXTICON.matcher(line);
            String image = match.matches() ? "keywords/source/" + match.group(1).toLowerCase(java.util.Locale.ROOT) : null;
            int[] size = image == null ? new int[2] : UiAssets.imageSize(image);
            if (size[0] <= 0 || size[1] <= 0) { text.add(line); continue; }
            addBodyText(rows, text, wrap);
            if (!rows.isEmpty()) rows.add(new BodyRow(net.minecraft.util.FormattedCharSequence.EMPTY, null, 0, 0));
            int width = Math.min(bodyWidth, units(size[0]));
            rows.add(new BodyRow(null, image, width, Math.max(1, Math.round(width * (float)size[1] / size[0]))));
        }
        addBodyText(rows, text, wrap);
        return rows;
    }
    private void addBodyText(List<BodyRow> rows, List<String> text, int wrap) {
        var lines = dev.hoi.client.ui.tooltip.Tooltips.split(font, tooltipText.component(String.join("\n", text)).copy().withStyle(net.minecraft.ChatFormatting.BOLD), wrap);
        text.clear();
        if (lines.isEmpty()) return;
        if (!rows.isEmpty() && rows.getLast().image() != null) rows.add(new BodyRow(net.minecraft.util.FormattedCharSequence.EMPTY, null, 0, 0));
        for (var line : lines) rows.add(new BodyRow(line, null, 0, 0));
    }
    public void update(DialogView next) {
        if (!view.token().equals(next.token()) || next.revision() < view.revision()) return;
        if (next.revision() > view.revision()) pending = false;
        view = next; rebuildWidgets();
    }
    private boolean paper() { return view.kind() == DialogView.Kind.EVENT || view.kind() == DialogView.Kind.GLOBAL_EVENT; }
    private boolean sourceEvent() { return paper() || view.kind() == DialogView.Kind.SUPER_EVENT; }
    private int units(double value) { return (int)Math.round(value * frameScale); }
    int[] eventPictureBounds() { return eventPicture.clone(); }
    int[] eventHeadingBounds() { return eventHeading.clone(); }
    int[] eventBodyBounds() { return new int[]{bodyLeft, bodyTop, bodyWidth, bodyBottom - bodyTop}; }
    double frameScale() { return frameScale; }
    List<String> displayedEventChoices() { return displayedEventChoices; }
    float textScale() { return dev.hoi.client.ui.UiText.scale(font); }
    float eventTitleRatio() { return EVENT_TITLE_SCALE; }
    float eventTitleScale() { return textScale() * eventTitleRatio(); }
    private int eventTitlePitch() { return (int)Math.ceil(font.lineHeight * eventTitleScale()); }
    private List<net.minecraft.network.chat.FormattedText> eventTitleRows(int width) {
        var title = tooltipText.component(view().title()).copy().withStyle(net.minecraft.ChatFormatting.BOLD);
        return font.getSplitter().splitLines(title, Math.max(1, (int)(width / eventTitleScale())), net.minecraft.network.chat.Style.EMPTY);
    }
    private float eventTitleBlock(int lines) { return (Math.max(1, lines) - 1) * eventTitlePitch() + font.lineHeight * eventTitleScale(); }
    int eventTitleLines(int width) { return Math.max(1, eventTitleRows(width).size()); }
    int eventTitleHeight(int width) { return (int)Math.ceil(eventTitleBlock(eventTitleLines(width))); }
    void eventTitle(GuiGraphicsExtractor g, int x, int y, int width, int height, int color, boolean shadow) {
        float scale = eventTitleScale();
        int pitch = eventTitlePitch(), available = Math.max(1, (int)(width / scale));
        var rows = new ArrayList<>(eventTitleRows(width));
        int fit = Math.max(1, 1 + (int)Math.floor((height - font.lineHeight * scale) / pitch));
        if (rows.size() > fit) {
            var marker = Component.literal("…").withStyle(net.minecraft.ChatFormatting.BOLD);
            rows.set(fit - 1, net.minecraft.network.chat.FormattedText.composite(font.substrByWidth(rows.get(fit - 1), Math.max(0, available - font.width(marker))), marker));
            rows.subList(fit, rows.size()).clear();
        }
        int top = Math.round(y + (height - eventTitleBlock(rows.size())) / 2);
        for (int i = 0; i < rows.size(); i++) {
            var line = net.minecraft.locale.Language.getInstance().getVisualOrder(rows.get(i));
            g.pose().pushMatrix();
            g.pose().translate(Math.round(x + (width - font.width(line) * scale) / 2), top + i * pitch);
            g.pose().scale(scale);
            g.text(font, line, 0, 0, color, shadow);
            g.pose().popMatrix();
        }
    }
    boolean boldEvent() { return sourceEvent() || view.leaderEvent(); }
    public Component bodyText() {
        Component body = sourceEvent() ? tooltipText.component(view.body())
                : dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(dev.hoi.client.ui.tooltip.Tooltips.sourceSpacing(Component.literal(view.body())),false);
        return boldEvent() ? body.copy().withStyle(net.minecraft.ChatFormatting.BOLD) : body;
    }
    @Override protected void init() {
        resetChoiceTooltips();
        if (sourceEvent()) { initSourceEvent(); return; }
        pane = Math.min(width - 16, view.kind() == DialogView.Kind.STATE ? 440 : 450);
        panelHeight = Math.min(height - 16, 530);
        left = (width - pane) / 2; top = (height - panelHeight) / 2;
        int cell = (pane - 28) / 2;
        int choiceHeight = view.kind() == DialogView.Kind.EVENT ? Math.max(20, view.choices().stream().mapToInt(c -> UiMenuButton.labelHeight(font, c.label(), cell, textScale())).max().orElse(0)) : 20;
        int rowHeight = choiceHeight + 4;
        int capacity = 2 * Math.clamp((panelHeight - 60 - 40 - 34) / rowHeight, 1, 3);
        int shown = Math.min(capacity, view.choices().size()), rows = (shown + 1) / 2;
        int footer = shown == 0 ? 30 : rows * rowHeight + 10 + (view.choices().size() > capacity ? 24 : 0);
        bodyTop = top + 60; bodyBottom = top + panelHeight - footer;
        addRenderableWidget(new UiMenuButton("×", left + pane - 26, top + 5, 20, 20, this::onClose));
        int pages = Math.max(1, (view.choices().size() + capacity - 1) / capacity); choicePage = Math.clamp(choicePage, 0, pages - 1);
        if (shown == 0) addRenderableWidget(new UiMenuButton("닫기", left + pane / 2 - 44, bodyBottom + 5, 88, 20, this::onClose));
        else for (int i = 0; i < shown; i++) {
            int index = choicePage * capacity + i; if (index >= view.choices().size()) break;
            var choice = view.choices().get(index);
            int bx = shown == 1 ? left + (pane - cell) / 2 : left + 12 + (i % 2) * (cell + 4);
            var button = new UiMenuButton(choice.label(), bx, bodyBottom + 5 + (i / 2) * rowHeight,
                    cell, choiceHeight, () -> choose(choice.id()));
            if (view.kind() == DialogView.Kind.EVENT) button.wrapLabel();
            addRenderableWidget(button);
        }
        if (pages > 1) {
            int y = top + panelHeight - 25;
            var prev = new UiMenuButton("이전 선택지", left + 12, y, 100, 20, () -> { choicePage--; rebuildWidgets(); }); prev.active = choicePage > 0; addRenderableWidget(prev);
            var next = new UiMenuButton("다음 선택지", left + pane - 112, y, 100, 20, () -> { choicePage++; rebuildWidgets(); }); next.active = choicePage + 1 < pages; addRenderableWidget(next);
        }
        scroll = Math.clamp(scroll, 0, Math.max(0, total - (bodyBottom - bodyTop)));
        for (var child : children()) if (child instanceof UiMenuButton button) {
            button.textScale(textScale());
            if (view.kind() == DialogView.Kind.SUPER_EVENT && button.getY() >= bodyBottom) button.setTooltip(null);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (!stacked) g.fill(0, 0, width, height, 0x70000000);
        if (sourceEvent()) { renderSourceEvent(g, mx, my, delta); return; }
        UiMenuStyle.panel(g, left, top, pane, panelHeight);
        String caption = switch (view.kind()) {
            case STATE -> "주 정보"; case DIPLOMACY -> "외교 알림"; case EVENT -> "국가 이벤트 · 자국";
            case GLOBAL_EVENT -> "국제 뉴스 · 전 세계"; case SUPER_EVENT -> "SUPER EVENT · 전 세계"; case POLITICS -> "정치"; case COMMANDER -> "지휘관";
        };
        UiMenuStyle.heading(g, font, caption, left + 12, top + 11, pane - 48, UiMenuStyle.TEXT);
        int ink = paper() ? 0xff29212b : UiMenuStyle.TEXT;
        int start = left + 12;
        if (!view.flag().isEmpty() && UiAssets.draw(g, view.flag(), start, top + 34, 27, 18)) start += 35;
        if (paper()) {
            int titleWidth = left + pane - 14 - start;
            int titleTop = top + Math.min(32, 48 - (int)Math.ceil(font.lineHeight * eventTitleScale()));
            eventTitle(g, start, titleTop, titleWidth, 18, ink, false);
        }
        else text(g, net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(dev.hoi.client.ui.tooltip.Tooltips.officerText(view.title(), true), (int)((left + pane - 14 - start) / textScale()))), start, top + 35, ink);
        text(g, trim(view.subtitle(), (int)((pane - 28) / textScale())).getVisualOrderText(), left + 12, top + 48, paper() ? 0xff62566a : UiMenuStyle.MUTED);
        g.enableScissor(left + 8, bodyTop, left + pane - 8, bodyBottom);
        int y = bodyTop + 5 - scroll;
        if (view.kind() != DialogView.Kind.EVENT) y = image(g, y);
        if (view.kind() != DialogView.Kind.STATE) {
            for (var line : dev.hoi.client.ui.tooltip.Tooltips.split(font,bodyText(), (int)((pane - 32) / textScale()))) { text(g, line, left + 15, y, ink); y += paper() ? 12 : 13; }
            if (!view.body().isEmpty()) y += 10;
        }
        if (view.kind() == DialogView.Kind.EVENT) y = image(g, y);
        if (view.kind() == DialogView.Kind.STATE) {
            for (int i = 0; i < view.tiles().size(); i++) {
                var tile = view.tiles().get(i); int cell = (pane - 32) / 3;
                int x = left + 12 + (i % 3) * (cell + 4), ty = y + (i / 3) * 59;
                UiMenuStyle.recess(g, x, ty, cell, 55);
                UiAssets.draw(g, tile.icon(), x + 5, ty + 5, 25, 23);
                g.text(font, trim(tile.name(), cell - 10), x + 5, ty + 31, UiMenuStyle.TEXT);
                g.text(font, trim(tile.value(), cell - 39), x + 35, ty + 12, UiMenuStyle.ACCENT);
            }
            y += ((view.tiles().size() + 2) / 3) * 59;
            y += 8;
            for (var line : dev.hoi.client.ui.tooltip.Tooltips.split(font,Component.literal(view.body()), pane - 32)) { g.text(font, line, left + 15, y, ink); y += 13; }
        } else for (var tile : view.tiles()) {
            text(g, trim(tile.name() + (tile.value().isEmpty() ? "" : " · " + tile.value()), (int)((pane - 32) / textScale())).getVisualOrderText(), left + 15, y, ink); y += paper() ? 15 : 17;
            for (var line : dev.hoi.client.ui.tooltip.Tooltips.split(font,Component.literal(tile.detail()), (int)((pane - 40) / textScale()))) { text(g, line, left + 19, y, ink); y += paper() ? 12 : 13; }
            y += 15;
        }
        total = y + scroll - bodyTop;
        g.disableScissor();
        int available = bodyBottom - bodyTop;
        if (total > available) {
            int thumb = Math.max(12, available * available / total);
            int sy = bodyTop + (available - thumb) * scroll / Math.max(1, total - available);
            g.fill(left + pane - 6, sy, left + pane - 3, sy + thumb, 0xffa876b6);
        }
        super.extractRenderState(g, mx, my, delta);
    }
    private void initSourceEvent() {
        boolean news = view.kind() == DialogView.Kind.GLOBAL_EVENT;
        boolean wide = view.kind() == DialogView.Kind.SUPER_EVENT;
        int nativeWidth = wide ? 1001 : news ? 713 : 605;
        int nativeHeight = wide ? 639 : news ? 935 : 673;
        double screenScale = Math.min(width / 1280.0, height / 720.0);
        frameScale = Math.min(Math.min(width - 16, Math.max(wide ? 320 : 240, nativeWidth * .5 * screenScale)) / nativeWidth,
                (height - 16.0 - (wide || news ? 0 : 48)) / nativeHeight);
        frameScale = Math.max(.05, frameScale);
        pane = units(nativeWidth);
        int choiceWidth = units(wide ? 300 : 352);
        int choiceHeight = Math.max(units(wide ? 35 : 48), view.choices().stream()
                .mapToInt(c -> UiMenuButton.labelHeight(font, c.label(), choiceWidth, textScale(), true)).max().orElse(0));
        int slotHeight = Math.max(units(news ? 47 : 45), choiceHeight + (choiceHeight > units(48) ? 2 : 0));
        int capacity = wide ? 1 : 3;
        if (!wide && !news) capacity = Math.clamp((height - 16 - units(123 + 250 + 70) - 48) / Math.max(1, slotHeight), 1, 3);
        int pages = Math.max(1, (view.choices().size() + capacity - 1) / capacity);
        choicePage = Math.clamp(choicePage, 0, pages - 1);
        int shown = Math.min(capacity, Math.max(0, view.choices().size() - choicePage * capacity));
        displayedEventChoices = view.choices().stream().skip((long)choicePage * capacity).limit(shown).map(DialogView.Choice::id).toList();
        int optionsHeight = shown * slotHeight + (pages > 1 ? 24 : 0);
        int[] original = UiAssets.eventImageSize(view.image());
        double imageScale = original[0] > 0 && original[1] > 0 ? Math.min(1,
                Math.min((wide ? 1001.0 : news ? 400.0 : 526.0) / original[0], (wide ? 609.0 : news ? 150.0 : 300.0) / original[1])) : 0;
        int imageWidth = units(original[0] * imageScale), imageHeight = units(original[1] * imageScale);
        bodyWidth = units(wide ? 500 : news ? 560 : 526);
        total = bodyRows().stream().mapToInt(BodyRow::rowHeight).sum();
        int headingWidth = units(wide ? 490 : news ? 460 : 551), titleHeight = eventTitleHeight(headingWidth), oneLine = (int)Math.ceil(eventTitleBlock(1));
        int headingTop = units(wide ? 20 : news ? 106 : 70), headingHeight = Math.max(units(wide ? 20 : 32), titleHeight);
        if (wide && titleHeight > oneLine) { headingTop = units(5); headingHeight = units(60); }
        newsShift = news ? headingHeight - Math.max(units(32), oneLine) : 0;
        int optionTop;
        int imageTop;
        if (wide) {
            panelHeight = units(639);
            bodyTop = units(435);
            bodyBottom = bodyTop + Math.max(12, units(100) / 12 * 12);
            optionTop = units(585);
            imageTop = units(30);
        } else if (news) {
            panelHeight = units(935);
            bodyTop = units(325) + newsShift;
            bodyBottom = bodyTop + Math.max(12, Math.min(total, Math.min(units(475), units(800) - optionsHeight - units(18) - bodyTop)) / 12 * 12);
            optionTop = bodyBottom + units(18);
            imageTop = units(164) + newsShift;
        } else {
            imageTop = Math.max(units(130), optionsHeight - units(15) + units(12));
            int bottomHeight = Math.max(units(450), imageTop + Math.max(imageHeight, units(250)) + units(70));
            bodyTop = headingTop + headingHeight + 12;
            int bodyHeight = Math.max(12, Math.min((Math.max(units(70), total) + 11) / 12 * 12,
                    (height - 16 - bodyTop - units(43) - bottomHeight) / 12 * 12));
            bodyBottom = bodyTop + bodyHeight;
            bottomTop = bodyBottom + units(43);
            middleHeight = bottomTop - units(123);
            optionTop = bottomTop - units(15);
            imageTop += bottomTop;
            panelHeight = bottomTop + bottomHeight;
        }
        int[] superOffset = wide ? UiAssets.eventSuperOffset(view.image()) : new int[2];
        left = Math.clamp(width / 2 + units(wide ? superOffset[0] : news ? -385 : -360), 8, Math.max(8, width - pane - 8));
        top = Math.clamp(wide ? height / 2 + units(superOffset[1]) : (height - panelHeight) / 2, 8, Math.max(8, height - panelHeight - 8));
        bodyLeft = left + units(wide ? 250 : news ? 75 : 40);
        bodyTop += top;
        bodyBottom += top;
        bottomTop += top;
        eventHeading = new int[]{left + units(wide ? 260 : news ? 120 : 22), top + headingTop, headingWidth, headingHeight};
        eventPicture = new int[]{left + units(wide ? 0 : news ? 150 : 49), top + imageTop, imageWidth, imageHeight};
        scroll = Math.clamp(scroll, 0, Math.max(0, total - bodyViewportHeight()));
        minimizeButton(left + units(news ? 45 : 35), top + units(news ? 20 : 45), units(33));
        for (int i = 0; i < shown; i++) {
            var choice = view.choices().get(choicePage * capacity + i);
            var button = new UiMenuButton(choice.label(), left + units(wide ? 350 : news ? 180 : 115), top + optionTop + i * slotHeight,
                    choiceWidth, choiceHeight, () -> choose(choice.id())).background(wide ? "events/frame/spacebar" : "events/leader/choice").textShadow(false).wrapLabel().bold();
            button.textScale(textScale());
            button.sound(wide ? "ui.confirm" : "ui.click");
            if (wide) button.setTooltip(null);
            else choiceTooltip(button, choice);
            addRenderableWidget(button);
        }
        if (pages > 1) {
            int x = left + units(wide ? 350 : news ? 180 : 115), y = top + optionTop + shown * slotHeight;
            int controlWidth = Math.min(80, (choiceWidth - 4) / 2);
            var previous = new UiMenuButton("이전 선택지", x, y, controlWidth, 20, () -> { choicePage--; rebuildWidgets(); });
            previous.active = choicePage > 0;
            addRenderableWidget(previous);
            var next = new UiMenuButton("다음 선택지", x + choiceWidth - controlWidth, y, controlWidth, 20, () -> { choicePage++; rebuildWidgets(); });
            next.active = choicePage + 1 < pages;
            addRenderableWidget(next);
        }
    }
    private void renderSourceEvent(GuiGraphicsExtractor g, int mx, int my, float delta) {
        boolean wide = view.kind() == DialogView.Kind.SUPER_EVENT;
        boolean news = view.kind() == DialogView.Kind.GLOBAL_EVENT;
        if (wide) {
            UiAssets.draw(g, view.image(), eventPicture[0], eventPicture[1], eventPicture[2], eventPicture[3]);
            UiAssets.draw(g, "events/frame/super", left, top, pane, panelHeight);
        } else if (news) {
            UiAssets.draw(g, "events/frame/news", left, top, pane, panelHeight);
            UiAssets.draw(g, view.image(), eventPicture[0], eventPicture[1], eventPicture[2], eventPicture[3]);
            UiAssets.draw(g, "events/frame/news_overlay", left + units(143), top + units(160) + newsShift, units(415), units(155));
        } else {
            UiAssets.draw(g, "events/leader/top", left, top, pane, units(123));
            tileEventMiddle(g);
            UiAssets.nineSlice(g, "events/frame/national_bottom", left, bottomTop, pane, panelHeight - bottomTop + top, 16, frameScale);
            UiAssets.draw(g, view.image(), eventPicture[0], eventPicture[1], eventPicture[2], eventPicture[3]);
        }
        eventTitle(g, eventHeading[0], eventHeading[1], eventHeading[2], eventHeading[3], wide ? UiMenuStyle.TEXT : 0xff000000, false);
        g.enableScissor(bodyLeft, bodyTop, bodyLeft + bodyWidth, bodyBottom);
        int y = bodyTop - scroll;
        for (var row : bodyRows()) {
            if (row.image() != null) UiAssets.draw(g, row.image(), bodyLeft + (bodyWidth - row.width()) / 2, y, row.width(), row.height());
            else dev.hoi.client.ui.UiText.text(g, font, row.line(), wide ? bodyLeft + (bodyWidth - (int)(font.width(row.line()) * textScale())) / 2 : bodyLeft,
                    y, wide ? UiMenuStyle.TEXT : 0xff000000, false);
            y += row.rowHeight();
        }
        g.disableScissor();
        if (total > bodyViewportHeight()) {
            int available = bodyViewportHeight();
            int thumb = Math.max(6, available * available / total);
            int yThumb = bodyTop + (available - thumb) * scroll / (total - available);
            g.fill(bodyLeft + bodyWidth + 2, bodyTop, bodyLeft + bodyWidth + 4, bodyBottom, 0x5062566a);
            g.fill(bodyLeft + bodyWidth + 2, yThumb, bodyLeft + bodyWidth + 4, yThumb + thumb, 0xffa876b6);
        }
        for (var child : children()) if (child instanceof UiMenuButton button) button.extractRenderState(g, mx, my, delta);
        if (!wide) renderChoiceTooltip(g, mx, my);
    }
    private void tileEventMiddle(GuiGraphicsExtractor g) {
        var id = net.minecraft.resources.Identifier.parse("hoi:textures/gui/events/leader/middle.png");
        int middleTop = top + units(123);
        int edge = Math.min(middleHeight / 2, units(16));
        g.blit(id, left, middleTop, left + pane, middleTop + edge, 0, 1, 0, .2f);
        int tile = Math.max(1, units(48));
        for (int offset = edge; offset < middleHeight - edge; offset += tile) {
            int part = Math.min(tile, middleHeight - edge - offset);
            g.blit(id, left, middleTop + offset, left + pane, middleTop + offset + part, 0, 1, .2f, .2f + .6f * part / tile);
        }
        g.blit(id, left, middleTop + middleHeight - edge, left + pane, middleTop + middleHeight, 0, 1, .8f, 1);
    }
    private int image(GuiGraphicsExtractor g, int y) {
        if (view.image().isEmpty()) return y;
        int imageWidth = paper() ? pane * 2 / 3 : pane - 32;
        int imageHeight = paper() ? Math.max(48, panelHeight / 5) : Math.min(180, Math.max(75, panelHeight * 2 / 5));
        if (UiAssets.draw(g, view.image(), left + (pane - imageWidth) / 2, y, imageWidth, imageHeight)) return y + imageHeight + 12;
        return y;
    }
    private Component trim(String text, int space) { return dev.hoi.client.ui.UiText.ellipsize(font, tooltipText.component(text), space); }
    private void text(GuiGraphicsExtractor g, net.minecraft.util.FormattedCharSequence text, int x, int y, int color) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(textScale());
        g.text(font, text, 0, 0, color, !paper()); g.pose().popMatrix();
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (scrollChoiceTooltip(x, y, vertical)) return true;
        if (sourceEvent()) {
            if (x >= bodyLeft && x <= bodyLeft + bodyWidth && y >= bodyTop && y <= bodyBottom) {
                scroll = Math.clamp(scroll - (int)Math.ceil(Math.abs(vertical) * 3) * (int)Math.signum(vertical) * 12, 0, Math.max(0, total - bodyViewportHeight()));
                return true;
            }
            return false;
        }
        if (x >= left && x <= left + pane && y >= bodyTop && y <= bodyBottom) {
            int visible = view.kind() == DialogView.Kind.SUPER_EVENT ? quoteViewport : bodyBottom - bodyTop;
            scroll = Math.clamp(scroll - (int)(vertical * 32), 0, Math.max(0, total - visible)); return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }
    protected void choose(String choice) {
        if (minimizable()) {
            if (pending || view.choices().stream().noneMatch(c -> c.id().equals(choice))) return;
            pending = true;
            choiceTooltips.keySet().forEach(button -> button.active = false);
        }
        send.accept(view, choice);
    }
    @Override public void onClose() {
        if (minimizable()) { if (minimize != null) minimize.run(); else minecraft.gui.setScreen(null); return; }
        choose(""); if (stacked) DialogCoordinator.close(view.token()); else minecraft.gui.setScreen(null);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (sourceEvent() && total > bodyViewportHeight()) {
            int maximum = total - bodyViewportHeight();
            int next = switch (event.key()) {
                case com.mojang.blaze3d.platform.InputConstants.KEY_PAGEUP -> scroll - bodyViewportHeight();
                case com.mojang.blaze3d.platform.InputConstants.KEY_PAGEDOWN -> scroll + bodyViewportHeight();
                case com.mojang.blaze3d.platform.InputConstants.KEY_HOME -> 0;
                case com.mojang.blaze3d.platform.InputConstants.KEY_END -> maximum;
                default -> -1;
            };
            if (next != -1) { scroll = Math.clamp(next, 0, maximum); return true; }
        }
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_RETURN || event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER) {
            if (view.choices().size() == 1) { choose(view.choices().getFirst().id()); return true; }
            if (view.choices().isEmpty()) { onClose(); return true; }
            if (getFocused() instanceof net.minecraft.client.gui.components.Button button && button.active)
                button.onPress(event);
            else onClose();
            return true;
        }
        return super.keyPressed(event);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
