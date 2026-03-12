match (g:FunctionalGroup) set g.filter="FunctionalGroup";

match (g:DisciplineGroup) where g.name <> g.filter set g.filter=g.name;

match (g:FuncGroup) where g.name <> g.filter set g.filter=g.name;

match (g:CommunityGroup) where g.type='read' set g.filter='CommunityRead';
match (g:CommunityGroup) where g.type='contrib' set g.filter='CommunityContrib';
match (g:CommunityGroup) where g.type='manager' set g.filter='CommunityManager';

match (g:CommunityMemberGroup) set g.filter='CommunityMember';
match (g:CommunityAdminGroup) set g.filter='CommunityAdmin';

match (g:ManualGroup) where not has(g.filter) set g.filter="Manual";
