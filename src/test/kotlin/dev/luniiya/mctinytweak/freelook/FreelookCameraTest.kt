package dev.luniiya.mctinytweak.freelook

import net.minecraft.client.Camera
import net.minecraft.client.CameraType
import net.minecraft.client.MouseHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FreelookCameraTest {
    @Test
    fun `starting in first person switches to the back view and stopping restores it`() {
        val camera = FreelookCamera()
        assertEquals(CameraType.THIRD_PERSON_BACK, camera.start(30f, 10f, CameraType.FIRST_PERSON))
        assertTrue(camera.active)
        assertEquals(30f, camera.yaw)
        assertEquals(10f, camera.pitch)
        assertEquals(CameraType.FIRST_PERSON, camera.stop())
        assertFalse(camera.active)
        assertNull(camera.stop(), "a second stop has nothing to restore")
    }

    @Test
    fun `third person views are kept while looking`() {
        assertEquals(CameraType.THIRD_PERSON_FRONT, FreelookCamera().start(0f, 0f, CameraType.THIRD_PERSON_FRONT))
        assertEquals(CameraType.THIRD_PERSON_BACK, FreelookCamera.perspectiveWhileLooking(CameraType.THIRD_PERSON_BACK))
    }

    @Test
    fun `mouse movement uses the vanilla scale and clamps only the pitch`() {
        val camera = FreelookCamera()
        camera.start(0f, 80f, CameraType.FIRST_PERSON)
        camera.turn(2400.0, 200.0)
        assertEquals(360f, camera.yaw, 0.001f)
        assertEquals(90f, camera.pitch)
        camera.turn(0.0, -2000.0)
        assertEquals(-90f, camera.pitch)
    }

    @Test
    fun `freelook mixins apply to the vanilla camera and mouse handler`() {
        assertTrue(Camera::class.java.declaredMethods.any { it.name.contains("useFreelookRotation") })
        assertTrue(MouseHandler::class.java.declaredMethods.any { it.name.contains("turnFreelook") })
    }
}
