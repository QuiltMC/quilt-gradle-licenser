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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.gradle.licenser.QuiltLicenserGradlePlugin;
import org.quiltmc.gradle.licenser.impl.LicenseUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Represents a license header rule, it describes one valid license header format.
 *
 * @author LambdAurora
 * @version 1.1.1
 * @since 1.0.0
 */
public class LicenseRule {
	private final HeaderFormat headerFormat;
	private final Pattern validator;
	private final @Nullable Pattern matcher;
	private final LicenseYearDisplayMode yearDisplayMode;
	private final LicenseYearSelectionMode yearSelectionMode;

	public LicenseRule(String headerFormat) {
		this(headerFormat, LicenseYearDisplayMode.LATEST_ONLY, LicenseYearSelectionMode.PROJECT);
	}

	public LicenseRule(String headerFormat, LicenseYearDisplayMode yearDisplayMode, LicenseYearSelectionMode yearSelectionMode) {
		this.headerFormat = new HeaderFormat(headerFormat);
		this.validator = LicenseUtils.getValidator(this.headerFormat);
		this.matcher = LicenseUtils.getMatcher(this.headerFormat);

		this.yearDisplayMode = this.headerFormat.getEnumValue("year_display", yearDisplayMode);
		this.yearSelectionMode = this.headerFormat.getEnumValue("year_selection", yearSelectionMode);
	}

	private static String loadFile(Path path) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new GradleException(String.format("Failed to load license header %s", path), e);
		}
	}

	public static LicenseRule fromFile(Path path) {
		return new LicenseRule(loadFile(path));
	}

	public static LicenseRule fromFile(Path path, LicenseYearDisplayMode yearDisplayMode, LicenseYearSelectionMode yearSelectionMode) {
		return new LicenseRule(loadFile(path), yearDisplayMode, yearSelectionMode);
	}

	/**
	 * Returns whether this rule has a special file matching.
	 *
	 * @return {@code true} if this rule has a special file matching, otherwise {@code false}
	 */
	public final boolean hasSpecialMatching() {
		return this.matcher != null;
	}

	/**
	 * Returns whether the file's licensing should respect this rule.
	 *
	 * @param source the source of the file
	 * @return {@code true} if the file's licensing should respect this rule, otherwise {@code false}
	 */
	public boolean match(String source) {
		if (!this.hasSpecialMatching()) {
			return true;
		}

		if (this.matcher.matcher(source).find()) {
			return true;
		} else {
			return this.validator.matcher(source).find();
		}
	}

	public boolean validate(String source) {
		return this.validator.matcher(source).find();
	}

	public boolean formatFile(Project project, Path rootPath, Path path) {
		String source = LicenseUtils.readFile(path);
		String year = this.getYearString(project, path, source);

		if (QuiltLicenserGradlePlugin.DEBUG_MODE) {
			project.getLogger().lifecycle("  => Selected \"{}\" as the year string.", year);
		}

		int delimiter = source.indexOf("package");
		if (path.toString().endsWith(".kt")) {
			delimiter = source.substring(0, delimiter).contains("@file") ? source.indexOf("@file") : delimiter;
		}
		if (delimiter != -1) {
			// @TODO have a way to specify custom variables?
			var map = new HashMap<String, String>();
			map.put(LicenseHeader.YEAR_KEY, year);
			var newSource = this.getLicenseString(map) + source.substring(delimiter);

			if (newSource.equals(source)) {
				return false;
			}

			var backupPath = LicenseUtils.getBackupPath(project, rootPath, path);

			if (backupPath == null) {
				throw new GradleException("Cannot backup file " + path + ", abandoning formatting.");
			}

			try {
				if (!Files.isDirectory(backupPath.getParent())) {
					Files.createDirectories(backupPath.getParent());
				}

				Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new GradleException("Cannot backup file " + path + ", abandoning formatting.", e);
			}

			try {
				Files.writeString(path, newSource);
			} catch (IOException e) {
				throw new GradleException("Failed to write updated file " + path + ", abandoning formatting.", e);
			}

			return true;
		}

		return false;
	}

	private String getLicenseString(Map<String, String> variables) {
		var builder = new StringBuilder();

		var lines = this.headerFormat.getHeaderLines(variables);

		builder.append("/*").append(this.headerFormat.getLineSeparator());
		for (var line : lines) {
			if (line.isEmpty()) {
				builder.append(" *").append(this.headerFormat.getLineSeparator());
			} else {
				builder.append(" * ").append(line).append(this.headerFormat.getLineSeparator());
			}
		}

		builder.append(" */").append(this.headerFormat.getLineSeparator()).append(this.headerFormat.getLineSeparator());

		return builder.toString();
	}

	private String getYearString(Project project, Path sourcePath, String source) {
		int lastModifiedYear = this.yearSelectionMode.getYear(project, sourcePath);
		var matcher = this.validator.matcher(source);

		if (QuiltLicenserGradlePlugin.DEBUG_MODE) {
			project.getLogger().lifecycle("  => Found last modification year {}", lastModifiedYear);
		}

		String yearValue = null;

		if (matcher.find()) {
			if (matcher.groupCount() >= 1) {
				yearValue = matcher.group(1);

				if (QuiltLicenserGradlePlugin.DEBUG_MODE) {
					project.getLogger().lifecycle("  => Found current year value in file: \"{}\"", yearValue);
				}
			}
		} else if (QuiltLicenserGradlePlugin.DEBUG_MODE) {
			project.getLogger().lifecycle("  => Could not find current year value in file.");
		}

		return this.yearDisplayMode.getYearString(yearValue, lastModifiedYear);
	}

	@Override
	public String toString() {
		return "LicenseRule{" +
				"headerFormat=" + this.headerFormat +
				", validator=" + this.validator +
				", matcher=" + this.matcher +
				", yearDisplayMode=" + this.yearDisplayMode +
				", yearSelectionMode=" + this.yearSelectionMode +
				'}';
	}
}
