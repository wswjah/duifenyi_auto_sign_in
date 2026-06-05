package com.duifenyi.app.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.duifenyi.app.data.model.Course
import com.duifenyi.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseSelector(
    courses: List<Course>,
    selectedCourse: Course?,
    onCourseSelected: (Course) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = "课程选择",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedCourse?.courseName ?: (if (courses.isEmpty()) "请先登录" else "请选择课程"),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceVariant,
                    unfocusedContainerColor = SurfaceVariant,
                    unfocusedBorderColor = BorderColor,
                    focusedBorderColor = Primary
                ),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                courses.forEach { course ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = course.courseName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (course.teacherName.isNotEmpty()) {
                                    Text(
                                        text = course.teacherName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCourseSelected(course)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
