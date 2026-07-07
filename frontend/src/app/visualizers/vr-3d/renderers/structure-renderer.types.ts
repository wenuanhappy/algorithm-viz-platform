import * as THREE from 'three';

export type StructureType =
  | 'array'
  | 'stack'
  | 'queue'
  | 'linked-list'
  | 'binary-tree'
  | 'b-plus-tree';

export interface StructureInfo {
  title: string;
  definition: string;
  features: string[];
  operations: { name: string; complexity: string; description: string }[];
  useCases: string[];
}

export interface CustomStructureData {
  values: string[];
}

export interface StructureRendererContext {
  addObject: (obj: THREE.Object3D) => void;
  data: CustomStructureData;
}

export interface StructureOperation {
  name: string;
  complexity: string;
  description: string;
}
