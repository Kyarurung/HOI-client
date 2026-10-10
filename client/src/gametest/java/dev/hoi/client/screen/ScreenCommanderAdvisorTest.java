package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;
import java.nio.file.*;
import java.util.*;

public final class ScreenCommanderAdvisorTest {
    private ScreenCommanderAdvisorTest() {}
    private static ProtocolDialogView source(String directory,String name) throws java.io.IOException{return new ProtocolDialogPayloads.Show(Files.readString(Path.of(directory,name+".json")),true).view().issued(UUID.randomUUID().toString(),3);}
    private static net.minecraft.client.input.MouseButtonEvent click(AbstractWidget button){return new net.minecraft.client.input.MouseButtonEvent(button.getX()+button.getWidth()/2,button.getY()+button.getHeight()/2,new net.minecraft.client.input.MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0));}
    public static void run(ClientGameTestContext context,String directory) throws java.io.IOException {
        var tree=source(directory,"kor-advisor-role-tree");var sent=new ArrayList<ProtocolDialogPayloads.Action>();context.setScreen(()->new ScreenCommanderTraits(tree,null,sent::add));context.waitTicks(3);context.takeScreenshot("hoi-commander-advisor-entry-gui2");
        context.runOnClient(client->{var screen=(ScreenCommanderTraits)client.gui.screen();var button=screen.choiceButton("advisor-roles");if(button==null||!button.active)throw new AssertionError("Static advisor role entry is clickable");screen.mouseClicked(click(button),false);screen.mouseClicked(click(button),true);if(!sent.equals(List.of(new ProtocolDialogPayloads.Action(tree.token(),3,"advisor-roles"))))throw new AssertionError("Role entry sends one issued action");});
        var roles=source(directory,"kor-advisor-roles");sent.clear();context.setScreen(()->new ScreenCommanderRoster(roles,null,sent::add));context.waitTicks(3);context.takeScreenshot("hoi-commander-advisor-roles-gui2");
        context.runOnClient(client->{var screen=(ScreenCommanderRoster)client.gui.screen();var button=screen.choiceButton(roles.choices().getFirst().id());if(button==null||!button.active)throw new AssertionError("Static role appointment is clickable");screen.mouseClicked(click(button),false);screen.mouseClicked(click(button),true);if(sent.size()!=1||!sent.getFirst().choice().equals(roles.choices().getFirst().id()))throw new AssertionError("Role selection sends its issued choice once");});
        var confirm=source(directory,"kor-advisor-confirm");var choices=new ArrayList<String>();context.setScreen(()->new ScreenDialog(confirm){@Override protected void choose(String id){choices.add(id);}});context.waitTicks(3);context.takeScreenshot("hoi-commander-advisor-confirm-gui2");
        context.runOnClient(client->{var screen=(ScreenDialog)client.gui.screen();String body=screen.bodyText().getString();if(!confirm.body().contains("£")||body.contains("£")||body.codePoints().filter(code->code>=0xe000&&code<=0xf8ff).count()<3)throw new AssertionError("Source advisor effect icons must render as keyword glyphs");var button=screen.children().stream().filter(c->c instanceof AbstractWidget).map(c->(AbstractWidget)c).filter(c->c.getMessage().getString().equals("취소")).findFirst().orElseThrow();screen.mouseClicked(click(button),false);if(!choices.equals(List.of("cancel")))throw new AssertionError("Confirmation cancellation sends cancel");});
        for(String name:List.of("kor-advisor-selected","kor-admiral-roles","kor-advisor-admirals")){var view=source(directory,name);context.setScreen(()->new ScreenCommanderRoster(view,null,a->{}));context.waitTicks(3);context.takeScreenshot("hoi-commander-"+name+"-gui2");}
    }
}
