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

package org.quiltmc.gradle.licenser;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A simple functional test for the 'org.quiltmc.gradle.licenser' plugin.
 */
class QuiltGradleLicenserPluginFunctionalTest {
	@TempDir
	File projectDir;

	private Path path(String path) {
		return this.projectDir.toPath().resolve(path).normalize();
	}

	private Path copy(String pathStr) throws IOException {
		Path destinationPath = this.path(pathStr);
		Files.createDirectories(destinationPath.getParent());
		Files.copy(
				Objects.requireNonNull(QuiltGradleLicenserPluginFunctionalTest.class.getResourceAsStream("/" + pathStr)),
				destinationPath
		);
		return destinationPath.toAbsolutePath();
	}

	private File getSettingsFile() {
		return new File(this.projectDir, "settings.gradle");
	}

	@Test
	void canRunTask() throws IOException {
		this.writeString(this.getSettingsFile(), "");
		copy("build.gradle");
		Path testClassPath = copy("src/main/java/test/TestClass.java");
		Path testKotlinFilePath = copy("src/main/java/test/TestKotlinFile.kt");

		Files.copy(Paths.get("codeformat", "HEADER"), this.path("HEADER"));

		// Run the build
		var runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("applyLicenses", "--stacktrace");
		runner.withProjectDir(projectDir);
		BuildResult result = runner.build();

		// Verify the result
		assertTrue(result.getOutput().contains("- Updated file " + testClassPath), "Missing updated file string in output log.");
		assertTrue(Files.readString(testClassPath).contains("Licensed under the Apache License, Version 2.0 (the \"License\");"));

		assertTrue(result.getOutput().contains("- Updated file " + testKotlinFilePath), "Missing updated file string in output log.");
		assertTrue(Files.readString(testKotlinFilePath).contains("Licensed under the Apache License, Version 2.0 (the \"License\");"));
		assertTrue(Files.readString(testKotlinFilePath).contains("@file:JvmName(\"TestKotlinFile\")"));

		assertTrue(result.getOutput().contains("Updated 2 out of 2 files."), "Missing update status string in output log.");


		runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("checkLicenses", "--stacktrace");
		runner.withProjectDir(projectDir);
		runner.build();
	}

	private void writeString(File file, String string) throws IOException {
		try (var writer = new FileWriter(file)) {
			writer.write(string);
		}
	}
}
