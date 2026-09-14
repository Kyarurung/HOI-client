package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public final class CountryScreenChecks {
    public static void authoritativePoliticsTooltips(ClientGameTestContext context) {
        String directory = System.getenv("HOI_POLITICS_PREVIEW");
        if (directory == null) return;
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2));
        context.runOnClient(c -> {
            String prose = "첫 문장. 두 번째 문장!\n\n다음 구역: +1.25%. 유지됩니다.";
            String wrapped = dev.hoi.client.ui.HoiTooltips.sentences(prose);
            if (!wrapped.equals("첫 문장.\n두 번째 문장!\n\n다음 구역: +1.25%.\n유지됩니다."))
                throw new AssertionError("Sentence wrapping changed paragraphs or decimals: " + wrapped);
            var colored = net.minecraft.network.chat.Component.literal("첫 문장. 다음 문장.").withColor(0x55FF55);
            var rewritten = dev.hoi.client.ui.HoiTooltips.sentences(colored);
            if (!rewritten.getString().equals("첫 문장.\n다음 문장.") || !rewritten.visit((style, part) ->
                    style.getColor() != null && style.getColor().getValue() == 0x56AC5B ? Optional.of(true) : Optional.empty(),
                    net.minecraft.network.chat.Style.EMPTY).orElse(false)) throw new AssertionError("Sentence wrapping lost style");
            for (String effect : List.of("안정도: §a+4%§r", "저항 활동 확률: §a-20%§r", "월간 인플레이션: §4+0.1%§r", "금리: §e+5%§r", "매일 사회자유주의 지지도: §a+0.01§r")) {
                var colors = new java.util.HashSet<Integer>();
                var icons = new java.util.concurrent.atomic.AtomicInteger();
                for (var line : dev.hoi.client.ui.HoiTooltips.lines(c.font, effect, 1280))
                    line.accept((index, style, character) -> {
                        if (character >= 0xe000 && character <= 0xf8ff) icons.incrementAndGet();
                        if (style.getColor() != null) colors.add(style.getColor().getValue());
                        return true;
                    });
                int expected = effect.contains("§a") ? 0x56AC5B : effect.contains("§4") ? 0xDE5646 : 0xEEC923;
                if (icons.get() != 1 || !colors.contains(expected))
                    throw new AssertionError("Tooltip keyword or value color missing: " + effect + " " + colors);
            }
        });
        try {
            partyChartTooltips(context, directory);
            var menu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(
                    java.nio.file.Path.of(directory, "KNU.json"))).view();
            context.setScreen(() -> new HoiMenuScreen(menu));
            context.waitTicks(3);
            for (String key : List.of("supplies", "nuclear", "gdp", "debt")) {
                var point = context.computeOnClient(c -> {
                    int width = c.gui.screen().width;
                    var items = dev.hoi.client.ui.HoiMenuBar.indicators(menu.hud());
                    int x = dev.hoi.client.ui.HoiMenuBar.flagWidth(width) + 10;
                    int total = items.stream().mapToInt(dev.hoi.client.ui.HoiMenuBar.Indicator::width).sum();
                    for (var item : items) {
                        if (item.icon().equals("gdp")) x += Math.max(0, dev.hoi.client.ui.HoiMenuBar.statsRight(width)
                                - dev.hoi.client.ui.HoiMenuBar.flagWidth(width) - 10 - total - 3);
                        if (item.icon().equals(key)) {
                            if (item.raw() == null && !item.value().equals("0")) throw new AssertionError("Missing HUD value must display zero: " + key);
                            var text = new StringBuilder();
                            for (var line : dev.hoi.client.ui.HudTooltip.body(c.font, width, menu.hud(), item))
                                line.accept((index, style, character) -> { text.appendCodePoint(character); return true; });
                            if (text.indexOf("—") >= 0) throw new AssertionError("Obsolete HUD placeholder: " + key);
                            double scale = c.getWindow().getGuiScale();
                            return new double[]{(x + item.width() / 2.0) * scale, 7 * scale};
                        }
                        x += item.width();
                    }
                    throw new AssertionError("Missing HUD indicator: " + key);
                });
                context.getInput().setCursorPos(point[0], point[1]);
                context.waitTicks(3);
                context.takeScreenshot("hoi-server-hud-" + key + "-gui2");
            }
        } catch (java.io.IOException e) { throw new AssertionError("Server HUD snapshot unavailable", e); }
        for (String tag : List.of("KOR", "PRC")) {
            try {
                var menu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(java.nio.file.Path.of(directory, tag + ".json"))).view();
                String banner = tag.equals("KOR") ? "gfx_banner_kor_dominance_of_chaebols" : "gfx_prc_down_with_bureaucracy_banner";
                context.setScreen(() -> new HoiMenuScreen(menu));
                context.waitTicks(3);
                var point = context.computeOnClient(c -> {
                    var screen = (HoiMenuScreen)c.gui.screen();
                    var box = screen.politicsLayout().spirits();
                    int y = box.y() + box.height() / 2;
                    var texture = net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi", "textures/gui/politics/art/" + banner + ".png");
                    if (c.getResourceManager().getResource(texture).isEmpty()) throw new AssertionError("Missing spirit description image: " + banner);
                    for (int x = box.x(); x < box.x() + box.width(); x++) {
                        var spirit = screen.politicsHover(x, y);
                        if (spirit != null && spirit.detail().contains(banner)) {
                            double scale = c.getWindow().getGuiScale();
                            return new double[]{x * scale, y * scale};
                        }
                    }
                    throw new AssertionError("Server spirit detail missing image marker: " + banner);
                });
                context.getInput().setCursorPos(point[0], point[1]);
                context.waitTicks(3); context.takeScreenshot("hoi-spirit-image-" + tag + "-gui2");
            } catch (java.io.IOException error) { throw new AssertionError(error); }
        }
        try {
            var menu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(java.nio.file.Path.of(directory, "PRK.json"))).view();
            context.setScreen(() -> new HoiMenuScreen(menu)); context.waitTicks(3);
            var point = context.computeOnClient(c -> {
                var screen = (HoiMenuScreen)c.gui.screen();
                var box = screen.politicsLayout().spirits();
                int y = box.y() + box.height() / 2;
                for (int x = box.x(); x < box.x() + box.width(); x++) {
                    var spirit = screen.politicsHover(x, y);
                    if (spirit != null && spirit.detail().contains("저항 활동 확률")) {
                        double scale = c.getWindow().getGuiScale();
                        return new double[]{x * scale, y * scale};
                    }
                }
                throw new AssertionError("Missing resistance activity translation");
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(3);
            context.takeScreenshot("hoi-resistance-activity-icon-gui2");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        try {
            var all = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
                    java.nio.file.Path.of(directory, "all-country-spirits.json"))).getAsJsonObject();
            int rendered = 0;
            for (var country : all.entrySet()) for (var raw : country.getValue().getAsJsonArray()) {
                var entry = new com.google.gson.Gson().fromJson(raw, MenuView.Entry.class);
                context.runOnClient(c -> {
                    var images = java.util.regex.Pattern.compile("\\[image:([a-z0-9_/]+)\\]").matcher(entry.detail());
                    while (images.find()) {
                        var texture = net.minecraft.resources.Identifier.fromNamespaceAndPath("hoi", "textures/gui/" + images.group(1) + ".png");
                        if (c.getResourceManager().getResource(texture).isEmpty())
                            throw new AssertionError(country.getKey() + " " + entry.name() + " missing image: " + texture);
                    }
                });
                context.setScreen(() -> new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal(country.getKey())) {
                    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float delta) {
                        g.fill(0, 0, width, height, 0xFF202226);
                        dev.hoi.client.ui.PoliticalTooltip.draw(g, font, entry, 20, 30, width, height, 0);
                    }
                });
                context.waitTicks(1);
                rendered++;
            }
            if (all.size() < 100 || rendered < 50) throw new AssertionError("All-country spirit fixture unexpectedly incomplete: " + rendered);
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        String[] tags = {"KOR", "KOR", "KOR", "KOR", "KOR", "PRC", "KOR", "PRC", "PRK", "CAM"};
        String[] labels = {"내무장관", "금리법", "군사 훈련", "여성권", "산업 개발", "공군참모총장", "지도자", "지도자", "지도자", "지도자"};
        for (int i = 0; i < labels.length; i++) {
            final MenuView menu;
            try {
                menu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(
                        java.nio.file.Path.of(directory, tags[i] + ".json"))).view();
            } catch (java.io.IOException e) { throw new AssertionError("Server tooltip snapshot unavailable", e); }
            String label = labels[i];
            context.setScreen(() -> new HoiMenuScreen(menu));
            context.waitTicks(3);
            var point = context.computeOnClient(c -> {
                var screen = (HoiMenuScreen)c.gui.screen();
                int x, y;
                if (label.equals("지도자")) {
                    var box = screen.politicsLayout().leader();
                    x = box.x() + box.width() / 2;
                    y = box.y() + box.height() / 2;
                } else {
                    var button = screen.children().stream().filter(w -> w instanceof Button b
                            && b.getMessage().getString().startsWith(label + " · "))
                            .map(w -> (Button)w).findFirst().orElseThrow();
                    x = button.getX() + button.getWidth() / 2;
                    y = button.getY() + button.getHeight() / 2;
                }
                var entry = screen.politicsHover(x, y);
                if (entry == null || (!label.equals("지도자") && entry.detail().isBlank())) throw new AssertionError("Missing server effect tooltip: " + label);
                if (label.equals("지도자") && !entry.detail().equals(entry.detail().strip()))
                    throw new AssertionError("Leader tooltip has empty boundary rows: " + entry.detail());
                double scale = c.getWindow().getGuiScale();
                return new double[]{x * scale, y * scale};
            });
            context.getInput().setCursorPos(point[0], point[1]);
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-policy-" + i + "-gui2");
        }
        try {
            var backdrop = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"KOR.json"))).view();
            context.setScreen(() -> new HoiMenuScreen(backdrop));context.waitTicks(3);
            var economyPoint=context.computeOnClient(c -> {
                var screen=(HoiMenuScreen)c.gui.screen();
                var box=screen.politicsLayout().economy();
                double scale=c.getWindow().getGuiScale();
                return new double[]{(box.x()+box.width()/2.0)*scale,(box.y()+box.height()/2.0)*scale};
            });
            context.getInput().setCursorPos(economyPoint[0],economyPoint[1]);context.waitTicks(3);
            context.takeScreenshot("hoi-economic-model-icon-spacing-gui2");
            var partyPoint=context.computeOnClient(c -> {
                var screen=(HoiMenuScreen)c.gui.screen();
                int pane=dev.hoi.client.ui.HoiPanelLayout.width(MenuTab.POLITICS,screen.width);
                var layout=new dev.hoi.client.ui.PoliticsLayout(pane,dev.hoi.client.ui.HoiMenuBar.height(screen.width));
                var list=layout.parties();
                for(int i=0;i<20;i++) {
                    int x=list.x()+8,y=list.y()+4+i*11+5;
                    var entry=screen.politicsHover(x,y);
                    if(entry!=null && entry.detail().contains("25.00%")) {
                        if(!entry.value().matches("[0-9]+\\.[0-9]{2}%"))throw new AssertionError("Party precision: "+entry.value());
                        double scale=c.getWindow().getGuiScale();return new double[]{x*scale,y*scale};
                    }
                }
                throw new AssertionError("No original 40-50 percent party tooltip");
            });
            context.getInput().setCursorPos(partyPoint[0],partyPoint[1]);context.waitTicks(3);
            context.takeScreenshot("hoi-party-scaled-effects-gui2");
            for(String power:List.of("0","2000","minority")) {
                var choices=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"choices-"+power+".json")),DialogView.class);
                context.setScreen(() -> new HoiMenuScreen(backdrop));context.waitTicks(3);
                context.runOnClient(c -> c.gui.setScreen(new PoliticsChoiceScreen(choices,c.gui.screen())));
                context.getInput().setCursorPos(1600,1000);context.waitTicks(3);
                context.takeScreenshot("hoi-politics-cost-"+power+"-gui2");
                var point=context.computeOnClient(c -> {
                    var screen=c.gui.screen();
                    var buttons=screen.children().stream().filter(w -> w instanceof Button).map(w -> (Button)w).toList();
                    var button=buttons.get(power.equals("minority") ? buttons.size() - 1 : 1);
                    double scale=c.getWindow().getGuiScale();
                    return new double[]{(button.getX()+button.getWidth()/2.0)*scale,(button.getY()+button.getHeight()/2.0)*scale};
                });
                context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(18);
                context.takeScreenshot("hoi-politics-choice-tooltip-"+power+"-gui2");
            }
            for (int index = 0; index < 6; index++) {
                var cabinet = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(directory, "cabinet-" + index + ".json")), DialogView.class);
                context.setScreen(() -> new HoiMenuScreen(backdrop)); context.waitTicks(2);
                context.runOnClient(c -> c.gui.setScreen(new PoliticsChoiceScreen(cabinet, c.gui.screen())));
                context.getInput().setCursorPos(1800, 1100); context.waitTicks(3);
                context.takeScreenshot("hoi-government-candidates-" + index + "-gui2");
            }
            var party=dev.hoi.client.ui.HoiMenuBar.indicators(backdrop.hud()).stream().filter(i -> i.icon().equals("party_support")).findFirst().orElseThrow();
            if(!party.value().matches("[0-9]+%"))throw new AssertionError("HUD party precision: "+party.value());
        } catch(java.io.IOException e) {throw new AssertionError("Cost snapshots unavailable",e);}
        try {
            var source = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(
                    java.nio.file.Path.of(directory, "KOR.json"))).view();
            var pages = source.pages().stream().map(page -> page.tab() != MenuTab.POLITICS ? page
                    : new MenuView.Page(page.tab(), page.sections().stream().map(section -> !section.title().equals("국가 중점") ? section
                            : new MenuView.Section(section.title(), section.icon(), section.entries().stream().map(entry -> entry.progress() < 0 ? entry
                                    : new MenuView.Entry(entry.name(), entry.value(), entry.detail(), .5, entry.icon())).toList())).toList())).toList();
            context.setScreen(() -> new HoiMenuScreen(new MenuView(source.country(), source.countryName(), source.date(), source.speed(),
                    pages, source.hud(), source.manufacturers(), source.balance())));
            context.getInput().setCursorPos(1500, 800);
            context.waitTicks(3);
            context.takeScreenshot("hoi-focus-half-progress-fixture-gui2");
        } catch (java.io.IOException e) { throw new AssertionError("Server focus snapshot unavailable", e); }
        try {
            var foreign = new CountryProtocol.Response(java.util.UUID.randomUUID().toString(),
                    java.nio.file.Files.readString(java.nio.file.Path.of(directory, "diplomacy-PRC.json"))).view();
            context.setScreen(() -> new CountryScreen(foreign));
            context.getInput().setCursorPos(1500, 800);
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-diplomacy-PRC-gui2");
            var spiritPoint = context.computeOnClient(c -> {
                var screen = (CountryScreen)c.gui.screen();
                int pane = screen.panelWidth();
                int top = dev.hoi.client.ui.HoiMenuBar.height(screen.width);
                var header = new DiplomacyHeader(foreign, pane);
                int y = DiplomacyHeader.spiritsTop(top, pane) + 10;
                for (int x = 0; x < pane; x++) {
                    var spirit = header.spiritAt(x, y, pane, top);
                    if (spirit != null && spirit.detail().contains("gfx_prc_down_with_bureaucracy_banner")) {
                        double scale = c.getWindow().getGuiScale();
                        return new double[]{x * scale, y * scale};
                    }
                }
                throw new AssertionError("Diplomacy spirit banner not available");
            });
            context.getInput().setCursorPos(spiritPoint[0], spiritPoint[1]);
            context.waitTicks(3); context.takeScreenshot("hoi-diplomacy-spirit-image-PRC-gui2");
            context.getInput().setCursorPos(65, 300);
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-diplomacy-leader-PRC-gui2");
            context.getInput().setCursorPos(1500, 800);
            var changed = new CountryProtocol.Response(java.util.UUID.randomUUID().toString(),
                    java.nio.file.Files.readString(java.nio.file.Path.of(directory, "diplomacy-PRC-changed.json"))).view();
            context.runOnClient(c -> {
                var screen = (CountryScreen)c.gui.screen();
                screen.update(CountryProtocol.Response.of(java.util.UUID.randomUUID().toString(), changed));
                if (!screen.view().equals(foreign)) throw new AssertionError("Unissued party update accepted");
                screen.update(CountryProtocol.Response.of(screen.token(), changed));
                if (!screen.view().equals(changed)) throw new AssertionError("Party update not applied");
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-diplomacy-PRC-changed-gui2");
            var originalMenu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(
                    java.nio.file.Path.of(directory, "PRC.json"))).view();
            var changedMenu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(
                    java.nio.file.Path.of(directory, "PRC-changed.json"))).view();
            context.setScreen(() -> new HoiMenuScreen(originalMenu));
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-parties-PRC-gui2");
            context.runOnClient(c -> ((HoiMenuScreen)c.gui.screen()).update(changedMenu));
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-parties-PRC-changed-gui2");
        } catch (java.io.IOException e) { throw new AssertionError("Server diplomacy snapshot unavailable", e); }
        try {
            var menu = new MenuProtocol.OpenScreen(java.nio.file.Files.readString(java.nio.file.Path.of(directory, "KNU.json"))).view();
            context.setScreen(() -> new HoiMenuScreen(menu));
            context.getInput().setCursorPos(1500, 800);
            context.waitTicks(3);
            context.takeScreenshot("hoi-server-politics-KNU-gui2");
            var foreign = new CountryProtocol.Response(java.util.UUID.randomUUID().toString(),
                    java.nio.file.Files.readString(java.nio.file.Path.of(directory, "diplomacy-KNU.json"))).view();
            context.setScreen(() -> new CountryScreen(foreign));
            context.waitTicks(3);
            context.runOnClient(client -> {
                if (!foreign.name().equals("카렌 민족해방군")) throw new AssertionError("TFR Korean Myanmar name missing");
                if (((CountryScreen)client.gui.screen()).children().stream()
                        .filter(w -> w instanceof Button b && foreign.actions().entries().stream().anyMatch(a -> a.name().equals(b.getMessage().getString()))).count() != foreign.actions().entries().size())
                    throw new AssertionError("Myanmar diplomacy actions not rendered");
            });
            context.takeScreenshot("hoi-server-diplomacy-KNU-gui2");
        } catch (java.io.IOException e) { throw new AssertionError("Myanmar server snapshot unavailable", e); }
        context.getInput().setCursorPos(1500, 800);
        context.getInput().resizeWindow(1600, 1000);
        context.waitTicks(3);
    }

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
        for (int i = 0; i < names.length; i++) overview.add(new MenuView.Entry(names[i], shares[i] + "%", "정당 지지도 · 검증용 분포", shares[i] / 100.0, "politics/party/" + colors[i]));
        sections.set(0, new MenuView.Section("국가 현황", "politics", overview));
        sections.set(1, new MenuView.Section("국가 정신", "politics", java.util.stream.IntStream.range(0, 24)
                .mapToObj(i -> new MenuView.Entry("검증용 정신 " + i, "", "연구 속도: §a+5%§r\n안정도: §4-3%§r\n\n현재 아이콘에 해당하는 설명입니다.", -1, "menu/politics")).toList()));
        sections.removeIf(section -> section.icon().equals("government"));
        sections.add(new MenuView.Section("정부", "government", List.of(new MenuView.Entry("정부 수반", "검증용 관료", "안정도: §a+5%§r\n정치력: §4-3%§r", -1, "politics/empty/leader"))));
        var pages = source.pages().stream().map(p -> p.tab() == MenuTab.POLITICS ? new MenuView.Page(p.tab(), sections) : p).toList();
        context.setScreen(() -> new HoiMenuScreen(new MenuView(source.country(), source.countryName(), source.date(), source.speed(), pages, source.hud())));
        context.waitTicks(2);
        context.runOnClient(c -> {
            var screen = (HoiMenuScreen)c.gui.screen(); var layout = screen.politicsLayout();
            var policy = screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().contains("검증용 관료"))
                    .map(w -> (Button)w).findFirst().orElseThrow();
            var effects = screen.politicsHover(policy.getX() + 2, policy.getY() + 2);
            if (effects == null || !effects.detail().contains("+5%")) throw new AssertionError("Government hover exposes current effect values");

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
                if (button.getMessage().getString().equals("세력")) throw new AssertionError("Faction emblem must not navigate from politics");
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
    private static void partyChartTooltips(ClientGameTestContext context, String directory) throws java.io.IOException {
        var cases = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(
                java.nio.file.Path.of(directory, "party-chart-cases.json"))).getAsJsonObject();
        if (cases.isEmpty()) throw new AssertionError("Server party chart cases are missing");
        for (var entry : cases.entrySet()) for (boolean selection : new boolean[]{false, true}) {
            String mode = selection ? "selection" : "politics";
            String json = java.nio.file.Files.readString(java.nio.file.Path.of(directory, "party-chart-" + entry.getKey() + "-" + mode + ".json"));
            var view = selection ? new MenuProtocol.SelectionPreview(json).view() : new MenuProtocol.OpenScreen(json).view();
            String icon = entry.getValue().getAsJsonArray().get(1).getAsString();
            var parties = view.page(MenuTab.POLITICS).sections().stream().flatMap(section -> section.entries().stream())
                    .filter(party -> party.icon().startsWith("politics/party/") && party.progress() >= 0).toList();
            var expected = parties.stream().filter(party -> party.icon().equals(icon)).findFirst().orElseThrow();
            double before = parties.subList(0, parties.indexOf(expected)).stream().mapToDouble(MenuView.Entry::progress).sum();
            double angle = (before + expected.progress() / 2) * Math.PI * 2 - Math.PI / 2;
            context.setScreen(() -> selection ? HoiMenuScreen.selection(view) : new HoiMenuScreen(view));
            context.waitTicks(3);
            var point = context.computeOnClient(c -> {
                var screen = (HoiMenuScreen)c.gui.screen(); var box = screen.politicsLayout().partyChart();
                double x = box.x() + box.width() / 2.0 + Math.cos(angle) * box.width() / 4;
                double y = box.y() + box.height() / 2.0 + Math.sin(angle) * box.height() / 4;
                if (!expected.equals(screen.politicsHover(x, y)))
                    throw new AssertionError("Chart must expose the complete server party entry: " + view.country() + " " + expected.name());
                if (screen.politicsHover(box.x() + 1, box.y() + 1) != null)
                    throw new AssertionError("Chart corner must not have a tooltip");
                var colors = new HashSet<Integer>();
                for (var line : dev.hoi.client.ui.HoiTooltips.lines(c.font, expected.detail(), 1280))
                    line.accept((index, style, character) -> {
                        if (style.getColor() != null) colors.add(style.getColor().getValue());
                        return true;
                    });
                if (entry.getKey().equals("zero")) {
                    if (expected.detail().contains("매일 변화:")) throw new AssertionError("Zero growth must stay hidden");
                } else {
                    int color = entry.getKey().equals("ruling-rise") || entry.getKey().equals("opposition-fall") ? 0x56AC5B : 0xDE5646;
                    if (!colors.contains(color)) throw new AssertionError("Server drift lost its TFR color: " + expected.detail());
                }
                double scale = c.getWindow().getGuiScale();
                return new double[]{(x + (selection ? screen.selectionLeft() : 0)) * scale, y * scale};
            });
            context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(3);
            context.takeScreenshot("hoi-server-" + view.country() + "-" + mode + "-party-chart-" + entry.getKey() + "-gui2");
        }
        context.getInput().setCursorPos(1500, 800);
    }

    public static void adviserFrames(ClientGameTestContext context) {
        context.setScreen(() -> new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("인물 프레임 검증")) {
            @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float delta) {
                g.fill(0, 0, width, height, 0xFF202226);
                String[] ids = {"sov_mikhailov", "pak_jong_chon", "ast_rick_burr_chief", "hum_david_brian_stone_high_command", "mim_paul_d_rogers_high_command", "kor_lee_joung_soo_high_command"};
                for (int i = 0; i < ids.length; i++) {
                    int x = 20 + i * 195;
                    if (!dev.hoi.client.ui.UiAssets.draw(g, "politics/appointments/" + ids[i], x, 60, 124, 134))
                        throw new AssertionError("Adviser portrait is absent: " + ids[i]);
                    dev.hoi.client.ui.UiText.cell(g, font, ids[i], x - 10, 205, 180, 24, 0xFFFFFFFF, false);
                    dev.hoi.client.ui.UiAssets.draw(g, "politics/appointments/" + ids[i], x + 42, 245, 31, 34);
                }
            }
        });
        dev.hoi.client.ResearchScreenGameTest.gui2Screenshot(context, "hoi-adviser-original-frames");
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
        var diplomacy = new ArrayList<MenuView.Entry>(List.of(
                new MenuView.Entry("관계", "비동맹", ""),
                new MenuView.Entry("집권 정당", "중국공산당", ""),
                new MenuView.Entry("이념", "덩샤오핑 이론", "", -1, "politics/art/gfx_ideology_prc_market_socialism"),
                new MenuView.Entry("지도자", "시진핑", "", -1, "politics/leaders/prc_xi_jinping"),
                new MenuView.Entry("다음 선거", "선거 없음", ""),
                new MenuView.Entry("안정도", "-100%", "현재 안정도", -1, "hud/stability"),
                new MenuView.Entry("전쟁 지지도", "100%", "현재 전쟁 지지도", 1, "hud/war_support")));
        diplomacy.add(new MenuView.Entry("세력","검증용 세력","",-1,"politics/art/gfx_faction_member"));
        for (int i = 0; i < 24; i++) diplomacy.add(new MenuView.Entry("보유 국민정신 " + i, "", "국가 정신\n안정도: §a+5%§r", -1, "politics/art/gfx_idea_prc_chinese_communist_party"));
        var fixture = new CountryView("KOR",hud,"PRC","중화인민공화국",false,List.of(new CountryView.Choice("KOR","대한민국"),new CountryView.Choice("PRC","중국")),
                diplomacy,List.of(new MenuView.Entry("알려지지 않은 중점","민간 첩보 70% 필요","")),reports,null,List.of(
                    new CountryView.Relation("세력","diplomacy/relations/faction",List.of(new CountryView.Choice("PRK","조선민주주의인민공화국"),new CountryView.Choice("JAP","일본"),new CountryView.Choice("KOR","대한민국"))),
                    new CountryView.Relation("군사 통행권","diplomacy/relations/offer_military_access_initiator",List.of(new CountryView.Choice("KOR","대한민국")))));
        context.setScreen(() -> new CountryScreen(fixture)); context.waitTicks(3); context.takeScreenshot("hoi-country-diplomacy"); dev.hoi.client.ResearchScreenGameTest.gui2Screenshot(context, "hoi-country-diplomacy");
        var sixSpirits = new CountryView(fixture.viewer(), fixture.hud(), fixture.target(), fixture.name(), fixture.shared(), fixture.countries(),
                diplomacy.subList(0, diplomacy.size() - 18), fixture.focuses(), fixture.reports(), fixture.actions(), fixture.relations());
        context.setScreen(() -> new CountryScreen(sixSpirits)); context.waitTicks(3);
        context.runOnClient(c -> {
            int pane = ((CountryScreen)c.gui.screen()).panelWidth(), top = dev.hoi.client.ui.HoiMenuBar.height(c.gui.screen().width);
            var header = new DiplomacyHeader(sixSpirits, pane);
            int x = DiplomacyHeader.scaled(140, pane) + 2, y = DiplomacyHeader.spiritsTop(top, pane) + 8;
            var first = header.spiritAt(x, y, pane, top);
            header.scrollSpirits(x, y, -20, pane, top);
            if (!first.equals(header.spiritAt(x, y, pane, top))) throw new AssertionError("One spirit row must not scroll into an empty row");
        });
        dev.hoi.client.ResearchScreenGameTest.gui2Screenshot(context, "hoi-country-six-spirits");
        var allFlags = context.computeOnClient(c -> c.getResourceManager().listResources("textures/gui/country",
                id -> id.getNamespace().equals("hoi") && id.getPath().matches("textures/gui/country/[a-z]{3}/flag\\.png"))
                .keySet().stream().map(id -> id.getPath().split("/")[3].toUpperCase(Locale.ROOT)).sorted().toList());
        if (allFlags.isEmpty()) throw new AssertionError("Country flags unavailable for diplomacy coverage");
        for (String tag : allFlags) {
            var country = new CountryView(fixture.viewer(), fixture.hud(), tag, tag, false, fixture.countries(),
                    List.of(new MenuView.Entry("관계", "비동맹", "")), fixture.focuses(), fixture.reports(), null, List.of());
            context.setScreen(() -> new CountryScreen(country)); context.waitTicks(1);
            context.runOnClient(c -> {
                var screen = (CountryScreen)c.gui.screen();
                if (screen.children().stream().anyMatch(w -> w instanceof net.minecraft.client.gui.components.Button b
                        && b.getMessage().getString().startsWith("관계 ·")))
                    throw new AssertionError("Empty relations reopened the legacy diplomacy list: " + tag);
                if (screen.children().stream().noneMatch(w -> w instanceof net.minecraft.client.gui.components.Button b
                        && b.getMessage().getString().equals("외교")))
                    throw new AssertionError("Current diplomacy header missing: " + tag);
            });
        }
        System.out.println("Current diplomacy layout checked for " + allFlags.size() + " country flags");
        dev.hoi.client.ResearchScreenGameTest.gui2Screenshot(context, "hoi-country-empty-relations");
        var noFactionEntries = new ArrayList<>(diplomacy.stream().filter(e -> !e.name().equals("세력")).toList());
        noFactionEntries.add(new MenuView.Entry("세력", "세력 없음", "", -1, "politics/art/gfx_faction_member"));
        var noFaction = new CountryView(fixture.viewer(), fixture.hud(), fixture.target(), fixture.name(), fixture.shared(), fixture.countries(),
                noFactionEntries, fixture.focuses(), fixture.reports(), fixture.actions(), List.of());
        context.setScreen(() -> new CountryScreen(noFaction)); context.waitTicks(3);
        dev.hoi.client.ResearchScreenGameTest.gui2Screenshot(context, "hoi-country-no-faction");
        context.setScreen(() -> new CountryScreen(fixture)); context.waitTicks(3);
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(c -> c.options.guiScale().set(2)); context.waitTicks(3);
        context.runOnClient(c -> {
            int pane = ((CountryScreen)c.gui.screen()).panelWidth(), top = dev.hoi.client.ui.HoiMenuBar.height(c.gui.screen().width);
            var header = new DiplomacyHeader(fixture, pane);
            int x = DiplomacyHeader.scaled(140, pane) + 2, y = DiplomacyHeader.spiritsTop(top, pane) + 8;
            var first = header.spiritAt(x, y, pane, top);
            if (first == null || !first.name().equals("보유 국민정신 0")) throw new AssertionError("Active spirit hover");
            int bottom = DiplomacyHeader.spiritsTop(top, pane) + Math.max(32, DiplomacyHeader.scaled(72, pane)) - 5;
            if (!header.spiritAt(pane - 19, y, pane, top).name().equals("보유 국민정신 5"))
                throw new AssertionError("National spirits must use six columns");
            if (!header.scrollSpirits(x, y, -1, pane, top)
                    || !header.spiritAt(x, y, pane, top).name().equals("보유 국민정신 6"))
                throw new AssertionError("Vertical scrolling must advance to the next six spirits");
            if (!header.scrollSpirits(x, y, -20, pane, top)
                    || !header.spiritAt(pane - 19, bottom, pane, top).name().equals("보유 국민정신 23"))
                throw new AssertionError("Vertical scroll must reach the final spirit row");
            header.scrollSpirits(x, y, 20, pane, top);
            if (!header.spiritAt(x, y, pane, top).equals(first)) throw new AssertionError("Spirit scroll must clamp at the first row");
            if (!c.gui.screen().mouseScrolled(x, y, 0, -20)) throw new AssertionError("Diplomacy must route vertical spirit scrolling");
        });
        context.waitTicks(3);
        context.takeScreenshot("hoi-country-spirits-scrolled-gui2");
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
        context.runOnClient(c -> {
            var screen=(CountryScreen)c.gui.screen();
            int pane=screen.panelWidth(),top=dev.hoi.client.ui.HoiMenuBar.height(screen.width);
            int y=DiplomacyHeader.body(top,pane)+18;
            if(screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(15,y,new net.minecraft.client.input.MouseButtonInfo(0,0)),false))
                throw new AssertionError("Diplomacy relation background must not be clickable");
            if(screen.children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().startsWith("세력 ·")))
                throw new AssertionError("Diplomacy relation row must not have hover/click widgets");
            var selected=screen.flagAt(40,y);
            if(selected==null||!selected.tag().equals("PRK"))throw new AssertionError("Visible diplomacy flag target");
            String previous=screen.token();
            if(!screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(40,y,new net.minecraft.client.input.MouseButtonInfo(0,0)),false)
                    ||screen.view()!=null||screen.token().equals(previous))throw new AssertionError("Flag click must open a fresh country request");
            screen.update(CountryProtocol.Response.of(screen.token(),fixture));
            if(screen.view()!=null)throw new AssertionError("Clicked flag must not accept another country's response");
            var response=new CountryView(fixture.viewer(),fixture.hud(),"PRK","조선민주주의인민공화국",false,fixture.countries(),
                    fixture.diplomacy(),fixture.focuses(),fixture.reports(),null,fixture.relations());
            screen.update(CountryProtocol.Response.of(screen.token(),response));
            if(screen.view()==null||!screen.view().target().equals("PRK"))throw new AssertionError("Clicked country's diplomacy did not open");
            c.gui.setScreen(new CountryScreen(fixture));
        });
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
