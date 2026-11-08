package dev.hoi.protocol.dialog;

import java.util.*;

public record DialogView(String token,long revision,Kind kind,String title,String subtitle,String body,
        String flag,String image,String sound,List<Tile> tiles,List<Choice> choices,String presentation) {
    public DialogView(String token,long revision,Kind kind,String title,String subtitle,String body,
            String flag,String image,String sound,List<Tile> tiles,List<Choice> choices) {
        this(token,revision,kind,title,subtitle,body,flag,image,sound,tiles,choices,"");
    }
    public enum Kind { STATE, DIPLOMACY, EVENT, GLOBAL_EVENT, SUPER_EVENT, POLITICS, COMMANDER }
    public record LeaderTrait(String icon,String name,String tooltip,Boolean active) {
        public LeaderTrait(String icon,String name,String tooltip) {this(icon,name,tooltip,null);}
        public LeaderTrait {asset(icon);text(name,160);text(tooltip,3000);}
    }
    public record Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips,List<LeaderTrait> traitDetails,String promotion,String advisor,String advisorIcon,String skillTooltip,String capacityTooltip,String assignTooltip,int advisorState) {
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips,List<LeaderTrait> traitDetails,String promotion,String advisor,String advisorIcon) {this(role,skill,abilities,capacity,assigned,traits,abilityTooltips,traitDetails,promotion,advisor,advisorIcon,"","","",0);}
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips,List<LeaderTrait> traitDetails,String promotion,String advisor) {this(role,skill,abilities,capacity,assigned,traits,abilityTooltips,traitDetails,promotion,advisor,"");}
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips,List<LeaderTrait> traitDetails,String promotion) {this(role,skill,abilities,capacity,assigned,traits,abilityTooltips,traitDetails,promotion,"");}
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips,List<LeaderTrait> traitDetails) {this(role,skill,abilities,capacity,assigned,traits,abilityTooltips,traitDetails,"");}
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits,List<String> abilityTooltips) {this(role,skill,abilities,capacity,assigned,traits,abilityTooltips,List.of());}
        public Leader(String role,int skill,List<Integer> abilities,int capacity,boolean assigned,List<String> traits) {this(role,skill,abilities,capacity,assigned,traits,List.of());}
        public Leader {
            text(role,32);abilities=List.copyOf(abilities);traits=List.copyOf(traits);
            if(skill<1||skill>10||abilities.size()!=4||abilities.stream().anyMatch(v->v==null||v<1||v>10)||capacity<0||capacity>10000||traits.size()>128)throw new IllegalArgumentException("Invalid commander row");
            traits.forEach(v->text(v,160));abilityTooltips=abilityTooltips==null?List.of():List.copyOf(abilityTooltips);
            if(!abilityTooltips.isEmpty()&&abilityTooltips.size()!=4)throw new IllegalArgumentException("Invalid commander ability tooltips");
            abilityTooltips.forEach(v->text(v,500));traitDetails=traitDetails==null?List.of():List.copyOf(traitDetails);
            if(traitDetails.size()>128)throw new IllegalArgumentException("Invalid commander trait details");
            promotion=promotion==null?"":promotion;text(promotion,600);advisor=advisor==null?"":advisor;text(advisor,600);advisorIcon=advisorIcon==null?"":advisorIcon;asset(advisorIcon);
            skillTooltip=skillTooltip==null?"":skillTooltip;text(skillTooltip,1200);capacityTooltip=capacityTooltip==null?"":capacityTooltip;text(capacityTooltip,600);assignTooltip=assignTooltip==null?"":assignTooltip;text(assignTooltip,600);if(advisorState<0||advisorState>2)throw new IllegalArgumentException("Invalid commander advisor state");
        }
        public Leader withTooltips(String skillTooltip,String capacityTooltip,String assignTooltip) {return new Leader(role,skill,abilities,capacity,assigned,traits,abilityTooltips,traitDetails,promotion,advisor,advisorIcon,skillTooltip,capacityTooltip,assignTooltip,advisorState);}
    }
    public record TraitNode(int row,int column,boolean terrain,List<String> parents,List<String> exclusive,boolean owned,boolean assignable,boolean available,String kind,int needed,List<String> bonuses) {
        public static final List<String> BONUSES=List.of("research","equipment","production","organization");
        public TraitNode(int row,int column,boolean terrain,List<String> parents,List<String> exclusive,boolean owned,boolean assignable,boolean available,String kind,int needed) {this(row,column,terrain,parents,exclusive,owned,assignable,available,kind,needed,List.of());}
        public TraitNode(int row,int column,boolean terrain,List<String> parents,List<String> exclusive,boolean owned,boolean assignable,boolean available,String kind) {this(row,column,terrain,parents,exclusive,owned,assignable,available,kind,0);}
        public TraitNode(int row,int column,boolean terrain,List<String> parents,List<String> exclusive,boolean owned,boolean assignable,boolean available) {this(row,column,terrain,parents,exclusive,owned,assignable,available,"");}
        public TraitNode {
            kind=kind==null?"":kind;text(kind,64);if(!kind.isEmpty()&&!kind.matches("[a-z0-9_]+"))throw new IllegalArgumentException("Invalid commander trait kind");
            parents=List.copyOf(parents);exclusive=List.copyOf(exclusive);bonuses=bonuses==null?List.of():List.copyOf(bonuses);
            if(!BONUSES.containsAll(bonuses)||new java.util.HashSet<>(bonuses).size()!=bonuses.size())throw new IllegalArgumentException("Invalid commander trait bonuses");
            if(row<-32||row>100||column<-32||column>32||parents.size()>32||exclusive.size()>32||needed<0||needed>parents.size())throw new IllegalArgumentException("Invalid commander trait node");
            for(String id:parents)text(id,80);for(String id:exclusive)text(id,80);
            if(available&&(owned||!assignable))throw new IllegalArgumentException("Invalid commander trait action");
        }
    }
    public record TacticCard(boolean attacking,boolean unlocked,boolean selected,List<String> effects,String phase) {
        public TacticCard {effects=List.copyOf(effects);if(effects.size()!=4&&effects.size()!=6)throw new IllegalArgumentException("Invalid tactic effects");effects.forEach(effect->text(effect,80));text(phase,160);}
    }
    public record Officer(String title,String unit,String unitIcon,String type,int unitLevel,double unitExperience,int medals,double experience,String experienceTooltip,String promotion,Integer insignia) {
        public Officer(String title,String unit,String unitIcon,String type,int unitLevel,double unitExperience,int medals,double experience,String experienceTooltip,String promotion) {this(title,unit,unitIcon,type,unitLevel,unitExperience,medals,experience,experienceTooltip,promotion,null);}
        public Officer {
            text(title,64);text(unit,160);asset(unitIcon);type=type==null?"":type;text(type,160);
            experienceTooltip=experienceTooltip==null?"":experienceTooltip;text(experienceTooltip,600);promotion=promotion==null?"":promotion;text(promotion,1200);
            if(unitLevel<0||unitLevel>4||!Double.isFinite(unitExperience)||unitExperience<0||unitExperience>1||medals<0||medals>1000||!Double.isFinite(experience)||experience<0||experience>100000||insignia!=null&&(insignia<0||insignia>80))throw new IllegalArgumentException("Invalid unit officer row");
        }
    }
    public record Tile(String section,String name,String value,String icon,String detail,Gauge gauge,String overlay,Leader leader,TraitNode traitNode,TacticCard tactic,Officer officer) {
        public Tile(String section,String name,String value,String icon,String detail,Gauge gauge,String overlay,Leader leader,TraitNode traitNode,TacticCard tactic) {this(section,name,value,icon,detail,gauge,overlay,leader,traitNode,tactic,null);}
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
    public DialogView {
        presentation=presentation==null?"":presentation;
        if(!Set.of("","research_complete","focus_complete","agency_upgrade_complete","doctrine_reward_complete","doctrine_milestone_complete","leader_event","minor_flavor","lend_lease_request","lend_lease_reply").contains(presentation)||Set.of("leader_event","minor_flavor").contains(presentation)&&kind!=Kind.EVENT||Set.of("lend_lease_request","lend_lease_reply").contains(presentation)&&kind!=Kind.DIPLOMACY)throw new IllegalArgumentException("Invalid dialog presentation");
        if(!token.isEmpty())UUID.fromString(token);
        if(revision<0)throw new IllegalArgumentException("Invalid dialog revision");
        Objects.requireNonNull(kind);text(title,240);text(subtitle,600);text(body,24000);asset(flag);asset(image);asset(sound);
        tiles=List.copyOf(tiles);choices=List.copyOf(choices);
        if(tiles.size()>160||choices.size()>(kind==Kind.COMMANDER?480:kind==Kind.POLITICS?160:24))throw new IllegalArgumentException("Dialog limit exceeded");
        if(choices.stream().map(Choice::id).distinct().count()!=choices.size())throw new IllegalArgumentException("Duplicate dialog choice");
    }
    public DialogView issued(String id,long version) {return new DialogView(id,version,kind,title,subtitle,body,flag,image,sound,tiles,choices,presentation);}
    public boolean completion() {return presentation.equals("research_complete")||presentation.equals("focus_complete")||presentation.equals("agency_upgrade_complete")||presentation.startsWith("doctrine_");}
    public boolean leaderEvent() {return presentation.equals("leader_event");}
    public boolean minorFlavor() {return presentation.equals("minor_flavor");}
    private static void asset(String value) {text(value,180);if(!value.isEmpty()&&!value.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid dialog image");}
    private static void text(String value,int limit) {if(value==null||value.length()>limit)throw new IllegalArgumentException("Dialog text limit exceeded");}
}
