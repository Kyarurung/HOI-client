package dev.hoi.client.screen.agency;

import dev.hoi.client.ui.*;
import dev.hoi.protocol.agency.AgencyView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

public final class AgencyCandidates extends Screen {
    private final AgencyScreen parent;
    private final Consumer<String> submit;
    private final AgencyOperation operation;
    private final int parameter;
    private AgencyView view;
    private String query = "";
    private boolean byName, reverse, pending, sorted;
    private EditBox search;
    private int left, top, scroll, offsetX, offsetY;
    private boolean moving, scrolling;
    private double dragX, dragY;
    AgencyCandidates(AgencyScreen parent, AgencyView view, Consumer<String> submit) {
        this(parent, view, submit, null, -1);
    }
    AgencyCandidates(AgencyScreen parent, AgencyView view, Consumer<String> submit, AgencyOperation operation, int parameter) {
        super(Component.literal(operation == null ? view.candidates().getFirst().title() : UiCommandLabels.text("SELECT_OPERATIVE_TITLE", "할당할 요원을 선택")));
        this.parent = parent; this.view = view; this.submit = submit;
        this.operation = operation; this.parameter = parameter;
    }
    public AgencyScreen backdrop() { return parent; }
    void update(AgencyView next) {
        view = next; pending = false;
        if (operation != null && !operation.updatePicker(next, parameter)) { minecraft.gui.setScreen(parent); return; }
        if (candidates().isEmpty()) { onClose(); return; }
        boolean focused = search != null && search.isFocused();
        rebuildWidgets();
        if (focused) setFocused(search);
    }
    @Override public void tick() { parent.tick(); }
    private List<AgencyView.Candidate> candidates() {
        return operation == null ? view.candidates() : operation.candidates(parameter);
    }
    private List<AgencyView.Candidate> rows() {
        Comparator<AgencyView.Candidate> order = byName ? Comparator.comparing(AgencyView.Candidate::name) : Comparator.comparingInt(AgencyView.Candidate::level).reversed();
        if (reverse) order = order.reversed();
        var rows = candidates().stream().filter(c -> c.name().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)));
        return (sorted ? rows.sorted(order) : rows).toList();
    }
    @Override protected void init() {
        parent.resize(width,height);
        left = Math.clamp(width / 2 - 112 + offsetX, 4, Math.max(4,width - 279));
        top = Math.clamp(height / 2 - 112 + offsetY, UiMenuBar.height(width) + 4, Math.max(UiMenuBar.height(width) + 4,height - 308));
        addRenderableWidget(new UiMenuButton("모집 창 닫기",left+256,top+2,16,17,this::onClose).caption("").background("menu/close").sound("ui.close"));
        addRenderableWidget(new UiMenuButton("기술",left+7,top+19,41,15,()->{reverse=sorted&&!byName&&!reverse;byName=false;sorted=true;rebuildWidgets();}).background(sortTexture(sorted&&!byName)).sound("ui.scroll"));
        addRenderableWidget(new UiMenuButton("이름",left+48,top+19,41,15,()->{reverse=sorted&&byName&&!reverse;byName=true;sorted=true;rebuildWidgets();}).background(sortTexture(sorted&&byName)).sound("ui.scroll"));
        if (search == null) {
            search = new EditBox(font,left+101,top+22,133,12,Component.literal("검색"));
            search.setBordered(false); search.setMaxLength(64); search.setHint(Component.literal("검색"));
            search.setResponder(value->{query=value;scroll=0;rebuildWidgets();setFocused(search);});
        }
        search.setPosition(left+101,top+22); addRenderableWidget(search);
        var rows = rows(); scroll = Math.clamp(scroll,0,Math.max(0,rows.size()-5));
        for (int i=scroll;i<Math.min(rows.size(),scroll+5);i++) {
            var candidate=rows.get(i); int y=top+38+(i-scroll)*49;
            var row = new UiPanelButton(candidate.name(),left+7,y,255,49,()->{
                if (pending) return;
                pending=true; minecraft.gui.setScreen(parent); submit.accept(candidate.item());
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float delta) {
                    UiAssets.draw(g,"commander/ui/army",getX(),getY(),getWidth(),getHeight());
                    var skill=Component.literal(Integer.toString(candidate.level())).withStyle(net.minecraft.ChatFormatting.BOLD);
                    g.pose().pushMatrix();g.pose().translate(getX()+10,getY()+10);g.pose().scale(2);g.text(font,skill,0,0,UiMenuStyle.TEXT,false);g.pose().popMatrix();
                    UiAssets.draw(g,art(candidate.portrait()),getX()+28,getY()+2,33,44);
                    UiAssets.draw(g,"commander/officers/gfx_clip/frame_0",getX()+27,getY(),8,15);
                    UiText.leftSingleLine(g,font,Component.literal(candidate.rank()),getX()+64,getY()+6,50,9,0xFF171717);
                    UiText.leftSingleLine(g,font,Component.literal(candidate.name()),getX()+64,getY()+13,105,9,0xFF171717);
                    if (operation != null) {
                        var operative = view.operatives().stream().filter(value -> value.id().equals(candidate.item())).findFirst().orElse(null);
                        int frame = operative == null ? -1 : dev.hoi.client.map.agency.AgencyHud.missionFrame(view, operative);
                        if (frame >= 0) {
                            UiAssets.draw(g, AgencyOperation.sprite("GFX_mapicon_operative_mission_background"), getX()+51, getY()+33, 12, 12);
                            UiAssets.draw(g, "agency/operations/gfx_operative_mission_icons_small/frame_"+frame, getX()+52, getY()+34, 10, 10);
                        }
                    }
                    g.pose().pushMatrix();g.pose().translate(getX()+227.5f,getY()+9.25f);
                    dev.hoi.client.ui.UiCountryFlags.draw(g,"country/"+view.country().toLowerCase(Locale.ROOT)+"/flag","small2",0,0,13,9);g.pose().popMatrix();
                    for(int j=0;j<candidate.traits().size();j++)UiAssets.draw(g,art(candidate.traits().get(j).icon()),getX()+65+j*12,getY()+30,12,16);
                }
            };
            row.active=!pending&&(operation == null ? view.items().stream().anyMatch(item->item.id().equals(candidate.item())&&item.enabled()) : operation.candidateEnabled(parameter, candidate.item()));
            addRenderableWidget(row);
        }
        if (operation != null && operation.canResume(parameter)) {
            String label = UiCommandLabels.text("OPERATIVE_RESUME_MISSION_AFTER_OPERATION_CHECKBOX_LABEL", "임무 재개");
            var checkbox = new UiPanelButton(label, left + 50, top + 279, 13, 13, () -> {
                operation.resume(parameter, !operation.resumes(parameter)); rebuildWidgets();
            }) {
                @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float delta) {
                    UiAssets.drawLeaderControl(g, "agency/operations/gfx_generic_checkbox3/frame_" + (operation.resumes(parameter) ? 1 : 0), getX(), getY(), 13, 13, active);
                }
            };
            checkbox.active = !pending && !parent.awaitingResponse();
            checkbox.setTooltip(UiTooltips.create(Component.literal(UiCommandLabels.text("OPERATIVE_RESUME_MISSION_AFTER_OPERATION_TOOLTIP", "작전이 시작될 때 맡은 임무를 재개할지의 여부."))));
            addRenderableWidget(checkbox);
        }
    }
    private String sortTexture(boolean selected) { return "commander/officers/gfx_sort_up_down_button_83x29/frame_" + (selected ? reverse ? 2 : 1 : 0); }
    private UiScrollBar scrollbar() { return new UiScrollBar(left + 264, top + 38, top + 283, 5, rows().size()); }
    private String art(String source) {
        return "agency/operative/"+(source.replace('\\','/').toLowerCase(Locale.ROOT).contains("/kor/")?"korea/":"tfr/")+source.replace('\\','/').replaceAll("\\.[^.]+$","").toLowerCase(Locale.ROOT);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float delta) {
        if (operation == null) parent.extractRenderState(g,-1,-1,delta); else operation.extractRenderState(g,-1,-1,delta);
        g.nextStratum();
        UiAssets.draw(g,"commander/ui/panel",left,top,275,304);
        UiText.leftSingleLine(g,font,UiTooltips.officerText(operation == null && !view.candidates().isEmpty()?view.candidates().getFirst().title():getTitle().getString(),true),left+10,top+5,223,10,UiMenuStyle.TEXT);
        super.extractRenderState(g,mx,my,delta);
        scrollbar().draw(g, scroll);
        if (operation != null && operation.canResume(parameter)) UiText.leftSingleLine(g, font,
                Component.literal(UiCommandLabels.text("OPERATIVE_RESUME_MISSION_AFTER_OPERATION_CHECKBOX_LABEL", "임무 재개")), left + 75, top + 282, 170, 10, UiMenuStyle.TEXT);
        var rows=rows();
        for(int i=scroll;i<Math.min(rows.size(),scroll+5);i++) {
            var candidate=rows.get(i);int y=top+38+(i-scroll)*49;
            for(int j=0;j<candidate.traits().size();j++)if(mx>=left+72+j*12&&mx<left+84+j*12&&my>=y+30&&my<y+46)UiTooltips.drawSourceScrollable(g,font,candidate.traits().get(j).tooltip(),mx,my,width,height,0,260);
        }
    }
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        scroll-=(int)vertical;rebuildWidgets();return true;
    }
    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean twice) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            if (scrollbar().contains(event.x(), event.y())) {
                scrolling = !scrollbar().arrow(event.y());
                scroll = scrolling ? scrollbar().offset(event.y()) : scrollbar().step(event.y(), scroll, 1);
                rebuildWidgets(); return true;
            }
            if (event.x() >= left && event.x() < left + 254 && event.y() >= top && event.y() < top + 18) {
                moving = true; dragX = event.x() - left; dragY = event.y() - top; return true;
            }
        }
        return super.mouseClicked(event, twice);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        if (moving) {
            offsetX = (int)Math.round(event.x() - dragX) - (width / 2 - 112);
            offsetY = (int)Math.round(event.y() - dragY) - (height / 2 - 112);
            rebuildWidgets(); return true;
        }
        if (scrolling) { scroll = scrollbar().offset(event.y()); rebuildWidgets(); return true; }
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        if (moving || scrolling) { moving = false; scrolling = false; return true; }
        return super.mouseReleased(event);
    }
    @Override public void onClose() {
        if (operation != null) operation.closePicker();
        minecraft.gui.setScreen(operation == null ? parent : operation);
    }
    @Override public boolean isPauseScreen() { return false; }
}
