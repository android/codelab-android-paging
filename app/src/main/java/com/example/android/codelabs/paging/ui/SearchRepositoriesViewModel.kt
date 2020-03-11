/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.PagingData.Companion.insertSeparators
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import com.example.android.codelabs.paging.data.GithubRepository
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ViewModel for the [SearchRepositoriesActivity] screen.
 * The ViewModel works with the [GithubRepository] to get the data.
 */
@ExperimentalCoroutinesApi
class SearchRepositoriesViewModel(private val repository: GithubRepository) : ViewModel() {

    @Volatile
    private var currentQueryValue: String? = null
    @Volatile
    private var currentSearchResult: Flow<PagingData<UiModel>>? = null

    /**
     * Search a repository based on a query string.
     */
    fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {
        val lastResult = currentSearchResult
        if (queryString == currentQueryValue && lastResult != null) {
            return lastResult
        }
        currentQueryValue = queryString
        val newResult: Flow<PagingData<UiModel>> = repository.getSearchResultStream(queryString)
                .map { pagingData -> pagingData.map { UiModel.RepoItem(it) } }
                .map {
                    it.insertSeparators<UiModel.RepoItem, UiModel> { before, after ->
                        if (before == null && after is UiModel.RepoItem) {
                            Log.d("ViewModel", "-> adding null separator")
                            UiModel.SeparatorItem("${after.repo.stars / 10_000}0.000+ stars")
                        } else if (before is UiModel.RepoItem && after is UiModel.RepoItem
                                && before.repo.stars / 10_000 > after.repo.stars / 10_000) {
                            if (after.repo.stars >= 10_000) {
                                UiModel.SeparatorItem("${after.repo.stars / 10_000}0.000+ stars")
                            } else {
                                UiModel.SeparatorItem("< 10.000+ stars")
                            }
                        } else {
                            // no separator
                            null
                        }
                    }
                }
                .cachedIn(viewModelScope)
        currentSearchResult = newResult
        return newResult
    }
}

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
}
