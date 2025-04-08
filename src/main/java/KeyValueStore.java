import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueStore<V> {

    private final Map<String, Map<String, TreeMap<Long, V>>> data;

    public KeyValueStore() {
        this.data = new ConcurrentHashMap<>();
    }

    public void put(String key1, String subKey, long timestamp, V value) {
        Map<String, TreeMap<Long, V>> subKeyMap = data.computeIfAbsent(key1, k -> new ConcurrentHashMap<>());
        TreeMap<Long, V> timestampMap = subKeyMap.computeIfAbsent(subKey, k -> new TreeMap<>());
        timestampMap.put(timestamp, value);
    }

    public Map<String, TreeMap<Long, V>> getAllValues(String key1) {
        Map<String, TreeMap<Long, V>> subKeyMap = data.get(key1);
        if (subKeyMap == null) {
            return Collections.emptyMap();
        }
        return new HashMap<>(subKeyMap);
    }

    public Optional<V> getLatestValue(String key1, String subKey) {
        Map<String, TreeMap<Long, V>> subKeyMap = data.get(key1);
        if (subKeyMap != null) {
            TreeMap<Long, V> timestampMap = subKeyMap.get(subKey);
            if (timestampMap != null && !timestampMap.isEmpty()) {
                Map.Entry<Long, V> latestEntry = timestampMap.lastEntry();
                return Optional.ofNullable(latestEntry.getValue());
            }
        }
        return Optional.empty();
    }

    public Optional<V> getValueAtTimestamp(String key1, String subKey, long queryTimestamp) {
        Map<String, TreeMap<Long, V>> subKeyMap = data.get(key1);
        if (subKeyMap != null) {
            TreeMap<Long, V> timestampMap = subKeyMap.get(subKey);
            if (timestampMap != null && !timestampMap.isEmpty()) {
                Map.Entry<Long, V> entry = timestampMap.floorEntry(queryTimestamp);
                if (entry != null) {
                    return Optional.ofNullable(entry.getValue());
                }
            }
        }
        return Optional.empty();
    }

    public boolean deleteKey(String key1) {
        return data.remove(key1) != null;
    }

    public boolean deleteSubkey(String key1, String subKey) {
        Map<String, TreeMap<Long, V>> subKeyMap = data.get(key1);
        if (subKeyMap != null) {
            boolean removed = subKeyMap.remove(subKey) != null;
            if (removed) {
                if (subKeyMap.isEmpty()) {
                    data.remove(key1, subKeyMap);
                }
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        KeyValueStore<String> store = new KeyValueStore<>();

        store.put("user1", "email", 100L, "alice@example.com");
        store.put("user1", "city", 110L, "New York");
        store.put("user2", "email", 120L, "bob@example.com");
        store.put("user1", "email", 130L, "alice.updated@example.com");
        store.put("user1", "city", 140L, "London");

        System.out.println("Current time (conceptual): 150L\n");

        System.out.println("1. All values for user1:");
        Map<String, TreeMap<Long, String>> user1Data = store.getAllValues("user1");
        user1Data.forEach((subKey, timestamps) -> {
            System.out.println("  SubKey: " + subKey);
            timestamps.forEach((ts, val) -> System.out.println("    Timestamp: " + ts + ", Value: " + val));
        });
        System.out.println("---");

        System.out.println("2. Latest email for user1:");
        Optional<String> latestEmail = store.getLatestValue("user1", "email");
        System.out.println("  " + latestEmail.orElse("Not found"));
        System.out.println("   Latest city for user1:");
        Optional<String> latestCity = store.getLatestValue("user1", "city");
        System.out.println("  " + latestCity.orElse("Not found"));
        System.out.println("   Latest email for user_unknown:");
        System.out.println("  " + store.getLatestValue("user_unknown", "email").orElse("Not found"));
        System.out.println("---");

        System.out.println("3. Email for user1 at timestamp 115L:");
        System.out.println("  " + store.getValueAtTimestamp("user1", "email", 115L).orElse("Not found"));
        System.out.println("   Email for user1 at timestamp 130L:");
        System.out.println("  " + store.getValueAtTimestamp("user1", "email", 130L).orElse("Not found"));
        System.out.println("   Email for user1 at timestamp 90L:");
        System.out.println("  " + store.getValueAtTimestamp("user1", "email", 90L).orElse("Not found"));
        System.out.println("   City for user1 at timestamp 135L:");
        System.out.println("  " + store.getValueAtTimestamp("user1", "city", 135L).orElse("Not found"));
        System.out.println("---");

        System.out.println("4. Deleting user1's city subkey...");
        boolean deletedSubkey = store.deleteSubkey("user1", "city");
        System.out.println("  Deleted? " + deletedSubkey);
        System.out.println("   Latest city for user1 now:");
        System.out.println("  " + store.getLatestValue("user1", "city").orElse("Not found"));
        System.out.println("   All values for user1 now:");
        store.getAllValues("user1").forEach((subKey, timestamps) -> {
            System.out.println("     SubKey: " + subKey);
        });
        System.out.println("---");

        System.out.println("5. Deleting key user1...");
        boolean deletedKey = store.deleteKey("user1");
        System.out.println("  Deleted? " + deletedKey);
        System.out.println("   All values for user1 now:");
        System.out.println("  " + store.getAllValues("user1"));
        System.out.println("   Latest email for user1 now:");
        System.out.println("  " + store.getLatestValue("user1", "email").orElse("Not found"));
        System.out.println("---");

        System.out.println("Check user2:");
        System.out.println("  Latest email for user2: " + store.getLatestValue("user2", "email").orElse("Not found"));
    }
}