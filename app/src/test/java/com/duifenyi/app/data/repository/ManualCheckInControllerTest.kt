package com.duifenyi.app.data.repository

import com.duifenyi.app.data.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualCheckInControllerTest {
    private val course = Course(courseId = "course", tclassId = "class", courseName = "测试课程")
    private val pending = CheckInRecord(id = "123", checkInType = "1", canApply = "1")
    private val grid = listOf("亍", "偻", "梏", "荥", "箧", "氤", "瓒", "佝", "隗").map { HanziOption(it, "") }

    @Test fun waitsForInputAndPreservesLeadingZero() = runTest {
        var submitted = ""
        var latest = pending
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> latest }) { _, _, code ->
            submitted = code
            latest = pending.copy(statusID = "1")
            "已提交"
        }
        val job = async { controller.run(course, pending, "test-user") }
        runCurrent()
        assertEquals("", submitted)
        controller.respond(controller.state.value!!.requestId, ManualCheckInAction.Submit("0123"))
        runCurrent()
        assertEquals("0123", submitted)
        assertEquals(ManualCheckInResult.CONFIRMED, job.await())
        assertNull(controller.state.value)
    }

    @Test fun failedSubmissionStaysOpenAndCanRetryWithoutFalseSuccess() = runTest {
        var calls = 0
        var latest = pending
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> latest }) { _, _, _ ->
            calls++
            if (calls == 2) latest = pending.copy(statusName = "出勤")
            "未签到成功"
        }
        val job = async { controller.run(course, pending, "test-user") }
        runCurrent()
        val requestId = controller.state.value!!.requestId
        controller.respond(requestId, ManualCheckInAction.Submit("1234"))
        controller.respond(requestId, ManualCheckInAction.Submit("1234"))
        runCurrent()
        assertEquals(1, calls)
        assertFalse(job.isCompleted)
        assertFalse(controller.state.value!!.isBusy)
        controller.respond(requestId, ManualCheckInAction.Submit("4321"))
        runCurrent()
        assertEquals(ManualCheckInResult.CONFIRMED, job.await())
        assertEquals(2, calls)
    }

    @Test fun endedActivityIsNotSubmitted() = runTest {
        var calls = 0
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> pending.copy(canApply = "0") }) { _, _, _ ->
            calls++; ""
        }
        val job = async { controller.run(course, pending, "test-user") }
        runCurrent()
        controller.respond(controller.state.value!!.requestId, ManualCheckInAction.Submit("1234"))
        runCurrent()
        assertEquals(ManualCheckInResult.EXPIRED, job.await())
        assertEquals(0, calls)
    }

    @Test fun invalidInputAndSkipNeverSubmit() = runTest {
        var calls = 0
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> pending }) { _, _, _ -> calls++; "" }
        val job = async { controller.run(course, pending, "test-user") }
        runCurrent()
        val id = controller.state.value!!.requestId
        controller.respond(id, ManualCheckInAction.Submit("１２３４"))
        runCurrent()
        assertEquals(0, calls)
        assertFalse(job.isCompleted)
        controller.respond(id, ManualCheckInAction.Skip)
        runCurrent()
        assertEquals(ManualCheckInResult.SKIPPED, job.await())
        assertNull(controller.state.value)
    }

    @Test fun gridFailureCanBeRefreshedAndDoesNotSubmitAnAnswer() = runTest {
        var loads = 0
        val record = pending.copy(checkInType = "4")
        val controller = ManualCheckInController({ _, _ ->
            if (++loads == 1) error("暂时无法加载")
            grid
        }, { _, _, _ -> record }) { _, _, _ -> error("不应提交") }
        val job = async { controller.run(course, record, "test-user") }
        runCurrent()
        assertTrue(controller.state.value!!.grid.isEmpty())
        val id = controller.state.value!!.requestId
        controller.respond(id, ManualCheckInAction.RefreshGrid)
        runCurrent()
        assertEquals(grid, controller.state.value!!.grid)
        controller.respond(id, ManualCheckInAction.Skip)
        runCurrent()
        assertEquals(ManualCheckInResult.SKIPPED, job.await())
    }

    @Test fun timeoutClearsDialogWithoutSubmitting() = runTest {
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> pending }) { _, _, _ -> error("不应提交") }
        val job = async { controller.run(course, pending, "test-user") }
        runCurrent()
        advanceTimeBy(30 * 60 * 1000L + 1)
        runCurrent()
        assertEquals(ManualCheckInResult.EXPIRED, job.await())
        assertNull(controller.state.value)
    }

    @Test fun stoppingCancelsWaitingAndRejectsLateInput() = runTest {
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> pending }) { _, _, _ -> error("不应提交") }
        val job = async { controller.run(course, pending, "test-user") }
        runCurrent()
        val id = controller.state.value!!.requestId
        controller.cancel()
        controller.respond(id, ManualCheckInAction.Submit("1234"))
        runCurrent()
        assertTrue(job.isCancelled)
        assertNull(controller.state.value)
    }

    @Test fun validatesHanziByCodePointAndCandidateMembership() {
        assertTrue(ManualCheckInController.isValidCode("隗亍箧荥", true, grid))
        assertFalse(ManualCheckInController.isValidCode("亍亍箧荥", true, grid))
        assertFalse(ManualCheckInController.isValidCode("甲乙丙丁", true, grid))
        assertFalse(ManualCheckInController.isValidCode("隗亍箧", true, grid))
    }

    @Test fun submitsChosenHanziInUserOrderAndConfirmsFromRecord() = runTest {
        val record = pending.copy(checkInType = "4")
        var latest = record
        var submitted = ""
        val controller = ManualCheckInController({ _, _ -> grid }, { _, _, _ -> latest }) { type, _, code ->
            assertEquals("4", type)
            submitted = code
            latest = record.copy(checkInDate = "2026/10/31 10:00:00")
            "已提交"
        }
        val job = async { controller.run(course, record, "test-user") }
        runCurrent()
        assertEquals(grid, controller.state.value!!.grid)
        controller.respond(controller.state.value!!.requestId, ManualCheckInAction.Submit("隗亍箧荥"))
        runCurrent()
        assertEquals("隗亍箧荥", submitted)
        assertEquals(ManualCheckInResult.CONFIRMED, job.await())
    }
}
