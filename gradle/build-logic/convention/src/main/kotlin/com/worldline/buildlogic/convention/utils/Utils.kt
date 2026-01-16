package com.worldline.buildlogic.convention.utils

import java.util.Locale

fun String.capitalize() = this.replaceFirstChar {
    if (it.isLowerCase()) {
        it.titlecase(Locale.getDefault())
    } else {
        it.toString()
    }
}
