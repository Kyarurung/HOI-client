package dev.hoi.protocol;

/** Optional private snapshot values. Missing data is unknown, never an inferred zero. */
public record CountryHud(Double armyExperience, Double navyExperience, Double airExperience,
                         Double gdpBillions, Double debtBillions, Double worldTension,
                         boolean nuclearResearched, Long nuclearStockpile, NationalIndicators national) {
    public static final CountryHud UNKNOWN = new CountryHud(null, null, null, null, null, null, false, null);

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
    }

    /** Ratios are 0..1. Null means unavailable; zero is a recorded value. */
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
            for (Double value : new Double[]{stability, warSupport, energyRatio, supplyEfficiency, transportEfficiency, rulingPartySupport})
                if (value != null && (!Double.isFinite(value) || value < 0 || value > 1))
                    throw new IllegalArgumentException("Invalid national ratio");
            if (factories != null && factories < 0 || convoys != null && convoys < 0 || manpower != null && manpower < 0)
                throw new IllegalArgumentException("Invalid national count");
        }
    }
}
