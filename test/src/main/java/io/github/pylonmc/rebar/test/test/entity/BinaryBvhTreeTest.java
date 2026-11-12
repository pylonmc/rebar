package io.github.pylonmc.rebar.test.test.entity;

import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.util.BinaryBvhTree;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

// tests mostly ChatGPT generated
public class BinaryBvhTreeTest extends AsyncTest {
    @Override
    protected void test() {
        farAwayElementsDoNotProduceFalsePositives();
        insertingEqualElementReturnsFalse();
        insertReturnsFalseForDuplicateElement();
        insertReturnsTrueForNewElement();
        multipleInsertsUpdateSize();
        rayLengthIsRespected();
        rayParallelToFaceCanIntersect();
        rayStartingInsideElementReturnsOrigin();
        rayThatMissesElementReturnsNoIntersections();
        rayTooShortToReachElementReturnsNoIntersections();
        removeExistingElementRemovesItsIntersections();
        removeExistingElementReturnsTrue();
        removeNonexistentElementReturnsFalse();
        removeFindsElementWhenBoundingBoxesOverlap();
        removeOnlyElementLeavesTreeEmpty();
        removeFromEmptyTreeReturnsFalse();
        rotatedBoundingBoxCanBeIntersected();
        scaledBoundingBoxIsIntersectedCorrectly();
        translatedElementIsIntersectedAtCorrectPosition();
        singleElementIsIntersected();
        startsEmpty();

//        TestElement testElement = element();
//        BinaryBvhTree<TestElement> tree = new BinaryBvhTree<>();
//        tree.insert(testElement);
//
//        for (int i = 0; i < 10_000; i++) {
//            tree.insert(element(ThreadLocalRandom.current().nextFloat(1_000), ThreadLocalRandom.current().nextFloat(1_000), ThreadLocalRandom.current().nextFloat(1_000)));
//        }
//        println("Inserted");
//
//        long time = System.nanoTime();
//        assertThat(tree.getClosestIntersection(new Vector3f(0, 0, 0), new Vector3f(0.5f, 0, 0))).isNotNull();
//        println(System.nanoTime() - time);
//
//        time = System.nanoTime();
//        tree = BinaryBvhTree.buildFromElements(tree);
//        println(System.nanoTime() - time);
//
//        time = System.nanoTime();
//        assertThat(tree.getClosestIntersection(new Vector3f(0, 0, 0), new Vector3f(0.5f, 0, 0))).isNotNull();
//        println(System.nanoTime() - time);
    }

    private record TestElement(
            Vector3f position,
            Matrix4f boundingBoxTransform
    ) implements BinaryBvhTree.Element {

        @Override
        public @NonNull Vector3f getPosition() {
            return position;
        }

        @Override
        public @NonNull Matrix4f getBoundingBoxTransform() {
            return boundingBoxTransform;
        }
    }

    private static TestElement element() {
        return element(0, 0, 0);
    }

    private static TestElement element(float x, float y, float z) {
        return element(x, y, z, new Matrix4f());
    }

    private static TestElement element(
            float x,
            float y,
            float z,
            Matrix4f transform
    ) {
        return new TestElement(
                new Vector3f(x, y, z),
                transform
        );
    }

    void startsEmpty() {
        var tree = new BinaryBvhTree<TestElement>();

        assertThat(tree.getSize()).isZero();
        assertThat(tree.getClosestIntersection(
                new Vector3f(0, 0, -5),
                new Vector3f(0, 0, 10)
        )).isNull();
    }

    void insertReturnsTrueForNewElement() {
        var tree = new BinaryBvhTree<TestElement>();

        assertThat(tree.insert(element())).isTrue();
        assertThat(tree.getSize()).isOne();
    }

    void insertReturnsFalseForDuplicateElement() {
        var tree = new BinaryBvhTree<TestElement>();
        var element = element();

        assertThat(tree.insert(element)).isTrue();
        assertThat(tree.insert(element)).isFalse();

        assertThat(tree.getSize()).isOne();
    }

    void insertingEqualElementReturnsFalse() {
        var tree = new BinaryBvhTree<TestElement>();

        assertThat(tree.insert(element(1, 2, 3))).isTrue();
        assertThat(tree.insert(element(1, 2, 3))).isFalse();

        assertThat(tree.getSize()).isOne();
    }

    void multipleInsertsUpdateSize() {
        var tree = new BinaryBvhTree<TestElement>();

        assertThat(tree.insert(element(0, 0, 1))).isTrue();
        assertThat(tree.insert(element(0, 0, 2))).isTrue();
        assertThat(tree.insert(element(0, 0, 3))).isTrue();

        assertThat(tree.getSize()).isEqualTo(3);
    }

    void singleElementIsIntersected() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getClosestIntersection(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 4)
        );

        assertThat(intersections).isNotNull();
        assertThat(intersections.getSecond().x())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getSecond().y())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getSecond().z())
                .isCloseTo(-0.5f, offset(1e-6f));
    }

    void rayThatMissesElementReturnsNoIntersections() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getClosestIntersection(
                new Vector3f(2, 0, -2),
                new Vector3f(0, 0, 4)
        );

        assertThat(intersections).isNull();
    }

    void rayTooShortToReachElementReturnsNoIntersections() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getClosestIntersection(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 1)
        );

        assertThat(intersections).isNull();
    }

    void rayLengthIsRespected() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getClosestIntersection(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 2)
        );

        assertThat(intersections).isNotNull();
        assertThat(intersections.getSecond().z())
                .isCloseTo(-0.5f, offset(1e-6f));
    }

    void translatedElementIsIntersectedAtCorrectPosition() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element(0, 0, 5));

        var intersections = tree.getClosestIntersection(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        );

        assertThat(intersections).isNotNull();
        assertThat(intersections.getSecond().z())
                .isCloseTo(4.5f, offset(1e-6f));
    }

    void scaledBoundingBoxIsIntersectedCorrectly() {
        var tree = new BinaryBvhTree<TestElement>();

        var transform = new Matrix4f()
                .scale(2, 1, 1);

        tree.insert(element(0, 0, 0, transform));

        var intersections = tree.getClosestIntersection(
                new Vector3f(-3, 0, 0),
                new Vector3f(5, 0, 0)
        );

        assertThat(intersections).isNotNull();
        assertThat(intersections.getSecond().x())
                .isCloseTo(-1f, offset(1e-6f));
    }

    void removeExistingElementReturnsTrue() {
        var tree = new BinaryBvhTree<TestElement>();
        var element = element(0, 0, 2);

        tree.insert(element);

        assertThat(tree.remove(element)).isTrue();
        assertThat(tree.getSize()).isZero();
    }

    void removeExistingElementRemovesItsIntersections() {
        var tree = new BinaryBvhTree<TestElement>();
        var element = element(0, 0, 2);

        tree.insert(element);
        tree.remove(element);

        assertThat(tree.getClosestIntersection(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        )).isNull();
    }

    void removeNonexistentElementReturnsFalse() {
        var tree = new BinaryBvhTree<TestElement>();

        tree.insert(element(0, 0, 2));

        assertThat(tree.remove(element(0, 0, 5))).isFalse();
        assertThat(tree.getSize()).isOne();
    }

    void removeFromEmptyTreeReturnsFalse() {
        var tree = new BinaryBvhTree<TestElement>();

        assertThat(tree.remove(element())).isFalse();
        assertThat(tree.getSize()).isZero();
    }

    void removeOnlyElementLeavesTreeEmpty() {
        var tree = new BinaryBvhTree<TestElement>();
        var element = element();

        tree.insert(element);

        assertThat(tree.remove(element)).isTrue();
        assertThat(tree.getSize()).isZero();
        assertThat(tree.getClosestIntersection(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 4)
        )).isNull();
    }

    void rayStartingInsideElementReturnsOrigin() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getClosestIntersection(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 2)
        );

        assertThat(intersections).isNotNull();
        assertThat(intersections.getSecond().x())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getSecond().y())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getSecond().z())
                .isCloseTo(0f, offset(1e-6f));
    }

    void rayParallelToFaceCanIntersect() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getClosestIntersection(
                new Vector3f(-2, 0, 0),
                new Vector3f(4, 0, 0)
        );

        assertThat(intersections).isNotNull();
        assertThat(intersections.getSecond().x())
                .isCloseTo(-0.5f, offset(1e-6f));
    }

    void rotatedBoundingBoxCanBeIntersected() {
        var tree = new BinaryBvhTree<TestElement>();

        var transform = new Matrix4f()
                .rotateZ((float) Math.toRadians(45));

        tree.insert(element(0, 0, 0, transform));

        var intersections = tree.getClosestIntersection(
                new Vector3f(-2, 0, 0),
                new Vector3f(4, 0, 0)
        );

        assertThat(intersections).isNotNull();
    }

    void farAwayElementsDoNotProduceFalsePositives() {
        var tree = new BinaryBvhTree<TestElement>();

        tree.insert(element(100, 0, 10));
        tree.insert(element(200, 0, 20));
        tree.insert(element(300, 0, 30));

        var intersections = tree.getClosestIntersection(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 40)
        );

        assertThat(intersections).isNull();
    }

    void removeFindsElementWhenBoundingBoxesOverlap() {
        var tree = new BinaryBvhTree<TestElement>();

        var first = element(0, 0, 0);
        var second = element(0.75f, 0, 0);

        tree.insert(first);
        tree.insert(second);

        assertThat(tree.remove(second)).isTrue();
        assertThat(tree.getSize()).isOne();

        var intersections = tree.getClosestIntersection(
                new Vector3f(-2, 0, 0),
                new Vector3f(4, 0, 0)
        );

        assertThat(intersections).isNotNull();
    }
}

