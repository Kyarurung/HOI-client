package dev.hoi.client.screen;

import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.client.ui.UiMenuButton;
import dev.hoi.client.ui.UiMenuStyle;
import dev.hoi.client.ui.UiPanelLayout;
import dev.hoi.client.ui.UiAssets;
import dev.hoi.protocol.ProtocolDialogView;
import dev.hoi.protocol.ProtocolMenuTab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ScreenPoliticsChoice extends ScreenDialog {
    private final Screen parent;
    private final java.util.function.Consumer<dev.hoi.protocol.ProtocolDialogPayloads.Action> sender;
    private final java.util.Map<net.minecraft.client.gui.components.AbstractWidget, String> officerTooltips = new java.util.IdentityHashMap<>();
    private String hoveredOfficer="";
    private dev.hoi.client.ui.UiTooltips.ScrolledTooltip tooltipBounds;
    dev.hoi.client.ui.UiTooltips.ScrolledTooltip tooltipBounds() {return tooltipBounds;}
    private int tooltipScroll;
    private boolean pending;
    private int left, top, pane, bottom, scroll;
    ScreenPoliticsChoice(ProtocolDialogView view, Screen parent) { this(view,parent,null); }
    ScreenPoliticsChoice(ProtocolDialogView view, Screen parent,
            java.util.function.Consumer<dev.hoi.protocol.ProtocolDialogPayloads.Action> sender) {
        super(view); this.parent=parent; this.sender=sender;
    }
    @Override void update(ProtocolDialogView next) {
        if(!view().token().equals(next.token()) || next.revision()<view().revision() || pending&&next.revision()==view().revision())return;
        pending=false; super.update(next);
    }
    private void submitPoliticsChoice(String id) {
        if(pending || view().token().isEmpty())return;
        if(sender==null && !net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(dev.hoi.protocol.ProtocolDialogPayloads.Action.TYPE))return;
        pending=true;
        for(var child:children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget widget)widget.active=false;
        var action=new dev.hoi.protocol.ProtocolDialogPayloads.Action(view().token(),view().revision(),id);
        if(sender!=null)sender.accept(action);
        else net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(action);
    }
    void dismiss() { minecraft.gui.setScreen(parent); }
    private boolean officer() { return view().image().startsWith("officer/"); }
    private boolean spirit() { return officer() && !view().image().equals("officer/political_power") && !view().image().equals("officer/command_power"); }
    private int replacementHeight() { return spirit() && !view().subtitle().isEmpty() ? 11 : 0; }
    private int headerHeight() { return spirit() ? 63 + replacementHeight() : 29; }
    private int columns() { return officer() ? 2 : 1; }
    private int cellWidth() { return (pane - 12 - (columns() - 1) * 6) / columns(); }
    private int tileX(int index) { return left + 6 + index % columns() * (cellWidth() + 6); }
    private int tileY(int index) { return top + headerHeight() + index / columns() * 39 - scroll; }
    @Override protected void init() {
        officerTooltips.clear();tooltipBounds=null;
        left = UiPanelLayout.width(officer() ? ProtocolMenuTab.OFFICER_CORPS : ProtocolMenuTab.POLITICS, width);
        top = UiMenuBar.height(width);
        pane = Math.min(240, width - left - 6);
        if (pane < 170) { pane = Math.min(240, width - 12); left = width - pane - 6; }
        bottom = Math.min(height - 10, top + (spirit() ? 323 : 300));
        addRenderableWidget(new UiMenuButton("×", left + pane - 23, top + 3, 19, 19, this::onClose));
        int start = top + headerHeight();
        scroll = Math.clamp(scroll, 0, Math.max(0, ((view().tiles().size() + columns() - 1) / columns()) * 39 - (bottom - start)));
        for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = tileY(i);
            if (y < start || y + 36 > bottom) continue;
            var button = new UiMenuButton(tile.name(), tileX(i), y, cellWidth(), 36,
                    () -> submitPoliticsChoice(tile.section())).caption("");
            button.active = !pending && view().choices().stream().anyMatch(c -> c.id().equals(tile.section()));
            button.setTooltip(officer() ? dev.hoi.client.ui.UiTooltips.createOfficer("§e" + tile.name() + "§r\n" + tile.detail()) : tile.name().equals("공석") || tile.icon().equals("politics/vacant") ? null : dev.hoi.client.ui.UiTooltips.create(Component.empty().append(Component.literal(tile.name()).withColor(dev.hoi.client.ui.UiTooltips.GOLD)).append(Component.literal("\n"+tile.detail().replace("적용 중\n", "").stripTrailing()))));
            if (officer()) {
                button.setTooltip(null);
                officerTooltips.put(button, "§e" + tile.name() + "§r\n" + tile.detail());
            }
            addRenderableWidget(button);
        }
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        if (parent != null) { parent.extractRenderState(g, mx < left ? mx : -1, mx < left ? my : -1, delta); g.nextStratum(); }
        UiMenuStyle.panel(g, left, top, pane, bottom - top + 4);
        var heading=officer()?Component.empty().append(dev.hoi.client.ui.UiTooltips.officerText("§e"+view().title()))
                .append(Component.literal(" 선택").withColor(UiMenuStyle.TEXT)):Component.literal(view().title());
        dev.hoi.client.ui.UiText.centered(g,font,heading,left+4,top+2,pane-29,21,UiMenuStyle.TEXT);
        if(spirit()) {
            int replacement=replacementHeight();
            if(replacement>0) dev.hoi.client.ui.UiText.leftSingleLine(g,font,
                    dev.hoi.client.ui.UiTooltips.officerText(view().subtitle()),left+6,top+24,pane-12,11,UiMenuStyle.TEXT);
            String branch=view().image().substring(8);
            UiAssets.draw(g,"officer/"+branch+"/spirit",left+5,top+25+replacement,pane-10,33);
            String medal=view().title().contains("사관학교") ? "academy"
                    : view().title().contains("사령부") ? "command" : branch;
            UiAssets.draw(g,"officer/medal/"+medal,left+pane/2-20,top+22+replacement,40,38);
        }
        superWidgets(g, mx, my, delta);
        int start = top + headerHeight();
        for (int i = 0; i < view().tiles().size(); i++) {
            var tile = view().tiles().get(i); int y = tileY(i), x = tileX(i), cell = cellWidth();
            if (y < start || y + 36 > bottom) continue;
            if (tile.value().equals("적용 중")) g.fill(x + 1, y + 1, x + cell - 1, y + 35, 0x603F6D1D);
            UiAssets.drawContent(g, tile.icon(), x + 3, y + 3, 30, 30);
            int textLeft = x + 37, textWidth = cell - 43;
            dev.hoi.client.ui.UiText.centered(g,font,Component.literal(tile.name()),textLeft,y+3,textWidth,13,UiMenuStyle.TEXT);
            if (!tile.value().equals("적용 중")) {
                String number=tile.value().replaceAll("§.", "").replace(" 정치력", "");
                int color=tile.value().startsWith("§c")?dev.hoi.client.ui.UiTooltips.BAD:dev.hoi.client.ui.UiTooltips.GOLD;
                int costX=textLeft;
                String currencyIcon = view().image().equals("officer/command_power") ? "hud/command_power" : officer() && !view().image().equals("officer/political_power")
                        ? "hud/" + view().image().substring(8) + "_experience" : "hud/political_power";
                UiAssets.draw(g,currencyIcon,costX,y+20,10,10);
                text(g,Component.literal(number),costX+12,y+18,textWidth-12,14,0xFF000000|color);
            }
        }
        if (officer()) {
            String text = officerTooltips.entrySet().stream().filter(entry -> dev.hoi.client.ui.UiTooltips.contains(entry.getKey(),mx,my))
                    .map(java.util.Map.Entry::getValue).findFirst().orElse("");
            if (!text.equals(hoveredOfficer)) {hoveredOfficer=text;tooltipScroll=0;}
            tooltipBounds=text.isEmpty()?null:dev.hoi.client.ui.UiTooltips.drawSourceScrollable(g,font,text,mx,my,width,height,tooltipScroll);
            if(tooltipBounds!=null)tooltipScroll=tooltipBounds.scroll();
        }
        if (view().tiles().isEmpty()) {
            if (officer()) dev.hoi.client.ui.UiText.left(g,font,view().body(),left+6,start+12,pane-12,Math.max(10,bottom-start-12),UiMenuStyle.MUTED);
            else dev.hoi.client.ui.UiText.centeredAt(g,font,font.plainSubstrByWidth(view().body(),pane-12),left+pane/2,start+12,UiMenuStyle.MUTED);
        }
    }
    private void text(GuiGraphicsExtractor g, Component value, int x, int y, int width, int height, int color) {
        if (officer()) dev.hoi.client.ui.UiText.left(g,font,value,x,y,width,height,color);
        else dev.hoi.client.ui.UiText.centered(g,font,value,x,y,width,height,color);
    }
    private void superWidgets(GuiGraphicsExtractor g, int mx, int my, float delta) {
        for (var child : children()) if (child instanceof net.minecraft.client.gui.components.Renderable widget) widget.extractRenderState(g, mx, my, delta);
    }
    Screen backdrop() { return parent; }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (parent != null && event.x() < left) {
            dismiss();
            return parent.mouseClicked(event, twice);
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (officerTooltips.keySet().stream().anyMatch(widget->dev.hoi.client.ui.UiTooltips.contains(widget,x,y))
                && com.mojang.blaze3d.platform.InputConstants.isKeyDown(com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)) {
            tooltipScroll-=(int)(vertical*30);return true;
        }
        if (x < left || x > left + pane) return false;
        scroll -= (int)(vertical * 39); rebuildWidgets(); return true;
    }
    @Override protected void choose(String choice) {submitPoliticsChoice(choice);}
    @Override public void onClose() {submitPoliticsChoice("");dismiss();}
}
