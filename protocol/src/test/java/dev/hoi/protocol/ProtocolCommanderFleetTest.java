package dev.hoi.protocol;

import com.google.gson.Gson;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import org.junit.jupiter.api.Test;
import static dev.hoi.protocol.ProtocolCommanderHud.*;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCommanderFleetTest {
    private static FleetData fleet(Map<String,Double> strength) {
        return new FleetData(2,"활동 중인 지역: 2","파라셀 군도","hoi:tfr/province/1047","§H모항§!",137,false,"",Map.of("unit_hud/navy_destroyer","§Y구축함§! 유형의 함정 §Y8§!척"),strength,Map.of("unit_hud/navy_destroyer",.5),false,"자동 모항 선택");
    }
    @Test void fleetPanelAndTaskForceRowRoundTripAndLegacyJsonStaysValid() {
        var gson=new Gson();var data=fleet(Map.of("unit_hud/navy_destroyer",1.0));
        var naval=new NavalData(1,true,false,"MEDIUM",Map.of(),null,null,"£mapicon_port_strike_result_text  £flag_CHI §H파라셀 군도§!에 정박함","ship_activity_docked",false,"임무: §H대기§!",137,4,false,0.0,"£GFX_naval_dominance_texticon£ 이 §Y태스크 포스§!는 §G0§!의 해상 지배를 제공합니다");
        var panel=new Panel("","",null,List.of(new Member("force","TF","","","",1,new Open(Mode.NAVAL_MISSION,"force"),"",1.0,Map.of(),"","HOLD",new MemberData("","",15,4,naval,null))),1,List.of(),null,null,List.of(),null,Map.of("unit_hud/navy_destroyer",8),null,null,"",.5,1.0,"",null,null,4,null,null,null,null,List.of(),List.of(),data);
        var view=new View("CHI","hoi:atlas_navy_0",Mode.NAVY_HQ,List.of(new Card("fleet","제3함대","","","","",9,0,"",null,panel)));
        assertEquals(view,Snapshot.of(view).view());assertEquals(data,gson.fromJson(gson.toJson(data),FleetData.class));
        var legacy=gson.toJsonTree(new NavalData(null,null,null,"LOW",Map.of()));var restored=gson.fromJson(legacy,NavalData.class);assertEquals("",restored.activity());assertEquals(0,restored.fleetShips());assertNull(restored.fleetInsignia());assertNull(restored.dominance());assertEquals("",restored.dominanceTooltip());assertNull(restored.carrierStance());assertTrue(restored.hullStrength().isEmpty());
        var carrier=new NavalData(1,true,false,"MEDIUM",Map.of(),null,null,"","",false,"",3,null,false,null,"",Map.of("unit_hud/navy_carrier",.5),Map.of("unit_hud/navy_carrier",.25),4,Map.of("unit_hud/navy_carrier",.75));assertEquals(carrier,gson.fromJson(gson.toJson(carrier),NavalData.class));assertTrue(restored.hullAir().isEmpty());
        var air=new FleetData(0,"","","","",1,false,"",Map.of(),Map.of(),Map.of(),null,"",Map.of("unit_hud/navy_carrier",.5));assertEquals(air,gson.fromJson(gson.toJson(air),FleetData.class));assertTrue(data.hullAir().isEmpty());
        var oldPanel=gson.toJsonTree(new Panel("","",null,List.of(),0));oldPanel.getAsJsonObject().remove("fleet");assertNull(gson.fromJson(oldPanel,Panel.class).fleet());
    }
    @Test void fleetValuesAreBounded() {
        assertThrows(IllegalArgumentException.class,()->fleet(Map.of("unit_hud/navy_destroyer",1.5)));
        assertThrows(IllegalArgumentException.class,()->fleet(Map.of("Invalid Key",.5)));
        assertThrows(IllegalArgumentException.class,()->new FleetData(-1,"","","","",0,false,"",Map.of(),Map.of(),Map.of(),null,""));
        assertThrows(IllegalArgumentException.class,()->new FleetData(0,"","","bad province","",0,false,"",Map.of(),Map.of(),Map.of(),null,""));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","Bad Icon",false,"",0,null,false));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,81,false));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"가".repeat(601),"",false,"",0,null,false));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,-1.0,""));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,Double.NaN,""));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,1.0,"가".repeat(601)));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,null,"",Map.of(),Map.of(),5,null));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,null,"",Map.of("unit_hud/navy_carrier",1.5),Map.of(),null,null));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,null,"",Map.of(),Map.of(),null,Map.of("unit_hud/navy_carrier",1.5)));
        assertThrows(IllegalArgumentException.class,()->new FleetData(0,"","","","",0,false,"",Map.of(),Map.of(),Map.of(),null,"",Map.of("unit_hud/navy_carrier",2.0)));
        assertThrows(IllegalArgumentException.class,()->new NavalData(null,null,null,"LOW",Map.of(),null,null,"","",false,"",0,null,false,null,"",Map.of(),Map.of("Bad Key",.5),null,null));
    }
    @Test void seaHoverPayloadsRoundTripAndRejectInvalidValues() {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);var hover=new SeaHover(7,new Ray(0,-1,0));SeaHover.CODEC.encode(buffer,hover);assertEquals(hover,SeaHover.CODEC.decode(buffer));
        var tooltip=new SeaHoverTooltip(7,"hoi:tfr/region/37","지배율: §G0.00%§!");SeaHoverTooltip.CODEC.encode(buffer,tooltip);assertEquals(tooltip,SeaHoverTooltip.CODEC.decode(buffer));
        assertEquals(new SeaHoverTooltip(1,"",""),new SeaHoverTooltip(1,null,null));
        assertThrows(IllegalArgumentException.class,()->new SeaHover(-1,new Ray(0,-1,0)));assertThrows(IllegalArgumentException.class,()->new SeaHoverTooltip(0,"bad region",""));assertThrows(IllegalArgumentException.class,()->new SeaHoverTooltip(0,"","가".repeat(4001)));
    }
}
