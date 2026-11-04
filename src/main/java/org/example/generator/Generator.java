package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class Generator {

    private static final int MAX_DEPTH = 16;
    private static final int MAX_TYPE_OCCURRENCES = 2;
    private final Random random;

    public Generator() {
        this(new Random());
    }

    public Generator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Primitive Class tokens return their boxed values. */
    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> type) {
        if (type == null) {
            throw new GenerationException("Requested type must not be null");
        }
        return (T) generate(type, new ArrayList<>(), 0);
    }

    private Object generate(Type type, List<Class<?>> path, int depth) {
        if (depth > MAX_DEPTH) {
            throw new GenerationException("Maximum generation depth exceeded at " + type.getTypeName());
        }
        return switch (type) {
            case Class<?> concreteType -> generateClass(concreteType, path, depth);
            case ParameterizedType genericType -> generateCollection(genericType, path, depth);
            default -> throw new GenerationException("Unsupported generic type: " + type.getTypeName());
        };
    }

    private Object generateClass(Class<?> type, List<Class<?>> path, int depth) {
        if (type.isEnum()) {
            return generateEnum(type);
        }
        if (type.isArray()) {
            return generateArray(type.getComponentType(), path, depth);
        }

        return switch (type.getName()) {
            case "boolean", "java.lang.Boolean" -> random.nextBoolean();
            case "byte", "java.lang.Byte" -> (byte) random.nextInt(256);
            case "short", "java.lang.Short" -> (short) random.nextInt(65536);
            case "int", "java.lang.Integer" -> random.nextInt(2001) - 1000;
            case "long", "java.lang.Long" -> random.nextLong();
            case "float", "java.lang.Float" -> random.nextFloat() * 100;
            case "double", "java.lang.Double" -> random.nextDouble() * 100;
            case "char", "java.lang.Character" -> generateChar();
            case "java.lang.String", "java.lang.Object" -> generateString();
            case "java.util.List", "java.util.ArrayList" -> generateList(String.class, path, depth);
            case "java.util.Map", "java.util.LinkedHashMap" ->
                    generateMap(String.class, Integer.class, path, depth);
            case "void", "java.lang.Void" -> throw new GenerationException("Cannot generate void/Void");
            default -> generateObject(type, path, depth);
        };
    }

    private Object generateCollection(ParameterizedType type, List<Class<?>> path, int depth) {
        Type[] arguments = type.getActualTypeArguments();
        return switch (type.getRawType().getTypeName()) {
            case "java.util.List", "java.util.ArrayList" -> generateList(arguments[0], path, depth);
            case "java.util.Map", "java.util.LinkedHashMap" ->
                    generateMap(arguments[0], arguments[1], path, depth);
            default -> throw new GenerationException("Unsupported parameterized type: " + type.getTypeName());
        };
    }

    private char generateChar() {
        return (char) ('a' + random.nextInt(26));
    }

    private String generateString() {
        int length = 1 + random.nextInt(12);
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(generateChar());
        }
        return result.toString();
    }

    private Object generateEnum(Class<?> type) {
        Object[] constants = type.getEnumConstants();
        if (constants.length == 0) {
            throw new GenerationException("Empty enum: " + type.getName());
        }
        return constants[random.nextInt(constants.length)];
    }

    private Object generateArray(Class<?> elementType, List<Class<?>> path, int depth) {
        int length = randomSize();
        Object array = Array.newInstance(elementType, length);
        for (int i = 0; i < length; i++) {
            Array.set(array, i, generate(elementType, path, depth + 1));
        }
        return array;
    }

    private Object generateObject(Class<?> type, List<Class<?>> path, int depth) {
        Generatable annotation = type.getDeclaredAnnotation(Generatable.class);
        if (annotation == null) {
            throw new GenerationException("Type must declare @Generatable: " + type.getName());
        }
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            return generateImplementation(type, annotation, path, depth);
        }
        if (annotation.implementations().length != 0) {
            throw new GenerationException("Concrete type must not list implementations: " + type.getName());
        }
        if (Collections.frequency(path, type) >= MAX_TYPE_OCCURRENCES) {
            return null;
        }
        path.add(type);
        try {
            return construct(type, path, depth);
        } finally {
            path.remove(path.size() - 1);
        }
    }

    private Object generateImplementation(Class<?> type, Generatable annotation,
                                          List<Class<?>> path, int depth) {
        List<Class<?>> implementations = new ArrayList<>(Arrays.asList(annotation.implementations()));
        if (implementations.isEmpty()) {
            throw new GenerationException("No implementations declared for " + type.getName());
        }
        for (Class<?> implementation : implementations) {
            if (!type.isAssignableFrom(implementation) || implementation.isInterface()
                    || Modifier.isAbstract(implementation.getModifiers())
                    || implementation.getDeclaredAnnotation(Generatable.class) == null) {
                throw new GenerationException("Invalid @Generatable implementation "
                        + implementation.getName() + " for " + type.getName());
            }
        }
        Collections.shuffle(implementations, random);
        GenerationException failure = new GenerationException("No constructible implementation for " + type.getName());
        for (Class<?> implementation : implementations) {
            try {
                return generate(implementation, path, depth + 1);
            } catch (GenerationException e) {
                failure.addSuppressed(e);
            }
        }
        throw failure;
    }

    private Object construct(Class<?> type, List<Class<?>> path, int depth) {
        List<Constructor<?>> constructors = new ArrayList<>(Arrays.asList(type.getDeclaredConstructors()));
        Collections.shuffle(constructors, random);
        GenerationException failure = new GenerationException("No usable constructor for " + type.getName());
        for (Constructor<?> constructor : constructors) {
            try {
                if (!constructor.trySetAccessible()) {
                    throw new GenerationException("Constructor is inaccessible: " + constructor);
                }
                Object[] arguments = generateArguments(constructor, path, depth);
                return constructor.newInstance(arguments);
            } catch (InvocationTargetException e) {
                failure.addSuppressed(new GenerationException("Constructor threw: " + constructor, e.getCause()));
            } catch (ReflectiveOperationException | RuntimeException e) {
                failure.addSuppressed(new GenerationException("Cannot invoke " + constructor, e));
            }
        }
        throw failure;
    }

    private Object[] generateArguments(Constructor<?> constructor, List<Class<?>> path, int depth) {
        Type[] parameterTypes = constructor.getGenericParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = generate(parameterTypes[i], path, depth + 1);
        }
        return arguments;
    }

    private List<Object> generateList(Type elementType, List<Class<?>> path, int depth) {
        List<Object> result = new ArrayList<>();
        int length = randomSize();
        for (int i = 0; i < length; i++) {
            result.add(generate(elementType, path, depth + 1));
        }
        return result;
    }

    private Map<Object, Object> generateMap(Type keyType, Type valueType, List<Class<?>> path, int depth) {
        Map<Object, Object> result = new LinkedHashMap<>();
        int attempts = randomSize();
        for (int i = 0; i < attempts; i++) {
            Object key = generate(keyType, path, depth + 1);
            Object value = generate(valueType, path, depth + 1);
            result.put(key, value);
        }
        return result;
    }

    private int randomSize() {
        return 1 + random.nextInt(3);
    }
}
