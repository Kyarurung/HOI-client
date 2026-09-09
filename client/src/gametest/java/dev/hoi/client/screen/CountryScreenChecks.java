package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class CountryScreenChecks {
    public static void politicsHover(ClientGameTestContext context, MenuView source) {
        var sections = new ArrayList<>(politics());
        var overview = new ArrayList<>(sections.getFirst().entries());
        overview.set(1, new MenuView.Entry("지도자", "검증용 지도자", "검증용 특성\n안정도: §a+5%§r\n정치력: §4-3%§r\n\n지도자 설명 줄바꿈과 호버 영역을 확인하는 렌더링 검증 데이터입니다.", -1, "politics/empty/leader"));
        overview.add(new MenuView.Entry("세부 이념", "공격적 자유주의", "세부 이념 설명을 확인하는 렌더링 검증 데이터입니다."));
        sections.set(0, new MenuView.Section("국가 현황", "politics", overview));
        sections.set(1, new MenuView.Section("국가 정신", "politics", java.util.stream.IntStream.range(0, 24)
                .mapToObj(i -> new MenuView.Entry("검증용 정신 " + i, "", "연구 속도: §a+5%§r\n안정도: §4-3%§r\n\n현재 아이콘에 해당하는 설명입니다.", -1, "menu/politics")).toList()));
        var pages = source.pages().stream().map(p -> p.tab() == MenuTab.POLITICS ? new MenuView.Page(p.tab(), sections) : p).toList();
        context.setScreen(() -> new HoiMenuScreen(new MenuView(source.country(), source.countryName(), source.date(), source.speed(), pages, source.hud())));
        context.waitTicks(2);
        double[] cursor = new double[2];
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); int top = dev.hoi.client.ui.HoiMenuBar.height(screen.width);
            if (!screen.politicsHover(15, top + 45).equals(screen.politicsHover(15, top + 115))) throw new AssertionError("Portrait and name must share leader tooltip");
            if (screen.politicsHover(15, top + 108) != null) throw new AssertionError("Separate portrait and name cells must leave a noninteractive gap");
            if (screen.children().stream().anyMatch(w -> w instanceof Button b && b.isMouseOver(15, top + 45)))
                throw new AssertionError("Leader portrait must have no clickable or highlighting button");
            if (screen.politicsHover(screen.politicsSplit() + 15, top + 75) != null)
                throw new AssertionError("Ideology space must not expose a national spirit tooltip");
            double scale = c.getWindow().getWidth() / (double)screen.width;
            cursor[0] = 15 * scale; cursor[1] = (top + 45) * scale;
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(3); context.takeScreenshot("hoi-politics-leader-tooltip");
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); int top = dev.hoi.client.ui.HoiMenuBar.height(screen.width), x = screen.politicsSplit() + 40;
            var first = screen.politicsHover(x, top + 75);
            screen.mouseScrolled(x, top + 75, -3, 0);
            var last = screen.politicsHover(x, top + 75);
            if (first == null || last == null || first.equals(last)) throw new AssertionError("Horizontal wheel must reveal later spirits");
            if (screen.politicsHover(screen.politicsSplit() + 32, top + 75) != null)
                throw new AssertionError("Scrolled spirits must stay out of left spacer");
            screen.mouseScrolled(x, top + 75, 0, 100);
            if (!first.equals(screen.politicsHover(x, top + 75))) throw new AssertionError("Vertical wheel must scroll spirit strip back");
            double scale = c.getWindow().getWidth()/(double)screen.width;
            cursor[0] = x * scale; cursor[1] = (top + 75) * scale;
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(3); context.takeScreenshot("hoi-politics-spirit-tooltip");
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); int top = dev.hoi.client.ui.HoiMenuBar.height(screen.width);
            if (!screen.politicsHover(screen.politicsSplit()+5, top+103).value().equals("공격적 자유주의"))
                throw new AssertionError("Subtype label and tooltip must use the same server entry");
            double scale = c.getWindow().getWidth()/(double)screen.width;
            cursor[0] = (screen.politicsSplit()+5)*scale; cursor[1] = (top+103)*scale;
        });
        context.getInput().setCursorPos(cursor[0], cursor[1]); context.waitTicks(3); context.takeScreenshot("hoi-politics-ideology-tooltip");
        context.getInput().setCursorPos(1500,800);
        context.setScreen(() -> new HoiMenuScreen(source)); context.waitTicks(2);
    }
    public static List<MenuView.Section> politics() {
        var result = new ArrayList<MenuView.Section>();
        result.add(new MenuView.Section("국가 현황", "politics", List.of(new MenuView.Entry("정치력","72","화면 검증용 데이터"),
                new MenuView.Entry("지도자","정보 없음","",-1,"politics/empty/leader"),
                new MenuView.Entry("집권 정당","민주주의",""),new MenuView.Entry("이념","민주주의",""),
                new MenuView.Entry("경제-정치 연합","정보 없음",""),new MenuView.Entry("통치 형태","민주주의",""),
                new MenuView.Entry("다음 선거","5월 2032",""),
                new MenuView.Entry("경제 모델","정보 없음",""),new MenuView.Entry("세력","미가입",""),new MenuView.Entry("정치 체제","정보 없음",""),
                new MenuView.Entry("민주주의","70%","검증 데이터",.7,"politics/ideology/democratic"),
                new MenuView.Entry("공산주의","10%","검증 데이터",.1,"politics/ideology/communist"),
                new MenuView.Entry("파시즘","5%","검증 데이터",.05,"politics/ideology/fascist"),
                new MenuView.Entry("비동맹","15%","검증 데이터",.15,"politics/ideology/non_aligned"))));
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
    public static void run(ClientGameTestContext context, CountryHud hud) {
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
