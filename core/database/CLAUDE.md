# 核心数据库模块指南

## 模块概述

此模块提供了统一的本地数据持久化功能，基于 Room（Android 官方 ORM 库）构建，支持数据库 CRUD 操作、类型转换、数据库迁移等功能。作为核心模块，为其他功能模块提供本地数据存储能力。

**模块信息：**
- **包名**: `com.mjc.core.database`
- **依赖**: Room 2.8.4, KSP 2.3.6, Kotlin Coroutines
- **架构**: 基于 Room 的分层持久化架构

## 模块结构

### 目录结构
模块规划包含以下目录和文件：
- **entity/**: 实体类定义（对应数据库表结构）
- **dao/**: 数据访问对象（Data Access Object，定义 SQL 查询）
- **converter/**: 类型转换器（TypeConverter，支持复杂类型的存储）
- **database/**: 数据库实例定义（RoomDatabase 子类）
- **migration/**: 数据库迁移策略（版本升级时的数据迁移）

## 核心技术栈

### 核心依赖
1. **ORM 框架**: Room 2.8.4（自 2.7.0 起已内置 KMP 支持）
2. **注解处理器**: KSP 2.3.6（替代已废弃的 KAPT）
3. **异步处理**: Kotlin Coroutines + Flow

### 版本选择说明
- **Room 2.8.4**: 当前最新稳定版（2025-11-19 发布），已内置 KMP 支持，`room-ktx` 的 API 已合并到 `room-runtime` 中，无需额外引入 `room-ktx`
- **KSP 2.3.6**: 当前最新版，已独立于 Kotlin 版本号发布，与 Kotlin 2.3.20 兼容

## Room 核心组件

### Entity（实体）
使用 `@Entity` 注解定义数据库表结构，每个 Entity 类对应一张数据库表。

```kotlin
@Entity(tableName = "example")
data class ExampleEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val createdAt: Date
)
```

### Dao（数据访问对象）
使用 `@Dao` 注解定义数据访问接口，支持 SQL 查询和 Kotlin Coroutines 集成。

```kotlin
@Dao
interface ExampleDao {
    @Query("SELECT * FROM example")
    fun getAll(): Flow<List<ExampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ExampleEntity)

    @Delete
    suspend fun delete(entity: ExampleEntity)
}
```

### Database（数据库实例）
使用 `@Database` 注解定义数据库入口，声明所有 Entity 和 Dao。

```kotlin
@Database(entities = [ExampleEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exampleDao(): ExampleDao
}
```

### TypeConverter（类型转换器）
使用 `@TypeConverter` 注解将复杂类型转换为数据库可存储的基本类型。

```kotlin
class Converters {
    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(millis: Long?): Date? = millis?.let { Date(it) }
}
```

### Migration（数据库迁移）
定义数据库版本升级时的迁移策略，确保数据不丢失。

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE example ADD COLUMN description TEXT")
    }
}
```

## 架构设计

### 分层架构
```
功能模块 (feature)
    ↓ 调用 Dao 接口
core/database
    ├── Dao       → 定义 SQL 查询（返回 Flow / suspend 函数）
    ├── Entity    → 定义表结构
    ├── Database  → RoomDatabase 实例（单例）
    ├── Converter → 类型转换
    └── Migration → 版本迁移
    ↓ 操作
SQLite 数据库
```

### 依赖关系
- 功能模块通过 Dao 接口与数据库交互
- Dao 通过 Entity 定义数据结构
- Database 汇总所有 Entity 和 Dao
- TypeConverter 处理复杂类型转换
- Migration 处理版本升级

## Room + Coroutines 集成

### Flow（响应式查询）
Room 原生支持返回 `Flow<T>`，当表数据变化时自动发出新值，适用于需要实时观察数据变化的场景。

```kotlin
@Query("SELECT * FROM example WHERE id = :id")
fun getById(id: Long): Flow<ExampleEntity?>
```

### Suspend 函数（一次性操作）
插入、更新、删除等一次性操作使用 `suspend` 函数，在 IO 线程安全执行。

```kotlin
@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun insertAll(entities: List<ExampleEntity>)

@Query("DELETE FROM example WHERE id = :id")
suspend fun deleteById(id: Long)
```

## Room Gradle Plugin 配置

本模块使用 Room Gradle Plugin（自 Room 2.6.0+ 推荐）自动管理 schema 导出：

```kotlin
plugins {
    alias(libs.plugins.room)
}

android {
    room {
        schemaDirectory("$projectDir/schemas")
    }
}
```

**作用**：
- 自动配置 schema 导出目录（无需在 `@Database` 注解中手动设置 `exportSchema`）
- 生成的 schema 文件用于数据库迁移测试
- schema 文件目录：`core/database/schemas/`

## 构建配置

### KSP（替代 KAPT）
本模块使用 KSP 进行 Room 注解处理，而非已废弃的 KAPT：

```kotlin
plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
}
```

**KSP 优势**：
- 编译速度比 KAPT 快 2 倍以上
- 支持增量注解处理
- 与 Kotlin 版本解耦，独立发布

### Convention 插件
本模块使用 `example.android.library` convention 插件统一管理 Android 库配置（compileSdk、minSdk、Java 版本等），避免各模块重复声明。

## 测试策略

### 单元测试
使用 `room-testing` 依赖提供的 `MigrationTestHelper` 进行迁移测试：

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val testHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate1To2() {
        val db = testHelper.createDatabase(TEST_DB, 1)
        db.close()
        testHelper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
    }
}
```

### 测试依赖
- `room-testing`: 提供 `MigrationTestHelper` 和 `InstantTaskExecutorRule` 支持
- `junit`: 基础单元测试框架

### 测试要点
1. **Entity 测试**: 验证实体类的字段映射
2. **Dao 测试**: 使用内存数据库验证 SQL 查询逻辑
3. **Migration 测试**: 使用 schema 文件验证版本迁移的正确性
4. **TypeConverter 测试**: 验证类型转换的双向正确性

## 性能优化建议

1. **索引优化**: 在频繁查询的字段上添加 `@Index` 注解
2. **批量操作**: 使用 `@Insert` 的 `List` 参数进行批量插入，避免逐条操作
3. **事务管理**: 使用 `@Transaction` 注解确保多步操作的原子性
4. **Flow 去重**: Room 的 Flow 查询默认使用 `distinctUntilChanged` 语义，避免不必要的数据发射
5. **预打包数据库**: 对于初始数据量大的场景，提供预填充数据库
6. **连接池**: 合理配置 `RoomDatabase` 的连接池大小

## 功能路线图

### 阶段1: 基础设施（当前）
- [x] 模块初始化（Room + KSP 依赖配置）
- [x] Convention 插件集成
- [x] Room Gradle Plugin schema 导出配置
- [ ] 基础 Entity 和 Dao 定义
- [ ] AppDatabase 实例创建

### 阶段2: 核心功能
- [ ] TypeConverter 实现
- [ ] 数据库迁移策略
- [ ] 数据库预填充
- [ ] 加密数据库支持（SQLCipher）

### 阶段3: 高级功能
- [ ] 全文搜索（FTS）支持
- [ ] 关系查询（Relation / Junction）
- [ ] 数据库备份与恢复
- [ ] 性能监控与分析

## 常见问题

### Q1: 为什么不使用 room-ktx？
A: 自 Room 2.7.0 起，`room-ktx` 的所有 API（包括 Coroutines 支持、Flow 支持）已合并到 `room-runtime` 中。`room-ktx` 现在是一个空壳 artifact，引入它只会增加构建时间，没有任何实际作用。

### Q2: KSP 和 KAPT 有什么区别？
A: KSP（Kotlin Symbol Processing）是 Kotlin 的官方注解处理器 API，编译速度比 KAPT 快 2 倍以上，支持增量处理。KAPT 已被官方标记为废弃，新项目应使用 KSP。

### Q3: Schema 文件有什么用？
A: Schema 文件记录了每个数据库版本的表结构快照，主要用于：
1. 数据库迁移测试（通过 `MigrationTestHelper` 加载特定版本的 schema）
2. 版本间的 schema 对比
3. 确保迁移的完整性和正确性

### Q4: 如何处理复杂的数据库迁移？
A: 对于复杂迁移，可以：
1. 使用 `Migration` 类定义逐步迁移逻辑
2. 使用 `fallbackToDestructiveMigration()` 作为最后手段（会清除所有数据）
3. 编写迁移测试确保数据完整性
4. 利用 schema 文件进行版本间对比

## 参考资料

1. [Room 官方文档](https://developer.android.com/training/data-storage/room)
2. [Room KSP 迁移指南](https://developer.android.com/build/migrate-to-ksp)
3. [Room Gradle Plugin 配置](https://developer.android.com/training/data-storage/room/migrating-db-versions)
4. [KSP 官方文档](https://kotlinlang.org/docs/ksp-overview.html)

---
**文档版本**: 1.0
**创建日期**: 2026-04-08
**适用模块**: core:database
