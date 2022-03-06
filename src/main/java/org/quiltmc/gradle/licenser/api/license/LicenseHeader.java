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

package org.quiltmc.gradle.licenser.api.license;

import org.gradle.api.Project;
import org.quiltmc.gradle.licenser.impl.LicenseUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a license header.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LicenseHeader {
	public static final String YEAR_KEY = "YEAR";
	public static final String MATCH_FROM_KEY = "match_from: ";
	public static final String METADATA_MARKER = ";;";
	public static final String COMMENT_MARKER = METADATA_MARKER + "#";
	private final List<LicenseRule> rules;

	public LicenseHeader(LicenseRule... rules) {
		this.rules = new ArrayList<>(List.of(rules));
	}

	public LicenseHeader(List<LicenseRule> rules) {
		this.rules = rules;
	}

	/**
	 * {@return {@code true} if this license header is valid and can be used for validation, otherwise {@code false}}
	 */
	public boolean isValid() {
		return !this.rules.isEmpty();
	}

	public void addRule(LicenseRule rule) {
		this.rules.add(rule);
	}

	/**
	 * Validates the given file.
	 *
	 * @param path the path to the file to validate
	 * @return {@code true} if the file respects the license header format, otherwise {@code false}
	 */
	public boolean validate(Path path) {
		String source = LicenseUtils.readFile(path);

		for (var rule : this.rules) {
			if (rule.match(source)) {
				return rule.validate(source);
			}
		}

		return false;
	}

	/**
	 * Formats the given file to contain the correct license header.
	 *
	 * @param project the project the file is in
	 * @param rootPath the root path of the project
	 * @param path the path of the file
	 * @return {@code true} if files changed, otherwise {@code false}
	 */
	public boolean format(Project project, Path rootPath, Path path) {
		String source = LicenseUtils.readFile(path);

		for (var rule : this.rules) {
			if (rule.match(source)) {
				return rule.formatFile(project, rootPath, path);
			}
		}

		return false;
	}
}
