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

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.quiltmc.gradle.licenser.api.license.LicenseHeader;
import org.quiltmc.gradle.licenser.extension.QuiltLicenserGradleExtension;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ApplyLicenseTask extends JavaSourceBasedTask {
	private final LicenseHeader licenseHeader;

	@Inject
	public ApplyLicenseTask(SourceSet sourceSet, QuiltLicenserGradleExtension extension) {
		super(sourceSet, extension.asPatternFilterable());
		this.licenseHeader = extension.getLicenseHeader();
		this.setDescription("Applies the correct license headers to source files in the " + sourceSet.getName() + " source set.");
		this.setGroup("generation");

		if (!this.licenseHeader.isValid()) {
			this.setEnabled(false);
		}
	}

	@TaskAction
	public void execute() {
		this.execute(new Consumer(this.licenseHeader));
	}

	public static class Consumer implements JavaSourceConsumer {
		private final LicenseHeader licenseHeader;
		private final List<Path> updatedFiles = new ArrayList<>();
		private int total = 0;

		public Consumer(LicenseHeader licenseHeader) {
			this.licenseHeader = licenseHeader;
		}

		@Override
		public void consume(Project project, Path rootPath, Path path) {
			if (this.licenseHeader.format(project, rootPath, path)) {
				this.updatedFiles.add(path);
			}

			this.total++;
		}

		@Override
		public void end(Logger logger) {
			for (var path : this.updatedFiles) {
				logger.lifecycle(" - Updated file {}", path);
			}

			logger.lifecycle("Updated {} out of {} files.", this.updatedFiles.size(), this.total);
		}
	}
}
