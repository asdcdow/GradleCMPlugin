package org.kercheval.gradle.buildrelease;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.kercheval.gradle.info.GradleInfoSource;

public class BuildReleaseTask
	extends DefaultTask
{
	public BuildReleaseTask()
	{
		final Project project = getProject();
		final GradleInfoSource gradleUtil = new GradleInfoSource(project);
		final BuildReleaseInitTask initTask = (BuildReleaseInitTask) gradleUtil
			.getTask(BuildReleasePlugin.INIT_TASK_NAME);
		dependsOn(":" + BuildReleasePlugin.MERGE_TASK_NAME);
		dependsOn(":" + initTask.getUploadtask());
	}

	@TaskAction
	public void doTask()
	{}
}
