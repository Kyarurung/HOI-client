package dev.hoi.client.screen;

import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.protocol.ProtocolFocusPayloads;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class ScreenFocusTest {
    private ScreenFocusTest() {}
    public static void run(ClientGameTestContext context) {
        var source = source();
        var pixelFailures = new ArrayList<String>();
        for (int[] size : List.of(new int[]{1280, 720}, new int[]{2560, 1440})) {
            context.getInput().resizeWindow(size[0], size[1]);
            context.waitTicks(2);
            context.runOnClient(client -> check(client.getWindow().getGuiScale() == 2, "Focus input checks require the existing GUI2 test setup"));
            cancelSelectedFocus(context, source);
            repeatedStartAndSameRevision(context, source);
            selectedNodeRemoval(context, source);
            navigationAndGraphUpdates(context, source);
            staleResponses(context, source);
            exactActionAcknowledgement(context, source);
            timeoutRecoveryAcknowledgement(context, source);
            lateDetails(context, source);
            coalescedDetailQueries(context, source);
            detailDragging(context, source);
            exactDetailText(context, source, size[0]);
            progressStates(context, source, size[0]);
            stackedMenu(context, source, size[0]);
            pixelFailures.addAll(ScreenFocusPixelsTest.run(context, size[0]));
        }
        context.setScreen(() -> null);
        context.getInput().resizeWindow(1600, 1000);
        context.waitTicks(2);
        check(pixelFailures.isEmpty(), "Focus GPU pixel comparison failed: " + String.join("; ", pixelFailures));
    }
    private static void cancelSelectedFocus(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var original=source.nodes().getFirst();
        var running=new ProtocolFocusPayloads.Node(original.id(),original.name(),original.description(),original.icon(),0,0,List.of(),List.of(),"진행 중",24,false);
        var initial=view(source,source.tree(),7,List.of(running),true);
        var requests=new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(()->new ScreenFocus(initial,requests::add));
        context.runOnClient(client->{
            var screen=(ScreenFocus)client.gui.screen();
            check(screen.children().stream().noneMatch(child->child instanceof Button button && button.getMessage().getString().equals("중점 취소")),"No bottom cancel button may exist without a selected focus");
            clickNode(screen,running.id());
            var cancel=button(screen,"중점 취소");
            check(cancel.active,"The running focus must expose cancellation in its detail action slot");
            float x=screen.panX(),y=screen.panY();
            clickButton(screen,cancel);
            clickButton(screen,button(screen,"중점 취소"));
            check(count(requests,ProtocolFocusPayloads.Action.CANCEL)==1,"Repeated cancellation must submit once");
            var request=requests.stream().filter(r->r.action()==ProtocolFocusPayloads.Action.CANCEL).findFirst().orElseThrow();
            check(request.token().equals(screen.token()) && request.session().equals(initial.session()) && request.revision()==initial.revision() && request.id().equals(running.id()),"Cancel must carry the selected running focus and authoritative context");
            check(!button(screen,"중점 취소").active && screen.selectedId().equals(running.id()),"Pending cancellation must retain the detail and disable its action");
            check(screen.panX()==x && screen.panY()==y,"Cancellation must not pan the graph");
        });
    }
    private static void stackedMenu(ClientGameTestContext context, ProtocolFocusPayloads.View source, int pixelWidth) {
        var previous=context.computeOnClient(client->dev.hoi.client.ClientCampaignHud.visible()?dev.hoi.protocol.ProtocolHudPayloads.State.of(dev.hoi.client.ClientCampaignHud.country(),dev.hoi.client.ClientCampaignHud.hud()):dev.hoi.protocol.ProtocolHudPayloads.State.HIDDEN);
        try {
            context.runOnClient(client->dev.hoi.client.ClientCampaignHud.accept(dev.hoi.protocol.ProtocolHudPayloads.State.of(source.country(),source.hud())));
            context.setScreen(()->new ScreenFocus(source,request->{}));
            context.getInput().setCursorPos(0,200);context.waitTicks(2);
            var baseline=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-focus-menu-direct-"+pixelWidth+"-gui2").toFile());
            context.setScreen(()->new ScreenDialogStack(new ScreenFocus(source,request->{})));
            context.getInput().setCursorPos(0,200);context.waitTicks(2);
            var stacked=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-focus-menu-stacked-"+pixelWidth+"-gui2").toFile());
            int menuHeight=context.computeOnClient(client->dev.hoi.client.ui.UiMenuBar.height(client.getWindow().getGuiScaledWidth())*2);
            int dockWidth=context.computeOnClient(client->dev.hoi.client.ui.UiMenuBar.dockWidth(client.getWindow().getGuiScaledWidth())*2);
            for(int y=0;y<menuHeight;y++)for(int x=0;x<(y<30?baseline.getWidth():dockWidth);x++)check(baseline.getRGB(x,y)==stacked.getRGB(x,y),"Stacked focus must preserve the menu pixel at "+x+","+y);
        } catch(IOException error) {throw new java.io.UncheckedIOException(error);}
        finally {context.setScreen(()->null);context.runOnClient(client->dev.hoi.client.ClientCampaignHud.accept(previous));}
    }
    private static void repeatedStartAndSameRevision(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var initial = view(source, source.tree(), 7, nodes(source, true), false);
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            String selected = initial.nodes().getFirst().id();
            clickNode(screen, selected);
            check(button(screen, "중점 시작").active, "Enabled legacy node must remain startable without optional detail data");
            clickButton(screen, button(screen, "중점 시작"));
            clickButton(screen, button(screen, "중점 시작"));
            check(count(requests, ProtocolFocusPayloads.Action.START) == 1, "Repeated start clicks must submit exactly one action");
            var request = requests.stream().filter(r -> r.action() == ProtocolFocusPayloads.Action.START).findFirst().orElseThrow();
            check(request.id().equals(selected) && request.token().equals(screen.token()) && request.session().equals(initial.session()) && request.revision() == initial.revision(), "Start must send the selected ID with the authoritative session and revision");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), initial));
            check(!button(screen, "중점 시작").active, "An equal-revision refresh must not acknowledge the pending start");
            clickButton(screen, button(screen, "중점 시작"));
            check(count(requests, ProtocolFocusPayloads.Action.START) == 1, "An equal-revision response must not permit a duplicate start");
        });
    }
    private static void selectedNodeRemoval(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var initial = view(source, source.tree(), 7, nodes(source, false), true);
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            clickNode(screen, initial.nodes().getFirst().id());
            var remaining = List.of(initial.nodes().getLast());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), view(initial, initial.tree(), 8, remaining, true)));
            check(screen.selectedId().isEmpty(), "Removing the selected node must clear the detail selection");
            check(screen.children().stream().noneMatch(child -> child instanceof Button button && button.getMessage().getString().equals("중점 시작")), "Removed node detail must no longer expose a start button");
            check(screen.children().stream().noneMatch(child -> child instanceof Button button && button.getMessage().getString().equals("중점 취소")), "Removed selection must not leave a cancel button");
        });
    }
    private static void navigationAndGraphUpdates(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var initial = view(source, source.tree(), 7, nodes(source, false), false);
        var replacement = view(source, source.tree() + "_replacement", 9, List.of(node(source.nodes().get(2), 8, 2, false)), false);
        float[] replacementNavigation = new float[3];
        context.setScreen(() -> new ScreenFocus(replacement, request -> {}));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            replacementNavigation[0] = screen.panX();
            replacementNavigation[1] = screen.panY();
            replacementNavigation[2] = screen.zoomLevel();
        });
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            for (int mouse : List.of(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOUSE_BUTTON_RIGHT, InputConstants.MOUSE_BUTTON_MIDDLE)) {
                float x = screen.panX(), y = screen.panY();
                var event = event(5, screen.height - 42, mouse);
                check(screen.mouseClicked(event, false), "Empty graph space must accept pan button " + mouse);
                check(screen.mouseDragged(event, 13, 7), "Pan drag must be consumed for mouse button " + mouse);
                screen.mouseReleased(event);
                check(screen.panX() == x + 13 && screen.panY() == y + 7 && screen.selectedId().isEmpty(), "Dragging empty graph space must pan without selecting a node");
                screen.mouseDragged(event, 13, 7);
                check(screen.panX() == x + 13 && screen.panY() == y + 7, "Released pointer must stop panning");
            }
            screen.mouseScrolled(screen.width / 2.0, screen.height / 2.0, 0, -1);
            var search = screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
            search.setValue(initial.nodes().getFirst().name());
            String selected = initial.nodes().getFirst().id();
            clickNode(screen, selected);
            float x = screen.panX(), y = screen.panY(), zoom = screen.zoomLevel();
            var expanded = new ArrayList<>(initial.nodes());
            expanded.add(node(source.nodes().get(2), 9, 3, false));
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), view(initial, initial.tree(), 8, expanded, false)));
            check(screen.selectedId().equals(selected), "Appending nodes to the same tree must preserve selection");
            check(screen.panX() == x && screen.panY() == y && screen.zoomLevel() == zoom, "Appending nodes to the same tree must preserve pan and zoom");
            check(screen.searchText().equals(initial.nodes().getFirst().name()) && screen.children().contains(search), "Graph expansion must preserve the search text and widget");
            check(count(requests, ProtocolFocusPayloads.Action.START) == 0 && count(requests, ProtocolFocusPayloads.Action.CANCEL) == 0, "Navigation must not mutate focus state");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), replacement));
            check(screen.selectedId().isEmpty(), "Changing tree ID must clear the old selection");
            check(screen.panX() == replacementNavigation[0] && screen.panY() == replacementNavigation[1] && screen.zoomLevel() == replacementNavigation[2], "Replacement tree must use its initial navigation state");
            check(screen.searchText().equals(initial.nodes().getFirst().name()), "Tree replacement must preserve the existing search input policy");
        });
    }
    private static void staleResponses(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var initial = view(source, source.tree(), 7, nodes(source, false), true);
        context.setScreen(() -> new ScreenFocus(initial, request -> {}));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            String selected = initial.nodes().getFirst().id();
            clickNode(screen, selected);
            float x = screen.panX(), y = screen.panY(), zoom = screen.zoomLevel();
            var stale = view(initial, initial.tree() + "_stale", 6, List.of(), false);
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), stale));
            check(screen.selectedId().equals(selected) && screen.panX() == x && screen.panY() == y && screen.zoomLevel() == zoom, "An older same-session snapshot must not replace the graph or selection");
            String oldToken = UUID.randomUUID().toString();
            screen.update(ProtocolFocusPayloads.Response.of(oldToken, view(initial, initial.tree() + "_other", 8, List.of(), false)));
            screen.update(new ProtocolFocusPayloads.Response(oldToken, ""));
            check(client.gui.screen() == screen && screen.selectedId().equals(selected), "An unrelated token must neither replace nor revoke the current private screen");
            check(button(screen, "중점 취소").active, "Stale responses must not remove the authoritative cancel control");
            screen.update(new ProtocolFocusPayloads.Response(screen.token(), ""));
            check(client.gui.screen() == null, "The current private token must still honor revocation");
        });
    }
    private static void exactActionAcknowledgement(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var base = view(source, source.tree(), 7, nodes(source, true), false);
        var initial = extended(base, base.session(), base.revision(), null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            var selected = initial.nodes().getFirst();
            clickNode(screen, selected.id());
            var detail = detail(selected, selected.description(), List.of());
            var refresh = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), initial.revision(), selected.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, detail, refresh)));
            check(button(screen, "중점 시작").active, "A loaded enabled detail must allow start");
            clickButton(screen, button(screen, "중점 시작"));
            var request = requests.stream().filter(r -> r.action() == ProtocolFocusPayloads.Action.START).findFirst().orElseThrow();
            check(screen.pendingAction(), "Extended start must remain pending until its exact acknowledgement");
            for (long revision : List.of(7L, 8L)) {
                screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), revision, detail, refresh)));
                check(screen.pendingAction() && !button(screen, "중점 시작").active, "A refresh acknowledgement must not unlock a pending start at revision " + revision);
                clickButton(screen, button(screen, "중점 시작"));
            }
            var otherAction = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.CANCEL, request.session(), request.revision(), request.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 9, detail, otherAction)));
            check(screen.pendingAction(), "An acknowledgement for another action must not unlock start");
            var otherId = new ProtocolFocusPayloads.Ack(request.action(), request.session(), request.revision(), initial.nodes().getLast().id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 10, detail, otherId)));
            check(screen.pendingAction() && count(requests, ProtocolFocusPayloads.Action.START) == 1, "An acknowledgement for another focus must not permit a duplicate start");
            String nextSession = UUID.randomUUID().toString();
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, nextSession, 0, detail, ProtocolFocusPayloads.Ack.of(request))));
            check(!screen.pendingAction(), "Exact request acknowledgement must unlock even when the response starts a new session at revision zero");
            var oldNodes = view(initial, initial.tree() + "_retired", 11, List.of(), false);
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(oldNodes, initial.session(), 11, null, ProtocolFocusPayloads.Ack.of(request))));
            check(screen.selectedId().equals(selected.id()) && screen.detailId().equals(selected.id()), "An acknowledgement from the retired session must not replace the new session graph");
            var previousQuery = requests.stream().filter(r -> r.action() == ProtocolFocusPayloads.Action.REFRESH).findFirst().orElseThrow();
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(oldNodes, previousQuery.session(), 11, null, ProtocolFocusPayloads.Ack.of(previousQuery))));
            check(screen.selectedId().equals(selected.id()) && screen.detailId().equals(selected.id()), "A delayed detail-query refresh from the retired session must not replace the new session graph");
            clickButton(screen, button(screen, "중점 시작"));
            var nextRequest = requests.stream().filter(r -> r.action() == ProtocolFocusPayloads.Action.START).reduce((a, b) -> b).orElseThrow();
            check(count(requests, ProtocolFocusPayloads.Action.START) == 2 && nextRequest.session().equals(nextSession) && nextRequest.revision() == 0, "The next action must use the newly accepted session after a retired response");
        });
    }
    private static void timeoutRecoveryAcknowledgement(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var base = view(source, source.tree(), 7, nodes(source, false), true);
        var initial = extended(base, base.session(), 7, null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            clickNode(screen, initial.nodes().getFirst().id());
            requests.clear();
            clickButton(screen, button(screen, "중점 취소"));
            for (int tick = 0; tick < 119; tick++) {
                screen.tick();
            }
            check(screen.pendingAction() && !button(screen, "중점 취소").active && requests.size() == 1, "The pending action must stay locked before the 120-tick recovery interval");
            var earlyRefresh = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), 7, "");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, null, earlyRefresh)));
            check(screen.pendingAction(), "An ordinary refresh before the recovery query must not clear the action");
            screen.tick();
            check(requests.size() == 2 && requests.getLast().action() == ProtocolFocusPayloads.Action.REFRESH, "Timeout recovery must query state without resending the mutation");
            var recovery = requests.getLast();
            check(recovery.session().equals(initial.session()) && recovery.revision() == 7 && recovery.id().equals(initial.nodes().getFirst().id()), "Recovery must query the current authoritative context and selection");
            check(screen.pendingAction() && !button(screen, "중점 취소").active, "Sending recovery must leave the action locked until its response");
            clickButton(screen, button(screen, "중점 취소"));
            var wrongRevision = new ProtocolFocusPayloads.Ack(recovery.action(), recovery.session(), recovery.revision() - 1, recovery.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, null, wrongRevision)));
            check(screen.pendingAction(), "A recovery acknowledgement with another request revision must not unlock the action");
            var wrongId = new ProtocolFocusPayloads.Ack(recovery.action(), recovery.session(), recovery.revision(), "");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, null, wrongId)));
            check(screen.pendingAction(), "A recovery acknowledgement for another selection must not unlock the action");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, null, ProtocolFocusPayloads.Ack.of(recovery))));
            check(!screen.pendingAction() && button(screen, "중점 취소").active, "The exact recovery acknowledgement must restore actions from the authoritative snapshot");
            check(requests.size() == 2 && count(requests, ProtocolFocusPayloads.Action.CANCEL) == 1, "Recovery must never replay the cancelled mutation automatically");
        });
    }
    private static void lateDetails(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var base = view(source, source.tree(), 7, nodes(source, false), false);
        var initial = extended(base, base.session(), 7, null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        context.setScreen(() -> new ScreenFocus(initial, request -> {}));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            var a = initial.nodes().getFirst();
            var b = initial.nodes().getLast();
            var detailA = detail(a, a.description(), List.of());
            var detailB = detail(b, b.description(), List.of());
            var ackA = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), 7, a.id());
            var ackB = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), 7, b.id());
            clickNode(screen, a.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, detailA, ackA)));
            check(screen.detailId().equals(a.id()), "Selected detail A must accept its authoritative body");
            closeDetail(screen);
            check(screen.selectedId().isEmpty(), "Detail close must deselect the focus");
            clickNode(screen, b.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 8, detailA, ackA)));
            check(screen.selectedId().equals(b.id()) && !screen.detailId().equals(a.id()), "A late detail A must not replace the selected detail B");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 9, detailB, ackB)));
            check(screen.detailId().equals(b.id()), "The current detail B must remain deliverable after an unrelated detail");
            closeDetail(screen);
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 10, detailB, ackB)));
            check(screen.selectedId().isEmpty() && screen.detailId().isEmpty(), "A late detail must not reopen a closed selection");
            clickNode(screen, b.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 11, detailB, ackB)));
            var removed = view(initial, initial.tree(), 12, List.of(a), true);
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(removed, removed.session(), 12, null, ackB)));
            check(screen.selectedId().isEmpty() && screen.detailId().isEmpty(), "Removing the selected graph node must discard its cached detail");
            check(screen.children().stream().noneMatch(child -> child instanceof Button button && button.getMessage().getString().equals("중점 취소")), "Extended detail removal must remove its cancellation action");
        });
    }
    private static void coalescedDetailQueries(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var graph = List.of(node(source.nodes().get(0), 0, 0, false), node(source.nodes().get(1), 3, 0, false), node(source.nodes().get(2), 6, 0, false));
        var base = view(source, source.tree(), 7, graph, false);
        var initial = extended(base, base.session(), 7, null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            clickNode(screen, graph.get(0).id());
            check(requests.size() == 1 && requests.getFirst().action() == ProtocolFocusPayloads.Action.REFRESH && requests.getFirst().id().equals(graph.get(0).id()), "The first detail selection must send a read-only query immediately");
            var first = requests.getFirst();
            closeDetail(screen);
            clickNode(screen, graph.get(1).id());
            closeDetail(screen);
            clickNode(screen, graph.get(2).id());
            check(requests.size() == 1, "Rapid changes inside the request interval must not send every selection");
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 8, detail(graph.get(0), graph.get(0).description(), List.of()), ProtocolFocusPayloads.Ack.of(first))));
            check(screen.selectedId().equals(graph.get(2).id()) && screen.detailId().isEmpty(), "An early selection response must not complete the latest queued detail");
            for (int tick = 0; tick < 19; tick++) {
                screen.tick();
            }
            check(requests.size() == 1, "A queued detail query must respect the full twenty-tick interval");
            screen.tick();
            check(requests.size() == 2 && requests.getLast().action() == ProtocolFocusPayloads.Action.REFRESH && requests.getLast().id().equals(graph.get(2).id()) && requests.getLast().revision() == 8, "The interval boundary must send only the latest selected ID with the latest revision");
            closeDetail(screen);
            clickNode(screen, graph.get(1).id());
            check(requests.size() == 2, "A new selection inside the interval must defer its query");
            closeDetail(screen);
            for (int tick = 0; tick < 20; tick++) {
                screen.tick();
            }
            check(requests.size() == 2 && screen.selectedId().isEmpty(), "Closing a selection must discard its deferred detail query");
        });
    }
    private static void detailDragging(ClientGameTestContext context, ProtocolFocusPayloads.View source) {
        var base = view(source, source.tree(), 7, nodes(source, true), false);
        var initial = extended(base, base.session(), 7, null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            var selected = initial.nodes().getFirst();
            clickNode(screen, selected.id());
            var ack = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), 7, selected.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, detail(selected, selected.description(), List.of()), ack)));
            int[] before = screen.detailBounds();
            float panX = screen.panX(), panY = screen.panY();
            var header = event(before[0] + 20, before[1] + 10, InputConstants.MOUSE_BUTTON_LEFT);
            check(screen.mouseClicked(header, false) && screen.mouseDragged(header, -32, -16), "The detail header must accept an actual left-button drag");
            screen.mouseReleased(header);
            int[] moved = screen.detailBounds();
            check(moved[0] == before[0] - 32 && moved[1] == before[1] - 16, "Dragging the detail header must move the panel by the pointer delta");
            check(screen.panX() == panX && screen.panY() == panY, "Dragging the detail header must not move the focus graph");
            screen.mouseDragged(header, -32, -16);
            check(java.util.Arrays.equals(screen.detailBounds(), moved), "Releasing the header must end detail dragging");
            checkBounds(screen, screen.detailBounds(), "Moved focus detail");
            clickButton(screen, button(screen, "중점 시작"));
            check(count(requests, ProtocolFocusPayloads.Action.START) == 1 && requests.getLast().id().equals(selected.id()), "The moved start button must send the selected focus through its new coordinates");
            closeDetail(screen);
            check(screen.selectedId().isEmpty() && screen.pendingAction(), "The foreground detail close button must deselect without losing the submitted action after dragging over the toolbar");
        });
    }
    private static void exactDetailText(ClientGameTestContext context, ProtocolFocusPayloads.View source, int pixelWidth) {
        var base = view(source, source.tree(), 7, nodes(source, false), false);
        var initial = extended(base, base.session(), 7, null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        var node = initial.nodes().getFirst();
        String exact = "§G조건 문장 0.00%§!\n\n§Y두 번째 문장 +0.01§!";
        var requirement = new ProtocolFocusPayloads.DetailLine(exact, 1, ProtocolFocusPayloads.Assessment.UNMET, node.id(), true);
        String description = (node.description() + "\n\n").repeat(96);
        var detail = detail(node, description, List.of(requirement));
        int[] preservedScroll = new int[1];
        context.setScreen(() -> new ScreenFocus(initial, request -> {}));
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            clickNode(screen, node.id());
            var ack = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), 7, node.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(initial, initial.session(), 7, detail, ack)));
            check(screen.sourceDetailText().contains(exact), "An unmet requirement marker must preserve independent green text, blank lines and explicit precision");
            check(screen.sourceDetailText().contains(description), "Long source description must remain available without truncation");
            checkBounds(screen, screen.detailBounds(), "Focus detail");
            checkBounds(screen, screen.requirementBounds(), "Focus requirement area");
        });
        context.waitTicks(2);
        context.takeScreenshot("hoi-focus-exact-detail-" + pixelWidth + "-gui2");
        context.runOnClient(client -> {
            var screen = (ScreenFocus) client.gui.screen();
            int[] bounds = screen.detailBounds();
            int before = screen.detailScroll();
            screen.mouseScrolled(bounds[0] + bounds[2] / 2.0, bounds[1] + bounds[3] / 2.0, 0, -100);
            check(screen.detailScroll() > before, "Long detail body must scroll inside its panel");
            check(screen.sourceDetailText().contains(exact), "Scrolling must preserve the complete exact source text");
            preservedScroll[0] = screen.detailScroll();
            var graph = new ArrayList<>(initial.nodes());
            graph.add(node(source.nodes().get(2), 9, 3, false));
            var expanded = view(initial, initial.tree(), 8, graph, false);
            var ack = new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.REFRESH, initial.session(), 7, node.id());
            screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(expanded, expanded.session(), 8, detail, ack)));
            check(screen.detailScroll() == preservedScroll[0], "Appending nodes to the same tree must preserve the selected detail scroll");
        });
        context.waitTicks(2);
        context.runOnClient(client -> check(((ScreenFocus) client.gui.screen()).detailScroll() == preservedScroll[0], "Rendering a refreshed detail must keep its previous scroll within the unchanged content bounds"));
        context.takeScreenshot("hoi-focus-exact-detail-scrolled-" + pixelWidth + "-gui2");
    }
    private static void progressStates(ClientGameTestContext context, ProtocolFocusPayloads.View source, int pixelWidth) {
        var original = source.nodes().getFirst();
        var first = new ProtocolFocusPayloads.Node(original.id(), original.name(), original.description(), original.icon(), 0, 0, List.of(), List.of(), "진행 중", 24, false);
        var base = view(source, source.tree(), 7, List.of(first), true);
        var initial = extended(base, base.session(), 7, null, new ProtocolFocusPayloads.Ack(ProtocolFocusPayloads.Action.OPEN, "", 0, ""));
        var requests = new ArrayList<ProtocolFocusPayloads.Request>();
        context.setScreen(() -> new ScreenFocus(initial, requests::add));
        context.runOnClient(client -> clickNode((ScreenFocus) client.gui.screen(), original.id()));
        for (int elapsed : List.of(0, 12, 24)) {
            var node = new ProtocolFocusPayloads.Node(original.id(), original.name(), original.description(), original.icon(), 0, 0, List.of(), List.of(), elapsed < 24 ? "진행 중" : "완료", 24 - elapsed, false);
            var detail = new ProtocolFocusPayloads.Detail(original.id(), original.name(), original.description(), 24, elapsed, 0, 0, 0, "test-fixture/progress/" + original.id(), 1, List.of(), List.of(), true, "", true);
            var stage = view(initial, initial.tree(), 8 + elapsed / 12, List.of(node), elapsed < 24);
            context.runOnClient(client -> {
                var screen = (ScreenFocus) client.gui.screen();
                check(client.getWindow().getGuiScale() == 2, "Progress captures require the actual GUI scale to remain two");
                var query = requests.stream().filter(request -> request.action() == ProtocolFocusPayloads.Action.REFRESH).findFirst().orElseThrow();
                screen.update(ProtocolFocusPayloads.Response.of(screen.token(), extended(stage, stage.session(), stage.revision(), detail, ProtocolFocusPayloads.Ack.of(query))));
                check(screen.selectedId().equals(original.id()) && screen.detailId().equals(original.id()), "Progress updates must preserve the selected source focus");
                check(elapsed < 24 ? button(screen, "중점 취소").active : !button(screen, "중점 시작").active, "Running focus cancels in place; completed focus cannot start");
                boolean cancelPresent = screen.children().stream().anyMatch(child -> child instanceof Button button && button.getMessage().getString().equals("중점 취소"));
                check(cancelPresent == (elapsed < 24), "Only an incomplete progress snapshot may expose the cancel action");
                check(count(requests, ProtocolFocusPayloads.Action.START) == 0 && count(requests, ProtocolFocusPayloads.Action.CANCEL) == 0, "Rendering progress snapshots must not mutate focus state");
                checkBounds(screen, screen.detailBounds(), "Progress detail");
                observeProgressTooltip(client, screen, elapsed);
            });
            context.getInput().setCursorPos(0, 0);
            context.waitTicks(2);
            context.takeScreenshot("hoi-focus-progress-" + elapsed * 100 / 24 + "-" + pixelWidth + "-gui2");
        }
    }
    private static void observeProgressTooltip(net.minecraft.client.Minecraft client, ScreenFocus screen, int elapsed) {
        int[] detailBounds = screen.detailBounds();
        int mouseX = detailBounds[0] + 146, mouseY = detailBounds[1] + 33;
        var lines = new ArrayList<String>();
        var tooltipBounds = new ArrayList<int[]>();
        int[] scissorDepth = new int[1];
        var graphics = new net.minecraft.client.gui.GuiGraphicsExtractor(client, new net.minecraft.client.renderer.state.gui.GuiRenderState(), mouseX, mouseY) {
            @Override public void enableScissor(int left, int top, int right, int bottom) {
                super.enableScissor(left, top, right, bottom);
                scissorDepth[0]++;
            }
            @Override public void disableScissor() {
                check(scissorDepth[0] > 0, "Scissor calls must not underflow the observed nesting depth");
                super.disableScissor();
                scissorDepth[0]--;
            }
            @Override public void setTooltipForNextFrame(net.minecraft.client.gui.Font font, List<? extends net.minecraft.util.FormattedCharSequence> text, int x, int y) {
                for (var sequence : text) {
                    var line = new StringBuilder();
                    sequence.accept((index, style, codePoint) -> {
                        line.appendCodePoint(codePoint);
                        return true;
                    });
                    lines.add(line.toString());
                }
                super.setTooltipForNextFrame(font, text, x, y);
            }
            @Override public void tooltip(net.minecraft.client.gui.Font font, List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> components, int x, int y, net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner positioner, net.minecraft.resources.Identifier style, boolean focused) {
                check(scissorDepth[0] == 0, "The deferred progress tooltip must render outside all detail scissors");
                var observed = new ArrayList<>(components);
                var original = observed.getFirst();
                observed.set(0, new net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent() {
                    public int getWidth(net.minecraft.client.gui.Font f) { return original.getWidth(f); }
                    public int getHeight(net.minecraft.client.gui.Font f) { return original.getHeight(f); }
                    public void extractText(net.minecraft.client.gui.GuiGraphicsExtractor g, net.minecraft.client.gui.Font f, int px, int py) {
                        int width = components.stream().mapToInt(c -> c.getWidth(f)).max().orElse(0);
                        int height = components.stream().mapToInt(c -> c.getHeight(f)).sum() - (!focused || components.size() == 1 ? 2 : 0);
                        tooltipBounds.add(new int[]{px - 7, py - 7, width + 14, height + 14});
                        original.extractText(g, f, px, py);
                    }
                });
                super.tooltip(font, observed, x, y, positioner, style, focused);
            }
        };
        screen.extractRenderState(graphics, mouseX, mouseY, 0);
        check(scissorDepth[0] == 0, "Extracting the focus screen must balance all detail scissors");
        graphics.extractDeferredElements(mouseX, mouseY, 0);
        String text = String.join("\n", lines);
        check(text.contains("1일 · 24시간") && text.contains("경과: " + elapsed + "시간"), "The rendered tooltip must preserve the exact duration and elapsed hours for progress " + elapsed);
        check(tooltipBounds.size() == 1, "Hovering the progress strip must render exactly one deferred tooltip");
        int[] outer = tooltipBounds.getFirst();
        checkBounds(screen, outer, "Progress tooltip background");
        check(outer[0] < detailBounds[0] || outer[1] < detailBounds[1] || outer[0] + outer[2] > detailBounds[0] + detailBounds[2] || outer[1] + outer[3] > detailBounds[1] + detailBounds[3], "The progress tooltip must remain visible beyond the detail panel bounds");
    }
    private static ProtocolFocusPayloads.Detail detail(ProtocolFocusPayloads.Node node, String description, List<ProtocolFocusPayloads.DetailLine> requirements) {
        return new ProtocolFocusPayloads.Detail(node.id(), node.name(), description, 24, 0, 0, 0, 0, "test-fixture/" + node.id(), 1, requirements, List.of(), true, "", true);
    }
    private static ProtocolFocusPayloads.View extended(ProtocolFocusPayloads.View source, String session, long revision, ProtocolFocusPayloads.Detail detail, ProtocolFocusPayloads.Ack ack) {
        return new ProtocolFocusPayloads.View(source.country(), source.name(), source.tree(), session, revision, source.message(), source.hud(), source.nodes(), source.cancelable(), detail, ack);
    }
    private static void closeDetail(ScreenFocus screen) {
        int[] bounds = screen.detailBounds();
        check(bounds != null, "A selected detail must expose its close-button bounds");
        var close = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getMessage().getString().equals("×") && button.getX() >= bounds[0] && button.getX() + button.getWidth() <= bounds[0] + bounds[2] && button.getY() >= bounds[1] && button.getY() + button.getHeight() <= bounds[1] + bounds[3]).findFirst().orElseThrow(() -> new AssertionError("Missing detail close button"));
        clickButton(screen, close);
    }
    private static void checkBounds(ScreenFocus screen, int[] bounds, String label) {
        check(bounds != null && bounds.length == 4 && bounds[2] > 0 && bounds[3] > 0, label + " bounds must be nonempty");
        check(bounds[0] >= 0 && bounds[1] >= 0 && bounds[0] + bounds[2] <= screen.width && bounds[1] + bounds[3] <= screen.height, label + " must remain within the GUI2 viewport");
    }
    private static ProtocolFocusPayloads.View source() {
        try (var input = ScreenFocusTest.class.getResourceAsStream("/hoi/focus/KOR.json")) {
            if (input == null) {
                throw new AssertionError("Missing original focus fixture KOR");
            }
            return new Gson().fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8), ProtocolFocusPayloads.View.class);
        } catch (IOException error) {
            throw new AssertionError(error);
        }
    }
    private static List<ProtocolFocusPayloads.Node> nodes(ProtocolFocusPayloads.View source, boolean enabled) {
        return List.of(node(source.nodes().getFirst(), 0, 0, enabled), node(source.nodes().get(1), 3, 1, false));
    }
    private static ProtocolFocusPayloads.Node node(ProtocolFocusPayloads.Node source, double x, double y, boolean enabled) {
        return new ProtocolFocusPayloads.Node(source.id(), source.name(), source.description(), source.icon(), x, y, List.of(), List.of(), enabled ? "선택 가능" : source.status(), enabled ? 0 : source.remaining(), enabled);
    }
    private static ProtocolFocusPayloads.View view(ProtocolFocusPayloads.View source, String tree, long revision, List<ProtocolFocusPayloads.Node> nodes, boolean cancelable) {
        return new ProtocolFocusPayloads.View(source.country(), source.name(), tree, source.session(), revision, "", source.hud(), nodes, cancelable);
    }
    private static Button button(ScreenFocus screen, String label) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(button -> button.getMessage().getString().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing focus button " + label));
    }
    private static void clickButton(ScreenFocus screen, Button button) {
        var event = event(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0, InputConstants.MOUSE_BUTTON_LEFT);
        screen.mouseClicked(event, false);
        screen.mouseReleased(event);
    }
    private static void clickNode(ScreenFocus screen, String id) {
        int[] bounds = screen.nodeBounds(id);
        check(bounds != null && bounds.length == 4, "Node bounds must exist for " + id);
        double x = bounds[0] + bounds[2] / 2.0, y = bounds[1] + Math.min(12, bounds[3] / 2.0);
        check(x >= 0 && x < screen.width && y >= 0 && y < screen.height, "Selected focus node must be inside the test viewport");
        var event = event(x, y, InputConstants.MOUSE_BUTTON_LEFT);
        check(screen.mouseClicked(event, false), "Node coordinate click must be consumed");
        screen.mouseReleased(event);
        check(screen.selectedId().equals(id), "Node coordinate click must select the requested ID");
    }
    private static MouseButtonEvent event(double x, double y, int mouse) {
        return new MouseButtonEvent(x, y, new MouseButtonInfo(mouse, 0));
    }
    private static long count(List<ProtocolFocusPayloads.Request> requests, ProtocolFocusPayloads.Action action) {
        return requests.stream().filter(request -> request.action() == action).count();
    }
    private static void check(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
