package org.horizontal.tella.mobile.domain.peertopeer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IpAddressInputMaskTest {

    @Test
    fun appendDigit_autoInsertsDotAfterThirdDigit() {
        assertEquals("192.", IpAddressInputMask.appendDigit("", '1')?.let {
            IpAddressInputMask.appendDigit(it, '9')?.let { v2 ->
                IpAddressInputMask.appendDigit(v2, '2')
            }
        })
    }

    @Test
    fun appendDigit_doesNotPadWithZeros() {
        assertEquals("1", IpAddressInputMask.appendDigit("", '1'))
        assertEquals("19", IpAddressInputMask.appendDigit("1", '9'))
    }

    @Test
    fun appendDot_allowsShortOctet() {
        assertEquals("192.168.1.", IpAddressInputMask.appendDot("192.168.1"))
    }

    @Test
    fun applyInput_buildsTypicalAddress() {
        var value = ""
        for (char in "1921681.1") {
            value = IpAddressInputMask.appendChar(value, char).orEmpty()
        }
        assertEquals("192.168.1.1", value)
    }

    @Test
    fun appendDigit_rejectsFourthDigitWithoutDot() {
        assertNull(IpAddressInputMask.appendDigit("192", '4'))
    }

    @Test
    fun appendDigit_rejectsValueAbove255() {
        assertNull(IpAddressInputMask.appendDigit("25", '6'))
    }

    @Test
    fun isValidCompleteIp_acceptsStandardAddress() {
        assertTrue(IpAddressInputMask.isValidCompleteIp("192.168.1.1"))
    }

    @Test
    fun isValidCompleteIp_rejectsPartialAddress() {
        assertFalse(IpAddressInputMask.isValidCompleteIp("192.168.1"))
    }

    @Test
    fun toCanonicalIp_stripsLeadingZeros() {
        assertEquals("192.168.1.1", IpAddressInputMask.toCanonicalIp("192.168.001.001"))
    }

    @Test
    fun appendDigit_doesNotAddDotAfterFourthOctet() {
        assertEquals("192.168.1.1", IpAddressInputMask.appendDigit("192.168.1.", '1'))
    }
}
