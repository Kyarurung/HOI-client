package dev.hoi.client;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Actual vanilla renderer: source VP sprites, terrain in all modes, army water and operational coasts. */
final class AtlasSurfaceRenderChecks {
    private AtlasSurfaceRenderChecks() {}
    static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(1400,1000);
        try(var world=context.worldBuilder().adjustSettings(settings->settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server=world.getServer();
            java.util.function.Consumer<String> command=server::runCommand;
            command.accept("gamerule minecraft:send_command_feedback false");
            command.accept("time set noon");command.accept("weather clear");command.accept("gamemode spectator @a");
            context.setScreen(()->null);
            context.runOnClient(client->{client.options.fov().set(60);});
            // Same fixed item context, billboard, half-turn correction and source sprites as the server.
            command.accept("tp @a 6 70 -8 0 0");
            String[] shapes={"square","pentagon","circle","star"},tones={"own","enemy","neutral"};
            for(int row=0;row<3;row++)for(int col=0;col<4;col++) {
                command.accept("summon minecraft:item_display "+(col*4)+" "+(73-row*3)+" 8 {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:victory_point/"+shapes[col]+"_"+tones[row]+"\"}},item_display:\"fixed\",billboard:\"center\",transformation:{translation:[0f,0f,0f],scale:[2.4f,2.4f,2.4f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},width:6f,height:6f,view_range:8f,brightness:{block:15,sky:15}}");
            }
            context.waitTicks(30);
            var victory=context.takeScreenshot("hoi-victory-original-direction");
            checkPixels(victory,true);
            command.accept("kill @e[type=minecraft:item_display]");
            JsonObject scene;
            try(var input=AtlasSurfaceRenderChecks.class.getResourceAsStream("/atlas-client-scene.json")) {
                if(input==null)throw new AssertionError("Production atlas geometry fixture missing");
                scene=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            int size=scene.get("size").getAsInt();
            for(String mode:new String[]{"ARMY","NAVY","AIR"}) {
                command.accept("kill @e[type=minecraft:block_display]");command.accept("kill @e[type=minecraft:item_display]");command.accept("kill @e[type=minecraft:text_display]");
                command.accept("fill 0 66 0 "+(size-1)+" 68 "+(size-1)+" minecraft:air");
                command.accept("fill 0 64 0 "+(size-1)+" 64 "+(size-1)+" minecraft:stone");
                var data=scene.getAsJsonObject("scenes").getAsJsonObject(mode);
                // Horizontal runs reduce command overhead while preserving every source cell.
                String[] blocks=new String[size*size];
                for(var entry:data.getAsJsonArray("cells")){var c=entry.getAsJsonArray();blocks[c.get(1).getAsInt()*size+c.get(0).getAsInt()]=c.get(2).getAsString();}
                for(int z=0;z<size;z++)for(int x=0;x<size;) {
                    int end=x+1;while(end<size&&blocks[z*size+end].equals(blocks[z*size+x]))end++;
                    command.accept("fill "+x+" 65 "+z+" "+(end-1)+" 65 "+z+" "+blocks[z*size+x]+" strict");x=end;
                }
                for(var entry:data.getAsJsonArray("relief")) {
                    var c=entry.getAsJsonArray();command.accept("setblock "+c.get(0).getAsInt()+" "+c.get(3).getAsInt()+" "+c.get(1).getAsInt()+" "+c.get(2).getAsString()+" strict");
                }
                var boxes=new java.util.ArrayList<dev.hoi.protocol.AtlasSceneProtocol.Box>();
                for(var entry:data.getAsJsonArray("meshes")) {
                    var b=entry.getAsJsonArray();boxes.add(new dev.hoi.protocol.AtlasSceneProtocol.Box(
                            b.get(0).getAsFloat(),b.get(1).getAsFloat(),b.get(2).getAsFloat(),b.get(3).getAsFloat(),b.get(4).getAsFloat(),b.get(5).getAsFloat(),b.get(6).getAsFloat(),
                            dev.hoi.protocol.AtlasSceneProtocol.Material.valueOf(b.get(7).getAsString())));
                }
                context.runOnClient(client->{
                    AtlasSceneClient.clear();var id=java.util.UUID.randomUUID();int count=(boxes.size()+1023)/1024;
                    for(int i=0;i<count;i++)AtlasSceneClient.receive(new dev.hoi.protocol.AtlasSceneProtocol.Page(id,"minecraft:overworld",i,count,boxes.subList(i*1024,Math.min((i+1)*1024,boxes.size()))));
                    if(AtlasSceneClient.tileCount()==0)throw new AssertionError("Static scene did not assemble");
                });
                // The tested borders, rivers, saplings and cities contain no display entities.
                command.accept("tp @a 28 111 28 180 90");context.waitTicks(25);
                var screenshot=context.takeScreenshot("hoi-atlas-"+mode.toLowerCase()+"-coasts");checkPixels(screenshot,false);
                if(mode.equals("ARMY")) {
                    var city=java.util.stream.StreamSupport.stream(data.getAsJsonArray("terrain").spliterator(),false)
                            .map(JsonElement::getAsJsonArray).filter(t->t.get(2).getAsString().equals("urban")).findFirst().orElseThrow();
                    double cityX=city.get(0).getAsDouble()+.5,cityZ=city.get(1).getAsDouble()+.5;
                    command.accept("tp @a "+cityX+" 66.4 "+(cityZ-2.6)+" 0 31");
                    context.waitTicks(20);checkPixels(context.takeScreenshot("hoi-city-detail"),false);
                    var step=java.util.stream.StreamSupport.stream(data.getAsJsonArray("water").spliterator(),false)
                            .map(JsonElement::getAsJsonArray).filter(w->w.get(6).getAsBoolean())
                            .filter(w->w.get(0).getAsDouble()>8&&w.get(0).getAsDouble()<size-8&&w.get(1).getAsDouble()>8&&w.get(1).getAsDouble()<size-8).findFirst().orElseThrow();
                    double wx=step.get(0).getAsDouble(),wz=step.get(1).getAsDouble(),wy=step.get(2).getAsDouble();
                    command.accept("tp @a "+wx+" "+(wy+5)+" "+(wz-7)+" 0 35");context.waitTicks(20);context.takeScreenshot("hoi-river-height-step");
                    var cap=data.getAsJsonArray("capital");
                    double cx=cap.get(0).getAsDouble(),cz=cap.get(1).getAsDouble(),cy=cap.get(2).getAsDouble();
                    command.accept("summon minecraft:item_display "+cx+" "+cy+" "+cz+" {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:victory_point/star_own\"}},item_display:\"fixed\",billboard:\"center\",transformation:{translation:[0f,0f,0f],scale:[1.4f,1.4f,1.4f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},width:2f,height:2f,view_range:8f,brightness:{block:15,sky:15}}");
                    for(var entry:data.getAsJsonArray("capitalName")) {
                        var t=entry.getAsJsonObject();
                        command.accept("summon minecraft:text_display "+cx+" "+cy+" "+cz+" "+t);
                    }
                    command.accept("tp @a "+cx+" "+(cy+5)+" "+(cz-11)+" 0 24");context.waitTicks(20);context.takeScreenshot("hoi-capital-name-terrain");
                }
            }
            context.runOnClient(client->{client.options.fov().set(70);AtlasSceneClient.clear();});
        }
    }
    private static void checkPixels(Path path,boolean victory) {
        try {
            var image=javax.imageio.ImageIO.read(path.toFile());int missing=0,green=0,red=0;
            for(int y=image.getHeight()/8;y<image.getHeight()*7/8;y++)for(int x=image.getWidth()/8;x<image.getWidth()*7/8;x++) {
                int rgb=image.getRGB(x,y),r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
                if(r>230&&b>230&&g<20)missing++;
                if(g>r*1.3&&g>b*1.15)green++;
                if(r>g*1.3&&r>b*1.3)red++;
            }
            if(missing>10)throw new AssertionError("Missing texture pixels: "+missing);
            if(victory&&(green<1500||red<1500))throw new AssertionError("Original victory sprites did not render: "+green+"/"+red);
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
}
