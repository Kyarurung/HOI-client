package dev.hoi.client.screen;

import dev.hoi.protocol.IndustryView;
import java.util.Locale;

final class ScreenProductionTooltips {
    private ScreenProductionTooltips() {}
    static String modifier(IndustryView.Modifier modifier) {
        return "§e"+modifier.name()+"§r\n"+modifier.detail();
    }
    static String efficiency(IndustryView view,IndustryView.Line line) {
        var text=new StringBuilder("£prod_eff 생산 효율: §e").append(percent(line.efficiency(),2)).append("§r");
        var cap=view.modifiers().stream().filter(m->m.id().equals("cap")).findFirst();
        if(line.efficiencyGain()!=null&&cap.map(value->line.efficiency()+1e-12<value.value()).orElse(true))
            text.append(" (매일 §e").append(percent(line.efficiencyGain(),2)).append("§r)");
        cap.ifPresent(value->text.append("\n").append(value.name()).append(": §e").append(percent(value.value(),2)).append("§r\n--------------\n").append(contributions(value)));
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
        var text=new StringBuilder("§e").append(equipment.name()).append("§r");
        if(equipment.familyName()!=null&&!equipment.familyName().equals(equipment.name()))text.append("\n§e").append(equipment.familyName()).append("§r");
        text.append("\n\n일일 생산량: §e").append(dev.hoi.protocol.ProtocolNumbers.fixed(line.daily(), 2)).append("§r");
        var models=view.equipment().stream().filter(e->e.group().equals(equipment.group())&&(equipment.family()==null?e.id().equals(equipment.id()):equipment.family().equals(e.family()))).toList();
        if(models.stream().allMatch(e->e.demand()!=null)) {
            long demand=models.stream().mapToLong(IndustryView.Equipment::demand).sum();
            long stock=models.stream().mapToLong(IndustryView.Equipment::stockpile).sum();
            text.append(demand<=stock?"\n현재로서는 수요가 §a충족§r된 상태입니다.":"\n충원 수요 부족: §c"+(demand-stock)+"§r");
        }
        dev.hoi.client.research.ResearchDetails.equipment(equipment.sourceEquipment()).ifPresent(card->{
            text.append("\n\n§e원본 기본 능력치§r");
            card.effects().stream().filter(stat->!stat.startsWith("생산 비용:")).forEach(stat->text.append("\n").append(stat.replaceFirst(": (.*)",": §e$1§r")));
        });
        boolean conversion=line.conversion()!=null&&line.conversion().source()!=null;
        text.append("\n생산 비용: §e").append(dev.hoi.protocol.ProtocolNumbers.fixed(conversion?line.conversion().cost():equipment.cost(), 2)).append("§r");
        text.append("\n\n§e공장당 자원 사용:§r");
        var costs=conversion?line.conversion().resources():equipment.resources();
        for(var entry:new java.util.TreeMap<>(costs).entrySet())if(entry.getValue()>0) {
            String label=view.resources().stream().filter(r->r.id().equals(entry.getKey())).map(IndustryView.Resource::name).findFirst().orElse(entry.getKey());
            text.append("\n").append(label).append(": §e").append(java.math.BigDecimal.valueOf(entry.getValue()).stripTrailingZeros().toPlainString()).append("§r");
        }
        text.append("\n§a생산 장비를 변경하려면 클릭하십시오.");
        if(!equipment.naval())text.append("\n관련 장비로 변경할 경우, 기존 생산 효율의 일부를 유지하게 됩니다.");
        text.append("§r\n§c이 생산 라인의 진행도를 전부 잃게 됩니다.§r");
        return text.toString();
    }
    private static String percent(double value,int places) {
        return dev.hoi.protocol.ProtocolNumbers.percent(value, places);
    }
}
