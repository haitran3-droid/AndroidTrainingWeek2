package com.example.datastoreexercise.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.datastoreexercise.ui.MainActivity
import com.example.datastoreexercise.R
import com.example.datastoreexercise.data.model.User
import com.example.datastoreexercise.databinding.UserItemBinding

/**
 * Adapter quản lý hiển thị danh sách User trong RecyclerView.
 *
 * @param users Danh sách lưu trữ các User trong bộ nhớ Adapter.
 * @param listener Interface nhận sự kiện khi người dùng tương tác với menu tùy chọn (update, delete, detail).
 */
class UserAdapter(
    private val users: MutableList<User> = mutableListOf(),
    private val listener: MainActivity.OptionMenuClickListener
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    /**
     * ViewHolder nắm giữ ViewBinding của từng item (user_item.xml).
     */
    class ViewHolder(
        v: View,
        binding: UserItemBinding,
        private val listener: MainActivity.OptionMenuClickListener
    ) : RecyclerView.ViewHolder(v) {
        private val binding: UserItemBinding
        private lateinit var user: User

        init {
            this.binding = binding
            // Đăng ký sự kiện click nút menu 3 chấm
            this.binding.btnOption.setOnClickListener { onOptionMenuClick() }
        }

        /**
         * Gán dữ liệu của User vào các View tương ứng.
         */
        fun bindData(user: User) {
            this.user = user
            binding.textEmail.text = user.email
            binding.textFullname.text = user.fullName
        }

        /**
         * Hiển thị PopupMenu chứa các hành động: Sửa (Update), Xóa (Delete), Chi tiết (Detail).
         */
        private fun onOptionMenuClick() {
            val popup = PopupMenu(binding.root.context, binding.btnOption)
            popup.inflate(R.menu.option_menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_item_delete -> {
                        listener.delete(user)
                        true
                    }

                    R.id.menu_item_update -> {
                        listener.update(user)
                        true
                    }

                    R.id.menu_item_detail -> {
                        listener.viewDetail(user)
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    /**
     * Cập nhật toàn bộ danh sách dữ liệu mới và thông báo cho RecyclerView vẽ lại.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(users: List<User>) {
        this.users.clear()
        this.users.addAll(users)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: UserItemBinding = UserItemBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding.root, binding, listener)
    }

    override fun getItemCount(): Int {
        return users.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(users[position])
    }
}