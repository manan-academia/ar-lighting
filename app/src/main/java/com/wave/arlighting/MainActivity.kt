package com.wave.arlighting

import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.function.Consumer
import java.util.function.Function

class MainActivity : AppCompatActivity() {
  private var fragment: ArFragment? = null
  private val pointer = PointerDrawable()
  private var isTracking = false
  private var isHitting = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    setSupportActionBar(toolbar)

    fab.setOnClickListener { view ->
      Snackbar.make(
        view,
        "Replace with your own action",
        Snackbar.LENGTH_LONG
      ).setAction(
        "Action",
        null
      ).show()
    }

    fragment =
      supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment?
    //        fragment!!.arSceneView.scene
    //            .addOnUpdateListener { frameTime: FrameTime? ->
    //                fragment!!.onUpdate(frameTime)
    //                onUpdate()
    //            }
    val utils = Utils(
      this,
      fragment
    )

    fragment!!.setOnTapArPlaneListener { hitResult, _, _ ->
      //            utils.makeSphere(hitResult, Color.BLUE)
      ModelRenderable.builder() // To load as an asset from the 'assets' folder ('src/main/assets/andy.sfb'):
        .setSource(
          this,
          Uri.parse("untitled.sfb")
        ) // Instead, load as a resource from the 'res/raw' folder ('src/main/res/raw/andy.sfb'):
        //.setSource(this, R.raw.andy)
        .build().thenAccept { renderable1: ModelRenderable ->
          val myRenderable = renderable1
          val anchor: Anchor = hitResult.createAnchor()
          val anchorNode = AnchorNode(anchor)

          myRenderable.material.setFloat3(
            "andyColor",
            1.0F,
            0.0F,
            0.0F
          )

          myRenderable.material.setFloat(
            "metallic",
            1.0F
          )

          myRenderable.material.setFloat(
            "reflectance",
            0.56F
          )

          Log.d(
            "Renderable",
            myRenderable.material.toString()
          )

          TransformableNode(fragment!!.transformationSystem).apply {
            renderable = myRenderable
            setParent(anchorNode)
            select()
          }

          fragment!!.arSceneView.scene.addChild(anchorNode)
        }.exceptionally { throwable: Throwable? ->
          Log.e(
            "",
            "Unable to load Renderable.",
            throwable
          )
          null
        }
    }
  }

  private fun onUpdate() {
    val trackingChanged: Boolean = updateTracking()
    val contentView: View = findViewById(android.R.id.content)
    if (trackingChanged) {
      if (isTracking) {
        contentView.overlay.add(pointer)
      } else {
        contentView.overlay.remove(pointer)
      }
      contentView.invalidate()
    }
    if (isTracking) {
      val hitTestChanged: Boolean = updateHitTest()
      if (hitTestChanged) {
        pointer.setEnabled(isHitting)
        contentView.invalidate()
      }
    }
  }

  private fun updateTracking(): Boolean {
    val frame: Frame? = fragment!!.arSceneView.arFrame
    val wasTracking = isTracking
    isTracking =
      frame != null && frame.camera.trackingState === TrackingState.TRACKING
    return isTracking != wasTracking
  }

  private fun updateHitTest(): Boolean {
    val frame = fragment!!.arSceneView.arFrame
    val pt = getScreenCenter()
    val hits: List<HitResult>
    val wasHitting = isHitting
    isHitting = false
    if (frame != null) {
      hits = frame.hitTest(
        pt.x.toFloat(),
        pt.y.toFloat()
      )
      for (hit in hits) {
        val trackable = hit.trackable
        if (trackable is Plane && (trackable).isPoseInPolygon(hit.hitPose)) {
          isHitting = true
          break
        }
      }
    }
    return wasHitting != isHitting
  }

  private fun getScreenCenter(): Point {
    val vw = findViewById<View>(android.R.id.content)
    return Point(
      vw.width / 2,
      vw.height / 2
    )
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(
      R.menu.menu_main,
      menu
    )
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    return when (item.itemId) {
      R.id.action_settings -> true
      else -> super.onOptionsItemSelected(item)
    }
  }
}
