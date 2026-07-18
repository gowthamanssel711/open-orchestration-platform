declare module 'bpmn-js/lib/Modeler' {
  export default class BpmnModeler {
    constructor(options?: any);
    importXML(xml: string): Promise<{ warnings: any[] }>;
    saveXML(options?: { format?: boolean }): Promise<{ xml?: string }>;
    saveSVG(): Promise<{ svg?: string }>;
    get(name: string): any;
    on(event: string, callback: (...args: any[]) => void): void;
    destroy(): void;
  }
}

declare module 'bpmn-js-properties-panel' {
  export const BpmnPropertiesPanelModule: any;
  export const BpmnPropertiesProviderModule: any;
  export const CamundaPlatformPropertiesProviderModule: any;
  export const ZeebePropertiesProviderModule: any;
}

declare module 'camunda-bpmn-moddle/resources/camunda.json' {
  const content: any;
  export default content;
}
