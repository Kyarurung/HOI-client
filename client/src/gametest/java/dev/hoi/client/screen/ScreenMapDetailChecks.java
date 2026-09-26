package dev.hoi.client.screen;

import dev.hoi.protocol.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.Identifier;
import java.util.*;

final class ScreenMapDetailChecks {
    static void run(ClientGameTestContext context) {
        ProtocolDialogView tokyo;
        try(var input=ScreenMapDetailChecks.class.getResourceAsStream("/tokyo-slots-fixture.json")) {
            tokyo=new com.google.gson.Gson().fromJson(new java.io.InputStreamReader(Objects.requireNonNull(input),java.nio.charset.StandardCharsets.UTF_8),ProtocolDialogView.class);
        } catch(java.io.IOException e) {throw new java.io.UncheckedIOException(e);}
        context.getInput().resizeWindow(2560,1440);
        context.runOnClient(client->{
            client.options.guiScale().set(2);
            var slots=tokyo.tiles().stream().filter(t->t.section().equals("slots")).findFirst().orElseThrow();
            var styled=dev.hoi.client.ui.UiTooltips.styled(net.minecraft.network.chat.Component.literal(slots.detail()),false);
            var decorated=dev.hoi.client.ui.UiKeywordIcons.decorateLines(styled,false);
            if(!decorated.getString().contains("경제 침체")||decorated.getString().contains("£"))throw new AssertionError("Slot tooltip must resolve its original spirit icon and name");
            boolean red=decorated.visit((style,part)->part.contains("-20%")&&style.getColor()!=null&&style.getColor().getValue()==0xFF3232?Optional.of(true):Optional.empty(),net.minecraft.network.chat.Style.EMPTY).orElse(false);
            if(!red)throw new AssertionError("Slot reduction must retain the original red effect colour");
            if(decorated.getString().codePoints().noneMatch(c->c>=0xe000&&c<=0xf8ff))throw new AssertionError("The economic stagnation icon must render as a source glyph");
        });
        context.setScreen(()->new ScreenMapDetail(tokyo));context.waitTicks(3);
        var slotPoint=context.computeOnClient(client->{var screen=(ScreenMapDetail)client.gui.screen();double scale=screen.panelWidth()/463.0;return new double[]{2*(screen.panelLeft()+250*scale),2*(screen.panelTop()+148*scale)};});
        context.getInput().setCursorPos(slotPoint[0],slotPoint[1]);context.waitTicks(8);context.takeScreenshot("hoi-tokyo-slot-effects-gui2");

        context.runOnClient(client -> {
            var text=dev.hoi.client.ui.UiTooltips.sentences(net.minecraft.network.chat.Component.literal("§e집권 정당: §#3659D9더불어민주당§r"));
            if(!text.getString().equals("집권 정당: 더불어민주당"))throw new AssertionError("TFR RGB markup leaked into tooltip text");
            boolean blue=text.visit((style,part)->part.equals("더불어민주당")&&style.getColor()!=null&&style.getColor().getValue()==0x3659D9?Optional.of(true):Optional.empty(),net.minecraft.network.chat.Style.EMPTY).orElse(false);
            if(!blue)throw new AssertionError("TFR party name lost its original blue color");
            for(var entry:Map.of("§e서울특별시장:",0xFFBD00,"§f박원순",0xFFFFFF,"§#CD3737국민의힘",0xCD3737,"§#23CEFF조국혁신당",0x23CEFF,"§#FCA97D설명",0xFCA97D,"§#FB607FSeoul, my soul",0xFB607F).entrySet()) {
                var styled=dev.hoi.client.ui.UiTooltips.styled(net.minecraft.network.chat.Component.literal(entry.getKey()),false);
                boolean correct=styled.visit((style,part)->style.getColor()!=null&&style.getColor().getValue()==entry.getValue()?Optional.of(true):Optional.empty(),net.minecraft.network.chat.Style.EMPTY).orElse(false);
                if(!correct)throw new AssertionError("TFRKP tooltip color mismatch: "+entry.getKey());
            }

        });
        var own = new ArrayList<ProtocolDialogView.Tile>();
        own.add(tile("owner", "주 소유국", "대한민국", "country/kor/flag"));
        own.add(tile("claims", "타국의 주장", "조선민주주의인민공화국", "country/prk/flag"));
        own.add(new ProtocolDialogView.Tile("population", "인구", "3.41M", "hud/manpower", "§f§e모병 가능 인구§r: §e34.10K§r/§e31.00K§r\n§f§e민간인 인구§r: §e3.38M§r/§e3.07M§r\n\n§f£pol_idea 광범위한 징병: §e2.00%§r\n\n§f실질 총인력이 §c50%§r(으)로 변동되는 이유:\n§f  £pol_idea 일반적인 면제: §c-50.0%§r\n\n§f§e일간 성장§r: 0.00% (모병 가능: §a1§r, 민간인: §a10§r)\n\n§f기본: §e0.00%§r\n§f  £pol_idea 약화되는 고령화 문제: §c-75.0%§r\n\n§f이 주에 살고 있는 인구수입니다. 징병법에 의거해 일정 비율의 인구를 육, 해, 공군에서 복무할 인력으로 활용할 수 있습니다."));
        var buildings = List.of("infrastructure", "air_base", "anti_air", "radar", "railway");
        for (String id : buildings) own.add(tile("state", id, id.equals("radar") ? "2/2" : "5/5", "construction/" + id));
        own.add(new ProtocolDialogView.Tile("slots", "열린 슬롯", "10/25", "", "대도시: §a+5§r\n원자로: §a+1§r\n부산광역시: §a+1§r\n원자로: §a+40%§r"));
        for (int i = 0; i < 25; i++) own.add(tile("shared", i < 6 ? "민간공장" : i < 10 ? "빈 건설 슬롯" : "잠긴 건설 슬롯", "", i < 6 ? "construction/civilian_factory" : i < 10 ? "construction/empty_slot" : "map_detail/locked_slot"));
        int power=java.util.stream.IntStream.range(0,own.size()).filter(i->own.get(i).section().equals("shared")).findFirst().orElseThrow()+4;
        own.set(power,new ProtocolDialogView.Tile("shared","발전소","","construction/power_plant",
                "§e발전소§r (주 등급: §e3§r)\n발전소의 잠재 최대 레벨: §e4§r\n\n이 건물은 국가에 §a+6§r£resources_strip|7 을 제공하며, §c-4§r£resources_strip|1 및 §c-1§r£resources_strip|5 을(를) 소비합니다.\n\n§c이 주는 §e원자로§r나 §e에너지 농장§r을 건설할 수 없는 주입니다!§r"));
        for (String id : List.of("civilian_factory", "military_factory", "dockyard", "office_park", "refinery", "fuel_silo", "rocket_site", "nuclear_reactor", "power_plant", "energy_farm")) own.add(tile("limits", id, "1/4", "construction/" + id));
        for (String id : List.of("oil", "aluminum", "rubber", "tungsten", "iron", "chromium")) own.add(tile("resources", id, id.equals("oil") ? "-12" : id.equals("iron") ? "1379" : "123456", "production/resources/" + id));
        int oilCell=java.util.stream.IntStream.range(0,own.size()).filter(i->own.get(i).icon().equals("production/resources/oil")).findFirst().orElseThrow();
        own.set(oilCell,new ProtocolDialogView.Tile("resources","석유","-12","production/resources/oil","§e석유§r\n기본: §e0§r £resources_strip|1\n건물: §e-12§r £resources_strip|1\n   기반시설: §ex2.00§r\n   보급 경로: §ex1.10§r\n---------------------\n석유는 연료, 윤활유, 화학 제품 등으로 정제되는 주요 자원입니다. 이는 수송망에 동력을 공급하고 산업 활동을 지원하며, 차량, 항공기, 해상 운송 운영에 필수적입니다."));
        own.add(new ProtocolDialogView.Tile("energy", "에너지 효율", "158", "map_detail/energy", "에너지 효율: §e1.67§r\n  사무단지: §c-2%§r\n  사무단지: §c-2%§r\n공장 에너지 소비량: §c-15%§r\n공장 에너지 사용량: §e157.67§r\n군수공장 에너지 사용량: §e67.57§r\n해군 조선소 에너지 사용량: §e7.50§r\n민간공장 에너지 사용량: §e82.59§r"));
        own.add(new ProtocolDialogView.Tile("building_modifiers", "건설 주 변동치", "", "map_detail/building_modifiers", "건설 주 변동치\n§e민간공장:§r\n  강철 소비량: §c-3§r\n§e원자로:§r\n  주의 최대 공장: §a+1§r\n  주의 최대 공장: §a+40%§r\n  현지 건설 속도: §a+20%§r\n§e에너지 농장:§r\n  주의 최대 공장: §a+1§r\n  주의 최대 공장: §a+20%§r\n  현지 건설 속도: §a+10%§r\n§e사무단지:§r\n  현지 인력: §a+20%§r\n  현지 공장: §a+25%§r\n  지역 공장 에너지 소비량: §c+2%§r"));
        own.add(new ProtocolDialogView.Tile("terrain", "지역", "도시", "", "§e부산§r\n§e부대 효과:§r\n이동: §e95.0%§r\n공격: §e30.0%§r\n방어: §e106.0%§r\n이 지형 내에서의 이동 비용 §e×3.20§r\n\n§e지역 주 효과:§r\n사단 공격: §c-10%§r\n돌파: §c-15%§r\n참호 부설 속도: §c-30%§r\n추위로 인한 손실: §c+25%§r"));
        own.add(new ProtocolDialogView.Tile("province_status", "지역 상태", "", "map_detail/temperature_1", "§e보통 · 15.0°C§r"));
        own.add(new ProtocolDialogView.Tile("features", "부산광역시", "", "map_detail/art/gfx_modifiers_kor_busan", "주의 최대 공장: §a+1§r\n\n§e부산광역시장:§r\n[image:map_detail/art/gfx_oh_keo_don_portrait]\n[caption:§f오거돈§r]\n\n§e집권 정당: §#3659D9더불어민주당§r"));
        for (String id : List.of("hub", "fort", "coastal_fort", "port")) own.add(tile("province", id, id.equals("hub") ? "1/1" : id.equals("port") ? "6/10" : id.equals("fort") ? "5/5" : "4/5", "construction/" + id));
        var hidden = new ArrayList<ProtocolDialogView.Tile>();
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
        var sea = new ArrayList<ProtocolDialogView.Tile>();
        sea.add(tile("supremacy", "제해권", "48%", "hud/convoys"));
        sea.add(tile("weather", "날씨", "비", "map_detail/ocean_rain"));
        for (String section : List.of("naval", "air")) for (int i = 0; i < 4; i++) sea.add(tile(section, i == 0 ? "아군" : "정보 부족", i == 1 || i == 2 ? "0%" : "0", i == 0 ? "production/equipment/" + (section.equals("naval") ? "destroyer" : "fighter") : ""));
        sea.add(new ProtocolDialogView.Tile("marker","해상 지배","","","§e해상 지배§r\n한 지역이 완전히 아군 통제하에 있다고 간주되려면 지배율이 §e60%§r 이상이어야 합니다."));
        sea.add(new ProtocolDialogView.Tile("weather_icon","날씨","","map_detail/weather_rain","§e비§r\n\n임무 효율: §e80%§r\n공군 임무 효율: §e80%§r"));
        sea.add(new ProtocolDialogView.Tile("sea_terrain","바다","","","§e바다§r\n\n§e해상 지배: 48%§r"));
        sea.add(new ProtocolDialogView.Tile("access","접근","","","이용 단계: §a허용§r\n - 평소처럼 이 지역을 우리 §e함대§r와 §e상선§r의 경로로 사용할 것입니다."));
        sea.add(new ProtocolDialogView.Tile("access","접근","","","이용 단계: §e회피§r\n - 다른 경로가 있다면, 이 전역을 우리 §e함대§r와 §e상선§r의 경로로 사용하는 것을 §e피할§r 것입니다."));
        sea.add(new ProtocolDialogView.Tile("access","접근","","","이용 단계: §c봉쇄§r\n - 이 전역은 우리 §e상선 경로§r로 §c사용되지 않을§r 것입니다.\n - 우리 §e함대§r는 다른 경로가 없지 않은 한, 이 전역을 절대 §c통과하지 않을§r 것입니다."));
        sea.add(new ProtocolDialogView.Tile("logbook","침몰한 함선","","","침몰한 함선"));
        for(var entries:List.of(own,hidden)) for(int i=0;i<entries.size();i++) {
            var t=entries.get(i);
            if(!Set.of("state","province","shared").contains(t.section())||t.icon().endsWith("slot"))continue;
            boolean partial=t.icon().equals("construction/civilian_factory")&&i==10;
            String tooltip=t.section().equals("shared")?"레벨 내구도: §e"+(partial?"65.00%":"100.00%")+"§r":"손상 단계: 0/§e"+t.value().split("/")[0]+"§r";
            entries.set(i,new ProtocolDialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),new ProtocolDialogView.Gauge(partial?.35:0,tooltip)));
        }
        var stack=new dev.hoi.client.ui.UiFlagStack();
        if(!stack.update(List.of("PRK","JAP")).equals(List.of("PRK","JAP")))throw new AssertionError("Initial flag order");
        if(!stack.update(List.of("KOR","JAP","PRK")).equals(List.of("PRK","JAP","KOR")))throw new AssertionError("New claimant must be on top at the right");
        if(dev.hoi.client.ui.UiFlagStack.left(0,1,7,80,30)!=7
                ||dev.hoi.client.ui.UiFlagStack.left(0,2,7,80,30)!=7
                ||dev.hoi.client.ui.UiFlagStack.left(1,2,7,80,30)!=37)throw new AssertionError("Flags start on the left without overlap while space remains");
        int old=dev.hoi.client.ui.UiFlagStack.left(1,2,0,80,30);
        if(dev.hoi.client.ui.UiFlagStack.left(1,3,0,80,30)>=old||dev.hoi.client.ui.UiFlagStack.left(2,3,0,80,30)!=50)throw new AssertionError("Overflow shifts older flags left and leaves the latest flag on the right");
        own.set(3,new ProtocolDialogView.Tile("state","기반시설","4/5","construction/infrastructure","",new ProtocolDialogView.Gauge(0,"손상 단계: 0/4"),"map_detail/constructing"));
        int firstFactory=java.util.stream.IntStream.range(0,own.size()).filter(i->own.get(i).icon().equals("construction/civilian_factory")).findFirst().orElseThrow();
        var factory=own.get(firstFactory);
        own.set(firstFactory,new ProtocolDialogView.Tile(factory.section(),factory.name(),factory.value(),factory.icon(),factory.detail(),new ProtocolDialogView.Gauge(.35,"레벨 내구도: 65.00%"),"map_detail/repairing"));
        var occupied=new ArrayList<>(own);
        occupied.add(tile("controller","지배국","일본","country/jap/flag"));
        occupied.add(tile("claims","타국의 주장","일본","country/jap/flag"));
        occupied.add(tile("claims","타국의 주장","대한민국","country/kor/flag"));
        occupied.add(new ProtocolDialogView.Tile("resistance","저항도","82%","map_detail/resistance","저항 강도: §c82.34%§r\n\n저항도 목표: §e31%§r\n  기본값: §e35%§r\n  지배국이 평화로운 상태: §a-10%§r\n  낮은 안정도: §c18%§r\n  순응도: §a-12%§r\n\n저항도 감소: §a0.100%§r"));
        occupied.add(new ProtocolDialogView.Tile("compliance","순응도","24%","map_detail/compliance","순응 강도: §a24.56%§r\n\n순응도 증가: §a0.062%§r\n  기본값: §e0.075%§r\n  지배국이 평화로운 상태: §a10%§r\n추가 순응도 변화:\n  높은 순응도: §c-0.020%§r"));
        occupied.add(new ProtocolDialogView.Tile("resistance_target","저항도 목표","","map_detail/resistance_target","저항도 목표: §e31%§r",new ProtocolDialogView.Gauge(.31,"저항도 목표: §e31%§r")));
        occupied.removeIf(t->t.section().equals("province_status"));
        occupied.add(new ProtocolDialogView.Tile("province_status","기온","","map_detail/temperature_0","§e혹독한 추위 · -18.0°C§r\n공격: §c-20.0%§r\n돌파: §c-30.0%§r\n조직력 회복: §c-5.0%§r\n보급 효율: §c-15.0%§r\n사단 손실: §c+20.0%§r"));
        occupied.add(new ProtocolDialogView.Tile("province_weather","날씨","","map_detail/weather_blizzard","§e눈보라§r\n조직력 회복: §c-10.0%§r\n보급 소비량: §c+15.0%§r\n보급 효율: §c-25.0%§r"));
        occupied.add(new ProtocolDialogView.Tile("province_surface","지면","","map_detail/surface_deep_snow","§e깊이 쌓인 눈§r\n공격: §c-40.0%§r\n돌파: §c-30.0%§r\n사단 속도: §c-50.0%§r\n조직력 회복: §c-5.0%§r\n보급 효율: §c-40.0%§r"));
        int overlayIndex=0;
        for(int i=0;i<occupied.size();i++) {
            var t=occupied.get(i);
            if(t.section().equals("shared")&&overlayIndex<2)occupied.set(i,new ProtocolDialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),t.gauge(),"map_detail/"+(overlayIndex++==0?"constructing":"repairing")));
        }
        var seoul=new ArrayList<>(own);
        for(int i=0;i<seoul.size();i++) {
            var t=seoul.get(i);
            if(t.section().equals("features"))seoul.set(i,new ProtocolDialogView.Tile("features","서울특별시","","map_detail/art/gfx_modifiers_kor_seoul","§e서울특별시장:§r\n[image:map_detail/art/gfx_park_won_soon_portrait]\n[caption:§f박원순§r]\n\n§e집권 정당: §#3659D9더불어민주당§r"));
        }
        for(int i=0;i<own.size();i++)if(own.get(i).section().equals("terrain"))own.set(i,new ProtocolDialogView.Tile("terrain","지역","부산","","§e부산§r\n§e부대 효과:§r\n이동: §e95.0%§r\n공격: §e30.0%§r\n방어: §e106.0%§r\n전투 너비: §e96§r (추가 공격: §e32§r)\n적 공중 지원: §a-50%§r\n보급 물류 감소: §e0.0%§r\n이 지형 내에서의 이동 비용 §e×3.20§r\n\n----\n§#FCA97D대한민국 제2의 도시이자 최대 항구 도시인 부산은 남동쪽 해안에서 바다와 함께 숨 쉬는 역동적인 도시입니다. 한국 전쟁 당시 임시 수도로서 수많은 피난민을 끌어안으며 국가의 마지막 보루 역할을 했던 이곳은 강인한 생명력과 포용의 정신을 간직하고 있습니다.\n\n오늘날 부산은 세계적인 물류 허브이자 아시아 최대 영화 축제인 부산국제영화제(BIFF)가 열리는 '영화의 도시'로 국제적인 명성을 떨치고 있습니다. 해운대의 눈부신 해변과 광안대교의 야경, 자갈치 시장의 활기, 그리고 감천문화마을의 예술적인 풍경은 부산만의 다채로운 매력을 선사합니다. 거친 파도와 같은 열정과 드넓은 바다 같은 포용력을 지닌 부산은 사람과 문화, 산업이 어우러져 끊임없이 새로운 이야기를 만들어가는 곳입니다.§r\n\n§#FB607F돌아와요 부산항에..§r"));
        for(int i=0;i<seoul.size();i++)if(seoul.get(i).section().equals("terrain"))seoul.set(i,new ProtocolDialogView.Tile("terrain","지역","서울","","§e서울§r\n§e부대 효과:§r\n이동: §e95.0%§r\n공격: §e30.0%§r\n방어: §e106.0%§r\n전투 너비: §e96§r (추가 공격: §e32§r)\n적 공중 지원: §a-50%§r\n보급 물류 감소: §e0.0%§r\n이 지형 내에서의 이동 비용 §e×3.20§r\n\n----\n§#FCA97D600년 넘게 한 민족의 심장 역할을 해온 수도 서울은 유구한 역사와 눈부신 현대성이 공존하는 역동적인 대도시입니다. 조선 왕조의 수도였던 한양에서 시작하여 한국 전쟁의 폐허를 딛고 '한강의 기적'을 이뤄낸 서울은 불굴의 의지와 끊임없는 혁신의 상징이 되었습니다.\n\n오늘날 서울은 대한민국 정부와 삼성, 현대 등 세계적인 기업들의 본사가 자리한 정치, 경제의 중심지이자, K팝과 영화를 필두로 한 한류 문화를 통해 전 세계에 막대한 영향력을 미치는 문화 도시입니다. 경복궁과 같은 고궁의 장엄함과 미래지향적인 동대문디자인플라자(DDP)가 어우러져 있으며, 세계적인 수준의 대중교통 시스템은 도시의 활기 넘치는 리듬을 뒷받침합니다. 서울은 24시간 잠들지 않는 에너지와 무한한 가능성으로 가득 찬, 과거와 미래가 만나는 도시입니다.§r\n\n§#FB607FSeoul, my soul§r"));
        var fixtures = List.of(view("부산", "map_detail/terrain/urban", own), view("평안남도", "map_detail/terrain/plains", hidden), view("동해", "map_detail/sea", sea), view("도쿄", "map_detail/terrain/toyko", occupied), view("서울", "map_detail/terrain/seoul", seoul));
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
                    var screen = (ScreenMapDetail)client.gui.screen();
                    if (screen.panelLeft() != 0 || screen.panelTop() + screen.panelHeight() != screen.height) throw new AssertionError("Map detail anchors to the bottom left");
                    if (screen.panelTop() < dev.hoi.client.ui.UiMenuBar.height(screen.width)) throw new AssertionError("Detail cannot overlap the top menu");
                    for (var child : screen.children()) if (child instanceof Button b && (b.getX() < 0 || b.getY() < 0 || b.getRight() > screen.width || b.getBottom() > screen.height)) throw new AssertionError("Detail button escaped viewport");
                    if (!screen.allowsMovement()) throw new AssertionError("Map movement remains available");

                });
                context.takeScreenshot("hoi-map-detail-" + index + "-" + size[0] + "-gui2");
                if(index==3&&size[0]==2560)context.runOnClient(client -> {
                    var screen=(ScreenMapDetail)client.gui.screen();
                    double scale=screen.panelWidth()/463.0;
                    int start=(int)Math.round(20*scale),area=(int)Math.round(120*scale),fw=(int)Math.round(52*scale);
                    String[] countries={"PRK","JAP","KOR"};
                    for(int i=0;i<countries.length;i++) {
                        int fx=dev.hoi.client.ui.UiFlagStack.left(i,countries.length,start,area,fw);
                        int visible=dev.hoi.client.ui.UiFlagStack.visibleWidth(i,countries.length,start,area,fw);
                        String picked=screen.flagAt(fx+visible/2.0,screen.panelTop()+Math.round(125*scale));
                        if(!countries[i].equals(picked))throw new AssertionError("Visible claimant area picked "+picked+" instead of "+countries[i]);
                    }
                });
                if(index==3&&size[0]==2560) {
                    for(int[] cell:new int[][]{{110,209},{80,244},{78,197},{420,550},{382,550},{345,550}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(ScreenMapDetail)client.gui.screen();
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
                        var decorated=dev.hoi.client.ui.UiKeywordIcons.decorate(net.minecraft.network.chat.Component.literal("이동: 95.0%"));
                        if(decorated.getString().charAt(0)<0xe000)throw new AssertionError("Movement requires the original text icon");
                        var law=dev.hoi.client.ui.UiKeywordIcons.decorateLines(dev.hoi.client.ui.UiTooltips.sentences(net.minecraft.network.chat.Component.literal("£pol_idea 광범위한 징병: §e2.00%§r")));
                        if(law.getString().charAt(0)<0xe000||law.getString().contains("£"))throw new AssertionError("Population laws retain the original political idea icon");
                    });
                    for(int[] cell:new int[][]{{80,172},{35,302},{35,445},{443,410},{230,530},{419,550},{434,468},{409,185}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(ScreenMapDetail)client.gui.screen();
                            double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                            return new double[]{cell[0]*ratio*gui,(screen.panelTop()+cell[1]*ratio)*gui};
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                        context.takeScreenshot("hoi-state-tooltip-"+cell[0]+"-gui2");
                    }
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                }
                if(index==4&&size[0]==2560) {
                    for(int[] cell:new int[][]{{35,445},{230,530}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(ScreenMapDetail)client.gui.screen();
                            double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                            return new double[]{cell[0]*ratio*gui,(screen.panelTop()+cell[1]*ratio)*gui};
                        });
                        context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                        context.takeScreenshot("hoi-seoul-"+(cell[0]==35?"modifier":"terrain")+"-tooltip-gui2");
                    }
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                }
                if(index==2&&size[0]==2560) {
                    for(int[] cell:new int[][]{{244,124},{402,85},{36,264},{71,264},{106,264},{397,263}}) {
                        var point=context.computeOnClient(client -> {
                            var screen=(ScreenMapDetail)client.gui.screen();
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
                        var screen=(ScreenMapDetail)client.gui.screen();
                        double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                        return new double[]{190*ratio*gui,(screen.panelTop()+148*ratio)*gui};
                    });
                    context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                    context.takeScreenshot("hoi-map-detail-slots-tooltip-gui2");
                    var gaugePoint=context.computeOnClient(client -> {
                        var screen=(ScreenMapDetail)client.gui.screen();
                        double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                        return new double[]{(149+58+5)*ratio*gui,(screen.panelTop()+175*ratio)*gui};
                    });
                    context.getInput().setCursorPos(gaugePoint[0],gaugePoint[1]);context.waitTicks(3);
                    context.takeScreenshot("hoi-map-detail-durability-tooltip-gui2");
                    context.getInput().setCursorPos(1800,600);context.waitTicks(2);
                    var fullDamage=view.tiles().stream().map(t->t.gauge()==null?t:new ProtocolDialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),new ProtocolDialogView.Gauge(1,"손상 100% 정렬 확인"))).toList();
                    open(context, view(view.title(),view.image(),fullDamage));
                    context.takeScreenshot("hoi-map-detail-full-damage-gui2");
                    int damageStep=0;
                    for(double fraction:new double[]{0,.25,.75,.25,0}) {
                        var damageTiles=view.tiles().stream().map(t->t.gauge()==null?t:new ProtocolDialogView.Tile(t.section(),t.name(),t.value(),t.icon(),t.detail(),new ProtocolDialogView.Gauge(fraction,"손상 비율 확인"))).toList();
                        context.runOnClient(client -> {
                            var screen=(ScreenMapDetail)client.gui.screen();
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
            if(!weather.equals("clear"))variant.add(new ProtocolDialogView.Tile("weather_icon","날씨","","map_detail/weather_"+weather,"날씨 아이콘 툴팁"));
            open(context,view(terrain+" · "+weather,"map_detail/sea",variant));
            context.takeScreenshot("hoi-sea-"+terrain+"-"+weather+"-gui2");
        }
        for(String terrain:List.of("plains","hills","mountain","forest","seoul","pyongyang")) {
            var variant=new ArrayList<>(own);
            variant.removeIf(t->t.section().equals("terrain"));
            variant.add(new ProtocolDialogView.Tile("terrain","지역",terrain,"",terrain.equals("seoul")?"§e서울§r\n§e부대 효과:§r\n이동: §e95.0%§r\n공격: §e30.0%§r\n방어: §e106.0%§r\n전투 너비: §e96§r (추가 공격: §e32§r)\n적 공중 지원: §a-50%§r\n보급 물류 감소: §e0.0%§r\n이 지형 내에서의 이동 비용 §e×3.20§r\n\n§e지역 주 효과:§r\n사단 공격: §c-10%§r\n돌파: §c-15%§r\n참호 부설 속도: §c-30%§r\n추위로 인한 손실: §c+25%§r":terrain));
            variant.add(new ProtocolDialogView.Tile("features","주한미군기지","","map_detail/art/gfx_modifiers_kor_usfk","핵심 영토에서 사단 방어: §a+10%§r\n사단 조직력: §a+10%§r\n\n§e주한미군사령관:§r\n[image:map_detail/art/gfx_robert_abrams_portrait]\n[caption:§f로버트 B. 에이브럼스§r]\n\n§e주둔군: §b미합중국군§r"));
            open(context,view(terrain,"map_detail/terrain/"+terrain,variant));
            context.getInput().setCursorPos(1800,600);context.waitTicks(2);
            context.takeScreenshot("hoi-terrain-"+terrain+"-gui2");
            if(terrain.equals("seoul")) {
                context.runOnClient(client -> {
                    var caption=dev.hoi.client.ui.UiTooltips.styled(net.minecraft.network.chat.Component.literal("§f로버트 B. 에이브럼스§r"),false);
                    if(!caption.getString().equals("로버트 B. 에이브럼스"))throw new AssertionError("Portrait name must preserve initials without linebreaks");
                });
                for(int px:new int[]{35,95,230}) {
                    var point=context.computeOnClient(client -> {
                        var screen=(ScreenMapDetail)client.gui.screen();double ratio=screen.panelWidth()/463.0,gui=client.getWindow().getGuiScale();
                        return new double[]{px*ratio*gui,(screen.panelTop()+(px==230?530:445)*ratio)*gui};
                    });
                    context.getInput().setCursorPos(point[0],point[1]);context.waitTicks(3);
                    context.takeScreenshot("hoi-updated-state-tooltip-"+px+"-gui2");
                }
            }
        }
        context.setScreen(() -> null);
    }
    private static void open(ClientGameTestContext context, ProtocolDialogView view) {
        context.setScreen(() -> null);
        context.runOnClient(client -> {
            ScreenDialogCoordinator.receive(dev.hoi.protocol.ProtocolDialogPayloads.Show.of(view, true));
            if (!(client.gui.screen() instanceof ScreenMapDetail screen) || !screen.view().equals(view))
                throw new AssertionError("State and sea packets must open the current map detail UI");
        });
        context.waitTicks(3);
    }
    private static ProtocolDialogView.Tile tile(String section, String name, String value, String icon) { return new ProtocolDialogView.Tile(section, name, value, icon, ""); }
    private static ProtocolDialogView view(String title, String image, List<ProtocolDialogView.Tile> tiles) { return new ProtocolDialogView(UUID.randomUUID().toString(), 1, ProtocolDialogView.Kind.STATE, title, "", "", "", image, "", tiles, List.of()); }
}
