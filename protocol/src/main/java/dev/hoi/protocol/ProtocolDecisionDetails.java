package dev.hoi.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolDecisionDetails {
    public static final int MAX_JSON=160_000,MAX_LINES=256,MAX_TEXT=60_000,MAX_SECTIONS=6;
    public static final int MAX_LINE_TEXT=4096,MAX_DEPTH=16,MAX_ID=256,MAX_SOURCE_KEY=256;
    public static final int MAX_NAME=512,MAX_DESCRIPTION=16_384,MAX_SOURCE=512,MAX_LIMITATION=2048;
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private ProtocolDecisionDetails() {}
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE,Response.CODEC);
        registered=true;
    }
    public enum Phase {READY_REQUIREMENTS,COMPLETE,MODIFIER,REMOVE,CANCEL,TIMEOUT}
    public enum Assessment {MET,UNMET,NOT_EVALUATED,UNKNOWN}
    public record Request(String token,String session,long revision,String id) implements CustomPacketPayload {
        public Request {
            try {
                uuid(token);uuid(session);identifier(id);
                if(revision<0)throw new IllegalArgumentException("Negative revision");
            } catch(RuntimeException error) {throw invalid("request",error);}
        }
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:decision_detail_request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of(
                (buffer,value)->{buffer.writeUtf(value.token,36);buffer.writeUtf(value.session,36);buffer.writeVarLong(value.revision);buffer.writeUtf(value.id,MAX_ID);},
                buffer->new Request(buffer.readUtf(36),buffer.readUtf(36),buffer.readVarLong(),buffer.readUtf(MAX_ID)));
        @Override public Type<Request> type() {return TYPE;}
    }
    public record Line(String text,int depth,Assessment assessment,String sourceKey,boolean supported) {
        public Line {
            try {
                ProtocolCountryView.text(text,MAX_LINE_TEXT);ProtocolCountryView.text(sourceKey,MAX_SOURCE_KEY);Objects.requireNonNull(assessment);
                if(depth<0||depth>MAX_DEPTH)throw new IllegalArgumentException("Invalid depth");
            } catch(RuntimeException error) {throw invalid("line",error);}
        }
    }
    public record Section(Phase phase,List<Line> lines) {
        public Section {
            try {Objects.requireNonNull(phase);lines=ProtocolCountryView.bounded(lines,MAX_LINES);}
            catch(RuntimeException error) {throw invalid("section",error);}
        }
    }
    public record View(String country,String session,long revision,String id,String name,String description,String source,int line,
            List<Section> sections,String limitation,boolean complete) {
        public View {
            try {
                ProtocolCountryView.tag(country);uuid(session);identifier(id);
                ProtocolCountryView.text(name,MAX_NAME);ProtocolCountryView.text(description,MAX_DESCRIPTION);
                ProtocolCountryView.text(source,MAX_SOURCE);ProtocolCountryView.text(limitation,MAX_LIMITATION);
                sections=ProtocolCountryView.bounded(sections,MAX_SECTIONS);
                if(revision<0||line<1)throw new IllegalArgumentException("Invalid source or revision");
                int count=0,characters=country.length()+session.length()+id.length()+name.length()+description.length()+source.length()+limitation.length();
                for(var section:sections) {
                    count+=section.lines().size();
                    for(var row:section.lines())characters+=row.text().length()+row.sourceKey().length();
                }
                if(count>MAX_LINES||characters>MAX_TEXT)throw new IllegalArgumentException("Detail budget exceeded");
            } catch(RuntimeException error) {throw invalid("view",error);}
        }
    }
    public record Response(String token,String json) implements CustomPacketPayload {
        public Response {
            try {uuid(token);ProtocolCountryView.text(json,MAX_JSON);}
            catch(RuntimeException error) {throw invalid("response",error);}
        }
        public static final Type<Response> TYPE=new Type<>(Identifier.parse("hoi:decision_detail_response_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of(
                (buffer,value)->{buffer.writeUtf(value.token,36);buffer.writeUtf(value.json,MAX_JSON);},
                buffer->new Response(buffer.readUtf(36),buffer.readUtf(MAX_JSON)));
        public static Response of(String token,View view) {
            try {return new Response(token,JSON.toJson(Objects.requireNonNull(view)));}
            catch(RuntimeException error) {throw invalid("response",error);}
        }
        public View view() {
            try {
                var root=JsonParser.parseString(json).getAsJsonObject();
                required(root,"country","session","revision","id","name","description","source","line","sections","limitation","complete");
                booleanField(root,"complete");
                var sections=root.getAsJsonArray("sections");
                if(sections.size()>MAX_SECTIONS)throw new IllegalArgumentException("Section limit");
                int count=0;
                for(var entry:sections) {
                    var section=entry.getAsJsonObject();required(section,"phase","lines");
                    var lines=section.getAsJsonArray("lines");count+=lines.size();
                    if(count>MAX_LINES)throw new IllegalArgumentException("Line limit");
                    for(var entryLine:lines) {
                        var row=entryLine.getAsJsonObject();required(row,"text","depth","assessment","sourceKey","supported");
                        booleanField(row,"supported");
                    }
                }
                return ProtocolJsonCodec.read(JSON,json,View.class);
            } catch(RuntimeException error) {throw invalid("view",error);}
        }
        @Override public Type<Response> type() {return TYPE;}
    }
    private static void uuid(String value) {
        ProtocolCountryView.text(value,36);
        if(value.length()!=36||!UUID.fromString(value).toString().equalsIgnoreCase(value))throw new IllegalArgumentException("Invalid UUID");
    }
    private static void identifier(String value) {
        ProtocolCountryView.text(value,MAX_ID);
        if(value.isBlank())throw new IllegalArgumentException("Missing decision ID");
    }
    private static void required(JsonObject value,String... fields) {
        for(String field:fields)if(!value.has(field)||value.get(field).isJsonNull())throw new IllegalArgumentException("Missing detail field: "+field);
    }
    private static void booleanField(JsonObject value,String field) {
        if(!value.get(field).isJsonPrimitive()||!value.getAsJsonPrimitive(field).isBoolean())throw new IllegalArgumentException("Invalid detail boolean: "+field);
    }
    private static IllegalArgumentException invalid(String kind,RuntimeException cause) {return new IllegalArgumentException("Invalid decision detail "+kind,cause);}
}
