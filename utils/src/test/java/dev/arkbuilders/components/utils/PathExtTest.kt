package dev.arkbuilders.components.utils

import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class PathExtTest {

    // --- Tests with Set for roots parameter ---

    @Test
    fun `hasNestedOrParentalRoot should return true when there is an exact match in roots`() {
        val path = Paths.get("/parent/child")
        val roots = setOf(Paths.get("/parent/child"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when there is a direct parent in roots`() {
        val path = Paths.get("/parent/child")
        val roots = setOf(Paths.get("/parent"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when there is a direct child in roots`() {
        val path = Paths.get("/parent")
        val roots = setOf(Paths.get("/parent/child"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when there is a nested parent in roots`() {
        val path = Paths.get("/parent/child/grandchild")
        val roots = setOf(Paths.get("/parent"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when there is a nested child in roots`() {
        val path = Paths.get("/parent")
        val roots = setOf(Paths.get("/parent/child/grandchild"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return false when there are no nested or parental roots`() {
        val path = Paths.get("/parent/child")
        val roots = setOf(Paths.get("/unrelated"), Paths.get("/another"))
        assertEquals(path.hasNestedOrParentalRoot(roots), false)
    }

    @Test
    fun `hasNestedOrParentalRoot should return false when roots set is empty`() {
        val path = Paths.get("/parent/child")
        val roots = emptySet<Path>()
        assertEquals(path.hasNestedOrParentalRoot(roots), false)
    }

    // --- Tests with List for roots parameter ---

    @Test
    fun `hasNestedOrParentalRoot should return true when an exact match exists in roots (List)`() {
        val path = Paths.get("/parent/child")
        val roots = listOf(Paths.get("/parent/child"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when a direct parent exists in roots (List)`() {
        val path = Paths.get("/parent/child")
        val roots = listOf(Paths.get("/parent"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when a direct child exists in roots (List)`() {
        val path = Paths.get("/parent")
        val roots = listOf(Paths.get("/parent/child"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when a nested parent exists in roots (List)`() {
        val path = Paths.get("/parent/child/grandchild")
        val roots = listOf(Paths.get("/parent"))
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return false when there are no nested or parental roots (List)`() {
        val path = Paths.get("/parent/child")
        val roots = listOf(Paths.get("/unrelated"), Paths.get("/another"))
        assertEquals(path.hasNestedOrParentalRoot(roots), false)
    }

    @Test
    fun `hasNestedOrParentalRoot should return false when roots is empty`() {
        val path = Paths.get("/parent/child")
        val roots = emptyList<Path>()
        assertEquals(path.hasNestedOrParentalRoot(roots), false)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when duplicates of an exact match exist in roots (List)`() {
        val path = Paths.get("/parent/child")
        val roots = listOf(Paths.get("/parent/child"), Paths.get("/parent/child")) // Duplicate exact match
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when duplicates of a direct parent exist in roots (List)`() {
        val path = Paths.get("/parent/child")
        val roots = listOf(Paths.get("/parent"), Paths.get("/parent")) // Duplicate direct parent
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return true when duplicates of a nested parent exist in roots (List)`() {
        val path = Paths.get("/parent/child/grandchild")
        val roots = listOf(Paths.get("/parent"), Paths.get("/parent")) // Duplicate nested parent
        assertEquals(path.hasNestedOrParentalRoot(roots), true)
    }

    @Test
    fun `hasNestedOrParentalRoot should return false when duplicates of unrelated paths exist in roots (List)`() {
        val path = Paths.get("/parent/child")
        val roots = listOf(Paths.get("/unrelated"), Paths.get("/unrelated")) // Duplicate unrelated paths
        assertEquals(path.hasNestedOrParentalRoot(roots), false)
    }
}
