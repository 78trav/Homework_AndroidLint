package ru.otus.homework.lintchecks

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Assert.*
import org.junit.Test

class JobInBuilderDetectorTest {
    private val lintTask = TestLintTask.lint()
        .allowMissingSdk()
        .issues(JobInBuilderDetector.ISSUE)

    private val stubs = TestFiles.kotlin(
        """
                    package kotlinx.coroutines
                    
                    class Job

                    object NonCancellable: Job

                    class SupervisorJob : Job
            """
    )

    @Suppress("CheckResult")
    @Test
    fun `usage kotlinx-coroutines-Job test`() {
        lintTask
            .files(
                LintDetectorTest.kotlin(
                    """
                        package test.pkg

                        import kotlinx.coroutines.Job
                        import kotlinx.coroutines.NonCancellable

                        object viewModelScope {
                            fun launch(context: Job, start: Int = 0, block: () -> Unit) : Job
                        }
                        
                        class ViewModel

                        class JobInBuilderTestCase(
                            private val job: Job
                        ) : ViewModel() {
                        
                            fun case1() {
                                viewModelScope.launch(SupervisorJob() + Dispatchers.IO) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        
                            fun case2() {
                                viewModelScope.launch(start = CoroutineStart.DEFAULT, context = Job()) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        
                            fun case3() {
                                viewModelScope.launch(job) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        
                            fun case4() {
                                viewModelScope.launch(NonCancellable) {
                                    delay(1000)
                                    println("Hello World")
                                }
                            }
                        }
                        """.trimIndent()
                ), stubs
            )
            .testModes(TestMode.PARENTHESIZED)
            .run()
            .expect("src/test/pkg/viewModelScope.kt:17: Error: Использование экземпляра 'kotlinx.coroutines.Job' внутри корутин билдера. [JobInBuilderUsage]\n" +
                    "        viewModelScope.launch(((SupervisorJob()) + Dispatchers.IO)) {\n" +
                    "                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
                    "src/test/pkg/viewModelScope.kt:24: Error: Использование экземпляра 'kotlinx.coroutines.Job' внутри корутин билдера. [JobInBuilderUsage]\n" +
                    "        viewModelScope.launch(start = CoroutineStart.DEFAULT, context = (Job())) {\n" +
                    "                                                                        ~~~~~~~\n" +
                    "src/test/pkg/viewModelScope.kt:31: Error: Использование экземпляра 'kotlinx.coroutines.Job' внутри корутин билдера. [JobInBuilderUsage]\n" +
                    "        viewModelScope.launch(job) {\n" +
                    "                              ~~~\n" +
                    "src/test/pkg/viewModelScope.kt:38: Error: Использование экземпляра 'kotlinx.coroutines.Job' внутри корутин билдера. [JobInBuilderUsage]\n" +
                    "        viewModelScope.launch(NonCancellable) {\n" +
                    "                              ~~~~~~~~~~~~~~\n" +
                    "4 errors, 0 warnings")

    }

}
