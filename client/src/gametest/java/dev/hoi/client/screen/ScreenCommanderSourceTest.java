package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

public final class ScreenCommanderSourceTest {
    private ScreenCommanderSourceTest() {}
    public static void run(ClientGameTestContext context) {
        ScreenCommanderRosterTest.run(context);
        ScreenAdmiralSourceTest.run(context);
        ScreenAirAceSourceTest.run(context);
        ScreenCommanderTraitTreeTest.run(context);
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            var quote=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-army-appointment.json")),true).view();
            var menu=new ProtocolMenuPayloads.OpenScreen(Files.readString(Path.of(directory,"kor-appointment-menu.json"))).view();
            if(quote.kind()!=ProtocolDialogView.Kind.COMMANDER||quote.choices().isEmpty())throw new AssertionError("Server appointment quote is required");
            var sent=new ArrayList<ProtocolDialogPayloads.Action>();var issued=quote.issued(UUID.randomUUID().toString(),3);
            context.getInput().resizeWindow(2560,1440);context.runOnClient(client->client.options.guiScale().set(2));
            context.runOnClient(client->dev.hoi.client.ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",menu.hud())));
            context.setScreen(()->new ScreenMenu(menu,ProtocolMenuTab.OFFICER_CORPS,()->{}));context.waitTicks(2);
            var parent=context.computeOnClient(client->client.gui.screen());
            context.setScreen(()->new ScreenCommanderRoster(issued,parent,sent::add));
            context.runOnClient(client->{
                for(String asset:List.of("panel","army","navy","marshal","attack","defense","planning","logistics","maneuver","coordination"))if(client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/commander/ui/"+asset+".png")).isEmpty())throw new AssertionError("Missing source commander frame "+asset);
                long candidates=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->issued.tiles().stream().anyMatch(t->t.name().equals(c.getMessage().getString()))).count();
                if(candidates!=4)throw new AssertionError("Roster must retain four complete rows above the footer: "+candidates);
                var screen=(ScreenCommanderRoster)client.gui.screen();var reached=new HashSet<String>();
                for(int step=0;step<issued.tiles().size();step++){
                    var visible=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->issued.tiles().stream().anyMatch(t->t.name().equals(c.getMessage().getString()))).toList();
                    visible.forEach(c->reached.add(c.getMessage().getString()));
                    var row=visible.getFirst();screen.mouseScrolled(row.getX()+1,row.getY()+1,0,-1);
                }
                for(var tile:issued.tiles())if(tile.leader()!=null&&!reached.contains(tile.name()))throw new AssertionError("Scrolling must reach server candidate "+tile.name());
                var row=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->reached.contains(c.getMessage().getString())).findFirst().orElseThrow();
                screen.mouseScrolled(row.getX()+1,row.getY()+1,0,issued.tiles().size());
            });
            context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-commander-army-appointment-gui2");
            if(issued.tiles().stream().anyMatch(t->!t.section().equals("remove")&&t.leader()==null))throw new AssertionError("Native rows require server supplied role, skills and capacity");
            String selected=issued.choices().getFirst().id();String name=issued.tiles().stream().filter(t->t.section().equals(selected)).findFirst().orElseThrow().name();
            context.runOnClient(client->{
                var screen=(ScreenCommanderRoster)client.gui.screen();
                var search=screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();
                search.setValue(name);
                if(screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).noneMatch(c->c.getMessage().getString().equals(name)))throw new AssertionError("Native search must retain the named server candidate");
                search=screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow();search.setValue("");
            });
            var position=context.computeOnClient(client->{
                var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                        .filter(c->c.getMessage().getString().equals(name)).findFirst().orElseThrow();
                return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};
            });
            context.getInput().setCursorPos(position[0]*2,position[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-army-candidate-tooltip-gui2");
            context.runOnClient(client->{
                var screen=(ScreenCommanderRoster)client.gui.screen();
                var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                if(sent.size()!=1||!sent.getFirst().equals(new ProtocolDialogPayloads.Action(issued.token(),3,selected)))throw new AssertionError("Commander click must send one issued choice");
                screen.update(issued.issued(issued.token(),2));screen.mouseClicked(click,false);
                if(sent.size()!=1)throw new AssertionError("Old reply unlocked a pending commander choice");
                screen.update(issued.issued(issued.token(),4));
                if(screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.getMessage().getString().equals(name)).noneMatch(c->c.active))throw new AssertionError("Authoritative refresh must unlock a rejected choice");
            });
            context.runOnClient(client->{
                var screen=(ScreenCommanderRoster)client.gui.screen();
                screen.children().stream().filter(c->c instanceof net.minecraft.client.gui.components.EditBox).map(c->(net.minecraft.client.gui.components.EditBox)c).findFirst().orElseThrow().setValue("전동진");
            });
            var biographyPosition=context.computeOnClient(client->{
                var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.getMessage().getString().equals("전동진")).findFirst().orElseThrow();
                return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};
            });
            context.getInput().setCursorPos(biographyPosition[0]*2,biographyPosition[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-jeon-source-biography-gui2");
            var jeonTile=issued.tiles().stream().filter(t->t.name().equals("전동진")).findFirst().orElseThrow();
            if(jeonTile.leader().abilityTooltips().size()!=4||!jeonTile.leader().abilityTooltips().get(2).contains("최대 계획: §a+8.0%§r"))throw new AssertionError("Server supplied current planning breakdown required");
            var abilityPosition=context.computeOnClient(client->{
                var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.getMessage().getString().equals("전동진")).findFirst().orElseThrow();
                return new int[]{button.getX()+156+2*13+6,button.getY()+10};
            });
            context.getInput().setCursorPos(abilityPosition[0]*2,abilityPosition[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-jeon-planning-breakdown-gui2");
            var promotion=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-marshal-discount.json")),true).view().issued(UUID.randomUUID().toString(),7);
            if(!promotion.tiles().getFirst().value().equals("지휘력 30")||!promotion.choices().getFirst().id().equals("promote"))throw new AssertionError("Source career-officer marshal quote must be30");
            if(promotion.tiles().getFirst().leader().traitDetails().stream().noneMatch(t->t.name().equals("직업장교")&&t.icon().equals("commander/traits/tfrkp/career_officer")&&t.tooltip().contains("평생을 장교로 헌신")))throw new AssertionError("Original career trait icon and individual description required");
            var promotionSent=new ArrayList<ProtocolDialogPayloads.Action>();
            context.setScreen(()->new ScreenCommanderRoster(promotion,parent,promotionSent::add));context.waitTicks(2);
            var promotionPosition=context.computeOnClient(client->{
                var button=((ScreenCommanderRoster)client.gui.screen()).choiceButton("promote");
                if(button==null||!button.active)throw new AssertionError("Discount promotion must expose the server-issued confirmation button");
                return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};
            });
            context.getInput().setCursorPos(promotionPosition[0]*2,promotionPosition[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-marshal-discount-quote-gui2");
            var careerTraitPosition=context.computeOnClient(client->{
                var tile=promotion.tiles().getFirst();var button=client.gui.screen().children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.getMessage().getString().equals(tile.name())).findFirst().orElseThrow();
                int index=java.util.stream.IntStream.range(0,tile.leader().traitDetails().size()).filter(i->tile.leader().traitDetails().get(i).name().equals("직업장교")).findFirst().orElseThrow();
                for(var trait:tile.leader().traitDetails())if(!trait.icon().isEmpty()&&client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/"+trait.icon()+".png")).isEmpty())throw new AssertionError("Missing declared source trait texture "+trait.icon());
                int x=dev.hoi.client.ui.UiFlagStack.left(index,tile.leader().traitDetails().size(),button.getX()+65,94,12),visibleWidth=dev.hoi.client.ui.UiFlagStack.visibleWidth(index,tile.leader().traitDetails().size(),button.getX()+65,94,12);
                return new int[]{x+visibleWidth/2,button.getY()+36};
            });
            context.getInput().setCursorPos(careerTraitPosition[0]*2,careerTraitPosition[1]*2);context.waitTicks(3);context.takeScreenshot("hoi-commander-career-trait-source-tooltip-gui2");
            context.runOnClient(client->{
                var screen=(ScreenCommanderRoster)client.gui.screen();
                var click=new net.minecraft.client.input.MouseButtonEvent(promotionPosition[0],promotionPosition[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                if(promotionSent.size()!=1||!promotionSent.getFirst().equals(new ProtocolDialogPayloads.Action(promotion.token(),7,"promote")))throw new AssertionError("Marshal discount click must send one server-issued promotion");
            });
            var selectedView=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-army-selected.json")),true).view();
            if(selectedView.tiles().stream().noneMatch(t->t.value().equals("선택 중"))||selectedView.choices().stream().noneMatch(c->c.id().equals("remove")))throw new AssertionError("Selected server fixture must contain appointment and removal");
            var selectedMenu=new ProtocolMenuPayloads.OpenScreen(Files.readString(Path.of(directory,"kor-selected-menu.json"))).view();
            context.runOnClient(client->dev.hoi.client.ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR",selectedMenu.hud())));
            context.setScreen(()->new ScreenMenu(selectedMenu,ProtocolMenuTab.OFFICER_CORPS,()->{}));context.waitTicks(2);
            var selectedParent=context.computeOnClient(client->client.gui.screen());
            context.setScreen(()->new ScreenCommanderRoster(selectedView.issued(UUID.randomUUID().toString(),0),selectedParent,a->{}));
            context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-commander-army-selected-gui2");
        } catch(java.io.IOException error){throw new AssertionError("Commander server fixtures unavailable",error);}
    }
}
