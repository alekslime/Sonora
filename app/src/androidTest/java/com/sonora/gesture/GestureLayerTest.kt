package com.sonora.gesture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Live multi-pointer verification of [SonoraGestureLayer] through the real
 * input pipeline. The hold timer runs on real time, so hold cases sleep
 * just past the 700ms default.
 */
class GestureLayerTest {

    @get:Rule
    val rule = createComposeRule()

    private val actions = mutableListOf<GestureAction>()

    private fun host(context: GestureContext) {
        actions.clear()
        rule.setContent {
            SonoraGestureLayer(
                enabled = true,
                context = context,
                onAction = { actions += it },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("pad"),
                )
            }
        }
    }

    @Test
    fun threeFingerHoldOpensLiquid() {
        host(GestureContext.NOW_PLAYING)
        rule.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(340f, 1200f))
            down(1, Offset(540f, 1200f))
            down(2, Offset(740f, 1200f))
        }
        Thread.sleep(950)
        rule.onNodeWithTag("pad").performTouchInput {
            up(0)
            up(1)
            up(2)
        }
        rule.waitUntil(3000) { actions.isNotEmpty() }
        assertEquals(listOf(GestureAction.OPEN_LIQUID), actions)
    }

    @Test
    fun threeFingerSwipeUpOpensStudio() {
        host(GestureContext.NOW_PLAYING)
        rule.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(340f, 1500f))
            down(1, Offset(540f, 1500f))
            down(2, Offset(740f, 1500f))
            updatePointerTo(0, Offset(340f, 950f))
            updatePointerTo(1, Offset(540f, 950f))
            updatePointerTo(2, Offset(740f, 950f))
            move()
            up(0)
            up(1)
            up(2)
        }
        rule.waitUntil(3000) { actions.isNotEmpty() }
        assertEquals(listOf(GestureAction.OPEN_STUDIO), actions)
    }

    @Test
    fun threeFingerSwipeDownClosesStudio() {
        host(GestureContext.STUDIO)
        rule.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(340f, 800f))
            down(1, Offset(540f, 800f))
            down(2, Offset(740f, 800f))
            updatePointerTo(0, Offset(340f, 1350f))
            updatePointerTo(1, Offset(540f, 1350f))
            updatePointerTo(2, Offset(740f, 1350f))
            move()
            up(0)
            up(1)
            up(2)
        }
        rule.waitUntil(3000) { actions.isNotEmpty() }
        assertEquals(listOf(GestureAction.CLOSE_STUDIO), actions)
    }

    @Test
    fun shortThreeFingerTapDoesNothing() {
        host(GestureContext.NOW_PLAYING)
        rule.onNodeWithTag("pad").performTouchInput {
            down(0, Offset(340f, 1200f))
            down(1, Offset(540f, 1200f))
            down(2, Offset(740f, 1200f))
            up(0)
            up(1)
            up(2)
        }
        rule.waitForIdle()
        assertTrue(actions.isEmpty())
    }

    @Test
    fun singleTapPassesThrough() {
        var taps = 0
        actions.clear()
        rule.setContent {
            SonoraGestureLayer(
                enabled = true,
                context = GestureContext.NOW_PLAYING,
                onAction = { actions += it },
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("pad")
                        .clickable { taps++ },
                )
            }
        }
        rule.onNodeWithTag("pad").performClick()
        rule.waitForIdle()
        assertEquals(1, taps)
        assertTrue(actions.isEmpty())
    }
}
