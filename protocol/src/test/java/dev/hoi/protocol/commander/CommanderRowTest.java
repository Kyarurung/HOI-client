package dev.hoi.protocol.commander;

import dev.hoi.protocol.dialog.DialogView;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderRowTest {
    @Test void sourceRowsRoundTripWithoutInferringSkillsAndLegacyTilesRemainCompatible() {
        var row=new DialogView.Leader("제독",3,List.of(3,3,2,2),10,true,List.of("명사수"));
        var tile=new DialogView.Tile("hoi:leader","강동길","제독 · 3","commander/tfrkp/kor_kang_dong_gil_char/navy","원본 설명",null,"",row);
        var gson=new Gson();assertEquals(tile,gson.fromJson(gson.toJson(tile),DialogView.Tile.class));
        var legacy=gson.toJsonTree(new DialogView.Tile("hoi:legacy","구 저장","1","",""));legacy.getAsJsonObject().remove("leader");
        assertNull(gson.fromJson(legacy,DialogView.Tile.class).leader());
        assertThrows(IllegalArgumentException.class,()->new DialogView.Leader("제독",1,List.of(1,1,1),10,false,List.of()));
        assertThrows(IllegalArgumentException.class,()->new DialogView.Leader("제독",1,List.of(1,1,1,11),10,false,List.of()));
        assertThrows(IllegalArgumentException.class,()->new DialogView.Leader("제독",1,List.of(1,1,1,1),-1,false,List.of()));
    }
}
