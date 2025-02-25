package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var categories = listOf<CategoryUiData>()
    private var tasks = listOf<TaskUiData>()

    private val categoryAdapter = CategoryListAdapter()
    private val taskAdapter by lazy {
        TaskListAdapter()
    }


    val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            TaskBeatDataBase::class.java, "database-Task_Beat"
        ).build()
    }

    val categoryDao by lazy {
        db.getCategoryDao()
    }

    val taskdao by lazy {
        db.getTaskDao()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rvCategory = findViewById<RecyclerView>(R.id.rv_categories)
        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks)
        val fabCreateTask = findViewById<FloatingActionButton>(R.id.fab_create_task)

        fabCreateTask.setOnClickListener {
            showCreateOrUpdateTaskBottomSheet()
        }

        taskAdapter.setOnClickListener {
            showCreateOrUpdateTaskBottomSheet()
        }

        categoryAdapter.setOnClickListener { selected ->
            if (selected.name == "+") {
                val createCategoryBottomSheet = CreateCategoryBottomSheet { categoryName ->
                    val categoryEntity = CategoryEntity(
                        name = categoryName,
                        isSelected = false
                    )
                    insertCategory(categoryEntity)
                }

                createCategoryBottomSheet.show(supportFragmentManager, "createCategoryBottomSheet")
            } else {
                val categoryTemp = categories.map { item ->
                    when {
                        item.name == selected.name && !item.isSelected -> item.copy(isSelected = true)
                        item.name == selected.name && item.isSelected -> item.copy(isSelected = false)
                        else -> item
                    }
                }

                val taskTemp =
                    if (selected.name != "ALL") {
                        tasks.filter { it.category == selected.name }
                    } else {
                        tasks
                    }
                taskAdapter.submitList(taskTemp)


                categoryAdapter.submitList(categoryTemp)
            }

        }

        rvCategory.adapter = categoryAdapter
        GlobalScope.launch(Dispatchers.Main) {
            getCategoriesFromDataBase()
        }



        rvTask.adapter = taskAdapter
        GlobalScope.launch(Dispatchers.Main) {
            getTasksFromDataBase()
        }
    }

    private fun getCategoriesFromDataBase() {
        GlobalScope.launch(Dispatchers.IO) {
            val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
            val categoriesUiData = categoriesFromDb.map {
                CategoryUiData(
                    name = it.name,
                    isSelected = it.isSelected
                )
            }.toMutableList()

            categoriesUiData.add(
                CategoryUiData(
                    name = "+",
                    isSelected = false
                )
            )
            GlobalScope.launch(Dispatchers.Main) {
                categories = categoriesUiData
                categoryAdapter.submitList(categoriesUiData)
                getCategoriesFromDataBase()
            }
        }

    }

    private fun getTasksFromDataBase() {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksFromDb: List<TaskEntity> = taskdao.getAll()
            val tasksUiData: List<TaskUiData> = tasksFromDb.map {
                TaskUiData(
                    id = it.id,
                    name = it.name,
                    category = it.category
                )
            }
            GlobalScope.launch(Dispatchers.Main) {
                tasks = tasksUiData
                taskAdapter.submitList(tasksUiData)
            }
        }

    }

    private fun insertCategory(categoryEntity: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            categoryDao.insert(categoryEntity)
        }

    }

    private fun insertTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskdao.insert(taskEntity)
            getTasksFromDataBase()
        }
    }

    private fun showCreateOrUpdateTaskBottomSheet() {
        val createTaskBottomSheet = CreateOrUpdateTaskBottomSheet(
            categories
        ) { taskToBeCreated ->
            val tasEntityToBeCreatedInsert = TaskEntity(
                name = taskToBeCreated.name,
                category = taskToBeCreated.category
            )
            insertTask(tasEntityToBeCreatedInsert)
        }
        createTaskBottomSheet.show(supportFragmentManager, "createTaskBottomSheet")

    }

}