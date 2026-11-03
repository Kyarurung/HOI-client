package dev.hoi.protocol;

import java.util.*;

public record ProtocolDoctrineView(String session, long revision, String country, long progressionRevision,
        String folder, String branch, String experience, String grand, List<Folder> folders,
        List<Choice> grands, List<Track> tracks, String message, Map<String, String> uiText, List<Track> previewTracks) {
    public ProtocolDoctrineView(String session, long revision, String country, long progressionRevision,
            String folder, String branch, String experience, String grand, List<Folder> folders,
            List<Choice> grands, List<Track> tracks, String message, Map<String, String> uiText) {
        this(session, revision, country, progressionRevision, folder, branch, experience, grand, folders, grands, tracks, message, uiText, List.of());
    }
    public ProtocolDoctrineView(String session, long revision, String country, long progressionRevision,
            String folder, String branch, String experience, String grand, List<Folder> folders,
            List<Choice> grands, List<Track> tracks, String message) {
        this(session, revision, country, progressionRevision, folder, branch, experience, grand, folders, grands, tracks, message, Map.of());
    }
    public ProtocolDoctrineView {
        text(session, 36); text(country, 3); text(folder, 32); text(branch, 8); text(experience, 64); text(grand, 96); text(message, 512);
        if (revision < 0 || progressionRevision < 0 || !country.isEmpty() && !country.matches("[A-Z0-9]{3}"))
            throw new IllegalArgumentException("Invalid doctrine revision/country");
        uiText = uiText == null ? Map.of() : Map.copyOf(uiText);
        if (uiText.size() > 32) throw new IllegalArgumentException("Doctrine labels limit exceeded");
        uiText.forEach((key, value) -> { text(key, 64); text(value, 8000); });
        folders = List.copyOf(folders); grands = List.copyOf(grands); tracks = List.copyOf(tracks); previewTracks = previewTracks == null ? List.of() : List.copyOf(previewTracks);
        if (folders.size() > 4 || grands.size() > 16 || tracks.size() > 64 || previewTracks.size() > 64) throw new IllegalArgumentException("Doctrine view limit exceeded");
        unique(folders.stream().map(Folder::id).toList()); unique(grands.stream().map(Choice::id).toList()); unique(tracks.stream().map(Track::id).toList()); unique(previewTracks.stream().map(Track::id).toList());
        var previewIds = previewTracks.stream().map(Track::id).collect(java.util.stream.Collectors.toSet());
        for (var grandChoice : grands) if (!previewIds.containsAll(grandChoice.previewTracks())) throw new IllegalArgumentException("Unknown doctrine preview track");
        for (var track : previewTracks) if (!track.selected().isEmpty() || track.candidates().stream().anyMatch(c -> c.selected() || !c.previewTracks().isEmpty() || c.rewards().stream().anyMatch(Reward::unlocked)))
            throw new IllegalArgumentException("Doctrine preview carries progress");
    }
    public record Folder(String id, String name, String texture, int frame) {
        public Folder { text(id, 32); text(name, 128); text(texture, 160); if (frame < 0 || frame > 3) throw new IllegalArgumentException("Invalid doctrine frame"); }
    }
    public record Choice(String id, String name, String description, String texture, String cost,
            String detail, boolean enabled, boolean selected, String advancedDetail, List<Reward> rewards, String assignDetail, boolean requirementsMet,
            List<Milestone> milestones, List<String> previewTracks) {
        public Choice(String id, String name, String description, String texture, String cost,
                String detail, boolean enabled, boolean selected, String advancedDetail, List<Reward> rewards, String assignDetail, boolean requirementsMet,
                List<Milestone> milestones) {
            this(id, name, description, texture, cost, detail, enabled, selected, advancedDetail, rewards, assignDetail, requirementsMet, milestones, List.of());
        }
        public Choice(String id, String name, String description, String texture, String cost,
                String detail, boolean enabled, boolean selected, String advancedDetail, List<Reward> rewards, String assignDetail, boolean requirementsMet) {
            this(id, name, description, texture, cost, detail, enabled, selected, advancedDetail, rewards, assignDetail, requirementsMet, List.of());
        }
        public Choice(String id, String name, String description, String texture, String cost,
                String detail, boolean enabled, boolean selected, String advancedDetail, List<Reward> rewards, String assignDetail) {
            this(id, name, description, texture, cost, detail, enabled, selected, advancedDetail, rewards, assignDetail, true);
        }
        public Choice(String id, String name, String description, String texture, String cost, String detail, boolean enabled, boolean selected) {
            this(id, name, description, texture, cost, detail, enabled, selected, "");
        }
        public Choice(String id, String name, String description, String texture, String cost, String detail, boolean enabled, boolean selected, String advancedDetail) {
            this(id, name, description, texture, cost, detail, enabled, selected, advancedDetail, List.of(), "");
        }
        public Choice {
            advancedDetail = advancedDetail == null ? "" : advancedDetail; assignDetail = assignDetail == null ? "" : assignDetail; rewards = rewards == null ? List.of() : List.copyOf(rewards);
            milestones = milestones == null ? List.of() : List.copyOf(milestones); previewTracks = previewTracks == null ? List.of() : List.copyOf(previewTracks);
            if (milestones.size() > 16 || previewTracks.size() > 64) throw new IllegalArgumentException("Doctrine milestone limit exceeded");
            previewTracks.forEach(track -> text(track, 96)); unique(previewTracks);
            text(id, 96); text(name, 128); text(description, 8000); text(texture, 160); text(cost, 64); text(detail, 24000); text(advancedDetail, 24000); text(assignDetail, 8000);
            if (rewards.size() > 256) throw new IllegalArgumentException("Doctrine choice limit exceeded");
            unique(rewards.stream().map(Reward::id).toList());
        }
    }
    public record Reward(String id, String mastery, String detail, boolean unlocked) {
        public Reward { text(id, 96); text(mastery, 64); text(detail, 24000); }
    }
    public record Milestone(String track, String name, String texture, boolean unlocked, String detail) {
        public static final Milestone NONE = new Milestone("", "", "", false, "");
        public Milestone { text(track, 96); text(name, 256); text(texture, 160); text(detail, 24000); }
    }
    public record Track(String id, String name, String background, String icon, String decoration,
            String selected, String nameplate, String mastery, String dailyMastery, String bank,
            List<Reward> rewards, List<Choice> candidates, Milestone milestone) {
        public Track(String id, String name, String background, String icon, String decoration,
                String selected, String nameplate, String mastery, String dailyMastery, String bank,
                List<Reward> rewards, List<Choice> candidates) {
            this(id, name, background, icon, decoration, selected, nameplate, mastery, dailyMastery, bank, rewards, candidates, Milestone.NONE);
        }
        public Track {
            milestone = milestone == null ? Milestone.NONE : milestone;
            text(id, 96); text(name, 128); text(background, 160); text(icon, 160); text(decoration, 160);
            text(selected, 96); text(nameplate, 128); text(mastery, 64); text(dailyMastery, 64); text(bank, 64);
            rewards = List.copyOf(rewards); candidates = List.copyOf(candidates);
            if (rewards.size() > 256 || candidates.size() > 128) throw new IllegalArgumentException("Doctrine track limit exceeded");
            unique(rewards.stream().map(Reward::id).toList()); unique(candidates.stream().map(Choice::id).toList());
        }
    }
    static void text(String value, int bound) { if (value == null || value.length() > bound) throw new IllegalArgumentException("Doctrine text limit exceeded"); }
    private static void unique(List<String> ids) { if (ids.stream().distinct().count() != ids.size()) throw new IllegalArgumentException("Duplicate doctrine identifier"); }
}
