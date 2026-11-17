package dev.hoi.client.map.scene;

import dev.hoi.client.map.commander.CommanderMissionOverlayRenderTest;
import dev.hoi.client.map.commander.CommanderUnitHud;
import com.google.gson.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

public final class SceneSurfaceRenderChecks {
    private SceneSurfaceRenderChecks() {}
    private static void landPlanOverlay(ClientGameTestContext context,java.util.function.Consumer<String> command) {
        String fixture=System.getenv("HOI_LAND_PLAN_PREVIEW");if(fixture==null||fixture.isBlank())return;
        java.util.List<dev.hoi.protocol.map.MapAtlasScene.Box> boxes;
        try(var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of(fixture))) {
            boxes=java.util.List.of(new Gson().fromJson(reader,dev.hoi.protocol.map.MapAtlasScene.Box[].class));
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        if(boxes.isEmpty())throw new AssertionError("Original plan geometry fixture must not be empty");
        double maximumX=boxes.stream().mapToDouble(b->b.x()+b.sz()+b.sx()).max().orElseThrow(),maximumZ=boxes.stream().mapToDouble(b->b.z()+b.sz()+b.sx()).max().orElseThrow();
        int size=(int)Math.ceil(Math.max(maximumX,maximumZ))+4;if(size>128)throw new AssertionError("Plan render fixture must stay local");
        command.accept("fill 0 64 0 "+size+" 64 "+size+" minecraft:stone strict");
        command.accept("tp @a "+size/2.0+" "+(65+size*.9)+" "+size/2.0+" 0 90");
        context.runOnClient(client->{
            SceneRenderer.clear();dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.HIDDEN);
            SceneRenderer.receivePlans("",new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,boxes));
        });
        context.waitFor(client->SceneRenderer.visibleTileCount()>0);context.waitTicks(5);var screenshot=context.takeScreenshot("hoi-land-plan-persistent-front-arrow");
        try {
            var picture=javax.imageio.ImageIO.read(screenshot.toFile());int green=0;
            for(int y=0;y<picture.getHeight();y++)for(int x=0;x<picture.getWidth();x++){int pixel=picture.getRGB(x,y),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;if(g>70&&g>r*1.3&&g>b*1.15)green++;}
            if(green<20)throw new AssertionError("Persistent front and objective arrow did not render: "+green);
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        context.runOnClient(client->SceneRenderer.receivePlans("PRK",new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,java.util.List.of())));
        context.waitTicks(2);context.runOnClient(client->{if(SceneRenderer.visibleTileCount()==0)throw new AssertionError("Foreign plan update must not clear owned geometry");});
        try(var reader=java.nio.file.Files.newBufferedReader(java.nio.file.Path.of(fixture).resolveSibling("land-plan-preview.json"))) {
            var preview=java.util.List.of(new Gson().fromJson(reader,dev.hoi.protocol.map.MapAtlasScene.Box[].class));
            context.runOnClient(client->{if(client.options.guiScale().get()!=2||client.getWindow().getGuiScale()!=2)throw new AssertionError("Plan preview capture requires actual GUI scale 2");SceneRenderer.receivePlans("",new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,preview));});
            context.waitTicks(5);var picture=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-land-plan-edit-draft-gui2").toFile());int blue=0;
            for(int y=0;y<picture.getHeight();y++)for(int x=0;x<picture.getWidth();x++){int pixel=picture.getRGB(x,y),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;if(b>80&&b>r*1.4&&b>g*1.2)blue++;}
            if(blue<20)throw new AssertionError("Uncommitted blue plan preview did not render: "+blue);
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        context.runOnClient(client->SceneRenderer.receivePlans("",new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,java.util.List.of())));
        context.waitFor(client->SceneRenderer.visibleTileCount()==0);context.takeScreenshot("hoi-land-plan-deleted-clears-map");
        context.runOnClient(client->SceneRenderer.clearPlans());command.accept("fill 0 64 0 "+size+" 64 "+size+" minecraft:air strict");
    }
    private static void constructionPalette(ClientGameTestContext context,java.util.function.Consumer<String> command) {
        String fixture=System.getenv("HOI_CONSTRUCTION_PALETTE_PREVIEW");if(fixture==null||fixture.isBlank())return;
        try {
            var samples=JsonParser.parseString(java.nio.file.Files.readString(Path.of(fixture))).getAsJsonArray();
            command.accept("tp @a 6 73 -5 0 0");
            int column=0;
            for(var value:samples) {
                var sample=value.getAsJsonObject();String phase=sample.get("phase").getAsString();
                if(!java.util.Set.of("reserved","reserved-full","completed-full").contains(phase))continue;
                command.accept("summon minecraft:block_display "+(column++*4)+" 72 8 {block_state:"+sample.get("block")+",transformation:{translation:[0f,0f,0f],scale:[3f,3f,1f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f]},brightness:{block:15,sky:15}}");
            }
            if(column!=3)throw new AssertionError("Construction preview needs actual reserved, reserved-full and paid completed states");
            context.waitTicks(12);var picture=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-construction-reserved-reserved-full-completed-full-gui2").toFile());
            int yellow=0,blue=0;
            for(int y=picture.getHeight()/4;y<picture.getHeight()*3/4;y++)for(int x=0;x<picture.getWidth();x++) {
                int pixel=picture.getRGB(x,y),r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
                if(r>100&&g>80&&r>b*1.5&&g>b*1.5)yellow++;
                if(b>50&&b>r*2&&b>g*1.5)blue++;
            }
            if(yellow<100||blue<100)throw new AssertionError("Actual construction reservation/completion colours did not render: "+yellow+"/"+blue);
            command.accept("kill @e[type=minecraft:block_display]");
        }catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
    }
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
            context.runOnClient(client->{client.options.fov().set(60);client.options.guiScale().set(2);});
            CommanderMissionOverlayRenderTest.run(context,command);
            landPlanOverlay(context,command);
            constructionPalette(context,command);
            countryNamesAboveWater(context,command);
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
                String status=row==0&&statuses[col].equals("selected_empty")?"empty":statuses[col];
                command.accept("summon minecraft:item_display "+(col*3)+" "+(73-row*4)+" 8 {item:{id:\"minecraft:paper\",count:1,components:{\"minecraft:item_model\":\"hoi:base/"+base+"_"+status+"\"}},item_display:\"fixed\",billboard:\"center\",transformation:{translation:[0f,0f,0f],scale:[2.4f,2.4f,2.4f],left_rotation:[0f,0f,0f,1f],right_rotation:[0f,1f,0f,0f]},brightness:{block:15,sky:15}}");
            }
            context.waitTicks(4);context.takeScreenshot("hoi-base-empty-occupied-unknown-selected");
            command.accept("kill @e[type=minecraft:item_display]");command.accept("time set noon");
            context.runOnClient(client -> {
                SceneRenderer.clear();
                SceneRenderer.receive(new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(), "minecraft:overworld", 0, 1, java.util.List.of(
                    new dev.hoi.protocol.map.MapAtlasScene.Box(0, 72, 8, 12, .2f, .2f, 0, dev.hoi.protocol.map.MapAtlasScene.Material.DMZ),
                    new dev.hoi.protocol.map.MapAtlasScene.Box(0, 69, 8, 12, .2f, .2f, 0, dev.hoi.protocol.map.MapAtlasScene.Material.RED))));
            });
            command.accept("time set midnight"); context.waitTicks(5); context.takeScreenshot("hoi-dmz-emissive-night");
            context.runOnClient(client -> SceneRenderer.clear()); command.accept("time set noon");
            for(var material:java.util.List.of(dev.hoi.protocol.map.MapAtlasScene.Material.RESISTANCE,dev.hoi.protocol.map.MapAtlasScene.Material.COMPLIANCE)) {
                context.runOnClient(client->{
                    var stripes=new java.util.ArrayList<dev.hoi.protocol.map.MapAtlasScene.Box>();
                    for(int z=0;z<16;z++)for(int x=0;x<16;x++)if((x+z)%8<3)
                        stripes.add(new dev.hoi.protocol.map.MapAtlasScene.Box(x,70.03f,z,1,.015f,1,0,material));
                    SceneRenderer.receiveOccupation(new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,stripes));
                });
                command.accept("tp @a 8 90 8 0 90");
                context.waitFor(client->SceneRenderer.visibleTileCount()>0);
                context.takeScreenshot("hoi-occupation-stripes-"+material.name().toLowerCase(java.util.Locale.ROOT));
                context.runOnClient(client->SceneRenderer.receiveOccupation(new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,java.util.List.of())));
                context.waitFor(client->SceneRenderer.visibleTileCount()==0);
            }
            context.runOnClient(client->SceneRenderer.clearOccupation());
            JsonObject scene;
            try(var input=SceneSurfaceRenderChecks.class.getResourceAsStream("/atlas-client-scene.json")) {
                if(input==null)throw new AssertionError("Production atlas geometry fixture missing");
                scene=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            var counts=new java.util.HashMap<Integer,Integer>();
            for(var entry:scene.getAsJsonArray("provinces")) {
                var cell=entry.getAsJsonArray();int id=cell.get(2).getAsInt();counts.merge(id,1,Integer::sum);
                if(id==13692&&cell.get(4).getAsBoolean())throw new AssertionError("Seoul must be inland in current fixture");
                if(id==7221&&!cell.get(4).getAsBoolean())throw new AssertionError("Incheon must retain its coast");
            }
            if(counts.getOrDefault(7221,0)!=4||counts.getOrDefault(13692,0)!=8||counts.getOrDefault(7125,0)!=6)
                throw new AssertionError("Outdated Korean province fixture");
            for(var mode:scene.getAsJsonObject("scenes").entrySet()) {
                var capital=mode.getValue().getAsJsonObject().getAsJsonArray("capital");
                if(Math.abs(capital.get(0).getAsDouble()-20.5)>.001||Math.abs(capital.get(1).getAsDouble()-24.5)>.001)
                    throw new AssertionError("Seoul city and capital must move one cell north within Seoul");
                boolean frontCity=false;
                for(var marker:mode.getValue().getAsJsonObject().getAsJsonArray("terrain")) {
                    var row=marker.getAsJsonArray();
                    if(row.get(0).getAsInt()==20&&row.get(1).getAsInt()==23&&row.get(2).getAsString().equals("urban"))frontCity=true;
                }
                if(!frontCity)throw new AssertionError("The front province city must move south to the cell immediately north of Seoul");
            }
            int size=scene.get("size").getAsInt();
            for(String mode:new String[]{"ARMY","NAVY","AIR","SUPPLY","RESOURCES","FACTIONS"}) {
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
                var boxes=new java.util.ArrayList<dev.hoi.protocol.map.MapAtlasScene.Box>();
                for(var entry:data.getAsJsonArray("meshes")) {
                    var b=entry.getAsJsonArray();boxes.add(new dev.hoi.protocol.map.MapAtlasScene.Box(
                            b.get(0).getAsFloat(),b.get(1).getAsFloat(),b.get(2).getAsFloat(),b.get(3).getAsFloat(),b.get(4).getAsFloat(),b.get(5).getAsFloat(),b.get(6).getAsFloat(),
                            dev.hoi.protocol.map.MapAtlasScene.Material.valueOf(b.get(7).getAsString())));
                }
                context.runOnClient(client->{
                    SceneRenderer.clear();var id=java.util.UUID.randomUUID();int count=(boxes.size()+1023)/1024;
                    for(int i=0;i<count;i++)SceneRenderer.receive(new dev.hoi.protocol.map.MapAtlasScene.Page(id,"minecraft:overworld",i,count,boxes.subList(i*1024,Math.min((i+1)*1024,boxes.size()))));
                    if(SceneRenderer.tileCount()==0)throw new AssertionError("Static scene did not assemble");
                });
                if(mode.equals("RESOURCES")) {
                    var resources=data.getAsJsonArray("resources");
                    if(resources.isEmpty())throw new AssertionError("Resource mode needs actual state resource markers");
                    for(var entry:resources) {
                        var row=entry.getAsJsonArray();
                        command.accept("summon minecraft:text_display "+row.get(0).getAsDouble()+" "+row.get(1).getAsDouble()+" "+row.get(2).getAsDouble()+" "+row.get(3));
                    }
                    context.getInput().resizeWindow(2560,1440);
                    command.accept("tp @a 28 111 28 180 90");context.waitTicks(25);
                    checkPixels(context.takeScreenshot("hoi-atlas-resources-gui2"),false);
                    command.accept("tp @a 23 90 32 180 90");context.waitTicks(25);
                    checkPixels(context.takeScreenshot("hoi-atlas-resources-detail-gui2"),false);
                    command.accept("tp @a 23 78 15 0 30");context.waitTicks(25);
                    checkPixels(context.takeScreenshot("hoi-atlas-resources-floating-gui2"),false);
                    context.getInput().resizeWindow(1400,1000);
                }
                command.accept("tp @a 28 111 28 180 90");context.waitTicks(25);
                var screenshot=context.takeScreenshot("hoi-atlas-"+mode.toLowerCase()+"-coasts");checkPixels(screenshot,false);
                if(java.util.Set.of("ARMY","NAVY","AIR").contains(mode)) {
                    context.getInput().resizeWindow(2560,1440);
                    var counters=new Gson().fromJson(data.get("unitCounters"),dev.hoi.protocol.unit.UnitHud.Counter[].class);
                    if(counters.length==0)throw new AssertionError("Missing starting counters: "+mode);
                    for(var facility:data.getAsJsonArray("facilities"))summonFixtureModel(command,facility.getAsJsonArray(),false);
                    context.waitTicks(10);
                    context.runOnClient(client->CommanderUnitHud.accept(new dev.hoi.protocol.unit.UnitHud.Snapshot("minecraft:overworld",java.util.List.of(counters))));
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-starting-"+mode.toLowerCase()+"-gui2");
                    context.runOnClient(client->{
                        var box=CommanderUnitHud.boxes().stream().filter(b->mode.equals("ARMY")?!b.counter().navy()&&!b.counter().air():mode.equals("NAVY")?b.counter().navy():b.counter().air()).findFirst().orElseThrow();
                        if(CommanderUnitHud.selectAt(box.x()+box.width()/2.0,box.y()+box.height()/2.0))throw new AssertionError("A viewer without a country must not select a counter");
                        dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.of(box.counter().country(),dev.hoi.protocol.country.CountryHud.UNKNOWN));
                        if(!CommanderUnitHud.selectAt(box.x()+box.width()/2.0,box.y()+box.height()/2.0))throw new AssertionError("Starting counter selection failed");
                    });
                    context.waitTicks(2);
                    context.takeScreenshot("hoi-starting-"+mode.toLowerCase()+"-selected-gui2");
                    context.runOnClient(client->{CommanderUnitHud.reset();dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.hud.HudPayloads.State.HIDDEN);});
                    command.accept("kill @e[type=minecraft:item_display]");
                    context.getInput().resizeWindow(1400,1000);
                }
                if(mode.equals("ARMY")||mode.equals("FACTIONS")) {
                    context.getInput().resizeWindow(2560,1440);
                    for(var countryVictory:data.getAsJsonArray("victories"))summonFixtureModel(command,countryVictory.getAsJsonArray(),true);
                    for(int distance=0;distance<3;distance++) {
                        command.accept("kill @e[type=minecraft:text_display]");
                        for(var entry:data.getAsJsonArray("countryNames")) {
                            var row=entry.getAsJsonArray();
                            if(row.get(distance==1?5:4).getAsBoolean())command.accept("summon minecraft:text_display "+row.get(0).getAsDouble()+" "+row.get(1).getAsDouble()+" "+row.get(2).getAsDouble()+" "+row.get(3));
                        }
                        command.accept(distance==0?"tp @a 28 111 28 180 90":distance==1?"tp @a 23 77 32 180 90":"tp @a 28 160 28 180 90");context.waitTicks(25);
                        checkPixels(context.takeScreenshot("hoi-country-names-"+(mode.equals("FACTIONS")?"factions-":"")+(distance==0?"far":distance==1?"near":"distant")+"-gui2"),false);
                    }
                    command.accept("kill @e[type=minecraft:text_display]");
                    command.accept("kill @e[type=minecraft:item_display]");
                    context.getInput().resizeWindow(1400,1000);
                }
                if(mode.equals("ARMY")) {
                    for(var facility:data.getAsJsonArray("facilities"))summonFixtureModel(command,facility.getAsJsonArray(),false);
                    var city=boxes.stream().filter(b->b.material()==dev.hoi.protocol.map.MapAtlasScene.Material.CITY).findFirst().orElseThrow();
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
                    command.accept("tp @a "+cx+" "+(cy+18)+" "+cz+" 180 90");context.waitTicks(20);context.takeScreenshot("hoi-seoul-current-provinces");
                }
            }
            checkRegion(context,command,"okinawa",1);
            checkRegion(context,command,"phnom-penh",0);
            checkRegion(context,command,"china-corner",0);
            checkRegion(context,command,"beijing",0);
            checkFacilities(context,command);
            checkDistance(context,command);
            checkRegionNames(context,command);
            context.runOnClient(client->{client.options.fov().set(70);SceneRenderer.clear();});
        }
    }
    private static void checkRegionNames(ClientGameTestContext context,java.util.function.Consumer<String> command) {
        JsonObject scenes;
        try(var input=SceneSurfaceRenderChecks.class.getResourceAsStream("/region-names-client-scene.json")) {
            if(input==null)throw new AssertionError("Region label geometry fixture missing");
            scenes=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        if(scenes.size()<6)throw new AssertionError("Expected complete sea/air region fixtures");
        int renderDistance=context.computeOnClient(client->client.options.renderDistance().get());
        context.runOnClient(client->client.options.renderDistance().set(12));
        context.runOnClient(client->SceneRenderer.clear());
        context.getInput().resizeWindow(2560,1440);
        for(String region:java.util.List.of("186","193","76","77","79","80","72","93")) {
            var data=scenes.getAsJsonObject(region);int width=data.get("width").getAsInt(),depth=data.get("depth").getAsInt();
            command.accept("kill @e[type=minecraft:block_display]");command.accept("kill @e[type=minecraft:item_display]");command.accept("kill @e[type=minecraft:text_display]");
            for(int y=65;y<=67;y++)command.accept("fill 0 "+y+" 0 127 "+y+" 127 minecraft:air");
            for(var entry:data.getAsJsonArray("runs")) {
                var row=entry.getAsJsonArray();
                command.accept("fill "+row.get(0).getAsInt()+" "+row.get(3).getAsInt()+" "+row.get(2).getAsInt()+" "+row.get(1).getAsInt()+" "+row.get(3).getAsInt()+" "+row.get(2).getAsInt()+" "+row.get(4).getAsString()+" strict");
            }
            for(var entry:data.getAsJsonArray("names")) {
                var row=entry.getAsJsonArray();
                command.accept("summon minecraft:text_display "+row.get(0).getAsDouble()+" "+row.get(1).getAsDouble()+" "+row.get(2).getAsDouble()+" "+row.get(3));
            }
            double height=68+Math.max(depth*.95,width*.6);
            command.accept("tp @a "+width/2.0+" "+height+" "+depth/2.0+" 180 90");context.waitTicks(30);
            var screenshot=context.takeScreenshot("hoi-region-name-"+region+"-gui2");
            checkPixels(screenshot,false);
            if(whitePixels(screenshot)<500)throw new AssertionError("Region name is not visible: "+region);
            try {
                var pixels=javax.imageio.ImageIO.read(screenshot.toFile());int terrain=0;
                for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++) {
                    int rgb=pixels.getRGB(x,y);
                    if(((rgb>>16)&255)<190&&((rgb>>8)&255)<190&&(rgb&255)<190)terrain++;
                }
                if(terrain<pixels.getWidth()*pixels.getHeight()/20)throw new AssertionError("Region terrain is outside the camera range: "+region);
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        }
        command.accept("kill @e[type=minecraft:text_display]");
        context.runOnClient(client->client.options.renderDistance().set(renderDistance));
        context.getInput().resizeWindow(1400,1000);
    }
    private static void checkRegion(ClientGameTestContext context,java.util.function.Consumer<String> command,String region,int routes) {
        JsonObject fixture;
        try(var input=SceneSurfaceRenderChecks.class.getResourceAsStream("/"+region+"-client-scene.json")) {
            if(input==null)throw new AssertionError("Production geometry fixture missing: "+region);
            fixture=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
        boolean beijing=region.equals("beijing");
        var data=fixture.getAsJsonObject("scenes").getAsJsonObject(beijing?"AIR":"ARMY");
        context.runOnClient(client->SceneRenderer.clear());
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
        var boxes=new java.util.ArrayList<dev.hoi.protocol.map.MapAtlasScene.Box>();
        for(var entry:data.getAsJsonArray("meshes")) {
            var b=entry.getAsJsonArray();boxes.add(new dev.hoi.protocol.map.MapAtlasScene.Box(
                    b.get(0).getAsFloat(),b.get(1).getAsFloat(),b.get(2).getAsFloat(),b.get(3).getAsFloat(),b.get(4).getAsFloat(),b.get(5).getAsFloat(),b.get(6).getAsFloat(),
                    dev.hoi.protocol.map.MapAtlasScene.Material.valueOf(b.get(7).getAsString())));
        }
        if(boxes.stream().filter(b->b.material()==dev.hoi.protocol.map.MapAtlasScene.Material.RED).count()!=routes)
            throw new AssertionError("Unexpected connection count in "+region);
        context.runOnClient(client->{
            var id=java.util.UUID.randomUUID();int count=(boxes.size()+1023)/1024;
            for(int i=0;i<count;i++)SceneRenderer.receive(new dev.hoi.protocol.map.MapAtlasScene.Page(id,"minecraft:overworld",i,count,boxes.subList(i*1024,Math.min((i+1)*1024,boxes.size()))));
        });
        for(var f:data.getAsJsonArray("facilities"))summonFixtureModel(command,f.getAsJsonArray(),false);
        var cityIds=new java.util.HashSet<Integer>();
        for(var city:data.getAsJsonArray("cities"))cityIds.add(city.getAsInt());
        if(boxes.stream().filter(b->b.material()==dev.hoi.protocol.map.MapAtlasScene.Material.CITY).count()!=cityIds.size())
            throw new AssertionError("Current urban provinces must retain exactly one city model each");
        for(var v:data.getAsJsonArray("victories")) {
            var row=v.getAsJsonArray();summonFixtureModel(command,row,true);
            double x=row.get(1).getAsDouble(),z=row.get(3).getAsDouble();
            if(cityIds.contains(row.get(0).getAsInt())&&boxes.stream().noneMatch(b->b.material()==dev.hoi.protocol.map.MapAtlasScene.Material.CITY
                    &&Math.abs(b.x()+b.sx()/2-x)<.0001&&Math.abs(b.z()+b.sz()/2-z)<.0001))
                throw new AssertionError("Victory marker lacks its centered city model");
        }
        command.accept("tp @a 18 96 16 180 90");context.waitTicks(25);
        checkPixels(context.takeScreenshot("hoi-"+region+"-land-and-straits"),false);
        var point=data.getAsJsonArray("capital");double x=point.get(0).getAsDouble(),z=point.get(1).getAsDouble(),y=point.get(2).getAsDouble();
        command.accept("tp @a "+x+" "+(y+4)+" "+(z-8)+" 0 32");context.waitTicks(20);
        checkPixels(context.takeScreenshot("hoi-"+region+"-city-and-port"),false);
        if(beijing) {
            if(boxes.stream().noneMatch(b->b.material()==dev.hoi.protocol.map.MapAtlasScene.Material.CITY
                    &&Math.abs(b.x()+b.sx()/2-x)<.0001&&Math.abs(b.z()+b.sz()/2-z)<.0001))
                throw new AssertionError("Beijing capital marker must have a centered city model");
            for(var entry:data.getAsJsonArray("capitalName"))
                command.accept("summon minecraft:text_display "+x+" "+y+" "+z+" "+entry.getAsJsonObject());
            context.getInput().resizeWindow(2560,1440);
            context.runOnClient(client->client.options.guiScale().set(2));
            command.accept("tp @a "+x+" "+(y-1)+" "+(z-5)+" 0 30");
            context.waitTicks(20);context.takeScreenshot("hoi-beijing-city-gui2");
            command.accept("tp @a "+x+" "+(y+10)+" "+(z-14)+" 0 42");
            context.waitTicks(20);context.takeScreenshot("hoi-beijing-map-gui2");
            dev.hoi.protocol.dialog.DialogView view;
            try(var input=SceneSurfaceRenderChecks.class.getResourceAsStream("/beijing-air-detail.json")) {
                if(input==null)throw new AssertionError("Beijing server air report missing");
                view=new Gson().fromJson(new String(input.readAllBytes(),StandardCharsets.UTF_8),dev.hoi.protocol.dialog.DialogView.class);
            } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            command.accept("tp @a "+(x+13)+" "+(y+10)+" "+(z-14)+" 0 42");
            context.runOnClient(client->dev.hoi.client.screen.map.MapAirDetailChecks.show(view));
            context.getInput().setCursorPos(0,0);context.waitTicks(20);
            context.takeScreenshot("hoi-beijing-air-detail-gui2");
            context.setScreen(()->null);context.getInput().resizeWindow(1400,1000);
            command.accept("kill @e[type=minecraft:text_display]");
        }
        context.runOnClient(client->SceneRenderer.clear());
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
        context.runOnClient(client->SceneRenderer.clear());
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
            SceneRenderer.clear();
            var boxes=new java.util.ArrayList<dev.hoi.protocol.map.MapAtlasScene.Box>();
            int index=0;
            for(var material:dev.hoi.protocol.map.MapAtlasScene.Material.values()) {
                boxes.add(new dev.hoi.protocol.map.MapAtlasScene.Box(4+index++*2,70,8,1,1,1,0,material));
            }
            for(int i=0;i<256;i++)boxes.add(new dev.hoi.protocol.map.MapAtlasScene.Box(
                    512+(i%16)*32,70,512+(i/16)*32,1,1,1,0,dev.hoi.protocol.map.MapAtlasScene.Material.BLACK));
            SceneRenderer.receive(new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,boxes));
        });
        command.accept("tp @a 11 110 8 0 90");
        context.waitFor(client->SceneRenderer.visibleTileCount()==dev.hoi.protocol.map.MapAtlasScene.Material.values().length);
        context.takeScreenshot("hoi-atlas-near-all-materials");
        var cached=new java.util.concurrent.atomic.AtomicReference<Object>();
        context.runOnClient(client->cached.set(sprites()));
        context.waitTicks(3);
        context.runOnClient(client->{
            if(cached.get()==null||cached.get()!=sprites())throw new AssertionError("Atlas sprites were rebuilt between frames");
        });
        var reload=new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
        context.runOnClient(client->reload.set(client.reloadResourcePacks()));
        context.waitFor(client->reload.get().isDone());reload.get().join();
        context.waitFor(client->client.gui.overlay()==null&&SceneRenderer.visibleTileCount()==dev.hoi.protocol.map.MapAtlasScene.Material.values().length&&sprites()!=null&&sprites()!=cached.get());
        context.takeScreenshot("hoi-atlas-after-resource-reload");
        context.runOnClient(client->{
            if(SceneRenderer.examinedTileCount()>=SceneRenderer.tileCount()/4)
                throw new AssertionError("Distance selection scanned distant tiles");
        });
        command.accept("tp @a 11 132 8 0 90");
        context.waitFor(client->SceneRenderer.visibleTileCount()==dev.hoi.protocol.map.MapAtlasScene.Material.values().length);
        context.takeScreenshot("hoi-atlas-within-64-all-materials");
        command.accept("tp @a 11 140 8 0 90");
        context.waitFor(client->SceneRenderer.visibleTileCount()==0);
        context.takeScreenshot("hoi-atlas-far-hidden");
        command.accept("tp @a 11 110 8 0 90");
        context.waitFor(client->SceneRenderer.visibleTileCount()==dev.hoi.protocol.map.MapAtlasScene.Material.values().length);
        context.takeScreenshot("hoi-atlas-return-near");
        context.runOnClient(client->{
            SceneRenderer.clear();
            var longBox=new dev.hoi.protocol.map.MapAtlasScene.Box(-512,70,8,1024,1,1,0,dev.hoi.protocol.map.MapAtlasScene.Material.BLACK);
            SceneRenderer.receive(new dev.hoi.protocol.map.MapAtlasScene.Page(java.util.UUID.randomUUID(),"minecraft:overworld",0,1,java.util.List.of(longBox)));
        });
        context.waitFor(client->SceneRenderer.visibleTileCount()==1);
    }
    private static Object sprites() {
        try {
            var field=SceneRenderer.class.getDeclaredField("sprites");field.setAccessible(true);return field.get(null);
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    private static void countryNamesAboveWater(ClientGameTestContext context,java.util.function.Consumer<String> command) {
        command.accept("fill -16 100 -16 16 102 16 minecraft:air strict");
        command.accept("fill -16 100 -16 16 100 16 minecraft:stone strict");
        command.accept("tp @a 0 135 0 180 90");
        command.accept("summon minecraft:text_display 0 101.03 0 {Tags:[\"hoi_country_water_test\"],text:{text:\"뉴질랜드\",font:\"hoi:country_names\",color:\"white\"},billboard:\"fixed\",background:0,see_through:true,line_width:1024,transformation:{translation:[0f,0f,0f],scale:[12f,12f,12f],left_rotation:[-0.70710677f,0f,0f,0.70710677f],right_rotation:[0f,0f,0f,1f]},brightness:{block:15,sky:15},view_range:64f}");
        context.waitTicks(30);
        int land=whitePixels(context.takeScreenshot("hoi-country-name-over-land-gui2"));
        command.accept("fill -16 100 -16 16 100 16 minecraft:water strict");
        context.waitTicks(30);
        int water=whitePixels(context.takeScreenshot("hoi-country-name-over-water-gui2"));
        command.accept("fill -16 101 -16 16 101 16 minecraft:stone strict");
        context.waitTicks(10);
        int raised=whitePixels(context.takeScreenshot("hoi-country-name-through-terrain-gui2"));
        if(land<1000||water<land*.98||raised<land*.85)throw new AssertionError("Country glyphs must remain white over water and terrain: "+land+"/"+water+"/"+raised);
        command.accept("kill @e[tag=hoi_country_water_test]");
        command.accept("fill -16 100 -16 16 102 16 minecraft:air strict");
    }
    private static int whitePixels(Path path) {
        try {
            var image=javax.imageio.ImageIO.read(path.toFile());int count=0;
            for(int y=image.getHeight()/4;y<image.getHeight()*3/4;y++)for(int x=image.getWidth()/4;x<image.getWidth()*3/4;x++) {
                int color=image.getRGB(x,y);
                if(((color>>16)&255)>230&&((color>>8)&255)>230&&(color&255)>230)count++;
            }
            return count;
        } catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
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
