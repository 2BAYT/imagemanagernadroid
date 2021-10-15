package com.twobayt.imagemanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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

    private val tag: String =  "ImageManager"
    private var applicationId:String

    private var galleryLauncher: ActivityResultLauncher<Intent>? = null
    private var cameraLauncher: ActivityResultLauncher<Intent>? = null

    private var mCurrentPhotoPath: String? = ""
    private var mDestinationUri: Uri? = null

    private var contextWeakReference: WeakReference<Context>? = null
    private var isCrop = true
    private var sampleSize = SampleSize.NORMAL
    private var targetWidth = Int.MAX_VALUE
    private var targetHeight = Int.MAX_VALUE
    private var debugLogEnabled = false
    private var fixExif = false


    private val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

    init {
        this.fixExif = builder.fixExif
        this.debugLogEnabled = builder.debugLogEnabled
        this.isCrop = builder.isCrop
        this.targetWidth = builder.targetWidth
        this.targetHeight = builder.targetHeight
        this.sampleSize = builder.sampleSize

        this.contextWeakReference = WeakReference(builder.context)
        this.applicationId = builder.context!!.packageName
    }

    data class Builder(var context: Context? = null, var isCrop: Boolean = true, var sampleSize: SampleSize = SampleSize.NORMAL, var targetWidth: Int = Int.MAX_VALUE, var targetHeight: Int = Int.MAX_VALUE, var debugLogEnabled:Boolean = false, var fixExif:Boolean = false)
    {
        // builder code
        fun debugLogEnabled() = apply { this.debugLogEnabled = true }
        fun fixExif() = apply { this.fixExif = true }
        fun crop(isCrop: Boolean) = apply { this.isCrop = isCrop }
        fun sampleSize(sampleSize: SampleSize) = apply { this.sampleSize = sampleSize }
        fun targetWidth(targetWidth: Int) = apply { this.targetWidth = targetWidth }
        fun targetHeight(targetHeight: Int) = apply { this.targetHeight = targetHeight }
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
        this.cameraLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultBitmap: Bitmap? = getBitmapFromCurrentPath()
                handleBitmap(openCropProvider, resultBitmap, callback, Source.CAMERA)
            }
        }
    }

    fun registerGalleryLauncher(activity: Activity, fragment: Fragment, openCropProvider: ICropProvider?, callback: (bitmap: Bitmap?) -> Unit){
        this.galleryLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                mCurrentPhotoPath = getRealPath(activity, result.data?.data)
                val resultBitmap = getBitmapFromCurrentPath()
                handleBitmap(openCropProvider, resultBitmap, callback, Source.GALLERY)
            }
        }
    }

     fun calculateAccordingToTargets(width: Float, height:Float): Array<Float> {
        val widthRatio:Float = width / targetWidth.toFloat()
        val heightRatio:Float = height / targetHeight.toFloat()
        val newWidth:Float
        val newHeight:Float
        if(heightRatio > widthRatio){
            // resize according to height ratio
             newWidth = (width/heightRatio)
             newHeight = (height/heightRatio)
        }else{
            // resize according to width ratio
             newWidth = (width/widthRatio)
             newHeight = (height/widthRatio)
        }

        if(heightRatio<1 && widthRatio<1){ // must not resize
            return arrayOf(width, height) // return same values
        }

        return arrayOf(newWidth, newHeight)
    }

    private fun handleBitmap(openCropProvider: ICropProvider?, bitmap: Bitmap?, callback: (bitmap: Bitmap?) -> Unit, source: Source) {
        bitmap ?: return
        var resultBitmap:Bitmap? = bitmap
        if(debugLogEnabled){ Log.d(tag, "selected bitmap width = "+bitmap.width+" height = "+bitmap.height) }

        val dimens = calculateAccordingToTargets(bitmap.width.toFloat(), bitmap.height.toFloat()) // For height
        val newDimens = calculateAccordingToTargets(width = dimens[0], height = dimens[1]) // for width

        if(bitmap.width!=newDimens[0].toInt() || bitmap.height!=newDimens[1].toInt()){ // when dimens changed resize
            resultBitmap = getResizedBitmap(bitmap, newWidth = newDimens[0].toInt(), newHeight = newDimens[1].toInt())
        }

        if(source == Source.CAMERA && fixExif){
            resultBitmap = fixExifOrientation(bitmap, mCurrentPhotoPath)
        }

        if(debugLogEnabled){ Log.d(tag, "resized bitmap width = "+newDimens[0].toInt()+" height = "+newDimens[1].toInt()) }

        if(isCrop){
            val cropFragment = CropFragment.newInstance(resultBitmap)
            cropFragment.setOnCropDoneListener(object : CropFragment.CropDoneListener{
                override fun onCropped(bitmap: Bitmap?) {
                    callback(bitmap)
                }
            })
            openCropProvider?.openCrop(cropFragment)
            if(openCropProvider==null){
                Log.e(tag, "Crop Provider Not Found")
            }
        }else{
            callback(resultBitmap)
        }
    }

    @Throws(IOException::class)
    fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = contextWeakReference?.get()?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(imageFileName,  /* prefix */".jpg",  /* suffix */storageDir /* directory */)
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
                    mDestinationUri = FileProvider.getUriForFile(contextWeakReference?.get()!!, "$applicationId.provider", photoFile) //(use your app signature + ".provider" )imageFile);
                } else {
                    mDestinationUri = Uri.fromFile(photoFile)
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mDestinationUri)
            }
        }
        return takePictureIntent
    }

    fun onSaveInstanceState(outState: Bundle?){
        outState?:return
        outState.putParcelable(::mDestinationUri.name, mDestinationUri)
        outState.putString(::mCurrentPhotoPath.name, mCurrentPhotoPath)
        outState.putBoolean(::isCrop.name, isCrop)
        outState.putInt("sampleSize", sampleSize.value)
        outState.putInt(::targetWidth.name, targetWidth)
        outState.putInt(::targetHeight.name, targetHeight)
        printBundle(outState)
    }

    fun prepareInstance(savedInstanceState: Bundle?){
        savedInstanceState ?:return
        mDestinationUri = savedInstanceState.getParcelable(::mDestinationUri.name)
        mCurrentPhotoPath = savedInstanceState.getString(::mCurrentPhotoPath.name)
        isCrop = savedInstanceState.getBoolean(::isCrop.name)
        sampleSize = SampleSize.NORMAL// TODO::
        targetWidth = savedInstanceState.getInt(::targetWidth.name)
        targetHeight = savedInstanceState.getInt(::targetHeight.name)
        printBundle(savedInstanceState)
    }

    private fun printBundle(bundle: Bundle?) {
        if(!debugLogEnabled) return
        bundle ?: return

        val text = bundle.keySet().joinToString(", ", "{", "}") { key -> "$key=${bundle[key]}" }

        Log.d(tag, text)
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

    private fun fixExifOrientation(b: Bitmap?, image_path: String?): Bitmap? {
        var bitmap = b
        return try {
            val exif = ExifInterface(image_path!!)
            if(debugLogEnabled){
                Log.d(tag, "Exif value = $exif")
            }

            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            bitmap = rotateBitmap(bitmap!!, orientation)
            if(debugLogEnabled){
                Log.d(tag, "Exif value = $orientation")
            }
            bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        return try {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> return bitmap
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
                else -> return bitmap
            }
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            if(debugLogEnabled){
                Log.d(tag, "exif rotated ")
            }
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        } catch (e: Exception) {
            bitmap
        }
    }


    private fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        if(debugLogEnabled){ Log.d(tag, "resized") }
        return try {
            val width = bm.width
            val height = bm.height
            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height
            // CREATE A MATRIX FOR THE MANIPULATION
            val matrix = Matrix()
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight)
            // "RECREATE" THE NEW BITMAP
            // bm.recycle();
            Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false)
        } catch (e: Exception) {
            bm
        }
    }
}