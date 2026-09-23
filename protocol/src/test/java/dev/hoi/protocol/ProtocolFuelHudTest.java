package dev.hoi.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolFuelHudTest {
    @Test void signedFuelTrendKeepsOldSnapshotsCompatible() {
        var gson=new Gson();
        var legacy=gson.fromJson("{\"fuel\":500}",CountryHud.NationalIndicators.class);
        assertNull(legacy.fuelDailyChange());assertNull(legacy.fuelDays());
        var declining=gson.fromJson("{\"fuel\":500,\"fuelRatio\":0.25,\"fuelDailyChange\":-100,\"fuelDays\":5}",CountryHud.NationalIndicators.class);
        assertEquals(-100,declining.fuelDailyChange());assertEquals(5,declining.fuelDays());
        assertEquals(declining,gson.fromJson(gson.toJson(declining),CountryHud.NationalIndicators.class));
        assertThrows(IllegalArgumentException.class,()->new CountryHud.NationalIndicators(null,null,null,null,null,null,null,null,null,null,null,null,null,null,0.0,-1.0));
    }
    @Test void fuelGaugeRemainsOptionalAndValidatesItsIndependentCapacityRatio() {
        var gson=new Gson();
        var legacy=gson.fromJson("{\"fuel\":500}",CountryHud.NationalIndicators.class);
        assertEquals(500,legacy.fuel());assertNull(legacy.fuelRatio());
        var value=gson.fromJson("{\"fuel\":500,\"fuelRatio\":0.25}",CountryHud.NationalIndicators.class);
        assertEquals(.25,value.fuelRatio());
        assertEquals(value,gson.fromJson(gson.toJson(value),CountryHud.NationalIndicators.class));
        for(double ratio:new double[]{-0.01,1.01,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->new CountryHud.NationalIndicators(null,null,null,null,null,null,null,null,null,null,null,null,null,ratio));
    }
}
