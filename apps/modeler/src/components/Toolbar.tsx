interface ToolbarProps {
  filename: string;
  onNew: () => void;
  onOpen: () => void;
  onSave: () => void;
  onDownload: () => void;
  onDeploy: () => void;
  dirty: boolean;
}

export function Toolbar({ filename, onNew, onOpen, onSave, onDownload, onDeploy, dirty }: ToolbarProps) {
  return (
    <div className="flex h-12 items-center justify-between border-b border-gray-200 bg-white px-4">
      <div className="flex items-center gap-4">
        <h1 className="text-sm font-bold text-gray-900">OOP Modeler</h1>
        <span className="text-sm text-gray-500">
          {filename}{dirty ? ' *' : ''}
        </span>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={onNew}
          className="rounded px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          title="New Diagram"
        >
          New
        </button>
        <button
          onClick={onOpen}
          className="rounded px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          title="Open File"
        >
          Open
        </button>
        <button
          onClick={onDownload}
          className="rounded px-3 py-1.5 text-sm text-gray-600 hover:bg-gray-100"
          title="Download BPMN"
        >
          Download
        </button>
        <div className="mx-2 h-5 w-px bg-gray-200" />
        <button
          onClick={onDeploy}
          className="rounded-md bg-blue-600 px-4 py-1.5 text-sm font-medium text-white hover:bg-blue-700"
          title="Deploy to Runtime"
        >
          Deploy
        </button>
      </div>
    </div>
  );
}
