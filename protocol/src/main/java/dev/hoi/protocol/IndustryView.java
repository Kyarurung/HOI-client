package dev.hoi.protocol;

import java.util.*;

/** Country-private production, trade, stockpile and recruitment data. All amounts are server-issued. */
public record IndustryView(String session, long revision, String country, CountryHud hud, Economy economy,
        List<Resource> resources, List<Modifier> modifiers, List<Equipment> equipment, List<Line> lines,
        List<Partner> partners, List<Trade> trades, List<Template> templates, List<Choice> battalions,
        List<Choice> companies, List<Choice> locations, List<Recruit> recruits, List<Deployed> deployed,
        Template draft, String message, NavalRepairs navalRepairs) {
    /** Additive, optional report: old servers omit it, rather than claiming an empty queue. */
    public IndustryView(String session, long revision, String country, CountryHud hud, Economy economy,
            List<Resource> resources, List<Modifier> modifiers, List<Equipment> equipment, List<Line> lines,
            List<Partner> partners, List<Trade> trades, List<Template> templates, List<Choice> battalions,
            List<Choice> companies, List<Choice> locations, List<Recruit> recruits, List<Deployed> deployed,
            Template draft, String message) {
        this(session, revision, country, hud, economy, resources, modifiers, equipment, lines, partners, trades,
                templates, battalions, companies, locations, recruits, deployed, draft, message, null);
    }
    public record NavalRepair(String id, String name, String texture, String port, double hp, double maxHp, String status) {
        public NavalRepair {
            text(id,128); text(name,256); text(texture,160); text(port,256); text(status,128);
            if (!Double.isFinite(hp) || !Double.isFinite(maxHp) || hp < 0 || maxHp <= 0 || hp > maxHp)
                throw new IllegalArgumentException("Invalid ship condition");
        }
    }
    public record NavalRepairs(int availableDockyards, List<NavalRepair> ships, Integer usedDockyards) {
        public NavalRepairs(int availableDockyards, List<NavalRepair> ships) {
            this(availableDockyards, ships, null);
        }
        public NavalRepairs {
            if (availableDockyards < 0) throw new IllegalArgumentException("Invalid repair dockyards");
            if (usedDockyards != null && (usedDockyards < 0 || usedDockyards > availableDockyards))
                throw new IllegalArgumentException("Invalid used repair dockyards");
            ships = bounded(ships,4096);
        }
    }
    public record Economy(int military, int dockyards, int civilian, int freeCivilian, int consumer,
            double fuel, Double gdp, Double debt, long manpower, double armyXp, double energy, double energyDemand) {}
    public record Resource(String id, String name, double extracted, double imported, double exported,
            double available, double demand) {}
    public record Modifier(String id, String name, double value, String detail) {}
    public record Equipment(String id, String name, String texture, String group, boolean naval, boolean unlocked,
            double cost, int factoryLimit, Map<String,Double> resources, long stockpile, long reserved, long deployed, long deficit,
            String replacement, String family) {
        /** Missing replacement information from older servers is unknown, not inferred from equipment names. */
        public Equipment(String id, String name, String texture, String group, boolean naval, boolean unlocked,
                double cost, int factoryLimit, Map<String,Double> resources, long stockpile, long reserved, long deployed, long deficit) {
            this(id,name,texture,group,naval,unlocked,cost,factoryLimit,resources,stockpile,reserved,deployed,deficit,null,null);
        }
        public boolean outdated() { return replacement != null && !replacement.isEmpty(); }
        public Equipment {
            text(id,128); text(name,128); text(texture,160); text(group,32);
            if (replacement != null) { text(replacement,128); if (replacement.equals(id)) throw new IllegalArgumentException("Self replacement"); }
            if (family != null) text(family,128);
            resources=boundedMap(resources,8);
            if (!Double.isFinite(cost) || cost <= 0 || factoryLimit < 1 || stockpile < 0 || reserved < 0 || deployed < 0 || deficit < 0)
                throw new IllegalArgumentException("Invalid equipment amounts");
        }
    }
    public record Line(String id, String equipment, int factories, int availableFactories, double efficiency,
            double daily, double progress, double shortage) {}
    public record Partner(String id, String name, Map<String,Double> exports, boolean route) {
        public Partner { text(id,32); text(name,128); exports=boundedMap(exports,8); }
    }
    public record Trade(String id, String partner, String resource, int factories, double delivered, boolean importing) {}
    public record Choice(String id, String name, String texture) {
        public Choice { text(id,128); text(name,256); text(texture,160); }
    }
    public record Stat(String name, String group, double value, String unit) {}
    public record Template(String id, String name, List<String> line, List<String> support, List<Stat> stats,
            Map<String,Long> equipment, double days, long manpower) {
        public Template {
            text(id,128); text(name,128);
            line=bounded(line,25); support=bounded(support,5); stats=bounded(stats,64); equipment=boundedMap(equipment,512);
            if (line.isEmpty() || !Double.isFinite(days) || days < 0 || manpower < 0)
                throw new IllegalArgumentException("Invalid division template");
        }
    }
    public record Recruit(String id, String template, String location, int priority, double progress,
            long manpower, Map<String,Long> equipment) {
        public Recruit { equipment=boundedMap(equipment,512); }
    }
    public record Deployed(String id, String name, String location, long manpower, Map<String,Long> equipment) {
        public Deployed { equipment=boundedMap(equipment,512); }
    }
    public IndustryView {
        text(session,36);text(country,32);text(message,512);
        if(revision<0)throw new IllegalArgumentException("Invalid revision");
        if(hud==null)hud=CountryHud.UNKNOWN;
        if(!country.isEmpty()&&economy==null)throw new IllegalArgumentException("Missing country economy");
        resources=bounded(resources,8);modifiers=bounded(modifiers,32);equipment=bounded(equipment,512);
        lines=bounded(lines,512);partners=bounded(partners,64);trades=bounded(trades,512);templates=bounded(templates,256);
        battalions=bounded(battalions,64);companies=bounded(companies,32);locations=bounded(locations,2048);
        recruits=bounded(recruits,512);deployed=bounded(deployed,4096);
    }
    public static void text(String s,int max) {if(s==null||s.length()>max)throw new IllegalArgumentException("Invalid industry text");}
    private static <T> List<T> bounded(List<T> values,int max) {
        if(values==null||values.size()>max)throw new IllegalArgumentException("Industry snapshot limit");return List.copyOf(values);
    }
    private static <K,V> Map<K,V> boundedMap(Map<K,V> values,int max) {
        if(values==null||values.size()>max)throw new IllegalArgumentException("Industry snapshot map limit");return Map.copyOf(values);
    }
    public static IndustryView revoked(String token,String message) {
        return new IndustryView(token,0,"",CountryHud.UNKNOWN,null,List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),List.of(),null,message);
    }
}
