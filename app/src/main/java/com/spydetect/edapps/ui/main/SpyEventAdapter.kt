package com.spydetect.edapps.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.spydetect.edapps.R
import com.spydetect.edapps.data.model.SpyEvent
import com.spydetect.edapps.databinding.ItemSpyEventBinding

class SpyEventAdapter(private val onLongClick: (SpyEvent) -> Unit) :
  ListAdapter<SpyEvent, SpyEventAdapter.ViewHolder>(DiffCallback) {

  class ViewHolder(private val binding: ItemSpyEventBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(event: SpyEvent, onLongClick: (SpyEvent) -> Unit) {
      val context = binding.root.context
      val name = event.deviceName ?: context.getString(R.string.label_unknown_device)
      binding.tvName.text = name
      binding.tvTimestamp.text = event.timestampFormatted
      binding.tvCompany.text = event.companyName
      binding.tvReason.text = event.detectionReason
      binding.chipRssi.text = context.getString(R.string.label_rssi_unit, event.rssi)

      binding.ivDeviceIcon.setImageResource(
        when {
          event.deviceType?.contains("Apple", ignoreCase = true) == true ->
            R.drawable.ic_smart_toy_24
          event.deviceType?.contains("Samsung", ignoreCase = true) == true ->
            R.drawable.ic_smart_toy_24
          event.deviceType?.contains("Tile", ignoreCase = true) == true ->
            R.drawable.ic_smart_toy_24
          else -> R.drawable.ic_smart_toy_24
        }
      )

      if (event.hitCount > 1) {
        binding.tvHitCount.visibility = android.view.View.VISIBLE
        binding.tvHitCount.text = context.getString(R.string.hit_count, event.hitCount)
      } else {
        binding.tvHitCount.visibility = android.view.View.GONE
      }

      if (!event.deviceType.isNullOrBlank()) {
        binding.tvDeviceType.visibility = android.view.View.VISIBLE
        binding.tvDeviceType.text = context.getString(R.string.label_type_prefix, event.deviceType)
      } else {
        binding.tvDeviceType.visibility = android.view.View.GONE
      }

      if (!event.matchedDevices.isNullOrBlank()) {
        binding.tvMatchedDevices.visibility = android.view.View.VISIBLE
        binding.tvMatchedDevices.text =
          context.getString(R.string.label_potential_prefix, event.matchedDevices)
      } else {
        binding.tvMatchedDevices.visibility = android.view.View.GONE
      }

      binding.root.setOnLongClickListener {
        onLongClick(event)
        true
      }
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding = ItemSpyEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position), onLongClick)
  }

  object DiffCallback : DiffUtil.ItemCallback<SpyEvent>() {
    override fun areItemsTheSame(oldItem: SpyEvent, newItem: SpyEvent): Boolean {
      return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SpyEvent, newItem: SpyEvent): Boolean {
      return oldItem == newItem
    }
  }
}
