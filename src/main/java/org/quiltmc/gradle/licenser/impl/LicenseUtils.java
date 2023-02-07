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

package org.quiltmc.gradle.licenser.impl;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.gradle.licenser.api.license.HeaderFormat;
import org.quiltmc.gradle.licenser.api.license.LicenseHeader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ApiStatus.Internal
public class LicenseUtils {
	private LicenseUtils() {
		throw new UnsupportedOperationException("LicenseUtils only contains static definitions.");
	}

	private static String escapeRegexControl(String input) {
		return input.replace("(", "\\(")
				.replace(")", "\\)")
				.replace(".", "\\.")
				.replace("/", "\\/");
	}

	/**
	 * Returns the line separator for the given text.
	 *
	 * @param text the text
	 * @return the line separator
	 */
	public static String getLineSeparator(String text) {
		if (text.contains("\r\n")) {
			return "\r\n"; // CR-LF
		} else if (text.contains("\n")) {
			return "\n"; // LF
		} else {
			return System.lineSeparator(); // We don't know the line separator, rely on system's default.
		}
	}

	public static Pattern getValidator(HeaderFormat headerFormat) {
		String singleYearRegex = "\\d{4}(?: ?- ?\\d{4})?";

		String pattern = escapeRegexControl(headerFormat.getSource())
				.replace("${" + LicenseHeader.YEAR_KEY + "}", "((?:" + singleYearRegex
						+ ")(?:, " + singleYearRegex + ")*)");
		String[] lines = pattern.split(headerFormat.getLineSeparator());
		var patternBuilder = new StringBuilder("\\/\\*" + headerFormat.getLineSeparator());

		for (var line : lines) {
			if (line.isBlank()) {
				patternBuilder.append(" \\*").append(headerFormat.getLineSeparator());
			} else {
				patternBuilder.append(" \\* ").append(line).append(headerFormat.getLineSeparator());
			}
		}

		patternBuilder.append(" \\*\\/").append(headerFormat.getLineSeparator()).append(headerFormat.getLineSeparator());

		return Pattern.compile("^" + patternBuilder);
	}

	public static @Nullable Pattern getMatcher(HeaderFormat headerFormat) {
		var match = new StringBuilder();

		for (var line : headerFormat.getMetadataLines()) {
			if (line.startsWith(LicenseHeader.MATCH_FROM_KEY)) {
				if (!match.isEmpty()) {
					match.append('|');
				}
				match.append('(').append(line.substring(LicenseHeader.MATCH_FROM_KEY.length())).append(')');
			}
		}

		if (match.isEmpty()) {
			return null;
		}

		return Pattern.compile("^" + match);
	}

	public static <T extends Enum<T>> T getEnumValue(HeaderFormat format, String key, T defaultValue) {
		var match = key + ": ";

		for (var line : format.getMetadataLines()) {
			if (line.startsWith(match)) {
				String valueStr = line.substring(match.length()).strip();

				for (var constant : defaultValue.getDeclaringClass().getEnumConstants()) {
					if (constant.name().equalsIgnoreCase(valueStr)) {
						return constant;
					}
				}

				break;
			}
		}

		return defaultValue;
	}

	public static String[] getHeaderLines(String[] lines) {
		int max = lines.length;
		for (int i = lines.length - 1; i > 0; i--) {
			if (lines[i].startsWith(";;") || lines[i].isEmpty()) {
				max = i;
			} else {
				break;
			}
		}

		var headerLines = new String[max];
		System.arraycopy(lines, 0, headerLines, 0, max);
		return headerLines;
	}

	public static String readFile(Path path) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new GradleException(String.format("Failed to load file %s", path), e);
		}
	}

	public static List<String> getMetadata(String[] headerFormat) {
		var list = new ArrayList<String>();

		for (var line : headerFormat) {
			if (line.startsWith(LicenseHeader.METADATA_MARKER)) {
				if (line.startsWith(LicenseHeader.COMMENT_MARKER))
					continue;

				list.add(line.substring(LicenseHeader.METADATA_MARKER.length()));
			}
		}

		return list;
	}

	public static @Nullable Path getBackupPath(Project project, Path rootPath, Path path) {
		Path backupDir = project.getBuildDir().toPath().resolve("quilt/licenser");

		try {
			Files.createDirectories(backupDir);
		} catch (IOException e) {
			return null;
		}

		var pathAsString = path.toAbsolutePath().toString();
		var rootPathAsString = rootPath.toString();

		if (pathAsString.startsWith(rootPathAsString)) {
			return backupDir.resolve(Paths.get(pathAsString.substring(rootPathAsString.length() + 1)))
					.normalize();
		}

		return null;
	}
}
