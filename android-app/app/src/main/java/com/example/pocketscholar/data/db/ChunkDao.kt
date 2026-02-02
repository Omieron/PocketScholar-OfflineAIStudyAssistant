package com.example.pocketscholar.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlin.jvm.JvmSuppressWildcards

@Dao
interface ChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<ChunkEntity>): @JvmSuppressWildcards LongArray

    @Query("SELECT * FROM chunks WHERE documentId = :documentId ORDER BY pageNumber, chunkIndex")
    suspend fun getByDocumentId(documentId: String): @JvmSuppressWildcards List<ChunkEntity>

    @Query("SELECT * FROM chunks ORDER BY documentId, pageNumber, chunkIndex")
    suspend fun getAll(): @JvmSuppressWildcards List<ChunkEntity>

    @Query("DELETE FROM chunks WHERE documentId = :documentId")
    suspend fun deleteByDocumentId(documentId: String): @JvmSuppressWildcards Int
}
