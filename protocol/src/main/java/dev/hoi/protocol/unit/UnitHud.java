package dev.hoi.protocol.unit;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class UnitHud {
    public static final int MAX_ROWS=192;
    public static final double RANGE=96;
    public static final Set<String> TYPES=Set.of("infantry","motorized_infantry","mechanized_infantry","marine",
            "airborne","mountaineer","tank","light_tank","artillery","self_propelled_artillery","anti_tank","air_defense",
            "special_forces","militia","light_mechanized","self_propelled_air_defense","amphibious_mechanized","recce","headquarters",
            "motorized_rocket_artillery","amphibious_armor","motorized_militia","exoskeleton_infantry");
    public static final Set<String> NAVAL_TYPES=Set.of("destroyer","light_cruiser","heavy_cruiser","battlecruiser","battleship","carrier","submarine");
    public static final Set<String> AIR_TYPES=Set.of("fighter","heavy_fighter","cas","naval_bomber","tactical_bomber","strategic_bomber","scout","transport");
    public enum Allegiance { OURS, ALLIED, ENEMY, OTHER }
    public enum Status { IDLE, MOVING, FIGHTING, TRAINING, OPERATING, RETREATING }
    private static boolean registered;
    private UnitHud() {}
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);
        registered=true;
    }
    public record Counter(String stack,String country,String countryName,String type,String name,int count,
            double x,double y,double z,double organization,double strength,double supply,Map<String,Integer> battalions,int capitals,Status status,Allegiance allegiance,Map<String,Integer> battalionMaximums) {
        public Counter(String stack,String country,String countryName,String type,String name,int count,
                double x,double y,double z,double organization,double strength,double supply,Map<String,Integer> battalions,int capitals,Status status,Allegiance allegiance) {
            this(stack,country,countryName,type,name,count,x,y,z,organization,strength,supply,battalions,capitals,status,allegiance,Map.of());
        }
        public Counter(String stack,String country,String countryName,String type,String name,int count,
                double x,double y,double z,double organization,double strength,double supply,Map<String,Integer> battalions) {
            this(stack,country,countryName,type,name,count,x,y,z,organization,strength,supply,battalions,0,Status.IDLE);
        }
        public Counter(String stack,String country,String countryName,String type,String name,int count,
                double x,double y,double z,double organization,double strength,double supply,Map<String,Integer> battalions,int capitals,Status status) {
            this(stack,country,countryName,type,name,count,x,y,z,organization,strength,supply,battalions,capitals,status,Allegiance.OTHER);
        }
        public Counter withAllegiance(Allegiance value) {
            return allegiance==value?this:new Counter(stack,country,countryName,type,name,count,x,y,z,organization,strength,supply,battalions,capitals,status,value,battalionMaximums);
        }
        public boolean navy(){return type.startsWith("navy_");}
        public boolean air(){return type.startsWith("air_");}
        public boolean unknown(){return count==0;}
        public Counter concealed() {
            if(unknown())return this;
            var minimums=new TreeMap<String,Integer>();var maximums=new TreeMap<String,Integer>();
            battalions.forEach((key,value)->{minimums.put(key,value/2);maximums.put(key,(value*3+1)/2);});
            return new Counter(stack,country,countryName,type,"?",0,x,y,z,organization,strength,0,minimums,0,Status.IDLE,allegiance,maximums);
        }
        public Counter {
            text(stack,128);text(country,16);text(countryName,96);text(name,96);
            if(!country.matches("[A-Z0-9_]{1,16}")||!(TYPES.contains(type)||type.startsWith("navy_")&&NAVAL_TYPES.contains(type.substring(5))||type.startsWith("air_")&&AIR_TYPES.contains(type.substring(4)))||count<0||count>1_000_000)
                throw new IllegalArgumentException("Invalid unit counter");
            for(double position:new double[]{x,y,z})if(!Double.isFinite(position)||Math.abs(position)>30_000_000)
                throw new IllegalArgumentException("Invalid unit counter position");
            for(double ratio:new double[]{organization,strength,supply})if(!Double.isFinite(ratio)||ratio<0||ratio>1)
                throw new IllegalArgumentException("Invalid unit counter ratio");
            Objects.requireNonNull(status);Objects.requireNonNull(allegiance);
            if(capitals<0||capitals>count||!type.startsWith("navy_")&&capitals!=0)throw new IllegalArgumentException("Invalid capital ship count");
            battalions=Map.copyOf(battalions);
            battalionMaximums=battalionMaximums==null?Map.of():Map.copyOf(battalionMaximums);
            if(count==0&&(type.startsWith("navy_")||type.startsWith("air_")||!name.equals("?")||supply!=0||status!=Status.IDLE||!battalionMaximums.keySet().equals(battalions.keySet())))
                throw new IllegalArgumentException("Unknown counter exposes private data");
            if(count>0&&!battalionMaximums.isEmpty())throw new IllegalArgumentException("Known counter has estimates");
            if(!TYPES.contains(type)&&!battalions.isEmpty())throw new IllegalArgumentException("Non-army battalions");
            long total=0;
            for(var entry:battalions.entrySet()) {
                if(!TYPES.contains(entry.getKey())||entry.getValue()<(count==0?0:1))throw new IllegalArgumentException("Invalid counter battalion");
                if(count==0&&(battalionMaximums.get(entry.getKey())<=entry.getValue()||battalionMaximums.get(entry.getKey())>37_500_000))throw new IllegalArgumentException("Invalid battalion estimate");
                total+=entry.getValue();
            }
            if(total>(count==0?12_500_000:25L*count))throw new IllegalArgumentException("Too many counter battalions");
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
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:unit_hud_v5"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)-> {
            b.writeUtf(p.dimension,128);b.writeVarInt(p.counters.size());
            for(var c:p.counters) {
                b.writeUtf(c.stack,128);b.writeUtf(c.country,16);b.writeUtf(c.countryName,96);b.writeUtf(c.type,32);b.writeUtf(c.name,96);
                b.writeVarInt(c.count);b.writeDouble(c.x);b.writeDouble(c.y);b.writeDouble(c.z);
                b.writeDouble(c.organization);b.writeDouble(c.strength);b.writeDouble(c.supply);b.writeVarInt(c.capitals);b.writeEnum(c.status);b.writeEnum(c.allegiance);
                b.writeVarInt(c.battalions.size());
                for(var entry:new TreeMap<>(c.battalions).entrySet()){b.writeUtf(entry.getKey(),32);b.writeVarInt(entry.getValue());if(c.unknown())b.writeVarInt(c.battalionMaximums.get(entry.getKey()));}
            }
        },b->{
            String dimension=b.readUtf(128);int count=b.readVarInt();
            if(count<0||count>MAX_ROWS)throw new IllegalArgumentException("Too many unit counter rows");
            var rows=new ArrayList<Counter>(count);
            for(int i=0;i<count;i++) {
                String stack=b.readUtf(128),country=b.readUtf(16),countryName=b.readUtf(96),type=b.readUtf(32),name=b.readUtf(96);
                int units=b.readVarInt();double x=b.readDouble(),y=b.readDouble(),z=b.readDouble();
                double organization=b.readDouble(),strength=b.readDouble(),supply=b.readDouble();int capitals=b.readVarInt();var status=b.readEnum(Status.class);var allegiance=b.readEnum(Allegiance.class);int size=b.readVarInt();
                if(size<0||size>TYPES.size())throw new IllegalArgumentException("Too many unit counter battalions");
                var battalions=new HashMap<String,Integer>();var maximums=new HashMap<String,Integer>();
                for(int j=0;j<size;j++) {
                    String key=b.readUtf(32);
                    if(battalions.put(key,b.readVarInt())!=null)throw new IllegalArgumentException("Duplicate counter battalion");
                    if(units==0)maximums.put(key,b.readVarInt());
                }
                rows.add(new Counter(stack,country,countryName,type,name,units,x,y,z,organization,strength,supply,battalions,capitals,status,allegiance,maximums));
            }
            return new Snapshot(dimension,rows);
        });
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    private static void text(String text,int maximum) {
        if(text==null||text.isBlank()||text.length()>maximum)throw new IllegalArgumentException("Invalid unit counter text");
    }
}
