package dev.hoi.client.screen.industry;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.hoi.client.research.ResearchScreen;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import dev.hoi.protocol.research.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;

public final class IndustryOrganizationAssignmentTest {
    private static final Gson JSON = new Gson();
    private IndustryOrganizationAssignmentTest() {}
    private static <T> T source(String name, Class<T> type) throws java.io.IOException {
        return JSON.fromJson(Files.readString(Path.of(System.getenv("HOI_COMMANDER_PREVIEW"), name + ".json")), type);
    }
    private static AbstractWidget named(Screen screen, String name) {
        return screen.children().stream().filter(w -> w instanceof AbstractWidget b && b.getMessage().getString().equals(name)).map(w -> (AbstractWidget)w).findFirst().orElseThrow(() -> new AssertionError("Missing " + name));
    }
    private static void click(Screen screen, AbstractWidget widget, int button, int mods) {
        var event = new MouseButtonEvent(widget.getX() + widget.getWidth() / 2., widget.getY() + widget.getHeight() / 2., new MouseButtonInfo(button, mods));
        screen.mouseClicked(event, false); screen.mouseReleased(event);
    }
    private static void click(Screen screen, String name) { click(screen, named(screen, name), InputConstants.MOUSE_BUTTON_LEFT, 0); }
    private static void field(Object screen, String name, Object value) {
        try { var f = screen.getClass().getDeclaredField(name); f.setAccessible(true); f.set(screen, value); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static int rowHeight(IndustryScreen screen) {
        try { var method = IndustryScreen.class.getDeclaredMethod("productionRowHeight"); method.setAccessible(true); return (int)method.invoke(screen); }
        catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static ResearchView revision(ResearchView source, long revision) {
        var json = JSON.toJsonTree(source).getAsJsonObject(); json.addProperty("revision", revision); return JSON.fromJson(json, ResearchView.class);
    }
    public static void run(ClientGameTestContext game) {
        int scale = game.computeOnClient(c -> c.options.guiScale().get());
        try {
            var before = source("kor-production-mio-unassigned", IndustryView.class);
            var after = source("kor-production-mio-assigned", IndustryView.class);
            var line = after.lines().stream().filter(l -> l.manufacturer() != null).findFirst().orElseThrow();
            var research = source("kor-research-design-team-before", ResearchView.class);
            var researched = source("kor-research-design-team-after", ResearchView.class);
            var assignedSlot = researched.slots().stream().filter(s -> !s.designTeam().isEmpty()).findFirst().orElseThrow();
            var team = researched.designTeams().stream().filter(t -> t.id().equals(assignedSlot.designTeam())).findFirst().orElseThrow();
            for (int[] size : List.of(new int[]{2560, 1440}, new int[]{1280, 720})) {
                game.getInput().resizeWindow(size[0], size[1]); game.runOnClient(c -> c.options.guiScale().set(2));
                var sent = new ArrayList<IndustryPayloads.Request>();
                var industry = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, before.session(), before, sent::add)); game.setScreen(() -> industry);
                game.runOnClient(c -> {
                    int index = before.lines().indexOf(before.lines().stream().filter(l -> l.id().equals(line.id())).findFirst().orElseThrow());
                    field(industry, "scroll", index * rowHeight(industry));
                    industry.init(industry.width, industry.height);
                });
                game.waitTicks(2); game.takeScreenshot("hoi-production-mio-unassigned-" + size[0] + "-gui2");
                game.runOnClient(c -> {
                    var empty = named(industry, "군수산업체 · " + line.id());
                    click(industry, empty, InputConstants.MOUSE_BUTTON_RIGHT, 0);
                    industry.mouseClicked(new MouseButtonEvent(15, empty.getY() + empty.getHeight() + 6, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0)), false);
                    if (!sent.isEmpty()) throw new AssertionError("Unassigned logo and production row must not send removal requests");
                });
                game.runOnClient(c -> click(industry, "군수산업체 · " + line.id())); game.waitTicks(2);
                game.takeScreenshot("hoi-production-mio-picker-" + size[0] + "-gui2");
                game.runOnClient(c -> {
                    click(industry, "이름 정렬"); click(industry, "이름 정렬"); click(industry, "규모 정렬"); click(industry, "규모 정렬");
                    click(industry, line.manufacturer().name()); click(industry, "군수산업체 · " + line.id());
                    var changes = sent.stream().filter(r -> r.action() == IndustryPayloads.Action.MANUFACTURER).toList();
                    if (changes.size() != 1 || !changes.getFirst().item().equals(line.id()) || !changes.getFirst().other().equals(line.manufacturer().id()) || !changes.getFirst().session().equals(before.session()) || changes.getFirst().revision() != before.revision()) throw new AssertionError("One authenticated production assignment required: " + changes);
                });
                var assigned = game.computeOnClient(c -> new IndustryScreen(MenuTab.PRODUCTION, after.session(), after, sent::add)); game.setScreen(() -> assigned);
                game.runOnClient(c -> {
                    int index = after.lines().indexOf(line); field(assigned, "scroll", index * rowHeight(assigned)); assigned.init(assigned.width, assigned.height);
                });
                var logo = game.computeOnClient(c -> named(assigned, "군수산업체 · " + line.id())); game.getInput().setCursorPos((logo.getX() + 4) * 2, (logo.getY() + 4) * 2); game.waitTicks(3);
                game.takeScreenshot("hoi-production-mio-assigned-tooltip-" + size[0] + "-gui2");
                game.runOnClient(c -> {
                    sent.clear();
                    assigned.mouseClicked(new MouseButtonEvent(15, logo.getY() + logo.getHeight() + 6, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0)), false);
                    click(assigned, logo, InputConstants.MOUSE_BUTTON_RIGHT, 0);
                    if (sent.size() != 1 || sent.getFirst().action() != IndustryPayloads.Action.MANUFACTURER || !sent.getFirst().other().isEmpty()
                            || !sent.getFirst().item().equals(line.id()) || !sent.getFirst().session().equals(after.session()) || sent.getFirst().revision() != after.revision()) throw new AssertionError("Production row removal must send one authenticated request and block repeated logo clicks");
                    var response = JSON.toJsonTree(before).getAsJsonObject();
                    response.addProperty("session", after.session()); response.addProperty("revision", after.revision() + 1);
                    assigned.update(JSON.fromJson(response, IndustryView.class));
                    click(assigned, named(assigned, "군수산업체 · " + line.id()), InputConstants.MOUSE_BUTTON_RIGHT, 0);
                    if (sent.size() != 1) throw new AssertionError("Acknowledged unassignment must not send another removal");
                });
                game.getInput().setCursorPos(size[0] - 5, 80); game.waitTicks(2);
                game.takeScreenshot("hoi-production-mio-row-unassigned-after-response-" + size[0] + "-gui2");
                var requests = new ArrayList<ResearchPayloads.Request>();
                var screen = game.computeOnClient(c -> new ResearchScreen(research, requests::add)); game.setScreen(() -> screen);
                game.runOnClient(c -> { screen.showDetail(assignedSlot.technology()); click(screen, dev.hoi.client.ui.UiTooltips.officerText(research.designTeamText().get("header")).getString()); });
                var row = game.computeOnClient(c -> named(screen, team.name())); game.getInput().setCursorPos((row.getX() + 100) * 2, (row.getY() + 10) * 2); game.waitTicks(4);
                game.takeScreenshot("hoi-research-design-team-picker-" + size[0] + "-gui2");
                game.runOnClient(c -> {
                    click(screen, team.name());
                    if (requests.size() != 1 || requests.getFirst().action() != ResearchPayloads.Action.DESIGN_TEAM || !requests.getFirst().technology().equals(team.id()) || requests.getFirst().slot() != assignedSlot.index() || requests.getFirst().revision() != research.revision()) throw new AssertionError("Active research must assign using current session/revision: " + requests);
                    screen.update(revision(researched, research.revision() + 1));
                });
                game.getInput().setCursorPos(400, 80); game.waitTicks(3); game.takeScreenshot("hoi-research-design-team-assigned-" + size[0] + "-gui2");
                var teamLogo = game.computeOnClient(c -> named(screen, team.name())); game.getInput().setCursorPos((teamLogo.getX() + 3) * 2, (teamLogo.getY() + 3) * 2); game.waitTicks(4); game.takeScreenshot("hoi-research-design-team-slot-tooltip-" + size[0] + "-gui2");
                var availableJson = JSON.toJsonTree(research).getAsJsonObject();
                availableJson.getAsJsonArray("slots").get(assignedSlot.index()).getAsJsonObject().addProperty("technology", "");
                for (var element : availableJson.getAsJsonArray("technologies")) if (element.getAsJsonObject().get("id").getAsString().equals(assignedSlot.technology())) element.getAsJsonObject().addProperty("status", "AVAILABLE");
                var available = JSON.fromJson(availableJson, ResearchView.class);
                var sequence = new ArrayList<ResearchPayloads.Request>(); var start = game.computeOnClient(c -> new ResearchScreen(available, sequence::add)); game.setScreen(() -> start);
                game.runOnClient(c -> { start.showDetail(assignedSlot.technology()); click(start, "연구"); }); game.waitTicks(2); game.takeScreenshot("hoi-research-design-team-start-picker-synthetic-" + size[0] + "-gui2");
                game.runOnClient(c -> {
                    click(start, team.name());
                    if (sequence.size() != 1 || sequence.getFirst().action() != ResearchPayloads.Action.START) throw new AssertionError("START must wait for a verified response before DESIGN_TEAM");
                    start.update(research);
                    if (sequence.size() != 1) throw new AssertionError("Stale response must not assign");
                    start.update(revision(research, research.revision() + 1));
                    if (sequence.size() != 2 || sequence.get(1).action() != ResearchPayloads.Action.DESIGN_TEAM || sequence.get(1).revision() != research.revision() + 1) throw new AssertionError("Assignment must use the acknowledged START revision");
                    start.update(revision(research, research.revision() + 1));
                    if (sequence.size() != 2) throw new AssertionError("Duplicate response must not repeat assignment");
                });
                var noTeamRequests=new ArrayList<ResearchPayloads.Request>();
                var without=game.computeOnClient(c->new ResearchScreen(available,noTeamRequests::add));game.setScreen(()->without);game.runOnClient(c->without.showDetail(assignedSlot.technology()));
                game.getInput().holdKey(InputConstants.KEY_LSHIFT);
                try {game.runOnClient(c->click(without,"연구"));} finally {game.getInput().releaseKey(InputConstants.KEY_LSHIFT);}
                game.runOnClient(c->{if(noTeamRequests.size()!=1||noTeamRequests.getFirst().action()!=ResearchPayloads.Action.START)throw new AssertionError("Shift must start without a design team");without.update(revision(research,research.revision()+1));if(noTeamRequests.size()!=1)throw new AssertionError("Shift START must not queue DESIGN_TEAM");});
                var rejectedRequests=new ArrayList<ResearchPayloads.Request>();var rejected=game.computeOnClient(c->new ResearchScreen(available,rejectedRequests::add));game.setScreen(()->rejected);
                game.runOnClient(c->{rejected.showDetail(assignedSlot.technology());click(rejected,"연구");click(rejected,team.name());rejected.update(revision(available,available.revision()+1));if(rejectedRequests.size()!=1)throw new AssertionError("Rejected START must not assign a design team");});
                var detachRequests=new ArrayList<ResearchPayloads.Request>();var detach=game.computeOnClient(c->new ResearchScreen(researched,detachRequests::add));game.setScreen(()->detach);
                game.runOnClient(c->{detach.showDetail(assignedSlot.technology());click(detach,dev.hoi.client.ui.UiTooltips.officerText(research.designTeamText().get("header")).getString());click(detach,"이름");click(detach,"비용");click(detach,dev.hoi.client.ui.UiTooltips.officerText(researched.designTeamText().get("empty")).getString());if(detachRequests.size()!=1||detachRequests.getFirst().action()!=ResearchPayloads.Action.DESIGN_TEAM||!detachRequests.getFirst().technology().isEmpty())throw new AssertionError("Empty row must detach without restarting research");});
            }
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        finally { game.setScreen(() -> null); game.runOnClient(c -> c.options.guiScale().set(scale)); }
    }
}
