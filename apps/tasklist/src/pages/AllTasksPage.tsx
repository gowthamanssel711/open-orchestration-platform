import { useEffect, useState } from 'react';

interface Task {
  taskId: string;
  name: string;
  assignee: string;
  status: string;
  processInstanceId: string;
  createdAt: string;
}

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export function AllTasksPage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [claimingId, setClaimingId] = useState<string | null>(null);

  useEffect(() => {
    fetchTasks();
  }, []);

  async function fetchTasks() {
    setIsLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/query/tasks?size=50`, {
        headers: { 'Content-Type': 'application/json' },
      });
      const data = await res.json();
      setTasks(data.content || []);
    } finally {
      setIsLoading(false);
    }
  }

  async function claimTask(taskId: string) {
    setClaimingId(taskId);
    try {
      await fetch(`${API_BASE}/api/v1/tasks/${taskId}/claim`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      });
      fetchTasks();
    } finally {
      setClaimingId(null);
    }
  }

  const STATUS_COLORS: Record<string, string> = {
    CREATED: 'bg-gray-100 text-gray-800',
    ASSIGNED: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
  };

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">All Tasks</h2>
          <p className="mt-1 text-sm text-gray-500">View and claim available tasks.</p>
        </div>
        <button
          onClick={fetchTasks}
          className="rounded-md bg-gray-100 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-200"
        >
          Refresh
        </button>
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
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Task</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Assignee</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Created</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {tasks.map((task) => (
                <tr key={task.taskId} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{task.name}</td>
                  <td className="px-4 py-3 text-sm text-gray-600">{task.assignee || '—'}</td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[task.status] || 'bg-gray-100 text-gray-800'}`}>
                      {task.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(task.createdAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-3">
                    {task.status === 'CREATED' && (
                      <button
                        onClick={() => claimTask(task.taskId)}
                        disabled={claimingId === task.taskId}
                        className="text-sm text-blue-600 hover:text-blue-800 disabled:opacity-50"
                      >
                        {claimingId === task.taskId ? 'Claiming...' : 'Claim'}
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
