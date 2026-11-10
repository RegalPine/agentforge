/**
 * DataBinding - maps to backend DataBinding.
 * Connects node outputs to node inputs.
 */
export interface DataBinding {
  id: string;
  sourceNodeId: string;
  sourceField: string;
  targetNodeId: string;
  targetField: string;
  expression?: string;
}

/**
 * Expression - maps to backend Expression.
 */
export interface Expression {
  id: string;
  language: string;
  body: string;
}
