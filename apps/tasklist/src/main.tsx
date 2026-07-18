import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import '@bpmn-io/form-js-viewer/dist/assets/form-js.css';
import App from './App';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>
);
