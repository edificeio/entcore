begin transaction
CREATE CONSTRAINT ON (school:School) ASSERT school.UAI IS UNIQUE;
commit

