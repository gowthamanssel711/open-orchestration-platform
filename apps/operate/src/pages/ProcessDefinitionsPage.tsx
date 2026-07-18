import { useProcessDefinitions, useStartProcess } from '../api/hooks';

export function ProcessDefinitionsPage() {
  const { data: definitions, isLoading } = useProcessDefinitions();
  const startProcess = useStartProcess();

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">Process Definitions</h2>
          <p className="mt-1 text-sm text-gray-500">
            Deploy and manage your BPMN process definitions.
          </p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {definitions?.map((def) => (
          <div
            key={def.id}
            className="rounded-lg border border-gray-200 bg-white p-5 shadow-sm"
          >
            <h3 className="font-medium text-gray-900">{def.name || def.key}</h3>
            <p className="mt-1 text-sm text-gray-500">Key: {def.key}</p>
            <p className="text-sm text-gray-500">Version: {def.version}</p>
            <button
              onClick={() => startProcess.mutate({ processDefinitionKey: def.key })}
              disabled={startProcess.isPending}
              className="mt-3 rounded-md bg-blue-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {startProcess.isPending ? 'Starting...' : 'Start Instance'}
            </button>
          </div>
        ))}
      </div>

      {(!definitions || definitions.length === 0) && (
        <div className="py-12 text-center text-gray-500">
          No process definitions deployed yet.
        </div>
      )}
    </div>
  );
}

function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
    </div>
  );
}
