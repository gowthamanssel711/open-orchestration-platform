import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '@oop/auth';
import { useApiClient } from '../api';
import { FormRenderer } from '../components/FormRenderer';

interface TaskForm {
  hasForm: boolean;
  taskId: string;
  formKey?: string;
  schema?: string;
  data?: Record<string, unknown>;
  message?: string;
}

interface TaskDetail {
  id: string;
  taskId?: string;
  name: string;
  assignee: string | null;
  status: string;
  processInstanceId: string;
  taskDefinitionKey: string;
  formKey: string | null;
  createTime?: string;
  createdAt?: string;
  priority: number;
}

export function TaskDetailPage() {
  const { taskId } = useParams<{ taskId: string }>();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { apiFetch } = useApiClient();

  const [task, setTask] = useState<TaskDetail | null>(null);
  const [form, setForm] = useState<TaskForm | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionId, setActionId] = useState<string | null>(null);

  useEffect(() => {
    if (taskId) loadTask();
  }, [taskId]);

  async function loadTask() {
    setLoading(true);
    setError(null);
    try {
      const taskData = await apiFetch(`/api/v1/tasks/${taskId}`);
      setTask(taskData);

      if (taskData.status === 'ACTIVE' || taskData.status === 'ASSIGNED' || taskData.status === 'CREATED') {
        try {
          const formData = await apiFetch(`/api/v1/tasks/${taskId}/form`);
          setForm(formData);
        } catch {
          setForm({ hasForm: false, taskId: taskId! });
        }
      }
    } catch (e: any) {
      setError(e.message || 'Failed to load task');
    } finally {
      setLoading(false);
    }
  }

  async function handleFormSubmit(formData: Record<string, unknown>) {
    setSubmitting(true);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/complete`, {
        method: 'POST',
        body: JSON.stringify(formData),
      });
      navigate(-1);
    } catch (e: any) {
      setError(e.message || 'Failed to complete task');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleCompleteWithoutForm() {
    setSubmitting(true);
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/complete`, {
        method: 'POST',
        body: JSON.stringify({}),
      });
      navigate(-1);
    } catch (e: any) {
      setError(e.message || 'Failed to complete task');
    } finally {
      setSubmitting(false);
    }
  }

  async function claimTask() {
    setActionId('claim');
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/claim?assignee=${user?.username}`, {
        method: 'POST',
      });
      await loadTask();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setActionId(null);
    }
  }

  async function releaseTask() {
    setActionId('release');
    try {
      await apiFetch(`/api/v1/tasks/${taskId}/release`, { method: 'POST' });
      await loadTask();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setActionId(null);
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-gray-200 border-t-blue-600" />
      </div>
    );
  }

  if (error && !task) {
    return (
      <div className="rounded-lg border border-red-200 bg-red-50 p-6">
        <p className="text-red-700">{error}</p>
        <button
          onClick={() => navigate(-1)}
          className="mt-4 text-sm text-blue-600 hover:text-blue-800"
        >
          Go back
        </button>
      </div>
    );
  }

  if (!task) return null;

  const isAssignedToMe = task.assignee === user?.username;
  const isActive = task.status === 'ACTIVE' || task.status === 'ASSIGNED' || task.status === 'CREATED';

  return (
    <div className="mx-auto max-w-4xl">
      <button
        onClick={() => navigate(-1)}
        className="mb-4 text-sm text-gray-500 hover:text-gray-700"
      >
        &larr; Back to tasks
      </button>

      <div className="rounded-lg border border-gray-200 bg-white">
        <div className="border-b border-gray-200 px-6 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900">
                {task.name || task.taskDefinitionKey || taskId}
              </h2>
              <p className="mt-1 text-sm text-gray-500">
                Task ID: <span className="font-mono">{taskId}</span>
              </p>
            </div>
            <span
              className={`inline-flex rounded-full px-3 py-1 text-xs font-medium ${
                task.status === 'COMPLETED'
                  ? 'bg-green-100 text-green-800'
                  : task.status === 'ASSIGNED'
                  ? 'bg-blue-100 text-blue-800'
                  : 'bg-yellow-100 text-yellow-800'
              }`}
            >
              {task.status}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4 border-b border-gray-200 px-6 py-4 text-sm">
          <div>
            <span className="font-medium text-gray-500">Process Instance:</span>
            <span className="ml-2 font-mono text-gray-900">{task.processInstanceId}</span>
          </div>
          <div>
            <span className="font-medium text-gray-500">Assignee:</span>
            <span className="ml-2 text-gray-900">{task.assignee || 'Unassigned'}</span>
          </div>
          <div>
            <span className="font-medium text-gray-500">Task Definition:</span>
            <span className="ml-2 font-mono text-gray-900">{task.taskDefinitionKey}</span>
          </div>
          <div>
            <span className="font-medium text-gray-500">Created:</span>
            <span className="ml-2 text-gray-900">
              {(task.createTime || task.createdAt)
                ? new Date(task.createTime || task.createdAt!).toLocaleString()
                : '—'}
            </span>
          </div>
          {task.formKey && (
            <div>
              <span className="font-medium text-gray-500">Form:</span>
              <span className="ml-2 font-mono text-gray-900">{task.formKey}</span>
            </div>
          )}
        </div>

        {error && (
          <div className="border-b border-red-200 bg-red-50 px-6 py-3">
            <p className="text-sm text-red-700">{error}</p>
          </div>
        )}

        {isActive && (
          <div className="px-6 py-4">
            {!isAssignedToMe && !task.assignee && (
              <div className="mb-4 rounded-md bg-yellow-50 p-4">
                <p className="text-sm text-yellow-800">
                  This task is not assigned. Claim it to work on it.
                </p>
                <button
                  onClick={claimTask}
                  disabled={actionId === 'claim'}
                  className="mt-2 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
                >
                  {actionId === 'claim' ? 'Claiming...' : 'Claim Task'}
                </button>
              </div>
            )}

            {isAssignedToMe && (
              <>
                {form?.hasForm && form.schema ? (
                  <div className="rounded-md border border-gray-200">
                    <FormRenderer
                      schema={form.schema}
                      data={form.data || {}}
                      onSubmit={handleFormSubmit}
                      onCancel={() => navigate(-1)}
                      submitting={submitting}
                    />
                  </div>
                ) : (
                  <div className="space-y-4">
                    {!form?.hasForm && (
                      <p className="text-sm text-gray-500">
                        No form is configured for this task. You can complete it directly.
                      </p>
                    )}
                    <div className="flex gap-3">
                      <button
                        onClick={handleCompleteWithoutForm}
                        disabled={submitting}
                        className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
                      >
                        {submitting ? 'Completing...' : 'Complete Task'}
                      </button>
                      <button
                        onClick={releaseTask}
                        disabled={actionId === 'release'}
                        className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                      >
                        {actionId === 'release' ? 'Releasing...' : 'Release Task'}
                      </button>
                    </div>
                  </div>
                )}

                {form?.hasForm && form.schema && (
                  <div className="mt-3 flex justify-end">
                    <button
                      onClick={releaseTask}
                      disabled={actionId === 'release'}
                      className="text-sm text-gray-500 hover:text-gray-700"
                    >
                      Release Task
                    </button>
                  </div>
                )}
              </>
            )}

            {task.assignee && !isAssignedToMe && (
              <p className="text-sm text-gray-500">
                This task is assigned to <strong>{task.assignee}</strong>.
              </p>
            )}
          </div>
        )}

        {task.status === 'COMPLETED' && (
          <div className="px-6 py-4">
            <p className="text-sm text-green-700">This task has been completed.</p>
          </div>
        )}
      </div>
    </div>
  );
}
