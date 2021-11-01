package com.twobayt.imagemanagerexample

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.twobayt.imagemanager.*
import android.app.Activity
import java.lang.UnsupportedOperationException


class HomeFragment : Fragment(), ICropProvider{

    private lateinit var camera: TextView
    private lateinit var gallery: TextView
    private lateinit var selectedImage: ImageView

    private var imageManager: ImageManager? = null
    var TAG:String = "HomeFragment"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        buildImageManager(savedInstanceState)
        prepareView(v)
        setListeners()
        checkWriteStoragePermission()
        return v
    }


    private fun buildImageManager(savedInstanceState: Bundle?) {
        imageManager = ImageManager.Builder(context)
                .debugLogEnabled()
                .fixExif()
                .targetWidth(1500)
                .targetHeight(1278)
                .crop(true)
                .sampleSize(SampleSize.EXTRABIG)
                .build()

        imageManager?.prepareInstance(savedInstanceState)
        imageManager?.register("home", requireActivity(),this, this) { bitmap, _ -> onImageSelected(bitmap) }

    }



    override fun openCrop(fragment: CropFragment) {

        activity?.supportFragmentManager
            ?.beginTransaction()
            ?.add(R.id.content, fragment)
            ?.addToBackStack("crop fragment")
            ?.commit()
    }

    private fun checkWriteStoragePermission(){
        val hasWriteContactsPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsResultCallback.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        imageManager?.onSaveInstanceState(outState)
    }

    private fun onImageSelected(bitmap: Bitmap?) {
        selectedImage.setImageBitmap(bitmap)
    }

    private fun setListeners() {
        camera.setOnClickListener {
            imageManager?.launchCamera()
        }
        gallery.setOnClickListener {
            imageManager?.launchGallery()
        }
    }

    private fun prepareView(v:View) {
        camera = v.findViewById(R.id.camera_button)
        gallery = v.findViewById(R.id.gallery_button)
        selectedImage = v.findViewById(R.id.selected_image)
    }

    private val permissionsResultCallback = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        when (it) {
            true -> { println("Permission has been granted by user") }
            false -> {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }


}