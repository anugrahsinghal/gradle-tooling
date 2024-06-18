// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package intellij;


/**
 * @author Vladislav.Soroka
 */
public class DefaultUnresolvedExternalDependency extends AbstractExternalDependency implements UnresolvedExternalDependency {
	private static final long serialVersionUID = 1L;

	private String failureMessage;

	public DefaultUnresolvedExternalDependency() {
	}

	public DefaultUnresolvedExternalDependency(UnresolvedExternalDependency dependency) {
		super(dependency);
		failureMessage = dependency.getFailureMessage();
	}

	@Override
	public String getFailureMessage() {
		return failureMessage;
	}

	public void setFailureMessage(String failureMessage) {
		this.failureMessage = failureMessage;
	}

	@Override
	public String toString() {
		return "Unresolved dependency '" + getId() + "':" + failureMessage;
	}
}
