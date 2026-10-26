/*
 * Copyright (c) 2026 Auxio Project
 * MenuSheet.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.bumbumapps.musicplayer.ui

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.MenuRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButtonToggleGroup
import org.bumbumapps.musicplayer.R
import org.bumbumapps.musicplayer.util.inflater

/**
 * A bottom sheet that shows the items of a [Menu] as large icon rows, with an optional
 * header (cover + title) and an optional ascending/descending toggle for sort menus.
 * Replaces the old dropdown [PopupMenu]s.
 */
open class MenuSheet(context: Context) : BottomSheetDialog(context) {
    private val root: View = context.inflater.inflate(R.layout.sheet_menu, null)
    private val items: LinearLayout = root.findViewById(R.id.sheet_items)

    init {
        setContentView(root)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true

        // No header unless one is set.
        root.findViewById<View>(R.id.sheet_header).isVisible = false
        root.findViewById<View>(R.id.sheet_divider).isVisible = false
    }

    /**
     * Show a header above the items. [bindCover] loads the cover, pass null for a text-only header.
     */
    fun setHeader(title: String, subtitle: String?, bindCover: ((ImageView) -> Unit)? = null) {
        root.findViewById<View>(R.id.sheet_header).isVisible = true
        root.findViewById<View>(R.id.sheet_divider).isVisible = true
        root.findViewById<TextView>(R.id.sheet_title).text = title

        root.findViewById<TextView>(R.id.sheet_subtitle).apply {
            text = subtitle
            isVisible = !subtitle.isNullOrEmpty()
        }

        root.findViewById<View>(R.id.sheet_cover_card).isVisible = bindCover != null
        bindCover?.invoke(root.findViewById(R.id.sheet_cover))
    }

    /**
     * Add a row for every visible item of [menu]. The item with [checkedId] gets a check mark.
     * The sheet is dismissed after [onClick] runs.
     */
    fun addItems(menu: Menu, checkedId: Int? = null, onClick: (MenuItem) -> Unit) {
        menu.forEach { item ->
            if (!item.isVisible || item.hasSubMenu()) {
                return@forEach
            }

            val row = context.inflater.inflate(R.layout.item_sheet_action, items, false)
            row.id = item.itemId
            row.findViewById<TextView>(R.id.sheet_action_label).text = item.title
            row.findViewById<ImageView>(R.id.sheet_action_icon).apply {
                setImageDrawable(item.icon)
                isVisible = item.icon != null
            }

            val checked = item.itemId == checkedId
            row.isSelected = checked
            row.findViewById<View>(R.id.sheet_action_check).isVisible = checked

            row.setOnClickListener {
                onClick(item)
                dismiss()
            }

            items.addView(row)
        }
    }

    /**
     * Show the ascending/descending toggle. [onChange] receives the new ascending value and
     * the sheet is dismissed afterwards.
     */
    fun setDirection(isAscending: Boolean, onChange: (Boolean) -> Unit) {
        root.findViewById<MaterialButtonToggleGroup>(R.id.sheet_direction).apply {
            isVisible = true
            check(if (isAscending) R.id.sheet_direction_asc else R.id.sheet_direction_desc)

            addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (isChecked) {
                    onChange(checkedId == R.id.sheet_direction_asc)
                    dismiss()
                }
            }
        }
    }

    companion object {
        /** Inflate [menuRes] into a throwaway [Menu] so that it can be shown in a [MenuSheet]. */
        fun inflateMenu(context: Context, @MenuRes menuRes: Int): Menu =
            PopupMenu(context, View(context)).apply { inflate(menuRes) }.menu

        /** An empty [Menu] to fill with [Menu.add] for a [MenuSheet]. */
        fun emptyMenu(context: Context): Menu = PopupMenu(context, View(context)).menu
    }
}
