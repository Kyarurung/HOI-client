package dev.hoi.client;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Actual vanilla renderer: source VP sprites, army terrain/water, and non-army coast geometry. */
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
                    var c=entry.getAsJsonArray();command.accept("setblock "+c.get(0).getAsInt()+" 66 "+c.get(1).getAsInt()+" "+c.get(2).getAsString()+" strict");
                }
                for(var entry:data.getAsJsonArray("water")) {
                    var c=entry.getAsJsonArray();
                    for(var border:c.get(7).getAsJsonArray()) {
                        var r=border.getAsJsonArray();
                        command.accept("summon minecraft:block_display "+r.get(0)+" "+r.get(1)+" "+r.get(2)+" {block_state:{Name:\"minecraft:black_concrete\"},transformation:{translation:[0f,0f,0f],scale:["+r.get(3)+"f,"+r.get(4)+"f,"+r.get(5)+"f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},width:2f,height:2f,view_range:8f,brightness:{block:15,sky:15}}");
                    }
                    command.accept("summon minecraft:block_display "+c.get(0).getAsDouble()+" "+c.get(2).getAsDouble()+" "+c.get(1).getAsDouble()+" {block_state:"+blockState(scene.get("riverBlock").getAsString())+",transformation:{translation:[0f,0f,0f],scale:["+c.get(3).getAsDouble()+"f,"+c.get(4).getAsDouble()+"f,"+c.get(5).getAsDouble()+"f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},width:2f,height:2f,view_range:8f,brightness:{block:15,sky:15}}");
                }
                // Use the exact clipped production faces, rather than expanding centerlines
                // again in the fixture and accidentally extending caps across the coastline.
                for(var entry:data.getAsJsonArray("strokes")) {
                    var b=entry.getAsJsonArray();
                    command.accept("summon minecraft:block_display "+b.get(0)+" "+b.get(2)+" "+b.get(1)+" {block_state:{Name:\"minecraft:black_concrete\"},transformation:{translation:[0f,0f,0f],scale:["+b.get(3)+"f,"+b.get(4)+"f,"+b.get(5)+"f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},width:120f,height:4f,view_range:8f,brightness:{block:15,sky:15}}");
                }
                for(var entry:data.getAsJsonArray("lines")) {
                    var s=entry.getAsJsonArray();if(!s.get(5).getAsString().equals("CROSSING"))continue;
                    double x=s.get(0).getAsDouble(),z=s.get(1).getAsDouble();
                    double dx=s.get(2).getAsDouble()-x,dz=s.get(3).getAsDouble()-z,dy=s.get(7).getAsDouble()-s.get(6).getAsDouble(),length=Math.sqrt(dx*dx+dy*dy+dz*dz),width=s.get(4).getAsDouble();
                    if(length<.00001)continue;
                    boolean upright=dx==0&&dz==0;
                    var rotation=new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,0,1),new org.joml.Vector3f((float)dx,(float)dy,(float)dz).normalize());
                    double ox=x-dx*width/2/length-dz*width/2/length,oz=z-dz*width/2/length+dx*width/2/length;
                    if(upright){ox=x-width/2;oz=z-width/2;rotation.identity();}
                    String scale=upright?width+"f,"+(length+.025)+"f,"+width+"f":width+"f,0.025f,"+(length+width)+"f";
                    String block=s.get(5).getAsString().equals("CROSSING")?"red_concrete":"black_concrete";
                    command.accept("summon minecraft:block_display "+ox+" "+(s.get(6).getAsDouble()-(upright?0:dy*width/2/length))+" "+oz+" {block_state:{Name:\"minecraft:"+block+"\"},transformation:{translation:[0f,0f,0f],scale:["+scale+"],left_rotation:["+rotation.x+"f,"+rotation.y+"f,"+rotation.z+"f,"+rotation.w+"f],right_rotation:[0f,0f,0f,1f]},width:120f,height:2f,view_range:8f,brightness:{block:15,sky:15}}");
                }
                for(var entry:data.getAsJsonArray("terrain")) {
                    var t=entry.getAsJsonArray();terrain(command,t.get(0).getAsDouble()+.1,t.get(1).getAsDouble()+.1,t.get(2).getAsString(),t.get(3).getAsString(),t.get(4).getAsDouble());
                }
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
            context.runOnClient(client->{client.options.fov().set(70);});
        }
    }
    private static String blockState(String value) {
        int start=value.indexOf('[');
        if(start<0)return "{Name:\""+value+"\"}";
        var properties=new java.util.StringJoiner(",");
        for(String pair:value.substring(start+1,value.length()-1).split(",")) {
            var parts=pair.split("=");properties.add(parts[0]+":\""+parts[1]+"\"");
        }
        return "{Name:\""+value.substring(0,start)+"\",Properties:{"+properties+"}}";
    }
    private static void terrain(java.util.function.Consumer<String> command,double x,double z,String kind,String block,double height) {
        if(kind.equals("urban"))command.accept("summon minecraft:item_display "+x+" 66 "+z+" {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:map/terrain/city\"}},item_display:\"fixed\",transformation:{translation:[0.4f,0.4f,0.4f],scale:[0.8f,0.8f,0.8f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},width:2f,height:2f,view_range:8f}");
        else command.accept("summon minecraft:block_display "+x+" 66 "+z+" {block_state:{Name:\""+block+"\""+(kind.equals("hills")?",Properties:{type:\"bottom\"}":"")+"},transformation:{translation:[0f,0f,0f],scale:[0.8f,"+height+"f,0.8f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},width:2f,height:2f,view_range:8f}");
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
