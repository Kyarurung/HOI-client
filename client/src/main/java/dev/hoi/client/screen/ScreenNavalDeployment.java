package dev.hoi.client.screen;

import dev.hoi.client.ui.UiEquipmentDesigns;
import dev.hoi.protocol.ProtocolIndustryView.NavalTarget;

final class ScreenNavalDeployment {
    private ScreenNavalDeployment() {}
    static String tooltip(NavalTarget target) {
        if(target.id().isEmpty())return UiEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOYMENT_AUTO_DEPLOYMENT");
        String name=target.name()+(target.fleet()?"":" ("+target.level()+")");
        String tooltip=UiEquipmentDesigns.tooltip(target.fleet()?"PRODUCTION_DEPLOY_TO_FLEET":"PRODUCTION_DEPLOY_TO_BASE")
                .replace(target.fleet()?"$FLEET|H$":"$BASE|H$","§e"+name+"§r");
        return tooltip+(target.port().isEmpty()?"":"\n\n"+UiEquipmentDesigns.tooltip("PRODUCTION_RIGHT_CLICK_TO_ZOOM"));
    }
}
