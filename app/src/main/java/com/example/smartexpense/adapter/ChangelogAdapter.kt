package com.example.smartexpense.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartexpense.R
import com.example.smartexpense.databinding.ItemChangelogBulletBinding
import com.example.smartexpense.databinding.ItemChangelogEarlierBinding
import com.example.smartexpense.databinding.ItemChangelogGroupBinding
import com.example.smartexpense.databinding.ItemChangelogHeaderBinding
import com.example.smartexpense.databinding.ItemChangelogVersionBinding
import com.example.smartexpense.ui.changelog.Change
import com.example.smartexpense.ui.changelog.ChangeKind
import com.example.smartexpense.ui.changelog.VersionEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Nhật ký thay đổi (mục 5.8): dòng thời gian các phiên bản.
 *
 * Cấu trúc dòng:
 * - Header: icon app + chip phiên bản đang cài
 * - Version: tiêu đề v1.x + tóm tắt (Hôm nay · N tính năng...), bấm để mở/thu chi tiết
 * - Earlier: nhãn "Phiên bản trước" giữa phiên bản cài và các bản cũ hơn
 *
 * Chi tiết mỗi phiên bản gồm các thẻ nhóm (New / Fixes / Removes); mỗi thẻ liệt kê
 * tối đa [MAX_BULLETS] dòng, nút "Hiển thị tất cả (N)" để mở hết / "Thu gọn".
 */
class ChangelogAdapter(
    private val versions: List<VersionEntry>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private sealed interface Row {
        data object Header : Row
        data class Version(val index: Int) : Row
        data object Earlier : Row
    }

    private val rows: List<Row> = buildList {
        add(Row.Header)
        val latestIndex = versions.indexOfFirst { it.isLatest }.takeIf { it >= 0 }
        versions.forEachIndexed { index, entry ->
            // Nhãn "Phiên bản trước" chen trước các bản cũ hơn bản đang cài
            if (latestIndex != null && index == latestIndex + 1) add(Row.Earlier)
            add(Row.Version(index))
        }
    }

    /** Index phiên bản đang mở chi tiết (chỉ 1 mở tại một thời điểm). */
    private var expandedVersion: Int = -1

    /** Cặp "verIdx:groupIdx" đang hiển thị toàn bộ bullet (đã bấm Hiển thị tất cả). */
    private val expandedGroups = mutableSetOf<String>()

    override fun getItemCount(): Int = rows.size

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> TYPE_HEADER
        is Row.Version -> TYPE_VERSION
        is Row.Earlier -> TYPE_EARLIER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(
                ItemChangelogHeaderBinding.inflate(inflater, parent, false)
            )
            TYPE_EARLIER -> EarlierHolder(
                ItemChangelogEarlierBinding.inflate(inflater, parent, false)
            )
            else -> VersionHolder(
                ItemChangelogVersionBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> bindHeader(holder as HeaderHolder)
            is Row.Version -> bindVersion(holder as VersionHolder, row.index)
            is Row.Earlier -> Unit // nhãn tĩnh đã có sẵn trong layout
        }
    }

    private fun bindHeader(holder: HeaderHolder) {
        // Chip "SmartExpense • v1.3.1": tên app + phiên bản đang cài
        holder.binding.tvAppChip.text = holder.binding.root.context.getString(
            R.string.changelog_app_chip,
            holder.binding.root.context.getString(R.string.app_name),
            com.example.smartexpense.BuildConfig.VERSION_NAME
        )
    }

    private fun bindVersion(holder: VersionHolder, versionIndex: Int) {
        val entry = versions[versionIndex]
        val binding = holder.binding
        val context = binding.root.context

        binding.tvVersion.text = context.getString(R.string.changelog_version_short, entry.version)
        binding.tvInstalled.visibility =
            if (entry.isLatest) View.VISIBLE else View.GONE
        binding.tvSummary.text = buildSummary(context, entry)

        val expanded = versionIndex == expandedVersion
        binding.llDetails.visibility = if (expanded) View.VISIBLE else View.GONE
        binding.ivExpand.animate()
            .rotation(if (expanded) 90f else 0f)
            .setDuration(150L)
            .start()

        if (expanded) {
            // Nạp lại chi tiết mỗi lần bind để trạng thái nhóm phản ánh expandedGroups
            binding.llDetails.removeAllViews()
            buildGroups(binding.llDetails, versionIndex, entry)
        }

        binding.rowHeader.setOnClickListener {
            expandedVersion = if (expanded) -1 else versionIndex
            notifyDataSetChanged()
        }
    }

    /** "Hôm nay · 3 tính năng · 2 bản sửa lỗi · 1 mục đã bỏ" */
    private fun buildSummary(context: Context, entry: VersionEntry): String {
        val parts = mutableListOf<String>()

        val date = runCatching {
            LocalDate.parse(entry.date, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }.getOrNull()
        if (date != null) {
            val daysAgo = ChronoUnit.DAYS.between(date, LocalDate.now())
            parts.add(
                when {
                    daysAgo <= 0L -> context.getString(R.string.changelog_time_today)
                    daysAgo == 1L -> context.getString(R.string.changelog_time_yesterday)
                    else -> context.resources.getQuantityString(
                        R.plurals.changelog_days_ago, daysAgo.toInt(), daysAgo.toInt()
                    )
                }
            )
        }

        val added = entry.changes.count { it.kind == ChangeKind.ADDED }
        val changed = entry.changes.count { it.kind == ChangeKind.CHANGED }
        val removed = entry.changes.count { it.kind == ChangeKind.REMOVED }
        if (added > 0) parts.add(
            context.resources.getQuantityString(R.plurals.changelog_features, added, added)
        )
        if (changed > 0) parts.add(
            context.resources.getQuantityString(R.plurals.changelog_fixes, changed, changed)
        )
        if (removed > 0) parts.add(
            context.resources.getQuantityString(R.plurals.changelog_removes, removed, removed)
        )
        return parts.joinToString(" · ")
    }

    /** Thêm các thẻ nhóm New / Fixes / Removes vào ll_details. */
    private fun buildGroups(
        parent: ViewGroup,
        versionIndex: Int,
        entry: VersionEntry
    ) {
        val context = parent.context
        val inflater = LayoutInflater.from(context)

        groupSpecs.forEachIndexed { groupIndex, spec ->
            val changes = entry.changes.filter { it.kind == spec.kind }
            if (changes.isEmpty()) return@forEachIndexed

            val groupBinding = ItemChangelogGroupBinding.inflate(inflater, parent, false)
            val key = "$versionIndex:$groupIndex"
            val showAll = key in expandedGroups

            groupBinding.ivGroupIcon.setImageResource(spec.iconRes)
            // titleRes là R.plurals.* nên phải qua getQuantityString (setText(int) chỉ nhận string)
            groupBinding.tvGroupTitle.text = context.resources.getQuantityString(
                spec.titleRes, changes.size, changes.size
            )
            groupBinding.tvGroupCount.text = changes.size.toString()

            val bullets = if (showAll) changes else changes.take(MAX_BULLETS)
            bullets.forEachIndexed { bulletIndex, change ->
                val bulletBinding = ItemChangelogBulletBinding.inflate(
                    inflater, groupBinding.llBullets, false
                )
                bulletBinding.divider.visibility =
                    if (bulletIndex == 0) View.GONE else View.VISIBLE
                bulletBinding.dot.background?.let { drawable ->
                    androidx.core.graphics.drawable.DrawableCompat.wrap(drawable.mutate()).also {
                        androidx.core.graphics.drawable.DrawableCompat.setTint(
                            it, context.getColor(spec.dotColor)
                        )
                    }
                }
                bulletBinding.tvText.text = context.getString(change.text)
                groupBinding.llBullets.addView(bulletBinding.root)
            }

            if (changes.size > MAX_BULLETS) {
                groupBinding.btnShowAll.visibility = View.VISIBLE
                groupBinding.btnShowAll.setText(
                    if (showAll) R.string.changelog_show_less
                    else R.string.changelog_show_all
                )
                if (!showAll) {
                    groupBinding.btnShowAll.text = context.getString(
                        R.string.changelog_show_all, changes.size
                    )
                }
                groupBinding.btnShowAll.setOnClickListener {
                    if (showAll) expandedGroups.remove(key) else expandedGroups.add(key)
                    notifyItemChanged(versionRowPosition(versionIndex))
                }
            }

            parent.addView(groupBinding.root)
        }
    }

    /** Vị trí dòng RecyclerView của phiên bản (để refresh khi đổi trạng thái nhóm). */
    private fun versionRowPosition(versionIndex: Int): Int =
        rows.indexOfFirst { it is Row.Version && it.index == versionIndex }

    /** [TimelineDecoration] hỏi adapter dòng nào là phiên bản (để vẽ chấm dòng thời gian). */
    fun isVersionRow(position: Int): Boolean = rows.getOrNull(position) is Row.Version

    class HeaderHolder(val binding: ItemChangelogHeaderBinding) :
        RecyclerView.ViewHolder(binding.root)

    class VersionHolder(val binding: ItemChangelogVersionBinding) :
        RecyclerView.ViewHolder(binding.root)

    class EarlierHolder(val binding: ItemChangelogEarlierBinding) :
        RecyclerView.ViewHolder(binding.root)

    /**
     * Vẽ dòng thời gian dọc + chấm xanh cho từng phiên bản (dùng chung vĩnh viễn
     * với [ChangelogActivity] - mục 5.8).
     */
    class TimelineDecoration(
        context: Context,
        private val adapter: ChangelogAdapter
    ) : RecyclerView.ItemDecoration() {

        private val density = context.resources.displayMetrics.density
        private val lineX = 14f * density
        private val dotYOffset = 22f * density
        private val dotRadius = 5f * density
        private val lineWidth = 2f * density

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.divider)
            strokeWidth = lineWidth
            style = Paint.Style.STROKE
        }
        private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.blue_primary)
            style = Paint.Style.FILL
        }
        private val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.surface)
            style = Paint.Style.FILL
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            var firstY = -1f
            var lastY = -1f
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val pos = parent.getChildAdapterPosition(child)
                if (pos == RecyclerView.NO_POSITION || !adapter.isVersionRow(pos)) continue
                val y = child.top + dotYOffset
                if (firstY < 0) firstY = y
                lastY = y
            }
            if (firstY >= 0 && lastY > firstY) {
                c.drawLine(lineX, firstY, lineX, lastY, linePaint)
            }
        }

        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                val pos = parent.getChildAdapterPosition(child)
                if (pos == RecyclerView.NO_POSITION || !adapter.isVersionRow(pos)) continue
                val cx = lineX
                val cy = child.top + dotYOffset
                c.drawCircle(cx, cy, dotRadius + lineWidth, dotBorderPaint)
                c.drawCircle(cx, cy, dotRadius, dotPaint)
            }
        }
    }

    /** Thông số 1 nhóm bullet: loại thay đổi, tên nhóm, icon, màu chấm. */
    private data class GroupSpec(
        val kind: ChangeKind,
        val titleRes: Int,
        val iconRes: Int,
        val dotColor: Int
    )

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_VERSION = 1
        const val TYPE_EARLIER = 2

        /** Số bullet hiển thị mặc định mỗi nhóm trước khi bấm "Hiển thị tất cả". */
        const val MAX_BULLETS = 3

        val groupSpecs = listOf(
            GroupSpec(
                kind = ChangeKind.ADDED,
                titleRes = R.plurals.changelog_group_new,
                iconRes = R.drawable.ic_add,
                dotColor = R.color.green
            ),
            GroupSpec(
                kind = ChangeKind.CHANGED,
                titleRes = R.plurals.changelog_group_fix,
                iconRes = R.drawable.ic_edit,
                dotColor = R.color.blue_primary
            ),
            GroupSpec(
                kind = ChangeKind.REMOVED,
                titleRes = R.plurals.changelog_group_remove,
                iconRes = R.drawable.ic_delete,
                dotColor = R.color.red
            )
        )
    }
}
