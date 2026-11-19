package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class IndustryRegimentChecks {
    public static void run(ClientGameTestContext context) {
        for (String fixture : List.of("industry-columns-fixture.json","industry-regiment-fixture.json")) {
            IndustryView v;
            try (var input=IndustryRegimentChecks.class.getResourceAsStream("/"+fixture)) {
                v=new IndustryPayloads.Response(new String(Objects.requireNonNull(input).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view();
            } catch (Exception e) { throw new AssertionError(e); }
            var template=v.templates().stream().filter(t->t.name().equals("열별 편제")).findFirst().orElseThrow();
            var requests=new ArrayList<IndustryPayloads.Request>();
            context.getInput().resizeWindow(1600,1000);context.waitTicks(2);
            context.setScreen(()->new IndustryScreen(MenuTab.RECRUITMENT,v.session(),copy(v,template,null,1),requests::add));
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();
                press(button(screen,"새 사단 편제 설계"));
                check(requests.getLast().action()==IndustryPayloads.Action.NEW_TEMPLATE,"New designer requests an empty draft");
                var empty=new IndustryView.Template("draft","새 사단 편제",List.of(),List.of(),v.draft().stats(),Map.of(),0,0,List.of(0,0,0,0,0),Map.of());
                screen.update(copy(v,template,empty,2));
            });
            context.waitTicks(3);
            context.runOnClient(client->{
                var screen=(IndustryScreen)client.gui.screen();
                check(screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("대대 추가")).count()==5,
                        "The new designer opens with five empty regiment columns");
                check(requests.stream().noneMatch(r->r.action()==IndustryPayloads.Action.SAVE_TEMPLATE),"Opening never saves a template");
            });
            if(fixture.equals("industry-columns-fixture.json"))context.takeScreenshot("hoi-empty-division-designer");
            requests.clear();
            for (int[] size:new int[][]{{1600,1000},{2560,1440},{1280,720},{854,480}}) {
                context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(2);
                context.setScreen(()->new IndustryScreen(MenuTab.RECRUITMENT,v.session(),copy(v,template,null,1),requests::add));
                context.runOnClient(client->{
                    var s=(IndustryScreen)client.gui.screen();press(button(s,"열별 편제 편제"));
                    s.update(copy(v,template,v.draft(),2));
                });
                context.waitTicks(3);context.takeScreenshot("hoi-"+fixture.replace(".json","")+"-"+size[0]);
                if(size[0]==2560||size[0]==1280) {
                    context.runOnClient(client->{
                        var s=(IndustryScreen)client.gui.screen();
                        var before=slotBounds(s);
                        s.mouseScrolled((s.width-600)/2.0+150,(s.height-360)/2.0+200,0,-100);
                        check(slotBounds(s).equals(before),"Full-height designer slots stay fixed when scrolling");
                    });
                    context.waitTicks(2);context.takeScreenshot("hoi-regiment-fixed-after-wheel-"+fixture.replace(".json","")+"-"+size[0]);
                }
                if(size[0]==1600)context.runOnClient(client->{
                    var s=(IndustryScreen)client.gui.screen();
                    var adds=s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("대대 추가"))
                            .map(w->(Button)w).sorted(Comparator.comparingInt(Button::getX)).toList();
                    check(adds.size()==5,"Every column has its own next add button");
                    int firstRow=adds.get(1).getY();
                    check(adds.get(0).getY()==firstRow+23,"First column opens immediately below its battalion");
                    check(adds.get(2).getY()==firstRow+23*v.draft().columnSize(2),"Third column advances independently");
                    check(adds.get(4).getY()==firstRow,"Fifth column starts unlocked");
                    if(v.draft().columnSize(2) < 3) {
                        check(s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(dev.hoi.client.ui.industry.IndustryEquipmentDesigns.tooltip("DESIGNER_BLOCKED_REGIMENTAL_SUPPORT"))&&!b.active).count()==5,
                                "All regiment support cells stay locked with fewer than three battalions");
                        press(adds.getLast());
                        int oldRequests=requests.size();
                        press(button(s,"보병 대대"));
                        check(requests.size()==oldRequests,"Category does not change a battalion");
                        press(button(s,v.battalions().stream().filter(b->b.id().equals("INFANTRY")).findFirst().orElseThrow().name()));
                        check(requests.getLast().action()==IndustryPayloads.Action.LINE_COLUMN&&requests.getLast().amount()==20&&requests.getLast().item().equals("INFANTRY"),"Fifth column submits its own cell, not the flattened end");
                    } else {
                        var support=button(s,"연대 지원");check(!support.active,"Regiment row is decorative even after three battalions");
                    }
                });
                if(size[0]==854) {
                    context.runOnClient(client->{var s=(IndustryScreen)client.gui.screen();s.mouseScrolled(s.width/2.0,s.height/2.0,0,-100);});
                    context.waitTicks(2);context.takeScreenshot("hoi-regiment-compact-bottom-"+fixture.replace(".json",""));
                }
            }
            if(fixture.equals("industry-regiment-fixture.json"))compactSlots(context,v,template);
        }
        context.getInput().resizeWindow(1600,1000);context.waitTicks(2);context.setScreen(()->null);
    }
    private static List<String> slotBounds(IndustryScreen screen) {
        return screen.children().stream().filter(w->w instanceof Button b&&(b.getWidth()==44||b.getWidth()==32)).map(w->{
            var b=(Button)w;
            return b.getMessage().getString()+":"+b.getX()+":"+b.getY()+":"+b.getWidth()+":"+b.getHeight();
        }).toList();
    }
    private static void compactSlots(ClientGameTestContext context,IndustryView v,IndustryView.Template template) {
        var original=v.draft();
        var draft=new IndustryView.Template(original.id(),original.name(),Collections.nCopies(20,original.line().getFirst()),
                v.companies().stream().limit(4).map(IndustryView.Choice::id).toList(),original.stats(),original.equipment(),original.days(),original.manpower(),List.of(4,4,4,4,4),Map.of(),original.experienceCost());
        var requests=new ArrayList<IndustryPayloads.Request>();
        context.getInput().resizeWindow(854,480);context.waitTicks(2);
        context.setScreen(()->new IndustryScreen(MenuTab.RECRUITMENT,v.session(),copy(v,template,null,1),requests::add));
        context.runOnClient(client->{
            var s=(IndustryScreen)client.gui.screen();press(button(s,"열별 편제 편제"));s.update(copy(v,template,draft,2));requests.clear();
        });
        context.waitTicks(3);
        context.runOnClient(client->{
            var s=(IndustryScreen)client.gui.screen();
            var before=slotBounds(s);
            s.mouseScrolled(150,160,0,-4.4);
            check(!slotBounds(s).equals(before),"Compact designer scrolls its slot area");
            check(s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("연대 지원")&&!b.active).count()==5,"All five decorative regiment support cells become reachable");
            for(var widget:s.children())if(widget instanceof Button b)check(b.getX()>=0&&b.getY()>=0&&b.getRight()<=s.width&&b.getBottom()<=s.height,"Compact designer buttons stay inside the viewport");
        });
        context.waitTicks(2);context.takeScreenshot("hoi-regiment-compact-all-bottom-slots-synthetic");
        for(String label:List.of("대대 추가","지원중대 추가")) {
            context.runOnClient(client->{
                var s=(IndustryScreen)client.gui.screen();
                var b=s.children().stream().filter(w->w instanceof Button candidate&&candidate.active&&candidate.getMessage().getString().equals(label)).map(w->(Button)w).max(Comparator.comparingInt(Button::getX)).orElseThrow();
                var event=new net.minecraft.client.input.MouseButtonEvent(b.getX()+2,b.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                s.mouseClicked(event,false);s.mouseReleased(event);
                check(button(s,"대대 목록 닫기").active,"Bottom slot opens its unit picker");
                check(requests.isEmpty(),"Opening a bottom slot never mutates the server draft");
            });
            context.waitTicks(2);context.takeScreenshot(label.equals("대대 추가")?"hoi-regiment-compact-bottom-battalion-picker":"hoi-regiment-compact-bottom-support-picker");
            context.runOnClient(client->press(button((IndustryScreen)client.gui.screen(),"대대 목록 닫기")));
        }
    }
    private static IndustryView copy(IndustryView v,IndustryView.Template template,IndustryView.Template draft,long revision) {
        return new IndustryView(v.session(),revision,v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),v.equipment(),v.lines(),v.partners(),v.trades(),
                List.of(template),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),draft,"",v.navalRepairs());
    }
    private static Button button(IndustryScreen screen,String name) {
        return (Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(name)).findFirst().orElseThrow(()->new AssertionError("Missing "+name));
    }
    private static void press(Button b) { check(b.active,"Control is active");b.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0)); }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
