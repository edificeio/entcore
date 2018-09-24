MATCH (g:FunctionGroup) WHERE g.name ENDS WITH '-AdminLocal' and (g.displayNameSearchField contains 'admin') set g.displayNameSearchField = g.displayNameSearchField + 'adminlocal';
