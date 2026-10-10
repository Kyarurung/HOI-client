package dev.hoi.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolDiplomacyVolunteerViewTest {
    private static final String SESSION = "00000000-0000-0000-0000-000000000001";
    private static final String DETAIL = "§H원문 지휘관§!\n효과: §G+1.50%§!\n\n설명\n";
    @Test void exactSourceFormattingAndUnknownValuesSurviveThePrivateSnapshot() {
        var army = army("opaque-1", "hoi:textures/gui/command/army_shield.png", 11);
        var original = view(List.of(army), List.of(army.key()));
        var response = ProtocolDiplomacyVolunteerPayloads.Response.of(SESSION,
                ProtocolDiplomacyVolunteerPayloads.Status.OPEN, original);
        assertEquals(original, response.view());
        assertEquals(DETAIL, response.view().armies().getFirst().commanderTooltip());
        assertEquals("", response.view().threatText());
        assertEquals("", response.view().armies().getFirst().commanderPortrait());
        assertEquals("", response.view().armies().getFirst().commanderSkillText());
    }
    @Test void armyAndSelectionLimitsPreserveOrderAndRejectDuplicateOrUnissuedKeys() {
        var armies = IntStream.range(0, ProtocolDiplomacyVolunteerView.MAX_ARMIES)
                .mapToObj(i -> army("choice-" + i, "command/army_shield", 0)).toList();
        var keys = armies.reversed().stream().map(ProtocolDiplomacyVolunteerView.ArmyRow::key).toList();
        assertEquals(keys, view(armies, keys).selectedArmyKeys());
        var excessive = new ArrayList<>(armies);
        excessive.add(army("extra", "", 0));
        assertThrows(IllegalArgumentException.class, () -> view(excessive, List.of()));
        assertThrows(IllegalArgumentException.class, () -> view(List.of(armies.getFirst(), armies.getFirst()), List.of()));
        assertThrows(IllegalArgumentException.class, () -> view(armies, List.of("choice-0", "choice-0")));
        assertThrows(IllegalArgumentException.class, () -> view(armies, List.of("not-issued")));
        assertThrows(IllegalArgumentException.class, () -> view(armies, IntStream.range(0, 129).mapToObj(i -> "choice-" + i).toList()));
    }
    @Test void snapshotCollectionsCannotChangeAfterPublication() {
        var rows = new ArrayList<>(List.of(army("choice", "", 0)));
        var selected = new ArrayList<>(List.of("choice"));
        var original = view(rows, selected);
        rows.clear();
        selected.clear();
        assertEquals(1, original.armies().size());
        assertEquals(List.of("choice"), original.selectedArmyKeys());
        assertThrows(UnsupportedOperationException.class, () -> original.armies().clear());
        assertThrows(UnsupportedOperationException.class, () -> original.selectedArmyKeys().clear());
        assertThrows(UnsupportedOperationException.class, () -> original.armies().getFirst().unitCounts().clear());
    }
    @Test void unsupportedTexturesAndUnboundedSourceFieldsFailInsteadOfBeingRewritten() {
        for (var texture : List.of("../outside", "hoi:textures/gui/../outside.png", "minecraft:textures/gui/icon.png",
                "hoi:textures/entity/icon.png", "hoi:textures/gui/icon", "C:/icon.png", "command\\icon", "a".repeat(161))) {
            assertThrows(IllegalArgumentException.class, () -> army("choice", texture, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> army("", "", 0));
        assertThrows(IllegalArgumentException.class, () -> army(" ", "", 0));
        assertThrows(IllegalArgumentException.class, () -> army("x".repeat(65), "", 0));
        assertThrows(IllegalArgumentException.class, () -> army("choice", "", 12));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerView.UnitCount("", "x".repeat(129)));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerView.ArmyRow("choice", "", "", "", "", "", "",
                "x".repeat(4001), List.of(), false, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerView(SESSION, -1, "KOR", "PRK", "",
                "", "", "", List.of(), List.of(), "", "", "", false, false, "", false, ""));
        assertThrows(IllegalArgumentException.class, () -> new ProtocolDiplomacyVolunteerView(SESSION, 0, "bad country", "PRK", "",
                "", "", "", List.of(), List.of(), "", "", "", false, false, "", false, ""));
    }
    private static ProtocolDiplomacyVolunteerView.ArmyRow army(String key, String icon, int unitCount) {
        return new ProtocolDiplomacyVolunteerView.ArmyRow(key, "제1군", icon, "4", "원문 지휘관", "", "", DETAIL,
                IntStream.range(0, unitCount).mapToObj(i -> new ProtocolDiplomacyVolunteerView.UnitCount("units/infantry", "1")).toList(),
                true, "");
    }
    private static ProtocolDiplomacyVolunteerView view(List<ProtocolDiplomacyVolunteerView.ArmyRow> armies, List<String> selected) {
        return new ProtocolDiplomacyVolunteerView(SESSION, 7, "KOR", "PRK", "의용군 파병", DETAIL, "§H7§!일",
                "2개 사단이 군에 배정되지 않음", armies, selected, "§H4§!개 사단을 보냅니다.", "원문 한도\n\n조건", "",
                false, false, "서버가 발급한 사용 불가 사유", true, "");
    }
}
