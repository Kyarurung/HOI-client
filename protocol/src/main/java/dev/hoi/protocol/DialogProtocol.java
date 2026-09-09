package dev.hoi.protocol;

import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class DialogProtocol {
    private static final Gson JSON=new Gson();
    private static boolean registered;
    private DialogProtocol() {}
    public record Show(String json,boolean open) implements CustomPacketPayload {
        public Show {if(json==null||json.length()>220000)throw new IllegalArgumentException("Dialog payload too large");}
        public static final Type<Show> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","dialog_show_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Show> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.json,220000);b.writeBoolean(p.open);},b->new Show(b.readUtf(220000),b.readBoolean()));
        public static Show of(DialogView view,boolean open){return new Show(JSON.toJson(view),open);}
        public DialogView view(){return Objects.requireNonNull(JSON.fromJson(json,DialogView.class));}
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
                    || !target.matches("(?:research|focus|story-focus)/[A-Za-z0-9_:/.-]+"))
                throw new IllegalArgumentException("Invalid completion target");
        }
        public static final Type<Details> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "completion_details_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Details> CODEC = StreamCodec.of(
                (b, p) -> { b.writeUtf(p.token, 36); b.writeUtf(p.target, 256); },
                b -> new Details(b.readUtf(36), b.readUtf(256)));
        @Override public Type<Details> type() { return TYPE; }
    }
    public record MapClick() implements CustomPacketPayload {
        public static final MapClick INSTANCE=new MapClick();
        public static final Type<MapClick> TYPE=new Type<>(Identifier.fromNamespaceAndPath("hoi","map_state_click_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,MapClick> CODEC=StreamCodec.unit(INSTANCE);
        @Override public Type<MapClick> type(){return TYPE;}
    }
    public static synchronized void registerPayloadTypes() {
        if(registered)return;
        PayloadTypeRegistry.clientboundPlay().register(Show.TYPE,Show.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Close.TYPE,Close.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Details.TYPE,Details.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MapClick.TYPE,MapClick.CODEC);
        registered=true;
    }
}
