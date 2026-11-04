package dev.hoi.protocol.commander;

import dev.hoi.protocol.dialog.DialogView;
import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderTraitTreeTest {
    @Test void optionalBoundedTraitNodesPreserveLegacyTilesAndExistingModeOrdinals() {
        var old=new DialogView.Tile("one","지휘관","","","");var gson=new Gson();var legacy=gson.toJsonTree(old).getAsJsonObject();legacy.remove("traitNode");assertEquals(old,gson.fromJson(legacy,DialogView.Tile.class));
        var node=new DialogView.TraitNode(3,1,true,List.of("winter_specialist"),List.of(),false,true,false,"assignable_terrain_trait");var tile=new DialogView.Tile("winter_expert","동계전 전문가","15","commander/traits/tfrkp/winter_expert","조건\n\n원문",null,"",null,node);assertEquals(tile,gson.fromJson(gson.toJson(tile),DialogView.Tile.class));assertThrows(UnsupportedOperationException.class,()->node.parents().clear());
        assertThrows(IllegalArgumentException.class,()->new DialogView.TraitNode(101,0,false,List.of(),List.of(),false,true,true));assertThrows(IllegalArgumentException.class,()->new DialogView.TraitNode(0,0,false,List.of(),List.of(),true,true,true));assertThrows(IllegalArgumentException.class,()->new DialogView.TraitNode(0,0,false,List.of(),List.of(),false,false,true));
        assertEquals(25,CommanderHudPayloads.Mode.NAVY_HEADQUARTERS.ordinal());assertEquals(26,CommanderHudPayloads.Mode.COMMANDER_TRAITS.ordinal());assertEquals(27,CommanderHudPayloads.Mode.COMMANDER_TRAIT_CONFIRM.ordinal());
    }
}
