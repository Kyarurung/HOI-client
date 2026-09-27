package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UiTooltipTextTest {
    @Test void sourceSpacingPreservesBlankLinesColorsZeroValuesAndDecimalPlaces() {
        String source="§e보급§r\n\n\n효율: §a+5.00%§r\n§f----§r\n연료: §e0.00§r\n";
        assertEquals(source.replace("§f----§r",""),UiTooltipText.sourceSpacing(source));
    }
    @Test void terrainColorsContinueAcrossParagraphsUntilReset() {
        assertEquals(java.util.List.of("§#FCA97D서울 설명", "", "§#FCA97D다음 문단§r", "§#FB607FSeoul, my soul§r", "기본"),
                UiTooltipText.colorLines("§#FCA97D서울 설명\n\n다음 문단§r\n§#FB607FSeoul, my soul§r\n기본"));
        assertEquals(java.util.List.of("§e직함", "[image:portrait]", "[caption:§f박원순§r]", "정당"),
                UiTooltipText.colorLines("§e직함\n[image:portrait]\n[caption:§f박원순§r]\n정당"));
    }

    @Test void exactTooltipSpacingPreservesZeroAndPrecisionWithoutEmptyMargins() {
        assertEquals("수송선: 0\n\n효율: §c20.00%§r",UiTooltipText.spacing("\n§r\n수송선: 0\n\n \n효율: §c20.00%§r\n\n§r"));
    }

    @Test void sourceSeparatorsBecomeBlankLinesWithoutChangingHyphenatedValues() {
        assertEquals("숲\n\n공격: -12.5%",UiTooltipText.spacing("숲\n§f----§r\n공격: -12.5%"));
    }

    @Test void populationPreservesZeroGrowthAndParagraphs() {
        String text="§e민간인 인구§f: §e3.00M§f/§e3.00M§f\n\n§e일간 성장§f: 0.00% (모병 가능: 0, 민간인: 0)\n\n기본: §e0.00%§f";
        assertEquals(text,UiTooltipText.format(text,true));
    }

    @Test void rgbColorsDoNotBecomeAmountsAndCaptionsKeepTheirText() {
        assertEquals("§e집권 정당: §#3659D9더불어민주당§r",UiTooltipText.format("§e집권 정당: §#3659D9더불어민주당§r"));
        assertEquals("",UiTooltipText.format("공격: §#3659D90§r"));
        assertEquals("[image:map_detail/art/portrait]\n[caption:§f오거돈§r]",UiTooltipText.format("[image:map_detail/art/portrait]\n[caption:§f오거돈§r]"));
    }

    @Test void hidesZeroRowsAndCollapsesRepeatedEmptyLines() {
        assertEquals("§e현대화된 농업§r\n\n§c제거 불가능",UiTooltipText.format("§e현대화된 농업§r\n\n  \n§c제거 불가능"));
        assertEquals("사단 방어: §c-5%§r",UiTooltipText.format("사단 훈련 시간: 0\n특수부대 수용량: +0.000%\n사단 방어: §c-5%§r\n육군 고문 비용: §a0§r"));
    }
    @Test void roundsOrdinaryAmountsAndRemovesRoundedZero() {
        assertEquals("공격: §c-12.3%§r\n에너지: 2\n손실: 13.5",UiTooltipText.format("공격: §c-12.345%§r\n에너지: 2.000\n손실: 13.45\n사고: 0.001%"));
    }
    @Test void preservesPartyPrecisionAndNonAmountNumbers() {
        assertEquals("매일 사회자유주의 지지도: §a+0.01§r",UiTooltipText.format("매일 사회자유주의 지지도: §a+0.01§r"));
        assertEquals("정당 지지도: 48.1234%\n매일 변화: +0.0025%",UiTooltipText.format("정당 지지도: 48.1234%\n매일 변화: +0.0025%"));
        assertEquals("슬롯: 0/5\n2020-01-01\nhoi:air_0\n[image:air_detail/result_0]",UiTooltipText.format("슬롯: 0/5\n2020-01-01\nhoi:air_0\n[image:air_detail/result_0]"));
    }
    @Test void hidesResourceZeroBaseAndPreservesSourceMultipliers() {
        String text="§e석유§r\n기본: §e0§r £resources_strip|1\n건물: §e-12§r £resources_strip|1\n   기반시설: §ex2.00§r\n   보급 경로: §ex1.10§r";
        assertEquals(text.replace("기본: §e0§r £resources_strip|1\n",""),UiTooltipText.format(text));
    }    @Test void sourceEffectsKeepExactDecimals() {
        String text="£fuel_texticon 연료 보유량: §a+20.00%§r\n£GFX_dx_yellow 개인 비용 (B): §e+0.125§r";
        assertEquals(text,UiTooltipText.format(text));
        assertEquals("사단 방어: §a+20.00%§r",UiTooltipText.format("사단 방어: §a+20.00%§r",true));
    }
}
