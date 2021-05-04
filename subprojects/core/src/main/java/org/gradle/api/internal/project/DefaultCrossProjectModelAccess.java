/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.project;

import org.gradle.api.UnknownProjectException;

import java.util.Set;
import java.util.TreeSet;

public class DefaultCrossProjectModelAccess implements CrossProjectModelAccess {
    private final ProjectRegistry<ProjectInternal> projectRegistry;

    public DefaultCrossProjectModelAccess(ProjectRegistry<ProjectInternal> projectRegistry) {
        this.projectRegistry = projectRegistry;
    }

    @Override
    public ProjectInternal getProject(ProjectInternal referrer, ProjectInternal relativeTo, String path) {
        ProjectInternal project = projectRegistry.getProject(relativeTo.absoluteProjectPath(path));
        if (project == null) {
            throw new UnknownProjectException(String.format("Project with path '%s' could not be found in %s.", path, relativeTo));
        }
        return project;
    }

    @Override
    public Set<? extends ProjectInternal> getSubprojects(ProjectInternal referrer, ProjectInternal relativeTo) {
        return new TreeSet<>(projectRegistry.getSubProjects(relativeTo.getPath()));
    }

    @Override
    public Set<? extends ProjectInternal> getAllprojects(ProjectInternal referrer, ProjectInternal relativeTo) {
        return new TreeSet<>(projectRegistry.getAllProjects(relativeTo.getPath()));
    }
}
