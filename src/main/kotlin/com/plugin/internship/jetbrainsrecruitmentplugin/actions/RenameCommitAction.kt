package com.plugin.internship.jetbrainsrecruitmentplugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator
import git4idea.GitVcs
import git4idea.branch.GitBranchUtil
import git4idea.checkin.GitCheckinEnvironment
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

class RenameCommitAction:DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val newMessage = Messages.showInputDialog(
            project,
            "Rename last commit name",
            "Rename Last Commit",
            null,
            null,
            NonEmptyInputValidator(),
        ) ?: return
        ApplicationManager.getApplication()
            .executeOnPooledThread {
                try {
                    val gitVcs = GitVcs.getInstance(project)
                    val gitCheckinEnvironment =
                        gitVcs.checkinEnvironment as? GitCheckinEnvironment
                    if (gitCheckinEnvironment == null) {
                        displayErrorMessageDialog(
                            project,
                            "There is a problem with getting Git environment. Operation is not possible.",
                            "Git Environment Error"
                        )
                        return@executeOnPooledThread
                    }
                    if (!gitVcs.isCommitActionDisabled && gitCheckinEnvironment.isAmendCommitSupported()) {
                        val gitRepository = GitBranchUtil.guessWidgetRepository(project, event.dataContext)
                        if (gitRepository != null) {
                            if (gitRepository.isFresh) {
                                displayErrorMessageDialog(project, "No commit was found to rename", "No Commit Found")
                            }
                            if (hasStagedChanges(project, gitRepository)) {
                                displayErrorMessageDialog(
                                    project,
                                    "There are staged changes present. Please unstage changes before renaming the last commit.",
                                    "Staged Changes Detected"
                                )
                                return@executeOnPooledThread
                            }
                            val handler = getGitLineHandler(
                                project,
                                gitRepository,
                                GitCommand.COMMIT,
                                "--amend", "-m", newMessage
                            )
                            val command = Git.getInstance().runCommand(handler)
                            command.throwOnError()
                            gitRepository.update()
                        }
                    }
                } catch (e: Exception) {
                    displayErrorMessageDialog(
                        project,
                        e.message ?: "Error information not provided",
                        "Error"
                    )
                    throw RuntimeException(e)
                }
            }
    }

    private fun displayErrorMessageDialog(project: Project, message: String, title: String) {
        ApplicationManager.getApplication().invokeAndWait {
            Messages.showMessageDialog(
                project,
                message,
                title,
                Messages.getWarningIcon()
            )
        }
    }

    private fun hasStagedChanges(project: Project, repository: GitRepository): Boolean {
        val handler = getGitLineHandler(project, repository, GitCommand.DIFF, "--cached", "HEAD")
        val result = Git.getInstance().runCommand(handler)
        return !result.success()
    }

    private fun getGitLineHandler(
        project: Project,
        repository: GitRepository,
        gitCommand: GitCommand,
        vararg parameters: String
    ): GitLineHandler {
        val handler =
            GitLineHandler(project, repository.root, gitCommand)
        handler.setStdoutSuppressed(true)
        handler.addParameters(*parameters)
        handler.addParameters("--quiet")
        return handler
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project == null) {
            event.presentation.isEnabledAndVisible = false
            return
        }

        val gitRepository = GitBranchUtil.guessWidgetRepository(project, event.dataContext)
        if (gitRepository == null) {
            event.presentation.isEnabledAndVisible = false
            return
        }
        event.presentation.isVisible = true
        event.presentation.isEnabled = !gitRepository.isFresh
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}