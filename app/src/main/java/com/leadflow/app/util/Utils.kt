package com.leadflow.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.leadflow.app.data.Lead

object Validators {
    private val mobile = Regex("^[6-9][0-9]{9}$")

    /** Returns an error message, or null if the number is a valid Indian mobile. */
    fun phoneError(p: String): String? = when {
        p.length != 10 -> "Enter exactly 10 digits"
        p[0] !in '6'..'9' -> "Must start with 6, 7, 8 or 9 (landlines are not accepted)"
        !mobile.matches(p) -> "Invalid mobile number"
        else -> null
    }
}

object PhoneFormat {
    /**
     * Turns a phone string from an outside source (e.g. OpenStreetMap: "+91 98765 43210",
     * "098765 43210", "0413 2222 111; 98765 43210") into a 10-digit Indian mobile number.
     * Returns "" when no valid mobile is present, so landlines never reach the WhatsApp button.
     */
    fun toIndianMobile(raw: String): String {
        for (part in raw.split(';', ',', '/')) {
            var digits = part.filter { it in '0'..'9' }
            if (digits.length == 12 && digits.startsWith("91")) digits = digits.drop(2)
            else if (digits.length == 11 && digits.startsWith("0")) digits = digits.drop(1)
            if (Validators.phoneError(digits) == null) return digits
        }
        return ""
    }
}

object WhatsApp {
    fun message(template: String, businessName: String) =
        template.replace("{Business Name}", businessName)

    fun link(phone: String, message: String) =
        "https://wa.me/91$phone?text=${Uri.encode(message)}"

    /** Opens the chat with the message pre-filled. The user still taps Send themselves. */
    fun open(context: Context, lead: Lead, template: String) {
        val url = link(lead.phone, message(template, lead.businessName))
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "WhatsApp or a browser is required", Toast.LENGTH_LONG).show()
        }
    }
}
