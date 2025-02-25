package com.devspace.taskbeats

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class CreateOrUpdateTaskBottomSheet(
    private val categoryList: List<CategoryUiData>,
    private val task: TaskUiData? = null,
    private val onCreateClicked: (TaskUiData) -> Unit,
    private val onUpdateClicked: (TaskUiData) -> Unit,
    private val onDeleteClicked: (TaskUiData) -> Unit

) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.create_or_update_task_bottom_sheet, container, false)

        val tvTitle = view.findViewById<TextView>(R.id.tv_title_create_task)
        val btnCreatOrUdapte = view.findViewById<Button>(R.id.btn_task_create_or_update)
        val btnDelete = view.findViewById<Button>(R.id.btn_task_delete)
        val tieTaskname = view.findViewById<TextInputEditText>(R.id.tie_task_creat)
        val spinner: Spinner = view.findViewById(R.id.category_list)

        var taskCategory: String? = null
        val categoryStr: List<String> = categoryList.map { it.name }

        ArrayAdapter(
            requireActivity().baseContext,
            android.R.layout.simple_spinner_item,
            categoryStr.toList()
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                taskCategory = categoryStr[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        if (task == null) {
            tvTitle.setText(R.string.creat_task_title)
            btnCreatOrUdapte.setText(R.string.creat)
            btnDelete.isVisible = false
        } else {
            tvTitle.setText(R.string.update_task_title)
            btnCreatOrUdapte.setText(R.string.update)
            btnDelete.isVisible = true
            tieTaskname.setText(task.name)

            val currentcategory = categoryList.firstOrNull{it.name == task.category}
            val index = categoryList.indexOf(currentcategory)
            spinner.setSelection(index)
        }

        btnDelete.setOnClickListener {
            if (task != null){
                onDeleteClicked.invoke(task)
            } else {
                Log .d("CreateOrUpdateTaskBottomSheet", "Task Not Found")
            }
            dismiss()
        }

        btnCreatOrUdapte.setOnClickListener {
            val name = tieTaskname.text.toString().trim()
            if (taskCategory != null && name.isNotEmpty()) {
                if (task == null){
                    onCreateClicked.invoke(
                        TaskUiData(
                            id = 0,
                            name = name,
                            category = requireNotNull(taskCategory)
                        )
                    )
                } else {
                    onUpdateClicked.invoke(
                       TaskUiData(
                           id = task.id,
                           name = name,
                           category = requireNotNull(taskCategory)
                       )
                    )
                }

                dismiss()
            } else {
                Snackbar.make(btnCreatOrUdapte, "Please select a category", Snackbar.LENGTH_LONG)
                    .show()
            }
        }
        return view
    }
}