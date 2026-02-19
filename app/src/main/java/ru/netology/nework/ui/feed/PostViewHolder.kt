import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nework.model.Post

class PostViewHolder(
    private val textView: TextView
) : RecyclerView.ViewHolder(textView) {

    fun bind(post: Post) {
        textView.text = post.content
    }
}
