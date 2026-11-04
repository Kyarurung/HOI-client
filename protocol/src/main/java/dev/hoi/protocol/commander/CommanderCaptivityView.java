package dev.hoi.protocol.commander;

import dev.hoi.protocol.dialog.DialogView;
import java.util.*;

public record CommanderCaptivityView(boolean ours,String query,String sort,boolean descending,int page,int total,int missing,int held,List<Row> rows) {
    public static final int PAGE_SIZE=4;
    public static final Set<String> SORTS=Set.of("name","skill","attack","defense","planning","logistics","capacity");
    public CommanderCaptivityView {Objects.requireNonNull(query);if(query.length()>64||!SORTS.contains(sort)||page<0||total<0||missing<0||held<0||total>16384||missing>16384||held>16384||page>Math.max(0,(total-1)/PAGE_SIZE))throw new IllegalArgumentException("Invalid captured commander page");rows=List.copyOf(rows);if(rows.size()>PAGE_SIZE||rows.size()!=Math.min(PAGE_SIZE,Math.max(0,total-page*PAGE_SIZE)))throw new IllegalArgumentException("Invalid captured commander rows");}
    public record Row(String id,String name,String originalCountry,String captor,String province,long sinceHour,String portrait,String detail,DialogView.Leader leader) {
        public Row {if(id==null||id.length()>128||name==null||name.length()>160||!originalCountry.matches("[A-Z0-9]{3}")||!captor.matches("[A-Z0-9]{3}")||province==null||province.length()>128||sinceHour<0||portrait==null||portrait.length()>256||detail==null||detail.length()>3000||leader==null)throw new IllegalArgumentException("Invalid captured commander row");}
    }
}
