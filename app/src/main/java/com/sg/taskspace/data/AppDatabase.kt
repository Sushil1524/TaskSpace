package com.sg.taskspace.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Task::class,
        JournalEntry::class,
        Habit::class,
        HabitLog::class,
        Goal::class,
        Project::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao
    abstract fun goalDao(): GoalDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `journal_entries` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `content` TEXT NOT NULL, `mood` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `frequency` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `habit_logs` (`id` TEXT NOT NULL, `habitId` TEXT NOT NULL, `date` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT, `deadlineDate` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns to tasks table
                db.execSQL("ALTER TABLE tasks ADD COLUMN projectId TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN estimatedMinutes INTEGER NOT NULL DEFAULT 0")
                // Create projects table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `projects` " +
                    "(`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT, " +
                    "`colorHex` TEXT NOT NULL DEFAULT '#6366F1', `createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN dueDate TEXT")
                db.execSQL("ALTER TABLE projects ADD COLUMN reminderTimeMillis INTEGER")
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE tasks ADD COLUMN deletedAt INTEGER")

                db.execSQL("ALTER TABLE journal_entries ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN deletedAt INTEGER")

                db.execSQL("ALTER TABLE habits ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE habits ADD COLUMN deletedAt INTEGER")

                db.execSQL("ALTER TABLE goals ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals ADD COLUMN deletedAt INTEGER")

                db.execSQL("ALTER TABLE projects ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE projects ADD COLUMN deletedAt INTEGER")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN type TEXT NOT NULL DEFAULT 'BUILD'")
                db.execSQL("ALTER TABLE habits ADD COLUMN targetValue REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE habits ADD COLUMN unit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN scheduleType TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE habits ADD COLUMN selectedDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN weeklyTargetCount INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE habits ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE habits ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#10B981'")
                db.execSQL("ALTER TABLE habits ADD COLUMN iconName TEXT NOT NULL DEFAULT 'Check'")
                db.execSQL("ALTER TABLE habits ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE habits ADD COLUMN reminderTime TEXT")

                db.execSQL("ALTER TABLE habit_logs ADD COLUMN value REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN relapseNotes TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "taskspace_database")
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigrationFrom(true)
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
