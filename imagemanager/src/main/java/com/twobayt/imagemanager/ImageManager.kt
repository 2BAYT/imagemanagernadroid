package com.twobayt.imagemanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

class ImageManager private constructor(builder: Builder)  {
    private var applicationId:String

    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null

    private var mCurrentPhotoPath: String? = ""
    private var mDestinationUri: Uri? = null

    private var contextWeakReference: WeakReference<Context>? = null
    private var isCrop = true
    private var sampleSize = SampleSize.NORMAL

    private val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)


    init {
        this.isCrop = builder.isCrop
        this.contextWeakReference = WeakReference(builder.context)
        this.sampleSize = builder.sampleSize
        this.applicationId = builder.context!!.packageName
    }

    data class Builder(var context: Context? = null, var isCrop: Boolean = true, var sampleSize: SampleSize = SampleSize.NORMAL)
    {
        // builder code

        fun crop(isCrop: Boolean) = apply { this.isCrop = isCrop }
        fun sampleSize(sampleSize: SampleSize) = apply { this.sampleSize = sampleSize }
        fun build() = ImageManager(this)
    }

    fun writeToFile(bitmap: Bitmap?): File {
        val compress = 90
        val rnd = Random()
        val number = rnd.nextInt(3000)
        val file = File(contextWeakReference?.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path + "/saved_images" + number + ".jpg")
        try {
            file.createNewFile()
            val stream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, compress, stream)
            stream.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    fun registerCameraLauncher(activity: Activity, fragment: Fragment, openCropProvider: ICropProvider?, callback: (bitmap: Bitmap?) -> Unit){
        this.cameraLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultBitmap: Bitmap? = getBitmapFromCurrentPath()
                handleBitmap(openCropProvider, resultBitmap, callback)
            }
        }
    }

    fun registerGalleryLauncher(activity: Activity, fragment: Fragment, openCropProvider: ICropProvider?, callback: (bitmap: Bitmap?) -> Unit){
        this.galleryLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                mCurrentPhotoPath = getRealPath(activity, result.data?.data)
                val resultBitmap = getBitmapFromCurrentPath()
                handleBitmap(openCropProvider, resultBitmap, callback)
            }
        }
    }



    private fun handleBitmap(openCropProvider: ICropProvider?, bitmap: Bitmap?, callback: (bitmap: Bitmap?) -> Unit) {
        if(isCrop){
            val cropFragment = CropFragment.newInstance(bitmap)
            cropFragment.setOnCropDoneListener(object : CropFragment.CropDoneListener{
                override fun onCropped(bitmap: Bitmap?) {
                    callback(bitmap)
                }
            })
            openCropProvider?.openCrop(cropFragment)
        }else{
            callback(bitmap)
        }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir =
            contextWeakReference?.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun dispatchGalleryIntent(): Intent {
        return galleryIntent
    }

    private fun dispatchTakePictureIntent(): Intent {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        var photoFile: File? = null
        if (takePictureIntent.resolveActivity(contextWeakReference?.get()?.packageManager!!) != null) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the Fil...
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
                if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT) {
                    mDestinationUri = FileProvider.getUriForFile(
                        contextWeakReference?.get()!!,
                        "$applicationId.provider",
                        photoFile
                    ) //(use your app signature + ".provider" )imageFile);
                } else {
                    mDestinationUri = Uri.fromFile(photoFile)
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mDestinationUri)
            }
        }
        return takePictureIntent
    }

    fun onSaveInstanceState(outState: Bundle){
        outState.putParcelable("savedUri", mDestinationUri)
        outState.putString("mCurrentPhotoPath", mCurrentPhotoPath)
        outState.putBoolean("isCrop", isCrop)
        outState.putInt("sampleSize", sampleSize.value)

    }

    fun prepareInstance(savedInstanceState: Bundle?){
        savedInstanceState?.apply {
            mDestinationUri = savedInstanceState.getParcelable("savedUri")
            mCurrentPhotoPath = savedInstanceState.getString("mCurrentPhotoPath")
            isCrop = savedInstanceState.getBoolean("isCrop")
            sampleSize = SampleSize.NORMAL// TODO::
        }
    }

    private fun getBitmap(size:Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(mCurrentPhotoPath, options)
        options.inSampleSize = calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
        return  BitmapFactory.decodeFile(mCurrentPhotoPath, options)
    }

    private fun getBitmapFromCurrentPath(): Bitmap? {
        return try{
            getBitmap(sampleSize.value)
        }catch (e:OutOfMemoryError){
            getBitmap(sampleSize.value / 2)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun launchCamera() {
        val intent = dispatchTakePictureIntent()
        cameraLauncher?.launch(intent)
    }

    fun launchGallery() {
        val intent = dispatchGalleryIntent()
        galleryLauncher?.launch(intent)
    }

    private fun getRealPath(activity: Activity, selectedImageUri: Uri?): String? { // for gallery uri
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = activity.contentResolver.query(selectedImageUri!!, proj, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } finally {
            cursor?.close()
        }
    }
}