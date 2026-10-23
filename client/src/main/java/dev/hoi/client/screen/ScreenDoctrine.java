package dev.hoi.client.screen;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class ScreenDoctrine extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int SLOT = 212, TEXT = 0xFFE3E1D8, GOLD = 0xFFE7C779;
    private final Screen parent;
    private final String token;
    private final Consumer<ProtocolDoctrinePayloads.Request> transport;
    private final boolean fixture;
    private ProtocolDoctrineView view;
    private String folder, picking;
    private int top, tracksTop, horizontal, popupScroll, refreshTicks, pendingTicks;
    private ProtocolDoctrinePayloads.Kind pendingKind;
    private boolean advanced;
    private final Map<net.minecraft.client.gui.components.AbstractWidget, String> scrollableTooltips = new IdentityHashMap<>();
    private String hoveredDetail = "";
    private int tooltipScroll;
    private UiTooltips.ScrolledTooltip tooltipBounds;
    UiTooltips.ScrolledTooltip tooltipBounds() { return tooltipBounds; }
    String hoveredDetail() { return hoveredDetail; }
    public ScreenDoctrine(Screen parent, String folder) {
        this(parent, folder, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(ProtocolDoctrinePayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        });
    }
    public ScreenDoctrine(Screen parent, String folder, String token, ProtocolDoctrineView view,
            Consumer<ProtocolDoctrinePayloads.Request> transport) {
        super(Component.literal("교리")); this.parent = parent; this.folder = folder;
        this.token = token; this.view = view; this.transport = transport; fixture = view != null;
    }
    public static void open(Screen parent, String folder) {
        var screen = new ScreenDoctrine(parent, folder); ScreenDialogCoordinator.setContent(screen); screen.open();
    }
    public void open() { send(ProtocolDoctrinePayloads.Kind.OPEN, "", ""); }
    public void update(ProtocolDoctrineView next) {
        if (!next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !view.country().equals(next.country())) { revoke(); return; }
        if (view != null && next.revision() <= view.revision()) return;
        if (view != null && next.progressionRevision() > view.progressionRevision()
                && (pendingKind == ProtocolDoctrinePayloads.Kind.SELECT_GRAND || pendingKind == ProtocolDoctrinePayloads.Kind.SELECT_SUBDOCTRINE)) picking = null;
        if (!folder.equals(next.folder())) { horizontal = 0; picking = null; popupScroll = 0; }
        folder = next.folder(); view = next; pendingTicks = 0; pendingKind = null; refreshTicks = 0; rebuildWidgets();
    }
    private void send(ProtocolDoctrinePayloads.Kind kind, String track, String item) {
        pendingKind = kind; pendingTicks = 100;
        transport.accept(new ProtocolDoctrinePayloads.Request(kind, token, view == null ? 0 : view.revision(), folder, track, item));
        rebuildWidgets();
    }
    @Override protected void init() {
        scrollableTooltips.clear(); tooltipBounds = null;
        InputSidebarMovement.release(minecraft); top = Math.max(41, UiMenuBar.height(width)); tracksTop = top + 100;
        UiMenuBar.buttons(width, view == null ? "" : view.country(), ProtocolMenuTab.OFFICER_CORPS, tab -> {
            if (tab == ProtocolMenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }).forEach(this::addRenderableWidget);
        if (view == null) return;
        horizontal = Math.clamp(horizontal, 0, Math.max(0, view.tracks().size() * SLOT - width));
        for (int i = 0; i < view.folders().size(); i++) {
            var f = view.folders().get(i); int x = 8 + i * 128;
            button(f.name(), x, top, 128, 31, () -> { folder = f.id(); picking = null; horizontal = 0; send(ProtocolDoctrinePayloads.Kind.REFRESH, "", ""); }, "", picking == null && pendingTicks == 0);
        }
        button("닫기", width - 22, top + 25, 17, 17, this::onClose, "닫기", true);
        var grand = view.grands().stream().filter(ProtocolDoctrineView.Choice::selected).findFirst();
        button(grand.map(ProtocolDoctrineView.Choice::name).orElse("대교리 선택"), width / 2 - 112, top + 38, 225, 65,
            () -> { picking = "grand"; popupScroll = 0; rebuildWidgets(); }, grand.map(ProtocolDoctrineView.Choice::detail).orElse(""), picking == null && pendingTicks == 0);
        for (int i = 0; i < view.tracks().size(); i++) {
            var track = view.tracks().get(i); int x = trackX(i);
            if (x + SLOT < 0 || x > width) continue;
            button(track.name(), x + 52, tracksTop + 80, 74, 55,
                () -> { picking = track.id(); popupScroll = 0; rebuildWidgets(); }, selectedDetail(track), picking == null && pendingTicks == 0);
            var counter=new dev.hoi.client.ui.UiHoverRegion(track.name()+" mastery",x+126,tracksTop+89,37,14);
            counter.active=picking==null;
            String masteryDetail=view.uiText().getOrDefault("mastery/"+i,"");
            if(!masteryDetail.isBlank())scrollableTooltips.put(counter,masteryDetail);
            addRenderableWidget(counter);
            for (int j = 0; j < track.rewards().size(); j++) {
                var reward = track.rewards().get(j); int y = tracksTop + 130 + j * 44;
                if (y >= height - 8) break;
                var hover=new UiHoverRegion("통달 "+reward.mastery(),x+(j%2==0?37:126),y,50,Math.min(44,height-8-y));
                hover.active=picking==null;
                if(!reward.detail().isBlank())scrollableTooltips.put(hover,reward.detail());
                addRenderableWidget(hover);
            }
        }
        if (picking != null) popupWidgets();
    }
    private String selectedDetail(ProtocolDoctrineView.Track track) {
        String base = track.candidates().stream().filter(ProtocolDoctrineView.Choice::selected).map(ProtocolDoctrineView.Choice::detail).findFirst().orElse(track.name());
        return base;
    }
    static int tabFrame(String tab, String selected) { return tab.equals(selected) ? 0 : 1; }
    private int trackX(int index) { return Math.max(0, (width - view.tracks().size() * SLOT) / 2) + index * SLOT - horizontal; }
    private int popupX() {
        if (picking.equals("grand")) return (width - 280) / 2;
        for (int i = 0; i < view.tracks().size(); i++) if (view.tracks().get(i).id().equals(picking))
            return Math.clamp(trackX(i) + 193, 4, Math.max(4, width - 284));
        return (width - 280) / 2;
    }
    private int popupY() { return Math.max(top + 36, Math.min(height - 292, picking.equals("grand") ? top + 183 : tracksTop + 105)); }
    private List<ProtocolDoctrineView.Choice> choices() {
        if (picking.equals("grand")) return view.grands();
        return view.tracks().stream().filter(t -> t.id().equals(picking)).findFirst().map(ProtocolDoctrineView.Track::candidates).orElse(List.of());
    }
    private void popupWidgets() {
        var choices = choices(); int x = popupX(), y = popupY();
        button("고급 세부 사항", x + 250, y + 260, 21, 21, () -> { advanced = !advanced; rebuildWidgets(); },
            view.uiText().getOrDefault(advanced ? "DOCTRINE_DETAILS_BUTTON_DISABLE" : "DOCTRINE_DETAILS_BUTTON_ENABLE", ""), pendingTicks == 0);
        popupScroll = Math.clamp(popupScroll, 0, Math.max(0, (choices.size() + 1) / 2 - 5));
        button("선택 창 닫기", x + 257, y + 6, 17, 17, () -> { picking = null; rebuildWidgets(); }, "닫기", true);
        for (int i = 0; i < choices.size(); i++) {
            var choice = choices.get(i); int row = i / 2 - popupScroll;
            if (row < 0 || row >= 5) continue;
            int cx = x + 13 + i % 2 * 125, cy = y + 91 + row * 38;
            button(choice.name(), cx, cy, 123, 35, () -> send(picking.equals("grand")
                ? ProtocolDoctrinePayloads.Kind.SELECT_GRAND : ProtocolDoctrinePayloads.Kind.SELECT_SUBDOCTRINE,
                picking.equals("grand") ? "" : picking, choice.id()), advanced && !choice.advancedDetail().isEmpty() ? choice.advancedDetail() : choice.detail(), pendingTicks == 0 && choice.enabled(), advanced && !choice.advancedDetail().isEmpty());
        }
    }
    private void button(String name, int x, int y, int w, int h, Runnable action, String detail, boolean enabled) {
        button(name, x, y, w, h, action, detail, enabled, false);
    }
    private void button(String name, int x, int y, int w, int h, Runnable action, String detail, boolean enabled, boolean scrollable) {
        var button = new InvisibleButton(name, x, y, w, h, action);
        button.active = enabled;
        if (!detail.isBlank()) {
            scrollableTooltips.put(button, detail);
        }
        addRenderableWidget(button);
    }
    private static final class InvisibleButton extends Button {
        InvisibleButton(String name, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.literal(name), ignored -> action.run(), DEFAULT_NARRATION);
        }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {}
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        UiAssets.nineSlice(g, "doctrine/gfx_tiled_window_transparent", 2, top + 20, width - 4, height - top - 22, 12, .5);
        g.fill(4, top + 24, width - 4, height - 5, 0xFF141416);
        UiMenuBar.draw(g, width, ClientCampaignHud.hud(), mx, my, 0);
        if (view == null) { UiText.left(g, font, "불러오는 중…", 0, top + 40, width, 30, TEXT); return; }
        int frame = view.folders().stream().filter(f -> f.id().equals(view.folder())).findFirst().orElseThrow().frame();
        for (int i = 0; i < view.folders().size(); i++) {
            var f = view.folders().get(i); int x = 8 + i * 128;
            UiAssets.draw(g, f.texture() + "/frame_" + tabFrame(f.id(), view.folder()), x, top, 128, 31);
            UiText.centered(g, font, f.name(), x + 42, top + 7, 77, 17, TEXT);
        }
        UiAssets.draw(g, "doctrine/gfx_grand_doctrine_frame/frame_" + frame, width / 2 - 112, top + 38, 225, 65);
        var grand = view.grands().stream().filter(ProtocolDoctrineView.Choice::selected).findFirst();
        grand.ifPresent(choice -> UiAssets.draw(g, choice.texture(), width / 2 - 14, top + 40, 32, 32));
        centeredHeading(g, grand.map(ProtocolDoctrineView.Choice::name).orElse("대교리 선택"), width / 2 - 95, top + 74, 190, 18, 1.35f);
        g.enableScissor(4, top + 95, width - 4, height - 8);
        for (int i = 0; i < view.tracks().size(); i++) {
            var track = view.tracks().get(i); int x = trackX(i);
            if (x + SLOT < 0 || x > width) continue;
            UiAssets.draw(g, track.background(), x + 15, tracksTop + 45, 188, 342);
            UiAssets.draw(g, "doctrine/gfx_track_frame/frame_0", x + 6, tracksTop + 36, 200, 352);
            UiAssets.draw(g, track.decoration() + "/frame_0", x + 10, tracksTop + 4, 191, 41);
            UiAssets.draw(g, track.icon() + "/frame_" + (track.selected().isEmpty() ? 1 : 0), x + 80, tracksTop + 2, 53, 42);
            centeredHeading(g, track.name(), x + 31, tracksTop + 52, 150, 22, 1.35f);
            UiAssets.draw(g, "doctrine/gfx_subdoctrine_button_and_text_frame/frame_" + frame, x + 52, tracksTop + 80, 118, 55);
            track.candidates().stream().filter(ProtocolDoctrineView.Choice::selected).findFirst()
                .ifPresent(choice -> UiAssets.draw(g, choice.texture(), x + 91, tracksTop + 82, 31, 31));
            UiText.centered(g, font, track.nameplate(), x + 58, tracksTop + 116, 106, 15, TEXT);
            UiAssets.draw(g, "doctrine/gfx_mastery_amount_frame_big", x + 126, tracksTop + 89, 37, 14);
            boolean banking=track.selected().isEmpty()||!track.rewards().isEmpty()&&track.rewards().stream().allMatch(ProtocolDoctrineView.Reward::unlocked);
            var masteryIcon=UiKeywordIcons.decorateLines(Component.literal("£doctrine_mastery"),false);
            var masteryIconLine=firstLine(masteryIcon,10);
            String masteryLine=UiText.ellipsize(font,banking?track.bank():track.mastery(),Math.max(1,(int)(22/UiText.scale(font))));
            int masteryIconWidth=Math.round(font.width(masteryIconLine)*UiText.scale(font)),masteryWidth=Math.round(font.width(masteryLine)*UiText.scale(font));
            int masteryLeft=x+128+(33-masteryIconWidth-1-masteryWidth)/2;
            UiText.text(g,font,masteryIconLine,masteryLeft,tracksTop+91,GOLD);
            UiText.text(g,font,masteryLine,masteryLeft+masteryIconWidth+1,tracksTop+91,GOLD);
            for (int j = 0; j < track.rewards().size(); j++) {
                var reward = track.rewards().get(j); int ry = tracksTop + 130 + j * 44;
                if(ry>=height-8)break;
                UiAssets.draw(g,"doctrine/gfx_techtree_line_down_currently_researching/frame_0",x+102,ry+26,8,18);
                UiAssets.draw(g,"doctrine/gfx_mastery_progress_bar_bg",x+98,ry,16,36);
                int fill=Math.round(36*rewardProgress(track,j));
                if(fill>0) {
                    g.enableScissor(x+98,ry+36-fill,x+114,ry+36);
                    UiAssets.draw(g,"doctrine/gfx_mastery_progress_bar_bg_filled",x+98,ry,16,36);
                    g.disableScissor();
                }
                UiAssets.draw(g,"doctrine/gfx_mastery_amount_frame/frame_0",x+92,ry+32,28,13);
                UiText.centered(g,font,rewardThreshold(track,j),x+94,ry+32,24,13,0xFF000000 | UiTooltips.GOLD);
                UiAssets.draw(g, "doctrine/gfx_mastery_reward_frame/frame_" + rewardFrame(frame, reward.unlocked()), x + (j % 2 == 0 ? 37 : 126), ry, 50, 44);
                UiAssets.draw(g, "doctrine/gfx_mastery_reward_icons/frame_" + (Math.min(4, j) + (reward.unlocked() ? 0 : 5)), x + (j % 2 == 0 ? 37 : 126), ry, 50, 44);
                UiAssets.draw(g,"doctrine/gfx_mastery_reward_level/frame_"+Math.min(4,j),x+(j%2==0?35:159),ry+32,23,23);
            }
        }
        g.disableScissor();
        UiMenuStyle.close(g, width - 22, top + 25, 17, 17, TEXT);
        if (!view.message().isEmpty()) UiText.left(g, font, view.message(), 8, height - 23, width - 16, 15, GOLD);
        if (picking != null) drawPopup(g);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable renderable)
            renderable.extractRenderState(g, mx, my, delta);
        String detail = scrollableTooltips.entrySet().stream().filter(e -> e.getKey().isMouseOver(mx, my))
            .map(Map.Entry::getValue).findFirst().orElse("");
        if (!detail.equals(hoveredDetail)) { hoveredDetail = detail; tooltipScroll = 0; }
        tooltipBounds = detail.isEmpty() ? null : UiTooltips.drawSourceScrollable(g, font, detail, mx, my, width, height, tooltipScroll);
        if (tooltipBounds != null) tooltipScroll = tooltipBounds.scroll();
    }
    private net.minecraft.util.FormattedCharSequence firstLine(Component text,int width) {
        var lines=UiTooltips.split(font,text,Math.max(1,(int)(width/UiText.scale(font))));
        return lines.isEmpty()?Component.empty().getVisualOrderText():lines.getFirst();
    }
    static int rewardFrame(int branch, boolean unlocked) { return branch * 2 + (unlocked ? 0 : 1); }
    static String rewardThreshold(ProtocolDoctrineView.Track track,int index) {
        try {
            java.math.BigDecimal amount=java.math.BigDecimal.ZERO;
            for(int i=0;i<=index;i++)amount=amount.add(new java.math.BigDecimal(track.rewards().get(i).mastery()));
            return amount.stripTrailingZeros().toPlainString();
        }catch(NumberFormatException unknown){return "?";}
    }
    static float rewardProgress(ProtocolDoctrineView.Track track,int index) {
        if(track.rewards().get(index).unlocked())return 1;
        try {
            double previous=index==0?0:Double.parseDouble(rewardThreshold(track,index-1));
            double threshold=Double.parseDouble(rewardThreshold(track,index));
            double current=Double.parseDouble(track.mastery());
            return threshold<=previous||!Double.isFinite(current)?0:(float)Math.clamp((current-previous)/(threshold-previous),0,1);
        }catch(NumberFormatException unknown){return 0;}
    }
    private void drawPopup(GuiGraphicsExtractor g) {
        int x = popupX(), y = popupY(); var choices = choices();
        UiAssets.nineSlice(g, "doctrine/gfx_tiled_window_w_close", x, y, 280, 288, 12, .5);
        var selected = choices.stream().filter(ProtocolDoctrineView.Choice::selected).findFirst();
        String heading = view.uiText().getOrDefault(picking.equals("grand") ? "GRAND_DOCTRINE_SELECTION_TITLE" : "SUB_DOCTRINE_SELECTION_TITLE",
            picking.equals("grand") ? "대교리 선택" : "하위 교리 선택");
        centeredHeading(g, heading, x + 15, y + 12, 250, 20, 1.5f);
        UiAssets.nineSlice(g, "doctrine/gfx_tiled_paper_bg", x + 15, y + 33, 250, 49, 8, .5);
        String description = view.uiText().getOrDefault(picking.equals("grand") ? "GRAND_DOCTRINE_DESCRIPTION" : "SUBDOCTRINE_DESCRIPTION", "");
        UiText.left(g, font, description, x + 25, y + 39, 230, 40, TEXT);
        for (int i = 0; i < choices.size(); i++) {
            var choice = choices.get(i); int row = i / 2 - popupScroll;
            if (row < 0 || row >= 5) continue;
            int cx = x + 13 + i % 2 * 125, cy = y + 91 + row * 38;
            UiAssets.nineSlice(g, "doctrine/gfx_tiled_decisions_bg_small", cx, cy, 123, 35, 6, .5);
            if (choice.selected()) UiAssets.nineSlice(g, "doctrine/gfx_tiled_research_bg", cx - 2, cy - 2, 127, 40, 8, .5);
            if (choice.enabled() || choice.selected()) UiAssets.draw(g, choice.texture(), cx + 5, cy + 2, 31, 31);
            else UiAssets.draw(g, choice.texture() + "/disabled", cx + 5, cy + 2, 31, 31);
            UiText.centered(g, font, choice.name(), cx + 36, cy + 3, 70, 24, TEXT);
            if (!choice.selected()) {
                var currency=UiKeywordIcons.decorateLines(Component.literal("£"+view.branch().toLowerCase(Locale.ROOT)+"_experience"),false);
                UiText.centered(g,font,currency,cx+37,cy+23,12,11,TEXT);
                UiText.left(g,font,choice.cost(),cx+50,cy+23,69,11,choice.enabled()?GOLD:0xFFC85F5C);
            }
        }
        UiAssets.draw(g, "doctrine/gfx_focus_filter_icon_bg", x + 250, y + 260, 21, 21);
        UiAssets.draw(g, "doctrine/gfx_focus_filter_historical", x + 254, y + 264, 14, 14);
        UiMenuStyle.close(g, x + 257, y + 6, 17, 17, TEXT);
    }
    private void centeredHeading(GuiGraphicsExtractor g, String text, int x, int y, int w, int h, float scale) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale);
        UiText.centered(g, font, text, 0, 0, Math.round(w / scale), Math.round(h / scale), TEXT);
        g.pose().popMatrix();
    }
    @Override public void tick() {
        if (!fixture && (!ClientCampaignHud.visible() || view != null && !view.country().equals(ClientCampaignHud.country()))) { revoke(); return; }
        if (pendingTicks > 0 && --pendingTicks == 0) { pendingKind = null; rebuildWidgets(); }
        if (view != null && pendingTicks == 0 && ++refreshTicks >= 40) { refreshTicks = 0; send(ProtocolDoctrinePayloads.Kind.REFRESH, "", ""); }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontalDelta, double verticalDelta) {
        if (view == null) return false;
        if (tooltipBounds != null && tooltipBounds.maxScroll() > 0
                && scrollableTooltips.keySet().stream().anyMatch(button -> button.isMouseOver(x, y))) {
            tooltipScroll = Math.clamp(tooltipScroll - (int)(verticalDelta * 30), 0, tooltipBounds.maxScroll());
            return true;
        }
        if (picking != null) popupScroll -= (int)verticalDelta;
        else horizontal -= (int)((horizontalDelta != 0 ? horizontalDelta : verticalDelta) * SLOT / 2);
        rebuildWidgets(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && picking != null) { picking = null; rebuildWidgets(); return true; }
        return super.keyPressed(event);
    }
    private void revoke() { transport.accept(new ProtocolDoctrinePayloads.Request(ProtocolDoctrinePayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), folder, "", "")); ScreenDialogCoordinator.setContent(null); }
    @Override public void onClose() {
        transport.accept(new ProtocolDoctrinePayloads.Request(ProtocolDoctrinePayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), folder, "", ""));
        ScreenDialogCoordinator.setContent(parent);
    }
    @Override public void removed() { InputSidebarMovement.release(minecraft); }
    @Override public int unitHudLeft() { return width; }
    @Override public boolean allowsMovement() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
