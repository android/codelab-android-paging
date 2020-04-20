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
import com.example.android.codelabs.paging.db.RemoteKeys
import com.example.android.codelabs.paging.db.RepoDatabase
import com.example.android.codelabs.paging.model.Repo
import retrofit2.HttpException
import java.io.IOException
import java.io.InvalidObjectException

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
const val GITHUB_STARTING_PAGE_INDEX = 1

class GithubRemoteMediator(
        private val query: String,
        private val service: GithubService,
        private val repoDatabase: RepoDatabase
) : RemoteMediator<Int, Repo>() {

    /**
     * The paging library will call this method when there is no more data in the database to
     * load from.
     *
     * For every item we retrieve we store the previous and next keys, so we know how to request
     * more data, based on that specific item.
     *
     * state.pages holds the pages that were loaded until now and saved in the database.
     *
     * If the load type is REFRESH it means that we are in one of the following situations:
     *  * it's the first time when the query was triggered and there was nothing saved yet in the
     *  database
     *  * a swipe to refresh was called
     * If the load type is START, we need to rely on the first item available in the pages
     * retrieved so far to compute they key for the previous page.
     * If the load type is END, we need to rely on the last item available in the pages
     * retrieved so far to compute they key for the next page.
     */
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Repo>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }
            LoadType.START -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                if (remoteKeys == null) {
                    // The LoadType is START so some data was loaded before,
                    // so we should have been able to get remote keys
                    // If the remoteKeys are null, then we're an invalid state and we have a bug
                    throw InvalidObjectException("Remote key and the prevKey should not be null")
                }
                // If the previous key is null, then we can't request more data
                val prevKey = remoteKeys.prevKey
                if (prevKey == null) {
                    return MediatorResult.Success(endOfPaginationReached = false)
                }
                remoteKeys.prevKey
            }
            LoadType.END -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                if (remoteKeys?.nextKey == null) {
                    // The LoadType is END, so some data was loaded before,
                    // so we should have been able to get remote keys
                    // If the remoteKeys are null, then we're an invalid state and we have a bug
                    throw InvalidObjectException("Remote key should not be null for $loadType")
                }
                remoteKeys.nextKey
            }
        }

        val apiQuery = query + IN_QUALIFIER

        try {
            val apiResponse = service.searchRepos(apiQuery, page, state.config.pageSize)

            return if (apiResponse.isSuccessful) {
                val repos = apiResponse.body()?.items ?: emptyList()
                val canRequestMoreData = repos.isNotEmpty()
                repoDatabase.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        repoDatabase.remoteKeysDao().clearRemoteKeys()
                        repoDatabase.reposDao().clearRepos()
                    }
                    val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                    val nextKey = if (canRequestMoreData) page + 1 else null
                    val keys = repos.map {
                        RemoteKeys(repoId = it.id, prevKey = prevKey, nextKey = nextKey)
                    }
                    repoDatabase.remoteKeysDao().insertAll(keys)
                    repoDatabase.reposDao().insert(repos)
                }
                MediatorResult.Success(endOfPaginationReached = canRequestMoreData)
            } else {
                MediatorResult.Error(IOException(apiResponse.message()))
            }
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    /**
     * Get the remote keys of on the last item retrieved
     */
    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Repo>): RemoteKeys? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull() { it.data.isNotEmpty() }?.data?.lastOrNull()
                ?.let { repo ->
                    // Get the remote keys of the last item retrieved
                    repoDatabase.remoteKeysDao().remoteKeysRepoId(repo.id)
                }
    }

    /**
     * Get the remote keys of the first item retrieved
     */
    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Repo>): RemoteKeys? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull() { it.data.isNotEmpty() }?.data?.firstOrNull()
                ?.let { repo ->
                    // Get the remote keys of the first items retrieved
                    repoDatabase.remoteKeysDao().remoteKeysRepoId(repo.id)
                }
    }

    /**
     * Get the remote keys of the closest item to the anchor position
     */
    private suspend fun getRemoteKeyClosestToCurrentPosition(
            state: PagingState<Int, Repo>
    ): RemoteKeys? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { repoId ->
                // Get the remote keys of the item
                repoDatabase.remoteKeysDao().remoteKeysRepoId(repoId)
            }
        }
    }
}
