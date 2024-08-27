package ch.niceideas.bigdata.controlers;

import ch.niceideas.bigdata.model.AbstractInformationHolder;
import ch.niceideas.bigdata.utils.ReturnStatusHelper;
import org.springframework.stereotype.Controller;


@Controller
public class AbstractInformationController<T, R>  {

    String getLastlineElement(AbstractInformationHolder<T, R> is) {
        return lastLine(is.getLastElement());
    }

    String lastLine(Integer lastLine) {
        return ReturnStatusHelper.createOKStatus(map -> map.put("lastLine", lastLine));
    }

    public String clear(AbstractInformationHolder<T, R> is) {
        is.clear();
        return ReturnStatusHelper.createOKStatus();
    }
}
