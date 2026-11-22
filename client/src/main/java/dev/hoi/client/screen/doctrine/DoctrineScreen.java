package dev.hoi.client.screen.doctrine;

import dev.hoi.client.ClientCampaignHud;
import dev.hoi.client.HoiClient;
import dev.hoi.client.input.InputSidebarMovement;
import dev.hoi.client.ui.*;
import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.protocol.doctrine.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.client.ui.hud.*;
import dev.hoi.client.ui.tooltip.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

public final class DoctrineScreen extends Screen implements InputSidebarMovement.InputMovementScreen {
    private static final int SLOT = 212, TEXT = 0xFFE3E1D8, GOLD = 0xFFE7C779;
    private final Screen parent;
    private final String token, target;
    private final Consumer<DoctrinePayloads.Request> transport;
    private final boolean fixture;
    private final LongSupplier clock;
    private long previewStarted, grandPreviewStarted;
    private DoctrineView view;
    private String folder, picking, previewId, previewGrandId;
    private int top, tracksTop, horizontal, popupScroll, refreshTicks, pendingTicks;
    private DoctrinePayloads.Kind pendingKind;
    private boolean advanced;
    private boolean popupScrolling;
    private boolean popupDragging;
    private int popupLeft = -1, popupTop = -1;
    private double popupGrabX, popupGrabY;
    private final Map<net.minecraft.client.gui.components.AbstractWidget, String> scrollableTooltips = new IdentityHashMap<>();
    private final Set<net.minecraft.client.gui.components.AbstractWidget> popupButtons = Collections.newSetFromMap(new IdentityHashMap<>());
    private String hoveredDetail = "";
    private int tooltipScroll;
    private Tooltips.ScrolledTooltip tooltipBounds;
    Tooltips.ScrolledTooltip tooltipBounds() { return tooltipBounds; }
    String hoveredDetail() { return hoveredDetail; }
    public DoctrineScreen(Screen parent, String folder) { this(parent, folder, ""); }
    public DoctrineScreen(Screen parent, String folder, String target) {
        this(parent, folder, UUID.randomUUID().toString(), null, request -> {
            if (ClientPlayNetworking.canSend(DoctrinePayloads.Request.TYPE)) ClientPlayNetworking.send(request);
        }, net.minecraft.util.Util::getMillis, target);
    }
    public DoctrineScreen(Screen parent, String folder, String token, DoctrineView view,
            Consumer<DoctrinePayloads.Request> transport) {
        this(parent, folder, token, view, transport, net.minecraft.util.Util::getMillis);
    }
    DoctrineScreen(Screen parent, String folder, String token, DoctrineView view,
            Consumer<DoctrinePayloads.Request> transport, LongSupplier clock) {
        this(parent, folder, token, view, transport, clock, "");
    }
    DoctrineScreen(Screen parent, String folder, String token, DoctrineView view,
            Consumer<DoctrinePayloads.Request> transport, LongSupplier clock, String target) {
        super(Component.literal("교리")); this.parent = parent; this.folder = folder;
        this.token = token; this.view = view; this.transport = transport; fixture = view != null;
        this.clock = clock; this.target = target;
    }
    public static void open(Screen parent, String folder) { open(parent, folder, ""); }
    public static void open(Screen parent, String folder, String target) {
        var screen = new DoctrineScreen(parent, folder, target); DialogCoordinator.setContent(screen); screen.open();
    }
    public String target() { return target; }
    public String folder() { return folder; }
    public void open() { send(DoctrinePayloads.Kind.OPEN, "", ""); }
    public void update(DoctrineView next) {
        if (!next.session().equals(token)) return;
        if (next.country().isEmpty() || view != null && !view.country().equals(next.country())) { revoke(); return; }
        if (view != null && next.revision() <= view.revision()) return;
        if (view != null && next.progressionRevision() > view.progressionRevision()
                && (pendingKind == DoctrinePayloads.Kind.SELECT_GRAND || pendingKind == DoctrinePayloads.Kind.SELECT_SUBDOCTRINE)) { picking = null; previewId = null; previewGrandId = null; }
        if (!folder.equals(next.folder())) { horizontal = 0; picking = null; previewGrandId = null; popupScroll = 0; }
        folder = next.folder(); view = next; pendingTicks = 0; pendingKind = null; refreshTicks = 0; rebuildWidgets();
    }
    private void send(DoctrinePayloads.Kind kind, String track, String item) {
        pendingKind = kind; pendingTicks = 100;
        transport.accept(new DoctrinePayloads.Request(kind, token, view == null ? 0 : view.revision(), folder, track, item, target));
        rebuildWidgets();
    }
    @Override protected void init() {
        scrollableTooltips.clear(); popupButtons.clear(); tooltipBounds = null;
        InputSidebarMovement.release(minecraft); top = Math.max(41, HudMenuBar.height(width)); tracksTop = top + 100;
        HudMenuBar.buttons(width, view == null ? "" : target.isEmpty() ? view.country() : ClientCampaignHud.country(), MenuTab.OFFICER_CORPS, tab -> {
            if (tab == MenuTab.RESEARCH) HoiClient.open(); else HoiClient.openMenu(tab);
        }, top).forEach(this::addRenderableWidget);
        if (view == null) return;
        horizontal = Math.clamp(horizontal, 0, Math.max(0, milestoneSlots().size() * SLOT - width));
        for (int i = 0; i < view.folders().size(); i++) {
            var f = view.folders().get(i); int x = 8 + i * 128;
            button(f.name(), x, top, 128, 31, () -> { folder = f.id(); picking = null; previewGrandId = null; horizontal = 0; send(DoctrinePayloads.Kind.REFRESH, "", ""); }, "", picking == null && pendingTicks == 0);
        }
        button("닫기", width - 22, top + 25, 17, 17, this::onClose, "닫기", true);
        var grand = view.grands().stream().filter(DoctrineView.Choice::selected).findFirst();
        button(grand.map(DoctrineView.Choice::name).orElse("대교리 선택"), width / 2 - 112, top + 38, 225, 65,
            () -> openPicker("grand"), grand.map(DoctrineView.Choice::detail).orElse(""), picking == null && pendingTicks == 0);
        var tracks = displayedTracks();
        for (int i = 0; i < tracks.size(); i++) {
            var track = displayedTrack(tracks.get(i)); int x = trackX(i);
            if (x + SLOT < 0 || x > width) continue;
            button(track.name(), x + 47, tracksTop + 80, 74, 55,
                () -> openPicker(track.id()), selectedDetail(track), (picking == null || grandPreview()) && pendingTicks == 0);
            var counter=new dev.hoi.client.ui.UiHoverRegion(track.name()+" mastery",x+121,tracksTop+89,37,14);
            counter.active=picking==null;
            String masteryDetail=grandPreview() ? "" : view.uiText().getOrDefault("mastery/"+i,"");
            if(!masteryDetail.isBlank())scrollableTooltips.put(counter,masteryDetail);
            addRenderableWidget(counter);
            for (int j = 0; j < track.rewards().size(); j++) {
                var reward = track.rewards().get(j); int y = tracksTop + 130 + j * 44;
                if (y >= height - 8) break;
                var hover=new UiHoverRegion("통달 "+reward.mastery(),x+(j%2==0?37:126),y,50,Math.min(44,height-8-y));
                hover.active=picking==null || grandPreview();
                if(!reward.detail().isBlank())scrollableTooltips.put(hover,reward.detail());
                addRenderableWidget(hover);
            }
        }
        var milestones = displayedMilestones();
        for (int i = 0; i < milestones.size(); i++) {
            var milestone = milestones.get(i);
            int x = milestoneX(i);
            if (x + SLOT < 0 || x > width) continue;
            var hover = new UiHoverRegion(milestone.name(), x + 80, tracksTop + 2, 53, 42);
            hover.active = pendingTicks == 0;
            if (!milestone.detail().isBlank()) scrollableTooltips.put(hover, milestone.detail());
            addRenderableWidget(hover);
        }
        if (picking != null) popupWidgets();
    }
    private String selectedDetail(DoctrineView.Track track) {
        String base = track.candidates().stream().filter(DoctrineView.Choice::selected).map(DoctrineView.Choice::detail).findFirst().orElse(track.name());
        return base;
    }
    static int tabFrame(String tab, String selected) { return tab.equals(selected) ? 0 : 1; }
    private void openPicker(String id) {
        if ("grand".equals(picking) && grandPreview()) previewGrandId = previewId;
        if ("grand".equals(id)) previewGrandId = null;
        picking = id; previewId = null; popupScroll = 0; popupLeft = -1; popupTop = -1;
        popupDragging = false; popupScrolling = false;
        rebuildWidgets();
    }
    private void closePicker() {
        if (previewGrandId != null) {
            picking = "grand"; previewId = previewGrandId; previewGrandId = null;
        } else { picking = null; previewId = null; }
        popupLeft = -1; popupTop = -1; popupDragging = false; popupScrolling = false;
        rebuildWidgets();
    }
    private int trackX(int index) { return Math.max(0, (width - milestoneSlots().size() * SLOT) / 2) + index * SLOT - horizontal; }
    private int popupX() {
        if (popupLeft >= 0) return Math.clamp(popupLeft, 4, Math.max(4, width - 284));
        if (picking.equals("grand")) return (width - 280) / 2;
        for (int i = 0; i < displayedTracks().size(); i++) if (displayedTracks().get(i).id().equals(picking))
            return Math.clamp(trackX(i) + 193, 4, Math.max(4, width - 284));
        return (width - 280) / 2;
    }
    private int popupY() { return Math.max(top + 4, Math.min(height - 292, popupTop >= 0 ? popupTop : picking.equals("grand") ? top + 183 : tracksTop + 105)); }
    private List<DoctrineView.Choice> choices() {
        if (picking == null) return List.of();
        if (picking.equals("grand")) return view.grands();
        return displayedTracks().stream().filter(t -> t.id().equals(picking)).findFirst().map(DoctrineView.Track::candidates).orElse(List.of());
    }
    DoctrineView.Choice preview() {
        return choices().stream().filter(choice -> choice.id().equals(previewId)).findFirst().orElse(null);
    }
    private void preview(String id) {
        previewStarted = clock.getAsLong();
        if ("grand".equals(picking)) grandPreviewStarted = previewStarted;
        previewId = id;
        rebuildWidgets();
    }
    int previewAlpha(int reward) {
        return Math.round(Math.clamp((clock.getAsLong() - previewStarted - 600L * reward) / 600f, 0, 1) * 255);
    }
    int milestonePreviewAlpha() {
        return Math.round(Math.clamp((clock.getAsLong() - grandPreviewStarted) / 400f, 0, 1) * 255);
    }
    int masteryAnimationFrame() {
        return (int)(Math.floorMod(clock.getAsLong(), 1000L) * 24 / 1000);
    }
    boolean grandPreview() {
        var grand = previewGrand();
        return grand != null && !grand.selected();
    }
    private DoctrineView.Choice previewGrand() {
        String id = "grand".equals(picking) ? previewId : previewGrandId;
        return view.grands().stream().filter(choice -> choice.id().equals(id)).findFirst().orElse(null);
    }
    List<DoctrineView.Track> displayedTracks() {
        if (!grandPreview()) return view.tracks();
        var tracks = new HashMap<String, DoctrineView.Track>();
        view.previewTracks().forEach(track -> tracks.put(track.id(), track));
        return previewGrand().previewTracks().stream().map(tracks::get).filter(Objects::nonNull).toList();
    }
    DoctrineView.Track displayedTrack(DoctrineView.Track track) {
        var choice = preview();
        if (choice == null || !track.id().equals(picking)) return track;
        return new DoctrineView.Track(track.id(), track.name(), track.background(), track.icon(), track.decoration(),
                track.selected(), choice.name(), track.mastery(), track.dailyMastery(), track.bank(), choice.rewards(), track.candidates(), track.milestone());
    }
    DoctrineView.Milestone displayedMilestone(DoctrineView.Track track) {
        var candidate = previewGrand();
        if (candidate != null) {
            return candidate.milestones().stream().filter(milestone -> milestone.track().equals(track.id()))
                .findFirst().orElse(DoctrineView.Milestone.NONE);
        }
        return track.milestone();
    }
    List<DoctrineView.Milestone> displayedMilestones() {
        var candidate = previewGrand();
        if (candidate != null) return candidate.milestones();
        return view.tracks().stream().map(track -> track.milestone().texture().isEmpty()
            ? new DoctrineView.Milestone(track.id(), track.name() + " milestone", track.icon(), false, track.milestone().detail())
            : track.milestone()).toList();
    }
    private int milestoneX(int index) {
        return trackX(milestoneSlots().indexOf(displayedMilestones().get(index).track()));
    }
    private List<String> milestoneSlots() {
        var slots = new LinkedHashSet<String>();
        displayedTracks().forEach(track -> slots.add(track.id()));
        displayedMilestones().forEach(milestone -> slots.add(milestone.track()));
        return List.copyOf(slots);
    }
    private void popupWidgets() {
        var choices = choices(); int x = popupX(), y = popupY();
        var previous = new HashSet<>(children());
        var preview = preview();
        button("고급 세부 사항", x + 250, y + 260, 21, 21, () -> { advanced = !advanced; rebuildWidgets(); },
            preview == null ? view.uiText().getOrDefault(advanced ? "DOCTRINE_DETAILS_BUTTON_DISABLE" : "DOCTRINE_DETAILS_BUTTON_ENABLE", "")
                : advanced ? preview.advancedDetail() : preview.detail(), pendingTicks == 0);
        if (preview != null) button("잠금 해제", x + 109, y + 260, 62, 17,
            () -> send(picking.equals("grand") ? DoctrinePayloads.Kind.SELECT_GRAND : DoctrinePayloads.Kind.SELECT_SUBDOCTRINE,
                picking.equals("grand") ? "" : picking, preview.id()), preview.assignDetail(), pendingTicks == 0 && preview.enabled() && previewGrandId == null);
        popupScroll = Math.clamp(popupScroll, 0, popupBar().maximum());
        button("선택 창 닫기", x + 257, y + 6, 17, 17, this::closePicker, "닫기", true);
        for (int i = 0; i < choices.size(); i++) {
            var choice = choices.get(i); int row = i / 2 - popupScroll;
            if (row < 0 || row >= 5) continue;
            int cx = x + 13 + i % 2 * 125, cy = y + 91 + row * 38;
            button(choice.name(), cx, cy, 123, Math.min(35, y + 258 - cy), () -> preview(choice.id()),
                advanced && !choice.advancedDetail().isEmpty() ? choice.advancedDetail() : choice.detail(), pendingTicks == 0, advanced && !choice.advancedDetail().isEmpty());
        }
        for (var child : children()) if (!previous.contains(child) && child instanceof net.minecraft.client.gui.components.AbstractWidget widget) popupButtons.add(widget);
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
        @Override public void playDownSound(net.minecraft.client.sounds.SoundManager manager) {
            dev.hoi.client.audio.AudioUiSounds.play(getMessage().getString().equals("잠금 해제") ? "ui.designer.tank.save"
                : getMessage().getString().contains("닫기") ? "ui.close" : "ui.click");
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        g.enableScissor(0, 0, width, top);
        HudMenuBar.drawBackground(g, width, ClientCampaignHud.hud(), 0);
        g.disableScissor();
        g.nextStratum();
        g.pose().pushMatrix();
        g.pose().translate(0, top + 20);
        g.pose().scale(.5f);
        UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_bg", 0, 0, width * 2, (height - top - 20) * 2, 64, 1);
        UiAssets.nineSlice(g, "doctrine/gfx_tiled_window_transparent", 0, 0, width * 2, (height - top - 20) * 2, 1, 1);
        UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_generic_overlay_bg1", 0, 0, width * 2, (height - top - 20) * 2, 268, 1);
        g.pose().popMatrix();
        if (view == null) { UiText.left(g, font, "불러오는 중…", 0, top + 40, width, 30, TEXT); HudMenuBar.drawTooltip(g, width, ClientCampaignHud.hud(), my < top ? mx : -1, my, 0); return; }
        int frame = view.folders().stream().filter(f -> f.id().equals(view.folder())).findFirst().orElseThrow().frame();
        for (int i = 0; i < view.folders().size(); i++) {
            var f = view.folders().get(i); int x = 8 + i * 128;
            UiAssets.draw(g, f.texture() + "/frame_" + tabFrame(f.id(), view.folder()), x, top, 128, 31);
            UiMenuStyle.title(g, font, UiMenuStyle.SCREEN, f.name(), x + 42, top + (tabFrame(f.id(), view.folder()) == 0 ? 15.5 : 13), 77, 0, TEXT);
        }
        UiAssets.draw(g, "doctrine/gfx_grand_doctrine_frame/frame_" + frame, width / 2 - 112, top + 38, 225, 65);
        var grand = view.grands().stream().filter(DoctrineView.Choice::selected).findFirst();
        grand.ifPresent(choice -> UiAssets.draw(g, choice.texture(), width / 2 - 14, top + 40, 32, 32));
        centeredHeading(g, grand.map(DoctrineView.Choice::name).orElse("대교리 선택"), width / 2 - 95, top + 74, 190, 18, 1.35f);
        if (grand.isEmpty() && !grandPreview()) {
            String missing = view.uiText().getOrDefault("NO_GRAND_DOCTRINE_ASSIGNED", "");
            String benefits = view.uiText().getOrDefault("GRAND_DOCTRINE_BENEFIT_DESCRIPTION", "");
            if (!missing.isBlank()) centeredHeading(g, missing, width / 2 - 100, top + 185, 200, 50, 1.35f);
            if (!benefits.isBlank()) centeredParagraph(g, TooltipKeywordIcons.decorateLines(Tooltips.officerText(benefits, true), false), width / 2 - 100, top + 235, 200, 50);
        }
        g.enableScissor(8, top + 95, width - 8, height - 8);
        var tracks = displayedTracks();
        for (int i = 0; i < tracks.size(); i++) {
            var track = displayedTrack(tracks.get(i)); int x = trackX(i);
            if (x + SLOT < 0 || x > width) continue;
            if (view.folder().equals("special_forces")) {
                g.enableScissor(x + 15, tracksTop + 45, x + 203, tracksTop + 387);
                g.pose().pushMatrix();
                g.pose().translate(x + 7.5f, tracksTop + 45);
                g.pose().scale(.5f);
                if (grandPreview()) UiAssets.drawGrayscale(g, track.background(), 0, 0, 464, 290);
                else UiAssets.drawTinted(g, track.background(), 0, 0, 464, 290, 0xFFFFDC66);
                g.pose().popMatrix();
                g.disableScissor();
            } else drawTrackAsset(g, track.background(), x + 15, tracksTop + 45, 188, 342);
            drawTrackAsset(g, "doctrine/gfx_track_frame/frame_0", x + 6, tracksTop + 36, 200, 352);
            drawTrackAsset(g, track.decoration() + "/frame_0", x + 10, tracksTop + 4, 191, 41);
            centeredHeading(g, track.name(), x + 31, tracksTop + 52, 150, 22, 1.35f);
            drawTrackAsset(g, "doctrine/gfx_subdoctrine_button_and_text_frame/frame_" + frame, x + 47, tracksTop + 80, 118, 55);
            String visibleChoice = preview() != null && track.id().equals(picking) ? preview().id() : track.selected();
            track.candidates().stream().filter(choice -> choice.id().equals(visibleChoice)).findFirst()
                .ifPresent(choice -> drawTrackAsset(g, choice.texture(), x + 91, tracksTop + 82, 31, 31));
            UiText.centered(g, font, track.nameplate(), x + 53, tracksTop + 112, 106, 15, TEXT);
            drawTrackAsset(g, "doctrine/gfx_mastery_amount_frame_big", x + 121, tracksTop + 89, 37, 14);
            boolean banking=track.selected().isEmpty()||!track.rewards().isEmpty()&&track.rewards().stream().allMatch(DoctrineView.Reward::unlocked);
            var masteryIcon=TooltipKeywordIcons.decorateLines(Component.literal("£doctrine_mastery"),false);
            var masteryIconLine=firstLine(masteryIcon,10);
            String masteryLine=banking?track.bank():track.mastery();
            int masteryIconWidth=Math.round(font.width(masteryIconLine)*UiText.scale(font)),masteryWidth=Math.min(22,Math.round(font.width(masteryLine)*UiText.scale(font)));
            int masteryLeft=x+123+(33-masteryIconWidth-1-masteryWidth)/2;
            if (grandPreview()) UiAssets.drawGrayscale(g, "keywords/doctrine_mastery", masteryLeft, tracksTop + 91, masteryIconWidth, 8);
            else UiText.text(g,font,masteryIconLine,masteryLeft,tracksTop+91,GOLD);
            UiText.marquee(g,font,Tooltips.officerText(masteryLine),masteryLeft+masteryIconWidth+1,tracksTop+91,masteryWidth,UiText.scale(font),grandPreview() ? TEXT : GOLD,true,-1);
            for (int j = 0; j < track.rewards().size(); j++) {
                var reward = track.rewards().get(j); int ry = tracksTop + 130 + j * 44;
                if(ry>=height-8)break;
                drawTrackAsset(g,"doctrine/gfx_techtree_line_down_currently_researching/frame_0",x+102,ry+26,8,18);
                drawTrackAsset(g,"doctrine/gfx_mastery_progress_bar_bg",x+98,ry,16,36);
                int fill=preview()!=null && track.id().equals(picking) && !preview().selected()?0:Math.round(36*rewardProgress(track,j));
                if(fill>0) {
                    g.enableScissor(x+98,ry,x+114,ry+fill);
                    UiAssets.draw(g,"doctrine/gfx_mastery_progress_bar_animated_" + view.folder() + "/frame_" + masteryAnimationFrame(),x+98,ry,16,36);
                    g.disableScissor();
                }
                drawTrackAsset(g,"doctrine/gfx_mastery_amount_frame/frame_" + (reward.unlocked() ? frame + 1 : 0),x+92,ry+32,28,13);
                String threshold = rewardThreshold(track, j);
                var numberBounds = font.prepareText(Component.literal(threshold).getVisualOrderText(), 0, 0, 0xFFFFFFFF, false, false, 0).bounds();
                float numberScale = 4f / Math.max(1, numberBounds == null ? font.lineHeight : numberBounds.height() - 1);
                g.pose().pushMatrix();
                g.pose().translate(x + 94 + (24 - font.width(threshold) * numberScale) / 2, UiText.centeredY(font, threshold, ry + 32, 13, numberScale));
                g.pose().scale(numberScale);
                g.text(font, threshold, 0, 0, 0xFFE0C956);
                g.pose().popMatrix();
                if (!grandPreview() && preview() != null && track.id().equals(picking) && !preview().selected()) {
                    g.pose().pushMatrix();g.pose().translate(x + (j % 2 == 0 ? 29 : 118), ry - 4.5f);g.pose().scale(.5f);
                    int alpha = previewAlpha(j);
                    if (alpha > 0) UiAssets.drawTinted(g, "doctrine/gfx_ongoing_generic_glow_yellow/frame_0", 0, 0, 141, 115, alpha << 24 | 0xFFFFFF);
                    g.pose().popMatrix();
                }
                drawTrackAsset(g, "doctrine/gfx_mastery_reward_frame/frame_" + rewardFrame(frame, reward.unlocked()), x + (j % 2 == 0 ? 37 : 126), ry, 50, 44);
                drawTrackAsset(g, "doctrine/gfx_mastery_reward_icons/frame_" + (Math.min(4, j) + (reward.unlocked() ? 0 : 5)), x + (j % 2 == 0 ? 37 : 126), ry, 50, 44);
                drawTrackAsset(g,"doctrine/gfx_mastery_reward_level/frame_"+Math.min(4,j),x+(j%2==0?38:159),ry+20,23,23);
            }
        }
        g.disableScissor();
        g.enableScissor(8, top + 28, width - 8, height - 8);
        var milestones = displayedMilestones();
        for (int i = 0; i < milestones.size(); i++) {
            var milestone = milestones.get(i);
            int x = milestoneX(i);
            if (x + SLOT < 0 || x > width) continue;
            if (grandPreview()) {
                int alpha = milestonePreviewAlpha();
                if (alpha > 0) {
                    g.pose().pushMatrix(); g.pose().translate(x + 53.5f, tracksTop - 20); g.pose().scale(.75f);
                    UiAssets.drawTinted(g, "doctrine/gfx_ongoing_generic_glow_yellow/frame_0", 0, 0, 141, 115, alpha << 24 | 0xFFFFFF);
                    g.pose().popMatrix();
                }
            }
            UiAssets.draw(g, milestone.texture() + "/frame_" + (!grandPreview() && milestone.unlocked() ? 0 : 1), x + 80, tracksTop + 2, 53, 42);
        }
        g.disableScissor();
        UiAssets.drawExact(g, "menu/close", width - 22, top + 25, 16, 16.5f);
        if (picking != null) drawPopup(g);
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable renderable)
            renderable.extractRenderState(g, mx, my, delta);
        boolean overPopup = picking != null && mx >= popupX() && mx < popupX() + 280 && my >= popupY() && my < popupY() + 288;
        String detail = scrollableTooltips.entrySet().stream().filter(e -> contains(e.getKey(), mx, my) && (!overPopup || popupButtons.contains(e.getKey())))
            .map(Map.Entry::getValue).findFirst().orElse("");
        if (!detail.equals(hoveredDetail)) { hoveredDetail = detail; tooltipScroll = 0; }
        tooltipBounds = detail.isEmpty() ? null : Tooltips.drawSourceScrollable(g, font, detail, mx, my, width, height, tooltipScroll);
        if (tooltipBounds != null) tooltipScroll = tooltipBounds.scroll();
        HudMenuBar.drawTooltip(g, width, ClientCampaignHud.hud(), my < top ? mx : -1, my, 0);
    }
    private net.minecraft.util.FormattedCharSequence firstLine(Component text,int width) {
        var lines=Tooltips.split(font,text,Math.max(1,(int)(width/UiText.scale(font))));
        return lines.isEmpty()?Component.empty().getVisualOrderText():lines.getFirst();
    }
    private void drawTrackAsset(GuiGraphicsExtractor g, String path, int x, int y, int width, int height) {
        if (grandPreview()) UiAssets.drawGrayscale(g, path, x, y, width, height);
        else UiAssets.draw(g, path, x, y, width, height);
    }
    static int rewardFrame(int branch, boolean unlocked) { return branch * 2 + (unlocked ? 0 : 1); }
    static String rewardThreshold(DoctrineView.Track track,int index) {
        try {
            java.math.BigDecimal amount=java.math.BigDecimal.ZERO;
            for(int i=0;i<=index;i++)amount=amount.add(new java.math.BigDecimal(track.rewards().get(i).mastery()));
            return amount.stripTrailingZeros().toPlainString();
        }catch(NumberFormatException unknown){return "0";}
    }
    static float rewardProgress(DoctrineView.Track track,int index) {
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
        UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_window_w_close", x, y, 280, 288, 64, .5);
        var preview = preview();
        String heading = preview != null ? preview.name() : view.uiText().getOrDefault(picking.equals("grand") ? "GRAND_DOCTRINE_SELECTION_TITLE" : "SUB_DOCTRINE_SELECTION_TITLE",
            picking.equals("grand") ? "대교리 선택" : "하위 교리 선택");
        centeredHeading(g, heading, x + 15, y + 11, 250, 20, 1.5f);
        UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_paper_bg", x + 15, y + 33, 250, 49, 64, .5);
        UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_paper_small_bg", x + 10, y + 85, 260, 173, 64, .5);
        String description = preview != null ? preview.description() : view.uiText().getOrDefault(picking.equals("grand") ? "GRAND_DOCTRINE_DESCRIPTION" : "SUBDOCTRINE_DESCRIPTION", "");
        centeredParagraph(g, TooltipKeywordIcons.decorateLines(Tooltips.officerText(description, false), false), x + 25, y + 39, 230, 40);
        g.enableScissor(x + 10, y + 85, x + 270, y + 258);
        for (int i = 0; i < choices.size(); i++) {
            var choice = choices.get(i); int row = i / 2 - popupScroll;
            if (row < 0 || row >= 5) continue;
            int cx = x + 13 + i % 2 * 125, cy = y + 91 + row * 38;
            UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_decisions_bg_small", cx, cy, 123, 35, 16, .5);
            if (choice.id().equals(previewId) || previewId == null && choice.selected()) UiAssets.tiledNineSlice(g, "doctrine/gfx_tiled_research_bg", cx - 2, cy - 2, 127, 40, 32, .5);
            if (choice.enabled() || choice.selected()) UiAssets.draw(g, choice.texture(), cx + 5, cy + 2, 31, 31);
            else UiAssets.draw(g, choice.texture() + "/disabled", cx + 5, cy + 2, 31, 31);
            var name = TooltipKeywordIcons.decorateLines(Component.literal((choice.requirementsMet() || choice.selected() ? "" : "£trigger_no ") + choice.name()), false);
            UiText.centered(g, font, name, cx + 36, cy + 3, 63, 24, TEXT);
            if (!choice.selected()) {
                String currency = "hud/" + view.branch().toLowerCase(Locale.ROOT) + "_experience";
                float scale = 8f / font.lineHeight;
                int costX = cx + 119 - Math.round(font.width(choice.cost()) * scale);
                UiAssets.draw(g, currency, costX - 9, cy + 25, 8, 8);
                g.pose().pushMatrix();g.pose().translate(costX, cy + 25);g.pose().scale(scale);
                g.text(font, choice.cost(), 0, 0, canAfford(choice) ? GOLD : 0xFFC85F5C);
                g.pose().popMatrix();
            }
        }
        g.disableScissor();
        popupBar().draw(g, popupScroll);
        if (preview != null) {
            UiAssets.draw(g, "commander/commands/button_123x34/frame_0", x + 109, y + 260, 62, 17);
            drawUnlockLabel(g, preview, x + 111, y + 260);
        }
        UiAssets.draw(g, "doctrine/gfx_focus_filter_icon_bg", x + 250, y + 260, 21, 21);
        UiAssets.draw(g, "doctrine/gfx_focus_filter_historical", x + 254, y + 264, 14, 14);
        UiAssets.drawExact(g, "menu/close", x + 257.5f, y + 6.5f, 16, 16.5f);
    }
    private void drawUnlockLabel(GuiGraphicsExtractor g, DoctrineView.Choice preview, int x, int y) {
        var caption = Component.literal(preview.cost() + " 잠금 해제");
        int icon = 10, gap = 2;
        float scale = UiText.scale(font);
        int textWidth = Math.min(58 - icon - gap, Math.round(font.width(caption) * scale)), left = x + (58 - icon - gap - textWidth) / 2;
        UiAssets.draw(g, "hud/" + view.branch().toLowerCase(Locale.ROOT) + "_experience", left, y + (17 - icon) / 2, icon, icon);
        UiText.marquee(g, font, caption, left + icon + gap, UiText.centeredY(font, caption.getVisualOrderText(), y, 17, scale), textWidth, scale, preview.enabled() ? TEXT : 0xFF91918E, true, -1);
    }
    private boolean canAfford(DoctrineView.Choice choice) {
        if (!target.isEmpty()) return true;
        try { return new java.math.BigDecimal(view.experience()).compareTo(new java.math.BigDecimal(choice.cost())) >= 0; }
        catch (NumberFormatException unknown) { return false; }
    }
    private boolean contains(net.minecraft.client.gui.components.AbstractWidget widget, double x, double y) {
        return widget.visible && x >= widget.getX() && x < widget.getRight() && y >= widget.getY() && y < widget.getBottom();
    }
    private UiScrollBar popupBar() { return new UiScrollBar(popupX() + 268, popupY() + 85, popupY() + 258, 4, (choices().size() + 1) / 2); }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (picking != null && event.x() >= popupX() && event.x() < popupX() + 280 && event.y() >= popupY() && event.y() < popupY() + 288) {
            if (event.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return true;
            for (var widget : popupButtons) if (contains(widget, event.x(), event.y()) && widget.mouseClicked(event, twice)) return true;
            if (event.y() < popupY() + 32) {
                popupGrabX = event.x() - popupX(); popupGrabY = event.y() - popupY(); popupDragging = true;
                return true;
            }
            if (!popupBar().contains(event.x(), event.y())) return true;
        }
        if (picking != null && event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && popupBar().contains(event.x(), event.y())) {
            popupScrolling = !popupBar().arrow(event.y());
            popupScroll = popupScrolling ? popupBar().offset(event.y()) : popupBar().step(event.y(), popupScroll, 1);
            rebuildWidgets(); return true;
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (popupDragging && picking != null) {
            popupLeft = Math.clamp((int)Math.round(event.x() - popupGrabX), 4, Math.max(4, width - 284));
            popupTop = Math.max(top + 4, Math.min(height - 292, (int)Math.round(event.y() - popupGrabY)));
            rebuildWidgets(); return true;
        }
        if (popupScrolling && picking != null) { popupScroll = popupBar().offset(event.y()); rebuildWidgets(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (popupDragging) { popupDragging = false; return true; }
        if (popupScrolling) { popupScrolling = false; return true; }
        return super.mouseReleased(event);
    }
    private void centeredParagraph(GuiGraphicsExtractor g, Component text, int x, int y, int width, int height) {
        float scale = UiText.scale(font);
        var lines = Tooltips.split(font, text, Math.round(width / scale));
        while (lines.size() * (font.lineHeight + 1) * scale > height && scale > .5f) {
            scale -= .025f;
            lines = Tooltips.split(font, text, Math.round(width / scale));
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y + (height - lines.size() * (font.lineHeight + 1) * scale) / 2);
        g.pose().scale(scale);
        for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), Math.round((width / scale - font.width(lines.get(i))) / 2), i * (font.lineHeight + 1), TEXT);
        g.pose().popMatrix();
    }
    private void centeredHeading(GuiGraphicsExtractor g, String text, int x, int y, int w, int h, float scale) {
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().scale(scale);
        UiText.centered(g, font, Tooltips.officerText(text, true), 0, 0, Math.round(w / scale), Math.round(h / scale), TEXT);
        g.pose().popMatrix();
    }
    @Override public void tick() {
        if (!fixture && (!ClientCampaignHud.visible() || view != null && !view.country().equals(target.isEmpty() ? ClientCampaignHud.country() : target))) { revoke(); return; }
        if (pendingTicks > 0 && --pendingTicks == 0) { pendingKind = null; rebuildWidgets(); }
        if (view != null && pendingTicks == 0 && ++refreshTicks >= 40) { refreshTicks = 0; send(DoctrinePayloads.Kind.REFRESH, "", ""); }
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontalDelta, double verticalDelta) {
        if (view == null) return false;
        if (tooltipBounds != null && tooltipBounds.maxScroll() > 0
                && scrollableTooltips.keySet().stream().anyMatch(button -> contains(button, x, y))) {
            tooltipScroll = Math.clamp(tooltipScroll - (int)(verticalDelta * 30), 0, tooltipBounds.maxScroll());
            return true;
        }
        if (picking != null && x >= popupX() && x < popupX() + 280 && y >= popupY() && y < popupY() + 288) popupScroll -= (int)verticalDelta;
        else horizontal -= (int)((horizontalDelta != 0 ? horizontalDelta : verticalDelta) * SLOT / 2);
        rebuildWidgets(); return true;
    }
    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE && picking != null) { closePicker(); return true; }
        return super.keyPressed(event);
    }
    private void revoke() { transport.accept(new DoctrinePayloads.Request(DoctrinePayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), folder, "", "")); DialogCoordinator.setContent(null); }
    @Override public void onClose() {
        transport.accept(new DoctrinePayloads.Request(DoctrinePayloads.Kind.CLOSE, token, view == null ? 0 : view.revision(), folder, "", ""));
        DialogCoordinator.setContent(parent);
    }
    @Override public void removed() { InputSidebarMovement.release(minecraft); }
    @Override public int unitHudLeft() { return width; }
    @Override public boolean allowsMovement() { return false; }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean isInGameUi() { return true; }
}
