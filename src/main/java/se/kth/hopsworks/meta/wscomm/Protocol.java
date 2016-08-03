package se.kth.hopsworks.meta.wscomm;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import se.kth.bbc.project.fb.Inode;
import se.kth.hopsworks.meta.entity.EntityIntf;
import se.kth.hopsworks.meta.entity.Field;
import se.kth.hopsworks.meta.entity.HdfsMetadataLog;
import se.kth.hopsworks.meta.entity.InodeTableComposite;
import se.kth.hopsworks.meta.entity.MTable;
import se.kth.hopsworks.meta.entity.Metadata;
import se.kth.hopsworks.meta.exception.ApplicationException;
import se.kth.hopsworks.meta.wscomm.message.Command;
import se.kth.hopsworks.meta.wscomm.message.Message;
import se.kth.hopsworks.meta.wscomm.message.MetadataLogMessage;
import se.kth.hopsworks.meta.wscomm.message.MetadataMessage;
import se.kth.hopsworks.meta.wscomm.message.RemoveMetadataMessage;
import se.kth.hopsworks.meta.wscomm.message.StoreMetadataMessage;
import se.kth.hopsworks.meta.wscomm.message.TextMessage;
import se.kth.hopsworks.meta.wscomm.message.UpdateMetadataMessage;

/**
 * Constructs responses depending on the incoming message requests. Maintains
 * the communication with the front end.
 * <p/>
 * @author Vangelis
 */
@Stateless(name = "protocol")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class Protocol {

  private static final Logger logger = Logger.
          getLogger(Protocol.class.getName());

  @EJB
  private Utils utils;
  @EJB
  private ResponseBuilder builder;

  public Protocol() {
    logger.log(Level.INFO, "Protocol initialized");
  }

  /**
   * Process an incoming message and create and send back the response
   *
   * @param message the incoming message
   * @return a new response message or an error message
   */
  public Message GFR(Message message) {

    Message msg;
    try {
      msg = this.processMessage(message);
    } catch (ApplicationException e) {
      TextMessage response = new TextMessage("Server", e.getMessage());
      response.setStatus("ERROR");
      return response;
    }
    return msg;
  }

  /**
   * Receives a user message, translates it into the command it represents,
   * executes the command and sends back the produced response.
   * Commands 'store_metadata' and 'update_metadata' are always followed by
   * 'create_meta_log', and these two must be atomic. Hence the
   * TransactionAttribute annotation
   * <p/>
   * @param message
   * @return
   * @throws ApplicationException
   */
  private Message processMessage(Message message) throws ApplicationException {

    Command action = Command.valueOf(message.getAction().toUpperCase());

    switch (action) {
      case STORE_METADATA:
        return this.processStoreMetadataMessageCm(message);
      case UPDATE_METADATA:
        return this.processUpdateMetadataMessageCm(message);
      case REMOVE_METADATA:
        return this.processRemoveMetadataMessageCm(message);
      default:
        return processMessageNm(message);
    }
  }

  /**
   * Handles the case of attaching metadata to an inode. Action store metadata
   * has to be followed by an inode mutation (i.e. add an entry to
   * hdfs_metadata_log table) so that elastic rivers will pick up the already
   * indexed inode, but this time along with its attached metadata.
   * Those two actions have to be executed atomically (either all or nothing)
   * <p/>
   * @param message. The incoming message
   * @return
   * @throws ApplicationException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Message processStoreMetadataMessageCm(Message message) throws
          ApplicationException {

    List<EntityIntf> composite = ((StoreMetadataMessage) message).
            superParseSchema();
    
    //variables not set in the json message
    if(composite == null){
      throw new ApplicationException("Incorrect json message/Missing values");
    }
    List<EntityIntf> rawData = message.parseSchema();

    //Persist metadata
    Inode inode = this.utils.storeRawData(composite, rawData);

    return this.InodeMutation(inode, message);
  }

  /**
   * Handles the case of updating existing inode metadata. This action has to be
   * followed by an inode mutation (i.e. add an entry to hdfs_metadata_log
   * table) so that elastic rivers will pick up the already indexed inode
   * <p/>
   * @param message
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Message processUpdateMetadataMessageCm(Message message) throws
          ApplicationException {

    List<EntityIntf> composite = ((UpdateMetadataMessage) message).
            superParseSchema();

    if(composite == null){
      throw new ApplicationException("Incorrect json message/Missing values");
    }
    //update metadata
    Metadata metadata = (Metadata) message.parseSchema().get(0);
    Inode inode = this.utils.updateMetadata(composite, metadata.getMetadataPK().getId(), metadata.getData());

    return this.InodeMutation(inode, message);

  }
  
  
    /**
   * Handles the case of deleting existing inode metadata. 
   * This will delete the metadata from the table only.
   * This will not remove the elastic index of this record.
   * <p/>
   * @param message
   * @return
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Message processRemoveMetadataMessageCm(Message message) throws
          ApplicationException {

    List<EntityIntf> composite = ((RemoveMetadataMessage) message).
            superParseSchema();

    if(composite == null){
      throw new ApplicationException("Incorrect json message/Missing values");
    }
    //delete metadata
    Metadata metadata = (Metadata) message.parseSchema().get(0);
    Inode inode = this.utils.removeMetadata(composite, metadata.getMetadataPK().getId(), metadata.getData());

    return this.InodeMutation(inode, message);

  }
  
  

  /**
   * Processes incoming messages according to the command they carry, and
   * produces the appropriate message response
   * <p/>
   * @param message. The incoming message
   * @return
   * @throws ApplicationException
   */
  private Message processMessageNm(Message message) throws ApplicationException {

    Command action = Command.valueOf(message.getAction().toUpperCase());

    switch (action) {
      /*
       * saves either a metadata field, or a whole template schema to the
       * database
       */
      case ADD_NEW_TEMPLATE:
        return this.builder.addNewTemplate(message);

      case REMOVE_TEMPLATE:
        return this.builder.removeTemplate(message);

      case UPDATE_TEMPLATE_NAME:
        return this.builder.updateTemplateName(message);

      case STORE_FIELD:
      case EXTEND_TEMPLATE:
      case STORE_TEMPLATE:
        this.builder.storeSchema(message);
        //create and send the new schema back to everyone
        return this.builder.createSchema(message);

      case FETCH_TEMPLATE:
        //create and send the new schema back to everyone
        return this.builder.createSchema(message);

      case FETCH_TEMPLATES:
        return this.builder.fetchTemplates(message);

      case DELETE_TABLE:
        MTable table = (MTable) message.parseSchema().get(0);
        this.builder.checkDeleteTable(table);
        return this.builder.createSchema(message);

      case DELETE_FIELD:
        Field field = ((MTable) message.parseSchema().get(0)).getFields().
                get(0);
        this.builder.checkDeleteField(field);
        return this.builder.createSchema(message);

      case FETCH_METADATA:
        //need to fetch the metadata a table carries for a specific inode
        //joining two different entities
        InodeTableComposite itc = (InodeTableComposite) message.parseSchema().
                get(0);
        return this.builder.fetchInodeMetadata(itc);

      case FETCH_TABLE_METADATA:
        table = (MTable) message.parseSchema().get(0);
        return this.builder.fetchTableMetadata(table);

      case FETCH_FIELD_TYPES:
        return this.builder.fetchFieldTypes(message);

      case BROADCAST:
      case TEST:
      case QUIT:
        return new TextMessage(message.getSender(), message.getMessage());

      case IS_TABLE_EMPTY:
        table = (MTable) message.parseSchema().get(0);
        return this.builder.checkTableFields(table);

      case IS_FIELD_EMPTY:
        field = (Field) message.parseSchema().get(0);
        return this.builder.checkFieldContents(field);
    }

    return new TextMessage();
  }

  private Message InodeMutation(Inode inode, Message message) throws
          ApplicationException {

    //create a meta-log
    MetadataLogMessage msg = (MetadataLogMessage) ((MetadataMessage) message).
            getMetadataLogMessage(inode);

    HdfsMetadataLog log = (HdfsMetadataLog) msg.parseSchema().get(0);

    return this.builder.inodeMutationResponse(log);
  }
}
