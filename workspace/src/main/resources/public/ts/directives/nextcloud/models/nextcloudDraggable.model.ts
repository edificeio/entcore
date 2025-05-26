export interface Draggable {
  dragStartHandler(event: DragEvent, content?: any): void;
  dragDropHandler(event: DragEvent, content?: any): void;
  dragConditionHandler(event: DragEvent, content?: any): boolean;
  dropConditionHandler(event: DragEvent, content?: any): boolean;
  dragEndHandler(event: DragEvent, content?: any): void;
}
