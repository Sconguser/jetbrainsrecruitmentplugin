package com.plugin.internship.jetbrainsrecruitmentplugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.NonEmptyInputValidator
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import git4idea.GitUtil
import git4idea.GitVcs
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
        )
        ApplicationManager.getApplication()
            .executeOnPooledThread {
                try {
                    val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                        ?: throw RuntimeException("File is null")
                    val gitVcs = GitVcs.getInstance(project)
                    val gitCheckinEnvironment =
                        gitVcs.checkinEnvironment as GitCheckinEnvironment? ?: return@executeOnPooledThread
                    if (!gitVcs.isCommitActionDisabled && gitCheckinEnvironment.isAmendCommitSupported()) {
                        val repository: GitRepository? =
                            GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(file)
                        if (repository != null) {
                            val handler =
                                gitLineHandler(project, repository, newMessage)
                            val command = Git.getInstance().runCommand(handler)
                            command.throwOnError()
                            repository.update()
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
/// ToDO: file is null in some contexts (anything but file structure open as a sidebar)
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        ProjectLevelVcsManager.getInstance(project)
            .allActiveVcss
        val vcs = ProjectLevelVcsManager.getInstance(project)
            .getVcsFor(file)
        val isGit = (vcs is GitVcs)
        event.presentation.isEnabledAndVisible = isGit
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
}