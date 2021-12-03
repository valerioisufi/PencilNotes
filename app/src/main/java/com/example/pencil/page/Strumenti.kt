package com.example.pencil.page

import android.content.Context
import androidx.core.content.res.ResourcesCompat
import com.example.pencil.R

class Strumenti(context: Context, strumento: Pennello){
    enum class Pennello {
        PENNA,
        GOMMA,
        EVIDENZIATORE,
        LAZO,
        TESTO
    }

    init {
        when(strumento){
            Pennello.PENNA ->{

            }
            Pennello.GOMMA ->{

            }
            Pennello.EVIDENZIATORE ->{

            }
            Pennello.LAZO ->{

            }
            Pennello.TESTO ->{

            }

        }
    }

    var color = ResourcesCompat.getColor(context.resources, R.color.white, null)
}