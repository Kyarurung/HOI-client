package dev.hoi.client.screen.research;

import dev.hoi.protocol.research.ResearchPayloads;
import dev.hoi.protocol.research.ResearchView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

public final class ResearchInteractionTest {
    private ResearchInteractionTest() {}
    public static void run(ClientGameTestContext game,ResearchView base) {
        var tech=base.technologies().getFirst();
        var active=base.technologies().stream().filter(t->t.status()==ResearchView.Status.ACTIVE).findFirst().orElseThrow();
        var teams=List.of(team("slower",tech.id(),1,100),team("small",tech.id(),2,1),team("best",tech.id(),2,4),team("tie",tech.id(),2,4));
        var slots=List.of(new ResearchView.Slot(0,active.id(),0),new ResearchView.Slot(1,"",0),new ResearchView.Slot(2,"",0),new ResearchView.Slot(3,"",0));
        var view=view(base,base.revision(),slots,teams);
        var busyTeams=List.of(team("best",tech.id(),2,4));
        var busyView=view(base,base.revision(),List.of(new ResearchView.Slot(0,active.id(),0,"best"),new ResearchView.Slot(1,tech.id(),0,"best"),new ResearchView.Slot(2,"",0),new ResearchView.Slot(3,"",0)),busyTeams);
        game.runOnClient(c->{if(!new ResearchScreen(busyView,r->{}).teamTooltip(busyTeams.getFirst(),"",false).endsWith(active.name()+"\n"+tech.name()))throw new AssertionError("One design team researches several slots and lists each as busy");});
        var zero=view(base,base.revision(),slots,List.of(team("zero",tech.id(),2,0)));
        var zeroScreen=game.computeOnClient(c->new ResearchScreen(zero,r->{}));game.setScreen(()->zeroScreen);
        game.runOnClient(c->{var rows=zeroScreen.hoverLines(tech).stream().map(ResearchInteractionTest::visible).toList();if(rows.stream().noneMatch(row->row.startsWith("Shift+클릭하여 zero")))throw new AssertionError("A size-zero design team keeps its Shift+click instruction: "+rows);});
        for(int width:new int[]{2560,1280}) {
            game.getInput().resizeWindow(width,width*9/16);
            game.runOnClient(c->c.options.guiScale().set(2));
            hover(game,view,tech.id(),width,"with-team");
            for(int modifiers:new int[]{0,1,256,257}) {
                var requests=new ArrayList<ResearchPayloads.Request>();
                var screen=game.computeOnClient(c->new ResearchScreen(view,requests::add));
                game.setScreen(()->screen);
                game.runOnClient(c->{
                    screen.showDetail(tech.id());screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));
                    if(!screen.preferredTeam(tech.id()).id().equals("best"))throw new AssertionError("Rate, size and server-order tie break");
                    String tooltip=String.join("\n",screen.tooltipLines(tech).stream().map(t->t.getString()).toList());
                    if(!tooltip.contains("Shift+클릭")||!tooltip.contains("Alt+클릭")||!tooltip.contains("Shift+Alt+클릭")||!tooltip.contains("best")||!tooltip.contains("규모 4"))throw new AssertionError("Source interaction instructions and selected organization");
                    assertInstructions(screen,tech,true);
                    click(screen,tech.id(),modifiers);
                    if(modifiers==0) {
                        if(!requests.isEmpty()||field(screen,"detail")==null)throw new AssertionError("Plain click opens details only");
                        var open=screen.tooltipLines(tech).stream().map(t->t.getString()).toList();
                        if(!open.getLast().equals("Ctrl+클릭하여 다른 창을 열어 놓은 채로 세부 사항 보기")||!open.get(open.size()-2).startsWith("Shift+Alt+클릭"))throw new AssertionError("With the detail window open the instructions end with CLICK_DETAILS_CTRL: "+open);
                        return;
                    }
                    if(modifiers!=1)click(screen,tech.id(),modifiers);
                    if(requests.size()!=1||requests.getFirst().action()!=ResearchPayloads.Action.START||requests.getFirst().slot()!=0||field(screen,"detail")!=null)throw new AssertionError("Modified click sends one START for the selected slot without opening details: "+modifiers+" "+requests+" detail="+field(screen,"detail"));
                    if((boolean)field(screen,"overview")!=(modifiers==1))throw new AssertionError("Alt retains the tree");
                    screen.update(view(base,base.revision()+1,slots,teams));
                    if(requests.size()!=1)throw new AssertionError("Passive day response must not assign a team before the technology exists");
                    var started=new ArrayList<>(slots);started.set(2,new ResearchView.Slot(2,tech.id(),0));
                    var response=view(base,base.revision()+2,started,teams);screen.update(response);
                    if(requests.size()!=2||requests.getLast().action()!=ResearchPayloads.Action.DESIGN_TEAM||requests.getLast().slot()!=2||!requests.getLast().technology().equals("best")||requests.getLast().revision()!=response.revision())throw new AssertionError("Assign the actual authoritative response slot after START");
                    screen.update(response);screen.update(view);
                    if(requests.size()!=2)throw new AssertionError("Duplicate and older responses must not repeat assignment");
                    if((int)field(screen,"selectedSlot")!=(modifiers==257?1:0))throw new AssertionError("Only Shift+Alt selects the next empty slot");
                });
                game.getInput().setCursorPos(width-4,width*9/16-4);game.waitTicks(2);
                game.takeScreenshot("hoi-research-modifiers-"+modifiers+"-"+width+"-gui2");
            }
            var plain=view(base,base.revision(),slots,List.of());
            hover(game,plain,tech.id(),width,"without-team");
            game.setScreen(()->new ResearchScreen(plain,r->{}));
            game.runOnClient(c->{
                var screen=(ResearchScreen)c.gui.screen();
                String tooltip=String.join("\n",screen.tooltipLines(tech).stream().map(t->t.getString()).toList());
                if(!tooltip.contains("Shift+클릭하여 연구")||tooltip.contains("규모"))throw new AssertionError("No-team instructions must use the original plain-research keys");
                assertInstructions(screen,tech,false);
            });
            for(int modifiers:new int[]{1,256,257}) {
                var requests=new ArrayList<ResearchPayloads.Request>();
                var screen=game.computeOnClient(c->new ResearchScreen(plain,requests::add));game.setScreen(()->screen);
                game.runOnClient(c->{
                    screen.showDetail(tech.id());screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));click(screen,tech.id(),modifiers);
                    if(requests.size()!=1||requests.getFirst().action()!=ResearchPayloads.Action.START)throw new AssertionError("No-team modified click must start research directly");
                    screen.update(view(base,base.revision()+1,slots,List.of()));
                    if((int)field(screen,"selectedSlot")!=0)throw new AssertionError("A passive day response must not choose the next empty slot before START completes");
                    var started=new ArrayList<>(slots);started.set(2,new ResearchView.Slot(2,tech.id(),0));screen.update(view(base,base.revision()+2,started,List.of()));
                    if(requests.size()!=1||(int)field(screen,"selectedSlot")!=(modifiers==257?1:0))throw new AssertionError("No-team start must not assign an invented organization");
                });
                game.waitTicks(2);game.takeScreenshot("hoi-research-no-team-modifiers-"+modifiers+"-"+width+"-gui2");
            }
        }
    }
    private static void assertInstructions(ResearchScreen screen,ResearchView.Tech tech,boolean team) {
        var lines=screen.tooltipLines(tech);
        var expected=team?List.of("클릭하여 세부 사항 보기","Shift+클릭하여 best(을)를 이용해 연구(규모 4)","Alt+클릭하여 best(을)를 이용해 연구하고 현재 창 유지","Shift+Alt+클릭하여 best(을)를 이용해 연구하고 다음 빈 연구 슬롯 선택"):List.of("클릭하여 세부 사항 보기","Shift+클릭하여 연구","Alt+클릭하여 연구하고 이 창을 열린 상태로 유지","Shift+Alt+클릭하여 연구하고 다음 빈 슬롯 선택");
        int separator=lines.size()-5;
        if(separator<1||!lines.get(separator).getString().equals("--------------")||lines.get(separator-1).getString().isBlank()||!lines.subList(separator+1,lines.size()).stream().map(t->t.getString()).toList().equals(expected))throw new AssertionError("The source fourteen-dash line must immediately separate the last effect from all four exact interaction instructions");
        var text=screen.hoverLines(tech).stream().map(ResearchInteractionTest::visible).toList();int index=text.indexOf("클릭하여 세부 사항 보기");
        if(index<2||!text.get(index-1).isEmpty()||text.get(index-2).isBlank()||text.contains("--------------")||java.util.stream.IntStream.range(1,text.size()).anyMatch(i->text.get(i).isEmpty()&&text.get(i-1).isEmpty())
                ||!expected.stream().allMatch(line->text.stream().anyMatch(row->row.startsWith(line.substring(0,Math.min(10,line.length()))))))throw new AssertionError("The separator renders as one blank line before all four instructions and no blank lines double: "+text);
    }
    private static String visible(net.minecraft.util.FormattedCharSequence row) {
        var text=new StringBuilder();row.accept((i,s,c)->{text.appendCodePoint(c);return true;});return text.toString();
    }
    private static void hover(ClientGameTestContext game,ResearchView view,String technology,int width,String kind) {
        var screen=game.computeOnClient(c->new ResearchScreen(view,r->{if(r.action()!=ResearchPayloads.Action.CLOSE)throw new AssertionError("Hover must not send a research action");}));game.setScreen(()->screen);
        double[] point=game.computeOnClient(c->{screen.showDetail(technology);screen.keyPressed(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0));var layout=(ResearchLayout)field(screen,"layout");var node=layout.nodes().stream().filter(n->n.tech().id().equals(technology)).findFirst().orElseThrow();return new double[]{node.x()+node.width()/2-(double)field(screen,"scrollX"),(int)field(screen,"treeTop")+node.y()+node.height()/2-(double)field(screen,"scrollY")};});
        game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(3);game.takeScreenshot("hoi-research-hover-source-"+kind+"-"+width+"-gui2");
    }
    private static ResearchView.DesignTeam team(String id,String technology,double rate,int size) {
        return new ResearchView.DesignTeam(id,id,"",size,"",List.of(),rate-1,0,0,"",List.of(technology),List.of(rate),List.of(0),"");
    }
    private static ResearchView view(ResearchView base,long revision,List<ResearchView.Slot> slots,List<ResearchView.DesignTeam> teams) {
        return new ResearchView(base.session(),revision,base.country(),base.countryName(),base.day(),base.date(),base.speed(),slots,base.technologies(),"",base.hud(),base.benefits(),teams,Map.of());
    }
    private static Object field(ResearchScreen screen,String name) {
        try{var field=ResearchScreen.class.getDeclaredField(name);field.setAccessible(true);return field.get(screen);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static void click(ResearchScreen screen,String technology,int modifiers) {
        var layout=(ResearchLayout)field(screen,"layout");
        var node=layout.nodes().stream().filter(n->n.tech().id().equals(technology)).findFirst().orElseThrow();
        double x=node.x()+node.width()/2-(double)field(screen,"scrollX"),y=(int)field(screen,"treeTop")+node.y()+node.height()/2-(double)field(screen,"scrollY");
        screen.mouseClicked(new MouseButtonEvent(x,y,new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,modifiers)),false);
    }
}
