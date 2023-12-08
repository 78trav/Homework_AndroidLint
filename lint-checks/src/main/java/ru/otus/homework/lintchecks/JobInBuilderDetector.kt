package ru.otus.homework.lintchecks

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UBinaryExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.USimpleNameReferenceExpression
import org.jetbrains.uast.getParentOfType

private const val VIEW_MODEL_SCOPE = "viewModelScope"

@Suppress("UnstableApiUsage")
class JobInBuilderDetector : Detector(), Detector.UastScanner {

    companion object {

        private const val ISSUE_ID = "JobInBuilderUsage"
        private const val BRIEF_DESCRIPTION = "Использование экземпляра 'kotlinx.coroutines.Job' внутри корутин билдера."

        val ISSUE = Issue.create(
            id = ISSUE_ID,
            briefDescription = BRIEF_DESCRIPTION,
            explanation = BRIEF_DESCRIPTION,
            implementation = Implementation(
                JobInBuilderDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            ),
            moreInfo = "https://medium.com/androiddevelopers/exceptions-in-coroutines-ce8da1ec060c\n" +
                    "https://www.lukaslechner.com/7-common-mistakes-you-might-be-making-when-using-kotlin-coroutines",
            category = Category.create("Custom lint checks", 10),
            priority = 10,
            severity = Severity.ERROR
        )
    }

    override fun getApplicableMethodNames(): List<String> {
        return listOf("launch", "async")
    }

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {

        val inViewModelScope = if (context.evaluator.extendsClass(
                node.getParentOfType<UClass>(),
                "androidx.lifecycle.ViewModel"
            )
        ) {
            if (node.receiver is USimpleNameReferenceExpression) ((node.receiver as USimpleNameReferenceExpression).identifier == VIEW_MODEL_SCOPE)
            else
            {
                var r = false
                var expression = node.getParentOfType<USimpleNameReferenceExpression>()
                while (expression != null) {
                    if (expression.identifier == VIEW_MODEL_SCOPE) {
                        r = true
                        expression = null
                    } else expression = expression.getParentOfType()
                }
                r
            }
        } else false

        context.evaluator.computeArgumentMapping(node, method).forEach {
            if (it.value.name == "context") {

                var op: UExpression? = null
                if (it.key is UBinaryExpression) {
                    (it.key as UBinaryExpression).operands.forEach { e ->
                        e.getExpressionType()?.superTypes?.forEach { t ->
                            if (t.canonicalText == "kotlinx.coroutines.Job")
                                op = e
                        }
                    }
                } else op = it.key

                if (op != null) {
                    val t = try {
                        op!!.getExpressionType()?.canonicalText ?: ""
                    } catch (e: Exception) {
                        ""
                    }

                    if (t == "kotlinx.coroutines.NonCancellable") {
                        context.report(
                            ISSUE,
                            op,
                            context.getLocation(op),
                            BRIEF_DESCRIPTION,
                            fix().replace().range(context.getLocation(node)).text("${VIEW_MODEL_SCOPE}.${node.methodName}(NonCancellable)").with("withContext(Dispatchers.Default)").build()
                        )
                    } else
                        if (t.isNotEmpty()) {

                            if (inViewModelScope) {
                                if (op!!.isSupervisorJob())
                                    context.report(
                                        ISSUE,
                                        op,
                                        context.getLocation(op),
                                        BRIEF_DESCRIPTION,
                                        fix().replace().text("SupervisorJob()").with(null).build()
                                    )
                            } else
                                context.report(
                                    ISSUE,
                                    op,
                                    context.getLocation(op),
                                    BRIEF_DESCRIPTION
                                )
                        }
                }
            }
        }

    }

}

fun UExpression.isSupervisorJob() = if (this is UCallExpression) (methodName == "SupervisorJob") else false
