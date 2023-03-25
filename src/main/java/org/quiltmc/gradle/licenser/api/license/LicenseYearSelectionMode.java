/*
 * Copyright 2022-2023 QuiltMC
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

package org.quiltmc.gradle.licenser.api.license;

import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.gradle.licenser.api.util.GitUtils;

import java.nio.file.Path;

/**
 * Represents the mode in which the year should be fetched.
 *
 * @author LambdAurora
 * @version 2.0.0
 * @since 1.0.0
 */
public enum LicenseYearSelectionMode {
	/**
	 * The license year is project-wide, a change in any file of the project will update every file.
	 */
	PROJECT((project, path) -> project.getProjectDir().toPath()),
	/**
	 * Each file has its own year.
	 */
	FILE((project, path) -> path);

	private final CommitPathReference commitPathReference;

	LicenseYearSelectionMode(CommitPathReference commitPathReference) {
		this.commitPathReference = commitPathReference;
	}

	/**
	 * Gets the last modification year in which the file got modified.
	 * <p>
	 * In the case of {@link #PROJECT} the last modification year isn't file dependent.
	 *
	 * @param project the project the file is in
	 * @param path the path to the file
	 * @return the last modification year
	 */
	public int getModificationYear(Project project, Path path) {
		Path commitPath = this.commitPathReference.getPathForCommitFetching(project, path);
		return GitUtils.getModificationYear(project, commitPath);
	}

	@FunctionalInterface
	interface CommitPathReference {
		/**
		 * Gets the path to use to fetch the latest commit.
		 *
		 * @param project the project the path is in
		 * @param path the path
		 * @return the path to use to get the latest commit
		 */
		@Nullable Path getPathForCommitFetching(Project project, Path path);
	}
}
