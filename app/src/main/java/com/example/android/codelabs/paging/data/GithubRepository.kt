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

package com.example.android.codelabs.paging.data

import android.util.Log
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingDataFlow
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.searchRepos
import com.example.android.codelabs.paging.model.Repo
import com.example.android.codelabs.paging.model.RepoSearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow

/**
 * Repository class that works with local and remote data sources.
 */
@FlowPreview
@ExperimentalCoroutinesApi
class GithubRepository(private val service: GithubService) {

    // keep the list of all results received
    private val inMemoryCache = mutableListOf<Repo>()

    // keep channel of results. The channel allows us to broadcast updates so
    // the subscriber will have the latest data
    private val searchResults = ConflatedBroadcastChannel<RepoSearchResult>()

    // keep the last requested page. When the request is successful, increment the page number.
    private var lastRequestedPage = 1

    // avoid triggering multiple requests in the same time
    private var isRequestInProgress = false

    /**
     * Search repositories whose names match the query, exposed as a stream of data that will emit
     * every time we get more data from the network.
     */
    suspend fun getSearchResultStream(query: String): Flow<PagingData<Repo>> {
        Log.d("GithubRepository", "New query: $query")
        lastRequestedPage = 1
        requestAndSaveData(query)

        val pagedList = PagingDataFlow(
                config = PagingConfig(pageSize = NETWORK_PAGE_SIZE),
                pagingSourceFactory = { GithubPagingSource(service, query) }
        )

        return pagedList
    }

    suspend fun requestMore(query: String) {
        requestAndSaveData(query)
    }

    private suspend fun requestAndSaveData(query: String) {
        if (isRequestInProgress) return

        isRequestInProgress = true
        val apiResponse = searchRepos(service, query, lastRequestedPage, NETWORK_PAGE_SIZE)
        Log.d("GithubRepository", "response $apiResponse")
        // add the new result list to the existing list
        when (apiResponse) {
            is RepoSearchResult.Success -> {
                inMemoryCache.addAll(apiResponse.data)
                val reposByName = reposByName(query)
                searchResults.offer(RepoSearchResult.Success(reposByName))
            }
            is RepoSearchResult.Error -> {
                searchResults.offer(RepoSearchResult.Error(apiResponse.error))
            }
        }
        lastRequestedPage++
        isRequestInProgress = false
    }

    private fun reposByName(query: String): List<Repo> {
        // from the in memory cache select only the repos whose name or description matches
        // the query. Then order the results.
        return inMemoryCache.filter {
            it.name.contains(query, true) ||
                    (it.description != null && it.description.contains(query, true))
        }.sortedWith(compareByDescending<Repo> { it.stars }.thenBy { it.name })
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 50
    }
}
