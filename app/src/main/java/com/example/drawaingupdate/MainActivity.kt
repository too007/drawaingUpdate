package com.example.drawaingupdate

import ImageAdapter
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter

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
        val getallimage = findViewById<LinearLayout>(R.id.getallimage)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)


        // Ensure the layout is drawn
        mainLayout.post {
            val bitmap: Bitmap = createBitmapFromView(mainLayout)
            saveBitmap(bitmap) // Save the bitmap if needed
            saveImageInAndroidApi29AndAbove(bitmap,this)
        }
        createAppDirectoryInDownloads(this)

        getallimage.setOnClickListener {

        }

        // Fetch the image URIs from the folder
        val imageUris = getAllImagesFromFolder(this)
        Log.e("TAG", "onCreate1234: "+imageUris.size )
        // Initialize the adapter with the image URIs
        imageAdapter = ImageAdapter(imageUris, this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = imageAdapter
    }
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

    @Throws(IOException::class)
    fun saveImageInAndroidApi29AndAbove(bitmap: Bitmap, mainActivity: MainActivity): Uri {
        val values = ContentValues()
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_" + System.currentTimeMillis())
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        if (SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM+"/sagar")
        }
        val resolver = mainActivity.contentResolver
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = resolver.insert(contentUri, values)
            if (uri == null) {
                //isSuccess = false;
                throw IOException("Failed to create new MediaStore record.")
            }
            resolver.openOutputStream(uri).use { stream ->
                if (stream == null) {
                    //isSuccess = false;
                    throw IOException("Failed to open output stream.")
                }
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                    //isSuccess = false;
                    throw IOException("Failed to save bitmap.")
                }
            }
            //isSuccess = true;
            return uri
        } catch (e: IOException) {
            if (uri != null) {
                resolver.delete(uri, null, null)
            }
            throw e
        }
    }

    fun getAllImagesFromFolder(mainActivity: MainActivity): List<Uri> {
        val images = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("${Environment.DIRECTORY_DCIM}/sagar%")

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        val queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val resolver = mainActivity.contentResolver

        resolver.query(queryUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = Uri.withAppendedPath(queryUri, id.toString())
                images.add(contentUri)
            }
        }

        return images
    }





