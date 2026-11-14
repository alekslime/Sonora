package com.sonora.shelf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfGeometryTest {

    @Test
    fun selectedSleeveIsFullScaleAndOpaque() {
        val p = ShelfGeometry.placement(0f, 200f)
        assertEquals(1f, p.scale, 0.001f)
        assertEquals(1f, p.alpha, 0.001f)
        assertEquals(0f, p.yCenterPx, 0.001f)
    }

    @Test
    fun placementIsSymmetric() {
        val above = ShelfGeometry.placement(-2f, 200f)
        val below = ShelfGeometry.placement(2f, 200f)
        assertEquals(above.scale, below.scale, 0.0001f)
        assertEquals(above.alpha, below.alpha, 0.0001f)
        assertEquals(-above.yCenterPx, below.yCenterPx, 0.001f)
    }

    @Test
    fun distantSleevesShrinkFadeAndDim() {
        val near = ShelfGeometry.placement(1f, 200f)
        val far = ShelfGeometry.placement(4f, 200f)
        assertTrue(far.scale < near.scale)
        assertTrue(far.alpha < near.alpha)
        assertTrue(far.dim > near.dim)
        assertTrue(far.alpha >= 0f)
        assertTrue(far.dim <= 0.6f)
    }

    @Test
    fun saturationFadesWithDistance() {
        assertEquals(1f, ShelfGeometry.saturation(0f), 0.001f)
        assertEquals(0.91f, ShelfGeometry.saturation(1f), 0.001f)
        assertEquals(0.64f, ShelfGeometry.saturation(-4f), 0.001f)
        assertEquals(0.6f, ShelfGeometry.saturation(9f), 0.001f)
    }

    @Test
    fun visibleRangeStaysInBounds() {
        assertEquals(0..2, ShelfGeometry.visibleRange(0f, 20))
        assertEquals(15..19, ShelfGeometry.visibleRange(19f, 20))
        assertEquals(1..7, ShelfGeometry.visibleRange(5f, 20))
        assertTrue(ShelfGeometry.visibleRange(0f, 0).isEmpty())
    }
}
