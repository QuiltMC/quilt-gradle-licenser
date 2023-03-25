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

package org.quiltmc.gradle.licenser.api.license.comment;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the license comment reader and writer for Java-like files.
 *
 * @version 1.2.0
 * @since 1.2.0
 */
public class JavaLicenseComment implements LicenseComment {
	public static final JavaLicenseComment JAVA = new JavaLicenseComment();

	@Override
	public @NotNull Result findLicenseComment(@NotNull String source) {
		int i;
		String existing = null;

		for (i = 0; i < source.length(); i++) {
			char c = source.charAt(i);

			if (c == '/' && existing == null) {
				if (i + 1 != source.length() && source.charAt(i + 1) == '*'
						&& i + 2 != source.length() && source.charAt(i + 2) != '*') {
					// License!
					int j = i + 2;
					while (j < source.length()) {
						j = source.indexOf('*', j + 1);

						if (j == -1) {
							return new Result(source.length() - 1, source.substring(i + 2));
						}

						if (j + 1 == source.length()) {
							return new Result(j, this.extractLicense(source.substring(i + 2)));
						}

						if (source.charAt(j + 1) == '/') {
							// The end!
							existing = this.extractLicense(source.substring(i + 2, j - 1));
							i = j + 1;
							break;
						}
					}
				} else break;
			} else if (!Character.isWhitespace(c)) break;
		}

		return new Result(i, null);
	}

	@Override
	public @NotNull String getLicenseComment(@NotNull String[] lines, @NotNull String lineSeparator) {
		var builder = new StringBuilder();

		builder.append("/*").append(lineSeparator);
		for (var line : lines) {
			if (line.isEmpty()) {
				builder.append(" *").append(lineSeparator);
			} else {
				builder.append(" * ").append(line).append(lineSeparator);
			}
		}

		builder.append(" */").append(lineSeparator).append(lineSeparator);

		return builder.toString();
	}

	private String extractLicense(String source) {
		return source.trim().replaceAll("\n\r? \\* ", "\n");
	}
}
