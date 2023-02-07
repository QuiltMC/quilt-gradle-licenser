/*
 * Copyright 2023 QuiltMC
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

package org.quiltmc.gradle.licenser.api.license.comment;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

/**
 * Represents the license header comment reader and writer for a language.
 *
 * @version 1.2.0
 * @since 1.2.0
 */
public interface LicenseComment {
	/**
	 * Attempts to find the license comment.
	 *
	 * @param source the source
	 * @return the found comment
	 */
	@NotNull Result findLicenseComment(@NotNull String source);

	/**
	 * Gets the license comment as a string from the given license lines and the line separator used.
	 *
	 * @param lines the lines of the license text
	 * @param lineSeparator the line separator
	 * @return the license comment
	 */
	@NotNull String getLicenseComment(@NotNull String[] lines, @NotNull String lineSeparator);

	record Result(int endIndex, @Nullable String existing) {}
}
