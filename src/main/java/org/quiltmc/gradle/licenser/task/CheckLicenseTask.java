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

import org.gradle.api.GradleException;
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

public class CheckLicenseTask extends JavaSourceBasedTask {
	private final LicenseHeader licenseHeader;

	@Inject
	public CheckLicenseTask(SourceSet sourceSet, QuiltLicenserGradleExtension extension) {
		super(sourceSet, extension.asPatternFilterable());
		this.licenseHeader = extension.getLicenseHeader();
		this.setDescription("Checks whether source files in the " + sourceSet.getName() + " source set contain a valid license header.");
		this.setGroup("verification");

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
		private final List<Path> failedChecks = new ArrayList<>();
		private int total = 0;

		public Consumer(LicenseHeader licenseHeader) {
			this.licenseHeader = licenseHeader;
		}

		@Override
		public void consume(Project project, Path sourceSetPath, Path path) {
			if (!this.licenseHeader.validate(path)) {
				this.failedChecks.add(path);
			}

			this.total++;
		}

		@Override
		public void end(Logger logger) {
			if (this.failedChecks.isEmpty()) {
				logger.lifecycle("All license header checks passed ({} files).", this.total);
			} else {
				for (var failedPath : this.failedChecks) {
					logger.error(" - {} - license checks have failed.", failedPath);
				}

				throw new GradleException(
						String.format("License header checks have failed on %s out of %d files.",
								this.failedChecks.size(), this.total
						)
				);
			}
		}
	}
}
