package com.corey.notepad3.app

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class TrackpadPoint(
    val x: Float,
    val y: Float,
)

data class TrackpadDelta(
    val dx: Float,
    val dy: Float,
)

data class TrackpadBounds(
    val width: Float,
    val height: Float,
)

data class TrackpadInsets(
    val left: Float = 8f,
    val top: Float = 8f,
    val right: Float = 8f,
    val bottom: Float = 8f,
)

enum class VirtualTrackpadSize(
    val bounds: TrackpadBounds,
) {
    SMALL(TrackpadBounds(width = 180f, height = 132f)),
    MEDIUM(TrackpadBounds(width = 220f, height = 160f)),
    LARGE(TrackpadBounds(width = 270f, height = 198f)),
    EXTRA_LARGE(TrackpadBounds(width = 336f, height = 248f)),
    HUGE(TrackpadBounds(width = 390f, height = 300f));

    fun next(): VirtualTrackpadSize =
        when (this) {
            SMALL -> MEDIUM
            MEDIUM -> LARGE
            LARGE -> EXTRA_LARGE
            EXTRA_LARGE -> HUGE
            HUGE -> SMALL
        }

    fun nextFitting(
        container: TrackpadBounds,
        insets: TrackpadInsets,
    ): VirtualTrackpadSize {
        var candidate = next()
        repeat(entries.size) {
            if (candidate.fits(container, insets)) return candidate
            candidate = candidate.next()
        }
        return this
    }

    fun coerceToFit(
        container: TrackpadBounds,
        insets: TrackpadInsets,
    ): VirtualTrackpadSize =
        if (fits(container, insets)) {
            this
        } else {
            entries.asReversed().firstOrNull { it.fits(container, insets) } ?: SMALL
        }

    private fun fits(
        container: TrackpadBounds,
        insets: TrackpadInsets,
    ): Boolean =
        bounds.width <= max(0f, container.width - insets.left - insets.right) &&
            bounds.height <= max(0f, container.height - insets.top - insets.bottom)
}

enum class TrackpadDirection {
    LEFT,
    UP,
    DOWN,
    RIGHT,
}

data class VirtualTrackpadState(
    val position: TrackpadPoint = TrackpadPoint(x = 0f, y = 0f),
    val size: VirtualTrackpadSize = VirtualTrackpadSize.MEDIUM,
    val isPinned: Boolean = false,
    val pointerPosition: TrackpadPoint = TrackpadPoint(x = 0f, y = 0f),
    val insets: TrackpadInsets = TrackpadInsets(),
    val pointerSensitivity: Float = DEFAULT_POINTER_SENSITIVITY,
    val tapSlop: Float = DEFAULT_TAP_SLOP,
) {
    val bounds: TrackpadBounds
        get() = size.bounds

    val center: TrackpadPoint
        get() = TrackpadPoint(
            x = position.x + bounds.width / 2f,
            y = position.y + bounds.height / 2f,
        )

    fun togglePinned(): VirtualTrackpadState =
        copy(isPinned = !isPinned)

    fun cycleSize(container: TrackpadBounds): VirtualTrackpadState {
        val currentCenter = center
        val nextSize = size.nextFitting(container, insets)
        val nextBounds = nextSize.bounds
        return copy(
            size = nextSize,
            position = TrackpadPoint(
                x = currentCenter.x - nextBounds.width / 2f,
                y = currentCenter.y - nextBounds.height / 2f,
            ),
        ).clampedTo(container)
    }

    fun movePanelBy(
        dx: Float,
        dy: Float,
        container: TrackpadBounds,
    ): VirtualTrackpadState {
        if (isPinned) return this
        return copy(
            position = TrackpadPoint(
                x = position.x + dx,
                y = position.y + dy,
            ),
        ).clampedTo(container)
    }

    fun clampedTo(container: TrackpadBounds): VirtualTrackpadState =
        size.coerceToFit(container, insets).let { fittingSize ->
            copy(
                size = fittingSize,
                position = clampPanelPosition(position, fittingSize.bounds, container, insets),
            )
        }

    fun movePointerBy(
        dx: Float,
        dy: Float,
        pointerBounds: TrackpadBounds,
    ): VirtualTrackpadState =
        copy(
            pointerPosition = clampPointerPosition(
                TrackpadPoint(
                    x = pointerPosition.x + dx * pointerSensitivity,
                    y = pointerPosition.y + dy * pointerSensitivity,
                ),
                pointerBounds,
            ),
        )

    fun directionalDelta(
        direction: TrackpadDirection,
        step: Int = 1,
    ): TrackpadDelta {
        val amount = step.coerceAtLeast(1).toFloat()
        return when (direction) {
            TrackpadDirection.LEFT -> TrackpadDelta(dx = -amount, dy = 0f)
            TrackpadDirection.UP -> TrackpadDelta(dx = 0f, dy = -amount)
            TrackpadDirection.DOWN -> TrackpadDelta(dx = 0f, dy = amount)
            TrackpadDirection.RIGHT -> TrackpadDelta(dx = amount, dy = 0f)
        }
    }

    companion object {
        const val DEFAULT_POINTER_SENSITIVITY = 1.8f
        const val DEFAULT_TAP_SLOP = 4f

        fun defaultPosition(
            container: TrackpadBounds,
            layoutBottomInset: Float = 64f,
            margin: Float = 12f,
        ): TrackpadPoint {
            val bounds = VirtualTrackpadSize.MEDIUM.bounds
            return clampPanelPosition(
                position = TrackpadPoint(
                    x = container.width - bounds.width - margin,
                    y = container.height - layoutBottomInset - 16f - bounds.height,
                ),
                panel = bounds,
                container = container,
                insets = TrackpadInsets(left = margin, top = margin, right = margin, bottom = margin),
            )
        }
    }
}

data class TrackpadPointerDrag(
    val lastTranslation: TrackpadPoint = TrackpadPoint(x = 0f, y = 0f),
    val movedPastTapSlop: Boolean = false,
) {
    fun update(
        state: VirtualTrackpadState,
        translation: TrackpadPoint,
        pointerBounds: TrackpadBounds,
    ): TrackpadPointerDragResult {
        val rawDelta = TrackpadDelta(
            dx = translation.x - lastTranslation.x,
            dy = translation.y - lastTranslation.y,
        )
        val scaledDelta = TrackpadDelta(
            dx = rawDelta.dx * state.pointerSensitivity,
            dy = rawDelta.dy * state.pointerSensitivity,
        )
        return TrackpadPointerDragResult(
            state = state.movePointerBy(rawDelta.dx, rawDelta.dy, pointerBounds),
            drag = copy(
                lastTranslation = translation,
                movedPastTapSlop = movedPastTapSlop ||
                    abs(translation.x) > state.tapSlop ||
                    abs(translation.y) > state.tapSlop,
            ),
            delta = scaledDelta,
        )
    }

    fun reset(): TrackpadPointerDrag = TrackpadPointerDrag()
}

data class TrackpadPointerDragResult(
    val state: VirtualTrackpadState,
    val drag: TrackpadPointerDrag,
    val delta: TrackpadDelta,
) {
    val movedPastTapSlop: Boolean
        get() = drag.movedPastTapSlop
}

data class TrackpadCaretMovementAccumulator(
    val horizontalRemainder: Float = 0f,
    val verticalRemainder: Float = 0f,
    val stepThreshold: Float = DEFAULT_CARET_STEP_THRESHOLD,
) {
    fun update(delta: TrackpadDelta): TrackpadCaretMovementResult {
        val horizontal = horizontalRemainder + delta.dx
        val vertical = verticalRemainder + delta.dy
        val horizontalSteps = stepsFor(horizontal)
        val verticalSteps = stepsFor(vertical)
        return TrackpadCaretMovementResult(
            accumulator = copy(
                horizontalRemainder = horizontal - horizontalSteps * stepThreshold,
                verticalRemainder = vertical - verticalSteps * stepThreshold,
            ),
            horizontalSteps = horizontalSteps,
            verticalSteps = verticalSteps,
        )
    }

    private fun stepsFor(value: Float): Int =
        if (abs(value) < stepThreshold) 0 else (value / stepThreshold).toInt()

    companion object {
        const val DEFAULT_CARET_STEP_THRESHOLD = 6f
    }
}

data class TrackpadCaretMovementResult(
    val accumulator: TrackpadCaretMovementAccumulator,
    val horizontalSteps: Int,
    val verticalSteps: Int,
)

private fun clampPanelPosition(
    position: TrackpadPoint,
    panel: TrackpadBounds,
    container: TrackpadBounds,
    insets: TrackpadInsets,
): TrackpadPoint {
    val minX = insets.left
    val minY = insets.top
    val maxX = max(minX, container.width - insets.right - panel.width)
    val maxY = max(minY, container.height - insets.bottom - panel.height)
    return TrackpadPoint(
        x = position.x.coerceIn(minX, maxX),
        y = position.y.coerceIn(minY, maxY),
    )
}

private fun clampPointerPosition(
    position: TrackpadPoint,
    bounds: TrackpadBounds,
): TrackpadPoint {
    val maxX = max(0f, bounds.width - 1f)
    val maxY = max(0f, bounds.height - 1f)
    return TrackpadPoint(
        x = min(max(0f, position.x), maxX),
        y = min(max(0f, position.y), maxY),
    )
}
