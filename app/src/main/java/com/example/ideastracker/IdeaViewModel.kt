package com.example.ideastracker

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class IdeaViewModel(private val dao: IdeaDao) : ViewModel() {

    private val _isAsc = MutableLiveData(0) // 0 for DESC (Newest), 1 for ASC (Oldest)
    val isAsc: LiveData<Int> = _isAsc

    fun toggleSort() {
        _isAsc.value = if (_isAsc.value == 0) 1 else 0
    }

    fun getIdeas(status: IdeaStatus): LiveData<List<Idea>> {
        return _isAsc.switchMap { asc ->
            dao.getIdeasByStatus(status, asc)
        }
    }

    fun insert(idea: Idea) = viewModelScope.launch {
        dao.insert(idea)
    }

    fun update(idea: Idea) = viewModelScope.launch {
        dao.update(idea)
    }

    fun updateStatus(idea: Idea, newStatus: IdeaStatus) = viewModelScope.launch {
        val updatedIdea = when (newStatus) {
            IdeaStatus.ONGOING -> idea.copy(status = newStatus, startedTimestamp = idea.startedTimestamp ?: System.currentTimeMillis())
            IdeaStatus.DONE -> idea.copy(status = newStatus, finishedTimestamp = idea.finishedTimestamp ?: System.currentTimeMillis())
            IdeaStatus.FUTURE -> idea.copy(status = newStatus)
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
