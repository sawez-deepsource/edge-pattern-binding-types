package edge;

import java.util.*;

/**
 * Pattern binding variables — edge cases for Spoon PR #6444.
 * Pattern bindings create CtLocalVariable nodes that:
 * - Have no defaultExpression
 * - Have no CtVariableWrite
 * - Have parent CtTypePattern
 * - Are guaranteed non-null by the instanceof guard
 */
public class PatternBindingEdgeCases {

    sealed interface Shape permits Circle, Rectangle, Triangle {}
    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double a, double b, double c) implements Shape {}

    record Wrapper<T>(T value) {}
    record Nested(Wrapper<String> inner) {}

    // --- BoxedConditional (JAVA-E1054) ---
    // Pattern binding Boolean is guaranteed non-null — must NOT flag
    String booleanPattern(Object obj) {
        if (obj instanceof Boolean b) {
            return b ? "yes" : "no"; // b is non-null, no NPE risk
        }
        return "unknown";
    }

    // Same for Integer — non-null
    int integerPattern(Object obj) {
        if (obj instanceof Integer i) {
            return i + 1; // unboxing is safe, i is non-null
        }
        return 0;
    }

    // --- MultipleVarOnSameLine (JAVA-C1003) ---
    // Record deconstruction creates multiple CtLocalVariable on same line
    double area(Shape shape) {
        return switch (shape) {
            case Circle(var r) -> Math.PI * r * r;
            case Rectangle(var w, var h) -> w * h; // two vars, same line
            case Triangle(var a, var b, var c) -> { // three vars, same line
                var s = (a + b + c) / 2;
                yield Math.sqrt(s * (s - a) * (s - b) * (s - c));
            }
        };
    }

    // --- UnwrittenLocalVariable (JAVA-E1064) ---
    // Pattern binding variable — written by the match, not explicit write
    void patternVarIsWritten(Object obj) {
        if (obj instanceof String s) {
            System.out.println(s.length()); // s IS written — by the pattern match
        }
    }

    // --- Nested record deconstruction ---
    void deepDeconstruction(Object obj) {
        if (obj instanceof Nested(Wrapper<String>(var inner))) {
            System.out.println(inner); // deeply nested pattern binding
        }
    }

    // --- Pattern var in assertion (WrongArgumentOrderInAssertion) ---
    // Pattern var is effectively final → isConstantExpression should return true
    void patternVarInAssertion(Object obj) {
        if (obj instanceof Integer expected) {
            // expected is effectively final — it's a "constant" for assertion purposes
            // assertEquals(expected, computeResult()); — this should NOT flag wrong order
        }
    }

    // --- Pattern var used as loop variable ---
    void patternInLoop(List<Object> objects) {
        for (Object obj : objects) {
            if (obj instanceof String s) {
                // s is bound fresh each iteration — no mutation concern
                System.out.println(s.toUpperCase());
            }
        }
    }

    // --- Guarded pattern with && ---
    void guardedPattern(Object obj) {
        if (obj instanceof String s && s.length() > 5) {
            System.out.println("long string: " + s);
        }
    }

    // --- Real violations alongside pattern usage ---
    void realBugsWithPatterns(Object obj) {
        if (obj instanceof String s) {
            int[] arr = new int[3];
            System.out.println(arr.toString()); // BadArrayToString — should fire
        }
    }
}
