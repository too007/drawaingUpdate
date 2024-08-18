package com.example.drawaingupdate

import ImageAdapter
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), Deletebyid {

    private lateinit var imageUris: List<Uri>
    private lateinit var saveRelative: RelativeLayout
    private lateinit var colorPallet: LinearLayout
    private lateinit var eraserButton: ImageButton

    private lateinit var currentColorButton: ImageButton
    private lateinit var brushSizeSelectButton: ImageButton
    private lateinit var img: ImageView
    private lateinit var undoButton: ImageButton
    private lateinit var myDrawingView: MyDrawingView

    private val REQUEST_CODE_PERMISSION = 100
    private val REQUEST_CODE_PICK_IMAGE = 101
    private val REQUEST_CODE_UCROP = 102

    class MyDrawingView

    private lateinit var imageName: String
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: ImageAdapter

    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false
    val sdkLevel = Build.VERSION.SDK_INT

    // Initialize the ActivityResultLauncher
    private val deleteLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            val updatedList = getAllImagesFromFolder(this)
            imageAdapter.updateData(updatedList)
            // Notify the adapter about the data set change
            imageAdapter.notifyDataSetChanged()

        }

    private val multiplePermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (sdkLevel <= Build.VERSION_CODES.TIRAMISU) {
                isReadPermissionGranted = permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: isReadPermissionGranted
            } else {
                isReadPermissionGranted = permissions[android.Manifest.permission.READ_MEDIA_IMAGES]
                    ?: isReadPermissionGranted
            }

            isWritePermissionGranted = permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE]
                ?: isWritePermissionGranted
        }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<ConstraintLayout>(R.id.main)
        val getallimage = findViewById<LinearLayout>(R.id.getallimage)
        recyclerView = findViewById(R.id.recyclerView)

        createAppDirectoryInDownloads(this)

        getallimage.setOnClickListener {
//            val intent = Intent(Intent.ACTION_PICK)
//            intent.type = "image/*"
//            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)

            mainLayout.post {
                val bitmap: Bitmap = createBitmapFromView(mainLayout)
                saveBitmap(bitmap) // Save the bitmap if needed
                saveImageInAndroidApi29AndAbove(bitmap, this)
            }

        }

        // Fetch the image URIs from the folder
         imageUris = getAllImagesFromFolder(this)
        Log.e("TAG", "onCreate1234: ${imageUris.size}")
        // Initialize the adapter with the image URIs
        imageAdapter = ImageAdapter(imageUris, this, this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = imageAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            if (selectedImageUri != null) {
                startCrop(selectedImageUri)
            }
        } else if (requestCode == REQUEST_CODE_UCROP && resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                val intent = Intent(this, CropActivity::class.java)
                intent.putExtra("croppedImageUri", resultUri.toString())
                startActivity(intent)
            }
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "croppedImage.jpg"))
        val options = UCrop.Options()

        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .withOptions(options)
            .start(this, REQUEST_CODE_UCROP)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("NotifyDataSetChanged")
    override fun deletebyid(uri: Uri) {
        Log.e("TAG", "deletebyid: $uri")
        val file = uriToFile(this, uri)
        if (file != null) {
            Log.e("TAG", "File Path: ${file.absolutePath}")
            // Perform the delete operation here if needed
//            deleteFileByPath(file.absolutePath)
            delete(uri,uri)

        } else {
            Log.e("TAG", "File conversion failed")
        }
        imageAdapter.notifyDataSetChanged()
    }



    fun uriToFile(context: Context, uri: Uri): File? {
        val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(uri, filePathColumn, null, null, null)
        cursor?.moveToFirst()
        val columnIndex: Int = cursor?.getColumnIndex(filePathColumn[0]) ?: return null
        val filePath: String = cursor.getString(columnIndex)
        cursor.close()
        return File(filePath)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("Range")
    fun delete(
        urifront: Uri,
        uriback: Uri
    ) {
        val contentResolver: ContentResolver = contentResolver
        val collection = mutableListOf<Uri>()
        collection.add(urifront)
        collection.add(uriback)

        val pendingIntent = MediaStore.createDeleteRequest(contentResolver, collection)
        pendingIntent?.let {
            val sender = it.intentSender
            val request = IntentSenderRequest.Builder(sender).build()
            deleteLauncher.launch(request)
        } ?: run {
            Toast.makeText(this, "Failed to create delete request", Toast.LENGTH_SHORT).show()
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/sagar")
        }
        val resolver = mainActivity.contentResolver
        var uri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            uri = resolver.insert(contentUri, values)
            if (uri == null) {
                throw IOException("Failed to create new MediaStore record.")
            }
            resolver.openOutputStream(uri).use { stream ->
                if (stream == null) {
                    throw IOException("Failed to open output stream.")
                }
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                    throw IOException("Failed to save bitmap.")
                }
            }
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
}
