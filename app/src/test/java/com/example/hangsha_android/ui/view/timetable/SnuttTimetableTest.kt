package com.example.hangsha_android.ui.view.timetable

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnuttTimetableTest {
    @Test
    fun parse_acceptsCurrentSnakeCasePayload() {
        val timetable = SnuttTimetableMessageParser.parse(
            """
            {
              "type": "SNUTT_TIMETABLE_SELECTED",
              "payload": {
                "title": "2026-2",
                "year": 2026,
                "semester": 2,
                "lecture_list": [
                  {
                    "course_title": "운영체제",
                    "course_number": "M1522.000100",
                    "lecture_number": "001",
                    "credit": 3,
                    "instructor": "교수",
                    "class_time_json": [
                      { "day": 0, "startMinute": 540, "endMinute": 615, "place": "301동" }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        requireNotNull(timetable)
        assertEquals("2026-2", timetable.title)
        assertEquals(2026, timetable.year)
        assertEquals(2, timetable.semester)
        assertEquals("운영체제", timetable.lectures.single().courseTitle)
        assertEquals(SnuttTimeSlot(day = 0, startMinute = 540, endMinute = 615), timetable.lectures.single().classPlaceAndTimes.single())
    }

    @Test
    fun parse_acceptsCamelCaseAliasesAndDropsInvalidTimeSlots() {
        val timetable = SnuttTimetableMessageParser.parse(
            """
            {
              "type": "SNUTT_TIMETABLE_SELECTED",
              "payload": {
                "title": "시간표",
                "year": 2026,
                "semester": 3,
                "lectures": [
                  {
                    "courseTitle": "계절 수업",
                    "classPlaceAndTimes": [
                      { "day": 6, "startMinute": 600, "endMinute": 660 },
                      { "day": 7, "startMinute": 600, "endMinute": 660 },
                      { "day": 1, "startMinute": 700, "endMinute": 700 }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        requireNotNull(timetable)
        assertEquals(listOf(SnuttTimeSlot(6, 600, 660)), timetable.lectures.single().classPlaceAndTimes)
    }

    @Test
    fun parse_rejectsWrongMessageOrMalformedLecture() {
        assertNull(SnuttTimetableMessageParser.parse("""{"type":"OTHER","payload":{}}"""))
        assertNull(
            SnuttTimetableMessageParser.parse(
                """
                {
                  "type": "SNUTT_TIMETABLE_SELECTED",
                  "payload": {
                    "title": "시간표",
                    "year": 2026,
                    "semester": 1,
                    "lecture_list": [{ "course_title": "제목만 있음" }]
                  }
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun map_keepsWeekendCoursesAndExcludesLecturesWithoutValidTime() {
        val mapped = SnuttTimetableMapper.toImportData(
            SnuttTimetable(
                title = "  ",
                year = 2026,
                semester = 4,
                lectures = listOf(
                    SnuttLecture(
                        courseTitle = "주말 수업",
                        classPlaceAndTimes = listOf(SnuttTimeSlot(5, 600, 720)),
                        courseNumber = "COURSE",
                        lectureNumber = "001",
                        credit = 2,
                        instructor = "교수"
                    ),
                    SnuttLecture(
                        courseTitle = "시간 미정",
                        classPlaceAndTimes = emptyList(),
                        courseNumber = null,
                        lectureNumber = null,
                        credit = null,
                        instructor = null
                    )
                )
            )
        )

        assertEquals("SNUTT 시간표 (SNUTT)", mapped.timetableName)
        assertEquals("WINTER", mapped.semester)
        assertEquals(1, mapped.excludedCourseCount)
        assertEquals(1, mapped.weekendCourseCount)
        assertEquals("SAT", mapped.courses.single().timeSlots.single().dayOfWeek)
        assertTrue(mapped.courses.single().timeSlots.isNotEmpty())
    }

    @Test
    fun map_convertsEverySnuttSemesterCodeToHangshaSemester() {
        val expectedSemesters = mapOf(
            1 to "SPRING",
            2 to "SUMMER",
            3 to "FALL",
            4 to "WINTER"
        )

        expectedSemesters.forEach { (snuttSemester, hangshaSemester) ->
            val mapped = SnuttTimetableMapper.toImportData(
                SnuttTimetable(
                    title = "시간표",
                    year = 2026,
                    semester = snuttSemester,
                    lectures = emptyList()
                )
            )

            assertEquals(hangshaSemester, mapped.semester)
        }
    }
}
