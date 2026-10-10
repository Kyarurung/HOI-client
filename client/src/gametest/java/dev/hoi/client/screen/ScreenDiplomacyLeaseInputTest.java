package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class ScreenDiplomacyLeaseInputTest {
    private ScreenDiplomacyLeaseInputTest() {}
    public static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(c->c.options.guiScale().set(2));
        var requests=new ArrayList<ProtocolCountryPayloads.Action>();String session=UUID.randomUUID().toString();
        var reports=Arrays.stream(ProtocolIntelDomain.values()).map(d->new ProtocolCountryView.Report(d,0,List.of())).toList();
        var action=new ProtocolCountryView.Action("lend:fixture","무기대여 시작 · 보병 장비","일회성 · 저장량: 2\n수락 전 장비 이전 없음\nHOI 적용: 첫 배송은 수락 후 30일 · 부족분은 부분 배송",true,new ProtocolCountryView.AmountInput("보병 장비",2));
        var view=new ProtocolCountryView("KOR",ProtocolCountryHud.UNKNOWN,"PRK","조선민주주의인민공화국",false,List.of(),List.of(),List.of(),reports,new ProtocolCountryView.Actions(session,8,"",List.of(action)));
        context.setScreen(()->new ScreenCountry(view,requests::add));context.waitTicks(3);
        context.runOnClient(c->{
            var screen=(ScreenCountry)c.gui.screen();var open=button(screen,action.name());
            int x=open.getX()+open.getWidth()/2,y=open.getY()+open.getHeight()/2;
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0)),false);
            if(screen.children().stream().anyMatch(w->w instanceof EditBox))throw new AssertionError("Right click opened the amount form");
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
            var input=input(screen);input.setValue("9223372036854775808");
            if(button(screen,"확인").active)throw new AssertionError("Overflow amount is enabled");
            input.setValue("0");if(button(screen,"확인").active)throw new AssertionError("Zero amount is enabled");
            for(String invalid:List.of("", "abc", "+1", "-1", " 1", "1.5", "１")) {
                input.setValue(invalid);if(button(screen,"확인").active)throw new AssertionError("Invalid amount is enabled: "+invalid);
            }
            input.setValue("100");if(!button(screen,"확인").active)throw new AssertionError("Stock was incorrectly treated as a quantity cap");
            if(screen.allowsMovement()||!requests.isEmpty())throw new AssertionError("Opening amount input sent a mutation or allowed movement");
        });
        context.waitTicks(3);context.takeScreenshot("hoi-diplomacy-lend-lease-input-gui2");
        context.runOnClient(c->{
            var screen=(ScreenCountry)c.gui.screen();press(button(screen,"취소"));
            if(!requests.isEmpty())throw new AssertionError("Cancelling input sent an offer");
            press(button(screen,action.name()));input(screen).setValue(Long.toString(Long.MAX_VALUE));
            press(button(screen,"확인"));press(button(screen,"확인"));
            if(requests.size()!=1||requests.getFirst().amount()!=Long.MAX_VALUE||requests.getFirst().revision()!=8||!requests.getFirst().session().equals(session))throw new AssertionError("Amount submission changed issued authority or duplicated the offer");
            for(int i=0;i<100;i++)screen.tick();
            if(requests.size()!=1)throw new AssertionError("Timeout retransmitted a lend-lease offer");
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
        context.setScreen(()->new ScreenCountry(view,requests::add));context.waitTicks(3);
        context.runOnClient(c->{var screen=(ScreenCountry)c.gui.screen();press(button(screen,action.name()));});
        context.waitTicks(3);context.takeScreenshot("hoi-diplomacy-lend-lease-input-compact-gui2");
        context.runOnClient(c->{
            var screen=(ScreenCountry)c.gui.screen();
            for(var child:screen.children())if(child instanceof net.minecraft.client.gui.components.AbstractWidget w)
                if(w.getX()<0||w.getY()<0||w.getRight()>screen.width||w.getBottom()>screen.height)throw new AssertionError("Lend-lease input exceeds GUI2 screen bounds");
            screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));
            if(!screen.allowsMovement()||requests.size()!=1)throw new AssertionError("Escape does not cancel the amount input");
        });
        cancellation(context);
        ScreenDiplomacyMonthlyLeaseTest.run(context);
    }
    private static void cancellation(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        var requests = new ArrayList<ProtocolCountryPayloads.Action>();
        String session = UUID.randomUUID().toString();
        var reports = Arrays.stream(ProtocolIntelDomain.values()).map(d -> new ProtocolCountryView.Report(d, 0, List.of())).toList();
        var action = new ProtocolCountryView.Action("cancel_lease:fixture", "제공받는 무기대여 취소",
                "현재 국가로부터 장비를 그만 받습니다.\n정치력 비용 없음\n취소 후 추가 배송이 중단됩니다. 이미 전달된 장비는 반환되지 않습니다.", true);
        var view = new ProtocolCountryView("PRK", ProtocolCountryHud.UNKNOWN, "KOR", "대한민국", false,
                List.of(), List.of(), List.of(), reports, new ProtocolCountryView.Actions(session, 11, "", List.of(action)));
        context.setScreen(() -> new ScreenCountry(view, requests::add));
        context.waitTicks(3);
        context.runOnClient(c -> {
            var screen = (ScreenCountry)c.gui.screen();
            press(button(screen, action.name()));
            if (screen.children().stream().anyMatch(w -> w instanceof EditBox)) throw new AssertionError("Cancellation opened an amount input");
            if (!requests.isEmpty()) throw new AssertionError("Opening cancellation sent a request");
        });
        context.waitTicks(3);
        context.takeScreenshot("hoi-diplomacy-recipient-cancel-gui2");
        context.runOnClient(c -> {
            var screen = (ScreenCountry)c.gui.screen();
            press(button(screen, "취소"));
            if (!requests.isEmpty()) throw new AssertionError("Closing cancellation sent a request");
            press(button(screen, action.name()));
            press(button(screen, "확인"));
            press(button(screen, "확인"));
            if (requests.size() != 1 || !requests.getFirst().id().equals(action.id()) || requests.getFirst().amount() != 0)
                throw new AssertionError("Recipient cancellation duplicated or changed the issued action");
        });
    }
    private static EditBox input(ScreenCountry screen) {return (EditBox)screen.children().stream().filter(w->w instanceof EditBox).findFirst().orElseThrow();}
    private static Button button(ScreenCountry screen,String name) {return (Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(name)).findFirst().orElseThrow();}
    private static void press(Button button) {button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));}
}
