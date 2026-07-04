package com.github.jpmcodes.spawner.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Cache<V> {
    private List<V> elements = new LinkedList<>();

    public List<V> getCachedElements() {
        return this.elements;
    }

    public <E> List<E> cachedMap(Function<V, E> function) {
        List<E> copy = new LinkedList<>();

        for (V element : this.elements) {
            copy.add(function.apply(element));
        }
        return copy;
    }

    public boolean containsCachedElement(V element) {
        return this.elements.contains(element);
    }

    public void addCachedElement(V element) {
        this.elements.add(element);
    }

    @SafeVarargs
    public final void addCachedElements(V... elementArg) {
        this.elements.addAll(Arrays.asList(elementArg));
    }

    public void removeCachedElement(V element) {
        this.elements.remove(element);
    }

    @SafeVarargs
    public final boolean removeCachedElements(V... elementArg) {
        return this.elements.removeAll(Arrays.asList((Object[]) elementArg));
    }

    public V getByIndex(int index) {
        return this.elements.get(index);
    }

    public V getCached(Predicate<V> predicate) {
        if (this.elements == null)
            this.elements = new LinkedList<>();

        for (V element : this.elements) {
            if (predicate.test(element))
                return element;
        }
        return null;
    }

    public List<V> getCachedElements(Predicate<V> predicate) {
        List<V> array = new LinkedList<>();
        for (V element : this.elements) {
            if (predicate.test(element))
                array.add(element);
        }
        return array;
    }

    public boolean isEmpty() {
        return this.elements.isEmpty();
    }

    public Optional<V> findCached(Predicate<V> predicate) {
        return Optional.ofNullable(getCached(predicate));
    }

    public Optional<V> findAndRemove(Predicate<V> predicate) {
        Optional<V> optional = findCached(predicate);
        optional.ifPresent(this::removeCachedElement);
        return optional;
    }

    public Iterator<V> iterator() {
        return this.elements.iterator();
    }

    public int size() {
        return this.elements.size();
    }

    public void removeIf(Predicate<V> predicate) {
        for (V element : this.elements) {
            if (predicate.test(element))
                removeCachedElement(element);
        }
    }
}