import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { AuthProvider, AuthGuard, createKeycloakInstance, useAuth } from '@oop/auth';
import { MyTasksPage } from './pages/MyTasksPage';
import { AllTasksPage } from './pages/AllTasksPage';
import { TaskDetailPage } from './pages/TaskDetailPage';
import { ProcessInstancesPage } from './pages/ProcessInstancesPage';

const keycloak = createKeycloakInstance({ clientId: 'tasklist-ui' });

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 3000,
      retry: 1,
    },
  },
});

function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const { user, logout } = useAuth();

  const navItems = [
    { path: '/', label: 'My Tasks' },
    { path: '/all', label: 'All Tasks' },
    { path: '/instances', label: 'Instances' },
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      <aside className="flex w-64 flex-col border-r border-gray-200 bg-white">
        <div className="border-b border-gray-100 px-4 py-4">
          <h1 className="text-lg font-bold text-gray-900">OOP Tasklist</h1>
          <p className="text-xs text-gray-500">Task Management</p>
        </div>
        <nav className="flex-1 space-y-1 px-3 py-4">
          {navItems.map((item) => (
            <button
              key={item.path}
              onClick={() => navigate(item.path)}
              className={`w-full rounded-md px-3 py-2 text-left text-sm font-medium transition-colors ${
                location.pathname === item.path
                  ? 'bg-blue-50 text-blue-700'
                  : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>
        <div className="border-t border-gray-100 px-4 py-3">
          <p className="text-sm font-medium text-gray-700">{user?.username}</p>
          <p className="text-xs text-gray-400">{user?.email}</p>
          <button
            onClick={logout}
            className="mt-2 text-xs text-red-600 hover:text-red-800"
          >
            Logout
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-6">
        <Routes>
          <Route path="/" element={<MyTasksPage />} />
          <Route path="/all" element={<AllTasksPage />} />
          <Route path="/instances" element={<ProcessInstancesPage />} />
          <Route path="/task/:taskId" element={<TaskDetailPage />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider keycloak={keycloak}>
      <AuthGuard>
        <QueryClientProvider client={queryClient}>
          <BrowserRouter>
            <AppLayout />
          </BrowserRouter>
        </QueryClientProvider>
      </AuthGuard>
    </AuthProvider>
  );
}
