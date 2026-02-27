package com.spydetect.edapps.ui.deviceids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.spydetect.edapps.R
import com.spydetect.edapps.data.local.entity.TrackerEntity
import com.spydetect.edapps.data.model.DiscoveredDevice
import com.spydetect.edapps.data.repository.ScannerStatusRepository
import com.spydetect.edapps.data.repository.TrackerRepository
import com.spydetect.edapps.databinding.ActivityDeviceIdManagerBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeviceIdManagerActivity : AppCompatActivity() {

  @Inject lateinit var scannerStatus: ScannerStatusRepository
  @Inject lateinit var trackerRepo: TrackerRepository
  private lateinit var binding: ActivityDeviceIdManagerBinding

  private val savedIdAdapter =
    SavedIdAdapter(
      onEdit = { showEditTrackerDialog(it) },
      onDelete = { showDeleteTrackerConfirmDialog(it) }
    )

  private val discoveredIdAdapter =
    DiscoveredIdAdapter(
      onAdd = { device ->
        lifecycleScope.launch {
          val hexValue = "0x%04X".format(device.id)
          val tracker =
            TrackerEntity(
              hex = hexValue,
              sigName = device.companyName,
              commonName = getString(R.string.default_discovered_name),
              verified = false
            )
          trackerRepo.insertTracker(tracker)
          Toast.makeText(
              this@DeviceIdManagerActivity,
              getString(R.string.id_added, hexValue),
              Toast.LENGTH_SHORT
            )
            .show()
        }
      }
    )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityDeviceIdManagerBinding.inflate(layoutInflater)
    setContentView(binding.root)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setupToolbar()
    setupInsets()
    setupRecyclerViews()
    observeDatabase()
    setupListeners()
  }

  private fun setupToolbar() {
    setSupportActionBar(binding.toolbar)
    binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
  }

  private fun setupInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.deviceIdRoot) { _, insets ->
      val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      binding.appbar.setPadding(bars.left, bars.top, bars.right, 0)
      binding.deviceIdRoot.setPadding(0, 0, 0, bars.bottom)
      insets
    }
  }

  private fun setupRecyclerViews() {
    binding.rvSavedIds.adapter = savedIdAdapter
    binding.rvDiscoveredIds.adapter = discoveredIdAdapter
  }

  private fun observeDatabase() {
    lifecycleScope.launch {
      trackerRepo.allTrackers.collectLatest { trackers ->
        val sorted = trackers.sortedBy { it.displayName ?: "" }
        savedIdAdapter.submitList(sorted)
        binding.tvNoSavedIds.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
      }
    }

    lifecycleScope.launch {
      scannerStatus.discoveredIds.collectLatest { ids ->
        val sorted = ids.sortedByDescending { it.rssi }
        discoveredIdAdapter.submitList(sorted)
        binding.tvNoNearbyIds.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
      }
    }

    lifecycleScope.launch {
      scannerStatus.isScanning.collectLatest { scanning ->
        binding.scanProgress.visibility = if (scanning) View.VISIBLE else View.GONE
      }
    }
  }

  private fun setupListeners() {
    binding.root.findViewById<View>(R.id.btn_clear_all)?.setOnClickListener {
      showClearAllConfirmDialog()
    }
    binding.fabAdd.setOnClickListener { showAddIdDialog() }
  }

  private fun showClearAllConfirmDialog() {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.action_clear_all)
      .setMessage(R.string.confirm_clear_all)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        lifecycleScope.launch {
          trackerRepo.seedIfNeeded()
          Toast.makeText(this@DeviceIdManagerActivity, R.string.reset_success, Toast.LENGTH_SHORT)
            .show()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun showAddIdDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_add_tracker, null)
    val etHex = dialogView.findViewById<TextInputEditText>(R.id.et_hex)
    val etSigName = dialogView.findViewById<TextInputEditText>(R.id.et_sig_name)
    val etCommonName = dialogView.findViewById<TextInputEditText>(R.id.et_common_name)
    val etDevices = dialogView.findViewById<TextInputEditText>(R.id.et_devices)
    val cgType = dialogView.findViewById<ChipGroup>(R.id.cg_type)

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.dialog_add_id_title)
      .setView(dialogView)
      .setPositiveButton(R.string.btn_add) { _, _ ->
        val hexRaw = etHex.text?.toString()?.trim() ?: ""
        val sigName = etSigName.text?.toString()?.trim() ?: ""
        val commonName = etCommonName.text?.toString()?.trim() ?: ""
        val devicesRaw = etDevices.text?.toString()?.trim() ?: ""
        val typeId = cgType.checkedChipId
        val typeName =
          when (typeId) {
            R.id.chip_apple -> "Apple"
            R.id.chip_samsung -> "Samsung"
            R.id.chip_tile -> "Tile"
            else -> "Other"
          }

        if (hexRaw.isNotEmpty()) {
          val devices = devicesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          lifecycleScope.launch {
            val tracker =
              TrackerEntity(
                hex = if (hexRaw.startsWith("0x")) hexRaw else "0x$hexRaw",
                sigName = sigName,
                commonName = commonName,
                devices = devices,
                type = typeName,
                verified = false
              )
            trackerRepo.insertTracker(tracker)
            Toast.makeText(this@DeviceIdManagerActivity, R.string.id_added, Toast.LENGTH_SHORT)
              .show()
          }
        } else {
          Toast.makeText(
              this@DeviceIdManagerActivity,
              R.string.error_invalid_id,
              Toast.LENGTH_SHORT
            )
            .show()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun showEditTrackerDialog(tracker: TrackerEntity) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_add_tracker, null)
    val etHex = dialogView.findViewById<TextInputEditText>(R.id.et_hex)
    val etSigName = dialogView.findViewById<TextInputEditText>(R.id.et_sig_name)
    val etCommonName = dialogView.findViewById<TextInputEditText>(R.id.et_common_name)
    val etDevices = dialogView.findViewById<TextInputEditText>(R.id.et_devices)
    val cgType = dialogView.findViewById<ChipGroup>(R.id.cg_type)

    etHex.setText(tracker.hex)
    etSigName.setText(tracker.sigName)
    etCommonName.setText(tracker.commonName)
    etDevices.setText(tracker.devices.joinToString(", "))
    when (tracker.type) {
      "Apple" -> cgType.check(R.id.chip_apple)
      "Samsung" -> cgType.check(R.id.chip_samsung)
      "Tile" -> cgType.check(R.id.chip_tile)
      else -> cgType.check(R.id.chip_other)
    }

    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.dialog_edit_tracker_title)
      .setView(dialogView)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        val hexRaw = etHex.text?.toString()?.trim() ?: ""
        val sigName = etSigName.text?.toString()?.trim() ?: ""
        val commonName = etCommonName.text?.toString()?.trim() ?: ""
        val devicesRaw = etDevices.text?.toString()?.trim() ?: ""
        val typeId = cgType.checkedChipId
        val typeName =
          when (typeId) {
            R.id.chip_apple -> "Apple"
            R.id.chip_samsung -> "Samsung"
            R.id.chip_tile -> "Tile"
            else -> "Other"
          }
        val devices = devicesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        lifecycleScope.launch {
          val updated =
            tracker.copy(
              hex = hexRaw,
              sigName = sigName,
              commonName = commonName,
              devices = devices,
              type = typeName
            )
          trackerRepo.insertTracker(updated)
          Toast.makeText(this@DeviceIdManagerActivity, R.string.update_success, Toast.LENGTH_SHORT)
            .show()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  private fun showDeleteTrackerConfirmDialog(tracker: TrackerEntity) {
    MaterialAlertDialogBuilder(this)
      .setTitle(R.string.dialog_edit_tracker_title)
      .setMessage(R.string.confirm_delete_tracker)
      .setPositiveButton(android.R.string.ok) { _, _ ->
        lifecycleScope.launch {
          trackerRepo.deleteTracker(tracker)
          Toast.makeText(this@DeviceIdManagerActivity, R.string.delete_success, Toast.LENGTH_SHORT)
            .show()
        }
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
  }

  // --- Adapters ---

  private class SavedIdAdapter(
    private val onEdit: (TrackerEntity) -> Unit,
    private val onDelete: (TrackerEntity) -> Unit
  ) : ListAdapter<TrackerEntity, TrackerViewHolder>(TrackerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackerViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_id, parent, false)
      return TrackerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackerViewHolder, position: Int) {
      val tracker = getItem(position)
      holder.bind(tracker, onEdit, onDelete)
    }
  }

  private class DiscoveredIdAdapter(private val onAdd: (DiscoveredDevice) -> Unit) :
    ListAdapter<DiscoveredDevice, DiscoveredViewHolder>(DiscoveredDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoveredViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_id, parent, false)
      return DiscoveredViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiscoveredViewHolder, position: Int) {
      val device = getItem(position)
      holder.bind(device, onAdd)
    }
  }

  // --- ViewHolders ---

  private class TrackerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val icon: ImageView = view.findViewById(R.id.item_icon)
    private val tvHex: TextView = view.findViewById(R.id.tv_id_hex)
    private val tvSecondary: TextView = view.findViewById(R.id.tv_secondary_info)
    private val btnEdit: MaterialButton = view.findViewById(R.id.btn_edit)
    private val btnAction: MaterialButton = view.findViewById(R.id.btn_action)

    fun bind(
      tracker: TrackerEntity,
      onEdit: (TrackerEntity) -> Unit,
      onDelete: (TrackerEntity) -> Unit
    ) {
      icon.setImageResource(
        when (tracker.type) {
          "Apple" -> R.drawable.ic_settings
          "Samsung" -> R.drawable.ic_settings
          "Tile" -> R.drawable.ic_settings
          else -> R.drawable.ic_settings
        }
      )

      tvHex.text =
        tracker.displayName
          ?: tracker.sigName
          ?: tracker.hex
          ?: itemView.context.getString(R.string.not_available)
      tvSecondary.visibility = View.VISIBLE
      tvSecondary.text = buildString {
        if (tracker.hex != null) append(tracker.hex)
        if (tracker.deviceList.isNotEmpty()) {
          if (isNotEmpty()) append(" | ")
          append(
            itemView.context.getString(
              R.string.device_list_format,
              tracker.deviceList.joinToString(", ")
            )
          )
        }
      }

      btnEdit.visibility = View.VISIBLE
      btnEdit.setOnClickListener { onEdit(tracker) }
      btnAction.setIconResource(R.drawable.ic_delete)
      btnAction.setOnClickListener { onDelete(tracker) }
    }
  }

  private class DiscoveredViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    private val tvHex: TextView = view.findViewById(R.id.tv_id_hex)
    private val tvSecondary: TextView = view.findViewById(R.id.tv_secondary_info)
    private val btnEdit: MaterialButton = view.findViewById(R.id.btn_edit)
    private val btnAction: MaterialButton = view.findViewById(R.id.btn_action)

    fun bind(device: DiscoveredDevice, onAdd: (DiscoveredDevice) -> Unit) {
      val hexValue = "0x%04X".format(device.id)
      tvHex.text = device.companyName ?: itemView.context.getString(R.string.label_unknown_device)
      tvSecondary.visibility = View.VISIBLE
      tvSecondary.text =
        itemView.context.getString(R.string.discovery_secondary_info_v2, hexValue, device.rssi)

      btnEdit.visibility = View.GONE
      btnAction.setIconResource(R.drawable.ic_add)
      btnAction.setOnClickListener { onAdd(device) }
    }
  }

  // --- DiffCallbacks ---

  private class TrackerDiffCallback : DiffUtil.ItemCallback<TrackerEntity>() {
    override fun areItemsTheSame(oldItem: TrackerEntity, newItem: TrackerEntity) =
      oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: TrackerEntity, newItem: TrackerEntity) =
      oldItem == newItem
  }

  private class DiscoveredDiffCallback : DiffUtil.ItemCallback<DiscoveredDevice>() {
    override fun areItemsTheSame(oldItem: DiscoveredDevice, newItem: DiscoveredDevice) =
      oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: DiscoveredDevice, newItem: DiscoveredDevice) =
      oldItem == newItem
  }
}
