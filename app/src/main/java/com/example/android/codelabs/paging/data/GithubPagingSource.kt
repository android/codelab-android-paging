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

import androidx.paging.PagingSource
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.IN_QUALIFIER
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.HttpException
import java.io.IOException

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
private const val GITHUB_STARTING_PAGE_INDEX = 1

@ExperimentalCoroutinesApi
class GithubPagingSource(
        private val service: GithubService,
        private val query: String
) : PagingSource<Int, Repo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val currentPage = params.key ?: GITHUB_STARTING_PAGE_INDEX
        val apiQuery = query + IN_QUALIFIER
        try {
            val apiResponse = service.searchRepos(apiQuery, currentPage, params.loadSize)
            return if (apiResponse.isSuccessful) {
                val repos = apiResponse.body()?.items ?: emptyList()
                LoadResult.Page(
                        data = repos,
                        prevKey = if (currentPage == GITHUB_STARTING_PAGE_INDEX) null else currentPage - 1,
                        // if we don't get any results, we consider that we're at the last page
                        nextKey = if (repos.isEmpty()) null else currentPage + 1
                )
            } else {
                LoadResult.Error(IOException(apiResponse.message()))
            }
        } catch (exception: IOException) {
            return LoadResult.Error(exception)
        } catch (exception: HttpException) {
            return LoadResult.Error(exception)
        }
    }
}
