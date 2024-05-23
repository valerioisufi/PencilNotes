package com.studiomath.pencilnotes.document.tool

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import com.studiomath.pencilnotes.R
import com.google.android.material.chip.Chip

class DrawImpostazioni(context: Context, attrs: AttributeSet) : androidx.appcompat.widget.AppCompatImageView(context, attrs)  {
    var sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE)

    var modePenna = sharedPref.getBoolean(context.getString(R.string.mode_penna), false)


    init {
        setOnClickListener {
            showImpostazioniDialog(it)
        }
    }

    fun showImpostazioniDialog(view: View){
        var dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_draw_impostazioni)

        var window = dialog.window!!
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setGravity(Gravity.BOTTOM)
        window.attributes.windowAnimations = R.style.DialogAnimation

        dialog.setCancelable(true)
        window.setLayout(resources.displayMetrics.widthPixels, ViewGroup.LayoutParams.WRAP_CONTENT)

        var modePennaChip = dialog.findViewById<Chip>(R.id.modePennaView)
        modePennaChip.isChecked = modePenna

        dialog.show()

        modePennaChip.setOnCheckedChangeListener { buttonView, isChecked ->
            run {
                modePenna = isChecked
                with (sharedPref.edit()) {
                    putBoolean(context.getString(R.string.mode_penna), isChecked)
                    apply()
                }
            }
        }

    }

}