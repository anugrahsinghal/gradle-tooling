// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package intellij;

import org.gradle.internal.impldep.com.google.common.base.Objects;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public final class DefaultFileCollectionDependency extends AbstractExternalDependency implements FileCollectionDependency {
	private static final long serialVersionUID = 1L;

	private Collection<File> files;
	private boolean excludedFromIndexing;

	public DefaultFileCollectionDependency() {
		this(new ArrayList<>());
	}

	public DefaultFileCollectionDependency(Collection<File> files) {
		super(new DefaultExternalDependencyId(null, files.toString(), null));
		this.files = new ArrayList<>(files);
		excludedFromIndexing = false;
	}

	public DefaultFileCollectionDependency(FileCollectionDependency dependency) {
		super(dependency);
		files = new ArrayList<>(dependency.getFiles());
		excludedFromIndexing = dependency.isExcludedFromIndexing();
	}

	@Override
	public @NotNull Collection<File> getFiles() {
		return files;
	}

	public void setFiles(@NotNull Collection<File> files) {
		this.files = new ArrayList<>(files);
	}

	@Override
	public boolean isExcludedFromIndexing() {
		return excludedFromIndexing;
	}

	public void setExcludedFromIndexing(boolean excludedFromIndexing) {
		this.excludedFromIndexing = excludedFromIndexing;
	}

	@Override
	public String toString() {
		return "file collection dependency{" +
				"files=" + files +
				'}';
	}
}
