package dev.hoi.protocol.commander;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;

class CommanderAirBaseTest {
    @Test void optionalBaseRoundTripsAndLegacyAirStateRemainsUnknown(){var gson=new Gson();var data=new AirData("","","ANY",new AirBase("base-a",150,250,400,"KOR"));assertEquals(data,gson.fromJson(gson.toJson(data),AirData.class));assertNull(gson.fromJson("{\"airGroup\":\"\",\"airGroupName\":\"\",\"reinforcementPolicy\":\"ANY\"}",AirData.class).base());}
    @Test void rejectsInconsistentCountsAndNonFiniteCapacity(){assertThrows(IllegalArgumentException.class,()->new AirBase("base",2,1,400));assertThrows(IllegalArgumentException.class,()->new AirBase("base",-1,1,400));assertThrows(IllegalArgumentException.class,()->new AirBase("base",1,1,Double.NaN));assertThrows(IllegalArgumentException.class,()->new AirBase("base",1,1,-1));}
    @Test void optionalWingDetailsRoundTripAndRejectInvalidSchedules(){var gson=new Gson();var data=new AirData("","","ANY",null,new AirWingDetails(100,"NIGHT","production/equipment/fighter",false,"대기 중"));assertEquals(data,gson.fromJson(gson.toJson(data),AirData.class));assertNull(new AirData("","","ANY").wing());assertThrows(IllegalArgumentException.class,()->new AirWingDetails(0,"DAY","",false,""));assertThrows(IllegalArgumentException.class,()->new AirWingDetails(100,"INVALID","",false,""));}
}
