package xh.zero.launcher.repo

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DesktopItem::class],
    version = 1,
    exportSchema = false
)
abstract class LauncherDB : RoomDatabase() {
    abstract fun desktopItemDao(): DesktopItemDao
}