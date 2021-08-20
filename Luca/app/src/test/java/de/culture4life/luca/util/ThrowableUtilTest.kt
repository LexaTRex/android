package de.culture4life.luca.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrowableUtilTest {

    @Test
    fun isCause_directMatch_returnsTrue() {
        assertTrue(
            ThrowableUtil.isCause(
                IllegalStateException::class.java,
                IllegalStateException(IllegalArgumentException())
            )
        )
    }

    @Test
    fun isCause_directMatchWithoutCause_returnsTrue() {
        assertTrue(
            ThrowableUtil.isCause(
                IllegalStateException::class.java,
                IllegalStateException()
            )
        )
    }

    @Test
    fun isCause_wrappedMatch_returnsTrue() {
        assertTrue(
            ThrowableUtil.isCause(
                IllegalArgumentException::class.java,
                IllegalStateException(IllegalArgumentException())
            )
        )
    }

    @Test
    fun isCause_noMatch_returnsFalse() {
        assertFalse(
            ThrowableUtil.isCause(
                NullPointerException::class.java,
                IllegalStateException(IllegalArgumentException())
            )
        )
    }

}