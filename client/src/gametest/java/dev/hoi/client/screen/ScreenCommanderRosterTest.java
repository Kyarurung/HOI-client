package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

public final class ScreenCommanderRosterTest {
    static void assertBoldHeaders(ClientGameTestContext game,net.minecraft.client.gui.screens.Screen screen,ProtocolDialogView.Tile hero) {
        game.runOnClient(client->{
            var found=new HashSet<String>();
            var graphics=new net.minecraft.client.gui.GuiGraphicsExtractor(client,new net.minecraft.client.renderer.state.gui.GuiRenderState(),-1,-1){
                @Override public void text(net.minecraft.client.gui.Font font,net.minecraft.util.FormattedCharSequence line,int x,int y,int color,boolean shadow){
                    var text=new StringBuilder();var bold=new boolean[]{true};line.accept((index,style,code)->{text.appendCodePoint(code);if(!Character.isWhitespace(code)&&!style.isBold())bold[0]=false;return true;});
                    if(text.toString().equals(hero.name())||text.toString().equals(hero.leader().role())){if(!bold[0])throw new AssertionError("Leader name and role glyphs must be bold: "+text);found.add(text.toString());}
                    super.text(font,line,x,y,color,shadow);
                }
            };
            screen.extractRenderState(graphics,-1,-1,0);
            if(!found.containsAll(List.of(hero.name(),hero.leader().role())))throw new AssertionError("Leader header text was not rendered");
            dev.hoi.client.ui.UiTooltips.officerText("§f장군§r 이름",true).visit((style,part)->{if(!part.isBlank()&&!style.isBold())throw new AssertionError("Source color resets must not clear required bold");return Optional.empty();},net.minecraft.network.chat.Style.EMPTY);
        });
    }
    public static void run(ClientGameTestContext game) {
        int previous=game.computeOnClient(c->c.options.guiScale().get());
        try {
            for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})) {
                game.getInput().resizeWindow(size[0],size[1]);
                game.runOnClient(c->c.options.guiScale().set(2));
                source(game,size);
                traitChoice(game,size);
                overlap(game,size);
                game.setScreen(()->new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("Bold glyph comparison")) {
                    @Override public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY,float delta) {
                        graphics.fill(10,40,400,160,0xFF888888);
                        var plain=dev.hoi.client.ui.UiTooltips.officerText("장군 전동진 제독 안상민",false);var bold=dev.hoi.client.ui.UiTooltips.officerText("장군 전동진 제독 안상민",true);
                        if(font.width(bold)<=font.width(plain))throw new AssertionError("Bold header glyphs must have additional advance");
                        graphics.text(font,plain,20,50,0xFFFFFFFF,false);graphics.text(font,bold,20,70,0xFFFFFFFF,false);
                        graphics.text(font,plain,20,100,0xFF000000,false);graphics.text(font,bold,20,120,0xFF000000,false);
                    }
                });game.waitTicks(2);game.takeScreenshot("hoi-header-bold-comparison-synthetic-"+size[0]+"x"+size[1]+"-gui2");
                for(boolean group:List.of(false,true)) {
                    var tiles=List.of(tile("general-low","장군",1),tile("marshal-high","원수",5),tile("general-high","장군",4),tile("marshal-low","원수",2));
                    var choices=new ArrayList<>(tiles.stream().filter(t->!t.section().equals("marshal-low")).map(t->new ProtocolDialogView.Choice(t.section(),t.name())).toList());
                    choices.add(new ProtocolDialogView.Choice("traits/general-high","세부 사항"));
                    choices.add(new ProtocolDialogView.Choice("promote/general-high","진급"));
                    choices.add(new ProtocolDialogView.Choice("advisor/general-high","장교단 역할"));
                    var view=new ProtocolDialogView(UUID.randomUUID().toString(),7,ProtocolDialogView.Kind.COMMANDER,group?"집단군 임명 순서 합성 검증":"군 임명 순서 합성 검증","","","",group?"commander/group":"commander/view","",tiles,choices);
                    var sent=new ArrayList<ProtocolDialogPayloads.Action>();
                    var screen=game.computeOnClient(c->new ScreenCommanderRoster(view,null,sent::add));
                    game.setScreen(()->screen);game.waitTicks(2);
                    game.runOnClient(c->{
                        var order=screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiMenuButton).map(w->(dev.hoi.client.ui.UiMenuButton)w).map(w->w.getMessage().getString()).filter(s->s.contains("-")).toList();
                        var expected=group?List.of("marshal-high","general-high","marshal-low","general-low"):List.of("general-high","general-low","marshal-high","marshal-low");
                        if(screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiMenuButton).map(w->((dev.hoi.client.ui.UiMenuButton)w).getMessage().getString()).anyMatch(label->label.contains("임명된 지휘관")))throw new AssertionError("Assigned filter must not be present");
                        if(!order.equals(expected))throw new AssertionError("Roster rank/skill order changed: "+order);
                        if(screen.choiceButton("marshal-low")!=null)throw new AssertionError("Sorting granted an unavailable choice");
                        if(screen.choiceButton("promote/marshal-high")!=null||screen.choiceButton("promote/general-low").active||!screen.choiceButton("promote/general-high").active)throw new AssertionError("Promotion visibility and authorization must follow the server fields");
                        var disabled=screen.choiceButton("advisor/general-low");if(disabled==null||disabled.active)throw new AssertionError("Unavailable advisor must retain a disabled tooltip target");
                        var denied=new net.minecraft.client.input.MouseButtonEvent(disabled.getX()+2,disabled.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(denied,false);if(!sent.isEmpty())throw new AssertionError("Disabled advisor must consume input without assigning the row");
                    });
                    game.getInput().setCursorPos(size[0]-10,size[1]-10);game.waitTicks(2);
                    game.takeScreenshot("hoi-commander-rank-order-"+(group?"group":"army")+"-synthetic-"+size[0]+"x"+size[1]+"-gui2");
                    for(String id:List.of("traits/general-high","promote/general-high","advisor/general-high")){
                        game.runOnClient(c->{var b=screen.choiceButton(id);var e=new net.minecraft.client.input.MouseButtonEvent(b.getX()+b.getWidth()/2.,b.getY()+b.getHeight()/2.,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(e,false);screen.mouseClicked(e,true);if(sent.size()!=1||!sent.getFirst().choice().equals(id))throw new AssertionError("Row control must consume the click before candidate assignment: "+id+" active="+b.active+" sent="+sent);screen.update(view.issued(view.token(),screen.view().revision()+1));sent.clear();});game.waitTicks(2);
                    }
                    game.runOnClient(c->screen.update(view.issued(view.token(),7+3)));
                    game.runOnClient(c->{
                        var button=screen.choiceButton(group?"marshal-high":"general-high");
                        var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2.,button.getY()+button.getHeight()/2.,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                        screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                        if(sent.size()!=1||!sent.getFirst().token().equals(view.token())||sent.getFirst().revision()!=10)throw new AssertionError("Sorted row must preserve authenticated request");
                        if(!sent.getFirst().choice().equals(group?"marshal-high":"general-high"))throw new AssertionError("Sorted row sent another candidate");
                        if(screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.AbstractWidget).map(w->(net.minecraft.client.gui.components.AbstractWidget)w).anyMatch(w->w.active))throw new AssertionError("Pending selection must disable repeat input");
                    });
                }
            }
        } finally {game.setScreen(()->null);game.runOnClient(c->c.options.guiScale().set(previous));}
    }
    private static void source(ClientGameTestContext game,int[] size) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            for(boolean group:List.of(false,true)) {
                String kind=group?"army-group":"army";
                var view=new ProtocolDialogPayloads.Show(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-appointment-"+kind+"-mixed.json")),true).view().issued(UUID.randomUUID().toString(),7);
                String preferred=group?"원수":"장군";
                var rank=Comparator.<ProtocolDialogView.Tile>comparingInt(t->t.leader().role().equals(preferred)?0:1);
                var skill=Comparator.comparingInt((ProtocolDialogView.Tile t)->t.leader().skill()).reversed();
                var rows=view.tiles().stream().filter(t->t.leader()!=null).sorted((group?skill.thenComparing(rank):rank.thenComparing(skill))).toList();
                if(rows.stream().noneMatch(t->t.leader().role().equals("원수"))||rows.stream().noneMatch(t->t.leader().role().equals("장군")))throw new AssertionError("Authoritative mixed ranks are required");
                var sent=new ArrayList<ProtocolDialogPayloads.Action>();
                var screen=game.computeOnClient(c->new ScreenCommanderRoster(view,null,sent::add));game.setScreen(()->screen);game.waitTicks(2);
                assertBoldHeaders(game,screen,rows.getFirst());
                game.runOnClient(c->{
                    var labels=screen.children().stream().filter(w->w instanceof dev.hoi.client.ui.UiMenuButton).map(w->((dev.hoi.client.ui.UiMenuButton)w).getMessage().getString()).filter(s->rows.stream().anyMatch(t->t.name().equals(s))).toList();
                    if(labels.isEmpty()||!labels.equals(rows.stream().limit(labels.size()).map(ProtocolDialogView.Tile::name).toList()))throw new AssertionError("Source rank and skill order changed: "+labels);
                });
                game.getInput().setCursorPos(size[0]-10,size[1]-10);game.waitTicks(2);game.takeScreenshot("hoi-commander-source-appointment-"+kind+"-"+size[0]+"x"+size[1]+"-gui2");
                var candidate=rows.stream().filter(t->t.leader().role().equals("장군")&&view.choices().stream().anyMatch(c->c.id().equals(t.section()))).findFirst().orElseThrow();
                if(group&&(!candidate.value().startsWith("지휘력 ")||!candidate.detail().contains("진급")))throw new AssertionError("Source general promotion cost and explanation are required");
                game.runOnClient(c->{var search=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).map(w->(net.minecraft.client.gui.components.EditBox)w).findFirst().orElseThrow();search.setValue(candidate.name());});game.waitTicks(2);
                var position=game.computeOnClient(c->{var button=screen.choiceButton(candidate.section());if(button==null)throw new AssertionError("Source candidate choice disappeared");return new int[]{button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2};});
                game.getInput().setCursorPos(position[0]*2,position[1]*2);game.waitTicks(16);game.takeScreenshot("hoi-commander-source-candidate-"+kind+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(c->{
                    var click=new net.minecraft.client.input.MouseButtonEvent(position[0],position[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));
                    screen.mouseClicked(click,false);screen.mouseClicked(click,true);
                    if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(view.token(),view.revision(),candidate.section()))))throw new AssertionError("Source candidate must send its authenticated choice exactly once");
                });
            }
            var confirmation=new ProtocolDialogPayloads.Show(java.nio.file.Files.readString(java.nio.file.Path.of(directory,"kor-promote-confirm.json")),true).view().issued(UUID.randomUUID().toString(),11);
            for(String choice:List.of("promote","cancel")){
                var actions=new ArrayList<ProtocolDialogPayloads.Action>();var screen=game.computeOnClient(c->new ScreenCommanderRoster(confirmation,null,actions::add));game.setScreen(()->screen);game.waitTicks(2);
                game.getInput().setCursorPos(size[0]-5,size[1]-5);game.takeScreenshot("hoi-commander-promotion-"+choice+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(c->{var button=screen.choiceButton(choice);if(button==null||!button.active)throw new AssertionError("Promotion confirmation must expose the issued choice");var click=new net.minecraft.client.input.MouseButtonEvent(button.getX()+2,button.getY()+2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(!actions.equals(List.of(new ProtocolDialogPayloads.Action(confirmation.token(),11,choice))))throw new AssertionError("Promotion confirmation must send one authenticated action");});
            }
        } catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static void traitChoice(ClientGameTestContext game,int[] size) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            for(String file:List.of("kor-army-appointment-advisors","kor-army-group-marshal","kor-navy-admirals-source")) {
                var view=new ProtocolDialogPayloads.Show(java.nio.file.Files.readString(java.nio.file.Path.of(directory,file+".json")),true).view().issued(UUID.randomUUID().toString(),7);
                var candidates=view.tiles().stream().filter(t->t.leader()!=null&&t.leader().traitDetails().stream().anyMatch(ti->ti.icon().equals("commander/traits/trait_add_new"))).toList();
                if(candidates.size()!=(file.contains("navy")?9:11))throw new AssertionError("Selectable source trait count changed");
                if(view.tiles().stream().filter(t->Set.of("전동진","손식").contains(t.name())).anyMatch(candidates::contains))throw new AssertionError("Unavailable trait choices must not show plus");
                var candidate=candidates.stream().filter(t->t.name().equals("이두희")).findFirst().orElse(candidates.getFirst());
                var detail=candidate.leader().traitDetails().getLast();
                if(!detail.icon().equals("commander/traits/trait_add_new")||!detail.name().isEmpty())throw new AssertionError("Plus must be the unnamed final trait detail");
                var actions=new ArrayList<ProtocolDialogPayloads.Action>();var screen=game.computeOnClient(c->new ScreenCommanderRoster(view,null,actions::add));game.setScreen(()->screen);
                game.runOnClient(c->{var search=screen.children().stream().filter(w->w instanceof net.minecraft.client.gui.components.EditBox).map(w->(net.minecraft.client.gui.components.EditBox)w).findFirst().orElseThrow();search.setValue(candidate.name());});game.waitTicks(2);
                game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(2);game.takeScreenshot("hoi-roster-plus-"+file+"-"+size[0]+"x"+size[1]+"-gui2");
                var point=game.computeOnClient(c->{int left=screen.width/2-113,top=Math.max(dev.hoi.client.ui.UiMenuBar.height(screen.width),screen.height/2-148),count=candidate.leader().traitDetails().size();return new int[]{dev.hoi.client.ui.UiFlagStack.left(count-1,count,left+70,94,12)+2,top+38+30};});
                game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(2);game.runOnClient(c->{if(!screen.renderedTooltip().equals(detail.tooltip()))throw new AssertionError("Plus must preserve its source tooltip");});game.takeScreenshot("hoi-roster-plus-hover-"+file+"-"+size[0]+"x"+size[1]+"-gui2");
                game.runOnClient(c->{var expected=screen.choiceButton("traits/"+(candidate.section().startsWith("admiral/")?candidate.section().substring(8):candidate.section()));if(expected==null||!expected.active)throw new AssertionError("Plus requires the issued traits choice");var click=new net.minecraft.client.input.MouseButtonEvent(point[0],point[1],new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(click,false);screen.mouseClicked(click,true);if(actions.size()!=1||!actions.getFirst().choice().startsWith("traits/")||!actions.getFirst().token().equals(view.token())||actions.getFirst().revision()!=7)throw new AssertionError("Plus must send one authenticated traits action");});
            }
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static void overlap(ClientGameTestContext game,int[] size) {
        String directory=System.getenv("HOI_COMMANDER_PREVIEW");if(directory==null)return;
        try {
            for(String file:List.of("kor-army-appointment-advisors","kor-navy-admirals-source")) {
                var source=new ProtocolDialogPayloads.Show(java.nio.file.Files.readString(java.nio.file.Path.of(directory,file+".json")),true).view();
                var candidate=source.tiles().stream().filter(t->t.leader()!=null).max(Comparator.comparingInt(t->t.leader().traitDetails().size())).orElseThrow();
                for(boolean dense:List.of(false,true)) {
                    var leader=candidate.leader();var traits=new ArrayList<>(leader.traitDetails());
                    if(dense){while(traits.size()<12)traits.add(traits.get(traits.size()%leader.traitDetails().size()));leader=new ProtocolDialogView.Leader(leader.role(),leader.skill(),leader.abilities(),leader.capacity(),leader.assigned(),leader.traits(),leader.abilityTooltips(),traits,leader.promotion(),leader.advisor(),leader.advisorIcon());}
                    var tile=new ProtocolDialogView.Tile(candidate.section(),candidate.name(),candidate.value(),candidate.icon(),candidate.detail(),candidate.gauge(),candidate.overlay(),leader);
                    var view=new ProtocolDialogView(UUID.randomUUID().toString(),1,source.kind(),dense?"합성 배치(실제 서버 상태 아님)":source.title(),source.subtitle(),source.body(),source.flag(),source.image(),source.sound(),List.of(tile),source.choices());
                    var screen=game.computeOnClient(c->new ScreenCommanderRoster(view,null,a->{}));game.setScreen(()->screen);game.getInput().setCursorPos(size[0]-5,80);game.waitTicks(2);game.takeScreenshot("hoi-roster-traits-"+file+(dense?"-dense-synthetic":"-source")+"-"+size[0]+"x"+size[1]+"-gui2");
                    for(int index:List.of(0,traits.size()-1)) {
                        int n=index;var expected=traits.get(n).tooltip();int count=traits.size();
                        var point=game.computeOnClient(c->{int left=screen.width/2-113,top=Math.max(dev.hoi.client.ui.UiMenuBar.height(screen.width),screen.height/2-148);return new int[]{dev.hoi.client.ui.UiFlagStack.left(n,count,left+70,94,12)+1,top+38+30};});
                        game.getInput().setCursorPos(point[0]*2,point[1]*2);game.waitTicks(2);game.runOnClient(c->{if(!screen.renderedTooltip().equals(expected))throw new AssertionError("Overlapped traits must hover the top visible icon");});
                    }
                    game.takeScreenshot("hoi-roster-traits-hover-"+file+(dense?"-dense-synthetic":"-source")+"-"+size[0]+"x"+size[1]+"-gui2");
                }
            }
        }catch(java.io.IOException error){throw new AssertionError(error);}
    }
    private static ProtocolDialogView.Tile tile(String id,String role,int skill) {
        var leader=new ProtocolDialogView.Leader(role,skill,List.of(1,2,3,4),24,id.equals("marshal-low"),List.of(),List.of(),List.of(),role.equals("장군")?"진급 £command_power §c40.00§r":"","장교단 역할\n지정할 수 있는 역할이 없습니다.",id.equals("general-high")?"commander/commands/idea_traits_strip/frame_6":"");
        return new ProtocolDialogView.Tile(id,id,role+" · "+skill,"politics/empty/leader","",null,"",leader);
    }
}
