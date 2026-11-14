package dev.hoi.client.screen.decision;

import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.decision.DecisionPayloads;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractWidget;

public final class DecisionMarqueeTest {
    private DecisionMarqueeTest() {}
    public static void run(ClientGameTestContext game,CountryHud hud) {
        String name="합성 긴 결정 제목 가로 스크롤 검사 ".repeat(5);
        var rows=new ArrayList<DecisionPayloads.Row>();
        for(int index=0;index<2;index++)rows.add(new DecisionPayloads.Row("cost"+index,"category","합성 분류","",name+index,"","",12345.0,0,"",index==0,"§W방역물자: 12345§!","","READY",null,null,List.of(),index==1,"",null));
        var view=new DecisionPayloads.View("KOR",UUID.randomUUID().toString(),1,"",0,2,hud,rows,List.of(new DecisionPayloads.Category("category","합성 긴 분류 이름 ".repeat(9),"","","","",false)),1);
        try {
            for(int width:new int[]{2560,1280}) {
                game.getInput().resizeWindow(width,width*9/16);game.runOnClient(c->c.options.guiScale().set(2));
                var screen=game.computeOnClient(c->new DecisionScreen(view,r->{}));game.setScreen(()->screen);
                game.getInput().setCursorPos(width-4,width*9/16-4);game.waitTicks(3);
                int[] bounds=game.computeOnClient(c->{
                    try{var pane=DecisionScreen.class.getDeclaredField("pane");pane.setAccessible(true);if(pane.getInt(screen)!=275)throw new AssertionError("Decision width must match the original 550 pixels at GUI2");}
                    catch(ReflectiveOperationException error){throw new AssertionError(error);}
                    var button=screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals(name+0)).map(AbstractWidget.class::cast).findFirst().orElseThrow();
                    return new int[]{34,button.getY(),65,button.getHeight()};
                });
                var first=ImageIO.read(game.takeScreenshot("hoi-decisions-marquee-a-"+width+"-gui2").toFile());
                int gold=0,red=0;
                for(int y=bounds[1]*2;y<Math.min(first.getHeight(),(bounds[1]+bounds[3]*2+8)*2);y++)for(int x=240;x<440;x++) {
                    int rgb=first.getRGB(x,y)&0xFFFFFF;if(rgb==0xFFBD00)gold++;if(rgb==0xFF3232)red++;
                }
                if(gold<5||red<5)throw new AssertionError("Every custom-cost number must use original yellow or red despite an explicit white source span: "+gold+"/"+red);
                game.waitTicks(38);
                var second=ImageIO.read(game.takeScreenshot("hoi-decisions-marquee-b-"+width+"-gui2").toFile());
                int changed=0;
                for(int y=bounds[1]*2;y<(bounds[1]+bounds[3])*2;y++)for(int x=bounds[0]*2;x<(bounds[0]+bounds[2])*2;x++)if(first.getRGB(x,y)!=second.getRGB(x,y))changed++;
                if(changed<4)throw new AssertionError("Long decision names must move inside the fixed cell");
            }
        } catch(java.io.IOException error){throw new AssertionError(error);}
    }
}
