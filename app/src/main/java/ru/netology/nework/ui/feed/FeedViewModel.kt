package ru.netology.nework.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ru.netology.nework.model.Attachment
import ru.netology.nework.model.AttachmentType
import ru.netology.nework.model.Post

class FeedViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    init {
        loadMock()
    }

    private fun loadMock() {
        _posts.value = listOf(
            Post(
                id = 1,
                author = "Leo Lipshutz",
                authorAvatar = null,
                published = System.currentTimeMillis(),
                content = "Шляпа — это головной убор, который носили в Древней Греции. В наше время шляпы носят для защиты от солнца или просто для красоты.",
                likedByMe = false,
                likes = 10,
                attachment = null,
                link = null,
                authorId = 1
            ),
            Post(
                id = 2,
                author = "Leo Lipshutz",
                authorAvatar = null,
                published = System.currentTimeMillis(),
                content = "Шляпа — это головной убор, который носили в Древней Греции. В наше время шляпы носят для защиты от солнца или просто для красоты.",
                likedByMe = false,
                likes = 10,
                attachment = Attachment(
                    url = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRnTQ04WdzI8_nx_D7_gGQK5nyjsunQOHNm5g&s",
                    type = AttachmentType.IMAGE
                ),

                link = null,
                authorId = 1
            ),
            Post(
                id = 3,
                author = "Leo Lipshutz",
                authorAvatar = null,
                published = System.currentTimeMillis(),
                content = "Видео пост",
                likedByMe = false,
                likes = 10,
                attachment = Attachment(
                    url = "https://img.youtube.com/vi/KnQJbQXjiH0/0.jpg",
                    type = AttachmentType.VIDEO
                ),
                link = "https://www.youtube.com/watch?v=KnQJbQXjiH0",
                authorId = 1
            ),
                    Post(
                    id = 4,
            author = "Leo Lipshutz",
            authorAvatar = null,
            published = System.currentTimeMillis(),
            content = "Видео пост",
            likedByMe = false,
            likes = 10,
            attachment = Attachment(
                url = "https://img.youtube.com/vi/RCrHfrb9uWA/hqdefault.jpg",
                type = AttachmentType.VIDEO
            ),
            link = "https://www.youtube.com/watch?v=RCrHfrb9uWA",
            authorId = 1
        )

        )
    }


    fun toggleLike(postId: Long) {
        _posts.value = _posts.value?.map {
            if (it.id == postId) {
                it.copy(
                    likedByMe = !it.likedByMe,
                    likes = if (!it.likedByMe) it.likes + 1 else it.likes - 1
                )
            } else it
        }
    }

    fun isLoggedIn(): Boolean {
        return false
    }
}

