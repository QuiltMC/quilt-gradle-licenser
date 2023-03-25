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

import org.gradle.internal.impldep.org.joda.time.DateTimeFieldType;
import org.gradle.internal.impldep.org.joda.time.Instant;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		copy("HEADER");
		copy("src/custom/java/test/TestClass2.java");
		Path testClassPath = copy("src/main/java/test/TestClass.java");
		Path testPackageInfoPath = copy("src/main/java/test/package-info.java");
		Path testKotlinPath = copy("src/main/kotlin/test/TestKotlinFile.kt");

		// Run the build
		var runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("applyLicenses", "--stacktrace");
		runner.withProjectDir(projectDir);
		BuildResult result = runner.build();

		// Verify the result
		assertTrue(result.getOutput().contains("- Updated file " + testClassPath), "Missing updated file string in output log.");
		assertTrue(result.getOutput().contains("- Updated file " + testPackageInfoPath), "Missing updated package-info file string in output log.");
		assertTrue(result.getOutput().contains("- Updated file " + testKotlinPath), "Missing updated Kotlin file string in output log.");
		assertTrue(result.getOutput().contains("Updated 3 out of 3 files."), "Missing update status string in output log.");
		assertTrue(result.getOutput().contains("> Task :applyLicenseCustom SKIPPED"), "custom source set should be skipped.");

		assertTrue(Files.readString(testClassPath).contains("Licensed under the Apache License, Version 2.0 (the \"License\");"));
		assertTrue(Files.readString(testPackageInfoPath).contains("Licensed under the Apache License, Version 2.0 (the \"License\");"));

		assertEquals(Files.readString(testKotlinPath), """
				/*
				 * Copyright ${year} QuiltMC
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
				    
				@file:JvmName("TestKotlinFile")
				    
				package src.main.kotlin.test
				    
				fun main(args: Array<String>) {
				    println("Hello, world!")
				}
				""".replace("${year}", String.valueOf(Instant.now().get(DateTimeFieldType.year()))));

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
