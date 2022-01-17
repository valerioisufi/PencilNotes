package com.example.pencil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileFolderAdapter(context: Context, var dataSet: MutableList<MutableMap<String, String>>?) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    private var fileListener : OnFileClickListener? = null
    interface OnFileClickListener{
        fun onItemClick(position : Int)
        fun onMoreInfoClick(position : Int)
    }
    fun setOnFileClickListener(listener : OnFileClickListener) {
        fileListener = listener
    }
    class ViewHolderFile(view: View, listener : OnFileClickListener?) : RecyclerView.ViewHolder(view) {
        val rv_file_anteprima_image: ImageView = view.findViewById(R.id.rv_file_anteprima_image)
        val rv_file_title_text: TextView = view.findViewById(R.id.rv_file_title_text)
        val rv_file_info_image: ImageView = view.findViewById(R.id.rv_file_info_image)

        init {
            if(listener != null) {
                view.setOnClickListener {
                    listener.onItemClick(adapterPosition)
                }
                rv_file_info_image.setOnClickListener{
                    listener.onMoreInfoClick(adapterPosition)
                }
            }

        }
    }


    private var folderListener : OnFolderClickListener? = null
    interface OnFolderClickListener{
        fun onItemClick(position : Int)
        fun onMoreInfoClick(position : Int)
    }
    fun setOnFolderClickListener(listener : OnFolderClickListener) {
        folderListener = listener
    }
    class ViewHolderFolder(view: View, listener : OnFolderClickListener?) : RecyclerView.ViewHolder(view) {
        val rv_folder_title_text: TextView = view.findViewById(R.id.rv_folder_title_text)
        val rv_folder_info_image: ImageView = view.findViewById(R.id.rv_folder_info_image)

        init {
            if(listener != null) {
                view.setOnClickListener {
                    listener.onItemClick(adapterPosition)
                }
                rv_folder_info_image.setOnClickListener{
                    listener.onMoreInfoClick(adapterPosition)
                }
            }

        }
    }


    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType){
            0 -> {
                // Create a new view, which defines the UI of the list item
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.rv_file, viewGroup, false)

                return ViewHolderFile(view, fileListener)
            }
            else -> {
                // Create a new view, which defines the UI of the list item
                val view = LayoutInflater.from(viewGroup.context)
                    .inflate(R.layout.rv_folder, viewGroup, false)

                return ViewHolderFolder(view, folderListener)
            }
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType){
            0 -> {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                val data = dataSet!![position]
                val viewHolderFile: ViewHolderFile = holder as ViewHolderFile

                viewHolderFile.rv_file_title_text.text = data["nome"]
            }
            1 -> {
                // Get element from your dataset at this position and replace the
                // contents of the view with that element
                val data = dataSet!![position]
                val viewHolderFolder: ViewHolderFolder = holder as ViewHolderFolder

                viewHolderFolder.rv_folder_title_text.text = data["nome"]
            }
        }


    }

    override fun getItemViewType(position: Int): Int {
        when(dataSet!![position]["type"]){
            "file" -> return 0
            "folder" -> return 1
        }
        return 1
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int{
        if (dataSet == null) return 0 else return dataSet!!.size
    }

}
