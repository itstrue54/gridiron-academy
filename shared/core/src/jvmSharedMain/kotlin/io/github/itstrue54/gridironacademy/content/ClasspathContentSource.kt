package io.github.itstrue54.gridironacademy.content

/** Reads the bundled content JSON, packaged as Java resources on the JVM and Android. */
class ClasspathContentSource(private val root: String = "content") : ContentSource {
    override fun read(path: String): String? =
        javaClass.classLoader.getResourceAsStream("$root/$path")?.bufferedReader()?.use { it.readText() }
}
