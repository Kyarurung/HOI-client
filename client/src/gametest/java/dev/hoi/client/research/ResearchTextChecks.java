package dev.hoi.client.research;

import dev.hoi.client.ui.KeywordIcons;

import dev.hoi.protocol.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class ResearchTextChecks {
    public static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var resources=KeywordIcons.decorateLines(Component.literal("+6£resources_strip|7 -4£resources_strip|1 -1£resources_strip|5"));
            check(!resources.getString().contains("£")&&!resources.getString().contains("|"), "Resource frame syntax is fully consumed");
            check(resources.getString().chars().filter(c->c>=0xe000&&c<=0xf8ff).distinct().count()==3, "Energy, oil and steel use distinct source frames");
            var labeled = KeywordIcons.decorate(Component.literal("건설 속도: +15%"));
            check(labeled.getString().endsWith(" 건설 속도: +15%") && labeled.getString().charAt(0) >= 0xe000, "Source-bound keyword glyph precedes construction speed");
            for (String label : List.of("기반시설 건설 속도", "사무단지 건설 속도"))
                check(KeywordIcons.decorate(Component.literal(label + ": -5%")).getString().charAt(0) == labeled.getString().charAt(0), "Buildings share construction speed art");
            var detail = KeywordIcons.decorateLines(dev.hoi.client.ui.HoiTooltips.sentences(
                    Component.literal("낮은 금리\n  건설 속도: §a+5%§r\n\n§c제거 불가능§r")));
            check(detail.getString().equals("낮은 금리\n" + labeled.getString().charAt(0) + " 건설 속도: +5%\n\n제거 불가능"),
                    "Choice tooltips decorate each effect, trim source indentation and preserve blank rows");
            detail.visit((style, part) -> {
                if (part.contains("+5%")) check(style.getColor().getValue() == dev.hoi.client.ui.HoiTooltips.GOOD, "Effect icon insertion preserves TFR colours");
                return Optional.empty();
            }, Style.EMPTY);
            char business = KeywordIcons.decorate(Component.literal("사업 가치 변동치: +5%")).getString().charAt(0);
            char personal = KeywordIcons.decorate(Component.literal("개인 가치 변동치: -5%")).getString().charAt(0);
            check(business >= 0xe000 && personal >= 0xe000 && business != personal, "Business and personal value use distinct source dollar art");
            labeled.visit((style, part) -> {
                if (part.contains("건설 속도")) check(style.getFont().equals(Style.EMPTY.getFont()), "Keyword font never leaks into Korean text");
                return Optional.empty();
            }, Style.EMPTY);
            check(ResearchText.zeroEffect("공중 우세: 0") && ResearchText.zeroEffect("유지비: +0.0%"), "Exact zero effects are omitted");
            check(!ResearchText.zeroEffect("유지비: +0.01%") && !ResearchText.zeroEffect("K-200") && !ResearchText.zeroEffect("2000년형"), "Small effects and names remain visible");
            var bonus = ResearchText.effect("매월 인구: +7.0%");
            check(bonus.getString().equals("매월 인구: +7%"), "Integral percentages have no decimal suffix");
            color(bonus, "+7%", ChatFormatting.GREEN);
            check(ResearchText.decorateEffect(bonus).getString().charAt(0) == 0xe05f, "Monthly population uses its original population icon");
            color(ResearchText.effect("기갑 비율: 0.05"), "+5%", ChatFormatting.GREEN);
            color(ResearchText.effect("속도: -1%"), "-1%", ChatFormatting.DARK_RED);
            var cost = ResearchText.effect("민간공장 건설 비용: +2.5%");
            color(cost, "+2.5%", ChatFormatting.DARK_RED);
            color(cost, "민간공장", ChatFormatting.GOLD);
            color(cost, " 건설 비용: ", ChatFormatting.WHITE);
            color(ResearchText.effect("연료 사용량: -100%"), "-100%", ChatFormatting.GREEN);
            for (String label : List.of("공장 폭격 취약도", "건설 비용", "유지비 변동치", "연료 사용량", "소비재", "가시성", "훈련 시간", "항공기 사고 확률", "치명타 입을 확률", "치명타 지속 효과", "상륙 준비 시간", "어뢰 발각 확률", "이동시 조직력 손실", "적의 공중 지원")) {
                for (String value : List.of("-5%", "+5%")) {
                    var effect = ResearchText.effect(label + ": " + value);
                    check(effect.getString().equals(label + ": " + value), "Benefit colors must never reverse the source sign");
                    color(effect, value, value.startsWith("-") ? ChatFormatting.GREEN : ChatFormatting.DARK_RED);
                }
            }
            for (String title : List.of("장갑 화물 열차", "장갑 화물 열차 II", "장갑 화물 열차 3", "공격 헬리콥터"))
                check(ResearchText.decorateEffect(ResearchText.gold(title)).getString().equals(title), "Names never acquire stat icons");
            check(ResearchText.decorateEffect(ResearchText.effect("건설 속도: +15%")).getString().charAt(0) >= 0xe000, "Beneficial upgrade retains its stat icon");
            check(ResearchText.decorateEffect(ResearchText.effect("건설 속도: -15%")).getString().charAt(0) >= 0xe000, "Penalty retains the original stat icon");
            var development = ResearchText.decorateEffect(ResearchText.effect("£development 학문 발전도 획득: +15%"));
            check(!development.getString().contains("£") && development.getString().charAt(0) >= 0xe000, "Development gain renders its original inline icon");
            color(development, "+15%", ChatFormatting.GREEN);
            var stability = ResearchText.decorateEffect(ResearchText.effect("£stability_texticon 기본 안정도 획득: -5%"));
            check(!stability.getString().contains("£") && stability.getString().charAt(0) >= 0xe000, "Base stability gain renders the original stability icon");
            color(stability, "-5%", ChatFormatting.DARK_RED);
            check(ResearchText.effect("속도: +2.555%").getString().equals("속도: +2.5%"), "Displayed precision truncates to one decimal after conversion");
            var attack = ResearchText.effect("대물 공격: 0.5");
            check(attack.getString().equals("대물 공격: 0.5"), "Absolute equipment stats are never multiplied by 100");
            color(attack, "0.5", ChatFormatting.GOLD);
            for (String label : List.of("정비 인력", "조직력", "훈련 시간"))
                color(ResearchText.effect(label + ": 100"), "100", ChatFormatting.WHITE);
        });
        ResearchView source;
        try (var input = ResearchTextChecks.class.getResourceAsStream("/tfr-research-view.json")) {
            source = new ResearchProtocol.Response(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).view();
        } catch (Exception e) { throw new AssertionError(e); }
        for (int[] size : new int[][]{{1600, 1000}, {854, 480}}) {
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(2);
            for (String id : List.of("infantry_weapons1", "infantry_weapons2")) {
                var original = source.technology("hoi:tfr/technology/" + id);
                String name = id.endsWith("1") ? "K1 기관단총" : "K2 돌격소총";
                String generic = id.endsWith("1") ? "냉전 이후 보병장비" : "현대 보병장비";
                var s = original.source();
                var tech = new ResearchView.Tech(original.id(), original.category(), name, 2025, original.tier(), original.baseDays(),
                        0, 1, original.prerequisites(), original.effects(), original.unlocks(), ResearchView.Status.LOCKED,
                        new ResearchView.Source(s.id(), s.folder(), s.x(), s.y(), s.vertical(), s.anyOf(), s.excludes(), s.conditions(),
                                s.unlockLabels(), s.description(), s.deferredEffects(), Map.of(generic, name)));
                var techs = source.technologies().stream().map(t -> t.id().equals(tech.id()) ? tech : t).toList();
                var view = new ResearchView(source.session(), source.revision(), source.country(), source.countryName(), 0,
                        source.date(), source.speed(), source.slots(), techs, "", source.hud(), source.benefits());
                context.setScreen(() -> new ResearchScreen(view, request -> {}));
                context.runOnClient(client -> {
                    var screen = (ResearchScreen)client.gui.screen(); screen.showDetail(tech.id());
                    var lines = screen.tooltipLines(tech);
                    color(lines.getFirst(), name, ChatFormatting.GOLD);
                    check(lines.stream().anyMatch(c -> c.getString().startsWith("연구 시간: ")), "Tooltip shows the research duration");
                    check(lines.stream().anyMatch(c -> c.getString().startsWith("5.00년 앞선 기술입니다.")), "Ahead warning uses campaign date");
                    check(lines.stream().anyMatch(c -> c.getString().equals(" ")), "Effects have a blank separator");
                    var unlock = lines.stream().filter(c -> c.getString().equals("사용 가능 " + name)).findFirst().orElseThrow();
                    color(unlock, name, ChatFormatting.GOLD);
                    check(ResearchDetails.cards(tech, "KOR").stream().anyMatch(c -> c.name().equals(name)), "Equipment card uses country translation");
                    if (id.endsWith("2")) check(lines.stream().anyMatch(c -> c.getString().startsWith("기술 필요 (")), "Missing source prerequisites are shown");
                    for (String unit : List.of("경보병", "민병")) lines.stream().filter(c -> c.getString().equals("사용 가능 " + unit))
                            .forEach(c -> color(c, unit, ChatFormatting.WHITE));
                });
                context.waitTicks(3); context.takeScreenshot("hoi-localized-" + id + "-" + size[0]);
                context.setScreen(() -> new net.minecraft.client.gui.screens.Screen(Component.literal("연구 툴팁")) {
                    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor g, int mx, int my, float delta) {
                        g.fill(0, 0, width, height, 0xff101a21);
                        var screen = new ResearchScreen(view, request -> {});
                        var lines = screen.tooltipLines(tech).stream().flatMap(c -> font.split(c, Math.min(310, width - 24)).stream()).toList();
                        g.setTooltipForNextFrame(font, lines, 20, 20);
                    }
                });
                context.waitTicks(2); context.takeScreenshot("hoi-tooltip-" + id + "-" + size[0]);
            }
        }
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().resizeWindow(2560, 1440); context.waitTicks(3);
        context.setScreen(() -> new ResearchScreen(source, request -> {}));
        for(int index=0;index<2;index++) {
            final int half=index;
            double[] cursor=context.computeOnClient(client -> {
                var screen=(ResearchScreen)client.gui.screen();
                int pane=dev.hoi.client.ui.HoiPanelLayout.width(dev.hoi.protocol.MenuTab.RESEARCH,screen.width);
                int top=dev.hoi.client.ui.HoiMenuBar.height(screen.width);
                int banner=Math.max(1,Math.min((pane-12)*138/532,screen.height-top-80-6*17));
                return new double[]{(pane*(half==0?.25:.75))*client.getWindow().getGuiScale(),(top+72+banner-16)*client.getWindow().getGuiScale()};
            });
            context.getInput().setCursorPos(cursor[0],cursor[1]);context.waitTicks(3);
            context.takeScreenshot("hoi-research-overview-tooltip-"+index+"-gui2");
        }
        context.getInput().setCursorPos(1800,1000);
        for(var original:source.technologies()) {
            if(original.source()==null||original.source().parentTechnology().isEmpty())continue;
            var child=new ResearchView.Tech(original.id(),original.category(),original.name(),original.year(),original.tier(),original.baseDays(),
                    original.progress(),original.dailyRate(),original.prerequisites(),original.effects(),original.unlocks(),ResearchView.Status.AVAILABLE,original.source(),original.tooltipOnlyEffects());
            var technologies=source.technologies().stream().map(t->t.id().equals(child.id())?child:t).toList();
            var view=new ResearchView(source.session(),source.revision(),source.country(),source.countryName(),source.day(),source.date(),source.speed(),source.slots(),technologies,"",source.hud(),source.benefits());
            var requests=new ArrayList<ResearchProtocol.Request>();
            context.setScreen(()->new ResearchScreen(view,requests::add));
            context.runOnClient(client->{
                var screen=(ResearchScreen)client.gui.screen();
                screen.showDetail(child.source().parentTechnology());
                check(screen.subTechnology(view.technology(child.source().parentTechnology())).id().equals(child.id()),"Original child research is reachable");
                check(!ResearchDetails.cards(child,"KOR").isEmpty(),"Child research has original equipment details");
                if(List.of("amphibious_mechanized_infantry","amphibious_mechanized_infantry_2","ship_hull_super_heavy","ship_hull_advanced_super_heavy").contains(child.source().id()))
                    check(ResearchDetails.cards(child,"KOR").stream().filter(c->c.kind().equals("장비")).allMatch(c->c.texture().startsWith("technology/country/kor/")),"Sub-technology research uses original Korean equipment art");
            });
            context.waitTicks(2);context.takeScreenshot("hoi-subtechnology-parent-"+child.source().id()+"-gui2");
            context.runOnClient(client->{
                var button=client.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals(child.name()))
                        .map(c->(net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();
                button.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
            });
            context.waitTicks(2);context.takeScreenshot("hoi-subtechnology-detail-"+child.source().id()+"-gui2");
            context.runOnClient(client->{
                var button=client.gui.screen().children().stream().filter(c->c instanceof net.minecraft.client.gui.components.Button b&&b.active&&b.getMessage().getString().equals("연구"))
                        .map(c->(net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();
                button.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
                check(requests.size()==1&&requests.getFirst().action()==ResearchProtocol.Action.START&&requests.getFirst().technology().equals(child.id()),"Child button starts its own server research");
            });
        }

        for (String id : List.of("mechanised_infantry3", "dispersed_industry4", "basic_nanomachines", "tech_10000_qubit_computing")) {
            var tech = source.technologies().stream().filter(t -> t.id().equals("hoi:tfr/technology/" + id)).findFirst();
            if (tech.isEmpty()) continue;
            context.setScreen(() -> new ResearchScreen(source, request -> {}));
            context.runOnClient(client -> {
                var screen = (ResearchScreen)client.gui.screen();
                screen.showDetail(tech.get().id());
                var effects = screen.detailLines(tech.get()).stream().map(Component::getString).toList();
                check(effects.stream().noneMatch(line -> line.endsWith(" 해금") || ResearchText.zeroEffect(line)), "Detail effects omit duplicate unlocks and zero values");
                for(String effect:tech.get().tooltipOnlyEffects()) {
                    check(effects.stream().noneMatch(line -> line.equals(ResearchText.effect(effect).getString())), "Completion rewards stay outside detail effects");
                    check(screen.tooltipLines(tech.get()).stream().anyMatch(line -> line.getString().equals(ResearchText.effect(effect).getString())), "Completion rewards remain in tooltips");
                }
                if (id.equals("mechanised_infantry3")) check(!ResearchDetails.cards(tech.get(), "KOR").isEmpty(), "Removing the unlock effect preserves equipment details");
            });
            context.waitTicks(3); context.takeScreenshot("hoi-research-effects-" + id + "-gui2");
        }
        context.setScreen(() -> new ResearchScreen(source, request -> {}));
        context.runOnClient(client -> {
            var screen = (ResearchScreen)client.gui.screen();
            screen.showDetail("hoi:tfr/technology/power_plants4");
            var close = screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("×"))
                    .map(w -> (net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
            close.onPress(new net.minecraft.client.input.MouseButtonEvent(close.getX() + 1, close.getY() + 1, new net.minecraft.client.input.MouseButtonInfo(0, 0)));
        });
        context.waitTicks(3); context.takeScreenshot("hoi-engineering-exclusive-caption-gui2");
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(2);
        context.setScreen(() -> null);
    }
    private static void color(Component component, String text, ChatFormatting expected) {
        var colors = new ArrayList<Integer>(); var contents = new StringBuilder();
        component.visit((style, part) -> {
            contents.append(part);
            for (int i = 0; i < part.length(); i++) colors.add(style.getColor() == null ? -1 : style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        int start = contents.indexOf(text);
        check(start >= 0, "Missing text: " + text + " in " + contents);
        int expectedColor = Component.empty().withStyle(expected).getStyle().getColor().getValue();
        for (int i = start; i < start + text.length(); i++) check(colors.get(i).equals(expectedColor), "Incorrect color for " + text);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
