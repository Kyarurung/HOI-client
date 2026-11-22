package dev.hoi.client.screen.decision;

import dev.hoi.protocol.dialog.DialogView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class DecisionPreferences {
    private static DecisionPreferences current;
    private final Properties values = new Properties();
    private final Path file;
    DecisionPreferences(Path file) throws IOException {
        this.file=file;
        if(file!=null && Files.exists(file))try(var reader=Files.newBufferedReader(file,StandardCharsets.UTF_8)) { values.load(reader); }
    }
    public static DecisionPreferences current() {
        if(current==null)try { current=new DecisionPreferences(FabricLoader.getInstance().getConfigDir().resolve("hoi-decisions.properties")); }
        catch(IOException | IllegalArgumentException error) {
            org.slf4j.LoggerFactory.getLogger(DecisionPreferences.class).warn("Unable to load decision preferences",error);
            try { current=new DecisionPreferences(null); } catch(IOException impossible) { throw new IllegalStateException(impossible); }
        }
        return current;
    }
    static String server() {
        var client=Minecraft.getInstance();
        var server=client.getCurrentServer();
        if(server!=null)return server.ip;
        var integrated=client.getSingleplayerServer();
        return integrated==null?"disconnected":"local:"+integrated.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().normalize();
    }
    private static String key(String server,String country,String id) {
        int suffix=id.indexOf("@");
        String decision=suffix<0?id:id.substring(0,suffix);
        return "track."+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString((server+"\n"+country+"\n"+decision).getBytes(StandardCharsets.UTF_8));
    }
    public boolean tracked(String country,String id) { return tracking(server(),country,id); }
    public void mute(String country,java.util.Collection<String> ids) { tracking(server(),country,ids,false); }
    boolean tracking(String server,String country,String id) { return Boolean.parseBoolean(values.getProperty(key(server,country,id),"true")); }
    void tracking(String server,String country,String id,boolean enabled) { set(key(server,country,id),enabled); }
    void tracking(String server,String country,java.util.Collection<String> ids,boolean enabled) {
        for(String id:ids)values.setProperty(key(server,country,id),Boolean.toString(enabled));
        save();
    }
    boolean popup(String type) { return Boolean.parseBoolean(values.getProperty("popup."+type,type.equals("minor")?"false":"true")); }
    void popup(String type,boolean enabled) { set("popup."+type,enabled); }
    public boolean opens(DialogView view) {
        if(view.completion() || view.kind()==DialogView.Kind.SUPER_EVENT)return true;
        if(view.kind()==DialogView.Kind.GLOBAL_EVENT)return popup("news");
        if(view.kind()!=DialogView.Kind.EVENT)return true;
        return switch(view.presentation()) { case "minor_flavor" -> popup("minor"); case "", "leader_event" -> popup("events"); default -> true; };
    }
    private void set(String key,boolean enabled) {
        values.setProperty(key,Boolean.toString(enabled));
        save();
    }
    private void save() {
        if(file==null)return;
        try {
            Path parent=file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path temporary=Files.createTempFile(parent,"hoi-decisions-",".tmp");
            try {
                try(var writer=Files.newBufferedWriter(temporary,StandardCharsets.UTF_8)) { values.store(writer,null); }
                try { Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                catch(AtomicMoveNotSupportedException unsupported) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        } catch(IOException error) { org.slf4j.LoggerFactory.getLogger(DecisionPreferences.class).warn("Unable to save decision preferences",error); }
    }
}
