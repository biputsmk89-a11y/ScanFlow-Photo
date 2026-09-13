package com.scanflow.photocompressor.work

import android.content.Context
import android.net.Uri
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.scanflow.photocompressor.data.storage.FileManager
import com.scanflow.photocompressor.engine.ImageEngine
import com.scanflow.photocompressor.domain.model.ProcessingResult
import com.scanflow.photocompressor.domain.repository.HistoryRepository
import dagger.hilt.android.EntryPointAccessors
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class BatchProcessingWorkerTest {

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var entryPoint: WorkManagerEntryPoint
    private lateinit var imageEngine: ImageEngine
    private lateinit var fileManager: FileManager
    private lateinit var historyRepository: HistoryRepository

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        entryPoint = mockk()
        imageEngine = mockk()
        fileManager = mockk(relaxed = true)
        historyRepository = mockk(relaxed = true)

        every { entryPoint.imageEngine() } returns imageEngine
        every { entryPoint.fileManager() } returns fileManager
        every { entryPoint.historyRepository() } returns historyRepository

        mockkStatic(EntryPointAccessors::class)
        every {
            EntryPointAccessors.fromApplication(any(), WorkManagerEntryPoint::class.java)
        } returns entryPoint

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val str = firstArg<String>()
            val u = mockk<Uri>()
            every { u.toString() } returns str
            every { u.lastPathSegment } returns str.substringAfterLast('/')
            u
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `doWork returns success with 0 counts when input URIs empty`() = runTest {
        every { workerParams.inputData } returns workDataOf()

        val worker = spyk(BatchProcessingWorker(context, workerParams))
        coEvery { worker.setProgress(any()) } returns Unit

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val output = (result as ListenableWorker.Result.Success).outputData
        assertEquals(0, output.getInt(WorkKeys.KEY_TOTAL_COUNT, -1))
        assertEquals(0, output.getInt(WorkKeys.KEY_SUCCESS_COUNT, -1))
    }

    @Test
    fun `Batch Failure Policy 29 - 100 input, 98 success, 1 failed, 1 cancelled produces valid success result`() = runTest {
        // Setup 100 items
        val uriStrings = (1..100).map { "content://media/external/images/media/$it" }.toTypedArray()
        every { workerParams.inputData } returns workDataOf(
            WorkKeys.KEY_SOURCE_URIS to uriStrings
        )

        val worker = spyk(BatchProcessingWorker(context, workerParams))
        coEvery { worker.setProgress(any()) } returns Unit

        // First 98 succeed, 99th fails (corrupt image), 100th is cancelled (isStopped becomes true)
        for (i in 1..98) {
            val outUri = mockk<Uri>()
            every { outUri.toString() } returns "file:///storage/out_$i.jpg"
            every { outUri.lastPathSegment } returns "out_$i.jpg"

            coEvery {
                imageEngine.process(match { it.toString() == uriStrings[i - 1] }, any())
            } returns ProcessingResult(
                outputUri = outUri,
                outputMimeType = "image/jpeg",
                outputBytes = 500_000L,
                width = 1920,
                height = 1080,
                originalBytes = 1_000_000L,
                reductionPercent = 50f,
                processingTimeMs = 100L
            )
        }

        // 99th item throws IOException (corrupt file)
        coEvery {
            imageEngine.process(match { it.toString() == uriStrings[98] }, any())
        } throws IOException("Corrupted image stream")

        // When item 100 is reached, worker isStopped = true
        every { worker.isStopped } returnsMany listOf(
            // items 1 to 99 are not stopped (false x 99)
            *(Array(99) { false }),
            // item 100 is stopped
            true
        )

        val result = worker.doWork()

        // RULE 29: Overall batch result MUST BE SUCCESS and valid
        assertTrue(
            "Batch must return Result.Success even with 1 failure and 1 cancellation",
            result is ListenableWorker.Result.Success
        )

        val output = (result as ListenableWorker.Result.Success).outputData
        assertEquals(100, output.getInt(WorkKeys.KEY_TOTAL_COUNT, 0))
        assertEquals(98, output.getInt(WorkKeys.KEY_SUCCESS_COUNT, 0))
        assertEquals(1, output.getInt(WorkKeys.KEY_FAILED_COUNT, 0))
        assertEquals(1, output.getInt(WorkKeys.KEY_CANCELLED_COUNT, 0))

        val outputUris = output.getStringArray(WorkKeys.KEY_OUTPUT_URIS)
        assertNotNull(outputUris)
        assertEquals(98, outputUris!!.size)

        // Verify temp files were cleaned up in finally
        verify(atLeast = 1) { fileManager.cleanTempFiles() }
    }

    @Test
    fun `single corrupt image does not abort the entire batch`() = runTest {
        val uriStrings = arrayOf(
            "content://media/img1.jpg",
            "content://media/corrupt.jpg",
            "content://media/img3.jpg"
        )
        every { workerParams.inputData } returns workDataOf(
            WorkKeys.KEY_SOURCE_URIS to uriStrings
        )

        val out1 = mockk<Uri>()
        every { out1.toString() } returns "file:///storage/out_1.jpg"
        every { out1.lastPathSegment } returns "out_1.jpg"

        val out3 = mockk<Uri>()
        every { out3.toString() } returns "file:///storage/out_3.jpg"
        every { out3.lastPathSegment } returns "out_3.jpg"

        coEvery {
            imageEngine.process(match { it.toString() == uriStrings[0] }, any())
        } returns ProcessingResult(out1, "image/jpeg", 500L, 100, 100, 1000L, 50f, 10L)

        coEvery {
            imageEngine.process(match { it.toString() == uriStrings[1] }, any())
        } throws IllegalArgumentException("Corrupt EXIF or decode failure")

        coEvery {
            imageEngine.process(match { it.toString() == uriStrings[2] }, any())
        } returns ProcessingResult(out3, "image/jpeg", 500L, 100, 100, 1000L, 50f, 10L)

        val worker = spyk(BatchProcessingWorker(context, workerParams))
        coEvery { worker.setProgress(any()) } returns Unit

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val output = (result as ListenableWorker.Result.Success).outputData
        assertEquals(3, output.getInt(WorkKeys.KEY_TOTAL_COUNT, 0))
        assertEquals(2, output.getInt(WorkKeys.KEY_SUCCESS_COUNT, 0))
        assertEquals(1, output.getInt(WorkKeys.KEY_FAILED_COUNT, 0))
        assertEquals(0, output.getInt(WorkKeys.KEY_CANCELLED_COUNT, 0))

        val outputs = output.getStringArray(WorkKeys.KEY_OUTPUT_URIS)
        assertEquals(2, outputs?.size)
        assertEquals("file:///storage/out_1.jpg", outputs?.get(0))
        assertEquals("file:///storage/out_3.jpg", outputs?.get(1))

        // Verify temp cleaning
        verify(atLeast = 1) { fileManager.cleanTempFiles() }
    }
}
