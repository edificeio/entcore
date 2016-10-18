import { appPrefix, infraPrefix } from './globals';

export declare interface IModel{
	api: { get?: string, put?: string, post?: string, delete?: string }
}

export class Model {
	constructor(data:any = undefined){
		if(typeof this.updateData === 'function'){
			this.updateData(data, false);
		}
    }
}

export declare interface Model{
	build();
	updateData(data: any, refreshView?: boolean);
	makeModel(ctr: any, mixin?: any, namespace?: any);
	makeModels(ctrList: any[] | any);
	sync();
	saveModifications();
	create();
	save();
	remove();
	models: Model[];
	collection(ctr: any, mixin?: any);
	on(eventName: string, callback: (data?: any) => void);
	unbind(eventName: string, callback: () => void);
	one(eventName: string, callback: (data?: any) => void);
	trigger(eventName: string, eventData?: any);
	behaviours(serviceName: string);
    inherits(target: any, prototypeFn: any);
    selected: boolean;
    myRights: Map<string, boolean>;
}

Model.prototype.build = function () { };

export class BaseModel extends Model{
	me: any;
    calendar: any;
    widgets: any;
	mediaLibrary: any;
	bootstrapped: boolean;
	
	build(){
		
	}
}

export var model = new BaseModel();

export class Collection<T>{
	all: T[];
	obj: any;
	callbacks: {};
	
	constructor(obj: any){
		this.all = [];
		this.obj = obj;
		this.callbacks = {};
		this.sync = function(){
			
		}
	}
}

export declare interface Collection<T>{
	sync: any;
	composer: any;
	model?: any;
	on(eventName: string, cb: (data?: any) => void);
	one(eventName: string, cb: (data?: any) => void);
	trigger: (eventName: string) => void;
	unbind(eventName: string, callback: () => void);
    forEach(callback: (item: T) => void);
    first(): T;
    select(predicate: (item: T) => boolean);
    deselect(predicate: (item: T) => boolean);
    selectAll();
    deselectAll();
	concat(col: Collection<T>);
	closeAll();
	current: T;
	setCurrent(item: T);
    map: (filter: (item: T) => T) => void;
    filter: (filter: (item: T) => boolean) => T[];
	reject: (filter: (item: T) => boolean) => T[];
    findWhere: (filter: any) => T;
    find: (filter: (item: T) => boolean) => T;
    where: (filter: any) => T[];
	slice(index: number, nbItems: number);
	push: (item: T, refreshView?: boolean) => void;
	remove(item: T, refreshView?: boolean);
	removeAt(index: number);
	insertAt(index: number, item: T);
	moveUp(item: T);
	moveDown(item: T);
	getIndex(item: T);
    indexOf?: (item: T) => number;
	splice(...args: any[]);
	selectItem(item: T);
	selection: () => T[];
	removeSelection: () => void;
	addRange: (data: T[], cb?: (item: T) => void, refreshView?: boolean) => void;
	load: (data: T[], cb?: (item: T) => void, refreshView?: boolean) => void;
	empty: () => void;
	length(): number;
	request(httpMethod: string, path: string, cb?: (result: any) => void);
	contains(item: T);
	last(): T;
	removeAll();
}

if (!window.entcore) {
    window.entcore = {};
}
window.entcore.model = model;
(window as any).model = model;

window.entcore.Model = Model;
(window as any).Model = Model;
window.entcore.Collection = Collection;
