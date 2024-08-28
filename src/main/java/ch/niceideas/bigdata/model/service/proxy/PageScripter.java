package ch.niceideas.bigdata.model.service.proxy;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PageScripter {

    private final String resourceUrl;
    private final String script;
}
