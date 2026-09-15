package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;
import java.util.*;

final class MapDetailScreenChecks {
    static void run(ClientGameTestContext context) {
        var own = new ArrayList<DialogView.Tile>();
        own.add(tile("owner", "주 소유국", "대한민국", "country/kor/flag"));
        own.add(tile("claims", "타국의 주장", "조선민주주의인민공화국", "country/prk/flag"));
        own.add(tile("population", "인구", "3.41M", "hud/manpower"));
        var buildings = List.of("infrastructure", "air_base", "anti_air", "radar", "railway");
        for (String id : buildings) own.add(tile("state", id, id.equals("radar") ? "2/2" : "5/5", "construction/" + id));
        own.add(new DialogView.Tile("slots", "열린 슬롯", "9/25", "", "대도시: §a+5§r\n원자로: §a+1§r\n부산광역시: §a+1§r\n원자로: §a+40%§r"));
        for (int i = 0; i < 25; i++) own.add(tile("shared", i < 6 ? "민간공장" : i < 9 ? "빈 건설 슬롯" : "잠긴 건설 슬롯", "", i < 6 ? "construction/civilian_factory" : i < 9 ? "construction/empty_slot" : "map_detail/locked_slot"));
        int power=java.util.stream.IntStream.range(0,own.size()).filter(i->own.get(i).section().equals("shared")).findFirst().orElseThrow()+4;
        own.set(power,new DialogView.Tile("shared","발전소","","construction/power_plant",
                "§e발전소§r (주 등급: §e3§r)\n발전소의 잠재 최대 레벨: §e4§r\n\n이 건물은 국가에 §a+6§r£resources_strip|7 을 제공하며, §c-4§r£resources_strip|1 및 §c-1§r£resources_strip|5 을(를) 소비합니다.\n\n§c이 주는 §e원자로§r나 §e에너지 농장§r을 건설할 수 없는 주입니다!§r"));
        for (String id : List.of("civilian_factory", "military_factory", "dockyard", "office_park", "refinery", "fuel_silo", "rocket_site", "nuclear_reactor", "power_plant", "energy_farm")) own.add(tile("limits", id, "1/4", "construction/" + id));
        for (String id : List.of("oil", "aluminum", "rubber", "tungsten", "iron", "chromium")) own.add(tile("resources", id, id.equals("oil") ? "-12" : "917", "production/resources/" + id));
        own.add(new DialogView.Tile("energy", "에너지 효율", "158", "map_detail/energy", "에너지 효율: §e1.67§r\n  사무단지: §c-2%§r\n  사무단지: §c-2%§r\n공장 에너지 소비량: §c-15%§r\n공장 에너지 사용량: §e157.67§r\n군수공장 에너지 사용량: §e67.57§r\n해군 조선소 에너지 사용량: §e7.50§r\n민간공장 에너지 사용량: §e82.59§r"));
        own.add(new DialogView.Tile("building_modifiers", "건설 주 변동치", "", "map_detail/building_modifiers", "건설 주 변동치\n§e민간공장:§r\n  강철 소비량: §c-3§r\n§e원자로:§r\n  주의 최대 공장: §a+1§r\n  주의 최대 공장: §a+40%§r\n  현지 건설 속도: §a+20%§r\n§e에너지 농장:§r\n  주의 최대 공장: §a+1§r\n  주의 최대 공장: §a+20%§r\n  현지 건설 속도: §a+10%§r\n§e사무단지:§r\n  현지 인력: §a+20%§r\n  현지 공장: §a+25%§r\n  지역 공장 에너지 소비량: §c+2%§r"));
        own.add(new DialogView.Tile("terrain", "지역", "도시", "", "§e부산§r\n§e부대 효과:§r\n이동: §e95.0%§r\n공격: §e30.0%§r\n방어: §e106.0%§r\n이 지형 내에서의 이동 비용 §e×3.20§r"));
        own.add(new DialogView.Tile("province_status", "지역 상태", "", "map_detail/temperature_1", "§e보통 · 15.0°C§r"));
        own.add(new DialogView.Tile("features", "부산광역시", "", "map_detail/art/gfx_modifiers_kor_busan", "주의 최대 공장: §a+1§r"));
        for (String id : List.of("hub", "fort", "coastal_fort", "port")) own.add(tile("province", id, id.equals("hub") ? "1/1" : id.equals("port") ? "6/10" : id.equals("fort") ? "5/5" : "4/5", "construction/" + id));
        var hidden = new ArrayList<DialogView.Tile>();
        hidden.add(tile("owner", "주 소유국", "조선민주주의인민공화국", "country/prk/flag"));
        hidden.add(tile("claims", "타국의 주장", "대한민국", "country/kor/flag"));
        hidden.add(tile("population", "인구", "5.03M", "hud/manpower"));
        hidden.add(tile("terrain", "지역", "평원", ""));
        for (String id : buildings) hidden.add(tile("state", id, id.equals("radar")?"0/1":id.equals("railway")?"4/5":id.equals("infrastructure")?"2/5":"0/5", "map_detail/disabled/" + id));
        hidden.add(tile("slots", "열린 슬롯", "9/25", ""));
        hidden.add(tile("province", "보급 중심지", "0/1", "map_detail/disabled/hub"));
        hidden.add(tile("province", "요새", "0/5", "map_detail/disabled/fort"));
        hidden.add(tile("province", "항구", "0/10", "map_detail/disabled/port"));
        hidden.add(tile("province", "해안 요새", "0/5", "map_detail/disabled/coastal_fort"));
        hidden.add(tile("resources", "텅스텐", "8", "production/resources/tungsten"));
        hidden.add(tile("resources", "강철", "26", "production/resources/iron"));
        var sea = new ArrayList<DialogView.Tile>();
        sea.add(tile("supremacy", "제해권", "48%", "hud/convoys"));
        sea.add(tile("weather", "날씨", "비", "map_detail/ocean_rain"));
        for (String section : List.of("naval", "air")) for (int i = 0; i < 4; i++) sea.add(tile(section, i == 0 ? "아군" : "정보 부족", i == 1 || i == 2 ? "0%" : "0", i == 0 ? "production/equipment/" + (section.equals("naval") ? "destroyer" : "fighter") : ""));
        sea.add(new DialogView.Tile("marker","해상 지배","","","§e해상 지배§r\n한 지역이 완전히 아군 통제하에 있다고 간주되려면 지배율이 §e60%§r 이상이어야 합니다."));
        sea.add(new DialogView.Tile("weather_icon","날씨","","map_detail/weather_rain","§e비§r\n\n임무 효율: §e80%§r\n공군 임무 효율: §e80%§r"));
        sea.add(new DialogView.Tile("sea_terrain","바다","","","§e바다§r\n\n§e해상 지배: 48%§r"));
        sea.add(new DialogView.Tile("access","접근","","","이용 단계: §a허용§r\n - 평소처럼 이 지역을 우리 §e함대§r와 §e상선§r의 경로로 사용할 것입니다."));
        sea.add(new DialogView.Tile("access","접근","","","이용 단계: §e회피§r\n - 다른 경로가 있다면, 이 전역을 우리 §e함대§r와 §e상선§r의 경로로 사용하는 것을 §e피할§r 것입니다."));
        sea.add(new DialogView.Tile("access","접근","","","이용 단계: §c봉쇄§r\n - 이 전역은 우리 §e상선 경로§r로 §c사용되지 않을§r 것입니다.\n - 우리 §e함대§r는 다른 경로가 없지 않은 한, 이 전역을 절대 §c통과하지 않을§r 것입니다."));
        sea.add(new DialogView.Tile("logbook","침몰한 함선","","","침몰한 함선"));
        for(var entries:List.of(own,hidden)) for(int i=0;i<entries.size();i++) {
            var t=entries.get(i);
            if(!Set.of("state","province","shared").contains(t.section())||t.icon().endsWith("slot"))continue;
            boolean partial=t.icon().equals("construction/civilian_factory")&&i==10;
            String tooltip=t.section().equals("shared")?"레벨 내구도: §e"+(partial?"65.00%":"100.00%")+"§r":"손상 단계: 0/§e"+t.value().split("/")[0]+"§r";
            entries.set(i,new DialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),new DialogView.Gauge(partial?.35:0,tooltip)));
        }
        var stack=new dev.hoi.client.ui.FlagStack();
        if(!stack.update(List.of("PRK","JAP")).equals(List.of("PRK","JAP")))throw new AssertionError("Initial flag order");
        if(!stack.update(List.of("KOR","JAP","PRK")).equals(List.of("PRK","JAP","KOR")))throw new AssertionError("New claimant must be on top at the right");
        if(dev.hoi.client.ui.FlagStack.left(0,1,7,80,30)!=7
                ||dev.hoi.client.ui.FlagStack.left(0,2,7,80,30)!=7
                ||dev.hoi.client.ui.FlagStack.left(1,2,7,80,30)!=37)throw new AssertionError("Flags start on the left without overlap while space remains");
        int old=dev.hoi.client.ui.FlagStack.left(1,2,0,80,30);
        if(dev.hoi.client.ui.FlagStack.left(1,3,0,80,30)>=old||dev.hoi.client.ui.FlagStack.left(2,3,0,80,30)!=50)throw new AssertionError("Overflow shifts older flags left and leaves the latest flag on the right");
        own.set(3,new DialogView.Tile("state","기반시설","4/5","construction/infrastructure","",new DialogView.Gauge(0,"손상 단계: 0/4"),"map_detail/constructing"));
        int firstFactory=java.util.stream.IntStream.range(0,own.size()).filter(i->own.get(i).icon().equals("construction/civilian_factory")).findFirst().orElseThrow();
        var factory=own.get(firstFactory);
        own.set(firstFactory,new DialogView.Tile(factory.section(),factory.name(),factory.value(),factory.icon(),factory.detail(),new DialogView.Gauge(.35,"레벨 내구도: 65.00%"),"map_detail/repairing"));
        var occupied=new ArrayList<>(own);
        occupied.add(tile("controller","지배국","일본","country/jap/flag"));
        occupied.add(tile("claims","타국의 주장","일본","country/jap/flag"));
        occupied.add(tile("claims","타국의 주장","대한민국","country/kor/flag"));
        occupied.add(new DialogView.Tile("resistance","저항도","82%","map_detail/resistance","저항 강도: §c82.34%§r\n\n저항도 목표: §e31%§r\n  기본값: §e35%§r\n  지배국이 평화로운 상태: §a-10%§r\n  낮은 안정도: §c18%§r\n  순응도: §a-12%§r\n\n저항도 감소: §a0.100%§r"));
        occupied.add(new DialogView.Tile("compliance","순응도","24%","map_detail/compliance","순응 강도: §a24.56%§r\n\n순응도 증가: §a0.062%§r\n  기본값: §e0.075%§r\n  지배국이 평화로운 상태: §a10%§r\n추가 순응도 변화:\n  높은 순응도: §c-0.020%§r"));
        occupied.add(new DialogView.Tile("resistance_target","저항도 목표","","map_detail/resistance_target","저항도 목표: §e31%§r",new DialogView.Gauge(.31,"저항도 목표: §e31%§r")));
        occupied.removeIf(t->t.section().equals("province_status"));
        occupied.add(new DialogView.Tile("province_status","기온","","map_detail/temperature_0","§e혹독한 추위 · -18.0°C§r\n공격: §c-20.0%§r\n돌파: §c-30.0%§r\n조직력 회복: §c-5.0%§r\n보급 효율: §c-15.0%§r\n사단 손실: §c+20.0%§r"));
        occupied.add(new DialogView.Tile("province_weather","날씨","","map_detail/weather_blizzard","§e눈보라§r\n조직력 회복: §c-10.0%§r\n보급 소비량: §c+15.0%§r\n보급 효율: §c-25.0%§r"));
        occupied.add(new DialogView.Tile("province_surface","지면","","map_detail/surface_deep_snow","§e깊이 쌓인 눈§r\n공격: §c-40.0%§r\n돌파: §c-30.0%§r\n사단 속도: §c-50.0%§r\n조직력 회복: §c-5.0%§r\n보급 효율: §c-40.0%§r"));
        int overlayIndex=0;
        for(int i=0;i<occupied.size();i++) {
            var t=occupied.get(i);
            if(t.section().equals("shared")&&overlayIndex<2)occupied.set(i,new DialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),t.gauge(),"map_detail/"+(overlayIndex++==0?"constructing":"repairing")));
        }
        var fixtures = List.of(view("부산", "map_detail/terrain/urban", own), view("평안남도", "map_detail/terrain/plains", hidden), view("동해", "map_detail/sea", sea), view("도쿄", "map_detail/terrain/toyko", occupied));
        int index = 0;
        for (var view : fixtures) {
            context.runOnClient(client -> {
                for (String asset : List.of("flag_frame", "flag_frame_thin", "locked_slot", "level_bg", "damage_bar", "damage_bg", "energy_bg", "sea_header", "ocean_rain", "ocean_storm", "ocean_snow", "ocean_blizzard")) if (client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/map_detail/" + asset + ".png")).isEmpty()) throw new AssertionError("Missing frame: " + asset);
                for(var t:view.tiles())if(!t.overlay().isBlank()&&client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/"+t.overlay()+".png")).isEmpty())throw new AssertionError("Missing overlay: "+t.overlay());
                for (var t : view.tiles()) if (!t.icon().isBlank() && client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/" + t.icon() + ".png")).isEmpty()) throw new AssertionError("Missing detail asset: " + t.icon());
            });
            for (int[] size : new int[][]{{2560, 1440}, {1600, 1000}, {854, 480}}) {
                context.runOnClient(client -> client.options.guiScale().set(2));
                context.getInput().resizeWindow(size[0], size[1]); context.waitTicks(3);
                open(context, view);
                context.runOnClient(client -> {
                    var screen = (MapDetailScreen)client.gui.screen();
                    if (screen.panelLeft() != 0 || screen.panelTop() + screen.panelHeight() != screen.height) throw new AssertionError("Map detail anchors to the bottom left");
                    if (screen.panelTop() < dev.hoi.client.ui.HoiMenuBar.height(screen.width)) throw new AssertionError("Detail cannot overlap the top menu");
                    for (var child : screen.children()) if (child instanceof Button b && (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height)) throw new AssertionError("Detail button escaped viewport");
                    if (!screen.allowsMovement()) throw new AssertionError("Map movement remains available");
                    if(size[0]==2560&&view.tiles().stream().anyMatch(t->t.section().equals("resources"))) {
                        double ratio=screen.panelWidth()/463.0;
                        for(String number:List.of("917","-12")) if(client.font.width(number)*Math.max(10,12*ratio)/client.font.lineHeight>Math.round(30*ratio))
                            throw new AssertionError("Resource number escaped its cell: "+number);
                    }
                });
                context.takeScreenshot("hoi-map-detail-" + index + "-" + size[0] + "-gui2");
                if(index==3&&size[0]==2560)context.runOnClient(client -> {
                    var screen=(MapDetailScreen)client.gui.screen();
                    double scale=screen.panelWidth()/463.0;
                    int start=(int)Math.round(20*scale),area=(int)Math.round(120*scale),fw=(int)Math.round(52*scale);
                    String[] countries={"PRK","JAP","KOR"};
                    for(int i=0;i<countries.length;i++) {
                        int fx=dev.hoi.client.ui.FlagStack.left(i,countries.length,start,area,fw);
                        int visible=dev.hoi.client.ui.FlagStack.visibleWidth(i,countries.length,start,area,fw);
                        String picked=screen.flagAt(fx+visible/2.0,screen.panelTop()+Math.round(125*scale));
                        if(!countries[i].equals(picked))throw new AssertionError("Visible claimant area picked "+picked+" instead of "+countries[i]);
                    }
                });
                if(index==3&&size[0]==2560) {
                    for(int[] cell:new int[][]{{110,209},{80,244},{78,197},{420,550},{382,550},{345,550}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(MapDetailScreen)client.gui.screen();
                            double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                            return new double[]{cell[0]*ratio*gui,(screen.panelTop()+cell[1]*ratio)*gui};
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                        context.takeScreenshot("hoi-occupation-"+(cell[1]==209?"resistance":cell[1]==244?"compliance":cell[1]==197?"target":cell[0]==420?"temperature":cell[0]==382?"weather":"surface")+"-tooltip-gui2");
                    }
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                }
                if(index==0&&size[0]==2560) {
                    context.runOnClient(client -> {
                        var decorated=dev.hoi.client.ui.KeywordIcons.decorate(net.minecraft.network.chat.Component.literal("이동: 95.0%"));
                        if(decorated.getString().charAt(0)<0xe000)throw new AssertionError("Movement requires the original text icon");
                    });
                    for(int[] cell:new int[][]{{443,410},{230,530},{419,550},{434,468},{409,185}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(MapDetailScreen)client.gui.screen();
                            double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                            return new double[]{cell[0]*ratio*gui,(screen.panelTop()+cell[1]*ratio)*gui};
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                        context.takeScreenshot("hoi-state-tooltip-"+cell[0]+"-gui2");
                    }
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                }
                if(index==2&&size[0]==2560) {
                    for(int[] cell:new int[][]{{244,124},{402,85},{36,264},{71,264},{106,264},{397,263}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(MapDetailScreen)client.gui.screen();
                            double ratio=screen.panelWidth()/433.0,gui=client.getWindow().getGuiScale();
                            return new double[]{cell[0]*ratio*gui,(screen.panelTop()+cell[1]*ratio)*gui};
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                        context.takeScreenshot("hoi-sea-tooltip-"+cell[0]+"-gui2");
                    }
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                }
                if(index==0&&size[0]==2560) {
                    var point=context.computeOnClient(client -> {
                        var screen=(MapDetailScreen)client.gui.screen();
                        double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                        return new double[]{190*ratio*gui,(screen.panelTop()+148*ratio)*gui};
                    });
                    context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                    context.takeScreenshot("hoi-map-detail-slots-tooltip-gui2");
                    var gaugePoint=context.computeOnClient(client -> {
                        var screen=(MapDetailScreen)client.gui.screen();
                        double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                        return new double[]{(149+58+5)*ratio*gui,(screen.panelTop()+175*ratio)*gui};
                    });
                    context.getInput().setCursorPos(gaugePoint[0],gaugePoint[1]);context.waitTicks(3);
                    context.takeScreenshot("hoi-map-detail-durability-tooltip-gui2");
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                    var fullDamage=view.tiles().stream().map(t->t.gauge()==null?t:new DialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),new DialogView.Gauge(1,"손상 100% 정렬 확인"))).toList();
                    open(context, view(view.title(),view.image(),fullDamage));
                    context.takeScreenshot("hoi-map-detail-full-damage-gui2");
                    int damageStep=0;
                    for(double fraction:new double[]{0,.25,.75,.25,0}) {
                        var damageTiles=view.tiles().stream().map(t->t.gauge()==null?t:new DialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),new DialogView.Gauge(fraction,"손상 비율 확인"))).toList();
                        context.runOnClient(client -> {
                            var screen=(MapDetailScreen)client.gui.screen();
                            screen.update(view(view.title(),view.image(),damageTiles).issued(screen.view().token(),screen.view().revision()+1));
                            if(screen.view().tiles().stream().filter(t->t.gauge()!=null).anyMatch(t->t.gauge().fraction()!=fraction))throw new AssertionError("Damage snapshot did not refresh");
                        });
                        context.waitTicks(3);
                        context.takeScreenshot("hoi-map-damage-step-"+damageStep+++"-gui2");
                    }
                }
            }
            index++;
        }
        context.runOnClient(client -> client.options.guiScale().set(2));
        context.getInput().resizeWindow(2560,1440);context.waitTicks(3);
        for(String terrain:List.of("ocean","shallow","deep","fjords"))for(String weather:List.of("clear","rain","storm","snow","blizzard")) {
            String art="map_detail/sea_terrain/"+terrain+"/"+weather;
            context.runOnClient(client -> {
                if(client.getResourceManager().getResource(Identifier.parse("hoi:textures/gui/"+art+".png")).isEmpty())throw new AssertionError("Missing sea variant: "+art);
            });
            var variant=new ArrayList<>(sea);
            variant.removeIf(t->Set.of("weather","weather_icon").contains(t.section()));
            variant.add(tile("weather","날씨",weather,art));
            if(!weather.equals("clear"))variant.add(new DialogView.Tile("weather_icon","날씨","","map_detail/weather_"+weather,"날씨 아이콘 툴팁"));
            open(context,view(terrain+" · "+weather,"map_detail/sea",variant));
            context.takeScreenshot("hoi-sea-"+terrain+"-"+weather+"-gui2");
        }
        context.setScreen(() -> null);
    }
    private static void open(ClientGameTestContext context, DialogView view) {
        context.setScreen(() -> null);
        context.runOnClient(client -> {
            DialogClient.receive(dev.hoi.protocol.DialogProtocol.Show.of(view, true));
            if (!(client.gui.screen() instanceof MapDetailScreen screen) || !screen.view().equals(view))
                throw new AssertionError("State and sea packets must open the current map detail UI");
        });
        context.waitTicks(3);
    }
    private static DialogView.Tile tile(String section, String name, String value, String icon) { return new DialogView.Tile(section, name, value, icon, ""); }
    private static DialogView view(String title, String image, List<DialogView.Tile> tiles) { return new DialogView(UUID.randomUUID().toString(), 1, DialogView.Kind.STATE, title, "", "", "", image, "", tiles, List.of()); }
}
