# Versioning

Mini Core Banking follows Semantic Versioning for release planning.

Version numbers use the following format:

```text
MAJOR.MINOR.PATCH
```

# Version Line Policy

| Version Line | Meaning |
| --- | --- |
| v1.x | Learning Prototype |
| v2.0 | Reliable Transfer MVP |
| v2.1 | Reliability |
| v2.2 | Observability |
| v2.3 | Platform |
| v3 | Future Architecture |

# Major Release

A major release introduces significant architectural or compatibility changes.

Examples:

- Changing the primary architecture style.
- Reworking core domain boundaries.
- Introducing a new deployment or service topology.
- Breaking API or operational compatibility.

# Minor Release

A minor release adds planned capabilities without changing the major architecture direction.

Examples:

- Adding reliability features after the MVP.
- Adding observability components.
- Improving platform and runtime behavior.
- Extending existing APIs in a compatible way.

# Patch Release

A patch release fixes defects or makes small non-breaking improvements.

Examples:

- Bug fixes.
- Test fixes.
- Documentation corrections.
- Small configuration corrections.
- Security or dependency patch updates that do not change behavior.

# v2 Release Strategy

| Release | Scope |
| --- | --- |
| v2.0.0 | First reliable transfer MVP release. |
| v2.1.0 | Reliability upgrade release. |
| v2.2.0 | Observability release. |
| v2.3.0 | Platform readiness release. |
| v2.x.y | Patch releases for defects, documentation, and non-breaking maintenance. |

# Tagging Policy

Each completed release should be tagged using the full semantic version.

Examples:

- `v2.0.0`
- `v2.1.0`
- `v2.2.0`
- `v2.3.0`

Patch releases should increment only the patch number.

Example:

- `v2.0.1`
