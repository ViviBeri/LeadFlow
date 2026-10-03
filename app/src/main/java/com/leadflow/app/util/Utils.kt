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
