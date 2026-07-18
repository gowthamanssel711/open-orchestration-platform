import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@oop/auth';
import { useApiClient } from '../api';

interface Task {
  id?: number;
  taskId: string;
  name: string;
  assignee: string | null;
  status: string;
  processInstanceId: string;
  processDefinitionKey: string | null;
  candidateGroups: string | null;
  formKey: string | null;
  priority: number | null;
  dueDate: string | null;
  createdAt: string;
  completedAt: string | null;
}

type StatusFilter = 'ALL' | 'CREATED' | 'ASSIGNED' | 'COMPLETED';

export function AllTasksPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { apiFetch } = useApiClient();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [actionId, setActionId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');

  useEffect(() => {
    fetchTasks();
  }, [statusFilter]);

  async function fetchTasks() {
    setIsLoading(true);
    try {
      const params = statusFilter !== 'ALL' ? `?status=${statusFilter}` : '';
      const data = await apiFetch(`/api/v1/tasks${params}`);
      setTasks(data.content ?? data);
    } catch (e) {
      console.error('Failed to fetch tasks:', e);
    } finally {
      setIsLoading(false);
    }
  }

  async function claimTask(taskId: string) {
    setActionId(taskId);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/claim?assignee=${user?.username}`, {
        method: 'POST',
      });
      fetchTasks();
    } catch (e) {
      console.error('Failed to claim task:', e);
    } finally {
      setActionId(null);
    }
  }

  async function releaseTask(taskId: string) {
    setActionId(taskId);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/release`, { method: 'POST' });
      fetchTasks();
    } catch (e) {
      console.error('Failed to release task:', e);
    } finally {
      setActionId(null);
    }
  }

  async function completeTask(taskId: string) {
    setActionId(taskId);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/complete`, {
        method: 'POST',
        body: JSON.stringify({}),
      });
      fetchTasks();
    } catch (e) {
      console.error('Failed to complete task:', e);
    } finally {
      setActionId(null);
    }
  }

  const STATUS_COLORS: Record<string, string> = {
    CREATED: 'bg-yellow-100 text-yellow-800',
    ASSIGNED: 'bg-blue-100 text-blue-800',
    COMPLETED: 'bg-green-100 text-green-800',
  };

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">All Tasks</h2>
          <p className="mt-1 text-sm text-gray-500">View, claim, and manage tasks.</p>
        </div>
        <div className="flex items-center gap-3">
          <div className="flex rounded-md border border-gray-200 bg-white">
            {(['ALL', 'CREATED', 'ASSIGNED', 'COMPLETED'] as StatusFilter[]).map((s, i, arr) => (
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
            onClick={fetchTasks}
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
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Task</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Assignee</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Process</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Created</th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase text-gray-500">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 bg-white">
              {tasks.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-sm text-gray-500">
                    No tasks found.
                  </td>
                </tr>
              ) : (
                tasks.map((task) => (
                  <tr key={task.taskId} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-medium">
                      <button
                        onClick={() => navigate(`/task/${task.taskId}`)}
                        className="text-blue-600 hover:text-blue-800 hover:underline"
                      >
                        {task.name || task.taskId}
                      </button>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_COLORS[task.status] || 'bg-gray-100 text-gray-800'}`}>
                        {task.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{task.assignee || '—'}</td>
                    <td className="px-4 py-3 text-sm text-gray-500 font-mono">
                      {task.processInstanceId?.substring(0, 8)}...
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">
                      {task.createdAt ? new Date(task.createdAt).toLocaleString() : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        {task.status === 'CREATED' && !task.assignee && (
                          <button
                            onClick={() => claimTask(task.taskId)}
                            disabled={actionId === task.taskId}
                            className="rounded bg-blue-50 px-2 py-1 text-xs font-medium text-blue-700 hover:bg-blue-100 disabled:opacity-50"
                          >
                            Claim
                          </button>
                        )}
                        {task.status === 'ASSIGNED' && task.assignee === user?.username && (
                          <>
                            <button
                              onClick={() => completeTask(task.taskId)}
                              disabled={actionId === task.taskId}
                              className="rounded bg-green-50 px-2 py-1 text-xs font-medium text-green-700 hover:bg-green-100 disabled:opacity-50"
                            >
                              Complete
                            </button>
                            <button
                              onClick={() => releaseTask(task.taskId)}
                              disabled={actionId === task.taskId}
                              className="rounded bg-gray-50 px-2 py-1 text-xs font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50"
                            >
                              Release
                            </button>
                          </>
                        )}
                      </div>
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
