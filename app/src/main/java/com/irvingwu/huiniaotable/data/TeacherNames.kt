package com.irvingwu.huiniaotable.data

/**
 * 清洗教师名。
 *
 * 正方教务系统「教师」字段会把职称拼在姓名后面，例如：
 *  - `张伟高等学校教师/副教授`
 *  - `艾力·买买提高等学校教师/讲师（高校）`
 *  - `王敏副教授`、`李四/讲师`、`张三（教授）`
 *
 * 这里只保留姓名，职称一律去掉（界面不需要显示职称）。
 * 少数民族姓名里的间隔号「·」会保留。
 */
object TeacherNames {

    /** 职称关键字。取「最早出现的关键字」来截断，所以长关键字和短关键字一起列出也没问题。 */
    private val TITLE_KEYWORDS = listOf(
        "高等学校教师", "教授", "副教授", "讲师", "助教", "教员", "教师",
        "高级实验师", "实验师", "助理实验师", "实验员",
        "研究馆员", "副研究馆员", "馆员", "助理馆员",
        "研究员", "副研究员", "助理研究员",
        "教授级高级工程师", "高级工程师", "工程师", "助理工程师", "技术员",
        "主任医师", "副主任医师", "主治医师", "医师",
        "高级经济师", "经济师", "高级会计师", "会计师", "审计师", "统计师",
        "高级技师", "技师", "编审", "副编审", "编辑",
        "辅导员", "讲师（高校）",
    )

    /** 姓名后面可能跟着的分隔符。 */
    private const val TRAILING = "/\\、,，-—(（ \u00a0"

    fun clean(raw: String?): String? {
        val text = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val names = text.split(',', '，', '、')
            .map { stripTitle(it.trim()) }
            .filter { it.isNotEmpty() }
        return names.joinToString("、").takeIf { it.isNotEmpty() }
    }

    private fun stripTitle(value: String): String {
        if (value.isEmpty()) return ""
        val cut = TITLE_KEYWORDS
            .mapNotNull { keyword -> value.indexOf(keyword).takeIf { it >= 0 } }
            .minOrNull()
            ?: return value.trimEnd(*TRAILING.toCharArray())
        // 整个字段只有职称、没有姓名，那就当作没有教师信息
        if (cut == 0) return ""
        return value.take(cut).trimEnd(*TRAILING.toCharArray()).trimStart()
    }
}
