begin transaction

START n=node:node_auto_index(type='ETABEDUCNAT')
SET
n.name=n.ENTStructureNomCourant,
n.ENTStructureNomCourant=null,
n.type=null,
n.wpId=null,
n:School;

START n=node:node_auto_index(type='CLASSE')
SET
n.name=n.ENTGroupeNom,
n.ENTGroupeNom=null,
n.type=null,
n.wpId=null,
n:Class;

START n=node:node_auto_index(type='SUPERADMIN')
SET
n.login=n.ENTPersonLogin,
n.password=n.ENTPersonMotDePasse,
n.firstName=n.ENTPersonPrenom,
n.lastName=n.ENTPersonNom,
n.displayName=n.ENTPersonNomAffichage,
n.ENTPersonLogin=null,
n.ENTPersonMotDePasse=null,
n.ENTPersonNom=null,
n.ENTPersonPrenom=null,
n.ENTPersonNomAffichage=null,
n.type=null,
n:User:SuperAdmin;

START n=node:node_auto_index(type='ELEVE')
SET
n.login=n.ENTPersonLogin,
n.password=n.ENTPersonMotDePasse,
n.address=n.ENTPersonAdresse,
n.zipCode=n.ENTPersonCodePostal,
n.city=n.ENTPersonVille,
n.country=n.ENTPersonPays,
n.email=n.ENTPersonMail,
n.gender=n.ENTPersonSexe,
n.firstName=n.ENTPersonPrenom,
n.lastName=n.ENTPersonNom,
n.displayName=n.ENTPersonNomAffichage,
n.surname=n.ENTPersonNomPatro,
n.externalId=n.ENTPersonIdentifiant,
n.birthDate=n.ENTPersonDateNaissance,
n.schoolLevel=n.ENTEleveNiveau,
n.schoolSector=n.ENTEleveCycle,
n.classes=[n.ENTPersonClasses],
n.ENTPersonLogin=null,
n.ENTPersonMotDePasse=null,
n.ENTPersonAdresse=null,
n.ENTPersonCodePostal=null,
n.ENTPersonVille=null,
n.ENTPersonPays=null,
n.ENTPersonMail=null,
n.ENTPersonSexe=null,
n.ENTPersonNomPatro=null,
n.ENTPersonIdentifiant=null,
n.ENTPersonNom=null,
n.ENTPersonPrenom=null,
n.ENTPersonNomAffichage=null,
n.ENTPersonDateNaissance=null,
n.ENTEleveNiveau=null,
n.ENTEleveCycle=null,
n.ENTPersonClasses=null,
n.type=null,
n.wpId=null,
n:User:Student;

START n=node:node_auto_index(type='ENSEIGNANT')
SET
n.login=n.ENTPersonLogin,
n.password=n.ENTPersonMotDePasse,
n.address=n.ENTPersonAdresse,
n.zipCode=n.ENTPersonCodePostal,
n.city=n.ENTPersonVille,
n.country=n.ENTPersonPays,
n.email=n.ENTPersonMail,
n.firstName=n.ENTPersonPrenom,
n.lastName=n.ENTPersonNom,
n.displayName=n.ENTPersonNomAffichage,
n.surname=n.ENTPersonNomPatro,
n.externalId=n.ENTPersonIdentifiant,
n.classes=[n.ENTPersonClasses],
n.mobile=n.ENTPersRelEleveTelMobile,
n.homePhone=n.ENTPersonTelPerso,
n.title=n.ENTPersonCivilite,
n.principal=n.ENTEnsFonctionDir,
n.ENTPersonLogin=null,
n.ENTPersonMotDePasse=null,
n.ENTPersonAdresse=null,
n.ENTPersonCodePostal=null,
n.ENTPersonVille=null,
n.ENTPersonPays=null,
n.ENTPersonMail=null,
n.ENTPersonNomPatro=null,
n.ENTPersonIdentifiant=null,
n.ENTPersonNom=null,
n.ENTPersonPrenom=null,
n.ENTPersonNomAffichage=null,
n.ENTPersonClasses=null,
n.ENTPersRelEleveTelMobile=null,
n.ENTPersonTelPerso=null,
n.ENTPersonCivilite=null,
n.ENTEnsFonctionDir=null,
n.type=null,
n.wpId=null,
n:User:Teacher;

START n=node:node_auto_index(type='PERSRELELEVE')
SET
n.login=n.ENTPersonLogin,
n.password=n.ENTPersonMotDePasse,
n.address=n.ENTPersonAdresse,
n.zipCode=n.ENTPersonCodePostal,
n.city=n.ENTPersonVille,
n.country=n.ENTPersonPays,
n.email=n.ENTPersonMail,
n.firstName=n.ENTPersonPrenom,
n.lastName=n.ENTPersonNom,
n.displayName=n.ENTPersonNomAffichage,
n.surname=n.ENTPersonNomPatro,
n.externalId=n.ENTPersonIdentifiant,
n.classes=[n.ENTPersonClasses],
n.mobile=n.ENTPersRelEleveTelMobile,
n.homePhone=n.ENTPersonTelPerso,
n.title=n.ENTPersonCivilite,
n.workPhone=n.ENTPersRelElevTelPro,
n.ENTPersonLogin=null,
n.ENTPersonMotDePasse=null,
n.ENTPersonAdresse=null,
n.ENTPersonCodePostal=null,
n.ENTPersonVille=null,
n.ENTPersonPays=null,
n.ENTPersonMail=null,
n.ENTPersonNomPatro=null,
n.ENTPersonIdentifiant=null,
n.ENTPersonNom=null,
n.ENTPersonPrenom=null,
n.ENTPersonNomAffichage=null,
n.ENTPersonClasses=null,
n.ENTPersRelEleveTelMobile=null,
n.ENTPersonTelPerso=null,
n.ENTPersonCivilite=null,
n.ENTPersRelElevTelPro=null,
n.type=null,
n.wpId=null,
n:User:Relative;

START n=node:node_auto_index(type='GROUP_CLASSE_PERSRELELEVE')
SET
n.type=null,
n:ProfileGroup:ClassProfileGroup:ClassRelativeGroup;

START n=node:node_auto_index(type='GROUP_CLASSE_ELEVE')
SET
n.type=null,
n:ProfileGroup:ClassProfileGroup:ClassStudentGroup;

START n=node:node_auto_index(type='GROUP_CLASSE_ENSEIGNANT')
SET
n.type=null,
n:ProfileGroup:ClassProfileGroup:ClassTeacherGroup;

START n=node:node_auto_index(type='GROUP_ETABEDUCNAT_PERSRELELEVE')
SET
n.type=null,
n:ProfileGroup:SchoolProfileGroup:SchoolRelativeGroup;

START n=node:node_auto_index(type='GROUP_ETABEDUCNAT_ELEVE')
SET
n.type=null,
n:ProfileGroup:SchoolProfileGroup:SchoolStudentGroup;

START n=node:node_auto_index(type='GROUP_ETABEDUCNAT_ENSEIGNANT')
SET
n.type=null,
n:ProfileGroup:SchoolProfileGroup:SchoolTeacherGroup;

START n=node:node_auto_index(type='GROUP_ETABEDUCNAT_DIRECTEUR')
SET
n.type=null,
n:ProfileGroup:SchoolProfileGroup:SchoolPrincipalGroup;

START n=node:node_auto_index(type='APPLICATION')
SET
n.type=null,
n:Application;

START n=node:node_auto_index(type='SECURED_ACTION_AUTHENTICATED')
SET
n:Action:AuthenticatedAction;

START n=node:node_auto_index(type='SECURED_ACTION_WORKFLOW')
SET
n:Action:WorkflowAction;

START n=node:node_auto_index(type='SECURED_ACTION_RESOURCE')
SET
n:Action:ResourceAction;

START n=node:node_auto_index(type='ROLE')
SET
n.type=null,
n:Role;

START n=node:node_auto_index(type='USERBOOK')
SET
n.type=null,
n:UserBook;

START n=node:node_auto_index(type='HOBBIES')
SET
n.type=null,
n:Hobby;

commit

