package cn.chenyilei.maintain.manager.utils;

import lombok.Data;

/**
 * @author chenyilei
 * @since 2024/05/21 15:01
 */
@Data
public class MutablePair<K, V> {

    private K key;
    private V value;

    public MutablePair() {
    }

    public MutablePair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public static <K, V> MutablePair<K, V> of(K key, V value) {
        return new MutablePair<>(key, value);
    }
}
