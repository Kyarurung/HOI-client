package dev.hoi.protocol;

import java.util.*;

public record ProtocolDialogView(String token,long revision,Kind kind,String title,String subtitle,String body,
        String flag,String image,String sound,List<Tile> tiles,List<Choice> choices,String presentation) {
    public ProtocolDialogView(String token,long revision,Kind kind,String title,String subtitle,String body,
            String flag,String image,String sound,List<Tile> tiles,List<Choice> choices) {
        this(token,revision,kind,title,subtitle,body,flag,image,sound,tiles,choices,"");
    }
    public enum Kind { STATE, DIPLOMACY, EVENT, GLOBAL_EVENT, SUPER_EVENT, POLITICS, COMMANDER }
    public record LeaderTrait(String icon,String name,String tooltip,Boolean active) {
        public LeaderTrait(String icon,String name,String tooltip) {this(icon,name,tooltip,null);}
        public LeaderTrait {asset(icon);text(name,160);text(tooltip,3000);}
    }
    public record Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips,List<LeaderTrait> traitDetails) {
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips) {this(role,skill,abilities,capacity,assigned,traits,abilityTooltips,List.of());}
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits) {this(role,skill,abilities,capacity,assigned,traits,List.of());}
        public Leader {
            text(role,32);abilities=List.copyOf(abilities);traits=List.copyOf(traits);
            if(skill<1||skill>10||abilities.size()!=4||abilities.stream().anyMatch(v->v==null||v<1||v>10)||capacity<0||capacity>10000||traits.size()>128)throw new IllegalArgumentException("Invalid commander row");
            traits.forEach(v->text(v,160));abilityTooltips=abilityTooltips==null?List.of():List.copyOf(abilityTooltips);
            if(!abilityTooltips.isEmpty()&&abilityTooltips.size()!=4)throw new IllegalArgumentException("Invalid commander ability tooltips");
            abilityTooltips.forEach(v->text(v,500));traitDetails=traitDetails==null?List.of():List.copyOf(traitDetails);
            if(traitDetails.size()>128)throw new IllegalArgumentException("Invalid commander trait details");
        }
    }
    public record TraitNode(int row,int column,boolean terrain,List<String> parents,List<String> exclusive,boolean owned,boolean assignable,boolean available,String kind) {
        public TraitNode(int row,int column,boolean terrain,List<String> parents,List<String> exclusive,boolean owned,boolean assignable,boolean available) {this(row,column,terrain,parents,exclusive,owned,assignable,available,"");}
        public TraitNode {
            kind=kind==null?"":kind;text(kind,64);if(!kind.isEmpty()&&!kind.matches("[a-z0-9_]+"))throw new IllegalArgumentException("Invalid commander trait kind");
            parents=List.copyOf(parents);exclusive=List.copyOf(exclusive);
            if(row<-32||row>100||column<-32||column>32||parents.size()>32||exclusive.size()>32)throw new IllegalArgumentException("Invalid commander trait node");
            for(String id:parents)text(id,80);for(String id:exclusive)text(id,80);
            if(available&&(owned||!assignable))throw new IllegalArgumentException("Invalid commander trait action");
        }
    }
    public record TacticCard(boolean attacking,boolean unlocked,boolean selected,List<String> effects,String phase) {
        public TacticCard {effects=List.copyOf(effects);if(effects.size()!=4&&effects.size()!=6)throw new IllegalArgumentException("Invalid tactic effects");effects.forEach(effect->text(effect,80));text(phase,160);}
    }
    public record Tile(String section,String name,String value,String icon,String detail,Gauge gauge,String overlay,Leader leader,TraitNode traitNode,TacticCard tactic) {
        public Tile(String section,String name,String value,String icon,String detail,Gauge gauge,String overlay,Leader leader,TraitNode traitNode) {this(section,name,value,icon,detail,gauge,overlay,leader,traitNode,null);}
        public Tile(String section,String name,String value,String icon,String detail,Gauge gauge,String overlay,Leader leader) {this(section,name,value,icon,detail,gauge,overlay,leader,null);}
        public Tile(String section,String name,String value,String icon,String detail,Gauge gauge,String overlay) {this(section,name,value,icon,detail,gauge,overlay,null);}
        public Tile(String section,String name,String value,String icon,String detail) {this(section,name,value,icon,detail,null,"",null);}
        public Tile(String section,String name,String value,String icon,String detail,Gauge gauge) {this(section,name,value,icon,detail,gauge,"",null);}
        public Tile {overlay=overlay==null?"":overlay;text(section,80);text(name,160);text(value,160);asset(icon);asset(overlay);text(detail,3000);}
    }
    public record Gauge(double fraction,String tooltip) {
        public Gauge {
            if(!Double.isFinite(fraction)||fraction<0||fraction>1)throw new IllegalArgumentException("Invalid building damage");
            text(tooltip,240);
        }
    }
    public record Choice(String id,String label,String tooltip) {
        public Choice(String id,String label) {this(id,label,"");}
        public Choice {tooltip=tooltip==null?"":tooltip;text(id,160);text(label,240);text(tooltip,3000);}
    }
    public ProtocolDialogView {
        presentation=presentation==null?"":presentation;
        if(!Set.of("","research_complete","focus_complete","leader_event","minor_flavor").contains(presentation)||Set.of("leader_event","minor_flavor").contains(presentation)&&kind!=Kind.EVENT)throw new IllegalArgumentException("Invalid dialog presentation");
        if(!token.isEmpty())UUID.fromString(token);
        if(revision<0)throw new IllegalArgumentException("Invalid dialog revision");
        Objects.requireNonNull(kind);text(title,240);text(subtitle,600);text(body,24000);asset(flag);asset(image);asset(sound);
        tiles=List.copyOf(tiles);choices=List.copyOf(choices);
        if(tiles.size()>160||choices.size()>(kind==Kind.POLITICS||kind==Kind.COMMANDER?160:24))throw new IllegalArgumentException("Dialog limit exceeded");
        if(choices.stream().map(Choice::id).distinct().count()!=choices.size())throw new IllegalArgumentException("Duplicate dialog choice");
    }
    public ProtocolDialogView issued(String id,long version) {return new ProtocolDialogView(id,version,kind,title,subtitle,body,flag,image,sound,tiles,choices,presentation);}
    public boolean completion() {return presentation.equals("research_complete")||presentation.equals("focus_complete");}
    public boolean leaderEvent() {return presentation.equals("leader_event");}
    public boolean minorFlavor() {return presentation.equals("minor_flavor");}
    private static void asset(String value) {text(value,180);if(!value.isEmpty()&&!value.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid dialog image");}
    private static void text(String value,int limit) {if(value==null||value.length()>limit)throw new IllegalArgumentException("Dialog text limit exceeded");}
}
