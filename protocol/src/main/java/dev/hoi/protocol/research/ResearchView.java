package dev.hoi.protocol.research;

import dev.hoi.protocol.ProtocolCampaignStyle;
import dev.hoi.protocol.country.CountryHud;
import dev.hoi.protocol.menu.MenuView;
import java.util.List;
import java.util.Objects;

public record ResearchView(String session, long revision, String country, String countryName,
                           long day, String date, String speed, List<Slot> slots, List<Tech> technologies, String message, CountryHud hud, Benefits benefits, List<DesignTeam> designTeams, java.util.Map<String,String> designTeamText) {
    public ResearchView(String session,long revision,String country,String countryName,long day,String date,
            String speed,List<Slot> slots,List<Tech> technologies,String message,CountryHud hud,Benefits benefits) {
        this(session,revision,country,countryName,day,date,speed,slots,technologies,message,hud,benefits,List.of(),java.util.Map.of());
    }
    public ResearchView(String session,long revision,String country,String countryName,long day,String date,
            String speed,List<Slot> slots,List<Tech> technologies,String message,CountryHud hud) {
        this(session,revision,country,countryName,day,date,speed,slots,technologies,message,hud,Benefits.NONE);
    }
    public ResearchView(String session, long revision, String country, String countryName, long day, String date,
                        String speed, List<Slot> slots, List<Tech> technologies, String message) {
        this(session, revision, country, countryName, day, date, speed, slots, technologies, message, CountryHud.UNKNOWN);
    }
    public ResearchView {
        if (benefits == null) benefits = Benefits.NONE;
        if (hud == null) hud = CountryHud.UNKNOWN;
        Objects.requireNonNull(session); Objects.requireNonNull(country); Objects.requireNonNull(countryName);
        Objects.requireNonNull(date); ProtocolCampaignStyle.requireSpeed(speed); Objects.requireNonNull(message);
        slots = List.copyOf(slots); technologies = List.copyOf(technologies); designTeams = designTeams == null ? List.of() : List.copyOf(designTeams);
        if (designTeams.size() > 64) throw new IllegalArgumentException("연구 화면 데이터 한도 초과");
        designTeamText = designTeamText == null ? java.util.Map.of() : java.util.Map.copyOf(designTeamText);
        if (designTeamText.size() > 32 || designTeamText.entrySet().stream().anyMatch(e -> !e.getKey().matches("[a-z_]{1,32}") || e.getValue().length() > 1000)) throw new IllegalArgumentException("Invalid design team text");
        var teams = new java.util.HashSet<String>();
        for (var team : designTeams) if (!teams.add(team.id())) throw new IllegalArgumentException("Duplicate design team");
        if (slots.size() > 16 || technologies.size() > 512) throw new IllegalArgumentException("연구 화면 데이터 한도 초과");
        if (day < 0 || revision < 0 || !country.isEmpty() && slots.isEmpty()) throw new IllegalArgumentException("Invalid research view");
        var ids = new java.util.HashSet<String>();
        for (var technology : technologies) if (!ids.add(technology.id())) throw new IllegalArgumentException("Duplicate technology");
        var assigned = new java.util.HashSet<String>();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.index() != i || slot.savedDays() < 0 || slot.savedDays() > 30 || slot.technology() == null
                    || !slot.technology().isEmpty() && (!ids.contains(slot.technology()) || !assigned.add(slot.technology()))
                    || !slot.designTeam().isEmpty() && (slot.technology().isEmpty() || !teams.contains(slot.designTeam())))
                throw new IllegalArgumentException("Invalid research slot");
        }
    }
    public record Benefits(double researchSpeed,List<Benefit> limited,String speedTooltip,String limitedTooltip) {
        public Benefits(double researchSpeed,List<Benefit> limited) { this(researchSpeed,limited,"",""); }
        public static final Benefits NONE=new Benefits(0,List.of());
        public Benefits {
            speedTooltip=speedTooltip==null?"":speedTooltip;
            limitedTooltip=limitedTooltip==null?"":limitedTooltip;
            if(speedTooltip.length()>16000||limitedTooltip.length()>16000) throw new IllegalArgumentException("Invalid research tooltips");
            limited=List.copyOf(limited);
            if(!Double.isFinite(researchSpeed)||limited.size()>128) throw new IllegalArgumentException("Invalid research benefits");
        }
        public int remainingUses() {return limited.stream().mapToInt(Benefit::uses).sum();}
    }
    public record Benefit(String name,int uses,double speed,double aheadYears,List<String> scope) {
        public Benefit {
            Objects.requireNonNull(name);scope=List.copyOf(scope);
            if(name.length()>240||uses<1||uses>100||!Double.isFinite(speed)||speed<0||!Double.isFinite(aheadYears)||aheadYears<0||scope.size()>640)
                throw new IllegalArgumentException("Invalid research benefit");
        }
    }
    public enum Status { LOCKED, AVAILABLE, ACTIVE, COMPLETED }
    public record Slot(int index, String technology, int savedDays, String designTeam, String yearTooltip) {
        public Slot(int index,String technology,int savedDays) { this(index,technology,savedDays,""); }
        public Slot(int index,String technology,int savedDays,String designTeam) { this(index,technology,savedDays,designTeam,""); }
        public Slot { designTeam = designTeam == null ? "" : designTeam; yearTooltip = yearTooltip == null ? "" : yearTooltip; if (designTeam.length() > 128 || yearTooltip.length() > 400) throw new IllegalArgumentException("Invalid research slot"); }
    }
    public record DesignTeam(String id, String name, String icon, int size, String type, List<MenuView.ManufacturerEquipment> equipment, double bonus, double dailyCost, double completionFundsPerDay,
            String technology, List<String> technologies, List<Double> rates, List<Integer> funds, String tooltip) {
        public DesignTeam {
            Objects.requireNonNull(id); Objects.requireNonNull(name); icon = icon == null ? "" : icon; technology = technology == null ? "" : technology; tooltip = tooltip == null ? "" : tooltip;
            technologies = technologies == null ? List.of() : List.copyOf(technologies); rates = rates == null ? List.of() : List.copyOf(rates); funds = funds == null ? List.of() : List.copyOf(funds);
            type = type == null ? "" : type; equipment = equipment == null ? List.of() : List.copyOf(equipment);
            if (rates.size() != technologies.size() || funds.size() != technologies.size() || rates.stream().anyMatch(r -> !Double.isFinite(r) || r <= 0) || funds.stream().anyMatch(f -> f < 0) || type.length() > 128 || equipment.size() > 32)
                throw new IllegalArgumentException("Invalid design team");
            if (!id.matches("[A-Za-z0-9_]{1,128}") || name.length() > 128 || icon.length() > 160 || technology.length() > 160 || tooltip.length() > 3000 || size < 0 || size > 10000
                    || !Double.isFinite(bonus) || !Double.isFinite(dailyCost) || dailyCost < 0 || !Double.isFinite(completionFundsPerDay) || completionFundsPerDay < 0 || technologies.size() > 512)
                throw new IllegalArgumentException("Invalid design team");
        }
    }
    public record StatLine(String name, String value) {
        public StatLine { Objects.requireNonNull(name); Objects.requireNonNull(value); if (name.length() > 160 || value.length() > 64) throw new IllegalArgumentException("Invalid research stat"); }
    }
    public record TerrainValue(String id, String name, Double movement, Double attack, Double defence) {
        public TerrainValue { Objects.requireNonNull(id); Objects.requireNonNull(name); if (!id.matches("[a-z_]{1,32}") || name.length() > 64 || java.util.stream.Stream.of(movement, attack, defence).anyMatch(v -> v != null && !Double.isFinite(v))) throw new IllegalArgumentException("Invalid research terrain"); }
    }
    public record Upgrade(String scope, List<StatLine> lines, List<TerrainValue> terrain, String scopeId) {
        public Upgrade(String scope, List<StatLine> lines, List<TerrainValue> terrain) { this(scope, lines, terrain, ""); }
        public Upgrade { Objects.requireNonNull(scope); scopeId = scopeId == null ? "" : scopeId; lines = List.copyOf(lines); terrain = terrain == null ? List.of() : List.copyOf(terrain); if (scope.length() > 160 || !scopeId.matches("[a-z0-9_]{0,64}") || lines.size() > 32 || terrain.size() > 16) throw new IllegalArgumentException("Invalid research upgrade"); }
    }
    public record UnitCard(String id, String name, List<StatLine> stats, List<TerrainValue> terrain) {
        public UnitCard { Objects.requireNonNull(id); Objects.requireNonNull(name); stats = List.copyOf(stats); terrain = List.copyOf(terrain); if (id.length() > 64 || name.length() > 160 || stats.size() > 16 || terrain.size() > 16) throw new IllegalArgumentException("Invalid research unit"); }
    }
    public record Tech(String id, String category, String name, int year, int tier, int baseDays,
                       double progress, double dailyRate, List<String> prerequisites,
                       List<String> effects, List<String> unlocks, Status status, Source source,List<String> tooltipOnlyEffects,List<Upgrade> upgrades,List<UnitCard> units) {
        public Tech(String id,String category,String name,int year,int tier,int baseDays,double progress,double dailyRate,
                List<String> prerequisites,List<String> effects,List<String> unlocks,Status status,Source source,List<String> tooltipOnlyEffects) {
            this(id,category,name,year,tier,baseDays,progress,dailyRate,prerequisites,effects,unlocks,status,source,tooltipOnlyEffects,List.of(),List.of());
        }
        public Tech(String id,String category,String name,int year,int tier,int baseDays,double progress,double dailyRate,
                List<String> prerequisites,List<String> effects,List<String> unlocks,Status status,Source source) {
            this(id,category,name,year,tier,baseDays,progress,dailyRate,prerequisites,effects,unlocks,status,source,List.of());
        }
        public Tech(String id,String category,String name,int year,int tier,int baseDays,double progress,double dailyRate,
                List<String> prerequisites,List<String> effects,List<String> unlocks,Status status) {
            this(id,category,name,year,tier,baseDays,progress,dailyRate,prerequisites,effects,unlocks,status,null);
        }
        public Tech {
            tooltipOnlyEffects=tooltipOnlyEffects==null?List.of():List.copyOf(tooltipOnlyEffects);upgrades=upgrades==null?List.of():List.copyOf(upgrades);units=units==null?List.of():List.copyOf(units);
            if(upgrades.size()>16||units.size()>8) throw new IllegalArgumentException("Invalid research sections");
            if(tooltipOnlyEffects.size()>128||!effects.containsAll(tooltipOnlyEffects)) throw new IllegalArgumentException("Invalid tooltip effects");
            Objects.requireNonNull(id); Objects.requireNonNull(category); Objects.requireNonNull(name);
            Objects.requireNonNull(status);
            prerequisites = List.copyOf(prerequisites); effects = List.copyOf(effects); unlocks = List.copyOf(unlocks);
            if (baseDays < 1 || !Double.isFinite(progress) || progress < 0
                    || !Double.isFinite(dailyRate) || dailyRate <= 0) throw new IllegalArgumentException("Invalid research progress");
        }
        public double fraction() { return status == Status.COMPLETED ? 1 : Math.clamp(progress / baseDays, 0, 1); }
        public long remainingDays(int savedDays) {
            return status == Status.COMPLETED ? 0 : Math.max(1, (long)Math.floor((baseDays - progress) / dailyRate - savedDays));
        }
    }
    public Tech technology(String id) {
        return technologies.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }
    public record Source(String id,String folder,int x,int y,boolean vertical,List<String> anyOf,List<String> excludes,
            List<String> conditions,List<String> unlockLabels,String description,List<String> deferredEffects,
            java.util.Map<String,String> localizedNames,String parentTechnology) {
        public Source(String id,String folder,int x,int y,boolean vertical,List<String> anyOf,List<String> excludes,
                List<String> conditions,List<String> unlockLabels,String description,List<String> deferredEffects,java.util.Map<String,String> localizedNames) {
            this(id,folder,x,y,vertical,anyOf,excludes,conditions,unlockLabels,description,deferredEffects,localizedNames,"");
        }
        public Source(String id,String folder,int x,int y,boolean vertical,List<String> anyOf,List<String> excludes,
                List<String> conditions,List<String> unlockLabels,String description,List<String> deferredEffects) {
            this(id,folder,x,y,vertical,anyOf,excludes,conditions,unlockLabels,description,deferredEffects,java.util.Map.of());
        }
        public Source {
            parentTechnology=parentTechnology==null?"":parentTechnology;
            if(parentTechnology.length()>256) throw new IllegalArgumentException("Invalid parent technology");
            localizedNames = localizedNames == null ? java.util.Map.of() : java.util.Map.copyOf(localizedNames);
            if (localizedNames.size() > 128 || localizedNames.entrySet().stream().anyMatch(e -> e.getKey().length() > 256 || e.getValue().length() > 256))
                throw new IllegalArgumentException("Invalid localized research names");
            Objects.requireNonNull(id); Objects.requireNonNull(folder);
            description=description==null?"":description;deferredEffects=deferredEffects==null?List.of():List.copyOf(deferredEffects);
            anyOf=List.copyOf(anyOf); excludes=List.copyOf(excludes); conditions=List.copyOf(conditions); unlockLabels=List.copyOf(unlockLabels);
            if(x < -1 || y < -1 || x > 20000 || y > 20000 || anyOf.size()>32 || excludes.size()>32
                    || conditions.size()>16 || unlockLabels.size()>128 || description.length()>8000 || deferredEffects.size()>128) throw new IllegalArgumentException("Invalid research source metadata");
        }
    }
    public int availableSlot(int preferred) {
        if(preferred>=0&&preferred<slots.size()&&slots.get(preferred).technology().isEmpty()) return preferred;
        return slots.stream().filter(s->s.technology().isEmpty()).mapToInt(Slot::index).findFirst().orElse(-1);
    }
}
