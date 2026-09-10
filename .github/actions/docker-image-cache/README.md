# Composite GitHub Actions

Example usage can be found in [`run-integration-tests.yml`](../../workflows/run-integration-tests.yml)

## Notes
* [Parallel execution](https://github.blog/changelog/2026-06-25-actions-steps-can-now-be-run-in-parallel/) is not yet available for composite actions
  * Therefore `restore` and `setup-buildx` are currently split as it's recommended that they be implemented in parallel downstream
