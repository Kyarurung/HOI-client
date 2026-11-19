package dev.hoi.client.screen.decision;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.client.ui.hud.HudMenuBar;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.decision.DecisionPanelView;
import dev.hoi.protocol.decision.DecisionPayloads;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;

public final class DecisionPanelTest {
    private static final Gson JSON = new Gson();
    private record Document(JsonObject root, String sha256) {}
    private record Fixture(String id, String country, DecisionPanelView panel) {}
    private record Assets(JsonObject root, Map<String, String> sprites) {}
    private record Probe(int top, int width, DecisionPanel.Result result) {}
    private record Evidence(String id, int width, int height, String fixtureSha256, int panelHeight,
            List<DecisionPanel.Hit> hits, Map<String, String> screenshots) {}
    private DecisionPanelTest() {}
    public static void run(ClientGameTestContext context, CountryHud hud) {
        var original = context.computeOnClient(client -> new int[]{client.options.guiScale().get(),
                client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight()});
        var source = fixtureDocument();
        var document = source.root();
        var fixtures = new ArrayList<Fixture>();
        for (var value : document.getAsJsonArray("panels")) {
            var item = value.getAsJsonObject();
            fixtures.add(new Fixture(item.get("id").getAsString(), item.get("country").getAsString(),
                    JSON.fromJson(item.get("panel"), DecisionPanelView.class)));
        }
        require(fixtures.stream().map(Fixture::id).collect(java.util.stream.Collectors.toSet())
                .equals(Set.of("inner_circle", "national_assembly", "ldp", "riot_suppression")), "Four original panel fixtures are required");
        String fixtureHash = source.sha256();
        var evidence = new ArrayList<Evidence>();
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.waitTicks(3);
                progressPixels(context, size);
                for (var fixture : fixtures) {
                    String session = UUID.randomUUID().toString();
                    var requests = new ArrayList<DecisionPayloads.Request>();
                    context.setScreen(() -> new DecisionScreen(view(fixture, fixture.panel(), session, 1, hud), requests::add));
                    context.getInput().setCursorPos(2, 2);
                    context.waitTicks(3);
                    var assets = context.computeOnClient(DecisionPanelTest::assets);
                    var probe = context.computeOnClient(client -> probe(client, fixture, assets));
                    sourceGeometry(fixture, probe);
                    var screenshots = new LinkedHashMap<String, String>();
                    String suffix = fixture.id() + "-" + size[0] + "x" + size[1] + "-gui2";
                    screenshots.put("panel", screenshot(context, "hoi-decision-panel-" + suffix, size));
                    for (int hitIndex = 0; hitIndex < probe.result().hits().size(); hitIndex++) {
                        var hit = probe.result().hits().get(hitIndex);
                        int[] point = context.computeOnClient(client -> hoverPoint((DecisionScreen)client.gui.screen(), hit));
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                        context.waitTicks(2);
                        context.runOnClient(client -> {
                            var screen = (DecisionScreen)client.gui.screen();
                            var expected = topmost(probe, hit, point);
                        require(field(screen, "hover").equals(expected == null ? "" : "panel/" + fixture.id() + "/" + expected.id()), "Source panel hover missing: " + hit.id());
                        if (expected == null || expected != hit) return;
                            require(field(screen, "tooltipBounds") != null, "Source panel tooltip not rendered: " + hit.id());
                            verifyTooltipSpacing(client.font,(DecisionText)field(screen,"text"),hit.immediate()+hit.delayed());
                            if(fixture.id().equals("ldp")) {
                                var blocks=((DecisionText)field(screen,"text")).blocks(client.font,hit.immediate()+hit.delayed(),150);
                                var banner=blocks.stream().filter(block->block.image()!=null).findFirst().orElseThrow();
                                require(banner.width()==150&&banner.height()==64,"LDP banner must retain half scale while excluding its 108 transparent source rows");
                            }
                            var imageToken=java.util.regex.Pattern.compile("£([A-Za-z0-9_]+)£?").matcher(hit.immediate()+hit.delayed());
                            int imageWidth=0;
                            while(imageToken.find()) {
                                var image=assets.root().getAsJsonObject("textImages").getAsJsonObject(imageToken.group(1));
                                if(image!=null)imageWidth=Math.max(imageWidth,(int)Math.round(image.get("width").getAsInt()/2d));
                            }
                            if(imageWidth>0)require(((dev.hoi.client.ui.tooltip.Tooltips.ScrolledTooltip)field(screen,"tooltipBounds")).width()==Math.min(imageWidth,screen.width-20),"Image tooltip must wrap to its configured image width: "+hit.id());
                        });
                        String hoverImage=screenshot(context, "hoi-decision-panel-hover-" + hit.id().toLowerCase(java.util.Locale.ROOT) + "-" + hitIndex + "-" + suffix, size);
                        screenshots.put(hit.id() + "/" + hitIndex,hoverImage);
                        if(fixture.id().equals("national_assembly")&&hit.id().equals("NatAsm_logo"))speakerNameplate(context,hoverImage);
                    }
                    context.getInput().setCursorPos(2, 2);
                    context.runOnClient(client -> resetScroll((DecisionScreen)client.gui.screen()));
                    context.waitTicks(2);
                    if (fixture.id().equals("national_assembly")) {
                        var before = context.computeOnClient(client -> textures((DecisionScreen)client.gui.screen()));
                        require(before.size() == 5, "All five original radial layers must upload real textures");
                        context.waitTicks(3);
                        require(before.equals(context.computeOnClient(client -> textures((DecisionScreen)client.gui.screen()))), "Unchanged frames must reuse textures");
                        context.runOnClient(client -> DecisionScreen.resourcesReloaded());
                        context.waitTicks(3);
                        var after = context.computeOnClient(client -> textures((DecisionScreen)client.gui.screen()));
                        require(after.keySet().equals(before.keySet()) && before.values().stream().noneMatch(after::containsValue), "Resource reload must replace cached radial textures");
                        var reloaded = context.computeOnClient(client -> probe(client, fixture, assets(client)));
                        require(probe.result().equals(reloaded.result()), "Resource reload changed source panel geometry or tooltips");
                        screenshots.put("reloaded", screenshot(context, "hoi-decision-panel-reloaded-" + suffix, size));
                        var unknown = withoutFrames(fixture.panel());
                        context.runOnClient(client -> {
                            var screen = (DecisionScreen)client.gui.screen();
                            screen.update(DecisionPayloads.Response.of(screen.token(), view(fixture, unknown, session, 2, hud)));
                        });
                        context.waitTicks(3);
                        require(after.equals(context.computeOnClient(client -> textures((DecisionScreen)client.gui.screen()))), "Unknown frames must not upload a fabricated zero frame");
                        screenshots.put("unknown", screenshot(context, "hoi-decision-panel-unknown-" + suffix, size));
                        require(changedPixels(Path.of(screenshots.get("reloaded")), Path.of(screenshots.get("unknown")), probe) > 20,
                                "Unknown radial frames must replace the previously visible source layers");
                    }
                    require(requests.isEmpty(), "Panel rendering and hover must not issue gameplay requests");
                    if (fixture.id().equals("riot_suppression")) {
                        var reds = hit(probe, "KOR_riot_busan_button_reds");
                        int[] point = context.computeOnClient(client -> clickPoint((DecisionScreen)client.gui.screen(), probe, reds));
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                        context.waitTicks(2);
                        context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTicks(2);context.getInput().releaseMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
                        context.waitTicks(2);
                        require(requests.size() == 1 && requests.getFirst().action() == DecisionPayloads.Action.PANEL_CLICK
                                && requests.getFirst().id().equals("KOR_riot_suppression_gui/KOR_riot_busan_button_reds")
                                && requests.getFirst().session().equals(session), "Visible source button must send its panel click: " + requests);
                        context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTicks(2);context.getInput().releaseMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
                        context.waitTicks(2);
                        require(requests.size() == 1, "A pending panel click must not repeat before the server answers");
                        screenshots.put("clicked", screenshot(context, "hoi-decision-panel-clicked-" + suffix, size));
                        requests.clear();
                    }
                    evidence.add(new Evidence(fixture.id(), size[0], size[1], fixtureHash, probe.result().height(), probe.result().hits(), Map.copyOf(screenshots)));
                    Path report = Path.of(screenshots.get("panel")).getParent().resolve("hoi-decision-panels-evidence.json");
                    Files.writeString(report, JSON.toJson(evidence), StandardCharsets.UTF_8);
                    var closed = context.computeOnClient(client -> (DecisionScreen)client.gui.screen());
                    context.setScreen(() -> null);
                    context.runOnClient(client -> require(textures(closed).isEmpty(), "Closed panel must release its radial cache"));
                }
            }
            allKoreanPanels(context, hud);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(original[0]));
            context.getInput().resizeWindow(original[1], original[2]);
            context.waitTicks(3);
        }
    }
    private static void allKoreanPanels(ClientGameTestContext context, CountryHud hud) throws IOException {
        String directory = System.getenv("HOI_DECISION_PREVIEW");
        var root = JsonParser.parseString(Files.readString(Path.of(directory).resolve("decision-kor-panels.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        var missing = new ArrayList<String>();
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.waitTicks(3);
            var assets = context.computeOnClient(DecisionPanelTest::assets);
            var excluded = assets.root().has("excludedSprites") ? assets.root().getAsJsonObject("excludedSprites").keySet() : Set.<String>of();
            var bars = assets.root().has("progressBars") ? assets.root().getAsJsonObject("progressBars").keySet() : Set.<String>of();
            for (var value : root.getAsJsonArray("panels")) {
                var item = value.getAsJsonObject();
                var fixture = new Fixture(item.get("id").getAsString(), item.get("country").getAsString(), JSON.fromJson(item.get("panel"), DecisionPanelView.class));
                if (size[0] == 2560) visibleSprites(fixture.panel().elements(), sprite -> {
                    if (!sprite.contains("easter_egg") && !excluded.contains(sprite) && !bars.contains(sprite)) {
                        String path = assets.sprites().get(sprite);
                        boolean present = path != null && context.computeOnClient(client -> client.getResourceManager()
                                .getResource(Identifier.parse("hoi:textures/gui/" + path + ".png")).isPresent());
                        if (!present) missing.add(fixture.id() + "/" + sprite);
                    }
                });
                var requests = new ArrayList<DecisionPayloads.Request>();
                context.setScreen(() -> new DecisionScreen(view(fixture, fixture.panel(), UUID.randomUUID().toString(), 1, hud), requests::add));
                context.getInput().setCursorPos(2, 2);
                context.waitTicks(3);
                screenshot(context, "hoi-decision-kor-panel-" + fixture.id().toLowerCase(java.util.Locale.ROOT) + "-" + size[0] + "x" + size[1] + "-gui2", size);
                require(requests.isEmpty(), "Panel rendering must not issue gameplay requests: " + fixture.id());
                context.setScreen(() -> null);
            }
        }
        require(missing.isEmpty(), "Visible source panel sprites without pack art: " + missing);
    }
    private static void visibleSprites(List<DecisionPanelView.Element> elements, java.util.function.Consumer<String> consumer) {
        for (var element : elements) {
            if (!Boolean.TRUE.equals(element.visible())) continue;
            if (element.sprite() != null && !element.kind().equals("radial") && (element.frameVariable() == null || element.frame() != null)) consumer.accept(element.sprite());
            visibleSprites(element.children(), consumer);
        }
    }
    private static void speakerNameplate(ClientGameTestContext context,String screenshot)throws IOException {
        var bounds=context.computeOnClient(client->(dev.hoi.client.ui.tooltip.Tooltips.ScrolledTooltip)field(client.gui.screen(),"tooltipBounds"));
        int lineHeight=context.computeOnClient(client->client.font.lineHeight);
        int centerX=(bounds.x()+bounds.width()/2)*2,centerY=(bounds.y()+2*(lineHeight+1)+118-bounds.scroll())*2;
        var pixels=javax.imageio.ImageIO.read(Path.of(screenshot).toFile());int white=0;
        for(int y=centerY-lineHeight;y<centerY+lineHeight;y++)for(int x=centerX-50;x<centerX+50;x++) {
            int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;
            if(Math.min(r,Math.min(g,b))>210&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<15)white++;
        }
        require(white>30,"Speaker name must render inside the original portrait nameplate");
    }
    private static Document fixtureDocument() {
        String directory = System.getenv("HOI_DECISION_PREVIEW");
        require(directory != null && !directory.isBlank(), "HOI_DECISION_PREVIEW is required for server-exported decision panel fixtures");
        try {
            byte[] bytes = Files.readAllBytes(Path.of(directory).resolve("decision-panels.json"));
            var root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            require(root.get("version").getAsInt() == 1 && root.get("fixture").getAsBoolean(), "Source panel fixture metadata required");
            require(root.get("catalogSha256").getAsString().matches("[a-f0-9]{64}"), "Source catalog hash required");
            require(!root.getAsJsonObject("sourceHashes").isEmpty(), "Original GUI source hashes required");
            for (var entry : root.getAsJsonObject("sourceHashes").entrySet()) require(entry.getValue().getAsString().matches("[a-f0-9]{64}"), "Invalid source hash: " + entry.getKey());
            return new Document(root, hash(bytes));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
    private static void progressPixels(ClientGameTestContext context,int[] size) throws IOException {
        var assets=context.computeOnClient(DecisionPanelTest::assets);
        var resources=context.computeOnClient(Minecraft::getResourceManager);
        var bars=assets.root().getAsJsonObject("progressBars");
        String[] names={"vaccine","dissatisfaction","infection"};
        for(int frame:new int[]{0,50,100}) {
            var children=new ArrayList<DecisionPanelView.Element>();
            for(int index=0;index<names.length;index++) {
                String sprite="GFX_KOR_coronavirus_"+names[index]+"_progbar";
                children.add(new DecisionPanelView.Element(names[index],"icon","korea/interface/TFR_Korea_GUI.gui",1,
                        new DecisionPanelView.Layout(index*140.0,0.0,null,null,null,null,null,null,null,null,null,null),
                        sprite,(double)frame,null,null,null,true,null,List.of()));
            }
            var root=new DecisionPanelView.Element("synthetic_progress","container","korea/interface/TFR_Korea_GUI.gui",1,
                    new DecisionPanelView.Layout(null,null,new DecisionPanelView.Measure(404.0,false),new DecisionPanelView.Measure(124.0,false),null,null,null,null,null,null,null,null),
                    null,null,null,null,null,true,null,children);
            var panel=new DecisionPanelView("synthetic_progress","synthetic_progress","korea/interface/TFR_Korea_GUI.gui",1,List.of(root),List.of());
            var renderer=new DecisionPanel();renderer.assets(assets.root());
            try {
                context.setScreen(()->new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Synthetic progress pixel regression")) {
                    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
                        graphics.fill(0,0,width,height,0xFF101010);
                        renderer.render(graphics,font,panel,assets.sprites(),new DecisionText(),20,80,404);
                    }
                });
                context.waitTicks(3);
                var capture=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-decision-progress-synthetic-"+frame+"-"+size[0]+"-gui2").toFile());
                for(int index=0;index<names.length;index++) {
                    var bar=bars.getAsJsonObject("GFX_KOR_coronavirus_"+names[index]+"_progbar");
                    var images=new ArrayList<java.awt.image.BufferedImage>();
                    for(var texture:bar.getAsJsonArray("textures")) {
                        String path=texture.getAsJsonObject().get("path").getAsString();
                        var resource=resources.getResource(Identifier.parse("hoi:textures/gui/"+path+".png")).orElseThrow();
                        try(var input=resource.open()) {images.add(javax.imageio.ImageIO.read(input));}
                    }
                    int w=bar.get("width").getAsInt(),h=bar.get("height").getAsInt(),checked=0,wrong=0;
                    require(images.getFirst().getWidth()==h && images.getFirst().getHeight()==w,"Vertical source textures must retain their original transposed dimensions");
                    for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                        int sourceX=h-1-y,sourceY=w-1-x;
                        int background=images.get(1).getRGB(sourceX,sourceY);
                        int foreground=sourceX<(int)Math.round(h*frame/100.0)?images.getFirst().getRGB(sourceX,sourceY):0;
                        if((background>>>24)<16 && (foreground>>>24)<16)continue;
                        int actual=capture.getRGB((20+index*140+x)*2+1,(80+y)*2+1);checked++;
                        boolean mismatch=false;
                        for(int shift:new int[]{16,8,0}) {
                            double expected=16;
                            for(int pixel:new int[]{background,foreground}) {
                                double alpha=(pixel>>>24)/255.0;
                                expected=(pixel>>shift&255)*alpha+expected*(1-alpha);
                            }
                            if(Math.abs((actual>>shift&255)-Math.round(expected))>3)mismatch=true;
                        }
                        if(mismatch)wrong++;
                    }
                    require(checked>300 && wrong<=checked/100,"Vertical progress orientation/fill pixels differ: "+names[index]+" frame="+frame+" wrong="+wrong+" checked="+checked);
                }
            } finally {context.setScreen(()->null);context.runOnClient(client->renderer.close());}
        }
    }
    private static DecisionPayloads.View view(Fixture fixture, DecisionPanelView panel, String session, long revision, CountryHud hud) {
        var category = new DecisionPayloads.Category(fixture.id(), fixture.id(), "", "", "", panel.scriptedGui(), true, panel);
        return new DecisionPayloads.View(fixture.country(), session, revision, "", 0, 0, hud, List.of(), List.of(category), 1);
    }
    private static Assets assets(Minecraft client) {
        var resource = client.getResourceManager().getResource(Identifier.parse("hoi:ui/decision_assets.json")).orElseThrow();
        try (var input = resource.open()) {
            var root = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            var sprites = new HashMap<String, String>();
            for (var entry : root.getAsJsonObject("sprites").entrySet()) sprites.put(entry.getKey(), entry.getValue().getAsString());
            return new Assets(root, Map.copyOf(sprites));
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
    private static Probe probe(Minecraft client, Fixture fixture, Assets assets) {
        require(client.getWindow().getGuiScale() == 2, "Panel rendering requires GUI scale 2");
        var screen = (DecisionScreen)client.gui.screen();
        var button = screen.children().stream().filter(child -> child instanceof Button value && value.getMessage().getString().equals(fixture.id()))
                .map(child -> (Button)child).findFirst().orElseThrow();
        int top = button.getY() + 26, width = screen.unitHudLeft() - 32;
        var renderer = new DecisionPanel();
        var text = new DecisionText();
        renderer.assets(assets.root());
        text.assets(assets.root());
        try {
            return new Probe(top, width, renderer.render(null, client.font, fixture.panel(), assets.sprites(), text, 11, top, width));
        } finally {
            renderer.close();
        }
    }
    private static void sourceGeometry(Fixture fixture, Probe probe) {
        require(probe.result().height() > 0 && !probe.result().hits().isEmpty(), "Original panel is empty: " + fixture.id());
        for (var hit : probe.result().hits()) require(hit.width() > 0 && hit.height() > 0 && hit.x() >= 11 && hit.x() + hit.width() <= 12 + probe.width()
                && hit.y() >= probe.top() && hit.y() + hit.height() <= probe.top() + probe.result().height() + 1, "Source hover bounds exceed the panel: " + hit.id());
        switch (fixture.id()) {
            case "ldp" -> {
                require(fixture.panel().elements().getFirst().layout().width() == null, "LDP fixture must retain its absent root width");
                require(probe.result().height() == (int)Math.ceil(190.0 * probe.width() / 400), "Negative LDP offsets must form the original 400 by 190 canvas");
                for (var hit : probe.result().hits()) {
                    var element = elements(fixture.panel()).filter(value -> value.name().equals(hit.id())).findFirst().orElseThrow();
                    assertHit(hit, probe, 400, 5, element.layout().y() + 190, element.layout().width().value(), element.layout().height().value());
                }
                require(probe.result().hits().size() == 3, "Initial LDP source fixture must show three faction tooltips");
            }
            case "inner_circle" -> {
                require(fixture.panel().elements().getFirst().layout().width().value() == 480, "Inner circle source width must remain 480");
                require(probe.result().height() >= probe.width(), "Inner circle declared 480 by 480 canvas must survive");
                require(probe.result().hits().size() == 1, "Only the nonempty original advisor trait tooltip may create a hover target");
                for (int index = 1; index <= 3; index++) {
                    String name="TFR_KOR_ascended_advisor_"+index+"_trait";
                    var element=elements(fixture.panel()).filter(value->value.name().equals(name)).findFirst().orElseThrow();
                    String value=element.tooltip().immediate().value();
                    require(element.tooltip().immediate().complete() && (index==1?!value.isBlank():value.isEmpty()), "Source advisor trait selection must be resolved and preserve empty unmatched text: "+name);
                    if(index>1) {
                        require(probe.result().hits().stream().noneMatch(hit->hit.id().equals(name)), "Empty source tooltips must not create hover targets: "+name);
                        continue;
                    }
                    var hit = hit(probe, name);
                    require(hit.x() == 11 + (int)Math.floor((6 + (index - 1) * 160) * probe.width() / 480.0), "Nested advisor anchor shifted: " + index);
                    require(hit.y() == probe.top() + (int)Math.floor(462 * probe.width() / 480.0), "Nested advisor vertical anchor shifted: " + index);
                }
            }
            case "national_assembly" -> {
                require(fixture.panel().elements().getFirst().layout().width().value() == 0, "Assembly fixture must retain its zero root width");
                require(probe.result().height() == (int)Math.ceil(431.0 * probe.width() / 490), "Assembly canvas must exclude the transparent upper radial half");
                assertHit(hit(probe, "NatAsm_overlay"), probe, 490, 6, 0, 484, 257);
                assertHit(hit(probe, "NatAsm_logo"), probe, 490, 201, 159, 91, 91);
                require(elements(fixture.panel()).filter(value -> value.kind().equals("radial") && value.frame() != null).count() == 5, "Known source frame fixture must exercise all five original shaders");
            }
            case "riot_suppression" -> {
                var reds = hit(probe, "KOR_riot_busan_button_reds");
                require(reds.button().equals("KOR_riot_busan_button_reds") && reds.enabled(), "Visible enabled source button must be clickable");
                require(probe.result().hits().stream().noneMatch(hit -> hit.id().equals("KOR_riot_busan_button_army")), "Hidden source button must not create a hit");
                var element = elements(fixture.panel()).filter(value -> value.name().equals("KOR_riot_busan_button_reds")).findFirst().orElseThrow();
                double scale = probe.width() / 500.0;
                require(fixture.panel().elements().getFirst().layout().width().value() == 500, "Riot suppression source width must remain 500");
                require(reds.x() == 11 + (int)Math.floor(element.layout().x() * scale) && reds.y() == probe.top() + (int)Math.floor(element.layout().y() * scale), "Source button anchor shifted: " + reds);
            }
            default -> throw new AssertionError("Unknown panel fixture " + fixture.id());
        }
    }
    private static void assertHit(DecisionPanel.Hit hit, Probe probe, double sourceWidth, double x, double y, double width, double height) {
        double scale = probe.width() / sourceWidth;
        require(hit.x() == 11 + (int)Math.floor(x * scale) && hit.y() == probe.top() + (int)Math.floor(y * scale)
                && hit.width() == (int)Math.ceil(width * scale) && hit.height() == (int)Math.ceil(height * scale), "Original source hover rectangle changed: " + hit.id());
    }
    private static DecisionPanel.Hit hit(Probe probe, String name) {
        return probe.result().hits().stream().filter(value -> value.id().equals(name)).findFirst().orElseThrow();
    }
    private static Stream<DecisionPanelView.Element> elements(DecisionPanelView panel) {
        return walk(panel.elements());
    }
    private static Stream<DecisionPanelView.Element> walk(List<DecisionPanelView.Element> elements) {
        return elements.stream().flatMap(element -> Stream.concat(Stream.of(element), walk(element.children())));
    }
    private static int[] hoverPoint(DecisionScreen screen, DecisionPanel.Hit hit) {
        resetScroll(screen);
        int target = hit.y() + Math.min(hit.height() / 2, 8);
        for (int step = 0; target - screen.scrollOffset() > screen.height - 20 && step < 30; step++) {
            int before = screen.scrollOffset();
            screen.mouseScrolled(3, HudMenuBar.height(screen.width) + 53, 0, -3);
            if (screen.scrollOffset() == before) break;
        }
        int y = target - screen.scrollOffset();
        require(y >= HudMenuBar.height(screen.width) + 52 && y < screen.height - 10, "Panel tooltip cannot be reached by scrolling: " + hit.id());
        return new int[]{hit.x() + Math.min(hit.width() / 2, 8), y};
    }
    private static DecisionPanel.Hit topmost(Probe probe, DecisionPanel.Hit hit, int[] point) {
        DecisionPanel.Hit found = null;
        int x = point[0], y = hit.y() + Math.min(hit.height() / 2, 8);
        for (var value : probe.result().hits())
            if (!(value.immediate() + value.delayed()).isEmpty() && x >= value.x() && x < value.x() + value.width() && y >= value.y() && y < value.y() + value.height()) found = value;
        return found;
    }
    private static int[] clickPoint(DecisionScreen screen, Probe probe, DecisionPanel.Hit hit) {
        resetScroll(screen);
        for (int dy = 0; dy < hit.height(); dy++) for (int dx = 0; dx < hit.width(); dx++) {
            int x = hit.x() + dx, y = hit.y() + dy;
            DecisionPanel.Hit top = null;
            for (var value : probe.result().hits())
                if (!value.button().isEmpty() && x >= value.x() && x < value.x() + value.width() && y >= value.y() && y < value.y() + value.height()) top = value;
            if (top != hit) continue;
            for (int step = 0; y - screen.scrollOffset() > screen.height - 20 && step < 30; step++) {
                int before = screen.scrollOffset();
                screen.mouseScrolled(3, HudMenuBar.height(screen.width) + 53, 0, -3);
                if (screen.scrollOffset() == before) break;
            }
            int shown = y - screen.scrollOffset();
            require(shown >= HudMenuBar.height(screen.width) + 52 && shown < screen.height - 10, "Source button cannot be reached by scrolling: " + hit.id());
            return new int[]{x, shown};
        }
        throw new AssertionError("No point where the source button is on top: " + hit.id());
    }
    private static void resetScroll(DecisionScreen screen) {
        screen.mouseScrolled(3, HudMenuBar.height(screen.width) + 53, 0, Integer.MAX_VALUE / 28.0);
        require(screen.scrollOffset() == 0, "Panel scroll must reset at the margin");
    }
    private static Map<String, String> textures(DecisionScreen screen) {
        var radial = field(field(screen, "panels"), "radial");
        var result = new HashMap<String, String>();
        var cache = (Map<?, ?>)field(radial, "cache");
        require(cache.size() <= 5, "Radial cache exceeded five source layers");
        cache.forEach((name, cached) -> {
            Object texture = field(cached, "texture");
            if (texture != null) result.put(name.toString(), texture.toString());
        });
        return Map.copyOf(result);
    }
    private static Object field(Object instance, String name) {
        try {
            var field = instance.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(instance);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Missing panel observation field: " + name, error);
        }
    }
    private static DecisionPanelView withoutFrames(DecisionPanelView panel) {
        var value = JSON.toJsonTree(panel).getAsJsonObject();
        clearFrames(value.getAsJsonArray("elements"));
        return JSON.fromJson(value, DecisionPanelView.class);
    }
    private static void clearFrames(com.google.gson.JsonArray elements) {
        for (var value : elements) {
            var element = value.getAsJsonObject();
            if (element.get("kind").getAsString().equals("radial")) element.add("frame", com.google.gson.JsonNull.INSTANCE);
            clearFrames(element.getAsJsonArray("children"));
        }
    }
    public static void verifyTooltipSpacing(net.minecraft.client.gui.Font font,DecisionText text,String source) {
        for(int width:List.of(80,160,324)) {
            var blocks=text.blocks(font,source,width);
            boolean previous=false;
            for(int index=0;index<blocks.size();index++) {
                var block=blocks.get(index);
                boolean blank=block.image()==null&&dev.hoi.client.ui.tooltip.Tooltips.blank(block.text());
                require(!blank||(!previous&&index>0&&index<blocks.size()-1),"Repeated or outer blank rows remain after source tooltip wrapping");
                previous=blank;
            }
        }
    }
    private static String screenshot(ClientGameTestContext context, String name, int[] size) throws IOException {
        Path path = context.takeScreenshot(name);
        var image = javax.imageio.ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == size[0] && image.getHeight() == size[1], "Screenshot dimensions changed: " + path);
        return path.toString();
    }
    private static int changedPixels(Path before, Path after, Probe probe) throws IOException {
        var one = javax.imageio.ImageIO.read(before.toFile());
        var two = javax.imageio.ImageIO.read(after.toFile());
        int changed = 0, right = Math.min(one.getWidth(), (11 + probe.width()) * 2);
        int bottom = Math.min(one.getHeight() - 22, (probe.top() + probe.result().height()) * 2);
        for (int y = probe.top() * 2; y < bottom; y++) for (int x = 22; x < right; x++) if (one.getRGB(x, y) != two.getRGB(x, y)) changed++;
        return changed;
    }
    private static String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new AssertionError(error);
        }
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
