package dev.hoi.client.research;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;


final class ResearchText {
    private static final Pattern VALUE = Pattern.compile("^(.*?)([+-]?\\d+(?:\\.\\d+)?)(%|kn|km/h|km)?$");
    private static final Pattern KEYWORDS = Pattern.compile("해군 조선소|군수공장|민간공장|조선소|발전소|정유시설");
    private static final List<String> LOWER_IS_BETTER = List.of("비용", "유지비", "사용량", "소비", "가시성", "취약도", "불이익", "훈련 시간");
    private ResearchText() {}

    static MutableComponent white(String text) { return Component.literal(text).withStyle(ChatFormatting.WHITE); }
    static MutableComponent gold(String text) { return Component.literal(text).withStyle(ChatFormatting.GOLD); }

    static Component keywords(String text) {
        var result = white(""); var matcher = KEYWORDS.matcher(text); int end = 0;
        while (matcher.find()) {
            result.append(white(text.substring(end, matcher.start()))).append(gold(matcher.group()));
            end = matcher.end();
        }
        return result.append(white(text.substring(end)));
    }

    static Component effect(String text) {
        var match = VALUE.matcher(text);
        if (!match.matches()) return keywords(text);
        String label = match.group(1), raw = match.group(2), unit = match.group(3);
        var value = new BigDecimal(raw);
        boolean signed = raw.startsWith("+") || raw.startsWith("-");
        if (label.contains("기갑 비율") && unit == null) {
            value = value.multiply(BigDecimal.valueOf(100));
            unit = "%";
            signed = true;
        }

        if (signed && LOWER_IS_BETTER.stream().anyMatch(label::contains)) value = value.negate();
        String number = (signed && value.signum() > 0 ? "+" : "") + value.stripTrailingZeros().toPlainString();
        var color = signed && value.signum() != 0 ? (value.signum() > 0 ? ChatFormatting.GREEN : ChatFormatting.DARK_RED)
                : label.matches(".*(?:인력|조직력|훈련 시간)[: ]*") ? ChatFormatting.WHITE : ChatFormatting.GOLD;
        var result = white("").append(keywords(label)).append(Component.literal(number + ("%".equals(unit) ? "%" : "")).withStyle(color));
        if (unit != null && !unit.equals("%")) result.append(white(unit));
        return result;
    }
}
