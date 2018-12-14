import { Type } from '@angular/core';

/** 
* Usefull to desccribe (type and input data) a component 
* that will be dynamically instanciate
*/
export class ComponentDescriptor {
   constructor(
       public type: Type<any>, 
       public data: any
   ){}

   assignData(data:any) : ComponentDescriptor {
       Object.assign(this.data, data);
       return this;
   }
}
