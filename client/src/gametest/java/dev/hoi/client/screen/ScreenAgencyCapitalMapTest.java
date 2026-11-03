package dev.hoi.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.ProtocolAgencyPayloads;
import dev.hoi.protocol.ProtocolAgencyView;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.List;

public final class ScreenAgencyCapitalMapTest {
    private static ProtocolAgencyView view(long revision, String action, boolean enabled, double x, double z) {
        var recruit = new ProtocolAgencyView.Item("recruit", "agents", "모집", "0/2", "", "", "", false, -1, List.of());
        var unlocked = new ProtocolAgencyView.Item("cryptology_unlocked", "metadata", "", "true", "", "", "", false, -1, List.of());
        var target = new ProtocolAgencyView.Item(action + ":PRK", "cryptology", "북한", action.equals("pause") ? "50%" : "0%", "수도 암호학 모의 입력", "agency/cryptology", "암호 해독", enabled, action.equals("pause") ? .5 : 0, List.of());
        return new ProtocolAgencyView("capital-map-test", revision, "KOR", "국가정보원", "", List.of(recruit, unlocked, target), "", List.of(),
                List.of(action.equals("crypto_add") ? new ProtocolAgencyView.Capital("PRK", 1, x, z, enabled ? "crypto_add:PRK" : "", "Issued capital add contract") : new ProtocolAgencyView.Capital("PRK", 1, x, z)));
    }
    private static void click(ScreenAgency screen, int button) {
        var hit = screen.capitalHits().getFirst();
        var event = new MouseButtonEvent(hit.x() + 15, hit.y() + 14, new MouseButtonInfo(button, 0));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void tab(ScreenAgency screen, String name) {
        var button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(value -> value.getMessage().getString().equals(name)).findFirst().orElseThrow();
        screen.mouseClicked(new MouseButtonEvent(button.getX() + 20, button.getY() + 10,
                new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }
    public static void run(ClientGameTestContext context) {
        var server=context.computeOnClient(client->client.getSingleplayerServer());
        var playerRequest=server.submit(()->server.getPlayerList().getPlayers().getFirst());
        context.waitFor(client->playerRequest.isDone());var player=playerRequest.join();
        var position=context.computeOnClient(client->client.player.position());
        var dimension=context.computeOnClient(client->client.level.dimension());
        float[] rotation=context.computeOnClient(client->new float[]{client.player.getXRot(),client.player.getYRot()});
        var move=server.submit(()->{
            var map=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse("hoi:atlas_ui_test")));
            if(map==null)throw new AssertionError("Atlas test dimension is required");
            var blocks=new java.util.LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
            for(var pos:List.of(new net.minecraft.core.BlockPos(0,63,0),new net.minecraft.core.BlockPos(0,63,6),new net.minecraft.core.BlockPos(311,63,94),new net.minecraft.core.BlockPos(311,63,104))) {
                blocks.put(pos,map.getBlockState(pos));map.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            }
            player.teleportTo(map,.5,map.getHeight(Heightmap.Types.WORLD_SURFACE,0,0),.5,java.util.Set.of(),0,0,false);
            return blocks;
        });context.waitFor(client->move.isDone());var blocks=move.join();
        context.waitFor(client->client.level.dimension().identifier().toString().equals("hoi:atlas_ui_test")&&Math.abs(client.player.getX()-.5)<.1&&client.level.hasChunk(0,0));context.waitTicks(5);
        try { runOnMap(context); }
        finally {
            context.setScreen(()->null);
            var restore=server.submit(()->{
                var map=player.level();blocks.forEach((pos,state)->map.setBlock(pos,state,3));
                player.teleportTo(server.getLevel(dimension),position.x,position.y,position.z,java.util.Set.of(),rotation[1],rotation[0],false);
            });
            context.waitFor(client->restore.isDone());restore.join();
            context.waitFor(client->client.level.dimension().equals(dimension));
        }
    }
    private static void runOnMap(ClientGameTestContext context) {
        float[] rotation = context.computeOnClient(client -> new float[]{client.player.getXRot(), client.player.getYRot()});
        double[] point = context.computeOnClient(client -> {
            double x = client.player.getX(), z = client.player.getZ() + 6;
            double ground = client.level.getHeight(Heightmap.Types.WORLD_SURFACE, (int)Math.floor(x), (int)Math.floor(z));
            client.player.setYRot(0);
            client.player.setXRot((float)Math.toDegrees(Math.atan2(client.player.getEyeY() - ground - 1, 6)));
            return new double[]{x, z};
        });
        try {
            for (int[] size : new int[][]{{2560, 1440}, {1280, 720}}) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var sent = new ArrayList<ProtocolAgencyPayloads.Request>();
                var initial = view(1, "decrypt", true, point[0], point[1]);
                context.setScreen(() -> new ScreenAgency(null, initial.session(), initial, sent::add));
                context.runOnClient(client -> tab((ScreenAgency)client.gui.screen(), "암호학"));
                context.waitTicks(4);
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    if (screen.capitalHits().size() != 1) throw new AssertionError("A visible loaded capital must project into the map area at GUI2");
                });
                context.getInput().setCursorPos(size[0] - 2, size[1] - 2); context.waitTicks(2);
                context.takeScreenshot("hoi-agency-capital-synthetic-" + size[0] + "x" + size[1] + "-gui2");
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    click(screen, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (!sent.isEmpty()) throw new AssertionError("Right clicking a capital cannot mutate cryptology");
                    click(screen, InputConstants.MOUSE_BUTTON_LEFT); click(screen, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1 || !sent.getFirst().item().equals("decrypt:PRK") || sent.getFirst().revision() != 1)
                        throw new AssertionError("Capital click must send one issued target with the private revision");
                    screen.update(initial); click(screen, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1) throw new AssertionError("Stale snapshots cannot unlock a repeated capital request");
                    screen.update(view(2, "pause", true, point[0], point[1]));
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-agency-capital-progress-synthetic-" + size[0] + "x" + size[1] + "-gui2");
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    click(screen, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 2 || !sent.getLast().item().equals("pause:PRK") || sent.getLast().revision() != 2)
                        throw new AssertionError("The server-issued pause state must replace the start request");
                    screen.update(view(3, "pause", false, point[0], point[1]));
                });
                context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    click(screen, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 2) throw new AssertionError("Disabled capital action must remain blocked");
                    tab(screen, "작전");
                    if (!screen.capitalHits().isEmpty()) throw new AssertionError("Changing the tab must clear map hit targets immediately");
                    screen.update(new ProtocolAgencyView(initial.session(), 0, "", "", "", List.of(), ""));
                    if (client.gui.screen() != null) throw new AssertionError("Revocation must close the private cryptology screen");
                });
                sent.clear();
                var addView = view(4, "crypto_add", true, point[0], point[1]);
                context.setScreen(() -> new ScreenAgency(null, addView.session(), addView, sent::add));
                context.runOnClient(client -> tab((ScreenAgency)client.gui.screen(), "암호학")); context.waitTicks(3);
                context.takeScreenshot("hoi-cipher-capital-add-contract-" + size[0] + "-gui2");
                context.runOnClient(client -> {
                    var screen = (ScreenAgency)client.gui.screen();
                    click(screen, InputConstants.MOUSE_BUTTON_RIGHT);
                    if (!sent.isEmpty()) throw new AssertionError("Right click added cipher country");
                    click(screen, InputConstants.MOUSE_BUTTON_LEFT); click(screen, InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1 || !sent.getFirst().item().equals("crypto_add:PRK") || sent.getFirst().revision() != 4) throw new AssertionError("Capital.item contract or duplicate guard lost");
                    screen.update(view(5, "crypto_add", false, point[0], point[1]));
                }); context.waitTicks(2);
                context.runOnClient(client -> {
                    click((ScreenAgency)client.gui.screen(), InputConstants.MOUSE_BUTTON_LEFT);
                    if (sent.size() != 1) throw new AssertionError("Capital with no issued item sent a request");
                });
            }
        } finally {
            context.runOnClient(client -> { client.player.setXRot(rotation[0]); client.player.setYRot(rotation[1]); });
            context.setScreen(() -> null);
        }
        source(context);
    }
    private static void source(ClientGameTestContext context) {
        String directory = System.getenv("HOI_UI_REVIEW_PREVIEW");
        if (directory == null) return;
        try {
            var before = new ProtocolAgencyPayloads.Response(java.nio.file.Files.readString(java.nio.file.Path.of(directory, "agency-cryptology-ready.json"))).view();
            var after = new ProtocolAgencyPayloads.Response(java.nio.file.Files.readString(java.nio.file.Path.of(directory, "agency-cryptology-decrypting.json"))).view();
            var capital = before.capitals().stream().filter(c -> c.country().equals("JAP")).findFirst().orElseThrow();
            if (before.capitals().size() != 30 || capital.province() != 1182 || !after.session().equals(before.session())) throw new AssertionError("Issued capital/session contract changed");
            var server = context.computeOnClient(client -> client.getSingleplayerServer());
            var playerRequest=server.submit(()->server.getPlayerList().getPlayers().getFirst());
            context.waitFor(client->playerRequest.isDone());var player=playerRequest.join();
            var position = context.computeOnClient(client -> client.player.position());
            float[] rotation = context.computeOnClient(client -> new float[]{client.player.getXRot(),client.player.getYRot()});
            var move = server.submit(() -> player.teleportTo(player.level(), capital.x(), 68, capital.z() - 10, java.util.Set.of(), 0, 0, false));
            context.waitFor(client -> move.isDone()); move.join();
            context.waitFor(client -> Math.abs(client.player.getX() - capital.x()) < 1
                && client.level.getBlockState(new net.minecraft.core.BlockPos(311,63,94)).is(net.minecraft.world.level.block.Blocks.STONE)
                && client.level.getBlockState(new net.minecraft.core.BlockPos(311,63,104)).is(net.minecraft.world.level.block.Blocks.STONE));
            var settle=server.submit(()->{
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.teleportTo(player.level(),capital.x()+.5,64,capital.z()-10,java.util.Set.of(),0,0,false);
            });context.waitFor(client->settle.isDone());settle.join();
            context.waitFor(client->Math.abs(client.player.getY()-64)<.1);context.waitTicks(3);
            try {
                for (int[] size : new int[][]{{2560,1440},{1280,720}}) {
                    context.getInput().resizeWindow(size[0],size[1]); context.runOnClient(client -> client.options.guiScale().set(2));
                    context.runOnClient(client->{
                        double ground=client.level.getHeight(Heightmap.Types.WORLD_SURFACE,(int)capital.x(),(int)capital.z());
                        client.player.setYRot(0);client.player.setXRot((float)Math.toDegrees(Math.atan2(client.player.getEyeY()-ground-1,10)));
                    });
                    var sent = new ArrayList<ProtocolAgencyPayloads.Request>();
                    context.setScreen(() -> new ScreenAgency(null,before.session(),before,sent::add));
                    context.getInput().setCursorPos(size[0]-2,80);context.waitTicks(3);
                    context.takeScreenshot("hoi-agency-source-ready-"+size[0]+"-gui2");
                    context.runOnClient(client -> tab((ScreenAgency)client.gui.screen(),"암호학"));context.waitTicks(3);
                    context.takeScreenshot("hoi-agency-capital-source-ready-"+size[0]+"-gui2");
                    context.runOnClient(client->{
                        var screen=(ScreenAgency)client.gui.screen();
                        var targets=before.items().stream().filter(i->i.id().startsWith("decrypt:")||i.id().startsWith("pause:")).toList();
                        var row=screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(w->targets.stream().anyMatch(i->i.title().equals(w.getMessage().getString()))).findFirst().orElseThrow(()->new AssertionError("Cryptology must show a country row even at 1280x720"));
                        int x=row.getRight()+4;
                        var start=new MouseButtonEvent(x,row.getY()+15,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                        var end=new MouseButtonEvent(x,screen.height-14,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(start,false);screen.mouseDragged(end,0,end.y()-start.y());screen.mouseReleased(end);
                        if(screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).noneMatch(w->w.getMessage().getString().equals(targets.getLast().title())))throw new AssertionError("Cryptology scrollbar must reach the last issued country");
                        if(!sent.isEmpty())throw new AssertionError("Scrolling cryptology cannot start decryption");
                    });context.waitTicks(2);
                    context.takeScreenshot("hoi-agency-capital-source-list-bottom-"+size[0]+"-gui2");
                    context.runOnClient(client->((ScreenAgency)client.gui.screen()).mouseScrolled(20,client.gui.screen().height-30,0,1000));context.waitTicks(2);
                    context.runOnClient(client -> {
                        var screen=(ScreenAgency)client.gui.screen();
                        var hit=screen.capitalHits().stream().filter(h->h.country().equals("JAP")).findFirst().orElseThrow();
                        var event=new MouseButtonEvent(hit.x()+15,hit.y()+14,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(event,false);screen.mouseClicked(event,false);
                        if(sent.size()!=1 || !sent.getFirst().item().equals("decrypt:JAP") || sent.getFirst().revision()!=before.revision())throw new AssertionError("Actual capital input must issue the authenticated decrypt target once");
                        screen.update(after);
                    });context.waitTicks(3);
                    context.takeScreenshot("hoi-agency-capital-source-decrypting-"+size[0]+"-gui2");
                }
            } finally {
                context.setScreen(()->null);
                var restore=server.submit(()->player.teleportTo(player.level(),position.x,position.y,position.z,java.util.Set.of(),rotation[1],rotation[0],false));
                context.waitFor(client->restore.isDone());restore.join();
            }
        } catch(java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
}
