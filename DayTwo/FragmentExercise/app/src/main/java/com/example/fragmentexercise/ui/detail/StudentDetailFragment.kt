package com.example.fragmentexercise.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.fragmentexercise.R
import com.example.fragmentexercise.app.StudentApp
import com.example.fragmentexercise.common.DateUtils
import com.example.fragmentexercise.common.DateUtils.EXTRA_FORM_MODE
import com.example.fragmentexercise.common.DateUtils.EXTRA_STUDENT_ID
import com.example.fragmentexercise.data.model.Gender
import com.example.fragmentexercise.databinding.FragmentStudentDetailBinding
import com.example.fragmentexercise.ui.studentform.FormMode
import com.example.fragmentexercise.ui.studentform.StudentFormFragment
import com.google.android.material.snackbar.Snackbar
import kotlin.jvm.java


class StudentDetailFragment : Fragment() {
    private val viewModel: StudentDetailViewModel by viewModels {
        val repository = (requireActivity().application as StudentApp).studentRepository
        val factory = StudentDetailViewModelFactory(repository)
        factory
    }
    private var _binding: FragmentStudentDetailBinding? = null
    private val binding get() = _binding!!
    var isUpdate = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStudentDetailBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareStudentData()
        initViews()
        observeStudentData()
        setupFragmentResultListeners()
    }

    private fun prepareStudentData() {
        val studentId = arguments?.getString(EXTRA_STUDENT_ID)
        if (studentId != null) {
            viewModel.getStudent(studentId)
        }
    }

    private fun initViews() {
        binding.toolbarStudentDetailScreen.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply {
                putString(EXTRA_STUDENT_ID, viewModel.student.value?.id)
                putString(EXTRA_FORM_MODE, FormMode.UPDATE.name)
            }
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(
                    R.id.fcv_student,
                    StudentFormFragment::class.java,
                    bundle
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeStudentData() {
        viewModel.student.observe(viewLifecycleOwner) { student ->
            if (student != null) {
                val avatar = when (student.gender) {
                    Gender.MALE -> R.drawable.ic_man
                    Gender.FEMALE -> R.drawable.ic_woman
                    else -> R.drawable.ic_other_sex
                }
                binding.ivAvatarDetail.setImageResource(avatar)
                binding.tvIdDetail.text = getString(R.string.text_id_detail, student.id)
                binding.tvFullNameDetail.text =
                    getString(R.string.text_full_name_detail, student.fullName)
                binding.tvEmailDetail.text = getString(R.string.text_email_detail, student.email)
                binding.tvGenderDetail.text =
                    getString(R.string.text_gender_detail, student.gender?.value)
                binding.tvAddressDetail.text =
                    getString(R.string.text_address_detail, student.address)
                binding.tvGpaDetail.text =
                    getString(R.string.text_gpa_detail, student.gpa.toString())
                binding.tvBirthDateDetail.text = getString(
                    R.string.text_birth_date_detail,
                    DateUtils.dateToString(student.birthDate)
                )
                binding.tvMajorDetail.text = getString(R.string.text_major_detail, student.major)
                binding.tvYearDetail.text =
                    getString(R.string.text_year_detail, student.year)
            }
        }
    }

    private fun setupFragmentResultListeners() {
        parentFragmentManager.setFragmentResultListener(
            StudentFormFragment.Companion.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val updateStatus = bundle.getString(StudentFormFragment.Companion.RESULT_KEY_STATUS)
            if (updateStatus == StudentFormFragment.Companion.STATUS_SUCCESS) {
                Snackbar.make(
                    binding.root,
                    R.string.update_student_success,
                    Snackbar.LENGTH_SHORT
                ).show()
                isUpdate = true
            } else {
                isUpdate = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
