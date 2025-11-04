# Composite Actions

Reusable actions for Docker workflows.

## generate-image-tag

Generate Docker image tags: `{branch}.{date}-SHA{sha}` (e.g., `main.20251104-SHA1234567`)

```yaml
uses: ./.github/actions/generate-image-tag
with:
  image_name: my-image
```

**Outputs:** `image_tag`, `base_image`, `full_image`

## docker-build-push

Build and push Docker images to GCR.

```yaml
uses: ./.github/actions/docker-build-push
with:
  image_name: my-image
  context: ./path
  push: true
  build_args: |
    ARG1=value1
    ARG2=value2
  workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
  service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

**Outputs:** `image_tag`

## upload-docker-artifact

Store files as Docker images in GCR. Useful for large build artifacts (data files, compiled outputs).

```yaml
uses: ./.github/actions/upload-docker-artifact
with:
  file_path: path/to/file.tar.gz
  image_name: my-data
  workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
  service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

**Outputs:** `image_tag`

## download-docker-artifact

Extract files from Docker images stored in GCR.

```yaml
uses: ./.github/actions/download-docker-artifact
with:
  image: my-data:latest
  destination: ./output
  workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
  service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

**Outputs:** `artifact_file`

## docker-scan

Security scan Docker images with Anchore Grype. Uploads results to GitHub Security.

```yaml
uses: ./.github/actions/docker-scan
with:
  image: my-image:tag
  workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
  service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

Fails on critical vulnerabilities. Results appear in Security tab.

