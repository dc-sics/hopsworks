package se.kth.meta.wscomm.message;

import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import se.kth.meta.entity.EntityIntf;
import se.kth.meta.entity.MTable;

/**
 *
 * @author Vangelis
 */
public class CheckTableContentMessage extends ContentMessage {

  private static final Logger logger = Logger.getLogger(
          CheckTableContentMessage.class.
          getName());

  public CheckTableContentMessage() {
    super();
    this.TYPE = "CheckTableContentMessage";
  }

  public CheckTableContentMessage(String sender, String message) {
    this();
    this.sender = sender;
    this.message = message;
  }

  @Override
  public void init(JsonObject json) {
    this.sender = json.getString("sender");
    this.message = json.getString("message");
    this.action = json.getString("action");
  }

  @Override
  public String encode() {
    String value = Json.createObjectBuilder()
            .add("sender", this.sender)
            .add("type", this.TYPE)
            .add("status", this.status)
            .add("message", this.message)
            .build() //pretty necessary so as to build the actual json structure
            .toString();

    return value;
  }

  @Override
  public String getAction() {
    return this.action;
  }

  @Override
  public List<EntityIntf> parseSchema() {
    JsonObject obj = Json.createReader(new StringReader(this.message)).
            readObject();

    List<EntityIntf> list = null;

    try {
      int tableid = obj.getInt("tableid");

      MTable table = new MTable(tableid);
      list = new LinkedList<>();
      list.add(table);
    } catch (NullPointerException e) {
      logger.log(Level.SEVERE, "Table id was not present in the message");
    }

    return list;
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public void setMessage(String msg) {
    this.message = msg;
  }

  @Override
  public String getSender() {
    return this.sender;
  }

  @Override
  public void setSender(String sender) {
    this.sender = sender;
  }

  @Override
  public String getStatus() {
    return this.status;
  }

  @Override
  public void setStatus(String status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "{\"sender\": \"" + this.sender + "\", "
            + "\"type\": \"" + this.TYPE + "\", "
            + "\"status\": \"" + this.status + "\", "
            + "\"action\": \"" + this.action + "\", "
            + "\"message\": \"" + this.message + "\"}";
  }
}
