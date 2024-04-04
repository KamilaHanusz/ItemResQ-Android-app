package com.example.itemresq.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.itemresq.R
import com.example.itemresq.activity.SingleReportActivity
import com.example.itemresq.model.ReportModel
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class AdapterClassForRecyclerView(private val reportsList: List<ReportModel>): RecyclerView.Adapter<AdapterClassForRecyclerView.ViewHolderClassForRecyclerView>() {

    class ViewHolderClassForRecyclerView(itemView: View):RecyclerView.ViewHolder(itemView) {
        val rvImageUrl:ImageView = itemView.findViewById(R.id.image)
        val rvTitle:TextView = itemView.findViewById(R.id.title)
        val rvCategory:TextView = itemView.findViewById(R.id.category)
        val rvDate:TextView = itemView.findViewById(R.id.date)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolderClassForRecyclerView {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_layout_for_recycle_view, parent, false)
        return ViewHolderClassForRecyclerView(itemView)
    }

    override fun getItemCount(): Int {
        return reportsList.size
    }

    override fun onBindViewHolder(holder: ViewHolderClassForRecyclerView, position: Int) {
        val report = reportsList[position]

        if (report.imageUrls.isNotEmpty()) {
            val firstImageUrl = report.imageUrls[0]
            Picasso.get().load(firstImageUrl).into(holder.rvImageUrl)
        } else {
            Picasso.get().load(R.drawable.icon_image).placeholder(R.drawable.icon_image).into(holder.rvImageUrl)
        }

        holder.rvTitle.text = report.title
        holder.rvCategory.text = report.category
        val occurrenceDateConverted = report.occurrenceDate?.toDate()
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.rvDate.text = sdf.format(occurrenceDateConverted!!)

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, SingleReportActivity::class.java)
            intent.putExtra("REPORT_ID", report.reportId)
            holder.itemView.context.startActivity(intent)
        }
    }
}