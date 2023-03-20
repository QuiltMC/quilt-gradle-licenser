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

package org.quiltmc.gradle.licenser.api.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public final class GitUtils {
	private GitUtils() {
		throw new UnsupportedOperationException("GitUtils only contains static definitions.");
	}

	private static String standardizePath(Path path) {
		var pathStr = path.toString();

		if (!File.separator.equals("/")) {
			pathStr = pathStr.replace(File.separator, "/");
		}

		return pathStr;
	}

	private static Git openGit(Project project) throws IOException {
		return Git.open(project.getRootProject().getProjectDir());
	}

	private static Path getRepoRoot(Git git) {
		return git.getRepository().getDirectory().toPath().getParent();
	}

	private static @Nullable AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
		Ref head = repository.getRefDatabase().findRef(ref);

		if (head.getObjectId() == null) {
			return null;
		}

		var walk = new RevWalk(repository);
		RevCommit commit = walk.parseCommit(head.getObjectId());
		RevTree tree = walk.parseTree(commit.getTree().getId());
		var oldTreeParser = new CanonicalTreeParser();

		try (ObjectReader oldReader = repository.newObjectReader()) {
			oldTreeParser.reset(oldReader, tree.getId());
		}

		return oldTreeParser;
	}

	/**
	 * Gets the latest commit hash of a file.
	 *
	 * @param git the git instance
	 * @param path the file
	 */
	public static @Nullable RevCommit getLatestCommit(Git git, Path path) {
		try {
			var pathStr = standardizePath(path);

			var log = git.log();

			if (!pathStr.isEmpty()) {
				log.addPath(pathStr);
			}

			Iterator<RevCommit> iterator = log
					.setMaxCount(1)
					.call()
					.iterator();

			// No commits exist - you will need to create a commit to have a hash
			if (!iterator.hasNext()) {
				return null;
			}

			// We only care about the last commit
			return iterator.next();
		} catch (GitAPIException e) {
			throw new GradleException(
					String.format("Failed to get commit hash of last commit of path %s", path),
					e
			);
		}
	}

	private static int getLatestCommitYear(Git git, Path path) {
		RevCommit latestCommit = getLatestCommit(git, path);

		if (latestCommit != null) {
			PersonIdent authorIdent = latestCommit.getAuthorIdent();
			Date authorDate = authorIdent.getWhen();
			TimeZone authorTimeZone = authorIdent.getTimeZone();

			var calendar = Calendar.getInstance(authorTimeZone);
			calendar.setTime(authorDate);
			return calendar.get(Calendar.YEAR);
		}

		return Calendar.getInstance().get(Calendar.YEAR);
	}

	public static int getModificationYear(Project project, Path path) {
		try (var git = openGit(project)) {
			Path repoRoot = getRepoRoot(git);
			path = repoRoot.relativize(path);
			var pathString = standardizePath(path);

			var formatter = new DiffFormatter(System.out);
			formatter.setRepository(git.getRepository());
			AbstractTreeIterator commitTreeIterator = prepareTreeParser(git.getRepository(), Constants.HEAD);

			if (commitTreeIterator == null) {
				return getLatestCommitYear(git, path);
			}

			var workTreeIterator = new FileTreeIterator(git.getRepository());
			List<DiffEntry> diffEntries = formatter.scan(commitTreeIterator, workTreeIterator);

			for (var entry : diffEntries) {
				if (entry.getNewPath().equals(pathString)) {
					return Calendar.getInstance().get(Calendar.YEAR);
				}
			}

			return getLatestCommitYear(git, path);
		} catch (IOException | GradleException e) {
			// ignored
		}

		return Calendar.getInstance().get(Calendar.YEAR);
	}
}
