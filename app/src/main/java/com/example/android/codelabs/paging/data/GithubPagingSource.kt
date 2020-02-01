package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.searchRepos
import com.example.android.codelabs.paging.model.Repo
import com.example.android.codelabs.paging.model.RepoSearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class GithubPagingSource(
        private val service: GithubService,
        private val query: String
) : PagingSource<Int, Repo>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val position = params.key ?: 0
        val apiResponse = searchRepos(service, query, position, GithubRepository.NETWORK_PAGE_SIZE)
        return when(apiResponse){
            is RepoSearchResult.Success -> LoadResult.Page(
                    data = apiResponse.data,
                    prevKey = position,
                    nextKey = position + 1
            )
            is RepoSearchResult.Error -> LoadResult.Error(apiResponse.error)
        }
    }
}