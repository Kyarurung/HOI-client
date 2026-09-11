package dev.hoi.client.screen;

import dev.hoi.protocol.AgencyView.Item;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.resources.Identifier;

public final class AgencyScreenChecks {
    private static void press(AgencyScreen screen, String label) {
        var button = screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(label)).map(c -> (net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();
        screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() * .7, new net.minecraft.client.input.MouseButtonInfo(0, 0)), false);
    }

    public static void run(ClientGameTestContext context, List<Item> items) {
        var partial = items.stream().filter(i -> i.id().equals("upgrade:domestic_intelligence")).findFirst().orElseThrow();
        if (AgencyScreen.upgradeStages(partial) != 4 || AgencyScreen.upgradeLevel(partial) != 2
                || !AgencyScreen.upgradeIndicator(partial).equals("agency/researched/11"))
            throw new AssertionError("Two of four agency levels must use the original two-stripe indicator");
        var requests = new java.util.ArrayList<dev.hoi.protocol.AgencyProtocol.Request>();
        var view = new dev.hoi.protocol.AgencyView("agency-click", 1, "KOR", "국가정보원", "", items, "");
        context.setScreen(() -> new AgencyScreen(null, view.session(), view, requests::add));
        context.waitTicks(2);
        context.runOnClient(client -> {
            var screen = (AgencyScreen)client.gui.screen();
            var tab = screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals("정보공동체")).map(c -> (net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(tab.getX()+tab.getWidth()/2.0,tab.getY()+tab.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
            var button = screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().equals(partial.title())).map(c -> (net.minecraft.client.gui.components.Button)c).findFirst().orElseThrow();
            screen.mouseClicked(new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+47,new net.minecraft.client.input.MouseButtonInfo(0,0)),false);
            if (requests.size()!=1 || !requests.getFirst().item().equals(partial.id())) throw new AssertionError("Clicking the visible enabled upgrade label must send its issued request: " + requests);
        });
        for (boolean stillEnabled : new boolean[]{true, false}) {
            requests.clear();
            context.setScreen(() -> new AgencyScreen(null, view.session(), view, requests::add));
            context.runOnClient(client -> {
                var screen = (AgencyScreen)client.gui.screen();
                press(screen, "정보공동체");
                for (int i = 0; i < 60; i++) screen.tick();
                press(screen, partial.title()); press(screen, partial.title());
                if (requests.size() != 1 || requests.getFirst().kind() != dev.hoi.protocol.AgencyProtocol.Kind.REFRESH)
                    throw new AssertionError("Upgrade waits for the in-flight refresh revision");
                var updated = items.stream().map(i -> i.id().equals(partial.id()) ? new Item(i.id(), i.group(), i.title(), i.value(), i.detail(), i.texture(), i.button(), stillEnabled, i.progress(), i.parameters()) : i).toList();
                var next = new dev.hoi.protocol.AgencyView(view.session(), 2, view.country(), view.name(), "", updated, "");
                screen.update(next); screen.update(next);
                if (requests.size() != (stillEnabled ? 2 : 1)) throw new AssertionError("Queued click must run once only if still available");
                if (stillEnabled && (requests.getLast().revision() != 2 || !requests.getLast().item().equals(partial.id())))
                    throw new AssertionError("Queued action must use the refreshed issued choice");
            });
        }
        var searchItems = new java.util.ArrayList<>(items);
        searchItems.removeIf(i -> i.id().equals("recruit"));
        searchItems.add(new Item("recruit", "agents", "정보원 모집", "0/1", "", "agency/operative", "모집", true, -1,
                List.of(new dev.hoi.protocol.AgencyView.Parameter("대상 국가", List.of(new dev.hoi.protocol.AgencyView.Choice("KOR", "대한민국"), new dev.hoi.protocol.AgencyView.Choice("JAP", "일본"))))));
        var searchable = new dev.hoi.protocol.AgencyView(view.session(), 1, view.country(), view.name(), "", searchItems, "");
        context.setScreen(() -> new AgencyScreen(null, view.session(), searchable, requests::add));
        context.runOnClient(client -> {
            var screen = (AgencyScreen)client.gui.screen();
            press(screen, "정보원 모집"); press(screen, "대상 국가: 대한민국");
            var input = (net.minecraft.client.gui.components.EditBox)screen.children().stream().filter(c -> c instanceof net.minecraft.client.gui.components.EditBox).findFirst().orElseThrow();
            input.setValue("대한민국"); input.setCursorPosition(1); input.setHighlightPos(3);
            screen.update(new dev.hoi.protocol.AgencyView(view.session(), 2, view.country(), view.name(), "", searchItems, ""));
            if (!screen.children().contains(input) || screen.getFocused() != input || !input.isFocused()
                    || input.getCursorPosition() != 1 || !input.getHighlighted().equals("한민"))
                throw new AssertionError("Agency search retains its editor, focus and selection across refresh");
            input.insertText("국");
            if (!input.getValue().equals("대국국") || screen.getFocused() != input) throw new AssertionError("Search filtering must not reset typing");
        });
        context.runOnClient(client -> {
            for(String path:List.of("section","operatives","tab0","tab1","operation_row0","operation_row1","target","clipboard_top","operation_bottom"))
                if(client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/agency/ui/"+path+".png")).isEmpty())
                    throw new AssertionError("Missing original agency UI asset " + path);
            for (int i = 0; i < 14; i++)
                if (client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/agency/researched/" + i + ".png")).isEmpty())
                    throw new AssertionError("Missing original agency indicator frame " + i);
        });
    }
}
