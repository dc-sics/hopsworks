package se.kth.hopsworks.zeppelin.rest;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.rest.AppException;
import se.kth.hopsworks.zeppelin.rest.message.InterpreterSettingListForNoteBind;
import se.kth.hopsworks.zeppelin.rest.message.NewNotebookRequest;
import se.kth.hopsworks.zeppelin.server.JsonResponse;
import se.kth.hopsworks.zeppelin.server.ZeppelinConfig;

/**
 * Rest api endpoint for the noteBook.
 */
@RequestScoped
public class NotebookRestApi {

  Logger logger = LoggerFactory.getLogger(NotebookRestApi.class);
  Gson gson = new Gson();
  Project project;
  ZeppelinConfig zeppelinConf;
  String roleInProject;

  public NotebookRestApi() {
  }

  public void setParms(Project project, String userRole,
          ZeppelinConfig zeppelinConf) {
    this.project = project;
    this.zeppelinConf = zeppelinConf;
    this.roleInProject = userRole;
  }

  /**
   * bind a setting to note
   * <p/>
   * @param noteId
   * @param req
   * @return
   * @throws IOException
   */
  @PUT
  @Path("interpreter/bind/{noteId}")
  public Response bind(@PathParam("noteId") String noteId, String req) throws
          IOException {
    List<String> settingIdList = gson.fromJson(req,
            new TypeToken<List<String>>() {
    }.getType());
    zeppelinConf.getNotebook().bindInterpretersToNote(noteId, settingIdList);
    return new JsonResponse(Status.OK).build();
  }

  /**
   * list binded setting
   * <p/>
   * @param noteId
   * @return
   */
  @GET
  @Path("interpreter/bind/{noteId}")
  public Response bind(@PathParam("noteId") String noteId) {
    List<InterpreterSettingListForNoteBind> settingList;
    settingList = new LinkedList<>();

    List<InterpreterSetting> selectedSettings = zeppelinConf.getNotebook().
            getBindedInterpreterSettings(noteId);
    for (InterpreterSetting setting : selectedSettings) {
      settingList.add(new InterpreterSettingListForNoteBind(
              setting.id(),
              setting.getName(),
              setting.getGroup(),
              setting.getInterpreterGroup(),
              true)
      );
    }

    List<InterpreterSetting> availableSettings = zeppelinConf.getNotebook().
            getInterpreterFactory().get();
    for (InterpreterSetting setting : availableSettings) {
      boolean selected = false;
      for (InterpreterSetting selectedSetting : selectedSettings) {
        if (selectedSetting.id().equals(setting.id())) {
          selected = true;
          break;
        }
      }

      if (!selected) {
        settingList.add(new InterpreterSettingListForNoteBind(
                setting.id(),
                setting.getName(),
                setting.getGroup(),
                setting.getInterpreterGroup(),
                false)
        );
      }
    }
    return new JsonResponse(Status.OK, "", settingList).build();
  }

  @GET
  public Response getNotebookList() throws
          IOException {
    List<NoteInfo> notesInfo = zeppelinConf.getNotebookRepo().list();
    return new JsonResponse(Status.OK, "", notesInfo).build();
  }

  /**
   * Create new note REST API
   * <p>
   * @param message - JSON with new note name
   * @return JSON with new note ID
   * @throws IOException
   */
  @POST
  public Response createNote(String message) throws IOException {
    logger.info("Create new notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    Note note = zeppelinConf.getNotebook().createNote();
    note.addParagraph(); // it's an empty note. so add one paragraph
    String noteName = request.getName();
    if (noteName.isEmpty()) {
      noteName = "Note " + note.getId();
    }
    note.setName(noteName);
    note.persist();
//    zeppelinConf.getNotebookServer().broadcastNote(note);
//    zeppelinConf.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.CREATED, "", note.getId()).build();
  }

  /**
   * Delete note REST API
   * <p>
   * @param notebookId@return JSON with status.OK
   * @throws IOException
   */
  @DELETE
  @Path("{notebookId}")
  public Response deleteNote(@PathParam("notebookId") String notebookId) throws
          IOException {
    logger.info("Delete notebook {} ", notebookId);
    if (!(notebookId.isEmpty())) {
      Note note = zeppelinConf.getNotebook().getNote(notebookId);
      if (note != null) {
        zeppelinConf.getNotebook().removeNote(notebookId);
      }
    }
    zeppelinConf.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.OK, "").build();
  }

  /**
   * Clone note REST API@return JSON with status.CREATED
   * <p>
   * @param notebookId
   * @param message
   * @return
   * @throws IOException
   * @throws java.lang.CloneNotSupportedException
   */
  @POST
  @Path("{notebookId}")
  public Response cloneNote(@PathParam("notebookId") String notebookId,
          String message) throws
          IOException, CloneNotSupportedException, IllegalArgumentException {
    logger.info("clone notebook by JSON {}", message);
    NewNotebookRequest request = gson.fromJson(message,
            NewNotebookRequest.class);
    String newNoteName = request.getName();
    Note newNote = zeppelinConf.getNotebook().cloneNote(notebookId, newNoteName);
    zeppelinConf.getNotebookServer().broadcastNote(newNote);
    zeppelinConf.getNotebookServer().broadcastNoteList();
    return new JsonResponse(Status.CREATED, "", newNote.getId()).build();
  }

  /**
   * Create new note in a project
   * <p/>
   * @param newNote
   * @return note info if successful.
   * @throws se.kth.hopsworks.rest.AppException
   */
  @POST
  @Path("/new")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createNew(NewNotebookRequest newNote) throws
          AppException {
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    Note note;
    NoteInfo noteInfo;
    try {
      note = zeppelinConf.getNotebook().createNote();
      note.addParagraph(); // it's an empty note. so add one paragraph
      String noteName = newNote.getName();
      if (noteName == null || noteName.isEmpty()) {
        noteName = "Note " + note.getId();
      }
      note.setName(noteName);
      note.persist();
      noteInfo = new NoteInfo(note);
    } catch (IOException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Could not create notebook" + ex.getMessage());
    }
    return new JsonResponse(Status.OK, "", noteInfo).build();
  }
}
