package xh.zero.launcher.repo

import androidx.room.*

@Dao
abstract class DesktopItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: DesktopItem)

    @Query("SELECT * FROM DesktopItem")
    abstract suspend fun findAll() : List<DesktopItem>

    @Query("SELECT * FROM DesktopItem WHERE page=:page")
    abstract suspend fun findAllForPage(page: Int) : List<DesktopItem>

    @Query("SELECT COUNT(packageName) FROM DesktopItem")
    abstract suspend fun totalCount() : Int

    @Query("SELECT MAX(page) FROM DesktopItem")
    abstract suspend fun maxPage() : Int

    @Delete
    abstract suspend fun deleteItem(app: DesktopItem)

    @Query("DELETE FROM DesktopItem")
    abstract suspend fun clear()

    @Query("SELECT * FROM DesktopItem WHERE packageName=:name")
    abstract suspend fun findItem(name: String) : DesktopItem?
}