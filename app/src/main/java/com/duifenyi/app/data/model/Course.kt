package com.duifenyi.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 课程信息
 */
data class Course(
    @SerializedName("CourseName") val courseName: String = "",
    @SerializedName("TClassID") val tclassId: String = "",
    @SerializedName("CourseID") val courseId: String = "",
    @SerializedName("ClassName") val className: String = "",
    @SerializedName("TeacherName") val teacherName: String = ""
)
