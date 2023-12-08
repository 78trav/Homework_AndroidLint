package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test

class GlobalScopeDetectorTest {

    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(GlobalScopeDetector.ISSUE)

    @Suppress("CheckResult")
    @Test
    fun `usage GlobalScope test 1`() {

        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package test.pkg
                        
                        class ViewModel

                        class ViewModelTwo : ViewModel

                        interface ViewModelInterface

                        object GlobalScope {
                            fun launch(action: () -> Unit) {}
                        }

                        class Test : ViewModelTwo(), ViewModelInterface {

//                            fun case2() {
//                                viewModelScope.launch {
//                                    val deferred = GlobalScope.async {
//                                        delay(1000)
//                                        "Hello World"
//                                    }
//                                    println(deferred.await())
//                                }
//                            }

                            fun test() {
                                GlobalScope.launch {
                                    delay(1000)
                                    println(123)
                                }
                            }
                        }
                    """.trimIndent()
                )
            ).run()
            .expect("src/test/pkg/ViewModel.kt:26: Error: Не используйте GlobalScope в своём коде. [GlobalScopeUsage]\n" +
                    "                                GlobalScope.launch {\n" +
                    "                                ~~~~~~~~~~~\n" +
                    "1 errors, 0 warnings")

    }

    @Suppress("CheckResult")
    @Test
    fun `usage GlobalScope test 2`() {

        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package test.pkg
                        
                        class ViewModel

                        object GlobalScope {
                            fun launch(action: () -> Unit) {}
                        }

                        object viewModelScope {
                            fun launch(action: () -> Unit) {}
                        }

                        class GlobalScopeTestCase(private val scope: CoroutineScope) : ViewModel() {
                        
                            fun case1() {
                                GlobalScope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                                GlobalScope.actor<String> {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        
                            fun case2() {
                                viewModelScope.launch {
                                    val deferred = GlobalScope.async {
                                        delay(1000)
                                        "Hello World"
                                    }
                                    println(deferred.await())
                                }
                            }
                        
                            fun case3() {
                                scope.launch {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        
                        }
                    """.trimIndent()
                )
            ).run()
            .expect("src/test/pkg/ViewModel.kt:16: Error: Не используйте GlobalScope в своём коде. [GlobalScopeUsage]\n" +
                    "        GlobalScope.launch {\n" +
                    "        ~~~~~~~~~~~\n" +
                    "src/test/pkg/ViewModel.kt:20: Error: Не используйте GlobalScope в своём коде. [GlobalScopeUsage]\n" +
                    "        GlobalScope.actor<String> {\n" +
                    "        ~~~~~~~~~~~\n" +
                    "src/test/pkg/ViewModel.kt:28: Error: Не используйте GlobalScope в своём коде. [GlobalScopeUsage]\n" +
                    "            val deferred = GlobalScope.async {\n" +
                    "                           ~~~~~~~~~~~\n" +
                    "3 errors, 0 warnings")

    }

}
