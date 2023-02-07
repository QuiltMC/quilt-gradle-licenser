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

import org.quiltmc.gradle.licenser.impl.LicenseUtils;

import java.util.List;
import java.util.Map;

/**
 * Represents the format of a license header.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class HeaderFormat {
	private final String source;
	private final String lineSeparator;
	private final List<String> metadataLines;

	public HeaderFormat(String source) {
		this.lineSeparator = LicenseUtils.getLineSeparator(source);

		String[] lines = source.split(this.lineSeparator);

		this.source = String.join(this.lineSeparator, LicenseUtils.getHeaderLines(lines));
		this.metadataLines = List.copyOf(LicenseUtils.getMetadata(lines));
	}

	/**
	 * {@return the unformatted text of the license header}
	 */
	public String getSource() {
		return this.source;
	}

	/**
	 * {@return the line separator used by the license header}
	 */
	public String getLineSeparator() {
		return this.lineSeparator;
	}

	/**
	 * {@return the unformatted text of the license header}
	 */
	public String[] getHeaderLines() {
		return this.source.split(this.getLineSeparator());
	}

	/**
	 * {@return the formatted text of the license header}
	 *
	 * @param variables the variables present in the text
	 */
	public String[] getHeaderLines(Map<String, String> variables) {
		String source = this.source;

		for (var entry : variables.entrySet()) {
			source = source.replace("${" + entry.getKey() + "}", entry.getValue());
		}

		return source.split(this.getLineSeparator());
	}

	/**
	 * {@return the metadata lines of this license header}
	 */
	public List<String> getMetadataLines() {
		return this.metadataLines;
	}

	/**
	 * Gets an enum value by key in the metadata of the license header.
	 *
	 * @param key the key of the enum metadata
	 * @param defaultValue the default value of the enum value
	 * @param <T> the type of the enum
	 * @return the enum value
	 */
	public <T extends Enum<T>> T getEnumValue(String key, T defaultValue) {
		return LicenseUtils.getEnumValue(this, key, defaultValue);
	}
}
