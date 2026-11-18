package dev.hoi.client.screen.politics;

import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.protocol.dialog.DialogView;
import dev.hoi.protocol.menu.MenuTab;
import dev.hoi.client.screen.dialog.DialogScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PoliticsChoice extends DialogScreen {
    public static final String VACANT = "hoi:politics/vacant";
    private static final String APPLIED = "적용 중";
    private static final int ROW = 37, BLOCKED = 0xFFA0A0A0;
    private final Screen parent;
    private final java.util.function.Consumer<dev.hoi.protocol.dialog.DialogPayloads.Action> sender;
    private final java.util.Map<net.minecraft.client.gui.components.AbstractWidget, String> officerTooltips = new java.util.IdentityHashMap<>();
    private String hoveredOfficer="";
    private dev.hoi.client.ui.tooltip.Tooltips.ScrolledTooltip tooltipBounds;
    public dev.hoi.client.ui.tooltip.Tooltips.ScrolledTooltip tooltipBounds() {return tooltipBounds;}
    private int tooltipScroll;
    private boolean pending;
    private int left, top, pane, bottom, scroll;
    public PoliticsChoice(DialogView view, Screen parent) { this(view,parent,null); }
    public PoliticsChoice(DialogView view, Screen parent,
            java.util.function.Consumer<dev.hoi.protocol.dialog.DialogPayloads.Action> sender) {
        super(view); this.parent=parent; this.sender=sender;
    }
    @Override public void update(DialogView next) {
        if(!view().token().equals(next.token()) || next.revision()<view().revision() || pending&&next.revision()==view().revision())return;
        pending=false; super.update(next);
    }
    private void submitPoliticsChoice(String id) {
        if(pending || view().token().isEmpty())return;
        if(sender==null && !net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(dev.hoi.protocol.dialog.DialogPayloads.Action.TYPE))return;
        pending=true;
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        var action=new dev.hoi.protocol.dialog.DialogPayloads.Action(view().token(),view().revision(),id);
        if(sender!=null)sender.accept(action);
        else net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(action);
    }
    public void dismiss() { minecraft.gui.setScreen(parent); }
    private boolean advisor() { return view().image().startsWith("advisor/"); }
    private boolean officer() { return view().image().startsWith("officer/"); }
    private boolean minister() { return view().image().startsWith("minister/"); }
    private boolean spirit() { return officer() && !view().image().equals("officer/political_power") && !view().image().equals("officer/command_power"); }
    private boolean grid() { return advisor() || officer() || minister(); }
    private boolean sourceTooltips() { return advisor() || officer() || minister(); }
    private int headerHeight() { return advisor() ? 57 : spirit() ? 61 : 28; }
    private int columns() { return grid() ? 2 : 1; }
    private int cellWidth() { return grid() ? Math.min(115, (pane - 10) / 2) : Math.min(231, pane - 10); }
    private int rowStep() { return grid() ? ROW : ROW + 1; }
    private int tileX(int index) { return left + 5 + index % columns() * cellWidth(); }
    private int tileY(int index) { return top + headerHeight() + index / columns() * rowStep() - scroll; }
    private boolean selectable(String id) { return view().choices().stream().anyMatch(c -> c.id().equals(id)); }
    public static String cost(DialogView.Tile tile) { int cut = tile.value().indexOf('\n'); return cut < 0 ? tile.value() : tile.value().substring(0, cut); }
    public static java.util.List<String> traits(DialogView.Tile tile) {
        int cut = tile.value().indexOf('\n');
        if (cut < 0) return java.util.List.of();
        var lines = new java.util.ArrayList<String>();
        for (String line : tile.value().substring(cut + 1).split("\\R")) if (!line.isBlank()) lines.add(line.strip());
        if (lines.size() > 2) { String rest = String.join(" ", lines.subList(1, lines.size())); lines.subList(1, lines.size()).clear(); lines.add(rest); }
        return java.util.List.copyOf(lines);
    }
    private String currencyIcon() {
        return view().image().equals("officer/command_power") ? "hud/command_power"
                : spirit() ? "hud/" + view().image().substring(8) + "_experience" : "politics/choice/political_power/frame_0";
    }
    @Override protected void init() {
        officerTooltips.clear();tooltipBounds=null;
        if (parent != null && (parent.width != width || parent.height != height)) parent.resize(width, height);
        left = spirit() ? UiPanelLayout.width(MenuTab.OFFICER_CORPS, width) : width * 710 / 2560;
        top = spirit() ? HudMenuBar.height(width) : 40;
        pane = Math.min(250, width - left - 6);
        if (pane < 170) { pane = Math.min(250, width - 12); left = width - pane - 6; }
        bottom = Math.min(height - 10, top + (advisor() ? 295 : spirit() ? 329 : 300));
        addRenderableWidget(new UiMenuButton("×", left + pane - 23, top + 3, 19, 19, this::onClose));
        int start = top + headerHeight();
        scroll = Math.clamp(scroll, 0, Math.max(0, ((view().tiles().size() + columns() - 1) / columns()) * rowStep() - (bottom - start)));
        for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = tileY(i);
            if (y < start || y + ROW > bottom) continue;
            boolean current = cost(tile).equals(APPLIED), removal = current && advisor() && selectable(VACANT);
            var button = new UiMenuButton(tile.name(), tileX(i), y, cellWidth(), ROW,
                    () -> submitPoliticsChoice(removal ? VACANT : tile.section())).caption("");
            if (!spirit()) button.background("none");
            button.active = !pending && (removal || selectable(tile.section()));
            if (sourceTooltips()) {
                button.setTooltip(null);
                officerTooltips.put(button, "§e" + tile.name() + "§r\n" + tile.detail());
            } else button.setTooltip(tile.name().equals("공석") || tile.icon().equals("politics/vacant") ? null : dev.hoi.client.ui.tooltip.Tooltips.create(Component.empty().append(Component.literal(tile.name()).withColor(dev.hoi.client.ui.tooltip.Tooltips.GOLD)).append(Component.literal("\n"+tile.detail().replace("적용 중\n", "").stripTrailing()))));
            addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (parent != null) { parent.extractRenderState(g, mx < left ? mx : -1, mx < left ? my : -1, delta); g.nextStratum(); }
        if (spirit()) UiMenuStyle.panel(g, left, top, pane, bottom - top + 4);
        else {
            UiAssets.nineSlice(g,"politics/choice/window/frame_0",left,top,pane,bottom-top,64,.5);
            int listTop=top+(advisor()?49:21);
            UiAssets.nineSlice(g,"politics/choice/window/frame_0",left,listTop,pane,bottom-listTop,64,.5);
            UiAssets.nineSlice(g,"politics/choice/overlay/frame_0",left-1,listTop,pane,bottom-listTop,144,.5);
        }
        int titleY = spirit() ? top + 1 : top + 2, subtitleY = spirit() ? top + 12 : top + 14;
        dev.hoi.client.ui.UiText.centeredSingleLine(g,font,Component.literal(view().title()).withStyle(s->s.withBold(true)),left+12,titleY,pane-24,10,0xFFFFFFFF);
        if (!view().subtitle().isEmpty()) dev.hoi.client.ui.UiText.centeredSingleLine(g,font,
                dev.hoi.client.ui.tooltip.Tooltips.officerText(view().subtitle()),left+24,subtitleY,pane-48,10,UiMenuStyle.TEXT);
        if (advisor()) {
            int frame = switch (view().image().substring(8)) { case "navy_chief" -> 1; case "air_chief" -> 2; case "high_command" -> 3; default -> 0; };
            UiAssets.draw(g,"politics/choice/header/frame_"+frame,left+1,top+22,pane-3,30);
        }
        if(spirit()) {
            String branch=view().image().substring(8);
            UiAssets.draw(g,"officer/"+branch+"/spirit",left+5,top+25,pane-10,33);
            String medal=view().title().contains("사관학교") ? "academy"
                    : view().title().contains("사령부") ? "command" : branch;
            UiAssets.draw(g,"officer/medal/"+medal,left+pane/2-20,top+22,40,38);
        }
        int start = top + headerHeight();
        if (!spirit()) for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = tileY(i), x = tileX(i), cell = cellWidth();
            if (y < start || y + ROW > bottom) continue;
            UiAssets.draw(g, "politics/choice/" + (grid() ? "grid_entry" : "list_entry") + "/frame_" + (cost(tile).equals(APPLIED) ? 1 : 0), x, y, cell, ROW);
        }
        superWidgets(g, mx, my, delta);
        for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = tileY(i), x = tileX(i), cell = cellWidth();
            if (y < start || y + ROW > bottom) continue;
            boolean current = cost(tile).equals(APPLIED);
            if (spirit() && current) g.fill(x + 1, y + 1, x + cell - 1, y + ROW - 1, 0x603F6D1D);
            if (grayscale(tile)) UiAssets.drawGrayscale(g, tile.icon(), x + 5, y + 2, 31, 34);
            else UiAssets.draw(g, tile.icon(), x + 5, y + 2, 31, 34);
            int costWidth = current ? 0 : costWidth(tile);
            if (grid()) {
                int textLeft = x + (minister() ? 34 : 38), textWidth = cell - (minister() ? 37 : 41);
                long now = net.minecraft.util.Util.getMillis();
                dev.hoi.client.ui.UiText.scrollingCell(g,font,Component.literal(tile.name()).withStyle(s->s.withBold(true)),textLeft,y+5,textWidth,10,0xFFFFFFFF,false,now);
                var traits = traits(tile);
                for (int line = 0; line < traits.size(); line++) {
                    int traitWidth = line==0||current ? textWidth : Math.max(1,textWidth-costWidth-2);
                    dev.hoi.client.ui.UiText.scrollingCell(g,font,dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(Tooltips.officerText(traits.get(line)),false),
                            textLeft,y+14+line*9,traitWidth,9,UiMenuStyle.ACCENT,false,now);
                }
                if (!current) drawCost(g, tile, x + cell - 4 - costWidth, y + 25);
            } else {
                int nameWidth = current ? cell - 46 : Math.max(1, 135 - costWidth / 2 - 44);
                dev.hoi.client.ui.UiText.left(g,font,Component.literal(tile.name()).withStyle(s->s.withBold(true)),x+42,y+3,nameWidth,31,0xFFFFFFFF);
                if (!current) drawCost(g, tile, x + 135 - costWidth / 2, y + 14);
            }
        }
        if (sourceTooltips()) {
            String text = officerTooltips.entrySet().stream().filter(entry -> dev.hoi.client.ui.tooltip.Tooltips.contains(entry.getKey(),mx,my))
                    .map(java.util.Map.Entry::getValue).findFirst().orElse("");
            if (!text.equals(hoveredOfficer)) {hoveredOfficer=text;tooltipScroll=0;}
            tooltipBounds=text.isEmpty()?null:dev.hoi.client.ui.tooltip.Tooltips.drawSourceScrollable(g,font,text,mx,my,width,height,tooltipScroll);
            if(tooltipBounds!=null)tooltipScroll=tooltipBounds.scroll();
        }
        if (view().tiles().isEmpty()) {
            if (sourceTooltips()) dev.hoi.client.ui.UiText.left(g,font,view().body(),left+6,start+12,pane-12,Math.max(10,bottom-start-12),UiMenuStyle.MUTED);
            else dev.hoi.client.ui.UiText.centeredAt(g,font,font.plainSubstrByWidth(view().body(),pane-12),left+pane/2,start+12,UiMenuStyle.MUTED);
        }
    }
    private String number(DialogView.Tile tile) { return cost(tile).replaceAll("§.", "").replace(" 정치력", ""); }
    static boolean blocked(DialogView.Tile tile) { return tile.overlay().equals("politics/choice/unavailable"); }
    static boolean grayscale(DialogView.Tile tile) { return blocked(tile) || tile.overlay().equals("politics/choice/locked"); }
    private int costWidth(DialogView.Tile tile) { return (blocked(tile) ? 10 : 0) + 12 + font.width(number(tile)); }
    private void drawCost(GuiGraphicsExtractor g, DialogView.Tile tile, int x, int y) {
        boolean blocked = blocked(tile);
        int color = blocked ? BLOCKED : 0xFF000000 | (cost(tile).startsWith("§c") ? dev.hoi.client.ui.tooltip.Tooltips.BAD : dev.hoi.client.ui.tooltip.Tooltips.GOLD);
        if (blocked) { UiAssets.draw(g,"keywords/agency_trigger_no",x,y,8,9); x += 10; }
        UiAssets.draw(g,currencyIcon(),x,y,10,10);
        dev.hoi.client.ui.UiText.leftSingleLine(g,font,Component.literal(number(tile)),x+12,y,font.width(number(tile)),10,color);
    }
    private void superWidgets(GuiGraphicsExtractor g, int mx, int my, float delta) {
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(g, mx, my, delta);
    }
    public Screen backdrop() { return parent; }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (parent != null && event.x() < left) {
            dismiss();
            return parent.mouseClicked(event, twice);
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (officerTooltips.keySet().stream().anyMatch(widget->dev.hoi.client.ui.tooltip.Tooltips.contains(widget,x,y))
                && com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)) {
            tooltipScroll-=(int)(vertical*30);return true;
        }
        if (x < left || x > left + pane) return false;
        scroll -= (int)(vertical * rowStep()); rebuildWidgets(); return true;
    }
    @Override public void choose(String choice) {submitPoliticsChoice(choice);}
    @Override public void onClose() {submitPoliticsChoice("");dismiss();}
}
