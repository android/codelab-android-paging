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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
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
@FlowPreview
class SearchRepositoriesViewModel(private val repository: GithubRepository) : ViewModel() {

    @Volatile
    private var lastQueryValue: String? = null
    @Volatile
    private var lastSearchResult: Flow<PagingData<UiModel>>? = null

    /**
     * Search a repository based on a query string.
     */
    fun searchRepo(queryString: String): Flow<PagingData<UiModel>> {
        val result = lastSearchResult
        if (queryString == lastQueryValue && result != null) {
            return result
        }
        lastQueryValue = queryString
        val searchResult = repository.getSearchResultStream(queryString)
        val newResult: Flow<PagingData<UiModel>> = searchResult.pagingDataFlow
                .map { pagingData -> pagingData.map { UiModel.RepoItem(it) as UiModel } }
                .map {
                    it.insertSeparators { before, after ->
                        if (before == null && after is UiModel.RepoItem) {
                            UiModel.SeparatorItem("${after.repo.stars / 10_000}0.000+ stars")
                        }
                        if (before is UiModel.RepoItem && after is UiModel.RepoItem
                                && before.repo.stars / 10_000 > after.repo.stars / 10_000) {
                            UiModel.SeparatorItem("${after.repo.stars / 10_000}0.000+ stars")
                        } else {
                            // no separator
                            null
                        }
                    }
                    it.addHeader(UiModel.HeaderItem("Total repositories: ${searchResult.totalResultCount}"))
                }
                .cachedIn(viewModelScope)
        lastSearchResult = newResult
        return newResult
    }
}

sealed class UiModel {
    data class RepoItem(val repo: Repo) : UiModel()
    data class SeparatorItem(val description: String) : UiModel()
    data class HeaderItem(val description: String) : UiModel()
}