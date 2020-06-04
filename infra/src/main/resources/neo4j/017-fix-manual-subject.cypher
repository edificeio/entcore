match (sub:Subject) where not(has(sub.source)) or sub.source = 'CSV' set sub.source = 'MANUAL';
