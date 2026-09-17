/*
 * Copyright 2025 IBM Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.watsonx.ai.project;

import static java.util.Objects.isNull;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the response body containing the project metadata.
 */
public class Project {

    private final String guid;
    private final String url;
    private final String createdAt;
    private final String updatedAt;
    private final String name;
    private final String description;
    private final String type;
    private final boolean publicProject;
    private final String creator;
    private final String creatorIamId;
    private final String generator;
    private final ProjectScope scope;
    private final List<String> tags;
    private final ProjectStorage storage;
    private final ProjectCatalog catalog;
    private final List<ProjectComputeInstance> compute;
    private final Map<String, Object> settings;
    private final List<Map<String, Object>> members;
    private final List<String> tools;
    private final Map<String, Object> github;

    private Project(Builder builder) {
        guid = builder.guid;
        url = builder.url;
        createdAt = builder.createdAt;
        updatedAt = builder.updatedAt;
        name = builder.name;
        description = builder.description;
        type = builder.type;
        publicProject = builder.publicProject;
        creator = builder.creator;
        creatorIamId = builder.creatorIamId;
        generator = builder.generator;
        scope = builder.scope;
        tags = isNull(builder.tags) ? null : List.copyOf(builder.tags);
        storage = builder.storage;
        catalog = builder.catalog;
        compute = isNull(builder.compute) ? null : List.copyOf(builder.compute);
        settings = isNull(builder.settings) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.settings));
        members = isNull(builder.members) ? null : List.copyOf(builder.members);
        tools = isNull(builder.tools) ? null : List.copyOf(builder.tools);
        github = isNull(builder.github) ? null : Collections.unmodifiableMap(new LinkedHashMap<>(builder.github));
    }

    @SuppressWarnings("unchecked")
    static Project from(Map<String, Object> metadata, Map<String, Object> entity) {
        var builder = new Builder();
        if (metadata != null) {
            builder.guid((String) metadata.get("guid"));
            builder.url((String) metadata.get("url"));
            builder.createdAt((String) metadata.get("created_at"));
            builder.updatedAt((String) metadata.get("updated_at"));
        }
        if (entity != null) {
            builder.name((String) entity.get("name"));
            builder.description((String) entity.get("description"));
            builder.type((String) entity.get("type"));
            Object pub = entity.get("public");
            if (pub instanceof Boolean b)
                builder.publicProject(b);
            builder.creator((String) entity.get("creator"));
            builder.creatorIamId((String) entity.get("creator_iam_id"));
            builder.generator((String) entity.get("generator"));
            builder.scope(mapToScope((Map<String, Object>) entity.get("scope")));
            builder.tags((List<String>) entity.get("tags"));
            builder.storage(mapToStorage((Map<String, Object>) entity.get("storage")));
            builder.catalog(mapToCatalog((Map<String, Object>) entity.get("catalog")));
            builder.compute(mapToComputeList((List<Map<String, Object>>) entity.get("compute")));
            builder.settings((Map<String, Object>) entity.get("settings"));
            builder.members((List<Map<String, Object>>) entity.get("members"));
            builder.tools((List<String>) entity.get("tools"));
            builder.github((Map<String, Object>) entity.get("github"));
        }
        return builder.build();
    }

    private static ProjectScope mapToScope(Map<String, Object> m) {
        if (m == null)
            return null;
        Object enforce = m.get("enforce_members");
        return new ProjectScope(
            (String) m.get("bss_account_id"),
            enforce instanceof Boolean b && b,
            (String) m.get("saml_instance_name"));
    }

    private static ProjectCatalog mapToCatalog(Map<String, Object> m) {
        if (m == null)
            return null;
        Object pub = m.get("public");
        return new ProjectCatalog(
            (String) m.get("guid"),
            pub instanceof Boolean b && b);
    }

    @SuppressWarnings("unchecked")
    private static ProjectStorage mapToStorage(Map<String, Object> m) {
        if (m == null)
            return null;
        return new ProjectStorage(
            (String) m.get("type"),
            (String) m.get("guid"),
            mapToStorageProperties((Map<String, Object>) m.get("properties")));
    }

    @SuppressWarnings("unchecked")
    private static ProjectStorageProperties mapToStorageProperties(Map<String, Object> m) {
        if (m == null)
            return null;
        Map<String, Object> rawCredentials = (Map<String, Object>) m.get("credentials");
        Map<String, ProjectStorageCredential> credentials = null;
        if (rawCredentials != null) {
            var mapped = new LinkedHashMap<String, ProjectStorageCredential>();
            for (var entry : rawCredentials.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> cred) {
                    mapped.put(entry.getKey(), mapToStorageCredential((Map<String, Object>) cred));
                }
            }
            credentials = Collections.unmodifiableMap(mapped);
        }
        return new ProjectStorageProperties(
            (String) m.get("bucket_name"),
            (String) m.get("bucket_region"),
            (String) m.get("endpoint_url"),
            credentials);
    }

    private static ProjectStorageCredential mapToStorageCredential(Map<String, Object> m) {
        return new ProjectStorageCredential(
            (String) m.get("api_key"),
            (String) m.get("service_id"),
            (String) m.get("access_key_id"),
            (String) m.get("secret_access_key"),
            (String) m.get("resource_key_crn"));
    }

    @SuppressWarnings("unchecked")
    private static List<ProjectComputeInstance> mapToComputeList(List<Map<String, Object>> list) {
        if (list == null)
            return null;
        return list.stream()
            .map(m -> new ProjectComputeInstance(
                (String) m.get("name"),
                (String) m.get("guid"),
                (String) m.get("type"),
                (String) m.get("crn"),
                (Map<String, Object>) m.get("credentials")))
            .toList();
    }

    /**
     * Returns the project UUID.
     *
     * @return the project GUID
     */
    public String guid() {
        return guid;
    }

    /**
     * Returns the project URL.
     *
     * @return the project URL
     */
    public String url() {
        return url;
    }

    /**
     * Returns the creation timestamp in UTC ISO 8601 format.
     *
     * @return the creation timestamp
     */
    public String createdAt() {
        return createdAt;
    }

    /**
     * Returns the last-updated timestamp in UTC ISO 8601 format.
     *
     * @return the updated timestamp
     */
    public String updatedAt() {
        return updatedAt;
    }

    /**
     * Returns the human-readable project name.
     *
     * @return the project name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the project description.
     *
     * @return the project description, or {@code null}
     */
    public String description() {
        return description;
    }

    /**
     * Returns the project type.
     *
     * @return the project type
     */
    public String type() {
        return type;
    }

    /**
     * Returns whether the project is public.
     *
     * @return {@code true} if the project is public
     */
    public boolean publicProject() {
        return publicProject;
    }

    /**
     * Returns the username of the project creator.
     *
     * @return the creator username, or {@code null}
     */
    public String creator() {
        return creator;
    }

    /**
     * Returns the IAM ID of the project creator.
     *
     * @return the creator IAM ID, or {@code null}
     */
    public String creatorIamId() {
        return creatorIamId;
    }

    /**
     * Returns the generator label used to create the project.
     *
     * @return the generator, or {@code null}
     */
    public String generator() {
        return generator;
    }

    /**
     * Returns the project scope.
     *
     * @return the {@link ProjectScope}, or {@code null}
     */
    public ProjectScope scope() {
        return scope;
    }

    /**
     * Returns the list of tags associated with the project.
     *
     * @return the tag list, or {@code null}
     */
    public List<String> tags() {
        return tags;
    }

    /**
     * Returns the storage configuration for the project.
     *
     * @return the {@link ProjectStorage}, or {@code null}
     */
    public ProjectStorage storage() {
        return storage;
    }

    /**
     * Returns the catalog associated with the project.
     *
     * @return the {@link ProjectCatalog}, or {@code null}
     */
    public ProjectCatalog catalog() {
        return catalog;
    }

    /**
     * Returns the list of compute instances associated with the project.
     *
     * @return the list of {@link ProjectComputeInstance}, or {@code null}
     */
    public List<ProjectComputeInstance> compute() {
        return compute;
    }

    /**
     * Returns the project settings object.
     *
     * @return the settings map, or {@code null}
     */
    public Map<String, Object> settings() {
        return settings;
    }

    /**
     * Returns the list of project members.
     *
     * @return the members list, or {@code null}
     */
    public List<Map<String, Object>> members() {
        return members;
    }

    /**
     * Returns the list of tools configured for the project.
     *
     * @return the tools list, or {@code null}
     */
    public List<String> tools() {
        return tools;
    }

    /**
     * Returns the GitHub integration object for the project.
     *
     * @return the github map, or {@code null}
     */
    public Map<String, Object> github() {
        return github;
    }

    /**
     * Returns a new {@link Builder} instance.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for constructing {@link Project} instances.
     */
    public static final class Builder {

        private String guid;
        private String url;
        private String createdAt;
        private String updatedAt;
        private String name;
        private String description;
        private String type;
        private boolean publicProject;
        private String creator;
        private String creatorIamId;
        private String generator;
        private ProjectScope scope;
        private List<String> tags;
        private ProjectStorage storage;
        private ProjectCatalog catalog;
        private List<ProjectComputeInstance> compute;
        private Map<String, Object> settings;
        private List<Map<String, Object>> members;
        private List<String> tools;
        private Map<String, Object> github;

        private Builder() {}

        /**
         * Sets the project GUID.
         *
         * @param guid the project GUID
         */
        public Builder guid(String guid) {
            this.guid = guid;
            return this;
        }

        /**
         * Sets the project URL.
         *
         * @param url the project URL
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp in UTC ISO 8601 format
         */
        public Builder createdAt(String createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        /**
         * Sets the last update timestamp.
         *
         * @param updatedAt the last update timestamp in UTC ISO 8601 format
         */
        public Builder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Sets the project name.
         *
         * @param name the project name
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the project description.
         *
         * @param description the project description
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the project type.
         *
         * @param type the project type
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets whether the project is public.
         *
         * @param publicProject {@code true} if the project is public, {@code false} otherwise
         */
        public Builder publicProject(boolean publicProject) {
            this.publicProject = publicProject;
            return this;
        }

        /**
         * Sets the display name of the project creator.
         *
         * @param creator the display name of the project creator
         */
        public Builder creator(String creator) {
            this.creator = creator;
            return this;
        }

        /**
         * Sets the IAM ID of the project creator.
         *
         * @param creatorIamId the IAM ID of the project creator
         */
        public Builder creatorIamId(String creatorIamId) {
            this.creatorIamId = creatorIamId;
            return this;
        }

        /**
         * Sets the generator identifier for the project.
         *
         * @param generator the generator identifier
         */
        public Builder generator(String generator) {
            this.generator = generator;
            return this;
        }

        /**
         * Sets the project scope.
         *
         * @param scope the {@link ProjectScope}
         */
        public Builder scope(ProjectScope scope) {
            this.scope = scope;
            return this;
        }

        /**
         * Sets the project tags.
         *
         * @param tags the list of tags
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Sets the project storage configuration.
         *
         * @param storage the {@link ProjectStorage}
         */
        public Builder storage(ProjectStorage storage) {
            this.storage = storage;
            return this;
        }

        /**
         * Sets the project catalog configuration.
         *
         * @param catalog the {@link ProjectCatalog}
         */
        public Builder catalog(ProjectCatalog catalog) {
            this.catalog = catalog;
            return this;
        }

        /**
         * Sets the list of compute instances associated with the project.
         *
         * @param compute the list of {@link ProjectComputeInstance}
         */
        public Builder compute(List<ProjectComputeInstance> compute) {
            this.compute = compute;
            return this;
        }

        /**
         * Sets the project settings.
         *
         * @param settings the settings map
         */
        public Builder settings(Map<String, Object> settings) {
            this.settings = settings;
            return this;
        }

        /**
         * Sets the project members.
         *
         * @param members the list of member maps
         */
        public Builder members(List<Map<String, Object>> members) {
            this.members = members;
            return this;
        }

        /**
         * Sets the project tools.
         *
         * @param tools the list of tool identifiers
         */
        public Builder tools(List<String> tools) {
            this.tools = tools;
            return this;
        }

        /**
         * Sets the GitHub integration configuration.
         *
         * @param github the GitHub configuration map
         */
        public Builder github(Map<String, Object> github) {
            this.github = github;
            return this;
        }

        /**
         * Builds a {@link Project} instance using the configured parameters.
         *
         * @return a new instance of {@link Project}
         */
        public Project build() {
            return new Project(this);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((guid == null) ? 0 : guid.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
        result = prime * result + ((updatedAt == null) ? 0 : updatedAt.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + Boolean.hashCode(publicProject);
        result = prime * result + ((creator == null) ? 0 : creator.hashCode());
        result = prime * result + ((creatorIamId == null) ? 0 : creatorIamId.hashCode());
        result = prime * result + ((generator == null) ? 0 : generator.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((tags == null) ? 0 : tags.hashCode());
        result = prime * result + ((storage == null) ? 0 : storage.hashCode());
        result = prime * result + ((catalog == null) ? 0 : catalog.hashCode());
        result = prime * result + ((compute == null) ? 0 : compute.hashCode());
        result = prime * result + ((settings == null) ? 0 : settings.hashCode());
        result = prime * result + ((members == null) ? 0 : members.hashCode());
        result = prime * result + ((tools == null) ? 0 : tools.hashCode());
        result = prime * result + ((github == null) ? 0 : github.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        if (guid == null) {
            if (other.guid != null)
                return false;
        } else if (!guid.equals(other.guid))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        } else if (!createdAt.equals(other.createdAt))
            return false;
        if (updatedAt == null) {
            if (other.updatedAt != null)
                return false;
        } else if (!updatedAt.equals(other.updatedAt))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (publicProject != other.publicProject)
            return false;
        if (creator == null) {
            if (other.creator != null)
                return false;
        } else if (!creator.equals(other.creator))
            return false;
        if (creatorIamId == null) {
            if (other.creatorIamId != null)
                return false;
        } else if (!creatorIamId.equals(other.creatorIamId))
            return false;
        if (generator == null) {
            if (other.generator != null)
                return false;
        } else if (!generator.equals(other.generator))
            return false;
        if (scope == null) {
            if (other.scope != null)
                return false;
        } else if (!scope.equals(other.scope))
            return false;
        if (tags == null) {
            if (other.tags != null)
                return false;
        } else if (!tags.equals(other.tags))
            return false;
        if (storage == null) {
            if (other.storage != null)
                return false;
        } else if (!storage.equals(other.storage))
            return false;
        if (catalog == null) {
            if (other.catalog != null)
                return false;
        } else if (!catalog.equals(other.catalog))
            return false;
        if (compute == null) {
            if (other.compute != null)
                return false;
        } else if (!compute.equals(other.compute))
            return false;
        if (settings == null) {
            if (other.settings != null)
                return false;
        } else if (!settings.equals(other.settings))
            return false;
        if (members == null) {
            if (other.members != null)
                return false;
        } else if (!members.equals(other.members))
            return false;
        if (tools == null) {
            if (other.tools != null)
                return false;
        } else if (!tools.equals(other.tools))
            return false;
        if (github == null) {
            if (other.github != null)
                return false;
        } else if (!github.equals(other.github))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Project [guid=" + guid + ", url=" + url + ", createdAt=" + createdAt + ", updatedAt=" + updatedAt
            + ", name=" + name + ", description=" + description + ", type=" + type + ", publicProject=" + publicProject
            + ", creator=" + creator + ", creatorIamId=" + creatorIamId + ", generator=" + generator
            + ", scope=" + scope + ", tags=" + tags + ", storage=" + storage + ", catalog=" + catalog
            + ", compute=" + compute + ", settings=" + settings + ", members=" + members
            + ", tools=" + tools + ", github=" + github + "]";
    }
}
