package dev.hoi.client.screen;

import dev.hoi.client.screen.occupation.OccupationScreen;
import dev.hoi.protocol.menu.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class ScreenPoliticsTooltipsTest {
    public static void run(ClientGameTestContext context,MenuView fixture) {
        MenuView base = fixture;
        String directory = System.getenv("HOI_POLITICS_PREVIEW");
        if (directory != null) try {
            base = new MenuPayloads.OpenScreen(java.nio.file.Files.readString(java.nio.file.Path.of(directory, "KOR.json"))).view();
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(c->c.options.guiScale().set(2));
        String description="점령지에 점령 정책을 설정하고, 해당 구역에 핵심 지역을 가지고 있는 국가를 해방할 수 있습니다.";
        for(boolean occupied:new boolean[]{false,true}) {
            var statuses=List.of(new MenuView.Entry("점령지",occupied?"3개 주":"0개 주",(occupied?"§a점령지를 관리하려면 클릭하십시오§r":"§c현재 점령하고 있는 비핵심 영토가 없습니다.§r")+"\n\n"+description,occupied?1:0),new MenuView.Entry("협력정부","0개국","§c현재 우리와 협력 중인 정부가 없음§r",0));
            var pages=base.pages().stream().map(page->page.tab()!=MenuTab.POLITICS?page:new MenuView.Page(page.tab(),page.sections().stream().map(section->new MenuView.Section(section.title(),section.icon(),java.util.stream.Stream.concat(section.entries().stream().filter(e->!e.name().equals("점령지")&&!e.name().equals("협력정부")),section==page.sections().getFirst()?statuses.stream():java.util.stream.Stream.empty()).map(entry-> {
                if(entry.name().equals("점령지"))return new MenuView.Entry("점령지",occupied?"3개 주":"0개 주",(occupied?"§a점령지를 관리하려면 클릭하십시오§r":"§c현재 점령하고 있는 비핵심 영토가 없습니다.§r")+"\n\n"+description,occupied?1:0);
                if(entry.name().equals("협력정부"))return new MenuView.Entry("협력정부","0개국","§c현재 우리와 협력 중인 정부가 없음§r",0);
                return entry;
            }).toList())).toList())).toList();
            var view=new MenuView(base.country(),base.countryName(),base.date(),base.speed(),pages,base.hud());
            context.setScreen(()->new ScreenMenu(view));context.waitTicks(3);
            for(int target=0;target<3;target++) {
                final int index=target;
                var point=context.computeOnClient(c->{
                    var screen=(ScreenMenu)c.gui.screen();
                    var box=index==2?screen.politicsLayout().parties():screen.politicsLayout().status(index);
                    int x=box.x()+box.width()/2,y=box.y()+(index==2?8:box.height()/2);
                    var hovered=screen.politicsHover(x,y);
                    if(hovered==null)throw new AssertionError("Political tooltip hit area missing: "+index);
                    if(index<2&&!hovered.name().equals(index==0?"점령지":"협력정부"))throw new AssertionError("Wrong status tooltip target");
                    if (index < 2) {
                        var button = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(hovered.name())).findFirst().orElseThrow();
                        var cursors = new ArrayList<com.mojang.blaze3d.platform.cursor.CursorType>();
                        var graphics = new net.minecraft.client.gui.GuiGraphicsExtractor(c, new net.minecraft.client.renderer.state.gui.GuiRenderState(), x, y) {
                            @Override public void requestCursor(com.mojang.blaze3d.platform.cursor.CursorType cursor) { cursors.add(cursor); }
                        };
                        button.extractRenderState(graphics, x, y, 0);
                        if (cursors.contains(com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED)) throw new AssertionError("Status hover must not request the prohibited cursor");
                        if (button.active && !cursors.contains(com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND)) throw new AssertionError("Available occupation keeps its clickable cursor");
                    }
                    double scale=c.getWindow().getGuiScale();return new double[]{x*scale,y*scale};
                });
                context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                if (index < 2) context.runOnClient(c -> {
                    var screen = (ScreenMenu)c.gui.screen();
                    var button = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals(index == 0 ? "점령지" : "협력정부")).findFirst().orElseThrow();
                    int x = button.getX() + button.getWidth() / 2, y = button.getY() + button.getHeight() / 2;
                    var graphics = new net.minecraft.client.gui.GuiGraphicsExtractor(c, new net.minecraft.client.renderer.state.gui.GuiRenderState(), x, y) {
                        @Override public void setTooltipForNextFrame(net.minecraft.client.gui.Font font, List<net.minecraft.util.FormattedCharSequence> lines,
                                Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> component,
                                net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner,
                                int mouseX, int mouseY, boolean focused, net.minecraft.resources.Identifier style) {
                            throw new AssertionError("Default button tooltip must not replace the source status description");
                        }
                    };
                    button.extractRenderState(graphics, x, y, 0);
                });
                context.takeScreenshot("hoi-politics-"+List.of("occupation","collaboration","party").get(target)+(occupied?"-active":"-inactive")+"-gui2");
            }
            if (!occupied && directory != null) {
                var parties = view.page(MenuTab.POLITICS).sections().stream().flatMap(section -> section.entries().stream())
                        .filter(entry -> entry.icon().startsWith("politics/party/") || entry.icon().startsWith("politics/ideology/")).toList();
                for (var expected : Map.of("더불어민주당", "문재인", "국민의힘", "황교안", "정의당", "심상정").entrySet()) {
                    int row = java.util.stream.IntStream.range(0, parties.size()).filter(i -> parties.get(i).name().equals(expected.getKey())).findFirst().orElseThrow();
                    var point = context.computeOnClient(c -> {
                        var screen = (ScreenMenu)c.gui.screen();
                        var box = screen.politicsLayout().parties();
                        int x = box.x() + box.width() / 2, y = box.y() + 8 + row * 11;
                        var hovered = screen.politicsHover(x, y);
                        if (hovered == null || !hovered.detail().contains("국가 원수: §e" + expected.getValue() + "§r"))
                            throw new AssertionError("Party hover lost its authoritative leader: " + expected.getKey());
                        double scale = c.getWindow().getGuiScale();
                        return new double[]{x * scale, y * scale};
                    });
                    context.getInput().setCursorPos(point[0], point[1]); context.waitTicks(3);
                    context.takeScreenshot("hoi-politics-party-leader-" + row + "-gui2");
                }
            }
            context.runOnClient(c->{
                var screen=(ScreenMenu)c.gui.screen();
                var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("점령지")).findFirst().orElseThrow();
                if(button.active!=occupied)throw new AssertionError("Occupation availability differs from authoritative view");
                if(occupied) {
                    button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
                    if(!(c.gui.screen() instanceof OccupationScreen))throw new AssertionError("Politics occupation button must open occupation screen");
                }
            });
        }
        context.setScreen(()->null);
        context.getInput().resizeWindow(1600,1000);
    }
}
