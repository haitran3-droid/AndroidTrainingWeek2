package pro.branium.recyclerviewex.ui.detail

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import pro.branium.recyclerviewex.R
import pro.branium.recyclerviewex.app.StudentApp
import pro.branium.recyclerviewex.data.model.Gender
import pro.branium.recyclerviewex.databinding.ActivityStudentDetailsBinding
import pro.branium.recyclerviewex.ui.studentform.FormMode
import pro.branium.recyclerviewex.ui.studentform.StudentFormActivity
import pro.branium.recyclerviewex.utils.Utils
import pro.branium.recyclerviewex.utils.Utils.EXTRA_FORM_MODE
import pro.branium.recyclerviewex.utils.Utils.EXTRA_STUDENT_ID
import kotlin.jvm.java

class StudentDetailsActivity : AppCompatActivity() {
    var isUpdate = false
    private val viewModel: StudentDetailViewModel by viewModels {
        val repository = (application as StudentApp).studentRepository
        val factory = StudentDetailViewModel.Factory(repository)
        factory
    }

    private val editStudentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Snackbar.make(
                    binding.root,
                    R.string.update_student_success,
                    Snackbar.LENGTH_SHORT
                ).show()
                viewModel.getStudent(intent.getStringExtra(EXTRA_STUDENT_ID) ?: "")
                isUpdate = true
            }
        }
    private lateinit var binding: ActivityStudentDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityStudentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        extractData()
        setupViews()
        observeData()
    }

    private fun extractData() {
        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)
        if (studentId != null) {
            viewModel.getStudent(studentId)
        }
    }

    private fun setupViews() {
        binding.toolbarStudentDetailScreen.setNavigationOnClickListener {
            if (isUpdate) {
                setResult(RESULT_OK)
            }
            onBackPressedDispatcher.onBackPressed()
        }
        binding.btnEdit.setOnClickListener {
            val navigateIntent = Intent(this, StudentFormActivity::class.java).apply {
                putExtra(EXTRA_STUDENT_ID, viewModel.student.value?.id)
                putExtra(EXTRA_FORM_MODE, FormMode.EDIT.name)
            }
            editStudentLauncher.launch(navigateIntent)
        }
    }

    private fun observeData() {
        viewModel.student.observe(this) { student ->
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
                    Utils.dateToString(student.birthDate)
                )
                binding.tvMajorDetail.text = getString(R.string.text_major_detail, student.major)
                binding.tvYearDetail.text =
                    getString(R.string.text_year_detail, student.year)
            }
        }
    }
}
