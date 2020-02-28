package com.example.android.codelabs.paging.model

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow

data class SearchResult(val pagingDataFlow: Flow<PagingData<Repo>>, val totalResultCount: Int)