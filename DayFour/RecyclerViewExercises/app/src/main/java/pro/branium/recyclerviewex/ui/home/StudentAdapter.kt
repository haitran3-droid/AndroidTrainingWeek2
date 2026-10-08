package pro.branium.recyclerviewex.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import pro.branium.recyclerviewex.R
import pro.branium.recyclerviewex.data.model.Gender
import pro.branium.recyclerviewex.data.model.Student
import pro.branium.recyclerviewex.databinding.ItemStudentBinding

class StudentAdapter(
    private val students: MutableList<Student> = mutableListOf(),
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {
    override fun onCreateViewHolder(
        p0: ViewGroup,
        p1: Int
    ): StudentViewHolder {
        val layoutInflater = LayoutInflater.from(p0.context)
        val binding = ItemStudentBinding.inflate(layoutInflater, p0, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StudentViewHolder,
        position: Int
    ) {
        val student = students[position]
        holder.bind(student)
    }

    override fun getItemCount(): Int {
        return students.size
    }

    fun updateStudents(newStudents: List<Student>) {
        val diffcCallback = StudentDiffCallback(students, newStudents)
        val diffResult = DiffUtil.calculateDiff(diffcCallback)
        students.clear()
        students.addAll(newStudents)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class StudentViewHolder(private val binding: ItemStudentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(student: Student?) {
            student?.let {
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
                binding.root.setOnClickListener {
                    listener.onItemClick(student)
                }
                binding.root.setOnLongClickListener {
                    listener.onItemLongClick(student)
                    true
                }
            }
        }
    }

    class StudentDiffCallback(
        private val oldList: List<Student>,
        private val newList: List<Student>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    interface OnItemClickListener {
        fun onItemClick(student: Student)
        fun onItemLongClick(student: Student)
    }
}