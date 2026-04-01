import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import ru.netology.nework.data.api.dto.AttachmentDto
import ru.netology.nework.data.api.dto.CoordinatesDto

@JsonClass(generateAdapter = true)
data class CreatePostRequest(
    @Json(name = "id") val id: Long = 0,  // 0 для нового поста, существующий ID для обновления
    @Json(name = "content") val content: String,
    @Json(name = "attachment") val attachment: AttachmentDto? = null,
    @Json(name = "coords") val coords: CoordinatesDto? = null,
    @Json(name = "mentionIds") val mentionIds: List<Long> = emptyList()
)