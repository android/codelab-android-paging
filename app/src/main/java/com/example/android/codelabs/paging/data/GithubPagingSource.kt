package com.example.android.codelabs.paging.data

import androidx.paging.PagingSource
import com.example.android.codelabs.paging.api.GithubService
import com.example.android.codelabs.paging.api.searchRepos
import com.example.android.codelabs.paging.model.Repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@ExperimentalCoroutinesApi
@FlowPreview
class GithubPagingSource(
        private val service: GithubService,
        private val query: String
) : PagingSource<Int, Repo>() {

    private var _totalReposCount = 0
    val totalReposCount: Int
        get() = _totalReposCount

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Repo> {
        val position = params.key ?: 0
        val apiResponse = searchRepos(service, query, position, GithubRepository.NETWORK_PAGE_SIZE)
        return when (apiResponse) {
            is RepoSearchResult.Success -> {
                _totalReposCount = apiResponse.totalReposCount
                LoadResult.Page(
                        data = apiResponse.data,
                        prevKey = if (position == 0) null else -1,
                        // if we don't get any results, we consider that we're at the last page
                        nextKey = if (apiResponse.data.isEmpty()) null else position + 1
                )
            }
            is RepoSearchResult.Error -> {
                _totalReposCount = -1
                LoadResult.Error(apiResponse.error)
            }
        }
    }
}