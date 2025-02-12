import java.util.*;

public class GameConstants {
    public static final List<List<String>> MILL_CONDITIONS = List.of(
            // Horizontal
            List.of("a7", "d7", "g7"),
            List.of("b6", "d6", "f6"),
            List.of("c5", "d5", "e5"),
            List.of("a4", "b4", "c4"),
            List.of("e4", "f4", "g4"),
            List.of("c3", "d3", "e3"),
            List.of("b2", "d2", "f2"),
            List.of("a1", "d1", "g1"),
            // Vertical
            List.of("a1", "a4", "a7"),
            List.of("b2", "b4", "b6"),
            List.of("c3", "c4", "c5"),
            List.of("d1", "d2", "d3"),
            List.of("d5", "d6", "d7"),
            List.of("e3", "e4", "e5"),
            List.of("f2", "f4", "f6"),
            List.of("g1", "g4", "g7")
    );

    public static final Map<String, String[]> ADJACENT_MOVES = Map.ofEntries(
            Map.entry("a1", new String[]{"a4", "d1"}),
            Map.entry("d1", new String[]{"a1", "g1"}),
            Map.entry("g1", new String[]{"d1", "g4"}),
            Map.entry("b2", new String[]{"d2", "b4"}),
            Map.entry("d2", new String[]{"b2", "f2", "d1", "d3"}),
            Map.entry("f2", new String[]{"d2", "f4"}),
            Map.entry("c3", new String[]{"c4", "d3"}),
            Map.entry("d3", new String[]{"c3", "d2", "e3"}),
            Map.entry("e3", new String[]{"d3", "e4"}),
            Map.entry("a4", new String[]{"a7", "b4"}),
            Map.entry("b4", new String[]{"a4", "b2", "b6", "c4"}),
            Map.entry("c4", new String[]{"b4", "c3", "c5"}),
            Map.entry("e4", new String[]{"e3", "e5", "f4"}),
            Map.entry("f4", new String[]{"e4", "f2", "f6", "g4"}),
            Map.entry("g4", new String[]{"f4", "g1", "g7"}),
            Map.entry("c5", new String[]{"c4", "d5"}),
            Map.entry("d5", new String[]{"c5", "d6", "e5"}),
            Map.entry("e5", new String[]{"d5", "e4"}),
            Map.entry("b6", new String[]{"b4", "d6"}),
            Map.entry("d6", new String[]{"b6", "d7", "d5", "f6"}),
            Map.entry("a7", new String[]{"a4", "d7"}),
            Map.entry("d7", new String[]{"a7", "g7"}),
            Map.entry("g7", new String[]{"d7", "g4"})
    );

    private GameConstants() {
        // Private constructor to prevent instantiation
    }
}
