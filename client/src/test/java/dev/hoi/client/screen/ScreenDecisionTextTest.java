package dev.hoi.client.screen;

import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenDecisionTextTest {
    private static String plain(String source) {
        return ScreenDecisionText.sourceRuns(source).stream().map(ScreenDecisionText.Run::text).collect(Collectors.joining());
    }
    @Test void tfrColorsRemainCaseSensitiveInsteadOfBecomingMinecraftFormatting() {
        var runs=ScreenDecisionText.sourceRuns("§Llight§lyellow§ppink§Pblue§bblack§Bblue2§Ared§agray§0purple§!default");
        assertEquals(List.of(new ScreenDecisionText.Run("light",0xA2B9CF),new ScreenDecisionText.Run("yellow",0xFFF47D),
                new ScreenDecisionText.Run("pink",0xFB607F),new ScreenDecisionText.Run("blue",0x3659D9),
                new ScreenDecisionText.Run("black",0),new ScreenDecisionText.Run("blue2",0x3333FF),
                new ScreenDecisionText.Run("red",0xCD3737),new ScreenDecisionText.Run("gray",0x414141),
                new ScreenDecisionText.Run("purple",0xD700F5),new ScreenDecisionText.Run("default",null)),runs);
    }
    @Test void separatorsBecomeOneEmptyLineWithoutLosingTheirResetOrFollowingStyle() {
        String source="§L설명\n§W------------§!\n기본\n§p제목\n---\n계속\n\n";
        assertEquals("설명\n\n기본\n제목\n\n계속\n\n",plain(source));
        var runs=ScreenDecisionText.sourceRuns(source);
        assertEquals(new ScreenDecisionText.Run("기본",null),runs.stream().filter(r->r.text().equals("기본")).findFirst().orElseThrow());
        assertEquals(new ScreenDecisionText.Run("계속",0xFB607F),runs.stream().filter(r->r.text().equals("계속")).findFirst().orElseThrow());
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
        var runs=ScreenDecisionText.sourceRuns("§#123AbC색§! §q미확정§r끝§");
        assertEquals(new ScreenDecisionText.Run("색",0x123ABC),runs.getFirst());
        assertEquals("색 §q미확정§r끝§",plain("§#123AbC색§! §q미확정§r끝§"));
        assertEquals("§q---",plain("§q---"));
        assertEquals("앞\n\n뒤",plain("앞\n§!\n뒤"));
        assertEquals("",plain(""));
        assertEquals("\n\n",plain("\n\n"));
    }
}
