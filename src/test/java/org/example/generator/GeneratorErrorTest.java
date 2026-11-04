package org.example.generator;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorErrorTest {
    private final Generator generator = new Generator(new Random(42));

    @Test
    void rejectsNullInsteadOfAType() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(null));

        assertTrue(error.getMessage().contains("must not be null"));
    }

    @ParameterizedTest
    @ValueSource(classes = {void.class, Void.class})
    void rejectsVoid(Class<?> type) {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(type));

        assertTrue(error.getMessage().contains("void/Void"));
    }

    @Test
    void cannotChooseAConstantFromAnEmptyEnum() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(EmptyEnum.class));

        assertTrue(error.getMessage().contains("Empty enum"));
    }

    @ParameterizedTest
    @ValueSource(classes = {Unannotated.class, Runnable.class, Set.class})
    void requiresAnnotationForUnsupportedTypes(Class<?> type) {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(type));

        assertTrue(error.getMessage().contains("@Generatable"));
        assertTrue(error.getMessage().contains(type.getName()));
    }

    @Test
    void parentAnnotationDoesNotApplyToChild() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(UnannotatedChild.class));

        assertTrue(error.getMessage().contains("@Generatable"));
        assertTrue(error.getMessage().contains(UnannotatedChild.class.getName()));
    }

    @Test
    void interfaceMustListItsImplementations() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(NoImplementations.class));

        assertTrue(error.getMessage().contains("No implementations declared"));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("invalidImplementations")
    void rejectsInvalidImplementation(Class<?> type, String reason) {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(type), reason);

        assertTrue(error.getMessage().contains("Invalid @Generatable implementation"));
        assertTrue(error.getMessage().contains(type.getName()));
    }

    static Stream<Arguments> invalidImplementations() {
        return Stream.of(
                Arguments.of(UnrelatedImplementation.class, "класс не реализует интерфейс"),
                Arguments.of(UnmarkedImplementation.class, "у реализации нет аннотации"),
                Arguments.of(AbstractImplementation.class, "реализация абстрактная")
        );
    }

    @Test
    void concreteClassCannotDeclareImplementations() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(ConcreteWithImplementations.class));

        assertTrue(error.getMessage().contains("Concrete type must not list implementations"));
    }

    @ParameterizedTest
    @ValueSource(classes = {WildcardList.class, GenericValue.class, GenericArray.class})
    void rejectsConstructorArgumentsWithoutAConcreteType(Class<?> type) {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(type));

        Throwable argumentError = error.getSuppressed()[0].getCause();
        assertTrue(argumentError.getMessage().contains("Unsupported generic type"));
    }

    @Test
    void parameterizedUserClassesAreNotSupported() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(GenericHolder.class));

        Throwable argumentError = error.getSuppressed()[0].getCause();
        assertTrue(argumentError.getMessage().contains("Unsupported parameterized type"));
    }

    @Test
    void innerClassNeedsAGeneratableOuterInstance() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(InnerObject.class));

        Throwable argumentError = error.getSuppressed()[0].getCause();
        assertTrue(argumentError.getMessage().contains(GeneratorErrorTest.class.getName()));
    }

    @Test
    void keepsTheOriginalConstructorException() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(RejectingObject.class));

        assertTrue(error.getMessage().contains(RejectingObject.class.getName()));
        assertEquals(1, error.getSuppressed().length);
        Throwable constructorError = error.getSuppressed()[0].getCause();
        assertInstanceOf(IllegalArgumentException.class, constructorError);
        assertEquals("rejected input", constructorError.getMessage());
    }

    @Test
    void errorNamesTheUnsupportedConstructorArgument() {
        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(NeedsUnsupported.class));

        Throwable argumentError = error.getSuppressed()[0].getCause();
        assertTrue(argumentError.getMessage().contains(Unannotated.class.getName()));
    }

    @Test
    void failedRequestDoesNotBreakTheNextRequest() {
        assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(NeedsUnsupported.class));

        BinaryTreeNode tree = generator.generateValueOfType(BinaryTreeNode.class);

        assertNotNull(tree.getData());
        assertNotNull(tree.getLeft());
        assertNotNull(tree.getRight());
    }

    @Test
    void rejectsArraysDeeperThanTheNestingLimit() {
        Class<?> deepArrayType = Array.newInstance(int.class, new int[18]).getClass();

        GenerationException error = assertThrows(GenerationException.class,
                () -> generator.generateValueOfType(deepArrayType));

        assertTrue(error.getMessage().contains("Maximum generation depth"));
    }

    private enum EmptyEnum {
    }

    private static class Unannotated {
    }

    @Generatable
    private static class AnnotatedParent {
    }

    private static class UnannotatedChild extends AnnotatedParent {
    }

    @Generatable
    private interface NoImplementations {
    }

    @Generatable(implementations = Product.class)
    private interface UnrelatedImplementation {
    }

    @Generatable(implementations = Unmarked.class)
    private interface UnmarkedImplementation {
    }

    private static class Unmarked implements UnmarkedImplementation {
    }

    @Generatable(implementations = AbstractChild.class)
    private interface AbstractImplementation {
    }

    @Generatable
    private abstract static class AbstractChild implements AbstractImplementation {
    }

    @Generatable(implementations = Product.class)
    private static class ConcreteWithImplementations {
    }

    @Generatable
    private record WildcardList(List<?> values) {
    }

    @Generatable
    private record GenericValue<T>(T value) {
    }

    @Generatable
    private record GenericHolder(GenericValue<String> value) {
    }

    @Generatable
    private record GenericArray<T>(T[] values) {
    }

    @Generatable
    private class InnerObject {
    }

    @Generatable
    private static class RejectingObject {
        private RejectingObject() {
            throw new IllegalArgumentException("rejected input");
        }
    }

    @Generatable
    private record NeedsUnsupported(Unannotated value) {
    }
}
