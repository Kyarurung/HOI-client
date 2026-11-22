package dev.hoi.protocol.commander;

import dev.hoi.protocol.commander.CommanderBattlePayloads.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderBattlePayloadsTest {
    private static final String TOKEN=UUID.randomUUID().toString();
    private static final Icon ICON=new Icon("commander/battle/side_modifier/frame_1","적의 공중 우세");
    private static Marker marker(String id,double heading) {return new Marker(id,Kind.LAND,10.5,64,-3.25,heading,0,"93",Tone.WINNING,false,0,"공격 측: 2개 사단");}
    private static Division division(String id) {return new Division(id,"제2보병사단","PRK","unit/icon/infantry",false,.8,.6,"151","169","151",List.of(ICON),Map.of("row","툴팁"));}
    private static Side side(List<Division> committed) {
        return new Side("KOR","대한민국","정쉬안","commander/tfr/a/army","지휘관","2","기술: 2",List.of(ICON),true,"",new Tactic("tactic/picture/tactic_shock","충격",false),"104",List.of(ICON),new Air("0","격추당한 총수: 1","툴팁"),committed.size()+" 사단","인력",committed,"0 예비대",List.of());
    }
    private static Land land(List<Division> committed,int heading) {
        return new Land(TOKEN,"battle_1","공격","map_detail/terrain/plains","평지",.93,heading,"90",side(committed),side(List.of()),"현재 전투 양상: 기본",List.of(new TacticRow("tactic_shock","충격","tactic/picture/tactic_shock",true,true,"",List.of("+25%","0%","0%","0%"),"툴팁")),Map.of("CLOSE","닫기"),"");
    }
    private static Fleet fleet(String lane) {
        return new Fleet("KOR","대한민국","지휘관 없음","commander/battle/leader_unknown","",List.of(),List.of(),Map.of("positioning","79%"),Map.of("positioning","위치 선정"),"100%","0%",Map.of(lane,List.of(new Ships("commander/commands/navalcombat_ship_icon_destroyer/frame_0","KOR",4,1,.5,false,"구축함"))));
    }
    private static Naval naval(String lane) {return new Naval(TOKEN,"sea_1","타이베이 전투","map_detail/weather_rain","천해",.4,true,true,"철수",fleet(lane),fleet(lane),Map.of("CLOSE","닫기"),"");}
    private static Result result(int count) {
        var side=new ResultSide("KOR","대한민국","선진룽","commander/tfr/b/navy","제독",List.of(ICON),List.of(new Survivor("commander/commands/navalcombat_ship_icon_submarine/frame_0",count,List.of("KOR","JAP"),"생존:")),List.of(new Lost("commander/commands/navalcombat_ship_icon_convoy/frame_0","수송선",List.of(new Loss("JAP",14,"수송선")))),"세부");
        return new Result(TOKEN,"sea_2","타이베이 전투","승리",side,side,Map.of("DETAILS","세부 사항"),"");
    }
    private static RegistryFriendlyByteBuf buffer() {return new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);}
    @Test void requestsKeepEveryFieldAndRejectMalformedInput() {
        for(var action:Action.values())for(var kind:Kind.values()) {
            var request=new Request(TOKEN,action,kind,"hoi:battle/1");var buffer=buffer();
            try {Request.CODEC.encode(buffer,request);assertEquals(request,Request.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        }
        assertThrows(IllegalArgumentException.class,()->new Request("no-token",Action.OPEN,Kind.LAND,"battle"));
        assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.OPEN,Kind.LAND,"bad id"));
        assertThrows(NullPointerException.class,()->new Request(TOKEN,null,Kind.LAND,"battle"));
    }
    @Test void markersRoundTripAndStayBounded() {
        var markers=new Markers("hoi:atlas_army",List.of(marker("a",1),new Marker("a",Kind.NAVAL,1,2,3,0,0,"",Tone.LOSING,true,2,"")));var buffer=buffer();
        try {Markers.CODEC.encode(buffer,markers);assertEquals(markers,Markers.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        assertThrows(IllegalArgumentException.class,()->new Markers("hoi:atlas_army",List.of(marker("a",1),marker("a",0))));
        assertThrows(IllegalArgumentException.class,()->new Markers("Bad Dimension",List.of()));
        assertThrows(IllegalArgumentException.class,()->marker("a",2));
        assertThrows(IllegalArgumentException.class,()->marker("a",Double.NaN));
        assertThrows(IllegalArgumentException.class,()->new Marker("a",Kind.LAND,Double.POSITIVE_INFINITY,0,0,0,0,"",Tone.BALANCED,false,0,""));
        assertThrows(IllegalArgumentException.class,()->new Marker("a",Kind.LAND,0,0,0,0,0,"",Tone.BALANCED,false,16,""));
        var many=new ArrayList<Marker>();for(int i=0;i<=CommanderBattlePayloads.MARKERS;i++)many.add(marker("m"+i,0));
        assertThrows(IllegalArgumentException.class,()->new Markers("hoi:atlas_army",many));
        var oversized=buffer();
        try {oversized.writeUtf("hoi:atlas_army",128);oversized.writeVarInt(CommanderBattlePayloads.MARKERS+1);assertThrows(IllegalArgumentException.class,()->Markers.CODEC.decode(oversized));}finally{oversized.release();}
    }
    @Test void everyWindowRoundTripsThroughItsSnapshot() {
        var land=land(List.of(division("a"),division("b")),3);assertEquals(land,Snapshot.of(land).land());assertEquals("닫기",land.label("CLOSE"));assertEquals("툴팁",land.attacker().committed().getFirst().tooltip("row"));
        var naval=naval("screen");assertEquals(naval,Snapshot.of(naval).naval());assertEquals(4,naval.attacker().lane("screen").getFirst().count());assertTrue(naval.attacker().lane("sunk").isEmpty());
        var result=result(40);assertEquals(result,Snapshot.of(result).result());
        for(var snapshot:List.of(Snapshot.of(land),Snapshot.of(naval),Snapshot.of(result))) {
            var buffer=buffer();
            try {Snapshot.CODEC.encode(buffer,snapshot);assertEquals(snapshot,Snapshot.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        }
    }
    @Test void malformedWindowsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->land(List.of(),5));
        var many=new ArrayList<Division>();for(int i=0;i<=CommanderBattlePayloads.ROWS;i++)many.add(division("d"+i));
        assertThrows(IllegalArgumentException.class,()->land(many,2));
        assertThrows(IllegalArgumentException.class,()->naval("somewhere"));
        assertThrows(IllegalArgumentException.class,()->result(-1));
        assertThrows(IllegalArgumentException.class,()->new Land(TOKEN,"battle_1","","",  "",.5,2,"",null,null,"",List.of(),Map.of(),""));
        assertEquals("끝",new Land(TOKEN,"battle_1","","","",.5,2,"",null,null,"",List.of(),Map.of(),"끝").error());
        assertThrows(IllegalArgumentException.class,()->new Icon("Bad Icon",""));
        assertThrows(IllegalArgumentException.class,()->new Snapshot(Kind.LAND,"x".repeat(CommanderBattlePayloads.BYTES+1)));
        assertThrows(RuntimeException.class,()->new Snapshot(Kind.LAND,"{}").land());
    }
}
