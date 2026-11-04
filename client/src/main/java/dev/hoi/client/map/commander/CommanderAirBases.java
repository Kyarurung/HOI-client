package dev.hoi.client.map.commander;

import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.math.BigDecimal;

final class CommanderAirBases {
    private CommanderAirBases() {}
    static AirBase state(Member wing){return wing.data()==null||wing.data().air()==null?null:wing.data().air().base();}
    static String identity(Member wing){var base=state(wing);return base==null?"legacy/"+wing.group():base.id();}
    static String capacity(Member wing){var base=state(wing);return base==null?"0":UiCommandLabels.text("AIRBASE_CAPACITY","$VAL$/$MAX$").replace("$VAL$",Integer.toString(base.totalPlanes())).replace("$MAX$",number(base.capacity()));}
    static String tooltip(Member wing){var base=state(wing);if(base==null)return wing.group()+"\n수용량 정보가 없습니다.";return wing.group()+"\n"+UiCommandLabels.text("AIRBASE_AIRPLANES_KNOWN","수용량: $VALUE|H$/$CAPACITY|H$ 비행기").replace("$VALUE|H$","§H"+base.totalPlanes()+"§!").replace("$CAPACITY|H$","§H"+number(base.capacity())+"§!")+"\n\n"+UiCommandLabels.text("AIRBASE_CAPACITY_COUNTS_DESCRIPTION","").replace("$OURS|H$","§H"+base.ownPlanes()+"§!").replace("$TOTAL|H$","§H"+base.totalPlanes()+"§!");}
    static String fuelTooltip(String country){if(!country.equals(dev.hoi.client.ClientCampaignHud.country()))return "국가 연료 정보가 없습니다.";var hud=dev.hoi.client.ClientCampaignHud.hud();var detail=hud.details().get("fuel");if(detail!=null&&detail.sourceText()!=null)return "국가 연료\n"+detail.sourceText();var fuel=hud.national().fuel();return "국가 연료: "+(fuel==null?"0":number(fuel));}
    private static String number(double value){return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();}
}
