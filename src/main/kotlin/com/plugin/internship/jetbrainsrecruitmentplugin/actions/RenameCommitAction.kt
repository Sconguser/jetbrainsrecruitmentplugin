package com.plugin.internship.jetbrainsrecruitmentplugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import git4idea.GitUtil
import git4idea.GitVcs
import git4idea.checkin.GitCheckinEnvironment
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository


class RenameCommitAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        ApplicationManager.getApplication()
            .executeOnPooledThread {
                try {
                    val project = event.project ?: return@executeOnPooledThread
                    val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
                        ?: throw RuntimeException("File is null")
                    val gitVcs = GitVcs.getInstance(project)
                    val gitCheckinEnvironment =
                        gitVcs.checkinEnvironment as GitCheckinEnvironment? ?: return@executeOnPooledThread
                    if (!gitVcs.isCommitActionDisabled && gitCheckinEnvironment.isAmendCommitSupported()) {
                        val repositoryManager = GitUtil.getRepositoryManager(project)
                        val repository: GitRepository? = repositoryManager.getRepositoryForFileQuick(file);
                        if (repository != null) {
                            val handler =
                                GitLineHandler(project, repository.root, GitCommand.COMMIT)
                            handler.setStdoutSuppressed(false);
                            handler.addParameters("--amend", "-m", "Your amended commit message here");
                            val command = Git.getInstance().runCommand(handler)
                            command.throwOnError()
                        }
                    }
                } catch (e: Exception) {
                    println(e)
                }
            }
    }

    override fun update(event: AnActionEvent) {
        val project = event.project
        if (project == null) {
            event.presentation.isEnabledAndVisible = false
            return
        }
/// ToDO: file is null in some contexts (commit)
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