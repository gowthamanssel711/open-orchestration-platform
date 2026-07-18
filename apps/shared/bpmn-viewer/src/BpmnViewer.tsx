import { useEffect, useRef } from 'react';
import BpmnJS from 'bpmn-js/lib/NavigatedViewer';

export interface BpmnViewerProps {
  xml: string;
  activeElementIds?: string[];
  incidentElementIds?: string[];
  completedElementIds?: string[];
  onElementClick?: (elementId: string) => void;
  className?: string;
}

export function BpmnViewer({
  xml,
  activeElementIds = [],
  incidentElementIds = [],
  completedElementIds = [],
  onElementClick,
  className,
}: BpmnViewerProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<InstanceType<typeof BpmnJS> | null>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    const viewer = new BpmnJS({ container: containerRef.current });
    viewerRef.current = viewer;

    if (onElementClick) {
      const eventBus = viewer.get('eventBus') as { on: (event: string, cb: (e: { element: { id: string } }) => void) => void };
      eventBus.on('element.click', (event) => {
        onElementClick(event.element.id);
      });
    }

    return () => {
      viewer.destroy();
    };
  }, []);

  useEffect(() => {
    if (!viewerRef.current || !xml) return;

    viewerRef.current.importXML(xml).then(() => {
      const canvas = viewerRef.current!.get('canvas') as { zoom: (type: string) => void };
      canvas.zoom('fit-viewport');
      applyOverlays();
    });
  }, [xml]);

  useEffect(() => {
    applyOverlays();
  }, [activeElementIds, incidentElementIds, completedElementIds]);

  function applyOverlays() {
    if (!viewerRef.current) return;

    const canvas = viewerRef.current.get('canvas') as {
      addMarker: (id: string, cls: string) => void;
      removeMarker: (id: string, cls: string) => void;
    };

    activeElementIds.forEach((id) => canvas.addMarker(id, 'bpmn-active'));
    incidentElementIds.forEach((id) => canvas.addMarker(id, 'bpmn-incident'));
    completedElementIds.forEach((id) => canvas.addMarker(id, 'bpmn-completed'));
  }

  return (
    <div ref={containerRef} className={className} style={{ minHeight: '400px' }}>
      <style>{`
        .bpmn-active :not(defs) > .djs-visual > :nth-child(1) {
          stroke: #2563eb !important;
          fill: #dbeafe !important;
        }
        .bpmn-incident :not(defs) > .djs-visual > :nth-child(1) {
          stroke: #dc2626 !important;
          fill: #fee2e2 !important;
        }
        .bpmn-completed :not(defs) > .djs-visual > :nth-child(1) {
          stroke: #16a34a !important;
          fill: #dcfce7 !important;
        }
      `}</style>
    </div>
  );
}
