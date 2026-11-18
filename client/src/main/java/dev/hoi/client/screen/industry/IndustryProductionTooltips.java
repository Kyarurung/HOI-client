package dev.hoi.client.screen.industry;

import dev.hoi.protocol.industry.IndustryView;
import dev.hoi.client.ui.industry.IndustryEquipmentDesigns;

final class IndustryProductionTooltips {
    private static final java.util.List<String> STAT_ORDER=java.util.List.of("manpower","air_range","defense","breakthrough","hardness","soft_attack","hard_attack","air_defence","air_attack","air_agility","air_superiority","naval_strike_attack","naval_strike_targetting","surface_detection","sub_detection","surface_visibility","sub_visibility","lg_attack","lg_armor_piercing","hg_attack","hg_armor_piercing","torpedo_attack","sub_attack","anti_air_attack","maximum_speed","naval_speed","naval_range","max_organisation","max_strength","armor_value","ap_attack","reliability","fuel_consumption","weight","thrust");
    static String navalWaiting(IndustryView view,IndustryView.Line line) {
        String title=IndustryEquipmentDesigns.tooltip("PRODUCTION_CAN_NOT_DEPLOY");
        String reason=view.navalDeployment().waiting().get(line.id());
        return reason.equals("PRODUCTION_CAN_NOT_DEPLOY")?title:title+"\n"+IndustryEquipmentDesigns.tooltip(reason);
    }
    private IndustryProductionTooltips() {}
    static String modifier(IndustryView.Modifier modifier) {
        return "§e"+modifier.name()+"§r\n"+modifier.detail();
    }
    static String efficiency(IndustryView view,IndustryView.Line line) {
        var text=new StringBuilder("£prod_eff 생산 효율: §e").append(percent(line.efficiency(),2)).append("§r");
        var cap=view.modifiers().stream().filter(m->m.id().equals("cap")).findFirst();
        if(line.efficiencyGain()!=null&&cap.map(value->line.efficiency()+1e-12<value.value()).orElse(true))
            text.append(" (매일 §e").append(percent(line.efficiencyGain(),2)).append("§r)");
        cap.ifPresent(value->text.append("\n").append(value.name()).append(": §e").append(percent(value.value(),2)).append("§r\n\n").append(contributions(value)));
        text.append("\n\n§e효율 획득 속도 변동치:§r");
        if(line.efficiencyBaseGain()!=null)text.append("\n매일 기본 획득량: §e").append(percent(line.efficiencyBaseGain(),2)).append("§r");
        view.modifiers().stream().filter(m->m.id().equals("growth")).findFirst().ifPresent(growth->{
            String rows=contributions(growth);
            if(!rows.isBlank())text.append("\n").append(rows);
        });
        if(line.efficiencyResourcePenalty()!=null&&line.efficiencyResourcePenalty()>0)
            text.append("\n자원 부족: §c-").append(percent(line.efficiencyResourcePenalty(),2)).append("§r");
        cap.ifPresent(value->{int split=value.detail().lastIndexOf("\n\n");if(split>=0)text.append("\n\n").append(value.detail().substring(split+2));});
        return text.toString();
    }
    private static String contributions(IndustryView.Modifier modifier) {
        int split=modifier.detail().lastIndexOf("\n\n");
        return split<0?"":modifier.detail().substring(0,split);
    }
    static String equipment(IndustryView view,IndustryView.Line line,IndustryView.Equipment equipment) {
        return equipment(view,line,equipment,shortage(line));
    }
    static Long shortage(IndustryView.Line line) {
        return line.needState()==null?null:line.needState()==2?line.needValue():Long.valueOf(0);
    }
    static String equipment(IndustryView view,IndustryView.Line line,IndustryView.Equipment equipment,Long shortage) {
        var text=new StringBuilder("§e").append(equipment.name()).append("§r");
        if(equipment.familyName()!=null&&!equipment.familyName().equals(equipment.name()))text.append("\n§e").append(equipment.familyName()).append("§r");
        text.append("\n\n일일 생산량: §e").append(dev.hoi.protocol.ProtocolNumberBounds.fixed(line.daily(), 2)).append("§r");
        if(shortage!=null)text.append(shortage==0?"\n현재로서는 수요가 §a충족§r된 상태입니다.":"\n충원 수요 부족: §c"+shortage+"§r");
        appendStats(text,equipment);
        boolean conversion=line.conversion()!=null&&line.conversion().source()!=null;
        text.append("\n생산 비용: §e").append(dev.hoi.protocol.ProtocolNumberBounds.fixed(conversion?line.conversion().cost():equipment.cost(), 2)).append("§r");
        text.append("\n\n§e공장당 자원 사용:§r");
        var costs=conversion?line.conversion().resources():equipment.resources();
        for(var entry:new java.util.TreeMap<>(costs).entrySet())if(entry.getValue()>0) {
            String label=view.resources().stream().filter(r->r.id().equals(entry.getKey())).map(IndustryView.Resource::name).findFirst().orElse(entry.getKey());
            text.append("\n").append(label).append(": §e").append(java.math.BigDecimal.valueOf(entry.getValue()).stripTrailingZeros().toPlainString()).append("§r");
        }
        text.append("\n").append(IndustryEquipmentDesigns.tooltip(equipment.naval()?"PRODUCTION_LINE_CHANGE_NAVAL":"PRODUCTION_LINE_CHANGE"));
        return text.toString();
    }
    static String equipment(IndustryView view,IndustryView.Equipment equipment) {
        var text=new StringBuilder("§e").append(equipment.name()).append("§r");
        if(equipment.familyName()!=null&&!equipment.familyName().equals(equipment.name()))text.append("\n§e").append(equipment.familyName()).append("§r");
        appendStats(text,equipment);
        text.append("\n생산 비용: §e").append(dev.hoi.protocol.ProtocolNumberBounds.fixed(equipment.cost(),2)).append("§r");
        text.append("\n\n§e공장당 자원 사용:§r");
        for(var resource:view.resources()) {
            double amount=equipment.resources().getOrDefault(resource.id(),0.0);
            if(amount>0)text.append("\n  ").append(resource.name()).append(": §e").append(java.math.BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()).append("§r");
        }
        text.append("\n\n§a클릭§r하여 생산 시작");
        String description=IndustryEquipmentDesigns.equipmentDescription(view.country(),equipment.sourceEquipment());
        if(!description.isBlank())text.append("\n\n").append(description);
        return text.toString();
    }
    private static void appendStats(StringBuilder text,IndustryView.Equipment equipment) {
        if(!equipment.stats().isEmpty())text.append("\n");
        var ordered=equipment.stats().stream().sorted(java.util.Comparator.comparingInt(stat->{int index=STAT_ORDER.indexOf(stat.id());return index<0?STAT_ORDER.size():index;})).toList();
        for(var stat:ordered)if(!stat.id().equals("build_cost_ic")) {
            String caption=IndustryEquipmentDesigns.statCaption(stat.id(),stat.name()).strip();
            text.append("\n").append(caption).append(caption.endsWith(":")?" ":": ").append("§e")
                    .append(IndustryEquipmentDesigns.statValue(stat,stat.value())).append("§r");
        }
    }
    private static String percent(double value,int places) {
        return dev.hoi.protocol.ProtocolNumberBounds.percent(value, places);
    }
}
