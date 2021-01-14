package top.macondo.idea.plugins.cleanclean;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 1. 匹配规则存储类
 * 2. 目录遍历
 * 3. 增强，删除后提示。支持定向撤销；；其他，找出对应文件后，确认后再删除
 *
 * @author zhangchong
 **/
public class CleanFileAction extends AnAction {
	public static String pattern_new;
	private static final String PATTERN_INIT = "top.macondo.idea.plugins.cleanclean.pattern_init";
	private static NotificationGroup notificationGroup;

	static {
		notificationGroup = new NotificationGroup("cleanclean.NotificationGroup", NotificationDisplayType.BALLOON, true);
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
		Project project = anActionEvent.getProject();

		String pattern_init = PropertiesComponent.getInstance().getValue(PATTERN_INIT);
		pattern_new = Messages.showInputDialog(project, "Extra Files Regex(eg: rebel.xml|target)", "Clean Extra Files",
				Messages.getQuestionIcon(), pattern_init, null);
		PropertiesComponent.getInstance().setValue(PATTERN_INIT, pattern_new);

		//获取编辑的文件,可以拿到绝对路径和名称
		VirtualFile currentFile = PlatformDataKeys.VIRTUAL_FILE.getData(anActionEvent.getDataContext());

		List<VirtualFile> deleteFile = new ArrayList<>();
		getDeleteFiles(currentFile.getChildren(), deleteFile);
		ApplicationManager.getApplication().runWriteAction(() -> {
			try {
				for (VirtualFile file : deleteFile) {
					file.delete(null);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		Notification notification = notificationGroup.createNotification("Clean Complete.<br> The Extra Files has bean deleted.", NotificationType.INFORMATION);
		Notifications.Bus.notify(notification, project);
	}

	public static void getDeleteFiles(VirtualFile[] currentFile, List<VirtualFile> deleteFile) {

		Arrays.stream(currentFile).forEach(c -> {
			if (Pattern.matches(pattern_new, c.getName())) {
				deleteFile.add(c);
			} else if (c.isDirectory()) {
				getDeleteFiles(c.getChildren(), deleteFile);
			}
		});

	}

	@Override
	public void update(@NotNull AnActionEvent e) {
		final Project project = e.getProject();
		final VirtualFile virtualFile = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
		e.getPresentation().setEnabledAndVisible(project != null && virtualFile != null);
	}
}
