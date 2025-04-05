package com.plugin.internship.jetbrainsrecruitmentplugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import git4idea.GitVcs;
import org.jetbrains.annotations.NotNull;

public class RenameCurrentCommitAction extends AnAction {


    @Override
    public void actionPerformed (AnActionEvent event) {
        // Using the event, create and show a dialog
        Project currentProject = event.getProject();
        StringBuilder message = new StringBuilder(event.getPresentation()
                .getText() + " Selected!");
        // If an element is selected in the editor, add info about it.
        Navigatable selectedElement = event.getData(CommonDataKeys.NAVIGATABLE);
        if (selectedElement != null) {
            message.append("\nSelected Element: ")
                    .append(selectedElement);
        }
        String title = event.getPresentation()
                .getDescription();
        Messages.showMessageDialog(currentProject, message.toString(), title, Messages.getInformationIcon());
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
