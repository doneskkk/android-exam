package com.donesk.myexamapplication

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var previewImage: ImageView
    private lateinit var opacityValue: TextView
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var shareButton: Button

    private lateinit var imageButtons: List<ImageButton>
    private var selectedImageResId: Int = R.drawable.art_sunrise
    private var currentOpacityPercent: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewImage = findViewById(R.id.previewImage)
        opacityValue = findViewById(R.id.opacityValue)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)
        shareButton = findViewById(R.id.shareButton)

        imageButtons = listOf(
            findViewById(R.id.imageOptionOne),
            findViewById(R.id.imageOptionTwo),
            findViewById(R.id.imageOptionThree)
        )

        bindImageChooser()
        bindOpacityControls()
        bindShareAction()

        selectImage(imageButtons.first(), R.drawable.art_sunrise)
        opacitySeekBar.progress = currentOpacityPercent
    }

    private fun bindImageChooser() {
        val imageOptions = listOf(
            imageButtons[0] to R.drawable.art_sunrise,
            imageButtons[1] to R.drawable.art_city,
            imageButtons[2] to R.drawable.art_balloon
        )

        imageOptions.forEach { (button, drawableResId) ->
            button.setOnClickListener {
                selectImage(button, drawableResId)
            }
        }
    }

    private fun bindOpacityControls() {
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentOpacityPercent = progress
                applyOpacity(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun bindShareAction() {
        shareButton.setOnClickListener {
            val imageUri = createShareImageUri()
            if (imageUri == null) {
                Toast.makeText(this, R.string.share_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text, currentOpacityPercent))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)))
        }
    }

    private fun selectImage(selectedButton: ImageButton, drawableResId: Int) {
        selectedImageResId = drawableResId
        previewImage.setImageResource(drawableResId)

        imageButtons.forEach { button ->
            button.isSelected = button == selectedButton
        }

        applyOpacity(currentOpacityPercent)
    }

    private fun applyOpacity(progress: Int) {
        val alphaValue = ((progress / 100f) * 255).toInt()
        previewImage.imageAlpha = alphaValue
        opacityValue.text = getString(R.string.opacity_value, progress)
    }

    private fun createShareImageUri(): Uri? {
        return runCatching {
            val drawable = AppCompatResources.getDrawable(this, selectedImageResId)?.let { baseDrawable ->
                baseDrawable.constantState?.newDrawable()?.mutate() ?: baseDrawable.mutate()
            } ?: return null

            val imageSize = 1200
            drawable.alpha = ((currentOpacityPercent / 100f) * 255).toInt()
            drawable.setBounds(0, 0, imageSize, imageSize)

            val bitmap = Bitmap.createBitmap(imageSize, imageSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.draw(canvas)

            val imagesDirectory = File(cacheDir, "shared_images").apply { mkdirs() }
            val imageFile = File(imagesDirectory, "shared_image.png")

            FileOutputStream(imageFile).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            FileProvider.getUriForFile(this, "$packageName.fileprovider", imageFile)
        }.getOrNull()
    }
}
