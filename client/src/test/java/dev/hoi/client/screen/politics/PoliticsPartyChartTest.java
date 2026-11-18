package dev.hoi.client.screen.politics;

import dev.hoi.protocol.menu.MenuView;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PoliticsPartyChartTest {
    @Test void hitTestingFollowsCounterclockwiseSlicesFromThreeOclockAndExcludesEmptyAreas() {
        var zero=new MenuView.Entry("없음","0%","",0);
        var first=new MenuView.Entry("첫 정당","25%","",.25);
        var second=new MenuView.Entry("둘째 정당","25%","",.25);
        var parties=List.of(zero,first,second);
        assertEquals(first,PoliticsPartyChart.at(parties,5,-5,10));
        assertEquals(second,PoliticsPartyChart.at(parties,-5,-5,10));
        assertNull(PoliticsPartyChart.at(parties,5,5,10));
        assertNull(PoliticsPartyChart.at(parties,-5,5,10));
        assertNull(PoliticsPartyChart.at(parties,10,0,10));
        assertNull(PoliticsPartyChart.at(parties,9,9,10));
        assertNull(PoliticsPartyChart.at(List.of(zero),1,-1,10));
        assertEquals(0,PoliticsPartyChart.angle(1,0),1e-9);
        assertEquals(.25,PoliticsPartyChart.angle(0,-1),1e-9);
        assertEquals(.5,PoliticsPartyChart.angle(-1,0),1e-9);
        assertEquals(.75,PoliticsPartyChart.angle(0,1),1e-9);
    }
}
