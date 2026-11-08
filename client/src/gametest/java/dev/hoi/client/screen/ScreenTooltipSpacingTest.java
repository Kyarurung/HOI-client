package dev.hoi.client.screen;

import dev.hoi.client.ui.tooltip.TooltipSpacingTest;
import dev.hoi.client.screen.commander.CommanderTraits;
import dev.hoi.client.screen.diplomacy.DiplomacyCountryScreen;
import dev.hoi.protocol.country.*;
import dev.hoi.protocol.dialog.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;

public final class ScreenTooltipSpacingTest {
    private ScreenTooltipSpacingTest() {}
    public static void experience(ClientGameTestContext game) {
        try {
            Path directory=Path.of(Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW")));
            for(String name:List.of("kor-jeon-trait-tree","kor-navy-trait-tree")) {
                var tree=new DialogPayloads.Show(Files.readString(directory.resolve(name+".json")),true).view().issued(UUID.randomUUID().toString(),0);
                for(String id:name.contains("navy")?List.of("seawolf"):List.of("desert_fox","commando","fortress_buster")) {
                    var tile=tree.tiles().stream().filter(t->t.section().equals(id)).findFirst().orElseThrow();
                    if(tile.detail().contains("?/700")||tile.detail().contains("개별 특성 경험치: 미확인"))throw new AssertionError("Trait fixture must contain authoritative experience: "+id);
                    if(!tile.detail().contains("0/700"))throw new AssertionError("Starting trait experience must be zero: "+id);
                    for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                        game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->client.options.guiScale().set(2));
                        game.setScreen(()->new CommanderTraits(tree,null,a->{}));game.waitTicks(3);
                        var point=game.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var node=tile.traitNode();if(!screen.visible(node))throw new AssertionError("Experience trait must be visible: "+id);return new int[]{screen.x((node.terrain()?469.5:147)+104*node.column()+30),screen.y(63+27.5*node.row())};});
                        game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);
                        var bounds=game.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();var tip=screen.tooltipBounds();if(tip==null||tip.x()<4||tip.y()<4||tip.x()+tip.width()+4>screen.width||tip.y()+tip.height()+4>screen.height)throw new AssertionError("Experience tooltip must fit viewport: "+id);return tip;});
                        var image=game.takeScreenshot("hoi-trait-experience-"+id+"-"+size[0]+"x"+size[1]+"-gui2");
                        Files.writeString(image.resolveSibling(image.getFileName()+".json"),new com.google.gson.Gson().toJson(Map.of("fixture",name,"section",id,"source",tile.detail(),"cursor",List.of(point[0]*2,point[1]*2),"bounds",bounds)));
                    }
                }
            }
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {game.setScreen(()->null);}
    }
    public static void run(ClientGameTestContext game) {
        try {
            Path directory=Path.of(Objects.requireNonNull(System.getenv("HOI_COMMANDER_PREVIEW")));
            var country=new CountryPayloads.Response(UUID.randomUUID().toString(),Files.readString(directory.resolve("kor-diplomacy-PRK.json"))).view();
            var tree=new DialogPayloads.Show(Files.readString(directory.resolve("kor-navy-trait-tree.json")),true).view().issued(UUID.randomUUID().toString(),0);
            var guarantee=country.actions().entries().stream().filter(a->a.id().equals("guarantee")).findFirst().orElseThrow();
            var trait=tree.tiles().stream().filter(t->t.section().equals("inshore_fighter")).findFirst().orElseThrow();
            TooltipSpacingTest.components(game);TooltipSpacingTest.verify(game,guarantee.detail());TooltipSpacingTest.verify(game,trait.detail());
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);game.runOnClient(client->client.options.guiScale().set(2));
                game.setScreen(()->new DiplomacyCountryScreen(country,a->{}));game.waitTicks(3);
                var point=game.computeOnClient(client->{var screen=(DiplomacyCountryScreen)client.gui.screen();var button=(Button)screen.children().stream().filter(c->c instanceof Button b&&b.getMessage().getString().equals(guarantee.name())).findFirst().orElseThrow();return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
                game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(15);game.takeScreenshot("hoi-spacing-guarantee-"+size[0]+"x"+size[1]+"-gui2");
                game.setScreen(()->new CommanderTraits(tree,null,a->{}));game.waitTicks(3);
                point=game.computeOnClient(client->{var screen=(CommanderTraits)client.gui.screen();if(!screen.visible(trait.traitNode()))throw new AssertionError("Source coastal trait must be visible");return new int[]{screen.x(500),screen.y(63+27.5*trait.traitNode().row())};});
                game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);
                game.runOnClient(client->{if(((CommanderTraits)client.gui.screen()).tooltipBounds()==null)throw new AssertionError("Coastal trait tooltip was not drawn");});
                game.takeScreenshot("hoi-spacing-inshore-fighter-"+size[0]+"x"+size[1]+"-gui2");
            }
        } catch(java.io.IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {game.setScreen(()->null);}
    }
}
