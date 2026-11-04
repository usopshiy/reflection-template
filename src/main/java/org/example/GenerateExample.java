package org.example;


import org.example.classes.Example;
import org.example.classes.BinaryTreeNode;
import org.example.classes.Cart;
import org.example.classes.Shape;
import org.example.generator.Generator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GenerateExample {
    public static void main(String[] args) {
        var gen = new Generator();
        System.out.println("Integer: " + gen.generateValueOfType(Integer.class));
        System.out.println("String: " + gen.generateValueOfType(String.class));
        System.out.println("Example: " + gen.generateValueOfType(Example.class));
        System.out.println("Array: " + Arrays.toString(gen.generateValueOfType(int[].class)));
        System.out.println("List: " + gen.generateValueOfType(List.class));
        System.out.println("Map: " + gen.generateValueOfType(Map.class));
        Shape shape = gen.generateValueOfType(Shape.class);
        System.out.println("Shape: " + shape.getClass().getSimpleName() + ", perimeter=" + shape.getPerimeter());
        Cart cart = gen.generateValueOfType(Cart.class);
        System.out.println("Cart: " + cart.getItems().stream().map(item -> item.getName()).toList());
        BinaryTreeNode tree = gen.generateValueOfType(BinaryTreeNode.class);
        System.out.println("Tree: root=" + tree.getData() + ", left=" + tree.getLeft().getData()
                + ", right=" + tree.getRight().getData());
    }
}
