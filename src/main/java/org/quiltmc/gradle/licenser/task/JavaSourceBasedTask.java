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

package org.quiltmc.gradle.licenser.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.ApiStatus;

import java.nio.file.Path;

@ApiStatus.Internal
public abstract class JavaSourceBasedTask extends DefaultTask {
	protected final SourceSet sourceSet;
	protected final PatternFilterable patternFilterable;

	protected JavaSourceBasedTask(SourceSet sourceSet, PatternFilterable patternFilterable) {
		this.sourceSet = sourceSet;
		this.patternFilterable = patternFilterable;
	}

	protected void execute(JavaSourceConsumer consumer) {
		for (var javaDir : this.sourceSet.getAllSource().matching(this.patternFilterable)) {
			Path sourcePath = javaDir.toPath();
			consumer.consume(this.getProject(), this.getLogger(), this.getProject().getProjectDir().toPath(), sourcePath);
		}

		consumer.end(this.getLogger());
	}

	public interface JavaSourceConsumer {
		void consume(Project project, Logger logger, Path sourceSetPath, Path path);

		void end(Logger logger);
	}
}
