package com.adityaarora.liveedgedetection.enums

import android.content.Context
import android.graphics.drawable.Drawable
import com.adityaarora.liveedgedetection.R

/**
 * Enum that defines receipt detection messages
 */
enum class ScanHint {
    MOVE_AWAY,
    MOVE_CLOSER,
    FIND_RECT,
    ADJUST_ANGLE,
    CAPTURING_IMAGE,
    NO_MESSAGE;

    fun getText(context: Context): String {
        val stringResId = when (this) {
            MOVE_CLOSER -> R.string.move_closer
            MOVE_AWAY -> R.string.move_away
            ADJUST_ANGLE -> R.string.adjust_angle
            FIND_RECT -> R.string.finding_rect
            CAPTURING_IMAGE -> R.string.hold_still
            NO_MESSAGE -> -1
        }
        return if (stringResId != -1) context.getString(stringResId) else ""
    }

    fun getDrawable(context: Context): Drawable? {
        val drawableResId = when (this) {
            MOVE_CLOSER,
            MOVE_AWAY,
            ADJUST_ANGLE -> R.drawable.hint_red
            FIND_RECT -> R.drawable.hint_white
            CAPTURING_IMAGE -> R.drawable.hint_green
            NO_MESSAGE -> -1
        }
        return if (drawableResId != -1) context.getDrawable(drawableResId) else null
    }

}