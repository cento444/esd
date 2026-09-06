package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OrchardDao {
    @Query("SELECT * FROM orchards ORDER BY sortOrder ASC, id ASC")
    fun getAllOrchards(): Flow<List<OrchardEntity>>

    @Query("SELECT COUNT(*) FROM orchards")
    suspend fun getOrchardCount(): Int

    @Query("SELECT * FROM orchards WHERE id = :id")
    suspend fun getOrchardById(id: Long): OrchardEntity?

    @Query("SELECT * FROM orchards WHERE id = :id")
    fun getOrchardByIdFlow(id: Long): Flow<OrchardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrchard(orchard: OrchardEntity): Long

    @Update
    suspend fun updateOrchard(orchard: OrchardEntity)

    @Query("UPDATE orchards SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrchardOrder(id: Long, order: Int)

    @Delete
    suspend fun deleteOrchard(orchard: OrchardEntity)

    @Query("DELETE FROM orchards WHERE id = :id")
    suspend fun deleteOrchardById(id: Long)

    @Query("DELETE FROM orchards")
    suspend fun deleteAllOrchards()

    @Query("UPDATE orchards SET ownerName = :newName WHERE ownerType = 'propios' OR ownerType = 'propiedad'")
    suspend fun updatePropiosOwnerName(newName: String)

    @Query("UPDATE orchards SET ownerName = :newName WHERE ownerType = 'v_y_r_cb' OR ownerType = 'vr' OR ownerType = 'cb'")
    suspend fun updateVrOwnerName(newName: String)

    @Query("UPDATE orchards SET ownerName = :newName WHERE ownerType = 'otros'")
    suspend fun updateOtrosOwnerName(newName: String)
}

@Dao
interface WorkPartDao {
    @Query("SELECT * FROM work_parts ORDER BY dateTimestamp DESC")
    fun getAllWorkParts(): Flow<List<WorkPartEntity>>

    @Query("SELECT * FROM work_parts WHERE orchardId = :orchardId ORDER BY dateTimestamp DESC")
    fun getWorkPartsForOrchard(orchardId: Long): Flow<List<WorkPartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkPart(workPart: WorkPartEntity): Long

    @Delete
    suspend fun deleteWorkPart(workPart: WorkPartEntity)

    @Query("DELETE FROM work_parts WHERE id = :id")
    suspend fun deleteWorkPartById(id: Long)

    @Query("DELETE FROM work_parts")
    suspend fun deleteAllWorkParts()

    @Query("DELETE FROM work_parts WHERE orchardId = :orchardId")
    suspend fun deleteWorkPartsByOrchardId(orchardId: Long)

    @Query("DELETE FROM work_parts WHERE orchardId > 0 AND orchardId NOT IN (SELECT id FROM orchards)")
    suspend fun deleteOrphanWorkParts()

    @Query("UPDATE work_parts SET ownerCategory = 'Varios' WHERE LOWER(type) = 'varios' AND ownerCategory != 'Varios'")
    suspend fun updateVariosOwnerCategories()

    @Query("UPDATE work_parts SET ownerCategory = :newCategory WHERE ownerCategory = :oldCategory")
    suspend fun updateOwnerCategory(oldCategory: String, newCategory: String)
}

@Dao
interface SocialPostDao {
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<SocialPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: SocialPostEntity): Long

    @Update
    suspend fun updatePost(post: SocialPostEntity)

    @Delete
    suspend fun deletePost(post: SocialPostEntity)

    @Query("UPDATE social_posts SET likesCount = likesCount + (CASE WHEN isLiked THEN -1 ELSE 1 END), isLiked = NOT isLiked WHERE id = :id")
    suspend fun toggleLike(id: Long)

    @Query("UPDATE social_posts SET commentsCount = (SELECT COUNT(*) FROM post_comments WHERE postId = :id) WHERE id = :id")
    suspend fun updateCommentCount(id: Long)

    @Query("UPDATE social_posts SET isAlertResolved = :resolved WHERE id = :id")
    suspend fun setAlertResolved(id: Long, resolved: Boolean)

    @Query("DELETE FROM social_posts WHERE (authorName LIKE '%Lonja%' OR orchardName = 'Mercado y Lonjas') AND id NOT IN (SELECT id FROM social_posts WHERE (authorName LIKE '%Lonja%' OR orchardName = 'Mercado y Lonjas') ORDER BY timestamp DESC LIMIT 1)")
    suspend fun cleanupDuplicateLonjaPosts()

    @Query("DELETE FROM social_posts WHERE isAlert = 1")
    suspend fun deleteAllAlerts()

    @Query("DELETE FROM social_posts WHERE id = :id")
    suspend fun deletePostById(id: Long)

    @Query("DELETE FROM social_posts")
    suspend fun deleteAllPosts()
}

@Dao
interface PostCommentDao {
    @Query("SELECT * FROM post_comments WHERE postId = :postId ORDER BY timestamp ASC")
    fun getCommentsForPost(postId: Long): Flow<List<PostCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: PostCommentEntity): Long

    @Query("DELETE FROM post_comments WHERE id = :id")
    suspend fun deleteCommentById(id: Long)

    @Query("DELETE FROM post_comments WHERE postId = :postId")
    suspend fun deleteCommentsForPost(postId: Long)

    @Query("SELECT COUNT(*) FROM post_comments WHERE postId = :postId")
    suspend fun getCommentCountForPost(postId: Long): Int

    @Query("DELETE FROM post_comments")
    suspend fun deleteAllComments()
}

@Dao
interface CalendarTaskDao {
    @Query("SELECT * FROM calendar_tasks ORDER BY dateKey ASC, timeText ASC")
    fun getAllTasks(): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE dateKey = :dateKey ORDER BY timeText ASC")
    fun getTasksForDate(dateKey: String): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE orchardId = :orchardId ORDER BY dateKey ASC, timeText ASC")
    fun getTasksForOrchard(orchardId: Long): Flow<List<CalendarTaskEntity>>

    @Query("SELECT * FROM calendar_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): CalendarTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: CalendarTaskEntity): Long

    @Update
    suspend fun updateTask(task: CalendarTaskEntity)

    @Delete
    suspend fun deleteTask(task: CalendarTaskEntity)

    @Query("DELETE FROM calendar_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM calendar_tasks WHERE orchardId = :orchardId")
    suspend fun deleteCalendarTasksByOrchardId(orchardId: Long)

    @Query("DELETE FROM calendar_tasks WHERE orchardId IS NOT NULL AND orchardId > 0 AND orchardId NOT IN (SELECT id FROM orchards)")
    suspend fun deleteOrphanCalendarTasks()

    @Query("DELETE FROM calendar_tasks")
    suspend fun deleteAllCalendarTasks()

    @Query("UPDATE calendar_tasks SET isDone = :isDone, completedAt = :completedAt WHERE id = :id")
    suspend fun setTaskDone(id: Long, isDone: Boolean, completedAt: Long? = null)

    @Query("UPDATE calendar_tasks SET reminderEnabled = :reminderEnabled, reminderDate = :reminderDate, reminderTime = :reminderTime WHERE id = :id")
    suspend fun updateTaskReminder(id: Long, reminderEnabled: Boolean, reminderDate: String, reminderTime: String)

    @Query("SELECT * FROM calendar_tasks WHERE isDone = 0 AND reminderEnabled = 1")
    suspend fun getPendingTasksWithReminders(): List<CalendarTaskEntity>
}

@Dao
interface CustomReminderDao {
    @Query("SELECT * FROM custom_reminders ORDER BY timestamp DESC")
    fun getAllReminders(): Flow<List<CustomReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: CustomReminderEntity): Long

    @Delete
    suspend fun deleteReminder(reminder: CustomReminderEntity)

    @Query("DELETE FROM custom_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("DELETE FROM custom_reminders")
    suspend fun deleteAllReminders()
}

@Dao
interface NotificationSettingsDao {
    @Query("SELECT * FROM notification_settings WHERE id = 1")
    fun getSettings(): Flow<NotificationSettingsEntity?>

    @Query("SELECT * FROM notification_settings WHERE id = 1")
    suspend fun getSettingsDirect(): NotificationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: NotificationSettingsEntity)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileDirect(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProfile(profile: UserProfileEntity)
}

@Dao
interface IrrigationScheduleDao {
    @Query("SELECT * FROM irrigation_schedules WHERE orchardId = :orchardId")
    fun getSchedulesForOrchard(orchardId: Long): Flow<List<IrrigationScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<IrrigationScheduleEntity>)

    @Query("DELETE FROM irrigation_schedules WHERE orchardId = :orchardId")
    suspend fun deleteSchedulesForOrchard(orchardId: Long)

    @androidx.room.Transaction
    suspend fun replaceSchedulesForOrchard(orchardId: Long, schedules: List<IrrigationScheduleEntity>) {
        deleteSchedulesForOrchard(orchardId)
        insertSchedules(schedules)
    }
}
