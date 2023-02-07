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

package org.quiltmc.gradle.licenser;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A simple unit test for the 'org.quiltmc.gradle.licenser' plugin.
 */
class QuiltGradleLicenserPluginTest {
	@Test
	void pluginRegistersATask() {
		// Create a test project and apply the plugin
		Project project = ProjectBuilder.builder().build();
		project.getPlugins().apply("org.quiltmc.gradle.licenser");

		// Verify the result
		assertNotNull(project.getTasks().findByName("checkLicenses"));
		assertNotNull(project.getTasks().findByName("applyLicenses"));
	}
}
