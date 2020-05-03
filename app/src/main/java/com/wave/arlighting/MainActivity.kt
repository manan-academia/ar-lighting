package com.wave.arlighting

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.ar.core.*
import com.google.ar.core.TrackingFailureReason.*
import com.google.ar.core.TrackingState.*
import com.google.ar.sceneform.ArSceneView
import kotlinx.android.synthetic.main.bottom_sheet_node.*
import kotlinx.android.synthetic.main.bottom_sheet_node_body.*
import kotlinx.android.synthetic.main.bottom_sheet_node_header.*
import kotlinx.android.synthetic.main.bottom_sheet_scene.*
import kotlinx.android.synthetic.main.bottom_sheet_scene_body.*
import kotlinx.android.synthetic.main.bottom_sheet_scene_header.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : ARActivity(R.layout.activity_main) {
  private val coordinator by lazy {
    Coordinator(
      this,
      ::onArTap,
      ::onNodeSelected,
      ::onNodeFocused
    )
  }
  private val model: SceneViewModel by viewModels()
  private val settings by lazy { Settings.instance(this) }
  private val setOfMaterialViews by lazy {
    setOf(
      nodeColorValue,
      nodeColorLabel,
      nodeMetallicValue,
      nodeMetallicLabel,
      nodeRoughnessValue,
      nodeRoughnessLabel,
      nodeReflectanceValue,
      nodeReflectanceLabel
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initSceneBottomSheet()
    initNodeBottomSheet()
    initAr()
    initWithIntent(intent)
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    initWithIntent(intent)
  }

  override fun onBackPressed() {
    if (coordinator.selectedNode != null) {
      coordinator.selectNode(null)
    } else {
      super.onBackPressed()
    }
  }

  override fun arSceneView(): ArSceneView = arSceneView

  override fun recordingIndicator(): ImageView? = sceneRecording

  override fun config(session: Session): Config = Config(session).apply {
    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
    updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
    cloudAnchorMode = Config.CloudAnchorMode.ENABLED
    augmentedFaceMode = Config.AugmentedFaceMode.DISABLED
    focusMode = Config.FocusMode.AUTO
  }

  override fun onArResumed() {
    sceneBottomSheet.behavior().update(
      state = STATE_EXPANDED,
      isHideable = false
    )
  }

  private fun initWithIntent(intent: Intent?) {
    if (intent?.action != Intent.ACTION_VIEW) return
    intent.data?.let {
      Toast.makeText(
        this,
        it.toString(),
        Toast.LENGTH_SHORT
      ).show()
      this.intent = null
    }
  }

  private fun initSceneBottomSheet() {
    sceneBottomSheet.behavior().state = STATE_HIDDEN
    sceneHeader.setOnClickListener { sceneBottomSheet.behavior().toggle() }

    sceneAdd.setOnClickListener {
      val session = arSceneView.session
      val camera = arSceneView.arFrame?.camera ?: return@setOnClickListener
      if (session == null || camera.trackingState != TRACKING) return@setOnClickListener
      createNodeAndAddToScene(
        anchor = { session.createAnchor(Nodes.defaultPose(arSceneView)) },
        focus = false
      )
    }

    initPopupMenu(anchor = sceneMore,
      menu = R.menu.menu_scene,
      onClick = {
        when (it.itemId) {
          R.id.menu_item_clean_up_scene -> arSceneView.scene.callOnHierarchy { node -> (node as? Nodes)?.detach() }
          R.id.menu_item_sunlight -> settings.sunlight.toggle(
            it,
            arSceneView
          )
          R.id.menu_item_shadows -> settings.shadows.toggle(
            it,
            arSceneView
          )
        }
        when (it.itemId) {
          R.id.menu_item_sunlight, R.id.menu_item_shadows -> false
          else -> true
        }
      },
      onUpdate = {
        findItem(R.id.menu_item_clean_up_scene).isEnabled =
          arSceneView.scene.findInHierarchy { it is Nodes } != null
        settings.sunlight.applyTo(findItem(R.id.menu_item_sunlight))
        settings.shadows.applyTo(findItem(R.id.menu_item_shadows))
      })

    model.selection.observe(this,
      androidx.lifecycle.Observer {
        modelSphere.isSelected = it == Sphere::class
        modelCylinder.isSelected = it == Cylinder::class
        modelCube.isSelected = it == Cube::class
        modelAndy.isSelected = it == Andy::class
      })

    modelSphere.setOnClickListener { model.selection.value = Sphere::class }
    modelCylinder.setOnClickListener { model.selection.value = Cylinder::class }
    modelCube.setOnClickListener { model.selection.value = Cube::class }
    modelAndy.setOnClickListener { model.selection.value = Andy::class }
  }

  private fun initNodeBottomSheet() {
    nodeBottomSheet.behavior().apply {
      skipCollapsed = true
      addBottomSheetCallback(object : BottomSheetCallback() {
        override fun onSlide(
          bottomSheet: View,
          slideOffset: Float
        ) {
        }

        override fun onStateChanged(
          bottomSheet: View,
          newState: Int
        ) {
          bottomSheet.requestLayout()
          if (newState == STATE_HIDDEN) {
            coordinator.selectNode(null)
          }
        }
      })
      state = STATE_HIDDEN
    }
    nodeHeader.setOnClickListener { coordinator.selectNode(null) }
    nodeDelete.setOnClickListener { coordinator.focusedNode?.detach() }

    nodeColorValue.setOnColorChangeListener {
      focusedMaterialNode()?.update {
        color = it
      }
    }
    nodeMetallicValue.progress = MaterialProperties.DEFAULT.metallic
    nodeMetallicValue.setOnSeekBarChangeListener(SimpleSeekBarChangeListener {
      focusedMaterialNode()?.update {
        metallic = it
      }
    })
    nodeRoughnessValue.progress = MaterialProperties.DEFAULT.roughness
    nodeRoughnessValue.setOnSeekBarChangeListener(SimpleSeekBarChangeListener {
      focusedMaterialNode()?.update {
        roughness = it
      }
    })
    nodeReflectanceValue.progress = MaterialProperties.DEFAULT.reflectance
    nodeReflectanceValue.setOnSeekBarChangeListener(SimpleSeekBarChangeListener {
      focusedMaterialNode()?.update {
        reflectance = it
      }
    })
  }

  private fun focusedMaterialNode() = (coordinator.focusedNode as? MaterialNode)

  private fun materialProperties() = MaterialProperties(
    if (focusedMaterialNode() != null) nodeColorValue.getColor() else Color.WHITE,
    nodeMetallicValue.progress,
    nodeRoughnessValue.progress,
    nodeReflectanceValue.progress
  )

  private fun initAr() {
    arSceneView.scene.addOnUpdateListener { onArUpdate() }
    arSceneView.scene.addOnPeekTouchListener { hitTestResult, motionEvent ->
      coordinator.onTouch(
        hitTestResult,
        motionEvent
      )
    }

    settings.sunlight.applyTo(arSceneView)
    settings.shadows.applyTo(arSceneView)
  }

  private fun onArTap(motionEvent: MotionEvent) {
    val frame = arSceneView.arFrame ?: return
    if (frame.camera.trackingState != TRACKING) {
      coordinator.selectNode(null)
      return
    }

    frame.hitTest(motionEvent).firstOrNull {
      val trackable = it.trackable
      when {
        trackable is Plane && trackable.isPoseInPolygon(it.hitPose) -> true
        trackable is Point -> true
        else -> false
      }
    }?.let { createNodeAndAddToScene(anchor = { it.createAnchor() }) }
      ?: coordinator.selectNode(null)
  }

  private fun createNodeAndAddToScene(
    anchor: () -> Anchor,
    focus: Boolean = true
  ) {
    when (model.selection.value) {
      Sphere::class -> Sphere(
        this,
        materialProperties(),
        coordinator,
        settings
      )
      Cylinder::class -> Cylinder(
        this,
        materialProperties(),
        coordinator,
        settings
      )
      Cube::class -> Cube(
        this,
        materialProperties(),
        coordinator,
        settings
      )
      Andy::class -> Andy(
        this,
        coordinator,
        settings
      )
      else -> return
    }.attach(
      anchor(),
      arSceneView.scene,
      focus
    )
  }

  private fun onArUpdate() {
    val frame = arSceneView.arFrame
    val camera = frame?.camera
    val state = camera?.trackingState
    val reason = camera?.trackingFailureReason

    onArUpdateStatusText(
      state,
      reason
    )
    onArUpdateStatusIcon(
      state,
      reason
    )
    onArUpdateBottomSheet(state)
  }

  private fun onArUpdateStatusText(
    state: TrackingState?,
    reason: TrackingFailureReason?
  ) {
    sceneStatusLabel.setText(
      when (state) {
        TRACKING -> R.string.tracking_success
        PAUSED -> when (reason) {
          NONE -> R.string.tracking_failure_none
          BAD_STATE -> R.string.tracking_failure_bad_state
          INSUFFICIENT_LIGHT -> R.string.tracking_failure_insufficient_light
          EXCESSIVE_MOTION -> R.string.tracking_failure_excessive_motion
          INSUFFICIENT_FEATURES -> R.string.tracking_failure_insufficient_features
          CAMERA_UNAVAILABLE -> R.string.tracking_failure_camera_unavailable
          null -> 0
        }
        STOPPED -> R.string.tracking_stopped
        null -> 0
      }
    )
  }

  private fun onArUpdateStatusIcon(
    state: TrackingState?,
    reason: TrackingFailureReason?
  ) {
    sceneStatusIcon.setImageResource(
      when (state) {
        TRACKING -> android.R.drawable.presence_online
        PAUSED -> when (reason) {
          NONE -> android.R.drawable.presence_invisible
          BAD_STATE, CAMERA_UNAVAILABLE -> android.R.drawable.presence_busy
          INSUFFICIENT_LIGHT, EXCESSIVE_MOTION, INSUFFICIENT_FEATURES -> android.R.drawable.presence_away
          null -> 0
        }
        STOPPED -> android.R.drawable.presence_offline
        null -> 0
      }
    )
  }

  private fun onArUpdateBottomSheet(state: TrackingState?) {
    sceneAdd.isEnabled = state == TRACKING
    when (sceneBottomSheet.behavior().state) {
      STATE_HIDDEN, STATE_COLLAPSED -> Unit
      else -> {
        arSceneView.arFrame?.camera?.pose.let {}
      }
    }
  }

  private fun onNodeUpdate(node: Nodes) {
    when {
      node != coordinator.selectedNode || node != coordinator.focusedNode || nodeBottomSheet.behavior().state == STATE_HIDDEN -> Unit
      else -> {
        nodeStatus.setImageResource(node.statusIcon())
        nodeDelete.isEnabled = !node.isTransforming
      }
    }
  }

  private fun onNodeSelected(
    old: Nodes? = coordinator.selectedNode,
    new: Nodes?
  ) {
    old?.onNodeUpdate = null
    new?.onNodeUpdate = ::onNodeUpdate
  }

  private fun onNodeFocused(node: Nodes?) {
    val nodeSheetBehavior = nodeBottomSheet.behavior()
    val sceneBehavior = sceneBottomSheet.behavior()
    when (node) {
      null -> {
        nodeSheetBehavior.state = STATE_HIDDEN
        if ((sceneBottomSheet.tag as? Boolean) == true) {
          sceneBottomSheet.tag = false
          sceneBehavior.state = STATE_EXPANDED
        }
      }
      coordinator.selectedNode -> {
        nodeName.text = node.name
        (node as? MaterialNode)?.properties?.run {
          nodeColorValue.setColor(color)
          nodeMetallicValue.progress = metallic
          nodeRoughnessValue.progress = roughness
          nodeReflectanceValue.progress = reflectance
        }
        val materialVisibility = if (node is MaterialNode) VISIBLE else GONE
        setOfMaterialViews.forEach { it.visibility = materialVisibility }
        nodeSheetBehavior.state = STATE_EXPANDED
        if (sceneBehavior.state != STATE_COLLAPSED) {
          sceneBehavior.state = STATE_COLLAPSED
          sceneBottomSheet.tag = true
        }
      }
    }
  }
}
