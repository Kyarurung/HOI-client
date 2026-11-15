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
        String name="가 £GFX_KOR_tur_texticon £GFX_PRK_epidemic_supplies_texticon "+"합성 긴 결정 제목 가로 스크롤 검사 ".repeat(5);
        var rows=new ArrayList<DecisionPayloads.Row>();
        for(int index=0;index<2;index++)rows.add(new DecisionPayloads.Row("cost"+index+(index==1?"@c:POL":""),"category","합성 분류","",name+index,"",index==0?"democracy":"GFX_decision_icon_KOR_coronavirus_death",12345.0,0,"",index==0,"£GFX_PRK_epidemic_supplies_texticon §W방역물자: 12345§!","","READY",null,index==1?new DecisionPayloads.Target("country","POL","Poland",""):null,List.of(),index==1,"",null));
        var view=new DecisionPayloads.View("KOR",UUID.randomUUID().toString(),1,"",0,2,hud,rows,List.of(new DecisionPayloads.Category("category","합성 긴 분류 이름 ".repeat(9),"","","","",false)),1);
        try {
            for(int width:new int[]{2560,1280}) {
                game.getInput().resizeWindow(width,width*9/16);game.runOnClient(c->c.options.guiScale().set(2));
                var screen=game.computeOnClient(c->new DecisionScreen(view,r->{}));game.setScreen(()->screen);
                game.getInput().setCursorPos(width-4,width*9/16-4);game.waitTicks(3);
                int[] bounds=game.computeOnClient(c->{
                    try{var pane=DecisionScreen.class.getDeclaredField("pane");pane.setAccessible(true);if(pane.getInt(screen)!=275)throw new AssertionError("Decision width must match the original 550 pixels at GUI2");}
                    catch(ReflectiveOperationException error){throw new AssertionError(error);}
                    var text=new DecisionText();
                    if(text.component(name).getString().contains("£"))throw new AssertionError("Marquee text icons must use the keyword font");
                    try {
                        var asset=DecisionScreen.class.getDeclaredMethod("asset",String.class,String.class);asset.setAccessible(true);
                        if(dev.hoi.client.ui.UiAssets.imageHeight((String)asset.invoke(screen,"democracy","icon"),20)>0)throw new AssertionError("The missing source democracy sprite must not be invented");
                        if(dev.hoi.client.ui.UiAssets.imageHeight((String)asset.invoke(screen,rows.get(1).icon(),"icon"),20)<=0)throw new AssertionError("The image row requires its actual original sprite");
                    } catch(ReflectiveOperationException error){throw new AssertionError(error);}
                    var button=screen.children().stream().filter(w->w instanceof AbstractWidget b&&b.getMessage().getString().equals(text.component(name+0).getString())).map(AbstractWidget.class::cast).findFirst().orElseThrow();
                    return new int[]{34,button.getY(),65,button.getHeight()};
                });
                var first=ImageIO.read(game.takeScreenshot("hoi-decisions-marquee-a-"+width+"-gui2").toFile());
                int substituted=0;
                for(int y=(bounds[1]+4)*2;y<(bounds[1]+bounds[3]-4)*2;y++)for(int x=20;x<60;x++) {
                    int color=first.getRGB(x,y),red=color>>16&255,green=color>>8&255,blue=color&255;
                    if(green>red+8&&green>blue+8&&green>60)substituted++;
                }
                if(substituted>0)throw new AssertionError("A missing decision image must leave the source picture area empty instead of using the menu button: "+substituted);
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
                int pictureChanged=0;
                for(int y=(bounds[1]+bounds[3]+4)*2;y<(bounds[1]+bounds[3]*2-4)*2;y++)for(int x=20;x<60;x++)if(first.getRGB(x,y)!=second.getRGB(x,y))pictureChanged++;
                if(pictureChanged<4)throw new AssertionError("The original row picture must move beside the name inside their shared marquee boundary");
            }
        } catch(java.io.IOException error){throw new AssertionError(error);}
    }
}
