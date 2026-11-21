package dev.hoi.protocol.dialog;

import dev.hoi.protocol.ProtocolJsonCodec;
import dev.hoi.protocol.commander.CommanderHudPayloads;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class DialogPayloads {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private DialogPayloads() {}
    public record Show(String json,boolean open) implements CustomPacketPayload {
        public Show {if(json==null||json.length()>220000)throw new IllegalArgumentException("Dialog payload too large");}
        public static final Type<Show> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","dialog_show_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Show> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.json,220000);b.writeBoolean(p.open);},b->new Show(b.readUtf(220000),b.readBoolean()));
        public static Show of(DialogView view,boolean open){return new Show(JSON.toJson(view),open);}
        public DialogView view() { return ProtocolJsonCodec.read(JSON, json, DialogView.class); }
        @Override public Type<Show> type(){return TYPE;}
    }
    public record Close(String token) implements CustomPacketPayload {
        public Close {UUID.fromString(token);}
        public static final Type<Close> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","dialog_close_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Close> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.token,36),b->new Close(b.readUtf(36)));
        @Override public Type<Close> type(){return TYPE;}
    }
    public record Action(String token,long revision,String choice) implements CustomPacketPayload {
        public Action {UUID.fromString(token);if(revision<0||choice==null||choice.length()>160)throw new IllegalArgumentException("Invalid dialog action");}
        public static final Type<Action> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","dialog_action_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.token,36);b.writeVarLong(p.revision);b.writeUtf(p.choice,160);},b->new Action(b.readUtf(36),b.readVarLong(),b.readUtf(160)));
        @Override public Type<Action> type(){return TYPE;}
    }
    public record Details(String token, String target) implements CustomPacketPayload {
        public Details {
            UUID.fromString(token);
            if (target == null || target.length() > 256
                    || !target.matches("(?:research|focus|story-focus|source-focus)/[A-Za-z0-9_:/.-]+"))
                throw new IllegalArgumentException("Invalid completion target");
        }
        public static final Type<Details> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "completion_details_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Details> CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.token, 36); b.writeUtf(p.target, 256); },
                b -> new Details(b.readUtf(36), b.readUtf(256)));
        @Override public Type<Details> type() { return TYPE; }
    }
    public record PoliticsOpen(String section, int slot) implements CustomPacketPayload {
        public PoliticsOpen {
            if (!Set.of("government", "economic_laws", "military_laws", "social_laws", "development", "military_staff",
                    "officer_army", "officer_navy", "officer_air", "officer_high_command", "officer_preferred_tactic").contains(section)
                    || slot < 0 || slot >= switch (section) {
                        case "officer_army", "officer_navy" -> 4;
                        case "officer_air", "officer_high_command" -> 3;
                        case "officer_preferred_tactic" -> 1;
                        default -> 6;
                    }) throw new IllegalArgumentException("Invalid politics slot");
        }
        public static final Type<PoliticsOpen> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "politics_open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PoliticsOpen> CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.section, 24); b.writeVarInt(p.slot); }, b -> new PoliticsOpen(b.readUtf(24), b.readVarInt()));
        @Override public Type<PoliticsOpen> type() { return TYPE; }
    }
    public record MapClick(double x, double y, double z, int modifiers) implements CustomPacketPayload {
        public static final int SHIFT=1, CONTROL=2;
        public static final MapClick INSTANCE=new MapClick(0,0,0);
        public MapClick {
            double length=x*x+y*y+z*z;
            if(!Double.isFinite(length)||length!=0&&Math.abs(length-1)>.001||modifiers<0||modifiers>(SHIFT|CONTROL))throw new IllegalArgumentException("Invalid map ray");
        }
        public MapClick(double x, double y, double z){this(x,y,z,0);}
        public static final Type<MapClick> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","map_state_click_v3"));
        public static final StreamCodec<RegistryFriendlyByteBuf,MapClick> CODEC=StreamCodec.of(
                (b,p)->{b.writeDouble(p.x());b.writeDouble(p.y());b.writeDouble(p.z());b.writeByte(p.modifiers());},b->new MapClick(b.readDouble(),b.readDouble(),b.readDouble(),b.readByte()));
        @Override public Type<MapClick> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        CommanderHudPayloads.registerPayloadTypes();
        dev.hoi.protocol.map.MapSupplyHover.registerPayloadTypes();
        PayloadTypeRegistry.clientboundPlay().register(Show.TYPE,Show.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Close.TYPE,Close.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Details.TYPE,Details.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MapClick.TYPE,MapClick.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PoliticsOpen.TYPE,PoliticsOpen.CODEC);
        registered=true;
    }
}
