package dev.hoi.protocol;

import java.util.List;


public record ProtocolConstructionView(String session, long revision, String country, ProtocolCountryHud hud, String selected,
        Summary summary, List<Building> buildings, List<Project> projects, String message, List<ConversionState> conversions) {
    public ProtocolConstructionView(String session,long revision,String country,ProtocolCountryHud hud,String selected,Summary summary,List<Building> buildings,List<Project> projects,String message) {
        this(session,revision,country,hud,selected,summary,buildings,projects,message,List.of());
    }
    public record ConversionState(String state,String name,int province,double x,double y,double z,int civilian,int military) {
        public ConversionState {
            text(state,128);text(name,128);
            if(province<1||civilian<0||civilian>20||military<0||military>20)throw new IllegalArgumentException("Invalid conversion state");
            for(double n:new double[]{x,y,z})if(!Double.isFinite(n)||Math.abs(n)>30000000)throw new IllegalArgumentException("Invalid conversion position");
        }
    }
    public record Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
            double speed, double energy, double demand, Integer tradeGoods, String speedCalculation, String energyCalculation, String consumerCalculation) {
        public Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
                double speed, double energy, double demand, Integer tradeGoods) {
            this(total,consumer,reserved,used,repair,idle,repairPriority,speed,energy,demand,tradeGoods,null,null,null);
        }
        public Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
                double speed, double energy, double demand) {
            this(total, consumer, reserved, used, repair, idle, repairPriority, speed, energy, demand, null);
        }
        public Summary {
            for (String explanation : new String[]{speedCalculation,energyCalculation,consumerCalculation})
                if (explanation != null) text(explanation,8192);
            if (tradeGoods != null && tradeGoods < 0) throw new IllegalArgumentException("Invalid trade factories");
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
    public ProtocolConstructionView {
        text(session,36);text(country,32);text(selected,64);text(message,512);
        if(revision<0)throw new IllegalArgumentException("Invalid revision");
        buildings=List.copyOf(buildings);projects=List.copyOf(projects);conversions=conversions==null?List.of():List.copyOf(conversions);
        if(conversions.size()>1024)throw new IllegalArgumentException("Conversion state limit");
        if(buildings.size()>32||projects.size()>128)throw new IllegalArgumentException("Construction limit");
    }
    static void text(String value,int max) { if(value==null||value.length()>max)throw new IllegalArgumentException("Invalid text"); }
}
