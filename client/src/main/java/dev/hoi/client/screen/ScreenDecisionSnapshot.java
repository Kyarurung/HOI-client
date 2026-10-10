package dev.hoi.client.screen;

import dev.hoi.protocol.ProtocolDecisionPayloads;
import java.util.*;

final class ScreenDecisionSnapshot {
    enum Acceptance { IGNORED, STAGED, APPLIED, REVOKED }
    private ProtocolDecisionPayloads.View view, loading;
    private final Map<Integer,ProtocolDecisionPayloads.View> pages = new TreeMap<>();
    private List<ProtocolDecisionPayloads.Row> rows = List.of();
    private List<ProtocolDecisionPayloads.Category> categories = List.of();
    ScreenDecisionSnapshot(ProtocolDecisionPayloads.View initial) {
        if(initial!=null)accept(initial);
    }
    ProtocolDecisionPayloads.View view() { return view; }
    ProtocolDecisionPayloads.View authority() { return loading==null?view:loading; }
    List<ProtocolDecisionPayloads.Row> rows() { return rows; }
    List<ProtocolDecisionPayloads.Category> categories() { return categories; }
    boolean loading() { return loading!=null; }
    int nextPage() {
        if(loading==null)return 0;
        for(int page=0;page<loading.pageCount();page++)if(!pages.containsKey(page))return page;
        return 0;
    }
    Acceptance acceptLegacy(ProtocolDecisionPayloads.View next) {
        var current=authority();
        if(current!=null && !current.country().equals(next.country()))return Acceptance.REVOKED;
        view=next;rows=next.rows();categories=next.categories();loading=null;pages.clear();
        return Acceptance.APPLIED;
    }
    Acceptance accept(ProtocolDecisionPayloads.View next) {
        var current=authority();
        if(current!=null && !current.country().equals(next.country()))return Acceptance.REVOKED;
        if(current!=null && !current.session().equals(next.session()))return Acceptance.IGNORED;
        if(view!=null && next.revision()<=view.revision())return Acceptance.IGNORED;
        if(loading!=null && next.revision()<loading.revision())return Acceptance.IGNORED;
        if(loading==null || next.revision()>loading.revision()) {
            if(next.page()!=0)return Acceptance.IGNORED;
            loading=next;pages.clear();
        }
        if(next.pageCount()!=loading.pageCount() || next.total()!=loading.total()
                || !next.message().equals(loading.message()) || !next.hud().equals(loading.hud())
                || pages.containsKey(next.page()))return Acceptance.IGNORED;
        pages.put(next.page(),next);
        if(pages.size()!=loading.pageCount())return Acceptance.STAGED;
        var nextRows=new LinkedHashMap<String,ProtocolDecisionPayloads.Row>();
        var nextCategories=new LinkedHashMap<String,ProtocolDecisionPayloads.Category>();
        for(var part:pages.values()) {
            for(var row:part.rows())if(nextRows.putIfAbsent(row.id(),row)!=null)return reject();
            for(var category:part.categories())if(nextCategories.putIfAbsent(category.id(),category)!=null)return reject();
        }
        if(nextRows.size()!=loading.total())return reject();
        view=loading;rows=List.copyOf(nextRows.values());categories=List.copyOf(nextCategories.values());
        loading=null;pages.clear();
        return Acceptance.APPLIED;
    }
    private Acceptance reject() {
        loading=null;pages.clear();
        return Acceptance.IGNORED;
    }
}