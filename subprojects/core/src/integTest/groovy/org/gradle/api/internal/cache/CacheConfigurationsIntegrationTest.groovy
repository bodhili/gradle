/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.api.internal.cache

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.internal.time.TimestampSuppliers

class CacheConfigurationsIntegrationTest extends AbstractIntegrationSpec {
    private static final int MODIFIED_AGE_IN_DAYS_FOR_RELEASED_DISTS = CacheConfigurationsInternal.DEFAULT_MAX_AGE_IN_DAYS_FOR_RELEASED_DISTS + 1
    private static final int MODIFIED_AGE_IN_DAY_FOR_SNAPSHOT_DISTS = CacheConfigurationsInternal.DEFAULT_MAX_AGE_IN_DAYS_FOR_SNAPSHOT_DISTS + 1
    private static final int MODIFIED_AGE_IN_DAYS_FOR_DOWNLOADED_CACHE_ENTRIES = CacheConfigurationsInternal.DEFAULT_MAX_AGE_IN_DAYS_FOR_DOWNLOADED_CACHE_ENTRIES + 1
    private static final int MODIFIED_AGE_IN_DAYS_FOR_CREATED_CACHE_ENTRIES = CacheConfigurationsInternal.DEFAULT_MAX_AGE_IN_DAYS_FOR_CREATED_CACHE_ENTRIES + 1

    private static final String THIS_PROPERTY_FINAL_ERROR = "The value for this property is final and cannot be changed any further"
    private static final String REMOVE_UNUSED_ENTRIES_FINAL_ERROR = "The value for property 'removeUnusedEntriesOlderThan' is final and cannot be changed any further"

    def setup() {
        requireOwnGradleUserHomeDir()
    }

    def "can configure caches via init script and query from settings script"() {
        def initDir = new File(executer.gradleUserHomeDir, "init.d")
        initDir.mkdirs()
        new File(initDir, "cache-settings.gradle") << """
            beforeSettings { settings ->
                settings.caches {
                    cleanup = Cleanup.DISABLED
                    releasedWrappers.removeUnusedEntriesAfterDays = ${MODIFIED_AGE_IN_DAYS_FOR_RELEASED_DISTS}
                    snapshotWrappers.removeUnusedEntriesAfterDays = ${MODIFIED_AGE_IN_DAY_FOR_SNAPSHOT_DISTS}
                    downloadedResources.removeUnusedEntriesAfterDays = ${MODIFIED_AGE_IN_DAYS_FOR_DOWNLOADED_CACHE_ENTRIES}
                    createdResources.removeUnusedEntriesAfterDays = ${MODIFIED_AGE_IN_DAYS_FOR_CREATED_CACHE_ENTRIES}
                }
            }
        """
        settingsFile << """
            caches {
                releasedWrappers { ${assertValueIsSameInDays(MODIFIED_AGE_IN_DAYS_FOR_RELEASED_DISTS)} }
                snapshotWrappers { ${assertValueIsSameInDays(MODIFIED_AGE_IN_DAY_FOR_SNAPSHOT_DISTS)} }
                downloadedResources { ${assertValueIsSameInDays(MODIFIED_AGE_IN_DAYS_FOR_DOWNLOADED_CACHE_ENTRIES)} }
                createdResources { ${assertValueIsSameInDays(MODIFIED_AGE_IN_DAYS_FOR_CREATED_CACHE_ENTRIES)} }
            }
        """

        expect:
        succeeds("help")
    }

    def "can configure caches to a custom timestamp"() {
        def initDir = new File(executer.gradleUserHomeDir, "init.d")
        initDir.mkdirs()

        def releasedDistTimestamp = TimestampSuppliers.daysAgo(MODIFIED_AGE_IN_DAYS_FOR_RELEASED_DISTS).get()
        def snapshotDistTimestamp = TimestampSuppliers.daysAgo(MODIFIED_AGE_IN_DAY_FOR_SNAPSHOT_DISTS).get()
        def downloadedResourcesTimestamp = TimestampSuppliers.daysAgo(MODIFIED_AGE_IN_DAYS_FOR_DOWNLOADED_CACHE_ENTRIES).get()
        def createdResourcesTimestamp = TimestampSuppliers.daysAgo(MODIFIED_AGE_IN_DAYS_FOR_CREATED_CACHE_ENTRIES).get()

        new File(initDir, "cache-settings.gradle") << """
            import java.util.function.Supplier

            beforeSettings { settings ->
                settings.caches {
                    cleanup = Cleanup.DISABLED
                    releasedWrappers.removeUnusedEntriesOlderThan = ${releasedDistTimestamp}
                    snapshotWrappers.removeUnusedEntriesOlderThan = ${snapshotDistTimestamp}
                    downloadedResources.removeUnusedEntriesOlderThan = ${downloadedResourcesTimestamp}
                    createdResources.removeUnusedEntriesOlderThan = ${createdResourcesTimestamp}
                }
            }
        """
        settingsFile << """
            caches {
                assert releasedWrappers.removeUnusedEntriesOlderThan.get() == ${releasedDistTimestamp}
                assert snapshotWrappers.removeUnusedEntriesOlderThan.get() == ${snapshotDistTimestamp}
                assert downloadedResources.removeUnusedEntriesOlderThan.get() == ${downloadedResourcesTimestamp}
                assert createdResources.removeUnusedEntriesOlderThan.get() == ${createdResourcesTimestamp}
            }
        """

        expect:
        succeeds("help")
    }

    def "cannot configure caches from settings script (#property)"() {
        settingsFile << """
            caches {
                ${modifyCacheConfiguration(property, value)}
            }
        """

        expect:
        fails("help")
        failureCauseContains(error)

        where:
        property                                           | error                             | value
        'cleanup'                                          | THIS_PROPERTY_FINAL_ERROR         | 'Cleanup.DISABLED'
        'releasedWrappers.removeUnusedEntriesAfterDays'    | REMOVE_UNUSED_ENTRIES_FINAL_ERROR | "${MODIFIED_AGE_IN_DAYS_FOR_RELEASED_DISTS}"
        'snapshotWrappers.removeUnusedEntriesAfterDays'    | REMOVE_UNUSED_ENTRIES_FINAL_ERROR | "${MODIFIED_AGE_IN_DAY_FOR_SNAPSHOT_DISTS}"
        'downloadedResources.removeUnusedEntriesAfterDays' | REMOVE_UNUSED_ENTRIES_FINAL_ERROR | "${MODIFIED_AGE_IN_DAYS_FOR_DOWNLOADED_CACHE_ENTRIES}"
        'createdResources.removeUnusedEntriesAfterDays'    | REMOVE_UNUSED_ENTRIES_FINAL_ERROR | "${MODIFIED_AGE_IN_DAYS_FOR_CREATED_CACHE_ENTRIES}"
    }

    static String modifyCacheConfiguration(String property, String value) {
        return """
            ${property} = ${value}
        """
    }

    static String assertValueIsSameInDays(int configuredDaysAgo) {
        return """
            def timestamp = removeUnusedEntriesOlderThan.get()
            def daysAgo = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp)
            assert daysAgo == ${configuredDaysAgo}
        """
    }
}
