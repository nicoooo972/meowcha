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
 * `assetPath` pointe dans `app/src/main/assets/` (ex. "models/test_duck.glb" pour le modèle de
 * test livré avec ce spike, en attendant un premier export Blender du projet).
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
