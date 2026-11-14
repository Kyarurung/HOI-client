package dev.hoi.client.screen.focus;

import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.focus.FocusPayloads;
import dev.hoi.client.ui.UiCommandLabels;
import dev.hoi.client.ui.tooltip.Tooltips;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class FocusHoverTest {
    private FocusHoverTest() {}
    public static void run(ClientGameTestContext game) {
        var node=new FocusPayloads.Node("hover-test","합성 중점 호버","설명은 호버에 나타나면 안 됩니다","",0,0,List.of(),List.of(),"선택 가능",0,true);
        var requirement=new FocusPayloads.DetailLine("§R미충족 조건§!",0,FocusPayloads.Assessment.UNMET,"test/requirement",true);
        var reward=new FocusPayloads.DetailLine("§G합성 효과§!",0,FocusPayloads.Assessment.MET,"test/reward",true);
        var requirements=List.of(requirement,new FocusPayloads.DetailLine("§G충족 조건§!",1,FocusPayloads.Assessment.MET,"test/met",true),new FocusPayloads.DetailLine("조건 안내",0,FocusPayloads.Assessment.NOT_EVALUATED,"test/info",true));
        var detail=new FocusPayloads.Detail(node.id(),node.name(),node.description(),504,0,0,0,0,"test/hover",1,requirements,List.of(reward),true,"",false);
        var view=new FocusPayloads.View("KOR","합성 중점","test-tree",UUID.randomUUID().toString(),1,"",CountryHud.UNKNOWN,List.of(node),false,detail,null);
        for(int width:new int[]{2560,1280}) {
            game.getInput().resizeWindow(width,width*9/16);game.runOnClient(c->c.options.guiScale().set(2));
            var requests=new ArrayList<FocusPayloads.Request>();
            var screen=game.computeOnClient(c->new FocusScreen(view,requests::add));game.setScreen(()->screen);
            game.runOnClient(c->{
                screen.update(FocusPayloads.Response.of(screen.token(),view));
                assertTooltip(screen,node,"완료 시간: §H21§!일",c.font);
            });
            int[] point=game.computeOnClient(c->screen.nodeBounds(node.id()));
            game.getInput().setCursorPos((point[0]+point[2]/2)*2,(point[1]+point[3]/2)*2);game.waitTicks(3);
            game.takeScreenshot("hoi-focus-hover-source-before-"+width+"-gui2");
            var active=new FocusPayloads.Node(node.id(),node.name(),node.description(),node.icon(),node.x(),node.y(),node.parents(),node.exclusive(),"진행 중",240,node.enabled());
            var activeDetail=new FocusPayloads.Detail(detail.id(),detail.name(),detail.description(),504,264,0,0,0,detail.source(),detail.line(),detail.requirements(),detail.rewards(),false,"",false);
            var activeView=new FocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),2,"",view.hud(),List.of(active),false,activeDetail,null);
            game.runOnClient(c->{screen.update(FocusPayloads.Response.of(screen.token(),activeView));assertTooltip(screen,active,"완료 시간: §H21§!일. (§H10§!일 남음)",c.font);});
            game.waitTicks(3);game.takeScreenshot("hoi-focus-hover-source-inprogress-"+width+"-gui2");
            var completeNode=new FocusPayloads.Node(node.id(),node.name(),node.description(),node.icon(),node.x(),node.y(),node.parents(),node.exclusive(),"완료",240,false);
            var completeView=new FocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),3,"",view.hud(),List.of(completeNode),false,detail,null);
            game.runOnClient(c->{screen.update(FocusPayloads.Response.of(screen.token(),completeView));assertTooltip(screen,completeNode,"§G완료됨§!",c.font);});
            var completeDetail=new FocusPayloads.Detail(detail.id(),detail.name(),detail.description(),504,504,0,0,0,detail.source(),detail.line(),detail.requirements(),detail.rewards(),false,"",true);
            var detailCompleteView=new FocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),4,"",view.hud(),List.of(active),false,completeDetail,null);
            game.runOnClient(c->{screen.update(FocusPayloads.Response.of(screen.token(),detailCompleteView));assertTooltip(screen,active,"§G완료됨§!",c.font);});
            var zeroDetail=new FocusPayloads.Detail(detail.id(),detail.name(),detail.description(),0,0,0,0,0,detail.source(),detail.line(),detail.requirements(),detail.rewards(),true,"",false);
            var zeroView=new FocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),5,"",view.hud(),List.of(node),false,zeroDetail,null);
            game.runOnClient(c->{screen.update(FocusPayloads.Response.of(screen.token(),zeroView));assertTooltip(screen,node,"완료 시간: §H0§!일",c.font);});
            game.runOnClient(c->{
                var next=new FocusPayloads.View(view.country(),view.name(),view.tree(),view.session(),6,"",view.hud(),List.of(active),false);
                screen.update(FocusPayloads.Response.of(screen.token(),next));
                String expected="§Y"+active.name()+"§!\n§!--------------\n§G클릭§!하여 세부 사항 보기\n§GCtrl+클릭§!하여 다른 창을 열어 놓은 채로 세부 사항 보기";
                if(!screen.hoverTooltip(active).equals(expected))throw new AssertionError("A new revision must remove all cached status, requirements and rewards until authoritative details arrive");
            });
            game.waitTicks(23);
            if(requests.stream().noneMatch(r->r.action()==FocusPayloads.Action.REFRESH&&r.id().equals(node.id())&&r.revision()==6))throw new AssertionError("Hover must request authoritative details at the current revision without selecting the node");
            game.runOnClient(c->{if(!screen.selectedId().isEmpty())throw new AssertionError("Hover must not open the detail window");});
        }
    }
    private static void assertTooltip(FocusScreen screen,FocusPayloads.Node node,String status,net.minecraft.client.gui.Font font) {
        if(!UiCommandLabels.text("SEPARATION_LINE", "--------------\n").equals("--------------\n"))throw new AssertionError("The source label must retain exactly fourteen dashes and its trailing newline");
        String expected="§Y"+node.name()+"§!\n\n"+status+"\n\n§R미충족 조건§!\n  §G충족 조건§!\n조건 안내\n\n§H효과§!:\n§G합성 효과§!\n§!--------------\n§G클릭§!하여 세부 사항 보기\n§GCtrl+클릭§!하여 다른 창을 열어 놓은 채로 세부 사항 보기";
        String actual=screen.hoverTooltip(node);
        if(!actual.equals(expected))throw new AssertionError("Focus hover must retain the exact source status, paragraphs, effect header, separator and click instructions: "+actual);
        var rows=Tooltips.tooltipLines(font,actual,screen.width);
        var visible=rows.stream().map(FocusHoverTest::visible).toList();
        int separator=visible.indexOf("--------------"),header=visible.indexOf("효과:");
        if(separator<1||!visible.get(separator-1).equals("합성 효과")||!visible.get(separator+1).startsWith("클릭하여"))throw new AssertionError("Rendered separator must directly follow the last reward and precede click instructions without blank lines");
        rows.get(separator).accept((i,s,c)->{if(s.getColor()!=null&&s.getColor().getValue()!=0xFFFFFF)throw new AssertionError("The separator must use the default text color");return true;});
        rows.get(header).accept((i,s,c)->{int color=s.getColor()==null?0xFFFFFF:s.getColor().getValue();if(color!=(c==':'?0xFFFFFF:Tooltips.GOLD))throw new AssertionError("Only the source effect label is highlighted; its colon is white");return true;});
    }
    private static String visible(net.minecraft.util.FormattedCharSequence row) {
        var text=new StringBuilder();row.accept((i,s,c)->{text.appendCodePoint(c);return true;});return text.toString();
    }
}
