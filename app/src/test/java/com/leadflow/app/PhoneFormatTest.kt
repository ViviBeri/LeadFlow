package com.leadflow.app

import com.leadflow.app.util.PhoneFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneFormatTest {
    @Test fun plusNinetyOneWithSpaces() = assertEquals("9876543210", PhoneFormat.toIndianMobile("+91 98765 43210"))
    @Test fun leadingZero() = assertEquals("9876543210", PhoneFormat.toIndianMobile("098765 43210"))
    @Test fun plainTenDigits() = assertEquals("7012345678", PhoneFormat.toIndianMobile("7012345678"))
    @Test fun landlineIsRejected() = assertEquals("", PhoneFormat.toIndianMobile("+91 413 2222111"))
    @Test fun picksMobileFromList() = assertEquals("9876543210", PhoneFormat.toIndianMobile("0413 2222 111; +91 98765 43210"))
    @Test fun blankIsBlank() = assertEquals("", PhoneFormat.toIndianMobile(""))
}
