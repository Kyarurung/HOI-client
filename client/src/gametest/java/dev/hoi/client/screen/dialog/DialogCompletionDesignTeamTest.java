package dev.hoi.client.screen.dialog;

import dev.hoi.protocol.dialog.DialogView;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class DialogCompletionDesignTeamTest {
    private DialogCompletionDesignTeamTest() {}
    public static void run(ClientGameTestContext context) {
        int previous = context.computeOnClient(client -> client.options.guiScale().get());
        try {
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var requests = new ArrayList<String>();
                for (String fixture : List.of("kor-research-complete-design-team", "kor-research-complete-no-design-team", "prc-agency-upgrade-complete")) {
                    DialogView source;
                    try {
                        source = new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(java.nio.file.Path.of(System.getenv("HOI_COMPLETION_PREVIEW"), fixture + ".json")), DialogView.class).issued(UUID.randomUUID().toString(), 3);
                    } catch (java.io.IOException e) { throw new RuntimeException(e); }
                    if (source.tiles().isEmpty() == fixture.equals("kor-research-complete-design-team")) throw new AssertionError("Authoritative completed research must preserve its actual assignment");
                    context.setScreen(() -> new DialogCompletion(source, (v, choice) -> requests.add(choice)));
                    context.getInput().setCursorPos(size[0] - 2, size[1] - 2);
                    context.waitTicks(3);
                    var captured = context.takeScreenshot("hoi-completion-source-" + fixture + "-" + size[0] + "-gui2");
                    var bounds = context.computeOnClient(client -> {
                        var screen = (DialogCompletion)client.gui.screen();
                        return new int[]{screen.panelLeft() * 2, screen.panelTop() * 2};
                    });
                    try {
                        var actual = javax.imageio.ImageIO.read(captured.toFile());
                        var colors = new java.util.HashSet<Integer>();
                        for (int y = bounds[1] + 90; y < bounds[1] + 145; y++) for (int x = bounds[0] + 24; x < bounds[0] + 80; x++) colors.add(actual.getRGB(x, y) & 0xFFFFFF);
                        if (source.presentation().equals("research_complete") && !colors.equals(java.util.Set.of(0x161616))) throw new AssertionError("Research completion has a plain black background: " + colors.size());
                        if (source.presentation().equals("agency_upgrade_complete") && colors.size() < 30) throw new AssertionError("Agency completion preserves the original textured background");
                    } catch (java.io.IOException e) { throw new RuntimeException(e); }
                    context.runOnClient(client -> {
                        if (source.presentation().equals("agency_upgrade_complete")) {
                            var screen = (DialogCompletion)client.gui.screen();
                            if (!source.completion() || screen.panelHeight() != 130 || dev.hoi.client.ui.UiAssets.imageSize(source.image())[0] == 0) throw new AssertionError("Agency completion uses the native compact frame and authoritative image");
                            for (String choice : List.of("details", "ack")) {
                                var button = (net.minecraft.client.gui.components.Button)screen.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(choice.equals("details") ? "세부 사항" : "확인")).findFirst().orElseThrow();
                                int count = requests.size();
                                var right = new net.minecraft.client.input.MouseButtonEvent(button.getX() + 2, button.getY() + 2, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, 0));
                                screen.mouseClicked(right, false); screen.mouseReleased(right);
                                if (requests.size() != count) throw new AssertionError("Right click must not choose an agency completion action");
                                var left = new net.minecraft.client.input.MouseButtonEvent(button.getX() + 2, button.getY() + 2, new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                                screen.mouseClicked(left, false); screen.mouseReleased(left);
                                if (requests.size() != count + 1 || !requests.getLast().equals(choice)) throw new AssertionError("Agency completion dispatches the issued choice");
                            }
                            requests.clear();
                        }
                        for (var tile : source.tiles()) if (tile.section().equals("design_team") && dev.hoi.client.ui.UiAssets.imageSize(tile.icon())[0] == 0) throw new AssertionError("Missing authoritative design team art: " + tile.icon());
                    });
                }
                var team = new DialogView.Tile("design_team", "HD현대", "", "mio/country/kor/gfx_idea_kor_hdhyundai/frame_0", "");
                for (String presentation : List.of("research_complete", "focus_complete")) {
                    java.awt.image.BufferedImage without = null;
                    for (boolean assigned : List.of(false, true)) {
                        var view = new DialogView(UUID.randomUUID().toString(), 3, DialogView.Kind.DIPLOMACY, "완료 알림", "", "", "", "category/infantry", "", assigned ? List.of(team) : List.of(), List.of(new DialogView.Choice("details", "세부 사항"), new DialogView.Choice("ack", "확인")), presentation);
                        context.setScreen(() -> new DialogCompletion(view, (v, choice) -> requests.add(choice)));
                        context.getInput().setCursorPos(size[0] - 2, size[1] - 2);
                        context.waitTicks(3);
                        var file = context.takeScreenshot("hoi-completion-team-" + presentation + "-" + assigned + "-" + size[0] + "-gui2");
                        var region = context.computeOnClient(client -> {
                            var screen = (DialogCompletion)client.gui.screen();
                            return new int[]{(screen.panelLeft() + screen.panelWidth() * 400 / 500 - 28) * 2, (screen.panelTop() + 65 - 28) * 2};
                        });
                        try {
                            var actual = javax.imageio.ImageIO.read(file.toFile());
                            if (!assigned) without = actual;
                            else {
                                int changed = 0;
                                for (int y = region[1]; y < region[1] + 112; y++) for (int x = region[0]; x < region[0] + 112; x++) if (actual.getRGB(x, y) != without.getRGB(x, y)) changed++;
                                if (presentation.equals("research_complete") ? changed < 100 : changed != 0) throw new AssertionError("Only assigned research completion draws the original team logo: " + changed);
                            }
                        } catch (java.io.IOException e) { throw new RuntimeException(e); }
                    }
                }
                if (!requests.isEmpty()) throw new AssertionError("Rendering a completed design team must not assign it or request rewards");
            }
        } finally {
            context.runOnClient(client -> client.options.guiScale().set(previous));
        }
    }
}
