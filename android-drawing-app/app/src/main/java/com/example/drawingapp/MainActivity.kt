package com.example.drawingapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.drawingapp.databinding.ActivityMainBinding
import com.example.drawingapp.fragments.ColorPickerFragment
import com.example.drawingapp.fragments.DrawFragment
import com.example.drawingapp.fragments.ShapePickerFragment
import com.example.drawingapp.fragments.DrawingListFragment
import com.example.drawingapp.room.CanvasApplication
import com.example.drawingapp.room.CanvasEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.log
import com.example.drawingapp.fragments.loginFragment

class MainActivity : AppCompatActivity(), ColorPickerFragment.ColorPickerListener,
    ShapePickerFragment.ShapePickerListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    val viewModel: DrawingViewModel by viewModels{
        DrawingViewModelFactory((application as CanvasApplication).canvasRepository)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        Thread.sleep(2500)
        installSplashScreen()
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)
        auth = Firebase.auth

        if (auth.currentUser == null) {
            hideDrawingControls()
            supportFragmentManager.commit {
                replace(R.id.fragmentContainerView, loginFragment())
            }
        } else {
            initializeUI()
            tryDownloadFromCloud()
            createInitialCanvas()
            supportFragmentManager.commit {
                replace(R.id.fragmentContainerView, DrawFragment())
            }
            showDrawingControls()
        }

        binding.LOAD.isEnabled = false
        binding.LOAD.visibility = View.INVISIBLE

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentResumed(fm, f)

                    if (f is DrawingListFragment) {
                        viewModel.allCanvases.observe(this@MainActivity) { canvases ->
                            binding.delete.isEnabled = canvases.isNotEmpty()
                        }
                    } else {
                        binding.delete.isEnabled = false
                    }

                    // 根据当前 Fragment 显示或隐藏按钮
                    if (f is DrawFragment || f is DrawingListFragment || f is ShapePickerFragment || f is ColorPickerFragment) {
                        showDrawingControls()
                    } else {
                        hideDrawingControls()
                    }
                }
            }, true
        )
    }
    internal fun createInitialCanvas() {
        val emptyBitmap = Bitmap.createBitmap(1100, 1100, Bitmap.Config.ARGB_8888)
        val initialCanvas = CanvasEntity(
            id = -1,  // 使用-1作为初始ID，让Room自动生成新的ID
            filePath = "canvas_${System.currentTimeMillis()}.png",
            bitmap = emptyBitmap
        )
        viewModel.setCurrentCanvas(initialCanvas)
    }

    private fun initializeUI() {
        binding.delete.isEnabled = false
        binding.addNew.isEnabled = false

        val t = viewModel.latestCanvas.value?.id.toString()
        val t2 = viewModel.latestCanvas.value?.filePath
        Log.e("T", "" + t + " " + t2)

        viewModel.currentCanvas.observe(this) { current ->
            current?.let {
                Log.e("T", "Current Canvas ID: ${it.id}")
            }
        }

//        viewModel.allCanvases.observe(this) { canvases ->
//            binding.delete.isEnabled = canvases.isNotEmpty()
//        }

        supportFragmentManager.commit {
            replace(R.id.fragmentContainerView, DrawFragment())
        }

        // 设置所有按钮的点击监听器
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.penSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = seekBar?.progress ?: 20
                viewModel.penSize.value = progress.toFloat()
            }
        })

        binding.clear.setOnClickListener {
            viewModel.clearCanvas()
        }

        binding.save.setOnClickListener {
            val currId = viewModel.currentCanvas.value?.id
            val updatedBitmap = viewModel.currentCanvas.value?.bitmap

            if (currId != null && updatedBitmap != null) {
                Log.e("Save", "Saving canvas with ID: $currId")
                if (currId == -1) {
                    // observe the latest canvas
                    viewModel.saveFirst { newCanvas ->
                        newCanvas?.let {
                            viewModel.setCurrentCanvas(newCanvas)
                            Log.d("Save", "New canvas saved with ID: ${newCanvas.id}")
                        }
                    }
                    binding.delete.isEnabled = false
                } else {
                    // update an existing canvas
                    viewModel.updateCanvas(currId, updatedBitmap)
                    Log.d("Save", "Canvas updated with ID: $currId")
                    binding.delete.isEnabled = false
                }
            } else {
                Log.e("Save", "Cannot update: current canvas or bitmap is null")
                binding.delete.isEnabled = false
            }
        }


//        binding.delete.setOnClickListener {
//            viewModel.deleteCanvas()
//            viewModel.latestCanvas.observeOnce(this) { canvas ->
//                canvas.let {
//                    viewModel.setCurrentCanvas(canvas)
//                }
//            }
//        }
        binding.delete.setOnClickListener {
            viewModel.deleteCanvas { nextCanvas ->
                if (nextCanvas != null) {
                    Log.d("Delete", "Switched to new canvas with ID: ${nextCanvas.id}")
                } else {
                    Log.e("Delete", "No canvases available after deletion")
                }
            }
        }


        binding.Cloud.setOnClickListener {
            uploadLocalDataToCloud { success ->
                if (success) {
                    Toast.makeText(
                        this,
                        "Successfully uploaded to cloud",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to upload some data",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        binding.addNew.setOnClickListener {
            viewModel.addCanvas()
        }

        binding.pen.setOnClickListener {
            binding.color.isEnabled = true
            viewModel.setShape("pen")
            viewModel.switchTool(0)
        }

        binding.eraser.setOnClickListener {
            binding.color.isEnabled = false
            viewModel.switchTool(1)
        }

        binding.pour.setOnClickListener {
            binding.color.isEnabled = true
            viewModel.switchTool(8)
        }

        binding.logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            clearLocalData()
            handleLogout()
            hideDrawingControls()
            supportFragmentManager.commit {
                replace(R.id.fragmentContainerView, loginFragment())
                addToBackStack(null)
            }
        }
        binding.shape.setOnClickListener {
            val shapePickerFragment = ShapePickerFragment()
            shapePickerFragment.show(supportFragmentManager, "shapePicker")
        }
        binding.LOAD.setOnClickListener{
            tryDownloadFromCloud()
        }
        binding.color.setOnClickListener {
            val colorPickerFragment = ColorPickerFragment()
            colorPickerFragment.show(supportFragmentManager, "colorPicker")
        }

        binding.navigationButton.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
            when (currentFragment) {
                is DrawFragment -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainerView, DrawingListFragment())
                        addToBackStack(null)
//                        viewModel.allCanvases.value?.let {
//                            binding.delete.isEnabled = it.isNotEmpty()
//                        }
                        binding.addNew.isEnabled = true
                    }
                    binding.navigationButton.text = "Canvas"
                }
                is DrawingListFragment -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainerView, DrawFragment())
                        addToBackStack(null)
                    }
                    binding.navigationButton.text = "Drawing List"
                    binding.delete.isEnabled = false
                    binding.addNew.isEnabled = false
                }
                else -> {
                    supportFragmentManager.commit {
                        replace(R.id.fragmentContainerView, DrawFragment())
                        addToBackStack(null)
                    }
                    binding.navigationButton.text = "Canvas List"
                    binding.delete.isEnabled = false
                    binding.addNew.isEnabled = false
                }
            }
        }
    }
    override fun onShapeSelected(shape: String) {
        when (shape) {
            "circle" -> {
                binding.color.isEnabled = true
                viewModel.setShape("circle")
                viewModel.switchTool(0)
            }
            "rectangle" -> {
                binding.color.isEnabled = true
                viewModel.setShape("rectangle")
                viewModel.switchTool(0)
            }
        }
    }

    private fun hideDrawingControls() {
        // 隐藏所有绘画相关的控件
        binding.apply {
            shape.visibility = View.GONE
            pour.visibility = View.GONE
            clear.visibility = View.GONE
            pen.visibility = View.GONE
            eraser.visibility = View.GONE
            color.visibility = View.GONE
            save.visibility = View.GONE
            delete.visibility = View.GONE
            addNew.visibility = View.GONE
            navigationButton.visibility = View.GONE
            penSize.visibility = View.GONE
            logout.visibility = View.GONE
        }
    }
    private fun clearLocalData() {
        try {
            // 清除 ViewModel 中的数据
            viewModel.clearAllLocalCanvases{}
            viewModel.clearCanvas()



            // 删除文件目录中的所有文件
            val filesDir = applicationContext.filesDir
            filesDir.listFiles()?.forEach { file ->
                if (!file.delete()) {
                    Log.w("ClearData", "Failed to delete file: ${file.name}")
                }
            }

            // 清空 SharedPreferences
            val sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            Log.d("ClearData", "Successfully cleared all local data")
        } catch (e: Exception) {
            Log.e("ClearData", "Error clearing local data", e)
            Toast.makeText(
                this,
                "Error clearing local data",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun convertBitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun handleLogout() {
        // 先上传所有本地数据到云端
        uploadLocalDataToCloud { success ->
            if (success) {
                // 上传成功后清除本地数据
                clearLocalData()
                // 登出
                FirebaseAuth.getInstance().signOut()
                // 切换到登录界面
                hideDrawingControls()
                supportFragmentManager.commit {
                    replace(R.id.fragmentContainerView, loginFragment())
                    addToBackStack(null)
                }
                viewModel.allCanvases.observeOnce(this@MainActivity) { canvases ->
                    if (canvases.isEmpty()) {
                        Log.d("DatabaseStatus", "All canvases deleted successfully")
                        createInitialCanvas()
                    } else {
                        Log.e("DatabaseStatus", "Database still contains canvases: $canvases")
                    }
                }

            } else {
                // 如果上传失败，提示用户
                Toast.makeText(
                    this,
                    "Failed to sync some data to cloud. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        createInitialCanvas()

    }

    private fun uploadLocalDataToCloud(callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        val db = Firebase.firestore
        val storage = Firebase.storage

        viewModel.allCanvases.value?.let { canvases ->
            if (canvases.isEmpty()) {
                callback(true)
                return@let
            }

            var uploadedCount = 0
            var hasError = false

            canvases.forEach { canvas ->
                // 构建 Firebase Storage 路径
                val fileRef = storage.reference.child("drawings/$userId/canvases/${canvas.id}.png")

                // 将 Bitmap 转换为字节流
                val byteArray = convertBitmapToByteArray(canvas.bitmap)

                // 上传到 Firebase Storage
                fileRef.putBytes(byteArray)
                    .addOnSuccessListener {
                        // 上传成功后保存元数据到 Firestore
                        val canvasData = mapOf(
                            "id" to canvas.id,
                            "filePath" to "drawings/$userId/canvases/${canvas.id}.png",
                            "timestamp" to System.currentTimeMillis()
                        )

                        db.collection("users")
                            .document(userId)
                            .collection("canvases")
                            .document(canvas.id.toString())
                            .set(canvasData)
                            .addOnSuccessListener {
                                uploadedCount++
                                if (uploadedCount == canvases.size) {
                                    callback(!hasError)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("CloudUpload", "Error uploading canvas metadata", e)
                                hasError = true
                                uploadedCount++
                                if (uploadedCount == canvases.size) {
                                    callback(!hasError)
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CloudUpload", "Error uploading file to Storage", e)
                        hasError = true
                        uploadedCount++
                        if (uploadedCount == canvases.size) {
                            callback(!hasError)
                        }
                    }
            }
        } ?: callback(false)
    }
    fun tryDownloadFromCloud() {
        val userId = auth.currentUser?.uid ?: return
        val db = Firebase.firestore
        val storage = Firebase.storage

        // 不再清除本地数据
        db.collection("users")
            .document(userId)
            .collection("canvases")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("CloudDownload", "No documents found in cloud")
                    return@addOnSuccessListener
                }

                var downloadedCount = 0
                val totalCount = documents.size()

                documents.forEach { document ->
                    val canvasId = document.getLong("id")?.toInt() ?: return@forEach
                    val filePath = document.getString("filePath") ?: return@forEach

                    val fileRef = storage.reference.child(filePath)
                    val MAX_FILE_SIZE: Long = 640 * 1024

                    fileRef.getBytes(MAX_FILE_SIZE)
                        .addOnSuccessListener { bytes ->
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            // 创建画布实体
                            val canvasEntity = CanvasEntity(
                                id = canvasId,
                                filePath = filePath,
                                bitmap = bitmap
                            )

                            // 添加到本地数据库，但不设置为当前画布
                            viewModel.addCanvasToList(canvasEntity)

                            downloadedCount++
                            Log.d("CloudDownload", "Downloaded canvas $canvasId ($downloadedCount/$totalCount)")

                            // 下载完成后显示提示
                            if (downloadedCount == totalCount) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Successfully loaded all drawings from cloud",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("CloudDownload", "Failed to download canvas $canvasId", e)
                            downloadedCount++
                            if (downloadedCount == totalCount) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Some drawings failed to load",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("CloudDownload", "Error getting canvas metadata", e)
                Toast.makeText(
                    this,
                    "Failed to connect to cloud",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    private fun showDrawingControls() {
        // 显示所有绘画相关的控件
        binding.apply {
            shape.visibility = View.VISIBLE
            pour.visibility = View.VISIBLE
            clear.visibility = View.VISIBLE
            pen.visibility = View.VISIBLE
            eraser.visibility = View.VISIBLE
            color.visibility = View.VISIBLE
            save.visibility = View.VISIBLE
            delete.visibility = View.VISIBLE
            addNew.visibility = View.VISIBLE
            navigationButton.visibility = View.VISIBLE
            penSize.visibility = View.VISIBLE
            logout.visibility = View.VISIBLE
        }
    }

    override fun onColorSelected(color: Int) {
        viewModel.setColor(color)
    }
}

inline fun <T> LiveData<T>.observeOnce(owner: LifecycleOwner, crossinline observer: (T) -> Unit) {
    observe(owner, object : Observer<T> {
        override fun onChanged(t: T) {
            observer(t)
            removeObserver(this)
        }
    })
}