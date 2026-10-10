package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;

public final class ScreenDeterminateCommandDialogsTest {
    private ScreenDeterminateCommandDialogsTest() {}
    public static void run(ClientGameTestContext context,String directory) throws java.io.IOException {
        for(String name:List.of("army-ability-force_attack-source-kor-dialog","army-ability-last_stand-source-kor-dialog","army-ability-staff_office_plan-source-kor-dialog","army-ability-extra_suplies-source-kor-dialog","army-ability-siege_artillery-source-kor-dialog","army-ability-glider_planes-source-kor-dialog","army-ability-faster_naval_invasion_planning-source-kor-dialog","army-ability-probing_attack-source-kor-dialog","army-ability-makeshift_bridges-source-kor-dialog","army-ability-requisition_winter_gear-source-kor-dialog","army-ability-tactic_gas_bombardment-source-kor-dialog","army-ability-ALL-source-kor-dialog","naval-areas-source-kor-dialog","naval-reinforcement-source-kor-dialog")){
            var view=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,name+".json")),true).view().issued(UUID.randomUUID().toString(),1);
            if(view.kind()!=ProtocolDialogView.Kind.COMMANDER||view.tiles().isEmpty())throw new AssertionError("Actual source command information fixture required: "+name);
            if(name.startsWith("army-ability-")&&(!view.choices().equals(List.of(new ProtocolDialogView.Choice("cancel","닫기")))||view.tiles().stream().anyMatch(t->t.detail().isBlank())))throw new AssertionError("Native ability information cannot expose guessed activation or omit original rules");
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
                context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));var issued=new ArrayList<ProtocolDialogPayloads.Action>();context.setScreen(()->new ScreenCommanderRoster(view,null,issued::add));context.waitTicks(3);
                var hover=new int[2];var previousHover=new int[]{-1,-1};context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();var widgets=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).toList();for(var widget:widgets)if(widget.getX()<0||widget.getY()<0||widget.getX()+widget.getWidth()>client.getWindow().getGuiScaledWidth()||widget.getY()+widget.getHeight()>client.getWindow().getGuiScaledHeight())throw new AssertionError("Native rule dialog escaped GUI2 viewport: "+name);var row=widgets.stream().filter(w->w.getHeight()>30).min(Comparator.comparingInt(AbstractWidget::getY)).orElseThrow(()->new AssertionError("Actual source information row must be visible"));double scale=client.getWindow().getGuiScale();var previous=widgets.stream().filter(w->w.getHeight()>30).sorted(Comparator.comparingInt(AbstractWidget::getY)).skip(2).findFirst();if(name.startsWith("army-ability-")&&previous.isPresent()){previousHover[0]=(int)Math.round((previous.get().getX()+previous.get().getWidth()/2.0)*scale);previousHover[1]=(int)Math.round((previous.get().getY()+previous.get().getHeight()/2.0)*scale);}hover[0]=(int)Math.round((row.getX()+row.getWidth()/2.0)*scale);hover[1]=(int)Math.round((row.getY()+row.getHeight()/2.0)*scale);if(!issued.isEmpty())throw new AssertionError("Read-only information rendering must not issue an action");});
                if(previousHover[0]>=0){context.getInput().setCursorPos(previousHover[0],previousHover[1]);context.waitTicks(3);context.runOnClient(client->checkTooltip((ScreenCommanderRoster)client.gui.screen(),view.tiles().get(2).detail(),false,name+"/previous-row"));}
                context.getInput().setCursorPos(hover[0],hover[1]);context.waitTicks(16);context.takeScreenshot("hoi-native-command-dialog-"+name+"-"+size[0]+"x"+size[1]+"-gui2");
                context.runOnClient(client->{if(!issued.isEmpty())throw new AssertionError("Hovering original rules cannot submit a state mutation");});
                if(name.startsWith("army-ability-")){
                    var tooltipHover=new int[2];
                    context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();checkTooltip(screen,view.tiles().getFirst().detail(),false,name);var tip=screen.detailTooltip();double scale=client.getWindow().getGuiScale();tooltipHover[0]=(int)Math.round((tip.x()+tip.width()/2.0)*scale);tooltipHover[1]=(int)Math.round((tip.y()+tip.height()/2.0)*scale);});
                    context.getInput().setCursorPos(tooltipHover[0],tooltipHover[1]);context.waitTicks(3);
                    context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();checkTooltip(screen,view.tiles().getFirst().detail(),false,name);double scale=client.getWindow().getGuiScale();if(!screen.mouseScrolled(tooltipHover[0]/scale,tooltipHover[1]/scale,0,-10000))throw new AssertionError("Original rules tooltip must consume its wheel input");});
                    context.waitTicks(3);
                    context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();checkTooltip(screen,view.tiles().getFirst().detail(),true,name);if(!issued.isEmpty())throw new AssertionError("Reading the last original rule cannot issue a command");});
                    context.takeScreenshot("hoi-native-command-dialog-"+name+"-"+size[0]+"x"+size[1]+"-gui2-tooltip-bottom");
                    context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();var tip=screen.detailTooltip();screen.mouseScrolled(tip.x()+tip.width()/2.0,tip.y()+tip.height()/2.0,0,10000);});context.waitTicks(3);
                    context.runOnClient(client->checkTooltip((ScreenCommanderRoster)client.gui.screen(),view.tiles().getFirst().detail(),false,name));
                    context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(3);context.runOnClient(client->{if(((ScreenCommanderRoster)client.gui.screen()).detailTooltip()!=null)throw new AssertionError("Leaving the information row and tooltip must dismiss it");});
                }
            }
        }
    }
    private static void checkTooltip(ScreenCommanderRoster screen,String original,boolean bottom,String name){
        var tip=screen.detailTooltip();
        if(tip==null)throw new AssertionError("Source rule tooltip must exist after hovering its actual row: "+name);
        if(!screen.detailText().equals(original))throw new AssertionError("Source rule tooltip must match the hovered row including all original blank lines: "+name+" expectedHash="+original.hashCode()+" actualHash="+screen.detailText().hashCode());
        if(tip.x()-4<0||tip.y()-4<0||tip.x()+tip.width()+4>screen.width||tip.y()+tip.height()+4>screen.height)throw new AssertionError("Source rule tooltip escaped the GUI2 viewport: "+name);
        if(tip.scroll()!=(bottom?tip.maxScroll():0))throw new AssertionError("Source rule tooltip must expose its "+(bottom?"last":"first")+" line: "+name);
    }
}
