package com.atveyn.app

import android.graphics.BitmapFactory
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrDumpTest {

    @Test
    fun dumpSampleOcr() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inputStream = context.assets.open("sample_timetable.jpg")
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = Tasks.await(recognizer.process(image))
        Log.i("OCR_TEST", "=== FULL OCR TEXT START ===")
        Log.i("OCR_TEST", result.text)
        Log.i("OCR_TEST", "=== FULL OCR TEXT END ===")

        Log.i("OCR_TEST", "=== BLOCKS & BOXES START ===")
        for ((bIdx, block) in result.textBlocks.withIndex()) {
            val bBox = block.boundingBox
            Log.i("OCR_TEST", "BLOCK #$bIdx [${bBox?.left},${bBox?.top}-${bBox?.right},${bBox?.bottom}]: ${block.text.replace("\n", " | ")}")
            for ((lIdx, line) in block.lines.withIndex()) {
                val lBox = line.boundingBox
                Log.i("OCR_TEST", "   LINE #$lIdx [${lBox?.left},${lBox?.top}-${lBox?.right},${lBox?.bottom}]: ${line.text}")
            }
        }
        Log.i("OCR_TEST", "=== BLOCKS & BOXES END ===")
    }
}
