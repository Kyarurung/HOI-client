package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolMenuTab;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.util.List;

final class ScreenProductionPresentationTest {
    private ScreenProductionPresentationTest() {}
    static void run(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));
        var fixture=ScreenIndustryChecks.tankFixture("production-source-fixture.json");
        var designer=ScreenIndustryChecks.tankFixture("initial-air-fixture.json").tankDesigner();
        context.getInput().setCursorPos(2400,1300);
        context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,fixture.session(),fixture,request->{}));
        context.waitTicks(3);
        try {
            var normal=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-source-gui2").toFile());
            var bounds=context.computeOnClient(client->{
                var screen=(ScreenIndustry)client.gui.screen();
                var image=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                return new int[]{image.getX()*2,image.getY()*2,image.getWidth()*2,image.getHeight()*2};
            });
            var modal=fixture.withTankDesigner(designer);
            context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,modal.session(),modal,request->{}));
            context.waitTicks(3);
            var opened=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-production-designer-background-gui2").toFile());
            int matching=0,total=0;
            for(int y=bounds[1]+4;y<bounds[1]+bounds[3]-4;y++)for(int x=bounds[0]+4;x<bounds[0]+bounds[2]-4;x++) {
                total++;
                if(normal.getRGB(x,y)==opened.getRGB(x,y))matching++;
            }
            if(matching<total*.98)throw new AssertionError("Opening designer changed production image pixels: "+matching+"/"+total);
            context.runOnClient(client->{
                if(client.gui.screen().children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")))throw new AssertionError("Background production image must not receive modal clicks");
            });
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        for(String file:List.of("production-source-fixture.json","production-china-fixture.json")) {
            var source=ScreenIndustryChecks.tankFixture(file);
            context.setScreen(()->new ScreenIndustry(ProtocolMenuTab.PRODUCTION,source.session(),source,request->{}));
            context.waitTicks(3);
            var point=context.computeOnClient(client->{
                var picture=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().endsWith(" · 장비 교체")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                return new double[]{(picture.getX()+8)*client.getWindow().getGuiScale(),(picture.getY()+8)*client.getWindow().getGuiScale()};
            });
            context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
            context.takeScreenshot("hoi-production-equipment-tooltip-"+source.country().toLowerCase(java.util.Locale.ROOT)+"-gui2");
            context.getInput().setCursorPos(2400,1300);
            context.runOnClient(client->{
                var first=source.equipment().stream().filter(e->!e.stats().isEmpty()).findFirst().orElseThrow();
                String tooltip=ScreenProductionTooltips.equipment(source,first);
                if(tooltip.contains("원본 기본 능력치")||!tooltip.contains("\n\n"))throw new AssertionError("Source equipment tooltip sections are missing");
                var air=client.gui.screen().children().stream().filter(w->w instanceof net.minecraft.client.gui.components.Button b&&b.getMessage().getString().equals("항공기 제작")).map(w->(net.minecraft.client.gui.components.Button)w).findFirst().orElseThrow();
                air.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER,0,0));
            });
            context.waitTicks(3);
            context.takeScreenshot("hoi-production-air-missions-"+source.country().toLowerCase(java.util.Locale.ROOT)+"-gui2");
        }
    }
}
