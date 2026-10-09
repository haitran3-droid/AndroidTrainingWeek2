package com.example.fragmentexercise.ui.studentform

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.ArrayRes
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.example.fragmentexercise.R
import com.example.fragmentexercise.app.StudentApp
import com.example.fragmentexercise.common.DateUtils
import com.example.fragmentexercise.common.DateUtils.EXTRA_FORM_MODE
import com.example.fragmentexercise.common.DateUtils.EXTRA_STUDENT_ID
import com.example.fragmentexercise.data.model.Gender
import com.example.fragmentexercise.databinding.FragmentStudentFormBinding
import com.example.fragmentexercise.ui.dialog.ConfirmDialogFragment
import com.google.android.material.snackbar.Snackbar

class StudentFormFragment : Fragment() {

    // ViewBinding pattern chuẩn để tránh ?. lặp lại
    private var _binding: FragmentStudentFormBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StudentFormViewModel by viewModels {
        val repository = (requireActivity().application as StudentApp).studentRepository
        StudentFormViewModelFactory(repository)
    }

    private var formMode: FormMode = FormMode.ADD

    // Debounce validate
    private val validateHandler = Handler(Looper.getMainLooper())
    private var validateRunnable: Runnable? = null
    private var isPopulating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStudentFormBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        extractArgs()
        setupSpinners()
        setupToolbar()
        setupButtons()
        setupRealtimeValidation()
        observeValidationAndResult()
        fillStudentDataIfEdit()
        setupDialogResultListener()
    }

    private fun extractArgs() {
        formMode = arguments?.getString(EXTRA_FORM_MODE)
            ?.let { FormMode.valueOf(it) } ?: FormMode.ADD

        arguments?.getString(EXTRA_STUDENT_ID)?.let { id ->
            viewModel.getStudent(id)
        }
    }

    private fun setupSpinners() = with(binding) {
        spinnerAddress.adapter = createSimpleAdapter(R.array.city_array)
        spinnerMajor.adapter = createSimpleAdapter(R.array.major_array)
        spinnerYear.adapter = createSimpleAdapter(R.array.year_array)

        if (formMode == FormMode.UPDATE) {
            tilStudentId.isEnabled = false
            toolbarAddStudentScreen.title = getString(R.string.title_edit_student)
        }
    }

    private fun createSimpleAdapter(@ArrayRes arrayRes: Int): ArrayAdapter<CharSequence> =
        ArrayAdapter.createFromResource(
            requireContext(),
            arrayRes,
            android.R.layout.simple_spinner_item
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

    private fun setupToolbar() = with(binding) {
        toolbarAddStudentScreen.setNavigationOnClickListener {
            setResultAndPopBack()
        }
    }

    private fun setupButtons() = with(binding) {
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }

        btnSubmit.setOnClickListener {
            val valid = viewModel.validationState.value?.isSuccess == true
            if (!valid) {
                Snackbar.make(root, R.string.error_invalid_data, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (formMode == FormMode.ADD) {
                submit() // ADD trực tiếp
            } else {
                // todo: hiển thị dialog yêu cầu xác nhận update
                ConfirmDialogFragment.newInstance(
                    R.string.confirm_update_title,
                    R.string.confirm_update_message,
                    viewModel.student.value?.id ?: ""
                ).show(parentFragmentManager, "ConfirmDialogFragment")
            }
        }
    }

    private fun requestValidate(delay: Long = 400L) {
        if (isPopulating) return
        validateRunnable?.let(validateHandler::removeCallbacks)
        validateRunnable = Runnable { validateData() }
        validateHandler.postDelayed(validateRunnable!!, delay)
    }

    private fun setupRealtimeValidation() = with(binding) {
        // gắn cho tất cả EditText cần validate
        listOf(etStudentId, etFullName, etBirthDate, etEmail, etGpa)
            .forEach { it.addTextChangedListener { requestValidate() } }
    }

    private fun observeValidationAndResult() {
        viewModel.validationState.observe(viewLifecycleOwner) { state ->
            binding.tilStudentId.error = state.idError?.let(::getString)
            binding.tilFullName.error = state.fullNameError?.let(::getString)
            binding.tilBirthDate.error = state.birthDateError?.let(::getString)
            binding.tilEmail.error = state.emailError?.let(::getString)
            binding.tilGpa.error = state.gpaError?.let(::getString)
        }

        // Gộp xử lý kết quả add/update
        viewModel.isStudentAdded.observe(viewLifecycleOwner) {
            observeResult(it, R.string.add_student_success)
        }
        viewModel.isStudentUpdated.observe(viewLifecycleOwner) {
            observeResult(it, R.string.update_student_success)
        }
    }

    private fun observeResult(success: Boolean, successMsgRes: Int) {
        setResult(success)
        if (success) {
            Snackbar.make(binding.root, successMsgRes, Snackbar.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }
    }

    private fun setResult(success: Boolean) {
        setFragmentResult(
            REQUEST_KEY,
            Bundle().apply {
                putString(RESULT_KEY_STATUS, if (success) STATUS_SUCCESS else STATUS_FAIL)
            }
        )
    }

    private fun setResultAndPopBack(success: Boolean = false) {
        setResult(success)
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun fillStudentDataIfEdit() {
        viewModel.student.observe(viewLifecycleOwner) { student ->
            if (student == null) return@observe
            isPopulating = true
            binding.apply {
                etStudentId.setText(student.id)
                etFullName.setText(student.fullName.toString())
                etBirthDate.setText(DateUtils.dateToString(student.birthDate))
                etEmail.setText(student.email)
                etGpa.setText(student.gpa.toString())

                when (student.gender) {
                    Gender.MALE -> rbItemMale.isChecked = true
                    Gender.FEMALE -> rbItemFemale.isChecked = true
                    else -> rbItemOther.isChecked = true
                }

                val cities = resources.getStringArray(R.array.city_array)
                val majors = resources.getStringArray(R.array.major_array)
                val years = resources.getStringArray(R.array.year_array)

                spinnerAddress.setSelection(cities.indexOf(student.address).coerceAtLeast(0))
                spinnerMajor.setSelection(majors.indexOf(student.major).coerceAtLeast(0))
                spinnerYear.setSelection(years.indexOf(student.year.toString()).coerceAtLeast(0))
            }
            isPopulating = false
        }
    }

    // ---------------- Dialog result (UPDATE) ----------------

    private fun setupDialogResultListener() {
        parentFragmentManager.setFragmentResultListener(
            ConfirmDialogFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ -> submit() /* user confirmed update */ }
    }

    // ---------------- Validation & Submit ----------------

    private fun validateData() {
        val f = collectForm()
        viewModel.validateNormalData(
            id = f.id,
            fullName = f.fullName,
            birthDate = f.birthDate,
            email = f.email,
            gpa = f.gpa,
            formMode = formMode
        )
    }

    /** Đọc toàn bộ dữ liệu form 1 lần */
    private fun collectForm(): FormInput = with(binding) {
        FormInput(
            id = etStudentId.text.toString().uppercase(),
            fullName = etFullName.text.toString(),
            birthDate = etBirthDate.text.toString(),
            email = etEmail.text.toString(),
            gpa = etGpa.text.toString(),
            address = spinnerAddress.selectedItem?.toString().orEmpty(),
            major = spinnerMajor.selectedItem?.toString().orEmpty(),
            year = spinnerYear.selectedItem?.toString().orEmpty(),
            gender = getGenderText()
        )
    }

    /** Submit theo formMode, không lặp code đọc form */
    private fun submit() {
        val f = collectForm()
        if (formMode == FormMode.ADD) {
            viewModel.submitAdd(
                id = f.id,
                fullName = f.fullName,
                birthDate = f.birthDate,
                email = f.email,
                gpa = f.gpa,
                address = f.address,
                major = f.major,
                year = f.year,
                gender = f.gender
            )
        } else {
            viewModel.submitUpdate(
                id = f.id,
                fullName = f.fullName,
                birthDate = f.birthDate,
                email = f.email,
                gpa = f.gpa,
                address = f.address,
                major = f.major,
                year = f.year,
                gender = f.gender
            )
        }
    }

    private fun getGenderText(): String? = when (binding.rbGenderGroup.checkedRadioButtonId) {
        R.id.rb_item_male -> binding.rbItemMale.text.toString()
        R.id.rb_item_female -> binding.rbItemFemale.text.toString()
        R.id.rb_item_other -> binding.rbItemOther.text.toString()
        else -> null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class FormInput(
        val id: String,
        val fullName: String,
        val birthDate: String,
        val email: String,
        val gpa: String,
        val address: String,
        val major: String,
        val year: String,
        val gender: String?
    )

    companion object {
        const val REQUEST_KEY = "student_form_request_key"
        const val RESULT_KEY_STATUS = "result_status"
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAIL = "fail"
    }
}