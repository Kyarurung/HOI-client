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

public final class CommanderNavalInvasion {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private CommanderNavalInvasion() {}
    public enum Action { RESERVE, UNRESERVE, LAUNCH, ABORT }
    public record Point(double x,double z) {
        public Point {if(!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>65536||Math.abs(z)>65536)throw new IllegalArgumentException("Invalid invasion point");}
    }
    public record Plan(String id,String army,String configuration,String phase,Point departure,Point target,List<Action> actions,boolean ready,String detail) {
        public Plan {
            key(id);key(army);Objects.requireNonNull(departure);Objects.requireNonNull(target);
            if(configuration==null||!configuration.matches("[0-9a-f]{64}")||!Set.of("DRAFT","RESERVED","TRANSIT","LANDING","RETURNING","COMPLETED","CANCELLED").contains(phase))throw new IllegalArgumentException("Invalid invasion control");
            actions=List.copyOf(actions);if(actions.size()>2||new HashSet<>(actions).size()!=actions.size())throw new IllegalArgumentException("Invalid invasion actions");
            var allowed=switch(phase){case "DRAFT"->List.of(Action.RESERVE);case "RESERVED"->List.of(Action.LAUNCH,Action.UNRESERVE);case "TRANSIT","LANDING"->List.of(Action.ABORT);default->List.<Action>of();};
            if(!allowed.equals(actions)||detail==null||detail.length()>4000)throw new IllegalArgumentException("Invalid invasion presentation");
        }
    }
    public record View(String token,long revision,String country,String dimension,List<Plan> plans) {
        public View {
            UUID.fromString(token);if(revision<0||country==null||!country.matches("[A-Z0-9]{3}")||dimension==null||dimension.length()>128)throw new IllegalArgumentException("Invalid invasion scope");
            plans=List.copyOf(plans);if(plans.size()>128||plans.stream().map(Plan::id).distinct().count()!=plans.size())throw new IllegalArgumentException("Invalid invasion plans");
        }
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public Snapshot {if(json==null||json.length()>262144||json.getBytes(StandardCharsets.UTF_8).length>262144)throw new IllegalArgumentException("Invasion snapshot exceeds byte budget");}
        public static Snapshot of(View view){return new Snapshot(JSON.toJson(view));}
        public View view(){return ProtocolJsonCodec.read(JSON,json,View.class);}
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.parse("hoi:naval_invasion_controls_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,262144),b->new Snapshot(b.readUtf(262144)));
        @Override public Type<Snapshot> type(){return TYPE;}
    }
    private static void key(String value){if(value==null||!value.matches("[A-Za-z0-9_:/|.-]{1,160}"))throw new IllegalArgumentException("Invalid invasion key");}
    public static synchronized void registerPayloadTypes(){if(registered)return;PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE,Snapshot.CODEC);registered=true;}
}
