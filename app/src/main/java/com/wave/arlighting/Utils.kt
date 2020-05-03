package com.wave.arlighting

import android.content.Context
import android.graphics.Color
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.MaterialFactory.*
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class Utils(
  context: Context,
  private var fragment: ArFragment?
) {
  private var context: Context? = context

  fun makeSphere(
    hitResult: HitResult,
    color: Int
  ) {
    makeOpaqueWithColor(
      this.context,
      com.google.ar.sceneform.rendering.Color(
        color
      )
    ).thenAccept { material ->
      material.setFloat(MATERIAL_METALLIC,
        0.8F
      )
      addNodeToScene(
        this.fragment,
        hitResult.createAnchor(),
        ShapeFactory.makeSphere(
          0.1f,
          Vector3(
            0.0f,
            0.15f,
            0.0f
          ),
          material
        )
      )
    }
  }

  fun makeTransparentSphere(
    hitResult: HitResult,
    color: Int
  ) {
    makeTransparentWithColor(
      this.context,
      com.google.ar.sceneform.rendering.Color(
        Color.TRANSPARENT
      )
    ).thenAccept { material ->
      addNodeToScene(
        this.fragment,
        hitResult.createAnchor(),
        ShapeFactory.makeSphere(
          0.1f,
          Vector3(
            0.0f,
            0.15f,
            0.0f
          ),
          material
        )
      )
    }
  }

  private fun addNodeToScene(
    fragment: ArFragment?,
    anchor: Anchor,
    modelObject: ModelRenderable
  ) {
    val anchorNode = AnchorNode(anchor)

    TransformableNode(fragment!!.transformationSystem).apply {
      renderable = modelObject
      setParent(anchorNode)
      select()
    }

    fragment.arSceneView.scene.addChild(anchorNode)
  }
}
