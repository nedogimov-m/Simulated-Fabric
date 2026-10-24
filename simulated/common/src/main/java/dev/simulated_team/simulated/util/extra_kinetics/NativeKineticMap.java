package dev.simulated_team.simulated.util.extra_kinetics;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import java.util.*;
import java.util.function.*;

/** Real ordered map with channel-aware position keys, scoped to Create's private kinetic index. */
public final class NativeKineticMap<K,V> extends HashMap<K,V> {
    private final Map<K,V> storage = new Object2ObjectLinkedOpenCustomHashMap<>(NativeKineticPositions.STRATEGY);
    public NativeKineticMap() {}
    public NativeKineticMap(Map<? extends K,? extends V> source) { storage.putAll(source); }
    @Override public int size(){return storage.size();}
    @Override public boolean isEmpty(){return storage.isEmpty();}
    @Override public boolean containsKey(Object key){return storage.containsKey(key);}
    @Override public boolean containsValue(Object value){return storage.containsValue(value);}
    @Override public V get(Object key){return storage.get(key);}
    @Override public V getOrDefault(Object key,V fallback){return storage.getOrDefault(key,fallback);}
    @Override public V put(K key,V value){return storage.put(key,value);}
    @Override public V remove(Object key){return storage.remove(key);}
    @Override public void putAll(Map<? extends K,? extends V> source){storage.putAll(source);}
    @Override public void clear(){storage.clear();}
    @Override public Set<K> keySet(){return storage.keySet();}
    @Override public Collection<V> values(){return storage.values();}
    @Override public Set<Map.Entry<K,V>> entrySet(){return storage.entrySet();}
    @Override public V putIfAbsent(K key,V value){return storage.putIfAbsent(key,value);}
    @Override public boolean remove(Object key,Object value){return storage.remove(key,value);}
    @Override public boolean replace(K key,V previous,V replacement){return storage.replace(key,previous,replacement);}
    @Override public V replace(K key,V value){return storage.replace(key,value);}
    @Override public void replaceAll(BiFunction<? super K,? super V,? extends V> function){storage.replaceAll(function);}
    @Override public void forEach(BiConsumer<? super K,? super V> action){storage.forEach(action);}
    @Override public V computeIfAbsent(K key,Function<? super K,? extends V> function){return storage.computeIfAbsent(key,function);}
    @Override public V computeIfPresent(K key,BiFunction<? super K,? super V,? extends V> function){return storage.computeIfPresent(key,function);}
    @Override public V compute(K key,BiFunction<? super K,? super V,? extends V> function){return storage.compute(key,function);}
    @Override public V merge(K key,V value,BiFunction<? super V,? super V,? extends V> function){return storage.merge(key,value,function);}
    @Override public Object clone(){return new NativeKineticMap<>(storage);}
    @Override public boolean equals(Object other){return other==this||storage.equals(other);}
    @Override public int hashCode(){return storage.hashCode();}
    @Override public String toString(){return storage.toString();}
}
