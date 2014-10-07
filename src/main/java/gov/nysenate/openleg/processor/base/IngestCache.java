package gov.nysenate.openleg.processor.base;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A generic key-value pair cache to queue updates to the persistence layer so that they can be committed
 * to the file system at a later time in the processing stage.
 */
public class IngestCache<K,V>
{
    private Map<K, V> cache = new LinkedHashMap<>();

    /**
     * Retrieve the object from cache using a unique id key. Returns null
     * on a cache miss.
     *
     * @param key String - Unique Id
     * @return V
     */
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * Checks if the key is set in the cache.
     *
     * @param key String - Unique Id
     * @return boolean - true if key is found, false otherwise.
     */
    public boolean has(K key) {
        return cache.containsKey(key);
    }

    /**
     * Sets the value to the associated key.
     *
     * @param key String - Unique Id
     * @param obj T
     */
    public void set(K key, V obj) {
        cache.put(key, obj);
    }

    public Collection<V> getCurrentCache() {
        return cache.values();
    }

    public void flushCache() {
        cache.clear();
    }
}