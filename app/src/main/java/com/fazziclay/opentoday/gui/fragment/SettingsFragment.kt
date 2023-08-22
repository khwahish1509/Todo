package com.fazziclay.opentoday.gui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.fazziclay.opentoday.R
import com.fazziclay.opentoday.app.App
import com.fazziclay.opentoday.app.ColorHistoryManager
import com.fazziclay.opentoday.app.CrashReportContext
import com.fazziclay.opentoday.app.ImportWrapper
import com.fazziclay.opentoday.app.PinCodeManager
import com.fazziclay.opentoday.app.PinCodeManager.PinCodeNotValidateException
import com.fazziclay.opentoday.app.PinCodeManager.ValidationException
import com.fazziclay.opentoday.app.SettingsManager
import com.fazziclay.opentoday.app.SettingsManager.FirstTab
import com.fazziclay.opentoday.app.items.QuickNoteReceiver
import com.fazziclay.opentoday.app.items.item.ItemType
import com.fazziclay.opentoday.app.items.item.ItemsRegistry
import com.fazziclay.opentoday.databinding.ExportBinding
import com.fazziclay.opentoday.databinding.FragmentSettingsBinding
import com.fazziclay.opentoday.gui.ActivitySettings
import com.fazziclay.opentoday.gui.EnumsRegistry
import com.fazziclay.opentoday.gui.UI
import com.fazziclay.opentoday.gui.dialog.DialogSelectItemType
import com.fazziclay.opentoday.util.EnumUtil
import com.fazziclay.opentoday.util.InlineUtil.viewClick
import com.fazziclay.opentoday.util.Logger
import com.fazziclay.opentoday.util.SimpleSpinnerAdapter
import org.json.JSONException
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class SettingsFragment : Fragment() {
    companion object {
        private const val TAG = "SettingsFragment"

        @JvmStatic
        fun create(): SettingsFragment {
            return SettingsFragment()
        }
    }

    private lateinit var binding: FragmentSettingsBinding
    private lateinit var app: App
    private lateinit var settingsManager: SettingsManager
    private lateinit var colorHistoryManager: ColorHistoryManager
    private lateinit var pinCodeManager: PinCodeManager
    private var pinCodeCallback = Runnable {}
    private var easterEggLastClick: Long = 0
    private var easterEggCounter = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReportContext.FRONT.push("SettingsFragment")
        Logger.d(TAG, "onCreate")
        app = App.get(requireContext())
        settingsManager = app.settingsManager
        colorHistoryManager = app.colorHistoryManager
        pinCodeManager = app.pinCodeManager
        UI.getUIRoot(this).pushActivitySettings { a ->
            a.isNotificationsVisible = false
            a.isClockVisible = false
            a.toolbarSettings = ActivitySettings.ToolbarSettings.createBack(R.string.settings_title) { UI.rootBack(this@SettingsFragment) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CrashReportContext.FRONT.pop()
        UI.getUIRoot(this).popActivitySettings()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSettingsBinding.inflate(inflater)
        setupView()
        return binding.root
    }

    private fun setupView() {
        setupThemeSpinner()
        setupFirstDayOfWeekSpinner()
        setupFirstTabSpinner()

        viewClick(binding.dateAndTimeFormat, Runnable {
            val preview = TextView(requireContext())
            val spinner = Spinner(requireContext())
            val adapter = SimpleSpinnerAdapter<SettingsManager.DateAndTimePreset>(requireContext())
            spinner.adapter = adapter
            for (value in SettingsManager.DateAndTimePreset.values()) {
                adapter.add(value.name, value)
            }
            spinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view123: View?, position: Int, id: Long) {
                    val t = adapter.getItem(position)
                    settingsManager.applyDateAndTimePreset(t)
                    settingsManager.save()

                    val current = GregorianCalendar().time
                    val dateFormat = SimpleDateFormat(settingsManager.datePattern, Locale.getDefault())
                    val timeFormat = SimpleDateFormat(settingsManager.timePattern, Locale.getDefault())

                    val previewText = dateFormat.format(current) + "  " + timeFormat.format(current)

                    preview.text = previewText
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val view = LinearLayout(requireContext())
            view.orientation = LinearLayout.VERTICAL
            view.addView(preview)
            view.addView(spinner)

            AlertDialog.Builder(requireContext())
                .setView(view)
                .show()
        })

        // Debug
        viewClick(binding.themeTitle, Runnable { experimentalFeaturesInteract() })

        // QuickNote
        binding.quickNoteCheckbox.isChecked = settingsManager.isQuickNoteNotification
        viewClick(binding.quickNoteCheckbox, Runnable {
            settingsManager.isQuickNoteNotification = binding.quickNoteCheckbox.isChecked
            if (settingsManager.isQuickNoteNotification) {
                QuickNoteReceiver.sendQuickNoteNotification(requireContext())
            } else {
                QuickNoteReceiver.cancelQuickNoteNotification(requireContext())
            }
            settingsManager.save()
        })

        // Parse time from quick note
        binding.parseTimeFromQuickNote.isChecked = settingsManager.isParseTimeFromQuickNote
        viewClick(binding.parseTimeFromQuickNote, Runnable {
            settingsManager.isParseTimeFromQuickNote = binding.parseTimeFromQuickNote.isChecked
            settingsManager.save()
        })

        // Minimize gray color
        binding.minimizeGrayColor.isChecked = settingsManager.isMinimizeGrayColor
        viewClick(binding.minimizeGrayColor, Runnable {
            settingsManager.isMinimizeGrayColor = binding.minimizeGrayColor.isChecked
            settingsManager.save()
        })

        // Trim item names in Editor
        binding.trimItemNamesOnEdit.isChecked = settingsManager.isTrimItemNamesOnEdit
        viewClick(binding.trimItemNamesOnEdit, Runnable {
            settingsManager.isTrimItemNamesOnEdit = binding.trimItemNamesOnEdit.isChecked
            settingsManager.save()
        })

        // Lock color history
        binding.colorHistoryLocked.isChecked = colorHistoryManager.isLocked
        viewClick(binding.colorHistoryLocked, Runnable {
            colorHistoryManager.isLocked = binding.colorHistoryLocked.isChecked
        })

        // Export
        viewClick(binding.export, Runnable { showExportDialog(requireActivity(), settingsManager, colorHistoryManager) })

        // Is telemetry
        binding.isTelemetry.isChecked = settingsManager.isTelemetry
        viewClick(binding.isTelemetry, Runnable {
            val isTelemetry = binding.isTelemetry.isChecked
            settingsManager.isTelemetry = isTelemetry
            settingsManager.save()
            app.telemetry.setEnabled(isTelemetry)
            if (isTelemetry) AlertDialog.Builder(requireContext())
                .setTitle(R.string.setup_telemetry)
                .setMessage(R.string.setup_telemetry_details)
                .setPositiveButton(R.string.abc_ok, null)
                .show()
        })
        binding.defaultQuickNoteType.text = getString(R.string.settings_defaultQuickNoteType, getString(EnumsRegistry.nameResId(settingsManager.defaultQuickNoteType.itemType)))
        viewClick(binding.defaultQuickNoteType, Runnable {
            DialogSelectItemType(context) { type: ItemType ->
                settingsManager.defaultQuickNoteType = ItemsRegistry.REGISTRY.get(type)
                binding.defaultQuickNoteType.text = getString(R.string.settings_defaultQuickNoteType, getString(EnumsRegistry.nameResId(settingsManager.defaultQuickNoteType.itemType)))
                settingsManager.save()
            }.show()
        })
        pinCodeCallback = Runnable { binding.pincode.text = getString(R.string.settings_pincode, if (pinCodeManager.isPinCodeSet) getString(R.string.settings_pincode_on) else getString(R.string.settings_pincode_off)) }
        pinCodeCallback.run()
        viewClick(binding.pincode, Runnable { showPinCodeDialog() })

        // add item to top
        binding.addItemsToTop.isChecked = settingsManager.itemAddPosition == SettingsManager.ItemAddPosition.TOP
        viewClick(binding.addItemsToTop, Runnable {
            settingsManager.itemAddPosition = if (binding.addItemsToTop.isChecked) SettingsManager.ItemAddPosition.TOP else SettingsManager.ItemAddPosition.BOTTOM
            settingsManager.save()
        })

        // confirm easy-to-make changes
        binding.confirmFastChanges.isChecked = settingsManager.isConfirmFastChanges
        viewClick(binding.confirmFastChanges, Runnable {
            settingsManager.isConfirmFastChanges = binding.confirmFastChanges.isChecked
            settingsManager.save()
        })

        binding.autoCloseToolbar.isChecked = settingsManager.isAutoCloseToolbar
        viewClick(binding.autoCloseToolbar, Runnable {
            settingsManager.isAutoCloseToolbar = binding.autoCloseToolbar.isChecked
            settingsManager.save()
        })

        binding.scrollToAddedItem.isChecked = settingsManager.isScrollToAddedItem
        viewClick(binding.scrollToAddedItem, Runnable {
            settingsManager.isScrollToAddedItem = binding.scrollToAddedItem.isChecked
            settingsManager.save()
        })

        binding.itemInternalBackgroundFromItem.isChecked = settingsManager.isItemEditorBackgroundFromItem
        viewClick(binding.itemInternalBackgroundFromItem, Runnable {
            settingsManager.isItemEditorBackgroundFromItem = binding.itemInternalBackgroundFromItem.isChecked
            settingsManager.save()
        })
    }

    private fun setupFirstTabSpinner() {
        val adapter = SimpleSpinnerAdapter<FirstTab>(requireContext())
        EnumUtil.addToSimpleSpinnerAdapter(requireContext(), adapter, FirstTab.values())
        binding.firstTab.adapter = adapter
        binding.firstTab.setSelection(adapter.getValuePosition(settingsManager.firstTab))
        binding.firstTab.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val t = adapter.getItem(position)
                settingsManager.firstTab = t
                settingsManager.save()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupThemeSpinner() {
        val adapter = SimpleSpinnerAdapter<Int>(requireContext())
                .add(requireContext().getString(R.string.settings_theme_system), AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                .add(requireContext().getString(R.string.settings_theme_light), AppCompatDelegate.MODE_NIGHT_NO)
                .add(requireContext().getString(R.string.settings_theme_night), AppCompatDelegate.MODE_NIGHT_YES)
        binding.themeSpinner.adapter = adapter
        binding.themeSpinner.setSelection(adapter.getValuePosition(settingsManager.theme))
        binding.themeSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val theme = adapter.getItem(position)
                UI.setTheme(theme)
                settingsManager.theme = theme
                settingsManager.save()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupFirstDayOfWeekSpinner() {
        val weekdays = DateFormatSymbols.getInstance(Locale.getDefault()).weekdays
        val adapter = SimpleSpinnerAdapter<Int>(requireContext())
                .add(weekdays[Calendar.SUNDAY], Calendar.SUNDAY)
                .add(weekdays[Calendar.MONDAY], Calendar.MONDAY)
        binding.firstDayOfWeekSpinner.adapter = adapter
        binding.firstDayOfWeekSpinner.setSelection(adapter.getValuePosition(settingsManager.firstDayOfWeek))
        binding.firstDayOfWeekSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val day = adapter.getItem(position)
                settingsManager.firstDayOfWeek = day
                settingsManager.save()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showPinCodeDialog() {
        val isPinSet = pinCodeManager.isPinCodeSet
        val d = AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_pincode_title)
                .setMessage(if (isPinSet) getString(R.string.settings_pincode_message_on, pinCodeManager.pinCode) else getString(R.string.settings_pincode_message_off))
                .setNeutralButton(R.string.settings_pincode_cancel, null)
                .setPositiveButton(if (isPinSet) R.string.settings_pincode_button_disable else R.string.settings_pincode_button_enable) { _: DialogInterface?, _: Int ->
                    if (isPinSet) {
                        pinCodeManager.disablePinCode()
                        pinCodeCallback.run()
                        Toast.makeText(app, R.string.settings_pincode_disable_success, Toast.LENGTH_SHORT).show()
                    } else {
                        val t = EditText(requireContext())
                        t.inputType = InputType.TYPE_CLASS_NUMBER
                        t.setHint(R.string.settings_pincode_enable_hint)
                        t.filters = arrayOf(InputFilter.LengthFilter(PinCodeManager.MAX_LENGTH))
                        AlertDialog.Builder(requireContext())
                                .setTitle(R.string.settings_pincode_enable_title)
                                .setMessage(R.string.settings_pincode_enable_message)
                                .setView(t)
                                .setPositiveButton(R.string.settings_pincode_enable_apply) EnterNewPinCodeDialog@ { _: DialogInterface?, _: Int ->
                                    try {
                                        pinCodeManager.enablePinCode(t.text.toString())
                                        pinCodeCallback.run()
                                        Toast.makeText(app, R.string.settings_pincode_enable_success, Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        if (e is PinCodeNotValidateException) {
                                            if (e.validationException == ValidationException.CONTAINS_NON_DIGITS_CHARS) {
                                                Toast.makeText(app, R.string.settings_pincode_enable_nonDigitsError, Toast.LENGTH_SHORT).show()
                                                return@EnterNewPinCodeDialog
                                            } else if (e.validationException == ValidationException.EMPTY) {
                                                Toast.makeText(app, R.string.settings_pincode_enable_emptyError, Toast.LENGTH_SHORT).show()
                                                return@EnterNewPinCodeDialog
                                            } else if (e.validationException == ValidationException.TOO_LONG) {
                                                Toast.makeText(app, R.string.settings_pincode_enable_tooLongError, Toast.LENGTH_SHORT).show()
                                                return@EnterNewPinCodeDialog
                                            }
                                        }
                                        Toast.makeText(app, R.string.settings_pincode_enable_unknownError, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.settings_pincode_enable_cancel, null)
                                .show()
                    }
                }
        d.show()
    }

    private fun showExportDialog(context: Activity, settingsManager: SettingsManager?, colorHistoryManager: ColorHistoryManager?) {
        val binding = ExportBinding.inflate(context.layoutInflater)
        AlertDialog.Builder(context)
                .setView(binding.root)
                .setTitle(R.string.settings_export_dialog_title)
                .setNegativeButton(R.string.abc_cancel, null)
                .setPositiveButton(R.string.settings_export_dialog_export) { _: DialogInterface?, _: Int ->
                    val tabsManger = App.get(context).tabsManager
                    val perms: MutableList<ImportWrapper.Permission> = ArrayList()
                    val isAllItems = binding.exportAllItems.isChecked
                    val isSettings = binding.exportSettings.isChecked
                    val isColorHistory = binding.exportColorHistory.isChecked
                    val dialogMessage = binding.exportDialogMessage.text.toString().trim { it <= ' ' }
                    val isDialogMessage = dialogMessage.isNotEmpty()
                    if (isAllItems) perms.add(ImportWrapper.Permission.ADD_TABS)
                    if (isSettings) perms.add(ImportWrapper.Permission.OVERWRITE_SETTINGS)
                    if (isColorHistory) perms.add(ImportWrapper.Permission.OVERWRITE_COLOR_HISTORY)
                    if (isDialogMessage) perms.add(ImportWrapper.Permission.PRE_IMPORT_SHOW_DIALOG)
                    val i = ImportWrapper.createImport(*perms.toTypedArray())
                    if (isDialogMessage) i.setDialogMessage(dialogMessage)
                    if (isAllItems) i.addTabAll(*tabsManger.allTabs)
                    if (isSettings) {
                        try {
                            i.setSettings(settingsManager!!.exportJSONSettings())
                        } catch (e: JSONException) {
                            Toast.makeText(context, context.getString(R.string.export_error, e.toString()), Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    if (isColorHistory) {
                        try {
                            i.setColorHistory(colorHistoryManager!!.exportJSONColorHistory())
                        } catch (e: JSONException) {
                            Toast.makeText(context, context.getString(R.string.export_error, e.toString()), Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }
                    }
                    try {
                        val s = i.build().finalExport()
                        val clipboardManager = context.getSystemService(ClipboardManager::class.java)
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.export_clipdata_label), s))
                        Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.export_error, e.toString()), Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
    }

    private fun experimentalFeaturesInteract() {
        if (System.currentTimeMillis() - easterEggLastClick < 1000) {
            easterEggCounter++
            if (easterEggCounter >= 6) {
                easterEggCounter = 0
                UI.Debug.showFeatureFlagsDialog(app, requireContext())
            }
        } else {
            easterEggCounter = 0
        }
        easterEggLastClick = System.currentTimeMillis()
    }
}