package dev.hoi.client.map;

import dev.hoi.client.screen.ScreenMapCursor;
import dev.hoi.protocol.ProtocolCommanderHud;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class MapSelectorHudTest {
    private static <T> T onServer(ClientGameTestContext game,net.minecraft.server.MinecraftServer server,java.util.function.Supplier<T> action){var pending=server.submit(action);game.waitFor(client->pending.isDone());return pending.join();}
    private static void onServer(ClientGameTestContext game,net.minecraft.server.MinecraftServer server,Runnable action){onServer(game,server,()->{action.run();return true;});}
    static void run(ClientGameTestContext game,int[] size,ProtocolCommanderHud.View source){
        var server=game.computeOnClient(client->client.getSingleplayerServer());
        var player=onServer(game,server,()->server.getPlayerList().getPlayers().getFirst());
        var previous=onServer(game,server,()->player.getInventory().getNonEquipmentItems().stream().map(ItemStack::copy).toList());
        var dimension=onServer(game,server,()->player.level().dimension());
        var position=onServer(game,server,player::position);
        try{
            game.runOnClient(client->{MapCommanderHud.accept(new ProtocolCommanderHud.View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true));MapCommanderPanels.select("A",false);});game.setScreen(()->null);game.waitTicks(3);
            game.runOnClient(client->{if(MapSelectorHud.active()||!MapCommanderHud.hidesVanillaHud())throw new AssertionError("Running ordinary-world cards must hide the vanilla HUD");});
            game.takeScreenshot("hoi-army-outside-map-hud-hidden-"+size[0]+"x"+size[1]+"-gui2");
            for(Boolean running:new Boolean[]{false,null,true}){
                game.runOnClient(client->MapCommanderHud.accept(new ProtocolCommanderHud.View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),running)));game.waitTicks(2);
                game.runOnClient(client->{var u=MapCommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());if(MapCommanderHud.hidesVanillaHud()!=Boolean.TRUE.equals(running)||MapCommanderLayout.cardsBottom(u)!=u.height()-(Boolean.TRUE.equals(running)?0:u.size(82)))throw new AssertionError("Running/paused/legacy HUD visibility and card position must change together");});
                game.takeScreenshot("hoi-army-outside-map-"+(running==null?"legacy":running?"resumed":"paused")+"-"+size[0]+"x"+size[1]+"-gui2");
            }
            game.runOnClient(client->MapCommanderHud.reset());game.waitTicks(2);
            game.runOnClient(client->{if(MapSelectorHud.active()||MapCommanderHud.cardsVisible())throw new AssertionError("No map or cards must restore the vanilla HUD");});
            game.takeScreenshot("hoi-army-outside-map-hud-restored-"+size[0]+"x"+size[1]+"-gui2");
            onServer(game,server,()->{
                var map=server.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse("hoi:atlas_ui_test")));if(map==null)throw new AssertionError("Synthetic atlas test dimension must load");
                player.teleportTo(map,0,80,0,Set.of(),0,0,false);
                var modes=List.of("army","navy","air","operatives","supply","resistance","compliance","resources","factions");var names=List.of("기본","전략 해군","전략 공군","공작원","보급","저항도","순응도","자원","세력");
                for(int i=0;i<9;i++){var item=new ItemStack(Items.CARROT_ON_A_STICK);item.set(DataComponents.ITEM_MODEL,Identifier.parse("hoi:map/selector/"+modes.get(i)));item.set(DataComponents.CUSTOM_NAME,Component.literal(names.get(i)).withStyle(net.minecraft.ChatFormatting.GOLD).append(Component.literal(" 지도 모드").withStyle(net.minecraft.ChatFormatting.WHITE)));player.getInventory().setItem(i,item);}player.inventoryMenu.broadcastChanges();
            });
            game.waitFor(client->MapSelectorHud.active()&&MapSelectorHud.selector(client.player.getInventory().getItem(8)));
            game.runOnClient(client->{MapCommanderHud.accept(new ProtocolCommanderHud.View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),true));MapCommanderPanels.select("A",false);});game.setScreen(ScreenMapCursor::new);game.waitTicks(3);
            game.runOnClient(client->{
                if(MapSelectorHud.mapDimension("minecraft:overworld")||MapSelectorHud.selector(new ItemStack(Items.STONE)))throw new AssertionError("Ordinary worlds and items must retain vanilla behavior");
                var board=client.level.getScoreboard();var objective=board.addObjective("hoi_map_preview",net.minecraft.world.scores.criteria.ObjectiveCriteria.DUMMY,Component.literal("The Fire Rises"),net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType.INTEGER,false,net.minecraft.network.chat.numbers.BlankFormat.INSTANCE);
                var names=List.of("날짜: 2022년 12월 31일 23시","게임 속도: X10","Kiruy, 대한민국");for(int i=0;i<names.size();i++){var score=board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("map_row_"+i),objective);score.set(names.size()-i);score.display(Component.literal(names.get(i)));}board.setDisplayObjective(net.minecraft.world.scores.DisplaySlot.SIDEBAR,objective);
                var entries=new java.util.ArrayList<MapSelectorHud.Entry>();for(int i=0;i<9;i++)entries.add(new MapSelectorHud.Entry(i,client.player.getInventory().getItem(i)));var bounds=MapSelectorHud.bounds(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight(),entries);
                int width=client.getWindow().getGuiScaledWidth(),height=client.getWindow().getGuiScaledHeight();
                if(bounds.x()+bounds.width()!=width-6||bounds.y()+bounds.height()!=height-6)throw new AssertionError("Nine readable legend rows must anchor to the bottom-right corner");
                if(MapCommanderHud.boxes().stream().anyMatch(b->bounds.x()<b.x()+b.width()&&bounds.x()+bounds.width()>b.x()&&bounds.y()<b.y()+b.height()&&bounds.y()+bounds.height()>b.y()))throw new AssertionError("Legend must not overlap card controls");
                if(source.cards().stream().filter(c->c.id().equals("A")).anyMatch(c->c.panel()==null||!c.panel().commandCaption().equals("전투 계획")))throw new AssertionError("Map fixture must preserve the authoritative command caption at both resolutions");
            });game.waitTicks(2);
            game.takeScreenshot("hoi-army-map-legend-"+size[0]+"x"+size[1]+"-gui2");
            for(Boolean running:new Boolean[]{false,null,true}){
                game.runOnClient(client->MapCommanderHud.accept(new ProtocolCommanderHud.View(source.country(),client.level.dimension().identifier().toString(),source.branch(),source.cards(),source.wings(),running)));game.waitTicks(2);
                game.runOnClient(client->{if(MapCommanderHud.hidesVanillaHud()!=Boolean.TRUE.equals(running))throw new AssertionError("Map dimension alone must not hide the vanilla HUD while paused or before start");var board=client.level.getScoreboard();var objective=board.getObjective("hoi_map_preview");board.getOrCreatePlayerScore(net.minecraft.world.scores.ScoreHolder.forNameOnly("map_row_1"),objective).display(Component.literal("게임 속도: ").append(dev.hoi.protocol.ProtocolCampaignStyle.speed(Boolean.TRUE.equals(running)?"X10":"PAUSED")));});
                game.takeScreenshot("hoi-army-map-"+(running==null?"legacy":running?"resumed":"paused")+"-"+size[0]+"x"+size[1]+"-gui2");
            }
            game.setScreen(()->null);game.waitTicks(2);game.takeScreenshot("hoi-map-vanilla-hud-hidden-"+size[0]+"x"+size[1]+"-gui2");
        }finally{
            game.runOnClient(client->{var board=client.level.getScoreboard();var objective=board.getObjective("hoi_map_preview");if(objective!=null)board.removeObjective(objective);});
            onServer(game,server,()->{player.teleportTo(server.getLevel(dimension),position.x,position.y,position.z,Set.of(),0,0,false);for(int i=0;i<previous.size();i++)player.getInventory().setItem(i,previous.get(i));player.inventoryMenu.broadcastChanges();});
            game.waitFor(client->!MapSelectorHud.active());game.setScreen(ScreenMapCursor::new);
        }
    }
}
