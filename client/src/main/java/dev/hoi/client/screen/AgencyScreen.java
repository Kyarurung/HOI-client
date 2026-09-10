package dev.hoi.client.screen;

import dev.hoi.client.input.SidebarMovement;
import dev.hoi.client.HoiClient;
import dev.hoi.client.ui.HoiMenuBar;
import dev.hoi.client.ui.UiText;
import dev.hoi.client.ui.PanelButton;
import dev.hoi.client.ui.HoiMenuButton;
import dev.hoi.client.ui.HoiMenuStyle;
import dev.hoi.client.ui.HoiPanelLayout;
import dev.hoi.client.ui.UiAssets;

import dev.hoi.protocol.*;
import dev.hoi.protocol.AgencyView.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.Consumer;


public final class AgencyScreen extends Screen implements SidebarMovement.Screen {
    private static final int TEXT = 0xFFE0E3DD, GOLD = 0xFFFFAA00, MUTED = 0xFF9AABA8;
    public static final List<String> UPGRADE_ORDER = List.of("foreign_intelligence", "domestic_intelligence", "military_intelligence", "planning_and_direction", "collection", "processing_and_exploitation", "analysis", "dissemination", "humint", "sigint", "masint", "osint", "geoint", "cell_system", "communication_security", "support_services", "enhanced_interrogation_techniques", "termination", "elint", "comint", "advanced_cryptoanalytical_attack_models", "cryptosystem_algorithm_upgrade", "quantum_cryptography");
    private static final String[] UPGRADE_GROUPS = {"정보공동체", "정보 순환", "정보 수집 분야", "휴민트", "신호 정보"};
    private final Screen parent;
    private final Consumer<AgencyProtocol.Request> transport;
    private final String token;
    private AgencyView view;
    private String group = "operations", selectedId;
    private final List<String> arguments = new ArrayList<>();
    private int pane, top, scroll, detailScroll, choosing = -1, choiceScroll, pendingTicks, refreshTicks, overlayStart;
    private String searchText = "";
    private EditBox search;

    public AgencyScreen(Screen parent) {
        this(parent, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(AgencyProtocol.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public AgencyScreen(Screen parent, String token, AgencyView fixture, Consumer<AgencyProtocol.Request> transport) {
        super(Component.literal("정보기관")); this.parent = parent; this.token = token; this.view = fixture; this.transport = transport;
    }
    String session() { return token; }
    public void open() { send(AgencyProtocol.Kind.OPEN, "", List.of()); }
    public void update(AgencyView next) {
        if (!next.session().equals(token)) return;
        if (next.country().isEmpty()) { minecraft.gui.setScreen(null); return; }
        if (view != null && next.revision() < view.revision()) return;
        if (minecraft.gui.screen() instanceof AgencyOperationScreen operation && operation.backdrop() == this) operation.update(next);
        pendingTicks = 0;
        view = next;
        var item = selected();
        if (item == null) { selectedId = null; choosing = -1; }
        else normalizeArguments(item);
        rebuildWidgets();
    }
    private void normalizeArguments(Item item) {
        while (arguments.size() > item.parameters().size()) arguments.removeLast();
        for (int i = 0; i < item.parameters().size(); i++) {
            var choices = item.parameters().get(i).choices();
            if (arguments.size() <= i) arguments.add(choices.isEmpty() ? "" : choices.getFirst().value());
            String value = arguments.get(i);
            if (choices.stream().noneMatch(c -> c.value().equals(value))) arguments.set(i, choices.isEmpty() ? "" : choices.getFirst().value());
        }
    }
    @Override protected void init() {
        overlayStart = 0;
        SidebarMovement.release(minecraft);
        pane = Math.min(width - 16, Math.max(360, HoiPanelLayout.width(MenuTab.INTELLIGENCE, width)));
        top = HoiMenuBar.height(width);
        HoiMenuBar.buttons(width, view == null ? "" : view.country(), MenuTab.INTELLIGENCE, tab -> {
            if (tab == MenuTab.INTELLIGENCE) return;
            if (tab == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        addRenderableWidget(new HoiMenuButton("×", pane - 25, top + 3, 19, 19, this::onClose));
        if (view == null) return;
        String[] countLabels = {"모집된 요원", "적에게 포획된 요원", "적에게 사살당한 정보원"};
        String[] countIcons = {"total_operatives", "arrested_operatives", "dead_operatives"};
        for (int i = 0; i < countLabels.length; i++) {
            var indicator = new HoiMenuButton(countLabels[i], "agency/" + countIcons[i], false, counterX(i), top + 169, counterWidth(), 28, () -> {});
            indicator.background("none"); indicator.active = false; addRenderableWidget(indicator);
        }
        if (unestablished()) {
            var action = view.items().stream().filter(i -> i.id().equals("create") || i.id().equals("cancel_project")).findFirst().orElse(null);
            if (action != null) {
                var button = new PanelButton(action.id().equals("create") ? "정보기관 창설" : "기관 창설 취소", 10, top + 29, pane - 20, 59,
                        () -> send(AgencyProtocol.Kind.CALL, action.id(), List.of())) {
                    @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                        if (isHoveredOrFocused()) g.outline(getX(),getY(),getWidth(),getHeight(),HoiMenuStyle.ACCENT);
                    }
                };
                button.active = action.enabled() && pendingTicks == 0; addRenderableWidget(button);
            }
            for (int i=0;i<UPGRADE_GROUPS.length;i++) {
                var button=new HoiMenuButton(UPGRADE_GROUPS[i],8+i*(pane-16)/5,top+108,(pane-20)/5,28,()->{});
                button.textScale(.8f);button.active=false;addRenderableWidget(button);
            }
            for(int i=0;i<2;i++) {
                String id=i==0?"operations":"cryptology";
                var button=new HoiMenuButton(i==0?"작전":"암호학",null,group.equals(id),8+i*(pane-16)/2,top+217,(pane-20)/2,23,()->changeGroup(id));
                button.background("agency/ui/tab" + i); button.textScale(.8f);addRenderableWidget(button);
            }
            return;
        }
        if (view.items().stream().noneMatch(i -> i.id().equals("recruit"))) return;
        var master = new HoiMenuButton("세력 첩보장", "agency/spy_master", false, pane - 39, top + 40, 29, 29, () -> {});
        master.active = false;
        master.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.literal("첩보장").withStyle(style -> style.withColor(0xFFCC33))
                        .append(Component.literal("은 현재 없습니다.").withStyle(style -> style.withColor(0xFFFFFF)))));
        addRenderableWidget(master);
        for (int i = 0; i < UPGRADE_GROUPS.length; i++) {
            int category = i;
            var improveButton = new HoiMenuButton(UPGRADE_GROUPS[i], 8 + i * (pane - 16) / 5, top + 108, (pane - 20) / 5, 28, () -> {
                changeGroup("upgrades");
                var items = visibleItems();
                for (int n = 0; n < items.size(); n++) if (upgradeCategory(items.get(n)) == category) {
                    int columns = Math.max(1, Math.min(5, (upgradeWidth() - 16) / 100));
                    scroll = upgradeOffset(items, n, columns) - 16;
                    rebuildWidgets(); break;
                }
            });
            improveButton.textScale(.8f); addRenderableWidget(improveButton);
        }
        for (int i = 0; i < 2; i++) {
            String id = i == 0 ? "operations" : "cryptology", label = i == 0 ? "작전" : "암호학";
            addRenderableWidget(new HoiMenuButton(label, null, group.equals(id), 8 + i * (pane - 16) / 2, top + 217, (pane - 20) / 2, 23, () -> changeGroup(id)).background("agency/ui/tab" + i));
        }
        var recruit = view.items().stream().filter(i -> i.id().equals("recruit")).findFirst().orElseThrow();
        var recruitButton = new HoiMenuButton("정보원 모집", recruitX(), top + 190, pane - recruitX() - 10, 20, () -> selectItem(recruit));
        recruitButton.textScale(.8f); addRenderableWidget(recruitButton);
        var project = view.items().stream().filter(v -> v.id().equals("cancel_project")).findFirst().orElse(null);
        if (project != null) {
            recruitButton.setY(top + 191); recruitButton.setHeight(18);
            var cancel = new HoiMenuButton(project.title() + " · " + project.value(), recruitX(), top + 169, pane - recruitX() - 10, 20,
                    () -> send(AgencyProtocol.Kind.CALL, project.id(), List.of()));
            cancel.active = pendingTicks == 0; addRenderableWidget(cancel);
        }
        overlayStart = children().size();
        if (group.equals("upgrades")) {
            for (var child : children()) if (child instanceof net.minecraft.client.gui.components.AbstractWidget button) button.active = false;
            addRenderableWidget(new HoiMenuButton("개선 창 닫기", upgradeX() + upgradeWidth() - 26, upgradeY() + 4, 20, 19, () -> changeGroup("operations")).caption("×"));
        }
        var items = visibleItems();
        boolean grid = group.equals("upgrades");
        int x = grid ? upgradeX() + 8 : 8, y = grid ? upgradeY() + 39 : top + 245;
        int areaWidth = grid ? upgradeWidth() - 16 : pane - 16;
        int columns = grid ? Math.max(1, Math.min(5, areaWidth / 100)) : 1;
        int pitch = grid ? 72 : pane < 250 && group.equals("operations") ? 73 : 51;
        int contentHeight = grid ? (items.isEmpty() ? 0 : upgradeOffset(items, items.size() - 1, columns) + pitch) : items.size() * pitch;
        scroll = Math.clamp(scroll, 0, Math.max(0, contentHeight - (height - y - 40)));
        if (selectedId == null || !grid) for (int i = 0; i < items.size(); i++) {
            var item = items.get(i); int position = grid ? upgradePosition(items, i, columns) : i;
            int bx = x + (position % columns) * areaWidth / columns, by = y + (grid ? upgradeOffset(items, i, columns) : i * pitch) - scroll;
            if (by < y || by + pitch > height - 37) continue;
            var button = new PanelButton(item.title(), bx, by, areaWidth / columns - 4, pitch - 5, () -> {
                selectItem(item);
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    int w = getWidth(), h = getHeight();
                    if (item.group().equals("operations")) {
                        UiAssets.nineSlice(g, "agency/ui/operation_row" + (item.progress() >= 0 ? 1 : 0), bx, by, w, h, 4, .5);
                        int imageRight = bx + Math.round(w * 96f / 519) - 3;
                        int textX = imageRight + 8, flagX = bx + w - 26;
                        int actionX = bx + w - 69, actionY = by + h - 25;
                        g.enableScissor(bx + 4, by + 4, bx + w - 4, by + h - 4);
                        UiAssets.cover(g, item.texture(), bx + 5, by + 5, imageRight - bx - 5, h - 10);
                        UiText.text(g, font, trim(item.title(), (int)((flagX - textX - 6)/UiText.scale(font))), textX, by + 7, TEXT);
                        String target=item.parameters().isEmpty()?"":item.parameters().getFirst().choices().stream().findFirst().map(Choice::value).orElse("");
                        if(!target.isEmpty())UiAssets.draw(g,"country/"+target.toLowerCase(Locale.ROOT)+"/flag",flagX,by+7,16,11);
                        if(item.id().startsWith("operation:")) {
                            int requirementY = pane < 250 ? by + 24 : actionY + 2;
                            UiAssets.draw(g,"agency/ui/required",textX,requirementY,13,13);
                            UiText.text(g,font,"2",textX+14,requirementY+2,TEXT);
                            UiAssets.draw(g,"agency/ui/network",textX+25,requirementY,13,13);
                            UiText.text(g,font,"50%",textX+39,requirementY+2,TEXT);
                            UiText.text(g,font,"60일",pane < 250 ? textX : actionX-42,actionY+4,GOLD);
                        } else UiText.text(g,font,trim(item.value(),(int)((actionX-textX-6)/UiText.scale(font))),textX,actionY+4,GOLD);
                        HoiMenuStyle.control(g, actionX, actionY, 62, 18, false, false);
                        UiText.centered(g, font, item.id().startsWith("cancel_") ? "진행 중" : item.id().startsWith("network:") ? "정보망" : "준비하기", actionX, actionY, 62, 18, TEXT);
                        g.disableScissor();
                        return;
                    }
                    if (item.group().equals("cryptology")) {
                        HoiMenuStyle.metal(g, bx, by, w, h);
                        UiAssets.draw(g, "agency/cryptology", bx + 4, by + 4, 15, 15);
                        UiText.text(g, font, trim(item.title() + (item.id().equals("cipher_level") ? " " + item.value() : ""), (int)((w-28)/UiText.scale(font))), bx+23, by+5, TEXT);
                        if (!item.id().equals("cipher_level")) {
                            g.fill(bx+4,by+28,bx+w-120,by+40,0xFF16111C);
                            g.fill(bx+4,by+28,bx+4+(int)((w-124)*Math.max(0,item.progress())),by+40,0xFF795698);
                            UiText.centered(g,font,item.value(),bx+4,by+27,w-124,14,GOLD);
                        }
                        return;
                    }
                    int labelY = grid ? by + 34 : by;
                    int labelHeight = grid ? 26 : h;
                    g.fillGradient(bx, labelY, bx + w, labelY + labelHeight, item.enabled() ? 0xFF354036 : 0xFF292C31, 0xFF101519);
                    g.outline(bx, labelY, w, labelHeight, grid ? upgradeColor(item) : 0xFF62696F);
                    int artWidth = grid ? w : 40, artHeight = 34;
                    int ax = grid ? bx : bx + 2;
                    if (!UiAssets.draw(g, item.texture(), ax, by, artWidth, artHeight)) UiAssets.draw(g, "menu/intelligence", ax, by, artWidth, artHeight);
                    int tx = grid ? bx + w / 2 : bx + 44, ty = grid ? by + 37 : by + 7;
                    if (grid) {
                        UiText.centered(g, font, item.title(), bx + 3, by + 34, w - 6, 26, TEXT);
                        int stages = upgradeStages(item);
                        int completed = upgradeLevel(item);
                        for (int stage = 0; stages > 1 && stage < stages; stage++)
                            UiAssets.draw(g, stage < completed ? "agency/stage_complete" : "agency/stage_empty",
                                    bx + (w - (stages - 1) * 6 - 11) / 2 + stage * 6, by + h - 5, 11, 11);
                    } else {
                        UiText.text(g, font, trim(item.title(), w - 49), tx, ty, TEXT);
                        UiText.text(g, font, trim(item.value(), w - 49), tx, ty + 13, GOLD);
                    }
                    if (!grid && item.progress() >= 0) {
                        g.fill(bx + 3, by + h - 4, bx + w - 3, by + h - 2, 0xFF151A13);
                        g.fill(bx + 3, by + h - 4, bx + 3 + (int)((w - 6) * item.progress()), by + h - 2, 0xFF91AB70);
                    }
                }
            };
            button.active = selectedId == null && !item.group().equals("cryptology"); addRenderableWidget(button);
            if (item.id().startsWith("decrypt:") || item.id().startsWith("pause:")) {
                int rowWidth = areaWidth - 4;
                var toggle = new HoiMenuButton(item.button(), bx + rowWidth - 113, by + 23, 53, 19, () -> selectItem(item));
                toggle.active = item.enabled() && pendingTicks == 0; addRenderableWidget(toggle);
                var reveal = view.items().stream().filter(v -> v.id().equals("reveal:" + item.id().split(":", 2)[1])).findFirst().orElse(null);
                if (reveal != null) {
                    var action = new HoiMenuButton("암호 공개", bx + rowWidth - 57, by + 23, 53, 19, () -> selectItem(reveal));
                    action.active = reveal.enabled() && pendingTicks == 0; addRenderableWidget(action);
                }
            }

        }
        var item = selected();
        if (item != null) {
            int dx = detailX(), dw = detailWidth();
            addRenderableWidget(new PanelButton("닫기", dx + dw - 52, 44, 44, 20, () -> { selectedId = null; choosing = -1; rebuildWidgets(); }));
            if (choosing >= 0) { initChoices(item); return; }
            int bottom = height - 70;
            for (int i = 0; i < item.parameters().size(); i++) {
                final int index = i; var parameter = item.parameters().get(i); String value = arguments.get(i);
                String name = parameter.choices().stream().filter(c -> c.value().equals(value)).map(Choice::label).findFirst().orElse("선택 없음");
                addRenderableWidget(new PanelButton(parameter.label() + ": " + trim(name, dw - 116), dx + 10, bottom - (item.parameters().size() - i) * 25, dw - 20, 22,
                        () -> { choosing = index; searchText = ""; choiceScroll = 0; rebuildWidgets(); }));
            }
            if (!item.button().isBlank()) {
                var action = new PanelButton(pendingTicks > 0 ? "응답 대기…" : item.button(), dx + dw / 2 - 65, bottom, 130, 24,
                        () -> send(AgencyProtocol.Kind.CALL, item.id(), List.copyOf(arguments)));
                action.active = item.enabled() && pendingTicks == 0 && arguments.stream().noneMatch(String::isEmpty);
                addRenderableWidget(action);
            }
        }
    }
    private void initChoices(Item item) {
        int dx = detailX(), dw = detailWidth();
        search = new EditBox(font, dx + 12, 84, dw - 92, 20, Component.literal("검색"));
        search.setMaxLength(128); search.setValue(searchText);
        search.setResponder(value -> { searchText = value; choiceScroll = 0; rebuildWidgets(); });
        addRenderableWidget(search); setInitialFocus(search);
        addRenderableWidget(new PanelButton("뒤로", dx + dw - 74, 84, 62, 20, () -> { choosing = -1; rebuildWidgets(); }));
        var choices = matchingChoices(item);
        int count = Math.max(1, (height - 163) / 25);
        choiceScroll = Math.clamp(choiceScroll, 0, Math.max(0, choices.size() - count));
        for (int i = choiceScroll; i < Math.min(choices.size(), choiceScroll + count); i++) {
            var choice = choices.get(i);
            addRenderableWidget(new PanelButton(trim(choice.label(), dw - 38), dx + 12, 113 + (i - choiceScroll) * 25, dw - 24, 22,
                    () -> { arguments.set(choosing, choice.value()); choosing = -1; rebuildWidgets(); }));
        }
    }
    private List<Choice> matchingChoices(Item item) {
        String query = searchText.toLowerCase(Locale.ROOT);
        return item.parameters().get(choosing).choices().stream().filter(c -> (c.label() + " " + c.value()).toLowerCase(Locale.ROOT).contains(query)).toList();
    }
    private Item selected() { return view == null ? null : view.items().stream().filter(i -> i.id().equals(selectedId)).findFirst().orElse(null); }
    private List<Item> visibleItems() {
        return view.items().stream().filter(i -> i.group().equals(group) && !i.id().equals("cancel_project") && !i.id().equals("spy_master") && !i.id().startsWith("reveal:")).sorted(group.equals("upgrades") ? Comparator.comparingInt(AgencyScreen::upgradeOrder) : Comparator.comparingInt(i -> 0)).toList();
    }
    private void changeGroup(String next) { group = next; selectedId = null; choosing = -1; scroll = 0; rebuildWidgets(); }
    private void selectItem(Item item) {
        if ((item.id().startsWith("upgrade:") || item.group().equals("cryptology")) && item.parameters().isEmpty()) {
            if (item.enabled() && pendingTicks == 0) send(AgencyProtocol.Kind.CALL, item.id(), List.of());
            return;
        }
        if (item.id().startsWith("operation:")) {
            minecraft.gui.setScreen(new AgencyOperationScreen(this, item, args -> send(AgencyProtocol.Kind.CALL, item.id(), args)));
            return;
        }
        selectedId = item.id(); arguments.clear(); normalizeArguments(item); detailScroll = 0; rebuildWidgets();
    }
    private int upgradeWidth() { return Math.min(540, width - 16); }
    private int upgradeX() { return (width - upgradeWidth()) / 2; }
    private int upgradeY() { return Math.max(top + 4, (height - 410) / 2); }
    static int upgradeOrder(Item item) {
        int index = UPGRADE_ORDER.indexOf(item.id().replace("upgrade:", "").toLowerCase(Locale.ROOT));
        return index < 0 ? UPGRADE_ORDER.size() : index;
    }
    static int upgradeColor(Item item) {
        return item.progress() >= 1 ? GOLD : item.enabled() ? 0xFF55FF55 : 0xFFB53543;
    }
    static int upgradeStages(Item item) {
        if (!item.id().startsWith("upgrade:")) return 0;
        int slash = item.value().indexOf('/');
        if (slash < 0) return 0;
        try {
            int count = Integer.parseInt(item.value().substring(slash + 1).trim());
            return count > 0 && count <= 8 ? count : 0;
        } catch (NumberFormatException ignored) { return 0; }
    }
    static int upgradeLevel(Item item) {
        int slash = item.value().indexOf('/');
        if (slash < 0) return 0;
        try { return Math.clamp(Integer.parseInt(item.value().substring(0, slash).trim()), 0, upgradeStages(item)); }
        catch (NumberFormatException ignored) { return 0; }
    }
    static String upgradeIndicator(Item item) {
        int maximum = upgradeStages(item);
        return maximum < 1 || maximum > 4 ? "" : "agency/researched/" + (maximum * (maximum + 1) / 2 - 1 + upgradeLevel(item));
    }
    private static int upgradeCategory(Item item) {
        int index = upgradeOrder(item);
        return index < 3 ? 0 : index < 8 ? 1 : index < 13 ? 2 : index < 18 ? 3 : 4;
    }
    private static int upgradePosition(List<Item> items, int index, int columns) {
        int pos = 0;
        for (int i = 0; i < index; i++) {
            pos++;
            if (upgradeCategory(items.get(i)) != upgradeCategory(items.get(i + 1))) pos = (pos + columns - 1) / columns * columns;
        }
        return pos;
    }
    private static int upgradeOffset(List<Item> items, int index, int columns) {
        int headers = 1;
        for (int i = 1; i <= index; i++) if (upgradeCategory(items.get(i - 1)) != upgradeCategory(items.get(i))) headers++;
        return upgradePosition(items, index, columns) / columns * 72 + headers * 16;
    }
    private int detailX() { return Math.min(pane + 12, width - 260); }
    private int detailWidth() { return Math.min(540, width - detailX() - 12); }
    private void send(AgencyProtocol.Kind kind, String item, List<String> args) {
        if (pendingTicks > 0 && kind == AgencyProtocol.Kind.CALL) return;
        pendingTicks = 100;
        transport.accept(new AgencyProtocol.Request(kind, token, view == null ? 0 : view.revision(), item, args));
        rebuildWidgets();
    }
    @Override public void tick() {
        if (pendingTicks > 0 && --pendingTicks == 0) {

            send(view == null ? AgencyProtocol.Kind.OPEN : AgencyProtocol.Kind.REFRESH, "", List.of());
        } else if (++refreshTicks % 60 == 0 && pendingTicks == 0 && choosing < 0 && selectedId == null) send(AgencyProtocol.Kind.REFRESH, "", List.of());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        HoiMenuStyle.panel(g, 0, top, pane, height - top);
        HoiMenuBar.draw(g, width, parent instanceof HoiMenuScreen menu ? menu.hud() : CountryHud.UNKNOWN, mx, my, 0);
        HoiMenuStyle.heading(g, font, "정보기관", 10, top + 9, pane - 44, HoiMenuStyle.TEXT);
        if (unestablished()) { drawUnestablished(g); super.extractRenderState(g,mx,my,delta); return; }
        String flag = view == null ? "menu/intelligence" : view.country().equals("KOR") ? "country/kor/intelligence" : "menu/intelligence";
        UiAssets.cover(g, "agency/ui/header", 6, top + 29, pane - 12, 62);
        UiAssets.draw(g, flag, 12, top + 40, 38, 39);
        UiText.text(g, font, trim(view == null ? "불러오는 중…" : view.name(), pane - 98), 56, top + 46, TEXT);
        UiAssets.draw(g, "agency/passive_defense", 56, top + 68, 14, 14);
        UiText.text(g, font, "방첩 활동:", 73, top + 70, TEXT);
        String defense = view == null ? "—" : view.items().stream().filter(i -> i.id().equals("counter_intelligence"))
                .map(Item::value).findFirst().orElse("—");
        UiText.text(g, font, defense, 76 + Math.round(font.width("방첩 활동:") * UiText.scale(font)), top + 70, GOLD);
        drawOperationsChrome(g);
        if (view != null) for (int i = 0; i < UPGRADE_GROUPS.length; i++) {
            int category = i;
            var upgrades = view.items().stream().filter(item -> item.group().equals("upgrades") && !item.id().equals("cancel_project") && upgradeCategory(item) == category)
                    .sorted(Comparator.comparingInt(AgencyScreen::upgradeOrder)).toList();
            int count = upgrades.size(), size = Math.min(10, (pane - 20) / 5 / Math.max(1, count));
            int x = 8 + i * (pane - 16) / 5;
            for (int j = 0; j < count; j++)
                UiAssets.draw(g, upgradeIndicator(upgrades.get(j)), x + j * size, top + 138, size, size);
        }
        boolean overlay = view != null && group.equals("upgrades");
        if (overlay) {
            for (int i = 0; i < overlayStart; i++) if (children().get(i) instanceof net.minecraft.client.gui.components.Renderable widget)
                widget.extractRenderState(g, mx, my, delta);
            g.nextStratum();
        }
        if (view != null && group.equals("upgrades") && selectedId == null) {
            int ux = upgradeX(), uw = upgradeWidth();
            HoiMenuStyle.panel(g, ux, upgradeY(), uw, height - upgradeY() - 36);
            HoiMenuStyle.heading(g, font, "정보기관 개선", ux + 12, upgradeY() + 10, upgradeWidth() - 44, TEXT);
            var items = visibleItems(); int columns = Math.max(1, Math.min(5, (uw - 16) / 100));
            String[] titles = {"정보공동체", "정보 순환", "정보 수집 분야", "휴민트", "신호 정보"};
            for (int i = 0; i < items.size(); i++) if (i == 0 || upgradeCategory(items.get(i - 1)) != upgradeCategory(items.get(i))) {
                int by = upgradeY() + 39 + upgradeOffset(items, i, columns) - scroll - 16;
                if (by < upgradeY() + 34 || by + 17 > height - 37) continue;
                HoiMenuStyle.metal(g, ux + 8, by, uw - 16, 17);
                UiText.text(g, font, titles[upgradeCategory(items.get(i))], ux + 13, by + 4, TEXT);
            }
        }
        var item = selected();
        if (item != null) {
            int dx = detailX(), dw = detailWidth();
            g.fillGradient(dx, 38, dx + dw, height - 36, 0xFF343D46, 0xFF10151B); g.outline(dx, 38, dw, height - 74, 0xFF88919A);
            UiText.text(g, font, trim(item.title(), dw - 73), dx + 10, 51, GOLD);
            if (choosing < 0) {
                if (!UiAssets.draw(g, item.texture(), dx + 12, 77, 72, 58)) UiAssets.draw(g, "menu/intelligence", dx + 12, 77, 72, 58);
                UiText.text(g, font, trim(item.value(), dw - 109), dx + 97, 86, TEXT);
                if (item.group().equals("operations") && dw >= 360) {
                    String[] phases = {"phase_border", "phase_bribe", "phase_escape"};
                    int artWidth = Math.min(70, (dw - 124) / 3);
                    for (int i = 0; i < phases.length; i++) UiAssets.draw(g, "agency/" + phases[i], dx + 98 + i * (artWidth + 5), 104, artWidth, 37);
                }
                int bottom = height - 82 - item.parameters().size() * 25;
                var lines = font.split(Component.literal(item.detail()), dw - 26);
                detailScroll = Math.clamp(detailScroll, 0, Math.max(0, lines.size() * 13 - Math.max(0, bottom - 148)));
                g.enableScissor(dx + 10, 146, dx + dw - 10, Math.max(146, bottom));
                int y = 148 - detailScroll;
                for (var line : lines) { UiText.text(g, font, line, dx + 13, y, TEXT); y += 13; }
                g.disableScissor();
            }
        }
        if (overlay) {
            for (int i = overlayStart; i < children().size(); i++) if (children().get(i) instanceof net.minecraft.client.gui.components.Renderable widget)
                widget.extractRenderState(g, mx, my, delta);
        } else super.extractRenderState(g, mx, my, delta);
    }
    private int counterWidth() { return Math.round((pane - 12) * 52f / 522); }
    private int counterX(int i) { return 6 + Math.round((pane - 12) * (i == 0 ? 59f : i == 1 ? 136f : 203.5f) / 522) - counterWidth() / 2; }
    private int recruitX() { return Math.round(pane * .54f); }
    private void drawOperationsChrome(GuiGraphicsExtractor g) {
        UiAssets.nineSlice(g,"agency/ui/section",6,top+92,pane-12,17,5,.5);
        UiText.centered(g,font,"첩보기관",12,top+95,(pane-12)*185/530-8,11,TEXT);
        UiAssets.nineSlice(g,"agency/ui/section",6,top+150,pane-12,17,5,.5);
        UiText.centered(g,font,"작전",12,top+153,(pane-12)*185/530-8,11,TEXT);
        // Preserve the source slanted divider and the three inset counter wells.
        UiAssets.nineSlice(g,"agency/ui/operatives",6,top+168,pane-12,46,3,.5);
        if(view == null)return;
        String[] ids={"recruit","captured_count","killed_count"};
        for(int i=0;i<ids.length;i++) {
            String id=ids[i];
            String value=view.items().stream().filter(v->v.id().equals(id)).map(Item::value).findFirst().orElse(i==0?"0/0":"0");
            UiText.centered(g,font,value.replace(" ",""),counterX(i),top+201,counterWidth(),10,TEXT);
        }
        if(!unestablished() && view.items().stream().noneMatch(v->v.id().equals("cancel_project")))
            UiText.centered(g,font,pane < 250 ? "개선 없음" : "진행 중인 개선 없음",recruitX(),top+170,pane-recruitX()-10,17,TEXT);
        if(!unestablished() && group.equals("operations") && visibleItems().isEmpty())
            wrappedNotice(g,"작전은 정보원이 첩보망을 구축하여야 시행할 수 있습니다.",16,top+282,pane-32);
    }
    private boolean unestablished() { return view != null && view.items().stream().noneMatch(i -> i.id().equals("recruit")); }
    private void drawUnestablished(GuiGraphicsExtractor g) {
        var project=view.items().stream().filter(i->i.id().equals("cancel_project")).findFirst().orElse(null);
        UiAssets.draw(g,"agency/ui/create",10,top+29,pane-20,59);
        smallCentered(g,project==null?"기관 창설":"기관 창설 중",pane/2,top+51,TEXT);
        UiAssets.draw(g,"hud/factories",34,top+57,13,13);
        smallCentered(g,"5",54,top+60,0xFFCC4444);
        smallCentered(g,project==null?"30일":project.value().substring(project.value().lastIndexOf('·')+1).trim(),pane-52,top+60,GOLD);
        HoiMenuStyle.recess(g,34,top+77,pane-68,4);
        if(project!=null&&project.progress()>=0)g.fill(35,top+78,35+(int)((pane-70)*project.progress()),top+80,0xFF97AC6C);
        drawOperationsChrome(g);
        wrappedNotice(g,"기관을 창설하기 전에는 정보원을 모집할 수 없습니다.",recruitX(),top+178,pane-recruitX()-12);
        wrappedNotice(g,group.equals("cryptology")?"기관을 창설하면 암호 해독을 시작할 수 있습니다.":"작전은 정보원이 첩보망을 구축하여야 시행할 수 있습니다.",16,top+282,pane-32);
    }
    private void wrappedNotice(GuiGraphicsExtractor g,String value,int x,int y,int width) {
        var lines=font.split(Component.literal(value),Math.max(1,(int)(width/UiText.scale(font))));
        for(int i=0;i<lines.size();i++) UiText.text(g, font,lines.get(i),x+Math.round((width-font.width(lines.get(i))*UiText.scale(font))/2),y+i*(font.lineHeight+2),TEXT);
    }
    private void smallCentered(GuiGraphicsExtractor g,String value,int x,int y,int color) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(1f);UiText.centered(g,font,value,-100,0,200,10,color);g.pose().popMatrix();
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (choosing >= 0) choiceScroll -= (int)(vertical * 3);
        else if (selectedId != null) { detailScroll -= (int)(vertical * 26); return true; }
        else scroll -= (int)(vertical * 51);
        rebuildWidgets(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && selectedId != null) {
            if (choosing >= 0) choosing = -1; else selectedId = null;
            rebuildWidgets(); return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE && group.equals("upgrades")) { changeGroup("operations"); return true; }
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyPressed(event);
    }
    @Override public boolean keyReleased(KeyEvent event) {
        if (allowsMovement() && SidebarMovement.consumes(minecraft, event)) return true;
        return super.keyReleased(event);
    }
    @Override public void onClose() {
        transport.accept(new AgencyProtocol.Request(AgencyProtocol.Kind.CLOSE, token, view == null ? 0 : view.revision(), "", List.of()));
        minecraft.gui.setScreen(parent);
    }
    @Override public void removed() { SidebarMovement.release(minecraft); }
    @Override public boolean allowsMovement() { return selectedId == null && !group.equals("upgrades"); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
    private String trim(String s, int max) { return font.width(s) <= max ? s : font.plainSubstrByWidth(s, Math.max(1, max - 9)) + "…"; }
}
