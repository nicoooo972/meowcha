package com.meowcha.game.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Spike technique 2.0.0 : rendu d'un modèle .glb (Blender → glTF Binary) via SceneView/Filament.
 * `assetPath` pointe dans `app/src/main/assets/` (ex. "models/cat_cafe_mugs.glb" pour le premier
 * export Blender du projet ; "models/test_duck.glb" reste le modèle de test du spike initial).
 */
@Composable
fun Model3DPreview(assetPath: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val model = rememberModelInstance(modelLoader, assetPath)

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        cameraManipulator = rememberCameraManipulator(),
    ) {
        model?.let {
            ModelNode(
                modelInstance = it,
                scaleToUnits = 1f,
                autoAnimate = true,
            )
        }
    }
}
