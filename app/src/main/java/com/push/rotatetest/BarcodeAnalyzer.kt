package com.push.rotatetest

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.YuvImage
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.io.ByteArrayOutputStream


class BarcodeAnalyzer(val buffer: IBuffer, private val onBarcodeDetected: (Result) -> Unit) : ImageAnalysis.Analyzer {

    interface IBuffer{
        fun size(str: String)
    }

    private val rotationList = listOf(90F, 180F)

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {

            val bitmap = convertYUV420888ToBitmap(imageProxy, buffer)

            var result: Result? = null
            for(degrees in rotationList){
                result = getResultFound(bitmap, degrees)
                if(result!=null) break
            }

            result?.let{
                onBarcodeDetected(it)
            }
            imageProxy.close()
        }
    }

    private fun getResultFound(bitmap: Bitmap, degrees: Float): Result?{
        val rotatedBitmap = rotateBitmap(bitmap, degrees)

        val intArray = IntArray(rotatedBitmap.width * rotatedBitmap.height)
        rotatedBitmap.getPixels(intArray, 0, rotatedBitmap.width, 0, 0, rotatedBitmap.width, rotatedBitmap.height)
        val luminanceSource = RGBLuminanceSource(rotatedBitmap.width, rotatedBitmap.height, intArray)

        val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

        try {
            val reader = MultiFormatReader()
            val result = reader.decode(binaryBitmap)
            return result
        } catch (e: NotFoundException) {
            e.printStackTrace()
            return null //Result("Barcode not found: ${e.message}",null,null,null)
        }catch (e: Exception){
            return null //Result("Error decoding barcode: ${e.message}",null,null,null)
        }
    }

    private fun convertYUV420888ToBitmap(image: ImageProxy, buffer: IBuffer): Bitmap {
        // Логирование размеров буферов
        val bufferSize = """
            Y buffer size: ${image.planes[0].buffer.remaining()}
            U buffer size: ${image.planes[1].buffer.remaining()}
            V buffer size: ${image.planes[2].buffer.remaining()}
        """.trimIndent()
        buffer.size(bufferSize)
//        Log.d("BarcodeAnalyzer", "Y buffer size: ${image.planes[0].buffer.remaining()}")
//        Log.d("BarcodeAnalyzer", "U buffer size: ${image.planes[1].buffer.remaining()}")
//        Log.d("BarcodeAnalyzer", "V buffer size: ${image.planes[2].buffer.remaining()}")

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)

        val vOffset = ySize
        val uOffset = ySize + vSize

        vBuffer.get(nv21, vOffset, vSize)
        uBuffer.get(nv21, uOffset, uSize)

        val yuvImage = YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }


    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
