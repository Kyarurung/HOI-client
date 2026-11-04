package dev.hoi.client.map.commander;

import dev.hoi.protocol.commander.CommanderHudPayloads.*;
import java.util.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

final class CommanderAbilitiesRenderTest {
    private static Card abilities(Card card,List<String> ids,Mode role){
        var gson=new com.google.gson.Gson();var json=gson.toJsonTree(card).getAsJsonObject();json.addProperty("role",role.name());var panel=json.getAsJsonObject("panel");panel.addProperty("commander","synthetic-skill-limit");panel.add("abilities",gson.toJsonTree(ids));
        var details=new ArrayList<AbilityDetail>();if(ids!=null)for(int n=0;n<ids.size();n++)details.add(new AbilityDetail(ids.get(n),!Set.of("glider_planes","faster_naval_invasion_planning").contains(ids.get(n)),Set.of("glider_planes","faster_naval_invasion_planning").contains(ids.get(n))?"이 스킬의 효과는 아직 서버에 없어 사용할 수 없습니다.":"",n==0?Boolean.FALSE:n==1?Boolean.TRUE:null));panel.add("abilityDetails",gson.toJsonTree(details));return gson.fromJson(json,Card.class);
    }
    static void run(ClientGameTestContext game,View source,int[] size){
        var selected=source.cards().stream().filter(c->c.role()==Mode.ASSIGN_ARMY&&c.panel()!=null&&!c.panel().commander().isEmpty()).findFirst().orElseThrow();
        var all=List.of("force_attack","last_stand","staff_office_plan","extra_suplies","glider_planes","faster_naval_invasion_planning","siege_artillery","probing_attack","makeshift_bridges");
        String dimension=game.computeOnClient(client->client.level.dimension().identifier().toString());
        for(var role:List.of(Mode.ASSIGN_ARMY,Mode.ARMY_GROUP)){
            var target=source.cards().stream().filter(c->c.role()==role&&c.panel()!=null).findFirst().orElse(selected);
            var firstSlot=new int[]{-1};
            for(int count:new int[]{0,1,4,7,9,-1}){
                List<String> ids=count<0?null:count==7?List.of("force_attack","last_stand","staff_office_plan","glider_planes","extra_suplies","faster_naval_invasion_planning","siege_artillery"):count==9?List.of("force_attack","last_stand","staff_office_plan","faster_naval_invasion_planning","extra_suplies","glider_planes","siege_artillery","probing_attack","makeshift_bridges"):all.subList(0,count);
                var next=new View(source.country(),dimension,source.branch(),source.cards().stream().map(c->c.id().equals(target.id())?abilities(c,ids,role):c).toList(),source.wings(),true);
                game.runOnClient(client->{CommanderHud.accept(next);CommanderPanels.select(target.id(),false);});game.waitTicks(2);
                game.runOnClient(client->{
                    var hits=CommanderPanels.hits().stream().filter(h->h.icon().startsWith("ability_")).toList();
                    if(!hits.stream().map(h->h.icon().substring(8)).toList().equals(ids==null?List.of():ids.subList(0,Math.min(4,ids.size()))))throw new AssertionError("Keep at most four skills including the three basics in server order");
                    if(!hits.isEmpty()){if(hits.getFirst().y()!=CommanderPanels.actionbarTop(CommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight()),false)-17)throw new AssertionError("Ability background must meet the unchanged command bar");if(firstSlot[0]<0)firstSlot[0]=hits.getFirst().x();else if(firstSlot[0]!=hits.getFirst().x())throw new AssertionError("The first ability slot cannot shift when the list grows");}
                    var u=CommanderLayout.units(client.getWindow().getGuiScaledWidth(),client.getWindow().getGuiScaledHeight());
                    for(int n=1;n<hits.size();n++)if(hits.get(n).x()-hits.getFirst().x()!=u.size(n*18)||hits.get(n).y()!=hits.getFirst().y())throw new AssertionError("Abilities must fill fixed left-to-right slots");
                });
                var image=game.takeScreenshot("hoi-"+(role==Mode.ARMY_GROUP?"marshal":"army")+"-abilities-synthetic-"+(count<0?"unknown":count)+"-"+size[0]+"x"+size[1]+"-gui2");
                if(count>0)for(var grayId:count>=7?List.of("force_attack",ids.get(3)):List.of("force_attack")){var hit=game.computeOnClient(client->CommanderPanels.hits().stream().filter(h->h.icon().equals("ability_"+grayId)).findFirst().orElseThrow());if(!grayId.equals("force_attack")&&!hit.tip().equals("이 스킬의 효과는 아직 서버에 없어 사용할 수 없습니다."))throw new AssertionError("Unsupported skill tooltip must preserve the server text");try{var pixels=javax.imageio.ImageIO.read(image.toFile());int checked=0;for(int y=(hit.y()+3)*2;y<(hit.y()+hit.h()-3)*2;y++)for(int x=(hit.x()+3)*2;x<(hit.x()+hit.w()-3)*2;x++){int rgb=pixels.getRGB(x,y),r=rgb>>16&255,g=rgb>>8&255,b=rgb&255;if(Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))>4)throw new AssertionError("Unaffordable ability must be desaturated");checked++;}if(checked==0)throw new AssertionError("Missing grayscale samples");}catch(java.io.IOException error){throw new AssertionError(error);}}
            }
        }
        var overflow=new Card(selected.id(),selected.name(),selected.leader(),selected.portrait(),selected.marshal(),selected.marshalPortrait(),57,18,selected.detail(),selected.role(),selected.panel());
        game.runOnClient(client->{CommanderHud.accept(new View(source.country(),dimension,source.branch(),source.cards().stream().map(c->c.id().equals(overflow.id())?overflow:c).toList(),source.wings()));CommanderPanels.select(selected.id(),false);if(!CommanderHud.countLabel(overflow).equals("57/18")||CommanderHud.countColor(overflow)!=0xffe15b5b)throw new AssertionError("Assigned over-capacity army must retain the exact count and red display");});game.waitTicks(2);game.takeScreenshot("hoi-army-assigned-over-capacity-synthetic-"+size[0]+"x"+size[1]+"-gui2");
        game.runOnClient(client->{CommanderHud.accept(new View(source.country(),dimension,source.branch(),source.cards(),source.wings()));CommanderPanels.select(selected.id(),false);});game.waitTicks(2);
    }
}
