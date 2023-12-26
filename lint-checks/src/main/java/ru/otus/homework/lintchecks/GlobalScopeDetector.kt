package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiElement
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UReferenceExpression
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.kotlin.KotlinUSimpleReferenceExpression

@Suppress("UnstableApiUsage")
class GlobalScopeDetector: Detector(), Detector.UastScanner {

    companion object {

        private const val ISSUE_ID = "GlobalScopeUsage"
        private const val BRIEF_DESCRIPTION = "Не используйте GlobalScope в своём коде."

        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = BRIEF_DESCRIPTION,
            implementation = Implementation(
                GlobalScopeDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            moreInfo = "https://elizarov.medium.com/the-reason-to-avoid-globalscope-835337445abc",
            category = Category.create("Custom lint checks", 10),
            priority = 10,
            severity = Severity.ERROR
        )
    }

    override fun getApplicableReferenceNames() = listOf("GlobalScope")

    override fun visitReference(
        context: JavaContext,
        reference: UReferenceExpression,
        referenced: PsiElement
    ) {
        var fix = ""
        reference.getParentOfType<UClass>()?.run {

            if (context.evaluator.extendsClass(
                    this,
                    "androidx.lifecycle.ViewModel"
                )
            )
                fix = "viewModelScope"
            else
                if (context.evaluator.extendsClass(
                        this,
                        "androidx.fragment.app.Fragment"
                    )
                )
                    fix = "lifecycleScope"
        }

        var ktx = false

        if (fix.isNotEmpty()) {
            ktx = context.evaluator.dependencies?.getAll()?.any { lib ->
                lib.identifier.contains(if (fix == "viewModelScope") "androidx.lifecycle:lifecycle-viewmodel-ktx" else "androidx.lifecycle:lifecycle-runtime-ktx")
            } ?: false

            var expression = reference.getParentOfType<UCallExpression>()
            while (expression != null) {
                if (expression.receiver is KotlinUSimpleReferenceExpression) {
                    if ((expression.receiver as KotlinUSimpleReferenceExpression).identifier == fix) {
                        fix = "-"
                        expression = null
                    } else expression =
                        (expression.receiver as KotlinUSimpleReferenceExpression).getParentOfType()
                } else
                    expression = null
            }
        }

        context.report(
            ISSUE,
            reference,
            context.getLocation(reference),
            BRIEF_DESCRIPTION,
            if ((fix.isEmpty()) or (!ktx)) null
            else
                fix()
                    .replace()
                    .text(reference.resolvedName)
                    .with(if (fix == "-") null else fix)
                    .build()
        )

    }





//    override fun getApplicableUastTypes(): List<Class<out UElement>> {
//        return listOf(UCallExpression::class.java)
//    }
//
//    override fun createUastHandler(context: JavaContext): UElementHandler {
//
//        return object : UElementHandler() {
//
//            override fun visitCallExpression(node: UCallExpression) {
//
//                if (node.receiver is KotlinUSimpleReferenceExpression) {
//                    if ((node.receiver as KotlinUSimpleReferenceExpression).identifier == "GlobalScope") {
//                        var fix = ""
//                        node.getParentOfType<UClass>()?.run {
//                            if (context.evaluator.extendsClass(
//                                    this,
//                                    "androidx.lifecycle.ViewModel"
//                                )
//                            )
//                                fix = "viewModelScope"
//                            else
//                                if (context.evaluator.extendsClass(
//                                        this,
//                                        "androidx.fragment.app.Fragment"
//                                    )
//                                )
//                                    fix = "lifecycleScope"
//                        }
//                        if (fix.isNotEmpty()) {
//                            var expression = node.getParentOfType<UCallExpression>()
//                            while (expression != null) {
//                                if (expression.receiver is KotlinUSimpleReferenceExpression) {
//                                    if ((expression.receiver as KotlinUSimpleReferenceExpression).identifier == fix) {
//                                        fix = "-"
//                                        expression = null
//                                    } else expression =
//                                        (expression.receiver as KotlinUSimpleReferenceExpression).getParentOfType()
//                                } else
//                                    expression = null
//                            }
//                        }
//
//                        context.report(
//                            ISSUE,
//                            node.receiver,
//                            context.getLocation(node.receiver),
//                            BRIEF_DESCRIPTION,
//                            if (fix.isEmpty()) null
//                            else
//                                fix()
//                                    .replace()
//                                    .text("GlobalScope")
//                                    .with(if (fix == "-") null else fix)
//                                    .build()
//                        )
//
//                    }
//                }
//            }
//        }
//    }

}
