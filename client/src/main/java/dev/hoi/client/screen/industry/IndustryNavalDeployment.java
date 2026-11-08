package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;
import dev.hoi.protocol.industry.IndustryView.NavalTarget;

final class IndustryNavalDeployment {
    private IndustryNavalDeployment() {}
    static String tooltip(NavalTarget target) {
        if(target.id().isEmpty())return IndustryEquipmentDesigns.tooltip("PRODUCTION_NAVAL_DEPLOYMENT_AUTO_DEPLOYMENT");
        String name=target.name()+(target.fleet()?"":" ("+target.level()+")");
        String tooltip=IndustryEquipmentDesigns.tooltip(target.fleet()?"PRODUCTION_DEPLOY_TO_FLEET":"PRODUCTION_DEPLOY_TO_BASE")
                .replace(target.fleet()?"$FLEET|H$":"$BASE|H$","§e"+name+"§r");
        return tooltip+(target.port().isEmpty()?"":"\n\n"+IndustryEquipmentDesigns.tooltip("PRODUCTION_RIGHT_CLICK_TO_ZOOM"));
    }
}
