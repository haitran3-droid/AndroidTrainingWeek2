package pro.branium.recyclerviewex.ui.studentform

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.RadioButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import pro.branium.recyclerviewex.R
import pro.branium.recyclerviewex.app.StudentApp
import pro.branium.recyclerviewex.data.model.Gender
import pro.branium.recyclerviewex.databinding.ActivityStudentFormBinding
import pro.branium.recyclerviewex.utils.Utils
import pro.branium.recyclerviewex.utils.Utils.EXTRA_FORM_MODE
import pro.branium.recyclerviewex.utils.Utils.EXTRA_STUDENT_ID

class StudentFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStudentFormBinding
    private val viewModel: StudentFormViewModel by viewModels {
        val repository = (application as StudentApp).studentRepository
        val factory = StudentFormViewModel.Factory(repository)
        factory
    }
    private var formMode: FormMode = FormMode.ADD

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStudentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        extractData()
        setupView()
        setupButtonClickListener()
        setupObserver()
        fillStudentData()
    }

    private fun extractData() {
        val formModeStr = intent.getStringExtra(EXTRA_FORM_MODE)
        formMode = FormMode.valueOf(formModeStr ?: FormMode.ADD.name)
        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)
        if (studentId != null) {
            viewModel.getStudent(studentId)
        }
    }

    private fun setupView() {
        val addressAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.city_array,
            android.R.layout.simple_spinner_item
        )
        val majorAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.major_array,
            android.R.layout.simple_spinner_item
        )
        val yearAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.year_array,
            android.R.layout.simple_spinner_item
        )
        binding.spinnerAddress.adapter = addressAdapter
        binding.spinnerMajor.adapter = majorAdapter
        binding.spinnerYear.adapter = yearAdapter
        if (formMode == FormMode.EDIT) {
            binding.tilStudentId.isEnabled = false
            binding.toolbarAddStudentScreen.title = getString(R.string.title_edit_student)
        }
    }

    private fun setupButtonClickListener() {
        binding.toolbarAddStudentScreen.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnSubmit.setOnClickListener {
            val id = binding.etStudentId.text.toString()
            val fullName = binding.etFullName.text.toString()
            val birthDate = binding.etBirthDate.text.toString()
            val email = binding.etEmail.text.toString()
            val gpa = binding.etGpa.text.toString()
            val address = binding.spinnerAddress.selectedItem.toString()
            val major = binding.spinnerMajor.selectedItem.toString()
            val year = binding.spinnerYear.selectedItem.toString()
            val gender = getGender()
            viewModel.validateNormalData(
                id,
                fullName,
                birthDate,
                email,
                gpa,
                address,
                major,
                year,
                gender,
                formMode
            )
        }
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupObserver() {
        viewModel.validationState.observe(this) { state ->
            binding.tilStudentId.error = state.idError?.let { getString(it) }
            binding.tilFullName.error = state.fullNameError?.let { getString(it) }
            binding.tilBirthDate.error = state.birthDateError?.let { getString(it) }
            binding.tilEmail.error = state.emailError?.let { getString(it) }
            binding.tilGpa.error = state.gpaError?.let { getString(it) }
        }
        viewModel.isStudentUpdated.observe(this) { state ->
            if (state) {
                val messageId = if (formMode == FormMode.ADD) {
                    R.string.add_student_success
                } else {
                    R.string.update_student_success
                }
                Snackbar.make(
                    binding.root,
                    messageId,
                    Snackbar.LENGTH_SHORT
                ).show()
                if (formMode == FormMode.ADD) {
                    setResult(RESULT_OK)
                    clearForm()
                } else {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }

    private fun fillStudentData() {
        viewModel.student.observe(this) { student ->
            if (student != null) {
                binding.etStudentId.setText(student.id)
                binding.etFullName.setText(student.fullName.toString())
                binding.etBirthDate.setText(Utils.dateToString(student.birthDate))
                binding.etEmail.setText(student.email)
                binding.etGpa.setText(student.gpa.toString())
                when (student.gender) {
                    Gender.MALE -> binding.rbItemMale.isChecked = true
                    Gender.FEMALE -> binding.rbItemFemale.isChecked = true
                    else -> binding.rbItemOther.isChecked = true
                }
                // thiết lập cho spinner:
                val cities = resources.getStringArray(R.array.city_array)
                val major = resources.getStringArray(R.array.major_array)
                val years = resources.getStringArray(R.array.year_array)

                val addressPos = cities.indexOf(student.address)
                val majorPos = major.indexOf(student.major)
                val yearPos = years.indexOf(student.year.toString())

                val addressPosition = if (addressPos != -1) addressPos else 0
                val majorPosition = if (majorPos != -1) majorPos else 0
                val yearPosition = if (yearPos != -1) yearPos else 0

                binding.spinnerAddress.setSelection(addressPosition)
                binding.spinnerMajor.setSelection(majorPosition)
                binding.spinnerYear.setSelection(yearPosition)
            }
        }
    }

    private fun getGender(): String? {
        val selectedId = binding.rbGenderGroup.checkedRadioButtonId
        return if (selectedId != -1) {
            val radioButton = findViewById<RadioButton>(selectedId)
            val rbText = radioButton.text.toString()
            rbText
        } else {
            null
        }
    }

    private fun clearForm() {
        binding.etStudentId.text.clear()
        binding.etFullName.text.clear()
        binding.etBirthDate.text.clear()
        binding.etEmail.text.clear()
        binding.etGpa.text.clear()
        binding.rbGenderGroup.clearCheck()
    }
}
