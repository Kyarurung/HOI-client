package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;

public final class ScreenDeterminateCommandDialogsTest {
    private ScreenDeterminateCommandDialogsTest() {}
    public static void run(ClientGameTestContext context,String directory) throws java.io.IOException {
        for(String name:List.of("naval-areas-source-kor-dialog","naval-reinforcement-source-kor-dialog")){
            var view=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,name+".json")),true).view().issued(UUID.randomUUID().toString(),1);
            if(view.kind()!=ProtocolDialogView.Kind.COMMANDER||view.tiles().isEmpty())throw new AssertionError("Actual source command information fixture required: "+name);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));var issued=new ArrayList<ProtocolDialogPayloads.Action>();context.setScreen(()->new ScreenCommanderRoster(view,null,issued::add));context.waitTicks(3);
                var hover=new int[2];context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();var widgets=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).toList();for(var widget:widgets)if(widget.getX()<0||widget.getY()<0||widget.getX()+widget.getWidth()>client.getWindow().getGuiScaledWidth()||widget.getY()+widget.getHeight()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Native rule dialog escaped GUI2 viewport: "+name);var row=widgets.stream().filter(w->w.getHeight()>30).min(Comparator.comparingInt(AbstractWidget::getY)).orElseThrow(()->new AssertionError("Actual source information row must be visible"));double scale=client.getWindow().getGuiScale();hover[0]=(int)Math.round((row.getX()+row.getWidth()/2.0)*scale);hover[1]=(int)Math.round((row.getY()+row.getHeight()/2.0)*scale);if(!issued.isEmpty())throw new AssertionError("Read-only information rendering must not issue an action");});
                context.getInput().setCursorPos(hover[0],hover[1]);context.waitTicks(16);context.takeScreenshot("hoi-native-command-dialog-"+name+"-"+size[0]+"x"+size[1]+"-gui2");
                context.runOnClient(client->{if(!issued.isEmpty())throw new AssertionError("Hovering original rules cannot submit a state mutation");});
            }
        }
    }
}
