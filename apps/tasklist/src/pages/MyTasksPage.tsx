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
  createdAt: string;
}

export function MyTasksPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { apiFetch } = useApiClient();
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [actionId, setActionId] = useState<string | null>(null);

  useEffect(() => {
    if (user?.username) fetchMyTasks();
  }, [user?.username]);

  async function fetchMyTasks() {
    setIsLoading(true);
    try {
      const data = await apiFetch(`/api/v1/tasks/my`);
      setTasks(data.content ?? data);
    } catch (e) {
      console.error('Failed to fetch tasks:', e);
    } finally {
      setIsLoading(false);
    }
  }

  async function completeTask(taskId: string) {
    setActionId(taskId);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/complete`, {
        method: 'POST',
        body: JSON.stringify({}),
      });
      setTasks((prev) => prev.filter((t) => t.taskId !== taskId));
    } catch (e) {
      console.error('Failed to complete task:', e);
    } finally {
      setActionId(null);
    }
  }

  async function releaseTask(taskId: string) {
    setActionId(taskId);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/release`, { method: 'POST' });
      setTasks((prev) => prev.filter((t) => t.taskId !== taskId));
    } catch (e) {
      console.error('Failed to release task:', e);
    } finally {
      setActionId(null);
    }
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold text-gray-900">My Tasks</h2>
          <p className="mt-1 text-sm text-gray-500">Tasks assigned to you.</p>
        </div>
        <button
          onClick={fetchMyTasks}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          Refresh
        </button>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
        </div>
      ) : tasks.length === 0 ? (
        <div className="rounded-lg border border-gray-200 bg-white p-12 text-center">
          <p className="text-gray-500">No tasks assigned to you.</p>
          <button
            onClick={fetchMyTasks}
            className="mt-4 text-sm text-blue-600 hover:text-blue-800"
          >
            Check for new tasks
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {tasks.map((task) => (
            <div
              key={task.taskId}
              className="flex items-center justify-between rounded-lg border border-gray-200 bg-white px-5 py-4"
            >
              <div>
                <button
                  onClick={() => navigate(`/task/${task.taskId}`)}
                  className="font-medium text-blue-600 hover:text-blue-800 hover:underline"
                >
                  {task.name || task.taskId}
                </button>
                <p className="mt-0.5 text-sm text-gray-500">
                  Process: <span className="font-mono">{task.processInstanceId?.substring(0, 8)}...</span>
                </p>
                <p className="text-xs text-gray-400">
                  Created: {task.createdAt ? new Date(task.createdAt).toLocaleString() : '—'}
                </p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => completeTask(task.taskId)}
                  disabled={actionId === task.taskId}
                  className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                >
                  {actionId === task.taskId ? '...' : 'Complete'}
                </button>
                <button
                  onClick={() => releaseTask(task.taskId)}
                  disabled={actionId === task.taskId}
                  className="rounded-md bg-gray-200 px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-300 disabled:opacity-50"
                >
                  Release
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
