export type Reason =
  | 'VIOLENT_SCORNFUL'
  | 'HATRED'
  | 'HARASSMENT'
  | 'NUDITY'
  | 'FAKE_NEWS'
  | 'OTHER';

export type Status =
  'OPEN' | 'PENDING' | 'QUARANTINE' | 'RESOURCE_DELETED' | 'RESOURCE_KEPT';

export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonObject | JsonValue[];
export type JsonObject = { [key: string]: JsonValue };
