package com.scanflow.photocompressor.work

import android.net.Uri
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.scanflow.photocompressor.domain.model.*
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BackgroundJobManagerTest {

    private lateinit var context: android.content.Context
    private lateinit var workManager: WorkManager
    private lateinit var manager: BackgroundJobManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        manager = BackgroundJobManager(context, workManager)
    }

    @Test
    fun `enqueueBatch builds and enqueues OneTimeWorkRequest with tags`() {
        val uri1 = mockk<Uri>(relaxed = true)
        every { uri1.toString() } returns "content://media/1"
        val uri2 = mockk<Uri>(relaxed = true)
        every { uri2.toString() } returns "content://media/2"

        val job = BatchJob(
            items = listOf(BatchItem(sourceUri = uri1), BatchItem(sourceUri = uri2)),
            operation = ImagePipeline(listOf(ImageOperation.Compress(75)))
        )

        val workSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workSlot)) } returns mockk()

        val id = manager.enqueueBatch(job)

        assertNotNull(id)
        verify(exactly = 1) { workManager.enqueue(any<OneTimeWorkRequest>()) }

        val captured = workSlot.captured
        assertTrue(captured.tags.contains("batch_processing"))
        assertTrue(captured.tags.contains(job.id))
        assertEquals(75, captured.workSpec.input.getInt(WorkKeys.KEY_QUALITY, -1))
    }

    @Test
    fun `enqueueImageProcessing builds and enqueues request with image_processing tag`() {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://media/image"
        val pipeline = ImagePipeline(listOf(ImageOperation.Convert(ImageFormat.WEBP)))

        val workSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workSlot)) } returns mockk()

        val id = manager.enqueueImageProcessing(uri, pipeline)

        assertNotNull(id)
        val captured = workSlot.captured
        assertTrue(captured.tags.contains("image_processing"))
        assertEquals("WEBP", captured.workSpec.input.getString(WorkKeys.KEY_FORMAT))
    }

    @Test
    fun `enqueuePdfCreation builds and enqueues request with pdf_creation tag`() {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://media/doc_page_1"

        val workSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(workSlot)) } returns mockk()

        val id = manager.enqueuePdfCreation(listOf(uri), "TestInvoice")

        assertNotNull(id)
        val captured = workSlot.captured
        assertTrue(captured.tags.contains("pdf_creation"))
        assertEquals("TestInvoice", captured.workSpec.input.getString(WorkKeys.KEY_PDF_TITLE))
    }

    @Test
    fun `cancelWork delegates to workManager cancelWorkById`() {
        val id = UUID.randomUUID()
        every { workManager.cancelWorkById(id) } returns mockk()

        manager.cancelWork(id)

        verify(exactly = 1) { workManager.cancelWorkById(id) }
    }

    @Test
    fun `cancelAllWorkByTag delegates to workManager cancelAllWorkByTag`() {
        every { workManager.cancelAllWorkByTag("batch_processing") } returns mockk()

        manager.cancelAllWorkByTag("batch_processing")

        verify(exactly = 1) { workManager.cancelAllWorkByTag("batch_processing") }
    }
}
