package io.github.szpontium.api.librus

import io.github.szpontium.api.hebe.models.*
import io.github.szpontium.api.librus.models.LibrusMeResponse
import io.github.szpontium.api.librus.models.LibrusSynergiaAccount
import io.github.szpontium.api.librus.models.api.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

object LibrusMapper {
    fun toHebeAccount(
        synergiaAccount: LibrusSynergiaAccount,
        me: LibrusMeResponse
    ): Account {
        val names = synergiaAccount.studentName.split(" ")
        val firstName = names.getOrNull(0) ?: me.me.user.firstName ?: "Uczeń"
        val lastName = names.getOrNull(1) ?: me.me.user.lastName ?: ""
        val schoolName = synergiaAccount.schoolName ?: "Librus Synergia"

        return Account(
            topLevelPartition = "librus",
            partition = "librus",
            links = AccountLinks("", "", "", "", "", ""),
            unit = SchoolUnit(
                id = synergiaAccount.id,
                symbol = "librus",
                short = schoolName.take(10),
                restUrl = "librus",
                name = schoolName,
                displayName = schoolName,
                schoolTopic = ""
            ),
            constituentUnit = ConstituentUnit(
                id = synergiaAccount.id,
                short = "",
                name = schoolName,
                schoolTopic = ""
            ),
            capabilities = emptyList(),
            educatorsList = emptyList(),
            pupil = Pupil(
                id = synergiaAccount.id,
                loginId = synergiaAccount.id,
                firstName = firstName,
                secondName = "",
                surname = lastName,
                sex = true
            ),
            periods = listOf(
                Period(
                    capabilities = emptyList(),
                    id = 1,
                    level = 1,
                    number = 1,
                    start = LocalDate(2023, 9, 1),
                    end = LocalDate(2025, 1, 31),
                    current = true,
                    last = false
                )
            ),
            constraints = Constraints(0, LocalTime(0, 0)),
            state = 1,
            profileId = synergiaAccount.login
        )
    }

    fun mapGrades(
        librusGrades: List<LibrusGrade>,
        categories: List<LibrusGradeCategory>,
        subjects: List<LibrusSubject>
    ): List<Grade> {
        val categoryMap = categories.associateBy { it.id }
        val subjectMap = subjects.associateBy { it.id }
        
        return librusGrades.map { lGrade ->
            val cat = categoryMap[lGrade.category.id]
            val lSubject = subjectMap[lGrade.subject.id]
            val subjectName = lSubject?.name ?: lGrade.subject.name ?: "Brak nazwy"

            val date = try {
                LocalDateTime.parse(lGrade.addDate?.replace(" ", "T") ?: "")
            } catch (e: Exception) {
                LocalDateTime(2024, 1, 1, 0, 0)
            }
            
            Grade(
                id = lGrade.id.toInt(),
                key = lGrade.id.toString(),
                pupilId = 0,
                contentRaw = lGrade.grade,
                content = lGrade.grade,
                comment = "",
                value = lGrade.grade.filter { it.isDigit() }.toDoubleOrNull(),
                createdAt = date,
                modifiedAt = date,
                creator = Employee(0, "", lGrade.addedBy?.id?.toString() ?: "", ""),
                modifier = Employee(0, "", "", ""),
                column = GradeColumn(
                    id = lGrade.category.id.toInt(),
                    key = lGrade.category.id.toString(),
                    periodId = 1,
                    name = cat?.name ?: "",
                    code = "",
                    group = "",
                    number = 0,
                    color = 0,
                    weight = cat?.weight?.toDouble() ?: 0.0,
                    subject = Subject(lGrade.subject.id.toInt(), lGrade.subject.id.toString(), subjectName, "", 0),
                    category = null
                )
            )
        }
    }

    fun mapSchedule(
        timetable: Map<String, List<List<LibrusLesson>>>,
        subjects: List<LibrusSubject>,
        users: List<LibrusUser>,
        classrooms: List<LibrusClassroom>
    ): List<Schedule> {
        val result = mutableListOf<Schedule>()
        val subjectMap = subjects.associateBy { it.id }
        val userMap = users.associateBy { it.id }
        val classroomMap = classrooms.associateBy { it.id }

        timetable.forEach { (dateStr, lessonsOfDay) ->
            val lessonDate = LocalDate.parse(dateStr)
            lessonsOfDay.forEach { lessonList ->
                lessonList.forEach { lLesson ->
                    val startTime = try { LocalTime.parse(lLesson.hourFrom) } catch (e: Exception) { LocalTime(8, 0) }
                    val endTime = try { LocalTime.parse(lLesson.hourTo) } catch (e: Exception) { LocalTime(8, 45) }
                    
                    val subjectId = lLesson.subject?.id
                    val subjectName = subjectMap[subjectId]?.name ?: lLesson.subject?.name ?: "Brak"

                    val teacherId = lLesson.teacher?.id
                    val lUser = userMap[teacherId]
                    var teacherName = if (lUser != null) "${lUser.firstName ?: ""} ${lUser.lastName ?: ""}".trim() else lLesson.teacher?.name ?: ""

                    val roomId = lLesson.classroom?.id
                    val lRoom = classroomMap[roomId]
                    var roomName = if (lRoom != null) {
                        if (lRoom.symbol != null && lRoom.name != lRoom.symbol) "${lRoom.symbol} ${lRoom.name}" else lRoom.name
                    } else lLesson.classroom?.name ?: ""

                    if (lLesson.isSubstitutionClass) {
                        if (roomName.isBlank()) roomName = "-"
                        if (teacherName.isBlank()) teacherName = "Nauczyciel zastępczy"
                    }

                    val s = Schedule(
                        id = (lessonDate.toEpochDays().toLong() % 1000000 * 100 + lLesson.lessonNo).toInt(),
                        mergeChangeId = null,
                        event = if (lLesson.isCanceled) "Odwołana" else null,
                        date = lessonDate,
                        room = Room(0, roomName.ifBlank { "-" }),
                        timeSlot = Timeslot(lLesson.lessonNo, startTime, endTime, lLesson.lessonNo.toString(), lLesson.lessonNo),
                        subject = Subject(subjectId?.toInt() ?: 0, subjectId?.toString() ?: "", subjectName, "", 0),
                        teacherPrimary = Employee(0, "", teacherName.ifBlank { "Brak danych" }, ""),
                        teacherSecondary = null,
                        teacherSecondary2 = null,
                        clazz = Clazz(0, "0", "", ""),
                        distribution = null,
                        pupilAlias = null,
                        substitution = if (lLesson.isSubstitutionClass) {
                            ScheduleSubstitution(
                                id = 0, unitId = 0, scheduleId = 0, date = lessonDate,
                                classAbsence = false, noRoom = false, modifiedAt = LocalDateTime(2024, 1, 1, 0, 0),
                                reason = "Zastępstwo"
                            )
                        } else null,
                        parent = null
                    )
                    result.add(s)
                }
            }
        }
        return result
    }

    fun mapExams(
        lEvents: List<LibrusEvent>,
        subjects: List<LibrusSubject>,
        eventCategories: List<LibrusIdNameReference>,
        users: List<LibrusUser>
    ): List<Exam> {
        val subjectMap = subjects.associateBy { it.id }
        val categoryMap = eventCategories.associateBy { it.id }
        val userMap = users.associateBy { it.id }

        return lEvents.map { lEvent ->
            val date = try {
                LocalDateTime.parse(lEvent.addDate?.replace(" ", "T") ?: "")
            } catch (e: Exception) {
                LocalDateTime(2024, 1, 1, 0, 0)
            }
            val sName = subjectMap[lEvent.subject?.id]?.name ?: lEvent.subject?.name ?: "Brak"
            val categoryName = categoryMap[lEvent.category?.id]?.name ?: lEvent.category?.name ?: "Sprawdzian"

            val creatorId = lEvent.createdBy?.id
            val lUser = userMap[creatorId]
            val teacherName = if (lUser != null) "${lUser.firstName ?: ""} ${lUser.lastName ?: ""}".trim() else "Brak danych"

            Exam(
                id = lEvent.id.toInt(),
                key = lEvent.id.toString(),
                type = categoryName,
                typeId = 0,
                content = lEvent.content ?: "",
                createdAt = date,
                modifiedAt = date,
                deadline = LocalDate.parse(lEvent.date),
                creator = Employee(0, "", teacherName, teacherName),
                subject = Subject(lEvent.subject?.id?.toInt() ?: 0, lEvent.subject?.id?.toString() ?: "", sName, "", 0),
                pupilId = 0
            )
        }
    }

    fun mapHomework(
        lHomework: List<LibrusHomeWorkAssignment>,
        subjects: List<LibrusSubject>,
        users: List<LibrusUser>
    ): List<Homework> {
        val subjectMap = subjects.associateBy { it.id }
        val userMap = users.associateBy { it.id }

        return lHomework.map { lH ->
            val addedDate = try {
                LocalDateTime.parse(lH.date + "T00:00:00")
            } catch (e: Exception) {
                LocalDateTime(2024, 1, 1, 0, 0)
            }
            val deadline = try {
                LocalDate.parse(lH.dueDate)
            } catch (e: Exception) {
                LocalDate(2024, 12, 31)
            }
            val sName = subjectMap[lH.subject?.id]?.name ?: "Brak"
            
            val teacherId = lH.teacher?.id
            val lUser = userMap[teacherId]
            val teacherName = if (lUser != null) "${lUser.firstName ?: ""} ${lUser.lastName ?: ""}".trim() else "Brak danych"

            Homework(
                id = lH.id.toInt(),
                key = lH.id.toString(),
                pupilId = 0,
                homeworkId = lH.id.toInt(),
                content = (lH.topic ?: "") + "\n" + (lH.text ?: ""),
                isAnswerRequired = false,
                createdAt = addedDate,
                modifiedAt = addedDate,
                date = deadline,
                deadline = deadline,
                creator = Employee(0, "", teacherName, teacherName),
                subject = Subject(lH.subject?.id?.toInt() ?: 0, lH.subject?.id?.toString() ?: "", sName, "", 0),
                attachments = emptyList()
            )
        }
    }

    fun mapNotices(
        lNotices: List<LibrusNotice>,
        lCategories: List<LibrusNoticeCategory>,
        lUsers: List<LibrusUser>
    ): List<Note> {
        val categoryMap = lCategories.associateBy { it.id }
        val userMap = lUsers.associateBy { it.id }

        return lNotices.map { lN ->
            val date = try {
                LocalDate.parse(lN.date)
            } catch (e: Exception) {
                LocalDate(2024, 1, 1)
            }
            val dateTime = LocalDateTime(date.year, date.month, date.day, 0, 0)
            
            val cat = lN.category?.let { categoryMap[it.id] }
            val lUser = userMap[lN.teacher?.id]
            val teacherName = if (lUser != null) "${lUser.firstName ?: ""} ${lUser.lastName ?: ""}".trim() else "Brak danych"

            Note(
                id = lN.id.toInt(),
                key = lN.id.toString(),
                pupilId = 0,
                positive = lN.positive == 1,
                dateValid = date,
                dateModify = dateTime,
                creator = Employee(0, "", teacherName, teacherName),
                category = NoteCategory(
                    id = lN.category?.id?.toInt() ?: 0,
                    name = cat?.name ?: "Inna"
                ),
                content = lN.text,
                points = null
            )
        }
    }

    fun mapMessages(
        lMessages: List<LibrusMessage>
    ): List<io.github.szpontium.ui.model.UiMessage> {
        return lMessages.map { lM ->
            val date = try {
                LocalDateTime.parse(lM.date?.replace(" ", "T") ?: "")
            } catch (e: Exception) {
                null
            }
            
            io.github.szpontium.ui.model.UiMessage(
                id = lM.id.toString(),
                title = lM.subject ?: "(brak tematu)",
                senderOrRecipient = lM.sender?.name ?: lM.recipient?.name ?: "Nieznany",
                date = date,
                isUnread = lM.isRead == false,
                hasAttachments = lM.hasAttachment == true,
                content = lM.content
            )
        }
    }
}
