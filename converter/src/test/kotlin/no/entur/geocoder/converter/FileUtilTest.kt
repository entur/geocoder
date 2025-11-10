package no.entur.geocoder.converter

import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import kotlin.test.Test

class FileUtilTest {
    @Test
    fun `streamToFile creates temp file with content`() {
        val content = "Test content with special chars: æøå\n123"
        val file = FileUtil.streamToFile(ByteArrayInputStream(content.toByteArray()))

        assertTrue(file.exists())
        assertTrue(file.name.endsWith(".tmp"))
        assertEquals(content, file.readText())
        file.delete()
    }

    @Test
    fun `streamToFile handles binary content`() {
        val content = ByteArray(10_000) { (it % 256).toByte() }
        val file = FileUtil.streamToFile(ByteArrayInputStream(content))

        assertArrayEquals(content, file.readBytes())
        file.delete()
    }
}

