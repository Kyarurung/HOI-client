package dev.hoi.client.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class TooltipText {
    private static final Pattern NUMBER = Pattern.compile("(?:(?<![A-Za-z0-9_:/.§-])|(?<=§[0-9a-fA-F]))[+-]?\\d+(?:,\\d{3})*(?:\\.\\d+)?(?![A-Za-z0-9_:/.-])");
    private TooltipText() {}
    public static boolean precise(String text) {
        return text.contains("£") || text.contains("보급 경로:") || text.contains("부대 효과:") || text.contains("이 지형 내에서의 이동 비용") || text.contains("지지도") || text.contains("정당 인기도") || text.contains("지지도 증가") || text.contains("일일 지지도");
    }
    public static String numbers(String text) {
        return NUMBER.matcher(text).replaceAll(match -> {
            String original=match.group();
            if(!original.contains("."))return java.util.regex.Matcher.quoteReplacement(original);
            var value=new BigDecimal(original.replace(",", "")).setScale(1,RoundingMode.HALF_UP).stripTrailingZeros();
            String result=value.toPlainString();
            if(original.startsWith("+")&&value.signum()!=0)result="+"+result;
            return java.util.regex.Matcher.quoteReplacement(result);
        });
    }
    public static boolean blank(String line) { return line.replaceAll("§(?:#[0-9a-fA-F]{6}|.)", "").isBlank(); }
    public static boolean visible(String line) {
        String plain=line.replaceAll("§(?:#[0-9a-fA-F]{6}|.)", "").strip();
        if(plain.isEmpty())return false;
        if(plain.startsWith("[image:") || plain.contains("/") || plain.matches(".*\\d{4}[-.]\\d.*"))return true;
        var values=NUMBER.matcher(plain);
        boolean found=false;
        while(values.find()) {
            found=true;
            if(new BigDecimal(values.group().replace(",", "")).signum()!=0)return true;
        }
        return !found;
    }
    public static String spacing(String text) {
        var result=new StringBuilder();
        boolean separator=false;
        for(String line:text.split("\\R",-1)) {
            if(blank(line)){separator=!result.isEmpty();continue;}
            if(!result.isEmpty())result.append(separator?"\n\n":"\n");
            result.append(line);separator=false;
        }
        return result.toString();
    }
    public static String format(String text) {
        return format(text,precise(text));
    }
    public static String format(String text,boolean precise) {
        boolean space=false,terrain=text.contains("이 지형 내에서의 이동 비용")||text.contains("보급 경로:");
        var result=new StringBuilder();
        for(String source:text.split("\\R",-1)) {
            if(blank(source)){space=!result.isEmpty();continue;}
            String line=precise?source:numbers(source);
            if(text.contains("보급 경로:")&&line.replaceAll("§(?:#[0-9a-fA-F]{6}|.)", "").replaceAll("£[A-Za-z0-9_]+(?:\\|[0-9]+)?", "").strip().matches("기본:\\s*[+-]?0(?:\\.0+)?"))continue;
            if(!terrain&&!visible(line))continue;
            if(!result.isEmpty())result.append(space?"\n\n":"\n");
            result.append(line);space=false;
        }
        return result.toString();
    }
}
