begin transaction
CREATE INDEX ON :Action(type);
CREATE CONSTRAINT ON (action:Action) ASSERT action.name IS UNIQUE;
CREATE CONSTRAINT ON (role:Role) ASSERT role.id IS UNIQUE;
CREATE CONSTRAINT ON (role:Role) ASSERT role.name IS UNIQUE;
CREATE CONSTRAINT ON (application:Application) ASSERT application.id IS UNIQUE;
CREATE CONSTRAINT ON (application:Application) ASSERT application.name IS UNIQUE;
CREATE CONSTRAINT ON (school:School) ASSERT school.id IS UNIQUE;
CREATE CONSTRAINT ON (class:Class) ASSERT class.id IS UNIQUE;
CREATE CONSTRAINT ON (user:User) ASSERT user.id IS UNIQUE;
CREATE CONSTRAINT ON (user:User) ASSERT user.login IS UNIQUE;
CREATE CONSTRAINT ON (superAdmin:SuperAdmin) ASSERT superAdmin.id IS UNIQUE;
CREATE CONSTRAINT ON (superAdmin:SuperAdmin) ASSERT superAdmin.login IS UNIQUE;
CREATE CONSTRAINT ON (student:Student) ASSERT student.id IS UNIQUE;
CREATE CONSTRAINT ON (student:Student) ASSERT student.login IS UNIQUE;
CREATE CONSTRAINT ON (teacher:Teacher) ASSERT teacher.id IS UNIQUE;
CREATE CONSTRAINT ON (teacher:Teacher) ASSERT teacher.login IS UNIQUE;
CREATE CONSTRAINT ON (relative:Relative) ASSERT relative.id IS UNIQUE;
CREATE CONSTRAINT ON (relative:Relative) ASSERT relative.login IS UNIQUE;
CREATE CONSTRAINT ON (profileGroup:ProfileGroup) ASSERT profileGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (schoolProfileGroup:SchoolProfileGroup) ASSERT schoolProfileGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (classProfileGroup:ClassProfileGroup) ASSERT classProfileGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (schoolStudentGroup:SchoolStudentGroup) ASSERT schoolStudentGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (schoolTeacherGroup:SchoolTeacherGroup) ASSERT schoolTeacherGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (schoolRelativeGroup:SchoolRelativeGroup) ASSERT schoolRelativeGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (schoolPrincipalGroup:SchoolPrincipalGroup) ASSERT schoolPrincipalGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (classStudentGroup:ClassStudentGroup) ASSERT classStudentGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (classTeacherGroup:ClassTeacherGroup) ASSERT classTeacherGroup.id IS UNIQUE;
CREATE CONSTRAINT ON (classRelativeGroup:ClassRelativeGroup) ASSERT classRelativeGroup.id IS UNIQUE;
commit

