package dev.hoi.protocol;

import java.util.*;

/** Bounded presentation only. Choice IDs are issued by the authenticated server session. */
public record DialogView(String token,long revision,Kind kind,String title,String subtitle,String body,
        String flag,String image,String sound,List<Tile> tiles,List<Choice> choices,String presentation) {
    public DialogView(String token,long revision,Kind kind,String title,String subtitle,String body,
            String flag,String image,String sound,List<Tile> tiles,List<Choice> choices) {
        this(token,revision,kind,title,subtitle,body,flag,image,sound,tiles,choices,"");
    }
    public enum Kind { STATE, DIPLOMACY, EVENT, GLOBAL_EVENT, SUPER_EVENT, IDEOLOGIES }
    public record Tile(String section,String name,String value,String icon,String detail) {
        public Tile {text(section,80);text(name,160);text(value,160);asset(icon);text(detail,3000);}
    }
    public record Choice(String id,String label) {
        public Choice {text(id,160);text(label,240);}
    }
    public DialogView {
        presentation=presentation==null?"":presentation;
        if(!Set.of("","research_complete","focus_complete").contains(presentation))throw new IllegalArgumentException("Invalid dialog presentation");
        if(!token.isEmpty())UUID.fromString(token);
        if(revision<0)throw new IllegalArgumentException("Invalid dialog revision");
        Objects.requireNonNull(kind);text(title,240);text(subtitle,600);text(body,24000);asset(flag);asset(image);asset(sound);
        tiles=List.copyOf(tiles);choices=List.copyOf(choices);
        if(tiles.size()>160||choices.size()>24)throw new IllegalArgumentException("Dialog limit exceeded");
        if(choices.stream().map(Choice::id).distinct().count()!=choices.size())throw new IllegalArgumentException("Duplicate dialog choice");
    }
    public DialogView issued(String id,long version) {return new DialogView(id,version,kind,title,subtitle,body,flag,image,sound,tiles,choices,presentation);}
    public boolean completion() {return !presentation.isEmpty();}
    private static void asset(String value) {text(value,180);if(!value.isEmpty()&&!value.matches("[a-z0-9_/-]+"))throw new IllegalArgumentException("Invalid dialog image");}
    private static void text(String value,int limit) {if(value==null||value.length()>limit)throw new IllegalArgumentException("Dialog text limit exceeded");}
}
