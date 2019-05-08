package schema1

import com.xs0.gqlktx.GqlField
import com.xs0.gqlktx.trimToNull
import com.xs0.gqlktx.utils.NodeId
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList

object Data {
    val users = LinkedHashMap<NodeId, User>()
    val posts = LinkedHashMap<NodeId, Post>()
    val tags = LinkedHashMap<NodeId, Tag>()

    fun add(user: User): User {
        users[user.id] = user
        return user
    }

    fun add(post: Post): Post {
        posts[post.id] = post
        return post
    }

    fun add(tag: Tag): Tag {
        tags[tag.id] = tag
        return tag
    }

    init {
        val userA = add(User(1, "mslenc@gmail.com", null, null))
        val userB = add(User(2, "john@example.com", "John", null))
        val userC = add(User(3, "mary@example.com", "Mary", "Johnson"))

        val tagNew = add(Tag("new"))
        val tagOld = add(Tag("old"))

        add(Post(1, "First post ever", null, userA.id, setOf(tagOld.id)))
        add(Post(3, "Second post ever", "With text this time :)", userB.id, setOf()))
        add(Post(8, "Third post", "Something old, something new", userC.id, setOf(tagNew.id, tagOld.id)))
        add(Post(9, "Final post", "Again, some text", userA.id, setOf(tagNew.id)))
    }
}

class ContextualString(val value: String)

class TestContextProvider(value: String) {
    val ctxString = ContextualString(value)
}

object SchemaRoot {
    val query = QueryRoot
}

object QueryRoot {
    val users: Collection<User>
        get() { return Data.users.values }

    fun getPosts(): Collection<Post> {
        return Data.posts.values
    }

    @GqlField("tags")
    fun bibi(): CompletableFuture<Collection<Tag>> {
        val future = CompletableFuture<Collection<Tag>>()
        future.complete(Data.tags.values)
        return future
    }
}

data class Tag(
    val text: String
) {
    val id = NodeId.create("tag").add(text).build()

    fun getPosts(): Array<Post> {
        val res = ArrayList<Post>()

        for (post in Data.posts.values) {
            if (post.tagIds.contains(id))
                res.add(post)
        }

        return res.toTypedArray()
    }
}

class User(
    id: Int,
    val email: String,
    val firstName: String?,
    val lastName: String?
) {
    val id = NodeId.create("usr").add(id).build()

    val fullName: String?
        get() = ((firstName ?: "") + " " + (lastName ?: "")).trimToNull()

    fun getPosts(): List<Post> {
        val res = ArrayList<Post>()

        for (post in Data.posts.values) {
            if (post.ownerId == id)
                res.add(post)
        }

        return res
    }

    fun getContextCheck(cs: ContextualString, param: String): String {
        return cs.value + param
    }
}

class Post(
    id: Int,
    val title: String,
    val text: String?,
    internal val ownerId: NodeId,
    internal val tagIds: Set<NodeId>
) {
    val id = NodeId.create("pst").add(id).build()

    fun getOwner(): User {
        return Data.users[ownerId] ?: throw IllegalStateException("Missing owner")
    }

    @GqlField("tags")
    fun getEmTags(): Set<Tag> {
        if (tagIds.isEmpty())
            return emptySet()

        return tagIds.map { Data.tags[it]!! }.toSet()
    }
}