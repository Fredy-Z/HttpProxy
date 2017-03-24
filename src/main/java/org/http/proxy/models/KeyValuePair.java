package org.http.proxy.models;

public class KeyValuePair<K, V> {
    private K key;
    private V value;

    public KeyValuePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        if (null != key) {
            if (value != null) {
                return key + ":" + value;
            }
            return key.toString();
        }
        return "";
    }
}
