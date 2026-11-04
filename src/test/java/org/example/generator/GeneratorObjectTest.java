package org.example.generator;

import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Example;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GeneratorObjectTest {
    private final Generator generator = new Generator(new Random(42));

    @Test
    void createsExampleWithAnIntegerArgument() {
        Example example = generator.generateValueOfType(Example.class);

        assertTrue(example.toString().matches("Example\\(-?\\d+\\)"));
    }

    @Test
    void rectangleGetsGeneratedDimensions() {
        Rectangle rectangle = generator.generateValueOfType(Rectangle.class);

        assertTrue(rectangle.getArea() >= 0 && rectangle.getArea() < 10000);
        assertTrue(rectangle.getPerimeter() >= 0 && rectangle.getPerimeter() < 400);
    }

    @Test
    void triangleGetsThreeGeneratedSides() {
        Triangle triangle = generator.generateValueOfType(Triangle.class);

        assertTrue(triangle.getPerimeter() >= 0 && triangle.getPerimeter() < 300);
    }

    @Test
    void productHasANameAndPrice() {
        Product product = generator.generateValueOfType(Product.class);

        assertFalse(product.getName().isEmpty());
        assertTrue(product.getPrice() >= 0 && product.getPrice() < 100);
    }

    @Test
    void cartContainsProductsInsteadOfRawStrings() {
        Cart cart = generator.generateValueOfType(Cart.class);

        assertFalse(cart.getItems().isEmpty());
        for (Product product : cart.getItems()) {
            assertNotNull(product);
            assertFalse(product.getName().isEmpty());
        }
    }

    @Test
    void shapeCanBeARectangleOrATriangle() {
        Set<Class<?>> shapes = new HashSet<>();

        for (int i = 0; i < 100; i++) {
            Shape shape = generator.generateValueOfType(Shape.class);
            assertNotNull(shape);
            shapes.add(shape.getClass());
        }

        assertEquals(Set.of(Rectangle.class, Triangle.class), shapes);
    }

    @Test
    void callsPrivateConstructors() {
        Dog dog = generator.generateValueOfType(Dog.class);

        assertFalse(dog.name.isEmpty());
    }

    @Test
    void fillsNestedGenericCollections() {
        Warehouse warehouse = generator.generateValueOfType(Warehouse.class);

        assertFalse(warehouse.productsByCategory().isEmpty());
        for (var category : warehouse.productsByCategory().entrySet()) {
            assertFalse(category.getKey().isEmpty());
            assertFalse(category.getValue().isEmpty());
            for (Product product : category.getValue()) {
                assertFalse(product.getName().isEmpty());
            }
        }
    }

    @Test
    void createsRecordsThroughTheirConstructor() {
        Point point = generator.generateValueOfType(Point.class);

        assertTrue(point.x() >= -1000 && point.x() <= 1000);
        assertTrue(point.y() >= -1000 && point.y() <= 1000);
    }

    @Test
    void acceptsAClassWithNoConstructorArguments() {
        EmptyObject object = generator.generateValueOfType(EmptyObject.class);

        assertNotNull(object);
    }

    @Test
    void abstractClassUsesItsDeclaredImplementation() {
        Animal animal = generator.generateValueOfType(Animal.class);

        Dog dog = assertInstanceOf(Dog.class, animal);
        assertFalse(dog.name.isEmpty());
    }

    @Test
    @Timeout(2)
    void treeStopsAfterTwoLevels() {
        BinaryTreeNode root = generator.generateValueOfType(BinaryTreeNode.class);

        assertNotNull(root.getData());
        assertLeaf(root.getLeft());
        assertLeaf(root.getRight());
    }

    private void assertLeaf(BinaryTreeNode node) {
        assertNotNull(node);
        assertNotNull(node.getData());
        assertNull(node.getLeft());
        assertNull(node.getRight());
    }

    @Test
    @Timeout(2)
    void mutualReferencesAlsoStopGrowing() {
        Person person = generator.generateValueOfType(Person.class);

        Address address = person.address();
        assertNotNull(address);
        Person resident = address.resident();
        assertNotNull(resident);
        assertNotNull(resident.address());
        assertNull(resident.address().resident());
    }

    @Test
    void usesAnotherConstructorWhenAnArgumentIsUnsupported() {
        for (int i = 0; i < 30; i++) {
            NamedObject object = generator.generateValueOfType(NamedObject.class);

            assertFalse(object.name.isEmpty());
        }
    }

    @Test
    void usesAnotherImplementationWhenAConstructorFails() {
        for (int i = 0; i < 30; i++) {
            Service service = generator.generateValueOfType(Service.class);

            assertInstanceOf(WorkingService.class, service);
        }
    }

    @Generatable
    private record Warehouse(Map<String, List<Product>> productsByCategory) {
    }

    @Generatable
    private record Point(int x, int y) {
    }

    @Generatable
    private static class EmptyObject {
    }

    @Generatable(implementations = Dog.class)
    private abstract static class Animal {
    }

    @Generatable
    private static class Dog extends Animal {
        private final String name;

        private Dog(String name) {
            this.name = name;
        }
    }

    @Generatable
    private record Person(Address address) {
    }

    @Generatable
    private record Address(Person resident) {
    }

    @Generatable
    private static class NamedObject {
        private final String name;

        private NamedObject(Runnable unsupported) {
            throw new AssertionError("Аргумент Runnable нельзя сгенерировать");
        }

        private NamedObject(String name) {
            this.name = name;
        }
    }

    @Generatable(implementations = {BrokenService.class, WorkingService.class})
    private interface Service {
    }

    @Generatable
    private static class BrokenService implements Service {
        private BrokenService() {
            throw new IllegalStateException("Service is unavailable");
        }
    }

    @Generatable
    private static class WorkingService implements Service {
    }
}
