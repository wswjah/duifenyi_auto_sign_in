package com.duifenyi.app.data.network

import org.junit.Assert.*
import org.junit.Test

class CheckInProtocolTest {
    private val gridResponse = """
        {"msg":1,"checkintype":4,"ischeckin":0,"checkinid":123,"grid":[
          {"h":"亍","p":"chù"},{"h":"偻","p":"lóu"},{"h":"梏","p":"gù"},
          {"h":"荥","p":"xíng"},{"h":"箧","p":"qiè"},{"h":"氤","p":"yīn"},
          {"h":"瓒","p":"zàn"},{"h":"佝","p":"gōu"},{"h":"隗","p":"kuí"}]}
    """.trimIndent()

    @Test fun numericStatusAndTypesRetainIntegerSpelling() {
        val records = CheckInProtocol.parseRecords("""
            {"msg":1,"rows":[{"ID":123,"CheckInType":4,"CanApply":1}]}
        """)
        assertEquals("123", records.single().id)
        assertEquals("4", records.single().checkInType)
        assertEquals("1", records.single().canApply)
        assertEquals("", records.single().checkInCode)
    }

    @Test fun alsoAcceptsStringStatus() {
        assertTrue(CheckInProtocol.parseRecords("""{"msg":"1","rows":[]}""").isEmpty())
    }

    @Test fun gridKeepsServerOrderAndPinyin() {
        val grid = CheckInProtocol.parseHanziGrid(gridResponse, "123")
        assertEquals(9, grid.size)
        assertEquals("亍", grid.first().h)
        assertEquals("chù", grid.first().p)
        assertEquals("隗", grid.last().h)
    }

    @Test fun rejectsGridFromAnotherActivityOrCompletedActivity() {
        assertThrows(IllegalArgumentException::class.java) { CheckInProtocol.parseHanziGrid(gridResponse, "456") }
        assertThrows(IllegalArgumentException::class.java) {
            CheckInProtocol.parseHanziGrid(gridResponse.replace("\"ischeckin\":0", "\"ischeckin\":1"), "123")
        }
    }

    @Test fun rejectsMalformedOrDuplicateOptions() {
        assertThrows(IllegalArgumentException::class.java) {
            CheckInProtocol.parseHanziGrid(gridResponse.replace("隗", "亍"), "123")
        }
        assertThrows(IllegalArgumentException::class.java) {
            CheckInProtocol.parseHanziGrid(gridResponse.replace("隗", ""), "123")
        }
    }
}
