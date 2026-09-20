package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TooltipTextTest {
    @Test void exactTooltipSpacingPreservesZeroAndPrecisionWithoutEmptyMargins() {
        assertEquals("수송선: 0\n\n효율: §c20.00%§r",TooltipText.spacing("\n§r\n수송선: 0\n\n \n효율: §c20.00%§r\n\n§r"));
    }

    @Test void rgbColorsDoNotBecomeAmountsAndCaptionsKeepTheirText() {
        assertEquals("§e집권 정당: §#3659D9더불어민주당§r",TooltipText.format("§e집권 정당: §#3659D9더불어민주당§r"));
        assertEquals("",TooltipText.format("공격: §#3659D90§r"));
        assertEquals("[image:map_detail/art/portrait]\n[caption:§f오거돈§r]",TooltipText.format("[image:map_detail/art/portrait]\n[caption:§f오거돈§r]"));
    }

    @Test void hidesZeroRowsAndCollapsesRepeatedEmptyLines() {
        assertEquals("§e현대화된 농업§r\n\n§c제거 불가능",TooltipText.format("§e현대화된 농업§r\n\n  \n§c제거 불가능"));
        assertEquals("사단 방어: §c-5%§r",TooltipText.format("사단 훈련 시간: 0\n특수부대 수용량: +0.000%\n사단 방어: §c-5%§r\n육군 고문 비용: §a0§r"));
    }
    @Test void roundsOrdinaryAmountsAndRemovesRoundedZero() {
        assertEquals("공격: §c-12.3%§r\n에너지: 2\n손실: 13.5",TooltipText.format("공격: §c-12.345%§r\n에너지: 2.000\n손실: 13.45\n사고: 0.001%"));
    }
    @Test void preservesPartyPrecisionAndNonAmountNumbers() {
        assertEquals("매일 사회자유주의 지지도: §a+0.01§r",TooltipText.format("매일 사회자유주의 지지도: §a+0.01§r"));
        assertEquals("정당 지지도: 48.1234%\n매일 변화: +0.0025%",TooltipText.format("정당 지지도: 48.1234%\n매일 변화: +0.0025%"));
        assertEquals("슬롯: 0/5\n2020-01-01\nhoi:air_0\n[image:air_detail/result_0]",TooltipText.format("슬롯: 0/5\n2020-01-01\nhoi:air_0\n[image:air_detail/result_0]"));
    }
    @Test void hidesResourceZeroBaseAndPreservesSourceMultipliers() {
        String text="§e석유§r\n기본: §e0§r £resources_strip|1\n건물: §e-12§r £resources_strip|1\n   기반시설: §ex2.00§r\n   보급 경로: §ex1.10§r";
        assertEquals(text.replace("기본: §e0§r £resources_strip|1\n",""),TooltipText.format(text));
    }    @Test void sourceEffectsKeepExactDecimals() {
        String text="£fuel_texticon 연료 보유량: §a+20.00%§r\n£GFX_dx_yellow 개인 비용 (B): §e+0.125§r";
        assertEquals(text,TooltipText.format(text));
        assertEquals("사단 방어: §a+20.00%§r",TooltipText.format("사단 방어: §a+20.00%§r",true));
    }
}
