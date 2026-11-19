package dev.hoi.client.screen.industry;

import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.protocol.industry.IndustryView;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class IndustryTradeTexts {
    private static final Pattern FIELD = Pattern.compile("\\$([A-Za-z_]+)(?:\\|([^$]*))?\\$");
    private static final Map<String,String> SOURCE = Map.ofEntries(
            Map.entry("open_econ_button_tt", "§Y경제§!와 §Y무역§! 화면 간 전환"),
            Map.entry("TRADE_CIV_FACTORIES", "민간공장"),
            Map.entry("TRADE_CIV_FACTORIES_AVAIL", "이용 가능: $AVAIL$"),
            Map.entry("TRADE_AUTO_IMPORT", "자동 수입"),
            Map.entry("TRADE_AUTO_IMPORT_DESC", "활성화하면 부족한 전략 자원을 수입하는 데 §H민간공장§!이 자동으로 사용됩니다. 자동 수입은 자원이 부족 상태에 빠지지 않도록 무역을 시작하고 종료합니다. §H석유§!는 자체 생산량을 넘어 우리 군이 소비하는 §H연료§!를 충당할 만큼 수입합니다. §H연료 부족 시 석유 우선 수입§! 옵션이 활성화되어 있고 연료 비축량이 50% 아래로 떨어지면 석유를 우선 수입하고, 수입에 배정된 공장 중 남는 공장을 사용해 연료 비축량이 가득 찰 때까지 석유를 추가로 수입합니다.\n\n자동 수입은 필요한 자원을 전부 공급할 수 있는 상대를 우선하고, 그다음 §H세력 회원국§!, 공급량이 가장 많은 국가, 마지막으로 §H수송선§!이 필요 없는 경로를 선호합니다. 무역 상대국이 우리가 지불한 대가에 상응하는 자원을 더 이상 공급하지 못하면, 부족분을 채워 줄 더 나은 상대국이 있을 경우 해당 수입 거래의 규모를 줄이거나 거래 상대를 변경합니다.\n\n§g자동 수입이 활성화된 동안에는 수입을 수동으로 관리할 수 없습니다.§!\n"),
            Map.entry("TRADE_AUTO_IMPORT_BUDGET_DESC", "자동 수입에 사용할 수 있는 §H민간공장§!의 최대 수입니다.\n\n무역에 사용할 수 있는 공장 수보다 높게 설정할 수 없습니다.\n\n§R이 값을 변경하려면 자동 수입이 활성화되어 있어야 합니다.§!"),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITY_OPEN", "우선순위"),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITY_OPEN_TOOLTIP", "§H자동 수입§!으로 부족한 자원을 충당할 순서를 설정합니다."),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITY_HEADER", "수입 우선순위"),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITY_DESCRIPTION", "자원을 드래그하여 수입 우선순위를 변경합니다. §H-§!와 §H+§!로 허용할 자원 부족량을 설정합니다."),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITY_RESET", "초기화"),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITY_RESET_TOOLTIP", "자원 순서, §H허용 부족량§!, 연료 우선 설정을 기본값으로 되돌립니다."),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITIZE_FUEL", "연료 부족 시 석유 우선 수입"),
            Map.entry("TRADE_AUTO_IMPORT_PRIORITIZE_FUEL_DESC", "§H연료§! 비축량이 부족해지면 다른 모든 자원보다 §H석유§!를 우선 수입하며, 수입에 배정된 공장 중 남는 공장을 사용해 연료 비축량이 가득 찰 때까지 석유를 추가로 수입합니다.\n\n이 옵션을 해제하면 석유도 다른 자원과 마찬가지로 목록의 우선순위를 따릅니다."),
            Map.entry("TRADE_AUTO_IMPORT_ACCEPTED_SHORTAGE_DESC", "§H허용 부족량§!\n자동 수입 시 이 자원의 부족량을 설정한 수치까지 허용합니다."),
            Map.entry("TRADE_AUTO_IMPORT_ACCEPTED_SHORTAGE_ADD_DESC", "§H허용 부족량§!\n자동 수입 시 이 자원의 부족량을 설정한 수치까지 허용합니다.\n\n§G클릭§!하여 §Y+1§! 증가\n§GCtrl + 클릭§!하여 §Y+5§! 증가\n§GShift + 클릭§!하여 §Y+10§! 증가"),
            Map.entry("TRADE_AUTO_IMPORT_ACCEPTED_SHORTAGE_SUB_DESC", "§H허용 부족량§!\n자동 수입 시 이 자원의 부족량을 설정한 수치까지 허용합니다.\n\n§G클릭§!하여 §Y1§! 감소\n§GCtrl + 클릭§!하여 §Y5§! 감소\n§GShift + 클릭§!하여 §Y10§! 감소"),
            Map.entry("TRADE_AUTO_IMPORT_BLOCKS_PROPOSE", "§R자동 수입이 활성화되어 있습니다. 무역을 제안하려면 비활성화하십시오.§!"),
            Map.entry("TRADE_AUTO_IMPORT_BLOCKS_CANCEL", "§R자동 수입이 켜져 있습니다. 무역을 직접 변경하려면 먼저 꺼야 합니다.§!"),
            Map.entry("TRADEVIEW_RES_PRODUCED", "추출:"),
            Map.entry("TRADEVIEW_RES_IMPORTED", "수입:"),
            Map.entry("TRADEVIEW_RES_EXPORTED", "수출:"),
            Map.entry("TRADEVIEW_RES_SPECIAL_PROJECT", "계획:"),
            Map.entry("TRADEVIEW_RES_PRODUCTION_LINES", "생산:"),
            Map.entry("TRADEVIEW_RES_SURPLUS", "잉여분:"),
            Map.entry("OIL_NEED_TEXT", "C:\nM:"),
            Map.entry("CANCEL_RESOURCE_IMPORT", "$RESOURCE|H$ 수입을 취소하려면 §G클릭§!"),
            Map.entry("NO_IMPORT_TO_CANCEL", "§R수입한 물자가 없으므로 $RESOURCE|H$의 수입을 취소할 수 없습니다.§!"),
            Map.entry("TRADE_FILTER_BY", "$KEY|H$ 수출국 목록"),
            Map.entry("TRADE_SORT_COUNTRY", "국가"),
            Map.entry("TRADE_SORT_EXPORT", "수출"),
            Map.entry("TRADE_SORT_REQUEST", "운송됨"),
            Map.entry("TRADE_SORT_CONVOYS", "수송선"),
            Map.entry("europe", "유럽"),
            Map.entry("north_america", "북아메리카"),
            Map.entry("south_america", "남아메리카"),
            Map.entry("australia", "오세아니아"),
            Map.entry("africa", "아프리카"),
            Map.entry("asia", "아시아"),
            Map.entry("middle_east", "중동"),
            Map.entry("faction", "세력"),
            Map.entry("subject", "종속국"),
            Map.entry("RESOURCE_DETAILS_NET_SURPLUS", "우리는 $SURPLUS|G$만큼의 $RESOURCE|H$ 잉여분을 보유 중입니다."),
            Map.entry("RESOURCE_DETAILS_NET_DEFICIT", "우리는 $DEFICIT|R$만큼의 $RESOURCE|H$(이)가 부족한 상태입니다."),
            Map.entry("RESOURCE_DETAILS_SURPLUS", "사용 가능한 $RESOURCE|H$: $TOTAL|H$\n\n특별 프로젝트의 요구량: $PROJECTS|H$\n생산 라인의 요구량: $PRODUCTION|H$\n잉여분: $SURPLUS|H$"),
            Map.entry("RESOURCE_DETAILS_DEFICIT", "사용 가능한 $RESOURCE|H$: $TOTAL|H$ \n\n특별 프로젝트의 요구량: $PROJECTS|H$\n생산 라인의 요구량: $PRODUCTION|H$\n부족분: $DEFICIT|R$"),
            Map.entry("RESOURCE_DETAILS_FUEL", "사용 가능한 $RESOURCE|H$: $TOTAL|H$\n\n현재 소비량: $ACTIVE_FUEL|H$\n최대 소비량: $POTENTIAL_FUEL|H$"),
            Map.entry("TRADEVIEW_RES_PRODUCED_DETAILS", "우리는 $RES|Y$(을)를 $TOT|Y$만큼 생산"),
            Map.entry("TRADEVIEW_IMPORT_DETAILS_US", "§T우리는 $TOT|H$의 $RES|H$(을)를 $NUM|H$개국에서 수입 중:§!"),
            Map.entry("TRADEVIEW_IMPORT_DETAILS_NOTHING", "§T우리는 $RES|H$(을)를 수입하고 있지 않음§!"),
            Map.entry("TRADEVIEW_IMPORT_DETAILS_LINE_THEY", " - @$TAG$$WHO|H$ 측으로부터 $NUM$/$TOT$ 수입"),
            Map.entry("TRADEVIEW_RES_EXPORT_DETAILS", "우리는 무역법에 따라 $RES|H$(을)를 $NUM|H0$만큼 수출 중입니다."),
            Map.entry("TRADEVIEW_RES_EXPORT_NONE", "우리는 $RES|H$(을)를 수출하고 있지 않습니다."),
            Map.entry("TRADEVIEW_RES_USED_DETAILS", "§T우리 $CONSUMER|Y$이 $NUM|H$의 $RES|H$(을)를 소비 중입니다.§!"),
            Map.entry("TRADEVIEW_PRODUCTION_LINES", "생산 라인"));
    private IndustryTradeTexts() {}
    static String text(String key) {
        return UiCommandLabels.text(key, Objects.requireNonNull(SOURCE.get(key), key)).replace("§T", "§W");
    }
    static String fill(String key, Map<String,String> values) {
        var matcher = FIELD.matcher(text(key).replace("@$TAG$", ""));
        var out = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) { matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group())); continue; }
            String format = matcher.group(2) == null ? "" : matcher.group(2).replaceAll("[^A-Za-z]", "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(format.isEmpty() ? value : "§" + format.charAt(0) + value + "§!"));
        }
        matcher.appendTail(out);
        return out.toString();
    }
    static String amount(double value) { return Long.toString(Math.round(value)); }
    static String balance(IndustryView.Resource r) {
        long surplus = Math.round(r.available() - r.demand());
        return surplus >= 0 ? fill("RESOURCE_DETAILS_NET_SURPLUS", Map.of("SURPLUS", Long.toString(surplus), "RESOURCE", r.name()))
                : fill("RESOURCE_DETAILS_NET_DEFICIT", Map.of("DEFICIT", Long.toString(-surplus), "RESOURCE", r.name()));
    }
    static String surplus(IndustryView.Resource r) {
        if (r.fuelCurrent() != null && r.fuelMaximum() != null)
            return fill("RESOURCE_DETAILS_FUEL", Map.of("RESOURCE", r.name(), "TOTAL", amount(r.available()), "ACTIVE_FUEL", amount(r.fuelCurrent()), "POTENTIAL_FUEL", amount(r.fuelMaximum())));
        long surplus = Math.round(r.available() - r.demand());
        var values = new HashMap<String,String>(Map.of("RESOURCE", r.name(), "TOTAL", amount(r.available()), "PROJECTS", "0", "PRODUCTION", amount(r.demand())));
        if (surplus >= 0) values.put("SURPLUS", Long.toString(surplus)); else values.put("DEFICIT", Long.toString(-surplus));
        return fill(surplus >= 0 ? "RESOURCE_DETAILS_SURPLUS" : "RESOURCE_DETAILS_DEFICIT", values);
    }
    static String produced(IndustryView.Resource r) {
        return fill("TRADEVIEW_RES_PRODUCED_DETAILS", Map.of("RES", r.name(), "TOT", amount(r.extracted())));
    }
    static String imported(IndustryView view, IndustryView.Resource r) {
        var deals = view.trades().stream().filter(t -> t.importing() && t.resource().equals(r.id())).toList();
        if (deals.isEmpty()) return fill("TRADEVIEW_IMPORT_DETAILS_NOTHING", Map.of("RES", r.name()));
        var partners = new LinkedHashMap<String,double[]>();
        for (var deal : deals) {
            var partner = view.partners().stream().filter(p -> p.id().equals(deal.partner())).findFirst().orElse(null);
            double perFactory = partner == null || partner.resourcesPerFactory() == null ? 8 : partner.resourcesPerFactory();
            var total = partners.computeIfAbsent(deal.partner(), id -> new double[2]);
            total[0] += deal.delivered(); total[1] += deal.factories() * perFactory;
        }
        var text = new StringBuilder(fill("TRADEVIEW_IMPORT_DETAILS_US", Map.of("TOT", amount(r.imported()), "RES", r.name(), "NUM", Integer.toString(partners.size()))));
        partners.forEach((id, total) -> {
            String name = view.partners().stream().filter(p -> p.id().equals(id)).map(IndustryView.Partner::name).findFirst().orElse(id);
            text.append('\n').append(fill("TRADEVIEW_IMPORT_DETAILS_LINE_THEY", Map.of("WHO", name, "NUM", amount(total[0]), "TOT", amount(total[1]))));
        });
        return text.toString();
    }
    static String exported(IndustryView.Resource r) {
        return Math.round(r.exported()) == 0 ? fill("TRADEVIEW_RES_EXPORT_NONE", Map.of("RES", r.name()))
                : fill("TRADEVIEW_RES_EXPORT_DETAILS", Map.of("RES", r.name(), "NUM", amount(r.exported())));
    }
    static String used(IndustryView.Resource r) {
        return fill("TRADEVIEW_RES_USED_DETAILS", Map.of("CONSUMER", text("TRADEVIEW_PRODUCTION_LINES"), "NUM", amount(r.demand()), "RES", r.name()));
    }
}
