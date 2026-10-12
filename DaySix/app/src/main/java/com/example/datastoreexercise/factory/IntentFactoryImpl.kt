package com.example.datastoreexercise.factory

import android.content.Context
import android.content.Intent
import com.example.datastoreexercise.data.model.User
import com.example.datastoreexercise.ui.detail.UserDetailActivity
import com.example.datastoreexercise.ui.newuser.NewUserActivity
import com.example.datastoreexercise.ui.updateuser.UserUpdateActivity


/**
 * Lớp cài đặt cụ thể của IntentFactory.
 * Chịu trách nhiệm tạo Intent tương ứng với từng ActionType và đóng gói dữ liệu (Extras) cần thiết.
 */
class IntentFactoryImpl<T>(private val context: Context) : IntentFactory<T> {
    override fun createIntent(actionType: IntentActionType, obj: T?): Intent {
        return when(actionType) {
            IntentActionType.CREATE -> createIntentToAddNew()
            IntentActionType.EDIT -> createIntentToUpdate(obj)
            IntentActionType.VIEW_DETAIL -> createIntentToViewDetail(obj)
        }
    }

    /**
     * Tạo Intent mở màn hình NewUserActivity để thêm người dùng mới.
     */
    private fun createIntentToAddNew(): Intent {
        return Intent(context, NewUserActivity::class.java)
    }

    /**
     * Tạo Intent mở màn hình UserUpdateActivity, đính kèm ID người dùng qua EXTRA_ID.
     */
    private fun createIntentToUpdate(obj: T?): Intent {
        return Intent(context, UserUpdateActivity::class.java).apply {
            val user = obj as? User
            putExtra(UserUpdateActivity.EXTRA_ID, user?.id)
        }
    }

    /**
     * Tạo Intent mở màn hình UserDetailActivity, đính kèm ID người dùng qua EXTRA_ID.
     */
    private fun createIntentToViewDetail(obj: T?): Intent {
        return Intent(context, UserDetailActivity::class.java).apply {
            val user = obj as? User
            putExtra(UserDetailActivity.EXTRA_ID, user?.id)
        }
    }
}