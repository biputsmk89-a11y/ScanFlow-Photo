package com.scanflow.photocompressor.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class UriHelperTest {

    @Test
    fun `isContentUri and isFileUri detect correct schemes`() {
        val contentUri = mockk<Uri> {
            every { scheme } returns "content"
        }
        val fileUri = mockk<Uri> {
            every { scheme } returns "file"
        }

        assertTrue(UriHelper.isContentUri(contentUri))
        assertFalse(UriHelper.isFileUri(contentUri))

        assertTrue(UriHelper.isFileUri(fileUri))
        assertFalse(UriHelper.isContentUri(fileUri))
    }

    @Test
    fun `getFileName resolves displayName from ContentResolver for content URI`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val cursor = mockk<Cursor>()
        val uri = mockk<Uri> {
            every { scheme } returns "content"
            every { lastPathSegment } returns "fallback.jpg"
        }

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )
        } returns cursor

        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.moveToFirst() } returns true
        every { cursor.getString(0) } returns "my_photo.png"
        every { cursor.close() } just Runs

        val fileName = UriHelper.getFileName(context, uri)
        assertEquals("my_photo.png", fileName)
    }

    @Test
    fun `getFileName falls back to lastPathSegment when cursor is null`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri> {
            every { scheme } returns "content"
            every { lastPathSegment } returns "sample.jpg"
        }

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns null

        val fileName = UriHelper.getFileName(context, uri)
        assertEquals("sample.jpg", fileName)
    }

    @Test
    fun `getFileSize resolves size from ContentResolver for content URI`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val cursor = mockk<Cursor>()
        val uri = mockk<Uri> {
            every { scheme } returns "content"
            every { path } returns null
        }

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.SIZE),
                null,
                null,
                null
            )
        } returns cursor

        every { cursor.getColumnIndex(OpenableColumns.SIZE) } returns 0
        every { cursor.moveToFirst() } returns true
        every { cursor.isNull(0) } returns false
        every { cursor.getLong(0) } returns 1_234_567L
        every { cursor.close() } just Runs

        val size = UriHelper.getFileSize(context, uri)
        assertEquals(1_234_567L, size)
    }

    @Test
    fun `getFileSize falls back to ParcelFileDescriptor when cursor query returns null`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val pfd = mockk<ParcelFileDescriptor>()
        val uri = mockk<Uri> {
            every { scheme } returns "content"
            every { path } returns null
        }

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.query(uri, any(), null, null, null)
        } returns null

        every { contentResolver.openFileDescriptor(uri, "r") } returns pfd
        every { pfd.statSize } returns 987_654L
        every { pfd.close() } just Runs

        val size = UriHelper.getFileSize(context, uri)
        assertEquals(987_654L, size)
    }

    @Test
    fun `takePersistablePermission catches SecurityException safely and returns false`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri> {
            every { scheme } returns "content"
        }

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } throws SecurityException("No persistable permission grant")

        val result = UriHelper.takePersistablePermission(context, uri)
        assertFalse(result)
    }

    @Test
    fun `takePersistablePermission returns true when permission is granted`() {
        val context = mockk<Context>()
        val contentResolver = mockk<ContentResolver>()
        val uri = mockk<Uri> {
            every { scheme } returns "content"
        }

        every { context.contentResolver } returns contentResolver
        every {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } just Runs

        val result = UriHelper.takePersistablePermission(context, uri)
        assertTrue(result)
    }
}
