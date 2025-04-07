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
                    ///TODO: this potentially returns after user has already put new commit name
                    val gitCheckinEnvironment =
                        gitVcs.checkinEnvironment as GitCheckinEnvironment? ?: return@executeOnPooledThread
                    if (!gitVcs.isCommitActionDisabled && gitCheckinEnvironment.isAmendCommitSupported()) {
                        val gitRepository = GitBranchUtil.guessWidgetRepository(project, event.dataContext)
                        if (gitRepository != null) {
                            if (hasStagedChanges(project, gitRepository)) {
                                ApplicationManager.getApplication().invokeAndWait {
                                    Messages.showMessageDialog(
                                        project,
                                        "There are staged changes present. Please unstage changes before renaming the last commit.",
                                        "Staged Changes Detected",
                                        Messages.getWarningIcon()
                                    )
                                }
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
                    throw RuntimeException(e)
                }
            }
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
        event.presentation.isEnabledAndVisible = gitRepository != null
    }

    private fun hasStagedChanges(project: Project, repository: GitRepository): Boolean {
        val handler = getGitLineHandler(project, repository, GitCommand.DIFF, "--cached", "HEAD")
        val result = Git.getInstance().runCommand(handler)
        return !result.success()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}