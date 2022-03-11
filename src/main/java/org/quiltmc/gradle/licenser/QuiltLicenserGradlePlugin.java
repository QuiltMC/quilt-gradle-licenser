/*
 * Copyright 2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quiltmc.gradle.licenser;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import org.quiltmc.gradle.licenser.extension.QuiltLicenserGradleExtension;
import org.quiltmc.gradle.licenser.task.ApplyLicenseTask;
import org.quiltmc.gradle.licenser.task.CheckLicenseTask;

/**
 * Represents the Quilt Licenser Gradle plugin.
 */
public class QuiltLicenserGradlePlugin implements Plugin<Project> {
	public static final String LICENSE_TASK_SUFFIX = "License";
	public static final String CHECK_TASK_PREFIX = "check";
	public static final String APPLY_TASK_PREFIX = "apply";

	@Override
	public void apply(Project project) {
		var ext = project.getExtensions().create("license", QuiltLicenserGradleExtension.class, project);

		// Register tasks
		project.getPlugins().withType(JavaBasePlugin.class).configureEach(plugin -> {
			var sourceSets = (SourceSetContainer) project.getExtensions().getByName("sourceSets");

			sourceSets.all(sourceSet -> {
				project.getTasks().register(this.getTaskName("check", sourceSet), CheckLicenseTask.class, sourceSet, ext).getName();
				project.getTasks().register(this.getTaskName("apply", sourceSet), ApplyLicenseTask.class, sourceSet, ext);
			});
		});

		var globalCheck = this.registerGroupedTask(project, CHECK_TASK_PREFIX, task -> {
			task.dependsOn(project.getTasks().withType(CheckLicenseTask.class));

			task.setDescription("Checks whether source files in every source sets contain a valid license header.");
			task.setGroup("verification");
		});
		this.registerGroupedTask(project, APPLY_TASK_PREFIX, task -> {
			task.dependsOn(project.getTasks().withType(ApplyLicenseTask.class));

			task.setDescription("Applies the correct license headers to source files in every source sets.");
			task.setGroup("generation");
		});

		project.getPlugins().withType(LifecycleBasePlugin.class).configureEach(plugin -> {
			project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task -> {
				task.dependsOn(globalCheck);
			});
		});
	}

	private String getTaskName(String action, SourceSet sourceSet) {
		if (sourceSet.getName().equals("main")) {
			return action + LICENSE_TASK_SUFFIX + "Main";
		}
		return sourceSet.getTaskName(action + LICENSE_TASK_SUFFIX, null);
	}

	private TaskProvider<Task> registerGroupedTask(Project project, String action, Action<Task> consumer) {
		var task = project.getTasks().register(action + LICENSE_TASK_SUFFIX + 's');
		task.configure(consumer);
		return task;
	}
}
