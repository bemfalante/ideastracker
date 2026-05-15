package com.jules.ideastracker

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class IdeaViewModel(private val dao: IdeaDao) : ViewModel() {

    private val _isAsc = MutableLiveData(0) // 0 for DESC (Newest), 1 for ASC (Oldest)
    val isAsc: LiveData<Int> = _isAsc

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _lastInsertedId = MutableLiveData<Long?>()
    val lastInsertedId: LiveData<Long?> = _lastInsertedId

    fun toggleSort() {
        _isAsc.value = if (_isAsc.value == 0) 1 else 0
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getIdeas(status: IdeaStatus): LiveData<List<Idea>> {
        val result = MediatorLiveData<List<Idea>>()

        fun update() {
            val asc = _isAsc.value ?: 0
            val query = _searchQuery.value ?: ""

            val source = if (query.isEmpty()) {
                when (status) {
                    IdeaStatus.FUTURE -> dao.getFutureIdeas(asc)
                    IdeaStatus.ONGOING -> dao.getOngoingIdeas(asc)
                    IdeaStatus.DONE -> dao.getDoneIdeas(asc)
                }
            } else {
                when (status) {
                    IdeaStatus.FUTURE -> dao.searchFutureIdeas(query, asc)
                    IdeaStatus.ONGOING -> dao.searchOngoingIdeas(query, asc)
                    IdeaStatus.DONE -> dao.searchDoneIdeas(query, asc)
                }
            }

            result.removeSource(source)
            result.addSource(source) { result.value = it }
        }

        result.addSource(_isAsc) { update() }
        result.addSource(_searchQuery) { update() }

        return result
    }

    fun insert(idea: Idea) = viewModelScope.launch {
        val id = dao.insert(idea)
        _lastInsertedId.value = id
    }

    fun clearLastInsertedId() {
        _lastInsertedId.value = null
    }

    fun update(idea: Idea) = viewModelScope.launch {
        val updatedIdea = if (idea.status == IdeaStatus.ONGOING) {
            idea.copy(lastSavedTimestamp = System.currentTimeMillis())
        } else {
            idea
        }
        dao.update(updatedIdea)
    }

    fun moveToTop(idea: Idea) = viewModelScope.launch {
        // We set manualOrder to be 1 less than the current minimum
        val minOrder = dao.getMinManualOrder() ?: 0
        dao.update(idea.copy(manualOrder = minOrder - 1))
    }

    fun updateStatus(idea: Idea, newStatus: IdeaStatus, dod: String? = null, nextSteps: String? = null, conclusion: String? = null) = viewModelScope.launch {
        val updatedIdea = when (newStatus) {
            IdeaStatus.ONGOING -> {
                if (idea.status == IdeaStatus.FUTURE) {
                    idea.copy(
                        status = newStatus,
                        startedTimestamp = System.currentTimeMillis(),
                        definitionOfDone = dod ?: idea.definitionOfDone ?: "",
                        nextSteps = nextSteps ?: idea.nextSteps ?: ""
                    )
                } else {
                    idea.copy(status = newStatus, finishedTimestamp = null)
                }
            }
            IdeaStatus.DONE -> {
                idea.copy(
                    status = newStatus,
                    finishedTimestamp = System.currentTimeMillis(),
                    conclusion = conclusion ?: idea.conclusion ?: ""
                )
            }
            IdeaStatus.FUTURE -> {
                idea.copy(status = newStatus, startedTimestamp = null, finishedTimestamp = null)
            }
        }
        dao.update(updatedIdea)
    }

    fun delete(idea: Idea) = viewModelScope.launch {
        dao.delete(idea)
    }
}

class IdeaViewModelFactory(private val dao: IdeaDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(IdeaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return IdeaViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
