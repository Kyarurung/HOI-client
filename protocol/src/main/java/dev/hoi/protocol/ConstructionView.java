package dev.hoi.protocol;

import java.util.List;

/** Private, bounded construction snapshot. No country, cost or capacity is accepted from the client. */
public record ConstructionView(String session, long revision, String country, CountryHud hud, String selected,
        Summary summary, List<Building> buildings, List<Project> projects, String message) {
    public record Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
            double speed, double energy, double demand) {
        public Summary {
            for (int n : new int[]{total,consumer,reserved,used,repair,idle,repairPriority}) if(n<0) throw new IllegalArgumentException("Invalid factories");
            for (double n : new double[]{speed,energy,demand}) if(!Double.isFinite(n)||n<0) throw new IllegalArgumentException("Invalid economy");
        }
    }
    public record Building(String id, String name, String texture, boolean enabled, String reason) {
        public Building { text(id,64);text(name,80);text(texture,128);text(reason,256); }
    }
    public record Project(String id, String state, String building, String name, int levels, int factories,
            double progress, double daily, double cost) {
        public Project {
            text(id,128);text(state,128);text(building,64);text(name,128);
            if(levels<1||factories<0||factories>15)throw new IllegalArgumentException("Invalid project");
            for(double n:new double[]{progress,daily,cost})if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid work");
        }
    }
    public ConstructionView {
        text(session,36);text(country,32);text(selected,64);text(message,512);
        if(revision<0)throw new IllegalArgumentException("Invalid revision");
        buildings=List.copyOf(buildings);projects=List.copyOf(projects);
        if(buildings.size()>32||projects.size()>128)throw new IllegalArgumentException("Construction limit");
    }
    static void text(String value,int max) { if(value==null||value.length()>max)throw new IllegalArgumentException("Invalid text"); }
}
