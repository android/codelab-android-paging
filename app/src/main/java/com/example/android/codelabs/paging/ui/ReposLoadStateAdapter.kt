/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.codelabs.paging.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ReposLoadStateAdapter(private val retry: () -> Unit) : RecyclerView.Adapter<ReposLoadStateViewHolder>() {

    /**
     * LoadState to present in the adapter.
     *
     * Changing this property will immediately notify the Adapter to change the item it's
     * presenting.
     */
    var loadState: LoadState = LoadState.Done
        set(loadState) {
            if (field != loadState) {
                val oldItem = displayLoadStateAsItem(field)
                val newItem = displayLoadStateAsItem(loadState)

                if (oldItem && !newItem) {
                    notifyItemRemoved(0)
                } else if (newItem && !oldItem) {
                    notifyItemInserted(0)
                } else if (oldItem && newItem) {
                    notifyItemChanged(0)
                }
                field = loadState
            }
        }

    override fun onBindViewHolder(holder: ReposLoadStateViewHolder, position: Int) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReposLoadStateViewHolder {
        return ReposLoadStateViewHolder.create(retry, parent)
    }

    override fun getItemViewType(position: Int): Int = 0

    override fun getItemCount(): Int = if (displayLoadStateAsItem(loadState)) 1 else 0

    /**
     * Returns true if the LoadState should be displayed as a list item when active.
     *
     *  [LoadState.Loading] and [LoadState.Error] present as list items,
     * [LoadState.Done] is not.
     */
    private fun displayLoadStateAsItem(loadState: LoadState): Boolean {
        return loadState is LoadState.Loading || loadState is LoadState.Error
    }
}
