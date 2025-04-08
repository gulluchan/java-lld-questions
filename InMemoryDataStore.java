
package org.example;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDataStore {

    private final Map<String, ValueWithTTL> store = new ConcurrentHashMap<>();
    private final Deque<Operation> undoStack = new LinkedList<>();
    private final Deque<Operation> redoStack = new LinkedList<>();

    private static class ValueWithTTL {
        final String value;
        final long expiryTimestamp;

        ValueWithTTL(String value, long expiryTimestamp) {
            this.value = value;
            this.expiryTimestamp = expiryTimestamp;
        }

        String getValue() {
            return value;
        }

        long getExpiryTimestamp() {
            return expiryTimestamp;
        }

        boolean hasExpired() {
            return expiryTimestamp != -1 && System.currentTimeMillis() > expiryTimestamp;
        }

        @Override
        public String toString() {
            return "ValueWithTTL{" +
                    "value='" + value + '\'' +
                    ", expiryTimestamp=" + expiryTimestamp +
                    '}';
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValueWithTTL that = (ValueWithTTL) o;
            return expiryTimestamp == that.expiryTimestamp && Objects.equals(value, that.value);
        }
        @Override
        public int hashCode() {
            return Objects.hash(value, expiryTimestamp);
        }
    }

    private static class Operation {
        final Command command;
        final String key;
        final ValueWithTTL oldValue;
        final ValueWithTTL newValue;

        Operation(Command command, String key, ValueWithTTL oldValue, ValueWithTTL newValue) {
            this.command = command;
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        @Override
        public String toString() {
            return "Operation{" +
                    "command=" + command +
                    ", key='" + key + '\'' +
                    ", oldValue=" + oldValue +
                    ", newValue=" + newValue +
                    '}';
        }
    }

    private enum Command {
        SET,
        UPDATE,
        DELETE
    }

    public synchronized void set(String key, String value) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        setInternal(key, value, -1);
    }

    public synchronized void setWithTTL(String key, String value, long ttlInMillis) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null");
        long expiryTimestamp = (ttlInMillis > 0) ? System.currentTimeMillis() + ttlInMillis : -1;
        setInternal(key, value, expiryTimestamp);
    }

    private void setInternal(String key, String value, long expiryTimestamp) {
        ValueWithTTL oldValue = store.get(key);
        if (oldValue != null && oldValue.hasExpired()) {
            oldValue = null;
        }
        ValueWithTTL newValue = new ValueWithTTL(value, expiryTimestamp);
        store.put(key, newValue);
        logOperation(new Operation(Command.SET, key, oldValue, newValue));
    }

    public synchronized String get(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        ValueWithTTL entry = store.get(key);

        if (entry == null) {
            return null;
        }

        if (entry.hasExpired()) {
            store.remove(key);
            return null;
        }

        return entry.getValue();
    }

    public synchronized boolean delete(String key) {
        Objects.requireNonNull(key, "Key cannot be null");
        ValueWithTTL oldValue = store.get(key);
        if (oldValue != null && oldValue.hasExpired()) {
            oldValue = null;
        }
        ValueWithTTL removedValue = store.remove(key);
        if (oldValue != null) {
            logOperation(new Operation(Command.DELETE, key, oldValue, null));
            return true;
        } else {
            return removedValue != null;
        }
    }

    public synchronized boolean update(String key, String newValue) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(newValue, "Value cannot be null");
        return updateInternal(key, newValue, null);
    }

    public synchronized boolean updateWithTTL(String key, String newValue, long ttlInMillis) {
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(newValue, "Value cannot be null");
        return updateInternal(key, newValue, ttlInMillis);
    }

    private boolean updateInternal(String key, String value, Long newTtlInMillis) {
        ValueWithTTL oldValue = store.get(key);
        if (oldValue == null || oldValue.hasExpired()) {
            if (oldValue != null && oldValue.hasExpired()) {
                store.remove(key);
            }
            return false;
        }

        long newExpiryTimestamp;
        if (newTtlInMillis == null) {
            newExpiryTimestamp = oldValue.getExpiryTimestamp();
        } else {
            newExpiryTimestamp = (newTtlInMillis > 0) ? System.currentTimeMillis() + newTtlInMillis : -1;
        }

        ValueWithTTL newValue = new ValueWithTTL(value, newExpiryTimestamp);
        store.put(key, newValue);
        logOperation(new Operation(Command.UPDATE, key, oldValue, newValue));
        return true;
    }

    public synchronized List<String> scan() {
        List<String> activeKeys = new ArrayList<>();
        store.forEach((key, valueWithTTL) -> {
            if (!valueWithTTL.hasExpired()) {
                activeKeys.add(key);
            }
        });
        return activeKeys;
    }

    public synchronized List<String> scanByPrefix(String prefix) {
        Objects.requireNonNull(prefix, "Prefix cannot be null");
        List<String> matchingKeys = new ArrayList<>();
        store.forEach((key, valueWithTTL) -> {
            if (key.startsWith(prefix) && !valueWithTTL.hasExpired()) {
                matchingKeys.add(key);
            }
        });
        return matchingKeys;
    }

    public synchronized boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }

        Operation lastOperation = undoStack.pop();
        switch (lastOperation.command) {
            case SET:
                if (lastOperation.oldValue == null) {
                    store.remove(lastOperation.key);
                } else {
                    store.put(lastOperation.key, lastOperation.oldValue);
                }
                break;
            case UPDATE:
                store.put(lastOperation.key, lastOperation.oldValue);
                break;
            case DELETE:
                if (lastOperation.oldValue != null) {
                    store.put(lastOperation.key, lastOperation.oldValue);
                }
                break;
        }
        redoStack.push(lastOperation);
        return true;
    }

    public synchronized boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }

        Operation operationToRedo = redoStack.pop();
        switch (operationToRedo.command) {
            case SET:
            case UPDATE:
                store.put(operationToRedo.key, operationToRedo.newValue);
                break;
            case DELETE:
                store.remove(operationToRedo.key);
                break;
        }
        undoStack.push(operationToRedo);
        return true;
    }

    private synchronized void logOperation(Operation operation) {
        undoStack.push(operation);
        redoStack.clear();
    }

    public synchronized int cleanupExpiredKeys() {
        List<String> keysToRemove = new ArrayList<>();
        store.forEach((key, valueWithTTL) -> {
            if (valueWithTTL.hasExpired()) {
                keysToRemove.add(key);
            }
        });
        keysToRemove.forEach(store::remove);
        return keysToRemove.size();
    }

    public static void main(String[] args) throws InterruptedException {
        InMemoryDataStore ds = new InMemoryDataStore();

        System.out.println("--- Part 1: Basic CRUD ---");
        ds.set("name", "Alice");
        System.out.println("Get name: " + ds.get("name"));
        ds.set("city", "New York");
        System.out.println("Get city: " + ds.get("city"));
        ds.update("city", "London");
        System.out.println("Get updated city: " + ds.get("city"));
        System.out.println("Update non-existent key 'country': " + ds.update("country", "UK"));
        System.out.println("Delete city: " + ds.delete("city"));
        System.out.println("Get deleted city: " + ds.get("city"));
        System.out.println("Delete non-existent key 'city': " + ds.delete("city"));

        System.out.println("\n--- Part 2: Scan ---");
        ds.set("user:1", "Alice");
        ds.set("user:2", "Bob");
        ds.set("product:1", "Laptop");
        System.out.println("Scan all keys: " + ds.scan());
        System.out.println("Scan prefix 'user:': " + ds.scanByPrefix("user:"));
        System.out.println("Scan prefix 'prod': " + ds.scanByPrefix("prod"));
        System.out.println("Scan prefix 'xyz': " + ds.scanByPrefix("xyz"));

        System.out.println("\n--- Part 3: TTL ---");
        ds.setWithTTL("tempKey", "Temporary", 100);
        System.out.println("Get tempKey immediately: " + ds.get("tempKey"));
        Thread.sleep(150);
        System.out.println("Get tempKey after 150ms: " + ds.get("tempKey"));
        System.out.println("Store contains tempKey after expiration? " + ds.store.containsKey("tempKey"));

        ds.set("persistentKey", "Data");
        ds.updateWithTTL("persistentKey", "Data with TTL", 200);
        System.out.println("Get persistentKey after TTL update: " + ds.get("persistentKey"));
        Thread.sleep(250);
        System.out.println("Get persistentKey after TTL expiry: " + ds.get("persistentKey"));

        System.out.println("\n--- Part 4: Undo/Redo ---");
        ds.set("a", "1");
        ds.set("b", "2");
        ds.update("a", "11");
        ds.setWithTTL("c", "3", 500);
        ds.delete("b");

        System.out.println("Current state (a): " + ds.get("a"));
        System.out.println("Current state (b): " + ds.get("b"));
        System.out.println("Current state (c): " + ds.get("c"));

        System.out.println("\nUndo 1 (DELETE b):");
        ds.undo();
        System.out.println("State a: " + ds.get("a"));
        System.out.println("State b: " + ds.get("b"));
        System.out.println("State c: " + ds.get("c"));

        System.out.println("\nUndo 2 (SET c=3 TTL):");
        ds.undo();
        System.out.println("State a: " + ds.get("a"));
        System.out.println("State b: " + ds.get("b"));
        System.out.println("State c: " + ds.get("c"));

        System.out.println("\nUndo 3 (UPDATE a=11):");
        ds.undo();
        System.out.println("State a: " + ds.get("a"));
        System.out.println("State b: " + ds.get("b"));
        System.out.println("State c: " + ds.get("c"));

        System.out.println("\nRedo 1 (UPDATE a=11):");
        ds.redo();
        System.out.println("State a: " + ds.get("a"));
        System.out.println("State b: " + ds.get("b"));
        System.out.println("State c: " + ds.get("c"));

        System.out.println("\nRedo 2 (SET c=3 TTL):");
        ds.redo();
        System.out.println("State a: " + ds.get("a"));
        System.out.println("State b: " + ds.get("b"));
        System.out.println("State c: " + ds.get("c"));

        System.out.println("\nSet new value 'd', clearing redo stack:");
        ds.set("d", "4");
        System.out.println("Redo after new operation: " + ds.redo());

        System.out.println("\nUndo stack size: " + ds.undoStack.size());

        System.out.println("\nCleanup Check:");
        ds.setWithTTL("exp1", "gone soon", 50);
        ds.setWithTTL("exp2", "gone soon too", 50);
        System.out.println("Scan before cleanup: " + ds.scanByPrefix("exp"));
        Thread.sleep(100);
        System.out.println("Manual cleanup count: " + ds.cleanupExpiredKeys());
        System.out.println("Scan after cleanup: " + ds.scanByPrefix("exp"));
    }
}
