package com.example.data.repository

import com.example.data.AppDatabase
import com.example.data.firebase.FirebaseSyncManager
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val database: AppDatabase,
    private val syncManager: FirebaseSyncManager? = null
) {

    // Orchards
    val allOrchards: Flow<List<OrchardEntity>> = database.orchardDao().getAllOrchards()

    suspend fun getOrchardById(id: Long): OrchardEntity? = database.orchardDao().getOrchardById(id)

    fun getOrchardByIdFlow(id: Long): Flow<OrchardEntity?> = database.orchardDao().getOrchardByIdFlow(id)

    suspend fun insertOrchard(orchard: OrchardEntity): Long {
        val id = database.orchardDao().insertOrchard(orchard)
        val entityWithId = if (orchard.id == 0L) orchard.copy(id = id) else orchard
        syncManager?.pushOrchard(entityWithId)
        return id
    }

    suspend fun updateOrchard(orchard: OrchardEntity) {
        database.orchardDao().updateOrchard(orchard)
        syncManager?.pushOrchard(orchard)
    }

    suspend fun updateOrchardsOrder(orderedOrchards: List<OrchardEntity>) {
        orderedOrchards.forEachIndexed { index, orchard ->
            database.orchardDao().updateOrchardOrder(orchard.id, index)
            syncManager?.pushOrchard(orchard.copy(sortOrder = index))
        }
    }

    suspend fun deleteOrchardById(id: Long) {
        database.orchardDao().deleteOrchardById(id)
        database.workPartDao().deleteWorkPartsByOrchardId(id)
        database.calendarTaskDao().deleteCalendarTasksByOrchardId(id)
        database.irrigationScheduleDao().deleteSchedulesForOrchard(id)
        syncManager?.deleteOrchard(id)
    }

    suspend fun cleanOrphanRecords() {
        database.workPartDao().deleteOrphanWorkParts()
        database.calendarTaskDao().deleteOrphanCalendarTasks()
    }

    suspend fun updatePropiosOwnerName(newName: String) = database.orchardDao().updatePropiosOwnerName(newName)
    suspend fun updateVrOwnerName(newName: String) = database.orchardDao().updateVrOwnerName(newName)
    suspend fun updateOtrosOwnerName(newName: String) = database.orchardDao().updateOtrosOwnerName(newName)

    // Work Parts
    val allWorkParts: Flow<List<WorkPartEntity>> = database.workPartDao().getAllWorkParts()

    fun getWorkPartsForOrchard(orchardId: Long): Flow<List<WorkPartEntity>> =
        database.workPartDao().getWorkPartsForOrchard(orchardId)

    suspend fun insertWorkPart(workPart: WorkPartEntity): Long {
        val id = database.workPartDao().insertWorkPart(workPart)
        val entityWithId = if (workPart.id == 0L) workPart.copy(id = id) else workPart
        syncManager?.pushWorkPart(entityWithId)
        return id
    }

    suspend fun deleteWorkPartById(id: Long) {
        database.workPartDao().deleteWorkPartById(id)
        syncManager?.deleteWorkPart(id)
    }

    suspend fun updateVariosOwnerCategories() = database.workPartDao().updateVariosOwnerCategories()

    suspend fun updateWorkPartOwnerCategory(oldCategory: String, newCategory: String) =
        database.workPartDao().updateOwnerCategory(oldCategory, newCategory)

    // Social Posts
    val allPosts: Flow<List<SocialPostEntity>> = database.socialPostDao().getAllPosts()

    suspend fun insertPost(post: SocialPostEntity): Long {
        val id = database.socialPostDao().insertPost(post)
        val entityWithId = if (post.id == 0L) post.copy(id = id) else post
        syncManager?.pushSocialPost(entityWithId)
        return id
    }

    suspend fun toggleLike(id: Long) = database.socialPostDao().toggleLike(id)

    suspend fun setAlertResolved(id: Long, resolved: Boolean) = database.socialPostDao().setAlertResolved(id, resolved)

    suspend fun deleteAllAlerts() {
        database.socialPostDao().deleteAllAlerts()
    }

    suspend fun deletePostById(id: Long) {
        database.socialPostDao().deletePostById(id)
        syncManager?.deleteSocialPost(id)
    }

    suspend fun cleanupDuplicateLonjaPosts() = database.socialPostDao().cleanupDuplicateLonjaPosts()

    // Comments
    fun getCommentsForPost(postId: Long): Flow<List<PostCommentEntity>> =
        database.postCommentDao().getCommentsForPost(postId)

    suspend fun insertComment(comment: PostCommentEntity): Long {
        val id = database.postCommentDao().insertComment(comment)
        database.socialPostDao().updateCommentCount(comment.postId)
        return id
    }

    suspend fun deleteComment(commentId: Long, postId: Long) {
        database.postCommentDao().deleteCommentById(commentId)
        database.socialPostDao().updateCommentCount(postId)
    }

    // Calendar Tasks
    val allCalendarTasks: Flow<List<CalendarTaskEntity>> = database.calendarTaskDao().getAllTasks()

    fun getTasksForDate(dateKey: String): Flow<List<CalendarTaskEntity>> =
        database.calendarTaskDao().getTasksForDate(dateKey)

    fun getTasksForOrchard(orchardId: Long): Flow<List<CalendarTaskEntity>> =
        database.calendarTaskDao().getTasksForOrchard(orchardId)

    suspend fun getCalendarTaskById(id: Long): CalendarTaskEntity? =
        database.calendarTaskDao().getTaskById(id)

    suspend fun insertCalendarTask(task: CalendarTaskEntity): Long {
        val id = database.calendarTaskDao().insertTask(task)
        val entityWithId = if (task.id == 0L) task.copy(id = id) else task
        syncManager?.pushCalendarTask(entityWithId)
        return id
    }

    suspend fun updateCalendarTask(task: CalendarTaskEntity) {
        database.calendarTaskDao().updateTask(task)
        syncManager?.pushCalendarTask(task)
    }

    suspend fun setCalendarTaskDone(id: Long, isDone: Boolean) {
        database.calendarTaskDao().setTaskDone(id, isDone, if (isDone) System.currentTimeMillis() else null)
        val task = database.calendarTaskDao().getTaskById(id)
        if (task != null) {
            syncManager?.pushCalendarTask(task)
        }
    }

    suspend fun updateTaskReminder(id: Long, reminderEnabled: Boolean, reminderDate: String, reminderTime: String) =
        database.calendarTaskDao().updateTaskReminder(id, reminderEnabled, reminderDate, reminderTime)

    suspend fun getPendingTasksWithReminders(): List<CalendarTaskEntity> =
        database.calendarTaskDao().getPendingTasksWithReminders()

    suspend fun deleteCalendarTaskById(id: Long) {
        database.calendarTaskDao().deleteTaskById(id)
        syncManager?.deleteCalendarTask(id)
    }

    // Custom Reminders
    val allReminders: Flow<List<CustomReminderEntity>> = database.customReminderDao().getAllReminders()

    suspend fun insertReminder(reminder: CustomReminderEntity): Long =
        database.customReminderDao().insertReminder(reminder)

    suspend fun deleteReminderById(id: Long) =
        database.customReminderDao().deleteReminderById(id)

    // Notification Settings
    val notificationSettings: Flow<NotificationSettingsEntity?> =
        database.notificationSettingsDao().getSettings()

    suspend fun updateNotificationSettings(settings: NotificationSettingsEntity) =
        database.notificationSettingsDao().updateSettings(settings)

    // User Profile
    val userProfile: Flow<UserProfileEntity?> = database.userProfileDao().getProfile()

    suspend fun updateUserProfile(profile: UserProfileEntity) {
        database.userProfileDao().updateProfile(profile)
        syncManager?.pushSharedTitulares(profile.titularPropios, profile.titularVr, profile.titularOtros)
    }

    // Irrigation Schedules
    fun getSchedulesForOrchard(orchardId: Long): Flow<List<IrrigationScheduleEntity>> =
        database.irrigationScheduleDao().getSchedulesForOrchard(orchardId)

    suspend fun saveSchedules(orchardId: Long, schedules: List<IrrigationScheduleEntity>) =
        database.irrigationScheduleDao().replaceSchedulesForOrchard(orchardId, schedules)

    suspend fun cleanupMockData() {
        com.example.data.AppDatabase.cleanupMockData(database)
    }

    suspend fun clearAllData() {
        database.orchardDao().deleteAllOrchards()
        database.workPartDao().deleteAllWorkParts()
        database.calendarTaskDao().deleteAllCalendarTasks()
        database.socialPostDao().deleteAllPosts()
        database.postCommentDao().deleteAllComments()
        database.customReminderDao().deleteAllReminders()
    }
}

