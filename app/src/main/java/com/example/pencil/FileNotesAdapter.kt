package com.example.pencil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

class FileNotesAdapter(context: Context, private val dataSet: List<Map<String,String>>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolderDate(view: View) : RecyclerView.ViewHolder(view) {
        val homeR_layout: ConstraintLayout = view.findViewById(R.id.homeR_layout)
        val homeR_title: TextView = view.findViewById(R.id.homeR_title)
    }

    class ViewHolderFile(view: View, listener : OnItemClickListener?) : RecyclerView.ViewHolder(view) {
        val layoutFileNotes: ConstraintLayout = view.findViewById(R.id.layoutFileNotes)
        val imageFileNotes: ImageView = view.findViewById(R.id.imageFileNotes)
        val titoloFileNotes: TextView = view.findViewById(R.id.titoloFileNotes)
        val sottotitoloFileNotes: TextView = view.findViewById(R.id.sottotitoloFileNotes)
        val dataCreazioneFileNotes: TextView = view.findViewById(R.id.dataCreazioneFileNotes)

        init {
            if(listener != null) {
                view.setOnClickListener {
                    listener.onItemClick(adapterPosition)
                }
            }
        }
    }

    class ViewHolderDivider(view: View) : RecyclerView.ViewHolder(view) {
        val r_item_decoration: View = view.findViewById(R.id.r_item_decoration)
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType){
            0 -> {
                // Create a new view, which defines the UI of the list item
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.home_recycler_text, viewGroup, false)

                return ViewHolderDate(view)
            }
            1 -> {
                // Create a new view, which defines the UI of the list item
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.home_recycler_file, viewGroup, false)

                return ViewHolderFile(view, mListener)
            }
            else -> {
                // Create a new view, which defines the UI of the list item
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.recycler_item_decoration, viewGroup, false)

                return ViewHolderDivider(view)
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType){
            0 -> {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                val dataList = dataSet[position]
                val viewHolderDate: ViewHolderDate = holder as ViewHolderDate

                viewHolderDate.homeR_title.text = dataList["textToWrite"]
            }
            1 -> {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                val dataList = dataSet[position]
                val viewHolderFile: ViewHolderFile = holder as ViewHolderFile

                viewHolderFile.titoloFileNotes.text = dataList["titolo"]
                viewHolderFile.sottotitoloFileNotes.text = dataList["sottotitolo"]
                viewHolderFile.dataCreazioneFileNotes.text = dataList["data"]
            }
            2 -> {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                val dataList = dataSet[position]
                val viewHolderDivider: ViewHolderDivider = holder as ViewHolderDivider
            }
        }


    }

    override fun getItemViewType(position: Int): Int {
        when(dataSet[position]["type"]){
            "text" -> return 0
            "file" -> return 1
            "divider" -> return 2
        }
        return 0
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size


    private var mListener : OnItemClickListener? = null

    interface OnItemClickListener{
        fun onItemClick(position : Int)
    }

    fun setOnItemClickListener(listener : OnItemClickListener) {
        mListener = listener
    }

}
