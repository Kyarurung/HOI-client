package dev.hoi.client.screen.industry;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.research.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class IndustryMioAvailabilityTest {
    private IndustryMioAvailabilityTest() {}
    public static void run(ClientGameTestContext context) {
        var root=Path.of(Objects.requireNonNull(System.getenv("HOI_MIO_PREVIEW")));
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);
            context.runOnClient(client->client.options.guiScale().set(2));
            var view=new IndustryPayloads.Response(read(root.resolve("designer.json"))).view();
            var requests=new ArrayList<IndustryPayloads.Request>();
            context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,view.session(),view,requests::add));
            context.runOnClient(client->click(client.gui.screen(),button(client.gui.screen()," · 군수산업체")));
            context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(3);
            context.takeScreenshot("hoi-mio-availability-list-"+size[0]+"-gui2");
            var choice=view.tankDesigner().designTeam().choices().getFirst();
            hover(context,choice.manufacturer().name(),"designer",size[0]);
            check(requests.isEmpty(),"Opening and hovering the list must not mutate the design");
            context.runOnClient(client->click(client.gui.screen(),button(client.gui.screen(),choice.manufacturer().name())));
            check(requests.size()==1&&requests.getFirst().other().equals(choice.manufacturer().id())&&requests.getFirst().session().equals(view.session())&&requests.getFirst().revision()==view.revision(),"Selection preserves exact server id, session and revision");
            var research=new ResearchPayloads.Response(read(root.resolve("research.json"))).view();
            var researchRequests=new ArrayList<ResearchPayloads.Request>();
            context.setScreen(()->new ResearchScreen(research,researchRequests::add,tab->{}));
            context.waitTicks(2);context.runOnClient(client->researchRequests.clear());
            var team=research.designTeams().stream().filter(t->t.id().equals(research.slots().getFirst().designTeam())).findFirst().orElseThrow();
            hover(context,team.name(),"research",size[0]);
            check(researchRequests.isEmpty(),"Research manufacturer hover must not mutate state");
        }
        context.setScreen(()->null);
    }
    private static void hover(ClientGameTestContext context,String name,String scene,int width) {
        var point=context.computeOnClient(client->{var b=button(client.gui.screen(),name);double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+Math.min(80,b.getWidth()/2))*scale,(b.getY()+Math.min(10,b.getHeight()/2))*scale};});
        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(5);
        context.takeScreenshot("hoi-mio-availability-"+scene+"-"+width+"-gui2");
    }
    private static String read(Path path) {
        try{return Files.readString(path);}catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
    }
    private static Button button(Screen screen,String name) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().endsWith(name)).findFirst().orElseThrow(()->new AssertionError("Missing control "+name));
    }
    private static void click(Screen screen,Button button) {
        var event=new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+Math.min(10,button.getHeight()/2.0),new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        screen.mouseClicked(event,false);screen.mouseReleased(event);
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
