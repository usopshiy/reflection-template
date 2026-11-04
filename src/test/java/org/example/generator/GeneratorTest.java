package org.example.generator;

import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorTest {
    private final Generator generator = new Generator(new Random(42));

    @ParameterizedTest(name = "{0} returns {1}")
    @MethodSource("valueTypes")
    void returnsRequestedTypeAndDifferentValues(Class<?> requestedType, Class<?> expectedType) {
        Set<Object> values = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            Object value = generator.generateValueOfType(requestedType);

            assertInstanceOf(expectedType, value);
            values.add(value);
        }

        assertTrue(values.size() > 1, "Generator shouldn't always return the same value");
    }

    static Stream<Arguments> valueTypes() {
        return Stream.of(
                Arguments.of(boolean.class, Boolean.class),
                Arguments.of(byte.class, Byte.class),
                Arguments.of(short.class, Short.class),
                Arguments.of(int.class, Integer.class),
                Arguments.of(long.class, Long.class),
                Arguments.of(float.class, Float.class),
                Arguments.of(double.class, Double.class),
                Arguments.of(char.class, Character.class),
                Arguments.of(Boolean.class, Boolean.class),
                Arguments.of(Byte.class, Byte.class),
                Arguments.of(Short.class, Short.class),
                Arguments.of(Integer.class, Integer.class),
                Arguments.of(Long.class, Long.class),
                Arguments.of(Float.class, Float.class),
                Arguments.of(Double.class, Double.class),
                Arguments.of(Character.class, Character.class),
                Arguments.of(String.class, String.class)
        );
    }

    @ParameterizedTest
    @ValueSource(classes = {int.class, Integer.class})
    void integersStayWithinTheChosenRange(Class<?> type) {
        for (int i = 0; i < 100; i++) {
            int number = (Integer) generator.generateValueOfType(type);

            assertTrue(number >= -1000 && number <= 1000);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {float.class, Float.class, double.class, Double.class})
    void floatingPointNumbersStayWithinTheChosenRange(Class<?> type) {
        for (int i = 0; i < 100; i++) {
            Number number = (Number) generator.generateValueOfType(type);

            assertTrue(number.doubleValue() >= 0 && number.doubleValue() < 100);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {char.class, Character.class})
    void charactersAreLowercaseLatinLetters(Class<?> type) {
        for (int i = 0; i < 100; i++) {
            char letter = (Character) generator.generateValueOfType(type);

            assertTrue(letter >= 'a' && letter <= 'z');
        }
    }

    @Test
    void stringsContainUpToTwelveLowercaseLetters() {
        for (int i = 0; i < 100; i++) {
            String text = generator.generateValueOfType(String.class);

            assertTrue(text.matches("[a-z]{1,12}"));
        }
    }

    @Test
    void objectRequestReturnsAString() {
        Object value = generator.generateValueOfType(Object.class);

        assertInstanceOf(String.class, value);
    }

    @Test
    void enumGenerationCanChooseEveryConstant() {
        Set<Color> colors = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            colors.add(generator.generateValueOfType(Color.class));
        }

        assertEquals(Set.of(Color.values()), colors);
    }

    @ParameterizedTest
    @ValueSource(classes = {
            int[].class, boolean[].class, byte[].class, short[].class,
            long[].class, float[].class, double[].class, char[].class,
            String[].class, Color[].class, Rectangle[].class, Shape[].class, int[][].class
    })
    void arraysHaveTheRequestedTypeAndContainElements(Class<?> type) {
        Object array = generator.generateValueOfType(type);

        assertInstanceOf(type, array);
        int length = Array.getLength(array);
        assertTrue(length >= 1 && length <= 3);
        for (int i = 0; i < length; i++) {
            assertNotNull(Array.get(array, i));
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {List.class, ArrayList.class})
    void rawListsContainStrings(Class<?> type) {
        List<?> items = (List<?>) generator.generateValueOfType(type);

        assertInstanceOf(type, items);
        assertTrue(items.size() >= 1 && items.size() <= 3);
        for (Object item : items) {
            assertInstanceOf(String.class, item);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {Map.class, LinkedHashMap.class})
    void rawMapsContainStringKeysAndIntegerValues(Class<?> type) {
        Map<?, ?> entries = (Map<?, ?>) generator.generateValueOfType(type);

        assertInstanceOf(type, entries);
        assertTrue(entries.size() >= 1 && entries.size() <= 3);
        for (Map.Entry<?, ?> entry : entries.entrySet()) {
            assertInstanceOf(String.class, entry.getKey());
            assertInstanceOf(Integer.class, entry.getValue());
        }
    }

    private enum Color {
        RED, GREEN, BLUE
    }
}
