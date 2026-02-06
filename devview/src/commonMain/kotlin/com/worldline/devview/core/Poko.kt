package com.worldline.devview.core

/**
 * Annotation for marking data classes that should be processed by the Poko compiler plugin.
 *
 * Poko is a Kotlin compiler plugin that generates `copy()`, `equals()`, `hashCode()`,
 * and `toString()` methods for data classes, providing similar functionality to Kotlin's
 * built-in data classes but with support for Kotlin Multiplatform.
 *
 * ## Usage
 * ```kotlin
 * @Poko
 * class MyDataClass(
 *     val id: String,
 *     val name: String,
 *     val enabled: Boolean
 * )
 * ```
 *
 * The Poko plugin will automatically generate:
 * - `copy()` method with default parameters
 * - `equals()` and `hashCode()` based on all properties
 * - `toString()` with property names and values
 *
 * ## When to Use
 * Use this annotation instead of Kotlin's `data class` keyword when:
 * - Working in Kotlin Multiplatform projects
 * - Need compatibility across all platforms
 * - Kotlin's data class features are not available
 *
 * @see <a href="https://github.com/drewhamilton/Poko">Poko GitHub Repository</a>
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
public annotation class Poko
