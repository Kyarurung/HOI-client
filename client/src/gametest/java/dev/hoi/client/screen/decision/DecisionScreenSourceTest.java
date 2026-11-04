package dev.hoi.client.screen.decision;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hoi.client.ui.UiMenuBar;
import dev.hoi.protocol.decision.DecisionPayloads;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;

public final class DecisionScreenSourceTest {
    private static final List<String> FIXTURES = List.of("prc", "kor", "jap", "prc-active", "prc-restored", "prc-completed", "kor-kfxx", "prc-eadi",
            "kor-scene","kor-scene-active","kor-scene-ownership-lost","kor-scene-mission-timeout","kor-scene-target-active","kor-economic-crisis");
    private static final List<String> HOVER_IDS = List.of("PRC_advocate_for_militarization", "PRC_hold_pro_government_rally",
            "KOR_second_imf_crisis",
            "KOR_kfxx_fighter_project_mission", "KOR_export_to_tur_k9", "PRC_declare_our_neutrality_in_europe", "PRC_invite_nepal_eadi",
            "KOR_export_to_pol_k2","KOR_export_to_pol_k2a1","KOR_export_to_raj_k9","KOR_export_to_ast_k9","KOR_export_to_egy_k9","KOR_export_to_chi_k1A1","KOR_arms_factory_ulsan","KOR_arms_factory_gyungnam","KOR_coronavirus_infection_mission","KOR_export_to_pol_k9_plus");
    private static final Pattern BODY_IMAGE = Pattern.compile("£([A-Za-z0-9_]+)£?");
    private record Preview(String name, List<String> pages, List<DecisionPayloads.Row> rows,
            List<DecisionPayloads.Category> categories, JsonObject metadata, String sha256) {}
    private record MissingSourceAsset(String owner, String field, String sourceName, String fallback) {}
    private record Evidence(String fixture, int width, int height, int guiScale, int paneWidth, int rowCount, int categoryCount,
            int declaredAssets, String fixtureSha256, JsonObject metadata, Set<String> observedNames,
            List<MissingSourceAsset> missingSourceAssets, Map<String, int[]> flagRows, Map<String, String> screenshots) {}
    private DecisionScreenSourceTest() {}
    public static void run(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var resource=client.getResourceManager().getResource(Identifier.parse("hoi:ui/decision_assets.json")).orElseThrow();
            try(var input=resource.open()) {
                var bindings=JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject();
                var sprites=bindings.getAsJsonObject("sprites");
                if(!bindings.getAsJsonObject("textImages").has("GFX_kor_diffusion"))throw new AssertionError("Defense export tooltip requires its original text image binding");
                for(String symbol:List.of("GFX_kor_diffusion","GFX_decision_icon_KOR_K9","GFX_decision_icon_KOR_K2","GFX_decision_icon_KOR_arms_factory_icon","GFX_decision_icon_KOR_coronavirus_death","GFX_decision_icon_KOR_mega_city_icon","GFX_decision_icon_KOR_raillway_icon","GFX_FRA_Military_Aid","GFX_SOV_decision_increase_moscow_budget","GFX_decision_generic_exploit_land","GFX_decision_generic_investment")) {
                    if(!sprites.has(symbol) || dev.hoi.client.ui.UiAssets.imageHeight(sprites.get(symbol).getAsString(),100)<=0)
                        throw new AssertionError("Missing original reference decision art: "+symbol);
                }
                for(String tag:List.of("pol","raj","aus","egy","tur","chi")) {
                    String token="£GFX_KOR_"+tag+"_texticon";
                    if(dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal(token),false).getString().contains("£"))
                        throw new AssertionError("Unresolved original decision country outline: "+token);
                }
            } catch(IOException error) {throw new UncheckedIOException(error);}
        });
        String directory = System.getenv("HOI_DECISION_PREVIEW");
        if (directory == null || directory.isBlank())
            throw new AssertionError("HOI_DECISION_PREVIEW is required for server-exported decision source fixtures");
        var original = context.computeOnClient(client -> new int[]{client.options.guiScale().get(),
                client.getWindow().getScreenWidth(), client.getWindow().getScreenHeight()});
        var evidence = new ArrayList<Evidence>();
        try {
            var previews = new ArrayList<Preview>();
            for (String name : System.getProperty("hoi.testSuite", "all").equals("economic-captivity") ? List.of("kor-economic-crisis") : FIXTURES) previews.add(read(Path.of(directory), name));
            var assets = context.computeOnClient(DecisionScreenSourceTest::assets);
            context.runOnClient(client -> {
                var text=sourceText();
                String wrappedPadding="§Y제목§!\n\n\n"+" ".repeat(400)+"§G효과: +0.00%§!\n\n§!\n끝\n\n\n";
                DecisionPanelTest.verifyTooltipSpacing(client.font,text,wrappedPadding);
                String padded="앞\n\n§r\n---\n[KOR.GetFlag]\n\n\n\n뒤";
                if(text.sourceRows(padded).size()!=9 || text.rows(padded).size()!=5
                        || !text.sourceRows(padded).get(2).getString().isEmpty())
                    throw new AssertionError("Source layout padding must remain available before paragraph spacing is collapsed");
                for(String tag:List.of("KOR","PRC","JAP","NEP","BHU","SRL")) {
                    var flag=text.component("["+tag+".GetFlag]");
                    if(flag.getString().length()!=1 || client.font.width(flag)!=11)
                        throw new AssertionError("Small decision flag must occupy 10 pixels plus one advance: "+tag);
                    var lines=text.lines(client.font,"§Y앞§!["+tag+".GetFlag]§C뒤§!\n\n끝",22);
                    if(lines.stream().anyMatch(line->client.font.width(line)>22) || lines.stream().noneMatch(line->line==net.minecraft.util.FormattedCharSequence.EMPTY))
                        throw new AssertionError("Flag wrapping must preserve paragraph breaks and bounded width");
                }
                if(!text.component("[ZZX.GetFlag]").getString().equals("[ZZX.GetFlag]"))
                    throw new AssertionError("Unmapped flags must retain the source token");
            });
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.waitTicks(3);
                for (var preview : previews) {
                    var requests = new ArrayList<DecisionPayloads.Request>();
                    context.setScreen(() -> new DecisionScreen(null, requests::add));
                    context.runOnClient(client -> {
                        var screen = (DecisionScreen) client.gui.screen();
                        for (String page : preview.pages()) screen.update(new DecisionPayloads.Response(screen.token(), page));
                        if (screen.rowCount() != preview.rows().size() || screen.pending())
                            throw new AssertionError("Incomplete source decision snapshot: " + preview.name());
                        if (client.getWindow().getGuiScale() != 2)
                            throw new AssertionError("Source decision rendering requires actual GUI scale 2");
                        for (var request : requests) if (request.action() != DecisionPayloads.Action.REFRESH
                                || request.page() <= 0 || request.page() >= preview.pages().size())
                            throw new AssertionError("Unexpected source preview request: " + request);
                    });
                    margin(context);
                    var screenshots = new LinkedHashMap<String, String>();
                    String suffix = preview.name() + "-" + size[0] + "x" + size[1] + "-gui2";
                    screenshots.put("top", screenshot(context, "hoi-decisions-source-" + suffix, size));
                    if (preview.name().equals("kor-economic-crisis")) {
                        var crisis = preview.categories().stream().filter(category -> category.id().equals("KOR_new_econimic_crisis_category")).findFirst().orElseThrow();
                        if (!preview.categories().getFirst().id().equals("KOR_national_assembly_category") || preview.categories().indexOf(crisis) != 1)
                            throw new AssertionError("Economic crisis must follow the source national assembly category");
                        context.runOnClient(client -> locate((DecisionScreen)client.gui.screen(), crisis.name()));
                        context.waitTicks(2);
                        int[] point = context.computeOnClient(client -> headerTooltip((DecisionScreen)client.gui.screen(), crisis));
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2); context.waitTicks(3);
                        screenshots.put("crisis-tooltip", screenshot(context, "hoi-decisions-source-crisis-tooltip-" + suffix, size));
                        context.runOnClient(client -> locate((DecisionScreen)client.gui.screen(), "제2차 외환위기 대응"));
                        margin(context);
                        screenshots.put("crisis-row", screenshot(context, "hoi-decisions-source-crisis-row-" + suffix, size));
                    }
                    if(preview.name().equals("kor")) {
                        var assembly=preview.categories().stream().filter(category->category.id().equals("KOR_national_assembly_category")).findFirst().orElseThrow();
                        int[] assemblyPoint=context.computeOnClient(client->headerTooltip((DecisionScreen)client.gui.screen(),assembly));
                        context.getInput().setCursorPos(assemblyPoint[0]*2,assemblyPoint[1]*2);context.waitTicks(3);
                        screenshots.put("assembly-tooltip",screenshot(context,"hoi-decisions-source-assembly-tooltip-"+suffix,size));
                        context.runOnClient(client -> {
                            var screen=(DecisionScreen)client.gui.screen();
                            var button=(Button)screen.children().stream().filter(child->child instanceof Button control
                                    && control.getMessage().getString().equals(sourceText().component(assembly.name()).getString())).findFirst().orElseThrow();
                            button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                        });
                        margin(context);
                        String image=screenshot(context,"hoi-decisions-source-arms-reference-"+suffix,size);
                        screenshots.put("arms-reference",image);
                        int[] bounds=context.computeOnClient(client -> {
                            var category=preview.categories().stream().filter(value->value.id().equals("KOR_korea_defense_business_category")).findFirst().orElseThrow();
                            var button=(Button)client.gui.screen().children().stream().filter(child->child instanceof Button control
                                    && control.getMessage().getString().equals(sourceText().component(category.name()).getString())).findFirst().orElseThrow();
                            return new int[]{button.getBottom()*2,button.getWidth()*2};
                        });
                        var rendered=javax.imageio.ImageIO.read(Path.of(image).toFile());
                        boolean banner=false;
                        for(int y=bounds[0]+12;y<Math.min(rendered.getHeight(),bounds[0]+50);y++) {
                            int white=0;
                            for(int x=15;x<Math.min(rendered.getWidth(),bounds[1]);x++) {
                                int color=rendered.getRGB(x,y),red=color>>16&255,green=color>>8&255,blue=color&255;
                                if(Math.min(red,Math.min(green,blue))>170 && Math.max(red,Math.max(green,blue))-Math.min(red,Math.min(green,blue))<20)white++;
                            }
                            if(white>150)banner=true;
                        }
                        if(!banner)throw new AssertionError("Original banner must follow its category header without double-counted text-icon padding");
                        var hoverPoint=context.computeOnClient(client->{
                            var category=preview.categories().stream().filter(value->value.id().equals("KOR_korea_defense_business_category")).findFirst().orElseThrow();
                            return headerTooltip((DecisionScreen)client.gui.screen(),category);
                        });
                        context.getInput().setCursorPos(hoverPoint[0]*2,hoverPoint[1]*2);context.waitTicks(3);
                        screenshots.put("arms-tooltip",screenshot(context,"hoi-decisions-source-arms-tooltip-"+suffix,size));
                        context.runOnClient(client->{var screen=(DecisionScreen)client.gui.screen();screen.mouseScrolled(3,UiMenuBar.height(screen.width)+53,0,-10000);});
                        margin(context);screenshots.put("arms-icons",screenshot(context,"hoi-decisions-source-arms-icons-"+suffix,size));
                        context.runOnClient(client -> {
                            var screen=(DecisionScreen)client.gui.screen();
                            resetScroll(screen);
                            var button=(Button)screen.children().stream().filter(child->child instanceof Button control
                                    && control.getMessage().getString().equals(sourceText().component(preview.categories().getFirst().name()).getString())).findFirst().orElseThrow();
                            button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                        });
                        margin(context);
                    }
                    var flagRows = new LinkedHashMap<String, int[]>();
                    if(preview.name().equals("kor-kfxx") || preview.name().equals("prc-eadi")) {
                        var row=preview.rows().stream().filter(candidate->candidate.name().contains(".GetFlag]")).findFirst().orElseThrow();
                        int[] bounds=context.computeOnClient(client->{
                            var screen=(DecisionScreen)client.gui.screen();locate(screen,row.name());
                            var button=(Button)screen.children().stream().filter(child->child instanceof Button b
                                    && b.getMessage().getString().equals(sourceText().component(row.name()).getString())).findFirst().orElseThrow();
                            return new int[]{button.getX(),button.getY(),button.getWidth(),button.getHeight()};
                        });
                        flagRows.put(row.id(),bounds);margin(context);
                        screenshots.put("flags",screenshot(context,"hoi-decisions-flags-"+suffix,size));
                    }
                    var observed = context.computeOnClient(client -> inspectNames((DecisionScreen) client.gui.screen(), preview));
                    context.waitTicks(2);
                    screenshots.put("bottom", screenshot(context, "hoi-decisions-source-bottom-" + suffix, size));
                    context.runOnClient(client -> {
                        var screen=(DecisionScreen)client.gui.screen();
                        int maximum=screen.scrollOffset();
                        if(maximum==0)return;
                        int x=screen.unitHudLeft()-7,top=UiMenuBar.height(screen.width)+26,bottom=screen.height-10;
                        var right=new net.minecraft.client.input.MouseButtonEvent(x,top+3,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT,0));
                        screen.mouseClicked(right,false);
                        if(screen.scrollOffset()!=maximum)throw new AssertionError("Right scrollbar click must not scroll or take a decision");
                        var up=new net.minecraft.client.input.MouseButtonEvent(x,top+3,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(up,false);
                        if(screen.scrollOffset()>=maximum)throw new AssertionError("Original scrollbar up arrow must move the decision list");
                        screen.mouseScrolled(5,top+1,0,10000);
                        var grab=new net.minecraft.client.input.MouseButtonEvent(x,top+12,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        var end=new net.minecraft.client.input.MouseButtonEvent(x,bottom-10,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(grab,false);screen.mouseDragged(end,0,bottom-top-22);screen.mouseReleased(end);
                        if(screen.scrollOffset()!=maximum)throw new AssertionError("Dragging the original scrollbar must reach the final decision");
                        screen.mouseDragged(grab,0,top-bottom);
                        if(screen.scrollOffset()!=maximum)throw new AssertionError("Releasing the scrollbar must stop its drag");
                        screen.mouseScrolled(5,top+1,0,10000);
                    });
                    for (String id : HOVER_IDS) {
                        var row = preview.rows().stream().filter(candidate -> candidate.id().equals(id)).findFirst();
                        if (row.isEmpty()) continue;
                        if (preview.rows().stream().filter(candidate -> candidate.name().equals(row.get().name())).count() != 1)
                            throw new AssertionError("Source tooltip row name is ambiguous: " + id);
                        margin(context);
                        int[] point = context.computeOnClient(client -> locate((DecisionScreen) client.gui.screen(), row.get().name()));
                        if(preview.name().equals("kor-scene")) {
                            var exported=JsonParser.parseString(Files.readString(Path.of(directory,"kor-scene-details.json"))).getAsJsonObject();
                            if(exported.has(id))context.runOnClient(client->acceptDetail((DecisionScreen)client.gui.screen(),id,exported.getAsJsonObject(id)));
                        }
                        context.getInput().setCursorPos(point[0] * 2, point[1] * 2);
                        context.waitTicks(3);
                        screenshots.put(id, screenshot(context, "hoi-decisions-source-hover-" + id.toLowerCase(Locale.ROOT) + "-" + suffix, size));
                    }
                    if(preview.name().startsWith("kor-scene")) {
                        var costRow=preview.rows().stream().filter(row->row.id().equals("KOR_export_to_pol_k9_plus")).findFirst().orElseThrow();
                        int[] rectangle=context.computeOnClient(client->{
                            var screen=(DecisionScreen)client.gui.screen();locate(screen,costRow.name());
                            var b=(Button)screen.children().stream().filter(child->child instanceof Button control && control.getX()==7 && control.getMessage().getString().equals(sourceText().component(costRow.name()).getString())).findFirst().orElseThrow();
                            return new int[]{Math.max(b.getX(),b.getRight()-110)*2,b.getY()*2,b.getRight()*2,b.getBottom()*2};
                        });
                        margin(context);
                        String capture=screenshot(context,"hoi-decisions-blocked-cost-"+suffix,size);screenshots.put("blocked-cost",capture);
                        var pixels=javax.imageio.ImageIO.read(Path.of(capture).toFile());int red=0;
                        for(int py=rectangle[1];py<rectangle[3];py++)for(int px=rectangle[0];px<rectangle[2];px++) {
                            int pixel=pixels.getRGB(px,py),r=pixel>>16&255,green=pixel>>8&255,blue=pixel&255;
                            if(r>120 && r>green*1.5 && r>blue*1.5)red++;
                        }
                        if(red<5)throw new AssertionError("The authoritative blocked cost quantity must render in its outer source red, not default white");
                    }
                    if(preview.name().startsWith("kor-scene"))for(String categoryId:List.of("KOR_coronavirus_category","KOR_korea_economy_category")) {
                        context.runOnClient(client->{
                            var screen=(DecisionScreen)client.gui.screen();
                            for(var category:preview.categories()) {
                                resetScroll(screen);
                                if(screen.collapsed(category.id())!=!category.id().equals(categoryId)) {
                                    locate(screen,category.name());
                                    var button=(Button)screen.children().stream().filter(child->child instanceof Button b && b.getMessage().getString().equals(sourceText().component(category.name()).getString())).findFirst().orElseThrow();
                                    button.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                                }
                            }
                            resetScroll(screen);
                        });
                        margin(context);
                        screenshots.put(categoryId,screenshot(context,"hoi-decisions-scene-"+categoryId.toLowerCase(Locale.ROOT)+"-"+suffix,size));
                        context.runOnClient(client->{var screen=(DecisionScreen)client.gui.screen();screen.mouseScrolled(3,UiMenuBar.height(screen.width)+53,0,-10000);});
                        margin(context);screenshots.put(categoryId+"-icons",screenshot(context,"hoi-decisions-scene-icons-"+categoryId.toLowerCase(Locale.ROOT)+"-"+suffix,size));
                    }
                    var missing = context.computeOnClient(client -> missingAssets(client, preview, assets));
                    int paneWidth=context.computeOnClient(client -> ((DecisionScreen)client.gui.screen()).unitHudLeft());
                    evidence.add(new Evidence(preview.name(), size[0], size[1], 2, paneWidth, preview.rows().size(), preview.categories().size(),
                            assets.size(), preview.sha256(), preview.metadata(), observed, missing, Map.copyOf(flagRows), Map.copyOf(screenshots)));
                    Path report = Path.of(evidence.getFirst().screenshots().get("top")).getParent().resolve("hoi-decisions-source-evidence.json");
                    Files.writeString(report, new Gson().toJson(evidence), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(client -> client.options.guiScale().set(original[0]));
            context.getInput().resizeWindow(original[1], original[2]);
            context.waitTicks(3);
        }
    }
    @SuppressWarnings("unchecked")
    private static void acceptDetail(DecisionScreen screen,String id,JsonObject exported){
        try{
            var requests=DecisionScreen.class.getDeclaredField("detailRequests");requests.setAccessible(true);((Map<String,Integer>)requests.get(screen)).put(id,0);
            var response=new dev.hoi.protocol.decision.DecisionDetailPayloads.Response(screen.token(),exported.toString());screen.update(response);
            var details=DecisionScreen.class.getDeclaredField("details");details.setAccessible(true);
            if(!response.view().equals(((Map<?,?>)details.get(screen)).get(id)))throw new AssertionError("Decision details must retain the matching authoritative session/revision: "+id);
            var hits=DecisionScreen.class.getDeclaredField("hits");hits.setAccessible(true);
            Object hit=null;for(var candidate:(List<?>)hits.get(screen)){var key=candidate.getClass().getDeclaredMethod("id");key.setAccessible(true);if(key.invoke(candidate).equals(id)){hit=candidate;break;}}
            if(hit==null)throw new AssertionError("Missing visible decision detail hit: "+id);
            var render=DecisionScreen.class.getDeclaredMethod("detailText",hit.getClass());render.setAccessible(true);String text=(String)render.invoke(screen,hit);
            for(var section:response.view().sections())for(var line:section.lines())if(!text.contains(line.text()))throw new AssertionError("Decision rendering changed a server source line: "+id);
            int complete=text.indexOf("선택 시 효과:"),modifier=text.indexOf("30일 동안:"),remove=text.indexOf("제거 시 효과:");
            if(complete<0||modifier<=complete||remove<=modifier||text.contains("else_if")||text.contains("조건부 원본 효과"))throw new AssertionError("Export detail requires complete, timed modifier and selected remove effects in source order: "+id);
            if(id.equals("KOR_export_to_pol_k2")&&!text.contains("구형이 먼저 제공됩니다."))throw new AssertionError("K2 export must preserve the source old-equipment sentence");
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static Preview read(Path directory, String name) throws IOException {
        byte[] bytes = Files.readAllBytes(directory.resolve(name + "-pages.json"));
        var raw = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonArray();
        var metadata = JsonParser.parseString(Files.readString(directory.resolve(name + "-metadata.json"))).getAsJsonObject();
        if (metadata.get("formatVersion").getAsInt() != 1 || !metadata.get("fixture").getAsBoolean()
                || !metadata.get("catalogSha256").getAsString().matches("[a-f0-9]{64}")
                || metadata.getAsJsonObject("sourceHashes").isEmpty() || metadata.get("catalogResource").getAsString().isBlank())
            throw new AssertionError("Source-backed sandbox fixture metadata required: " + name);
        var pages = new ArrayList<String>();
        var rows = new LinkedHashMap<String, DecisionPayloads.Row>();
        var categories = new LinkedHashMap<String, DecisionPayloads.Category>();
        var sourceIds = new ArrayList<String>();
        metadata.getAsJsonArray("sourceDecisionIds").forEach(value -> sourceIds.add(value.getAsString()));
        DecisionPayloads.View first = null;
        for (var element : raw) {
            String json = element.toString();
            var view = new DecisionPayloads.Response("00000000-0000-0000-0000-000000000000", json).view();
            if (first == null) first = view;
            if (view.page() != pages.size() || view.pageCount() != raw.size() || !view.country().equals(first.country())
                    || !view.session().equals(first.session()) || view.revision() != first.revision() || view.total() != first.total()
                    || !view.hud().equals(first.hud()) || !view.message().equals(first.message()))
                throw new AssertionError("Source fixture pages disagree: " + name);
            pages.add(json);
            for (var row : view.rows()) if (rows.putIfAbsent(row.id(), row) != null || !sourceIds.contains(row.id().split("@")[0]))
                throw new AssertionError("Duplicate or unproven source decision: " + name + "/" + row.id());
            for (var category : view.categories()) if (categories.putIfAbsent(category.id(), category) != null)
                throw new AssertionError("Duplicate source category: " + name + "/" + category.id());
        }
        if (first == null || first.total() != rows.size() || rows.size() != metadata.get("rowCount").getAsInt()
                || categories.size() != metadata.get("categoryCount").getAsInt() || pages.size() != metadata.get("pageCount").getAsInt()
                || !first.country().equals(metadata.get("country").getAsString())
                || !first.country().equals(name.split("-")[0].toUpperCase(Locale.ROOT))
                || !sourceIds.equals(rows.keySet().stream().map(id->id.split("@")[0]).distinct().toList()))
            throw new AssertionError("Source fixture aggregate disagrees with server metadata: " + name);
        try {
            return new Preview(name, List.copyOf(pages), List.copyOf(rows.values()), List.copyOf(categories.values()), metadata,
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
    private static Map<String, String> assets(Minecraft client) {
        var resource = client.getResourceManager().getResource(Identifier.parse("hoi:ui/decision_assets.json"))
                .orElseThrow(() -> new AssertionError("Missing original decision asset manifest"));
        try (var stream = resource.open()) {
            var root = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("version").getAsInt() != 1) throw new AssertionError("Unsupported decision asset manifest");
            var result = new TreeMap<String, String>();
            for (var entry : root.getAsJsonObject("sprites").entrySet()) {
                String path = entry.getValue().getAsString();
                if (!path.matches("[a-z0-9_/.-]+") || path.contains("..") || !exists(client, path))
                    throw new AssertionError("Declared source decision asset is missing: " + entry.getKey() + " -> " + path);
                result.put(entry.getKey(), path);
            }
            if (result.isEmpty()) throw new AssertionError("Empty decision source asset manifest");
            for (String path : List.of("decision/category", "decision/row", "decision/row_disabled", "decision/timer",
                    "decision/select", "decision/category_generic", "menu/decisions"))
                if (!exists(client, path)) throw new AssertionError("Missing decision renderer fallback: " + path);
            return Map.copyOf(result);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
    private static List<MissingSourceAsset> missingAssets(Minecraft client, Preview preview, Map<String, String> assets) {
        var result = new ArrayList<MissingSourceAsset>();
        for (var category : preview.categories()) {
            missingAsset(client, assets, result, category.id(), "icon", category.icon(), "decision/category_generic");
            missingAsset(client, assets, result, category.id(), "picture", category.picture(), "omitted");
            for (var line : new DecisionText().rows(category.description())) {
                var match = BODY_IMAGE.matcher(line.getString());
                if (match.find() && line.getString().replace(match.group(), "").isBlank()
                        && !assets.containsKey(match.group(1)) && !assets.containsKey("GFX_" + match.group(1)))
                    result.add(new MissingSourceAsset(category.id(), "body", match.group(1), "source text"));
            }
        }
        for (var row : preview.rows()) {
            missingAsset(client, assets, result, row.id(), "icon", row.icon(), "menu/decisions");
            if (preview.categories().stream().noneMatch(category -> category.id().equals(row.category())))
                missingAsset(client, assets, result, row.category(), "icon", row.categoryIcon(), "decision/category_generic");
        }
        return List.copyOf(result);
    }
    private static void missingAsset(Minecraft client, Map<String, String> assets, List<MissingSourceAsset> result,
            String owner, String kind, String source, String fallback) {
        if (source.isEmpty() || assets.containsKey(source)) return;
        String path = source.matches("[A-Za-z0-9_]+") ? "decision/" + kind + "/" + source.toLowerCase(Locale.ROOT) : "";
        if (path.isEmpty() || !exists(client, path)) result.add(new MissingSourceAsset(owner, kind, source, fallback));
    }
    private static boolean exists(Minecraft client, String path) {
        return client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + path + ".png")).isPresent();
    }
    private static Set<String> inspectNames(DecisionScreen screen, Preview preview) {
        var text = sourceText();
        var expected = new TreeSet<String>();
        for (var category : preview.categories()) expected.add(text.component(category.name()).getString());
        for (var row : preview.rows()) {
            expected.add(text.component(DecisionScreen.rowName(row)).getString());
            if (preview.categories().stream().noneMatch(category -> category.id().equals(row.category())))
                expected.add(text.component(row.categoryName()).getString());
        }
        var observed = new TreeSet<String>();
        resetScroll(screen);
        for (int step = 0; step < 10_000; step++) {
            for (var child : screen.children()) if (child instanceof Button button && button.getY() >= UiMenuBar.height(screen.width) + 52) {
                if (button.getX() < 0 || button.getRight() > screen.width || button.getBottom() > screen.height - 10)
                    throw new AssertionError("Source decision control exceeds viewport: " + preview.name());
                if(preview.name().equals("kor") && preview.rows().stream().anyMatch(row -> row.id().equals("KOR_export_to_tur_k9")
                        && text.component(row.name()).getString().equals(button.getMessage().getString())) && button.getHeight()>22)
                    throw new AssertionError("The longest Korean arms-export row must widen its pane and keep its name/cost on one line");
                observed.add(button.getMessage().getString());
            }
            if (!scroll(screen)) {
                var missing = new TreeSet<>(expected);
                missing.removeAll(observed);
                if (!missing.isEmpty()) throw new AssertionError("Source names unreachable by scrolling: " + preview.name() + " " + missing);
                return Set.copyOf(observed);
            }
        }
        throw new AssertionError("Source decision scrolling did not reach the end: " + preview.name());
    }
    public static int[] locate(DecisionScreen screen, String name) {
        String label = sourceText().component(name).getString();
        resetScroll(screen);
        for (int step = 0; step < 10_000; step++) {
            for (var child : screen.children()) if (child instanceof Button button && (button.getX() == 7 || button.getX() == 6)
                    && button.getY() >= UiMenuBar.height(screen.width) + 52 && button.getMessage().getString().equals(label))
                return new int[]{button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
            if (!scroll(screen)) break;
        }
        throw new AssertionError("Source tooltip row is unreachable: " + name);
    }
    private static int[] headerTooltip(DecisionScreen screen,DecisionPayloads.Category category) {
        String name=sourceText().component(category.name()).getString();
        try {
            var field=DecisionScreen.class.getDeclaredField("hits");field.setAccessible(true);
            boolean found=false;
            for(var hit:(List<?>)field.get(screen)) {
                var id=hit.getClass().getDeclaredMethod("id");id.setAccessible(true);
                if(!id.invoke(hit).equals("category/"+category.id()))continue;
                var tooltip=hit.getClass().getDeclaredMethod("tooltip");tooltip.setAccessible(true);
                if(!tooltip.invoke(hit).equals("§Y"+name+"§!"))throw new AssertionError("Category hover must contain only its original gold title: "+category.id());
                found=true;
            }
            if(!found)throw new AssertionError("Missing category hover: "+category.id());
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
        var button=(Button)screen.children().stream().filter(child->child instanceof Button control&&control.getMessage().getString().equals(name)).findFirst().orElseThrow();
        return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};
    }
    private static boolean scroll(DecisionScreen screen) {
        int before = screen.scrollOffset();
        int top = UiMenuBar.height(screen.width) + 52;
        screen.mouseScrolled(3, top + 1, 0, -Math.max(28, screen.height - top - 74) / 28.0);
        return screen.scrollOffset() != before;
    }
    private static DecisionText sourceText() {
        var text=new DecisionText();
        var resource=Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("hoi:ui/decision_flags.json"))
                .orElseThrow(()->new AssertionError("Missing decision flag bindings"));
        try(var input=resource.open()) {
            text.flags(JsonParser.parseString(new String(input.readAllBytes(),StandardCharsets.UTF_8)).getAsJsonObject());
        } catch(IOException error) {throw new UncheckedIOException(error);}
        return text;
    }
    private static void resetScroll(DecisionScreen screen) {
        screen.mouseScrolled(3, UiMenuBar.height(screen.width) + 53, 0, Integer.MAX_VALUE / 28.0);
        if (screen.scrollOffset() != 0) throw new AssertionError("Source preview margin scroll was consumed by a tooltip");
    }
    private static void margin(ClientGameTestContext context) {
        int y = context.computeOnClient(client -> UiMenuBar.height(client.gui.screen().width) + 53);
        context.getInput().setCursorPos(6, y * 2);
        context.waitTicks(2);
    }
    private static String screenshot(ClientGameTestContext context, String name, int[] size) throws IOException {
        Path path = context.takeScreenshot(name);
        var image = javax.imageio.ImageIO.read(path.toFile());
        if (image == null || image.getWidth() != size[0] || image.getHeight() != size[1])
            throw new AssertionError("Source screenshot has unexpected dimensions: " + path);
        return path.toString();
    }
}
