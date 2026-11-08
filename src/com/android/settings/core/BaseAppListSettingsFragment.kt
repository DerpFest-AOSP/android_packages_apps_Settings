/*
 * SPDX-FileCopyrightText: 2021 AOSP-Krypton Project
 * SPDX-FileCopyrightText: 2022 Nameless-AOSP Project
 * SPDX-FileCopyrightText: 2022, 2025 Paranoid Android
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.settings.core

import android.app.ActivityManager
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R

/**
 * Full-screen app checklist used by heads-up stoplist/blacklist and similar pages.
 *
 * Rows follow the Material 3 Expressive segmented list: position-aware corners, a small
 * gap inside a section, and a separate section for the apps that are already selected.
 */
abstract class BaseAppListSettingsFragment : Fragment(R.layout.app_list_layout) {

    private lateinit var activityManager: ActivityManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private lateinit var launcherApps: LauncherApps
    private lateinit var loadingView: View
    private lateinit var emptyView: View
    private lateinit var searchField: EditText
    private lateinit var clearSearch: View
    private var isLoading = true
    private val handlerThread = HandlerThread(TAG).apply { start() }
    private val bgHandler = Handler(handlerThread.looper)
    private var searchText = ""

    /** Whether to exclude system apps from the list. Subclasses may override. */
    protected open fun excludeSystemApps(): Boolean = true

    /** Whether to restart the selected package after change. Subclasses may override. */
    protected open fun restartPackageOnChange(): Boolean = true

    /** Optional additional app filter. Subclasses may override. */
    protected open fun appFilter(info: LauncherActivityInfo): Boolean = true

    @StringRes
    abstract fun getTitleResId(): Int

    /** @return an initial list of packages that should appear as selected. */
    abstract fun getInitialCheckedList(): List<String>

    /**
     * Called when user selects an item.
     *
     * @param packageName package that changed
     * @param isChecked new checked state
     */
    abstract fun onListUpdate(packageName: String, isChecked: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(getTitleResId())
        activityManager = requireContext().getSystemService(ActivityManager::class.java)!!
        launcherApps = requireContext().getSystemService(LauncherApps::class.java)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = AppListAdapter()
        recyclerView =
            view.findViewById<RecyclerView>(R.id.apps_list)!!.also {
                it.layoutManager = LinearLayoutManager(context)
                it.adapter = adapter
                it.addItemDecoration(adapter.spacingDecoration)
            }
        loadingView = view.findViewById(R.id.apps_loading)!!
        emptyView = view.findViewById(R.id.apps_empty)!!
        searchField = view.findViewById(R.id.apps_search)!!
        clearSearch = view.findViewById(R.id.apps_search_clear)!!
        searchField.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    searchText = s?.toString().orEmpty()
                    clearSearch.visibility = if (searchText.isEmpty()) View.GONE else View.VISIBLE
                    refreshList()
                }
            }
        )
        clearSearch.setOnClickListener { searchField.setText("") }
        refreshList()
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerThread.quitSafely()
    }

    private fun refreshList() {
        if (!::adapter.isInitialized) return
        val query = searchText
        val initialList = getInitialCheckedList()
        bgHandler.post {
            val list =
                launcherApps
                    .getActivityList(null, Process.myUserHandle())
                    .distinctBy { it.componentName.packageName }
                    .filter {
                        (!excludeSystemApps() || !it.applicationInfo!!.isSystemApp()) &&
                            (query.isEmpty() ||
                                it.label.contains(query, ignoreCase = true) ||
                                it.componentName.packageName.contains(query, ignoreCase = true)) &&
                            appFilter(it)
                    }
                    .map { it.toAppInfo() }
                    .sortedWith(
                        compareBy(
                            { it.packageName !in initialList },
                            { it.label.lowercase() }
                        )
                    )
            Log.d(TAG, "refreshList: ${list.size} items")
            val activity = activity ?: return@post
            activity.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                if (isLoading) {
                    isLoading = false
                    loadingView.visibility = View.GONE
                }
                adapter.setCheckedPackages(initialList)
                adapter.submitList(list) {
                    if (!isAdded) return@submitList
                    val empty = list.isEmpty()
                    recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
                    emptyView.visibility = if (empty) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun LauncherActivityInfo.toAppInfo() =
        AppInfo(
            packageName = componentName.packageName,
            label = label.toString(),
            icon = getIcon(0)
        )

    private inner class AppListAdapter : ListAdapter<AppInfo, AppListViewHolder>(itemCallback) {
        private val checkedPackages = mutableSetOf<String>()
        private val res: Resources = requireContext().resources
        private val outerRadius =
            res.getDimension(
                com.android.settingslib.widget.theme.R.dimen.settingslib_preference_corner_radius
            )
        private val innerRadius =
            res.getDimension(
                com.android.settingslib.widget.theme.R.dimen.settingslib_expressive_radius_extrasmall2
            )
        private val surfaceColor =
            ContextCompat.getColor(
                requireContext(),
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorSurfaceBright
            )
        private val checkedColor =
            ContextCompat.getColor(
                requireContext(),
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorSecondaryContainer
            )
        private val onSurfaceColor = resolveThemeColor(android.R.attr.textColorPrimary)
        private val onCheckedColor =
            ContextCompat.getColor(
                requireContext(),
                com.android.settingslib.widget.theme.R.color.settingslib_materialColorOnSecondaryContainer
            )
        private val rippleColor = resolveThemeColor(android.R.attr.colorControlHighlight)
        val spacingDecoration = SpacingDecoration()

        fun setCheckedPackages(packages: Collection<String>) {
            checkedPackages.clear()
            checkedPackages.addAll(packages)
        }

        fun isGroupBoundary(position: Int): Boolean {
            if (position <= 0 || position >= itemCount) return false
            return isChecked(position) != isChecked(position - 1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            AppListViewHolder(
                layoutInflater.inflate(R.layout.app_list_item, parent, false)
            )

        override fun onBindViewHolder(holder: AppListViewHolder, position: Int) {
            val appInfo = getItem(position) ?: return
            val checked = checkedPackages.contains(appInfo.packageName)
            holder.packageName = appInfo.packageName
            holder.label.text = appInfo.label
            holder.label.setTextColor(if (checked) onCheckedColor else onSurfaceColor)
            holder.icon.setImageDrawable(appInfo.icon)
            holder.checkBox.isChecked = checked
            bindSegmentBackground(holder.itemView, position, checked)
            holder.itemView.setOnClickListener {
                val index = holder.bindingAdapterPosition
                if (index != RecyclerView.NO_POSITION) {
                    toggle(index)
                }
            }
            holder.itemView.accessibilityDelegate =
                object : View.AccessibilityDelegate() {
                    override fun onInitializeAccessibilityNodeInfo(
                        host: View,
                        info: AccessibilityNodeInfo
                    ) {
                        super.onInitializeAccessibilityNodeInfo(host, info)
                        info.isCheckable = true
                        info.isChecked = checkedPackages.contains(holder.packageName)
                        info.text = holder.label.text
                    }
                }
        }

        private fun toggle(position: Int) {
            val appInfo = getItem(position) ?: return
            val nowChecked = !checkedPackages.contains(appInfo.packageName)
            if (nowChecked) {
                checkedPackages.add(appInfo.packageName)
            } else {
                checkedPackages.remove(appInfo.packageName)
            }
            onListUpdate(appInfo.packageName, nowChecked)
            if (restartPackageOnChange()) {
                runCatching { activityManager.forceStopPackage(appInfo.packageName) }
            }
            notifyItemChanged(position)
            if (position > 0) notifyItemChanged(position - 1)
            if (position + 1 < itemCount) notifyItemChanged(position + 1)
            recyclerView.invalidateItemDecorations()
        }

        private fun isChecked(position: Int): Boolean =
            checkedPackages.contains(getItem(position).packageName)

        private fun bindSegmentBackground(view: View, position: Int, checked: Boolean) {
            val previousChecked = position > 0 && isChecked(position - 1)
            val nextChecked = position + 1 < itemCount && isChecked(position + 1)
            val isFirst = position == 0 || previousChecked != checked
            val isLast = position == itemCount - 1 || nextChecked != checked
            val top = if (isFirst) outerRadius else innerRadius
            val bottom = if (isLast) outerRadius else innerRadius
            val radii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
            val fill = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(if (checked) checkedColor else surfaceColor)
                cornerRadii = radii
            }
            val mask = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.WHITE)
                cornerRadii = radii
            }
            view.background = RippleDrawable(ColorStateList.valueOf(rippleColor), fill, mask)
        }

        private fun resolveThemeColor(attr: Int): Int {
            val value = TypedValue()
            val theme = requireContext().theme
            return if (theme.resolveAttribute(attr, value, true)) {
                if (value.resourceId != 0) {
                    ContextCompat.getColor(requireContext(), value.resourceId)
                } else {
                    value.data
                }
            } else {
                Color.TRANSPARENT
            }
        }

        inner class SpacingDecoration : RecyclerView.ItemDecoration() {
            private val screenInsetStart =
                resolveThemeDimension(android.R.attr.listPreferredItemPaddingStart)
            private val screenInsetEnd =
                resolveThemeDimension(android.R.attr.listPreferredItemPaddingEnd)
            private val itemGap =
                res.getDimensionPixelSize(
                    com.android.settingslib.widget.theme.R.dimen.settingslib_expressive_space_extrasmall1
                )
            private val groupGap =
                res.getDimensionPixelSize(
                    com.android.settingslib.widget.theme.R.dimen.settingslib_expressive_space_small1
                )

            override fun getItemOffsets(
                outRect: android.graphics.Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val rtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
                outRect.left = if (rtl) screenInsetEnd else screenInsetStart
                outRect.right = if (rtl) screenInsetStart else screenInsetEnd
                val position = parent.getChildAdapterPosition(view)
                if (position == RecyclerView.NO_POSITION || position == 0) {
                    return
                }
                outRect.top = if (isGroupBoundary(position)) groupGap else itemGap
            }

            private fun resolveThemeDimension(attr: Int): Int {
                val value = TypedValue()
                val theme = requireContext().theme
                if (theme.resolveAttribute(attr, value, true) &&
                    value.type == TypedValue.TYPE_DIMENSION
                ) {
                    return TypedValue.complexToDimensionPixelSize(value.data, res.displayMetrics)
                }
                return (16f * res.displayMetrics.density).toInt()
            }
        }
    }

    private class AppListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var packageName: String = ""
        val icon: ImageView = itemView.findViewById(R.id.icon)!!
        val label: TextView = itemView.findViewById(R.id.label)!!
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)!!
    }

    private data class AppInfo(
        val packageName: String,
        val label: String,
        val icon: Drawable,
    )

    companion object {
        private const val TAG = "BaseAppListSettingsFragment"

        private val itemCallback =
            object : DiffUtil.ItemCallback<AppInfo>() {
                override fun areItemsTheSame(oldInfo: AppInfo, newInfo: AppInfo) =
                    oldInfo.packageName == newInfo.packageName

                // Position decides the segment shape, so a move has to rebind.
                override fun areContentsTheSame(oldInfo: AppInfo, newInfo: AppInfo) = false
            }
    }
}
