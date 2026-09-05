package com.example.pokerar

import android.content.Context
import android.graphics.Color
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.Frame
import com.google.ar.core.HitTestPoint
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import java.util.concurrent.CompletableFuture

class WarriorRenderer(private val context: Context) {
    
    companion object {
        private const val TAG = "WarriorRenderer"
    }

    private val suitColors = mapOf(
        "spade" to android.graphics.Color.BLACK,           // 黑色
        "heart" to android.graphics.Color.RED,             // 红色
        "diamond" to android.graphics.Color.YELLOW,        // 金色
        "club" to android.graphics.Color.GREEN             // 绿色
    )

    private val suitColorVec3 = mapOf(
        "spade" to Vector3(0f, 0f, 0f),           // 黑色
        "heart" to Vector3(1f, 0f, 0f),           // 红色
        "diamond" to Vector3(1f, 0.84f, 0f),      // 金色
        "club" to Vector3(0f, 0.5f, 0f)           // 绿色
    )

    private var currentAnchorNode: AnchorNode? = null

    /**
     * 渲染数字战士
     */
    fun renderWarrior(
        arSceneView: com.google.ar.sceneform.ArSceneView,
        number: Int,
        suit: String,
        frame: Frame
    ) {
        try {
            // 清除之前的模型
            currentAnchorNode?.let { 
                arSceneView.scene.removeChild(it)
            }

            // 从屏幕中心创建射线，找到平面交点
            val hitTestPoints = frame.hitTest(arSceneView.width / 2f, arSceneView.height / 2f)
            
            var bestHit: com.google.ar.core.HitTestResult? = null
            for (hit in hitTestPoints) {
                if (hit.trackable is Plane) {
                    bestHit = hit
                    break
                }
            }

            if (bestHit != null) {
                val anchor = bestHit.createAnchor()
                createWarrior(
                    arSceneView,
                    number,
                    suit,
                    anchor
                )
            }

            Log.d(TAG, "渲染战士: $number $suit")
        } catch (e: Exception) {
            Log.e(TAG, "渲染失败: ${e.message}")
        }
    }

    /**
     * 创建战士 3D 模型
     */
    private fun createWarrior(
        arSceneView: com.google.ar.sceneform.ArSceneView,
        number: Int,
        suit: String,
        anchor: Anchor
    ) {
        val color = suitColors[suit] ?: android.graphics.Color.WHITE
        val colorVec = suitColorVec3[suit] ?: Vector3(1f, 1f, 1f)
        val arColor = Color(android.graphics.Color.red(color) / 255f, 
                           android.graphics.Color.green(color) / 255f,
                           android.graphics.Color.blue(color) / 255f)

        // 创建锚点节点
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arSceneView.scene)
        
        currentAnchorNode = anchorNode

        // 创建主体（身体）
        createBodyParts(context, anchorNode, arColor, number, colorVec)
    }

    /**
     * 创建战士的身体部分
     */
    private fun createBodyParts(
        context: Context,
        parentNode: AnchorNode,
        color: Color,
        number: Int,
        colorVec: Vector3
    ) {
        try {
            // 创建身体（大球）
            MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { bodyMaterial ->
                val bodyRenderable = ShapeFactory.makeSphere(
                    0.15f,
                    Vector3(0f, 0f, 0f),
                    bodyMaterial
                )

                val bodyNode = Node()
                bodyNode.setParent(parentNode)
                bodyNode.renderable = bodyRenderable

                // 头部（小球）
                MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { headMaterial ->
                    val headRenderable = ShapeFactory.makeSphere(
                        0.08f,
                        Vector3(0f, 0.15f, 0f),
                        headMaterial
                    )

                    val headNode = Node()
                    headNode.setParent(bodyNode)
                    headNode.localPosition = Vector3(0f, 0.23f, 0f)
                    headNode.renderable = headRenderable

                    // 在头顶显示数字（使用文本网格）
                    addNumberDisplay(context, headNode, number, colorVec)
                }

                // 左臂
                MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { armMaterial ->
                    val armRenderable = ShapeFactory.makeCylinder(
                        0.03f,
                        0.15f,
                        Vector3(0f, 0f, 0f),
                        armMaterial
                    )

                    val leftArmNode = Node()
                    leftArmNode.setParent(bodyNode)
                    leftArmNode.localPosition = Vector3(-0.1f, 0.05f, 0f)
                    leftArmNode.localRotation = Quaternion.axisAngle(Vector3(0f, 0f, 1f), 45f)
                    leftArmNode.renderable = armRenderable
                }

                // 右臂
                MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { armMaterial ->
                    val armRenderable = ShapeFactory.makeCylinder(
                        0.03f,
                        0.15f,
                        Vector3(0f, 0f, 0f),
                        armMaterial
                    )

                    val rightArmNode = Node()
                    rightArmNode.setParent(bodyNode)
                    rightArmNode.localPosition = Vector3(0.1f, 0.05f, 0f)
                    rightArmNode.localRotation = Quaternion.axisAngle(Vector3(0f, 0f, 1f), -45f)
                    rightArmNode.renderable = armRenderable
                }

                // 左腿
                MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { legMaterial ->
                    val legRenderable = ShapeFactory.makeCylinder(
                        0.03f,
                        0.12f,
                        Vector3(0f, 0f, 0f),
                        legMaterial
                    )

                    val leftLegNode = Node()
                    leftLegNode.setParent(bodyNode)
                    leftLegNode.localPosition = Vector3(-0.05f, -0.15f, 0f)
                    leftLegNode.renderable = legRenderable
                }

                // 右腿
                MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { legMaterial ->
                    val legRenderable = ShapeFactory.makeCylinder(
                        0.03f,
                        0.12f,
                        Vector3(0f, 0f, 0f),
                        legMaterial
                    )

                    val rightLegNode = Node()
                    rightLegNode.setParent(bodyNode)
                    rightLegNode.localPosition = Vector3(0.05f, -0.15f, 0f)
                    rightLegNode.renderable = legRenderable
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建身体失败: ${e.message}")
        }
    }

    /**
     * 添加数字显示
     */
    private fun addNumberDisplay(
        context: Context,
        parentNode: Node,
        number: Int,
        colorVec: Vector3
    ) {
        try {
            // 创建一个简单的立方体来显示数字背景
            val numberStr = if (number == 1) "A" else number.toString()
            
            MaterialFactory.makeOpaqueWithColor(
                context,
                Color(colorVec.x, colorVec.y, colorVec.z, 0.9f)
            ).thenAccept { material ->
                val cubeRenderable = ShapeFactory.makeCube(
                    Vector3(0.08f, 0.08f, 0.01f),
                    Vector3(0f, 0f, 0f),
                    material
                )

                val numberNode = Node()
                numberNode.setParent(parentNode)
                numberNode.localPosition = Vector3(0f, 0.1f, 0.05f)
                numberNode.renderable = cubeRenderable
            }

            Log.d(TAG, "显示数字: $numberStr")
        } catch (e: Exception) {
            Log.e(TAG, "添加数字失败: ${e.message}")
        }
    }

    /**
     * 获取花色名称
     */
    fun getSuitName(suit: String): String {
        return when (suit) {
            "spade" -> "♠️"
            "heart" -> "♥️"
            "diamond" -> "♦️"
            "club" -> "♣️"
            else -> suit
        }
    }
}
