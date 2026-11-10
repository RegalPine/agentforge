/**
 * Reference - immutable identifier + version pinning.
 * Maps to backend io.github.regalpine.agentforge.capability.reference.Reference
 */
export interface Reference {
  id: string;
  version: string;
}

export function createReference(id: string, version: string): Reference {
  return { id, version };
}

export function referenceEquals(a: Reference, b: Reference): boolean {
  return a.id === b.id && a.version === b.version;
}

export function referenceToString(ref: Reference): string {
  return `${ref.id}:${ref.version}`;
}
