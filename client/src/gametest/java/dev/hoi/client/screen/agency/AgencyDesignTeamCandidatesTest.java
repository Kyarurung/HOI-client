package dev.hoi.client.screen.agency;

import dev.hoi.client.screen.dialog.DialogCoordinator;
import dev.hoi.client.screen.industry.IndustryScreen;
import dev.hoi.protocol.agency.*;
import dev.hoi.protocol.industry.*;
import dev.hoi.protocol.menu.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class AgencyDesignTeamCandidatesTest {
    private AgencyDesignTeamCandidatesTest() {}
    public static String fixture(String name) {
        try { return Files.readString(Path.of(Objects.requireNonNull(System.getenv("HOI_DESIGN_TEAM_PREVIEW")),name+".json")); }
        catch(java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
    }
    public static void run(ClientGameTestContext context) {
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
            context.getInput().resizeWindow(size[0],size[1]);
            context.runOnClient(client->client.options.guiScale().set(2));
            for(String stage:List.of("unassigned","assigned","outdated")) {
                var view=new IndustryPayloads.Response(fixture("kor-designer-mio-"+stage)).view();
                var requests=new ArrayList<IndustryPayloads.Request>();
                context.setScreen(()->new IndustryScreen(MenuTab.PRODUCTION,view.session(),view,requests::add));
                context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(3);
                context.takeScreenshot("hoi-design-team-"+stage+"-"+size[0]+"-gui2");
                context.runOnClient(client->{
                    var screen=client.gui.screen();var team=button(screen," · 군수산업체");var upgrade=button(screen,"최신 특성으로 개량");
                    check(upgrade.active==view.tankDesigner().designTeam().outdated(),"Upgrade follows authoritative state");
                    check(upgrade.getRight()<=team.getX(),"Upgrade hover does not overlap team");
                    if(stage.equals("unassigned"))click(screen,team,InputConstants.MOUSE_BUTTON_LEFT);
                    else click(screen,stage.equals("outdated")?upgrade:team,stage.equals("outdated")?InputConstants.MOUSE_BUTTON_LEFT:InputConstants.MOUSE_BUTTON_RIGHT);
                    if(!stage.equals("unassigned")) {
                        var mutation=requests.getFirst();
                        check(requests.size()==1&&mutation.action()==(stage.equals("outdated")?IndustryPayloads.Action.TANK_DESIGN_TEAM_UPGRADE:IndustryPayloads.Action.TANK_DESIGN_TEAM)&&mutation.other().isEmpty()&&mutation.revision()==view.revision()&&mutation.session().equals(view.session()),"Upgrade/unassign preserves authenticated contract");
                        click(screen,button(screen,stage.equals("outdated")?"최신 특성으로 개량":" · 군수산업체"),stage.equals("outdated")?InputConstants.MOUSE_BUTTON_LEFT:InputConstants.MOUSE_BUTTON_RIGHT);
                        check(requests.size()==1,"Pending blocks repeated designer mutation");
                    }
                });
                if(stage.equals("unassigned")) {
                    context.waitTicks(2);context.takeScreenshot("hoi-design-team-list-"+size[0]+"-gui2");
                    var choice=view.tankDesigner().designTeam().choices().getFirst();
                    context.runOnClient(client -> {
                        for (var candidate : view.tankDesigner().designTeam().choices()) {
                            String tooltip = candidate.manufacturer().tooltip();
                            for (String token : List.of("armor_texticon", "attacker_speed_texticon")) {
                                if (!tooltip.contains("£" + token)) continue;
                                var rendered = dev.hoi.client.ui.UiKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("£" + token + " "), false);
                                if (rendered.getString().codePoints().noneMatch(c -> c >= 0xE000 && c <= 0xF8FF)) throw new AssertionError("Missing source manufacturer glyph: " + token);
                            }
                        }
                    });
                    var point=context.computeOnClient(client->{var b=button(client.gui.screen(),choice.manufacturer().name());double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+80)*scale,(b.getY()+10)*scale};});
                    context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(4);context.takeScreenshot("hoi-design-team-preview-"+size[0]+"-gui2");
                    context.runOnClient(client->{
                        var screen=client.gui.screen();click(screen,button(screen,choice.manufacturer().name()),InputConstants.MOUSE_BUTTON_LEFT);
                        check(requests.size()==1&&requests.getFirst().action()==IndustryPayloads.Action.TANK_DESIGN_TEAM&&requests.getFirst().other().equals(choice.manufacturer().id()),"Choice assigns exact server id");
                    });
                }
            }
            var view=new AgencyPayloads.Response(fixture("prc-agency-recruit-candidates")).view();
            var requests=new ArrayList<AgencyPayloads.Request>();
            context.setScreen(()->new AgencyScreen(null,view.session(),view,requests::add));
            context.getInput().setCursorPos(size[0]-2,size[1]-2);context.waitTicks(2);
            context.runOnClient(client->{click(client.gui.screen(),button(client.gui.screen(),"정보원 모집"),InputConstants.MOUSE_BUTTON_LEFT);check(client.gui.screen() instanceof AgencyCandidates&&requests.isEmpty(),"Recruit opens candidates without a mutation");});
            context.waitTicks(3);context.takeScreenshot("hoi-agency-candidates-"+size[0]+"-gui2");
            context.runOnClient(client->{
                var screen=(AgencyCandidates)client.gui.screen();
                check(DialogCoordinator.contentScreen()==screen.backdrop(),"Private snapshots route to agency parent");
                click(screen,button(screen,"이름"),InputConstants.MOUSE_BUTTON_LEFT);
                click(screen,button(screen,"이름"),InputConstants.MOUSE_BUTTON_LEFT);
                click(screen,button(screen,"기술"),InputConstants.MOUSE_BUTTON_LEFT);
                var close=button(screen,"모집 창 닫기");int originalX=close.getX(),originalY=close.getY();
                var press=new MouseButtonEvent(originalX-240,originalY+5,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                screen.mouseClicked(press,false);
                var moved=new MouseButtonEvent(press.x()+20,press.y()-8,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
                screen.mouseDragged(moved,20,-8);screen.mouseReleased(moved);
                check(button(screen,"모집 창 닫기").getX()==originalX+20 && button(screen,"모집 창 닫기").getY()<=originalY && requests.isEmpty(),"Sorting and dragging the recruitment window cannot hire a candidate");
                var search=screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
                search.setValue(view.candidates().getFirst().name());
                screen.setFocused(search);search.setCursorPosition(1);search.setHighlightPos(3);
                screen.backdrop().update(new AgencyView(view.session(),view.revision()+1,view.country(),view.name(),view.status(),view.items(),view.message(),view.candidates()));
                check(client.gui.screen()==screen&&screen.getFocused()==search&&search.getCursorPosition()==1&&search.getHighlighted().length()==2,"Candidate refresh preserves search focus and selection");
                var candidate=view.candidates().getFirst();
                click(screen,button(screen,candidate.name()),InputConstants.MOUSE_BUTTON_RIGHT);check(requests.isEmpty(),"Right click does not hire");
                click(screen,button(screen,candidate.name()),InputConstants.MOUSE_BUTTON_LEFT);
                check(requests.size()==1&&requests.getFirst().kind()==AgencyPayloads.Kind.CALL&&requests.getFirst().item().equals(candidate.item())&&requests.getFirst().session().equals(view.session())&&requests.getFirst().revision()==view.revision()+1,"Hire preserves exact candidate and refreshed session revision");
                click(screen,button(screen,candidate.name()),InputConstants.MOUSE_BUTTON_LEFT);check(requests.size()==1,"Repeated candidate click does not hire twice");
            });
        }
        context.setScreen(()->null);
    }
    private static Button button(Screen screen,String name) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b->b.getMessage().getString().endsWith(name)).findFirst().orElseThrow(()->new AssertionError("Missing control "+name));
    }
    private static void click(Screen screen,Button button,int mouse) {
        var event=new MouseButtonEvent(button.getX()+button.getWidth()/2.0,button.getY()+Math.min(10,button.getHeight()/2.0),new MouseButtonInfo(mouse,0));
        screen.mouseClicked(event,false);screen.mouseReleased(event);
    }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
}
