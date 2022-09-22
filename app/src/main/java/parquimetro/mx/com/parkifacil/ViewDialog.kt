package parquimetro.mx.com.parkifacil

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget

class ViewDialog(activity:Activity) {
    internal var activity: Activity
    internal lateinit var dialog: Dialog
    lateinit var  animationDrawable: AnimationDrawable
    lateinit var  mProgressBar: ImageView

    init{
        this.activity = activity
    }

    fun showDialog() {
        dialog = Dialog(activity,android.R.style.Theme_Translucent_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_loading)
        val gifImageView = dialog.findViewById<ImageView>(R.id.custom_loading_imageView)
        val imageViewTarget = GlideDrawableImageViewTarget(gifImageView)
        Glide.with(activity)
                .load(R.raw.loading_gif)
                .placeholder(R.raw.loading_gif)
                .centerCrop()
                .crossFade()
                .into(imageViewTarget)
        dialog.show()
    }

    fun hideDialog() {
        dialog.dismiss()
    }
}