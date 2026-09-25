package dev.hoi.client.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

public final class UiTooltipText {
    private static final Pattern NUMBER = Pattern.compile("(?:(?<![A-Za-z0-9_:/.§-])|(?<=§[0-9a-fA-F]))[+-]?\\d+(?:,\\d{3})*(?:\\.\\d+)?(?![A-Za-z0-9_:/.-])");
    static final Pattern COLORS = Pattern.compile("(?i)§(#[0-9a-f]{6}|[0-9a-fk-or])");
    private static final Pattern CODES = Pattern.compile("§(?:#[0-9a-fA-F]{6}|.)");
    private static final Pattern SEPARATOR = Pattern.compile("(?:-{3,})?");
    private UiTooltipText() {}
    public static String sections(String... paragraphs) {
        return java.util.Arrays.stream(paragraphs).filter(value->value!=null&&!value.isBlank()).collect(java.util.stream.Collectors.joining("\n\n"));
    }
    public static java.util.List<String> colorLines(String text) {
        var lines=new java.util.ArrayList<String>();
        String active="";
        for(String line:text.split("\\R",-1)) {
            lines.add(line.isEmpty()||line.startsWith("[image:")||line.startsWith("[caption:")?line:active+line);
            var codes=COLORS.matcher(line);
            while(codes.find()) {
                String code=codes.group(1);
                if(code.equalsIgnoreCase("r"))active="";
                else if(code.startsWith("#")||code.matches("(?i)[0-9a-f]"))active=codes.group();
                else active+=codes.group();
            }
        }
        return java.util.List.copyOf(lines);
    }
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
    public static boolean blank(String line) { return SEPARATOR.matcher(CODES.matcher(line).replaceAll("").strip()).matches(); }
    public static boolean visible(String line) {
        String plain=CODES.matcher(line).replaceAll("").strip();
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
        boolean space=false,terrain=text.contains("이 지형 내에서의 이동 비용")||text.contains("보급 경로:")||text.contains("민간인 인구");
        var result=new StringBuilder();
        for(String source:text.split("\\R",-1)) {
            if(blank(source)){space=!result.isEmpty();continue;}
            String line=precise?source:numbers(source);
            if(text.contains("보급 경로:")&&CODES.matcher(line).replaceAll("").replaceAll("£[A-Za-z0-9_]+(?:\\|[0-9]+)?", "").strip().matches("기본:\\s*[+-]?0(?:\\.0+)?"))continue;
            if(!terrain&&!visible(line))continue;
            if(!result.isEmpty())result.append(space?"\n\n":"\n");
            result.append(line);space=false;
        }
        return result.toString();
    }
}
