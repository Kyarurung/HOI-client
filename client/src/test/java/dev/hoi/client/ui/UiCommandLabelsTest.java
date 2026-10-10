package dev.hoi.client.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class UiCommandLabelsTest {
    @Test void removesShortcutSectionAndOnlyItsAdjacentGap() {
        assertEquals("제목\n\n설명\n\n효과",UiCommandLabels.withoutShortcuts("제목\n\n설명\n\n효과\n\n§H단축키: §!Z"));
        assertEquals("설명\n효과\n\n다음 문단",UiCommandLabels.withoutShortcuts("설명\n\n키 설정: X\n\n효과\n\n다음 문단"));
        assertEquals("원문\n\n",UiCommandLabels.withoutShortcuts("원문\n\n"));
    }
    @Test void preservesMouseModifiersAndRemovesKeyOnlyAlternatives() {
        assertEquals("§G클릭§!\n현재 임무 중단\n\n§GCtrl + 클릭§!\n현재 위치에서 대기",UiCommandLabels.withoutShortcuts("§G클릭§! 또는 £h \n현재 임무 중단\n\n§GCtrl + 클릭§! 또는 £ctrl  + £h \n현재 위치에서 대기"));
        assertEquals("이 버튼을 눌러서 모드를 전환하십시오.\n§G우클릭§!하여 편집",UiCommandLabels.withoutShortcuts("이 버튼을 눌러서 모드를 전환하거나 §GAlt§!를 길게 누르십시오.\n§G우클릭§!하여 편집"));
        assertEquals("효과\n\nShift + 클릭하여 선택",UiCommandLabels.withoutShortcuts("효과\n\nShift + 클릭하여 선택"));
    }
}
