package util;

import model.annotation.MaxLength;
import model.annotation.Positive;

import java.lang.reflect.Field;
import java.util.ArrayList;

/** Reads annotations by reflection and checks the rules - works for ANY object. */
public class AnnotationValidator {
    private AnnotationValidator() {
    }

    public static String[] validate(Object obj) {
        ArrayList<String> errors = new ArrayList<>();
        // walk up the class hierarchy so inherited fields are checked too
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (field.isAnnotationPresent(Positive.class) && value instanceof Number n
                            && n.longValue() <= 0) {
                        errors.add(field.getAnnotation(Positive.class).message());
                    }
                    if (field.isAnnotationPresent(MaxLength.class) && value instanceof String s) {
                        int max = field.getAnnotation(MaxLength.class).value();
                        if (s.length() > max) {
                            errors.add(field.getName() + " length must be <= " + max);
                        }
                    }
                } catch (IllegalAccessException e) {
                    errors.add("cannot read field " + field.getName());
                }
            }
        }
        return errors.toArray(new String[0]);
    }
}
