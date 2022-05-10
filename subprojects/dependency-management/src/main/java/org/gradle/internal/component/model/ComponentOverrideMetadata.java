/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.internal.component.model;

import org.gradle.api.artifacts.ClientModule;

import javax.annotation.Nullable;

/**
 * Metadata about a component that will override the information obtained when resolving, typically specified by a dependency descriptor.
 * Metadata supplied in this way is applied inconsistently, because multiple dependencies can point to the same component with different
 * override metadata, and only one of these overrides will be used during dependency resolution.
 */
public interface ComponentOverrideMetadata {

    /**
     * If the dependency declared an artifact for the component, return it. Empty otherwise.
     */
    @Nullable
    IvyArtifactName getArtifact();

    /**
     * If the request originated from a ClientModule, return it. Null otherwise.
     */
    @Nullable
    ClientModule getClientModule();

    /**
     * Return true if the dependency declaration defines this component as changing.
     */
    boolean isChanging();

    /**
     * Return a copy of this override metadata with `isChanging()` set to true.
     */
    ComponentOverrideMetadata withChanging();
}
