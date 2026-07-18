import { useState } from 'react';

interface Task {
  taskId: string;
  name: string;
  assignee: string;
  status: string;
  processInstanceId: string;
  createdAt: string;
}

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export function MyTasksPage() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [completingId, setCompletingId] = useState<string | null>(null);

  async function fetchMyTasks() {
    setIsLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/query/tasks/my`, {
        headers: { 'Content-Type': 'application/json' },
      });
      const data = await res.json();
      setTasks(data.content || []);
    } finally {
      setIsLoading(false);
    }
  }

  async function completeTask(taskId: string) {
    setCompletingId(taskId);
    try {
      await fetch(`${API_BASE}/api/v1/tasks/${taskId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      });
      setTasks((prev) => prev.filter((t) => t.taskId !== taskId));
    } finally {
      setCompletingId(null);
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
                <h3 className="font-medium text-gray-900">{task.name}</h3>
                <p className="mt-0.5 text-sm text-gray-500">
                  Process: {task.processInstanceId.substring(0, 8)}...
                </p>
                <p className="text-xs text-gray-400">
                  Created: {new Date(task.createdAt).toLocaleString()}
                </p>
              </div>
              <button
                onClick={() => completeTask(task.taskId)}
                disabled={completingId === task.taskId}
                className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
              >
                {completingId === task.taskId ? 'Completing...' : 'Complete'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
