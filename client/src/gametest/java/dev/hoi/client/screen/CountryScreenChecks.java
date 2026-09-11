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
        overview.add(new MenuView.Entry("점령지", "0개 주", "점령지 없음", 0));
        overview.add(new MenuView.Entry("협력정부", "1개국", "검증용 종속국", 1));
        overview.removeIf(e -> e.name().equals("이념"));
        overview.add(new MenuView.Entry("세부 이념", "공격적 자유주의", "[image:politics/banner/gfx_ideology_ultra_conservatism]\n\n설명 이미지는 툴팁 안에 표시합니다.\n\n긴 설명은 화면 폭에 맞춰 줄바꿈합니다."));
        overview.removeIf(e -> e.icon().startsWith("politics/ideology/"));
        String[] names = {"민중민주당", "민주노동당", "민주노동당", "민주노동당", "더불어민주당", "국민의힘", "국민의힘", "우리공화당", "대한민국 국군", "자유의새벽당", "가자코리아"};
        int[] shares = {0,5,6,5,33,36,3,5,0,1,6};
        String[] colors = {"640000","a40000","ce3b3b","e76d77","e4bf39","eddf58","3261d0","434b81","636369","70411d","312b20"};
        for (int i = 0; i < names.length; i++) overview.add(new MenuView.Entry(names[i], shares[i] + "%", "목록 렌더링 검증용 분포와 색상", shares[i] / 100.0, "politics/party/" + colors[i]));
        sections.set(0, new MenuView.Section("국가 현황", "politics", overview));
        sections.set(1, new MenuView.Section("국가 정신", "politics", java.util.stream.IntStream.range(0, 24)
                .mapToObj(i -> new MenuView.Entry("검증용 정신 " + i, "", "연구 속도: §a+5%§r\n안정도: §4-3%§r\n\n현재 아이콘에 해당하는 설명입니다.", -1, "menu/politics")).toList()));
        var pages = source.pages().stream().map(p -> p.tab() == MenuTab.POLITICS ? new MenuView.Page(p.tab(), sections) : p).toList();
        context.setScreen(() -> new HoiMenuScreen(new MenuView(source.country(), source.countryName(), source.date(), source.speed(), pages, source.hud())));
        context.waitTicks(2);
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); var layout = screen.politicsLayout();
            for (int pane : new int[]{248,363,726}) {
                var sizing = new dev.hoi.client.ui.PoliticsLayout(pane, 40);
                var focusBox = sizing.focus(); var focusImage = sizing.focusImage();
                if (Math.abs(focusBox.width() - focusBox.height() * 359.0 / 107) > .5
                        || Math.abs(focusBox.x() + focusBox.height() * 49.5 / 107
                                - focusImage.x() - focusImage.width() / 2.0) > .5
                        || focusImage.x() != sizing.ideology().x()
                        || sizing.spirits().x() != sizing.election().x()
                        || sizing.spirits().x() + sizing.spirits().width() != pane - 8
                        || sizing.ideology().y() - focusBox.y() - focusBox.height() != 4
                        || focusImage.width() != focusImage.height()
                        || focusImage.height() != sizing.ideology().height())
                    throw new AssertionError("Focus must center its original frame on the icon and spirits must align with election");
                for (var box : List.of(sizing.union(), sizing.ideology(), sizing.government()))
                    if (box.width() != box.height()) throw new AssertionError("Political icon cells must stay square at every size");
                if (sizing.government().x() + sizing.government().width() > sizing.election().x()
                        || sizing.election().x() + sizing.election().width() >= sizing.parties().x()
                        || sizing.partyChart().x() + sizing.partyChart().width() >= sizing.parties().x())
                    throw new AssertionError("Square cells and chart must not overlap their neighbours");
            }
            for (var box : List.of(layout.economy(), layout.ideology(), layout.faction()))
                if (screen.politicsHover(box.x() + 2, box.y() + 2) == null) throw new AssertionError("Political information cells must expose their tooltip even without extra details");
            for (var child : screen.children()) if (child instanceof Button button) {
                if (button.getMessage().getString().equals("점령지") && button.active) throw new AssertionError("Empty occupation list must be disabled");
                if (button.getMessage().getString().equals("협력정부") && !button.active) throw new AssertionError("Existing subject list must be enabled");
            }
            var leader = layout.leader(); var image = layout.focusImage(); var title = layout.focusTitle();
            if (layout.election().x() + layout.election().width() >= layout.parties().x()
                    || layout.government().x() + layout.government().width() >= layout.parties().x()
                    || layout.parties().y() >= layout.summaryY())
                throw new AssertionError("Party names need the space beside government and election");
            if (!screen.politicsHover(leader.x()+5,leader.y()+10).equals(screen.politicsHover(leader.x()+5,leader.y()+leader.height()-8)))
                throw new AssertionError("Portrait and name must occupy one continuous cell");
            var focus = screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("국가 중점"))
                    .map(w -> (Button)w).findFirst().orElseThrow();
            if (focus.isMouseOver(image.x()+2,image.y()+2) || !focus.isMouseOver(title.x()+2,title.y()+2))
                throw new AssertionError("Only focus title must be clickable");
            var strip=layout.spirits(); int x=strip.x()+6,y=strip.y()+strip.height()/2;
            var first=screen.politicsHover(x,y);screen.mouseScrolled(x,y,-3,0);
            var last=screen.politicsHover(x,y);
            if(first==null||last==null||first.equals(last))throw new AssertionError("Horizontal scroll must expose later spirits");
            if(screen.politicsHover(strip.x()+1,y)!=null)throw new AssertionError("Spirit spacer must stay empty");
            screen.mouseScrolled(x,y,0,100);
            if(!first.equals(screen.politicsHover(x,y)))throw new AssertionError("Vertical wheel must restore strip");
        });
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); var box = screen.politicsLayout().ideology();
            var entry = screen.politicsHover(box.x()+2, box.y()+2);
            if (entry == null || !entry.detail().contains("[image:politics/banner/")) throw new AssertionError("Description image is tooltip content");
            var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi", "textures/gui/politics/banner/gfx_ideology_ultra_conservatism.png");
            if (c.getResourceManager().getResource(id).isEmpty()) throw new AssertionError("Original description image must exist");
        });
        var point = context.computeOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); var box = screen.politicsLayout().ideology();
            double scale = c.getWindow().getGuiScale();
            return new double[]{(box.x()+3)*scale, (box.y()+3)*scale};
        });
        context.getInput().setCursorPos(point[0],point[1]);
        context.waitTicks(2); context.takeScreenshot("hoi-politics-description-image");
        context.getInput().setCursorPos(1500,800);
        context.takeScreenshot("hoi-politics-layout");
        dev.hoi.client.ResearchScreenGameTest.gui2Screenshot(context, "hoi-politics-party-names");
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); var list = screen.politicsLayout().parties();
            if (!screen.mouseScrolled(list.x() + 10, list.y() + 10, 0, -20))
                throw new AssertionError("Party list must scroll independently");
        });
        context.waitTicks(2); context.takeScreenshot("hoi-politics-party-list-scrolled");
        context.getInput().setCursorPos(1500,800);
        sections.set(1, new MenuView.Section("국가 정신", "politics", List.of(sections.get(1).entries().getFirst())));
        sections.replaceAll(s -> s.title().equals("국가 중점") ? new MenuView.Section("국가 중점", "research", List.of()) : s);
        var singleSpiritPages = source.pages().stream().map(p -> p.tab() == MenuTab.POLITICS ? new MenuView.Page(p.tab(), sections) : p).toList();
        context.setScreen(() -> new HoiMenuScreen(new MenuView(source.country(), source.countryName(), source.date(), source.speed(), singleSpiritPages, source.hud())));
        context.waitTicks(2);
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); int top = dev.hoi.client.ui.HoiMenuBar.height(screen.width);
            var spirit = screen.politicsHover(screen.politicsLayout().spirits().x() + 6, screen.politicsLayout().spirits().y() + screen.politicsLayout().spirits().height()/2);
            if (spirit == null || !spirit.name().equals("검증용 정신 0")) throw new AssertionError("A single spirit must start at the left edge");
        });
        context.takeScreenshot("hoi-politics-left-aligned-spirit");
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
