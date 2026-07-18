import { useEffect, useRef, useState } from 'react';
import { Form } from '@bpmn-io/form-js-viewer';

interface FormRendererProps {
  schema: string;
  data: Record<string, unknown>;
  onSubmit: (data: Record<string, unknown>) => void;
  onCancel: () => void;
  submitting?: boolean;
}

export function FormRenderer({ schema, data, onSubmit, onCancel, submitting }: FormRendererProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const formRef = useRef<Form | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const form = new Form({ container: containerRef.current });
    formRef.current = form;

    const parsed = typeof schema === 'string' ? JSON.parse(schema) : schema;

    form.importSchema(parsed, data).catch((err: Error) => {
      setError(`Failed to load form: ${err.message}`);
    });

    return () => {
      form.destroy();
      formRef.current = null;
    };
  }, [schema, data]);

  async function handleSubmit() {
    if (!formRef.current) return;
    const { errors, data: formData } = formRef.current.submit();
    if (errors && Object.keys(errors).length > 0) {
      return;
    }
    onSubmit(formData);
  }

  if (error) {
    return (
      <div className="rounded-md bg-red-50 p-4">
        <p className="text-sm text-red-700">{error}</p>
        <button
          onClick={onCancel}
          className="mt-2 text-sm text-red-600 hover:text-red-800"
        >
          Close
        </button>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div ref={containerRef} className="flex-1 overflow-auto p-4 fjs-container" />
      <div className="flex justify-end gap-3 border-t border-gray-200 px-4 py-3 bg-gray-50">
        <button
          onClick={onCancel}
          disabled={submitting}
          className="rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
        >
          Cancel
        </button>
        <button
          onClick={handleSubmit}
          disabled={submitting}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {submitting ? 'Submitting...' : 'Complete Task'}
        </button>
      </div>
    </div>
  );
}
