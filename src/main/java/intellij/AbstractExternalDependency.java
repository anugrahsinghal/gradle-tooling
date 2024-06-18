// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package intellij;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
public abstract class AbstractExternalDependency implements ExternalDependency {
	private static final long serialVersionUID = 1L;

	private final @NotNull DefaultExternalDependencyId myId;
	private String myScope;
	private @NotNull Collection<? extends ExternalDependency> myDependencies;
	private String mySelectionReason;
	private int myClasspathOrder;
	private boolean myExported;

	public AbstractExternalDependency() {
		this(new DefaultExternalDependencyId());
	}

	public AbstractExternalDependency(ExternalDependencyId id) {
		this(id, null, null);
	}

	public AbstractExternalDependency(
			ExternalDependencyId id,
			String selectionReason,
			Collection<? extends ExternalDependency> dependencies
	) {
		this(id, selectionReason, dependencies, null, 0, false);
	}

	public AbstractExternalDependency(
			ExternalDependencyId id,
			String selectionReason,
			Collection<? extends ExternalDependency> dependencies,
			String scope,
			int classpathOrder,
			boolean exported
	) {
		myId = new DefaultExternalDependencyId(id);
		mySelectionReason = selectionReason;
		myDependencies = ModelFactory.createCopy(dependencies);
		myScope = scope;
		myClasspathOrder = classpathOrder;
		myExported = exported;
	}

	public AbstractExternalDependency(ExternalDependency dependency) {
		this(
				dependency.getId(),
				dependency.getSelectionReason(),
				dependency.getDependencies(),
				dependency.getScope(),
				dependency.getClasspathOrder(),
				dependency.getExported()
		);
	}

	@Override
	public @NotNull ExternalDependencyId getId() {
		return myId;
	}

	@Override
	public String getName() {
		return myId.getName();
	}

	public void setName(String name) {
		myId.setName(name);
	}

	@Override
	public String getGroup() {
		return myId.getGroup();
	}

	public void setGroup(String group) {
		myId.setGroup(group);
	}

	@Override
	public String getVersion() {
		return myId.getVersion();
	}

	public void setVersion(String version) {
		myId.setVersion(version);
	}

	@NotNull
	@Override
	public String getPackaging() {
		return myId.getPackaging();
	}

	public void setPackaging(@NotNull String packaging) {
		myId.setPackaging(packaging);
	}

	@Nullable
	@Override
	public String getClassifier() {
		return myId.getClassifier();
	}

	public void setClassifier(@Nullable String classifier) {
		myId.setClassifier(classifier);
	}

	@Nullable
	@Override
	public String getSelectionReason() {
		return mySelectionReason;
	}

	public void setSelectionReason(String selectionReason) {
		this.mySelectionReason = selectionReason;
	}

	@Override
	public int getClasspathOrder() {
		return myClasspathOrder;
	}

	public void setClasspathOrder(int order) {
		myClasspathOrder = order;
	}

	@Override
	public String getScope() {
		return myScope;
	}

	public void setScope(String scope) {
		this.myScope = scope;
	}

	@Override
	public @NotNull Collection<? extends ExternalDependency> getDependencies() {
		return myDependencies;
	}

	public void setDependencies(@NotNull Collection<? extends ExternalDependency> dependencies) {
		myDependencies = dependencies;
	}

	@Override
	public boolean getExported() {
		return myExported;
	}

	public void setExported(boolean exported) {
		myExported = exported;
	}


	private static final class DependenciesIterator implements Iterator<AbstractExternalDependency> {
		private final Set<AbstractExternalDependency> mySeenDependencies;
		private final ArrayDeque<ExternalDependency> myToProcess;
		private final ArrayList<Integer> myProcessedStructure;

		private DependenciesIterator(@NotNull Collection<? extends ExternalDependency> dependencies) {
			mySeenDependencies = Collections.newSetFromMap(new IdentityHashMap<>());
			myToProcess = new ArrayDeque<>(dependencies);
			myProcessedStructure = new ArrayList<>();
		}

		@Override
		public boolean hasNext() {
			AbstractExternalDependency dependency = (AbstractExternalDependency) myToProcess.peekFirst();
			if (dependency == null) return false;
			if (mySeenDependencies.contains(dependency)) {
				myToProcess.removeFirst();
				return hasNext();
			}
			return true;
		}

		@Override
		public AbstractExternalDependency next() {
			AbstractExternalDependency dependency = (AbstractExternalDependency) myToProcess.removeFirst();
			if (mySeenDependencies.add(dependency)) {
				myToProcess.addAll(dependency.myDependencies);
				myProcessedStructure.add(dependency.myDependencies.size());
				return dependency;
			} else {
				return next();
			}
		}
	}
}

