package dev.hoi.client;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

final class CountryScreenChecks {
    static List<MenuView.Section> politics() {
        var result = new ArrayList<MenuView.Section>();
        result.add(new MenuView.Section("국가 현황", "politics", List.of(new MenuView.Entry("정치력","72","화면 검증용 데이터"), new MenuView.Entry("집권 정당","집권 정당",""))));
        result.add(new MenuView.Section("국가 정신", "politics", List.of(new MenuView.Entry("국가 정신","활성",""))));
        result.add(new MenuView.Section("국가 중점", "research", List.of(new MenuView.Entry("산업 시설 확대","진행 중","검증용 중점",.3))));
        String[] names = {"정부","경제법","군법","사회법","발전도","군사 참모","연구 & 생산"};
        String[] ids = {"government","economic_laws","military_laws","social_laws","development","military_staff","research_production"};
        String[][] icons = {
                {"vacant","vacant","vacant","vacant","vacant","vacant"},
                {"economic_focus","trade","taxes","interest_rates","welfare","safety"},
                {"conscription","female_service_medium","medium_supervision","training","medium_racial_integration","general_exemptions"},
                {"immigration","education","racial","gender_equality","prison","security"},
                {"academic_development","farming_development","poverty_development","industrial_development","military_development","society_development"},
                {"army_chief","navy_chief","air_chief","high_command","high_command","high_command"},
                {"tank_manufacturer","naval_manufacturer","aircraft_manufacturer","materiel_manufacturer"}};
        for (int i=0;i<7;i++) {
            var entries=new ArrayList<MenuView.Entry>();
            for(int j=0;j<icons[i].length;j++) entries.add(new MenuView.Entry(names[i]+" 선택 "+j,i!=0&&j==0?"적용 중":"미지정","선택된 항목의 설명",-1,"politics/"+icons[i][j]));
            result.add(new MenuView.Section(names[i],ids[i],entries));
        }
        return result;
    }
    static void run(ClientGameTestContext context, CountryHud hud) {
        CountryProtocol.registerPayloadTypes(); CountryProtocol.registerPayloadTypes();
        context.runOnClient(client -> {
            for (var domain : IntelDomain.values()) if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/" + domain.icon + ".png")).isEmpty())
                throw new AssertionError("Missing vanilla intelligence icon: " + domain);
        });
        var reports = Arrays.stream(IntelDomain.values()).map(d -> new CountryView.Report(d,new double[]{59.6,29.6,56.6,34.3}[d.ordinal()],List.of(
                new MenuView.Entry(d.label+" 예상 수량","50–99","실제 서버 값을 사용하지 않는 렌더링 검증 데이터"),
                new MenuView.Entry("정보가 충분하지 않습니다","상세 첩보 필요","공개 기준 확인")))).toList();
        var fixture = new CountryView("KOR",hud,"PRC","중국",false,List.of(new CountryView.Choice("KOR","대한민국"),new CountryView.Choice("PRC","중국")),
                List.of(new MenuView.Entry("관계","비동맹","")),List.of(new MenuView.Entry("알려지지 않은 중점","민간 첩보 70% 필요","")),reports);
        context.setScreen(() -> new CountryScreen(fixture)); context.waitTicks(3); context.takeScreenshot("hoi-country-diplomacy");
        click(context,"첩보 장부");
        for (var d : IntelDomain.values()) { click(context,d.label+" 정보"); context.waitTicks(2); context.takeScreenshot("hoi-intel-"+d.name().toLowerCase(Locale.ROOT)); }
        context.getInput().resizeWindow(854,480);context.waitTicks(3);context.takeScreenshot("hoi-intel-compact");
        context.runOnClient(client -> {
            var screen=(CountryScreen)client.gui.screen();
            for(var child:screen.children()) if(child instanceof Button b && (b.getX()<0||b.getY()<0||b.getRight()>screen.width||b.getBottom()>screen.height)) throw new AssertionError("Foreign UI overflow");
            String old=screen.token();screen.select("KOR");
            screen.update(CountryProtocol.Response.of(old,fixture));
            if(screen.view()!=null)throw new AssertionError("Late foreign response leaked after target change");
            screen.update(new CountryProtocol.Response(screen.token(),""));
            if(client.gui.screen()!=null)throw new AssertionError("Revoked foreign UI must close");
        });
        context.getInput().resizeWindow(1600,1000);context.waitTicks(3);
    }
    private static void click(ClientGameTestContext context,String label) {
        context.runOnClient(client -> ((Button)client.gui.screen().children().stream().filter(c -> c instanceof Button b && b.getMessage().getString().equals(label)).findFirst().orElseThrow()).onPress(new KeyEvent(GLFW.GLFW_KEY_ENTER,0,0)));
    }
}
