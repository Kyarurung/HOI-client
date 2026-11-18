package dev.hoi.client.screen;

import dev.hoi.client.ui.tooltip.Tooltips;
import dev.hoi.client.screen.commander.CommanderOfficerComparisonTest;
import dev.hoi.client.screen.commander.CommanderPreferredTactics;
import dev.hoi.client.screen.politics.PoliticsChoice;
import dev.hoi.protocol.dialog.*;
import dev.hoi.protocol.menu.*;
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
            CommanderOfficerComparisonTest.verifySpecialBanner(context,new MenuPayloads.OpenScreen(Files.readString(Path.of(directory,"kor-initial.json"))).view());
            context.getInput().resizeWindow(2560,1440);
            context.runOnClient(client -> {
                var lines = Tooltips.sourceLines(client.font,"§H제목§!\n---\n§G+0.01§! £doctrine_mastery\n\n  조건",324);
                if (lines.size() != 5 || client.font.width(lines.get(1)) != 0 || client.font.width(lines.get(3)) != 0)
                    throw new AssertionError("Officer source separators must retain one empty line without a visible rule");
                var colors = new HashSet<Integer>(); var glyph = new boolean[1];
                for (var line : lines) line.accept((index,style,codepoint) -> {
                    if (style.getColor() != null) colors.add(style.getColor().getValue());
                    if (codepoint >= 0xe000 && codepoint <= 0xf8ff) glyph[0] = true;
                    if (codepoint == 0x200B) throw new AssertionError("Source separators cannot become synthetic rule markers");
                    return true;
                });
                if (!colors.contains(Tooltips.GOLD) || !colors.contains(Tooltips.GOOD) || !glyph[0])
                    throw new AssertionError("Original emphasis, benefit color and explicit mastery icon must survive wrapping");
            });
            for (String name : List.of("kor-initial", "prk-initial", "prc-initial", "kor-selected")) {
                var view = new MenuPayloads.OpenScreen(Files.readString(Path.of(directory, name + ".json"))).view();
                context.setScreen(() -> new ScreenMenu(view, MenuTab.OFFICER_CORPS, () -> {}));
                context.getInput().setCursorPos(2400, 1300);
                context.waitTicks(3);
                var stats = view.page(MenuTab.OFFICER_CORPS).sections().stream()
                        .filter(s -> s.icon().endsWith("_stats")).flatMap(s -> s.entries().stream()).toList();
                if (stats.size() != 47) throw new AssertionError("Incomplete source grid: " + name);
                context.runOnClient(client -> {
                    for (var stat : stats) {
                        var value = Tooltips.styled(net.minecraft.network.chat.Component.literal(stat.value()), false);
                        if (client.font.width(value) * ScreenMenu.officerStatScale(client.font, value, 22) > 22.001f)
                            throw new AssertionError("Statistic suffix clipped: " + stat.name());
                        var lines = Tooltips.exactLines(client.font, stat.detail(), 324);
                        if (lines.isEmpty() || lines.stream().anyMatch(line -> client.font.width(line) > 300))
                            throw new AssertionError("Unrenderable source tooltip: " + stat.name());
                    }
                });
                context.runOnClient(client -> {
                    var screen = (ScreenMenu)client.gui.screen();
                    var before = List.copyOf(screen.children());
                    for (var child : before) if (child instanceof dev.hoi.client.ui.UiHoverRegion region) {
                        var event = new net.minecraft.client.input.MouseButtonEvent(region.getX() + region.getWidth() / 2.0,
                                region.getY() + region.getHeight() / 2.0,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0));
                        if (region.mouseClicked(event, false) || region.mouseClicked(event, true))
                            throw new AssertionError("Statistic hover region consumed a click");
                        screen.mouseClicked(event, false);
                        if (!screen.children().equals(before) || client.gui.screen() != screen)
                            throw new AssertionError("Statistic click opened details or changed widgets");
                        region.setFocused(true);
                        if (region.isFocused()) throw new AssertionError("Statistic hover region acquired keyboard focus");
                    }
                    long counters = view.page(MenuTab.OFFICER_CORPS).sections().stream().filter(section -> section.icon().equals("army_hq")).count();
                    if (counters > 1 || before.stream().filter(child -> child instanceof dev.hoi.client.ui.UiHoverRegion).count() != 47 + counters)
                        throw new AssertionError("All source statistics and the army headquarters counter must be hover-only");
                    var removed = Set.of("지휘관", "장교", "제독", "사령부");
                    if (before.stream().filter(child -> child instanceof AbstractWidget).map(child -> (AbstractWidget)child)
                            .anyMatch(widget -> removed.contains(widget.getMessage().getString())))
                        throw new AssertionError("Removed officer-corps entry button remains reachable");
                });
                context.takeScreenshot("hoi-officer-" + name + "-gui2");
                if (name.equals("kor-initial")) {
                    var sections = view.page(MenuTab.OFFICER_CORPS).sections();
                    var armyEntries = sections.stream().filter(s -> s.icon().equals("army")).findFirst().orElseThrow().entries();
                    var command = sections.stream().filter(s -> s.icon().equals("high_command")).findFirst().orElseThrow().entries().getFirst();
                    var counter = sections.stream().filter(s -> s.icon().equals("army_hq")).findFirst().orElseThrow().entries().getFirst();
                    var expected = new LinkedHashMap<String, String>();
                    expected.put(armyEntries.get(1).name() + " · " + armyEntries.get(1).value(), armyEntries.get(1).detail());
                    expected.put(armyEntries.getFirst().name() + " · " + armyEntries.getFirst().value(), armyEntries.getFirst().detail());
                    expected.put(command.name() + " · " + command.value(), command.detail());
                    expected.put(counter.name(), counter.detail());
                    if (!armyEntries.get(1).detail().startsWith("§H육군 교리§!\n\n대교리: §H") || !armyEntries.get(1).detail().endsWith("\n\n§G클릭§!하여 교리 화면 열기")
                            || !armyEntries.getFirst().value().equals("미지정") || !armyEntries.getFirst().detail().equals("§G클릭§!하여 §H육군참모총장§G 추가")
                            || !command.detail().equals("§G클릭§!하여 §H최고사령부§G 추가") || !counter.detail().startsWith("배치된 §Y육군 사령부§!: §Y"))
                        throw new AssertionError("Officer source fixture must carry the native doctrine, staff and headquarters tooltips");
                    int shot = 0;
                    for (var entry : expected.entrySet()) {
                        var position = context.computeOnClient(client -> client.gui.screen().children().stream()
                                .filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                                .filter(c -> c.getMessage().getString().equals(entry.getKey())).findFirst()
                                .map(c -> new int[]{c.getX() + c.getWidth()/2, c.getY() + c.getHeight()/2}).orElseThrow());
                        context.getInput().setCursorPos(position[0] * 2, position[1] * 2);
                        context.waitTicks(3);
                        context.runOnClient(client -> {
                            String shown = ((ScreenMenu)client.gui.screen()).hoveredOfficerTooltip();
                            if (!shown.equals(entry.getValue())) throw new AssertionError("Officer tooltip must show the server text without an added title: " + entry.getKey() + " -> " + shown);
                            var lines = Tooltips.sourceLines(client.font, shown, 324);
                            for (int index = 1; index < lines.size(); index++)
                                if (client.font.width(lines.get(index)) == 0 && client.font.width(lines.get(index - 1)) == 0)
                                    throw new AssertionError("Officer tooltip must not stack blank lines: " + entry.getKey());
                        });
                        context.takeScreenshot("hoi-officer-native-tooltip-" + shot++ + "-gui2");
                    }
                    for (String label : List.of("포획된 지휘관", "육군 사령부 편제")) {
                        var position = context.computeOnClient(client -> client.gui.screen().children().stream()
                                .filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                                .filter(c -> c.getMessage().getString().equals(label)).findFirst()
                                .map(c -> new int[]{c.getX() + c.getWidth()/2, c.getY() + c.getHeight()/2}).orElseThrow());
                        context.getInput().setCursorPos(position[0] * 2, position[1] * 2);
                        context.waitTicks(3);
                        context.runOnClient(client -> {
                            String shown = ((ScreenMenu)client.gui.screen()).hoveredOfficerTooltip();
                            boolean captured = label.equals("포획된 지휘관");
                            if (captured ? !shown.startsWith("§e포획된 지휘관§r\n") || !shown.contains("\n\n전투 중에 지휘관이 포로로 잡힐 수 있습니다.")
                                    : !shown.contains("육군 사령부 편제") || !shown.contains("목록을 전환합니다."))
                                throw new AssertionError("Officer command tooltip must use the native wording: " + label + " -> " + shown);
                        });
                        context.takeScreenshot("hoi-officer-native-tooltip-" + shot++ + "-gui2");
                    }
                }
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
                    var quote = new DialogPayloads.Show(Files.readString(Path.of(directory, "kor-academy-choices.json")), true).view();
                    var sent=new ArrayList<DialogPayloads.Action>();
                    var issued=quote.issued(UUID.randomUUID().toString(),3);
                    var parent = context.computeOnClient(client -> client.gui.screen());
                    context.setScreen(() -> new PoliticsChoice(issued, parent, sent::add));
                    context.getInput().setCursorPos(2400, 1300);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-officer-academy-choices-gui2");
                    var columns = context.computeOnClient(client -> client.gui.screen().children().stream()
                            .filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                            .filter(c -> quote.tiles().stream().anyMatch(t -> t.name().equals(c.getMessage().getString())))
                            .map(AbstractWidget::getX).distinct().count());
                    if (columns != 2) throw new AssertionError("Officer source choice must have two columns");
                    context.runOnClient(client -> {
                        var screen=(PoliticsChoice)client.gui.screen();
                        var candidate=issued.choices().stream().filter(c->c.id().endsWith("bold_attack_spirit")).findFirst().orElseThrow();
                        var button=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                                .filter(c->c.getMessage().getString().equals(candidate.label())).findFirst().orElseThrow();
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,
                                button.getY()+button.getHeight()/2.0,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false); screen.mouseClicked(click,true);
                        if(sent.size()!=1 || !sent.getFirst().token().equals(issued.token())
                                || sent.getFirst().revision()!=3 || !sent.getFirst().choice().equals(candidate.id()))
                            throw new AssertionError("Officer click must issue one bounded authoritative choice: " + sent);
                        screen.choose(candidate.id());
                        if(sent.size()!=1)throw new AssertionError("Keyboard choice must retain the pending officer submission guard");
                        screen.update(issued.issued(issued.token(),4));
                        if(screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                                .filter(c->c.getMessage().getString().equals(candidate.label())).noneMatch(c->c.active))
                            throw new AssertionError("Server refresh must unlock a rejected officer choice");
                    });
                    var replacement=new DialogPayloads.Show(Files.readString(Path.of(directory,"kor-academy-replacement.json")),true).view();
                    context.setScreen(() -> new PoliticsChoice(replacement,parent));
                    context.getInput().setCursorPos(2400,1300);
                    context.waitTicks(3);
                    context.takeScreenshot("hoi-officer-academy-replacement-gui2");
                    var tacticMenu=new MenuPayloads.OpenScreen(Files.readString(Path.of(directory,"kor-tactic-menu.json"))).view();
                    var requests=new ArrayList<DialogPayloads.PoliticsOpen>();
                    context.setScreen(() -> new ScreenMenu(tacticMenu,MenuTab.OFFICER_CORPS,() -> {},requests::add));
                    context.runOnClient(client -> {
                        var screen=(ScreenMenu)client.gui.screen();
                        var button=screen.children().stream().filter(c -> c instanceof AbstractWidget).map(c -> (AbstractWidget)c)
                                .filter(c -> c.getMessage().getString().startsWith("국가 선호 전술 ·")).findFirst().orElseThrow();
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false);
                        if(requests.size()!=1 || !requests.getFirst().equals(new DialogPayloads.PoliticsOpen("officer_preferred_tactic",0)))
                            throw new AssertionError("Preferred tactic button must issue its bounded source request");
                    });
                    var tacticQuote=new DialogPayloads.Show(Files.readString(Path.of(directory,"kor-tactic-choices.json")),true).view();
                    var tacticActions=new ArrayList<DialogPayloads.Action>();var tacticIssued=tacticQuote.issued(UUID.randomUUID().toString(),3);
                    var tacticParent=context.computeOnClient(client -> client.gui.screen());
                    context.setScreen(() -> new CommanderPreferredTactics(tacticIssued,tacticParent,tacticActions::add));
                    context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-officer-preferred-tactic-choices-gui2");
                    context.runOnClient(client -> {
                        var screen=(CommanderPreferredTactics)client.gui.screen();
                        var candidate=tacticIssued.choices().stream().filter(c -> c.id().equals("tactic_basic_attack")).findFirst().orElseThrow();
                        var button=screen.choiceButton(candidate.id());
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+button.getHeight()/2.0,
                                new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                        if(tacticActions.size()!=1 || !tacticActions.getFirst().equals(new DialogPayloads.Action(tacticIssued.token(),3,"tactic_basic_attack")))
                            throw new AssertionError("Preferred tactic submits exactly one authoritative source choice");
                    });
                    var selectedTactic=new DialogPayloads.Show(Files.readString(Path.of(directory,"kor-tactic-selected.json")),true).view();
                    context.setScreen(() -> new CommanderPreferredTactics(selectedTactic,tacticParent));
                    context.getInput().setCursorPos(2400,1300);context.waitTicks(3);context.takeScreenshot("hoi-officer-preferred-tactic-selected-gui2");
                    for(int group=0;group<8;group++) {
                        var groupQuote=new DialogPayloads.Show(Files.readString(Path.of(directory,"kor-spirit-choices-"+group+".json")),true).view();
                        context.setScreen(() -> new PoliticsChoice(groupQuote,parent));
                        context.getInput().setCursorPos(2400,1300);
                        context.waitTicks(3);
                        var shot=javax.imageio.ImageIO.read(context.takeScreenshot("hoi-officer-spirit-group-"+group+"-gui2").toFile());
                        if(!groupQuote.tiles().isEmpty()) {
                            var first=groupQuote.tiles().getFirst();
                            var box=context.computeOnClient(client -> client.gui.screen().children().stream()
                                    .filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c)
                                    .filter(c->c.getMessage().getString().equals(first.name())).findFirst()
                                    .map(c->new int[]{c.getX(),c.getY(),c.getWidth(),c.getHeight()}).orElseThrow());
                            int[] size=context.computeOnClient(client->dev.hoi.client.ui.UiAssets.imageSize(first.icon()));
                            int left=Integer.MAX_VALUE,right=-1,top=Integer.MAX_VALUE,bottom=-1;
                            for(int y=(box[1]+3)*2;y<(box[1]+box[3]-3)*2;y++)for(int x=(box[0]+3)*2;x<(box[0]+33)*2;x++){
                                int c=shot.getRGB(x,y);
                                if(((c>>16&255)+(c>>8&255)+(c&255))/3>110){left=Math.min(left,x);right=Math.max(right,x+1);top=Math.min(top,y);bottom=Math.max(bottom,y+1);}
                            }
                            if(right<0||right-left>size[0]+2||bottom-top>size[1]+2)
                                throw new AssertionError("Spirit choice icons keep their source size: "+first.icon()+" "+(right-left)+"x"+(bottom-top)+" source "+size[0]+"x"+size[1]);
                            var point=new int[]{box[0]+box[2]/2,box[1]+box[3]/2};
                            context.getInput().setCursorPos(point[0]*2,point[1]*2);
                            context.waitTicks(6);
                            context.runOnClient(client->{
                                var screen=(PoliticsChoice)client.gui.screen();var bounds=screen.tooltipBounds();
                                if(bounds==null||bounds.x()-4<0||bounds.y()-4<0||bounds.x()+bounds.width()+4>screen.width||bounds.y()+bounds.height()+4>screen.height)
                                    throw new AssertionError("Selected and unavailable officer choices must retain visible source tooltips");
                            });
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
