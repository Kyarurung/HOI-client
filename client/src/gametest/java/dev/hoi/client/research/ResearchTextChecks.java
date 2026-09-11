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
            var labeled = KeywordIcons.decorate(Component.literal("건설 속도: +15%"));
            check(labeled.getString().endsWith(" 건설 속도: +15%") && labeled.getString().charAt(0) >= 0xe000, "Source-bound keyword glyph precedes construction speed");
            labeled.visit((style, part) -> {
                if (part.contains("건설 속도")) check(style.getFont().equals(Style.EMPTY.getFont()), "Keyword font never leaks into Korean text");
                return Optional.empty();
            }, Style.EMPTY);
            var bonus = ResearchText.effect("월간 인구 증가: +7.0%");
            check(bonus.getString().equals("월간 인구 증가: +7%"), "Integral percentages have no decimal suffix");
            color(bonus, "+7%", ChatFormatting.GREEN);
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
            check(ResearchText.decorateEffect(ResearchText.effect("건설 속도: -15%")).getString().equals("건설 속도: -15%"), "Penalty has no beneficial-upgrade icon");
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
