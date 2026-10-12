package com.example.datastoreexercise.factory

import android.content.Intent

/**
 * Interface áp dụng Factory Method Pattern để tạo các đối tượng Intent điều hướng.
 * Tách rời logic tạo Intent và truyền Extras ra khỏi Activity.
 *
 * @param T Kiểu dữ liệu của đối tượng đính kèm (ở đây là User hoặc Any).
 */
interface IntentFactory<T> {
    fun createIntent(actionType: IntentActionType, obj: T?) : Intent
}