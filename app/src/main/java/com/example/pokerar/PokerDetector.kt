package com.example.pokerar

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

data class DetectedCard(
    val number: Int,  // 1-10 (A=1, 2-10)
    val suit: String, // "spade", "heart", "diamond", "club"
    val confidence: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f
)

class PokerDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "PokerDetector"
    }

    private val digitPatterns = mapOf(
        1 to intArrayOf(1, 0, 0, 0, 0),     // A - 单个符号
        2 to intArrayOf(1, 1, 0, 0, 0),     // 2 - 两个角
        3 to intArrayOf(1, 1, 1, 0, 0),     // 3
        4 to intArrayOf(1, 1, 1, 1, 0),     // 4
        5 to intArrayOf(1, 1, 1, 1, 1),     // 5
        6 to intArrayOf(1, 1, 0, 0, 1),     // 6
        7 to intArrayOf(1, 1, 0, 1, 1),     // 7
        8 to intArrayOf(1, 1, 1, 0, 1),     // 8
        9 to intArrayOf(1, 0, 1, 1, 1),     // 9
        10 to intArrayOf(1, 0, 0, 1, 1)     // 10
    )

    /**
     * 检测图像中的扑克牌
     */
    fun detectCard(image: Image): DetectedCard? {
        try {
            // 1. 将 Image 转换为 Mat
            val mat = imageToMat(image)
            
            // 2. 预处理
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            
            // 3. 增强对比度
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(5.0, 5.0))
            val processed = Mat()
            Imgproc.morphologyEx(gray, processed, Imgproc.MORPH_CLOSE, kernel)
            Imgproc.morphologyEx(processed, processed, Imgproc.MORPH_OPEN, kernel)
            
            // 4. 边缘检测
            val edges = Mat()
            Imgproc.Canny(processed, edges, 50.0, 150.0)
            
            // 5. 寻找轮廓
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
            
            // 6. 过滤卡牌轮廓
            val cardContour = filterCardContour(contours, mat.size())
            
            if (cardContour != null) {
                // 7. 从轮廓提取卡牌信息
                val card = extractCardInfo(mat, cardContour)
                
                mat.release()
                gray.release()
                processed.release()
                edges.release()
                kernel.release()
                hierarchy.release()
                
                return card
            }
            
            mat.release()
            gray.release()
            processed.release()
            edges.release()
            kernel.release()
            hierarchy.release()
            
            return null
        } catch (e: Exception) {
            Log.e(TAG, "检测失败: ${e.message}")
            return null
        }
    }

    /**
     * 将 Android Image 转换为 OpenCV Mat
     */
    private fun imageToMat(image: Image): Mat {
        val planes = image.planes
        val ySize = planes[0].buffer.remaining()
        val uvSize = planes[1].buffer.remaining() + planes[2].buffer.remaining()
        
        val data = ByteArray(ySize + uvSize)
        planes[0].buffer.get(data, 0, ySize)
        planes[1].buffer.get(data, ySize, planes[1].buffer.remaining())
        planes[2].buffer.get(data, ySize + planes[1].buffer.remaining(), planes[2].buffer.remaining())
        
        val mat = Mat(image.height + image.height / 2, image.width, CvType.CV_8UC1)
        mat.put(0, 0, data)
        
        val rgbaMat = Mat()
        Imgproc.cvtColor(mat, rgbaMat, Imgproc.COLOR_YUV2RGBA_NV21)
        mat.release()
        
        return rgbaMat
    }

    /**
     * 过滤出有效的卡牌轮廓
     */
    private fun filterCardContour(contours: List<MatOfPoint>, imageSize: Size): MatOfPoint? {
        var largestCardContour: MatOfPoint? = null
        var largestArea = 0.0
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            val minArea = imageSize.width * imageSize.height * 0.05
            val maxArea = imageSize.width * imageSize.height * 0.9
            
            if (area > minArea && area < maxArea) {
                val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
                val approx = Mat()
                Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)
                
                // 卡牌应该是四边形
                if (approx.rows() >= 4 && area > largestArea) {
                    val rect = Imgproc.boundingRect(contour)
                    // 检查宽高比（卡牌通常是 2:3 比例）
                    val aspectRatio = rect.width.toFloat() / rect.height.toFloat()
                    if (aspectRatio > 0.4f && aspectRatio < 0.8f) {
                        largestArea = area
                        largestCardContour = contour
                    }
                }
                approx.release()
            }
        }
        
        return largestCardContour
    }

    /**
     * 从轮廓提取卡牌信息
     */
    private fun extractCardInfo(mat: Mat, contour: MatOfPoint): DetectedCard? {
        val rect = Imgproc.boundingRect(contour)
        
        // 提取卡牌区域
        val x = maxOf(0, rect.x)
        val y = maxOf(0, rect.y)
        val width = minOf(rect.width, mat.cols() - x)
        val height = minOf(rect.height, mat.rows() - y)
        
        if (width <= 0 || height <= 0) return null
        
        val cardRegion = Mat(mat, Rect(x, y, width, height))
        
        // 识别花色
        val suit = detectSuit(cardRegion)
        
        // 识别数字
        val number = detectNumber(cardRegion)
        
        cardRegion.release()
        
        return if (number in 1..10 && suit.isNotEmpty()) {
            DetectedCard(
                number = number,
                suit = suit,
                confidence = 0.8f,
                x = x.toFloat(),
                y = y.toFloat(),
                width = width.toFloat(),
                height = height.toFloat()
            )
        } else {
            null
        }
    }

    /**
     * 识别花色
     */
    private fun detectSuit(cardRegion: Mat): String {
        try {
            // 提取左上角（花色位置）
            val cornerHeight = cardRegion.rows() / 6
            val cornerWidth = cardRegion.cols() / 6
            
            if (cornerHeight <= 0 || cornerWidth <= 0) return ""
            
            val topLeft = Mat(cardRegion, Rect(0, 0, cornerWidth, cornerHeight))
            
            // 转换为 HSV 分析颜色
            val hsv = Mat()
            Imgproc.cvtColor(topLeft, hsv, Imgproc.COLOR_RGBA2HSV)
            
            // 计算平均色调
            val mean = Core.mean(hsv)
            val hue = mean[0]  // H 值 (0-180)
            val saturation = mean[1]  // S 值
            val value = mean[2]  // V 值
            
            Log.d(TAG, "颜色 - H: $hue, S: $saturation, V: $value")
            
            val suit = when {
                // 红色 (红桃/方块)
                (hue < 10 || hue > 170) && saturation > 100 -> {
                    // 区分红桃和方块的方式可以优化
                    if (saturation > 150) "heart" else "diamond"
                }
                // 黑色 (黑桃)
                value < 100 -> "spade"
                // 绿色 (梅花)
                (hue in 80.0..100.0) -> "club"
                // 黄色/金色 (也作为方块)
                (hue in 20.0..40.0) -> "diamond"
                else -> "spade"  // 默认黑桃
            }
            
            topLeft.release()
            hsv.release()
            
            return suit
        } catch (e: Exception) {
            Log.e(TAG, "花色识别失败: ${e.message}")
            return "spade"
        }
    }

    /**
     * 识别数字
     */
    private fun detectNumber(cardRegion: Mat): Int {
        try {
            // 提取左上角数字区域
            val numberRegion = Mat(
                cardRegion,
                Rect(0, 0, cardRegion.cols() / 4, cardRegion.rows() / 4)
            )
            
            // 转为灰度
            val gray = Mat()
            Imgproc.cvtColor(numberRegion, gray, Imgproc.COLOR_RGBA2GRAY)
            
            // 二值化
            val binary = Mat()
            Imgproc.threshold(gray, binary, 100.0, 255.0, Imgproc.THRESH_BINARY)
            
            // 寻找数字轮廓
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
            
            // 根据轮廓数和面积估测数字
            var digitCount = 0
            var totalArea = 0.0
            
            for (contour in contours) {
                val area = Imgproc.contourArea(contour)
                if (area > 10) {
                    digitCount++
                    totalArea += area
                }
            }
            
            Log.d(TAG, "轮廓数: $digitCount, 总面积: $totalArea")
            
            val number = when {
                digitCount < 3 -> 1  // A - 简单
                digitCount < 5 -> 2
                digitCount < 8 -> 3
                digitCount < 12 -> 4
                digitCount < 16 -> 5
                digitCount < 20 -> 6
                digitCount < 24 -> 7
                digitCount < 28 -> 8
                digitCount < 32 -> 9
                else -> 10
            }
            
            numberRegion.release()
            gray.release()
            binary.release()
            hierarchy.release()
            
            return number
        } catch (e: Exception) {
            Log.e(TAG, "数字识别失败: ${e.message}")
            return 1
        }
    }
}
