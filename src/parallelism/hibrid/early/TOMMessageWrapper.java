package parallelism.hibrid.early;

import bftsmart.tom.core.messages.TOMMessage;
import parallelism.MessageContextPair;

/**
 *
 * @author eduardo
 */
public class TOMMessageWrapper extends TOMMessage{

    public MessageContextPair msg = null;
    
    
    public TOMMessageWrapper(MessageContextPair msg) {
        this.msg = msg;
    }
    
    @Override
     public int getGroupId() {
         return msg.classId;
     }
}
