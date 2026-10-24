package dev.simulated_team.simulated.util.extra_kinetics;

import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import java.util.*;
import java.util.function.*;

/** The native discovery frontier must visit both genuine owners at the same block position. */
public final class NativeKineticSet<E> extends HashSet<E> {
    private final Set<E> storage = new ObjectLinkedOpenCustomHashSet<>(NativeKineticPositions.STRATEGY);
    public NativeKineticSet() {}
    public NativeKineticSet(Collection<? extends E> source){storage.addAll(source);}
    @Override public int size(){return storage.size();}
    @Override public boolean isEmpty(){return storage.isEmpty();}
    @Override public boolean contains(Object value){return storage.contains(value);}
    @Override public boolean add(E value){return storage.add(value);}
    @Override public boolean remove(Object value){return storage.remove(value);}
    @Override public boolean addAll(Collection<? extends E> source){return storage.addAll(source);}
    @Override public boolean removeAll(Collection<?> source){return storage.removeIf(value->source.stream().anyMatch(other->NativeKineticPositions.STRATEGY.equals(value,other)));}
    @Override public boolean retainAll(Collection<?> source){return storage.removeIf(value->source.stream().noneMatch(other->NativeKineticPositions.STRATEGY.equals(value,other)));}
    @Override public boolean removeIf(Predicate<? super E> filter){return storage.removeIf(filter);}
    @Override public void clear(){storage.clear();}
    @Override public Iterator<E> iterator(){return storage.iterator();}
    @Override public Object[] toArray(){return storage.toArray();}
    @Override public <T> T[] toArray(T[] target){return storage.toArray(target);}
    @Override public void forEach(Consumer<? super E> action){storage.forEach(action);}
    @Override public Spliterator<E> spliterator(){return storage.spliterator();}
    @Override public Object clone(){return new NativeKineticSet<>(storage);}
    @Override public boolean equals(Object other){return other==this||storage.equals(other);}
    @Override public int hashCode(){return storage.hashCode();}
    @Override public String toString(){return storage.toString();}
}
