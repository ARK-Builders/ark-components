@file:OptIn(ExperimentalComposeUiApi::class)

package dev.arkbuilders.canvas.presentation.edit

import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentManager
import dev.arkbuilders.canvas.R
import dev.arkbuilders.canvas.presentation.data.Resolution
import dev.arkbuilders.canvas.presentation.drawing.ArkColorPalette
import dev.arkbuilders.canvas.presentation.drawing.EditCanvas
import dev.arkbuilders.canvas.presentation.edit.blur.BlurIntensityPopup
import dev.arkbuilders.canvas.presentation.edit.crop.CropAspectRatiosMenu
import dev.arkbuilders.canvas.presentation.edit.resize.Hint
import dev.arkbuilders.canvas.presentation.edit.resize.ResizeInput
import dev.arkbuilders.canvas.presentation.edit.resize.delayHidingHint
import dev.arkbuilders.canvas.presentation.picker.toPx
import dev.arkbuilders.canvas.presentation.theme.Gray
import dev.arkbuilders.canvas.presentation.utils.askWritePermissions
import dev.arkbuilders.canvas.presentation.utils.getActivity
import dev.arkbuilders.canvas.presentation.utils.isWritePermGranted
import java.nio.file.Path

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
fun EditScreen(
    imagePath: Path?,
    imageUri: String?,
    fragmentManager: FragmentManager,
    navigateBack: () -> Unit,
    launchedFromIntent: Boolean,
    maxResolution: Resolution,
    onSaveSvg: () -> Unit,
    viewModel: EditViewModel,
) {
    val context = LocalContext.current
    val showDefaultsDialog = remember {
        mutableStateOf(
            imagePath == null && imageUri == null && !viewModel.isLoaded
        )
    }

    if (showDefaultsDialog.value) {
        viewModel.editManager.resolution.value?.let {
            NewImageOptionsDialog(
                it,
                maxResolution,
                viewModel.editManager.backgroundColor.value,
                navigateBack,
                viewModel.editManager,
                persistDefaults = { color, resolution ->
                    viewModel.persistDefaults(color, resolution)
                },
                onConfirm = {
                    showDefaultsDialog.value = false
                }
            )
        }
    }
    ExitDialog(
        viewModel = viewModel,
        navigateBack = {
            navigateBack()
            viewModel.isLoaded = false
        },
        launchedFromIntent = launchedFromIntent,
    )

    HandleImageSavedEffect(viewModel, launchedFromIntent, navigateBack)

    if (!showDefaultsDialog.value)
        DrawContainer(
            viewModel
        )

    Menus(
        imagePath,
        fragmentManager,
        viewModel,
        launchedFromIntent,
        navigateBack
    )

    if (viewModel.isSavingImage) {
        SaveProgress()
    }

    if (viewModel.showEyeDropperHint) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Hint(stringResource(R.string.pick_color)) {
                delayHidingHint(it) {
                    viewModel.showEyeDropperHint = false
                }
                viewModel.showEyeDropperHint
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun Menus(
    imagePath: Path?,
    fragmentManager: FragmentManager,
    viewModel: EditViewModel,
    launchedFromIntent: Boolean,
    navigateBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            TopMenu(
                imagePath,
                fragmentManager,
                viewModel,
                launchedFromIntent,
                navigateBack
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (viewModel.editManager.isRotateMode.value)
                Row {
                    Icon(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                viewModel.editManager.apply {
                                    rotate(-90f)
                                    invalidatorTick.value++
                                }
                            },
                        imageVector = ImageVector
                            .vectorResource(R.drawable.ic_rotate_left),
                        tint = ArkColorPalette.primary,
                        contentDescription = null
                    )
                    Icon(
                        modifier = Modifier
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                viewModel.editManager.apply {
                                    rotate(90f)
                                    invalidatorTick.value++
                                }
                            },
                        imageVector = ImageVector
                            .vectorResource(R.drawable.ic_rotate_right),
                        tint = ArkColorPalette.primary,
                        contentDescription = null
                    )
                }

            EditMenuContainer(viewModel, navigateBack)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DrawContainer(
    viewModel: EditViewModel
) {

    val context = LocalContext.current
    Box(
        modifier = Modifier
            .padding(bottom = 32.dp)
            .fillMaxSize()
            .background(
                if (viewModel.editManager.isCropMode.value) Color.White
                else Color.Gray
            )
            .pointerInteropFilter { event ->
                if (event.action == MotionEvent.ACTION_DOWN)
                    viewModel.strokeSliderExpanded = false
                false
            }
            .onSizeChanged { newSize ->
                if (newSize == IntSize.Zero || viewModel.showSavePathDialog) return@onSizeChanged
                viewModel.editManager.drawAreaSize.value = newSize
                if (viewModel.isLoaded) {
                    viewModel.editManager.onResizeChanged(newSize)
                }
                viewModel.loadImage(context)
            },
        contentAlignment = Alignment.Center
    ) {
        EditCanvas(viewModel)
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@Composable
private fun BoxScope.TopMenu(
    imagePath: Path?,
    fragmentManager: FragmentManager,
    viewModel: EditViewModel,
    launchedFromIntent: Boolean,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current

    if (viewModel.showSavePathDialog)
        SavePathDialog(
            initialImagePath = imagePath,
            fragmentManager = fragmentManager,
            onDismissClick = { viewModel.showSavePathDialog = false },
            onPositiveClick = { savePath ->
                viewModel.saveImage(savePath)
            }
        )
    if (viewModel.showMoreOptionsPopup)
        MoreOptionsPopup(
            onDismissClick = {
                viewModel.showMoreOptionsPopup = false
            },
            onShareClick = {
                viewModel.shareImage(context)
                viewModel.showMoreOptionsPopup = false
            },
            onSaveClick = {
                if (!context.isWritePermGranted()) {
                    context.askWritePermissions()
                    return@MoreOptionsPopup
                }
                viewModel.showSavePathDialog = true
            },
            onClearEdits = {
                viewModel.showConfirmClearDialog.value = true
                viewModel.showMoreOptionsPopup = false
            }
        )

    ConfirmClearDialog(
        viewModel.showConfirmClearDialog,
        onConfirm = {
            viewModel.editManager.clearEdits()
        }
    )

    if (
        !viewModel.menusVisible &&
        viewModel.editManager.isControlsDisabled()
    )
        return
    Icon(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .size(36.dp)
            .clip(CircleShape)
            .clickable {
                viewModel.editManager.apply {
                    if (shouldCancelOperation()) {
                        viewModel.cancelOperation()
                        return@clickable
                    }
                    if (isZoomMode.value) {
                        toggleZoomMode()
                        return@clickable
                    }
                    if (isPanMode.value) {
                        togglePanMode()
                        return@clickable
                    }
                    if (
                        !canUndo.value
                    ) {
                        if (launchedFromIntent) {
                            context
                                .getActivity()
                                ?.finish()
                        } else {
                            navigateBack()
                        }
                    } else {
                        viewModel.showExitDialog = true
                    }
                }
            },
        imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
        tint = ArkColorPalette.primary,
        contentDescription = null
    )

    Row(
        Modifier
            .align(Alignment.TopEnd)
    ) {
        Icon(
            modifier = Modifier
                .padding(8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    if (viewModel.editManager.shouldApplyOperation()) {
                        viewModel.applyOperation()
                        return@clickable
                    }
                    viewModel.showMoreOptionsPopup = true
                },
            imageVector = if (viewModel.editManager.shouldApplyOperation())
                ImageVector.vectorResource(R.drawable.ic_check)
            else ImageVector.vectorResource(R.drawable.ic_more_vert),
            tint = ArkColorPalette.primary,
            contentDescription = null
        )
    }
}

@Composable
private fun StrokeWidthPopup(
    modifier: Modifier,
    viewModel: EditViewModel
) {
    val editManager = viewModel.editManager
    editManager.setPaintStrokeWidth(viewModel.strokeWidth.dp.toPx())
    if (viewModel.strokeSliderExpanded) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .padding(
                            horizontal = 10.dp,
                            vertical = 5.dp
                        )
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(viewModel.strokeWidth.dp)
                        .clip(RoundedCornerShape(30))
                        .background(editManager.paintColor.value)
                )
            }

            Slider(
                modifier = Modifier
                    .fillMaxWidth(),
                value = viewModel.strokeWidth,
                onValueChange = {
                    viewModel.strokeWidth = it
                },
                valueRange = 0.5f..50f,
            )
        }
    }
}

@Composable
private fun EditMenuContainer(viewModel: EditViewModel, navigateBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        CropAspectRatiosMenu(
            isVisible = viewModel.editManager.isCropMode.value,
            viewModel.editManager.cropWindow
        )
        ResizeInput(
            isVisible = viewModel.editManager.isResizeMode.value,
            viewModel.editManager
        )

        Box(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(topStartPercent = 30, topEndPercent = 30))
                .background(Gray)
                .clickable {
                    viewModel.menusVisible = !viewModel.menusVisible
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (viewModel.menusVisible) Icons.Filled.KeyboardArrowDown
                else Icons.Filled.KeyboardArrowUp,
                contentDescription = "",
                modifier = Modifier.size(32.dp),
            )
        }
        AnimatedVisibility(
            visible = viewModel.menusVisible,
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            EditMenuContent(viewModel, navigateBack)
            EditMenuFlowHint(
                viewModel.bottomButtonsScrollIsAtStart.value,
                viewModel.bottomButtonsScrollIsAtEnd.value
            )
        }
    }
}

@Composable
private fun EditMenuContent(
    viewModel: EditViewModel,
    navigateBack: () -> Unit
) {
    val colorDialogExpanded = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val editManager = viewModel.editManager
    Column(
        Modifier
            .fillMaxWidth()
            .background(Gray)
    ) {
        StrokeWidthPopup(Modifier, viewModel)

        BlurIntensityPopup(editManager)

        Row(
            Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp)
                .horizontalScroll(scrollState)
        ) {
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (
                            editManager.isEligibleForUndoOrRedo()
                        ) {
                            editManager.undo()
                        }
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_undo),
                tint = if (
                    editManager.canUndo.value && (editManager.isEligibleForUndoOrRedo())
                ) ArkColorPalette.primary else Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (editManager.isEligibleForUndoOrRedo()) editManager.redo()
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_redo),
                tint = if (
                    editManager.canRedo.value &&
                    (editManager.isEligibleForUndoOrRedo())
                ) ArkColorPalette.primary else Color.Black,
                contentDescription = null
            )
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color = editManager.paintColor.value)
                    .clickable {
                        if (editManager.isEyeDropperMode.value) {
                            viewModel.toggleEyeDropper()
                            viewModel.cancelEyeDropper()
                            colorDialogExpanded.value = true
                            return@clickable
                        }
                        if (editManager.shouldExpandColorDialog())
                            colorDialogExpanded.value = true
                    }
            )
            ColorPickerDialog(
                isVisible = colorDialogExpanded,
                initialColor = editManager.paintColor.value,
                usedColors = viewModel.usedColors,
                enableEyeDropper = true,
                onToggleEyeDropper = {
                    viewModel.toggleEyeDropper()
                },
                onColorChanged = {
                    editManager.setPaintColor(it)
                    viewModel.trackColor(it)
                }
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (editManager.isEligibleForStrokeExpandOrErase())
                            viewModel.strokeSliderExpanded = !viewModel.strokeSliderExpanded
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_line_weight),
                tint = if (editManager.isEligibleForUndoOrRedo()) editManager.paintColor.value else Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (editManager.isEligibleForStrokeExpandOrErase())
                            editManager.toggleEraseMode()
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_eraser),
                tint = if (
                    editManager.isEraseMode.value
                )
                    ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (editManager.isEligibleForPanOrZoomMode()) editManager.toggleZoomMode()
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_zoom_in),
                tint = if (
                    editManager.isZoomMode.value
                )
                    ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (editManager.isEligibleForPanOrZoomMode()) editManager.togglePanMode()
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_pan_tool),
                tint = if (
                    editManager.isPanMode.value
                )
                    ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (!editManager.isEligibleForCropOrRotate()) return@clickable
                        editManager.enterCropMode()
                        viewModel.menusVisible = !editManager.isCropMode.value
                    },
                imageVector = ImageVector.vectorResource(R.drawable.ic_crop),
                tint = if (
                    editManager.isCropMode.value
                ) ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (!editManager.isEligibleForCropOrRotate()) return@clickable
                        editManager.enterRotateMode()
                        viewModel.menusVisible = !editManager.isRotateMode.value
                    },
                imageVector = ImageVector
                    .vectorResource(R.drawable.ic_rotate_90_degrees_ccw),
                tint = if (editManager.isRotateMode.value)
                    ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        editManager.enterResizeMode()
                        viewModel.menusVisible = !editManager.isResizeMode.value
                    },
                imageVector = ImageVector
                    .vectorResource(R.drawable.ic_aspect_ratio),
                tint = if (editManager.isResizeMode.value)
                    ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
            Icon(
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        editManager.enterBlurMode(viewModel.strokeSliderExpanded)
                    },
                imageVector = ImageVector
                    .vectorResource(R.drawable.ic_blur_on),
                tint = if (editManager.isBlurMode.value)
                    ArkColorPalette.primary
                else
                    Color.Black,
                contentDescription = null
            )
        }
    }
    viewModel.onBottomButtonStateChange(scrollState.value, 0, scrollState.maxValue)
}

@Composable
fun EditMenuFlowHint(
    scrollIsAtStart: Boolean = true,
    scrollIsAtEnd: Boolean = false
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = scrollIsAtEnd || (!scrollIsAtStart && !scrollIsAtEnd),
            enter = fadeIn(tween(durationMillis = 1000)),
            exit = fadeOut((tween(durationMillis = 1000))),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Icon(
                Icons.Filled.KeyboardArrowLeft,
                contentDescription = null,
                Modifier
                    .background(Gray)
                    .padding(top = 16.dp, bottom = 16.dp)
                    .size(32.dp)
            )
        }
        AnimatedVisibility(
            visible = scrollIsAtStart || (!scrollIsAtStart && !scrollIsAtEnd),
            enter = fadeIn(tween(durationMillis = 1000)),
            exit = fadeOut((tween(durationMillis = 1000))),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                Modifier
                    .background(Gray)
                    .padding(top = 16.dp, bottom = 16.dp)
                    .size(32.dp)
            )
        }
    }
}

@Composable
private fun HandleImageSavedEffect(
    viewModel: EditViewModel,
    launchedFromIntent: Boolean,
    navigateBack: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel.imageSaved) {
        if (!viewModel.imageSaved)
            return@LaunchedEffect
        if (launchedFromIntent)
            context.getActivity()?.finish()
        else
            navigateBack()
    }
}

@Composable
private fun ExitDialog(
    viewModel: EditViewModel,
    navigateBack: () -> Unit,
    launchedFromIntent: Boolean
) {
    if (!viewModel.showExitDialog) return

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {
            viewModel.showExitDialog = false
        },
        title = {
            Text(
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                text = "Do you want to save the changes?",
                fontSize = 16.sp
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.showExitDialog = false
                    viewModel.showSavePathDialog = true
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = ArkColorPalette.primary)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.showExitDialog = false
                    if (launchedFromIntent) {
                        context.getActivity()?.finish()
                    } else {
                        navigateBack()
                    }
                },
                colors = ButtonDefaults.textButtonColors(contentColor = ArkColorPalette.primary)
            ) {
                Text(
                    text = "Exit",
                    color = ArkColorPalette.primary,
                )
            }
        }
    )
}
