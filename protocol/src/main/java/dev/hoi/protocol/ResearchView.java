package dev.hoi.protocol;

import java.util.List;
import java.util.Objects;


public record ResearchView(String session, long revision, String country, String countryName,
                           long day, String date, String speed, List<Slot> slots, List<Tech> technologies, String message, CountryHud hud, Benefits benefits) {
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
        Objects.requireNonNull(date); Objects.requireNonNull(speed); Objects.requireNonNull(message);
        slots = List.copyOf(slots); technologies = List.copyOf(technologies);
        if (slots.size() > 16 || technologies.size() > 512) throw new IllegalArgumentException("연구 화면 데이터 한도 초과");
        if (day < 0 || revision < 0 || !country.isEmpty() && slots.isEmpty()) throw new IllegalArgumentException("Invalid research view");
        var ids = new java.util.HashSet<String>();
        for (var technology : technologies) if (!ids.add(technology.id())) throw new IllegalArgumentException("Duplicate technology");
        var assigned = new java.util.HashSet<String>();
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            if (slot.index() != i || slot.savedDays() < 0 || slot.savedDays() > 30 || slot.technology() == null
                    || !slot.technology().isEmpty() && (!ids.contains(slot.technology()) || !assigned.add(slot.technology())))
                throw new IllegalArgumentException("Invalid research slot");
        }
    }
    public record Benefits(double researchSpeed,List<Benefit> limited) {
        public static final Benefits NONE=new Benefits(0,List.of());
        public Benefits {
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
    public record Slot(int index, String technology, int savedDays) {}
    public record Tech(String id, String category, String name, int year, int tier, int baseDays,
                       double progress, double dailyRate, List<String> prerequisites,
                       List<String> effects, List<String> unlocks, Status status, Source source) {
        public Tech(String id,String category,String name,int year,int tier,int baseDays,double progress,double dailyRate,
                List<String> prerequisites,List<String> effects,List<String> unlocks,Status status) {
            this(id,category,name,year,tier,baseDays,progress,dailyRate,prerequisites,effects,unlocks,status,null);
        }
        public Tech {
            Objects.requireNonNull(id); Objects.requireNonNull(category); Objects.requireNonNull(name);
            Objects.requireNonNull(status);
            prerequisites = List.copyOf(prerequisites); effects = List.copyOf(effects); unlocks = List.copyOf(unlocks);
            if (baseDays < 1 || !Double.isFinite(progress) || progress < 0
                    || !Double.isFinite(dailyRate) || dailyRate <= 0) throw new IllegalArgumentException("Invalid research progress");
        }
        public double fraction() { return status == Status.COMPLETED ? 1 : Math.clamp(progress / baseDays, 0, 1); }

        public long remainingDays(int savedDays) {
            return status == Status.COMPLETED ? 0 : Math.max(1, (long)Math.ceil((baseDays - progress) / dailyRate - savedDays));
        }
    }
    public Tech technology(String id) {
        return technologies.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
    }

    public record Source(String id,String folder,int x,int y,boolean vertical,List<String> anyOf,List<String> excludes,
            List<String> conditions,List<String> unlockLabels,String description,List<String> deferredEffects,
            java.util.Map<String,String> localizedNames) {
        public Source(String id,String folder,int x,int y,boolean vertical,List<String> anyOf,List<String> excludes,
                List<String> conditions,List<String> unlockLabels,String description,List<String> deferredEffects) {
            this(id,folder,x,y,vertical,anyOf,excludes,conditions,unlockLabels,description,deferredEffects,java.util.Map.of());
        }
        public Source {
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
