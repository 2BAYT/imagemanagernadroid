package com.twobayt.imagemanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*

object BitmapUtils {

    public fun writeToFile(context: Context, bitmap: Bitmap?): File {
        val compress = 90
        val rnd = Random()
        val number = rnd.nextInt(3000)
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path + "/saved_images" + number + ".jpg")
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

    public fun applySettings(settings:ImageManager.Builder, imagePath: String?): Bitmap? {
        if(imagePath==null){
            return null
        }

        var resultBitmap:Bitmap? = getBitmapFromPath(imagePath!!, settings.sampleSize)
        //exif
        if(settings.fixExif && resultBitmap!=null){
            resultBitmap = fixExifOrientation(resultBitmap, imagePath)
        }
        //target size
        if(resultBitmap!=null){
            resultBitmap = getTargetResizedBitmap(resultBitmap, settings.targetWidth.toFloat(), settings.targetHeight.toFloat())
        }
        return resultBitmap
    }

    public fun fixExifOrientation(b: Bitmap?, image_path: String?): Bitmap? {
        var bitmap = b
        return try {
            val exif = ExifInterface(image_path!!)
            val orientation: Int = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            bitmap = rotateBitmap(bitmap!!, orientation)
            bitmap
        } catch (e: Exception) {
            bitmap
        }
    }

    public fun getTargetResizedBitmap(bitmap: Bitmap, targetWidth: Float, targetHeight: Float): Bitmap? {
        var resultBitmap:Bitmap? = bitmap
        val dimens = calculateAccordingToTargets(bitmap.width.toFloat(), bitmap.height.toFloat(), targetWidth, targetHeight ) // For height
        val newDimens = calculateAccordingToTargets(width = dimens[0], height = dimens[1], targetWidth, targetHeight) // for width

        if(bitmap.width!=newDimens[0].toInt() || bitmap.height!=newDimens[1].toInt()){ // when dimens changed resize
            resultBitmap = getResizedBitmap(bitmap, newWidth = newDimens[0].toInt(), newHeight = newDimens[1].toInt())
        }
        return resultBitmap
    }

    private fun getResizedBitmap(bm: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
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

    public fun calculateAccordingToTargets(width: Float, height:Float, targetWidth:Float, targetHeight:Float): Array<Float> {
        val widthRatio:Float = width / targetWidth
        val heightRatio:Float = height / targetHeight
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


    public fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
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
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            bitmap
        } catch (e: Exception) {
            bitmap
        }
    }


    public fun getBitmapFromPath(imagePath: String, sampleSize: SampleSize): Bitmap? {
        return try{
            getBitmap(imagePath, sampleSize.value)
        }catch (e:OutOfMemoryError){
            getBitmap(imagePath, sampleSize.value / 2)
        }
    }


    private fun getBitmap(path: String, size:Int): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, size, size)
        options.inJustDecodeBounds = false
        return  BitmapFactory.decodeFile(path, options)
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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

}