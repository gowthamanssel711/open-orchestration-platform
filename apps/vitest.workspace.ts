import { defineWorkspace } from 'vitest/config';

export default defineWorkspace([
  'apps/operate',
  'apps/tasklist',
  'apps/analytics',
  'apps/modeler',
  'apps/shared/*',
]);
