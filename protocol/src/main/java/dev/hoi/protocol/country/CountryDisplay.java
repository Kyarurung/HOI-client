package dev.hoi.protocol.country;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class CountryDisplay {
    public static final int MAX_COUNTRIES = 512;
    private static boolean registered;
    private CountryDisplay() {}
    public static synchronized void registerPayloadTypes() {
        if (registered) return;
        PayloadTypeRegistry.clientboundPlay().register(Table.TYPE, Table.CODEC);
        registered = true;
    }
    public record Entry(String tag, String name, String flag) {
        public Entry {
            if (tag == null || !tag.matches("[A-Z][A-Z0-9]{2}") || name == null || name.isBlank() || name.length() > 128
                    || flag == null || !flag.matches("[A-Za-z0-9_]{1,128}")) throw new IllegalArgumentException("Invalid country display");
        }
    }
    public record Table(List<Entry> countries) implements CustomPacketPayload {
        public Table {
            countries = List.copyOf(countries);
            if (countries.size() > MAX_COUNTRIES || countries.stream().map(Entry::tag).distinct().count() != countries.size())
                throw new IllegalArgumentException("Invalid country display table");
        }
        public Optional<Entry> country(String tag) { return countries.stream().filter(e -> e.tag().equals(tag)).findFirst(); }
        public static final Type<Table> TYPE = new Type<>(Identifier.fromNamespaceAndPath("hoi", "country_display_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Table> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.countries().size());
            for (var e : p.countries()) { b.writeUtf(e.tag(), 3); b.writeUtf(e.name(), 128); b.writeUtf(e.flag(), 128); }
        }, b -> {
            int size = b.readVarInt();
            if (size < 0 || size > MAX_COUNTRIES) throw new IllegalArgumentException("Invalid country display table");
            var countries = new ArrayList<Entry>(size);
            for (int i = 0; i < size; i++) countries.add(new Entry(b.readUtf(3), b.readUtf(128), b.readUtf(128)));
            return new Table(countries);
        });
        @Override public Type<Table> type() { return TYPE; }
    }
}
