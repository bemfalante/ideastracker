package com.jules.ideastracker

import androidx.lifecycle.*
import kotlinx.coroutines.launch

class IdeaViewModel(private val dao: IdeaDao) : ViewModel() {

    private val _isAsc = MutableLiveData(0) // 0 for DESC (Newest), 1 for ASC (Oldest)
    val isAsc: LiveData<Int> = _isAsc

    val lastInsertedId = MutableLiveData<Long>()

    fun toggleSort() {
        _isAsc.value = if (_isAsc.value == 0) 1 else 0
    }

    fun getIdeas(status: IdeaStatus): LiveData<List<Idea>> {
        return _isAsc.switchMap { asc ->
            when (status) {
                IdeaStatus.FUTURE -> dao.getFutureIdeas(asc ?: 0)
                IdeaStatus.ONGOING -> dao.getOngoingIdeas(asc ?: 0)
                IdeaStatus.DONE -> dao.getDoneIdeas(asc ?: 0)
            }
        }
    }

    fun insert(idea: Idea) = viewModelScope.launch {
        val id = dao.insert(idea)
        lastInsertedId.value = id
    }

    fun update(idea: Idea) = viewModelScope.launch {
        val updatedIdea = if (idea.status == IdeaStatus.ONGOING) {
            idea.copy(lastSavedTimestamp = System.currentTimeMillis())
        } else {
            idea
        }
        dao.update(updatedIdea)
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
