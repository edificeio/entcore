/*
 * Copyright © "Open Digital Education", 2020
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.UUID;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.template.TemplateProcessor;
import fr.wseduc.webutils.template.lambdas.I18nLambda;
import fr.wseduc.webutils.template.lambdas.LocaleDateLambda;

import static fr.wseduc.webutils.Utils.getOrElse;

public class TimetableReport
{
  private static class ReportEntity
  {
    protected JsonObject view;

    public JsonObject get() { return this.view; }

    @Override
    public String toString() { return view.toString(); }
  }

  public static class User extends ReportEntity
  {
    public User(String firstName, String lastName, String birthDate)
    {
      if(firstName == null) firstName = "—";
      if(lastName == null) lastName = "—";
      if(birthDate == null) birthDate = "—";

      this.view = new JsonObject()
        .put("firstName", firstName.trim())
        .put("lastName", lastName.trim().toUpperCase())
        .put("birthDate", birthDate.trim());
    }
  }

  public static class Teacher extends User
  {
    public Teacher(String firstName, String lastName, String birthDate)
    {
      super(firstName, lastName, birthDate);
    }
  }

  public static class Student extends User
  {
    public Student(String firstName, String lastName, String birthDate)
    {
      super(firstName, lastName, birthDate);
    }
  }

  public static class SchoolClass extends ReportEntity
  {
    public SchoolClass(String className)
    {
      if(className == null) className = "—";

      this.view = new JsonObject().put("name", className);
    }
  }

  public static class Subject extends ReportEntity
  {
    public Subject(String code)
    {
      this.view = new JsonObject().put("code", code);
    }
    @Override
    public String toString() { return this.view.getString("code"); }
  }

  private String fileID;
  private String UAI;
  private String source;
  private long startTime;
  private long endTime;

  private List<Integer> processedWeeks = new LinkedList<Integer>();

  private long nbTeachersFound = 0;
  private List<Teacher> unknownTeachers = new LinkedList<Teacher>();

  private long nbClassesFound = 0;
  private List<SchoolClass> classesToReconciliate = new LinkedList<SchoolClass>();

  private List<String> groupsCreated = new LinkedList<String>();
  private List<String> groupsUpdated = new LinkedList<String>();
  private List<String> groupsDeleted = new LinkedList<String>();

  private long nbCoursesCreated = 0;
  private long nbCoursesDeleted = 0;
  private long nbCoursesIgnored = 0;

  private List<Subject> createdSubjects = new LinkedList<Subject>();
  private Map<Subject, List<Teacher>> usersAttachedToSubject = new HashMap<Subject, List<Teacher>>();

  private long nbUsersFound = 0;
  private List<User> missingUsers = new LinkedList<User>();

  private static final Map<Vertx, TemplateProcessor> templateProcessors = new ConcurrentHashMap<Vertx, TemplateProcessor>();
  private TemplateProcessor templator;
  private boolean waitFileID = false;

  public TimetableReport(Vertx vertx)
  {
    this(vertx, "fr");
  }

  public TimetableReport(Vertx vertx, String locale)
  {
    this.templator = TimetableReport.templateProcessors.get(vertx);

    if(this.templator == null)
    {
      this.templator = new TemplateProcessor(vertx, "template").escapeHTML(false);

      this.templator.setLambda("i18n", new I18nLambda(locale));
      this.templator.setLambda("datetime", new LocaleDateLambda(locale));

      TimetableReport.templateProcessors.put(vertx, this.templator);
    }
  }

  public void persist()
  {
    if(this.fileID == null)
    {
      this.waitFileID = true;
      return;
    }
    else
      this.waitFileID = false;

    this.template(new Handler<String>()
    {
      @Override
      public void handle(String report)
      {
        JsonObject document = new JsonObject()
          .put("_id", UUID.randomUUID().toString())
          .put("created", MongoDb.now())
          .put("source", source)
          .put("UAI", UAI)
          .put("report", report)
          .put("fileID", fileID);

        MongoDb.getInstance().save("timetableImports", document, new Handler<Message<JsonObject>>()
        {
          @Override
          public void handle(Message<JsonObject> msg)
          {

          }
        });
      }
    });
  }

  public void template(Handler<String> handler)
  {
    JsonObject params = new JsonObject();

    JsonArray uas = new JsonArray();

    for(Map.Entry<Subject, List<Teacher>> entry : this.usersAttachedToSubject.entrySet())
    {
      JsonObject item = new JsonObject()
        .put("class", entry.getKey().toString())
        .put("teachers", this.getTemplateEntities(entry.getValue()));
      uas.add(item);
    }

    long elapsed = (endTime - startTime) / 1000;
    String seconds = Long.toString(elapsed % 60);
    String minutes = Long.toString((elapsed / 60) % 60);
    String hours = Long.toString(elapsed / 3600);
    String runTime =
      (hours.length() == 1 ? "0" : "") + hours + "h" +
      (minutes.length() == 1 ? "0" : "") + minutes + "m" +
      (seconds.length() == 1 ? "0" : "") + seconds + "s";

    params
      .put("UAI", this.UAI)
      .put("source", this.source)
      .put("date", startTime)
      .put("startTime", startTime)
      .put("endTime", endTime)
      .put("runTime", runTime)
      .put("weeks", this.getTemplateWeeks())
      .put("nbTeachersFound", nbTeachersFound)
      .put("unknownTeachers", this.getTemplateEntities(this.unknownTeachers))
      .put("nbClassesFound", nbClassesFound)
      .put("classesToReconciliate", this.getTemplateEntities(this.classesToReconciliate))
      .put("groupsCreated", new JsonArray(this.groupsCreated))
      .put("groupsUpdated", new JsonArray(this.groupsUpdated))
      .put("groupsDeleted", new JsonArray(this.groupsDeleted))
      .put("nbCoursesCreated", this.nbCoursesCreated)
      .put("nbCoursesDeleted", this.nbCoursesDeleted)
      .put("nbCoursesIgnored", this.nbCoursesIgnored)
      .put("createdSubjects", this.getTemplateEntities(this.createdSubjects))
      .put("usersAttachedToSubject", uas)
      .put("nbUsersFound", this.nbUsersFound)
      .put("missingUsers", this.getTemplateEntities(this.missingUsers))
      ;

    this.templator.processTemplate("timetable-report.txt", params, new Handler<String>()
    {
      @Override
      public void handle(String template)
      {
        handler.handle(template);
      }
    });
  }

  private JsonArray getTemplateWeeks()
  {
    JsonArray weeks = new JsonArray();
    JsonObject weekRange = null;
    for(byte i = 0; i < this.processedWeeks.size(); ++i)
    {
      byte wk = this.processedWeeks.get(i).byteValue();
      if(weekRange == null)
      {
        weekRange = new JsonObject().put("start", wk);
        weeks.add(weekRange);
      }
      else if(weekRange != null)
      {
        byte last = getOrElse(weekRange.getInteger("end"), weekRange.getInteger("start")).byteValue();
        if(last == wk - 1)
          weekRange.put("end", wk);
        else
        {
          weekRange = null;
          --i;
        }
      }
    }

    return weeks;
  }

  private JsonArray getTemplateEntities(List<? extends ReportEntity> l)
  {
    List<JsonObject> resList = new ArrayList<JsonObject>(l.size());

    for(ReportEntity re : l)
      resList.add(re.get());

    return new JsonArray(resList);
  }

  //====================================================== SETTERS ======================================================

  public void setFileID(String id)
  {
    this.fileID = id;
    if(this.waitFileID == true)
      this.persist();
  }

  public void setUAI(String UAI)
  {
    this.UAI = UAI;
  }

  public void setSource(String source)
  {
    this.source = source;
  }

  public void start()
  {
    this.startTime = System.currentTimeMillis();
  }

  public void end()
  {
    this.endTime = System.currentTimeMillis();

    this.persist();
  }

  public void addWeek(int week)
  {
    this.processedWeeks.add(week);
  }

  public void teacherFound()
  {
    ++this.nbTeachersFound;
  }

  public void addUnknownTeacher(Teacher teacher)
  {
    this.unknownTeachers.add(teacher);
  }

  public void classFound()
  {
    ++this.nbClassesFound;
  }

  public void addClassToReconciliate(SchoolClass recClass)
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

  public void addCreatedSubject(Subject subject)
  {
    this.createdSubjects.add(subject);
  }

  public void addUserToSubject(Teacher user, Subject subject)
  {
    if(this.usersAttachedToSubject.containsKey(subject) == false)
      this.usersAttachedToSubject.put(subject, new LinkedList<Teacher>());
    if(user != null)
    {
      List<Teacher> teachers = this.usersAttachedToSubject.get(subject);
      if(teachers.contains(user) == false)
        teachers.add(user);
    }
  }

  public void userFound()
  {
    ++this.nbUsersFound;
  }

  public void addMissingUser(User user)
  {
    this.missingUsers.add(user);
  }
}