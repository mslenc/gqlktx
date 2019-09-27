package schema1

import com.xs0.gqlktx.GqlField
import com.xs0.gqlktx.GqlInterface
import com.xs0.gqlktx.trimToNull
import com.xs0.gqlktx.utils.Maybe
import com.xs0.gqlktx.utils.NodeId
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.collections.ArrayList

object Data {
    val users = LinkedHashMap<NodeId, User>()
    val posts = LinkedHashMap<NodeId, Post>()
    val tags = LinkedHashMap<NodeId, Tag>()
    val orgs = LinkedHashMap<NodeId, Org>()

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

    fun add(org: Org): Org {
        orgs[org.id] = org
        return org
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

        val orgA = add(
            Org(11, "TheOrg",
                industry = Industry(EnumElementData(41, "TheIndustry", null, EnumElementType.INDUSTRY)),
                size = CompanySize(EnumElementData(55, "Big", null, EnumElementType.COMPANY_SIZE)),
                rating = ClientRating(EnumElementData(66, "Very good", "Used for very good clients", EnumElementType.CLIENT_RATING))
            ))

        val orgB = add(
            Org(44, "TheCompany",
                industry = Industry(EnumElementData(41, "The Other Industry", null, EnumElementType.INDUSTRY))
            ))
    }
}



class ContextualString(val value: String)

class TestContextProvider(value: String) {
    val ctxString = ContextualString(value)
}

data class PostFilters(
    val titleWords: String? = null,
    val keywords: Maybe<String?>? = null
)

object SchemaRoot {
    val query = QueryRoot
}

object QueryRoot {
    @GqlField
    val users: Collection<User>
        get() { return Data.users.values }

    @GqlField
    fun getPosts(filters: PostFilters?): Collection<Post> {
        return Data.posts.values
    }

    @GqlField
    fun getOrgs(): List<Org> {
        return Data.orgs.values.toList()
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

class Org(
    id: Int,
    val name: String,
    val industry: Industry? = null,
    val size: CompanySize? = null,
    val rating: ClientRating? = null
) {
    val id = NodeId.create("org").add(id).build()

    @GqlField
    fun getAllEnums(): List<EnumElement> {
        return listOfNotNull(industry, size, rating)
    }
}

enum class EnumElementType {
    INDUSTRY,
    COMPANY_SIZE,
    CLIENT_RATING
}

data class EnumElementData(
    val id: Long,
    val name: String,
    val comments: String?,
    val type: EnumElementType
) {
    companion object {
        fun createWrapper(data: EnumElementData): EnumElement {
            return when (data.type) {
                EnumElementType.INDUSTRY -> Industry(data)
                EnumElementType.COMPANY_SIZE -> CompanySize(data)
                EnumElementType.CLIENT_RATING -> ClientRating(data)
            }
        }
    }
}

@GqlInterface
abstract class EnumElement(internal val data: EnumElementData) {
    @GqlField
    val id = NodeId.create(data.type.toString().substring(0, 3)).add(data.id).build()

    @GqlField
    fun getName(): String = data.name
}

class Industry(data: EnumElementData) : EnumElement(data)
class CompanySize(data: EnumElementData) : EnumElement(data)
class ClientRating(data: EnumElementData) : EnumElement(data) {
    @GqlField
    fun getComments() = data.comments
}

