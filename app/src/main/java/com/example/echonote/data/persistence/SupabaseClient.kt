package com.example.echonote.data.persistence

import com.example.echonote.data.entities.Folder
import com.example.echonote.data.entities.Item
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.minutes

object SupabaseClient: IPersistence {
    private val supabase = createSupabaseClient(
        // public project url
        supabaseUrl = "https://sxrwvweeprivkwsgimun.supabase.co",
        // public anon key
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InN4cnd2d2VlcHJpdmt3c2dpbXVuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MjgwNzAyNzYsImV4cCI6MjA0MzY0NjI3Nn0.NNlLAQGMKQaOppISuPxkYsNzVv9thgaYLH2wwR65RE8"
    ) {
        install(Auth) {
            minimalSettings() // disables session saving and auto-refreshing
        }
        install(Postgrest)
        install(Storage)
    }
    private var currentUser: String? = null

    override fun getCurrentUser(): String? {
        return currentUser
    }

    override fun setCurrentUser(userId: String) {
        currentUser = userId
    }

    override suspend fun loadFolders(): List<Folder> {
        return getFoldersTable().select{
            filter { eq("user_id", currentUser!!) }
        }.decodeList<Folder>()
    }

    override suspend fun saveFolders(folders: List<Folder>) {
        getFoldersTable().upsert(folders)
    }

    override suspend fun loadItems(folderId: Long): List<Item> {
        return getItemsTable().select{
            filter { eq("folder_id", folderId) }
        }.decodeList<Item>()
    }

    override suspend fun saveItems(items: List<Item>) {
        getItemsTable().upsert(items)
    }

    override suspend fun saveItem(item: Item) {
        getItemsTable().upsert(item)
    }

    override suspend fun getFoldersCount(): Long {
        return getFoldersTable().select{
            count(Count.EXACT)
        }.countOrNull()!!
    }

    override suspend fun getItemsCount(): Long {
        return getItemsTable().select{
            count(Count.EXACT)
        }.countOrNull()!!
    }

    private fun getFoldersTable(): PostgrestQueryBuilder {
        return supabase.from("Folders")
    }

    private fun getItemsTable(): PostgrestQueryBuilder {
        return supabase.from("Items")
    }

    override suspend fun signupUser(userEmail: String, userPassword: String, userName: String) {
         supabase.auth.signUpWith(Email) {
            email = userEmail
            password = userPassword
             data = buildJsonObject {
                 put("first_name", JsonPrimitive(userName))
             }
         }
        logCurrentSession()
    }

    override suspend fun loginUser(userEmail: String, userPassword: String) {
        supabase.auth.signInWith(Email) {
            email = userEmail
            password = userPassword
        }
        logCurrentSession()
    }

    suspend fun uploadAudioFileAndGetUrl(filePath: String, fileData: ByteArray) : String {
        val bucket = supabase.storage.from("audio-storage")
        bucket.upload(filePath, fileData) {
            upsert = false
        }
        val url = bucket.createSignedUrl(path = filePath, expiresIn = 60.minutes)
        return url
    }

    fun logCurrentSession() {
        val currentSession = supabase.auth.currentSessionOrNull()
        println("Current Session: $currentSession")
        println("Current Session Access Token: ${currentSession?.accessToken}")
    }

    override fun getCurrentUserID() : String {
        val currentSession = supabase.auth.currentSessionOrNull()
        val uuid = currentSession?.user?.id ?: "unknown UUID"
        println("UUID: $uuid")
        return uuid
    }

    override fun getName() : String {
        val currentSession = supabase.auth.currentSessionOrNull()
        val name = currentSession?.user?.userMetadata?.get("first_name")?.jsonPrimitive?.content ?: "unknown"
        println("Name: $name")
        return name
    }

}
