package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCommanderTraitTreeTest {
    @Test void optionalBoundedTraitNodesPreserveLegacyTilesAndExistingModeOrdinals() {
        var old=new ProtocolDialogView.Tile("one","지휘관","","","");var gson=new Gson();var legacy=gson.toJsonTree(old).getAsJsonObject();legacy.remove("traitNode");assertEquals(old,gson.fromJson(legacy,ProtocolDialogView.Tile.class));
        var node=new ProtocolDialogView.TraitNode(3,1,true,List.of("winter_specialist"),List.of(),false,true,false,"assignable_terrain_trait");var tile=new ProtocolDialogView.Tile("winter_expert","동계전 전문가","15","commander/traits/tfrkp/winter_expert","조건\n\n원문",null,"",null,node);assertEquals(tile,gson.fromJson(gson.toJson(tile),ProtocolDialogView.Tile.class));assertThrows(UnsupportedOperationException.class,()->node.parents().clear());
        assertThrows(IllegalArgumentException.class,()->new ProtocolDialogView.TraitNode(101,0,false,List.of(),List.of(),false,true,true));assertThrows(IllegalArgumentException.class,()->new ProtocolDialogView.TraitNode(0,0,false,List.of(),List.of(),true,true,true));assertThrows(IllegalArgumentException.class,()->new ProtocolDialogView.TraitNode(0,0,false,List.of(),List.of(),false,false,true));
        assertEquals(25,ProtocolCommanderHud.Mode.NAVY_HEADQUARTERS.ordinal());assertEquals(26,ProtocolCommanderHud.Mode.COMMANDER_TRAITS.ordinal());assertEquals(27,ProtocolCommanderHud.Mode.COMMANDER_TRAIT_CONFIRM.ordinal());
    }
}
