package dev.hoi.client.screen;

import com.google.gson.Gson;
import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ScreenDialogChecks {
    private static void detailFlags(ClientGameTestContext context) {
        var previous = context.computeOnClient(c -> dev.hoi.client.ClientCampaignHud.visible()
                ? ProtocolHudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(), dev.hoi.client.ClientCampaignHud.hud()) : ProtocolHudPayloads.State.HIDDEN);
        try {
            context.getInput().resizeWindow(2560, 1440);
            context.runOnClient(c -> {
                c.options.guiScale().set(2);
                dev.hoi.client.ClientCampaignHud.accept(ProtocolHudPayloads.State.of("KOR", previous.hud()));
            });
            context.setScreen(ScreenMapCursor::new);
            context.getInput().setCursorPos(1800, 800); context.waitTicks(3);
            var baseline = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-map-country-flag-gui2").toFile());
            var bounds = context.computeOnClient(c -> {
                double scale = c.getWindow().getGuiScale();
                return new int[]{(int)(4 * scale), (int)(3 * scale),
                        (int)(dev.hoi.client.ui.UiMenuBar.flagWidth(c.gui.screen().width) * scale),
                        (int)((dev.hoi.client.ui.UiMenuBar.height(c.gui.screen().width) - 6) * scale)};
            });
            for (String image : List.of("map_detail/terrain/plains", "map_detail/sea", "air_detail/background")) {
                var view = new ProtocolDialogView(UUID.randomUUID().toString(), 1, ProtocolDialogView.Kind.STATE, "국기 유지 검증", "", "", "", image, "", List.of(), List.of());
                context.runOnClient(c -> ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(view, true)));
                context.waitTicks(3);
                var actual = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-detail-country-flag-" + image.substring(image.lastIndexOf('/') + 1) + "-gui2").toFile());
                for (int y = bounds[1]; y < bounds[1] + bounds[3]; y++)
                    for (int x = bounds[0]; x < bounds[0] + bounds[2]; x++)
                        if (actual.getRGB(x, y) != baseline.getRGB(x, y))
                            throw new AssertionError("Opening " + image + " replaced the player's country flag at " + x + "," + y);
            }
        } catch (IOException error) { throw new UncheckedIOException(error); }
        finally {
            context.setScreen(() -> null);
            context.runOnClient(c -> dev.hoi.client.ClientCampaignHud.accept(previous));
        }
    }
    private static void tooltipNumbers(ClientGameTestContext context) {
        context.getInput().resizeWindow(2560, 1369);
        context.runOnClient(c -> {
            c.options.guiScale().set(2);
            var rich = net.minecraft.network.chat.Component.literal("효과").withColor(0xEEC923)
                    .append(net.minecraft.network.chat.Component.literal("\n사단 방어: -5.555%\n훈련: 0\n\n\n제거 불가능").withColor(0xDE5646));
            var formatted = dev.hoi.client.ui.UiTooltips.format(rich);
            if (!formatted.getString().equals("효과\n사단 방어: -5.6%\n\n제거 불가능")) throw new AssertionError(formatted.getString());
            var party = dev.hoi.client.ui.UiTooltips.format(net.minecraft.network.chat.Component.literal("정당 지지도\n매일 변화: §a+0.0025§r"));
            if (!party.getString().contains("+0.0025")) throw new AssertionError("Party precision changed");
        });
        var tiles=List.of(
                new ProtocolDialogView.Tile("empty","현대화된 농업","0","politics/laws/industrial_development","\n\n\n§c제거 불가능"),
                new ProtocolDialogView.Tile("zero","전문여단","0","politics/laws/military_training","징병 가능 인구 변동치: §a+5%§r\n사단 훈련 시간: 0\n사단 조직력: §c-5%§r\n사단 방어: §c-5%§r\n사단 공격: §c-5%§r\n특수부대 수용량 증가치: 0\n육군 고문 비용: 0\n공군 고문 비용: 0\n해군 고문 비용: 0\n육군 교리 비용: §c+5%§r\n\n\n§c제거 불가능"));
        var view=new ProtocolDialogView(UUID.randomUUID().toString(),1,ProtocolDialogView.Kind.POLITICS,"툴팁 표시 검증","","","","","",tiles,List.of());
        context.setScreen(() -> new ScreenPoliticsChoice(view,null));
        for(int index=0;index<2;index++) {
            int target=index+1;
            var position=context.computeOnClient(c -> {
                var b=(Button)c.gui.screen().children().get(target);
                double scale=c.getWindow().getGuiScale();
                return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};
            });
            context.getInput().setCursorPos(position[0],position[1]);context.waitTicks(18);
            context.takeScreenshot("hoi-tooltip-"+(index==0?"single-blank-line":"zero-effects-hidden")+"-gui2");
        }
        context.setScreen(() -> null);
    }
    record ScreenDialogFixtures(List<ProtocolDialogView> views, List<String> sounds) {}
    public static void run(ClientGameTestContext context) {
        ScreenMapDetailChecks.run(context);
        ScreenAirDetailChecks.run(context);
        detailFlags(context);
        tooltipNumbers(context);
        ScreenDialogFixtures fixture;
        try (var input = ScreenDialogChecks.class.getResourceAsStream("/dialog-fixtures.json")) {
            fixture = new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8), ScreenDialogFixtures.class);
        } catch (IOException error) { throw new UncheckedIOException(error); }
        context.runOnClient(client -> {
            ProtocolDialogPayloads.registerPayloadTypes(); ProtocolDialogPayloads.registerPayloadTypes();
            for (var path : fixture.sounds()) if (client.getSoundManager().getSoundEvent(Identifier.fromNamespaceAndPath("hoi", path)) == null)
                throw new AssertionError("Missing super-event sound: " + path);
            for (var view : fixture.views()) {
                if (!EnumSet.of(ProtocolDialogView.Kind.EVENT, ProtocolDialogView.Kind.GLOBAL_EVENT, ProtocolDialogView.Kind.SUPER_EVENT).contains(view.kind()))
                    throw new AssertionError("Generic dialog fixtures must not recreate country or map detail screens");
                if (!view.image().isEmpty() && client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + view.image() + ".png")).isEmpty())
                    throw new AssertionError("Missing event picture: " + view.title());
                var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
                try { var payload = ProtocolDialogPayloads.Show.of(view, true); ProtocolDialogPayloads.Show.CODEC.encode(buffer, payload);
                    if (!view.equals(ProtocolDialogPayloads.Show.CODEC.decode(buffer).view())) throw new AssertionError("Dialog codec must preserve original text");
                } finally { buffer.release(); }
            }
        });
        var choices = java.util.stream.IntStream.rangeClosed(1, 11).mapToObj(i -> new ProtocolDialogView.Choice("option-" + i, "선택 " + i)).toList();
        var multiChoice = new ProtocolDialogView(UUID.randomUUID().toString(), 1, ProtocolDialogView.Kind.EVENT, "여러 선택지가 있는 국가 이벤트", "",
                "긴 이벤트 본문과 선택지가 작은 창에서도 겹치지 않아야 합니다.\n".repeat(35), "", "", "", List.of(), choices);
        var views = new ArrayList<>(fixture.views()); views.add(multiChoice);
        for (var view : views) {
            context.setScreen(() -> new ScreenDialog(view)); context.waitTicks(4);
            context.takeScreenshot("hoi-dialog-" + view.kind().name().toLowerCase(Locale.ROOT));
        }
        for (int[] size : new int[][]{{2560, 1440}, {1600, 1000}, {854, 480}, {640, 360}}) {
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            for (var view : views) {
                context.setScreen(() -> new ScreenDialog(view)); context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (ScreenDialog) client.gui.screen();
                    if (size[0] >= 1600 && (screen.width != size[0] / 2 || screen.height != size[1] / 2))
                        throw new AssertionError("Reference captures require effective GUI scale 2");
                    float expectedScale=dev.hoi.client.ui.UiText.scale(client.font);
                    if (screen.textScale()!=expectedScale) throw new AssertionError("Every event uses the minimum 10 GUI font height");
                    if (Math.abs(screen.panelLeft() * 2 + screen.panelWidth() - screen.width) > 1) throw new AssertionError("Dialog is not centered");
                    if (size[0] == 2560 && view.kind() == ProtocolDialogView.Kind.SUPER_EVENT && screen.panelWidth() <= screen.panelHeight())
                        throw new AssertionError("Super event keeps the wide reference proportions");
                    if (size[0] == 2560 && (view.kind() == ProtocolDialogView.Kind.EVENT || view.kind() == ProtocolDialogView.Kind.GLOBAL_EVENT) && screen.panelWidth() >= screen.panelHeight())
                        throw new AssertionError("Country and global events keep portrait proportions");
                    if (screen.bodyViewportHeight() <= 0) throw new AssertionError("Choices must leave a visible body viewport");
                    for (var child : screen.children()) if (child instanceof Button b && (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height))
                        throw new AssertionError("Dialog control outside compact screen");
                    screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0, 0, -100);
                    if (view == multiChoice) {
                        if (screen.scrollOffset() <= 0) throw new AssertionError("Long event body must scroll with many choices");
                        var reached = new HashSet<String>();
                        for (int page = 0; page < 11; page++) {
                            for (var child : screen.children()) if (child instanceof Button b && b.getMessage().getString().startsWith("선택 ")) {
                                if (b.getY() < 0 || b.getBottom() > screen.height) throw new AssertionError("Paged choice outside viewport");
                                reached.add(b.getMessage().getString());
                            }
                            var next = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("다음 선택지")).findFirst().orElseThrow();
                            if (!next.active) break;
                            next.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                        }
                        if (reached.size() != 11) throw new AssertionError("Every server choice must remain accessible in a compact viewport");
                    }
                });
                context.waitTicks(2); context.takeScreenshot("hoi-dialog-" + view.kind().name().toLowerCase(Locale.ROOT) + "-" + size[0] + (size[0] >= 1600 ? "-gui2" : "-compact"));
            }
        }
        context.runOnClient(client -> {
            for (var kind : List.of(ProtocolDialogView.Kind.EVENT, ProtocolDialogView.Kind.GLOBAL_EVENT, ProtocolDialogView.Kind.SUPER_EVENT)) {
                var sent = new ArrayList<String>();
                var view = new ProtocolDialogView(UUID.randomUUID().toString(), 7, kind, "Enter 확인", "", "서버 선택지", "", "", "",
                        List.of(), List.of(new ProtocolDialogView.Choice("issued-choice", "확인")));
                var screen = new ScreenDialog(view) {
                    @Override protected void choose(String choice) { sent.add(view().token() + "/" + view().revision() + "/" + choice); }
                };
                client.gui.setScreen(screen);
                for (int key : new int[]{com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER}) {
                    int before = sent.size();
                    screen.keyPressed(new net.minecraft.client.input.KeyEvent(key, 0, 0));
                    if (sent.size() != before + 1 || !sent.getLast().equals(view.token() + "/7/issued-choice"))
                        throw new AssertionError("Enter must submit exactly the server-issued event choice");
                }
                var multiple = new ProtocolDialogView(view.token(), 8, kind, view.title(), "", view.body(), "", "", "", List.of(),
                        List.of(new ProtocolDialogView.Choice("a", "첫 선택"), new ProtocolDialogView.Choice("b", "둘째 선택")));
                screen.update(multiple); screen.setFocused(null);
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                if (!sent.getLast().equals(view.token() + "/8/") || client.gui.screen() != null)
                    throw new AssertionError("Enter without a selected option dismisses without choosing gameplay effects");
                client.gui.setScreen(screen);
                var second = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("둘째 선택")).findFirst().orElseThrow();
                screen.setFocused(second);
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                if (!sent.getLast().equals(view.token() + "/8/b")) throw new AssertionError("Enter activates the focused server choice");
            }
            client.gui.setScreen(null);
            var one = fixture.views().getFirst().issued(UUID.randomUUID().toString(), 0);
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(one, true));
            ScreenDialogCoordinator.close(one.token());
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(one.issued(one.token(), 1), false));
            if (client.gui.screen() != null) throw new AssertionError("Late refresh reopened a revoked private dialog");
        });
        context.runOnClient(client -> {
            var event = fixture.views().stream().filter(v -> v.kind() == ProtocolDialogView.Kind.SUPER_EVENT).findFirst().orElseThrow()
                    .issued(UUID.randomUUID().toString(), 0);
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(event, true));
            if (!(client.gui.screen() instanceof ScreenDialogStack stack) || stack.find(event.token()) == null) throw new AssertionError("Super event must be visible before stop");
            ScreenDialogCoordinator.close(event.token());
            if (!dev.hoi.client.audio.AudioSuperEvent.active()) throw new AssertionError("Closing a popup must preserve event music");
            dev.hoi.client.audio.AudioStoryMusic.play("music.korea.extinction");
            if (!dev.hoi.client.audio.AudioSuperEvent.active()) throw new AssertionError("National focus waits for global super event music");
            dev.hoi.client.audio.AudioUiSounds.receive(ProtocolAudioPayloads.Cue.STOP);
            if (client.gui.screen() != null || dev.hoi.client.audio.AudioSuperEvent.active())
                throw new AssertionError("Stop must dismiss visible super event and its audio");
            client.gui.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            ScreenDialogCoordinator.receive(ProtocolDialogPayloads.Show.of(event.issued(UUID.randomUUID().toString(), 0), true));
            if (client.gui.screen() instanceof ScreenDialogStack) throw new AssertionError("Pause menu must defer super event");
            if (!dev.hoi.client.audio.AudioSuperEvent.active()) throw new AssertionError("Deferred popup must still start global music immediately");
            dev.hoi.client.audio.AudioUiSounds.receive(ProtocolAudioPayloads.Cue.STOP);
            client.gui.setScreen(null);
        });
        context.waitTicks(3);
        context.runOnClient(client -> {
            if (client.gui.screen() != null || dev.hoi.client.audio.AudioSuperEvent.active())
                throw new AssertionError("Stopped deferred super event must not reopen on the next tick");
        });
        context.getInput().resizeWindow(1600, 1000); context.waitTicks(3);
    }
}
