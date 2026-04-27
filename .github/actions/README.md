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

## upload-gcs-artifact

Upload a file to a public GCS bucket at a versioned path; write `<file>.sha256` sidecar and `<latest_tag>.txt` pointer.

```yaml
uses: ./.github/actions/upload-gcs-artifact
with:
  file_path: path/to/file.tar.gz
  prefix: my-prefix
  workload_identity_provider: ${{ vars.CI_WORKLOAD_IDENTITY_PROVIDER }}
  service_account: ${{ vars.CI_SERVICE_ACCOUNT }}
```

**Outputs:** `tag`, `gcs_uri`, `https_url`, `sha256`

## download-gcs-artifact

Download a public GCS object via HTTPS (no auth needed). Resolve via explicit `tag` or via `latest_tag` pointer file.

```yaml
uses: ./.github/actions/download-gcs-artifact
with:
  prefix: my-prefix
  tag: main.20260427-1023-SHAabc1234
  filename: file.tar.gz
  destination: ./output
```

**Outputs:** `artifact_file`, `resolved_tag`

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

