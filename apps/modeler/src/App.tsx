import { useState, useRef, useCallback } from 'react';
import { AuthProvider, AuthGuard, createKeycloakInstance, useAuth } from '@oop/auth';
import { BpmnEditor, EMPTY_BPMN, toActivitiXml } from './components/BpmnEditor';
import { Toolbar } from './components/Toolbar';
import { DeployDialog } from './components/DeployDialog';
import { useApiClient } from './api';

const keycloak = createKeycloakInstance({ clientId: 'modeler-ui' });

function ModelerApp() {
  const { user, logout } = useAuth();
  const { deployFiles } = useApiClient();

  const [xml, setXml] = useState<string>(EMPTY_BPMN);
  const [filename, setFilename] = useState('process.bpmn');
  const [dirty, setDirty] = useState(false);
  const [deployOpen, setDeployOpen] = useState(false);
  const [currentXml, setCurrentXml] = useState<string>(EMPTY_BPMN);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const modelerRef = useRef<any>(null);

  const handleXmlChange = useCallback((newXml: string) => {
    setCurrentXml(newXml);
    setDirty(true);
  }, []);

  function handleNew() {
    if (dirty && !confirm('Discard unsaved changes?')) return;
    setXml(EMPTY_BPMN);
    setCurrentXml(EMPTY_BPMN);
    setFilename('process.bpmn');
    setDirty(false);
  }

  function handleOpen() {
    fileInputRef.current?.click();
  }

  async function handleFileOpen(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const text = await file.text();
    setXml(text);
    setCurrentXml(text);
    setFilename(file.name);
    setDirty(false);
    e.target.value = '';
  }

  async function handleDownload() {
    const xmlToSave = await getLatestXml();
    const blob = new Blob([xmlToSave], { type: 'application/xml' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
    setDirty(false);
  }

  function handleSave() {
    handleDownload();
  }

  async function getLatestXml(): Promise<string> {
    if (modelerRef.current) {
      const { xml } = await modelerRef.current.saveXML({ format: true });
      return toActivitiXml(xml || currentXml);
    }
    return toActivitiXml(currentXml);
  }

  async function handleDeploy(files: File[]) {
    const result = await deployFiles(files);
    return result;
  }

  return (
    <div className="flex h-screen flex-col">
      <Toolbar
        filename={filename}
        onNew={handleNew}
        onOpen={handleOpen}
        onSave={handleSave}
        onDownload={handleDownload}
        onDeploy={() => setDeployOpen(true)}
        dirty={dirty}
      />

      <div className="flex flex-1 overflow-hidden">
        <div className="flex-1">
          <BpmnEditor
            xml={xml}
            onXmlChange={handleXmlChange}
            onModelerReady={(m) => { modelerRef.current = m; }}
          />
        </div>
      </div>

      <div className="flex h-8 items-center justify-between border-t border-gray-200 bg-gray-50 px-4 text-xs text-gray-500">
        <span>{user?.username}</span>
        <button onClick={logout} className="text-red-500 hover:text-red-700">Logout</button>
      </div>

      <input
        ref={fileInputRef}
        type="file"
        accept=".bpmn,.bpmn20.xml,.xml"
        className="hidden"
        onChange={handleFileOpen}
      />

      <DeployDialog
        open={deployOpen}
        onClose={() => setDeployOpen(false)}
        onDeploy={handleDeploy}
        bpmnXml={currentXml}
        bpmnFilename={filename}
      />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider keycloak={keycloak}>
      <AuthGuard>
        <ModelerApp />
      </AuthGuard>
    </AuthProvider>
  );
}
