package dev.hoi.protocol.industry;

import dev.hoi.protocol.country.CountryHud;
import java.util.List;

public record IndustryConstructionView(String session, long revision, String country, CountryHud hud, String selected,
        Summary summary, List<Building> buildings, List<Project> projects, String message, List<ConversionState> conversions, List<String> repairOrder, List<Repair> repairs) {
    public IndustryConstructionView(String session,long revision,String country,CountryHud hud,String selected,Summary summary,List<Building> buildings,List<Project> projects,String message) {
        this(session,revision,country,hud,selected,summary,buildings,projects,message,List.of());
    }
    public IndustryConstructionView(String session,long revision,String country,CountryHud hud,String selected,Summary summary,List<Building> buildings,List<Project> projects,String message,List<ConversionState> conversions) {
        this(session,revision,country,hud,selected,summary,buildings,projects,message,conversions,List.of(),List.of());
    }
    public record Repair(String id, String location, String building, String name, double level, double damage, int factories, double daily, int order, boolean pinned) {
        public Repair {
            text(id,128);text(location,96);text(building,32);text(name,128);
            if(factories<0||factories>20||order<1||order>4096)throw new IllegalArgumentException("Invalid repair");
            for(double n:new double[]{level,damage,daily})if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid repair work");
        }
    }
    public record ConversionState(String state,String name,int province,double x,double y,double z,int civilian,int military) {
        public ConversionState {
            text(state,128);text(name,128);
            if(province<1||civilian<0||civilian>20||military<0||military>20)throw new IllegalArgumentException("Invalid conversion state");
            for(double n:new double[]{x,y,z})if(!Double.isFinite(n)||Math.abs(n)>30000000)throw new IllegalArgumentException("Invalid conversion position");
        }
    }
    public record Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
            double speed, double energy, double demand, Integer tradeGoods, String speedCalculation, String energyCalculation, String consumerCalculation, String factoryCalculation,
            Integer nationalProjects,String projectsCalculation,String tradeCalculation,Integer owned,Integer fromTrade) {
        public Summary(int total,int consumer,int reserved,int used,int repair,int idle,int repairPriority,double speed,double energy,double demand,Integer tradeGoods,String speedCalculation,String energyCalculation,String consumerCalculation,String factoryCalculation,Integer nationalProjects,String projectsCalculation,String tradeCalculation) {
            this(total,consumer,reserved,used,repair,idle,repairPriority,speed,energy,demand,tradeGoods,speedCalculation,energyCalculation,consumerCalculation,factoryCalculation,nationalProjects,projectsCalculation,tradeCalculation,null,null);
        }
        public Summary(int total,int consumer,int reserved,int used,int repair,int idle,int repairPriority,double speed,double energy,double demand,Integer tradeGoods,String speedCalculation,String energyCalculation,String consumerCalculation,String factoryCalculation) {
            this(total,consumer,reserved,used,repair,idle,repairPriority,speed,energy,demand,tradeGoods,speedCalculation,energyCalculation,consumerCalculation,factoryCalculation,null,null,null);
        }
        public Summary(int total,int consumer,int reserved,int used,int repair,int idle,int repairPriority,double speed,double energy,double demand,Integer tradeGoods,String speedCalculation,String energyCalculation,String consumerCalculation) {
            this(total,consumer,reserved,used,repair,idle,repairPriority,speed,energy,demand,tradeGoods,speedCalculation,energyCalculation,consumerCalculation,null);
        }
        public Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
                double speed, double energy, double demand, Integer tradeGoods) {
            this(total,consumer,reserved,used,repair,idle,repairPriority,speed,energy,demand,tradeGoods,null,null,null);
        }
        public Summary(int total, int consumer, int reserved, int used, int repair, int idle, int repairPriority,
                double speed, double energy, double demand) {
            this(total, consumer, reserved, used, repair, idle, repairPriority, speed, energy, demand, null);
        }
        public Summary {
            for (String explanation : new String[]{speedCalculation,energyCalculation,consumerCalculation,factoryCalculation})
                if (explanation != null) text(explanation,8192);
            for(String explanation:new String[]{projectsCalculation,tradeCalculation})if(explanation!=null)text(explanation,32768);
            if(nationalProjects!=null&&nationalProjects<0)throw new IllegalArgumentException("Invalid national project factories");
            if(owned!=null&&owned<0||fromTrade!=null&&fromTrade<0)throw new IllegalArgumentException("Invalid factory sources");
            if (tradeGoods != null && tradeGoods < 0) throw new IllegalArgumentException("Invalid trade factories");
            for (int n : new int[]{total,consumer,reserved,used,repair,idle,repairPriority}) if(n<0) throw new IllegalArgumentException("Invalid factories");
            for (double n : new double[]{speed,energy,demand}) if(!Double.isFinite(n)||n<0) throw new IllegalArgumentException("Invalid economy");
        }
    }
    public record Building(String id, String name, String texture, boolean enabled, String reason, String tooltip) {
        public Building(String id,String name,String texture,boolean enabled,String reason) {this(id,name,texture,enabled,reason,null);}
        public Building { text(id,64);text(name,80);text(texture,128);text(reason,256);if(tooltip!=null)text(tooltip,4096); }
    }
    public record Project(String id, String state, String building, String name, int levels, int factories,
            double progress, double daily, double cost, String tooltip, String taskTooltip) {
        public Project(String id,String state,String building,String name,int levels,int factories,double progress,double daily,double cost,String tooltip) {
            this(id,state,building,name,levels,factories,progress,daily,cost,tooltip,null);
        }
        public Project(String id,String state,String building,String name,int levels,int factories,double progress,double daily,double cost) {
            this(id,state,building,name,levels,factories,progress,daily,cost,null);
        }
        public Project {
            text(id,128);text(state,128);text(building,64);text(name,128);
            if(tooltip!=null)text(tooltip,4096);
            if(taskTooltip!=null)text(taskTooltip,4096);
            if(levels<1||factories<0||factories>20)throw new IllegalArgumentException("Invalid project");
            for(double n:new double[]{progress,daily,cost})if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid work");
        }
    }
    public IndustryConstructionView {
        text(session,36);text(country,32);text(selected,64);text(message,512);
        if(revision<0)throw new IllegalArgumentException("Invalid revision");
        buildings=List.copyOf(buildings);projects=List.copyOf(projects);conversions=conversions==null?List.of():List.copyOf(conversions);
        repairOrder=repairOrder==null?List.of():List.copyOf(repairOrder);repairs=repairs==null?List.of():List.copyOf(repairs);
        for(String type:repairOrder)text(type,32);
        if(repairOrder.size()>32||repairs.size()>512)throw new IllegalArgumentException("Repair limit");
        if(conversions.size()>1024)throw new IllegalArgumentException("Conversion state limit");
        if(buildings.size()>32||projects.size()>128)throw new IllegalArgumentException("Construction limit");
    }
    static void text(String value,int max) { if(value==null||value.length()>max)throw new IllegalArgumentException("Invalid text"); }
}
