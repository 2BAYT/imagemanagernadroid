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
    var bitmap: Bitmap? = null

    interface CropDoneListener {
        fun onCropped(bitmap: Bitmap?)
    }

    var listener: CropDoneListener? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.crop_fragment, container, false)
        prepareView(v)
        setListeners()
        init()
        return v
    }

    private fun init() {
        cropIV.setGuidelines(1)
        cropIV.setImageBitmap(bitmap)
    }

    private fun setListeners() {
        okTV.setOnClickListener { _: View? ->
            if (listener != null) {
                listener!!.onCropped(cropIV!!.croppedImage)
                activity?.let {
                    if(!it.isFinishing){
                        it.onBackPressed()
                    }
                }

            }
        }

        cancelTV.setOnClickListener {
            if(activity?.isFinishing != true){
                activity?.onBackPressed()
            }
        }


        rotateRightIV.setOnClickListener { cropIV!!.rotateImage(90) }
        rotateLeftIV.setOnClickListener { cropIV!!.rotateImage(-90) }
    }

    fun setOnCropDoneListener(listener: CropDoneListener?) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            bitmap = requireArguments().getParcelable("bitmap")
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
        fun newInstance(bitmap: Bitmap?): CropFragment {
            val addPostFragment = CropFragment()
            val bundle = Bundle()
            bundle.putParcelable("bitmap", bitmap)
            addPostFragment.arguments = bundle
            return addPostFragment
        }
    }
}