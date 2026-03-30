package com.mjc.feature.camera.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q]) // 测试Android 10+环境
class MediaStoreHelperTest {

    @Test
    fun `generateImageFileName should return proper format`() {
        // Act
        val fileName = MediaStoreHelper.generateImageFileName()

        // Assert
        assertTrue(fileName.startsWith("IMG_"))
        assertTrue(fileName.endsWith(".jpg"))
        assertTrue(fileName.length > "IMG_20260101_120000.jpg".length)
    }

    @Test
    fun `createImageContentValues should include required fields`() {
        // Arrange
        val fileName = "IMG_test.jpg"

        // Act
        val values = MediaStoreHelper.createImageContentValues(fileName)

        // Assert
        assertEquals(fileName, values.getAsString(MediaStore.Images.Media.DISPLAY_NAME))
        assertEquals("image/jpeg", values.getAsString(MediaStore.Images.Media.MIME_TYPE))
        assertNotNull(values.getAsLong(MediaStore.Images.Media.DATE_ADDED))
        assertNotNull(values.getAsLong(MediaStore.Images.Media.DATE_MODIFIED))

        // Android 10+ specific fields
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertEquals("Pictures/MyApplication/", values.getAsString(MediaStore.Images.Media.RELATIVE_PATH))
            assertEquals(1, values.getAsInteger(MediaStore.Images.Media.IS_PENDING))
        }
    }

    @Test
    fun `createImageContentValues with dateTaken should include DATE_TAKEN`() {
        // Arrange
        val fileName = "IMG_test.jpg"
        val dateTaken = 1234567890L

        // Act
        val values = MediaStoreHelper.createImageContentValues(fileName, dateTaken)

        // Assert
        assertEquals(dateTaken, values.getAsLong(MediaStore.Images.Media.DATE_TAKEN))
    }

    @Test
    fun `getImagesContentUri should return correct URI`() {
        // Act
        val uri = MediaStoreHelper.getImagesContentUri()

        // Assert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertEquals(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), uri)
        } else {
            assertEquals(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, uri)
        }
    }

    @Test
    fun `finalizePendingImage should update IS_PENDING to 0 on Android Q+`() {
        // Arrange
        val mockContentResolver = mockk<ContentResolver>()
        val mockUri = mockk<Uri>()
        val contentValuesSlot = slot<ContentValues>()

        every {
            mockContentResolver.update(mockUri, capture(contentValuesSlot), any(), any())
        } returns 1

        // Act
        MediaStoreHelper.finalizePendingImage(mockContentResolver, mockUri)

        // Assert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            verify { mockContentResolver.update(mockUri, any(), any(), any()) }
            val values = contentValuesSlot.captured
            assertEquals(0, values.getAsInteger(MediaStore.Images.Media.IS_PENDING))
        } else {
            // Should not call update on pre-Q
            // MockK会记录调用，但我们不验证，因为方法可能不会调用
        }
    }

    @Test
    fun `cleanupFailedImage should delete URI`() {
        // Arrange
        val mockContentResolver = mockk<ContentResolver>()
        val mockUri = mockk<Uri>()

        every {
            mockContentResolver.delete(mockUri, any(), any())
        } returns 1

        // Act
        MediaStoreHelper.cleanupFailedImage(mockContentResolver, mockUri)

        // Assert
        verify { mockContentResolver.delete(mockUri, null, null) }
    }

    @Test
    fun `supportsPendingFlag should return true on Android Q+`() {
        // Act
        val supports = MediaStoreHelper.supportsPendingFlag()

        // Assert
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            assertTrue(supports)
        } else {
            assertFalse(supports)
        }
    }
}