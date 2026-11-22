package dev.hoi.protocol.commander;

import dev.hoi.protocol.commander.CommanderRecordPayloads.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderRecordPayloadsTest {
    private static final String TOKEN=UUID.randomUUID().toString();
    private static final Party KOR=new Party("KOR","참전국:\n대한민국","commander/tfrkp/kor_ahn_byung_seok_char/army","원수 안병석","인력: 1.25K","기갑: 3","항공: 0","기타: 173");
    private static final Loss LOSS=new Loss("보병 장비","country/kor/infantry","120","적이 노획: 4","12","훈련 중 손실: 12","30","복구");
    private static Battle battle(String id) {return new Battle(id,"hoi:tfr/province/7171","순천","전투 위치","3일 전.","종료일","commander/log/atk_def/frame_0","공격","map_detail/terrain/hills","언덕",KOR,KOR);}
    private static Log log(List<Battle> battles,Map<String,String> labels,Set<String> excluded,int months) {
        return new Log(TOKEN,"theater/1",months,36,excluded,"기간","인력","툴팁","승률",0,battles.size(),battles,LOSS,List.of(LOSS),List.of(LOSS),LOSS,List.of(),List.of(),List.of(new Template("hoi:division/kor/1","기계화","unit_hud/tank","67%","승리","3")),labels,"");
    }
    private static Losses losses(List<Section> battles,List<Section> periods) {
        return new Losses(TOKEN,"battle/b/1","KOR",2,16,"","",List.of(new Choice("PRK","북한")),List.of(),battles,periods,List.of(new Ship("함선","production/source/a","KOR","PRK","침몰")),Map.of("CLOSE","닫기"),"");
    }
    private static Section section(String id) {
        return new Section(id,"홍해 전투",1,15,List.of(new Hull("305b5f0","잠수함","production/source/a",1,List.of(new Kill("PRK","3"),new Kill("","12")),"툴팁")),List.of(new Accident("항공모함","production/source/b","0","1","4.5","30.0","사고")));
    }
    @Test void requestsKeepEveryFieldAndRejectMalformedInput(){
        for(var action:Action.values()){
            var request=new Request(TOKEN,action,"theater/1",36,24,List.of("army_1","KOR",""));var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
            try{Request.CODEC.encode(buffer,request);assertEquals(request,Request.CODEC.decode(buffer));assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        }
        for(String target:List.of("bad id","../x y","한글"))assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.LOG,target,0,0,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Request("not-a-token",Action.LOG,"theater/1",0,0,List.of()));
        assertThrows(NullPointerException.class,()->new Request(TOKEN,null,"theater/1",0,0,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.LOG,"theater/1",CommanderRecordPayloads.MONTHS+1,0,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.LOG,"theater/1",0,-1,List.of()));
        assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.LOG,"theater/1",0,0,Collections.nCopies(CommanderRecordPayloads.FILTERS+1,"army")));
        assertThrows(IllegalArgumentException.class,()->new Request(TOKEN,Action.LOG,"theater/1",0,0,List.of("bad id")));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            buffer.writeUtf(TOKEN,36);buffer.writeEnum(Action.LOG);buffer.writeUtf("theater/1",160);buffer.writeVarInt(0);buffer.writeVarInt(0);buffer.writeVarInt(CommanderRecordPayloads.FILTERS+1);
            assertThrows(IllegalArgumentException.class,()->Request.CODEC.decode(buffer));
        }finally{buffer.release();}
    }
    @Test void combatLogsRoundTripAndStayBounded(){
        var log=log(List.of(battle("battle_1")),Map.of("COMBAT_LOG","전투 기록"),Set.of("army_1"),12);
        assertEquals(log,LogSnapshot.of(log).log());assertEquals("전투 기록",log.label("COMBAT_LOG"));assertEquals("",log.label("MISSING"));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{LogSnapshot.CODEC.encode(buffer,LogSnapshot.of(log));assertEquals(log,LogSnapshot.CODEC.decode(buffer).log());assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        var many=new ArrayList<Battle>();for(int i=0;i<=CommanderRecordPayloads.ROWS;i++)many.add(battle("battle_"+i));
        assertThrows(IllegalArgumentException.class,()->log(many,Map.of(),Set.of(),12));
        assertThrows(IllegalArgumentException.class,()->log(List.of(),Map.of(),Set.of(),CommanderRecordPayloads.MONTHS+1));
        assertThrows(IllegalArgumentException.class,()->log(List.of(),Map.of(),Set.of("bad id"),12));
        var labels=new HashMap<String,String>();for(int i=0;i<49;i++)labels.put("KEY_"+i,"");
        assertThrows(IllegalArgumentException.class,()->log(List.of(),labels,Set.of(),12));
        assertThrows(IllegalArgumentException.class,()->new Loss("이름","Bad Icon","0","","","","",""));
        assertThrows(IllegalArgumentException.class,()->new LogSnapshot("x".repeat(CommanderRecordPayloads.BYTES+1)));
        assertThrows(IllegalArgumentException.class,()->new LogSnapshot("가".repeat(CommanderRecordPayloads.BYTES/2)));
        assertThrows(RuntimeException.class,()->new LogSnapshot("{}").log());
    }
    @Test void navalLossesRoundTripAndStayBounded(){
        var losses=losses(List.of(section("battle/b")),List.of(section("period/OLDER")));
        assertEquals(losses,LossesSnapshot.of(losses).losses());assertEquals("닫기",losses.label("CLOSE"));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{LossesSnapshot.CODEC.encode(buffer,LossesSnapshot.of(losses));assertEquals(losses,LossesSnapshot.CODEC.decode(buffer).losses());assertEquals(0,buffer.readableBytes());}finally{buffer.release();}
        assertThrows(IllegalArgumentException.class,()->losses(List.of(),Collections.nCopies(9,section("period/OLDER"))));
        assertThrows(IllegalArgumentException.class,()->losses(Collections.nCopies(129,section("battle/b")),List.of()));
        assertThrows(IllegalArgumentException.class,()->new Hull("a","이름","",-1,List.of(),""));
        assertThrows(IllegalArgumentException.class,()->new Hull("a","이름","",0,Collections.nCopies(4,new Kill("PRK","1")),""));
        assertThrows(IllegalArgumentException.class,()->new Section("bad id","이름",0,0,List.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new LossesSnapshot("x".repeat(CommanderRecordPayloads.BYTES+1)));
    }
}
