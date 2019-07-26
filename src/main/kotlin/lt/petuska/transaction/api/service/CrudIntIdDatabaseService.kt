package lt.petuska.transaction.api.service

import lt.petuska.transaction.api.domain.*
import lt.petuska.transaction.api.exception.*
import lt.petuska.transaction.api.util.*
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.*

abstract class CrudIntIdDatabaseService<TBL : IntIdTable, OBJ : IntIdObj>(
  private val database: Database,
  private val table: TBL
) {
  open suspend fun getAll(): List<OBJ> {
    return dbQuery(database) { table.selectAll().map(::toObject) }.filter { !exclusionCheck(it) }
  }

  open suspend fun getOne(id: Int): OBJ {
    return dbQuery(database) {
      table.select { table.id eq id }
        .mapNotNull(::toObject)
        .single()
    }.takeIf { !exclusionCheck(it) }
      ?: throw ApiException.BadRequestException("This entity is locked")
  }

  open suspend fun findOne(selector: SqlExpressionBuilder.() -> Op<Boolean>): OBJ? {
    return dbQuery(database) {
      table.select(selector)
        .mapNotNull(::toObject)
        .singleOrNull()
    }?.takeIf { !exclusionCheck(it) }
  }

  open suspend fun findAll(selector: SqlExpressionBuilder.() -> Op<Boolean>): List<OBJ> {
    return dbQuery(database) {
      table.select(selector)
        .map(::toObject)
    }.filter { !exclusionCheck(it) }
  }

  open suspend fun deleteOne(selector: SqlExpressionBuilder.() -> Op<Boolean>): Boolean {
    val obj = findOne(selector)
    return if (obj?.let { exclusionCheck(obj) } == true) {
      false
    } else {
      dbQuery(database) {
        table.deleteWhere(op = selector, limit = 1) == 1
      }
    }
  }

  open suspend fun deleteAll(selector: SqlExpressionBuilder.() -> Op<Boolean>): Int {
    return findAll(selector).filter {
      deleteOne {
        table.id eq it.id
      }
    }.size
  }

  open suspend fun create(builder: TBL.(InsertStatement<*>) -> Unit): Int {
    return dbQuery(database) {
      table.insert(builder)[table.id].value
    }
  }

  open suspend fun update(
    selector: SqlExpressionBuilder.() -> Op<Boolean>,
    builder: TBL.(UpdateStatement) -> Unit
  ): Boolean {
    val obj = findOne(selector)
    return if (obj?.let { exclusionCheck(obj) } == true) {
      false
    } else {
      dbQuery(database) {
        table.update(selector, 1, builder) == 1
      }
    }
  }

  protected abstract fun toObject(row: ResultRow): OBJ

  protected open fun exclusionCheck(obj: OBJ): Boolean = false
}