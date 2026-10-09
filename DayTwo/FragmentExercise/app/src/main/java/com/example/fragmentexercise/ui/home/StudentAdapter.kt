package com.example.fragmentexercise.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.fragmentexercise.R
import com.example.fragmentexercise.data.model.Gender
import com.example.fragmentexercise.data.model.Student
import com.example.fragmentexercise.databinding.ItemStudentBinding

class StudentAdapter(
    private val students: MutableList<Student> = mutableListOf(),
    private val listener: OnItemClickListener,
    private val onStudentMenuClickListener: OnStudentMenuClickListener
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StudentViewHolder,
        position: Int
    ) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size

    fun getStudentAt(position: Int): Student?{
        if(position < 0 || position >= students.size){
            return null
        }
        return students[position]
    }
    fun updateStudents(newStudents: List<Student>){
        val diffCallback = StudentDiffUtilCallback(students, newStudents)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        students.clear()
        students.addAll(newStudents)
        diffResult.dispatchUpdatesTo(this)
    }
    inner class StudentViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(student: Student){
            student?.let{
                val imageResource = when (it.gender) {
                    Gender.MALE -> R.drawable.ic_man
                    Gender.FEMALE -> R.drawable.ic_woman
                    Gender.OTHER -> R.drawable.ic_other_sex
                    else -> R.drawable.ic_other_sex
                }
                binding.ivAvatar.setImageResource(imageResource)
                binding.tvStudentId.text = it.id
                binding.tvFullName.text = it.fullName.toString()
                binding.tvGpa.text = it.gpa.toString()
                binding.btnOptionMenu.setOnClickListener {
                    showOptionMenu(student)
                }
                binding.root.setOnClickListener {
                    listener.onItemClick(student)
                }
                binding.root.setOnLongClickListener {
                    showOptionMenu(student)
                    true
                }
            }
        }
        private fun showOptionMenu(student: Student){
            val popupMenu = PopupMenu(binding.root.context, binding.btnOptionMenu)
            popupMenu.inflate(R.menu.student_item_menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when(item.itemId){
                    R.id.menu_edit -> onStudentMenuClickListener.onEdit(student)
                    R.id.menu_delete -> onStudentMenuClickListener.onDelete(student)
                    R.id.menu_view_detail -> onStudentMenuClickListener.onViewDetail(student)
                }
                true
            }
            popupMenu.show()
        }
    }
    class StudentDiffUtilCallback(
        private val oldList: List<Student>,
        private val newList: List<Student>
    ): DiffUtil.Callback(){
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(p0: Int, p1: Int): Boolean {
           return oldList[p0].id == newList[p1].id
        }

        override fun areContentsTheSame(p0: Int, p1: Int): Boolean {
            return oldList[p0] == newList[p1]
        }

    }
    interface OnItemClickListener {
        fun onItemClick(student: Student)
    }
}