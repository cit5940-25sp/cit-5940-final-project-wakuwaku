import java.util.*;

public class MovieTrie {
    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        List<String> titles = new ArrayList<>();
    }

    private final TrieNode root = new TrieNode();

    public void insert(String title) {
        if (title == null || title.isEmpty()) return;

        TrieNode node = root;
        for (char c : title.toLowerCase().toCharArray()) {
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
            node.titles.add(title);
        }
    }

    public List<String> autocomplete(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        TrieNode node = root;
        for (char c : prefix.toLowerCase().toCharArray()) {
            node = node.children.get(c);
            if (node == null) return Collections.emptyList();
        }
        return node.titles;
    }
}