import { useState, useRef } from 'react';

interface DeployDialogProps {
  open: boolean;
  onClose: () => void;
  onDeploy: (files: File[]) => Promise<void>;
  bpmnXml?: string;
  bpmnFilename?: string;
}

export function DeployDialog({ open, onClose, onDeploy, bpmnXml, bpmnFilename }: DeployDialogProps) {
  const [formFiles, setFormFiles] = useState<File[]>([]);
  const [includeBpmn, setIncludeBpmn] = useState(true);
  const [deploying, setDeploying] = useState(false);
  const [result, setResult] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  if (!open) return null;

  async function handleDeploy() {
    setDeploying(true);
    setError(null);
    setResult(null);

    try {
      const files: File[] = [...formFiles];

      if (includeBpmn && bpmnXml) {
        const filename = bpmnFilename || 'process.bpmn';
        const bpmnFile = new File([bpmnXml], filename, { type: 'application/xml' });
        files.unshift(bpmnFile);
      }

      if (files.length === 0) {
        setError('No files to deploy');
        setDeploying(false);
        return;
      }

      await onDeploy(files);
      setResult({ success: true });
      setFormFiles([]);
    } catch (e: any) {
      setError(e.message);
    } finally {
      setDeploying(false);
    }
  }

  function handleFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    if (e.target.files) {
      setFormFiles((prev) => [...prev, ...Array.from(e.target.files!)]);
    }
  }

  function removeFile(index: number) {
    setFormFiles((prev) => prev.filter((_, i) => i !== index));
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-lg rounded-lg bg-white shadow-xl">
        <div className="border-b border-gray-200 px-6 py-4">
          <h3 className="text-lg font-semibold text-gray-900">Deploy to Runtime</h3>
          <p className="mt-1 text-sm text-gray-500">Deploy BPMN and form files to the runtime bundle.</p>
        </div>

        <div className="px-6 py-4 space-y-4">
          {bpmnXml && (
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={includeBpmn}
                onChange={(e) => setIncludeBpmn(e.target.checked)}
                className="rounded border-gray-300"
              />
              <span className="text-sm text-gray-700">
                Include current BPMN ({bpmnFilename || 'process.bpmn'})
              </span>
            </label>
          )}

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Form files (.form)
            </label>
            <input
              ref={fileInputRef}
              type="file"
              accept=".form,.form.json,.json"
              multiple
              onChange={handleFileSelect}
              className="hidden"
            />
            <button
              onClick={() => fileInputRef.current?.click()}
              className="rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
            >
              Add Form Files
            </button>

            {formFiles.length > 0 && (
              <ul className="mt-2 space-y-1">
                {formFiles.map((file, i) => (
                  <li key={i} className="flex items-center justify-between rounded bg-gray-50 px-3 py-1.5 text-sm">
                    <span className="text-gray-700">{file.name}</span>
                    <button
                      onClick={() => removeFile(i)}
                      className="text-red-500 hover:text-red-700 text-xs"
                    >
                      Remove
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {error && (
            <div className="rounded-md bg-red-50 p-3">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {result?.success && (
            <div className="rounded-md bg-green-50 p-3">
              <p className="text-sm text-green-700">Deployed successfully!</p>
            </div>
          )}
        </div>

        <div className="flex justify-end gap-3 border-t border-gray-200 px-6 py-4">
          <button
            onClick={onClose}
            className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            Close
          </button>
          <button
            onClick={handleDeploy}
            disabled={deploying}
            className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {deploying ? 'Deploying...' : 'Deploy'}
          </button>
        </div>
      </div>
    </div>
  );
}
