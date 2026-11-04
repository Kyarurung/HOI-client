package dev.hoi.protocol.agency;

import java.util.List;

public record AgencyView(String session, long revision, String country, String name, String status,
        List<Item> items, String message, List<Candidate> candidates, List<Capital> capitals,
        List<Operative> operatives, List<Operation> operations, List<Network> networks, List<OperationOffer> offers, Integer operativeCapacity,
        List<Confirm> confirms, List<Cipher> ciphers) {
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message,
            List<Candidate> candidates, List<Capital> capitals, List<Operative> operatives, List<Operation> operations, List<Network> networks, List<OperationOffer> offers,
            Integer operativeCapacity, List<Confirm> confirms) {
        this(session, revision, country, name, status, items, message, candidates, capitals, operatives, operations, networks, offers, operativeCapacity, confirms, List.of());
    }
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message,
            List<Candidate> candidates, List<Capital> capitals, List<Operative> operatives, List<Operation> operations, List<Network> networks, List<OperationOffer> offers,
            Integer operativeCapacity) {
        this(session, revision, country, name, status, items, message, candidates, capitals, operatives, operations, networks, offers, operativeCapacity, List.of());
    }
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message,
            List<Candidate> candidates, List<Capital> capitals, List<Operative> operatives, List<Operation> operations, List<Network> networks, List<OperationOffer> offers) {
        this(session, revision, country, name, status, items, message, candidates, capitals, operatives, operations, networks, offers, null);
    }
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message) {
        this(session, revision, country, name, status, items, message, List.of());
    }
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message, List<Candidate> candidates) {
        this(session, revision, country, name, status, items, message, candidates, List.of());
    }
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message, List<Candidate> candidates, List<Capital> capitals) {
        this(session, revision, country, name, status, items, message, candidates, capitals, List.of(), List.of(), List.of());
    }
    public AgencyView(String session, long revision, String country, String name, String status, List<Item> items, String message, List<Candidate> candidates, List<Capital> capitals,
            List<Operative> operatives, List<Operation> operations, List<Network> networks) {
        this(session, revision, country, name, status, items, message, candidates, capitals, operatives, operations, networks, List.of());
    }
    public AgencyView {
        text(session, 36); text(country, 64); text(name, 128); text(status, 256); text(message, 512);
        if (operativeCapacity != null && (operativeCapacity < 0 || operativeCapacity > 64)) throw new IllegalArgumentException("Invalid operative capacity");
        if (revision < 0) throw new IllegalArgumentException("Invalid revision");
        items = List.copyOf(items); candidates = candidates == null ? List.of() : List.copyOf(candidates); capitals = capitals == null ? List.of() : List.copyOf(capitals);
        operatives = operatives == null ? List.of() : List.copyOf(operatives); operations = operations == null ? List.of() : List.copyOf(operations);
        networks = networks == null ? List.of() : List.copyOf(networks); offers = offers == null ? List.of() : List.copyOf(offers);
        confirms = confirms == null ? List.of() : List.copyOf(confirms); ciphers = ciphers == null ? List.of() : List.copyOf(ciphers);
        if (ciphers.size() > 256 || ciphers.stream().map(Cipher::country).distinct().count() != ciphers.size()) throw new IllegalArgumentException("Invalid agency ciphers");
        if (items.size() > 512 || items.stream().map(Item::id).distinct().count() != items.size() || candidates.size() > 8
                || capitals.size() > 512 || capitals.stream().map(Capital::country).distinct().count() != capitals.size()
                || !items.stream().map(Item::id).toList().containsAll(candidates.stream().map(Candidate::item).toList())
                || operatives.size() > 64 || operatives.stream().map(Operative::id).distinct().count() != operatives.size()
                || operations.size() > 64 || operations.stream().map(Operation::id).distinct().count() != operations.size()
                || networks.size() > 256 || networks.stream().map(Network::country).distinct().count() != networks.size()
                || offers.size() > 512 || offers.stream().map(OperationOffer::item).distinct().count() != offers.size()
                || confirms.size() > 320 || !java.util.stream.Stream.concat(items.stream().map(Item::id), ciphers.stream().flatMap(c -> java.util.stream.Stream.of(c.toggle(), c.activate(), c.delete())))
                        .toList().containsAll(confirms.stream().map(Confirm::item).toList()))
            throw new IllegalArgumentException("Invalid agency catalog");
    }
    public record Operative(String id, String name, String portrait, String status, String mission, String country, int province,
            double x, double z, String until, String experience, List<Trait> traits, String operation, String statusText,
            String codename, int level, String rank, double progress, String tooltip, String location, String activity, double activityProgress) {
        public static final List<String> ACTIVITIES = List.of("mission", "disabled", "captured", "operation");
        public Operative(String id, String name, String portrait, String status, String mission, String country, int province,
                double x, double z, String until, String experience, List<Trait> traits, String operation, String statusText,
                String codename, int level, String rank, double progress, String tooltip, String location) {
            this(id, name, portrait, status, mission, country, province, x, z, until, experience, traits, operation, statusText, codename, level, rank, progress, tooltip, location, "mission", 1);
        }
        public Operative(String id, String name, String portrait, String status, String mission, String country, int province,
                double x, double z, String until, String experience, List<Trait> traits, String operation, String statusText) {
            this(id, name, portrait, status, mission, country, province, x, z, until, experience, traits, operation, statusText, "", 0, "", -1, "", "");
        }
        public Operative(String id, String name, String portrait, String status, String mission, String country, int province,
                double x, double z, String until, String experience, List<Trait> traits, String operation) {
            this(id, name, portrait, status, mission, country, province, x, z, until, experience, traits, operation, "");
        }
        public Operative {
            statusText = statusText == null ? "" : statusText; codename = codename == null ? "" : codename; rank = rank == null ? "" : rank;
            tooltip = tooltip == null ? "" : tooltip; location = location == null ? "" : location; activity = activity == null || activity.isEmpty() ? "mission" : activity;
            if (!ACTIVITIES.contains(activity) || !Double.isFinite(activityProgress) || activityProgress < 0 || activityProgress > 1) throw new IllegalArgumentException("Invalid operative activity");
            text(id, 128); text(name, 128); text(portrait, 160); text(status, 64); text(mission, 128); text(country, 64);
            text(until, 64); text(experience, 32); text(operation, 128); text(statusText, 256); traits = List.copyOf(traits);
            text(codename, 64); text(rank, 64); text(tooltip, 2000); text(location, 128);
            if (province < 0 || !Double.isFinite(x) || !Double.isFinite(z) || traits.size() > 8 || level < 0 || level > 10
                    || !Double.isFinite(progress) || progress < -1 || progress > 1) throw new IllegalArgumentException("Invalid operative");
        }
    }
    public record Confirm(String item, String title, String body) {
        public Confirm { text(item, 128); text(title, 128); text(body, 1000); }
    }
    public record Preparation(int factories, int days, double progress, String label, String detail) {
        public static final Preparation NONE = new Preparation(0, 0, 1, "", "");
        public Preparation {
            label = label == null ? "" : label; detail = detail == null ? "" : detail; text(label, 128); text(detail, 1000);
            if (factories < 0 || factories > 1000 || days < 0 || days > 3650 || !Double.isFinite(progress) || progress < 0 || progress > 1)
                throw new IllegalArgumentException("Invalid operation preparation");
        }
    }
    public record Operation(String id, String item, String type, String name, String country, int province, double x, double z,
            String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
            String description, List<Phase> phases, String detail, Progress progress, String stage, boolean autoCommence, boolean autoRepeat,
            double risk, double bonus, String outlook, String reward, String outlookLabel, Preparation preparation, List<String> resume, List<Token> tokens) {
        public static final List<String> STAGES = List.of("COLLECTING", "PREPARED", "RUNNING", "COMPLETED");
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
                String description, List<Phase> phases, String detail, Progress progress, String stage, boolean autoCommence, boolean autoRepeat,
                double risk, double bonus, String outlook, String reward, String outlookLabel, Preparation preparation, List<String> resume) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, required, network, icon, mapIcon, description, phases, detail, progress,
                    stage, autoCommence, autoRepeat, risk, bonus, outlook, reward, outlookLabel, preparation, resume, List.of());
        }
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
                String description, List<Phase> phases, String detail, Progress progress, String stage, boolean autoCommence, boolean autoRepeat,
                double risk, double bonus, String outlook, String reward, String outlookLabel, Preparation preparation) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, required, network, icon, mapIcon, description, phases, detail, progress,
                    stage, autoCommence, autoRepeat, risk, bonus, outlook, reward, outlookLabel, preparation, List.of());
        }
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
                String description, List<Phase> phases, String detail, Progress progress, String stage, boolean autoCommence, boolean autoRepeat,
                double risk, double bonus, String outlook, String reward) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, required, network, icon, mapIcon, description, phases, detail, progress,
                    stage, autoCommence, autoRepeat, risk, bonus, outlook, reward, "", Preparation.NONE);
        }
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
                String description, List<Phase> phases, String detail, Progress progress) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, required, network, icon, mapIcon, description, phases, detail, progress,
                    "RUNNING", false, false, 0, 0, "", "");
        }
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
                String description, List<Phase> phases, String detail) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, required, network, icon, mapIcon, description, phases, detail, null);
        }
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives, int required, double network, String icon, String mapIcon,
                String description, List<Phase> phases) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, required, network, icon, mapIcon, description, phases, "");
        }
        public Operation(String id, String item, String type, String name, String country, int province, double x, double z,
                String completes, int days, int total, List<String> operatives) {
            this(id, item, type, name, country, province, x, z, completes, days, total, operatives, Math.max(1, operatives.size()), 0, "", "", "", List.of());
        }
        public Operation {
            text(id, 128); text(item, 128); text(type, 64); text(name, 128); text(country, 64); text(completes, 64);
            icon = icon == null ? "" : icon; mapIcon = mapIcon == null ? "" : mapIcon; description = description == null ? "" : description; detail = detail == null ? "" : detail;
            stage = stage == null || stage.isEmpty() ? "RUNNING" : stage; outlook = outlook == null ? "" : outlook; reward = reward == null ? "" : reward;
            outlookLabel = outlookLabel == null ? "" : outlookLabel; text(outlookLabel, 64); preparation = preparation == null ? Preparation.NONE : preparation;
            text(icon, 128); text(mapIcon, 128); text(description, 2000); text(detail, 1000); text(outlook, 2000); text(reward, 1000);
            if (!STAGES.contains(stage) || !Double.isFinite(risk) || risk < 0 || risk > 10 || !Double.isFinite(bonus) || bonus < 0 || bonus > 10)
                throw new IllegalArgumentException("Invalid agency operation stage");
            operatives = List.copyOf(operatives); operatives.forEach(o -> text(o, 128)); phases = phases == null ? List.of() : List.copyOf(phases);
            resume = resume == null ? List.of() : List.copyOf(resume); if (!operatives.containsAll(resume)) throw new IllegalArgumentException("Invalid agency operation resume");
            tokens = Token.list(tokens);
            if (province < 0 || !Double.isFinite(x) || !Double.isFinite(z) || total < 1 || days < 0 || days > total || operatives.size() > 8
                    || progress != null && progress.index() >= phases.size()
                    || required < 1 || required > 8 || !Double.isFinite(network) || network < 0 || network > 100 || phases.size() > 8)
                throw new IllegalArgumentException("Invalid agency operation");
        }
    }
    public record Progress(int index, int numerator, int denominator) {
        public Progress {
            if (index < 0 || index >= 8 || denominator < 1 || denominator > 1_000_000 || numerator < 0 || numerator > denominator)
                throw new IllegalArgumentException("Invalid operation phase progress");
        }
        public double fraction() { return numerator / (double) denominator; }
    }
    public record Phase(String id, String name, String description, String outcome, String picture, String icon) {
        public Phase { text(id, 128); text(name, 256); text(description, 2000); text(outcome, 2000); text(picture, 128); text(icon, 128); }
    }
    public record Token(String id, String name, String icon, boolean required) {
        public Token {
            text(id, 64); text(name, 128); text(icon, 128);
            if (!id.matches("[a-z0-9_]+") || icon.isEmpty()) throw new IllegalArgumentException("Invalid operation token");
        }
        static List<Token> list(List<Token> tokens) {
            tokens = tokens == null ? List.of() : List.copyOf(tokens);
            if (tokens.size() > 8 || tokens.stream().map(t -> t.required() + ":" + t.id()).distinct().count() != tokens.size()) throw new IllegalArgumentException("Invalid operation tokens");
            return tokens;
        }
    }
    public record OperationOffer(String item, String type, String name, String description, String icon, String mapIcon, String country,
            int required, double network, double currentNetwork, int days, int factories, boolean enabled, String reason,
            double risk, double bonus, String outlook, String reward, String outlookLabel, Preparation preparation, List<Phase> phases, List<Token> tokens) {
        public OperationOffer(String item, String type, String name, String description, String icon, String mapIcon, String country,
                int required, double network, double currentNetwork, int days, int factories, boolean enabled, String reason,
                double risk, double bonus, String outlook, String reward, String outlookLabel, Preparation preparation, List<Phase> phases) {
            this(item, type, name, description, icon, mapIcon, country, required, network, currentNetwork, days, factories, enabled, reason, risk, bonus, outlook, reward,
                    outlookLabel, preparation, phases, List.of());
        }
        public OperationOffer(String item, String type, String name, String description, String icon, String mapIcon, String country,
                int required, double network, double currentNetwork, int days, int factories, boolean enabled, String reason,
                double risk, double bonus, String outlook, String reward) {
            this(item, type, name, description, icon, mapIcon, country, required, network, currentNetwork, days, factories, enabled, reason, risk, bonus, outlook, reward,
                    "", Preparation.NONE, List.of());
        }
        public OperationOffer(String item, String type, String name, String description, String icon, String mapIcon, String country,
                int required, double network, double currentNetwork, int days, int factories, boolean enabled, String reason) {
            this(item, type, name, description, icon, mapIcon, country, required, network, currentNetwork, days, factories, enabled, reason, 0, 0, "", "");
        }
        public OperationOffer {
            outlook = outlook == null ? "" : outlook; reward = reward == null ? "" : reward; text(outlook, 2000); text(reward, 1000);
            outlookLabel = outlookLabel == null ? "" : outlookLabel; text(outlookLabel, 64); preparation = preparation == null ? Preparation.NONE : preparation;
            phases = phases == null ? List.of() : List.copyOf(phases); if (phases.size() > 8) throw new IllegalArgumentException("Invalid agency operation offer phases");
            tokens = Token.list(tokens);
            if (!Double.isFinite(risk) || risk < 0 || risk > 10 || !Double.isFinite(bonus) || bonus < 0 || bonus > 10)
                throw new IllegalArgumentException("Invalid agency operation outlook");
            text(item, 128); text(type, 64); text(name, 128); text(description, 2000); text(icon, 128); text(mapIcon, 128); text(country, 64); text(reason, 512);
            if (required < 1 || required > 8 || !Double.isFinite(network) || network < 0 || network > 100 || !Double.isFinite(currentNetwork)
                    || currentNetwork < 0 || currentNetwork > 100 || days < 1 || factories < 0 || factories > 1000)
                throw new IllegalArgumentException("Invalid agency operation offer");
        }
    }
    public record Network(String country, double strength, List<Integer> provinces, List<Segment> segments, List<Node> nodes) {
        public Network(String country, double strength, List<Integer> provinces, List<Segment> segments) { this(country, strength, provinces, segments, List.of()); }
        public Network(String country, double strength, List<Integer> provinces) { this(country, strength, provinces, List.of()); }
        public Network {
            text(country, 64); provinces = List.copyOf(provinces); segments = segments == null ? List.of() : List.copyOf(segments); nodes = nodes == null ? List.of() : List.copyOf(nodes);
            if (!Double.isFinite(strength) || strength < 0 || strength > 100 || provinces.size() > 4096 || provinces.stream().anyMatch(p -> p < 1) || segments.size() > 8192
                    || nodes.size() > 1024 || nodes.stream().map(Node::state).distinct().count() != nodes.size()
                    || !nodes.stream().map(Node::state).toList().containsAll(nodes.stream().map(Node::parent).filter(p -> !p.isEmpty()).toList()))
                throw new IllegalArgumentException("Invalid agency network");
        }
    }
    public record Node(String state, double x, double z, double strength, String parent) {
        public Node {
            text(state, 128); parent = parent == null ? "" : parent; text(parent, 128);
            if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(strength) || strength < 0 || strength > 100 || state.equals(parent))
                throw new IllegalArgumentException("Invalid agency network node");
        }
    }
    public record Segment(int fromX, int fromZ, int toX, int toZ) {
        public Segment { if (fromX != toX && fromZ != toZ || fromX == toX && fromZ == toZ) throw new IllegalArgumentException("Invalid agency network segment"); }
    }
    public record Candidate(String item, String name, String portrait, int level, String rank, String title, List<Trait> traits) {
        public Candidate {
            text(item, 128); text(name, 128); text(portrait, 160); text(rank, 64); text(title, 128); traits = List.copyOf(traits);
            if (level < 1 || level > 10 || traits.size() > 8) throw new IllegalArgumentException("Invalid operative candidate");
        }
    }
    public record Capital(String country, int province, double x, double z, String item, String tooltip) {
        public Capital(String country, int province, double x, double z) { this(country, province, x, z, "", ""); }
        public Capital {
            item = item == null ? "" : item; tooltip = tooltip == null ? "" : tooltip;
            text(country, 64); text(item, 128); text(tooltip, 1000);
            if (province < 1 || !Double.isFinite(x) || !Double.isFinite(z)) throw new IllegalArgumentException("Invalid agency capital");
        }
    }
    public record Cipher(String country, String name, String state, double progress, String status, String statusTooltip,
            String toggle, String toggleTooltip, String activate, boolean activateEnabled, String activateText, String activateTooltip,
            String delete, boolean deleteEnabled, String deleteTooltip) {
        public static final List<String> STATES = List.of("decrypting", "paused", "done", "active");
        public Cipher {
            text(country, 64); text(name, 128); text(status, 128); text(statusTooltip, 2000); text(toggle, 128); text(toggleTooltip, 1000);
            text(activate, 128); text(activateText, 128); text(activateTooltip, 4000); text(delete, 128); text(deleteTooltip, 1000);
            if (!STATES.contains(state) || !Double.isFinite(progress) || progress < 0 || progress > 1) throw new IllegalArgumentException("Invalid agency cipher");
        }
    }
    public record Trait(String id, String name, String icon, String tooltip) {
        public Trait { text(id, 64); text(name, 128); text(icon, 160); text(tooltip, 2000); }
    }
    public record Item(String id, String group, String title, String value, String detail, String texture,
            String button, boolean enabled, double progress, List<Parameter> parameters, int factories, Boolean factoriesAvailable) {
        public Item(String id, String group, String title, String value, String detail, String texture,
                String button, boolean enabled, double progress, List<Parameter> parameters, int factories) {
            this(id, group, title, value, detail, texture, button, enabled, progress, parameters, factories, null);
        }
        public Item(String id, String group, String title, String value, String detail, String texture,
                String button, boolean enabled, double progress, List<Parameter> parameters) {
            this(id, group, title, value, detail, texture, button, enabled, progress, parameters, 0);
        }
        public Item {
            text(id, 128); text(group, 32); text(title, 128); text(value, 128); text(detail, 2000);
            text(texture, 128); text(button, 64); parameters = List.copyOf(parameters);
            if (parameters.size() > 4 || !Double.isFinite(progress) || progress < -1 || progress > 1 || factories < 0 || factories > 1000)
                throw new IllegalArgumentException("Invalid agency item");
        }
    }
    public record Parameter(String label, List<Choice> choices) {
        public Parameter {
            text(label, 64); choices = List.copyOf(choices);
            if (choices.size() > 256 || choices.stream().map(Choice::value).distinct().count() != choices.size())
                throw new IllegalArgumentException("Invalid agency choices");
        }
    }
    public record Choice(String value, String label) {
        public Choice { text(value, 128); text(label, 128); }
    }
    static void text(String value, int limit) {
        if (value == null || value.length() > limit) throw new IllegalArgumentException("Agency data limit exceeded");
    }
}
