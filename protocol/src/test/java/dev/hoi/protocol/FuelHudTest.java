package dev.hoi.protocol;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FuelHudTest {
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
