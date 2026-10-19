package dev.hoi.protocol;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolDialogView.LeaderTrait;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolLeaderTraitTest {
    @Test void traitActivityPreservesFalseAndAcceptsLegacyMissingState(){
        var gson=new Gson();
        for(Boolean active:new Boolean[]{null,false,true}){
            var trait=new LeaderTrait("commander/traits/tfrkp/defensive_doctrine","방어적 교리","원문\n\n§R비활성§!",active);
            assertEquals(trait,gson.fromJson(gson.toJson(trait),LeaderTrait.class));
        }
        assertNull(new LeaderTrait("commander/traits/tfrkp/defensive_doctrine","방어적 교리","").active());
        assertNull(gson.fromJson("{\"icon\":\"commander/traits/tfrkp/defensive_doctrine\",\"name\":\"legacy\",\"tooltip\":\"\"}",LeaderTrait.class).active());
    }
}
