package com.example.pokerar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.ux.ArFragment
import java.util.concurrent.CompletableFuture

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "PokerAR"
        private const val CAMERA_PERMISSION_CODE = 100
    }

    private lateinit var arFragment: ArFragment
    private var arSession: Session? = null
    private lateinit var pokerDetector: PokerDetector
    private lateinit var characterRenderer: WarriorRenderer
    private lateinit var statusText: TextView
    private var lastDetectedCard: DetectedCard? = null
    private var detectionCooldown = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        
        // 检查 ARCore 支持
        checkARCoreSupport()

        // 检查摄像头权限
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            initializeAR()
        }
    }

    private fun checkARCoreSupport() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // 兼容性检查中
        }
        if (availability.isUnsupported) {
            Log.e(TAG, "设备不支持 ARCore")
            statusText.text = "设备不支持 ARCore"
            finish()
        }
    }

    private fun initializeAR() {
        try {
            arSession = Session(this)
            arFragment = supportFragmentManager.findFragmentById(R.id.arFragment) as ArFragment
            
            pokerDetector = PokerDetector(this)
            characterRenderer = WarriorRenderer(this)

            statusText.text = "AR 已初始化，请放入扑克牌"

            // 设置 ARFragment 的场景更新监听
            arFragment.arSceneView.scene.addOnUpdateListener { frameTime: FrameTime ->
                onArFrameUpdate()
            }

            Log.d(TAG, "AR 初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "AR 初始化失败: ${e.message}")
            statusText.text = "初始化失败: ${e.message}"
        }
    }

    private fun onArFrameUpdate() {
        try {
            val frame = arFragment.arSceneView.arFrame ?: return
            val image = frame.acquireCameraImage()
            
            // 降低检测频率（每 200ms 检测一次）
            val currentTime = System.currentTimeMillis()
            if (currentTime - detectionCooldown < 200) {
                image.close()
                return
            }
            detectionCooldown = currentTime
            
            // 检测扑克牌
            val detectedCard = pokerDetector.detectCard(image)
            
            if (detectedCard != null && (lastDetectedCard == null || 
                lastDetectedCard!!.number != detectedCard.number || 
                lastDetectedCard!!.suit != detectedCard.suit)) {
                
                lastDetectedCard = detectedCard
                
                // 更新 UI
                runOnUiThread {
                    statusText.text = "检测到: ${detectedCard.number} ${getSuitName(detectedCard.suit)}"
                }
                
                // 渲染战士
                characterRenderer.renderWarrior(
                    arFragment.arSceneView,
                    detectedCard.number,
                    detectedCard.suit,
                    frame
                )
                
                Log.d(TAG, "检测到卡牌: ${detectedCard.number} ${detectedCard.suit}")
            } else if (detectedCard == null) {
                lastDetectedCard = null
                runOnUiThread {
                    statusText.text = "未检测到卡牌，请调整位置"
                }
            }
            
            image.close()
        } catch (e: Exception) {
            Log.e(TAG, "处理帧失败: ${e.message}")
        }
    }

    private fun getSuitName(suit: String): String {
        return when (suit) {
            "spade" -> "♠️ 黑桃"
            "heart" -> "♥️ 红桃"
            "diamond" -> "♦️ 方块"
            "club" -> "♣️ 梅花"
            else -> suit
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAR()
            } else {
                Log.e(TAG, "摄像头权限被拒绝")
                statusText.text = "需要摄像头权限"
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        arSession?.resume()
    }

    override fun onPause() {
        super.onPause()
        arSession?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        arSession?.close()
    }
}
