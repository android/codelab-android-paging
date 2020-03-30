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

package com.example.android.codelabs.paging.data

import android.util.Log
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.db.RepoDatabase
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
const val GITHUB_STARTING_PAGE_INDEX = 1

class GithubRemoteMediator(
        private val query: String,
        private val service: GithubService,
        private val repoDatabase: RepoDatabase
) : RemoteMediator<Int, Repo>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
        Log.d("GithubRemoteMediator", "load type: $loadType ")
        // Get the last accessed item from the current PagingState, which holds all loaded
        // pages from DB.
        val dbPage = when (loadType) {
            LoadType.REFRESH -> GITHUB_STARTING_PAGE_INDEX
            LoadType.START -> state.pages.firstOrNull()?.prevKey
            LoadType.END -> state.pages.lastOrNull()?.nextKey
        } ?: return MediatorResult.Success(canRequestMoreData = false)

        val page = dbPage / state.config.pageSize + 1

        Log.d("GithubRemoteMediator", "dbPage: $dbPage, page: $page")

        val apiQuery = query + IN_QUALIFIER
        try {
            val apiResponse = service.searchRepos(apiQuery, page, state.config.pageSize)

            return if (apiResponse.isSuccessful) {
                val repos = apiResponse.body()?.items ?: emptyList()
                repoDatabase.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        repoDatabase.reposDao().clearRepos()
                    }
                    repoDatabase.reposDao().insert(repos)
                }
                Log.d("GithubRemoteMediator", "data: ${repos.size}")
                MediatorResult.Success(canRequestMoreData = repos.isNotEmpty())
            } else {
                MediatorResult.Error(IOException(apiResponse.message()))
            }
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }
}
