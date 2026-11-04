package dev.hoi.protocol.commander;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommanderOrderPayloadsTest {
    private static RegistryFriendlyByteBuf buffer(){return new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);}
    @Test void issuedContextCommandAndSelectionRoundTripAtSelectionAndRayBounds() {
        String token=UUID.randomUUID().toString();var context=new CommanderOrderPayloads.Context(token,42,"KOR","hoi:atlas_air_kor","LAND/OFFENSIVE/OBJECTIVE");var contextBuffer=buffer();try{CommanderOrderPayloads.Context.CODEC.encode(contextBuffer,context);assertEquals(context,CommanderOrderPayloads.Context.CODEC.decode(contextBuffer));assertEquals(0,contextBuffer.readableBytes());}finally{contextBuffer.release();}
        var keys=java.util.stream.IntStream.range(0,2048).mapToObj(i->"unit/"+"x".repeat(150)+String.format(Locale.ROOT,"%04d",i)).toList();var rays=new ArrayList<CommanderHudPayloads.Ray>();for(int i=0;i<64;i++)rays.add(new CommanderHudPayloads.Ray(0,-1,0));var command=new CommanderOrderPayloads.Command(token,42,"MAP",keys,rays);rays.clear();assertEquals(64,command.rays().size());var commandBuffer=buffer();try{CommanderOrderPayloads.Command.CODEC.encode(commandBuffer,command);assertTrue(commandBuffer.readableBytes()>32767);assertTrue(commandBuffer.readableBytes()<400000);assertEquals(command,CommanderOrderPayloads.Command.CODEC.decode(commandBuffer));assertEquals(0,commandBuffer.readableBytes());}finally{commandBuffer.release();}
        var mutable=new ArrayList<>(keys);var selection=new CommanderOrderPayloads.Selection(token,91,mutable);mutable.clear();assertEquals(2048,selection.keys().size());var selectionBuffer=buffer();try{CommanderOrderPayloads.Selection.CODEC.encode(selectionBuffer,selection);assertEquals(selection,CommanderOrderPayloads.Selection.CODEC.decode(selectionBuffer));assertEquals(0,selectionBuffer.readableBytes());}finally{selectionBuffer.release();}
    }
    @Test void malformedIdentityActionsSequencesAndSelectionsAreRejected() {
        String token=UUID.randomUUID().toString();assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Context("invalid",0,"KOR","hoi:atlas_air_kor",""));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Context(token,-1,"KOR","hoi:atlas_air_kor",""));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Context(token,0,"kor","hoi:atlas_air_kor",""));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Context(token,0,"KOR","x".repeat(129),""));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Command(token,-1,"MAP",List.of(),List.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Command(token,0,"AIR/UNKNOWN:INJECT",List.of(),List.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Command(token,0,"MAP",List.of("unit/a","unit/a"),List.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Command(token,0,"MAP",List.of("unit/foreign value"),List.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Command(token,0,"MAP",List.of("x".repeat(161)),List.of()));
        var excessive=java.util.stream.IntStream.range(0,2049).mapToObj(i->"unit/"+i).toList();assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Selection(token,0,excessive));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Selection(token,-1,List.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Command(token,0,"MAP",List.of(),Collections.nCopies(65,new CommanderHudPayloads.Ray(0,-1,0))));
    }
    @Test void invalidWireCountsAndRaysFailBeforeBuildingUnboundedRequests() {
        String token=UUID.randomUUID().toString();
        for(int count:List.of(-1,2049)){var b=buffer();try{b.writeUtf(token,36);b.writeVarLong(0);b.writeVarInt(count);assertThrows(IllegalArgumentException.class,()->CommanderOrderPayloads.Selection.CODEC.decode(b));}finally{b.release();}}
        for(int count:List.of(-1,65)){var b=buffer();try{b.writeUtf(token,36);b.writeVarLong(0);b.writeUtf("MAP",80);b.writeVarInt(0);b.writeVarInt(count);assertThrows(IllegalArgumentException.class,()->CommanderOrderPayloads.Command.CODEC.decode(b));}finally{b.release();}}
        var b=buffer();try{b.writeUtf(token,36);b.writeVarLong(0);b.writeUtf("MAP",80);b.writeVarInt(0);b.writeVarInt(1);b.writeDouble(Double.NaN);b.writeDouble(-1);b.writeDouble(0);assertThrows(IllegalArgumentException.class,()->CommanderOrderPayloads.Command.CODEC.decode(b));}finally{b.release();}
    }
    @Test void selectedDivisionPagesRoundTripAndRejectMalformedBounds(){
        var member=new CommanderHudPayloads.Member("unit","Name","Idle","unit_hud/infantry","Actual state",.5,new CommanderHudPayloads.Open(CommanderHudPayloads.Mode.ARMY_UNITS,"army"));
        String token=UUID.randomUUID().toString();var page=new CommanderOrderPayloads.DetailPage(token,4,9,0,1,List.of(member));var packet=CommanderOrderPayloads.Details.of(page);var b=buffer();
        try{CommanderOrderPayloads.Details.CODEC.encode(b,packet);assertEquals(page,CommanderOrderPayloads.Details.CODEC.decode(b).page());assertEquals(0,b.readableBytes());}finally{b.release();}
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.DetailPage(token,-1,0,0,1,List.of(member)));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.DetailPage(token,0,0,1,2,List.of(member)));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.DetailPage(token,0,0,0,2,List.of(member,member)));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.DetailPage(token,0,0,0,2049,List.of(member)));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Details("x".repeat(1000001)));
    }

    @Test void optionalAvailabilityRoundTripPreservesLegacyContextBytes() {
        String token=UUID.randomUUID().toString();var query=new CommanderOrderPayloads.Check(token,7,11,13,java.util.stream.IntStream.range(0,160).mapToObj(i->"army/"+i).toList());var b=buffer();
        try{CommanderOrderPayloads.Check.CODEC.encode(b,query);assertEquals(query,CommanderOrderPayloads.Check.CODEC.decode(b));assertEquals(0,b.readableBytes());}finally{b.release();}
        var result=new CommanderOrderPayloads.Assessment(token,7,11,13,Map.of("LAND/FRONT",new CommanderOrderPayloads.Decision(true,true,""),"ARMY/UNASSIGN_UNITS",new CommanderOrderPayloads.Decision(false,false,"배정 해제할 대상이 없습니다.")));var data=buffer();
        try{CommanderOrderPayloads.Availability.CODEC.encode(data,CommanderOrderPayloads.Availability.of(result));assertEquals(result,CommanderOrderPayloads.Availability.CODEC.decode(data).assessment());assertEquals(0,data.readableBytes());}finally{data.release();}
        var legacy=buffer();try{legacy.writeUtf(token,36);legacy.writeVarLong(7);legacy.writeUtf("KOR",3);legacy.writeUtf("hoi:atlas_army_kor",128);legacy.writeUtf("",160);assertEquals(new CommanderOrderPayloads.Context(token,7,"KOR","hoi:atlas_army_kor",""),CommanderOrderPayloads.Context.CODEC.decode(legacy));assertEquals(0,legacy.readableBytes());}finally{legacy.release();}
    }
    @Test void availabilityRejectsContradictoryStatusOversizeAndMalformedScope() {
        String token=UUID.randomUUID().toString();assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Check(token,0,0,-1,List.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Check(token,0,0,1,java.util.stream.IntStream.range(0,161).mapToObj(i->"army/"+i).toList()));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Decision(true,false,"denied"));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Decision(false,true,"denied"));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Decision(false,false,""));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Decision(false,false,"가".repeat(401)));
        assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Assessment(token,0,-1,0,Map.of()));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Assessment(token,0,0,0,Map.of("invalid",new CommanderOrderPayloads.Decision(true,false,""))));var excessive=new HashMap<String,CommanderOrderPayloads.Decision>();for(int i=0;i<65;i++)excessive.put("A".repeat(i+1),new CommanderOrderPayloads.Decision(true,false,""));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Assessment(token,0,0,0,excessive));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Availability("가".repeat(88000)));assertDoesNotThrow(()->new CommanderOrderPayloads.Availability("가".repeat(22000)));assertThrows(IllegalArgumentException.class,()->new CommanderOrderPayloads.Decision(true,false,"","가".repeat(3001),true));assertEquals(Boolean.TRUE,new CommanderOrderPayloads.Decision(true,false,"","가".repeat(3000),true).warning());assertNull(new CommanderOrderPayloads.Decision(true,false,"","").warning());
    }
}
