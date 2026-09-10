package dev.hoi.protocol;


public record CountryHud(Double armyExperience, Double navyExperience, Double airExperience,
                         Double gdpBillions, Double debtBillions, Double worldTension,
                         boolean nuclearResearched, Long nuclearStockpile, NationalIndicators national,
                         java.util.Map<String, HudDetail> details) {
    public static final CountryHud UNKNOWN = new CountryHud(null, null, null, null, null, null, false, null);

    public CountryHud(Double armyExperience, Double navyExperience, Double airExperience,
                      Double gdpBillions, Double debtBillions, Double worldTension,
                      boolean nuclearResearched, Long nuclearStockpile, NationalIndicators national) {
        this(armyExperience, navyExperience, airExperience, gdpBillions, debtBillions, worldTension,
                nuclearResearched, nuclearStockpile, national, java.util.Map.of());
    }

    public CountryHud(Double armyExperience, Double navyExperience, Double airExperience,
                      Double gdpBillions, Double debtBillions, Double worldTension,
                      boolean nuclearResearched, Long nuclearStockpile) {
        this(armyExperience, navyExperience, airExperience, gdpBillions, debtBillions, worldTension,
                nuclearResearched, nuclearStockpile, NationalIndicators.UNKNOWN);
    }

    public CountryHud {
        for (Double value : new Double[]{armyExperience, navyExperience, airExperience, gdpBillions, debtBillions, worldTension})
            if (value != null && (!Double.isFinite(value) || value < 0)) throw new IllegalArgumentException("Invalid HUD value");
        if (worldTension != null && worldTension > 1 || nuclearStockpile != null && nuclearStockpile < 0)
            throw new IllegalArgumentException("Invalid HUD range");
        if (national == null) national = NationalIndicators.UNKNOWN;
        details = details == null ? java.util.Map.of() : java.util.Map.copyOf(details);
        if (details.size() > 17) throw new IllegalArgumentException("Too many HUD details");
        int characters = 0;
        for (var entry : details.entrySet()) {
            if (!entry.getKey().matches("[a-z_]{1,32}")) throw new IllegalArgumentException("Invalid HUD detail key");
            for (var row : entry.getValue().rows()) characters += row.label().length() + row.value().length();
        }
        if (characters > 24000) throw new IllegalArgumentException("HUD details exceed budget");
    }


    public record NationalIndicators(Double politicalPower, Double stability, Double warSupport,
                                     Long factories, Double energyRatio, Double fuel, Double supplies,
                                     Double supplyEfficiency, Long convoys, Double transportEfficiency,
                                     Double commandPower, Double rulingPartySupport, Long manpower) {
        public static final NationalIndicators UNKNOWN = new NationalIndicators(
                null, null, null, null, null, null, null, null, null, null, null, null, null);

        public NationalIndicators {
            if (politicalPower != null && !Double.isFinite(politicalPower))
                throw new IllegalArgumentException("Invalid political power");
            for (Double value : new Double[]{fuel, supplies, commandPower})
                if (value != null && (!Double.isFinite(value) || value < 0))
                    throw new IllegalArgumentException("Invalid national amount");
            for (Double value : new Double[]{stability, warSupport})
                if (value != null && (!Double.isFinite(value) || value < -1 || value > 1))
                    throw new IllegalArgumentException("Invalid national support ratio");
            for (Double value : new Double[]{energyRatio, supplyEfficiency, transportEfficiency, rulingPartySupport})
                if (value != null && (!Double.isFinite(value) || value < 0 || value > 1))
                    throw new IllegalArgumentException("Invalid national ratio");
            if (factories != null && factories < 0 || convoys != null && convoys < 0 || manpower != null && manpower < 0)
                throw new IllegalArgumentException("Invalid national count");
        }
    }
}
