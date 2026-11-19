package dev.hoi.client.screen.dialog;

import dev.hoi.client.screen.map.*;
import dev.hoi.client.screen.politics.PoliticsChoice;
import dev.hoi.client.screen.politics.PoliticsChoiceTest;
import dev.hoi.protocol.audio.*;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.hud.*;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DialogScreenChecks {
    public static void disconnectThread(ClientGameTestContext context) {
        var client = context.computeOnClient(c -> c);
        var handler = context.computeOnClient(c -> c.getConnection());
        var hud = context.computeOnClient(c -> dev.hoi.client.ClientCampaignHud.visible() ? HudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(), dev.hoi.client.ClientCampaignHud.hud()) : HudPayloads.State.HIDDEN);
        var removedOnClient = new java.util.concurrent.atomic.AtomicReference<Boolean>();
        context.runOnClient(c -> {
            var detail = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.STATE,
                    "연결 종료 검사", "", "", "", "", "", List.of(), List.of());
            DialogCoordinator.receive(DialogPayloads.Show.of(detail, true));
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.remove(c.gui.screen()).register(screen -> removedOnClient.set(c.isSameThread()));
        });
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(handler, client);
        context.waitTicks(1);
        context.runOnClient(c -> {
            if (!Boolean.TRUE.equals(removedOnClient.get()) || c.gui.screen() != null)
                throw new AssertionError("Disconnect must remove the dialog on the client thread");
            dev.hoi.client.ClientCampaignHud.accept(hud);
        });
    }
    private static void detailFlags(ClientGameTestContext context) {
        var previous = context.computeOnClient(c -> dev.hoi.client.ClientCampaignHud.visible()
                ? HudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(), dev.hoi.client.ClientCampaignHud.hud()) : HudPayloads.State.HIDDEN);
        try {
            context.getInput().resizeWindow(2560, 1440);
            context.runOnClient(c -> {
                c.options.guiScale().set(2);
                dev.hoi.client.ClientCampaignHud.accept(HudPayloads.State.of("KOR", previous.hud()));
            });
            context.setScreen(MapCursor::new);
            context.getInput().setCursorPos(1800, 800); context.waitTicks(3);
            var baseline = javax.imageio.ImageIO.read(context.takeScreenshot("hoi-map-country-flag-gui2").toFile());
            var bounds = context.computeOnClient(c -> {
                double scale = c.getWindow().getGuiScale(), k = dev.hoi.client.ui.hud.HudMenuBar.flagWidth(c.gui.screen().width) / 49.0;
                int left = (int)Math.ceil(4.5 * k * scale), top = (int)Math.ceil(7.5 * k * scale);
                return new int[]{left, top, (int)Math.floor(45.5 * k * scale) - left, (int)Math.floor(32.5 * k * scale) - top};
            });
            for (String image : List.of("map_detail/terrain/plains", "map_detail/sea", "air_detail/background")) {
                var view = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.STATE, "국기 유지 검증", "", "", "", image, "", List.of(), List.of());
                context.runOnClient(c -> DialogCoordinator.receive(DialogPayloads.Show.of(view, true)));
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
            var formatted = dev.hoi.client.ui.tooltip.Tooltips.format(rich);
            if (!formatted.getString().equals("효과\n사단 방어: -5.6%\n\n제거 불가능")) throw new AssertionError(formatted.getString());
            var party = dev.hoi.client.ui.tooltip.Tooltips.format(net.minecraft.network.chat.Component.literal("정당 지지도\n매일 변화: §a+0.0025§r"));
            if (!party.getString().contains("+0.0025")) throw new AssertionError("Party precision changed");
        });
        var tiles=List.of(
                new DialogView.Tile("empty","현대화된 농업","0","politics/laws/industrial_development","\n\n\n§c제거 불가능"),
                new DialogView.Tile("zero","전문여단","0","politics/laws/military_training","징병 가능 인구 변동치: §a+5%§r\n사단 훈련 시간: 0\n사단 조직력: §c-5%§r\n사단 방어: §c-5%§r\n사단 공격: §c-5%§r\n특수부대 수용량 증가치: 0\n육군 고문 비용: 0\n공군 고문 비용: 0\n해군 고문 비용: 0\n육군 교리 비용: §c+5%§r\n\n\n§c제거 불가능"));
        var view=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.POLITICS,"툴팁 표시 검증","","","","","",tiles,List.of());
        context.setScreen(() -> new PoliticsChoice(view,null));
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
    private static void choiceLayouts(ClientGameTestContext context) {
        PoliticsChoiceTest.run(context);
        String detail="§H육군 경험치 획득량§!: §G+0.15§! 매일\n사단 조직력: §G+8.0%§!\n\n비용 (£pol_power ): §Y110§!";
        var staff=List.of(
                new DialogView.Tile("hoi:test/current","한웨이궈","적용 중\n육군 편성\n(전문가)","politics/vacant",detail+"\n\n§G클릭으로 §H한웨이궈§G 제거§!"),
                new DialogView.Tile("hoi:test/open","리차오밍","§e110§r\n육군 공격\n(전문가)","politics/vacant",detail),
                new DialogView.Tile("hoi:test/poor","류전리","§c9110§r\n육군 계획\n(전문가)","politics/vacant",detail),
                new DialogView.Tile("hoi:test/blocked","리쭤청","§e165§r\n육군 공격\n(천재)","politics/vacant",detail),
                new DialogView.Tile("hoi:test/plain","장유샤","§e110§r\n포병 만능론자","politics/vacant",detail));
        var staffChoices=List.of(new DialogView.Choice(PoliticsChoice.VACANT,"공석"),new DialogView.Choice("hoi:test/open","리차오밍"),
                new DialogView.Choice("hoi:test/plain","장유샤"));
        for(String slot:List.of("army_chief","navy_chief","air_chief","high_command")) {
            String title=switch(slot){case "navy_chief" -> "해군참모총장";case "air_chief" -> "공군참모총장";case "high_command" -> "최고사령부";default -> "육군참모총장";};
            var view=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.POLITICS,title,"§Y"+title+"§! 선택","","","advisor/"+slot,"",staff,staffChoices);
            var sent=new ArrayList<DialogPayloads.Action>();
            context.setScreen(() -> new PoliticsChoice(view,null,sent::add));
            context.getInput().setCursorPos(2400,1300);context.waitTicks(3);
            context.takeScreenshot("hoi-politics-choice-"+slot+"-gui2");
            context.runOnClient(client -> {
                var screen=(PoliticsChoice)client.gui.screen();
                var buttons=screen.children().stream().filter(c -> c instanceof Button).map(c -> (Button)c)
                        .filter(b -> staff.stream().anyMatch(tile -> tile.name().equals(b.getMessage().getString()))).toList();
                if(buttons.size()!=staff.size())throw new AssertionError("Every staff candidate needs one tile: "+buttons.size());
                if(buttons.stream().map(Button::getX).distinct().count()!=2)throw new AssertionError("Staff candidates use the source two-column grid");
                if(buttons.stream().anyMatch(b -> b.getWidth()!=115 || b.getHeight()!=37))throw new AssertionError("Staff candidate tiles keep the source 231x74 slot at GUI 2");
                for(var tile:staff) {
                    var button=buttons.stream().filter(b -> b.getMessage().getString().equals(tile.name())).findFirst().orElseThrow();
                    boolean expected=tile.section().equals("hoi:test/current") || staffChoices.stream().anyMatch(choice -> choice.id().equals(tile.section()));
                    if(button.active!=expected)throw new AssertionError("Unexpected staff candidate state: "+tile.name());
                }
                if(!PoliticsChoice.cost(staff.get(1)).equals("§e110§r") || !PoliticsChoice.traits(staff.get(1)).equals(List.of("육군 공격","(전문가)"))
                        || !PoliticsChoice.traits(staff.get(4)).equals(List.of("포병 만능론자")))throw new AssertionError("Staff tile cost and trait lines must stay separate");
                var current=buttons.stream().filter(b -> b.getMessage().getString().equals("한웨이궈")).findFirst().orElseThrow();
                screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(current.getX()+current.getWidth()/2.0,current.getY()+current.getHeight()/2.0,
                        new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0)),false);
                if(sent.size()!=1 || !sent.getFirst().choice().equals(PoliticsChoice.VACANT))throw new AssertionError("Clicking the appointed staff tile removes the appointment: "+sent);
            });
        }
        var laws=List.of(
                new DialogView.Tile("hoi:test/law_blocked","안보 없음","§c500§r","politics/vacant","효과 없음"),
                new DialogView.Tile("hoi:test/law_poor","부족한 안보","§c400§r","politics/vacant","효과 없음"),
                new DialogView.Tile("hoi:test/law_open","온건한 안보","§e200§r","politics/vacant","효과 없음"),
                new DialogView.Tile("hoi:test/law_current","집중 감시","적용 중","politics/vacant","효과 없음"),
                new DialogView.Tile("hoi:test/law_locked","감시국가","§e100§r","politics/vacant","효과 없음"));
        var lawView=new DialogView(UUID.randomUUID().toString(),1,DialogView.Kind.POLITICS,"안보법","§Y집중 감시§!(을)를 교체하시겠습니까?","","","","",laws,
                List.of(new DialogView.Choice("hoi:test/law_open","온건한 안보")));
        context.setScreen(() -> new PoliticsChoice(lawView,null));
        context.getInput().setCursorPos(2400,1300);context.waitTicks(3);
        context.takeScreenshot("hoi-politics-choice-law-list-gui2");
        context.runOnClient(client -> {
            var buttons=client.gui.screen().children().stream().filter(c -> c instanceof Button).map(c -> (Button)c)
                    .filter(b -> laws.stream().anyMatch(tile -> tile.name().equals(b.getMessage().getString()))).toList();
            if(buttons.size()!=laws.size() || buttons.stream().map(Button::getX).distinct().count()!=1)throw new AssertionError("Law candidates use the source single-column list");
            if(buttons.stream().anyMatch(b -> b.getWidth()!=231 || b.getHeight()!=37))throw new AssertionError("Law rows keep the source 462x75 slot at GUI 2");
        });
        context.setScreen(() -> null);
    }
    record DialogFixtures(List<DialogView> views, List<String> sounds) {}
    public static void run(ClientGameTestContext context) {
        DialogEventFrameTest.run(context);
        disconnectThread(context);
        MapDetailChecks.run(context);
        MapAirDetailChecks.run(context);
        detailFlags(context);
        tooltipNumbers(context);
        choiceLayouts(context);
        DialogFixtures fixture;
        try (var input = DialogScreenChecks.class.getResourceAsStream("/dialog-fixtures.json")) {
            fixture = new Gson().fromJson(new InputStreamReader(Objects.requireNonNull(input), StandardCharsets.UTF_8), DialogFixtures.class);
        } catch (IOException error) { throw new UncheckedIOException(error); }
        context.runOnClient(client -> {
            DialogPayloads.registerPayloadTypes(); DialogPayloads.registerPayloadTypes();
            for (var path : fixture.sounds()) if (client.getSoundManager().getSoundEvent(Identifier.fromNamespaceAndPath("hoi", path)) == null)
                throw new AssertionError("Missing super-event sound: " + path);
            for (var view : fixture.views()) {
                if (!EnumSet.of(DialogView.Kind.EVENT, DialogView.Kind.GLOBAL_EVENT, DialogView.Kind.SUPER_EVENT).contains(view.kind()))
                    throw new AssertionError("Generic dialog fixtures must not recreate country or map detail screens");
                if (!view.image().isEmpty() && client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + view.image() + ".png")).isEmpty())
                    throw new AssertionError("Missing event picture: " + view.title());
                var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
                try { var payload = DialogPayloads.Show.of(view, true); DialogPayloads.Show.CODEC.encode(buffer, payload);
                    if (!view.equals(DialogPayloads.Show.CODEC.decode(buffer).view())) throw new AssertionError("Dialog codec must preserve original text");
                } finally { buffer.release(); }
            }
        });
        var choices = java.util.stream.IntStream.rangeClosed(1, 11).mapToObj(i -> new DialogView.Choice("option-" + i, "선택 " + i)).toList();
        var multiChoice = new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.EVENT, "여러 선택지가 있는 국가 이벤트", "",
                "긴 이벤트 본문과 선택지가 작은 창에서도 겹치지 않아야 합니다.\n".repeat(35), "", "", "", List.of(), choices);
        var views = new ArrayList<>(fixture.views()); views.add(multiChoice);
        for (var view : views) {
            context.setScreen(() -> new DialogScreen(view)); context.waitTicks(4);
            context.takeScreenshot("hoi-dialog-" + view.kind().name().toLowerCase(Locale.ROOT));
        }
        for (int[] size : new int[][]{{2560, 1440}, {1600, 1000}, {854, 480}, {640, 360}}) {
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
            for (var view : views) {
                context.setScreen(() -> new DialogScreen(view)); context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (DialogScreen) client.gui.screen();
                    if (size[0] >= 1600 && (screen.width != size[0] / 2 || screen.height != size[1] / 2))
                        throw new AssertionError("Reference captures require effective GUI scale 2");
                    float expectedScale=dev.hoi.client.ui.UiText.scale(client.font);
                    if (screen.textScale()!=expectedScale) throw new AssertionError("Every event uses the minimum 10 GUI font height");
                    if (screen.panelLeft() < 0 || screen.panelTop() < 0 || screen.panelLeft() + screen.panelWidth() > screen.width || screen.panelTop() + screen.panelHeight() > screen.height) throw new AssertionError("Source event frame outside viewport");
                    if (size[0] == 2560 && view.kind() == DialogView.Kind.SUPER_EVENT && screen.panelWidth() <= screen.panelHeight())
                        throw new AssertionError("Super event keeps the wide reference proportions");
                    if (size[0] == 2560 && (view.kind() == DialogView.Kind.EVENT || view.kind() == DialogView.Kind.GLOBAL_EVENT) && screen.panelWidth() >= screen.panelHeight())
                        throw new AssertionError("Country and global events keep portrait proportions");
                    if (screen.bodyViewportHeight() <= 0) throw new AssertionError("Choices must leave a visible body viewport");
                    for (var child : screen.children()) if (child instanceof Button b && (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height))
                        throw new AssertionError("Dialog control outside compact screen");
                    int[] body = screen.eventBodyBounds();
                    screen.mouseScrolled(body[0] + 1, body[1] + 1, 0, -100);
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
            for (var kind : List.of(DialogView.Kind.EVENT, DialogView.Kind.GLOBAL_EVENT, DialogView.Kind.SUPER_EVENT)) {
                var sent = new ArrayList<String>();
                var view = new DialogView(UUID.randomUUID().toString(), 7, kind, "Enter 확인", "", "서버 선택지", "", "", "",
                        List.of(), List.of(new DialogView.Choice("issued-choice", "확인")));
                var screen = new DialogScreen(view) {
                    @Override protected void choose(String choice) { sent.add(view().token() + "/" + view().revision() + "/" + choice); }
                };
                client.gui.setScreen(screen);
                for (int key : new int[]{com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, com.mojang.blaze3d.platform.InputConstants.KEY_NUMPADENTER}) {
                    int before = sent.size();
                    screen.keyPressed(new net.minecraft.client.input.KeyEvent(key, 0, 0));
                    if (sent.size() != before + 1 || !sent.getLast().equals(view.token() + "/7/issued-choice"))
                        throw new AssertionError("Enter must submit exactly the server-issued event choice");
                }
                var multiple = new DialogView(view.token(), 8, kind, view.title(), "", view.body(), "", "", "", List.of(),
                        List.of(new DialogView.Choice("a", "첫 선택"), new DialogView.Choice("b", "둘째 선택")));
                screen.update(multiple); screen.setFocused(null);
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                if (client.gui.screen() != null || (kind == DialogView.Kind.SUPER_EVENT ? !sent.getLast().equals(view.token() + "/8/") : sent.size() != 2))
                    throw new AssertionError("Enter without a selected option minimizes answerable events without a gameplay request");
                client.gui.setScreen(screen);
                if (kind == DialogView.Kind.SUPER_EVENT) {
                    var next = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("다음 선택지")).findFirst().orElseThrow();
                    next.onPress(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                }
                var second = (Button)screen.children().stream().filter(w -> w instanceof Button b && b.getMessage().getString().equals("둘째 선택")).findFirst().orElseThrow();
                screen.setFocused(second);
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN, 0, 0));
                if (!sent.getLast().equals(view.token() + "/8/b")) throw new AssertionError("Enter activates the focused server choice");
            }
            client.gui.setScreen(null);
            var one = fixture.views().getFirst().issued(UUID.randomUUID().toString(), 0);
            DialogCoordinator.receive(DialogPayloads.Show.of(one, true));
            DialogCoordinator.close(one.token());
            DialogCoordinator.receive(DialogPayloads.Show.of(one.issued(one.token(), 1), false));
            if (client.gui.screen() != null) throw new AssertionError("Late refresh reopened a revoked private dialog");
        });
        context.runOnClient(client -> {
            var event = fixture.views().stream().filter(v -> v.kind() == DialogView.Kind.SUPER_EVENT).findFirst().orElseThrow()
                    .issued(UUID.randomUUID().toString(), 0);
            DialogCoordinator.receive(DialogPayloads.Show.of(event, true));
            if (!(client.gui.screen() instanceof DialogStack stack) || stack.find(event.token()) == null) throw new AssertionError("Super event must be visible before stop");
            DialogCoordinator.close(event.token());
            if (!dev.hoi.client.audio.AudioSuperEvent.active()) throw new AssertionError("Closing a popup must preserve event music");
            dev.hoi.client.audio.AudioStoryMusic.play("music.korea.extinction");
            if (!dev.hoi.client.audio.AudioSuperEvent.active()) throw new AssertionError("National focus waits for global super event music");
            dev.hoi.client.audio.AudioUiSounds.receive(AudioPayloads.Cue.STOP);
            if (client.gui.screen() != null || dev.hoi.client.audio.AudioSuperEvent.active())
                throw new AssertionError("Stop must dismiss visible super event and its audio");
            client.gui.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
            DialogCoordinator.receive(DialogPayloads.Show.of(event.issued(UUID.randomUUID().toString(), 0), true));
            if (client.gui.screen() instanceof DialogStack) throw new AssertionError("Pause menu must defer super event");
            if (!dev.hoi.client.audio.AudioSuperEvent.active()) throw new AssertionError("Deferred popup must still start global music immediately");
            dev.hoi.client.audio.AudioUiSounds.receive(AudioPayloads.Cue.STOP);
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
