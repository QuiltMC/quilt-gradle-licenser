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

package org.quiltmc.gradle.licenser.extension;

import groovy.lang.Closure;
import groovy.lang.Delegate;
import groovy.transform.PackageScope;
import org.gradle.api.Project;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.resources.TextResourceFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.gradle.licenser.api.license.LicenseHeader;
import org.quiltmc.gradle.licenser.api.license.LicenseRule;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QuiltLicenserGradleExtension implements PatternFilterable {
	/**
	 * The filter to apply to the source files.
	 * <p>
	 * By default, this only includes a few excludes for binary files or files without standardized comment formats.
	 */
	@Delegate
	public PatternFilterable patternFilterable;

	@PackageScope
	final LicenseHeader header = new LicenseHeader(new ArrayList<>());
	@PackageScope
	final TextResourceFactory textResources;

	@PackageScope
	Property<Boolean> buildDependCheck;

	@PackageScope
	final List<SourceSet> excludedSourceSets = new ArrayList<>();

	@Inject
	public QuiltLicenserGradleExtension(final ObjectFactory objects, final Project project) {
		this.patternFilterable = new PatternSet();
		this.textResources = project.getResources().getText();

		this.exclude(
				// Files without standard comment format.
				"**/*.txt",
				"**/*.json",
				"**/*.md", // Supports HTML comments but would require modification of this plugin first

				// Image files
				"**/*.jpg",
				"**/*.png",
				"**/*.gif",
				"**/*.bmp",
				"**/*.ico",
				"**/*.webp",
				"**/*.qoi",

				// Binary files
				"**/*.zip",
				"**/*.jar",
				"**/*.tar",
				"**/*.class",
				"**/*.bin",

				// Manifest
				"**/MANIFEST.MF",
				"**/META-INF/services/**",

				// @TODO remove once supported
				"**/package-info.java",
				"**/module-info.java"
		);
	}

	/**
	 * Adds a rule from a file.
	 *
	 * @param header the file
	 */
	public void rule(Object header) {
		if (header instanceof Path path) {
			this.rule(LicenseRule.fromFile(path));
			return;
		}

		this.rule(new LicenseRule(this.textResources.fromFile(header, StandardCharsets.UTF_8.name()).asString()));
	}

	/**
	 * Adds a license rule.
	 *
	 * @param rule the license rule
	 */
	public void rule(LicenseRule rule) {
		this.header.addRule(rule);
	}

	public LicenseHeader getLicenseHeader() {
		return this.header;
	}

	/**
	 * {@return the delegated filterable pattern}
	 */
	public PatternFilterable asPatternFilterable() {
		return this.patternFilterable;
	}

	@Override
	public @NotNull Set<String> getIncludes() {
		return this.patternFilterable.getIncludes();
	}

	@Override
	public @NotNull Set<String> getExcludes() {
		return this.patternFilterable.getExcludes();
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension setIncludes(@NotNull Iterable<String> iterable) {
		this.patternFilterable.setIncludes(iterable);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension setExcludes(@NotNull Iterable<String> iterable) {
		this.patternFilterable.setIncludes(iterable);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension include(String @NotNull ... strings) {
		this.patternFilterable.include(strings);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension include(@NotNull Iterable<String> iterable) {
		this.patternFilterable.include(iterable);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension include(@NotNull Spec<FileTreeElement> spec) {
		this.patternFilterable.include(spec);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension include(@NotNull Closure closure) {
		this.patternFilterable.include(closure);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension exclude(String @NotNull ... strings) {
		this.patternFilterable.exclude(strings);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension exclude(@NotNull Iterable<String> iterable) {
		this.patternFilterable.exclude(iterable);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension exclude(@NotNull Spec<FileTreeElement> spec) {
		this.patternFilterable.exclude(spec);
		return this;
	}

	@Override
	public @NotNull QuiltLicenserGradleExtension exclude(@NotNull Closure closure) {
		this.patternFilterable.exclude(closure);
		return this;
	}

	/**
	 * Excludes an entire source set.
	 *
	 * @param sourceSet the source set
	 * @return {@code this}
	 */
	public @NotNull QuiltLicenserGradleExtension exclude(@NotNull SourceSet sourceSet) {
		this.excludedSourceSets.add(sourceSet);
		return this;
	}

	/**
	 * {@return {@code true} if the source set is excluded, or {@code false} otherwise}
	 *
	 * @param sourceSet the source set to check
	 */
	public boolean isSourceSetExcluded(@NotNull SourceSet sourceSet) {
		return this.excludedSourceSets.contains(sourceSet);
	}
}
