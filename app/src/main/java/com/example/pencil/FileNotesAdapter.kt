package com.example.pencil

import android.content.Context
import android.text.TextUtils.split
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

/*import android.content.Context
import android.text.TextUtils.split
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter


class FileNotesAdapter(var context: Context, var fileNotesString: String) : BaseAdapter() {
    var fileNotesList: Array<String> = split(fileNotesString, "\n")
    //private val simple: SimpleDateFormat = SimpleDateFormat("dd/MM", Locale.ITALIAN)

    override fun getCount(): Int {
        return fileNotesList.size
    }

    override fun getItem(position: Int): String {
        return fileNotesList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, v: View?, vg: ViewGroup): View {
        var v: View? = v
        if (v == null) {
            v = LayoutInflater.from(context).inflate(R.layout.`file_notes_item.xml`, null)
        }
        val fileNotesInfo = split(getItem(position), ";")

        var txt: TextView = v!!.findViewById(R.id.titoloFileNotes)
        txt.text = fileNotesInfo[0]
        txt = v.findViewById(R.id.sottotitoloFileNotes)
        txt.text = fileNotesInfo[1]
        txt = v.findViewById(R.id.dataCreazioneFileNotes)
        txt.text = fileNotesInfo[2]
        //txt.setText(simple.format(fileNotesInfo.getDate()))
        return v
    }
}*/

class FileNotesAdapter(context: Context, private val dataSet: List<String>) :
    RecyclerView.Adapter<FileNotesAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutFileNotes: ConstraintLayout
        val imageFileNotes: ImageView
        val titoloFileNotes: TextView
        val sottotitoloFileNotes: TextView
        val dataCreazioneFileNotes: TextView

        init {
            // Define click listener for the ViewHolder's View.
            layoutFileNotes = view.findViewById(R.id.layoutFileNotes)
            imageFileNotes = view.findViewById(R.id.imageFileNotes)
            titoloFileNotes = view.findViewById(R.id.titoloFileNotes)
            sottotitoloFileNotes = view.findViewById(R.id.sottotitoloFileNotes)
            dataCreazioneFileNotes = view.findViewById(R.id.dataCreazioneFileNotes)
        }
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.file_notes_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        var listString = split(dataSet[position], ";").toList()
        viewHolder.titoloFileNotes.text = listString[0]
        viewHolder.sottotitoloFileNotes.text = listString[1]
        viewHolder.dataCreazioneFileNotes.text = listString[2]
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
