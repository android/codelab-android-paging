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

package com.example.android.codelabs.paging.api

import android.util.Log
import com.example.android.codelabs.paging.model.RepoSearchResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException

private const val TAG = "GithubService"
private const val IN_QUALIFIER = "in:name,description"

/**
 * Search repos based on a query.
 * Trigger a request to the Github searchRepo API with the following params:
 * @param query searchRepo keyword
 * @param page request page index
 * @param itemsPerPage number of repositories to be returned by the Github API per page
 */
suspend fun searchRepos(
        service: GithubService,
        query: String,
        page: Int,
        itemsPerPage: Int
): RepoSearchResult {
    Log.d(TAG, "query: $query, page: $page, itemsPerPage: $itemsPerPage")

    val apiQuery = query + IN_QUALIFIER

    val response = service.searchRepos(apiQuery, page, itemsPerPage)
    if (response.isSuccessful) {
        Log.d(TAG, "got a response $response")
        if (response.isSuccessful) {
            val repos = response.body()?.items ?: emptyList()
            return RepoSearchResult(repos)
        } else {
            return RepoSearchResult(emptyList(), IOException(response.message() ?: "Unknown error"))
        }
    } else {
        Log.d(TAG, "fail to get data")
        return RepoSearchResult(emptyList(), IOException(response.message() ?: "Unknown error"))
    }
}

/**
 * Github API communication setup via Retrofit.
 */
interface GithubService {
    /**
     * Get repos ordered by stars.
     */
    @GET("search/repositories?sort=stars")
    suspend fun searchRepos(
            @Query("q") query: String,
            @Query("page") page: Int,
            @Query("per_page") itemsPerPage: Int
    ): Response<RepoSearchResponse>

    companion object {
        private const val BASE_URL = "https://api.github.com/"

        fun create(): GithubService {
            val logger = HttpLoggingInterceptor()
            logger.level = Level.BASIC

            val client = OkHttpClient.Builder()
                    .addInterceptor(logger)
                    .build()
            return Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(GithubService::class.java)
        }
    }
}