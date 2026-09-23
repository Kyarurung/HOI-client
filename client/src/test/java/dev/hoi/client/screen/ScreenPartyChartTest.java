package dev.hoi.client.screen;

import dev.hoi.protocol.MenuView;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ScreenPartyChartTest {
    @Test void hitTestingFollowsClockwiseSlicesAndExcludesEmptyAreas() {
        var zero=new MenuView.Entry("없음","0%","",0);
        var first=new MenuView.Entry("첫 정당","25%","",.25);
        var second=new MenuView.Entry("둘째 정당","25%","",.25);
        var parties=List.of(zero,first,second);
        assertEquals(first,ScreenPartyChart.at(parties,5,-5,10));
        assertEquals(second,ScreenPartyChart.at(parties,5,5,10));
        assertNull(ScreenPartyChart.at(parties,-5,5,10));
        assertNull(ScreenPartyChart.at(parties,10,0,10));
        assertNull(ScreenPartyChart.at(parties,9,9,10));
        assertNull(ScreenPartyChart.at(List.of(zero),1,-1,10));
    }
}
