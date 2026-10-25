package service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.annotation.Id;

/** Generic repository: stores any type T, finding each object's key from its @Id field. */
public class Repository<T> {
    private final Map<String, T> store;

    public Repository() {
        this(new HashMap<>());
    }

    /** Lets the caller choose the map, e.g. a ConcurrentHashMap for thread safety. */
    public Repository(Map<String, T> backingMap) {
        this.store = backingMap;
    }

    public T save(T entity) {
        store.put(idOf(entity), entity);
        return entity;
    }

    @SafeVarargs
    public final void saveAll(T... items) {
        for (T item : items) {
            save(item);
        }
    }

    public T findById(String id) {
        return store.get(id);
    }

    public ArrayList<T> findAll() {
        return new ArrayList<>(store.values());
    }

    public boolean delete(String id) {
        return store.remove(id) != null;
    }

    public int count() {
        return store.size();
    }

    /** Finds the field marked @Id (also in superclasses) and returns its value as the key. */
    private String idOf(T entity) {
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    try {
                        field.setAccessible(true);
                        return String.valueOf(field.get(entity));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Cannot read @Id field", e);
                    }
                }
            }
        }
        throw new IllegalStateException(entity.getClass().getSimpleName() + " has no @Id field");
    }

    /** Generic method with a bounded type parameter. */
    public static <N extends Number> double average(N[] values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (N v : values) {
            sum += v.doubleValue();
        }
        return sum / values.length;
    }

    /** Wildcard example: works for List<Integer>, List<Long>, List<Double> ... */
    public static double sumAll(List<? extends Number> numbers) {
        double sum = 0;
        for (Number n : numbers) {
            sum += n.doubleValue();
        }
        return sum;
    }
}
