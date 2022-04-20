package com.example.android.codelabs.paging.ui

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.Pager
import androidx.paging.PagingSource
import androidx.paging.RemoteMediator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan

/**
 * An enum representing the status of items in the as fetched by the
 * [Pager] when used with a [RemoteMediator]
 */
enum class RemotePresentationState {
    INITIAL, REMOTE_LOADING, SOURCE_LOADING, PRESENTED
}

/**
 * Reduces [CombinedLoadStates] into [RemotePresentationState]. It operates ton the assumption that
 * successful [RemoteMediator] fetches always cause invalidation of the [PagingSource] as in the
 * case of the [PagingSource] provide by Room.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<CombinedLoadStates>.asRemotePresentationState(): Flow<RemotePresentationState> =
    scan(RemotePresentationState.INITIAL) { state, loadState ->
        when (state) {
            RemotePresentationState.PRESENTED -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }
            RemotePresentationState.INITIAL -> when (loadState.mediator?.refresh) {
                is LoadState.Loading -> RemotePresentationState.REMOTE_LOADING
                else -> state
            }
            RemotePresentationState.REMOTE_LOADING -> when (loadState.source.refresh) {
                is LoadState.Loading -> RemotePresentationState.SOURCE_LOADING
                else -> state
            }
            RemotePresentationState.SOURCE_LOADING -> when (loadState.source.refresh) {
                is LoadState.NotLoading -> RemotePresentationState.PRESENTED
                else -> state
            }
        }
    }
        .distinctUntilChanged()