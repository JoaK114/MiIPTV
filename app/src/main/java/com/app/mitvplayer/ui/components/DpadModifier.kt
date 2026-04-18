package com.app.mitvplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged

/**
 * Unified modifier for TV D-pad and mobile touch interaction.
 * Replaces the broken .focusable() + .onKeyEvent() + .clickable() chain
 * with a single clean modifier that works on both platforms.
 */
fun Modifier.dpadClickable(
    onClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit = {}
): Modifier = composed {
    this
        .onFocusChanged { onFocusChange(it.isFocused) }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
}
