package com.bkc

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class MiniAdapter(private val context : Context, private var miniModel: ArrayList<BanModel>, onClickInterface: onClickInterface) : RecyclerView.Adapter<MiniAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val imageView : AppCompatImageView = itemView.findViewById(R.id.imageView)
        val textView : AppCompatTextView = itemView.findViewById(R.id.textView)
        val constraint : ConstraintLayout = itemView.findViewById(R.id.constraint)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
    {
        val view = LayoutInflater.from(context).inflate(R.layout.mini_rv_layout,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val model = miniModel[position]

        Picasso.get().load(miniModel[position].imageName).into(holder.imageView)
       /// holder.imageView.setImageResource(model.imageName)
        holder.textView.text = model.id

        holder.itemView.setOnClickListener {
         /*   if (context is MainActivity)
            {
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtra("id_test",position)
                context.startActivity(intent)
            }*/
            if (context is MainActivity)
            {
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtra("image",model.imageName)
                intent.putExtra("text", model.id)
                context.startActivity(intent)
            }
            /*if (context is MainActivity) {


                val bundle = Bundle()

                //bundle.putString("key1", "GFG :- Main Activity")
                bundle.putString("text", model.textView)
                bundle.putInt("image", model.image)
                val intent = Intent(context, MainActivity::class.java)
                intent.putExtras(bundle)

                context.startActivity(intent)
            }*/
        }


    }


    fun addAll(channels: ArrayList<BanModel>) {
        miniModel = channels
        notifyDataSetChanged()
    }
    fun getChannel(position: Int): BanModel {
        return miniModel[position]
//        return channelArrayList.get(position%channelArrayList.size());
    }
    override fun getItemCount(): Int {
        return miniModel.size
    }
    interface onClickInterface{
        fun OnClick(position: Int, miniModel: BanModel)
    }
}