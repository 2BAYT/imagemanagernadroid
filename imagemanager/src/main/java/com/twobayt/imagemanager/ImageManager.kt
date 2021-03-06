package com.twobayt.imagemanager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class ImageManager private constructor(builder: Builder){

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

    private var source:Source? = null


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

    data class Builder(var context: Context? = null, var isCrop: Boolean = true, var sampleSize: SampleSize = SampleSize.NORMAL, var targetWidth: Int = Int.MAX_VALUE, var targetHeight: Int = Int.MAX_VALUE, var debugLogEnabled:Boolean = false, var fixExif:Boolean = false) : Serializable{
        // builder code
        fun debugLogEnabled() = apply { this.debugLogEnabled = true }
        fun fixExif() = apply { this.fixExif = true }
        fun crop(isCrop: Boolean) = apply { this.isCrop = isCrop }
        fun sampleSize(sampleSize: SampleSize) = apply { this.sampleSize = sampleSize }
        fun targetWidth(targetWidth: Int) = apply { this.targetWidth = targetWidth }
        fun targetHeight(targetHeight: Int) = apply { this.targetHeight = targetHeight }
        fun build() = ImageManager(this)
    }

    private fun registerCameraLauncher(tag: String?, activity: Activity, fragment: Fragment, openCropProvider: ICropProvider?){
        this.cameraLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                //val resultBitmap: Bitmap? = getBitmapFromCurrentPath()
                handleBitmap(tag, openCropProvider, mCurrentPhotoPath, Source.CAMERA)
            }
        }
    }


    private fun registerGalleryLauncher(tag: String?, activity: Activity, fragment: Fragment, openCropProvider: ICropProvider?){
        this.galleryLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                fixExif = false // force for gallery
                mCurrentPhotoPath = getRealPath(activity, result.data?.data)
                //val resultBitmap = getBitmapFromCurrentPath()
                handleBitmap(tag, openCropProvider, mCurrentPhotoPath, Source.GALLERY)
            }
        }
    }

    fun register(eventChannel:String?, activity: Activity, fragment: Fragment, openCropProvider: ICropProvider?,  bitmapCallback: (bitmap: Bitmap?, source:Source?) -> Unit) {
        registerCameraLauncher(eventChannel, activity, fragment, openCropProvider)
        registerGalleryLauncher(eventChannel, activity, fragment, openCropProvider)

        if(debugLogEnabled){
            Log.d("ImageManager", "registered")
        }

        RxBus.listen(RxEvent.EventImageSelected::class.java).subscribe {
            if(fragment==null || fragment.isDetached || !fragment.isAdded){
                return@subscribe
            }
            if(eventChannel==null || eventChannel == it.eventChannel){
                bitmapCallback(it.bitmap, it.source)
            }
        }
    }


    private fun handleBitmap(eventChannel: String?, openCropProvider: ICropProvider?, path: String?, source: Source) {
        path?:return

        this.source = source
        if(isCrop){ // crop handles exis and target resize
            var cropFragment = CropFragment.newInstance(eventChannel, path, getSettings(), source.ordinal)
            openCropProvider?.openCrop(cropFragment)
            if(openCropProvider==null){
                Log.e(tag, "Crop Provider Not Found")
            }
        }else{
            var bitmap = BitmapUtils.applySettings(getSettings(), path)
            bitmap?:return
            RxBus.publish(RxEvent.EventImageSelected(bitmap, source, eventChannel))
            //callback(bitmap)
        }
    }

    private fun getSettings(): Builder{
        return Builder(
            isCrop = isCrop,
            targetHeight = targetHeight,
            targetWidth = targetWidth,
            sampleSize = sampleSize,
            fixExif = fixExif,
        )
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
        source?.apply {
            outState.putInt("source", this.ordinal)
        }
        //outState.putBoolean("cropRegistered", cropRegistered!!)
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
        if(savedInstanceState.getInt("source")==0){
            source = Source.CAMERA
        }else{
            source = Source.GALLERY
        }

        //cropRegistered = savedInstanceState.getBoolean("cropRegistered")
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
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
        return  BitmapFactory.decodeFile(mCurrentPhotoPath, options)
    }

    private fun getBitmap(size:Int, path: String?): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = BitmapUtils.calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
        return  BitmapFactory.decodeFile(path, options)
    }

    private fun getBitmapFromCurrentPath(): Bitmap? {
        return try{
            getBitmap(sampleSize.value)
        }catch (e:OutOfMemoryError){
            getBitmap(sampleSize.value / 2)
        }
    }

    private fun getBitmapFromCurrentPath(path: String?): Bitmap? {
        return try{
            getBitmap(sampleSize.value, path)
        }catch (e:OutOfMemoryError){
            getBitmap(sampleSize.value / 2, path)
        }
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