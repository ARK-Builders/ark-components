package dev.arkbuilders.components.utils

import java.nio.file.Path

fun Path.hasNestedOrParentalRoot(roots: Iterable<Path>): Boolean {
    val hasNestedRoot = roots.any { path ->
        this.startsWith(path) || path.startsWith(this)
    }
    return hasNestedRoot
}