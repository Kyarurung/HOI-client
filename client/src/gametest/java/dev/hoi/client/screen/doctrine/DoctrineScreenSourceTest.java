package dev.hoi.client.screen.doctrine;

import dev.hoi.client.ui.tooltip.TooltipKeywordIcons;
import dev.hoi.protocol.doctrine.*;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

public final class DoctrineScreenSourceTest {
    private DoctrineScreenSourceTest() {}
    public static void runFullscreenHeader(ClientGameTestContext context) {
        try {
            var view = fixture("doctrine-land-selected");
            for (int[] size : List.of(new int[]{1280, 720}, new int[]{2560, 1440})) {
                context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
                context.setScreen(() -> new DoctrineScreen(null, "land", view.session(), view, request -> {}));
                dev.hoi.client.ui.hud.HudFullscreenTest.run(context, "doctrine", size[0]);
            }
            context.setScreen(() -> null);
        } catch (Exception error) { throw new RuntimeException(error); }
    }
    private static DoctrineView fixture(String name) throws Exception {
        String directory = System.getenv("HOI_DOCTRINE_PREVIEW");
        String json;
        if (directory != null) json = Files.readString(Path.of(directory, name + ".json"));
        else try (var input = DoctrineScreenSourceTest.class.getResourceAsStream("/doctrine/" + name + ".json")) {
            if (input == null) throw new AssertionError("Missing server doctrine fixture " + name);
            json = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        return new DoctrinePayloads.Response(json).view();
    }
    private static int[] position(ClientGameTestContext context, String name) {
        return context.computeOnClient(client -> {
            var button = client.gui.screen().children().stream().filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                .filter(c -> c.getMessage().getString().equals(name)).toList().getLast();
            return new int[]{button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
        });
    }
    public static void runIconReview(ClientGameTestContext context) {
        try {
            String directory=Objects.requireNonNull(System.getenv("HOI_DOCTRINE_GRAND_PREVIEW"));
            var view=new DoctrinePayloads.Response(Files.readString(Path.of(directory,"doctrine-land-grand-selected.json"))).view();
            var infantry=view.tracks().stream().filter(t->t.id().equals("infantry")).findFirst().orElseThrow();
            var future=infantry.candidates().stream().filter(c->c.id().equals("future_soldier")).findFirst().orElseThrow();
            var mechanized=view.tracks().stream().filter(t->t.id().equals("mechanized")).findFirst().orElseThrow();
            previewGlowReview(context, view, infantry, future);
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
                var actions=new ArrayList<DoctrinePayloads.Request>();
                var clock = new java.util.concurrent.atomic.AtomicLong(10000);
                context.setScreen(()->new DoctrineScreen(null,"land",view.session(),view,actions::add,clock::get));
                click(context,position(context,infantry.name()));click(context,position(context,future.name()));
                clock.addAndGet(3000);
                context.getInput().setCursorPos(size[0]-2,80);context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-future-soldier-"+size[0]+"-gui2");
                click(context,position(context,"선택 창 닫기"));
                var target=position(context,mechanized.name()+" mastery");
                context.getInput().setCursorPos(target[0]*2,target[1]*2);context.waitTicks(3);
                context.takeScreenshot("hoi-doctrine-mechanized-mastery-"+size[0]+"-gui2");
                if(!actions.isEmpty())throw new AssertionError("Source icon review cannot issue doctrine mutations");
            }
            branchColorReview(context, directory);
            milestoneStateReview(context, directory);
            specialForcesSelectionReview(context, directory);
            fullPreviewReview(context);
            authoritativeMilestoneReview(context);
            centeredLayoutStress(context);
        }catch(Exception failure){throw new AssertionError("Original doctrine icon review failed",failure);}
        finally{context.setScreen(()->null);}
    }
    private static void previewGlowReview(ClientGameTestContext context, DoctrineView view,
            DoctrineView.Track track, DoctrineView.Choice choice) throws Exception {
        var milestoneView = new DoctrinePayloads.Response(Files.readString(Path.of(
            Objects.requireNonNull(System.getenv("HOI_DOCTRINE_MILESTONE_PREVIEW")), "kor-doctrine-preview-land.json"))).view();
        long[] times = {0, 300, 600, 900, 1500, 2100, 2700, 3000};
        int[][] expected = {{0, 0, 0, 0, 0}, {128, 0, 0, 0, 0}, {255, 0, 0, 0, 0},
            {255, 128, 0, 0, 0}, {255, 255, 128, 0, 0}, {255, 255, 255, 128, 0},
            {255, 255, 255, 255, 128}, {255, 255, 255, 255, 255}};
        if (choice.selected() || choice.rewards().size() != 5) throw new AssertionError("Glow review requires five unselected source rewards");
        for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
            var clock = new java.util.concurrent.atomic.AtomicLong(10000);
            var actions = new ArrayList<DoctrinePayloads.Request>();
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(client -> client.options.guiScale().set(2));
            context.setScreen(() -> new DoctrineScreen(null, "land", view.session(), view, actions::add, clock::get));
            click(context, position(context, track.name()));
            click(context, position(context, choice.name()));
            dragPicker(context, size[0] / 2 - 284);
            context.getInput().setCursorPos(size[0] - 2, 80);
            for (int i = 0; i < times.length; i++) {
                clock.set(10000 + times[i]);
                int[] alphas = expected[i];
                context.runOnClient(client -> {
                    var screen = (DoctrineScreen)client.gui.screen();
                    for (int j = 0; j < alphas.length; j++) if (screen.previewAlpha(j) != alphas[j])
                        throw new AssertionError("Reward " + j + " opacity differs at " + (clock.get() - 10000));
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-preview-glow-" + times[i] + "ms-" + size[0] + "-gui2");
            }
            click(context, position(context, choice.name()));
            context.runOnClient(client -> {
                if (((DoctrineScreen)client.gui.screen()).previewAlpha(0) != 0)
                    throw new AssertionError("Repeated selection must restart glow");
            });
            var other = track.candidates().stream().filter(candidate -> !candidate.id().equals(choice.id())).findFirst().orElseThrow();
            click(context, position(context, other.name()));
            context.runOnClient(client -> {
                if (((DoctrineScreen)client.gui.screen()).previewAlpha(0) != 0)
                    throw new AssertionError("Changing preview must restart glow");
            });
            click(context, position(context, "선택 창 닫기"));
            clock.addAndGet(5000);
            click(context, position(context, track.name()));
            click(context, position(context, choice.name()));
            context.runOnClient(client -> {
                if (((DoctrineScreen)client.gui.screen()).previewAlpha(0) != 0 || !actions.isEmpty())
                    throw new AssertionError("Reopening preview must restart glow without server mutations");
            });
            context.setScreen(() -> new DoctrineScreen(null, "land", milestoneView.session(), milestoneView, actions::add, clock::get));
            var grand = milestoneView.grands().stream().filter(DoctrineView.Choice::selected).map(DoctrineView.Choice::name).findFirst().orElse("대교리 선택");
            var candidate = milestoneView.grands().stream().filter(value -> !value.selected()).findFirst().orElseThrow();
            if (milestoneView.previewTracks().isEmpty() || candidate.milestones().isEmpty())
                throw new AssertionError("Grand glow review requires authoritative preview tracks and milestones");
            click(context, position(context, grand));
            click(context, position(context, candidate.name()));
            long start = clock.get();
            context.getInput().setCursorPos(size[0] - 2, 80);
            for (int time : new int[]{0, 200, 400}) {
                clock.set(start + time);
                context.runOnClient(client -> {
                    var screen = (DoctrineScreen)client.gui.screen();
                    if (!screen.grandPreview() || screen.milestonePreviewAlpha() != Math.round(time * 255f / 400))
                        throw new AssertionError("Grand milestone fade differs at " + time);
                });
                context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-milestone-glow-" + time + "ms-" + size[0] + "-gui2");
            }
            if (!actions.isEmpty()) throw new AssertionError("Grand preview must not mutate doctrine");
        }
    }
    private static void branchColorReview(ClientGameTestContext context, String directory) throws Exception {
        for (String folder : List.of("land", "naval", "air", "special_forces")) {
            var source = new DoctrinePayloads.Response(Files.readString(Path.of(directory, "doctrine-" + folder + "-sub-selected.json"))).view();
            var tracks = new ArrayList<DoctrineView.Track>();
            for (var track : source.tracks()) {
                var candidate = track.candidates().stream().filter(DoctrineView.Choice::selected).findFirst().orElse(track.candidates().getFirst());
                var rewards = new ArrayList<DoctrineView.Reward>();
                for (int i = 0; i < candidate.rewards().size(); i++) {
                    var reward = candidate.rewards().get(i);
                    rewards.add(new DoctrineView.Reward(reward.id(), reward.mastery(), reward.detail(), i == 0));
                }
                double mastery = Double.parseDouble(rewards.getFirst().mastery()) + Double.parseDouble(rewards.get(1).mastery()) / 2;
                tracks.add(new DoctrineView.Track(track.id(), track.name(), track.background(), track.icon(), track.decoration(),
                    candidate.id(), candidate.name(), Double.toString(mastery), "0", track.bank(), rewards, track.candidates()));
            }
            var progress = new DoctrineView(source.session(), source.revision(), source.country(), source.progressionRevision(), source.folder(), source.branch(),
                source.experience(), source.grand(), source.folders(), source.grands(), tracks, source.message(), source.uiText(), source.previewTracks());
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var actions = new ArrayList<DoctrinePayloads.Request>();
                context.setScreen(() -> new DoctrineScreen(null, folder, source.session(), source, actions::add));
                context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-colors-" + folder + "-source-" + size[0] + "-gui2");
                context.setScreen(() -> new DoctrineScreen(null, folder, progress.session(), progress, actions::add));
                context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-colors-" + folder + "-synthetic-progress-" + size[0] + "-gui2");
                if (!actions.isEmpty()) throw new AssertionError("Color rendering cannot mutate authoritative doctrine state");
            }
        }
    }
    private static void specialForcesSelectionReview(ClientGameTestContext context, String directory) throws Exception {
        var source = new DoctrinePayloads.Response(Files.readString(Path.of(directory, "doctrine-special_forces-sub-selected.json"))).view();
        var first = source.tracks().getFirst();
        var second = source.tracks().get(1);
        var duplicate = second.candidates().stream().filter(candidate -> candidate.id().equals(first.selected())).findFirst().orElseThrow();
        var available = second.candidates().stream().limit(10).filter(DoctrineView.Choice::enabled).findFirst().orElseThrow();
        if (duplicate.enabled() || duplicate.requirementsMet() || duplicate.assignDetail().isBlank()) throw new AssertionError("Server fixture must reject the same subdoctrine in a second special-forces track");
        for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.runOnClient(client -> client.options.guiScale().set(2));
            var actions = new ArrayList<DoctrinePayloads.Request>();
            context.setScreen(() -> new DoctrineScreen(null, source.folder(), source.session(), source, actions::add));
            click(context, trackPosition(context, 1));
            click(context, position(context, duplicate.name()));
            var unlock = position(context, "잠금 해제");
            click(context, unlock); click(context, unlock);
            context.runOnClient(client -> {
                client.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(unlock[0], unlock[1],
                    new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, 0)), false);
                var screen = (DoctrineScreen)client.gui.screen();
                var button = screen.children().stream().filter(child -> child instanceof AbstractWidget widget && widget.getMessage().getString().equals("잠금 해제")).map(child -> (AbstractWidget)child).findFirst().orElseThrow();
                if (button.active || !actions.isEmpty() || !screen.preview().id().equals(duplicate.id())) throw new AssertionError("Duplicate preview must remain inspectable with disabled unlock and no request");
            });
            context.getInput().setCursorPos(unlock[0] * 2, unlock[1] * 2); context.waitTicks(2);
            context.runOnClient(client -> {
                if (!((DoctrineScreen)client.gui.screen()).hoveredDetail().equals(duplicate.assignDetail())) throw new AssertionError("Duplicate rejection tooltip must preserve the server wording");
            });
            context.takeScreenshot("hoi-doctrine-special-duplicate-" + size[0] + "-gui2");
            click(context, position(context, available.name()));
            context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
            context.takeScreenshot("hoi-doctrine-special-available-" + size[0] + "-gui2");
            click(context, position(context, "잠금 해제")); click(context, position(context, "잠금 해제"));
            context.runOnClient(client -> {
                var expected = new DoctrinePayloads.Request(DoctrinePayloads.Kind.SELECT_SUBDOCTRINE, source.session(), source.revision(), source.folder(), second.id(), available.id());
                if (!actions.equals(List.of(expected))) throw new AssertionError("A different eligible special-forces subdoctrine must issue exactly one request to the second track");
                ((DoctrineScreen)client.gui.screen()).update(source);
            });
            click(context, position(context, "잠금 해제"));
            if (actions.size() != 1) throw new AssertionError("Stale special-forces response cannot unlock repeated requests");
        }
    }
    private static void milestoneStateReview(ClientGameTestContext context, String directory) throws Exception {
        var source = new DoctrinePayloads.Response(Files.readString(Path.of(directory, "doctrine-land-sub-selected.json"))).view();
        var sourceTrack = source.tracks().getFirst();
        for (boolean unlocked : List.of(false, true)) {
            var milestone = new DoctrineView.Milestone(sourceTrack.id(), "이정표", sourceTrack.icon(), unlocked, sourceTrack.candidates().getFirst().detail());
            var track = new DoctrineView.Track(sourceTrack.id(), sourceTrack.name(), sourceTrack.background(), sourceTrack.icon(), sourceTrack.decoration(),
                sourceTrack.selected(), sourceTrack.nameplate(), sourceTrack.mastery(), sourceTrack.dailyMastery(), sourceTrack.bank(), sourceTrack.rewards(), sourceTrack.candidates(), milestone);
            var tracks = new ArrayList<>(source.tracks()); tracks.set(0, track);
            var view = new DoctrineView(source.session(), source.revision(), source.country(), source.progressionRevision(), source.folder(), source.branch(),
                source.experience(), source.grand(), source.folders(), source.grands(), tracks, source.message(), source.uiText(), source.previewTracks());
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var actions = new ArrayList<DoctrinePayloads.Request>();
                context.setScreen(() -> new DoctrineScreen(null, "land", view.session(), view, actions::add));
                context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (DoctrineScreen)client.gui.screen();
                    if (!screen.displayedMilestone(track).equals(milestone)) throw new AssertionError("Milestone display must use authoritative state independently of selected subdoctrine");
                });
                context.takeScreenshot("hoi-doctrine-milestone-synthetic-" + unlocked + "-" + size[0] + "-gui2");
                var at = position(context, milestone.name());
                context.getInput().setCursorPos(at[0] * 2, at[1] * 2); context.waitTicks(2);
                context.runOnClient(client -> {
                    var screen = (DoctrineScreen)client.gui.screen();
                    if (!screen.hoveredDetail().equals(milestone.detail())) throw new AssertionError("Milestone tooltip must retain the issued text");
                });
                context.takeScreenshot("hoi-doctrine-milestone-synthetic-tooltip-" + unlocked + "-" + size[0] + "-gui2");
                click(context, at); click(context, at);
                if (!actions.isEmpty()) throw new AssertionError("Milestone inspection must not mutate progression");
            }
        }
    }
    private static void authoritativeMilestoneReview(ClientGameTestContext context) throws Exception {
        String directory = System.getenv("HOI_DOCTRINE_MILESTONE_PREVIEW");
        if (directory == null) return;
        var failures = new ArrayList<String>();
        for (String state : List.of("unselected", "selected", "progress", "complete")) {
            var source = new DoctrinePayloads.Response(Files.readString(Path.of(directory, "kor-doctrine-milestones-" + state + ".json"))).view();
            if (state.equals("unselected") && !source.tracks().isEmpty()) throw new AssertionError("Unassigned grand fixture must have no active tracks");
            if (state.equals("complete") && (!source.tracks().getFirst().milestone().unlocked() || source.tracks().get(1).milestone().unlocked())) throw new AssertionError("Completion fixture must distinguish completed infantry from incomplete support");
            if (!state.equals("complete") && source.tracks().stream().anyMatch(t -> t.milestone().unlocked())) throw new AssertionError("Selecting a subdoctrine or partial rewards cannot unlock a milestone");
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                var actions = new ArrayList<DoctrinePayloads.Request>();
                context.setScreen(() -> new DoctrineScreen(null, source.folder(), source.session(), source, actions::add));
                context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-milestones-" + state + "-" + size[0] + "-gui2");
                if (!source.tracks().isEmpty()) milestoneTooltipReview(context, source.tracks().getFirst().milestone(), state, size[0], failures);
                for (var candidate : source.grands()) {
                    context.setScreen(() -> new DoctrineScreen(null, source.folder(), source.session(), source, actions::add));
                    String grand = source.grands().stream().filter(DoctrineView.Choice::selected).map(DoctrineView.Choice::name).findFirst().orElse("대교리 선택");
                    click(context, position(context, grand));
                    click(context, position(context, candidate.name()));
                    context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                    String name = state + "-preview-" + candidate.id();
                    context.takeScreenshot("hoi-doctrine-milestones-" + name + "-" + size[0] + "-gui2");
                    milestoneTooltipReview(context, candidate.milestones().getFirst(), name, size[0], failures);
                    var actual = context.computeOnClient(client -> client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                        .filter(w -> w.getWidth() == 53 && w.getHeight() == 42).map(w -> w.getMessage().getString()).toList());
                    int visible = (size[0] / 2 + 211) / 212;
                    var expected = candidate.milestones().stream().filter(m -> {
                        int index = candidate.milestones().indexOf(m);
                        if (!candidate.previewTracks().isEmpty()) index = candidate.previewTracks().indexOf(m.track());
                        return index < visible;
                    }).map(DoctrineView.Milestone::name).toList();
                    if (!actual.equals(expected)) failures.add(name + " " + size[0] + " preview entries differ: " + actual.size() + " != " + expected.size());
                    if (size[0] == 1280) {
                        context.runOnClient(client -> client.gui.screen().mouseScrolled(10, 160, 0, -100));
                        milestoneTooltipReview(context, candidate.milestones().getLast(), name + "-scroll-last", size[0], failures);
                    }
                    click(context, position(context, "선택 창 닫기"));
                    context.runOnClient(client -> {
                        var screen = (DoctrineScreen)client.gui.screen();
                        if (screen.preview() != null) throw new AssertionError("Closing candidate picker must clear preview state");
                        for (var track : source.tracks()) if (!screen.displayedMilestone(track).equals(track.milestone())) throw new AssertionError("Closing preview must restore current authoritative milestones");
                    });
                }
                if (actions.stream().anyMatch(request -> request.kind() == DoctrinePayloads.Kind.SELECT_GRAND || request.kind() == DoctrinePayloads.Kind.SELECT_SUBDOCTRINE)) throw new AssertionError("Milestone previews and repeated icon clicks must not spend XP or mutate progression");
            }
        }
        var complete = new DoctrinePayloads.Response(Files.readString(Path.of(directory, "kor-doctrine-milestones-complete.json"))).view();
        var partial = new DoctrineView(complete.session(), complete.revision(), complete.country(), complete.progressionRevision(), complete.folder(), complete.branch(),
            complete.experience(), complete.grand(), complete.folders(), complete.grands(), complete.tracks().subList(0, 2), complete.message(), complete.uiText(), complete.previewTracks());
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.setScreen(() -> new DoctrineScreen(null, partial.folder(), partial.session(), partial, request -> {}));
        click(context, position(context, partial.grands().stream().filter(DoctrineView.Choice::selected).findFirst().orElseThrow().name()));
        click(context, position(context, partial.grands().getFirst().name()));
        context.runOnClient(client -> {
            var positions = client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(w -> w.getWidth() == 53 && w.getHeight() == 42).map(AbstractWidget::getX).toList();
            if (positions.size() != 6 || positions.stream().distinct().count() != 6) failures.add("Synthetic missing-track candidate milestones overlap");
        });
        context.getInput().setCursorPos(2558, 80); context.waitTicks(2);
        context.takeScreenshot("hoi-doctrine-milestones-synthetic-missing-tracks-2560-gui2");
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }
    private static void fullPreviewReview(ClientGameTestContext context) throws Exception {
        String directory = System.getenv("HOI_DOCTRINE_MILESTONE_PREVIEW");
        if (directory == null || !Files.exists(Path.of(directory, "kor-doctrine-preview-land.json"))) return;
        var failures = new ArrayList<String>();
        for (String folder : List.of("land", "naval", "air", "special_forces")) {
            var source = new DoctrinePayloads.Response(Files.readString(Path.of(directory, "kor-doctrine-preview-" + folder + ".json"))).view();
            if (source.previewTracks().isEmpty()) throw new AssertionError("Full preview requires authoritative track data");
            for (int[] size : List.of(new int[]{2560,1440}, new int[]{1280,720})) {
                context.getInput().resizeWindow(size[0], size[1]);
                context.runOnClient(client -> client.options.guiScale().set(2));
                for (var candidate : source.grands()) {
                    var actions = new ArrayList<DoctrinePayloads.Request>();
                    context.setScreen(() -> new DoctrineScreen(null, source.folder(), source.session(), source, actions::add));
                    String current = source.grands().stream().filter(DoctrineView.Choice::selected).map(DoctrineView.Choice::name).findFirst().orElse("대교리 선택");
                    click(context, position(context, current));
                    click(context, position(context, candidate.name()));
                    String scene = "hoi-doctrine-full-preview-" + folder + "-" + candidate.id() + "-" + size[0] + "-gui2";
                    context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                    context.takeScreenshot(scene);
                    var expected = candidate.previewTracks().stream().map(id -> source.previewTracks().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow()).toList();
                    var visible = context.computeOnClient(client -> client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                        .filter(w -> w.getWidth() == 74 && w.getHeight() == 55).toList());
                    int count = Math.min(expected.size(), (size[0] / 2 + 211) / 212);
                    if (!visible.stream().map(w -> w.getMessage().getString()).toList().equals(expected.subList(0, count).stream().map(DoctrineView.Track::name).toList())) failures.add(scene + " wrong full track list/order");
                    if (!candidate.selected()) {
                        for (var button : visible) if (!button.active) failures.add(scene + " read-only track unavailable");
                        dragPicker(context, size[0] / 2 - 284);
                        context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                        context.takeScreenshot(scene + "-moved");
                        click(context, trackPosition(context, 0));
                        var track = expected.getFirst();
                        var sub = track.candidates().getFirst();
                        click(context, position(context, sub.name()));
                        context.runOnClient(client -> {
                            var screen = (DoctrineScreen)client.gui.screen();
                            if (!screen.grandPreview() || !screen.preview().id().equals(sub.id())
                                    || !screen.displayedTrack(track).rewards().equals(sub.rewards())) failures.add(scene + " missing read-only candidate rewards");
                            var unlock = screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                                .filter(w -> w.getMessage().getString().equals("잠금 해제")).findFirst().orElseThrow();
                            if (unlock.active) failures.add(scene + " read-only unlock enabled");
                        });
                        var unlock = position(context, "잠금 해제");
                        click(context, unlock); click(context, unlock);
                        context.runOnClient(client -> client.gui.screen().mouseClicked(new net.minecraft.client.input.MouseButtonEvent(unlock[0], unlock[1],
                            new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, 0)), false));
                        context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                        context.takeScreenshot(scene + "-subdoctrine");
                        var reward = sub.rewards().getFirst();
                        var rewardAt = context.computeOnClient(client -> {
                            var hover = client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                                .filter(w -> w.getWidth() == 50 && w.getMessage().getString().equals("통달 " + reward.mastery())).findFirst().orElseThrow();
                            return new int[]{hover.getX() + hover.getWidth() / 2, hover.getY() + hover.getHeight() / 2};
                        });
                        context.getInput().setCursorPos(rewardAt[0] * 2, rewardAt[1] * 2); context.waitTicks(2);
                        context.runOnClient(client -> {
                            if (!((DoctrineScreen)client.gui.screen()).hoveredDetail().equals(reward.detail())) failures.add(scene + " altered reward tooltip");
                        });
                        context.takeScreenshot(scene + "-reward-tooltip");
                        click(context, position(context, "선택 창 닫기"));
                        context.runOnClient(client -> {
                            var screen = (DoctrineScreen)client.gui.screen();
                            if (screen.preview() == null || !screen.preview().id().equals(candidate.id())) failures.add(scene + " lost parent preview");
                        });
                    }
                    context.runOnClient(client -> client.gui.screen().mouseScrolled(10, 160, 0, -100));
                    context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                    context.takeScreenshot(scene + "-last");
                    var last = context.computeOnClient(client -> client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                        .filter(w -> w.getWidth() == 74 && w.getHeight() == 55 && w.getX() >= 0 && w.getRight() <= client.gui.screen().width).toList());
                    if (last.isEmpty() || !last.getLast().getMessage().getString().equals(expected.getLast().name())) failures.add(scene + " last track inaccessible");
                    click(context, position(context, "선택 창 닫기"));
                    context.getInput().setCursorPos(size[0] - 2, 80); context.waitTicks(2);
                    context.takeScreenshot(scene + "-closed");
                    if (actions.stream().anyMatch(r -> r.kind() == DoctrinePayloads.Kind.SELECT_GRAND || r.kind() == DoctrinePayloads.Kind.SELECT_SUBDOCTRINE)) failures.add(scene + " preview spent XP");
                }
            }
        }
        if (!failures.isEmpty()) throw new AssertionError(String.join("; ", failures));
    }
    private static void dragPicker(ClientGameTestContext context, int left) {
        var close = position(context, "선택 창 닫기");
        context.runOnClient(client -> {
            var screen = client.gui.screen();
            double x = close[0] - 205, y = close[1];
            var info = new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0);
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(x, y, info), false);
            var moved = new net.minecraft.client.input.MouseButtonEvent(left + 60, y, info);
            screen.mouseDragged(moved, left + 60 - x, 0);
            screen.mouseReleased(moved);
        });
        var moved = position(context, "선택 창 닫기");
        context.runOnClient(client -> client.gui.screen().mouseDragged(new net.minecraft.client.input.MouseButtonEvent(1, 1,
            new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), -10, 0));
        if (!Arrays.equals(moved, position(context, "선택 창 닫기"))) throw new AssertionError("Released doctrine picker continued dragging");
        if (moved[0] <= close[0]) throw new AssertionError("Original moveable doctrine picker did not move");
    }
    private static void milestoneTooltipReview(ClientGameTestContext context, DoctrineView.Milestone milestone, String name, int width, List<String> failures) {
        var at = context.computeOnClient(client -> client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
            .filter(w -> w.getWidth() == 53 && w.getHeight() == 42 && w.getMessage().getString().equals(milestone.name()))
            .filter(w -> w.getX() >= 0 && w.getRight() <= client.gui.screen().width).toList());
        var target = at.isEmpty() ? null : name.endsWith("scroll-last") ? at.getLast() : at.getFirst();
        if (target == null) { failures.add(name + " " + width + " missing milestone hover"); return; }
        var position = new int[]{target.getX() + target.getWidth() / 2, target.getY() + target.getHeight() / 2};
        context.getInput().setCursorPos(position[0] * 2, position[1] * 2); context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (DoctrineScreen)client.gui.screen();
            if (!screen.hoveredDetail().equals(milestone.detail())) failures.add(name + " " + width + " changed original milestone tooltip");
            var bounds = screen.tooltipBounds();
            if (bounds == null || bounds.x() < 0 || bounds.y() < 0 || bounds.x() + bounds.width() > screen.width || bounds.y() + bounds.height() > screen.height) failures.add(name + " " + width + " tooltip outside screen");
            var right = new net.minecraft.client.input.MouseButtonEvent(position[0], position[1], new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT, 0));
            screen.mouseClicked(right, false);
        });
        click(context, position); click(context, position);
        context.takeScreenshot("hoi-doctrine-milestones-" + name + "-tooltip-" + width + "-gui2");
    }
    private static int[] trackPosition(ClientGameTestContext context, int index) {
        return context.computeOnClient(client -> {
            var button = client.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
                .filter(c -> c.getWidth() == 74 && c.getHeight() == 55).toList().get(index);
            return new int[]{button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2};
        });
    }
    private static void click(ClientGameTestContext context, int[] position) {
        context.runOnClient(client -> {
            var click = new net.minecraft.client.input.MouseButtonEvent(position[0], position[1],
                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
            client.gui.screen().mouseClicked(click, false);
        });
    }
    private static void branchSelection(ClientGameTestContext context, String folder, String trackId, String selectedId) throws Exception {
        var before = fixture("doctrine-" + folder + "-ready"); var after = fixture("doctrine-" + folder + "-selected");
        if (!before.folder().equals(folder) || !before.session().equals(after.session()) || !before.experience().equals("300") || !after.experience().equals("200"))
            throw new AssertionError("Server-issued branch XP300 to200 pair required");
        var track = before.tracks().stream().filter(t -> t.id().equals(trackId)).findFirst().orElseThrow();
        var choice = track.candidates().stream().filter(c -> c.id().equals(selectedId)).findFirst().orElseThrow();
        if (!choice.enabled() || !choice.cost().equals("100")) throw new AssertionError("Server-owned branch quote required");
        var sent = new ArrayList<DoctrinePayloads.Request>();
        context.setScreen(() -> new DoctrineScreen(null, folder, before.session(), before, sent::add));
        click(context, position(context, track.name())); context.waitTicks(2);
        var target = position(context, choice.name());
        context.getInput().setCursorPos(target[0] * 2, target[1] * 2); context.waitTicks(3);
        context.takeScreenshot("hoi-doctrine-" + folder + "-candidate-gui2");
        click(context, target); click(context, target);
        context.runOnClient(client -> {
            var screen = (DoctrineScreen)client.gui.screen();
            if (!sent.isEmpty() || screen.preview() == null || !screen.preview().id().equals(selectedId)
                    || !screen.displayedTrack(track).rewards().equals(choice.rewards()))
                throw new AssertionError("Candidate inspection must preview the source rewards without spending XP");
        });
        click(context, position(context, "잠금 해제")); click(context, position(context, "잠금 해제"));
        context.runOnClient(client -> {
            if (sent.size() != 1 || !sent.getFirst().equals(new DoctrinePayloads.Request(DoctrinePayloads.Kind.SELECT_SUBDOCTRINE,
                    before.session(), before.revision(), folder, trackId, selectedId))) throw new AssertionError("Branch double click must issue exactly one selection");
            ((DoctrineScreen)client.gui.screen()).update(before);
        });
        click(context, target);
        context.runOnClient(client -> {
            if (sent.size() != 1) throw new AssertionError("Stale branch response must not unlock selection");
            ((DoctrineScreen)client.gui.screen()).update(after);
        });
        context.getInput().setCursorPos(2450, 1300); context.waitTicks(3);
        context.takeScreenshot("hoi-doctrine-" + folder + "-selected-gui2");
        context.setScreen(() -> null);
    }
    private static void nativeProgress(ClientGameTestContext context) throws Exception {
        var progress=fixture("doctrine-land-progress");var track=progress.tracks().stream().filter(t->t.id().equals("support_units")).findFirst().orElseThrow();
        if(!DoctrineScreen.rewardThreshold(track,0).equals("150")||!DoctrineScreen.rewardThreshold(track,1).equals("300")
                ||DoctrineScreen.rewardProgress(track,0)!=1||DoctrineScreen.rewardProgress(track,1)!=.5f||DoctrineScreen.rewardProgress(track,2)!=0)
            throw new AssertionError("Cumulative source thresholds and partial authoritative mastery must share the native progress display");
        var actions=new ArrayList<DoctrinePayloads.Request>();
        context.setScreen(()->new DoctrineScreen(null,"land",progress.session(),progress,actions::add));
        context.runOnClient(client->net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterExtract(client.gui.screen()).register((screen,g,mx,my,delta)->{
            if(!g.containsPointInScissor(0,0)||!g.containsPointInScissor(screen.width-1,screen.height-1))
                throw new AssertionError("Partial mastery fill must restore the extractor scissor stack before tooltips and later UI");
        }));context.waitTicks(3);
        context.getInput().setCursorPos(2450,1300);context.waitTicks(2);context.takeScreenshot("hoi-doctrine-source-partial-mastery-gui2");
        var counter=position(context,track.name()+" mastery");context.getInput().setCursorPos(counter[0]*2,counter[1]*2);context.waitTicks(3);
        context.takeScreenshot("hoi-doctrine-source-mastery-tooltip-gui2");
        click(context,counter);context.runOnClient(client->{
            if(!actions.isEmpty())throw new AssertionError("Mastery counter inspection must not send a progression action");
            for(var child:client.gui.screen().children())if(child instanceof dev.hoi.client.ui.UiHoverRegion region)
                if(region.getY()<0||region.getBottom()>client.gui.screen().height)throw new AssertionError("Reward hover region must follow visible native bounds");
        });
        context.setScreen(()->null);
    }
    private static void confirmedGrandSwitch(ClientGameTestContext context) throws Exception {
        var before=fixture("doctrine-land-before-grand-switch");var after=fixture("doctrine-land-after-grand-switch");
        if(!before.grand().equals("nato_western_doctrine")||!after.grand().equals("csto_eastern_doctrine")
                ||!before.session().equals(after.session())||after.progressionRevision()!=before.progressionRevision()+1)
            throw new AssertionError("Current authoritative successful grand-switch pair required");
        for(String id:List.of("infantry","support_units")) {
            var prior=before.tracks().stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
            var reset=after.tracks().stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
            if(!reset.mastery().equals("0")||!prior.bank().equals(reset.bank())||!prior.selected().equals(reset.selected())
                    ||reset.rewards().stream().anyMatch(DoctrineView.Reward::unlocked))throw new AssertionError("Confirmed progress reset must preserve selected subdoctrine and bank in the private response");
            for(int i=0;i<reset.rewards().size();i++)if(DoctrineScreen.rewardProgress(reset,i)!=0)throw new AssertionError("Reset mastery must render an empty cumulative stage fill");
        }
        var actions=new ArrayList<DoctrinePayloads.Request>();context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->client.options.guiScale().set(2));context.setScreen(()->new DoctrineScreen(null,"land",before.session(),before,actions::add));
        context.getInput().setCursorPos(2450,1300);context.waitTicks(3);context.takeScreenshot("hoi-doctrine-before-grand-switch-gui2");
        context.runOnClient(client->{var screen=(DoctrineScreen)client.gui.screen();screen.update(after);screen.update(before);});
        context.waitTicks(3);context.takeScreenshot("hoi-doctrine-after-grand-switch-gui2");
        context.runOnClient(client->{if(!actions.isEmpty())throw new AssertionError("Authoritative grand-switch rendering cannot issue another selection or grant progress");});
        context.setScreen(()->null);
    }
    private static void centeredLayoutStress(ClientGameTestContext context) throws Exception {
        var source=fixture("doctrine-land-empty");var tracks=new ArrayList<DoctrineView.Track>();
        var amounts=List.of("0","8.12","999999.125");var uiText=new HashMap<>(source.uiText());
        for(int i=0;i<source.tracks().size();i++) {
            var track=source.tracks().get(i);
            tracks.add(new DoctrineView.Track(track.id(),track.name(),track.background(),track.icon(),track.decoration(),
                    track.selected(),track.nameplate(),track.mastery(),track.dailyMastery(),amounts.get(i%amounts.size()),track.rewards(),track.candidates()));
            uiText.put("mastery/"+i,"통달치: "+amounts.get(i%amounts.size()));
        }
        var stress=new DoctrineView(source.session(),source.revision(),source.country(),source.progressionRevision(),source.folder(),source.branch(),
                source.experience(),source.grand(),source.folders(),source.grands(),tracks,source.message(),uiText,source.previewTracks());
        var actions=new ArrayList<DoctrinePayloads.Request>();
        for(int[] size:new int[][]{{1280,720},{2560,1440}}) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            context.setScreen(()->new DoctrineScreen(null,stress.folder(),stress.session(),stress,actions::add));
            context.getInput().setCursorPos(size[0]-4,size[1]-4);context.waitTicks(3);
            context.runOnClient(client->{
                var screen=(DoctrineScreen)client.gui.screen();
                if(client.options.guiScale().get()!=2||screen.width!=size[0]/2||screen.height!=size[1]/2)
                    throw new AssertionError("Centered doctrine layout must retain actual GUI2 dimensions");
                if(!actions.isEmpty())throw new AssertionError("Synthetic layout stress cannot issue progression requests");
            });
            context.takeScreenshot("hoi-doctrine-centered-layout-stress-"+size[0]+"x"+size[1]+"-gui2");
            if(size[0]==1280) {
                var counter=position(context,tracks.get(2).name()+" mastery");
                context.getInput().setCursorPos(counter[0]*2,counter[1]*2);context.waitTicks(3);
                context.runOnClient(client->{
                    var screen=(DoctrineScreen)client.gui.screen();
                    if(screen.tooltipBounds()==null||!screen.hoveredDetail().equals("통달치: 999999.125"))
                        throw new AssertionError("Ellipsized mastery must retain the complete authoritative display string in its hovered tooltip");
                    if(!actions.isEmpty())throw new AssertionError("Reading a complete mastery amount cannot issue progression requests");
                });
                context.takeScreenshot("hoi-doctrine-centered-layout-stress-full-amount-tooltip-1280x720-gui2");
            }
            context.setScreen(()->null);
        }
    }
    public static void run(ClientGameTestContext context) {
        try {
            for (String selectedFolder : List.of("land", "naval", "air", "special_forces", "land", "air", "naval"))
                for (String tab : List.of("land", "naval", "air", "special_forces"))
                    if (DoctrineScreen.tabFrame(tab, selectedFolder) != (tab.equals(selectedFolder) ? 0 : 1))
                        throw new AssertionError("Selected doctrine tab must use the original front frame");
            int[][] originalRewardFrames = {{0, 1}, {2, 3}, {4, 5}, {6, 7}};
            for (int branch = 0; branch < originalRewardFrames.length; branch++)
                if (DoctrineScreen.rewardFrame(branch, true) != originalRewardFrames[branch][0]
                        || DoctrineScreen.rewardFrame(branch, false) != originalRewardFrames[branch][1])
                    throw new AssertionError("Original reward backgrounds interleave each branch unlocked and locked art");
            var empty = fixture("doctrine-land-empty"); var selected = fixture("doctrine-land-selected"); var air = fixture("doctrine-air-empty");
            if (!empty.session().equals(selected.session()) || !empty.experience().equals("300") || !selected.experience().equals("200"))
                throw new AssertionError("Original server-issued XP300→200 selection pair required");
            var infantry = empty.tracks().stream().filter(t -> t.id().equals("infantry")).findFirst().orElseThrow();
            var choice = infantry.candidates().stream().filter(c -> c.id().equals("motorized_infantry")).findFirst().orElseThrow();
            if (!choice.enabled() || !choice.detail().contains("속도: §G+5%§!")) throw new AssertionError("Source speed ratio must survive the server wire");
            var sent = new ArrayList<DoctrinePayloads.Request>();
            context.getInput().resizeWindow(2560, 1440); context.runOnClient(client -> client.options.guiScale().set(2));
            context.setScreen(() -> new DoctrineScreen(null, "land", empty.session(), empty, sent::add));
            context.runOnClient(client -> {
                for (String asset : List.of("gfx_track_frame/frame_0", "gfx_grand_doctrine_frame/frame_0", "gfx_subdoctrine_button_and_text_frame/frame_0"))
                    if (client.getResourceManager().getResource(net.minecraft.resources.Identifier.parse("hoi:textures/gui/doctrine/" + asset + ".png")).isEmpty())
                        throw new AssertionError("Missing original doctrine asset " + asset);
            });
            context.getInput().setCursorPos(2450, 1300); context.waitTicks(3); context.takeScreenshot("hoi-doctrine-land-empty-gui2");
            click(context, position(context, infantry.name())); context.waitTicks(2);
            var candidatePosition = position(context, choice.name());
            context.getInput().setCursorPos(candidatePosition[0] * 2, candidatePosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-infantry-candidate-tooltip-gui2");
            click(context, position(context, "고급 세부 사항"));
            context.runOnClient(client -> { if (!sent.isEmpty()) throw new AssertionError("Advanced details must not issue an XP selection"); });
            context.getInput().setCursorPos(candidatePosition[0] * 2, candidatePosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-infantry-advanced-tooltip-gui2");
            click(context, position(context, "고급 세부 사항"));
            click(context, candidatePosition); click(context, candidatePosition);
            context.runOnClient(client -> {
                var screen = (DoctrineScreen)client.gui.screen();
                if (!sent.isEmpty() || screen.preview() == null || !screen.preview().id().equals(choice.id())
                        || !screen.displayedTrack(infantry).rewards().equals(choice.rewards()) || !infantry.selected().isEmpty())
                    throw new AssertionError("Local candidate preview must use server rewards and preserve authoritative selection");
            });
            context.getInput().setCursorPos(2450,1300); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-preview-2560x1440-gui2");
            click(context, position(context, "고급 세부 사항"));
            var detailPosition = position(context, "고급 세부 사항");
            context.getInput().setCursorPos(detailPosition[0]*2,detailPosition[1]*2); context.waitTicks(3);
            context.runOnClient(client -> {
                if (!((DoctrineScreen)client.gui.screen()).hoveredDetail().equals(choice.advancedDetail()))
                    throw new AssertionError("The lower details button must expose the full selected candidate source tooltip");
            });
            context.takeScreenshot("hoi-doctrine-preview-details-2560x1440-gui2");
            context.getInput().resizeWindow(1280,720); context.waitTicks(3);
            context.getInput().setCursorPos(1270,710); context.waitTicks(2);
            context.takeScreenshot("hoi-doctrine-preview-1280x720-gui2");
            detailPosition = position(context, "고급 세부 사항");
            context.getInput().setCursorPos(detailPosition[0]*2,detailPosition[1]*2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-preview-details-1280x720-gui2");
            context.getInput().resizeWindow(2560,1440); context.waitTicks(2);
            click(context, position(context, "고급 세부 사항"));
            click(context, position(context, "잠금 해제")); click(context, position(context, "잠금 해제"));
            context.runOnClient(client -> {
                if (sent.size() != 1 || !sent.getFirst().equals(new DoctrinePayloads.Request(DoctrinePayloads.Kind.SELECT_SUBDOCTRINE,
                        empty.session(), empty.revision(), "land", "infantry", "motorized_infantry")))
                    throw new AssertionError("One click must issue exactly one authoritative subdoctrine selection");
                ((DoctrineScreen)client.gui.screen()).update(empty);
            });
            click(context, candidatePosition);
            context.runOnClient(client -> {
                if (sent.size() != 1) throw new AssertionError("Old response must not unlock the pending selection");
                ((DoctrineScreen)client.gui.screen()).update(selected);
            });
            context.getInput().setCursorPos(2450, 1300); context.waitTicks(3); context.takeScreenshot("hoi-doctrine-land-selected-gui2");
            var supportTrack = selected.tracks().stream().filter(t -> t.id().equals("support_units")).findFirst().orElseThrow();
            var engineering = supportTrack.candidates().stream().filter(c -> c.id().equals("complex_engineering")).findFirst().orElseThrow();
            if (!engineering.detail().contains("참호: §G+0.05§!") || !engineering.advancedDetail().contains("참호: §G+50.00%§!")
                    || !engineering.advancedDetail().contains("§G-1%§!"))
                throw new AssertionError("Native entrenchment point and percent displays must survive the server wire");
            click(context, position(context, supportTrack.name())); context.waitTicks(2);
            var engineeringPosition = position(context, engineering.name());
            context.getInput().setCursorPos(engineeringPosition[0] * 2, engineeringPosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-engineering-point-tooltip-gui2");
            click(context, position(context, "고급 세부 사항"));
            context.getInput().setCursorPos(engineeringPosition[0] * 2, engineeringPosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-engineering-percent-tooltip-gui2");
            context.runOnClient(client -> {
                var screen = (DoctrineScreen)client.gui.screen(); var bounds = screen.tooltipBounds();
                if (bounds == null || bounds.x() - 4 < 0 || bounds.y() - 4 < 0
                        || bounds.x() + bounds.width() + 4 > screen.width || bounds.y() + bounds.height() + 4 > screen.height)
                    throw new AssertionError("All doctrine tooltip edges must remain inside the viewport");
                if (bounds.scroll() != 0) throw new AssertionError("Advanced details must open at their first line");
            });
            click(context, position(context, "선택 창 닫기"));
            var longTrack = selected.tracks().stream().filter(t -> t.id().equals("combat_support")).findFirst().orElseThrow();
            var longChoice = longTrack.candidates().stream().filter(c -> c.id().equals("overwhelming_barrage")).findFirst().orElseThrow();
            click(context, position(context, longTrack.name()));
            context.getInput().resizeWindow(2560, 600); context.waitTicks(2);
            var compactPosition = position(context, longChoice.name());
            context.getInput().setCursorPos(compactPosition[0] * 2, compactPosition[1] * 2); context.waitTicks(2);
            context.takeScreenshot("hoi-doctrine-engineering-percent-tooltip-short-viewport-gui2");
            context.runOnClient(client -> {
                var screen = (DoctrineScreen)client.gui.screen(); var bounds = screen.tooltipBounds();
                if (bounds == null || bounds.maxScroll() <= 0 || bounds.y() - 4 < 0 || bounds.y() + bounds.height() + 4 > screen.height)
                    throw new AssertionError("Short viewports must retain bounded scrollable native rewards: " + bounds + " in " + screen.width + "x" + screen.height);
                screen.mouseScrolled(compactPosition[0], compactPosition[1], 0, -1000);
            });
            context.waitTicks(2); context.takeScreenshot("hoi-doctrine-engineering-percent-tooltip-bottom-gui2");
            context.runOnClient(client -> {
                var bounds = ((DoctrineScreen)client.gui.screen()).tooltipBounds();
                if (bounds.scroll() != bounds.maxScroll()) throw new AssertionError("Final reward must be reachable by scrolling");
            });
            context.getInput().resizeWindow(2560, 1440); context.waitTicks(2);
            click(context, position(context, "고급 세부 사항"));
            click(context, position(context, "선택 창 닫기"));
            var mechanizedTrack = selected.tracks().stream().filter(t -> t.id().equals("mechanized")).findFirst().orElseThrow();
            var ironWall = mechanizedTrack.candidates().stream().filter(c -> c.id().equals("iron_wall")).findFirst().orElseThrow();
            if (!ironWall.detail().contains("참호: §G+0.01§!")) throw new AssertionError("Iron-wall battalion addition must retain the native point display");
            click(context, position(context, mechanizedTrack.name())); context.waitTicks(2);
            var ironPosition = position(context, ironWall.name());
            context.getInput().setCursorPos(ironPosition[0] * 2, ironPosition[1] * 2); context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-iron-wall-point-tooltip-gui2");
            context.runOnClient(client -> {
                if (sent.stream().filter(r -> r.kind() == DoctrinePayloads.Kind.SELECT_SUBDOCTRINE).count() != 1
                        || sent.stream().anyMatch(r -> r.kind() != DoctrinePayloads.Kind.SELECT_SUBDOCTRINE && r.kind() != DoctrinePayloads.Kind.REFRESH))
                    throw new AssertionError("Inspecting and scrolling entrenchment rewards must not spend XP");
            });
            context.runOnClient(client -> ((DoctrineScreen)client.gui.screen()).update(air));
            context.waitTicks(3); context.takeScreenshot("hoi-doctrine-air-empty-gui2");
            var grandName = air.grands().stream().filter(DoctrineView.Choice::selected).findFirst().map(DoctrineView.Choice::name).orElse("대교리 선택");
            click(context, position(context, grandName)); context.waitTicks(3); context.takeScreenshot("hoi-doctrine-air-grand-candidates-gui2");
            var alternative=air.grands().stream().filter(c->!c.selected()).findFirst().orElseThrow();
            if(!alternative.detail().contains("대교리를 교체하면 §R모든§! £doctrine_mastery §H통달치§!§R가 초기화됩니다.§!"))throw new AssertionError("Native grand-switch warning must retain its wording, icon and colors");
            context.runOnClient(client -> {
                var icon = dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("£doctrine_mastery"), false).getString();
                if (icon.length() != 1 || icon.charAt(0) < 0xe000 || icon.charAt(0) > 0xf8ff) throw new AssertionError("Native mastery token must resolve to an actual resource-pack glyph");
            });
            var grandPosition=position(context,alternative.name());context.getInput().setCursorPos(grandPosition[0]*2,grandPosition[1]*2);context.waitTicks(3);
            context.takeScreenshot("hoi-doctrine-air-grand-switch-warning-gui2");
            click(context,position(context,"선택 창 닫기"));
            context.runOnClient(client->{if(sent.stream().anyMatch(r->r.kind()==DoctrinePayloads.Kind.SELECT_GRAND))throw new AssertionError("Inspecting and closing a grand-doctrine choice cannot spend XP");});
            context.setScreen(() -> null);
            branchSelection(context, "naval", "submarines", "wolfpacks");
            branchSelection(context, "air", "strike_aircraft", "advanced_recon");
            nativeProgress(context);
            confirmedGrandSwitch(context);
            centeredLayoutStress(context);
            sourceReview(context);
            grandReview(context);
            authoritativeMilestoneReview(context);
        } catch (Exception failure) { throw new AssertionError("Server doctrine screen fixture failed", failure); }
    }
    private static void sourceReview(ClientGameTestContext context) throws Exception {
        String directory=System.getenv("HOI_UI_REVIEW_PREVIEW");
        if(directory==null)return;
        for(String folder:List.of("land","naval","air","special_forces"))for(int[] size:new int[][]{{2560,1440},{1280,720}}) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            for(String state:List.of("ready","poor")) {
                String grandDirectory=System.getenv("HOI_DOCTRINE_GRAND_PREVIEW");
                var path=grandDirectory!=null?Path.of(grandDirectory,"doctrine-"+folder+"-"+(state.equals("ready")?"grand-selected":"nogrand-poor")+".json"):Path.of(directory,"doctrine-"+folder+"-"+state+".json");
                var view=new DoctrinePayloads.Response(Files.readString(path)).view();
                var sent=new ArrayList<DoctrinePayloads.Request>();
                context.setScreen(()->new DoctrineScreen(null,folder,view.session(),view,sent::add));
                var track=view.tracks().isEmpty()?null:view.tracks().getFirst();
                var choice=track==null?view.grands().getFirst():track.candidates().getFirst();
                String opener=track==null?view.grands().stream().filter(DoctrineView.Choice::selected).map(DoctrineView.Choice::name).findFirst().orElse("대교리 선택"):track.name();
                click(context,track==null?position(context,opener):trackPosition(context,0));context.waitTicks(2);
                context.getInput().setCursorPos(size[0]-2,80);context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-review-"+folder+"-"+state+"-choices-"+size[0]+"-gui2");
                click(context,position(context,choice.name()));context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-review-"+folder+"-"+state+"-preview-"+size[0]+"-gui2");
                context.runOnClient(client->{if(!sent.isEmpty()||!((DoctrineScreen)client.gui.screen()).preview().id().equals(choice.id()))throw new AssertionError("Inspecting any issued candidate is a local preview");});
                click(context,position(context,"고급 세부 사항"));
                var detail=position(context,"고급 세부 사항");context.getInput().setCursorPos(detail[0]*2,detail[1]*2);context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-review-"+folder+"-"+state+"-detail-"+size[0]+"-gui2");
                context.runOnClient(client->{
                    var screen=(DoctrineScreen)client.gui.screen();
                    if(!screen.hoveredDetail().equals(choice.advancedDetail()))throw new AssertionError("All branches preserve the exact server advanced-detail text");
                    var bounds=screen.tooltipBounds();
                    if(bounds!=null&&bounds.y()+bounds.height()+4>detail[1]-10)throw new AssertionError("Doctrine details must end above their button");
                    for(String token:List.of("unit_artillery_icon_small","unit_recce_icon_small","unit_category_all_infantry","trigger_no")) {
                        String glyph=TooltipKeywordIcons.decorateLines(Component.literal("£"+token),false).getString();
                        if(glyph.length()!=1||glyph.charAt(0)<0xe000||glyph.charAt(0)>0xf8ff)throw new AssertionError("Original doctrine glyph missing: "+token);
                    }
                });
                if(!choice.enabled()) {
                    var unlock=position(context,"잠금 해제");context.getInput().setCursorPos(unlock[0]*2,unlock[1]*2);context.waitTicks(2);
                    context.takeScreenshot("hoi-doctrine-review-"+folder+"-"+state+"-unlock-"+size[0]+"-gui2");
                    click(context,unlock);click(context,unlock);
                    context.runOnClient(client->{if(!sent.isEmpty()||!((DoctrineScreen)client.gui.screen()).hoveredDetail().equals(choice.assignDetail()))throw new AssertionError("Unavailable unlock must preserve issued explanation and block repeated mutations");});
                }
                if(track!=null&&track.candidates().size()>10) {
                    context.runOnClient(client->{
                        var screen=(DoctrineScreen)client.gui.screen();
                        var close=screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(w->w.getMessage().getString().equals("선택 창 닫기")).findFirst().orElseThrow();
                        int x=close.getX()-257,y=close.getY()-6;
                        var start=new net.minecraft.client.input.MouseButtonEvent(x+272,y+120,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        var end=new net.minecraft.client.input.MouseButtonEvent(x+272,y+248,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        boolean pressed=screen.mouseClicked(start,false),dragged=screen.mouseDragged(end,0,128);screen.mouseReleased(end);
                        var names=screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).map(w->w.getMessage().getString()).toList();
                        if(!pressed||!dragged||!names.contains(track.candidates().getLast().name()))throw new AssertionError("Scrollbar drag at "+x+","+y+" pressed="+pressed+" dragged="+dragged+" widgets="+names);
                    });
                    context.takeScreenshot("hoi-doctrine-review-scroll-drag-"+size[0]+"-gui2");
                    var last=track.candidates().getLast();click(context,position(context,last.name()));context.getInput().setCursorPos(size[0]-2,80);context.waitTicks(2);
                    context.takeScreenshot("hoi-doctrine-review-"+folder+"-"+state+"-scroll-last-"+size[0]+"-gui2");
                    context.runOnClient(client->{if(!sent.isEmpty()||!((DoctrineScreen)client.gui.screen()).preview().id().equals(last.id()))throw new AssertionError("Dragging the native scrollbar must reach the final candidate without spending XP");});
                }
            }
        }
        context.setScreen(()->null);
    }
    private static void grandReview(ClientGameTestContext context) throws Exception {
        String directory=System.getenv("HOI_DOCTRINE_GRAND_PREVIEW");if(directory==null)return;
        for(String folder:List.of("land","naval","air","special_forces"))for(int[] size:new int[][]{{2560,1440},{1280,720}}) {
            context.getInput().resizeWindow(size[0],size[1]);context.runOnClient(client->client.options.guiScale().set(2));
            var selected=new DoctrinePayloads.Response(Files.readString(Path.of(directory,"doctrine-"+folder+"-grand-selected.json"))).view();
            var sub=new DoctrinePayloads.Response(Files.readString(Path.of(directory,"doctrine-"+folder+"-sub-selected.json"))).view();
            for(String state:List.of("poor","ready")) {
                var view=new DoctrinePayloads.Response(Files.readString(Path.of(directory,"doctrine-"+folder+"-nogrand-"+state+".json"))).view();
                if(!view.grand().isEmpty()||!view.tracks().isEmpty()||view.uiText().getOrDefault("NO_GRAND_DOCTRINE_ASSIGNED","").isEmpty()||view.uiText().getOrDefault("GRAND_DOCTRINE_BENEFIT_DESCRIPTION","").isEmpty())throw new AssertionError("Original unassigned grand-doctrine text and empty tracks must be server-issued");
                var sent=new ArrayList<DoctrinePayloads.Request>();context.setScreen(()->new DoctrineScreen(null,folder,view.session(),view,sent::add));
                context.getInput().setCursorPos(size[0]-2,80);context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-grand-"+folder+"-"+state+"-empty-"+size[0]+"-gui2");
                click(context,position(context,"대교리 선택"));context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-grand-"+folder+"-"+state+"-choices-"+size[0]+"-gui2");
                var choice=view.grands().stream().filter(c->c.id().equals(selected.grand())).findFirst().orElseThrow();
                click(context,position(context,choice.name()));context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-grand-"+folder+"-"+state+"-preview-"+size[0]+"-gui2");
                if(!sent.isEmpty())throw new AssertionError("Grand-doctrine preview must not spend XP");
                click(context,position(context,"잠금 해제"));click(context,position(context,"잠금 해제"));
                if(state.equals("poor")){if(!sent.isEmpty())throw new AssertionError("Insufficient grand-doctrine XP must block mutations");continue;}
                if(sent.size()!=1||sent.getFirst().kind()!=DoctrinePayloads.Kind.SELECT_GRAND||!sent.getFirst().item().equals(choice.id())||sent.getFirst().revision()!=view.revision())throw new AssertionError("Grand unlock must issue one authenticated choice");
                context.runOnClient(client->((DoctrineScreen)client.gui.screen()).update(selected));context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-grand-"+folder+"-selected-"+size[0]+"-gui2");
                var selectedTrack=sub.tracks().stream().filter(t->!t.selected().isEmpty()).findFirst().orElseThrow();
                var sourceTrack=selected.tracks().stream().filter(t->t.id().equals(selectedTrack.id())).findFirst().orElseThrow();
                var subChoice=sourceTrack.candidates().stream().filter(c->c.id().equals(selectedTrack.selected())).findFirst().orElseThrow();
                click(context,trackPosition(context,selected.tracks().indexOf(sourceTrack)));click(context,position(context,subChoice.name()));
                click(context,position(context,"잠금 해제"));
                if(sent.size()!=2||sent.getLast().kind()!=DoctrinePayloads.Kind.SELECT_SUBDOCTRINE||!sent.getLast().item().equals(subChoice.id()))throw new AssertionError("Issued subdoctrine follows the confirmed grand doctrine");
                context.runOnClient(client->((DoctrineScreen)client.gui.screen()).update(sub));context.waitTicks(2);
                context.takeScreenshot("hoi-doctrine-grand-"+folder+"-sub-selected-"+size[0]+"-gui2");
            }
        }
        context.setScreen(()->null);
    }
}
