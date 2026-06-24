package com.example.hangsha_android.ui.view.mymemos

data class MyMemosUiState(
    val isLoading: Boolean = true,
    val deletingMemoId: Long? = null,
    val errorMessage: String? = null,
    val toastMessage: String? = null,
    val groupedMemos: List<MyMemoDateGroup> = emptyList()
)

data class MyMemoDateGroup(
    val dateDisplay: String,
    val memos: List<MyMemoItem>
)

data class MyMemoItem(
    val id: Long,
    val eventId: Long,
    val eventTitle: String,
    val content: String,
    val tagNames: List<String>,
    val dateDisplay: String
)
