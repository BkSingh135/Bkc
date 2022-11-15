package com.bkc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class ViewPagerAdapter(private var miniModel: ArrayList<BanModel>, val context : Context) : RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView : AppCompatImageView = itemView.findViewById(R.id.imageView)
       // val textView : AppCompatTextView = itemView.findViewById(R.id.textView)

      //  val constraint : ConstraintLayout = itemView.findViewById(R.id.constraint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       val view : View = LayoutInflater.from(context).inflate(R.layout.recycler_view_layout,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val mini : BanModel = miniModel[position]
      //  holder.textView.text = mini.id
        Picasso.get().load(mini.imageName).into(holder.imageView)


      /*  val intent: Intent = (context as Activity).intent
        val txt = intent.getStringExtra("text")
        holder.textView.text = txt
        val value = intent.getStringExtra("image")
        Picasso.get().load(value).into(holder.imageView)*/



     //   holder.textView.isSelected = true
       // holder.imageView.setImageResource(value)
       /* val value = intent.getIntExtra("image",position)
        holder.imageView.setImageResource(value)*/

    }
    fun getBan(channels: ArrayList<BanModel>) {
        miniModel = channels
        notifyDataSetChanged()
    }

    fun getItem(position: Int): BanModel {
        return miniModel[position]
//        return channelArrayList.get(position%channelArrayList.size());
    }
    override fun getItemCount(): Int {
        return miniModel.size
    }
}