package dev.hoi.client;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Path;
import java.util.zip.ZipFile;

/** Actual vanilla item-display rendering of the external map tiles, in a disposable fixture. */
final class AtlasSurfaceRenderChecks {
    private AtlasSurfaceRenderChecks() {}
    static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(1400,1000);
        try (var world=context.worldBuilder().adjustSettings(settings->settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server=world.getServer();
            server.runCommand("gamerule minecraft:send_command_feedback false");
            server.runCommand("fill 0 65 0 63 65 63 minecraft:green_concrete");
            server.runCommand("time set noon");
            server.runCommand("gamemode spectator @a");
            server.runCommand("tp @a 32 109 32 180 90");
            int count=0;
            try(var zip=new ZipFile(Path.of(System.getProperty("hoi.testResourcePack")).toFile())) {
                for(int z=64;z<128;z+=16)for(int x=240;x<304;x+=16) {
                    String id="hoi:map/surface/"+x+"_"+z;
                    if(zip.getEntry("assets/hoi/items/map/surface/"+x+"_"+z+".json")==null)continue;
                    server.runCommand("summon minecraft:item_display "+(x-240)+" 66.006 "+(z-64)
                            +" {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\""+id
                            +"\"}},item_display:\"fixed\",transformation:{translation:[8f,0f,8f],scale:[16f,16f,16f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},width:48f,height:1f,view_range:8f,brightness:{block:15,sky:15}}");
                    count++;
                }
            } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
            if(count<6)throw new AssertionError("Map surface fixture tiles are missing");
            context.setScreen(()->null);
            context.runOnClient(client->client.options.fov().set(60));
            context.waitTicks(40);
            Path screenshot=context.takeScreenshot("hoi-atlas-rivers-terrain");
            try {
                var image=javax.imageio.ImageIO.read(screenshot.toFile());int blue=0;
                for(int y=image.getHeight()/4;y<image.getHeight()*3/4;y++)for(int x=image.getWidth()/4;x<image.getWidth()*3/4;x++) {
                    int rgb=image.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                    if(b>100&&b>r*1.5&&b>g*1.1)blue++;
                }
                if(blue<500)throw new AssertionError("River pixels were not rendered on the transparent item surface: "+blue);
            } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
            context.runOnClient(client->client.options.fov().set(70));
        }
    }
}
