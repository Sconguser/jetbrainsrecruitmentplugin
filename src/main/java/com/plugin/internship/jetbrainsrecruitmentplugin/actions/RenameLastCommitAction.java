package com.plugin.internship.jetbrainsrecruitmentplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.GitVcs;
import git4idea.checkin.GitCheckinEnvironment;
import org.jetbrains.annotations.NotNull;


public class RenameLastCommitAction extends AnAction {


    @Override
    public void actionPerformed (@NotNull AnActionEvent event) {
        ApplicationManager.getApplication()
                .executeOnPooledThread(() -> {
                    try {
                        Project project = event.getProject();
                        if (project == null) {
                            return;
                        }
                        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
                        if (file == null) {
                            throw new RuntimeException("File is null");
                        }
                        VirtualFile root = GitUtil.getRootForFile(project, file);
                        ProjectLevelVcsManager.getInstance(project)
                                .getAllActiveVcss();
                        GitVcs gitVcs = GitVcs.getInstance(project);
                        GitCheckinEnvironment gitCheckinEnvironment = (GitCheckinEnvironment) gitVcs.getCheckinEnvironment();
                        if (gitCheckinEnvironment == null) {
                            return;
                        }
                        String lastCommitMessage = gitCheckinEnvironment.getLastCommitMessage(root);
                        if (lastCommitMessage == null) {
                            return;
                        }
                        if (gitCheckinEnvironment.isAmendCommitSupported()) {
//                            List<VcsException> exceptions = gitCheckinEnvironment.commit(new ArrayList<>(),
//                                    "newCommitMessage", commitContext, new HashSet<>());
//                            exceptions.forEach(e -> System.out.println(e.getMessage()));
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                });
    }

    @Override
    public void update (@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            event.getPresentation()
                    .setEnabledAndVisible(false);
            return;
        }

        ///   public static @NotNull GitVcs getInstance(@NotNull Project project) {
        ///     GitVcs gitVcs = (GitVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(NAME);
        ///     return Objects.requireNonNull(gitVcs);
        ///   }

        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        ProjectLevelVcsManager.getInstance(project)
                .getAllActiveVcss();
        AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project)
                .getVcsFor(file);
        boolean isGit = (vcs instanceof GitVcs);
        event.getPresentation()
                .setEnabledAndVisible(isGit);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread () {
        return ActionUpdateThread.BGT;
    }
}
