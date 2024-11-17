package com.example.subscriberapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val context: Context) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
    private val deviceList:MutableList<Device> = mutableListOf()
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentID: TextView = itemView.findViewById(R.id.tvStudentID)
        val tvMinSpeed: TextView = itemView.findViewById(R.id.tvMinSpeed)
        val tvMaxSpeed: TextView = itemView.findViewById(R.id.tvMaxSpeed)
        val btnViewMore: Button = itemView.findViewById(R.id.btnViewMore)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = deviceList[position]

        holder.tvStudentID.text = device.studentID
        holder.tvMinSpeed.text = context.getString(R.string.min_speed_label, device.minSpeed)
        holder.tvMaxSpeed.text = context.getString(R.string.max_speed_label, device.maxSpeed)

        holder.btnViewMore.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, DeviceReportActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            // Pass the device student id to the new activity
            intent.putExtra("studentID", device.studentID)  // You can pass additional data if needed
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return deviceList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newDeviceList:Collection<Device>){
        deviceList.clear()
        deviceList.addAll(newDeviceList)
        notifyDataSetChanged()
    }
}