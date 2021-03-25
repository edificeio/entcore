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

package org.entcore.feeder.timetable.edt;

import org.entcore.feeder.Feed;
import org.entcore.feeder.dictionary.structures.DefaultFunctions;
import org.entcore.feeder.dictionary.structures.DefaultProfiles;
import org.entcore.feeder.dictionary.structures.Importer;
import org.entcore.feeder.dictionary.structures.Structure;
import org.joda.time.LocalDate;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.security.NoSuchAlgorithmException;

import fr.wseduc.webutils.security.Md5;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class EDTFeeder implements Feed, EDTReader
{
  // ================================================ Groups classes ================================================

  public abstract class ExternalRessource
  {
    protected String structId;
    protected String externalId;

    public ExternalRessource(Structure s)
    {
      this.structId = s.getExternalId();
    }

    protected String getExternalIdPrefix()
    {
      return this.structId + "$";
    }

    public void setExternalId(String externalId)
    {
      String eidPrefix = this.getExternalIdPrefix();
      if(externalId.startsWith(eidPrefix))
        this.externalId = externalId;
      else
        this.externalId = eidPrefix + externalId;
    }
  }

  public abstract class LinkableRessource extends ExternalRessource
  {
    protected Structure structure;

    public LinkableRessource(Structure s)
    {
      super(s);
      this.structure = s;
    }

    public String[] getLinkArray()
    {
      String[] link = new String[3];

      link[0] = this.structId;
      link[1] = this.externalId;
      link[2] = "";

      return link;
    }
  }

  public interface TimegatedEnsemble
  {
    default public LocalDate getNow()
    {
      return new LocalDate();
    }

    default public boolean isCurrentlyInEnsemble(String entryDate, String leaveDate)
    {
      LocalDate now = this.getNow();
      return now.isAfter(new LocalDate(entryDate)) && now.isBefore(new LocalDate(leaveDate));
    }
  }

  public class Subject
  {
    public String code;
    public String name;

    public String getExternalId(Importer importer)
    {
      final String fosEId = importer.getFieldOfStudy().get(this.name);

      return fosEId != null ? fosEId : this.code;
    }

    public JsonObject get(Importer importer)
    {
      return new JsonObject()
        .put("name", this.name)
        .put("externalId", this.getExternalId(importer));
    }
  }

  public class Classe extends LinkableRessource implements TimegatedEnsemble
  {
    public String name;

    public HeadTeacherGroup headTeacherGroup;
    public List<Group> groups = new ArrayList<Group>();

    public Classe(Structure s)
    {
      super(s);
    }

    public HeadTeacherGroup getHeadTeacherGroup()
    {
      if(this.headTeacherGroup == null)
        this.headTeacherGroup = new HeadTeacherGroup(this.structure, this);
      return this.headTeacherGroup;
    }

    public JsonObject get()
    {
      return new JsonObject().put("name", this.name);
    }
  }

  public class PartieClasse implements TimegatedEnsemble
  {
    public Classe root;

    public List<Group> groups = new ArrayList<Group>();

    public PartieClasse(Classe root)
    {
      this.root = root;
    }
  }

  public class Group extends LinkableRessource implements TimegatedEnsemble
  {
    public String name;
    public String identity;

    public Group(Structure s)
    {
      super(s);
    }
  }

  public class HeadTeacherGroup extends Group
  {
    public HeadTeacherGroup(Structure s, Classe c)
    {
      super(s);

      this.name = c.name + "-ht";
      this.identity = UUID.randomUUID().toString();

      this.setExternalId(this.name);
    }
  }

  // ================================================ User classes ================================================

  public abstract class User extends ExternalRessource
  {
    public String firstName;
    public String lastName;

    private String profile;

    public Map<String, Classe> classes = new HashMap<String, Classe>();
    public Map<String, PartieClasse> subclasses = new HashMap<String, PartieClasse>();
    public Map<String, Group> groups = new HashMap<String, Group>();

    public User(Structure s, String profile)
    {
      super(s);
      this.profile = profile;
    }

    public String[][] getLinks(Map<String, ? extends LinkableRessource> innerMap)
    {
      String[][] links = new String[innerMap.size()][2];

      Collection<? extends LinkableRessource> rcs = innerMap.values();

      int i = 0;
      for(LinkableRessource lr : rcs)
        links[i++] = lr.getLinkArray();

      return links;
    }

    public JsonObject get()
    {
      return new JsonObject()
        .put("firstName", this.firstName)
        .put("lastName", this.lastName)
        .put("externalId", this.externalId)
        .put("structures", new JsonArray().add(this.structId))
        .put("profiles", new JsonArray().add(this.profile));
    }
  }

  public class Teacher extends User
  {
    public String birthDate;
    public String zipCode;

    public List<Classe> principalClasses = new ArrayList<Classe>();
    public Map<String, Subject> subjects = new HashMap<String, Subject>();

    public Teacher(Structure s)
    {
      super(s, DefaultProfiles.TEACHER_PROFILE.getString("name"));
    };

    @Override
    public JsonObject get()
    {
      JsonObject o = super.get();

      if(isNotEmpty(this.birthDate))
        o.put("birthDate", this.birthDate);
      if(isNotEmpty(this.zipCode))
        o.put("zipCode", this.zipCode);

      return o;
    }
  }

  public class Personnel extends User
  {
    public Personnel(Structure s) { super(s, DefaultProfiles.PERSONNEL_PROFILE.getString("name")); };
  }

  public class Student extends User
  {
    public String birthDate;

    public List<Parent> parents = new ArrayList<Parent>();

    public Student(Structure s)
    {
      super(s, DefaultProfiles.STUDENT_PROFILE.getString("name"));
    }

    public JsonArray getParentsIds()
    {
      JsonArray a = new JsonArray();
      for(Parent p : this.parents)
        a.add(p.externalId);
      return a;
    }

    public void generateGroups()
    {
      for(Classe c : this.classes.values())
      {
        for(Group g : c.groups)
        {
          if(this.groups.containsKey(g.identity) == false)
            this.groups.put(g.identity, g);
        }
      }

      for(PartieClasse pc : this.subclasses.values())
      {
        for(Group g : pc.groups)
        {
          if(this.groups.containsKey(g.identity) == false)
            this.groups.put(g.identity, g);
        }
      }
    }

    @Override
    public JsonObject get()
    {
      JsonObject o = super.get();

      if(isNotEmpty(this.birthDate))
        o.put("birthDate", this.birthDate);

      return o;
    }
  }

  public class Parent extends User
  {
    public String birthDate;
    public String zipCode;
    public String address;
    public String city;
    public String country;

    public List<Student> children = new ArrayList<Student>();

    public Parent(Structure s)
    {
      super(s, DefaultProfiles.RELATIVE_PROFILE.getString("name"));
    }

    public JsonArray getChildrenIds()
    {
      JsonArray a = new JsonArray();
      for(Student s : this.children)
        a.add(s.externalId);
      return a;
    }

    @Override
    public JsonObject get()
    {
      JsonObject o = super.get();

      if(isNotEmpty(this.birthDate))
        o.put("birthDate", this.birthDate);
      if(isNotEmpty(this.zipCode))
        o.put("zipCode", this.zipCode);
      if(isNotEmpty(this.address))
        o.put("address", this.address);
      if(isNotEmpty(this.city))
        o.put("city", this.city);
      if(isNotEmpty(this.country))
        o.put("country", this.country);

      return o;
    }
  }

  // ================================================ Feeder code ================================================

	public static final String IDENT = "Ident";
  protected static final Logger log = LoggerFactory.getLogger(EDTFeeder.class);

  private EDTUtils edtUtils;
  private String mode;
  private Structure structure;

  private Map<String, Teacher> teachers = new HashMap<String, Teacher>();
  private Map<String, Personnel> personnels = new HashMap<String, Personnel>();
  private Map<String, Student> students = new HashMap<String, Student>();
  private Map<String, Parent> parents = new HashMap<String, Parent>();
  private Map<String, Subject> subjects = new HashMap<String, Subject>();
  private Map<String, Classe> classes =  new HashMap<String, Classe>();
  private Map<String, PartieClasse> subclasses =  new HashMap<String, PartieClasse>();
  private Map<String, Group> groups = new HashMap<String, Group>();

  public EDTFeeder(EDTUtils edtUtils, String mode)
  {
    this.edtUtils = edtUtils;
    this.mode = mode;
  }

  @Override
  public void launch(Importer importer, Handler<Message<JsonObject>> handler) throws Exception
  {
		throw new UnsupportedOperationException();
  }

  @Override
  public void launch(Importer importer, String path, Handler<Message<JsonObject>> handler) throws Exception
  {
    JsonObject structureInfos = new JsonObject();
		final String content = this.edtUtils.getContent(path, mode, structureInfos);
    this.structure = importer.createOrUpdateStructure(structureInfos);

    // This method is synchronous
    this.edtUtils.parseContent(content, this);

    this.runImport(importer, this.structure, handler);
  }

  @Override
  public void launch(Importer importer, String path, JsonObject mappings, Handler<Message<JsonObject>> handler) throws Exception
  {
		this.launch(importer, path, handler);
  }

  // -------------------------------------------- IMPORT --------------------------------------------

  private void runImport(Importer importer, Structure structure, Handler<Message<JsonObject>> handler)
  {
    this.runDefaults(importer);
    this.importGroups(importer, structure);
    this.importSubjects(importer, structure);

    importer.flush(new Handler<Message<JsonObject>>()
    {
      @Override
      public void handle(Message<JsonObject> message)
      {
        if ("ok".equals(message.body().getString("status")))
        {
          importTeachers(importer, structure);
          importPersonnels(importer, structure);
          importStudents(importer, structure);

          importer.flush(new Handler<Message<JsonObject>>()
          {
            @Override
            public void handle(Message<JsonObject> message)
            {
              if ("ok".equals(message.body().getString("status")))
              {
                importParents(importer, structure);

                importer.flush(new Handler<Message<JsonObject>>()
                {
                  @Override
                  public void handle(Message<JsonObject> message)
                  {
                    if ("ok".equals(message.body().getString("status")))
                    {
                      end(importer, structure, handler);
                    }
                    else
                    {
                      importer.getReport().addErrorWithParams("flush.error", "parents");
                      handler.handle(null);
                    }
                  }
                });
              }
              else
              {
                importer.getReport().addErrorWithParams("flush.error", "users");
                handler.handle(null);
              }
            }
          });
        }
        else
        {
          importer.getReport().addErrorWithParams("flush.error", "groups");
          handler.handle(null);
        }
      }
    });
  }

  private void runDefaults(Importer importer)
  {
		importer.createOrUpdateProfile(DefaultProfiles.STUDENT_PROFILE);
		importer.createOrUpdateProfile(DefaultProfiles.RELATIVE_PROFILE);
		importer.createOrUpdateProfile(DefaultProfiles.PERSONNEL_PROFILE);
		importer.createOrUpdateProfile(DefaultProfiles.TEACHER_PROFILE);
		importer.createOrUpdateProfile(DefaultProfiles.GUEST_PROFILE);
    DefaultFunctions.createOrUpdateFunctions(importer);
  }

  private void importGroups(Importer importer, Structure structure)
  {
    for(Classe c : this.classes.values())
      this.structure.createClassIfAbsent(c.externalId, c.name);

    for(Group g : this.groups.values())
      this.structure.createFunctionalGroupIfAbsent(g.externalId, g.name, this.getFeederSource());

    // Create HeadTeacher groups
    for(Teacher t : this.teachers.values())
    {
      for(Classe c : t.principalClasses)
      {
        HeadTeacherGroup htg = c.getHeadTeacherGroup();
        this.structure.createHeadTeacherGroupIfAbsent(c.externalId, c.name);

        if(t.groups.containsKey(htg.identity) == false)
          t.groups.put(htg.identity, htg);
      }
    }
  }

  private void importSubjects(Importer importer, Structure structure)
  {
    for(Subject s : this.subjects.values())
      importer.createOrUpdateFieldOfStudy(s.get(importer));
  }

  private void importTeachers(Importer importer, Structure structure)
  {
    for(Teacher t : this.teachers.values())
    {
      JsonObject obj = t.get();
      importer.createOrUpdatePersonnel(obj, DefaultProfiles.TEACHER_PROFILE_EXTERNAL_ID,
        obj.getJsonArray("structures"), t.getLinks(t.classes), t.getLinks(t.groups), true, true);
    }
  }

  private void importPersonnels(Importer importer, Structure structure)
  {
    String[][] emptyLink = new String[0][];
    for(Personnel p : this.personnels.values())
    {
      JsonObject obj = p.get();
      importer.createOrUpdatePersonnel(obj, DefaultProfiles.PERSONNEL_PROFILE_EXTERNAL_ID,
        obj.getJsonArray("structures"), emptyLink, emptyLink, true, true);
    }
  }

  private void importStudents(Importer importer, Structure structure)
  {
    for(Student s : this.students.values())
    {
      JsonObject obj = s.get();
      importer.createOrUpdateStudent(obj, DefaultProfiles.STUDENT_PROFILE_EXTERNAL_ID, null, null,
      s.getLinks(s.classes), s.getLinks(s.groups), s.getParentsIds(), true, true);
    }
  }

  private void importParents(Importer importer, Structure structure)
  {
    for(Parent p : this.parents.values())
      importer.createOrUpdateUser(p.get(), p.getChildrenIds(), true);
  }

  private void end(Importer importer, Structure structure, Handler<Message<JsonObject>> handler)
  {
    // Pour les professeurs
    importer.getPersEducNat().createAndLinkSubjects(structure.getExternalId());

    // Pour les responsables
    importer.linkRelativeToClass(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID, null, structure.getExternalId());
    importer.linkRelativeToStructure(DefaultProfiles.RELATIVE_PROFILE_EXTERNAL_ID, null, structure.getExternalId());
    importer.addRelativeProperties(getFeederSource());

    // Finalisation
    importer.restorePreDeletedUsers();
    importer.addStructureNameInGroups(structure.getExternalId(), null);
    importer.persist(handler);
  }

  @Override
  public String getFeederSource()
  {
    return "EDT";
  }

  // --------- PROFESSEUR ---------

  @Override
  public void addProfesseur(JsonObject currentEntity)
  {
    Teacher prof = new Teacher(this.structure);

    prof.firstName = currentEntity.getString("Prenom");
    prof.lastName = currentEntity.getString("Nom");
    prof.setExternalId(currentEntity.getString("IDPN"));

    prof.birthDate = currentEntity.getString("DateNaissance");
    prof.zipCode = currentEntity.getString("CodePostal");

    this.teachers.put(currentEntity.getString(IDENT), prof);
  }

  // --------- PERSONNEL ---------

  @Override
  public void addPersonnel(JsonObject currentEntity)
  {
    Personnel pers = new Personnel(this.structure);

    pers.firstName = currentEntity.getString("Prenom");
    pers.lastName = currentEntity.getString("Nom");

    try
    {
      pers.setExternalId(Md5.hash(pers.lastName + pers.firstName));

      this.personnels.put(currentEntity.getString(IDENT), pers);
    }
    catch (NoSuchAlgorithmException e)
    {
			log.error("Error hash personnel Id.", e);
		}
  }

  // --------- ELEVE ---------

  @Override
  public void addEleve(JsonObject currentEntity)
  {
    Student ele = new Student(this.structure);

    ele.firstName = currentEntity.getString("Prenom");
    ele.lastName = currentEntity.getString("Nom");
    ele.setExternalId(currentEntity.getString("IDPN"));

    ele.birthDate = currentEntity.getString("DateNaissance");

    this.students.put(currentEntity.getString(IDENT), ele);

    JsonArray classes = currentEntity.getJsonArray("Classe", new JsonArray());
    JsonArray subclasses = currentEntity.getJsonArray("PartieDeClasse", new JsonArray());
    JsonArray parents = currentEntity.getJsonArray("Responsable", new JsonArray());

    for(Object oc : classes)
    {
      String ident = ((JsonObject)oc).getString(IDENT);
      if(ele.classes.containsKey(ident) == false)
      {
        Classe c = this.classes.get(ident);
        if(c.isCurrentlyInEnsemble(((JsonObject)oc).getString("DateEntree"), ((JsonObject)oc).getString("DateSortie")) == true)
          ele.classes.put(ident, c);
      }
    }

    for(Object opc : subclasses)
    {
      String ident = ((JsonObject)opc).getString(IDENT);
      if(ele.subclasses.containsKey(ident) == false)
      {
        PartieClasse pc = this.subclasses.get(ident);
        if(pc.isCurrentlyInEnsemble(((JsonObject)opc).getString("DateEntree"), ((JsonObject)opc).getString("DateSortie")) == true)
          ele.subclasses.put(ident, pc);
      }
    }

    // Compute groups from Classes and Subclasses
    ele.generateGroups();

    for(Object op : parents)
    {
      String ident = ((JsonObject)op).getString(IDENT);
      Parent p = this.parents.get(ident);
      if(p != null)
      {
        p.children.add(ele);
        ele.parents.add(p);
      }
    }
  }

  // --------- RESPONSABLE ---------

  @Override
  public void addResponsable(JsonObject currentEntity)
  {
    Parent resp = new Parent(this.structure);

    resp.firstName = currentEntity.getString("Prenom");
    resp.lastName = currentEntity.getString("Nom");
    resp.setExternalId(currentEntity.getString("IDPN"));

    resp.birthDate = currentEntity.getString("DateNaissance");
    resp.zipCode = currentEntity.getString("CodePostal");

    resp.address = currentEntity.getString("Adresse1", "")
                  + " " + currentEntity.getString("Adresse2", "")
                  + " " + currentEntity.getString("Adresse3", "")
                  + " " + currentEntity.getString("Adresse4", "").trim();
    resp.city = currentEntity.getString("Ville");
    resp.country = currentEntity.getString("Pays");

    this.parents.put(currentEntity.getString(IDENT), resp);
  }

  // --------- MATIERE ---------

  @Override
  public void addSubject(JsonObject currentEntity)
  {
    Subject s = new Subject();

    s.code = currentEntity.getString("Code");
    s.name = currentEntity.getString("Libelle");

    this.subjects.put(currentEntity.getString(IDENT), s);
  }


  // --------- CLASSE ---------

  @Override
  public void addClasse(JsonObject currentEntity)
  {
    Classe c = new Classe(this.structure);
    c.name = currentEntity.getString("Nom");
    c.setExternalId(c.name);

    this.classes.put(currentEntity.getString(IDENT), c);

    JsonArray pcs = currentEntity.getJsonArray("PartieDeClasse");
    if(pcs != null)
    {
      for(Object o : pcs)
        this.subclasses.put(((JsonObject)o).getString(IDENT), new PartieClasse(c));
    }

    JsonArray principalTeachers = currentEntity.getJsonArray("ProfesseurPrincipal");
    if(principalTeachers != null)
    {
      for(Object o : principalTeachers)
        this.teachers.get(((JsonObject)o).getString(IDENT)).principalClasses.add(c);
    }
  }

  // --------- GROUPE ---------

  @Override
  public void addGroup(JsonObject currentEntity)
  {
    Group g = new Group(this.structure);
    g.name = currentEntity.getString("Nom");
    g.identity = currentEntity.getString(IDENT);
    g.setExternalId(g.name);

    this.groups.put(currentEntity.getString(IDENT), g);

    JsonArray classesInGroup = currentEntity.getJsonArray("ProfesseurPrincipal");
    if(classesInGroup != null)
    {
      for(Object o : classesInGroup)
        this.classes.get(((JsonObject)o).getString(IDENT)).groups.add(g);
    }

    JsonArray pcs = currentEntity.getJsonArray("PartieDeClasse");
    if(pcs != null)
    {
      for(Object o : pcs)
        this.subclasses.get(((JsonObject)o).getString(IDENT)).groups.add(g);
    }
  }

  // --------- COURS ---------

  @Override
  public void addCourse(JsonObject currentEntity)
  {
    JsonArray profs = currentEntity.getJsonArray("Professeur", new JsonArray());
    JsonArray subjects = currentEntity.getJsonArray("Matiere", new JsonArray());
    JsonArray classes = currentEntity.getJsonArray("Classe", new JsonArray());
    JsonArray subclasses = currentEntity.getJsonArray("PartieDeClasse", new JsonArray());
    JsonArray groups = currentEntity.getJsonArray("Groupe", new JsonArray());

    for(Object op : profs)
    {
      Teacher prof = this.teachers.get(((JsonObject)op).getString(IDENT));

      for(Object oc : classes)
      {
        String ident = ((JsonObject)oc).getString(IDENT);
        if(prof.classes.containsKey(ident) == false)
        {
          Classe newClasse = this.classes.get(ident);
          prof.classes.put(ident, newClasse);
        }
      }
      for(Object os : subjects)
      {
        String ident = ((JsonObject)os).getString(IDENT);
        if(prof.subjects.containsKey(ident) == false)
          prof.subjects.put(ident, this.subjects.get(ident));
      }

      for(Object opc : subclasses)
      {
        String ident = ((JsonObject)opc).getString(IDENT);
        if(prof.subclasses.containsKey(ident) == false)
          prof.subclasses.put(ident, this.subclasses.get(ident));
      }

      for(Object og : groups)
      {
        String ident = ((JsonObject)og).getString(IDENT);
        if(prof.groups.containsKey(ident) == false)
          prof.groups.put(ident, this.groups.get(ident));
      }
    }
  }

  // --------- AUTRES ---------

  @Override
  public void addRoom(JsonObject currentEntity)
  {
    // Nothing to do
  }

  @Override
  public void addEquipment(JsonObject currentEntity)
  {
    // Nothing to do
  }

  @Override
  public void initSchedule(JsonObject currentEntity)
  {
    // Nothing to do
  }

  @Override
  public void initSchoolYear(JsonObject currentEntity)
  {
    // Nothing to do
  }

}