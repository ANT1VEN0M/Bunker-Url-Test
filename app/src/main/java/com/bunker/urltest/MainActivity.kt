package com.bunker.urltest

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

class MainActivity : AppCompatActivity() {

    data class LaunchableApp(val packageName: String, val label: String)
    data class PresetService(val name: String, val url: String)

    private lateinit var summary: TextView
    private lateinit var results: LinearLayout
    private lateinit var appsResults: LinearLayout
    private lateinit var checkButton: Button

    private var monitoring = true
    private var monitoringJob: Job? = null
    private val rows = mutableMapOf<String, TextView>()
    private val appStatusViews = mutableMapOf<String, TextView>()

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            summary.visibility = View.VISIBLE
            summary.text = "Разрешение на уведомления не выдано. Мониторинг в фоне может быть ограничен."
        }
    }

    private val popularServices = listOf(
        PresetService("YouTube", "https://www.youtube.com/"),
        PresetService("Telegram", "https://telegram.org/"),
        PresetService("Instagram", "https://www.instagram.com/"),
        PresetService("Discord", "https://discord.com/"),
        PresetService("ChatGPT", "https://chatgpt.com/"),
        PresetService("Steam", "https://store.steampowered.com/"),
        PresetService("TikTok", "https://www.tiktok.com/"),
        PresetService("Spotify", "https://www.spotify.com/"),
        PresetService("X / Twitter", "https://x.com/"),
        PresetService("WhatsApp", "https://www.whatsapp.com/"),
        PresetService("Facebook", "https://www.facebook.com/"),
        PresetService("Reddit", "https://www.reddit.com/"),
        PresetService("Netflix", "https://www.netflix.com/"),
        PresetService("Twitch", "https://www.twitch.tv/"),
        PresetService("VK", "https://vk.com/"),
        PresetService("Google", "https://www.google.com/"),
        PresetService("Yandex", "https://yandex.ru/")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.mainContentContainer)
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val sidePadding = resources.getDimensionPixelSize(R.dimen.bunker_screen_padding)
            val basePaddingBottom = resources.getDimensionPixelSize(R.dimen.bunker_space_xxl)
            view.setPadding(
                sidePadding + bars.left,
                bars.top + 16,
                sidePadding + bars.right,
                basePaddingBottom + bars.bottom
            )
            insets
        }

        summary = findViewById(R.id.summary)
        results = findViewById(R.id.results)
        appsResults = findViewById(R.id.appsResults)
        checkButton = findViewById(R.id.checkButton)

        findViewById<Button>(R.id.appsButton).setOnClickListener {
            showInstalledAppsPicker()
        }
        findViewById<Button>(R.id.customButton).setOnClickListener {
            showAddServiceMenu()
        }
        findViewById<Button>(R.id.restoreButton).setOnClickListener {
            confirmRestoreBuiltIns()
        }
        findViewById<Button>(R.id.auditButton).setOnClickListener {
            auditConfiguredAddresses()
        }
        checkButton.setOnClickListener {
            toggleMonitoring()
        }

        renderSelectedApps()
        startMonitoring()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        UrlTestMonitorService.start(this)
    }

    private fun installedLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = packageManager.queryIntentActivities(intent, 0)
        val apps = mutableListOf<LaunchableApp>()
        val seen = HashSet<String>()

        for (info in activities) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (!seen.add(pkg)) continue

            val label = try {
                info.loadLabel(packageManager).toString()
            } catch (_: Throwable) {
                info.activityInfo?.nonLocalizedLabel?.toString() ?: pkg
            }
            apps.add(LaunchableApp(pkg, label))
        }

        return apps.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    private fun showInstalledAppsPicker() {
        val apps = installedLaunchableApps()
        if (apps.isEmpty()) {
            summary.visibility = View.VISIBLE
            summary.text = "Не удалось получить список приложений"
            return
        }

        val prefs = getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
        val selected = prefs.getStringSet("packages", emptySet())?.toMutableSet() ?: mutableSetOf()

        val dialogView = layoutInflater.inflate(R.layout.dialog_app_picker, null)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.appsRecyclerView)
        val searchInput: EditText = dialogView.findViewById(R.id.searchEditText)
        val btnSelectAll: TextView = dialogView.findViewById(R.id.btnSelectAll)
        val btnClearAll: TextView = dialogView.findViewById(R.id.btnClearAll)
        val btnCancel: Button = dialogView.findViewById(R.id.btnDialogCancel)
        val btnSave: Button = dialogView.findViewById(R.id.btnDialogSave)

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = AppPickerAdapter(apps, selected)
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSelectAll.setOnClickListener { adapter.selectAll() }
        btnClearAll.setOnClickListener { adapter.clearAll() }
        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            prefs.edit().putStringSet("packages", selected).apply()
            renderSelectedApps()
            lifecycleScope.launch {
                checkSelectedApps()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun renderSelectedApps() {
        appsResults.removeAllViews()
        appStatusViews.clear()

        val prefs = getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
        val selected = prefs.getStringSet("packages", emptySet()) ?: emptySet()
        val installed = installedLaunchableApps().associateBy { it.packageName }

        if (selected.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Приложения не выбраны"
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text_muted))
                setPadding(12, 12, 12, 12)
            }
            appsResults.addView(emptyTv)
            return
        }

        for (pkg in selected.sorted()) {
            val app = installed[pkg]
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(14, 12, 8, 12)
                setBackgroundResource(R.drawable.bunker_row)
            }

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            }

            val titleTv = TextView(this).apply {
                text = app?.label ?: pkg
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text))
            }
            val statusTv = TextView(this).apply {
                text = if (app == null) "ПРИЛОЖЕНИЕ НЕ НАЙДЕНО" else "ОЖИДАЕТ ПРОВЕРКИ СЕТИ"
                textSize = 12f
                setPadding(0, 5, 0, 0)
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text_secondary))
            }

            textContainer.addView(titleTv)
            textContainer.addView(statusTv)
            row.addView(textContainer)

            val deleteBtn = createDeleteButton {
                val updated = selected.toMutableSet().apply { remove(pkg) }
                prefs.edit().putStringSet("packages", updated).apply()
                renderSelectedApps()
                startMonitoring()
            }
            row.addView(deleteBtn)

            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 8
            }
            appsResults.addView(row, params)
            appStatusViews[pkg] = statusTv
        }
    }

    private fun createDeleteButton(onDelete: () -> Unit): TextView {
        return TextView(this).apply {
            text = "🗑"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_red))
            setPadding(12, 8, 12, 8)
            minWidth = 44
            minHeight = 44
            contentDescription = "Удалить из списка"
            setOnClickListener { onDelete() }
        }
    }

    private fun syncRows(targets: List<Triple<String, String, String>>) {
        val currentKeys = rows.keys.toList()
        val newKeys = targets.map { it.first }
        if (currentKeys == newKeys && results.childCount > 0) {
            return
        }

        results.removeAllViews()
        rows.clear()

        for (target in targets) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12, 10, 8, 10)
                setBackgroundResource(R.drawable.bunker_row)
            }

            val statusTv = TextView(this).apply {
                text = "◌  ${target.second}\nПроверка…"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text_secondary))
                setPadding(10, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            }
            rows[target.first] = statusTv
            row.addView(statusTv)

            if (target.first.startsWith("custom_")) {
                val customId = target.first.removePrefix("custom_")
                row.addView(createDeleteButton {
                    CustomServiceStore.remove(this, customId)
                    startMonitoring()
                })
            }

            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 8
            }
            results.addView(row, params)
        }
    }

    private fun updateTargetRow(result: CheckResult) {
        val view = rows[result.id] ?: return
        val dot = if (result.available) "🟢" else "🔴"
        view.text = "$dot  ${result.title}\n${result.label}"
        val colorRes = if (result.available) R.color.bunker_success else R.color.bunker_error
        view.setTextColor(ContextCompat.getColor(this@MainActivity, colorRes))
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = lifecycleScope.launch {
            while (isActive && monitoring) {
                val targets = UrlChecker.targets(this@MainActivity)
                syncRows(targets)

                val deferreds = targets.map { target ->
                    async(Dispatchers.IO) {
                        val result = withTimeoutOrNull(2600L) {
                            UrlChecker.check(target)
                        } ?: CheckResult(target.first, target.second, null, null)

                        withContext(Dispatchers.Main) {
                            updateTargetRow(result)
                        }
                        result
                    }
                }

                val resultsList = deferreds.awaitAll()
                UrlTestWidget.render(this@MainActivity, resultsList)

                checkSelectedApps()
                delay(15_000)
            }
        }
    }

    private suspend fun checkSelectedApps() {
        val prefs = getSharedPreferences("selected_apps", Context.MODE_PRIVATE)
        val selected = prefs.getStringSet("packages", emptySet()) ?: emptySet()
        if (selected.isEmpty()) return

        val installed = installedLaunchableApps().associateBy { it.packageName }

        coroutineScope {
            selected.map { pkg ->
                async(Dispatchers.IO) {
                    val label = installed[pkg]?.label ?: pkg
                    val appResult = withTimeoutOrNull(3000L) {
                        AppNetworkChecker.check(pkg, label)
                    } ?: AppNetworkResult(pkg, label, emptyList(), emptyList(), null, false)

                    withContext(Dispatchers.Main) {
                        val view = appStatusViews[pkg] ?: return@withContext
                        val dot = if (appResult.isAvailable) "🟢" else "🔴"
                        view.text = "$dot  ${appResult.statusText}"
                        val colorRes = if (appResult.isAvailable) R.color.bunker_success else R.color.bunker_error
                        view.setTextColor(ContextCompat.getColor(this@MainActivity, colorRes))
                    }
                }
            }
        }
    }

    private fun toggleMonitoring() {
        monitoring = !monitoring
        if (monitoring) {
            checkButton.text = "ОСТАНОВИТЬ ПРОВЕРКУ"
            checkButton.setBackgroundResource(R.drawable.bunker_button_primary)
            startMonitoring()
            UrlTestMonitorService.start(this)
        } else {
            checkButton.text = "ЗАПУСТИТЬ ПРОВЕРКУ"
            checkButton.setBackgroundResource(R.drawable.bunker_button_secondary)
            monitoringJob?.cancel()
            UrlTestMonitorService.stop(this)
        }
    }

    private fun showAddServiceMenu() {
        AlertDialog.Builder(this)
            .setTitle("СЕРВИСЫ")
            .setItems(arrayOf("＋ Добавить сервис", "Мои добавленные сервисы")) { _, which ->
                when (which) {
                    0 -> showServiceSourceMenu(null)
                    1 -> showCustomServiceDialog()
                }
            }
            .setNegativeButton("ЗАКРЫТЬ", null)
            .show()
    }

    private fun showServiceSourceMenu(replace: CustomService?) {
        AlertDialog.Builder(this)
            .setTitle(replace?.let { "ЗАМЕНИТЬ: ${it.name}" } ?: "ДОБАВИТЬ СЕРВИС")
            .setItems(arrayOf("Популярные сервисы", "Добавить адрес вручную")) { _, which ->
                when (which) {
                    0 -> showPopularServicesPicker()
                    1 -> showCustomServiceEditor(replace)
                }
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun showPopularServicesPicker() {
        val labels = popularServices.map { "${it.name}  •  ${it.url.removePrefix("https://").trimEnd('/')}" }.toTypedArray()
        val checked = BooleanArray(popularServices.size)

        AlertDialog.Builder(this)
            .setTitle("ПОПУЛЯРНЫЕ СЕРВИСЫ")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("ДОБАВИТЬ В МОНИТОРИНГ") { _, _ ->
                var added = 0
                for (i in checked.indices) {
                    if (checked[i]) {
                        val svc = popularServices[i]
                        if (CustomServiceStore.add(this, svc.name, svc.url) != null) {
                            added++
                        }
                    }
                }
                if (added > 0) {
                    startMonitoring()
                }
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun showCustomServiceDialog() {
        val services = CustomServiceStore.getAll(this)
        val labels = services.map { "${it.name}  •  ${it.url}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("СВОИ СЕРВИСЫ")
            .setItems(labels) { _, which ->
                showCustomServiceEditor(services[which])
            }
            .setPositiveButton("＋ ДОБАВИТЬ") { _, _ ->
                showCustomServiceEditor(null)
            }
            .setNegativeButton("ЗАКРЫТЬ", null)
            .show()
    }

    private fun showCustomServiceEditor(service: CustomService?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }

        val nameInput = EditText(this).apply {
            hint = "Название (например: HDRezka)"
            isSingleLine = true
            setText(service?.name ?: "")
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text))
            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text_muted))
        }

        val urlInput = EditText(this).apply {
            hint = "https://example.com"
            isSingleLine = true
            setText(service?.url ?: "")
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text))
            setHintTextColor(ContextCompat.getColor(this@MainActivity, R.color.bunker_text_muted))
        }

        layout.addView(nameInput)
        layout.addView(urlInput)

        val builder = AlertDialog.Builder(this)
            .setTitle(if (service == null) "ДОБАВИТЬ СЕРВИС" else "ИЗМЕНИТЬ СЕРВИС")
            .setView(layout)
            .setPositiveButton("СОХРАНИТЬ", null)
            .setNegativeButton("ОТМЕНА", null)

        if (service != null) {
            builder.setNeutralButton("УДАЛИТЬ") { _, _ ->
                CustomServiceStore.remove(this, service.id)
                startMonitoring()
            }
        }

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()

                if (name.isBlank() || url.isBlank()) {
                    nameInput.error = "Заполните все поля"
                    return@setOnClickListener
                }

                if (!CustomServiceStore.isValidUrl(url)) {
                    urlInput.error = "Некорректный URL"
                    return@setOnClickListener
                }

                if (service == null) {
                    CustomServiceStore.add(this, name, url)
                } else {
                    CustomServiceStore.update(this, service.id, name, url)
                }
                startMonitoring()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmRestoreBuiltIns() {
        AlertDialog.Builder(this)
            .setTitle("ВЕРНУТЬ СТАНДАРТНЫЕ?")
            .setMessage("Все добавленные вручную сервисы будут удалены, а стандартный список сервисов восстановлен.")
            .setPositiveButton("ВЕРНУТЬ СТАНДАРТНЫЕ") { _, _ ->
                CustomServiceStore.restoreBuiltIns(this)
                startMonitoring()
            }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun auditConfiguredAddresses() {
        val targets = UrlChecker.targets(this)
        if (targets.isEmpty()) {
            summary.visibility = View.VISIBLE
            summary.text = "Нет адресов для аудита"
            return
        }

        val progress = AlertDialog.Builder(this)
            .setTitle("АУДИТ АДРЕСОВ")
            .setMessage("Проверяю ${targets.size} адресов...")
            .setNegativeButton("СКРЫТЬ", null)
            .create()
        progress.show()

        lifecycleScope.launch {
            val resultsList = withContext(Dispatchers.IO) {
                coroutineScope {
                    targets.map { target ->
                        async {
                            withTimeoutOrNull(2600L) {
                                UrlChecker.check(target)
                            } ?: CheckResult(target.first, target.second, null, null)
                        }
                    }.awaitAll()
                }
            }
            val available = resultsList.count { it.available }
            val blocked = resultsList.filterNot { it.available }

            val report = buildString {
                append("Доступно: $available из ${targets.size}\n\n")
                if (blocked.isNotEmpty()) {
                    append("Заблокированы или недоступны:\n")
                    for (b in blocked) {
                        append("• ${b.title}\n")
                    }
                } else {
                    append("🎉 Все настроенные сервисы отвечают!")
                }
            }

            if (progress.isShowing) {
                progress.dismiss()
            }

            AlertDialog.Builder(this@MainActivity)
                .setTitle("РЕЗУЛЬТАТ АУДИТА")
                .setMessage(report)
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        super.onDestroy()
    }
}
