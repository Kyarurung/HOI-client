package dev.hoi.protocol.decision;

import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.country.CountryView;
import dev.hoi.protocol.map.MapAtlasScene;
import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class DecisionPayloads {
    public static final int PAGE_SIZE = 48, MAX_JSON = 160_000, MAX_PAGES = 257, MAX_ROWS = 12_288;
    public static final int MAX_DESCRIPTION = 16_000, MAX_TOOLTIP = 24_000, MAX_COST_TEXT = 4_000;
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private DecisionPayloads() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        DecisionDetailPayloads.registerPayloadTypes();
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Highlight.TYPE, Highlight.CODEC);
        registered = true;
    }
    public enum Action { OPEN, REFRESH, TAKE, HIGHLIGHT, GOTO }
    public record Request(Action action, String token, String session, long revision, String id, int page) implements CustomPacketPayload {
        public Request {
            Objects.requireNonNull(action); UUID.fromString(token);
            if (!session.isEmpty()) UUID.fromString(session);
            CountryView.text(id, 256);
            if (revision < 0 || page < 0 || page > 256) throw new IllegalArgumentException("Invalid decision request");
            if ((action == Action.HIGHLIGHT || action == Action.GOTO) && (session.isEmpty() || id.isEmpty() || page != 0))
                throw new IllegalArgumentException("Invalid decision map request");
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.parse("hoi:decision_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeEnum(p.action); b.writeUtf(p.token,36); b.writeUtf(p.session,36); b.writeVarLong(p.revision); b.writeUtf(p.id,256); b.writeVarInt(p.page); },
                b -> new Request(b.readEnum(Action.class),b.readUtf(36),b.readUtf(36),b.readVarLong(),b.readUtf(256),b.readVarInt()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Category(String id, String name, String description, String icon, String picture, String scriptedGui, boolean visibleWhenEmpty,
            DecisionPanelView panel) {
        public Category(String id, String name, String description, String icon, String picture, String scriptedGui, boolean visibleWhenEmpty) {
            this(id,name,description,icon,picture,scriptedGui,visibleWhenEmpty,null);
        }
        public Category {
            for (String text : List.of(id,name,icon,picture,scriptedGui)) CountryView.text(text,256);
            CountryView.text(description,MAX_DESCRIPTION);
        }
    }
    public record Target(String kind, String id, String name, String flag) {
        public Target {
            flag = flag == null ? "" : flag;
            for (String text : List.of(kind,id,name,flag)) CountryView.text(text,256);
            switch (kind) {
                case "country" -> CountryView.tag(id);
                case "state" -> { if (!id.matches("[1-9][0-9]{0,5}") || !flag.isEmpty()) throw new IllegalArgumentException("Invalid decision target"); }
                default -> throw new IllegalArgumentException("Invalid decision target");
            }
        }
        public String suffix() { return "@" + kind.charAt(0) + ":" + id; }
    }
    public record Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
            Double cost, long remainingHours, String status, boolean enabled, String costText, String tooltip, String phase, Long totalHours,
            Target target, List<Integer> highlightStates, Boolean costMet, String progress, Integer highlightColor) {
        public static final int MAX_HIGHLIGHT_STATES = 64, MAX_HIGHLIGHT_COLOR = 63;
        public Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
                Double cost, long remainingHours, String status, boolean enabled, String costText, String tooltip, String phase, Long totalHours,
                Target target, List<Integer> highlightStates) {
            this(id,category,categoryName,categoryIcon,name,description,icon,cost,remainingHours,status,enabled,costText,tooltip,phase,totalHours,
                    target,highlightStates,null,"",null);
        }
        public Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
                Double cost, long remainingHours, String status, boolean enabled, String costText, String tooltip, String phase, Long totalHours) {
            this(id,category,categoryName,categoryIcon,name,description,icon,cost,remainingHours,status,enabled,costText,tooltip,phase,totalHours,null,List.of());
        }
        public Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
                double cost, long remainingHours, String status, boolean enabled, String costText, String tooltip, String phase, Long totalHours) {
            this(id,category,categoryName,categoryIcon,name,description,icon,Double.valueOf(cost),remainingHours,status,enabled,costText,tooltip,phase,totalHours);
        }
        public Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
                double cost, long remainingHours, String status, boolean enabled) {
            this(id,category,categoryName,categoryIcon,name,description,icon,cost,remainingHours,status,enabled,"","","",null);
        }
        public Row {
            costText = costText == null ? "" : costText;
            tooltip = tooltip == null ? "" : tooltip;
            phase = phase == null ? "" : phase;
            progress = progress == null ? "" : progress;
            if (!Set.of("","GOOD","BAD").contains(progress) || !progress.isEmpty() && !phase.equals("ACTIVE")
                    || highlightColor != null && (highlightColor < 0 || highlightColor > MAX_HIGHLIGHT_COLOR))
                throw new IllegalArgumentException("Invalid decision row state");
            highlightStates = highlightStates == null ? List.of() : List.copyOf(highlightStates);
            if (target != null && !id.endsWith(target.suffix()) || highlightStates.size() > MAX_HIGHLIGHT_STATES
                    || highlightStates.stream().anyMatch(state -> state < 1) || highlightStates.stream().distinct().count() != highlightStates.size())
                throw new IllegalArgumentException("Invalid decision target row");
            for (String text : List.of(id,category,categoryName,categoryIcon,name,icon,status)) CountryView.text(text,256);
            CountryView.text(description,MAX_DESCRIPTION);
            CountryView.text(costText,MAX_COST_TEXT);
            CountryView.text(tooltip,MAX_TOOLTIP);
            if (!Set.of("","ACTIVE","COOLDOWN","READY").contains(phase) || totalHours != null && totalHours < 0
                    || cost==null && enabled || cost!=null && (!Double.isFinite(cost) || cost < 0 || cost > 1000000) || remainingHours < 0)
                throw new IllegalArgumentException("Invalid decision row");
        }
    }
    public record View(String country, String session, long revision, String message, int page, int total,
            CountryHud hud, List<Row> rows, List<Category> categories, int pageCount) {
        public View(String country, String session, long revision, String message, int page, int total, CountryHud hud, List<Row> rows) {
            this(country,session,revision,message,page,total,hud,rows,List.of(),0);
        }
        public View {
            CountryView.tag(country); UUID.fromString(session); CountryView.text(message,512); Objects.requireNonNull(hud);
            rows = List.copyOf(rows);
            categories = categories == null ? List.of() : List.copyOf(categories);
            if (pageCount == 0) pageCount = Math.max(1,(total + PAGE_SIZE - 1) / PAGE_SIZE);
            if (revision < 0 || page < 0 || pageCount < 1 || pageCount > MAX_PAGES || page >= pageCount
                    || total < 0 || total > MAX_ROWS || rows.size() > PAGE_SIZE || rows.size() > total || categories.size() > PAGE_SIZE)
                throw new IllegalArgumentException("Invalid decision view");
            if (rows.stream().map(Row::id).distinct().count() != rows.size()) throw new IllegalArgumentException("Duplicate decision row");
            if (categories.stream().map(Category::id).distinct().count() != categories.size()) throw new IllegalArgumentException("Duplicate decision category");
        }
    }
    public record Highlight(String token, String session, long revision, String row, MapAtlasScene.Page page) implements CustomPacketPayload {
        public Highlight {
            UUID.fromString(token); UUID.fromString(session); CountryView.text(row,256); Objects.requireNonNull(page);
            if (revision < 0 || row.isEmpty()) throw new IllegalArgumentException("Invalid decision highlight");
        }
        public static final Type<Highlight> TYPE = new Type<>(Identifier.parse("hoi:decision_highlight_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Highlight> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.session,36); b.writeVarLong(p.revision); b.writeUtf(p.row,256); MapAtlasScene.Page.CODEC.encode(b,p.page); },
                b -> new Highlight(b.readUtf(36),b.readUtf(36),b.readVarLong(),b.readUtf(256),MapAtlasScene.Page.CODEC.decode(b)));
        @Override public Type<Highlight> type() { return TYPE; }
    }
    public record Response(String token, String json) implements CustomPacketPayload {
        public Response { UUID.fromString(token); CountryView.text(json,MAX_JSON); }
        public static final Type<Response> TYPE = new Type<>(Identifier.parse("hoi:decision_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Response> CODEC = StreamCodec.of(
                (b,p) -> { b.writeUtf(p.token,36); b.writeUtf(p.json,MAX_JSON); }, b -> new Response(b.readUtf(36),b.readUtf(MAX_JSON)));
        public static Response of(String token, View view) { return new Response(token,JSON.toJson(view)); }
        public View view() {
            try { return ProtocolJsonCodec.read(JSON,json,View.class); }
            catch (RuntimeException error) { throw new IllegalArgumentException("Invalid decision view",error); }
        }
        @Override public Type<Response> type() { return TYPE; }
    }
}
