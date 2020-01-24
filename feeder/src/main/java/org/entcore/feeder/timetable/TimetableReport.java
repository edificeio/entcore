/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.feeder.timetable;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;

import io.vertx.core.json.JsonObject;

public class TimetableReport
{
  private long startTime;
  private long endTime;

  private List<Integer> processedWeeks = new LinkedList<Integer>();

  private long nbTeachersFound = 0;
  private List<JsonObject> unknownTeachers = new LinkedList<JsonObject>();

  private long nbClassesFound = 0;
  private List<JsonObject> classesToReconciliate = new LinkedList<JsonObject>();

  private List<String> groupsCreated = new LinkedList<String>();
  private List<String> groupsUpdated = new LinkedList<String>();
  private List<String> groupsDeleted = new LinkedList<String>();

  private long nbCoursesCreated = 0;
  private long nbCoursesDeleted = 0;
  private long nbCoursesIgnored = 0;

  private List<JsonObject> createdSubjects = new LinkedList<JsonObject>();
  private Map<JsonObject, List<JsonObject>> usersAttachedToSubject = new HashMap<JsonObject, List<JsonObject>>();

  private long nbUsersFound = 0;
  private List<JsonObject> missingUsers = new LinkedList<JsonObject>();

  public String print()
  {
    return
      "S: " + startTime + "\tE: " + endTime + "\n" +
      "Weeks: " + processedWeeks + "\n" +
      "Teachers: " + nbTeachersFound + " OK; KO: " + unknownTeachers + "\n" +
      "Classes:  " + nbClassesFound +  " OK; KO: " + classesToReconciliate + "\n" +
      "Groups:\n" + groupsCreated + "\n" + groupsUpdated + "\n" + groupsDeleted + "\n" +
      "Courses:  " + nbCoursesCreated + "/" + nbCoursesDeleted + "/" + nbCoursesIgnored + "\n" +
      "Subjects:\n" + createdSubjects + "\nSubjectMap:\n" + usersAttachedToSubject + "\n" +
      "Users:    " + nbUsersFound +    " OK; KO: " + missingUsers + "\n";
  }

  public void start()
  {
    this.startTime = System.currentTimeMillis();
  }

  public void end()
  {
    this.endTime = System.currentTimeMillis();

    System.out.println("\n\n" + this.print() + "\n\n");
  }

  public void addWeek(int week)
  {
    this.processedWeeks.add(week);
  }

  public void teacherFound()
  {
    ++this.nbTeachersFound;
  }

  public void addUnknownTeacher(JsonObject teacher)
  {
    this.unknownTeachers.add(teacher);
  }

  public void classFound()
  {
    ++this.nbClassesFound;
  }

  public void addClassToReconciliate(JsonObject recClass)
  {
    this.classesToReconciliate.add(recClass);
  }

  public void groupCreated(String group)
  {
    this.groupsCreated.add(group);
  }

  public void groupUpdated(String group)
  {
    this.groupsUpdated.add(group);
  }

  public void groupDeleted(String group)
  {
    this.groupsDeleted.add(group);
  }

  public void courseCreated()
  {
    ++this.nbCoursesCreated;
  }

  public void courseCreated(int nb)
  {
    this.nbCoursesCreated += nb;
  }

  public void courseDeleted()
  {
    ++this.nbCoursesDeleted;
  }

  public void courseDeleted(int nb)
  {
    this.nbCoursesDeleted += nb;
  }

  public void courseIgnored()
  {
    ++this.nbCoursesIgnored;
  }

  public void courseIgnored(int nb)
  {
    this.nbCoursesIgnored += nb;
  }

  public void addCreatedSubject(JsonObject subject)
  {
    this.createdSubjects.add(subject);
  }

  public void addUserToSubject(JsonObject user, JsonObject subject)
  {
    if(this.usersAttachedToSubject.containsKey(subject) == false)
      this.usersAttachedToSubject.put(subject, new LinkedList<JsonObject>());
    if(user != null)
      this.usersAttachedToSubject.get(subject).add(user);
  }

  public void userFound()
  {
    ++this.nbUsersFound;
  }

  public void addMissingUser(JsonObject user)
  {
    this.missingUsers.add(user);
  }
}