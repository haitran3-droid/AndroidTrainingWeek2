package com.example.aquadaily.ui.infor

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.aquadaily.R

/**
 * Model đại diện cho một bài viết mẹo/kiến thức về sức khỏe và nước uống.
 *
 * @property id Định danh duy nhất của bài viết.
 * @property title Resource ID chuỗi tiêu đề bài viết.
 * @property body Resource ID chuỗi nội dung chi tiết bài viết.
 * @property art Resource ID hình minh họa (drawable).
 * @property background Resource ID màu nền của thẻ minh họa (color).
 */
data class Article(
    val id: Long,
    @field:StringRes val title: Int,
    @field:StringRes val body: Int,
    @field:DrawableRes val art: Int,
    @field:ColorRes val background: Int
)

/**
 * Model đại diện cho một chuyên mục thông tin (section) gồm nhiều bài viết con.
 *
 * @property id Định danh duy nhất của chuyên mục.
 * @property title Resource ID chuỗi tên chuyên mục.
 * @property articles Danh sách các bài viết thuộc chuyên mục này.
 */
data class InfoSection(
    val id: Long,
    @field:StringRes val title: Int,
    val articles: List<Article>
)

/**
 * Danh mục tĩnh chứa toàn bộ dữ liệu bài viết kiến thức và lời khuyên bổ ích về nước.
 */
object ArticleCatalog {
    val sections: List<InfoSection> = listOf(
        InfoSection(
            1, R.string.section_balance, listOf(
                Article(
                    11,
                    R.string.article_water,
                    R.string.body_water,
                    R.drawable.art_water,
                    R.color.pale_blue
                ),
                Article(
                    12,
                    R.string.article_timing,
                    R.string.body_timing,
                    R.drawable.art_clock,
                    R.color.pale_blue
                ),
                Article(
                    13,
                    R.string.article_glass,
                    R.string.body_glass,
                    R.drawable.art_water,
                    R.color.pale_blue
                )
            )
        ),
        InfoSection(
            2, R.string.section_habits, listOf(
                Article(
                    21,
                    R.string.article_routine,
                    R.string.body_routine,
                    R.drawable.art_leaf,
                    R.color.pale_green
                ),
                Article(
                    22,
                    R.string.article_track,
                    R.string.body_track,
                    R.drawable.art_leaf,
                    R.color.pale_green
                ),
                Article(
                    23,
                    R.string.article_goal,
                    R.string.body_goal,
                    R.drawable.art_clock,
                    R.color.pale_green
                )
            )
        ),
        InfoSection(
            3, R.string.section_wellbeing, listOf(
                Article(
                    31,
                    R.string.article_break,
                    R.string.body_break,
                    R.drawable.art_heart,
                    R.color.pale_pink
                ),
                Article(
                    32,
                    R.string.article_outside,
                    R.string.body_outside,
                    R.drawable.art_water,
                    R.color.pale_pink
                ),
                Article(
                    33,
                    R.string.article_review,
                    R.string.body_review,
                    R.drawable.art_heart,
                    R.color.pale_pink
                )
            )
        )
    )

    /** Tìm kiếm bài viết theo ID. */
    fun find(id: Long): Article? =
        sections.asSequence().flatMap { it.articles.asSequence() }.find { it.id == id }
}