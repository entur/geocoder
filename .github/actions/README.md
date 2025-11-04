# GitHub Actions - Composite Actions

This directory contains reusable composite actions for the geocoder project.

## generate-image-tag

Generates a consistent Docker image tag based on the current branch and commit SHA.

**Use case:** Standardize image tagging across all Docker builds.

**Usage:**
```yaml
- name: Generate tag
  id: tag
  uses: ./.github/actions/generate-image-tag

- name: Use the tag
  run: echo "Tag is ${{ steps.tag.outputs.image_tag }}"
```

**Outputs:**
- `image_tag`: The generated tag (format: `{branch}.{date}-SHA{short-sha}`, e.g., `main.20251103-SHA1234567`)

## upload-docker-artifact

Packages a workflow artifact or file as a Docker image and pushes it to GCR for long-term storage and easy reuse.

**Use case:** Store build artifacts (like compiled data files) as Docker images so they can be referenced by tag instead of workflow run IDs.

**Usage (from file):**
```yaml
- name: Upload to GCR
  uses: ./.github/actions/upload-docker-artifact
  with:
    file_path: path/to/file.tar.gz       # Path to file on disk
    image_name: my-data-image            # Docker image name (without registry and tag)
    workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
    service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

**Inputs:**
- `file_path` (optional): Direct path to file on disk
- `image_name` (required): Docker image name

**Outputs:**
- `image_tag`: The generated tag (e.g., `main.20251103-SHA1234567`)
- `full_image`: Full image reference (e.g., `eu.gcr.io/entur-system-1287/my-data-image:main.20251103-SHA1234567`)

## download-docker-artifact

Extracts an artifact from a Docker image stored in GCR.

**Use case:** Download previously stored artifacts from GCR by image tag instead of workflow run ID.

**Usage:**
```yaml
- name: Download from GCR
  uses: ./.github/actions/download-docker-artifact
  with:
    image: my-data-image:latest  # Image name and tag (without registry)
    destination: ./output        # Where to extract the file
    workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
    service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

**Outputs:**
- `artifact_file`: Path to the extracted artifact file

## Benefits

- **No run IDs needed:** Reference artifacts by semantic tags (e.g., `latest`, `v1.2.3`)
- **Long-term storage:** GCR retention vs. GitHub Actions artifact retention
- **Reusable:** Can be used across different workflows
- **Simple:** Clean abstraction over Docker commands

