package com.example.drawaingupdate

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var saveRelative: RelativeLayout
    private lateinit var colorPallet: LinearLayout
    private lateinit var eraserButton: ImageButton

    private lateinit var currentColorButton: ImageButton
    private lateinit var brushSizeSelectButton: ImageButton
    private lateinit var img: ImageView
    private lateinit var undoButton: ImageButton
    private lateinit var myDrawingView: MyDrawingView

    class MyDrawingView {

    }

    private lateinit var clearCanvas: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var bitmapToSave: Bitmap

    private lateinit var imageName: String

    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false
    val sdkLevel = Build.VERSION.SDK_INT

    /*the multiplePermissionLauncher gives an hashmap of permission name and whether its granted or not
    we take the values from hashmap from key- value pair and assign them to
    isReadPermissionGranted and isWritePermissionGranted
     */
    private val multiplePermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            if (sdkLevel <= Build.VERSION_CODES.TIRAMISU) {
                isReadPermissionGranted =
                    permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                        ?: isReadPermissionGranted
            } else {
                isReadPermissionGranted =
                    permissions[android.Manifest.permission.READ_MEDIA_IMAGES] ?: isReadPermissionGranted
            }

            isWritePermissionGranted =
                permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: isReadPermissionGranted
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<ConstraintLayout>(R.id.main)

        // Ensure the layout is drawn
        mainLayout.post {
            val bitmap: Bitmap = createBitmapFromView(mainLayout)
            saveBitmap(bitmap) // Save the bitmap if needed
        }
        createAppDirectoryInDownloads(this)

    }

    fun createAppDirectoryInDownloads(context: Context): File? {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDirectory = File(downloadsDirectory, "YourAppDirectoryName")

        if (!appDirectory.exists()) {
            val directoryCreated = appDirectory.mkdir()
            if (!directoryCreated) {
                // Failed to create the directory
                return null
            }
        }

        return appDirectory
    }




    private fun createBitmapFromView(view: View): Bitmap {
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(view.drawingCache)
        view.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap) {
        val fileName = "layout_bitmap.png"
        val directory = File(Environment.getExternalStorageDirectory().toString() + "/YourAppName/")
        if (!directory.exists()) {
            directory.mkdirs() // Create the directory if it doesn't exist
        }
        val file = File(directory, fileName)
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}


