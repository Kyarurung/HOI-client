package dev.hoi.client.screen.occupation;

import dev.hoi.client.ResearchScreenTest;
import dev.hoi.protocol.occupation.OccupationPayloads.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import java.util.*;

public final class OccupationScreenTest {
    public static void run(ClientGameTestContext context) {
        context.runOnClient(client->{
            var oil=net.minecraft.network.chat.Component.literal("석유 자원 사보타주: 15%");
            if(!dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(oil).getString().equals(oil.getString()))throw new AssertionError("Sabotage resources must not infer icons");
            var victory=dev.hoi.client.ui.tooltip.TooltipKeywordIcons.decorateLines(net.minecraft.network.chat.Component.literal("주 승점 £victory_points£: +10%"),false).getString();
            if(victory.contains("£")||victory.codePoints().noneMatch(c->c>=0xe000&&c<=0xf8ff))throw new AssertionError("Victory point source token must resolve without trailing markers");
        });
        String token=UUID.randomUUID().toString();var requests=new ArrayList<Request>();
        var rows=List.of(row("s1","서울","KOR","대한민국",.31,.5),row("s2","경기도","KOR","대한민국",.31,.68),row("s3","베이징","PRC","중화인민공화국",.31,.51));
        var templates=List.of(new Option("infantry","Infantry Brigade","production/units/infantry","인력: 261.8\n제압: 42",true,List.of("261.8","42","0")),new Option("armor","Armored Brigade with a very long division template name","production/units/tank","인력: 330.9\n제압: 121",true,List.of("330.9","121","75")),template());
        var laws=laws();
        var labels=labels();
        var view=new View(token,1,"JAP",rows,templates,laws,"infantry","CIVILIAN_OVERSIGHT","",labels);
        context.setScreen(()->new OccupationScreen(token,view,requests::add));context.waitTicks(2);
        context.getInput().setCursorPos(1500,900); ResearchScreenTest.gui2Screenshot(context,"hoi-occupation");
        for(int[] size:List.of(new int[]{2560,1440},new int[]{1280,720})){
            context.getInput().resizeWindow(size[0],size[1]);context.waitTicks(3);
            var country=context.computeOnClient(client->{var button=(Button)client.gui.screen().children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("대한민국")).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(button.getX()+button.getWidth()/2.0)*scale,(button.getY()+button.getHeight()/2.0)*scale};});
            context.getInput().setCursorPos(country[0],country[1]);context.waitTicks(12);context.takeScreenshot("hoi-occupation-country-zoom-tooltip-"+size[0]+"x"+size[1]+"-gui2");
        }
        context.getInput().resizeWindow(1600,900);context.waitTicks(3);
        context.runOnClient(client->{
            var screen=(OccupationScreen)client.gui.screen();
            if(screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("국가 해방")).map(w->(Button)w).anyMatch(b->b.active))throw new AssertionError("Country release must remain disabled");
            int closed=screen.children().size();
            press(screen,"▸ 대한민국");
            if(screen.children().size()!=closed+6)throw new AssertionError("Only the two Korean states expand with their controls and zoom areas");
            press(screen,"▾ 대한민국");
            if(screen.children().size()!=closed)throw new AssertionError("Collapse removes the state controls");
            if(requests.size()!=0)throw new AssertionError("Expansion must not mutate campaign state");
        });
        var arrow=context.computeOnClient(client->{var screen=(OccupationScreen)client.gui.screen();var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("▸ 대한민국")).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(button.getX()+button.getWidth()/2.0)*scale,(button.getY()+button.getHeight()/2.0)*scale};});
        context.getInput().setCursorPos(arrow[0],arrow[1]);context.getInput().pressMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);context.waitTicks(2);context.getInput().releaseMouse(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT);
        context.runOnClient(client->{if(client.gui.screen().children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("▾ 대한민국")))throw new AssertionError("The original arrow hitbox must expand its country");});
        context.runOnClient(client->{var screen=(OccupationScreen)client.gui.screen();int before=requests.size();press(screen,"대한민국");
            if(requests.size()!=before+1||requests.getLast().action()!=dev.hoi.protocol.occupation.OccupationPayloads.Action.ZOOM||!requests.getLast().state().equals("country:KOR"))throw new AssertionError("Clicking the country band asks the server to zoom to its occupied state: "+requests);
            screen.update(new dev.hoi.protocol.occupation.OccupationPayloads.View(token,view.revision()+1,view.country(),view.rows(),view.templates(),view.laws(),view.defaultTemplate(),view.defaultLaw(),"",view.labels()));});
        context.getInput().setCursorPos(1500,900); ResearchScreenTest.gui2Screenshot(context,"hoi-occupation-expanded");
        context.takeScreenshot("hoi-occupation-expanded-1600-gui2");
        var effectPosition=context.computeOnClient(client->{
            var button=(Button)client.gui.screen().children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("민간 감독")&&b.getWidth()<30).findFirst().orElseThrow();
            double scale=client.getWindow().getGuiScale(),unit=button.getX()/477.0;
            return new double[]{unit*365*scale,(button.getY()-unit*24+unit*24)*scale,unit*27*scale};
        });
        for(int i=0;i<3;i++){context.getInput().setCursorPos(effectPosition[0]-effectPosition[2]*(2-i),effectPosition[1]);context.waitTicks(12);context.takeScreenshot("hoi-occupation-resistance-tier-"+i+"-gui2");}
        context.runOnClient(client->{
            var screen=(OccupationScreen)client.gui.screen();
            requests.clear();screen.update(new View(token,2,"JAP",rows,templates,laws,"infantry","CIVILIAN_OVERSIGHT","",labels));
            var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("민간 감독")&&b.getWidth()<30).findFirst().orElseThrow();
            button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            if(screen.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().equals("우선 설정 해제")))throw new AssertionError("Reset uses the default row only");
        });
        context.getInput().setCursorPos(1500,900);context.takeScreenshot("hoi-occupation-state-law-gui2");
        context.runOnClient(client->{
            var screen=(OccupationScreen)client.gui.screen();requests.clear();screen.update(new View(token,3,"JAP",rows,templates,laws,"infantry","CIVILIAN_OVERSIGHT","",labels));press(screen,"지역 경찰대");
            if(requests.size()!=1||requests.getLast().action()!=Action.LAW||!requests.getLast().state().equals("s2"))throw new AssertionError("State law must target the selected state only");
            requests.clear();screen.update(new View(token,4,"JAP",rows,templates,laws,"infantry","CIVILIAN_OVERSIGHT","",labels));
        });
        context.runOnClient(client->press((OccupationScreen)client.gui.screen(),"민간 감독"));
        context.getInput().setCursorPos(1500,900); ResearchScreenTest.gui2Screenshot(context,"hoi-occupation-laws");
        var point=context.computeOnClient(client->{var screen=(OccupationScreen)client.gui.screen();var b=(Button)screen.children().stream().filter(v->v instanceof Button button&&button.getMessage().getString().equals("지역 자치")).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(12);context.takeScreenshot("hoi-occupation-law-tooltip-gui2");
        var civilian=context.computeOnClient(client->{var b=(Button)client.gui.screen().children().stream().filter(w->w instanceof Button button&&button.getMessage().getString().equals("민간 감독")).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
        context.getInput().setCursorPos(civilian[0],civilian[1]);context.waitTicks(12);context.takeScreenshot("hoi-occupation-civilian-oversight-tooltip-gui2");
        context.runOnClient(client->{
            press((OccupationScreen)client.gui.screen(),"지역 자치");
            if(requests.size()!=1||requests.getFirst().action()!=Action.LAW||!requests.getFirst().state().equals("*"))throw new AssertionError("Default law selection must use the authenticated session");
            ((OccupationScreen)client.gui.screen()).update(new View(token,5,"JAP",rows,templates,laws,"infantry","LOCAL_AUTONOMY","",labels));
            press((OccupationScreen)client.gui.screen(),"주둔 사단 편제");
        });
        context.getInput().setCursorPos(1500,900); ResearchScreenTest.gui2Screenshot(context,"hoi-occupation-templates");
        var templatePoint=context.computeOnClient(client->{
            var b=(Button)client.gui.screen().children().stream().filter(w->w instanceof Button button&&button.getMessage().getString().equals(template().name())).findFirst().orElseThrow();
            if(b.active)throw new AssertionError("Ineligible server templates must remain disabled");
            double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};
        });
        context.getInput().setCursorPos(templatePoint[0],templatePoint[1]);context.waitTicks(12);context.takeScreenshot("hoi-occupation-template-tooltip-gui2");
        context.runOnClient(client->{
            press((OccupationScreen)client.gui.screen(),"Armored Brigade with a very long division template name");
            if(requests.getLast().action()!=Action.TEMPLATE||!requests.getLast().value().equals("armor"))throw new AssertionError("Template selection must be sent unchanged");
            ((OccupationScreen)client.gui.screen()).update(new View(token,6,"JAP",rows,templates,laws,"armor","LOCAL_AUTONOMY","",labels));
            press((OccupationScreen)client.gui.screen(),"저항도 지도");
            if(requests.getLast().action()!=Action.RESISTANCE)throw new AssertionError("Resistance button selects resistance mode");
            ((OccupationScreen)client.gui.screen()).update(new View(token,7,"JAP",rows,templates,laws,"armor","LOCAL_AUTONOMY","",labels));
            press((OccupationScreen)client.gui.screen(),"순응도 지도");
            if(requests.getLast().action()!=Action.COMPLIANCE)throw new AssertionError("Compliance mode must be distinct from resistance");
        });
        context.runOnClient(client->{
            var extremes=List.of(row("s1","서울","KOR","대한민국",1,1),row("s2","경기도","KOR","대한민국",0,0),row("s3","베이징","PRC","중화인민공화국",1,1));
            ((OccupationScreen)client.gui.screen()).update(new View(token,8,"JAP",extremes,templates,laws,"armor","LOCAL_AUTONOMY","",labels));
        });
        context.getInput().setCursorPos(1500,900);context.takeScreenshot("hoi-occupation-gauge-bounds-gui2");
        context.runOnClient(client->{
            var screen=(OccupationScreen)client.gui.screen();
            requests.clear();screen.update(new View(token,9,"JAP",rows,templates,laws,"infantry","CIVILIAN_OVERSIGHT","",labels));
            var button=(Button)screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("주둔 사단 편제")).skip(2).findFirst().orElseThrow();
            button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
        });
        context.getInput().setCursorPos(1500,900);context.takeScreenshot("hoi-occupation-state-template-gui2");
        context.runOnClient(client->{
            var screen=(OccupationScreen)client.gui.screen();press(screen,"기본 (Infantry Brigade) 사용");
            if(requests.getLast().action()!=Action.RESET||!requests.getLast().state().equals("s2")||!requests.getLast().value().equals("template"))throw new AssertionError("Default template row restores only the selected state");
        });
        var variants=List.of(administration(row("s1","서울","KOR","대한민국",0,0),true,true,true),administration(row("s2","베이징","PRC","중화인민공화국",0,0),false,true,true),administration(row("s3","시드니","AST","오스트레일리아",.5,.5),true,false,false),administration(row("s1","웰링턴","NZL","뉴질랜드",0,0),false,false,false));
        var variantLabels=new HashMap<>(labels);
        context.setScreen(()->new OccupationScreen(token,new View(token,20,"JAP",variants,templates,laws,"infantry","CIVILIAN_OVERSIGHT","",variantLabels),requests::add));
        context.runOnClient(client->{
            var screen=(OccupationScreen)client.gui.screen();for(var row:variants)press(screen,"▸ "+row.countryName());
            long templatesCount=screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("주둔 사단 편제")).count();
            if(templatesCount!=5)throw new AssertionError("Zero resistance keeps controls; inapplicable rows have none");
            if(screen.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("국가 해방")).anyMatch(w->((Button)w).active))throw new AssertionError("TFR release remains blocked");
        });
        context.getInput().setCursorPos(1500,900);context.takeScreenshot("hoi-occupation-release-return-with-without-gauges-gui2");
        context.runOnClient(client->{
            requests.clear();
            var screen=(OccupationScreen)client.gui.screen();
            var actions=screen.children().stream().filter(w->w instanceof Button button&&Set.of("영토 반환","국가 해방").contains(button.getMessage().getString())).map(w->(Button)w).toList();
            for(var button:actions)if(button.active!=button.getMessage().getString().equals("영토 반환"))throw new AssertionError("Authorized territory return is enabled; country release stays blocked");
            press(screen,"영토 반환");
            if(screen.children().stream().noneMatch(w->w instanceof Button b&&b.getMessage().getString().equals("OK")))throw new AssertionError("Return opens confirmation");
            press(screen,"취소");
            for(var child:screen.children())if(child instanceof Button button&&button.getMessage().getString().equals("국가 해방"))button.onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
            if(!requests.isEmpty()||screen.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().equals("OK")))throw new AssertionError("Cancelled return and blocked release must not send requests");
        });
        for(String caption:List.of("영토 반환","국가 해방")) {
            var hover=context.computeOnClient(client->{var b=(Button)client.gui.screen().children().stream().filter(w->w instanceof Button button&&button.getMessage().getString().equals(caption)).findFirst().orElseThrow();double scale=client.getWindow().getGuiScale();return new double[]{(b.getX()+b.getWidth()/2.0)*scale,(b.getY()+b.getHeight()/2.0)*scale};});
            context.getInput().setCursorPos(hover[0],hover[1]);context.waitTicks(12);context.takeScreenshot(caption.equals("영토 반환")?"hoi-occupation-return-enabled-gui2":"hoi-occupation-release-blocked-gui2");
        }
        context.setScreen(()->null);
    }
    private static Row administration(Row row,boolean resistance,boolean returning,boolean enabled){return new Row(row.state(),row.name(),row.country(),row.countryName(),row.resistance(),row.compliance(),row.garrison(),row.factories(),row.template(),row.law(),row.resistanceTooltip(),row.complianceTooltip(),row.resistanceEffects(),row.complianceEffects(),row.x(),row.y(),row.z(),row.details(),new Administration(resistance,returning,enabled,(returning?"§T"+row.countryName()+"에 영토 반환§r은 불가능함:":"§T"+row.countryName()+" 해방 불가능:§r")+"\n\n게임 규칙: §c모든 국가는 절대 "+(returning?"영토를 반환":"국가를 해방")+"할 수 없습니다.§r"));}
    private static Option template() {
        try(var input=OccupationScreenTest.class.getResourceAsStream("/occupation-template-fixture.json")) {
            return new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8),Option.class);
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static List<Option> laws() {
        try(var input=OccupationScreenTest.class.getResourceAsStream("/occupation-laws-fixture.json")) {
            return new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8),new com.google.gson.reflect.TypeToken<List<Option>>(){}.getType());
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static Map<String,String> labels() {
        try(var input=OccupationScreenTest.class.getResourceAsStream("/occupation-labels-fixture.json")) {
            return new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(java.util.Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8),new com.google.gson.reflect.TypeToken<Map<String,String>>(){}.getType());
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static Row row(String id,String name,String tag,String country,double resistance,double compliance) {
        return new Row(id,name,tag,country,resistance,compliance,3300,8,"infantry","CIVILIAN_OVERSIGHT","저항군 강도: §c"+(resistance*100)+"%§r","순응도 강도: §a"+(compliance*100)+"%§r",effects().stream().map(e->new Effect(e.id(),e.name(),e.icon(),e.threshold(),!e.id().equals("resistance_90")&&resistance>=e.threshold(),e.tooltip())).toList(),List.of(),0,0,0,new Details(tag.equals("KOR")?2:1,.65,.8,.24,1,economy(id)));
    }
    private static List<Effect> effects() {
        try(var input=OccupationScreenTest.class.getResourceAsStream("/occupation-effects-fixture.json")) {
            return new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8),new com.google.gson.reflect.TypeToken<List<Effect>>(){}.getType());
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static Economy economy(String id) {
        try(var input=OccupationScreenTest.class.getResourceAsStream("/occupation-manpower-fixture.json")) {
            var values=new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8),Economy[].class);
            return values[Integer.parseInt(id.substring(1))-1];
        }catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
    }
    private static void press(OccupationScreen screen,String name) {
        ((Button)screen.children().stream().filter(w->w instanceof Button b&&b.active&&b.getMessage().getString().equals(name)).findFirst().orElseThrow()).onPress(new KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_RETURN,0,0));
    }
}
