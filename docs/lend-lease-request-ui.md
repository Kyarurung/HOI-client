# 무기대여 요청 원본 UI 계약 조사

원본 GUI·GFX와 설치 한글 번역을 읽기 전용으로 대조하고 입력 10 계약을 클라이언트에 연결한다. 서버 조건·정보 공개 범위·장비 수량·승인·저장은 서버가 결정한다. 원본 조사와 실제 렌더·전송 검증을 구별한다.

원본 요청 편집기 `incoming_lend_lease`는 장비 종류를 고르는 창이다. 수량·매월 비율·일회 단추는 요청을 받은 쪽의 `popup_lend_lease_request_item`과 일반 제공 편집기 `diplomacy_lend_lease_entry`에 있다. 따라서 요청자가 수량·일회 방식을 직접 지정한다고 GUI만으로 확정할 수 없다. `INCOMING_LEND_LEASE_PROMPT`도 “매월 받을 장비를 선택하십시오.”다.

`materiel_grid`는 군수 목록이며 GUI 안에 무기대여 요청 전송 단추나 이벤트 처리기가 없다. 상세의 `request`도 button이 아닌 instantTextbox다. 군수의 특정 장비 클릭에서 요청 창으로 이동하는 엔진 동작, 상대국 열람 때의 칸 가시성, 첩보 부족 값의 구체적 표현은 사진·엔진 관측이 필요하다. 이를 일반 군수 목록과 요청 장비 목록이 같은 화면이라는 근거로 쓰지 않는다.

원본 경로 약칭: Base=`Hearts of Iron IV`, TFR=`3350890356`. 아래 줄 번호는 컨테이너/컨트롤 선언 줄이다. 전체 원시 필드·두 버전·원본 SHA256은 첨부 `source-audit.json`에 보존한다.

| 원본 요소 | TFR GUI:줄 | sprite | 필요한 서버 값 |
| --- | --- | --- | --- |
| `countrylogisticsview/countrylogisticsview` | `interface/countrylogisticsview.gui:3` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `countrylogisticsview/viewing_flag` | `interface/countrylogisticsview.gui:38` | `GFX_flag_small2` | 대상국 tag·국기 key; 시청국과 대상국 구별 |
| `countrylogisticsview/materiel_grid` | `interface/countrylogisticsview.gui:71` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `countrylogisticsview/headers` | `interface/countrylogisticsview.gui:89` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_info_window/logistics_info_window` | `interface/countrylogisticsview.gui:647` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_info_window/variants_grid` | `interface/countrylogisticsview.gui:873` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_overview_land_equipment_entry/logistics_overview_land_equipment_entry` | `interface/countrylogisticsview.gui:1076` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_overview_land_equipment_entry/equipment_type` | `interface/countrylogisticsview.gui:1105` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_overview_land_equipment_entry/balance` | `interface/countrylogisticsview.gui:1148` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `logistics_overview_land_equipment_entry/in_stock` | `interface/countrylogisticsview.gui:1158` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `logistics_info_equipment_entry/logistics_info_equipment_entry` | `interface/countrylogisticsview.gui:1547` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_info_equipment_entry/equipment_variant` | `interface/countrylogisticsview.gui:1558` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `logistics_info_equipment_entry/in_stock` | `interface/countrylogisticsview.gui:1569` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `logistics_info_equipment_entry/weekly_production` | `interface/countrylogisticsview.gui:1579` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `logistics_info_equipment_entry/request` | `interface/countrylogisticsview.gui:1589` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `logistics_info_equipment_entry/delete_button` | `interface/countrylogisticsview.gui:1599` | `GFX_NV_btn_delete` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/incoming_lend_lease` | `interface/countrydiplomacyview.gui:2510` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/infantry_button` | `interface/countrydiplomacyview.gui:2530` | `GFX_add_prod_inf_art_line` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/armor_button` | `interface/countrydiplomacyview.gui:2538` | `GFX_add_prod_armour_line` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/air_button` | `interface/countrydiplomacyview.gui:2546` | `GFX_add_prod_aircraft_line` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/naval_button` | `interface/countrydiplomacyview.gui:2554` | `GFX_add_prod_naval_line` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/diplo_reply` | `interface/countrydiplomacyview.gui:2579` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `incoming_lend_lease/lend_leases_grid` | `interface/countrydiplomacyview.gui:2620` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease/send_button` | `interface/countrydiplomacyview.gui:2630` | `GFX_button_123x34` | session/revision·요청/제안 id·enabled·이유·원본 문구·전송/수락/거절 결과; 중복 전송 방지 |
| `incoming_lend_lease/cancel_button` | `interface/countrydiplomacyview.gui:2642` | `GFX_button_123x34` | session/revision·요청/제안 id·enabled·이유·원본 문구·전송/수락/거절 결과; 중복 전송 방지 |
| `incoming_lend_lease/current_lease_info` | `interface/countrydiplomacyview.gui:2664` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `incoming_lend_lease/num_convoys` | `interface/countrydiplomacyview.gui:2679` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `incoming_lend_lease/autonomy_score_gain` | `interface/countrydiplomacyview.gui:2695` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `incoming_lend_lease_equipment_window/incoming_lend_lease_equipment_window` | `interface/countrydiplomacyview.gui:4069` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease_equipment_window/equipments_grid` | `interface/countrydiplomacyview.gui:4093` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease_equipment_window/no_equipment` | `interface/countrydiplomacyview.gui:4130` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `incoming_lend_lease_equipment_window/close` | `interface/countrydiplomacyview.gui:4140` | `GFX_closebutton` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_request_incoming_lend_lease_entry/Background` | `interface/countrydiplomacyview.gui:4496` | `GFX_request_lendlease_big_bg` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_request_incoming_lend_lease_entry/equipment_name` | `interface/countrydiplomacyview.gui:4517` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_request_incoming_lend_lease_entry/deal_state` | `interface/countrydiplomacyview.gui:4528` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `diplomacy_request_incoming_lend_lease_entry/cancel_button` | `interface/countrydiplomacyview.gui:4539` | `GFX_closebutton_small_2` | session/revision·요청/제안 id·enabled·이유·원본 문구·전송/수락/거절 결과; 중복 전송 방지 |
| `diplomacy_incoming_lend_lease_equipment_entry/Background` | `interface/countrydiplomacyview.gui:4555` | `GFX_lendlease_small_bg` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_incoming_lend_lease_equipment_entry/name` | `interface/countrydiplomacyview.gui:4584` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_incoming_lend_lease_equipment_entry/in_pool` | `interface/countrydiplomacyview.gui:4601` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `diplomacy_incoming_lend_lease_equipment_entry/producing` | `interface/countrydiplomacyview.gui:4619` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `diplomacy_incoming_lend_lease_equipment_entry/lendleasing` | `interface/countrydiplomacyview.gui:4629` | `텍스트/동적 자식` | 권한에 따른 값/범위/알 수 없음, 표시 문자열·툴팁; 미공개 수량을 0으로 바꾸지 않음 |
| `diplomacy_standard_popup_window/diplomacy_standard_popup_window` | `interface/popupwindow.gui:3` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_standard_popup_window/title` | `interface/popupwindow.gui:27` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `diplomacy_standard_popup_window/description` | `interface/popupwindow.gui:94` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `diplomacy_standard_popup_window/decline_button` | `interface/popupwindow.gui:113` | `GFX_button_123x34` | session/revision·요청/제안 id·enabled·이유·원본 문구·전송/수락/거절 결과; 중복 전송 방지 |
| `diplomacy_standard_popup_window/accept_button` | `interface/popupwindow.gui:122` | `GFX_button_123x34` | session/revision·요청/제안 id·enabled·이유·원본 문구·전송/수락/거절 결과; 중복 전송 방지 |
| `popup_lend_lease_request_item/popup_lend_lease_request_item` | `interface/popupwindow.gui:366` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `popup_lend_lease_request_item/number_button` | `interface/popupwindow.gui:405` | `GFX_small_button_71x26` | 받는 요청에 대한 제공 모드·수량/생산 비율·범위·허용 여부·초안; 요청자 수량 지정으로 추정하지 않음 |
| `popup_lend_lease_request_item/percentage_button` | `interface/popupwindow.gui:414` | `GFX_small_button_71x26` | 받는 요청에 대한 제공 모드·수량/생산 비율·범위·허용 여부·초안; 요청자 수량 지정으로 추정하지 않음 |
| `popup_lend_lease_request_item/once_button` | `interface/popupwindow.gui:423` | `GFX_small_button_71x26` | 받는 요청에 대한 제공 모드·수량/생산 비율·범위·허용 여부·초안; 요청자 수량 지정으로 추정하지 않음 |
| `popup_lend_lease_request_item/value_box` | `interface/popupwindow.gui:440` | `텍스트/동적 자식` | 받는 요청에 대한 제공 모드·수량/생산 비율·범위·허용 여부·초안; 요청자 수량 지정으로 추정하지 않음 |
| `popup_lend_lease_info_item/popup_lend_lease_info_item` | `interface/popupwindow.gui:561` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `popup_lend_lease_info_item/value_box` | `interface/popupwindow.gui:607` | `텍스트/동적 자식` | 받는 요청에 대한 제공 모드·수량/생산 비율·범위·허용 여부·초안; 요청자 수량 지정으로 추정하지 않음 |
| `popup_lend_lease_info_item/deal_description` | `interface/popupwindow.gui:617` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `diplomacy_incoming_lend_lease_popup_window/diplomacy_incoming_lend_lease_popup_window` | `interface/popupwindow.gui:640` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `diplomacy_incoming_lend_lease_popup_window/title` | `interface/popupwindow.gui:665` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |
| `diplomacy_incoming_lend_lease_popup_window/confirm_button` | `interface/popupwindow.gui:714` | `GFX_button_123x34` | session/revision·요청/제안 id·enabled·이유·원본 문구·전송/수락/거절 결과; 중복 전송 방지 |
| `diplomacy_incoming_lend_lease_popup_window/list` | `interface/popupwindow.gui:736` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `message_popup_window/message_popup_window` | `interface/popupwindow.gui:902` | `텍스트/동적 자식` | 장비 type/variant id·원본 이름·그림·분류·제작국·선택 상태·공개 허용 목록/툴팁·빈 목록 이유 |
| `message_popup_window/message` | `interface/popupwindow.gui:979` | `텍스트/동적 자식` | 국가명·원본 제목/본문·현재 계약·수송선·자치도·수락 판단; 미지원 효과 구별 |

군수 열기 단추는 TFR `interface/countrydiplomacyview.gui:877`의 `logistics_button`이며 `GFX_button_221x34`, `OPEN_COUNTRY_LOGISTICS`를 사용한다. 바닐라 대응은 같은 파일의 `logistics_button`이다. 알림 팝업에 어떤 템플릿을 붙이는지는 GUI 이름만으로 확정할 수 없으므로 `popup_lend_lease_request_item`의 받는 쪽 역할과 일반 수락/거절 팝업의 엔진 연결은 원본 사진으로 확인해야 한다.

| sprite | GFX 선언:줄 | 현재 팩 |
| --- | --- | --- |
| `GFX_add_all_button_32x32` | `interface\core.gfx:661` | 없음: 원본 `gfx/interface/buttons/add_all_button_32x32.dds` 가져오기 필요 |
| `GFX_button_94x31` | `interface\core.gfx:595` | `hoi:textures/gui/diplomacy/dialog/clear_button/frame_0.png` |
| `GFX_diplo_popup_bottom_large` | `interface\popupwindow.gfx:42` | `hoi:textures/gui/diplomacy/lease/popup_bottom.png` |
| `GFX_diplo_popup_header_bg_large` | `interface\popupwindow.gfx:52` | `hoi:textures/gui/diplomacy/lease/popup_header.png` |
| `GFX_diplo_war_large_icon` | `interface\popupwindow.gfx:3` | 없음: 원본 `gfx/interface/diplo_war_large_icon.dds` 가져오기 필요 |
| `GFX_fuel_logisctics_icon` | `interface\countryproductionlineview.gfx:449` | 없음: 원본 `gfx/interface/fuel_logistics_icon.dds` 가져오기 필요 |
| `GFX_fuel_types_icon` | `interface\countryproductionlineview.gfx:463` | 없음: 원본 `gfx/interface/logistic_fuel_types.dds` 가져오기 필요 |
| `GFX_leader_frame_0_6` | `interface\general_stuff.gfx:3` | 없음: 원본 `gfx/interface/leader_frame_0_6.dds` 가져오기 필요 |
| `GFX_lendlease_extended_bg` | `interface\countrydiplomacyview.gfx:180` | `hoi:textures/gui/diplomacy/lease/answer_row.png` |
| `GFX_linechart_frame` | `interface\countrylogisticsview.gfx:3` | 없음: 원본 `gfx/interface/linechart_frame.dds` 가져오기 필요 |
| `GFX_logistics_air_equipment_entry_bg` | `interface\countrylogisticsview.gfx:85` | 없음: 원본 `gfx/interface/logistics_air_equipment_entry_bg.dds` 가져오기 필요 |
| `GFX_logistics_fuelbar` | `interface\countrylogisticsview.gfx:65` | 없음: 원본 `gfx/interface/logistics_status_fuel_bar.dds, gfx/interface/logistics_status_fuel_bar_bg.dds` 가져오기 필요 |
| `GFX_logistics_info_equip_variant_entry` | `interface\countrylogisticsview.gfx:8` | 없음: 원본 `gfx/interface/logistics_info_equip_variant_entry.dds` 가져오기 필요 |
| `GFX_logistics_naval_equipment_entry_bg` | `interface\countrylogisticsview.gfx:80` | 없음: 원본 `gfx/interface/logistics_naval_equipment_entry_bg.dds` 가져오기 필요 |
| `GFX_logistics_overview_resources_bg` | `interface\countrylogisticsview.gfx:30` | 없음: 원본 `gfx/interface/logistics_overview_resources_bg.dds` 가져오기 필요 |
| `GFX_logistics_win_bottom` | `interface\countrylogisticsview.gfx:13` | 없음: 원본 `gfx/interface/logistics_win_bottom.dds` 가져오기 필요 |
| `GFX_popup_progressbar` | `interface\popupwindow.gfx:165` | 없음: 원본 `gfx/interface/deployment_entry_progressbar.dds, gfx/interface/deployment_entry_progressbar_bg.dds` 가져오기 필요 |
| `GFX_progress_marker` | `interface\countrylogisticsview.gfx:105` | 없음: 원본 `gfx/interface/progress_marker.dds` 가져오기 필요 |
| `GFX_request_lendlease_big_bg` | `interface\countrydiplomacyview.gfx:174` | `hoi:textures/gui/diplomacy/lease/request_row.png` |
| `GFX_strategicair_details_linechart_bg` | `interface\strategicairview.gfx:494` | 없음: 원본 `gfx/interface/strategicair/details_linechart_bg.dds` 가져오기 필요 |
| `GFX_tiled_invisible_bg` | `interface\core.gfx:202` | 없음: 원본 `gfx/interface/tiles/tiled_invisible_bg.dds` 가져오기 필요 |
| `GFX_trade_filter_bg` | `interface\countrytradeview.gfx:55` | 없음: 원본 `gfx/interface/tradeview/trade_filter_bg.dds` 가져오기 필요 |
| `GFX_zoom_in` | `interface\menubar.gfx:139` | `hoi:textures/gui/diplomacy/dialog/increase/frame_0.png` |
| `GFX_zoom_out` | `interface\menubar.gfx:145` | `hoi:textures/gui/diplomacy/dialog/decrease/frame_0.png` |

모든 참조 sprite의 GFX 선언을 찾았다. 별도 texturefile이 없는 국기·게이지·그래프 타입의 원시 필드도 JSON에 보존했다. 국기·장비·연결선 등 엔진 동적 타입을 정적 이미지 누락과 구별한다. 존재하는 팩 경로는 JSON의 `packKeys`로 연결했다. 다른 파일명·다른 sprite로 임의 대체하지 않는다.

한글 키 확인: 설치 번역 패치3411756715에서 `DIPLOMACY_INCOMING_LEND_LEASE_*`·`DIPLOMACY_MESSAGE_REPLYED_INCOMING_LEND_LEASE_*`·`OPEN_COUNTRY_LOGISTICS` 대응을 찾지 못했다. TFR 대응도 없는 항목은 Base `localisation/korean/diplomacy_l_korean.yml`의372–381(요청 제목·이유),121–131(답변·매월·매일·일회 분류),436–437(요청 안내)을 사용한다. GUI 숫자 칸은 원본 `VALUE|0`, 비율 칸은 `VALUE|0%`를 필드별로 구별한다. 전체 텍스트의 소수를 일괄 백분율로 바꾸지 않는다.

확정 범위는 GUI의 컨테이너·크기·위치·자식 타입·sprite와 원문 키다. 상대국 비축 공개 임계값, 클릭/더블클릭 방식, 요청 승인 후 계약 생성·수송·비용·국가별 제한은 입력10 서버 계약 및 원본 관측 전 확정하지 않는다. 텍스트 구분선·단축키 툴팁을 추가하지 않으며 원본 버튼과 간격을 유지한다. 운영 접속·멀티플레이·직접 효과음 청취는 이 소스 조사에서 수행하지 않았다.

## 입력 10 화면 연결과 재현

`DiplomacyCountryScreen`은 `request_lend_lease`와 `answer_lease:<id>`를 확인창 없이 인증된 행동 요청으로 보낸다. token·viewer·target이 일치하는 OPEN 응답만 편집기로 전환한다. 요청은 `incoming_lend_lease`와 `diplomacy_request_incoming_lend_lease_entry`의 배치·원본 그림을 쓰며 장비를 `LeaseLine(key, REQUEST, 0)`으로 선택한다. 요청에는 수량·방식 입력이 없다. 빈 분류는 비활성화하고 원본 `INCOMING_LEND_LEASE_*_BUTTON_DISABLED`를 표시한다. 기존 제안의 대응 키는 `LEND_LEASE_*_BUTTON_DISABLED`다.

외교 탭의 `lease_line:<id>:<n>`은 서버 `name`의 `무기대여 요청 · <n+1>/<줄 수>`를 줄 이름으로 그대로 표시한다. 요청 상세는 요청받은 나라 기준 장비 이름 한 줄·빈 줄·`§c무기대여 요청의 장비 내역입니다.§r`이고 수량·방식이 없다. 기존 제공 상세는 장비·수량·방식을 포함한다. 클라이언트가 상세 첫 줄을 줄 이름으로 바꾸거나 제공 내역과 요청 내역을 추정해 재작성하지 않는다.

응답은 `popupwindow.gui:640`의 `diplomacy_incoming_lend_lease_popup_window`(584×480), `popup_lend_lease_request_item`(장비 행)을 사용한다. 요청받은 장비의 수량을 0에서 입력하고 매월·일회 방식을 선택한다. 미지원 비율은 비활성이다. 모든 수량을 0으로 두는 빈 응답도 서버 `submitEnabled`가 허용하면 전송한다. PREVIEW 검증 전 제출과 응답 대기 중 중복 전송은 차단한다. 배경·머리·하단·행·국기 틀은 원본 픽셀 크기와 공통 반 배율을 유지한다.

`DialogLeaseNotification`은 `lend_lease_request`·`lend_lease_reply`의 별도 알림 렌더다. 원본 사진이 없는 현재 대조 기준은 `popupwindow.gui:902`의 `message_popup_window`(350×220, 우하단), title·message·close_button의 GUI 정의다. 특정 원작 장면과 이 템플릿의 대응을 사진으로 검증한 것은 아니다. 본문은 원본 message의 x21/y76·308×110 경계에서 줄마다 가운데 정렬한다. 확인 단추 y160은 본문 다음 렌더 층에 그리며, 단추에 덮이는 줄의 윗조각이 비치지 않도록 단추 위의 온전한 줄만 표시한다. 스크롤은 끝까지 읽을 수 있는 온전한 줄 수를 기준으로 계산한다. 넘치는 본문에만 원본 `core.gui/standardtext_slider`의 `GFX_scroll_track`·`GFX_scroll_up`·`GFX_scroll_down`·`GFX_scroll_drager`를 기존 팩에서 재사용한다. 화살표와 손잡이의 원본16×16 크기는 내용량에 따라 늘리지 않으며 휠·화살표·트랙 클릭·드래그로 이동한다. 송수신 국기는 원본 `GFX_flag_small2`의41×26·마스크·오버레이와52×38 틀을 각각(22,23)/(18,19), (287,23)/(282,19)에 쓴다. 답변의 앞 공백·색·줄은 서버 원문에서 유지하며 텍스트 구분선·단축키 툴팁은 추가하지 않는다. 요청 알림의 확인은 서버에 선택을 보낸 뒤 요청국 외교 화면을 열고, 그 화면에서 새로 발급된 `answer_lease:<id>`가 활성일 때만 응답 편집기를 요청한다.

보내기·요청의 선택 목록은 원본 `lend_leases_grid`의70 간격을 쓴다. 응답은 `entries`(15,77) 안 `list`(2,6)에서 시작하며73 간격이다. 장비 이름은 보내기/요청(8,4), 응답 `equipment_variants/equipment_name`(2,4)의 위쪽에 가운데 정렬한다. 서버 값이 없는 응답 `equipment_type_name`·`expand_button`은 표시하지 않는다. 장비 원본 그림은 별도 맞춤 칸으로 축소하지 않고 보내기/요청/목록의 중심(90,48), 응답의 중심(90,45)에 원본 크기의0.8배(해군0.6배)를 적용한다. 요청의 `equipment_icon`에는 원본 scale이 없으므로 사진 대기 중 보내기 배율을 쓰는 검토자 지정값이다. 응답 `amount_in_pool`(402,42; 폭80)은 오른쪽·위 맞춤, `deal_description`(305,34; 폭100)은 왼쪽·세로 가운데, `production_speed`(400,10; 폭120)는 가운데·위 맞춤이다. 해당 문구의 폭과 간격은 임의로 늘리지 않는다.

`GFX_small_button_71x26`은 Base `countrydeploymentview.gfx:133`의 `textSpriteType`이며 실제 DDS71×26에 `noOfFrames`가 없는 한 프레임이다. 가상의 상태 프레임을 추가하지 않고 원본 ButtonState 효과를 재사용한다. 선택한 방식은 DOWN·흰 글자, 미선택은 UP·회색 글자, 비활성은 DISABLED·회색 글자다. 군수 국기 툴팁은 `productionTip`→`Tooltips.drawExact`→공통 `Tooltips.width` 경로를 사용한다. 현재 공통 상한181GUI는 [공통 툴팁 규칙](ui.md#툴팁-공통-가로-폭)을 따르며 원본 `core.gui/ToolTip.maxWidth=400`과는 다르다. 이 화면에만 별도 폭을 추가하지 않는다.

군수 열기는 `countrydiplomacyview.gui:877/logistics_button`의 x296·하단48, `GFX_button_221x34` 자리에 별도로 표시한다. 목록에서 `open_logistics`를 제외하고 새 UUID의 `FOREIGN_LOGISTICS` 요청으로 상대국을 연다. 상대 군수는 서버 `viewing.country`의 국기와 군수 메뉴만 그린다. 원본 `countrylogisticsview.gui:39–50`의 `viewing_flag`는 문구가 아닌 `GFX_flag_small2`(x445/y13, 41×26)이며 `viewing_flag_border`는 `GFX_diplo_countrylist_flag_frame`(x440/y8, 52×38)이다. 기존 팩의 small2 마스크·오버레이와 `map_detail/flag_frame`을 재사용한다. `viewing.title`은 국기 위의 툴팁으로만 표시하고 직접 문구나 가로 스크롤을 만들지 않는다. 자기 나라 화면(`viewing == null`)에는 국기와 테두리가 없다. 이 세션의 전송은 최초 FOREIGN_LOGISTICS, REFRESH, CLOSE로 제한한다. 군수 수치는 서버가 준 equipment·lines를 사용한다.

팩은 `python scripts/build_pack.py`로 먼저 빌드한다. Java 25에서 `test runGameTest remapJar -PhoiTestSuite=diplomacy`와 `-PhoiTestSuite=industry`를 각각 실행한다. 두 실행 모두 `-PhoiServerSource=<고정 입력>/HOI -PhoiProtocolSource=<고정 입력>/HOI/protocol/src/main/java -PhoiResourcePack=<팩 ZIP> --console=plain --no-daemon`을 지정한다. `HOI_COMMANDER_PREVIEW`, `HOI_AGENCY_OPERATION_PREVIEW`, `HOI_DOCTRINE_MILESTONE_PREVIEW`는 같은 고정 입력의 `fixtures/commander-source-fixtures`다.

실제 요청/응답 캡처에는 `HOI_LEASE_REQUEST_PREVIEW=<fixtures-lease 폴더>`를 추가한다. 기존 제안 입력을 위한 `HOI_LEASE_PREVIEW`와 다른 변수다. `ScreenLeaseFixturesTest`와 `IndustryForeignLogisticsTest`가 `SHA256SUMS.txt`의 파일별 SHA256을 검사하고 요청 빈/선택, 응답 빈/매월100+일회500, 요청 알림/답변/빈 답변, PRK/JAP 군수, 외교 세 상태의 JSON 12개를 그대로 읽는다. 입력11 자료는 `PersistencePlayableCampaign`·`SetupWorldBootstrap`의 실제 캠페인에서 서버 코드가 생성한 응답이다. 서버 소켓으로 받은 응답이나 운영 접속 검증을 뜻하지 않는다. `HOI_LEASE_CAPTURE_STAGE=before|after`는 캡처 이름만 구별한다. 두 해상도 GUI2의 전체 상단 HUD·오른쪽 위와 긴 본문의 처음/끝을 캡처하며, 기존 합성 입력은 별도의 회귀 검증으로 구별한다.

원본 요청·응답·알림·상대국 군수 사진의 대조, 운영 접속·다중 사용자·직접 효과음 청취는 미수행이다. 사진이 없는 비교의 원본 열에는 실제 GUI 좌표·sprite와 출처를 기록한다. 무필터 전체는 그림 리뷰와 별도 시작 신호가 있을 때만 실행한다. 집중 검증 통과를 무필터 전체·실제 서버 기능 검증으로 표시하지 않는다.

팝업 스크롤 조작 회귀는 DialogLeaseNotificationTest에서 GUI2 두 해상도로 재현한다. 휠로 마지막 본문을 읽은 뒤 우클릭 화살표는 본문 픽셀을 유지하고, 왼클릭 위 화살표는 다른 줄을 표시하며, 트랙 드래그는 휠과 같은 마지막 본문 픽셀에 도달하는지 검사한다. 확인 단추의 중복 전송 차단과 서버 본문 공백 보존도 함께 검사한다. 이 합성 조작 시험과 실제 입력11 자료 캡처를 구별한다.
