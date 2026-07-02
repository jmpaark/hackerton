package com.nbunone.app.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubStats(
    val repo: String,
    val totalCommits: Int,
    val byAuthor: List<Pair<String, Int>>
)

object GitHubAnalyzer {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    /** 공개 저장소 커밋을 최대 300개까지 가져와 저자별로 집계 */
    suspend fun analyze(url: String): Result<GitHubStats> = withContext(Dispatchers.IO) {
        runCatching {
            val match = Regex("github\\.com/([^/\\s]+)/([^/\\s#?]+)").find(url)
                ?: error("올바른 GitHub 저장소 주소가 아닙니다")
            val owner = match.groupValues[1]
            val repo = match.groupValues[2].removeSuffix(".git")

            val authors = mutableListOf<String>()
            var page = 1
            while (page <= 3) {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$owner/$repo/commits?per_page=100&page=$page")
                    .addHeader("Accept", "application/vnd.github+json")
                    .build()
                val pageSize = client.newCall(request).execute().use { resp ->
                    when {
                        resp.code == 404 -> error("저장소를 찾을 수 없습니다 (비공개 저장소일 수 있어요)")
                        resp.code == 403 -> error("GitHub API 요청 한도를 초과했습니다. 잠시 후 다시 시도하세요")
                        !resp.isSuccessful -> error("GitHub API 오류 (HTTP ${resp.code})")
                        else -> {
                            val arr = json.parseToJsonElement(resp.body?.string() ?: "[]").jsonArray
                            arr.forEach { el ->
                                val o = el.jsonObject
                                val login = o["author"]
                                    ?.takeIf { it !is JsonNull }
                                    ?.jsonObject?.get("login")?.jsonPrimitive?.content
                                val name = o["commit"]?.jsonObject
                                    ?.get("author")?.jsonObject
                                    ?.get("name")?.jsonPrimitive?.content
                                authors += (login ?: name ?: "unknown")
                            }
                            arr.size
                        }
                    }
                }
                if (pageSize < 100) break
                page++
            }
            if (authors.isEmpty()) error("커밋이 없습니다")

            GitHubStats(
                repo = "$owner/$repo",
                totalCommits = authors.size,
                byAuthor = authors.groupingBy { it }.eachCount()
                    .entries.sortedByDescending { it.value }
                    .map { it.key to it.value }
            )
        }
    }
}
