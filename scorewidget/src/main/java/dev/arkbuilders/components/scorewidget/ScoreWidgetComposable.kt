package dev.arkbuilders.components.scorewidget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.mdrlzy.counterslider.HorizontalCounterSlider
import com.mdrlzy.counterslider.VerticalCounterSlider
import org.orbitmvi.orbit.compose.collectAsState


@Composable
fun HorizontalScoreWidgetComposable(
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(200.dp, 80.dp),
    allowTopToReset: Boolean = true,
    allowBottomToReset: Boolean = true,
    controller: ScoreWidgetController,
) {
    val state by controller.collectAsState()
    if (state.visible.not())
        return
    HorizontalCounterSlider(
        modifier = modifier,
        size = size,
        value = state.score.toString(),
        allowTopToReset = allowTopToReset,
        allowBottomToReset = allowBottomToReset,
        onValueIncreaseClick = { controller.onIncrease() },
        onValueDecreaseClick = { controller.onDecrease() },
        onValueClearClick = { controller.onReset() }
    )
}

@Composable
fun VerticalScoreWidgetComposable(
    modifier: Modifier = Modifier,
    size: DpSize = DpSize(80.dp, 200.dp),
    allowLeftToReset: Boolean = true,
    allowRightToReset: Boolean = true,
    controller: ScoreWidgetController,
) {
    val state by controller.collectAsState()
    if (state.visible.not())
        return
    VerticalCounterSlider(
        modifier = modifier,
        size = size,
        value = state.score.toString(),
        allowLeftToReset = allowLeftToReset,
        allowRightToReset = allowRightToReset,
        onValueIncreaseClick = { controller.onIncrease() },
        onValueDecreaseClick = { controller.onDecrease() },
        onValueClearClick = { controller.onReset() }
    )
}
