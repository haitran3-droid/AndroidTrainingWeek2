package pro.branium.recyclerviewex.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import pro.branium.recyclerviewex.R
import pro.branium.recyclerviewex.app.StudentApp
import pro.branium.recyclerviewex.data.model.Student
import pro.branium.recyclerviewex.databinding.ActivityMainBinding
import pro.branium.recyclerviewex.ui.detail.StudentDetailsActivity
import pro.branium.recyclerviewex.ui.studentform.FormMode
import pro.branium.recyclerviewex.ui.studentform.StudentFormActivity
import pro.branium.recyclerviewex.utils.Utils
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var studentAdapter: StudentAdapter

    private val viewModel: StudentViewModel by viewModels {
        val repository = (application as StudentApp).studentRepository
        val factory = StudentViewModel.Factory(repository)
        factory
    }

    private val addStudentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadStudents()
        }
    }
    private val editStudentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadStudents()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        setupView()
        setupObserver()
    }

    private fun setupView() {
        studentAdapter = StudentAdapter(listener = object : StudentAdapter.OnItemClickListener {
            override fun onItemClick(student: Student) {
                Intent(this@MainActivity, StudentDetailsActivity::class.java).apply {
                    putExtra(Utils.EXTRA_STUDENT_ID, student.id)
                    editStudentLauncher.launch(this)
                }
            }

            override fun onItemLongClick(student: Student) {
                TODO("Not yet implemented")
            }
        })
        val itemDecoration = DividerItemDecoration(this, RecyclerView.VERTICAL)
        ContextCompat.getDrawable(this, R.drawable.divider)?.let{
            itemDecoration.setDrawable(it)
        }
        binding.rvStudent.adapter = studentAdapter
        binding.rvStudent.addItemDecoration(itemDecoration)
        binding.btnAdd.setOnClickListener{
            val intent = Intent(this, StudentFormActivity::class.java)
            intent.putExtra(Utils.EXTRA_FORM_MODE, FormMode.ADD.name)
            addStudentLauncher.launch(intent)
        }
    }

    private fun setupObserver() {
        viewModel.student.observe(this){
            studentAdapter.updateStudents(it)
        }
    }
}