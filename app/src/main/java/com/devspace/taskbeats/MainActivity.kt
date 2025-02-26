package com.devspace.taskbeats

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var categories = listOf<CategoryUiData>()
    private var categoriesEntities = listOf<CategoryEntity>()
    private var tasks = listOf<TaskUiData>()
    private lateinit var rvCategory: RecyclerView
    private lateinit var ctnEmptyView: LinearLayout
    private lateinit var fabCreateTask: FloatingActionButton

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

        val rvTask = findViewById<RecyclerView>(R.id.rv_tasks)
        val btnCreateEmpty = findViewById<Button>(R.id.btn_create_empty)

        rvCategory = findViewById(R.id.rv_categories)
        ctnEmptyView = findViewById(R.id.ll_empty_view)
        fabCreateTask = findViewById(R.id.fab_create_task)


        btnCreateEmpty.setOnClickListener {
            showCreateCategoryBottomSheet()
        }

        fabCreateTask.setOnClickListener {
            showCreateOrUpdateTaskBottomSheet()
        }

        taskAdapter.setOnClickListener { task ->
            showCreateOrUpdateTaskBottomSheet(task)
        }

        categoryAdapter.setOnLongClickListener { categoryToBeDeleted ->
            if (categoryToBeDeleted.name != "+" && categoryToBeDeleted.name != "ALL") {
                val title: String = this.getString(R.string.category_delete_title)
                val description: String = this.getString(R.string.category_delete_description)
                val btnText: String = this.getString(R.string.delete)

                showinfobottomsheet(
                    title,
                    description,
                    btnText
                ) {
                    val categoryEntityToBeDeleted = CategoryEntity(
                        name = categoryToBeDeleted.name,
                        isSelected = categoryToBeDeleted.isSelected
                    )
                    deleteCategory(categoryEntityToBeDeleted)
                }
            }

        }

        categoryAdapter.setOnClickListener { selected ->
            if (selected.name == "+") {
                showCreateCategoryBottomSheet()
            } else {
                val categoryTemp = categories.map { item ->
                    item.copy(isSelected = item.name == selected.name)
                }


                if (selected.name != "ALL") {
                    filterTaskByCategoryName(selected.name)
                } else {
                    getCategoriesFromDataBase()
                }

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

    private fun showinfobottomsheet(
        title: String,
        description: String,
        btnText: String,
        onClick: () -> Unit
    ) {
        val infoBottomSheet = InfoBottomSheet(
            title = title,
            description = description,
            btnText = btnText,
            onClick
        )

        infoBottomSheet.show(
            supportFragmentManager,
            "infoBottomSheet"
        )

    }

    private fun getCategoriesFromDataBase() {
        GlobalScope.launch(Dispatchers.IO) {
            val categoriesFromDb: List<CategoryEntity> = categoryDao.getAll()
            categoriesEntities = categoriesFromDb

            GlobalScope.launch(Dispatchers.Main) {
                if (categoriesEntities.isEmpty()){
                    rvCategory.isVisible = false
                    ctnEmptyView.isVisible = true
                    fabCreateTask.isVisible = false
                } else {
                    rvCategory.isVisible = true
                    ctnEmptyView.isVisible = false
                    fabCreateTask.isVisible = true
                }
            }


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
            val categoryTemList = mutableListOf(
                CategoryUiData(
                    name = "ALL",
                    isSelected = true,
                )
            )
            categoryTemList.addAll(categoriesUiData)
            GlobalScope.launch(Dispatchers.Main) {
                categories = categoryTemList
                categoryAdapter.submitList(categories)
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

    private fun deleteCategory(categoryEntity: CategoryEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksToBeDeleted = taskdao.getAllByCategoryName(categoryEntity.name)
            taskdao.deleteAll(tasksToBeDeleted)
            categoryDao.delete(categoryEntity)
            getTasksFromDataBase()
        }
    }

    private fun filterTaskByCategoryName(category: String) {
        GlobalScope.launch(Dispatchers.IO) {
            val tasksFromDb: List<TaskEntity> = taskdao.getAllByCategoryName(category)
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

    private fun insertTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskdao.insert(taskEntity)
            getTasksFromDataBase()
        }
    }

    private fun updateTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskdao.update(taskEntity)
            getTasksFromDataBase()

        }
    }

    private fun deleteTask(taskEntity: TaskEntity) {
        GlobalScope.launch(Dispatchers.IO) {
            taskdao.delete(taskEntity)
            getTasksFromDataBase()

        }
    }

    private fun showCreateOrUpdateTaskBottomSheet(taskUiData: TaskUiData? = null) {
        val createTaskBottomSheet = CreateOrUpdateTaskBottomSheet(
            task = taskUiData,
            categoryList = categoriesEntities,
            onCreateClicked = { taskToBeCreated ->
                val taskEntityToBeCreatedInsert = TaskEntity(
                    name = taskToBeCreated.name,
                    category = taskToBeCreated.category
                )
                insertTask(taskEntityToBeCreatedInsert)

            },
            onUpdateClicked = { taskToBeUpdated ->
                val taskEntityToBeUpdate = TaskEntity(
                    id = taskToBeUpdated.id,
                    name = taskToBeUpdated.name,
                    category = taskToBeUpdated.category
                )
                updateTask(taskEntityToBeUpdate)
            },
            onDeleteClicked = { taskTobeDeleted ->
                val taskEntityToBeDelete = TaskEntity(
                    id = taskTobeDeleted.id,
                    name = taskTobeDeleted.name,
                    category = taskTobeDeleted.category
                )
                deleteTask(taskEntityToBeDelete)
            }
        )
        createTaskBottomSheet.show(supportFragmentManager, "createTaskBottomSheet")

    }

    private fun showCreateCategoryBottomSheet() {
        val createCategoryBottomSheet = CreateCategoryBottomSheet { categoryName ->
            val categoryEntity = CategoryEntity(
                name = categoryName,
                isSelected = false
            )
            insertCategory(categoryEntity)
        }

        createCategoryBottomSheet.show(supportFragmentManager, "createCategoryBottomSheet")
    }

}