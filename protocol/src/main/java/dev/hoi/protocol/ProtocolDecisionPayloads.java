package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolDecisionPayloads {
    public static final int PAGE_SIZE = 48, MAX_JSON = 160_000, MAX_PAGES = 257, MAX_ROWS = 12_288;
    public static final int MAX_DESCRIPTION = 16_000, MAX_TOOLTIP = 24_000, MAX_COST_TEXT = 4_000;
    private static final Gson JSON = new Gson();
    private static boolean registered;
    private ProtocolDecisionPayloads() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        ProtocolDecisionDetails.registerPayloadTypes();
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE, Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE, Response.CODEC);
        registered = true;
    }
    public enum Action { OPEN, REFRESH, TAKE }
    public record Request(Action action, String token, String session, long revision, String id, int page) implements CustomPacketPayload {
        public Request {
            Objects.requireNonNull(action); UUID.fromString(token);
            if (!session.isEmpty()) UUID.fromString(session);
            ProtocolCountryView.text(id, 256);
            if (revision < 0 || page < 0 || page > 256) throw new IllegalArgumentException("Invalid decision request");
        }
        public static final Type<Request> TYPE = new Type<>(Identifier.parse("hoi:decision_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC = StreamCodec.of(
                (b,p) -> { b.writeEnum(p.action); b.writeUtf(p.token,36); b.writeUtf(p.session,36); b.writeVarLong(p.revision); b.writeUtf(p.id,256); b.writeVarInt(p.page); },
                b -> new Request(b.readEnum(Action.class),b.readUtf(36),b.readUtf(36),b.readVarLong(),b.readUtf(256),b.readVarInt()));
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Category(String id, String name, String description, String icon, String picture, String scriptedGui, boolean visibleWhenEmpty,
            ProtocolDecisionPanel panel) {
        public Category(String id, String name, String description, String icon, String picture, String scriptedGui, boolean visibleWhenEmpty) {
            this(id,name,description,icon,picture,scriptedGui,visibleWhenEmpty,null);
        }
        public Category {
            for (String text : List.of(id,name,icon,picture,scriptedGui)) ProtocolCountryView.text(text,256);
            ProtocolCountryView.text(description,MAX_DESCRIPTION);
        }
    }
    public record Row(String id, String category, String categoryName, String categoryIcon, String name, String description, String icon,
            Double cost, long remainingHours, String status, boolean enabled, String costText, String tooltip, String phase, Long totalHours) {
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
            for (String text : List.of(id,category,categoryName,categoryIcon,name,icon,status)) ProtocolCountryView.text(text,256);
            ProtocolCountryView.text(description,MAX_DESCRIPTION);
            ProtocolCountryView.text(costText,MAX_COST_TEXT);
            ProtocolCountryView.text(tooltip,MAX_TOOLTIP);
            if (!Set.of("","ACTIVE","COOLDOWN","READY").contains(phase) || totalHours != null && totalHours < 0
                    || cost==null && enabled || cost!=null && (!Double.isFinite(cost) || cost < 0 || cost > 1000000) || remainingHours < 0)
                throw new IllegalArgumentException("Invalid decision row");
        }
    }
    public record View(String country, String session, long revision, String message, int page, int total,
            ProtocolCountryHud hud, List<Row> rows, List<Category> categories, int pageCount) {
        public View(String country, String session, long revision, String message, int page, int total, ProtocolCountryHud hud, List<Row> rows) {
            this(country,session,revision,message,page,total,hud,rows,List.of(),0);
        }
        public View {
            ProtocolCountryView.tag(country); UUID.fromString(session); ProtocolCountryView.text(message,512); Objects.requireNonNull(hud);
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
    public record Response(String token, String json) implements CustomPacketPayload {
        public Response { UUID.fromString(token); ProtocolCountryView.text(json,MAX_JSON); }
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
