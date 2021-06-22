package org.beckn.one.sandbox.bap.protocol.repositories

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.conversions.Bson
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.getCollectionOfName

open class GenericRepository<R>(private val collection: MongoCollection<R>) {

  companion object {
    inline fun <reified T : Any> create(database: MongoDatabase): GenericRepository<T> {
      return GenericRepository(database.getCollection())
    }

    inline fun <reified T : Any> create(database: MongoDatabase, collectionName: String): GenericRepository<T> {
      return GenericRepository(database.getCollectionOfName(collectionName))
    }
  }

  fun size(): Long = collection.countDocuments()

  fun insertMany(documents: List<R>) = collection.insertMany(documents)

  fun insertOne(document: R) = collection.insertOne(document)

  fun findAll(query: Bson) = collection.find(query).toList()

  fun findOne(query: Bson) = collection.findOne(query)

  fun all() = collection.find().toList()

  fun clear() = collection.deleteMany(Document())

}
