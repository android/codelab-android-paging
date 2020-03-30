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
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.android.codelabs.paging.data.GithubRepository.Companion.NETWORK_PAGE_SIZE
import com.example.android.codelabs.paging.db.RepoDatabase
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class GithubRemoteMediator(
        private val pagingSource: GithubPagingSource,
        private val repoDatabase: RepoDatabase,
        private val ioDispatcher: CoroutineContext
) : RemoteMediator<Int, Repo>() {

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>?): MediatorResult {
        Log.d("GithubRemoteMediator", "load type: $loadType, state: $state")

        val key = when (loadType) {
            LoadType.REFRESH -> GITHUB_STARTING_PAGE_INDEX
            LoadType.START -> state?.pages?.last()?.prevKey ?: GITHUB_STARTING_PAGE_INDEX
            LoadType.END -> state?.pages?.last()?.nextKey ?: GITHUB_STARTING_PAGE_INDEX
        }

        val result = pagingSource.load(PagingSource.LoadParams(
                loadType = loadType,
                key = key,
                loadSize = NETWORK_PAGE_SIZE,
                placeholdersEnabled = false,
                pageSize = NETWORK_PAGE_SIZE
        ))

        return when (result) {
            is PagingSource.LoadResult.Page -> {
                withContext(ioDispatcher) {
                    repoDatabase.withTransaction {
                        if (loadType == LoadType.REFRESH) {
                            repoDatabase.reposDao().clearRepos()
                        }
                        repoDatabase.reposDao().insert(result.data)
                    }
                }
                MediatorResult.Success(hasMoreData = result.nextKey != null)
            }
            is PagingSource.LoadResult.Error -> MediatorResult.Error(result.throwable)

        }
    }
}