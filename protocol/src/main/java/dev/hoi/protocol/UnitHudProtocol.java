package dev.hoi.protocol;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class UnitHudProtocol {
    public static final int MAX_ROWS=192;
    public static final double RANGE=96;
    public static final Set<String> TYPES=Set.of("infantry","motorized_infantry","mechanized_infantry","marine",
            "airborne","mountaineer","tank","light_tank","artillery","self_propelled_artillery","anti_tank","air_defense");
    private static boolean registered;
    private UnitHudProtocol() {}
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);
        registered=true;
    }
    public record Counter(String stack,String country,String countryName,String type,String name,int count,
            double x,double y,double z,double organization,double strength,double supply,Map<String,Integer> battalions) {
        public Counter {
            text(stack,128);text(country,16);text(countryName,96);text(name,96);
            if(!country.matches("[A-Z0-9_]{1,16}")||!TYPES.contains(type)||count<1||count>1_000_000)
                throw new IllegalArgumentException("Invalid unit counter");
            for(double position:new double[]{x,y,z})if(!Double.isFinite(position)||Math.abs(position)>30_000_000)
                throw new IllegalArgumentException("Invalid unit counter position");
            for(double ratio:new double[]{organization,strength,supply})if(!Double.isFinite(ratio)||ratio<0||ratio>1)
                throw new IllegalArgumentException("Invalid unit counter ratio");
            battalions=Map.copyOf(battalions);
            long total=0;
            for(var entry:battalions.entrySet()) {
                if(!TYPES.contains(entry.getKey())||entry.getValue()<1)throw new IllegalArgumentException("Invalid counter battalion");
                total+=entry.getValue();
            }
            if(total>25L*count)throw new IllegalArgumentException("Too many counter battalions");
        }
        public String key(){return stack+"|"+type;}
        public double distanceSquared(double px,double py,double pz){return (x-px)*(x-px)+(y-py)*(y-py)+(z-pz)*(z-pz);}
    }
    public record Snapshot(String dimension,List<Counter> counters) implements CustomPacketPayload {
        public Snapshot {
            text(dimension,128);
            if(Identifier.tryParse(dimension)==null)throw new IllegalArgumentException("Invalid unit counter dimension");
            counters=List.copyOf(counters);
            if(counters.size()>MAX_ROWS||counters.stream().map(Counter::key).distinct().count()!=counters.size())
                throw new IllegalArgumentException("Invalid unit counter rows");
        }
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:unit_hud_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)-> {
            b.writeUtf(p.dimension,128);b.writeVarInt(p.counters.size());
            for(var c:p.counters) {
                b.writeUtf(c.stack,128);b.writeUtf(c.country,16);b.writeUtf(c.countryName,96);b.writeUtf(c.type,32);b.writeUtf(c.name,96);
                b.writeVarInt(c.count);b.writeDouble(c.x);b.writeDouble(c.y);b.writeDouble(c.z);
                b.writeDouble(c.organization);b.writeDouble(c.strength);b.writeDouble(c.supply);
                b.writeVarInt(c.battalions.size());
                for(var entry:new TreeMap<>(c.battalions).entrySet()){b.writeUtf(entry.getKey(),32);b.writeVarInt(entry.getValue());}
            }
        },b->{
            String dimension=b.readUtf(128);int count=b.readVarInt();
            if(count<0||count>MAX_ROWS)throw new IllegalArgumentException("Too many unit counter rows");
            var rows=new ArrayList<Counter>(count);
            for(int i=0;i<count;i++) {
                String stack=b.readUtf(128),country=b.readUtf(16),countryName=b.readUtf(96),type=b.readUtf(32),name=b.readUtf(96);
                int units=b.readVarInt();double x=b.readDouble(),y=b.readDouble(),z=b.readDouble();
                double organization=b.readDouble(),strength=b.readDouble(),supply=b.readDouble();int size=b.readVarInt();
                if(size<0||size>TYPES.size())throw new IllegalArgumentException("Too many unit counter battalions");
                var battalions=new HashMap<String,Integer>();
                for(int j=0;j<size;j++)if(battalions.put(b.readUtf(32),b.readVarInt())!=null)throw new IllegalArgumentException("Duplicate counter battalion");
                rows.add(new Counter(stack,country,countryName,type,name,units,x,y,z,organization,strength,supply,battalions));
            }
            return new Snapshot(dimension,rows);
        });
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    private static void text(String text,int maximum) {
        if(text==null||text.isBlank()||text.length()>maximum)throw new IllegalArgumentException("Invalid unit counter text");
    }
}
