package dev.hoi.client.screen.decision;

import dev.hoi.protocol.decision.DecisionPayloads;
import java.util.*;

final class DecisionSnapshot {
    enum Acceptance { IGNORED, STAGED, APPLIED, REVOKED }
    private DecisionPayloads.View view, loading;
    private final Map<Integer,DecisionPayloads.View> pages = new TreeMap<>();
    private List<DecisionPayloads.Row> rows = List.of();
    private List<DecisionPayloads.Category> categories = List.of();
    DecisionSnapshot(DecisionPayloads.View initial) {
        if(initial!=null)accept(initial);
    }
    DecisionPayloads.View view() { return view; }
    DecisionPayloads.View authority() { return loading==null?view:loading; }
    List<DecisionPayloads.Row> rows() { return rows; }
    List<DecisionPayloads.Category> categories() { return categories; }
    boolean loading() { return loading!=null; }
    int nextPage() {
        if(loading==null)return 0;
        for(int page=0;page<loading.pageCount();page++)if(!pages.containsKey(page))return page;
        return 0;
    }
    Acceptance acceptLegacy(DecisionPayloads.View next) {
        var current=authority();
        if(current!=null && !current.country().equals(next.country()))return Acceptance.REVOKED;
        view=next;rows=next.rows();categories=next.categories();loading=null;pages.clear();
        return Acceptance.APPLIED;
    }
    Acceptance accept(DecisionPayloads.View next) {
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
        var nextRows=new LinkedHashMap<String,DecisionPayloads.Row>();
        var nextCategories=new LinkedHashMap<String,DecisionPayloads.Category>();
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