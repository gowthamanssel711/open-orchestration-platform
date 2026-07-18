import { useEffect, useState } from 'react';
import { useApiClient } from '../api';

interface ProcessInstance {
  id?: number;
  processInstanceId: string;
  processDefinitionKey: string | null;
  businessKey: string | null;
  initiator: string | null;
  status: string;
  startedAt: string | null;
  completedAt: string | null;
}

type StatusFilter = 'ALL' | 'RUNNING' | 'COMPLETED' | 'CANCELLED';

export function ProcessInstancesPage() {
  const { apiFetch } = useApiClient();
  const [instances, setInstances] = useState<ProcessInstance[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionId, setActionId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');

  useEffect(() => {
    fetchInstances();
  }, [statusFilter]);

  async function fetchInstances() {
    setIsLoading(true);
    try {
      const params = statusFilter !== 'ALL' ? `?status=${statusFilter}` : '';
      const data = await apiFetch(`/api/v1/process-instances${params}`);
      setInstances(data.content ?? data);
    } catch (e) {
      console.error('Failed to fetch instances:', e);
    } finally {
      setIsLoading(false);
    }
  }

  async function cancelInstance(processInstanceId: string) {
    if (!confirm(`Cancel process instance ${processInstanceId}?`)) return;
    setActionId(processInstanceId);
    try {
      await apiFetch(`/api/v1/process-instances/${processInstanceId}/cancel`, {
        method: 'POST',
      });
      setInstances((prev) =>
        prev.map((pi) =>
          pi.processInstanceId === processInstanceId
            ? { ...pi, status: 'CANCELLED' }
            : pi
        )
      );
    } catch (e) {
      console.error('Failed to cancel instance:', e);
    } finally {
      setActionId(null);
    }
  }

  const STATUS_COLORS: Record<string, string> = {
    RUNNING: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
  };

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">Process Instances</h2>
          <p className="mt-1 text-sm text-gray-500">View and manage running processes.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex rounded-md border border-gray-200 bg-white">
            {(['ALL', 'RUNNING', 'COMPLETED', 'CANCELLED'] as StatusFilter[]).map((s, i, arr) => (
              <button
                key={s}
                onClick={() => setStatusFilter(s)}
                className={`px-3 py-1.5 text-xs font-medium transition-colors ${
                  statusFilter === s
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-600 hover:bg-gray-50'
                } ${i === 0 ? 'rounded-l-md' : ''} ${i === arr.length - 1 ? 'rounded-r-md' : ''}`}
              >
                {s}
              </button>
            ))}
          </div>
          <button
            onClick={fetchInstances}
            className="rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
          >
            Refresh
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
        </div>
      ) : (
        <div className="overflow-hidden rounded-lg border border-gray-200">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Instance ID</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Definition</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Initiator</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Started</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {instances.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-gray-500">
                    No process instances found.
                  </td>
                </tr>
              ) : (
                instances.map((pi) => (
                  <tr key={pi.processInstanceId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-mono text-gray-900">
                      {pi.processInstanceId.substring(0, 8)}...
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-700">
                      {pi.processDefinitionKey || '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[pi.status] || 'bg-gray-100 text-gray-800'}`}>
                        {pi.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {pi.initiator || '—'}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {pi.startedAt ? new Date(pi.startedAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      {pi.status === 'RUNNING' && (
                        <button
                          onClick={() => cancelInstance(pi.processInstanceId)}
                          disabled={actionId === pi.processInstanceId}
                          className="rounded bg-red-50 px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
                        >
                          {actionId === pi.processInstanceId ? '...' : 'Cancel'}
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
