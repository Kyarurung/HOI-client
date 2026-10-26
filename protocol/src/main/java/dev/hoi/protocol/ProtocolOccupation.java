package dev.hoi.protocol;

import com.google.gson.Gson;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ProtocolOccupation {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private ProtocolOccupation() {}
    public static final int RESISTANCE_COLOR=0xFFDED331,COMPLIANCE_COLOR=0xFF55AAC9;
    public record Overlay(ProtocolAtlasScene.Page page) implements CustomPacketPayload {
        public Overlay {Objects.requireNonNull(page);}
        public static final Type<Overlay> TYPE=new Type<>(Identifier.parse("hoi:occupation_overlay_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Overlay> CODEC=StreamCodec.of((b,p)->ProtocolAtlasScene.Page.CODEC.encode(b,p.page()),b->new Overlay(ProtocolAtlasScene.Page.CODEC.decode(b)));
        @Override public Type<Overlay> type(){return TYPE;}
    }
    public record Effect(String id,String name,String icon,double threshold,boolean active,String tooltip) {
        public Effect {text(id,80);text(name,160);text(icon,180);text(tooltip,4000);ratio(threshold);}
    }
    public record Economy(Double population,Double manpower,double resources,int civilian,int military,Map<String,Double> resourceAmounts) {
        public Economy(Double population,Double manpower,double resources,int civilian,int military){this(population,manpower,resources,civilian,military,Map.of());}
        public Economy {resourceAmounts=resourceAmounts==null?Map.of():Map.copyOf(resourceAmounts);if(resourceAmounts.size()>16)throw new IllegalArgumentException("Resource limit");resourceAmounts.forEach((k,v)->{text(k,80);positive(v);});if(population!=null)positive(population);if(manpower!=null)positive(manpower);positive(resources);if(civilian<0||military<0)throw new IllegalArgumentException("Occupation factories");}
    }
    public record Details(int totalStates,double factoryShare,double resourceShare,Double divisions,double coverage,Economy economy,boolean lawOverride) {
        public Details(int totalStates,double factoryShare,double resourceShare,Double divisions,double coverage,Economy economy){this(totalStates,factoryShare,resourceShare,divisions,coverage,economy,false);}
        public Details(int totalStates,double factoryShare,double resourceShare,Double divisions,double coverage){this(totalStates,factoryShare,resourceShare,divisions,coverage,null);}
        public Details {if(totalStates<0)throw new IllegalArgumentException("Occupation state total");positive(factoryShare);positive(resourceShare);ratio(coverage);if(divisions!=null)positive(divisions);}
    }
    public record Administration(boolean resistance,boolean returnTerritory,boolean enabled,String tooltip,String confirm) {
        public Administration(boolean resistance,boolean returnTerritory,boolean enabled,String tooltip) {this(resistance,returnTerritory,enabled,tooltip,"");}
        public Administration {text(tooltip,6000);confirm=confirm==null?"":confirm;text(confirm,6000);}
    }
    public record Row(String state,String name,String country,String countryName,double resistance,double compliance,
            double garrison,int factories,String template,String law,String resistanceTooltip,String complianceTooltip,
            List<Effect> resistanceEffects,List<Effect> complianceEffects,double x,double y,double z,Details details,Administration administration) {
        public Row(String state,String name,String country,String countryName,double resistance,double compliance,double garrison,int factories,String template,String law,String resistanceTooltip,String complianceTooltip,List<Effect> resistanceEffects,List<Effect> complianceEffects,double x,double y,double z,Details details){this(state,name,country,countryName,resistance,compliance,garrison,factories,template,law,resistanceTooltip,complianceTooltip,resistanceEffects,complianceEffects,x,y,z,details,null);}
        public boolean hasResistance(){return administration==null||administration.resistance();}
        public Row(String state,String name,String country,String countryName,double resistance,double compliance,double garrison,int factories,String template,String law,String resistanceTooltip,String complianceTooltip,List<Effect> resistanceEffects,List<Effect> complianceEffects,double x,double y,double z){this(state,name,country,countryName,resistance,compliance,garrison,factories,template,law,resistanceTooltip,complianceTooltip,resistanceEffects,complianceEffects,x,y,z,null);}
        public Row {
            text(state,160);text(name,160);text(country,16);text(countryName,160);text(template,160);text(law,80);
            text(resistanceTooltip,6000);text(complianceTooltip,6000);ratio(resistance);ratio(compliance);
            if(!Double.isFinite(garrison)||garrison<0||factories<0)throw new IllegalArgumentException("Invalid occupation totals");
            for(double coordinate:new double[]{x,y,z})if(!Double.isFinite(coordinate)||Math.abs(coordinate)>30_000_000)throw new IllegalArgumentException("Invalid occupation position");
            resistanceEffects=List.copyOf(resistanceEffects);complianceEffects=List.copyOf(complianceEffects);
            if(resistanceEffects.size()>16||complianceEffects.size()>16)throw new IllegalArgumentException("Occupation effect limit");
        }
    }
    public record Option(String id,String name,String icon,String tooltip,boolean enabled,List<String> columns,Map<String,String> comparisons) {
        public Option(String id,String name,String icon,String tooltip,boolean enabled,List<String> columns){this(id,name,icon,tooltip,enabled,columns,Map.of());}
        public Option(String id,String name,String icon,String tooltip,boolean enabled){this(id,name,icon,tooltip,enabled,List.of());}
        public Option {comparisons=comparisons==null?Map.of():Map.copyOf(comparisons);if(comparisons.size()>16)throw new IllegalArgumentException("Occupation comparisons");comparisons.forEach((k,v)->{text(k,80);text(v,6000);});text(id,160);text(name,160);text(icon,180);text(tooltip,6000);columns=columns==null?List.of():List.copyOf(columns);if(columns.size()>3)throw new IllegalArgumentException("Occupation option columns");columns.forEach(c->text(c,40));}
    }
    public record View(String token,long revision,String country,List<Row> rows,List<Option> templates,List<Option> laws,
            String defaultTemplate,String defaultLaw,String message,Map<String,String> labels) {
        public View(String token,long revision,String country,List<Row> rows,List<Option> templates,List<Option> laws,String defaultTemplate,String defaultLaw,String message){this(token,revision,country,rows,templates,laws,defaultTemplate,defaultLaw,message,Map.of());}
        public View {
            UUID.fromString(token);if(revision<0)throw new IllegalArgumentException("Invalid occupation revision");
            text(country,16);text(defaultTemplate,160);text(defaultLaw,80);text(message,600);
            rows=List.copyOf(rows);templates=List.copyOf(templates);laws=List.copyOf(laws);
            labels=labels==null?Map.of():Map.copyOf(labels);if(labels.size()>256)throw new IllegalArgumentException("Occupation label limit");labels.forEach((k,v)->{text(k,160);text(v,6000);});
            if(rows.size()>2048||templates.size()>256||laws.size()>64)throw new IllegalArgumentException("Occupation view limit");
        }
    }
    public enum Action { OPEN, REFRESH, CLOSE, LAW, TEMPLATE, RESET, RESISTANCE, COMPLIANCE, RETURN }
    public record Request(Action action,String token,long revision,String state,String value) implements CustomPacketPayload {
        public Request {Objects.requireNonNull(action);UUID.fromString(token);if(revision<0)throw new IllegalArgumentException("Invalid occupation revision");text(state,160);text(value,160);}
        public static final Type<Request> TYPE=new Type<>(Identifier.parse("hoi:occupation_request_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{b.writeEnum(p.action());b.writeUtf(p.token(),36);b.writeVarLong(p.revision());b.writeUtf(p.state(),160);b.writeUtf(p.value(),160);},b->new Request(b.readEnum(Action.class),b.readUtf(36),b.readVarLong(),b.readUtf(160),b.readUtf(160)));
        @Override public Type<Request> type(){return TYPE;}
    }
    public record Response(String json) implements CustomPacketPayload {
        public Response {text(json,1_000_000);}
        public static final Type<Response> TYPE=new Type<>(Identifier.parse("hoi:occupation_response_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Response> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json(),1_000_000),b->new Response(b.readUtf(1_000_000)));
        public static Response of(View view){return new Response(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        @Override public Type<Response> type(){return TYPE;}
    }
    public record MapView(String dimension,boolean compliance,List<Row> rows) {
        public MapView {text(dimension,128);rows=List.copyOf(rows);if(rows.size()>2048)throw new IllegalArgumentException("Occupation map limit");}
    }
    public record MapResponse(String json) implements CustomPacketPayload {
        public MapResponse {text(json,1_000_000);}
        public static final Type<MapResponse> TYPE=new Type<>(Identifier.parse("hoi:occupation_map_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,MapResponse> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json(),1_000_000),b->new MapResponse(b.readUtf(1_000_000)));
        public static MapResponse of(MapView view){return new MapResponse(JSON.toJson(view));}
        public MapView view(){return ProtocolJsonCodec.read(JSON,json,MapView.class);}
        @Override public Type<MapResponse> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Response.TYPE,Response.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MapResponse.TYPE,MapResponse.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Overlay.TYPE,Overlay.CODEC);
        registered=true;
    }
    private static void ratio(double n){if(!Double.isFinite(n)||n<0||n>1)throw new IllegalArgumentException("Invalid occupation ratio");}
    private static void positive(double n){if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid occupation amount");}
    private static void text(String value,int max){if(value==null||value.length()>max)throw new IllegalArgumentException("Occupation text limit");}
}
