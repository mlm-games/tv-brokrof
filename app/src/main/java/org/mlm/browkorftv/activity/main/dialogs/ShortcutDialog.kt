package org.mlm.browkorftv.activity.main.dialogs

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import org.mlm.browkorftv.R
import org.mlm.browkorftv.singleton.shortcuts.Shortcut
import org.mlm.browkorftv.singleton.shortcuts.ShortcutMgr

/**
 * Created by PDT on 06.08.2017.
 */

class ShortcutDialog(context: Context, private val shortcut: Shortcut) : Dialog(context), KoinComponent {
    private val tvActionTitle: TextView
    private val tvActionKey: TextView
    private val btnSetKey: Button
    private val btnClearKey: Button
    private var keyListenMode = false

    private val shortcutMgr: ShortcutMgr by inject()

    init {
        setCancelable(true)
        setContentView(R.layout.dialog_shortcut)
        setTitle(R.string.shortcut)

        tvActionTitle = findViewById(R.id.tvActionTitle)
        tvActionKey = findViewById(R.id.tvActionKey)
        btnSetKey = findViewById(R.id.btnSetKey)
        btnClearKey = findViewById(R.id.btnClearKey)

        tvActionTitle.setText(shortcut.titleResId)
        updateShortcutNameDisplay()
        btnSetKey.setOnClickListener { toggleKeyListenState() }

        btnClearKey.setOnClickListener { clearKey() }
    }

    private fun clearKey() {
        if (keyListenMode) {
            toggleKeyListenState()
        }
        shortcut.keyCode = 0
        shortcut.modifiers = 0
        shortcut.longPressFlag = false
        shortcutMgr.save(shortcut)
        updateShortcutNameDisplay()
    }

    private fun updateShortcutNameDisplay() {
        tvActionKey.text = if (shortcut.keyCode == 0)
            context.getString(R.string.not_set)
        else {
            Shortcut.shortcutKeysToString(shortcut, context)
        }
    }

    private fun toggleKeyListenState() {
        keyListenMode = !keyListenMode
        btnSetKey.setText(if (keyListenMode) R.string.press_eny_key else R.string.set_key_for_action)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (!keyListenMode) {
            return super.onKeyDown(keyCode, event)
        }
        Log.d(TAG, "onKeyDown: keyCode = $keyCode, event = $event")
        event.startTracking()
        shortcut.longPressFlag = false
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (!keyListenMode) {
            return super.onKeyUp(keyCode, event)
        }
        Log.d(TAG, "onKeyUp: keyCode = $keyCode, event = $event")
        shortcut.keyCode = if (keyCode != 0) keyCode else event.scanCode
        shortcut.modifiers = event.modifiers
        shortcutMgr.save(shortcut)
        toggleKeyListenState()
        updateShortcutNameDisplay()
        return true
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        if (!keyListenMode) {
            return super.onKeyLongPress(keyCode, event)
        }
        Log.d(TAG, "onKeyLongPress: keyCode = $keyCode, event = $event")
        shortcut.keyCode = if (keyCode != 0) keyCode else event.scanCode
        shortcut.modifiers = event.modifiers
        shortcut.longPressFlag = true
        shortcutMgr.save(shortcut)
        toggleKeyListenState()
        updateShortcutNameDisplay()
        return true
    }

    companion object {
        val TAG: String = ShortcutDialog::class.java.simpleName
    }
}
