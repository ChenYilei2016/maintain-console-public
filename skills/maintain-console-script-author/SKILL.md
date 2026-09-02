---
name: maintain-console-script-author
description: Generate a versioned Maintain Console tool import document from an operations goal. Use when authoring a new Groovy tool, its parameter schema, purpose, risk, and usage guidance for direct JSON import; do not use for editing an existing saved tool in place.
---

# Maintain Console Tool Author

Produce a runnable, reviewable tool import document rather than disconnected Groovy and Schema fragments.

## Workflow

1. Read [references/import-contract-v1.md](references/import-contract-v1.md). Its V1 field set and safety boundary are authoritative.
2. Decide whether the request can use platform helpers alone. When it needs business data, read [references/runtime-api.md](references/runtime-api.md).
3. If a required Bean name, method signature, arguments, return structure, or query/change semantic cannot be verified from the user's input or repository, ask only for those missing facts. Do not emit an import document yet.
4. Choose the smallest parameter set that makes the tool reusable. Use `$${name}` as a standalone Groovy expression and declare exactly the same names in `parameterSchema.parameters`.
5. Classify the tool as `QUERY`, `OPERATION`, or `UNSPECIFIED`. An `OPERATION` needs a concrete risk and impact note; use `UNSPECIFIED` only when the operation cannot be classified from confirmed facts.
6. Prefer structured results that match the operator's decision: text for explanation, metric for summary, table for rows, chart for trends, and file only for an actual download.
7. Validate the completed document against every invariant in the contract. Completion means the JSON is importable without manual field repair.

## Output

On success, output exactly one `json` code block containing the document. Keep all explanation, usage, and risk information inside the document fields so it can be pasted directly into the script workbench.

Sensitive parameters carry no `defaultValue`. Examples use synthetic values and never credentials. The document carries portable tool content only; placement, environments, identities, versions, targets, and script authorization stay in Maintain Console.
