package com.twobayt.imagemanager

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.edmodo.cropper.CropImageView


class CropFragment : Fragment() {

    var settings: ImageManager.Builder? = null
    var imagePath: String? = null
    var source: Int? = null
    var eventChannel: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.crop_fragment, container, false)
        prepareView(v)
        prepareImage()
        setListeners()
        return v
    }

    private fun prepareImage() {
        imagePath ?:return
        settings ?: return

        settings?.apply {
            var bitmap: Bitmap? = BitmapUtils.applySettings(this, imagePath!!)
            cropIV.setGuidelines(1)
            cropIV.setImageBitmap(bitmap)
        }
    }

    private fun closeFragment(){
        activity?.let {
            if(!it.isFinishing){
                it.onBackPressed()
            }
        }
    }


    private fun getSource(): Source {
        if(this.source!! == Source.CAMERA.ordinal){
            return Source.CAMERA
        }else{
            return Source.GALLERY
        }
    }

    private fun setListeners() {
        okTV.setOnClickListener {
            RxBus.publish(RxEvent.EventImageSelected(cropIV.croppedImage, getSource(), eventChannel))
            closeFragment()
        }

        cancelTV.setOnClickListener {
            closeFragment()
        }
        rotateRightIV.setOnClickListener { cropIV.rotateImage(90) }
        rotateLeftIV.setOnClickListener { cropIV.rotateImage(-90) }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            settings = requireArguments().getSerializable("settings") as ImageManager.Builder?
            imagePath = requireArguments().getString("imagePath")
            source = requireArguments().getInt("source")
            eventChannel = requireArguments().getString("eventChannel") as String?
        }
    }

    private lateinit var cropIV: CropImageView
    private lateinit var rotateRightIV: ImageView
    private lateinit var rotateLeftIV: ImageView
    private lateinit var okTV: TextView
    private lateinit var cancelTV: TextView
    private fun prepareView(v: View) {
        okTV = v.findViewById(R.id.okTV)
        cancelTV = v.findViewById(R.id.cancelTV)
        rotateRightIV = v.findViewById<View>(R.id.rotateRightIV) as ImageView
        rotateLeftIV = v.findViewById<View>(R.id.rotateLeftIV) as ImageView
        cropIV = v.findViewById<View>(R.id.cropIV) as CropImageView
    }

    companion object {
        @JvmStatic
        fun newInstance(eventChannel:String?, imagePath: String, settings: ImageManager.Builder, source: Int): CropFragment {
            val addPostFragment = CropFragment()
            val bundle = Bundle()
            bundle.putString("imagePath", imagePath)
            eventChannel?.apply { bundle.putString("eventChannel", eventChannel) }
            bundle.putInt("source", source)
            bundle.putSerializable("settings", settings)
            addPostFragment.arguments = bundle
            return addPostFragment
        }
    }

}