package com.wave.arlighting

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.DepthSensorUsage.DO_NOT_USE
import com.google.ar.core.CameraConfig.DepthSensorUsage.REQUIRE_AND_USE
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

val screenshotHandler =
  Handler(HandlerThread("screenshot").also { it.start() }.looper)

fun Vector3.format(context: Context) = context.getString(
  R.string.format_vector3,
  x,
  y,
  z
)

fun Quaternion.format(context: Context) = context.getString(
  R.string.format_quaternion,
  x,
  y,
  z,
  w
)

fun Session.format(context: Context) = context.getString(
  R.string.format_session,
  allAnchors.count(),
  getAllTrackables(Plane::class.java).count(),
  getAllTrackables(Point::class.java).count()
)

fun CameraConfig.format(context: Context) = context.getString(
  R.string.format_camera_config,
  textureSize,
  fpsRange,
  when (depthSensorUsage) {
    REQUIRE_AND_USE -> true
    DO_NOT_USE, null -> false
  }
)

class SimpleSeekBarChangeListener(val block: (Int) -> Unit) : SeekBar.OnSeekBarChangeListener {
  override fun onStartTrackingTouch(seekBar: SeekBar?) {}
  override fun onStopTrackingTouch(seekBar: SeekBar?) {}
  override fun onProgressChanged(
    seekBar: SeekBar?,
    progress: Int,
    fromUser: Boolean
  ) {
    block(progress)
  }
}

fun AppCompatActivity.redirectToApplicationSettings() {
  val intent = Intent().apply {
    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    data = Uri.fromParts(
      "package",
      packageName,
      null
    )
  }
  startActivity(intent)
}

fun filename(): String = SimpleDateFormat(
  "yyyy-MM-dd HH:mm:ss",
  Locale.US
).format(Date())

fun cacheFile(
  context: Context,
  extension: String
): File = File(
  context.cacheDir,
  filename() + extension
)

fun ArSceneView.screenshot() {
  Toast.makeText(
    context,
    R.string.screenshot_saving,
    Toast.LENGTH_LONG
  ).show()
  val bitmap = Bitmap.createBitmap(
    width,
    height,
    Bitmap.Config.ARGB_8888
  )
  PixelCopy.request(
    this,
    bitmap,
    { result ->
      when (result) {
        PixelCopy.SUCCESS -> {
          val file = cacheFile(
            context,
            ".png"
          )
          bitmap.compress(
            Bitmap.CompressFormat.PNG,
            100,
            file.outputStream()
          )
          val uri = FileProvider.getUriForFile(
            context,
            context.packageName,
            file
          )
          context.startActivity(
            viewOrShare(
              uri,
              "image/png"
            )
          )
        }
        else -> Toast.makeText(
          context,
          "Screenshot failure: $result",
          Toast.LENGTH_LONG
        ).show()
      }
    },
    screenshotHandler
  )
}

fun viewOrShare(
  data: Uri,
  mime: String
): Intent {
  val view = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(
      data,
      mime
    )
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  val share = Intent(Intent.ACTION_SEND).apply {
    type = mime
    putExtra(
      Intent.EXTRA_STREAM,
      data
    )
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  return Intent.createChooser(
    Intent(),
    null
  ).apply {
    putExtra(
      Intent.EXTRA_INITIAL_INTENTS,
      arrayOf(
        view,
        share
      )
    )
  }
}

fun @receiver:ColorInt Int.toArColor(): Color = Color(this)

fun UnavailableException?.message(): Int {
  return when (this) {
    is UnavailableArcoreNotInstalledException -> R.string.exception_arcore_not_installed
    is UnavailableApkTooOldException -> R.string.exception_apk_too_old
    is UnavailableSdkTooOldException -> R.string.exception_sdk_too_old
    is UnavailableDeviceNotCompatibleException -> R.string.exception_device_not_compatible
    is UnavailableUserDeclinedInstallationException -> R.string.exception_user_declined_installation
    else -> R.string.exception_unknown
  }
}

fun View.behavior(): BottomSheetBehavior<out View> =
  BottomSheetBehavior.from(this)

fun BottomSheetBehavior<out View>.toggle() {
  state = when (state) {
    BottomSheetBehavior.STATE_COLLAPSED -> BottomSheetBehavior.STATE_EXPANDED
    BottomSheetBehavior.STATE_EXPANDED -> BottomSheetBehavior.STATE_COLLAPSED
    else -> return
  }
}

fun BottomSheetBehavior<out View>.update(
  @BottomSheetBehavior.State state: Int,
  isHideable: Boolean?
) {
  this.state = state
  if (isHideable != null) {
    this.isHideable = isHideable
  }
}


