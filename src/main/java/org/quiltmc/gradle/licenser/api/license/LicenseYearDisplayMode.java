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

import org.eclipse.jgit.util.IntList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents how a year value should be represented in the license file.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public enum LicenseYearDisplayMode {
	LENIENT_RANGE {
		@Override
		protected String getYearString(IntList years) {
			int min = years.get(0);

			if (years.size() == 1) {
				return String.valueOf(min);
			}

			int max = years.get(years.size() - 1);
			return min + "-" + max;
		}
	},
	LIST {
		@Override
		protected String getYearString(IntList years) {
			var streamBuilder = IntStream.builder();
			for (int i = 0; i < years.size(); i++) streamBuilder.add(years.get(i));

			return streamBuilder.build().sorted()
					.mapToObj(String::valueOf)
					.collect(Collectors.joining(", "));
		}
	},
	LATEST_ONLY {
		@Override
		protected String getYearString(IntList years) {
			return String.valueOf(years.get(years.size() - 1));
		}
	};

	public String getYearString(@Nullable String yearValue, int lastModifiedYear) {
		if (yearValue != null) {
			String[] serializedYears = yearValue.split(" ?, ?");

			var years = new IntList();

			for (var serializedYear : serializedYears) {
				try {
					if (serializedYear.contains("-")) {
						String[] serializedYearRange = serializedYear.split(" ?- ?");

						if (serializedYearRange.length > 2) {
							continue; // Invalid year range.
						}

						int start = Integer.parseInt(serializedYearRange[0]);
						int end = Integer.parseInt(serializedYearRange[1]);

						if (end < start) continue; // Invalid year range.

						for (int i = start; i <= end; i++) {
							years.add(i);
						}

						continue;
					}

					int year = Integer.parseInt(serializedYear);
					years.add(year);
				} catch (NumberFormatException ignored) {
					// ignore
				}
			}

			if (!years.contains(lastModifiedYear)) {
				years.add(lastModifiedYear);
			}

			return this.getYearString(years);
		}

		return String.valueOf(lastModifiedYear);
	}

	protected abstract String getYearString(IntList years);
}
