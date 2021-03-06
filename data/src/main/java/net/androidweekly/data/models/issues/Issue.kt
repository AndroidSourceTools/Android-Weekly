package net.androidweekly.data.models.issues

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize
import net.androidweekly.data.models.items.IssueItem
import net.androidweekly.data.models.items.IssueTitle
import net.androidweekly.data.models.items.Item
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import java.text.SimpleDateFormat
import java.util.*


/**
 * Project: Android Weekly
 * Created: Oct 20, 2019
 *
 * @author Mohamed Hamdan
 */
@Parcelize
@Entity(tableName = "issues")
@Root(name = "item", strict = false)
data class Issue(

    @field:Element(name = "title", required = false)
    var title: String? = null,

    @PrimaryKey
    @field:Element(name = "link", required = false)
    var link: String = "",

    @field:Element(name = "description", required = false)
    var description: String? = null,

    @field:Element(name = "pubDate", required = false)
    var publishDate: String? = null
) : Parcelable {

    @Ignore
    private var isNextItemSponsored = false

    @Ignore
    private val allItems = mutableListOf<Item>()

    fun getItems(): List<Item> {
        if (allItems.size > 0) {
            return allItems
        }

        startParseDescription()

        return allItems
    }

    private fun startParseDescription() {
        val document = Jsoup.parse(description)
        val items = document.getElementsByClass("wrapper")
        parseWrapperClasses(items)
    }

    private fun parseWrapperClasses(items: Elements) {
        items.forEach { item ->
            val title = item.getElementsByTag("h2")
            if (title.size > 0) {
                parseIssueTitle(title)
            } else {
                parseOtherWrapper(item)
            }
        }
    }

    private fun parseIssueTitle(title: Elements) {
        allItems.add(IssueTitle(title = title.first().text()))
    }

    private fun parseOtherWrapper(item: org.jsoup.nodes.Element) {
        checkIsSponsored(item)

        val headline = item.getElementsByClass("article-headline")
        if (headline.size > 0) {
            parseIssueItem(item, headline)
        }
    }

    private fun checkIsSponsored(item: org.jsoup.nodes.Element) {
        val sponsored = item.getElementsByTag("h5").firstOrNull()
        if (sponsored != null) {
            isNextItemSponsored = true
        }
    }

    private fun parseIssueItem(item: org.jsoup.nodes.Element, headline: Elements) {
        val articleImage = item.getElementsByTag("img").firstOrNull()?.attr("src")
        val articleLink = headline.first().attr("href")
        val articleTitle = headline.first().text()
        val description = item.getElementsByTag("p").firstOrNull()?.text()

        allItems.add(
            IssueItem(
                title = articleTitle,
                description = description,
                image = articleImage,
                link = articleLink,
                host = Uri.parse(articleLink).host,
                isSponsored = isNextItemSponsored
            )
        )
        isNextItemSponsored = false
    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getFormattedDate(): String? {
        return try {
            val pattern = "E, dd MMM yyyy HH:mm:ss Z"
            val formattedPattern = "E, dd MMM yyyy"

            val simpleDateFormat = SimpleDateFormat(pattern, Locale.ENGLISH)
            val formattedSimpleDateFormat = SimpleDateFormat(formattedPattern, Locale.ENGLISH)

            val date: String = simpleDateFormat.format(simpleDateFormat.parse(publishDate))
            val formattedDate: String = formattedSimpleDateFormat.format(simpleDateFormat.parse(date))

            formattedDate
        } catch (e: Exception) {
            publishDate
        }
    }
}
