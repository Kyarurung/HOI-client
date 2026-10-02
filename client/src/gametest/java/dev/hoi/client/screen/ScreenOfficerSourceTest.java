package dev.hoi.client.screen;

import dev.hoi.client.ui.UiTooltips;
import dev.hoi.protocol.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;

public final class ScreenOfficerSourceTest {
    private ScreenOfficerSourceTest() {}
    public static void run(ClientGameTestContext context) {
        String directory = System.getenv("HOI_OFFICER_PREVIEW");
        if (directory == null) return;
        context.getInput().resizeWindow(2560, 1440);
        context.runOnClient(client -> client.options.guiScale().set(2));
        try {
            for (String name : List.of("kor-initial", "prk-initial", "prc-initial", "kor-selected")) {
                var view = new ProtocolMenuPayloads.OpenScreen(Files.readString(Path.of(directory, name + ".json"))).view();
                context.setScreen(() -> new ScreenMenu(view, ProtocolMenuTab.OFFICER_CORPS, () -> {}));
                context.getInput().setCursorPos(2400, 1300);
                context.waitTicks(3);
                var stats = view.page(ProtocolMenuTab.OFFICER_CORPS).sections().stream()
                        .filter(s -> s.icon().endsWith("_stats")).flatMap(s -> s.entries().stream()).toList();
                if (stats.size() != 47) throw new AssertionError("Incomplete source grid: " + name);
                context.runOnClient(client -> {
                    for (var stat : stats) {
                        var value = UiTooltips.styled(net.minecraft.network.chat.Component.literal(stat.value()), false);
                        if (client.font.width(value) * ScreenMenu.officerStatScale(client.font, value, 22) > 22.001f)
                            throw new AssertionError("Statistic suffix clipped: " + stat.name());
                        var lines = UiTooltips.exactLines(client.font, stat.detail(), 324);
                        if (lines.isEmpty() || lines.stream().anyMatch(line -> client.font.width(line) > 300))
                            throw new AssertionError("Unrenderable source tooltip: " + stat.name());
                    }
                });
                context.takeScreenshot("hoi-officer-" + name + "-gui2");
                if (name.equals("kor-selected")) {
                    for (String key : stats.stream().map(s->s.icon().substring("officer/stats/".length())).toList()) {
                        var stat = stats.stream().filter(s -> s.icon().equals("officer/stats/" + key)).findFirst().orElseThrow();
                        long occurrence=stats.subList(0,stats.indexOf(stat)).stream().filter(s->s.name().equals(stat.name())).count();
                        var position = context.computeOnClient(client -> client.gui.screen().children().stream()
                                .filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                                .filter(c -> c.getMessage().getString().equals(stat.name())).skip(occurrence).findFirst()
                                .map(c -> new int[]{c.getX() + c.getWidth()/2, c.getY() + c.getHeight()/2}).orElseThrow());
                        context.getInput().setCursorPos(position[0] * 2, position[1] * 2);
                        context.waitTicks(3);
                        context.takeScreenshot("hoi-officer-tooltip-" + key + "-gui2");
                    }
                    var quote = new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory, "kor-academy-choices.json")), true).view();
                    var sent=new ArrayList<ProtocolDialogPayloads.Action>();
                    var issued=quote.issued(UUID.randomUUID().toString(),3);
                    var parent = context.computeOnClient(client -> client.gui.screen());
                    context.setScreen(() -> new ScreenPoliticsChoice(issued, parent, sent::add));
                    context.getInput().setCursorPos(2400, 1300);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-officer-academy-choices-gui2");
                    var columns = context.computeOnClient(client -> client.gui.screen().children().stream()
                            .filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                            .filter(c -> quote.tiles().stream().anyMatch(t -> t.name().equals(c.getMessage().getString())))
                            .map(AbstractWidget::getX).distinct().count());
                    if (columns != 2) throw new AssertionError("Officer source choice must have two columns");
                    context.runOnClient(client -> {
                        var screen=(ScreenPoliticsChoice)client.gui.screen();
                        var candidate=issued.choices().stream().filter(c->c.id().endsWith("bold_attack_spirit")).findFirst().orElseThrow();
                        var button=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                                .filter(c->c.getMessage().getString().equals(candidate.label())).findFirst().orElseThrow();
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,
                                button.getY()+button.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false); screen.mouseClicked(click,true);
                        if(sent.size()!=1 || !sent.getFirst().token().equals(issued.token())
                                || sent.getFirst().revision()!=3 || !sent.getFirst().choice().equals(candidate.id()))
                            throw new AssertionError("Officer click must issue one bounded authoritative choice: " + sent);
                        screen.update(issued.issued(issued.token(),4));
                        if(screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                                .filter(c->c.getMessage().getString().equals(candidate.label())).noneMatch(c->c.active))
                            throw new AssertionError("Server refresh must unlock a rejected officer choice");
                    });
                    var replacement=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-academy-replacement.json")),true).view();
                    context.setScreen(() -> new ScreenPoliticsChoice(replacement,parent));
                    context.getInput().setCursorPos(2400,1300);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-officer-academy-replacement-gui2");
                    var tacticMenu=new ProtocolMenuPayloads.OpenScreen(Files.readString(Path.of(directory,"kor-tactic-menu.json"))).view();
                    var requests=new ArrayList<ProtocolDialogPayloads.PoliticsOpen>();
                    context.setScreen(() -> new ScreenMenu(tacticMenu,ProtocolMenuTab.OFFICER_CORPS,() -> {},requests::add));
                    context.runOnClient(client -> {
                        var screen=(ScreenMenu)client.gui.screen();
                        var button=screen.children().stream().filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                                .filter(c -> c.getMessage().getString().startsWith("국가 선호 전술 ·")).findFirst().orElseThrow();
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false);
                        if(requests.size()!=1 || !requests.getFirst().equals(new ProtocolDialogPayloads.PoliticsOpen("officer_preferred_tactic",0)))
                            throw new AssertionError("Preferred tactic button must issue its bounded source request");
                    });
                    var tacticQuote=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-tactic-choices.json")),true).view();
                    var tacticActions=new ArrayList<ProtocolDialogPayloads.Action>();var tacticIssued=tacticQuote.issued(UUID.randomUUID().toString(),3);
                    var tacticParent=context.computeOnClient(client -> client.gui.screen());
                    context.setScreen(() -> new ScreenPoliticsChoice(tacticIssued,tacticParent,tacticActions::add));
                    context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-officer-preferred-tactic-choices-gui2");
                    context.runOnClient(client -> {
                        var screen=(ScreenPoliticsChoice)client.gui.screen();
                        var candidate=tacticIssued.choices().stream().filter(c -> c.id().equals("tactic_basic_attack")).findFirst().orElseThrow();
                        var button=screen.children().stream().filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                                .filter(c -> c.getMessage().getString().equals(candidate.label())).findFirst().orElseThrow();
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                        if(tacticActions.size()!=1 || !tacticActions.getFirst().equals(new ProtocolDialogPayloads.Action(tacticIssued.token(),3,"tactic_basic_attack")))
                            throw new AssertionError("Preferred tactic submits exactly one authoritative source choice");
                    });
                    var selectedTactic=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-tactic-selected.json")),true).view();
                    context.setScreen(() -> new ScreenPoliticsChoice(selectedTactic,tacticParent));
                    context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-officer-preferred-tactic-selected-gui2");
                    for(int group=0;group<8;group++) {
                        var groupQuote=new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,"kor-spirit-choices-"+group+".json")),true).view();
                        context.setScreen(() -> new ScreenPoliticsChoice(groupQuote,parent));
                        context.getInput().setCursorPos(2400,1300);
                        context.waitTicks(3);
                        context.takeScreenshot("hoi-officer-spirit-group-"+group+"-gui2");
                        if(!groupQuote.tiles().isEmpty()) {
                            var first=groupQuote.tiles().getFirst();
                            var point=context.computeOnClient(client -> client.gui.screen().children().stream()
                                    .filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                                    .filter(c->c.getMessage().getString().equals(first.name())).findFirst()
                                    .map(c->new int[]{c.getX()+c.getWidth()/2,c.getY()+c.getHeight()/2}).orElseThrow());
                            context.getInput().setCursorPos(point[0]*2,point[1]*2);
                            context.waitTicks(6);
                            context.takeScreenshot("hoi-officer-spirit-group-"+group+"-candidate-tooltip-gui2");
                        }
                    }
                }
            }
        } catch (Exception failure) {
            throw new RuntimeException("Source officer renderer verification failed", failure);
        }
    }
}
