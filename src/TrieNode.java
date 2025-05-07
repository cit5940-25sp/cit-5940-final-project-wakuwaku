import java.util.*;

class TrieNode {
    final Map<Character, TrieNode> children = new HashMap<>();
    final List<String> titles = new ArrayList<>();
    boolean isEnd;
}