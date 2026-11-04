package dev.hoi.client.screen.decision;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DecisionTextTest {
    @Test void balancedBlockedCostRestoresTheOuterSourceColorAcrossLines() {
        assertEquals(List.of(new DecisionText.Run("자주포",0xFFBD00),new DecisionText.Run(" 250대 필요",0xCD3737)),
                DecisionText.sourceRuns("§A§Y자주포§! 250대 필요§!"));
        var runs=DecisionText.sourceRuns("§A부족\n§Y장비§! 100.00대§! 기본");
        assertTrue(runs.contains(new DecisionText.Run(" 100.00대",0xCD3737)));
        assertEquals(new DecisionText.Run(" 기본",null),runs.getLast());
    }
    @Test void flagsUseUntintedGlyphsWithoutChangingAdjacentStylesAndMissingTokensRemainVisible() {
        var text=new DecisionText();
        text.flags(com.google.gson.JsonParser.parseString("{\"version\":1,\"flags\":{\"KOR\":\"\\ue000\",\"JAP\":\"\\ue001\",\"PRC\":\"\\ue002\"}}").getAsJsonObject());
        assertEquals("\ue000 KF-XX: KFXX 전투기 계획",text.component("[KOR.GetFlag]§C KF-XX§!: KFXX 전투기 계획").getString());
        var rows=text.rows("§YEADI§!: [JAP.GetFlag]§Y일본§! 초대\n\n[ZZX.GetFlag]\n[kor.GetFlag]");
        assertEquals(List.of("EADI: \ue001일본 초대","","[ZZX.GetFlag]","[kor.GetFlag]"),rows.stream().map(net.minecraft.network.chat.Component::getString).toList());
        var parts=new ArrayList<String>();
        rows.getFirst().visit((style,part)->{
            if(part.equals("\ue001")) {
                assertEquals(new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.parse("hoi:decision_flags")),style.getFont());
                assertEquals(0xFFFFFF,style.getColor().getValue());assertEquals(0,style.getShadowColor());
            }
            if(part.equals("일본"))assertEquals(0xFFBD00,style.getColor().getValue());
            parts.add(part);return Optional.empty();
        },net.minecraft.network.chat.Style.EMPTY);
        assertTrue(parts.contains("일본"));
        text.flags(new com.google.gson.JsonObject());
        assertEquals("[KOR.GetFlag]대한민국",text.component("[KOR.GetFlag]대한민국").getString());
    }
    private static String plain(String source) {
        return DecisionText.sourceRuns(source).stream().map(DecisionText.Run::text).collect(Collectors.joining());
    }
    @Test void tfrColorsRemainCaseSensitiveInsteadOfBecomingMinecraftFormatting() {
        var runs=DecisionText.sourceRuns("§Llight§lyellow§ppink§Pblue§bblack§Bblue2§Ared§agray§0purple§!default");
        assertEquals(List.of(new DecisionText.Run("light",0xA2B9CF),new DecisionText.Run("yellow",0xFFF47D),
                new DecisionText.Run("pink",0xFB607F),new DecisionText.Run("blue",0x3659D9),
                new DecisionText.Run("black",0),new DecisionText.Run("blue2",0x3333FF),
                new DecisionText.Run("red",0xCD3737),new DecisionText.Run("gray",0x414141),
                new DecisionText.Run("purple",0xD700F5),new DecisionText.Run("default",null)),runs);
    }
    @Test void separatorsBecomeOneEmptyLineWithoutLosingTheirResetOrFollowingStyle() {
        String source="§L설명\n§W------------§!\n기본\n§p제목\n---\n계속\n\n";
        assertEquals("설명\n\n기본\n제목\n\n계속\n\n",plain(source));
        var runs=DecisionText.sourceRuns(source);
        assertEquals(new DecisionText.Run("기본",null),runs.stream().filter(r->r.text().equals("기본")).findFirst().orElseThrow());
        assertEquals(new DecisionText.Run("계속",0xFB607F),runs.stream().filter(r->r.text().equals("계속")).findFirst().orElseThrow());
        assertEquals("---- 원내 정당 목록 ---- ",plain("---- §Y원내 정당 목록§! ---- "));
    }
    @Test void bannerPaddingInlineIconsFramesAndSourcePrecisionRemainInOriginalOrder() {
        String banner="\n".repeat(14)+"£GFX_JAP_ldp_conservatives_banner"+"\n".repeat(10);
        assertEquals(banner,plain(banner));
        assertEquals("£GFX_ldp_conservatives_logo 보수파\n\n£resources_strip|2 0.000\n£pol_power +0.10",
                plain("£GFX_ldp_conservatives_logo §4보수파§!\n\n£resources_strip|2 0.000\n£pol_power §G+0.10§!"));
        assertEquals("\n\n£GFX_na_kyung_won_Portrait\n         나경원\n",
                plain("\r\n\r\n£GFX_na_kyung_won_Portrait\r\n§W         나경원§!\r\n"));
    }
    @Test void explicitHexAndUnknownCodesDoNotBecomeGuessedColorsOrDisappear() {
        var runs=DecisionText.sourceRuns("§#123AbC색§! §q미확정§r끝§");
        assertEquals(new DecisionText.Run("색",0x123ABC),runs.getFirst());
        assertEquals("색 §q미확정§r끝§",plain("§#123AbC색§! §q미확정§r끝§"));
        assertEquals("§q---",plain("§q---"));
        assertEquals("앞\n\n뒤",plain("앞\n§!\n뒤"));
        assertEquals("",plain(""));
        assertEquals("\n\n",plain("\n\n"));
    }
}
