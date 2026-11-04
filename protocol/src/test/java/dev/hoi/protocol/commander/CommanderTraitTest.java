package dev.hoi.protocol.commander;

import dev.hoi.protocol.dialog.DialogView;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderTraitTest {
    @Test void sourceTraitsRoundTripPreserveColorAndBlankLinesAndLegacyRows() {
        var gson=new Gson();var trait=new DialogView.LeaderTrait("commander/traits/tfrkp/career_officer","직업장교","§e직업장교§r\n\n§7원본 설명§r");
        var row=new DialogView.Leader("장군",3,List.of(2,3,4,1),24,false,List.of("직업장교"),List.of(),List.of(trait));
        assertEquals(row,gson.fromJson(gson.toJson(row),DialogView.Leader.class));
        var json=gson.toJsonTree(row).getAsJsonObject();json.remove("traitDetails");assertTrue(gson.fromJson(json,DialogView.Leader.class).traitDetails().isEmpty());
        assertTrue(new DialogView.Leader("장군",3,List.of(2,3,4,1),24,false,List.of("직업장교")).traitDetails().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new DialogView.LeaderTrait("../foreign","이름","설명"));
        assertThrows(IllegalArgumentException.class,()->new DialogView.LeaderTrait("","이름","x".repeat(3001)));
        assertThrows(IllegalArgumentException.class,()->new DialogView.Leader("장군",3,List.of(2,3,4,1),24,false,List.of(),List.of(),Collections.nCopies(129,trait)));
    }
}
