import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';
import { CustomStructureData, StructureType } from '../renderers/structure-renderer.types';

export interface AnimationContext {
  scene: THREE.Scene;
  camera: THREE.PerspectiveCamera;
  controls: OrbitControls;
  structureType: StructureType;
  addTemporaryObject: (obj: THREE.Object3D) => void;
  clearTemporaryObjects: () => void;
  data: CustomStructureData;
  updateData: (newData: CustomStructureData) => void;
  announce?: (message: string) => void;
}

export interface StructureAnimator {
  performOperation(
    operationName: string,
    ctx: AnimationContext,
    options?: unknown
  ): Promise<void>;
}
