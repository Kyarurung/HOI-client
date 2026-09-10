package dev.hoi.client.map;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;


public final class AtlasSurfaceRenderChecks {
    private AtlasSurfaceRenderChecks() {}
    public static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            for (String model : java.util.List.of("building/air_base", "building/naval_base", "building/radar_station", "map/terrain/city")) {
                var id = net.minecraft.resources.Identifier.parse("hoi:models/" + model + ".json");
                try (var input = client.getResourceManager().getResourceOrThrow(id).openAsReader()) {
                    var data = JsonParser.parseReader(input).getAsJsonObject();
                    int cubes = data.getAsJsonArray("elements").size();
                    if (cubes == 0 || cubes > 100) throw new AssertionError(model + " exceeds the 100-cube budget");
                    for (var texture : data.getAsJsonObject("textures").entrySet()) {
                        var material = net.minecraft.resources.Identifier.parse(texture.getValue().getAsString());
                        var path = material.withPath("textures/" + material.getPath() + ".png");
                        if (client.getResourceManager().getResource(path).isEmpty())
                            throw new AssertionError(model + " missing material " + path);
                    }
                } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
            }
        });
        context.getInput().resizeWindow(1400,1000);
        try(var world=context.worldBuilder().adjustSettings(settings->settings.setGameMode(
                net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode.CREATIVE)).create()) {
            var server=world.getServer();
            java.util.function.Consumer<String> command=server::runCommand;
            command.accept("gamerule minecraft:send_command_feedback false");
            command.accept("time set noon");command.accept("weather clear");command.accept("gamemode spectator @a");
            context.setScreen(()->null);
            context.runOnClient(client->{client.options.fov().set(60);});

            command.accept("tp @a 6 70 -8 0 0");
            String[] shapes={"square","pentagon","circle","star"},tones={"own","enemy","neutral"};
            for(int row=0;row<3;row++)for(int col=0;col<4;col++) {
                command.accept("summon minecraft:item_display "+(col*4)+" "+(73-row*3)+" 8 {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:victory_point/"+shapes[col]+"_"+tones[row]+"\"}},item_display:\"fixed\",billboard:\"center\",transformation:{translation:[0f,0f,0f],scale:[2.4f,2.4f,2.4f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},width:6f,height:6f,view_range:8f,brightness:{block:15,sky:15}}");
            }
            context.waitTicks(30);
            var victory=context.takeScreenshot("hoi-victory-original-direction");
            checkPixels(victory,true);
            command.accept("kill @e[type=minecraft:item_display]");
            command.accept("time set midnight");
            String[] statuses={"empty","occupied","unknown","selected_empty","selected_occupied"};
            for(int row=0;row<2;row++)for(int col=0;col<statuses.length;col++) {
                String base=row==0?"naval":"air";
                command.accept("summon minecraft:item_display "+(col*3)+" "+(73-row*4)+" 8 {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:base/"+base+"_"+statuses[col]+"\"}},item_display:\"fixed\",billboard:\"center\",transformation:{translation:[0f,0f,0f],scale:[2.4f,2.4f,2.4f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},brightness:{block:15,sky:15}}");
            }
            context.waitTicks(4);context.takeScreenshot("hoi-base-empty-occupied-unknown-selected");
            command.accept("kill @e[type=minecraft:item_display]");command.accept("time set noon");
            context.runOnClient(client -> {
                AtlasSceneClient.clear();
                AtlasSceneClient.receive(new dev.hoi.protocol.AtlasSceneProtocol.Page(java.util.UUID.randomUUID(), "minecraft:overworld", 0, 1, java.util.List.of(
                    new dev.hoi.protocol.AtlasSceneProtocol.Box(0, 72, 8, 12, .2f, .2f, 0, dev.hoi.protocol.AtlasSceneProtocol.Material.DMZ),
                    new dev.hoi.protocol.AtlasSceneProtocol.Box(0, 69, 8, 12, .2f, .2f, 0, dev.hoi.protocol.AtlasSceneProtocol.Material.RED))));
            });
            command.accept("time set midnight"); context.waitTicks(5); context.takeScreenshot("hoi-dmz-emissive-night");
            context.runOnClient(client -> AtlasSceneClient.clear()); command.accept("time set noon");


            JsonObject scene;
            try(var input=AtlasSurfaceRenderChecks.class.getResourceAsStream("/atlas-client-scene.json")) {
                if(input==null)throw new AssertionError("Production atlas geometry fixture missing");
                scene=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            int size=scene.get("size").getAsInt();
            for(String mode:new String[]{"ARMY","NAVY","AIR","SUPPLY"}) {
                command.accept("kill @e[type=minecraft:block_display]");command.accept("kill @e[type=minecraft:item_display]");command.accept("kill @e[type=minecraft:text_display]");
                command.accept("fill 0 66 0 "+(size-1)+" 68 "+(size-1)+" minecraft:air");
                command.accept("fill 0 64 0 "+(size-1)+" 64 "+(size-1)+" minecraft:stone");
                var data=scene.getAsJsonObject("scenes").getAsJsonObject(mode);

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

                command.accept("tp @a 28 111 28 180 90");context.waitTicks(25);
                var screenshot=context.takeScreenshot("hoi-atlas-"+mode.toLowerCase()+"-coasts");checkPixels(screenshot,false);
                if(mode.equals("ARMY")) {
                    var city=boxes.stream().filter(b->b.material()==dev.hoi.protocol.AtlasSceneProtocol.Material.CITY).findFirst().orElseThrow();
                    double cityX=city.x()+city.sx()/2,cityZ=city.z()+city.sz()/2;
                    command.accept("tp @a "+cityX+" "+(city.y()+1)+" "+(cityZ-2.6)+" 0 35");
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
            checkRegion(context,command,"okinawa",1);
            checkRegion(context,command,"phnom-penh",0);
            checkRegion(context,command,"china-corner",0);
            checkFacilities(context,command);
            checkDistance(context,command);
            context.runOnClient(client->{client.options.fov().set(70);AtlasSceneClient.clear();});
        }
    }
    private static void checkRegion(ClientGameTestContext context,java.util.function.Consumer<String> command,String region,int routes) {
        JsonObject fixture;
        try(var input=AtlasSurfaceRenderChecks.class.getResourceAsStream("/"+region+"-client-scene.json")) {
            if(input==null)throw new AssertionError("Production geometry fixture missing: "+region);
            fixture=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        var data=fixture.getAsJsonObject("scenes").getAsJsonObject("ARMY");
        context.runOnClient(client->AtlasSceneClient.clear());
        command.accept("kill @e[type=minecraft:item_display]");command.accept("kill @e[type=minecraft:text_display]");
        command.accept("fill 0 66 0 55 68 55 minecraft:air");
        command.accept("fill 0 64 0 55 64 55 minecraft:stone");
        command.accept("fill 0 65 0 55 65 55 minecraft:water strict");
        int size=fixture.get("size").getAsInt();var blocks=new String[size*size];
        for(var entry:data.getAsJsonArray("cells")) {
            var c=entry.getAsJsonArray();blocks[c.get(1).getAsInt()*size+c.get(0).getAsInt()]=c.get(2).getAsString();
        }
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
        if(boxes.stream().filter(b->b.material()==dev.hoi.protocol.AtlasSceneProtocol.Material.RED).count()!=routes)
            throw new AssertionError("Unexpected connection count in "+region);
        context.runOnClient(client->{
            var id=java.util.UUID.randomUUID();int count=(boxes.size()+1023)/1024;
            for(int i=0;i<count;i++)AtlasSceneClient.receive(new dev.hoi.protocol.AtlasSceneProtocol.Page(id,"minecraft:overworld",i,count,boxes.subList(i*1024,Math.min((i+1)*1024,boxes.size()))));
        });
        for(var f:data.getAsJsonArray("facilities"))summonFixtureModel(command,f.getAsJsonArray(),false);
        for(var v:data.getAsJsonArray("victories")) {
            var row=v.getAsJsonArray();summonFixtureModel(command,row,true);
            double x=row.get(1).getAsDouble(),z=row.get(3).getAsDouble();
            if(boxes.stream().noneMatch(b->b.material()==dev.hoi.protocol.AtlasSceneProtocol.Material.CITY
                    &&Math.abs(b.x()+b.sx()/2-x)<.0001&&Math.abs(b.z()+b.sz()/2-z)<.0001))
                throw new AssertionError("Victory marker lacks its centered city model");
        }
        command.accept("tp @a 18 96 16 180 90");context.waitTicks(25);
        checkPixels(context.takeScreenshot("hoi-"+region+"-land-and-straits"),false);
        var point=data.getAsJsonArray("capital");double x=point.get(0).getAsDouble(),z=point.get(1).getAsDouble(),y=point.get(2).getAsDouble();
        command.accept("tp @a "+x+" "+(y+4)+" "+(z-8)+" 0 32");context.waitTicks(20);
        checkPixels(context.takeScreenshot("hoi-"+region+"-city-and-port"),false);
        context.runOnClient(client->AtlasSceneClient.clear());
        command.accept("kill @e[type=minecraft:item_display]");
    }
    private static void summonFixtureModel(java.util.function.Consumer<String> command,JsonArray row,boolean victory) {
        double size=row.get(4).getAsDouble(),offset=victory?0:size/2;
        String nbt=String.format(java.util.Locale.ROOT,
                "{item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"%s\"}},item_display:\"fixed\",billboard:\"%s\",transformation:{translation:[%sf,%sf,%sf],scale:[%sf,%sf,%sf],left_rotation:[0f,0f,0f,1f],right_rotation:%s},width:2f,height:2f,view_range:8f,brightness:{block:15,sky:15}}",
                row.get(5).getAsString(),victory?"center":"fixed",offset,offset,offset,size,size,size,victory?"[0f,1f,0f,0f]":"[0f,0f,0f,1f]");
        command.accept("summon minecraft:item_display "+row.get(1).getAsDouble()+" "+row.get(2).getAsDouble()+" "+row.get(3).getAsDouble()+" "+nbt);
    }
    private static void checkFacilities(ClientGameTestContext context,java.util.function.Consumer<String> command) {
        context.runOnClient(client->AtlasSceneClient.clear());
        command.accept("kill @e[type=minecraft:item_display]");
        command.accept("kill @e[type=minecraft:text_display]");
        command.accept("fill 76 64 76 84 64 84 minecraft:stone");
        command.accept("tp @a 80.4 65 77.4 0 25");
        context.waitTicks(20);
        var empty=context.takeScreenshot("hoi-radar-empty-ground");
        for (String model : java.util.List.of("radar_station", "air_base", "naval_base", "city")) {
        String path = model.equals("city") ? "map/terrain/city" : "building/"+model;

        command.accept("summon minecraft:item_display 80 65 80 {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:"+path+"\"}},item_display:\"fixed\",transformation:{translation:[0.4f,0.4f,0.4f],scale:[0.8f,0.8f,0.8f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},width:1.6f,height:1.6f,view_range:8f,brightness:{block:15,sky:15}}");
        context.waitTicks(20);
        var visible=context.takeScreenshot("hoi-"+model+"-grounded");
        checkPixels(visible,false);
        try {
            var before=javax.imageio.ImageIO.read(empty.toFile());
            var after=javax.imageio.ImageIO.read(visible.toFile());
            int changed=0,greenPanel=0;
            for(int y=after.getHeight()/3;y<after.getHeight()*2/3;y++)
                for(int x=after.getWidth()/3;x<after.getWidth()*2/3;x++) {
                    int a=before.getRGB(x,y),b=after.getRGB(x,y);
                    int difference=0;
                    for(int shift:new int[]{0,8,16})difference+=Math.abs(((a>>shift)&255)-((b>>shift)&255));
                    if(difference>60)changed++;
                    int red=(b>>16)&255,green=(b>>8)&255,blue=b&255;
                    if(green>red*.98&&green>blue*1.2&&green-blue>10)greenPanel++;
                }
            if(changed<150)throw new AssertionError(model+" model is invisible or buried: "+changed);
            if(model.equals("radar_station")&&greenPanel<20)throw new AssertionError("The radar antenna's green panel is missing: "+greenPanel);
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        command.accept("kill @e[type=minecraft:item_display]");
        }
    }
    private static void checkDistance(ClientGameTestContext context,java.util.function.Consumer<String> command) {
        context.runOnClient(client->{
            AtlasSceneClient.clear();
            var boxes=new java.util.ArrayList<dev.hoi.protocol.AtlasSceneProtocol.Box>();
            int index=0;
            for(var material:dev.hoi.protocol.AtlasSceneProtocol.Material.values()) {
                boxes.add(new dev.hoi.protocol.AtlasSceneProtocol.Box(4+index++*2,70,8,1,1,1,0,material));
            }
            for(int i=0;i<256;i++)boxes.add(new dev.hoi.protocol.AtlasSceneProtocol.Box(
                    512+(i%16)*32,70,512+(i/16)*32,1,1,1,0,dev.hoi.protocol.AtlasSceneProtocol.Material.BLACK));
            AtlasSceneClient.receive(new dev.hoi.protocol.AtlasSceneProtocol.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,boxes));
        });
        command.accept("tp @a 11 110 8 0 90");
        context.waitFor(client->AtlasSceneClient.visibleTileCount()==dev.hoi.protocol.AtlasSceneProtocol.Material.values().length);
        context.takeScreenshot("hoi-atlas-near-all-materials");
        context.runOnClient(client->{
            if(AtlasSceneClient.examinedTileCount()>=AtlasSceneClient.tileCount()/4)
                throw new AssertionError("Distance selection scanned distant tiles");
        });
        command.accept("tp @a 11 132 8 0 90");
        context.waitFor(client->AtlasSceneClient.visibleTileCount()==dev.hoi.protocol.AtlasSceneProtocol.Material.values().length);
        context.takeScreenshot("hoi-atlas-within-64-all-materials");
        command.accept("tp @a 11 140 8 0 90");
        context.waitFor(client->AtlasSceneClient.visibleTileCount()==0);
        context.takeScreenshot("hoi-atlas-far-hidden");
        command.accept("tp @a 11 110 8 0 90");
        context.waitFor(client->AtlasSceneClient.visibleTileCount()==dev.hoi.protocol.AtlasSceneProtocol.Material.values().length);
        context.takeScreenshot("hoi-atlas-return-near");
        context.runOnClient(client->{
            AtlasSceneClient.clear();
            var longBox=new dev.hoi.protocol.AtlasSceneProtocol.Box(-512,70,8,1024,1,1,0,dev.hoi.protocol.AtlasSceneProtocol.Material.BLACK);
            AtlasSceneClient.receive(new dev.hoi.protocol.AtlasSceneProtocol.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,java.util.List.of(longBox)));
        });
        context.waitFor(client->AtlasSceneClient.visibleTileCount()==1);
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
