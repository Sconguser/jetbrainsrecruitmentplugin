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
        ///TODO: add localization
        val newMessage = Messages.showInputDialog(
            project,
            "Rename last commit name",
            "Rename Last Commit",
            null,
            null,
            NonEmptyInputValidator(),
        )
        ApplicationManager.getApplication()
            .executeOnPooledThread {
                try {
                    val gitVcs = GitVcs.getInstance(project)
                    val gitCheckinEnvironment =
                        gitVcs.checkinEnvironment as GitCheckinEnvironment? ?: return@executeOnPooledThread
                    if (!gitVcs.isCommitActionDisabled && gitCheckinEnvironment.isAmendCommitSupported()) {
                        val gitRepository = GitBranchUtil.guessWidgetRepository(project, event.dataContext)
                        if (gitRepository != null) {
                            val handler =
                                gitLineHandler(project, gitRepository, newMessage)
                            /// TODO: what if something is staged
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

    private fun gitLineHandler(
        project: Project,
        repository: GitRepository,
        newMessage: String?
    ): GitLineHandler {
        val handler =
            GitLineHandler(project, repository.root, GitCommand.COMMIT)
        handler.setStdoutSuppressed(false)
        handler.addParameters("--amend", "-m", newMessage)
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

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}