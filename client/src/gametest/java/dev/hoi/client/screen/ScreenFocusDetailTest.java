package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.protocol.ProtocolFocusPayloads;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenFocusDetailTest {
    private ScreenFocusDetailTest() {}
    public static void run(ClientGameTestContext context) {
        sourceScenes(context);
        ProtocolFocusPayloads.View source;
        try (var input=ScreenFocusDetailTest.class.getResourceAsStream("/hoi/focus/KOR.json")) {
            if(input==null)throw new AssertionError("Missing source focus tree");
            source=new Gson().fromJson(new String(input.readAllBytes(),StandardCharsets.UTF_8),ProtocolFocusPayloads.View.class);
        } catch(java.io.IOException failure){throw new AssertionError(failure);}
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(2);
            for(String id:List.of("KOR_discussion_of_covid_response_measures","KOR_mitigation_response_measures",
                    "KOR_a_new_decade_of_the_twentyonest_century","KOR_proud_k_quarantine",
                    "KOR_successful_vaccine_development","KOR_start_of_vaccine_research",
                    "KOR_school_closure_order","KOR_conducting_epidemiological_investigation")) {
                context.setScreen(() -> new ScreenFocus(id));
                context.runOnClient(client -> {
                    var screen=(ScreenFocus)client.gui.screen();
                    screen.update(ProtocolFocusPayloads.Response.of(screen.token(),source));
                    check(screen.selectedId().equals(id),"Source tree selection must preserve the actual focus ID");
                    check(screen.sourceDetailText().isEmpty(),"Tree-only capture must not invent missing server detail");
                });
                context.getInput().setCursorPos(0,0);context.waitTicks(2);
                context.takeScreenshot("hoi-focus-detail-tree-only-"+id+"-"+size[0]+"-gui2");
            }
            var node=source.nodes().getFirst();
            var line=new ProtocolFocusPayloads.DetailLine("§Y합성 스크롤 검사 0.00% +0.01§!\n\n",1,ProtocolFocusPayloads.Assessment.UNMET,"test-fixture/scroll",true);
            var lines=java.util.Collections.nCopies(40,line);
            var detail=new ProtocolFocusPayloads.Detail(node.id(),node.name(),(node.description()+"\n\n").repeat(40),24,0,0,0,0,"test-fixture/scroll",1,lines,lines,false,"",false);
            var screen=context.computeOnClient(client -> new DetailScreen(node,detail));
            context.setScreen(() -> screen);context.waitTicks(2);
            context.runOnClient(client -> {
                check(client.getWindow().getGuiScale()==2,"Detail regression requires actual GUI2");
                int total=screen.panel.scroll();
                var requirements=screen.panel.requirements();
                screen.panel.scroll(requirements.x()+1,requirements.y()+1,-3);
                check(screen.panel.scroll()==total,"Requirement wheel must leave description/effect offsets unchanged");
                screen.panel.reset();
                var description=screen.panel.description();
                double x=description.x()+description.width()+4;
                check(screen.panel.press(x,description.y()+10),"Original scrollbar track must accept left pointer input");
                check(screen.panel.drag(description.y()+description.height()-10),"Held thumb must follow the pointer");
                check(screen.panel.scroll()>0,"Dragging the description thumb must expose overflow");
                int offset=screen.panel.scroll();screen.panel.release();
                check(!screen.panel.drag(description.y()+10) && screen.panel.scroll()==offset,"Released scrollbar must stop moving");
                screen.panel.reset();
                var rewards=screen.panel.rewards();
                check(screen.panel.press(rewards.x()+rewards.width()+4,rewards.y()+rewards.height()-2),"Original down arrow must accept input");
                check(screen.panel.scroll()>0,"Effect down arrow must move only the effect section");
                screen.panel.reset();
                var bounds=screen.panel.bounds();
                check(bounds.x()>=0 && bounds.y()>=0 && bounds.x()+bounds.width()<=screen.width && bounds.y()+bounds.height()<=screen.height,"Compact detail must remain within the viewport");
            });
            context.getInput().setCursorPos(0,0);context.waitTicks(2);
            context.takeScreenshot("hoi-focus-detail-synthetic-scroll-"+size[0]+"-gui2");
        }
        context.setScreen(() -> null);context.getInput().resizeWindow(1600,1000);context.waitTicks(2);
    }
    private static void sourceScenes(ClientGameTestContext context) {
        String directory=System.getenv("HOI_FOCUS_PREVIEW");
        if(directory==null || directory.isBlank())return;
        var path=Path.of(directory);
        var gson=new Gson();
        try {
            var view=gson.fromJson(Files.readString(path.resolve("kor-oil-shock-view.json"),StandardCharsets.UTF_8),ProtocolFocusPayloads.View.class);
            var details=com.google.gson.JsonParser.parseString(Files.readString(path.resolve("kor-oil-shock-details.json"),StandardCharsets.UTF_8)).getAsJsonObject();
            var scenes=List.of(
                    new String[]{"20261014201913_1","KOR_discussion_of_covid_response_measures"},
                    new String[]{"20261014201915_1","KOR_mitigation_response_measures"},
                    new String[]{"20261014201916_1","KOR_a_new_decade_of_the_twentyonest_century"},
                    new String[]{"20261014201918_1","KOR_fourth_oil_shock"},
                    new String[]{"20261014201919_1","KOR_the_coming_economic_crisis"},
                    new String[]{"20261014201922_1","KOR_diversification_of_oil_imports"},
                    new String[]{"20261014201923_1","KOR_diversification_of_oil_imports"},
                    new String[]{"20261014201925_1","KOR_green_new_deal"},
                    new String[]{"20261014201927_1","KOR_follow_up_measures_fake"},
                    new String[]{"20261014201930_1","KOR_proud_k_quarantine"},
                    new String[]{"20261014201932_1","KOR_successful_vaccine_development"},
                    new String[]{"20261014201935_1","KOR_start_of_vaccine_research"},
                    new String[]{"20261014201936_1","KOR_school_closure_order"},
                    new String[]{"20261014201938_1","KOR_conducting_epidemiological_investigation"},
                    new String[]{"20261014201940_1","KOR_a_battleground_of_great_powers"},
                    new String[]{"20261014201945_1","KOR_cooperative_vaccine_development"});
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(2);
                for(var scene:scenes) {
                    check(details.has(scene[1]),"Missing authoritative focus detail: "+scene[1]);
                    var detail=gson.fromJson(details.get(scene[1]),ProtocolFocusPayloads.Detail.class);
                    var snapshot=new ProtocolFocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),view.revision(),view.message(),view.hud(),view.nodes(),view.cancelable(),detail,null);
                    context.setScreen(() -> new ScreenFocus(scene[1]));
                    context.runOnClient(client -> {
                        var screen=(ScreenFocus)client.gui.screen();
                        screen.update(ProtocolFocusPayloads.Response.of(screen.token(),snapshot));
                        check(screen.detailId().equals(scene[1]),"Source detail must reach the selected focus");
                        check(screen.sourceDetailText().equals(detail.description()+"\n"+String.join("\n",detail.requirements().stream().map(ProtocolFocusPayloads.DetailLine::text).toList())+"\n"+String.join("\n",detail.rewards().stream().map(ProtocolFocusPayloads.DetailLine::text).toList())),"Source text must remain unchanged");
                        check(client.getWindow().getGuiScale()==2,"Source focus scene requires actual GUI2");
                    });
                    context.getInput().setCursorPos(0,0);context.waitTicks(2);
                    if(scene[0].equals("20261014201923_1")) {
                        context.runOnClient(client -> {
                            var screen=(ScreenFocus)client.gui.screen();
                            int[] bounds=screen.detailBounds();
                            screen.mouseScrolled(bounds[0]+30,bounds[1]+bounds[3]-15,0,-7);
                            check(screen.detailScroll()>0,"Scrolled reference must expose later authoritative rewards");
                        });
                        context.waitTicks(2);
                    }
                    context.takeScreenshot("hoi-focus-source-"+scene[0]+"-"+size[0]+"-gui2");
                }
            }
        } catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static final class DetailScreen extends Screen {
        private final ScreenFocusDetail panel=new ScreenFocusDetail();
        private final ProtocolFocusPayloads.Node node;
        private final ProtocolFocusPayloads.Detail detail;
        private DetailScreen(ProtocolFocusPayloads.Node node,ProtocolFocusPayloads.Detail detail){super(Component.literal("Synthetic focus detail scroll regression"));this.node=node;this.detail=detail;}
        @Override protected void init(){panel.layout(width,height,65);}
        @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta){graphics.fill(0,0,width,height,0xFF030804);graphics.nextStratum();panel.draw(graphics,font,node,detail,false,mouseX,mouseY);}
        @Override public void removed(){panel.closeResources();super.removed();}
    }
}
