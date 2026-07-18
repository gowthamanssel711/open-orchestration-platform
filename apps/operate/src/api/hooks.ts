import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from './client';

interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ProcessDefinition {
  id: string;
  key: string;
  name: string;
  version: number;
}

export interface ProcessInstance {
  processInstanceId: string;
  processDefinitionKey: string;
  status: string;
  initiator: string;
  startedAt: string;
  completedAt: string | null;
}

export interface TaskItem {
  taskId: string;
  name: string;
  assignee: string;
  status: string;
  processInstanceId: string;
  createdAt: string;
}

export function useProcessDefinitions() {
  return useQuery({
    queryKey: ['process-definitions'],
    queryFn: () => api.get<ProcessDefinition[]>('/api/v1/processes'),
  });
}

export function useProcessInstances(page = 0, size = 20, status?: string) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) params.set('status', status);

  return useQuery({
    queryKey: ['process-instances', page, size, status],
    queryFn: () => api.get<PageResponse<ProcessInstance>>(`/api/v1/query/process-instances?${params}`),
    refetchInterval: 5000,
  });
}

export function useProcessInstance(id: string) {
  return useQuery({
    queryKey: ['process-instance', id],
    queryFn: () => api.get<ProcessInstance>(`/api/v1/query/process-instances/${id}`),
    enabled: !!id,
    refetchInterval: 3000,
  });
}

export function useTasks(page = 0, size = 20) {
  return useQuery({
    queryKey: ['tasks', page, size],
    queryFn: () => api.get<PageResponse<TaskItem>>(`/api/v1/query/tasks?page=${page}&size=${size}`),
    refetchInterval: 5000,
  });
}

export function useStartProcess() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { processDefinitionKey: string; variables?: Record<string, unknown> }) =>
      api.post('/api/v1/process-instances', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['process-instances'] });
    },
  });
}

export function useCancelProcess() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (processInstanceId: string) =>
      api.post(`/api/v1/process-instances/${processInstanceId}/cancel`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['process-instances'] });
    },
  });
}

export function useCompleteTask() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, variables }: { taskId: string; variables?: Record<string, unknown> }) =>
      api.post(`/api/v1/tasks/${taskId}/complete`, variables),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      queryClient.invalidateQueries({ queryKey: ['process-instances'] });
    },
  });
}
