package com.phlox.tvwebbrowser.activity.main.dialogs.settings

import android.app.AlertDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.phlox.tvwebbrowser.AppContext
import com.phlox.tvwebbrowser.R
import com.phlox.tvwebbrowser.TVBro
import com.phlox.tvwebbrowser.activity.main.AdblockModel
import com.phlox.tvwebbrowser.activity.main.MainActivity
import com.phlox.tvwebbrowser.activity.main.SettingsModel
import com.phlox.tvwebbrowser.databinding.ViewSettingsMainBinding
import com.phlox.tvwebbrowser.settings.AppSettings
import com.phlox.tvwebbrowser.settings.HomePageMode
import com.phlox.tvwebbrowser.settings.HomePageLinksMode
import com.phlox.tvwebbrowser.settings.Theme
import com.phlox.tvwebbrowser.settings.canRecommendGeckoView
import com.phlox.tvwebbrowser.utils.activemodel.ActiveModelsRepository
import com.phlox.tvwebbrowser.utils.activity
import com.phlox.tvwebbrowser.webengine.WebEngineFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.view.isGone

class MainSettingsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    private var vb = ViewSettingsMainBinding.inflate(LayoutInflater.from(getContext()), this, true)
    var settingsModel = ActiveModelsRepository.get(SettingsModel::class, activity!!)
    var adblockModel = ActiveModelsRepository.get(AdblockModel::class, activity!!)

    private val settingsManager = AppContext.provideSettingsManager()
    private val settings: AppSettings get() = AppContext.settings

    init {
        initWebBrowserEngineSettingsUI()
        initHomePageAndSearchEngineConfigUI()
        initUAStringConfigUI(context)
        initAdBlockConfigUI()
        initThemeSettingsUI()
        initKeepScreenOnUI()

        vb.btnClearWebCache.setOnClickListener {
            (activity as MainActivity).lifecycleScope.launch {
                WebEngineFactory.clearCache(context)
                Toast.makeText(context, android.R.string.ok, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initWebBrowserEngineSettingsUI() {
        if (WebEngineFactory.getProviders().size == 1) {
            vb.llWebEngine.visibility = GONE
            return
        }

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            AppSettings.SupportedWebEngines
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        vb.spWebEngine.adapter = adapter

        val currentEngineIndex = AppSettings.SupportedWebEngines.indexOf(settings.webEngine)
        vb.spWebEngine.setSelection(currentEngineIndex.coerceAtLeast(0), false)

        vb.spWebEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (settings.webEngine == AppSettings.SupportedWebEngines[position]) return

                if (AppSettings.SupportedWebEngines[position] == AppSettings.ENGINE_GECKO_VIEW && !canRecommendGeckoView()) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.settings_engine_change_gecko_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            updateWebEngine(position)
                            showRestartDialog()
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            val idx = AppSettings.SupportedWebEngines.indexOf(settings.webEngine)
                            vb.spWebEngine.setSelection(idx.coerceAtLeast(0), false)
                        }
                        .show()
                    return
                } else if (AppSettings.SupportedWebEngines[position] == AppSettings.ENGINE_WEB_VIEW) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.settings_engine_change_webview_msg)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            updateWebEngine(position)
                            showRestartDialog()
                        }
                        .setNegativeButton(R.string.cancel) { _, _ ->
                            val idx = AppSettings.SupportedWebEngines.indexOf(settings.webEngine)
                            vb.spWebEngine.setSelection(idx.coerceAtLeast(0), false)
                        }
                        .show()
                    return
                }

                updateWebEngine(position)
                showRestartDialog()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateWebEngine(position: Int) {
        (activity as? FragmentActivity)?.lifecycleScope?.launch {
            settingsManager.setWebEngine(position)
        }
    }

    private fun showRestartDialog() {
        AlertDialog.Builder(context)
            .setTitle(R.string.need_restart)
            .setMessage(R.string.need_restart_message)
            .setPositiveButton(R.string.exit) { _, _ ->
                TVBro.instance.needToExitProcessAfterMainActivityFinish = true
                TVBro.instance.needRestartMainActivityAfterExitingProcess = true
                activity!!.finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun initThemeSettingsUI() {
        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.themes)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        vb.spTheme.adapter = adapter
        vb.spTheme.setSelection(settings.theme, false)

        vb.spTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (settings.theme == position) return

                (activity as? FragmentActivity)?.lifecycleScope?.launch {
                    settingsManager.setTheme(Theme.entries[position])
                }
                Toast.makeText(context, context.getString(R.string.need_restart), Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun initKeepScreenOnUI() {
        vb.scKeepScreenOn.isChecked = settings.keepScreenOn

        vb.scKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            (activity as? FragmentActivity)?.lifecycleScope?.launch {
                settingsManager.setKeepScreenOn(isChecked)
            }
        }
    }

    private fun initAdBlockConfigUI() {
        vb.scAdblock.isChecked = settings.adBlockEnabled

        vb.llAdblock.setOnClickListener {
            vb.scAdblock.isChecked = !vb.scAdblock.isChecked
            (activity as? FragmentActivity)?.lifecycleScope?.launch {
                settingsManager.setAdBlockEnabled(vb.scAdblock.isChecked)
            }
            vb.llAdBlockerDetails.visibility = if (vb.scAdblock.isChecked) VISIBLE else GONE
        }

        vb.llAdBlockerDetails.visibility = if (settings.adBlockEnabled) VISIBLE else GONE

        adblockModel.clientLoading.subscribe(activity as FragmentActivity) {
            updateAdBlockInfo()
        }

        vb.btnAdBlockerUpdate.setOnClickListener {
            if (adblockModel.clientLoading.value) return@setOnClickListener
            adblockModel.loadAdBlockList(true)
            it.isEnabled = false
        }

        updateAdBlockInfo()
    }

    private fun updateAdBlockInfo() {
        val dateFormat = SimpleDateFormat("hh:mm dd MMMM yyyy", Locale.getDefault())
        val lastUpdate = if (settings.adBlockListLastUpdate == 0L)
            context.getString(R.string.never)
        else
            dateFormat.format(Date(settings.adBlockListLastUpdate))

        val infoText = "URL: ${settings.adBlockListURL}\n${context.getString(R.string.last_update)}: $lastUpdate"
        vb.tvAdBlockerListInfo.text = infoText

        val loadingAdBlockList = adblockModel.clientLoading.value
        vb.btnAdBlockerUpdate.visibility = if (loadingAdBlockList) GONE else VISIBLE
        vb.pbAdBlockerListLoading.visibility = if (loadingAdBlockList) VISIBLE else GONE
    }

    private fun initUAStringConfigUI(context: Context) {
        val currentUA = settings.effectiveUserAgent

        // Check for legacy UA string
        if (currentUA?.contains("TV Bro/1.0 ") == true) {
            // Clear legacy UA - will be handled in save()
        }

        val selected = if (currentUA.isNullOrEmpty()) {
            0 // Default
        } else {
            val idx = AppSettings.UserAgentStrings.indexOf(currentUA)
            if (idx != -1) idx else AppSettings.UserAgentStrings.size - 1 // Custom
        }

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            settingsModel.userAgentStringTitles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vb.spTitles.adapter = adapter

        if (selected != -1 && selected < AppSettings.UserAgentStrings.size - 1) {
            vb.spTitles.setSelection(selected, false)
            vb.etUAString.setText(AppSettings.UserAgentStrings[selected])
        } else {
            vb.spTitles.setSelection(settingsModel.userAgentStringTitles.size - 1, false)
            vb.llUAString.visibility = VISIBLE
            vb.etUAString.setText(currentUA ?: "")
            vb.etUAString.requestFocus()
        }

        vb.spTitles.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == settingsModel.userAgentStringTitles.size - 1 && vb.llUAString.isGone) {
                    vb.llUAString.visibility = VISIBLE
                    vb.llUAString.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
                    vb.etUAString.requestFocus()
                }
                if (position < AppSettings.UserAgentStrings.size) {
                    vb.etUAString.setText(AppSettings.UserAgentStrings[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun initHomePageAndSearchEngineConfigUI() {
        val currentSearchUrl = settings.searchEngineURL
        var selected = AppSettings.SearchEnginesURLs.indexOf(currentSearchUrl)
        if (selected == -1) {
            selected = AppSettings.SearchEnginesURLs.size - 1 // Custom
        }

        val adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            AppSettings.SearchEnginesTitles
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        vb.spEngine.adapter = adapter

        if (selected != -1 && selected < AppSettings.SearchEnginesURLs.size - 1) {
            vb.spEngine.setSelection(selected)
            vb.etUrl.setText(AppSettings.SearchEnginesURLs[selected])
        } else {
            vb.spEngine.setSelection(AppSettings.SearchEnginesTitles.size - 1)
            vb.llURL.visibility = VISIBLE
            vb.etUrl.setText(currentSearchUrl)
            vb.etUrl.requestFocus()
        }

        vb.spEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == (AppSettings.SearchEnginesTitles.size - 1)) {
                    if (vb.llURL.isGone) {
                        vb.llURL.visibility = VISIBLE
                        vb.llURL.startAnimation(
                            AnimationUtils.loadAnimation(context, android.R.anim.fade_in)
                        )
                    }
                    vb.etUrl.setText(settings.searchEngineURL)
                    vb.etUrl.requestFocus()
                    return
                } else {
                    vb.llURL.visibility = GONE
                    if (position < AppSettings.SearchEnginesURLs.size) {
                        vb.etUrl.setText(AppSettings.SearchEnginesURLs[position])
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val homePageSpinnerAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.home_page_modes)
        )
        homePageSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vb.spHomePage.adapter = homePageSpinnerAdapter
        vb.spHomePage.setSelection(settings.homePageMode)

        vb.spHomePage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val homePageMode = HomePageMode.entries[position]
                vb.llCustomHomePage.visibility = if (homePageMode == HomePageMode.CUSTOM) VISIBLE else GONE
                vb.llHomePageLinksMode.visibility = if (homePageMode == HomePageMode.HOME_PAGE) VISIBLE else GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        val homePageLinksSpinnerAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            context.resources.getStringArray(R.array.home_page_links_modes)
        )
        homePageLinksSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        vb.spHomePageLinks.adapter = homePageLinksSpinnerAdapter
        vb.spHomePageLinks.setSelection(settings.homePageLinksMode)

        vb.etCustomHomePageUrl.setText(settings.homePage)
    }

    fun save() {
        val customSearchEngineUrl = vb.etUrl.text.toString()
        settingsModel.setSearchEngineURL(customSearchEngineUrl)

        val homePageMode = HomePageMode.entries[vb.spHomePage.selectedItemPosition]
        val customHomePageURL = vb.etCustomHomePageUrl.text.toString()
        val homePageLinksMode = HomePageLinksMode.entries[vb.spHomePageLinks.selectedItemPosition]
        settingsModel.setHomePageProperties(homePageMode, customHomePageURL, homePageLinksMode)

        val userAgent = vb.etUAString.text.toString().trim(' ')
        val userAgentIndex = vb.spTitles.selectedItemPosition

        (activity as? FragmentActivity)?.lifecycleScope?.launch {
            settingsManager.update { settings ->
                settings.copy(
                    userAgentIndex = userAgentIndex,
                    userAgentCustom = if (userAgentIndex == AppSettings.UserAgentStrings.size - 1) {
                        userAgent.ifEmpty { null }
                    } else null
                )
            }
        }
    }
}