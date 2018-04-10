package com.example.android.codelabs.paging.model

import android.arch.lifecycle.LiveData

/**
 * RepoSearchResult from a search, which contains LiveData<List<Repo>> holding query data,
 * and a LiveData<String> of network error state.
 */
data class RepoSearchResult(
        val data: LiveData<List<Repo>>,
        val networkErrors: LiveData<String>
)