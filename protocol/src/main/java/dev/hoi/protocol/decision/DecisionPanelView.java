package dev.hoi.protocol.decision;

import dev.hoi.protocol.country.CountryView;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@JsonAdapter(DecisionPanelView.AdapterFactory.class)
public record DecisionPanelView(String scriptedGui, String windowName, String source, Integer line,
        List<Element> elements, List<String> unresolved) {
    public static final int MAX_ELEMENTS = 128, MAX_DEPTH = 8, MAX_TEXT = 4_096, MAX_PANEL_TEXT = 32_768;
    public static final double MAX_COORDINATE = 16_384, MAX_SCALE = 16, MAX_FRAME = 1_000_000;
    private static final Set<String> KINDS = Set.of("container","icon","background","text","grid","radial");
    private static final Set<String> ORIENTATIONS = Set.of("upper_left","upper_right","lower_left","lower_right",
            "center","center_up","center_down","center_left","center_right");
    private static final Set<String> STRING_FIELDS = Set.of("scriptedGui","windowName","source","name","kind","orientation",
            "alignment","font","sprite","frameVariable","array","index","entryContainer");
    private static final Set<String> BOOLEAN_FIELDS = Set.of("relative","complete","visible","centerPosition","clipping",
            "fixedSize","alwaysTransparent","addHorizontal");
    private static final Set<String> NUMBER_FIELDS = Set.of("line","x","y","scale","frame","maxSlotsVertical");
    public DecisionPanelView {
        identifier(scriptedGui); identifier(windowName); provenance(source,line);
        elements = copy(elements); unresolved = copy(unresolved);
        if (elements.isEmpty() || new HashSet<>(unresolved).size() != unresolved.size()) invalid();
        var budget = new int[2];
        for (var element : elements) tree(element,1,budget);
        int unresolvedSize = 0;
        for (String item : unresolved) {
            CountryView.text(item,MAX_TEXT);
            if (item.isEmpty()) invalid();
            unresolvedSize += item.length();
        }
        if (unresolvedSize > MAX_PANEL_TEXT) invalid();
    }
    public record Measure(Double value, Boolean relative) {
        public Measure {
            if (relative == null || value == null) invalid();
            number(value,0,relative ? 100 : MAX_COORDINATE);
        }
    }
    public record Layout(Double x, Double y, Measure width, Measure height, String orientation, Boolean centerPosition,
            Double scale, String alignment, String font, Boolean clipping, Boolean fixedSize, Boolean alwaysTransparent) {
        public Layout {
            number(x,-MAX_COORDINATE,MAX_COORDINATE); number(y,-MAX_COORDINATE,MAX_COORDINATE);
            number(scale,0,MAX_SCALE);
            if (scale != null && scale == 0 || orientation != null && !ORIENTATIONS.contains(orientation)
                    || alignment != null && !Set.of("left","center","right").contains(alignment)) invalid();
            if (font != null) identifier(font);
        }
    }
    public record Text(String value, Boolean complete) {
        public Text {
            CountryView.text(value,MAX_TEXT);
            if (complete == null) invalid();
        }
    }
    public record Tooltip(Text immediate, Text delayed) {
        public Tooltip { if (immediate == null && delayed == null) invalid(); }
    }
    public record Repeat(String array, String index, String value, String entryContainer) {
        public Repeat { identifier(array); identifier(index); identifier(value); identifier(entryContainer); }
    }
    public record Grid(Measure slotWidth, Measure slotHeight, Integer maxSlotsVertical, Boolean addHorizontal, Repeat repeat) {
        public Grid { if (maxSlotsVertical != null && (maxSlotsVertical < 0 || maxSlotsVertical > MAX_ELEMENTS)) invalid(); }
    }
    public record Element(String name, String kind, String source, Integer line, Layout layout, String sprite, Double frame,
            String frameVariable, Text text, Tooltip tooltip, Boolean visible, Grid grid, List<Element> children) {
        public Element {
            identifier(name); provenance(source,line);
            if (kind == null || !KINDS.contains(kind) || layout == null) invalid();
            if (sprite != null) identifier(sprite);
            if (frameVariable != null) identifier(frameVariable);
            number(frame,0,MAX_FRAME);
            children = copy(children);
            if (kind.equals("grid") != (grid != null) || !children.isEmpty() && !Set.of("container","grid").contains(kind)) invalid();
            var budget = new int[]{1,textSize(text,tooltip)};
            for (var child : children) tree(child,2,budget);
            limit(budget);
        }
    }
    private static void tree(Element element, int depth, int[] budget) {
        if (depth > MAX_DEPTH) invalid();
        budget[0]++; budget[1] += textSize(element.text(),element.tooltip());
        limit(budget);
        for (var child : element.children()) tree(child,depth + 1,budget);
    }
    private static int textSize(Text text, Tooltip tooltip) {
        return (text == null ? 0 : text.value().length()) + (tooltip == null ? 0
                : (tooltip.immediate() == null ? 0 : tooltip.immediate().value().length())
                + (tooltip.delayed() == null ? 0 : tooltip.delayed().value().length()));
    }
    private static void limit(int[] budget) { if (budget[0] > MAX_ELEMENTS || budget[1] > MAX_PANEL_TEXT) invalid(); }
    private static <T> List<T> copy(List<T> values) {
        if (values == null || values.size() > MAX_ELEMENTS || values.stream().anyMatch(Objects::isNull)) invalid();
        return List.copyOf(values);
    }
    private static void identifier(String value) {
        if (value == null || !value.matches("[A-Za-z_][A-Za-z0-9_.:@-]{0,255}")) invalid();
    }
    private static void provenance(String source, Integer line) {
        CountryView.text(source,512);
        if (line == null || line < 1 || line > 1_000_000 || !source.matches("(?:tfr|korea)/[A-Za-z0-9_ ./-]+\\.(?:gui|txt)")) invalid();
        for (String part : source.split("/",-1)) if (part.isBlank() || part.equals(".") || part.equals("..")) invalid();
    }
    private static void number(Double value, double minimum, double maximum) {
        if (value != null && (!Double.isFinite(value) || value < minimum || value > maximum)) invalid();
    }
    private static void invalid() { throw new IllegalArgumentException("Invalid decision panel"); }
    public static final class AdapterFactory implements TypeAdapterFactory {
        @Override public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            var delegate = gson.getDelegateAdapter(this,type);
            return new TypeAdapter<T>() {
                @Override public void write(JsonWriter writer, T value) throws IOException { delegate.write(writer,value); }
                @Override public T read(JsonReader reader) throws IOException { return delegate.fromJsonTree(readBounded(reader,0)); }
            }.nullSafe();
        }
    }
    private static JsonElement readBounded(JsonReader reader, int depth) throws IOException {
        if (depth > MAX_DEPTH * 2 + 8) invalid();
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                reader.beginObject(); var object = new JsonObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.length() > 256 || object.size() >= 64 || object.has(name)) invalid();
                    object.add(name,readBounded(reader,depth + 1));
                }
                reader.endObject(); wireTypes(object); yield object;
            }
            case BEGIN_ARRAY -> {
                reader.beginArray(); var array = new JsonArray();
                while (reader.hasNext()) {
                    if (array.size() >= MAX_ELEMENTS) invalid();
                    array.add(readBounded(reader,depth + 1));
                }
                reader.endArray(); yield array;
            }
            case STRING -> {
                String value = reader.nextString(); CountryView.text(value,MAX_TEXT); yield new JsonPrimitive(value);
            }
            case NUMBER -> {
                String value = reader.nextString();
                if (value.length() > 64) invalid();
                var number = new BigDecimal(value);
                yield number.signum() == 0 && value.startsWith("-") ? new JsonPrimitive(-0.0) : new JsonPrimitive(number);
            }
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException("Invalid decision panel JSON");
        };
    }
    private static void wireTypes(JsonObject object) {
        for (var entry : object.entrySet()) {
            String key = entry.getKey(); JsonElement value = entry.getValue();
            if (value.isJsonNull()) continue;
            if (key.equals("unresolved")) {
                if (!value.isJsonArray()) invalid();
                for (var item : value.getAsJsonArray()) if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) invalid();
            }
            boolean number = NUMBER_FIELDS.contains(key) || key.equals("value") && object.has("relative");
            boolean bool = BOOLEAN_FIELDS.contains(key);
            boolean string = STRING_FIELDS.contains(key) || key.equals("value") && !object.has("relative");
            if (number || bool || string) {
                if (!value.isJsonPrimitive()) invalid();
                var primitive = value.getAsJsonPrimitive();
                if (number && !primitive.isNumber() || bool && !primitive.isBoolean() || string && !primitive.isString()) invalid();
                if (key.equals("line") || key.equals("maxSlotsVertical")) {
                    try { primitive.getAsBigDecimal().intValueExact(); }
                    catch (ArithmeticException error) { invalid(); }
                }
            }
        }
    }
}
