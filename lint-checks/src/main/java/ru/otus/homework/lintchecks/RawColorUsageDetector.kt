package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.XmlContext
import org.w3c.dom.Attr
import org.w3c.dom.CharacterData

@Suppress("UnstableApiUsage")
class RawColorUsageDetector: Detector(), Detector.XmlScanner {

    private val palette = mapOf<String, String>().toMutableMap()
    private val locations = listOf<Location>().toMutableList()

    companion object {

        private const val ISSUE_ID = "RawColorUsage"
        private const val BRIEF_DESCRIPTION = "Использование цвета, отсутствующего в палитре 'colors.xml'."

        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = BRIEF_DESCRIPTION,
            implementation = Implementation(
                RawColorUsageDetector::class.java,
                Scope.ALL_RESOURCES_SCOPE
            ),
            moreInfo = "",
            category = Category.create("Custom lint checks", 10),
            priority = 10,
            severity = Severity.ERROR
        )
    }

    override fun getApplicableAttributes(): Collection<String> {
        return listOf("color", "tint", "fillColor", "background", "backgroundTint", "name")
    }

    override fun visitAttribute(context: XmlContext, attribute: Attr) {
        if (context.file.name == "colors.xml") {
            val color = (attribute.ownerElement.firstChild as CharacterData).data.removePrefix("@color/")
            palette[attribute.value] = color
            palette[color] = attribute.value
        }
        else
            if (!attribute.value.startsWith("@drawable/"))
                locations.add(context.getValueLocation(attribute))
    }

    override fun afterCheckRootProject(context: Context) {

        if ((palette.isEmpty()) || (locations.isEmpty()))
            return

        locations.forEach { location ->
            val attr = location.source as Attr
            val createReport: Boolean
            val f = if (palette[attr.value.removePrefix("@color/")] == null) {
                createReport = true
                null
            }
            else
                if (attr.value.startsWith("#")) {
                    createReport = true
                    fix().replace().text(attr.value).with(palette[attr.value]).build()
                } else {
                    createReport = false
                    null
                }

            if (createReport)
                context.report(
                    ISSUE,
                    location,
                    BRIEF_DESCRIPTION,
                    f
                )

        }

    }

}
