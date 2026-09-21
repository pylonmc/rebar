package io.github.pylonmc.rebar.test.test.entity;

import io.github.pylonmc.rebar.test.base.AsyncTest;
import io.github.pylonmc.rebar.util.BinaryBvhTree;
import java.util.ArrayList;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

// tests mostly ChatGPT generated
public class BinaryBvhTreeTest extends AsyncTest {
    @Override
    protected void test() {
        failedDuplicateInsertDoesNotChangeIntersections();
        farAwayElementsDoNotProduceFalsePositives();
        insertingEqualElementReturnsFalse();
        insertReturnsFalseForDuplicateElement();
        insertReturnsTrueForNewElement();
        intersectionsAreSortedByDistanceFromOrigin();
        manyElementsCanBeInsertedAndRemoved();
        multipleElementsReturnAllIntersections();
        multipleInsertsUpdateSize();
        rayLengthIsRespected();
        rayOnlyIntersectsSomeElements();
        rayParallelToFaceCanIntersect();
        rayStartingInsideElementReturnsOrigin();
        rayThatMissesElementReturnsNoIntersections();
        rayTooShortToReachElementReturnsNoIntersections();
        removeExistingElementRemovesItsIntersections();
        removeOnlyElementLeavesTreeEmpty();
        rotatedBoundingBoxCanBeIntersected();
        scaledBoundingBoxIsIntersectedCorrectly();
        singleElementIsIntersected();
        startsEmpty();
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
        assertThat(tree.getIntersections(
                new Vector3f(0, 0, -5),
                new Vector3f(0, 0, 10)
        )).isEmpty();
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

    void failedDuplicateInsertDoesNotChangeIntersections() {
        var tree = new BinaryBvhTree<TestElement>();
        var element = element();

        assertThat(tree.insert(element)).isTrue();
        assertThat(tree.insert(element)).isFalse();

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 4)
        );

        assertThat(intersections).hasSize(1);
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

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 4)
        );

        assertThat(intersections).hasSize(1);
        assertThat(intersections.getFirst().x())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getFirst().y())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getFirst().z())
                .isCloseTo(-0.5f, offset(1e-6f));
    }

    void rayThatMissesElementReturnsNoIntersections() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getIntersections(
                new Vector3f(2, 0, -2),
                new Vector3f(0, 0, 4)
        );

        assertThat(intersections).isEmpty();
    }

    void rayTooShortToReachElementReturnsNoIntersections() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 1)
        );

        assertThat(intersections).isEmpty();
    }

    void rayLengthIsRespected() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 2)
        );

        assertThat(intersections).hasSize(1);
        assertThat(intersections.getFirst().z())
                .isCloseTo(-0.5f, offset(1e-6f));
    }

    void translatedElementIsIntersectedAtCorrectPosition() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element(0, 0, 5));

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        );

        assertThat(intersections).hasSize(1);
        assertThat(intersections.getFirst().z())
                .isCloseTo(4.5f, offset(1e-6f));
    }

    void scaledBoundingBoxIsIntersectedCorrectly() {
        var tree = new BinaryBvhTree<TestElement>();

        var transform = new Matrix4f()
                .scale(2, 1, 1);

        tree.insert(element(0, 0, 0, transform));

        var intersections = tree.getIntersections(
                new Vector3f(-3, 0, 0),
                new Vector3f(5, 0, 0)
        );

        assertThat(intersections).hasSize(1);
        assertThat(intersections.getFirst().x())
                .isCloseTo(-1f, offset(1e-6f));
    }

    void multipleElementsReturnAllIntersections() {
        var tree = new BinaryBvhTree<TestElement>();

        tree.insert(element(0, 0, 2));
        tree.insert(element(0, 0, 5));
        tree.insert(element(0, 0, 8));

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        );

        assertThat(intersections).hasSize(3);
    }

    void intersectionsAreSortedByDistanceFromOrigin() {
        var tree = new BinaryBvhTree<TestElement>();

        tree.insert(element(0, 0, 8));
        tree.insert(element(0, 0, 2));
        tree.insert(element(0, 0, 5));

        var origin = new Vector3f(0, 0, 0);

        var intersections = tree.getIntersections(
                origin,
                new Vector3f(0, 0, 10)
        );

        assertThat(intersections).hasSize(3);

        assertThat(intersections.get(0).distanceSquared(origin))
                .isLessThanOrEqualTo(
                        intersections.get(1).distanceSquared(origin)
                );

        assertThat(intersections.get(1).distanceSquared(origin))
                .isLessThanOrEqualTo(
                        intersections.get(2).distanceSquared(origin)
                );
    }

    void rayOnlyIntersectsSomeElements() {
        var tree = new BinaryBvhTree<TestElement>();

        tree.insert(element(0, 0, 2));
        tree.insert(element(10, 0, 4));
        tree.insert(element(0, 0, 6));

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        );

        assertThat(intersections).hasSize(2);
        assertThat(intersections.get(0).z())
                .isCloseTo(1.5f, offset(1e-6f));
        assertThat(intersections.get(1).z())
                .isCloseTo(5.5f, offset(1e-6f));
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

        assertThat(tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        )).isEmpty();
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

    void removeFromMultipleElementTreePreservesOtherElements() {
        var tree = new BinaryBvhTree<TestElement>();

        var first = element(0, 0, 2);
        var second = element(0, 0, 5);
        var third = element(0, 0, 8);

        tree.insert(first);
        tree.insert(second);
        tree.insert(third);

        assertThat(tree.remove(second)).isTrue();
        assertThat(tree.getSize()).isEqualTo(2);

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 10)
        );

        assertThat(intersections).hasSize(2);
        assertThat(intersections.get(0).z())
                .isCloseTo(1.5f, offset(1e-6f));
        assertThat(intersections.get(1).z())
                .isCloseTo(7.5f, offset(1e-6f));
    }

    void removeOnlyElementLeavesTreeEmpty() {
        var tree = new BinaryBvhTree<TestElement>();
        var element = element();

        tree.insert(element);

        assertThat(tree.remove(element)).isTrue();
        assertThat(tree.getSize()).isZero();
        assertThat(tree.getIntersections(
                new Vector3f(0, 0, -2),
                new Vector3f(0, 0, 4)
        )).isEmpty();
    }

    void rayStartingInsideElementReturnsOrigin() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 2)
        );

        assertThat(intersections).hasSize(1);
        assertThat(intersections.getFirst().x())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getFirst().y())
                .isCloseTo(0f, offset(1e-6f));
        assertThat(intersections.getFirst().z())
                .isCloseTo(0f, offset(1e-6f));
    }

    void rayParallelToFaceCanIntersect() {
        var tree = new BinaryBvhTree<TestElement>();
        tree.insert(element());

        var intersections = tree.getIntersections(
                new Vector3f(-2, 0, 0),
                new Vector3f(4, 0, 0)
        );

        assertThat(intersections).hasSize(1);
        assertThat(intersections.getFirst().x())
                .isCloseTo(-0.5f, offset(1e-6f));
    }

    void rotatedBoundingBoxCanBeIntersected() {
        var tree = new BinaryBvhTree<TestElement>();

        var transform = new Matrix4f()
                .rotateZ((float) Math.toRadians(45));

        tree.insert(element(0, 0, 0, transform));

        var intersections = tree.getIntersections(
                new Vector3f(-2, 0, 0),
                new Vector3f(4, 0, 0)
        );

        assertThat(intersections).hasSize(1);
    }

    void farAwayElementsDoNotProduceFalsePositives() {
        var tree = new BinaryBvhTree<TestElement>();

        tree.insert(element(100, 0, 10));
        tree.insert(element(200, 0, 20));
        tree.insert(element(300, 0, 30));

        var intersections = tree.getIntersections(
                new Vector3f(0, 0, 0),
                new Vector3f(0, 0, 40)
        );

        assertThat(intersections).isEmpty();
    }

    void manyElementsCanBeInsertedAndRemoved() {
        var tree = new BinaryBvhTree<TestElement>();
        var elements = new ArrayList<TestElement>();

        for (int i = 0; i < 100; i++) {
            var element = element(0, 0, i * 2f);

            elements.add(element);
            assertThat(tree.insert(element)).isTrue();
        }

        assertThat(tree.getSize()).isEqualTo(100);

        assertThat(tree.getIntersections(
                new Vector3f(0, 0, -1),
                new Vector3f(0, 0, 201)
        )).hasSize(100);

        for (int i = 0; i < elements.size(); i += 2) {
            assertThat(tree.remove(elements.get(i))).isTrue();
        }

        assertThat(tree.getSize()).isEqualTo(50);

        assertThat(tree.getIntersections(
                new Vector3f(0, 0, -1),
                new Vector3f(0, 0, 201)
        )).hasSize(50);
    }

    void removeFindsElementWhenBoundingBoxesOverlap() {
        var tree = new BinaryBvhTree<TestElement>();

        var first = element(0, 0, 0);
        var second = element(0.75f, 0, 0);

        tree.insert(first);
        tree.insert(second);

        assertThat(tree.remove(second)).isTrue();
        assertThat(tree.getSize()).isOne();

        var intersections = tree.getIntersections(
                new Vector3f(-2, 0, 0),
                new Vector3f(4, 0, 0)
        );

        assertThat(intersections).hasSize(1);
    }
}

