package dev.hoi.client.ui.tooltip;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.util.Util;

public final class TooltipDelay {
    public static final char MARK = dev.hoi.protocol.ProtocolTooltip.DELAYED.charAt(0);
    public static final long DELAY = 1000;
    private static final Map<Tooltip, Tooltip> FULL = Collections.synchronizedMap(new WeakHashMap<>());
    private static String last;
    private static long start, seen;
    private TooltipDelay() {}
    public static boolean delayed(String text) { return text != null && text.indexOf(MARK) >= 0; }
    public static String summary(String text) {
        int at = text.indexOf(MARK);
        return at < 0 ? text : text.substring(0, at).replaceAll("(?:\\n(?:§[0-9A-Za-z!])*)+$", "");
    }
    public static String full(String text) { return text.replace(String.valueOf(MARK), ""); }
    public static String visible(String text) {
        if (!delayed(text)) return text;
        long now = Util.getMillis();
        if (!text.equals(last) || now - seen > 250) { last = text; start = now; }
        seen = now;
        return now - start >= DELAY ? full(text) : summary(text);
    }
    static Tooltip pair(Tooltip summary, Tooltip full) { FULL.put(summary, full); return summary; }
    public static Tooltip full(Tooltip summary) { return summary == null ? null : FULL.get(summary); }
}
