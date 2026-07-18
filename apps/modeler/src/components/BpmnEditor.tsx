import { useEffect, useRef, useCallback } from 'react';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule,
} from 'bpmn-js-properties-panel';
import CamundaBpmnModdle from 'camunda-bpmn-moddle/resources/camunda.json';

export function toActivitiXml(xml: string): string {
  return xml
    .replace(/xmlns:camunda="http:\/\/camunda\.org\/schema\/1\.0\/bpmn"/g, 'xmlns:activiti="http://activiti.org/bpmn"')
    .replace(/camunda:/g, 'activiti:');
}

const EMPTY_BPMN = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:activiti="http://activiti.org/bpmn"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn:process id="Process_1" name="New Process" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_1</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="EndEvent_1" />
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_1" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="400" y="160" width="36" height="36" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <dc:Bounds x="216" y="178" />
        <dc:Bounds x="400" y="178" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

interface BpmnEditorProps {
  xml?: string;
  onXmlChange?: (xml: string) => void;
  onModelerReady?: (modeler: BpmnModeler) => void;
}

export function BpmnEditor({ xml, onXmlChange, onModelerReady }: BpmnEditorProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const propertiesPanelRef = useRef<HTMLDivElement>(null);
  const modelerRef = useRef<BpmnModeler | null>(null);
  const initializedRef = useRef(false);

  const getXml = useCallback(async (): Promise<string> => {
    if (!modelerRef.current) return '';
    const { xml } = await modelerRef.current.saveXML({ format: true });
    return toActivitiXml(xml || '');
  }, []);

  useEffect(() => {
    if (!containerRef.current || !propertiesPanelRef.current || initializedRef.current) return;
    initializedRef.current = true;

    const modeler = new BpmnModeler({
      container: containerRef.current,
      propertiesPanel: {
        parent: propertiesPanelRef.current,
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule,
        CamundaPlatformPropertiesProviderModule,
      ],
      moddleExtensions: {
        camunda: CamundaBpmnModdle,
      },
      keyboard: { bindTo: document },
    } as any);

    modelerRef.current = modeler;

    const content = xml || EMPTY_BPMN;
    modeler.importXML(content).then(() => {
      const canvas = modeler.get('canvas') as any;
      canvas.zoom('fit-viewport');
    });

    modeler.on('commandStack.changed', async () => {
      if (onXmlChange) {
        const currentXml = await getXml();
        onXmlChange(currentXml);
      }
    });

    if (onModelerReady) {
      onModelerReady(modeler);
    }

    return () => {
      modeler.destroy();
      modelerRef.current = null;
      initializedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (xml && modelerRef.current && initializedRef.current) {
      modelerRef.current.importXML(xml).then(() => {
        const canvas = modelerRef.current!.get('canvas') as any;
        canvas.zoom('fit-viewport');
      });
    }
  }, [xml]);

  return (
    <div className="flex h-full">
      <div ref={containerRef} className="flex-1 h-full" />
      <div
        ref={propertiesPanelRef}
        className="w-[320px] h-full overflow-y-auto border-l border-gray-200 bg-white"
      />
    </div>
  );
}

export { EMPTY_BPMN };
