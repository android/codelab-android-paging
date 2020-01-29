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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.searchRepos
import com.example.android.codelabs.paging.model.Repo
import com.example.android.codelabs.paging.model.RepoSearchResult

/**
 * Repository class that works with local and remote data sources.
 */
class GithubRepository(private val service: GithubService) {

    // keep the list of responses
    private val inMemoryCache = MutableLiveData<List<Repo>>()

    // keep the last requested page. When the request is successful, increment the page number.
    private var lastRequestedPage = 1

    // LiveData of network errors.
    private val networkErrors = MutableLiveData<String>()

    // avoid triggering multiple requests in the same time
    private var isRequestInProgress = false

    /**
     * Search repositories whose names match the query.
     */
    fun search(query: String): RepoSearchResult {
        Log.d("GithubRepository", "New query: $query")
        lastRequestedPage = 1
        requestAndSaveData(query)

        // Get data from the in memory cache
        val data = reposByName(query)

        return RepoSearchResult(data, networkErrors)
    }

    fun requestMore(query: String) {
        requestAndSaveData(query)
    }

    private fun requestAndSaveData(query: String) {
        if (isRequestInProgress) return

        isRequestInProgress = true
        searchRepos(service, query, lastRequestedPage, NETWORK_PAGE_SIZE, { repos ->
            // add the new result list to the existing list
            val allResults = mutableListOf<Repo>()
            inMemoryCache.value?.let { allResults.addAll(it) }
            allResults.addAll(repos)

            inMemoryCache.postValue(allResults)
            lastRequestedPage++
            isRequestInProgress = false
        }, { error ->
            networkErrors.postValue(error)
            isRequestInProgress = false
        })
    }

    private fun reposByName(query: String): LiveData<List<Repo>> {
        return Transformations.switchMap(inMemoryCache) { repos ->
            // from the in memory cache select only the repos whose name or description matches
            // the query. Then order the results.
            val filteredList = repos.filter {
                it.name.contains(query, true) ||
                        (it.description != null && it.description.contains(query, true))
            }.sortedWith(compareByDescending<Repo> { it.stars }.thenBy { it.name })

            MutableLiveData(filteredList)
        }
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}
