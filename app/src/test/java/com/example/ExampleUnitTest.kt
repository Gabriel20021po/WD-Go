package com.example

import com.example.model.FileCategory
import com.example.util.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExampleUnitTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testMimeTypeResolution() {
        val pngFile = File("photo.png")
        assertEquals("image/png", FileUtils.getMimeType(pngFile))

        val zipFile = File("archive.zip")
        assertEquals("application/zip", FileUtils.getMimeType(zipFile))

        val txtFile = File("notes.txt")
        assertEquals("text/plain", FileUtils.getMimeType(txtFile))
    }

    @Test
    fun testCategoryDetection() {
        val imgFile = File("image.jpg")
        assertEquals(FileCategory.IMAGES, FileUtils.getCategoryForFile(imgFile))

        val videoFile = File("video.mp4")
        assertEquals(FileCategory.VIDEOS, FileUtils.getCategoryForFile(videoFile))

        val audioFile = File("music.mp3")
        assertEquals(FileCategory.AUDIO, FileUtils.getCategoryForFile(audioFile))

        val docFile = File("report.pdf")
        assertEquals(FileCategory.DOCUMENTS, FileUtils.getCategoryForFile(docFile))

        val apkFile = File("app.apk")
        assertEquals(FileCategory.APKS, FileUtils.getCategoryForFile(apkFile))

        val zipFile = File("backup.zip")
        assertEquals(FileCategory.ARCHIVES, FileUtils.getCategoryForFile(zipFile))
    }

    @Test
    fun testZipAndExtract() {
        val testDir = tempFolder.newFolder("source")
        val file1 = File(testDir, "hello.txt").apply { writeText("Hello World!") }
        val file2 = File(testDir, "test.md").apply { writeText("# Markdown") }

        val destZip = File(tempFolder.root, "bundle.zip")
        val zipped = FileUtils.zipFiles(listOf(file1, file2), destZip)
        assertTrue(zipped)
        assertTrue(destZip.exists())

        val extractDir = tempFolder.newFolder("extracted")
        val extracted = FileUtils.extractZip(destZip, extractDir)
        assertTrue(extracted)

        val extractedFile1 = File(extractDir, "hello.txt")
        assertTrue(extractedFile1.exists())
        assertEquals("Hello World!", extractedFile1.readText())
    }

    @Test
    fun testChecksumCalculation() {
        val file = tempFolder.newFile("sample.txt").apply { writeText("Checksum test string") }
        val checksum = FileUtils.calculateChecksum(file, "SHA-256")
        assertTrue(checksum.isNotEmpty())
        assertEquals(64, checksum.length) // SHA-256 is 64 hex characters
    }

    @Test
    fun testBasicAuthCredentialsEncoding() {
        val user = "admin"
        val pass = "1234"
        val raw = "$user:$pass"
        val encoded = java.util.Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8))
        val header = "Basic $encoded"
        val base64Credentials = header.substring(6).trim()
        val decoded = String(java.util.Base64.getDecoder().decode(base64Credentials), Charsets.UTF_8)
        assertEquals("admin:1234", decoded)
    }
}
