package dev.hoi.protocol;

import java.util.*;

public record ProtocolDoctrineView(String session, long revision, String country, long progressionRevision,
        String folder, String branch, String experience, String grand, List<Folder> folders,
        List<Choice> grands, List<Track> tracks, String message, Map<String, String> uiText) {
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
        folders = List.copyOf(folders); grands = List.copyOf(grands); tracks = List.copyOf(tracks);
        if (folders.size() > 4 || grands.size() > 16 || tracks.size() > 64) throw new IllegalArgumentException("Doctrine view limit exceeded");
        unique(folders.stream().map(Folder::id).toList()); unique(grands.stream().map(Choice::id).toList()); unique(tracks.stream().map(Track::id).toList());
    }
    public record Folder(String id, String name, String texture, int frame) {
        public Folder { text(id, 32); text(name, 128); text(texture, 160); if (frame < 0 || frame > 3) throw new IllegalArgumentException("Invalid doctrine frame"); }
    }
    public record Choice(String id, String name, String description, String texture, String cost,
            String detail, boolean enabled, boolean selected, String advancedDetail) {
        public Choice(String id, String name, String description, String texture, String cost, String detail, boolean enabled, boolean selected) {
            this(id, name, description, texture, cost, detail, enabled, selected, "");
        }
        public Choice {
            advancedDetail = advancedDetail == null ? "" : advancedDetail;
            text(id, 96); text(name, 128); text(description, 8000); text(texture, 160); text(cost, 64); text(detail, 24000); text(advancedDetail, 24000);
        }
    }
    public record Reward(String id, String mastery, String detail, boolean unlocked) {
        public Reward { text(id, 96); text(mastery, 64); text(detail, 24000); }
    }
    public record Track(String id, String name, String background, String icon, String decoration,
            String selected, String nameplate, String mastery, String dailyMastery, String bank,
            List<Reward> rewards, List<Choice> candidates) {
        public Track {
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
