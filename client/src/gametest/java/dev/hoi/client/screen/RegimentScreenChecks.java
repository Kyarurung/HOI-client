package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class RegimentScreenChecks {
    public static void run(ClientGameTestContext context) {
        for (String fixture : List.of("industry-columns-fixture.json","industry-regiment-fixture.json")) {
            IndustryView v;
            try (var input=RegimentScreenChecks.class.getResourceAsStream("/"+fixture)) {
                v=new IndustryProtocol.Response(new String(Objects.requireNonNull(input).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).view();
            } catch (Exception e) { throw new AssertionError(e); }
            var template=v.templates().stream().filter(t->t.name().equals("열별 편제")).findFirst().orElseThrow();
            var requests=new ArrayList<IndustryProtocol.Request>();
            for (int[] size:new int[][]{{1600,1000},{854,480}}) {
                context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(2);
                context.setScreen(()->new IndustryScreen(MenuTab.RECRUITMENT,v.session(),copy(v,template,null,1),requests::add));
                context.runOnClient(client->{
                    var s=(IndustryScreen)client.gui.screen();press(button(s,"열별 편제 편제"));
                    s.update(copy(v,template,v.draft(),2));
                });
                context.waitTicks(3);context.takeScreenshot("hoi-"+fixture.replace(".json","")+"-"+size[0]);
                if(size[0]==1600)context.runOnClient(client->{
                    var s=(IndustryScreen)client.gui.screen();
                    var adds=s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("대대 추가"))
                            .map(w->(Button)w).sorted(Comparator.comparingInt(Button::getX)).toList();
                    check(adds.size()==5,"Every column has its own next add button");
                    int firstRow=adds.get(1).getY();
                    check(adds.get(0).getY()==firstRow+25,"First column opens immediately below its battalion");
                    check(adds.get(2).getY()==firstRow+25*v.draft().columnSize(2),"Third column advances independently");
                    check(adds.get(4).getY()==firstRow,"Fifth column starts unlocked");
                    if(v.draft().columnSize(2) < 3) {
                        check(s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("연대 지원 · 같은 열에 대대 3개 필요")&&!b.active).count()==5,
                                "All regiment support cells stay locked with fewer than three battalions");
                        press(adds.getLast());
                        press(button(s,"보병 대대"));
                        String unit=v.battalions().stream().filter(b->b.id().equals("INFANTRY")).findFirst().orElseThrow().name();
                        press(button(s,unit));
                        check(requests.getLast().action()==IndustryProtocol.Action.LINE_COLUMN&&requests.getLast().amount()==20,"Fifth column submits its own cell, not the flattened end");
                    } else {
                        var support=button(s,"연대 지원 · 장식용");check(!support.active,"Regiment row is decorative even after three battalions");
                    }
                });
                if(size[0]==854) {
                    context.runOnClient(client->{var s=(IndustryScreen)client.gui.screen();s.mouseScrolled(s.width/2.0,s.height/2.0,0,-100);});
                    context.waitTicks(2);context.takeScreenshot("hoi-regiment-compact-bottom-"+fixture.replace(".json",""));
                }
            }
        }
        context.getInput().resizeWindow(1600,1000);context.waitTicks(2);context.setScreen(()->null);
    }
    private static IndustryView copy(IndustryView v,IndustryView.Template template,IndustryView.Template draft,long revision) {
        return new IndustryView(v.session(),revision,v.country(),v.hud(),v.economy(),v.resources(),v.modifiers(),v.equipment(),v.lines(),v.partners(),v.trades(),
                List.of(template),v.battalions(),v.companies(),v.locations(),v.recruits(),v.deployed(),draft,"",v.navalRepairs());
    }
    private static Button button(IndustryScreen screen,String name) {
        return (Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(name)).findFirst().orElseThrow(()->new AssertionError("Missing "+name));
    }
    private static void press(Button b) { check(b.active,"Control is active");b.onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)); }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
}
