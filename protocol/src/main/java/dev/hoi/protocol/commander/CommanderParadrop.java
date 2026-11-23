package dev.hoi.protocol.commander;

import dev.hoi.protocol.ProtocolJsonCodec;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class CommanderParadrop {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private CommanderParadrop() {}
    public record Point(double x,double z) {
        public Point {if(!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>65536||Math.abs(z)>65536)throw new IllegalArgumentException("Invalid paradrop point");}
    }
    public record Plan(String id,String army,String label,Point source,Point target,boolean active,boolean flying,String detail) {
        public Plan {
            key(id);key(army);Objects.requireNonNull(source);Objects.requireNonNull(target);
            if(label==null||label.length()>256||detail==null||detail.length()>4000)throw new IllegalArgumentException("Invalid paradrop presentation");
        }
    }
    public record View(String token,long revision,String country,String dimension,List<Plan> plans) {
        public View {
            UUID.fromString(token);if(revision<0||country==null||!country.matches("[A-Z0-9]{3}")||dimension==null||dimension.length()>128)throw new IllegalArgumentException("Invalid paradrop scope");
            plans=List.copyOf(plans);if(plans.size()>128||plans.stream().map(Plan::id).distinct().count()!=plans.size())throw new IllegalArgumentException("Invalid paradrop plans");
        }
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {if(json==null||json.length()>262144||json.getBytes(StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("Paradrop snapshot exceeds byte budget");}
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:paradrop_plans_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,262144),b->new Snapshot(b.readUtf(262144)));
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    private static void key(String value){if(value==null||!value.matches("[A-Za-z0-9_:/|.-]{1,160}"))throw new IllegalArgumentException("Invalid paradrop key");}
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);registered=true;}
}
